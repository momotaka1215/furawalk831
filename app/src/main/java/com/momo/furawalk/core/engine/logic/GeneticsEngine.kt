package com.momo.furawalk.core.engine.logic

import com.momo.furawalk.data.local.room.entity.PetEntity
import com.momo.furawalk.data.local.room.entity.PetSpeciesEntity
import com.momo.furawalk.data.local.room.entity.PlayerEntity
import java.security.MessageDigest
import java.util.Random

/**
 * ペットの遺伝、個体生成、突然変異を司るエンジン
 */
object GeneticsEngine {

    // 遺伝の重み設定 (合計で約1.0になるように調整)
    private const val WEIGHT_INHERITANCE = 0.7f // 親からの引き継ぎ
    private const val WEIGHT_USER_INITIAL = 0.2f // ユーザー情報の基礎傾向
    private const val WEIGHT_MUTATION = 0.1f    // 突然変異・乱数要素

    /**
     * ユーザー情報とペット種別から再現可能なLongシードを生成
     */
    fun generateUserPetSeed(player: PlayerEntity, speciesId: String): Long {
        val input = "${player.name}:${player.birthDate}:${player.gender}:$speciesId"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        // 最初の方の8バイトをLongとして使用
        var seed = 0L
        for (i in 0..7) {
            seed = (seed shl 8) or (bytes[i].toLong() and 0xFF)
        }
        return seed
    }

    /**
     * 第1世代のペットを生成
     */
    fun generateInitialPet(
        player: PlayerEntity,
        species: PetSpeciesEntity,
        name: String = species.name
    ): PetEntity {
        val seed = generateUserPetSeed(player, species.id)
        val random = Random(seed)

        // 種族ごとの基準値
        val baseHeight = when (species.species.lowercase()) {
            "cat" -> 20f
            "dog" -> 30f
            "monkey" -> 25f
            "rabbit" -> 15f
            else -> 25f
        }
        val baseWeight = when (species.species.lowercase()) {
            "cat" -> 1.0f
            "dog" -> 3.0f
            "monkey" -> 2.0f
            "rabbit" -> 0.5f
            else -> 1.5f
        }

        // 傾向の生成 (0.8 - 1.2 程度に分散)
        val hTrend = 0.8f + random.nextFloat() * 0.4f
        val bTrend = 0.8f + random.nextFloat() * 0.4f
        val iTrend = 0.8f + random.nextFloat() * 0.4f
        val sTrend = 0.8f + random.nextFloat() * 0.4f

        // 性格要素の生成 (20 - 80)
        val act = 20 + random.nextInt(61)
        val aff = 20 + random.nextInt(61)
        val brv = 20 + random.nextInt(61)
        val gnt = 20 + random.nextInt(61)

        // レアリティ判定
        val rarity = rollRarity(random)

        return PetEntity(
            name = name,
            speciesId = species.id,
            height = baseHeight * hTrend,
            weight = baseWeight * bTrend,
            bodyType = 50f,
            intelligence = (species.baseIntelligence * iTrend).toInt(),
            stamina = (species.baseStamina * sTrend).toInt(),
            activity = act,
            affection = aff,
            bravery = brv,
            gentleness = gnt,
            innateHeightTrend = hTrend,
            innateBodyTrend = bTrend,
            innateIntelligenceTrend = iTrend,
            innateStaminaTrend = sTrend,
            dnaSeed = seed,
            generation = 1,
            rarityTier = rarity,
            isMutant = false,
            lastUpdateAt = System.currentTimeMillis()
        )
    }

    /**
     * 次世代（進化/転生）のペットを生成
     */
    fun generateChildPet(
        parent: PetEntity,
        player: PlayerEntity,
        species: PetSpeciesEntity
    ): PetEntity {
        // 親のDNAとユーザーシードを混ぜて新しいシードを作成
        val userSeed = generateUserPetSeed(player, species.id)
        val childSeed = parent.dnaSeed xor userSeed xor System.nanoTime()
        val random = Random(childSeed)

        // 突然変異の判定
        val isMutant = random.nextFloat() < 0.05f // 5%

        // パラメーターの継承計算
        fun inherit(parentVal: Float, initialTrend: Float): Float {
            val mutation = if (isMutant) (random.nextFloat() * 0.6f - 0.3f) else (random.nextFloat() * 0.2f - 0.1f)
            return (parentVal * WEIGHT_INHERITANCE) + 
                   (initialTrend * WEIGHT_USER_INITIAL) + 
                   (mutation * WEIGHT_MUTATION)
        }

        val hTrend = inherit(parent.innateHeightTrend, 1.0f).coerceIn(0.5f, 2.0f)
        val bTrend = inherit(parent.innateBodyTrend, 1.0f).coerceIn(0.5f, 2.0f)
        val iTrend = inherit(parent.innateIntelligenceTrend, 1.0f).coerceIn(0.5f, 2.0f)
        val sTrend = inherit(parent.innateStaminaTrend, 1.0f).coerceIn(0.5f, 2.0f)

        // 種族ごとの基準値
        val baseHeight = when (species.species.lowercase()) {
            "cat" -> 20f
            "dog" -> 30f
            "monkey" -> 25f
            "rabbit" -> 15f
            else -> 25f
        }
        val baseWeight = when (species.species.lowercase()) {
            "cat" -> 1.0f
            "dog" -> 3.0f
            "monkey" -> 2.0f
            "rabbit" -> 0.5f
            else -> 1.5f
        }

        // 先祖返り判定 (低確率で親の傾向を無視して初期化に近い値を出すなど)
        val isAtavism = random.nextFloat() < 0.03f // 3%
        // TODO: lineageDataから過去の傾向を引き出すロジック

        // レアリティ判定
        val rarity = rollRarity(random)

        return PetEntity(
            name = "${parent.name}の子",
            speciesId = species.id,
            height = baseHeight * hTrend,
            weight = baseWeight * bTrend,
            bodyType = (parent.bodyType * 0.3f + 50f * 0.7f).toFloat(), // 体型は一部引き継ぐがリセット寄り
            intelligence = (species.baseIntelligence * iTrend).toInt(),
            stamina = (species.baseStamina * sTrend).toInt(),
            activity = (parent.activity * 0.5f + (20 + random.nextInt(61)) * 0.5f).toInt(),
            affection = (parent.affection * 0.5f + (20 + random.nextInt(61)) * 0.5f).toInt(),
            innateHeightTrend = hTrend,
            innateBodyTrend = bTrend,
            innateIntelligenceTrend = iTrend,
            innateStaminaTrend = sTrend,
            dnaSeed = childSeed,
            generation = parent.generation + 1,
            parentId = parent.id,
            rarityTier = rarity,
            isMutant = isMutant,
            lastUpdateAt = System.currentTimeMillis()
        )
    }

    private fun rollRarity(random: Random): Int {
        val roll = random.nextFloat()
        return when {
            roll < 0.001f -> 4 // 伝説
            roll < 0.01f  -> 3 // 超レア
            roll < 0.05f  -> 2 // レア
            roll < 0.15f  -> 1 // 珍しい
            else          -> 0 // 通常
        }
    }
}
