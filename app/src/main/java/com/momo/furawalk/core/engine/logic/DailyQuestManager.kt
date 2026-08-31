package com.momo.furawalk.core.engine.logic

import android.location.Location
import com.momo.furawalk.core.domain.model.map.Checkpoint
import com.momo.furawalk.core.domain.provider.LocationData
import com.momo.furawalk.data.local.room.dao.PlayerDao
import com.momo.furawalk.data.local.room.entity.DailyQuestEntity
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class DailyQuestManager(
    private val playerDao: PlayerDao
) {
    /**
     * 今日のデイリークエストが未設定の場合、現在地から最も近い目的地をデイリークエストとして設定する
     */
    suspend fun checkAndSetDailyQuest(
        currentLocation: LocationData?,
        allCheckpoints: List<Checkpoint>
    ): DailyQuestEntity? {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val existingQuest = playerDao.getDailyQuest(today).first()
        
        if (existingQuest != null) return existingQuest
        
        // 未設定の場合かつ位置情報・目的地リストがある場合のみ設定
        if (currentLocation == null || allCheckpoints.isEmpty()) return null
        
        val nearest = allCheckpoints.minByOrNull { cp ->
            val results = FloatArray(1)
            Location.distanceBetween(
                currentLocation.latitude, currentLocation.longitude,
                cp.latitude, cp.longitude,
                results
            )
            results[0]
        } ?: return null
        
        val newQuest = DailyQuestEntity(
            date = today,
            checkpointId = nearest.id,
            isCompleted = false
        )
        playerDao.insertOrUpdateDailyQuest(newQuest)
        return newQuest
    }

    suspend fun completeDailyQuest(checkpointId: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val quest = playerDao.getDailyQuest(today).first()
        if (quest != null && quest.checkpointId == checkpointId && !quest.isCompleted) {
            playerDao.insertOrUpdateDailyQuest(quest.copy(isCompleted = true))
        }
    }
}
