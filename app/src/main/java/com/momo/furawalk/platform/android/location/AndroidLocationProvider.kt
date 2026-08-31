package com.momo.furawalk.platform.android.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.location.*
import com.momo.furawalk.core.domain.provider.LocationData
import com.momo.furawalk.core.domain.provider.LocationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class AndroidLocationProvider(context: Context) : LocationProvider {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext)

    private val _location = MutableStateFlow<LocationData?>(null)
    override val location: StateFlow<LocationData?> = _location.asStateFlow()

    private val _currentDistance = MutableStateFlow(0.0)
    override val currentDistance: StateFlow<Double> = _currentDistance.asStateFlow()

    private val lock = Any()
    private var isTracking = false

    // --- 1. 移動状態の定義 ---
    private enum class MovementState {
        STATIONARY,             // 停止中
        MOVING_CANDIDATE,       // 移動開始の兆候
        MOVING,                 // 移動中
        MOVING_STOP_CANDIDATE   // 停止の兆候
    }

    private var currentState = MovementState.STATIONARY
    private var lastAcceptedLocation: Location? = null // 時系列計算用
    private var gameAnchorLocation: Location? = null   // 距離加算の基準点

    private var rawGpsDistance = 0.0
    private var acceptedGameDistance = 0.0

    // 連続判定用のカウンタ
    private var consecutiveMoveCount = 0
    private var consecutiveStopCount = 0

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            synchronized(lock) {
                if (!isTracking) return
                // 8. タイムスタンプ順にソート
                result.locations
                    .sortedBy { it.elapsedRealtimeNanos }
                    .forEach(::processLocation)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun startTracking() {
        synchronized(lock) {
            if (isTracking) return
            resetInternalState()
            isTracking = true

            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MILLIS)
                .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MILLIS)
                .setWaitForAccurateLocation(true)
                .build()

            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        }
    }

    override fun stopTracking() {
        synchronized(lock) {
            isTracking = false
            fusedLocationClient.removeLocationUpdates(locationCallback)
            resetInternalState()
        }
    }

    override fun resetDistance() {
        synchronized(lock) {
            _currentDistance.value = 0.0
            acceptedGameDistance = 0.0
            rawGpsDistance = 0.0
            resetInternalState()
        }
    }

    private fun resetInternalState() {
        lastAcceptedLocation = null
        gameAnchorLocation = null
        currentState = MovementState.STATIONARY
        consecutiveMoveCount = 0
        consecutiveStopCount = 0
    }

    private fun processLocation(current: Location) {
        // A. 品質・鮮度チェック
        if (!isUsableLocation(current)) return
        val ageSec = (SystemClock.elapsedRealtimeNanos() - current.elapsedRealtimeNanos) / NANOS_PER_SECOND
        if (ageSec > MAX_LOCATION_AGE_SECONDS || ageSec < 0) return

        val prev = lastAcceptedLocation
        if (prev == null) {
            setInitialAnchor(current)
            return
        }

        // 時系列整合性チェック（過去データや同時刻データの除外）
        if (current.elapsedRealtimeNanos <= prev.elapsedRealtimeNanos) return

        val deltaDist = prev.distanceTo(current).toDouble()
        val deltaSec = (current.elapsedRealtimeNanos - prev.elapsedRealtimeNanos) / NANOS_PER_SECOND
        val calcSpeed = if (deltaSec > 0) deltaDist / deltaSec else 0.0

        // 異常ジャンプ判定（破棄しても前回のタイムスタンプ位置を更新し、次回の計算破綻を防ぐ）
        if (calcSpeed > MAX_WALKING_SPEED_MPS) {
            logDecision(current, "REJECTED", "GPS Jump (${String.format(Locale.US, "%.1f", calcSpeed)}m/s)", deltaDist)
            lastAcceptedLocation = current // タイムスタンプの途絶防止
            return
        }

        // GPS通信途絶（復帰時はアンカーを再設定）
        if (deltaSec > MAX_GAP_SECONDS) {
            setInitialAnchor(current)
            return
        }

        rawGpsDistance += deltaDist

        // --- 総合的な移動判定指標 ---
        val anchorDist = gameAnchorLocation?.distanceTo(current)?.toDouble() ?: 0.0
        val driftThreshold = Math.max(MIN_MOVE_THRESHOLD, current.accuracy * 1.5)
        val isPhysicallyMoving = anchorDist > driftThreshold && current.speed > STOP_SPEED_MPS

        val oldState = currentState

        when (currentState) {
            MovementState.STATIONARY -> {
                if (isPhysicallyMoving) {
                    consecutiveMoveCount++
                    if (consecutiveMoveCount >= REQUIRED_CONSECUTIVE_MOVES) {
                        currentState = MovementState.MOVING_CANDIDATE
                    }
                } else {
                    consecutiveMoveCount = 0
                    // 静止中はアンカーを現在地に引き寄せて漂流（ドリフト）ノイズを徐々に吸収
                    gameAnchorLocation = current
                }
            }

            MovementState.MOVING_CANDIDATE -> {
                if (isPhysicallyMoving) {
                    // アンカーからの離脱距離が十分であれば移動中へ移行
                    if (anchorDist > CONFIRMATION_DISPLACEMENT) {
                        currentState = MovementState.MOVING
                        consecutiveMoveCount = 0
                        // 確定した段階で、アンカーからの直線変位を加算（ノイズを含まない実移動量）
                        addGameDistance(anchorDist)
                        gameAnchorLocation = current
                    }
                } else {
                    // ノイズと判定された場合は静止状態にリセット
                    currentState = MovementState.STATIONARY
                    consecutiveMoveCount = 0
                    gameAnchorLocation = current
                }
            }

            MovementState.MOVING -> {
                if (current.speed <= STOP_SPEED_MPS || deltaDist < MIN_MOVE_THRESHOLD) {
                    currentState = MovementState.MOVING_STOP_CANDIDATE
                    consecutiveStopCount = 1
                } else {
                    // MOVING中も精度確認（高精度時のみ距離を加算）
                    if (current.accuracy <= MAX_ACCURACY_METERS) {
                        addGameDistance(deltaDist)
                        gameAnchorLocation = current
                    }
                }
            }

            MovementState.MOVING_STOP_CANDIDATE -> {
                if (current.speed <= STOP_SPEED_MPS) {
                    consecutiveStopCount++
                    if (consecutiveStopCount >= REQUIRED_CONSECUTIVE_STOPS) {
                        currentState = MovementState.STATIONARY
                        consecutiveStopCount = 0
                        gameAnchorLocation = current
                    }
                } else if (isPhysicallyMoving) {
                    currentState = MovementState.MOVING
                    consecutiveStopCount = 0
                    addGameDistance(deltaDist)
                    gameAnchorLocation = current
                }
            }
        }

        lastAcceptedLocation = current
        _location.value = current.toLocationData()

        logDecision(current, if (oldState != currentState) "STATE_CHANGE" else "PROCESS", "OK", deltaDist)
    }

    private fun setInitialAnchor(location: Location) {
        lastAcceptedLocation = location
        gameAnchorLocation = location
        currentState = MovementState.STATIONARY
        consecutiveMoveCount = 0
        consecutiveStopCount = 0
        _location.value = location.toLocationData()
    }

    private fun addGameDistance(meters: Double) {
        if (meters > 0) {
            acceptedGameDistance += meters
            _currentDistance.value = acceptedGameDistance
        }
    }

    private fun logDecision(current: Location, result: String, reason: String, delta: Double) {
        val anchorDist = gameAnchorLocation?.distanceTo(current) ?: 0f
        val prev = lastAcceptedLocation
        val calcSpeed = if (prev != null) {
            val dt = (current.elapsedRealtimeNanos - prev.elapsedRealtimeNanos) / NANOS_PER_SECOND
            if (dt > 0) delta / dt else 0.0
        } else 0.0

        Log.d(TAG, String.format(Locale.US,
            "[%s] %s | State: %s | Acc: %.1fm | Spd: %.2f (Calc: %.2f) | Delta: %.1fm | AnchorDist: %.1fm | GameDist: %.1fm (Raw: %.1fm)",
            result, reason, currentState.name, current.accuracy, current.speed,
            calcSpeed, delta, anchorDist, acceptedGameDistance, rawGpsDistance
        ))
    }

    private fun isUsableLocation(l: Location) = l.hasAccuracy() && l.accuracy <= MAX_ACCURACY_METERS

    private fun Location.toLocationData() = LocationData(
        latitude = latitude, longitude = longitude, accuracy = accuracy, altitude = altitude,
        speed = speed, bearing = bearing, time = time, isFromMock = isFromMockProvider(),
        verticalAccuracy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) verticalAccuracyMeters else null
    )

    @Suppress("DEPRECATION")
    private fun Location.isFromMockProvider(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) isMock else isFromMockProvider

    private companion object {
        const val TAG = "LocationProvider"
        const val UPDATE_INTERVAL_MILLIS = 2000L
        const val MIN_UPDATE_INTERVAL_MILLIS = 1000L
        const val NANOS_PER_SECOND = 1_000_000_000.0

        const val MAX_ACCURACY_METERS = 25f        // 25m以上の誤差は破棄
        const val MAX_WALKING_SPEED_MPS = 4.2f      // 約15km/h (徒歩・走りの上限)
        const val STOP_SPEED_MPS = 0.3f            // 停止とみなす閾値速度
        const val MAX_LOCATION_AGE_SECONDS = 5.0
        const val MAX_GAP_SECONDS = 30.0

        const val MIN_MOVE_THRESHOLD = 2.0          // 最小判定移動距離 (m)
        const val REQUIRED_CONSECUTIVE_MOVES = 2    // 移動開始に必要な連続回数
        const val REQUIRED_CONSECUTIVE_STOPS = 3    // 停止確定に必要な連続回数
        const val CONFIRMATION_DISPLACEMENT = 10.0 // MOVING確定に必要な最小基準点離脱距離 (m)
    }
}