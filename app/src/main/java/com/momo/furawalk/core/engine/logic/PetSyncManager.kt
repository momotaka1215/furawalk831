package com.momo.furawalk.core.engine.logic

import android.util.Log
import com.momo.furawalk.data.local.room.dao.PetDao
import com.momo.furawalk.data.local.room.entity.PetSpeciesEntity
import com.momo.furawalk.data.remote.api.WorldApi
import com.momo.furawalk.data.remote.model.PetDataResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class PetSyncManager(
    private val worldApi: WorldApi,
    private val petDao: PetDao,
    private val petImageDir: File
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun syncPetData(url: String, onProgress: (String) -> Unit = {}) = withContext(Dispatchers.IO) {
        if (!petImageDir.exists()) petImageDir.mkdirs()

        try {
            val fileName = url.substringAfterLast("/")
            withContext(Dispatchers.Main) { onProgress(fileName) }
            Log.d("PetSyncManager", "Fetching pet data from: $url")
            val responseBody = worldApi.fetchPetData(url)
            val rawData = responseBody.string()
            val cleanJson = sanitizeJson(rawData)
            
            val response = json.decodeFromString<PetDataResponse>(cleanJson)
            
            for (dto in response.pets) {
                val existing = petDao.getSpeciesById(dto.id)
                var localPath: String? = existing?.localImagePath
                var isDownloaded = existing?.isDownloaded ?: false

                if (dto.imageUrl.isNotEmpty() && (!localFileExists(localPath) || !isDownloaded)) {
                    try {
                        val fileName = "pet_${dto.id}_${dto.imageUrl.substringAfterLast("/")}"
                        val localFile = File(petImageDir, fileName)
                        downloadFile(dto.imageUrl, localFile)
                        localPath = localFile.absolutePath
                        isDownloaded = true
                    } catch (e: Exception) {
                        Log.e("PetSyncManager", "Failed to download pet image: ${e.message}")
                    }
                }

                val entity = PetSpeciesEntity(
                    id = dto.id,
                    name = dto.name,
                    species = dto.species,
                    rarity = dto.rarity,
                    description = "", // JSONに説明がない場合は空
                    type1Description = when(dto.species) {
                        "dog" -> "イヌ科"
                        "cat" -> "ネコ科"
                        "monkey" -> "霊長類"
                        else -> "不明"
                    },
                    type2Description = "レア度:${dto.rarity}",
                    iconEmoji = when(dto.species) {
                        "dog" -> "🐕"
                        "cat" -> "🐈"
                        "monkey" -> "🐒"
                        else -> "🐾"
                    },
                    imageUrl = dto.imageUrl,
                    localImagePath = localPath,
                    isDownloaded = isDownloaded,
                    baseHp = dto.baseStatus.hp,
                    baseStamina = dto.baseStatus.stamina,
                    baseSpeed = dto.baseStatus.speed,
                    basePower = dto.baseStatus.power,
                    baseIntelligence = dto.baseStatus.intelligence,
                    firstPerson = dto.speech?.firstPerson ?: "ぼく",
                    ending = dto.speech?.ending ?: "！",
                    style = dto.speech?.style ?: "friendly"
                )
                petDao.insertOrUpdateSpecies(entity)
            }
            Log.d("PetSyncManager", "Pet sync complete. Species: ${response.pets.size}")
        } catch (e: Exception) {
            Log.e("PetSyncManager", "Pet sync failed for $url: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun localFileExists(path: String?): Boolean {
        return path?.let { File(it).exists() } ?: false
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
}
