package com.momo.furawalk.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class PetSpeciesDto(
    val id: String,
    val name: String,
    val species: String,
    val rarity: Int,
    val imageUrl: String,
    val baseStatus: PetBaseStatusDto,
    val speech: PetSpeechDto? = null
)

@Serializable
data class PetBaseStatusDto(
    val hp: Int,
    val stamina: Int,
    val speed: Int,
    val power: Int,
    val intelligence: Int
)

@Serializable
data class PetSpeechDto(
    val firstPerson: String,
    val ending: String,
    val style: String
)

@Serializable
data class PetDataResponse(
    val pets: List<PetSpeciesDto>
)
