package com.momo.furawalk.core.engine.conversation

import com.momo.furawalk.data.local.room.entity.PetEntity
import com.momo.furawalk.data.local.room.entity.PlayerEntity

/**
 * 会話のテンプレート定義
 */
data class DialogueTemplate(
    val id: String,
    val category: DialogueCategory,
    val textTemplate: String,
    val emotion: Emotion = Emotion.NORMAL,
    val priority: Int = 0,
    val condition: (PetEntity, PlayerEntity) -> Boolean = { _, _ -> true }
)

enum class DialogueCategory {
    GREETING,    // 挨拶
    HUNGRY,      // 空腹
    TIRED,       // 疲労
    FAVORITE,    // 好み（色、食べ物）
    PERSONALITY, // 性格由来
    STATUS,      // 状態（健康など）
    IDLE         // 暇な時
}
