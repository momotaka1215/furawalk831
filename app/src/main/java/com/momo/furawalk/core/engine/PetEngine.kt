package com.momo.furawalk.core.engine

import com.momo.furawalk.core.engine.logic.PetNurturingManager
import com.momo.furawalk.core.engine.logic.PetEvolutionManager
import com.momo.furawalk.core.engine.logic.PetInteractionManager

/**
 * ペット育成システム全体を統括する最上位エンジン
 */
class PetEngine(
    val nurturing: PetNurturingManager,
    val evolution: PetEvolutionManager,
    val interaction: PetInteractionManager
) {
    /**
     * アプリ起動時や定期的な更新が必要な処理をまとめる
     */
    suspend fun update() {
        nurturing.processTimePassage()
    }
}
