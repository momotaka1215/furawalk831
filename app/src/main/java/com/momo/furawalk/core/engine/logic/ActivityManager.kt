package com.momo.furawalk.core.engine.logic

import com.momo.furawalk.data.local.room.dao.PlayerDao
import com.momo.furawalk.data.local.room.entity.DailyActivityEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * リアルタイムの歩数・距離データをデータベースに記録・集計するマネージャー
 */
class ActivityManager(
    private val playerDao: PlayerDao,
    private val scope: CoroutineScope,
    private val petNurturingManager: PetNurturingManager? = null
) {
    private var lastSteps = 0
    private var lastDistance = 0.0

    fun startTracking(stepsFlow: StateFlow<Int>, distanceFlow: StateFlow<Double>) {
        scope.launch {
            stepsFlow.collectLatest { currentSteps ->
                val delta = if (lastSteps == 0) 0 else currentSteps - lastSteps
                if (delta > 0) {
                    updateActivity(deltaSteps = delta)
                }
                lastSteps = currentSteps
            }
        }
        scope.launch {
            distanceFlow.collectLatest { currentDistance ->
                val delta = if (lastDistance == 0.0) 0.0 else currentDistance - lastDistance
                if (delta > 0) {
                    updateActivity(deltaDistance = delta)
                }
                lastDistance = currentDistance
            }
        }
    }

    private suspend fun updateActivity(deltaSteps: Int = 0, deltaDistance: Double = 0.0) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        // 1. 日別統計の更新
        val activities = playerDao.getRecentActivity().first()
        val currentActivity = activities.find { it.date == today } ?: DailyActivityEntity(date = today)
            
        playerDao.insertOrUpdateDailyActivity(
            currentActivity.copy(
                steps = currentActivity.steps + deltaSteps,
                distanceMeters = currentActivity.distanceMeters + deltaDistance
            )
        )

        // 2. プレイヤー全体の累計統計の更新
        val profile = playerDao.getPlayerProfile().first()
        profile?.let {
            playerDao.insertOrUpdatePlayer(
                it.copy(
                    totalSteps = it.totalSteps + deltaSteps,
                    totalDistance = it.totalDistance + deltaDistance
                )
            )
        }

        // 3. ペットの成長への反映
        petNurturingManager?.applyActivityGrowth(deltaSteps, deltaDistance)
    }
}
