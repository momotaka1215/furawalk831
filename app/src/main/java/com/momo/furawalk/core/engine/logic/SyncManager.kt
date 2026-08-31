package com.momo.furawalk.core.engine.logic

import com.momo.furawalk.core.domain.repository.WorldRepository
import com.momo.furawalk.data.local.room.dao.AvatarDao
import com.momo.furawalk.data.local.room.entity.AvatarEntity
import com.momo.furawalk.data.remote.api.WorldApi
import com.momo.furawalk.data.remote.model.AvatarDto
import com.momo.furawalk.data.remote.model.CheckpointDto
import kotlinx.serialization.json.*
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * サーバーとローカルデータの同期を制御するマネージャー
 */
class SyncManager(
    private val worldApi: WorldApi,
    private val worldRepository: WorldRepository,
    private val avatarDao: AvatarDao,
    private val avatarDir: File
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun syncWorldData(urls: List<String>, onProgress: (String) -> Unit = {}): Int = withContext(Dispatchers.IO) {
        if (!avatarDir.exists()) avatarDir.mkdirs()

        val allCollectedCheckpoints = mutableListOf<CheckpointDto>()
        val allCollectedAvatars = mutableListOf<AvatarDto>()
        var successCount = 0

        for (url in urls) {
            try {
                val fileName = url.substringAfterLast("/")
                withContext(Dispatchers.Main) { onProgress(fileName) }
                Log.d("SyncManager", "Attempting sync with: $url")
                val responseBody = worldApi.fetchCheckpoints(url)
                val rawData = responseBody.string()
                
                if (rawData.isBlank()) {
                    Log.w("SyncManager", "Received empty response from: $url")
                    continue
                }

                val cleanJson = sanitizeJson(rawData)
                val (cps, avs) = parseJson(cleanJson)
                
                allCollectedCheckpoints.addAll(cps)
                allCollectedAvatars.addAll(avs)
                successCount++

                Log.d("SyncManager", "Sync partial success from $url. Collected CP: ${cps.size}")
            } catch (e: Exception) {
                val errorMsg = "Sync failed for $url: ${e.javaClass.simpleName} - ${e.message}"
                Log.e("SyncManager", errorMsg)
                println("SyncManager Error: $errorMsg")
                e.printStackTrace()
            }
        }

        applyCollectedData(allCollectedCheckpoints, allCollectedAvatars)

        if (allCollectedCheckpoints.isNotEmpty()) {
            return@withContext allCollectedCheckpoints.distinctBy { it.id }.size
        }
        
        return@withContext if (successCount > 0) 0 else -1
    }

    suspend fun syncFromAssets(jsonStr: String) = withContext(Dispatchers.IO) {
        val (cps, avs) = parseJson(jsonStr)
        applyCollectedData(cps, avs)
    }

    private fun parseJson(rawData: String): Pair<List<CheckpointDto>, List<AvatarDto>> {
        val cleanJson = sanitizeJson(rawData)
        val checkpoints = mutableListOf<CheckpointDto>()
        val avatars = mutableListOf<AvatarDto>()
        
        try {
            val rootElement = json.parseToJsonElement(cleanJson)
            extractData(rootElement, checkpoints, avatars)
        } catch (e: Exception) {
            val errorMsg = "Json element parsing failed. Error detail: ${e.message}"
            Log.e("SyncManager", errorMsg)
            println("SyncManager Error: $errorMsg")
            e.printStackTrace()
            fallbackExtract(cleanJson, checkpoints)
        }
        return checkpoints to avatars
    }

    private suspend fun applyCollectedData(checkpoints: List<CheckpointDto>, avatars: List<AvatarDto>) = withContext(Dispatchers.IO) {
        if (checkpoints.isNotEmpty()) {
            val uniqueCheckpoints = checkpoints.distinctBy { it.id }
            val domainCheckpoints = uniqueCheckpoints.map { it.toDomain() }
            worldRepository.updateCheckpoints(domainCheckpoints)
        }

        val uniqueAvatars = avatars.distinctBy { it.id }
        uniqueAvatars.map { dto ->
            async { processAvatar(dto) }
        }.awaitAll()
    }

    private suspend fun processAvatar(dto: AvatarDto) {
        val existing = avatarDao.getAvatarById(dto.id)
        val fileName = "avatar_${dto.id}_${dto.url.substringAfterLast("/")}"
        val localFile = File(avatarDir, fileName)

        if (existing == null || !existing.isDownloaded || !localFile.exists()) {
            try {
                downloadFile(dto.url, localFile)
                val entity = AvatarEntity(
                    id = dto.id,
                    name = dto.name,
                    remoteUrl = dto.url,
                    localPath = localFile.absolutePath,
                    isDownloaded = true
                )
                avatarDao.insertAvatar(entity)
                Log.d("SyncManager", "Downloaded avatar: ${dto.name}")
            } catch (e: Exception) {
                Log.e("SyncManager", "Failed to download avatar ${dto.name}: ${e.message}")
                // ダウンロード失敗してもメタデータだけは保存しておく（後でリトライ可能にするため）
                if (existing == null) {
                    avatarDao.insertAvatar(AvatarEntity(dto.id, dto.name, dto.url))
                }
            }
        }
    }

    private fun downloadFile(urlStr: String, targetFile: File) {
        val url = URL(urlStr)
        url.openStream().use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun sanitizeJson(rawData: String): String {
        var json = rawData.trim()
        if (json.startsWith("//")) {
            json = json.substringAfter("\n")
        }
        return json.replace("\uFEFF", "").trim()
    }

    private fun extractData(element: JsonElement, checkpoints: MutableList<CheckpointDto>, avatars: MutableList<AvatarDto>) {
        when (element) {
            is JsonArray -> {
                for (item in element) {
                    extractData(item, checkpoints, avatars)
                }
            }
            is JsonObject -> {
                // アバター配列を直接探す
                element["avatars"]?.jsonArray?.let { array ->
                    for (item in array) {
                        try {
                            avatars.add(json.decodeFromJsonElement<AvatarDto>(item))
                        } catch (e: Exception) {
                            Log.e("SyncManager", "Failed to decode AvatarDto: ${e.message}")
                        }
                    }
                }

                // チェックポイントを試みる
                try {
                    val dto = json.decodeFromJsonElement<CheckpointDto>(element)
                    if (dto.id.isNotEmpty()) checkpoints.add(dto)
                } catch (_: Exception) {
                    // 他のフィールドを探索
                    for (value in element.values) {
                        if (value is JsonArray || value is JsonObject) {
                            extractData(value, checkpoints, avatars)
                        }
                    }
                }
            }
            else -> {}
        }
    }

    private fun fallbackExtract(jsonStr: String, list: MutableList<CheckpointDto>) {
        val regex = Regex("\\{[^\\}]*\"id\"\\s*:[^\\}]*\\}")
        val matches = regex.findAll(jsonStr)
        for (match in matches) {
            try {
                val dto = json.decodeFromString<CheckpointDto>(match.value)
                list.add(dto)
            } catch (_: Exception) {}
        }
    }
}
