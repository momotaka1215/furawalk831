package com.momo.furawalk.core.engine.conversation

import com.momo.furawalk.data.local.room.entity.PetEntity
import com.momo.furawalk.data.local.room.entity.PlayerEntity
import kotlin.random.Random

/**
 * ペットの思考をシミュレートし、最適なセリフを生成するエンジン
 */
object DialogueEngine {

    /**
     * 現在の状態から最適なセリフを一つ生成する
     */
    fun generate(pet: PetEntity, player: PlayerEntity): Dialogue {
        val allTemplates = DialogueRepository.getAllTemplates()
        
        // 1. 条件に合うものを抽出
        val validTemplates = allTemplates.filter { it.condition(pet, player) }
        
        if (validTemplates.isEmpty()) {
            return Dialogue("......", Emotion.NORMAL)
        }

        // 2. 優先度に基づく重み付け選択
        val selectedTemplate = selectByPriority(validTemplates)
        
        // 3. プレースホルダーの置換
        val finalMessage = replacePlaceholders(selectedTemplate.textTemplate, pet, player)
        
        return Dialogue(
            text = finalMessage,
            emotion = selectedTemplate.emotion
        )
    }

    private fun selectByPriority(templates: List<DialogueTemplate>): DialogueTemplate {
        // 全体の優先度の合計を算出 (最低でも1を保証)
        val totalPriority = templates.sumOf { it.priority.coerceAtLeast(0) + 10 }
        var randomValue = Random.nextInt(totalPriority)
        
        for (template in templates) {
            val weight = template.priority.coerceAtLeast(0) + 10
            randomValue -= weight
            if (randomValue < 0) return template
        }
        
        return templates.random()
    }

    private fun replacePlaceholders(template: String, pet: PetEntity, player: PlayerEntity): String {
        var result = template
            .replace("{userName}", player.name)
            .replace("{petName}", pet.name)
            .replace("{favColor}", getJapaneseColorName(pet.favoriteColor))
            .replace("{favFood}", getJapaneseFoodName(pet.favoriteFood))
            .replace("{cry}", getSpeciesCry(pet.speciesId))
        
        return result
    }

    private fun getJapaneseColorName(color: String?): String {
        return when (color?.uppercase()) {
            "RED" -> "赤"
            "BLUE" -> "青"
            "GREEN" -> "緑"
            "YELLOW" -> "黄"
            "PURPLE" -> "紫"
            else -> "きれいな"
        }
    }

    private fun getJapaneseFoodName(food: String?): String {
        return when (food?.uppercase()) {
            "MEAT" -> "お肉"
            "FISH" -> "お魚"
            "VEGETABLE" -> "お野菜"
            "SNACK" -> "お菓子"
            "FRUIT" -> "果物"
            else -> "ごはん"
        }
    }

    private fun getSpeciesCry(speciesId: String): String {
        return when (speciesId.lowercase()) {
            "dog", "shiba_01" -> "ワン"
            "cat", "calico_01" -> "ニャー"
            "monkey" -> "ウホッ"
            else -> "クゥ"
        }
    }
}
