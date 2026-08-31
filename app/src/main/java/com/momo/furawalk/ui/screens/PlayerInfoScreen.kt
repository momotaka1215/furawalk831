package com.momo.furawalk.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momo.furawalk.data.local.room.entity.InventoryEntity
import com.momo.furawalk.data.local.room.entity.PetEntity
import com.momo.furawalk.data.local.room.entity.PetSpeciesEntity
import com.momo.furawalk.data.local.room.entity.PlayerEntity
import com.momo.furawalk.data.local.room.entity.ShopItemEntity
import com.momo.furawalk.data.local.room.dao.TypeVisitStat
import com.momo.furawalk.ui.theme.FurawalkTheme
import kotlin.math.roundToInt

@Composable
fun PlayerInfoScreen(
    playerProfile: PlayerEntity?,
    modifier: Modifier = Modifier,
    level: Int = 1,
    currentExp: Long = 0,
    currentMoney: Long = 0,
    totalSteps: Long = 0,
    totalDistance: Double = 0.0,
    inventory: List<InventoryEntity> = emptyList(),
    shopCatalog: List<ShopItemEntity> = emptyList(),
    petStatus: PetEntity? = null,
    petSpecies: List<PetSpeciesEntity> = emptyList(),
    visitStats: List<TypeVisitStat> = emptyList(),
    onNavigateToItems: () -> Unit = {},
    onNavigateToTrophies: () -> Unit = {},
    onNavigateToAvatarCatalog: () -> Unit = {},
    onNavigateToShop: () -> Unit = {},
    onNavigateToItemShop: () -> Unit = {}, // 追加
    onNavigateToHistory: () -> Unit = {}
) {
    val currentPetSpecies = petSpecies.find { it.id == petStatus?.speciesId }
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column {
                Text(
                    text = playerProfile?.name ?: "冒険者",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "プレイヤー情報",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onNavigateToItemShop) {
                            Text("アイテムへ 🥫")
                        }
                        Button(onClick = onNavigateToShop) {
                            Text("ショップへ 🛒")
                        }
                    }
                }
            }
        }
        
        // ヘッダー: アバター枠(50%幅) + ステータス
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // アバター用プレースホルダー (画面幅の50% / 1:1)
                val bitmap = remember(playerProfile?.avatarPath) {
                    playerProfile?.avatarPath?.let { path ->
                        try {
                            BitmapFactory.decodeFile(path)?.asImageBitmap()
                        } catch (e: Exception) {
                            null
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable { onNavigateToAvatarCatalog() },
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "アバター",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = "👤", 
                            style = MaterialTheme.typography.displayLarge
                        )
                    }
                }
                
                // ステータス表示
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "レベル: $level",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "獲得EXP: $currentExp",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "所持マネー: $currentMoney ヘソ",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                    Text(
                        text = "累計歩数: $totalSteps 歩 (過去120日)",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "累計距離: ${if (totalDistance >= 1000) "%.2f km".format(totalDistance / 1000) else "${totalDistance.roundToInt()} m"} (過去120日)",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "履歴を見る 📅",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onNavigateToHistory() }
                    )
                }
            }
        }

        item {
            Column {
                Text(text = "進行状況", style = MaterialTheme.typography.titleMedium)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "次のレベルまで", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = (currentExp % 1000) / 1000f, 
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 相棒ペット
        item {
            Column {
                Text(text = "相棒ペット", style = MaterialTheme.typography.titleMedium)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val bitmap = remember(currentPetSpecies?.localImagePath) {
                            currentPetSpecies?.localImagePath?.let { path ->
                                try {
                                    BitmapFactory.decodeFile(path)?.asImageBitmap()
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = petStatus?.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = currentPetSpecies?.iconEmoji ?: "🐾", 
                                    style = MaterialTheme.typography.displaySmall
                                )
                            }
                        }
                        Column(modifier = Modifier.padding(start = 16.dp)) {
                            Text(
                                text = petStatus?.name ?: "未設定", 
                                style = MaterialTheme.typography.titleLarge
                            )
                            if (petStatus != null && petStatus.speciesId.isNotEmpty()) {
                                Text(text = "Lv.${petStatus.level} (${currentPetSpecies?.name ?: "ペット"})")
                            } else {
                                Text(text = "ペットを連れていません", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        // 所有アイテム欄 6*3
        item {
            InventoryGrid(
                title = "所有アイテム", 
                color = MaterialTheme.colorScheme.secondaryContainer,
                inventory = inventory,
                shopCatalog = shopCatalog,
                onDetailClick = onNavigateToItems
            )
        }

        // 所有トロフィー欄 6*3
        item {
            TrophyGrid(
                title = "所有トロフィー", 
                stats = visitStats,
                onDetailClick = onNavigateToTrophies
            )
        }
    }
}

@Composable
fun TrophyGrid(
    title: String,
    stats: List<TypeVisitStat>,
    onDetailClick: () -> Unit
) {
    val trophies = remember(stats) {
        val list = mutableListOf<String>()
        stats.forEach { stat ->
            val icon = getTypeEmoji(stat.type)
            when {
                stat.totalCount >= 300 -> list.add("🥇$icon")
                stat.totalCount >= 150 -> list.add("🥈$icon")
                stat.totalCount >= 50 -> list.add("🥉$icon")
                else -> list.add("🏆$icon") // 50回未満は無地のトロフィー
            }
        }
        list
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onDetailClick) {
                Text(text = "詳しく見る →", style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) { rowIndex ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(6) { colIndex ->
                        val itemIndex = rowIndex * 6 + colIndex
                        val trophy = trophies.getOrNull(itemIndex)
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (trophy != null) {
                                Text(text = trophy, fontSize = 18.sp)
                            } else {
                                // 空のボックスには薄く無地のトロフィーを表示
                                Text(
                                    text = "🏆", 
                                    fontSize = 18.sp, 
                                    modifier = Modifier.alpha(0.3f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getTypeEmoji(type: String): String {
    return when (type) {
        "SIGHTSEEING" -> "📸"
        "SHOP" -> "🛍️"
        "PARK" -> "🌳"
        "STATION" -> "🚉"
        "GOVERNMENT" -> "🏛️"
        "PUBLIC" -> "🏢"
        "SCHOOL", "ELEMENTARY_SCHOOL", "JUNIOR_HIGH_SCHOOL", "HIGH_SCHOOL", "NURSING_SCHOOL" -> "🏫"
        "CULTURAL" -> "📖"
        "SPORT" -> "⚽"
        "BASEBALL_GROUND" -> "⚾"
        "PARK_GOLF_COURSE" -> "⛳"
        "POST" -> "📮"
        "TOURISM" -> "🗺️"
        "CONVENIENCE" -> "🏪"
        "SUPERMARKET" -> "🛒"
        "DRUGSTORE" -> "💊"
        "WELFARE" -> "🤝"
        "HOSPITAL" -> "🏥"
        "BANK" -> "🏦"
        "GAS" -> "⛽"
        "MAINTENANCE" -> "🔧"
        "SHRINE" -> "⛩️"
        "TEMPLE" -> "🏯"
        "RESTAURANT" -> "🍴"
        "RAMEN" -> "🍜"
        "SUSHI" -> "🍣"
        "CURRY" -> "🍛"
        "MEAT" -> "🥩"
        "BURGER" -> "🍔"
        "SOBA_UDON" -> "🥢"
        "BAKERY" -> "🥐"
        "IZAKAYA" -> "🍺"
        "LIVE_HOUSE" -> "🎸"
        "SWEETS" -> "🍰"
        "CAFE" -> "☕"
        "COMPANY" -> "🏢"
        "CROSSING" -> "🚦"
        else -> "📍"
    }
}

@Composable
fun InventoryGrid(
    title: String, 
    color: Color, 
    inventory: List<InventoryEntity>,
    shopCatalog: List<ShopItemEntity>,
    onDetailClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onDetailClick) {
                Text(text = "詳しく見る →", style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val gridItems = inventory.take(18) // 6*3 = 18マス分
            
            repeat(3) { rowIndex ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(6) { colIndex ->
                        val itemIndex = rowIndex * 6 + colIndex
                        val item = gridItems.getOrNull(itemIndex)
                        val shopItem = item?.let { inv -> shopCatalog.find { it.id == inv.itemId } }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (shopItem != null) {
                                val bitmap = remember(shopItem.localImagePath) {
                                    shopItem.localImagePath?.let { path ->
                                        try {
                                            BitmapFactory.decodeFile(path)?.asImageBitmap()
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                }
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = shopItem.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Text(text = "🎁", style = MaterialTheme.typography.bodySmall)
                                }
                                
                                // 所持数バッジ
                                if (item.quantity > 1) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                            .padding(horizontal = 4.dp)
                                    ) {
                                        Text(
                                            text = item.quantity.toString(),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            } else {
                                // 空のボックスには薄くギフトボックスを表示
                                Text(
                                    text = "🎁", 
                                    style = MaterialTheme.typography.bodySmall, 
                                    modifier = Modifier.alpha(0.3f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPlayerInfoScreen() {
    FurawalkTheme {
        PlayerInfoScreen(
            playerProfile = null,
            level = 10,
            currentExp = 12500,
            currentMoney = 3200,
            totalSteps = 50000,
            totalDistance = 12500.0,
            visitStats = listOf(
                TypeVisitStat("SHRINE", 55),
                TypeVisitStat("RAMEN", 160),
                TypeVisitStat("STATION", 310)
            )
        )
    }
}
