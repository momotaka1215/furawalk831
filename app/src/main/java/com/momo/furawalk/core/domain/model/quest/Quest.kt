package com.momo.furawalk.core.domain.model.quest

/**
 * ゲーム内のクエスト情報を表すドメインモデル
 */
data class Quest(
    val id: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean = false
)
