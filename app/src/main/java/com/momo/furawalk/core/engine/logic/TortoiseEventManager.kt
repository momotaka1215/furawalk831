package com.momo.furawalk.core.engine.logic

import android.content.Context
import android.location.Location
import android.util.Log
import com.momo.furawalk.core.domain.provider.LocationData
import com.momo.furawalk.data.local.room.dao.CheckpointDao
import com.momo.furawalk.data.local.room.dao.PlayerDao
import com.momo.furawalk.data.local.room.dao.TortoiseDao
import com.momo.furawalk.data.local.room.entity.InventoryEntity
import com.momo.furawalk.data.local.room.entity.PlayerCurrencyEntity
import com.momo.furawalk.data.local.room.entity.TortoiseEventStateEntity
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.util.*

@Serializable
data class TortoiseEventConfig(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val settings: TortoiseSettings,
    val stages: List<TortoiseStage>,
    val completionReward: TortoiseReward
)

@Serializable
data class TortoiseSettings(
    val maxRadiusMeters: Int,
    val arrivalRadiusMeters: Int,
    val maxEscapeCount: Int
)

@Serializable
data class TortoiseStage(
    val id: String,
    val name: String,
    val minDistanceMeters: Int,
    val maxDistanceMeters: Int,
    val timeLimitSeconds: Int,
    val rewardHeso: Int
)

@Serializable
data class TortoiseReward(
    val heso: Int
)

class TortoiseEventManager(
    private val tortoiseDao: TortoiseDao,
    private val checkpointDao: CheckpointDao,
    private val playerDao: PlayerDao,
    private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }
    private var config: TortoiseEventConfig? = null

    init {
        loadConfig()
    }

    private fun loadConfig() {
        try {
            val raw = context.assets.open("events.json").bufferedReader().use { it.readText() }
            val root = json.parseToJsonElement(raw).jsonObject
            val complex = root["complex_events"]?.jsonArray
            config = complex?.find { 
                it.jsonObject["id"]?.jsonPrimitive?.content == "achilles_and_tortoise" 
            }?.let { json.decodeFromJsonElement<TortoiseEventConfig>(it) }
        } catch (e: Exception) {
            Log.e("TortoiseEventManager", "Load error: ${e.message}")
        }
    }

    suspend fun startEvent(currentLoc: LocationData) {
        val cfg = config ?: return
        val now = System.currentTimeMillis()
        
        val newState = TortoiseEventStateEntity(
            eventId = cfg.id,
            state = "IN_PROGRESS",
            startLatitude = currentLoc.latitude,
            startLongitude = currentLoc.longitude,
            currentStageIndex = 0,
            startedAt = now,
            escapeCount = 0,
            earnedHeso = 0
        )
        
        val updatedState = selectNextDestination(newState)
        tortoiseDao.insertOrUpdate(updatedState)
    }

    suspend fun update(currentLoc: LocationData?) {
        val state = tortoiseDao.getEventState().first() ?: return
        if (state.state != "IN_PROGRESS") return

        val now = System.currentTimeMillis()

        // 1. タイムアウト判定 (制限時間切れ)
        if (now > state.destinationDeadline) {
            handleTimeout(state)
            return
        }

        // 2. 到着判定
        if (currentLoc != null && state.currentDestinationLatitude != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                currentLoc.latitude, currentLoc.longitude,
                state.currentDestinationLatitude, state.currentDestinationLongitude!!,
                results
            )
            val arrivalRadius = config?.settings?.arrivalRadiusMeters?.toFloat() ?: 30f
            if (results[0] <= arrivalRadius) {
                handleArrival(state)
            }
        }
    }

    private suspend fun handleArrival(state: TortoiseEventStateEntity) {
        val cfg = config ?: return
        val currentStage = cfg.stages.getOrNull(state.currentStageIndex) ?: return
        
        // 報酬付与 (ヘソ)
        val currencies = playerDao.getAllCurrencies().first()
        val money = currencies.find { it.type == "MONEY" }
        if (money != null) {
            playerDao.updateCurrency(money.copy(
                currentAmount = money.currentAmount + currentStage.rewardHeso,
                totalEarned = money.totalEarned + currentStage.rewardHeso
            ))
        }

        val nextIndex = state.currentStageIndex + 1
        if (nextIndex >= cfg.stages.size) {
            // 全ステージクリア (最終演出へ)
            completeEvent(state.copy(
                earnedHeso = state.earnedHeso + currentStage.rewardHeso,
                currentStageIndex = nextIndex,
                state = "COMPLETED"
            ))
        } else {
            // 次のステージへ
            val nextState = selectNextDestination(state.copy(
                currentStageIndex = nextIndex,
                earnedHeso = state.earnedHeso + currentStage.rewardHeso
            ))
            tortoiseDao.insertOrUpdate(nextState)
        }
    }

    private suspend fun handleTimeout(state: TortoiseEventStateEntity) {
        val cfg = config ?: return
        val nextEscapeCount = state.escapeCount + 1
        
        if (nextEscapeCount >= cfg.settings.maxEscapeCount) {
            // 逃走回数上限
            tortoiseDao.insertOrUpdate(state.copy(
                state = "ESCAPED",
                escapeCount = nextEscapeCount
            ))
        } else {
            // 目的地を再設定 (亀が逃げた)
            val nextState = selectNextDestination(state.copy(
                escapeCount = nextEscapeCount
            ))
            tortoiseDao.insertOrUpdate(nextState)
        }
    }

    private suspend fun completeEvent(state: TortoiseEventStateEntity) {
        val cfg = config ?: return
        
        // 最終報酬
        val currencies = playerDao.getAllCurrencies().first()
        val money = currencies.find { it.type == "MONEY" }
        if (money != null) {
            playerDao.updateCurrency(money.copy(
                currentAmount = money.currentAmount + cfg.completionReward.heso,
                totalEarned = money.totalEarned + cfg.completionReward.heso
            ))
        }
        
        // 宝箱 (既存システムがあれば。ここでは簡易的にログと状態更新のみ)
        tortoiseDao.insertOrUpdate(state)
    }

    private suspend fun selectNextDestination(state: TortoiseEventStateEntity): TortoiseEventStateEntity {
        val cfg = config ?: return state
        val stage = cfg.stages.getOrNull(state.currentStageIndex) ?: return state
        
        val startLat = state.startLatitude ?: return state
        val startLon = state.startLongitude ?: return state
        
        val allCheckpoints = checkpointDao.getAllCheckpoints().first()
        val candidates = allCheckpoints.filter { cp ->
            val res = FloatArray(1)
            Location.distanceBetween(startLat, startLon, cp.latitude, cp.longitude, res)
            res[0] >= stage.minDistanceMeters && res[0] <= stage.maxDistanceMeters
        }
        
        if (candidates.isEmpty()) {
            // 候補がない場合のフォールバック: 範囲を広げるか、一番近いのを選ぶ
            val fallback = allCheckpoints.minByOrNull { cp ->
                val res = FloatArray(1)
                Location.distanceBetween(startLat, startLon, cp.latitude, cp.longitude, res)
                Math.abs(res[0] - (stage.minDistanceMeters + stage.maxDistanceMeters) / 2)
            } ?: return state
            
            return applyDestination(state, fallback.id, fallback.name, fallback.latitude, fallback.longitude, stage.timeLimitSeconds)
        }
        
        val target = candidates.random()
        return applyDestination(state, target.id, target.name, target.latitude, target.longitude, stage.timeLimitSeconds)
    }

    private fun applyDestination(
        state: TortoiseEventStateEntity,
        id: String, name: String, lat: Double, lon: Double, limitSec: Int
    ): TortoiseEventStateEntity {
        val now = System.currentTimeMillis()
        return state.copy(
            currentDestinationId = id,
            currentDestinationName = name,
            currentDestinationLatitude = lat,
            currentDestinationLongitude = lon,
            destinationCreatedAt = now,
            destinationDeadline = now + (limitSec * 1000L)
        )
    }

    suspend fun pauseEvent() {
        val state = tortoiseDao.getEventState().first() ?: return
        if (state.state == "IN_PROGRESS") {
            tortoiseDao.insertOrUpdate(state.copy(state = "PAUSED"))
        }
    }

    suspend fun resumeEvent() {
        val state = tortoiseDao.getEventState().first() ?: return
        if (state.state == "PAUSED") {
            tortoiseDao.insertOrUpdate(state.copy(state = "IN_PROGRESS"))
        }
    }

    suspend fun cancelEvent() {
        tortoiseDao.deleteEvent("achilles_and_tortoise")
    }
}
