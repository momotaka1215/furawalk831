package com.momo.furawalk.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momo.furawalk.core.domain.greeting.GreetingContext
import com.momo.furawalk.core.domain.greeting.GreetingManager
import com.momo.furawalk.core.domain.model.map.Checkpoint
import com.momo.furawalk.core.domain.provider.MetPlayer
import com.momo.furawalk.data.local.room.entity.DailyActivityEntity
import com.momo.furawalk.data.local.room.entity.DailyQuestEntity
import com.momo.furawalk.data.local.room.entity.PetEntity
import com.momo.furawalk.data.local.room.entity.PetGrowthRecordEntity
import com.momo.furawalk.data.local.room.entity.PlayerEntity
import com.momo.furawalk.data.local.room.entity.ShopItemEntity
import com.momo.furawalk.data.local.room.entity.BokkaEventEntity
import com.momo.furawalk.data.local.room.entity.TortoiseEventStateEntity
import java.time.LocalDateTime
import java.util.Calendar

@Composable
fun HomeScreen(
    playerProfile: PlayerEntity? = null,
    petStatus: PetEntity? = null,
    recentActivity: List<DailyActivityEntity> = emptyList(),
    metPlayers: List<MetPlayer> = emptyList(),
    isScanning: Boolean = false,
    dailyQuest: DailyQuestEntity? = null,
    checkpoints: List<Checkpoint> = emptyList(),
    unshownGrowthRecords: List<PetGrowthRecordEntity> = emptyList(),
    shopCatalog: List<ShopItemEntity> = emptyList(),
    activeBokkaEvent: BokkaEventEntity? = null,
    tortoiseEventState: TortoiseEventStateEntity? = null, // 追加
    isDebugMode: Boolean = false, // 追加
    onToggleDebugMode: () -> Unit = {}, // 追加
    onMarkGrowthAsShown: () -> Unit = {},
    onInsertTestData: () -> Unit = {},
    onExportData: () -> Unit = {},
    onExportModifiedData: () -> Unit = {}, // 追加
    onAddNewCheckpoint: () -> Unit = {},
    onStartTortoiseEvent: () -> Unit = {}, // 追加
    onNavigateToShop: () -> Unit = {},
    onNavigateToBokkaShop: () -> Unit = {}, // 追加
    onNavigateToToraShop: () -> Unit = {}, // 追加
    onNavigateToPlay: () -> Unit = {}, // マップ画面への遷移用に追加
    modifier: Modifier = Modifier
) {
    val questCheckpoint = remember(dailyQuest, checkpoints) {
        checkpoints.find { it.id == dailyQuest?.checkpointId }
    }

    // カウントダウンタイマー用
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(activeBokkaEvent) {
        if (activeBokkaEvent != null) {
            while (true) {
                currentTime = System.currentTimeMillis()
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    val bokkaShopItemCount = remember(shopCatalog) {
        shopCatalog.count { it.shopType == "BOKKA" }
    }

    val toraShopItemCount = remember(shopCatalog) {
        shopCatalog.count { it.shopType == "TORA" }
    }

    val selectedGrowthRecord = remember(unshownGrowthRecords) {
        if (unshownGrowthRecords.isNotEmpty()) {
            unshownGrowthRecords.random()
        } else null
    }

    // 表示したら既読にする
    LaunchedEffect(selectedGrowthRecord) {
        if (selectedGrowthRecord != null) {
            onMarkGrowthAsShown()
        }
    }

    val welcomeData = remember(playerProfile, petStatus, recentActivity, metPlayers) {
        val yesterday = recentActivity.getOrNull(1)
        
        val isBirthday = playerProfile?.let { profile ->
            val now = Calendar.getInstance()
            val birth = Calendar.getInstance().apply { timeInMillis = profile.birthDate }
            now.get(Calendar.MONTH) == birth.get(Calendar.MONTH) &&
                    now.get(Calendar.DAY_OF_MONTH) == birth.get(Calendar.DAY_OF_MONTH)
        } ?: false

        val context = GreetingContext(
            userName = playerProfile?.name ?: "冒険者",
            dateTime = LocalDateTime.now(),
            isBirthday = isBirthday,
            petName = petStatus?.name ?: "ペット",
            isPetHungry = (petStatus?.hunger ?: 1.0f) < 0.3f,
            isPetUnhappy = (petStatus?.happiness ?: 1.0f) < 0.3f,
            yesterdayPassedCount = metPlayers.size, // 簡易的に現在のリストサイズを昨日の数として代用
            lastWalkDistanceKm = (yesterday?.distanceMeters ?: 0.0) / 1000.0,
            lastStepCount = yesterday?.steps ?: 0
        )
        GreetingManager().generateGreeting(context)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        contentPadding = PaddingValues(16.dp)
    ) {
        // ペット成長の報告セクション
        if (selectedGrowthRecord != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "✨", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ペットの成長報告",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = selectedGrowthRecord.message,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // ウェルカムメッセージセクション
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = welcomeData.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        lineHeight = 32.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = welcomeData.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        // 自分のステータス表示
        item {
            Surface(
                color = if (isScanning) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isScanning) "📡 すれ違い通信：探索中" else "⚠️ すれ違い通信：停止中",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 歩荷さん（ボッカさん）出現アラート
        if (activeBokkaEvent != null) {
            // ... (existing code)
            item {
                val remainingMillis = activeBokkaEvent.expireTime - currentTime
                val hours = (remainingMillis / (1000 * 60 * 60))
                val minutes = (remainingMillis % (1000 * 60 * 60)) / (1000 * 60)
                val seconds = (remainingMillis % (1000 * 60)) / 1000
                val timerText = "%02d:%02d:%02d".format(hours, minutes, seconds)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToPlay() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎒", fontSize = 36.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "歩荷さん、出現中！",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "希少な品を持ってきているようです。",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "残り $timerText",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // 「アキレスと亀」イベント告知
        if (tortoiseEventState == null || tortoiseEventState.state == "NOT_STARTED") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🐢", fontSize = 36.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "亀が現れた！",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ボクを追いかけられるかな？",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Button(onClick = onStartTortoiseEvent) {
                            Text("亀を追う")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // ボッカさんの出現告知
        if (bokkaShopItemCount > 0) {
            item {
                PeddlerNotificationCard(
                    name = "ボッカさん",
                    icon = "🎒",
                    count = bokkaShopItemCount,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    onClick = onNavigateToBokkaShop
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // トラさんの出現告知
        if (toraShopItemCount > 0) {
            item {
                PeddlerNotificationCard(
                    name = "トラさん",
                    icon = "🐫",
                    count = toraShopItemCount,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    onClick = onNavigateToToraShop
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (bokkaShopItemCount == 0 && toraShopItemCount == 0) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // デイリークエスト告知
        if (questCheckpoint != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (dailyQuest?.isCompleted == true) 
                            MaterialTheme.colorScheme.surfaceVariant 
                        else 
                            MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "📅 今日のクエスト", 
                                style = MaterialTheme.typography.titleMedium, 
                                fontWeight = FontWeight.Bold,
                                color = if (dailyQuest?.isCompleted == true) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            if (dailyQuest?.isCompleted == true) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "(完了！)", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (dailyQuest?.isCompleted == true)
                                "${questCheckpoint.name} への訪問に成功しました！"
                            else
                                "現在地から最も近い 「${questCheckpoint.name}」 を訪れよう！",
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 24.sp
                        )
                        if (dailyQuest?.isCompleted == false) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "報酬: ${questCheckpoint.rewards.exp} EXP / ${questCheckpoint.rewards.money} ヘソ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // すれ違い通信の結果表示
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🛰️ 周辺のプレイヤー: ${metPlayers.size}人", 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (metPlayers.isEmpty()) {
                        Text(
                            text = "現在、近くにプレイヤーは見つかりませんでした。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    } else {
                        metPlayers.forEach { player ->
                            ListItem(
                                headlineContent = { Text(player.name, fontWeight = FontWeight.Medium) },
                                leadingContent = { Text("👤", fontSize = 24.sp) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // ニュースセクション
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "最新のニュース", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "・富良野エリアに新しいチェックポイントが追加されました。", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "・期間限定イベント「ラベンダーの香り」開催中！", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // デバッグ用データ挿入ボタン
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            Text("🛠️ デバッグツール", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))

            // デバッグモードスイッチ
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleDebugMode() }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("デバッグモード (制限解除 & 補正常時表示)", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = isDebugMode, onCheckedChange = { onToggleDebugMode() })
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedButton(
                onClick = onInsertTestData,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🛠️ デバッグ：テストデータを挿入")
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onExportData,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("📋 デバッグ：全地点をJSON出力")
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onExportModifiedData,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("📝 デバッグ：補正した地点のみ出力")
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onAddNewCheckpoint,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text("📍 デバッグ：現在地に新規目的を作成")
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun PeddlerNotificationCard(
    name: String,
    icon: String,
    count: Int,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 32.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$name が訪れています！",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "珍しいアイテムを $count 点 持っているようです。",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = "見に行く →",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
