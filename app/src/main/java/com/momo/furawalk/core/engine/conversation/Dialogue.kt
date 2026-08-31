package com.momo.furawalk.core.engine.conversation

/**
 * ペットが喋る内容の最終的な出力データ
 */
data class Dialogue(
    val text: String,
    val emotion: Emotion = Emotion.NORMAL,
    val displayTimeMillis: Long = 5000L
)

enum class Emotion {
    NORMAL, HAPPY, SAD, ANGRY, SURPRISED, SLEEPY
}
