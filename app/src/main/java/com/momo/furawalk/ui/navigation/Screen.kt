package com.momo.furawalk.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Registration : Screen("registration", "登録")
    object Home : Screen("home", "ホーム", Icons.Default.Home)
    object Play : Screen("play", "プレイ", Icons.Default.PlayArrow)
    object Pet : Screen("pet", "ペット", Icons.Default.Pets)
    object PlayerInfo : Screen("player_info", "プレイヤー", Icons.Default.Person)
    object Task : Screen("task", "タスク", Icons.Default.List)
    object OwnedItems : Screen("owned_items", "所有アイテム")
    object OwnedTrophies : Screen("owned_trophies", "所有トロフィー")
    object Encounter : Screen("encounter", "すれ違い", Icons.Default.Group)
    object SetGreeting : Screen("set_greeting", "メッセージ設定", Icons.Default.QuestionAnswer)
    object AvatarCatalog : Screen("avatar_catalog", "アバターカタログ")
    object Shop : Screen("shop", "常設ショップ")
    object ItemShop : Screen("item_shop", "アイテムショップ") // 追加
    object BokkaShop : Screen("bokka_shop", "ボッカさんの店")
    object ToraShop : Screen("tora_shop", "トラさんの店") // 追加
    object PetCatalog : Screen("pet_catalog", "ペットカタログ")
    object PetDetail : Screen("pet_detail/{speciesId}", "ペット詳細") {
        fun createRoute(speciesId: String) = "pet_detail/$speciesId"
    }
    object ActivityHistory : Screen("activity_history", "活動履歴")
    object PetRegistration : Screen("pet_registration", "ペット登録")
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Play,
    Screen.Pet,
    Screen.PlayerInfo,
    Screen.Encounter
)
