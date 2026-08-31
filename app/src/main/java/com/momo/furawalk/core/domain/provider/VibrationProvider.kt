package com.momo.furawalk.core.domain.provider

/**
 * デバイスのバイブレーション機能を抽象化
 */
interface VibrationProvider {
    /**
     * 短い振動（通知用）
     */
    fun vibrateOnce()
    
    /**
     * 連続した振動（チェックポイント到着などの強調用）
     */
    fun vibrateSuccess()
    
    /**
     * キャンセル
     */
    fun cancel()
}
