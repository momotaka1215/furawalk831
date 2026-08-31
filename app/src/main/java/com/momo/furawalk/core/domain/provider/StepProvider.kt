package com.momo.furawalk.core.domain.provider

import kotlinx.coroutines.flow.StateFlow

/**
 * 端末の歩数センサーを抽象化
 */
interface StepProvider {
    /**
     * アプリ起動時、またはトラッキング開始時からの累計歩数
     */
    val currentSteps: StateFlow<Int>
    
    fun startListening()
    fun stopListening()
}
