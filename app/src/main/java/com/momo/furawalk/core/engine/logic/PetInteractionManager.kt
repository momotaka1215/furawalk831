package com.momo.furawalk.core.engine.logic

import com.momo.furawalk.data.local.room.dao.PetDao
import com.momo.furawalk.data.local.room.dao.GrowthDao
import com.momo.furawalk.data.local.room.entity.PetGrowthRecordEntity
import kotlinx.coroutines.flow.first

/**
 * なでる、たたく、驚かす等の直接的なインタラクションを管理するマネージャー
 */
class PetInteractionManager(
    private val petDao: PetDao,
    private val growthDao: GrowthDao? = null
) {
    /**
     * ペットをなでる
     * @param intensity なでる強さ/頻度
     */
    suspend fun petAnimal(intensity: Float) {
        val pet = petDao.getPetStatus().first() ?: return
        
        // なでることで幸福度と親密度がわずかに上昇
        val updatedHappiness = (pet.happiness + (0.01f * intensity)).coerceAtMost(1.0f)
        val updatedFriendship = (pet.friendship + (0.005f * intensity)).coerceAtMost(1.0f)
        
        // なつき度の上昇記録
        if ((updatedFriendship * 10).toInt() > (pet.friendship * 10).toInt()) {
            growthDao?.insertRecord(PetGrowthRecordEntity(
                petId = pet.id,
                message = "${pet.name}がもっと懐いてくれるようになったよ！なつき度${(updatedFriendship * 100).toInt()}%！",
                type = "FRIENDSHIP"
            ))
        }

        petDao.updatePetStatus(pet.copy(
            happiness = updatedHappiness,
            friendship = updatedFriendship
        ))
    }
}
