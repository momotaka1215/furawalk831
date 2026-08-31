package com.momo.furawalk.data.local.room.dao

import androidx.room.*
import com.momo.furawalk.data.local.room.entity.PetEntity
import com.momo.furawalk.data.local.room.entity.PetSpeciesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {
    @Query("SELECT * FROM pet_status WHERE id = :id")
    fun getPetStatus(id: String = "current_pet"): Flow<PetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updatePetStatus(pet: PetEntity)

    @Query("SELECT * FROM pet_species")
    fun getAllSpecies(): Flow<List<PetSpeciesEntity>>

    @Query("SELECT * FROM pet_species WHERE id = :id")
    suspend fun getSpeciesById(id: String): PetSpeciesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSpecies(species: PetSpeciesEntity)

    @Transaction
    suspend fun selectPet(speciesId: String, petName: String) {
        val currentPet = PetEntity(
            name = petName,
            speciesId = speciesId,
            lastUpdateAt = System.currentTimeMillis()
        )
        updatePetStatus(currentPet)
    }
}
