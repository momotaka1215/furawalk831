package com.momo.furawalk.core.engine.logic

import com.momo.furawalk.data.local.room.dao.GrowthDao
import com.momo.furawalk.data.local.room.dao.PetDao
import com.momo.furawalk.data.local.room.dao.PlayerDao
import com.momo.furawalk.data.local.room.dao.ShopDao
import com.momo.furawalk.data.local.room.entity.InventoryEntity
import com.momo.furawalk.data.local.room.entity.PetEntity
import com.momo.furawalk.data.local.room.entity.PetGrowthRecordEntity
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * ペットの育成ロジックを統合管理するマネージャー
 */
class PetNurturingManager(
    private val petDao: PetDao,
    private val playerDao: PlayerDao,
    private val shopDao: ShopDao,
    private val growthDao: GrowthDao? = null
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * アイテムを使用してペットにアクションを起こす
     */
    suspend fun useItemOnPet(itemId: String): Boolean {
        val inventory = playerDao.getInventory().first()
        val item = inventory.find { it.itemId == itemId } ?: return false
        
        if (item.quantity <= 0) return false
        
        val pet = petDao.getPetStatus().first() ?: return false
        val itemMaster = shopDao.getItemById(itemId)

        // 1. アイテム消費
        playerDao.updateInventoryItem(item.copy(quantity = item.quantity - 1))

        // 2. ステータス計算
        val updatedPet = if (itemMaster != null) {
            calculateNewStatusWithMaster(pet, itemMaster)
        } else {
            calculateNewStatus(pet, itemId) // 互換性のため残す
        }
        
        // 3. データベース更新
        petDao.updatePetStatus(updatedPet)
        
        return true
    }

    /**
     * お世話アクションを実行する（アイテムを消費しない基本行動）
     * @return 成功したか、回数制限などの理由で失敗したか
     */
    suspend fun performCareAction(actionType: String): String {
        val pet = petDao.getPetStatus().first() ?: return "NOT_FOUND"
        
        // 日付チェックとリセット
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val isNewDay = pet.lastActionResetDate != today
        
        val basePet = if (isNewDay) {
            pet.copy(
                dailyStrokeCount = 0,
                dailyPlayCount = 0,
                isResting = false,
                lastActionResetDate = today
            )
        } else pet

        if (basePet.isResting && actionType != "REST") {
            return "IS_RESTING"
        }

        val updatedPet = when (actionType) {
            "STROKE" -> { // なでる
                if (basePet.dailyStrokeCount >= 3) return "LIMIT_STROKE"
                basePet.copy(
                    happiness = (basePet.happiness + 0.05f).coerceAtMost(1.0f),
                    friendship = (basePet.friendship + 0.005f).coerceAtMost(1.0f),
                    dailyStrokeCount = basePet.dailyStrokeCount + 1,
                    lastUpdateAt = System.currentTimeMillis()
                )
            }
            "REST" -> { // 休む
                basePet.copy(
                    fatigue = (basePet.fatigue - 0.5f).coerceAtLeast(0f), // 大幅回復
                    health = (basePet.health + 0.1f).coerceAtMost(1.0f),
                    happiness = (basePet.happiness + 0.05f).coerceAtMost(1.0f),
                    isResting = true,
                    lastUpdateAt = System.currentTimeMillis()
                )
            }
            "PLAY_GENERIC" -> { // 遊ぶ（基本）
                if (basePet.dailyPlayCount >= 3) return "LIMIT_PLAY"
                basePet.copy(
                    happiness = (basePet.happiness + 0.1f).coerceAtMost(1.0f),
                    fatigue = (basePet.fatigue + 0.05f).coerceAtMost(1.0f),
                    dailyPlayCount = basePet.dailyPlayCount + 1,
                    lastUpdateAt = System.currentTimeMillis()
                )
            }
            else -> return "INVALID_ACTION"
        }
        
        petDao.updatePetStatus(updatedPet)
        return "SUCCESS"
    }

    /**
     * 時間経過によるステータス変化（空腹度、清潔度、疲労、身長の伸びなど）を計算
     */
    suspend fun processTimePassage() {
        val pet = petDao.getPetStatus().first() ?: return
        val now = System.currentTimeMillis()
        val elapsedMillis = now - pet.lastUpdateAt
        
        if (elapsedMillis < MIN_UPDATE_INTERVAL_MS) return

        val hoursPassed = elapsedMillis / (1000 * 60 * 60).toDouble()
        
        // 日付が変わったかどうかの判定
        val lastUpdateDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(pet.lastUpdateAt))
        val nowDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(now))
        val isNewDay = lastUpdateDate != nowDate

        // 基礎ステータスの減少
        val newHunger = (pet.hunger - (HUNGER_DECAY_PER_HOUR * hoursPassed)).coerceAtLeast(0.0).toFloat()
        val newHappiness = (pet.happiness - (HAPPINESS_DECAY_PER_HOUR * hoursPassed)).coerceAtLeast(0.0).toFloat()
        val newCleanliness = (pet.cleanliness - (CLEANLINESS_DECAY_PER_HOUR * hoursPassed)).coerceAtLeast(0.0).toFloat()
        val newFatigue = (pet.fatigue + (FATIGUE_GAIN_PER_HOUR * hoursPassed)).coerceAtMost(1.0).toFloat()
        
        // 身長の伸び (成長期に応じて伸び幅を変える)
        val growthFactor = if (pet.level < 50) 0.005f else 0.001f
        val heightGrowth = (growthFactor * hoursPassed * pet.innateHeightTrend).toFloat()
        val newHeight = pet.height + heightGrowth
        
        // 新しい日になった場合のボーナス (親密度と知能)
        var friendship = pet.friendship
        var intelligence = pet.intelligence
        var dailyStrokeCount = pet.dailyStrokeCount
        var dailyPlayCount = pet.dailyPlayCount
        var isResting = pet.isResting
        
        if (isNewDay) {
            friendship = (friendship + 0.02f).coerceAtMost(1.0f)
            intelligence += 1
            dailyStrokeCount = 0
            dailyPlayCount = 0
            isResting = false
            
            growthDao?.insertRecord(PetGrowthRecordEntity(
                petId = pet.id,
                message = "${pet.name}の知能が${intelligence}になったよ！",
                type = "INTELLIGENCE"
            ))
        }

        // 体型の自然変化
        var bodyType = pet.bodyType
        if (newHunger < 0.2f) {
            bodyType = (bodyType - 0.5f * hoursPassed.toFloat()).coerceAtLeast(0.0f)
        }
        
        // 身長の大幅な伸び記録
        if (newHeight.toInt() > pet.height.toInt()) {
            growthDao?.insertRecord(PetGrowthRecordEntity(
                petId = pet.id,
                message = "${pet.name}の身長が${String.format(Locale.getDefault(), "%.1f", newHeight)}センチになったよ！",
                type = "HEIGHT"
            ))
        }
        
        petDao.updatePetStatus(pet.copy(
            hunger = newHunger,
            happiness = newHappiness,
            cleanliness = newCleanliness,
            fatigue = newFatigue,
            height = newHeight,
            friendship = friendship,
            intelligence = intelligence,
            bodyType = bodyType,
            dailyStrokeCount = dailyStrokeCount,
            dailyPlayCount = dailyPlayCount,
            isResting = isResting,
            lastActionResetDate = nowDate,
            lastUpdateAt = now
        ))
    }

    /**
     * 歩行距離や歩数による成長を適用
     */
    suspend fun applyActivityGrowth(deltaSteps: Int, deltaDistance: Double) {
        val pet = petDao.getPetStatus().first() ?: return
        
        // 1. 100mごとのステータス変動ロジック
        var hunger = pet.hunger
        var cleanliness = pet.cleanliness
        var happiness = pet.happiness
        var fatigue = pet.fatigue
        var friendship = pet.friendship
        var health = pet.health
        var height = pet.height
        var weight = pet.weight
        var walkCounter = pet.walkDistanceCounter + deltaDistance.toFloat()

        while (walkCounter >= 100f) {
            walkCounter -= 100f
            
            // A. 基礎変動の準備
            var deltaHunger = -0.01f
            var deltaCleanliness = -0.01f
            var deltaHappiness = 0.01f
            var deltaFatigue = 0.01f
            var deltaFriendship = if (happiness >= 0.5f) 0.01f else -0.01f

            // B. 健康状態によるペナルティ適用
            val healthStatus = pet.getHealthStatus()
            when (healthStatus) {
                "病気", "弱っている" -> {
                    deltaFatigue = 0.02f * 1.3f // 元気がない状態(2%)の1.3倍 = 2.6%
                    if (deltaFriendship > 0) {
                        deltaFriendship = -0.013f // 強制減少かつ1.3倍
                    } else {
                        deltaFriendship *= 1.3f
                    }
                }
                "元気がない" -> {
                    deltaFatigue += 0.01f // +1%追加
                    deltaFriendship -= 0.01f // 変動から1%差し引き
                }
            }
            
            // C. ステータス更新
            hunger = (hunger + deltaHunger).coerceAtLeast(0f)
            cleanliness = (cleanliness + deltaCleanliness).coerceAtLeast(0f)
            happiness = (happiness + deltaHappiness).coerceAtMost(1f)
            fatigue = (fatigue + deltaFatigue).coerceAtMost(1f)
            friendship = (friendship + deltaFriendship).coerceIn(0f, 1.0f)
            
            // 健康の変動 (3項目連動)
            if (hunger >= 0.5f && happiness >= 0.5f && cleanliness >= 0.5f) {
                health = (health + 0.01f).coerceAtMost(1f)
            } else if (hunger <= 0.5f && happiness <= 0.5f && cleanliness <= 0.5f) {
                health = (health - 0.01f).coerceAtLeast(0f)
            }
            
            // 健康状態が良い場合(70%以上)の成長
            if (health >= 0.7f) {
                height += 0.001f
                weight += 0.001f
            }
            
            // 個別ペナルティ (30%以下)
            if (hunger <= 0.3f) {
                health = (health - 0.01f).coerceAtLeast(0f)
                friendship = (friendship - 0.01f).coerceAtLeast(0f)
            }
            if (cleanliness <= 0.3f) {
                health = (health - 0.01f).coerceAtLeast(0f)
                friendship = (friendship - 0.01f).coerceAtLeast(0f)
            }
            
            // 疲労度によるペナルティ (50%以下で減少 ※指定通り)
            if (fatigue <= 0.5f) {
                happiness = (happiness - 0.01f).coerceAtLeast(0f)
                friendship = (friendship - 0.01f).coerceAtLeast(0f)
            }

            // お土産拾い判定 (健康が30%より高い時のみ判定)
            if (health > 0.3f) {
                val baseChance = 0.01f
                val mentalBonus = (happiness * 0.04f) + (friendship * 0.03f) + (pet.affection / 100f * 0.02f)
                if (java.util.Random().nextFloat() < (baseChance + mentalBonus)) {
                    checkItemDiscovery(pet)
                }
            }
        }

        // 2. 既存の距離・歩数による成長
        val distanceKm = deltaDistance / 1000.0
        val heightGrowth = (0.02f * distanceKm * pet.innateHeightTrend).toFloat()
        val bodyTypeChange = (-0.5f * distanceKm).toFloat()
        val weightChange = (-0.01f * distanceKm).toFloat()
        
        // 経験値: 1kmごとに 50 EXP, 1000歩ごとに 20 EXP
        val activityExp = (distanceKm * 50).toLong() + (deltaSteps / 1000 * 20).toLong()
        
        var newExp = pet.experience + activityExp
        var newLevel = pet.level
        var newStamina = pet.stamina
        var newIntelligence = pet.intelligence
        
        // 新レベルアップ計算式: 1000 + (level-1)^2 * 100
        fun getRequiredExp(l: Int): Long = 1000L + (l - 1) * (l - 1) * 100L

        while (newLevel < 50 && newExp >= getRequiredExp(newLevel)) {
            newExp -= getRequiredExp(newLevel)
            newLevel += 1
            newStamina += 2
            newIntelligence += 1
            growthDao?.insertRecord(PetGrowthRecordEntity(petId = pet.id, message = "${pet.name}がレベル${newLevel}に成長したよ！", type = "LEVEL"))
        }
        
        petDao.updatePetStatus(pet.copy(
            hunger = hunger,
            cleanliness = cleanliness,
            happiness = happiness,
            fatigue = fatigue,
            friendship = friendship,
            health = health,
            height = height + heightGrowth,
            weight = (weight + weightChange).coerceAtLeast(0.5f),
            walkDistanceCounter = walkCounter,
            bodyType = (pet.bodyType + bodyTypeChange).coerceIn(0f, 100f),
            stamina = newStamina,
            intelligence = newIntelligence,
            experience = newExp,
            level = newLevel,
            lastUpdateAt = System.currentTimeMillis()
        ))
    }

    private suspend fun checkItemDiscovery(pet: PetEntity) {
        val profile = playerDao.getPlayerProfile().first() ?: return
        val lat = profile.startLatitude ?: return // 直近の測位地点を使用
        val lng = profile.startLongitude ?: return

        // エリア判定 (簡易)
        val itemId = when {
            // 山部エリア (Lat 43.20 - 43.28 付近)
            lat in 43.20..43.28 && lng in 142.35..142.40 -> "souvenir_melon"
            // 富良野中心部エリア (Lat 43.30 - 43.40 付近)
            lat in 43.30..43.40 && lng in 142.36..142.42 -> "souvenir_heso_manju"
            else -> null
        }

        if (itemId != null) {
            val currentInv = playerDao.getInventory().first()
            val invItem = currentInv.find { it.itemId == itemId }
            val newQty = (invItem?.quantity ?: 0) + 1
            
            if (newQty <= 30) {
                playerDao.updateInventoryItem(InventoryEntity(itemId, newQty))
                val itemMaster = shopDao.getItemById(itemId)
                growthDao?.insertRecord(PetGrowthRecordEntity(
                    petId = pet.id,
                    message = "${pet.name}が「${itemMaster?.name ?: itemId}」を拾ってきたよ！",
                    type = "ITEM_PICKUP"
                ))
            }
        }
    }

    private suspend fun calculateNewStatusWithMaster(
        pet: PetEntity, 
        item: com.momo.furawalk.data.local.room.entity.ShopItemEntity
    ): PetEntity {
        var hunger = pet.hunger
        var happiness = pet.happiness
        var health = pet.health
        var exp = pet.experience
        var level = pet.level
        var bodyType = pet.bodyType
        var weight = pet.weight
        var cleanliness = pet.cleanliness
        var fatigue = pet.fatigue
        var height = pet.height
        var stamina = pet.stamina
        var intelligence = pet.intelligence
        var friendship = pet.friendship
        var affection = pet.affection

        val effects = try {
            if (item.effectsJson.isNotEmpty()) {
                json.decodeFromString<List<com.momo.furawalk.data.remote.model.ItemEffectDto>>(item.effectsJson)
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        for (effect in effects) {
            when (effect.type) {
                "HUNGER" -> hunger = (hunger + effect.value).coerceIn(0f, 1.0f)
                "HAPPINESS" -> happiness = (happiness + effect.value).coerceIn(0f, 1.0f)
                "HEALTH" -> health = (health + effect.value).coerceIn(0f, 1.0f)
                "FATIGUE" -> fatigue = (fatigue + effect.value).coerceIn(0f, 1.0f)
                "CLEANLINESS" -> cleanliness = (cleanliness + effect.value).coerceIn(0f, 1.0f)
                "FRIENDSHIP" -> friendship = (friendship + effect.value).coerceIn(0f, 1.0f)
                "AFFECTION" -> affection = (affection + effect.value.toInt()).coerceIn(0, 100)
                "HEIGHT" -> height += effect.value
                "WEIGHT" -> weight = (weight + effect.value).coerceAtLeast(0.1f)
                "BODY_TYPE" -> bodyType = (bodyType + effect.value).coerceIn(0f, 100f)
                "INTELLIGENCE" -> intelligence += effect.value.toInt()
                "STAMINA" -> stamina += effect.value.toInt()
                "EXP" -> exp += effect.value.toLong()
                "MYSTERY" -> {
                    val roll = java.util.Random().nextInt(4)
                    when (roll) {
                        0 -> hunger = 1.0f
                        1 -> health = 0.1f
                        2 -> friendship = (friendship + 0.2f).coerceAtMost(1.0f)
                        3 -> { bodyType = (bodyType - 10f).coerceAtLeast(0f); weight -= 1.0f }
                    }
                }
                "OMNIPOTENT" -> {
                    hunger = (hunger + 0.1f).coerceAtMost(1.0f)
                    happiness = (happiness + 0.1f).coerceAtMost(1.0f)
                    friendship = (friendship + 0.05f).coerceAtMost(1.0f)
                }
            }
        }

        val species = petDao.getSpeciesById(pet.speciesId)
        if (item.favoriteSpecies != null && item.favoriteSpecies == species?.species) {
            val bonusEffects = try {
                if (item.favoriteBonusEffectsJson.isNotEmpty()) {
                    json.decodeFromString<List<com.momo.furawalk.data.remote.model.ItemEffectDto>>(item.favoriteBonusEffectsJson)
                } else emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            for (effect in bonusEffects) {
                when (effect.type) {
                    "HUNGER" -> hunger = (hunger + effect.value).coerceIn(0f, 1.0f)
                    "HAPPINESS" -> happiness = (happiness + effect.value).coerceIn(0f, 1.0f)
                    "HEALTH" -> health = (health + effect.value).coerceIn(0f, 1.0f)
                    "FRIENDSHIP" -> friendship = (friendship + effect.value).coerceIn(0f, 1.0f)
                    "EXP" -> exp += effect.value.toLong()
                }
            }
        }

        if (item.category == "TREAT") {
            happiness = (happiness + 0.3f).coerceAtMost(1.0f)
            exp += 20
        }

        while (exp >= level * 100) {
            exp -= (level * 100).toLong()
            level += 1
            stamina += 2
            intelligence += 1
            growthDao?.insertRecord(PetGrowthRecordEntity(petId = pet.id, message = "${pet.name}がレベル${level}になったよ！おめでとう！", type = "LEVEL"))
        }

        return pet.copy(
            health = health,
            hunger = hunger,
            happiness = happiness,
            experience = exp,
            level = level,
            bodyType = bodyType,
            weight = weight,
            height = height,
            stamina = stamina,
            intelligence = intelligence,
            cleanliness = cleanliness,
            fatigue = fatigue,
            friendship = friendship,
            affection = affection,
            lastUpdateAt = System.currentTimeMillis()
        )
    }

    private suspend fun calculateNewStatus(pet: PetEntity, itemId: String): PetEntity {
        return pet
    }

    companion object {
        private const val MIN_UPDATE_INTERVAL_MS = 60 * 1000L
        private const val HUNGER_DECAY_PER_HOUR = 0.05
        private const val HAPPINESS_DECAY_PER_HOUR = 0.03
        private const val CLEANLINESS_DECAY_PER_HOUR = 0.02
        private const val FATIGUE_GAIN_PER_HOUR = 0.04
    }
}
