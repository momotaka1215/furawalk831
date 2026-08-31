package com.momo.furawalk.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class AvatarDto(
    val id: String,
    val name: String,
    val url: String
)
