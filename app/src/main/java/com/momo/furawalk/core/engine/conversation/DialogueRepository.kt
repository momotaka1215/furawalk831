package com.momo.furawalk.core.engine.conversation

/**
 * 会話テンプレートを集中管理するリポジトリ
 */
object DialogueRepository {

    private val templates = mutableListOf<DialogueTemplate>()

    init {
        // --- 挨拶系 ---
        addTemplate(DialogueTemplate("greet_01", DialogueCategory.GREETING, "{userName}さん、こんにちは！", Emotion.HAPPY))
        addTemplate(DialogueTemplate("greet_02", DialogueCategory.GREETING, "今日も一緒にお散歩できて嬉しいな。", Emotion.HAPPY))
        addTemplate(DialogueTemplate("greet_03", DialogueCategory.GREETING, "なにかお話しする？", Emotion.NORMAL))

        // --- 空腹系 ---
        addTemplate(DialogueTemplate("hungry_01", DialogueCategory.HUNGRY, "なんだかお腹が空いてきちゃった...", Emotion.SAD, 100) { pet, _ ->
            pet.hunger < 0.3f
        })
        addTemplate(DialogueTemplate("hungry_02", DialogueCategory.HUNGRY, "{favFood}が食べたい気分だなぁ。", Emotion.HAPPY, 120) { pet, _ ->
            pet.hunger < 0.5f
        })

        // --- 疲労系 ---
        addTemplate(DialogueTemplate("tired_01", DialogueCategory.TIRED, "ふぅ、ちょっと疲れちゃったかも。", Emotion.SLEEPY, 100) { pet, _ ->
            pet.fatigue > 0.7f
        })
        addTemplate(DialogueTemplate("tired_02", DialogueCategory.TIRED, "少しお休みしてもいいかな？", Emotion.SLEEPY, 110) { pet, _ ->
            pet.fatigue > 0.8f
        })

        // --- 好み・性格系 ---
        addTemplate(DialogueTemplate("fav_color_01", DialogueCategory.FAVORITE, "{favColor}色を見ると、なんだか落ち着くんだ。", Emotion.HAPPY))
        addTemplate(DialogueTemplate("trait_brave", DialogueCategory.PERSONALITY, "ぼく、もっと強くなって{userName}さんを守るよ！", Emotion.HAPPY, 50) { pet, _ ->
            pet.bravery > 70
        })
        addTemplate(DialogueTemplate("trait_gentle", DialogueCategory.PERSONALITY, "のんびり歩くのも楽しいねぇ。", Emotion.NORMAL, 50) { pet, _ ->
            pet.gentleness > 70
        })

        // --- 種別固有 (プレースホルダーで鳴き声を挿入する想定) ---
        addTemplate(DialogueTemplate("species_dog", DialogueCategory.IDLE, "{cry}！遊んで遊んで！", Emotion.HAPPY) { pet, _ ->
            pet.speciesId == "dog"
        })
        addTemplate(DialogueTemplate("species_cat", DialogueCategory.IDLE, "{cry}...日向ぼっこしたいニャ。", Emotion.SLEEPY) { pet, _ ->
            pet.speciesId == "cat"
        })
        addTemplate(DialogueTemplate("species_monkey", DialogueCategory.IDLE, "{cry}！{favFood}はどこだー！", Emotion.HAPPY) { pet, _ ->
            pet.speciesId == "monkey"
        })
    }

    private fun addTemplate(template: DialogueTemplate) {
        templates.add(template)
    }

    fun getAllTemplates(): List<DialogueTemplate> = templates
}
