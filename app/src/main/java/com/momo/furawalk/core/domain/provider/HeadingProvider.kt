package com.momo.furawalk.core.domain.provider

import kotlinx.coroutines.flow.StateFlow

/**
 * 端末のコンパス（磁気センサー）方位を抽象化
 */
interface HeadingProvider {
    /**
     * 北を0度とした時計回りの角度 (0..359)
     */
    val heading: StateFlow<Float>
    fun startListening()
    fun stopListening()
}
