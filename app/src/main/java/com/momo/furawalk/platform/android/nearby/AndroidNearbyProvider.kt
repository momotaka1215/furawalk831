package com.momo.furawalk.platform.android.nearby

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.momo.furawalk.core.domain.provider.MetPlayer
import com.momo.furawalk.core.domain.provider.NearbyProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Nearby Connections APIを使用したすれ違い通信のAndroid実装
 *
 * 設計方針:
 * - Production Ready: エラーハンドリング、メモリリーク防止、リソース解放の徹底
 * - SOLID: 通信ロジックとデータ変換の分離（内部データ構造 NearbyPayload）
 * - Kotlin Idiomatic: StateFlowによるリアクティブかつスレッドセーフな状態管理
 */
class AndroidNearbyProvider(
    context: Context,
    // テスタビリティ向上のための DI 対応。applicationContext でリーク防止
    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context.applicationContext)
) : NearbyProvider {

    /**
     * 通信で交換するプレイヤー情報のシリアライズ用データ構造
     * クラス内部にカプセル化することで、名前衝突を回避
     */
    @Serializable
    private data class NearbyPayload(
        val userId: String,
        val name: String,
        val level: Int,
        val totalDistance: Double,
        val message: String,
        val avatarPath: String?
    ) {
        fun toBytes(): ByteArray {
            return Json.encodeToString(this).toByteArray(Charsets.UTF_8)
        }

        companion object {
            private val jsonFormat = Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }

            fun fromBytes(bytes: ByteArray): NearbyPayload? {
                return runCatching {
                    jsonFormat.decodeFromString<NearbyPayload>(String(bytes, Charsets.UTF_8))
                }.getOrNull()
            }
        }
    }

    companion object {
        private const val TAG = "NearbyProvider"
        // BLEパケット制限回避のためサービスIDを短縮
        private const val SERVICE_ID = "com.momo.fw.sc"
        // すれ違い通信には多対多のCLUSTER戦略が最適
        private val STRATEGY = Strategy.P2P_CLUSTER
        private const val MAX_MET_PLAYERS_CACHE = 15
    }

    private val _metPlayers = MutableStateFlow<List<MetPlayer>>(emptyList())
    override val metPlayers: StateFlow<List<MetPlayer>> = _metPlayers.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // 端末・セッション固有の UUID
    private val myUserId: String = UUID.randomUUID().toString()
    private var currentMyPayload: NearbyPayload? = null

    /**
     * Nearby Connections の 131 バイト制限を回避するための短い Endpoint 名を取得
     */
    private fun getShortEndpointName(name: String): String {
        val safeName = name.take(10)
        val shortId = myUserId.take(8)
        return "$safeName#$shortId"
    }

    // --- Callbacks ---

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.d(TAG, "Connection initiated: ${connectionInfo.endpointName} ($endpointId)")
            // 自動承認
            connectionsClient.acceptConnection(endpointId, payloadCallback)
                .addOnFailureListener { e -> Log.e(TAG, "Failed to accept connection from $endpointId", e) }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.i(TAG, "Connection established with $endpointId. Sending payload...")
                    sendMyProfile(endpointId)
                }
                else -> {
                    Log.w(TAG, "Connection failed with $endpointId: ${result.status.statusMessage}")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Disconnected from $endpointId")
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                payload.asBytes()?.let { bytes ->
                    val received = NearbyPayload.fromBytes(bytes)
                    if (received != null) {
                        handleMetPlayer(received)
                    } else {
                        Log.w(TAG, "Received invalid payload from $endpointId")
                    }
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            if (update.status == PayloadTransferUpdate.Status.FAILURE) {
                Log.w(TAG, "Payload transfer failed for $endpointId")
            }
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.i(TAG, "New endpoint found: ${info.endpointName}")
            val myPayload = currentMyPayload ?: return

            connectionsClient.requestConnection(
                getShortEndpointName(myPayload.name),
                endpointId,
                connectionLifecycleCallback
            ).addOnFailureListener { e ->
                Log.e(TAG, "Connection request failed for $endpointId", e)
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Endpoint lost: $endpointId")
        }
    }

    // --- API Implementation ---

    override fun startAdvertising(
        playerName: String,
        level: Int,
        totalDistance: Double,
        greeting: String,
        avatarPath: String?
    ) {
        val payload = NearbyPayload(
            userId = myUserId,
            name = playerName,
            level = level,
            totalDistance = totalDistance,
            message = greeting,
            avatarPath = avatarPath
        )
        currentMyPayload = payload

        stopAll() // 既存通信のリセット

        val options = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startAdvertising(
            getShortEndpointName(playerName),
            SERVICE_ID,
            connectionLifecycleCallback,
            options
        ).addOnSuccessListener {
            Log.i(TAG, "Advertising started successfully")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Advertising start failed", e)
        }
    }

    override fun startDiscovery() {
        if (_isScanning.value) return

        val options = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startDiscovery(
            SERVICE_ID,
            endpointDiscoveryCallback,
            options
        ).addOnSuccessListener {
            Log.i(TAG, "Discovery started successfully")
            _isScanning.value = true
        }.addOnFailureListener { e ->
            Log.e(TAG, "Discovery start failed", e)
            _isScanning.value = false
        }
    }

    override fun stopAll() {
        Log.i(TAG, "Stopping all Nearby services...")
        runCatching {
            connectionsClient.stopAdvertising()
            connectionsClient.stopDiscovery()
            connectionsClient.stopAllEndpoints()
        }.onFailure { e ->
            Log.e(TAG, "Error stopping Nearby services", e)
        }
        _isScanning.value = false
    }

    // --- Private Helpers ---

    private fun sendMyProfile(endpointId: String) {
        val payload = currentMyPayload ?: return
        connectionsClient.sendPayload(
            endpointId,
            Payload.fromBytes(payload.toBytes())
        ).addOnFailureListener { e ->
            Log.e(TAG, "Failed to send profile to $endpointId", e)
        }
    }

    private fun handleMetPlayer(payload: NearbyPayload) {
        // 自分自身のデータはスキップ
        if (payload.userId == myUserId) return

        val newPlayer = MetPlayer(
            id = payload.userId, // UUID を使用
            name = payload.name,
            level = payload.level,
            totalDistance = payload.totalDistance,
            message = payload.message,
            avatarPath = payload.avatarPath,
            timestamp = System.currentTimeMillis()
        )

        _metPlayers.update { currentList ->
            // UUID基準で重複排除
            if (currentList.any { it.id == newPlayer.id }) {
                currentList
            } else {
                Log.i(TAG, "🤝 Confirmed encounter with: ${newPlayer.name} (ID: ${newPlayer.id})")
                (listOf(newPlayer) + currentList).take(MAX_MET_PLAYERS_CACHE)
            }
        }
    }
}
