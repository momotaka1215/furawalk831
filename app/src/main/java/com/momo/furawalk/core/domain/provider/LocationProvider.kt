package com.momo.furawalk.core.domain.provider

import kotlinx.coroutines.flow.StateFlow

/**
 * GPSから取得可能な詳細情報を保持するデータクラス
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val altitude: Double,
    val speed: Float,
    val bearing: Float,
    val time: Long,
    val verticalAccuracy: Float? = null,
    val speedAccuracy: Float? = null,
    val bearingAccuracy: Float? = null,
    val isFromMock: Boolean = false
)

interface LocationProvider {
    val location: StateFlow<LocationData?>
    val currentDistance: StateFlow<Double> // 今回のセッションでの累計移動距離(m)
    
    fun startTracking()
    fun stopTracking()
    fun resetDistance()
}
