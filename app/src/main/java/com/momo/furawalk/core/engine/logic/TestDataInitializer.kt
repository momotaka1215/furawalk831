package com.momo.furawalk.core.engine.logic

import com.momo.furawalk.data.local.room.AppDatabase
import com.momo.furawalk.data.local.room.entity.*
import kotlinx.coroutines.flow.first

object TestDataInitializer {
    suspend fun insertMockData(db: AppDatabase) {
        val playerDao = db.playerDao()
        val petDao = db.petDao()
        val growthDao = db.growthDao()

        // 1. テストプレイヤーの作成 (全ステータスに値を投入)
        val testPlayer = PlayerEntity(
            id = "default_player",
            name = "テスト・ウォーカ",
            birthDate = System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 365 * 25), // 25年前
            gender = "male",
            
            // 累計アクティビティ
            totalSteps = 125000,
            totalDistance = 88.5 * 1000,
            totalPlayTimeMillis = 3600000 * 48, // 48時間
            totalEncounters = 42,
            uniquePlayersMet = 15,
            totalCheckIns = 85,
            
            // RPG要素
            jobId = "adventurer",
            playerLevel = 12,
            playerExp = 4500,
            str = 18,
            agi = 24,
            sta = 20,
            int = 15,
            luk = 12,
            cha = 30,
            availableAbilityPoints = 5,
            
            greetingMessage = "富良野を全部歩くのが目標です！",
            currentTitleId = "title_furano_expert"
        )
        playerDao.insertOrUpdatePlayer(testPlayer)

        // 2. 通貨の設定
        playerDao.updateCurrency(PlayerCurrencyEntity("MONEY", 75000))
        playerDao.updateCurrency(PlayerCurrencyEntity("EXP", 4500))

        // 3. ペット種別の確認
        val existingSpecies = petDao.getAllSpecies().first()
        if (existingSpecies.isEmpty()) {
            val catSpecies = PetSpeciesEntity(
                id = "cat_test",
                name = "テストねこ",
                species = "cat",
                rarity = 1,
                iconEmoji = "🐈",
                baseHp = 80, baseStamina = 70, baseSpeed = 90, basePower = 50, baseIntelligence = 80
            )
            petDao.insertOrUpdateSpecies(catSpecies)
        }

        // 4. ペットの状態設定 (全ステータスに値を投入)
        val testPet = PetEntity(
            id = "current_pet",
            name = "プロトタイプ・ミケ",
            speciesId = "cat_test",
            level = 22,
            experience = 880,
            health = 0.95f,
            hunger = 0.6f,
            happiness = 0.85f,
            friendship = 0.72f,
            fatigue = 0.1f,
            cleanliness = 0.9f,
            height = 24.8f,
            weight = 1.4f,
            bodyType = 45.0f,
            intelligence = 120,
            stamina = 95,
            activity = 75,
            affection = 80,
            bravery = 40,
            gentleness = 90,
            generation = 2,
            dnaSeed = 987654321L,
            lastUpdateAt = System.currentTimeMillis()
        )
        petDao.updatePetStatus(testPet)

        // 5. インベントリの全アイテム追加 (すべて1つずつ)
        val allItems = listOf(
            "food_chicken", "food_tuna", "food_fruit", "food_meat", "food_vege",
            "food_sweet", "food_nutrition", "food_love", "food_healthy",
            "food_hunger_max", "food_happiness_max", "food_friendship_max",
            "food_special_snack", "food_mystery",
            "action_petting", "action_talk", "action_hug", "action_brush", "item_shampoo",
            "play_cat_teaser", "play_chew_toy", "play_stick", "play_frisbee", "play_feather", "play_jungle_gym"
        )
        allItems.forEach { id ->
            playerDao.updateInventoryItem(InventoryEntity(id, 1))
        }

        // 6. 成長記録のモック
        val records = listOf(
            PetGrowthRecordEntity(petId = "current_pet", message = "ミケの体重が1.4キロになったよ！少し重くなったかな？", type = "HEIGHT", isShown = true),
            PetGrowthRecordEntity(petId = "current_pet", message = "ミケがレベル20の大台に乗ったよ！", type = "LEVEL", isShown = true),
            PetGrowthRecordEntity(petId = "current_pet", message = "ミケとのシンクロ率が上がってきた気がするよ。なつき度70%！", type = "FRIENDSHIP", isShown = false)
        )
        records.forEach { growthDao.insertRecord(it) }

        // 7. 活動履歴のモック (ランダムなバリエーション)
        val calendar = java.util.Calendar.getInstance()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        for (i in 1..10) {
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
            val date = sdf.format(calendar.time)
            db.playerDao().insertOrUpdateDailyActivity(
                DailyActivityEntity(
                    date = date,
                    steps = 3000 + (Math.random() * 12000).toInt(),
                    distanceMeters = 2000.0 + (Math.random() * 10000)
                )
            )
        }

        // 8. 歩荷さん（ボッカさん）の強制出現
        val bokkaDao = db.bokkaDao()
        val eventId = "debug_bokka_event"
        val now = System.currentTimeMillis()
        
        // 既存の古いイベントを消去
        bokkaDao.deactivateExpiredEvents(now + 1000) // 全て期限切れ扱いにする

        val debugBokka = BokkaEventEntity(
            eventId = eventId,
            spawnTime = now,
            expireTime = now + (24 * 60 * 60 * 1000L),
            latitude = 43.342,  // 富良野駅付近
            longitude = 142.383,
            spotName = "デバッグ用キャンプ場",
            bokkaType = "travel",
            message = "デバッグモードへようこそ！珍しいものがあるぞ。",
            isActive = true,
            isArrived = false
        )
        
        val debugBokkaItems = listOf(
            BokkaItemEntity(eventId = eventId, itemId = "food_special_snack", name = "デバッグ用万能おやつ", price = 10, stock = 99),
            BokkaItemEntity(eventId = eventId, itemId = "food_mystery", name = "デバッグ用？？？フード", price = 5, stock = 10),
            BokkaItemEntity(eventId = eventId, itemId = "food_friendship_max", name = "デバッグ用なつきMAX", price = 100, stock = 1)
        )
        
        bokkaDao.insertBokkaEvent(debugBokka)
        bokkaDao.insertBokkaInventory(debugBokkaItems)
    }
}
