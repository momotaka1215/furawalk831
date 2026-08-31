package com.momo.furawalk.core.domain.provider

import kotlinx.coroutines.flow.StateFlow

data class MetPlayer(
    val id: String,
    val name: String,
    val level: Int = 1,
    val totalDistance: Double = 0.0,
    val avatarPath: String? = null,
    val message: String = "",
    val timestamp: Long
)

interface NearbyProvider {
    val metPlayers: StateFlow<List<MetPlayer>>
    val isScanning: StateFlow<Boolean>
    
    fun startAdvertising(
        playerName: String, 
        level: Int,
        totalDistance: Double,
        greeting: String = "こんにちは！", 
        avatarPath: String? = null
    )
    fun startDiscovery()
    fun stopAll()
}
