package com.momo.furawalk.core.engine.logic

import com.momo.furawalk.data.local.room.entity.PetSpeciesEntity
import com.momo.furawalk.data.local.room.entity.PlayerEntity
import org.junit.Assert.*
import org.junit.Test

class GeneticsEngineTest {

    private val mockPlayer = PlayerEntity(
        name = "TestUser",
        birthDate = 1234567890L,
        gender = "male"
    )

    private val mockSpecies = PetSpeciesEntity(
        id = "dog_01",
        name = "柴犬",
        species = "dog",
        baseIntelligence = 100,
        baseStamina = 100
    )

    @Test
    fun testSeedGenerationIsDeterministic() {
        val seed1 = GeneticsEngine.generateUserPetSeed(mockPlayer, "dog_01")
        val seed2 = GeneticsEngine.generateUserPetSeed(mockPlayer, "dog_01")
        assertEquals(seed1, seed2)

        val seed3 = GeneticsEngine.generateUserPetSeed(mockPlayer.copy(name = "Other"), "dog_01")
        assertNotEquals(seed1, seed3)
    }

    @Test
    fun testInitialPetGeneration() {
        val pet1 = GeneticsEngine.generateInitialPet(mockPlayer, mockSpecies)
        val pet2 = GeneticsEngine.generateInitialPet(mockPlayer, mockSpecies)

        assertEquals(pet1.dnaSeed, pet2.dnaSeed)
        assertEquals(pet1.height, pet2.height, 0.01f)
        assertEquals(pet1.intelligence, pet2.intelligence)
        assertEquals(pet1.generation, 1)
    }

    @Test
    fun testInheritance() {
        val parent = GeneticsEngine.generateInitialPet(mockPlayer, mockSpecies)
        val child = GeneticsEngine.generateChildPet(parent, mockPlayer, mockSpecies)

        assertEquals(child.generation, 2)
        assertEquals(child.parentId, parent.id)
        // 子のシードは親とは異なる（ランダム要素が入るため）
        assertNotEquals(parent.dnaSeed, child.dnaSeed)
    }
}
