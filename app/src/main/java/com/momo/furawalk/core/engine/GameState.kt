package com.momo.furawalk.core.engine

import com.momo.furawalk.core.domain.model.pet.Pet
import com.momo.furawalk.core.domain.model.map.Checkpoint
import kotlinx.coroutines.flow.StateFlow

/**
 * UI（Compose）が唯一参照する、ゲームの現在の全状態
 */
data class GameState(
    val playerExp: Long = 0,
    val playerMoney: Int = 0,
    val currentPet: Pet? = null,
    val nearestCheckpoint: Checkpoint? = null,
    val distanceToNearest: Float? = null,
    val isTracking: Boolean = false
)

interface GameEngine {
    val state: StateFlow<GameState>
    fun onUserAction(action: GameAction)
}

sealed class GameAction {
    object StartWalk : GameAction()
    object StopWalk : GameAction()
    data class FeedPet(val foodId: String) : GameAction()
}
