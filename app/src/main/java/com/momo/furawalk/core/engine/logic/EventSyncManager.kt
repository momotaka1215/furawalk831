package com.momo.furawalk.core.engine.logic

import android.util.Log
import com.momo.furawalk.data.local.room.dao.EventDao
import com.momo.furawalk.data.local.room.entity.EventEntity
import com.momo.furawalk.data.remote.api.WorldApi
import com.momo.furawalk.data.remote.model.EventDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

class EventSyncManager(
    private val worldApi: WorldApi,
    private val eventDao: EventDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun syncEventData(url: String, onProgress: (String) -> Unit = {}) = withContext(Dispatchers.IO) {
        try {
            val fileName = url.substringAfterLast("/")
            withContext(Dispatchers.Main) { onProgress(fileName) }
            val responseBody = worldApi.fetchEventData(url)
            val rawData = responseBody.string()
            val root = json.parseToJsonElement(rawData)
            
            // 下位互換性: ルートが配列ならそのまま、オブジェクトなら "quests" キーを探す
            val dtos = if (root is JsonArray) {
                json.decodeFromJsonElement<List<EventDto>>(root)
            } else {
                root.jsonObject["quests"]?.let {
                    json.decodeFromJsonElement<List<EventDto>>(it)
                } ?: emptyList()
            }
            
            val entities = dtos.map { dto ->
                EventEntity(
                    id = dto.id,
                    title = dto.title,
                    description = dto.description,
                    bonusHeso = dto.bonusHeso,
                    bonusExp = dto.bonusExp,
                    rewardItemId = dto.rewardItemId,
                    startDate = dto.startDate,
                    endDate = dto.endDate,
                    iconEmoji = dto.iconEmoji,
                    targetCheckpointId = dto.targetCheckpointId,
                    conditionType = dto.conditionType,
                    conditionValue = dto.conditionValue
                )
            }
            eventDao.insertAll(entities)
            Log.d("EventSyncManager", "Synced ${entities.size} events")
        } catch (e: Exception) {
            Log.e("EventSyncManager", "Failed to sync events from $url: ${e.message}")
            e.printStackTrace()
        }
    }
}
