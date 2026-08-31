package com.momo.furawalk.core.engine.logic

import com.momo.furawalk.data.local.room.dao.PetDao
import com.momo.furawalk.data.local.room.dao.PlayerDao
import com.momo.furawalk.data.local.room.entity.PetEntity
import kotlinx.coroutines.flow.first

/**
 * ペットの進化（転生・世代交代）を管理するマネージャー
 */
class PetEvolutionManager(
    private val petDao: PetDao,
    private val playerDao: PlayerDao
) {
    /**
     * 次の世代へ進める（転生）準備ができているか判定
     */
    fun checkEvolutionAvailability(pet: PetEntity): Boolean {
        // 例: レベル30以上で転生可能
        return pet.level >= 30
    }

    /**
     * 次の世代へ生まれ変わる（転生）を実行
     */
    suspend fun reincarnatePet(pet: PetEntity): Boolean {
        val player = playerDao.getPlayerProfile().first() ?: return false
        val species = petDao.getAllSpecies().first().find { it.id == pet.speciesId } ?: return false

        // 新しい個体を生成
        val childPet = GeneticsEngine.generateChildPet(pet, player, species)
        
        // データベースを更新 (現在の個体を上書き)
        petDao.updatePetStatus(childPet)
        
        return true
    }

    /**
     * 進化（姿の変化）を実行する (旧来の互換性用)
     */
    suspend fun evolveForm(pet: PetEntity, targetFormId: String) {
        val evolvedPet = pet.copy(
            formId = targetFormId,
            experience = 0,
            health = 1.0f
        )
        petDao.updatePetStatus(evolvedPet)
    }
}
