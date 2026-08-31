package com.momo.furawalk.core.engine.logic

import com.momo.furawalk.data.local.room.entity.PetEntity

object PetInitialStatFactory {
    
    fun createPet(
        name: String,
        speciesId: String,
        weight: Float,
        height: Float,
        imagePath: String?,
        color: String,
        food: String
    ): PetEntity {
        // 可変の作業用変数
        var finalWeight = weight
        val finalHeight = height
        
        // 種別ごとのベースステータス設定
        var intelligence = 10
        var stamina = 10
        var activity = 50
        var affection = 50
        var bravery = 50
        var gentleness = 50
        var bodyType = 50f

        when (speciesId.uppercase()) {
            "DOG" -> {
                stamina = 15
                bravery = 60
                activity = 65
                affection = 60
            }
            "CAT" -> {
                intelligence = 15
                gentleness = 60
                activity = 40
                affection = 40
            }
            "MONKEY" -> {
                intelligence = 20
                activity = 70
                bravery = 50
                affection = 55
            }
        }
        
        // 好み（色と食べ物）による補正
        // 25種類のマトリックスロジック
        when (color) {
            "RED" -> { bravery += 20; activity += 10 }
            "BLUE" -> { intelligence += 15; gentleness += 10; activity -= 10 }
            "GREEN" -> { stamina += 15; gentleness += 10 }
            "YELLOW" -> { activity += 20; affection += 10 }
            "PURPLE" -> { affection += 20; intelligence += 5 }
        }
        
        when (food) {
            "MEAT" -> { stamina += 10; bravery += 10; finalWeight += 0.5f }
            "FISH" -> { intelligence += 10; activity += 5 }
            "VEGETABLE" -> { stamina += 5; gentleness += 10; bodyType -= 5f }
            "SNACK" -> { affection += 15; bodyType += 15f; activity -= 5 }
            "FRUIT" -> { activity += 15; intelligence += 5 }
        }
        
        // さらに組み合わせによるボーナスやユニークな調整（これで25通りの個性が際立つ）
        when ("$color-$food") {
            "RED-MEAT" -> { bravery += 10; stamina += 5 } // 熱血漢
            "BLUE-FISH" -> { intelligence += 10; gentleness += 5 } // 冷静沈着
            "GREEN-VEGETABLE" -> { stamina += 10; gentleness += 10 } // 健康優良児
            "YELLOW-SNACK" -> { activity += 10; affection += 10 } // お祭り好き
            "PURPLE-FRUIT" -> { affection += 10; intelligence += 10 } // ミステリアス
            // 他の組み合わせも個別に調整可能...
        }

        return PetEntity(
            name = name,
            speciesId = speciesId.lowercase(),
            weight = finalWeight,
            height = finalHeight,
            customImageUri = imagePath,
            favoriteColor = color,
            favoriteFood = food,
            intelligence = intelligence,
            stamina = stamina,
            activity = activity.coerceIn(0, 100),
            affection = affection.coerceIn(0, 100),
            bravery = bravery.coerceIn(0, 100),
            gentleness = gentleness.coerceIn(0, 100),
            bodyType = bodyType.coerceIn(0f, 100f)
        )
    }
}
