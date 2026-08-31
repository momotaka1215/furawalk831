package com.momo.furawalk.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pet_species")
data class PetSpeciesEntity(
    @PrimaryKey val id: String,
    val name: String,
    val species: String, // dog, cat, monkey etc.
    val rarity: Int = 1,
    val description: String = "",
    val type1Description: String = "",
    val type2Description: String = "",
    val iconEmoji: String = "🐾",
    val imageUrl: String? = null,
    val localImagePath: String? = null,
    val isDownloaded: Boolean = false,
    
    // 基礎ステータス
    val baseHp: Int = 100,
    val baseStamina: Int = 100,
    val baseSpeed: Int = 100,
    val basePower: Int = 100,
    val baseIntelligence: Int = 100,
    
    // 性格・セリフ設定 (JSONのまま保存するか、簡易化)
    val firstPerson: String = "ぼく",
    val ending: String = "！",
    val style: String = "friendly"
)
