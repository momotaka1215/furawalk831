package com.momo.furawalk.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momo.furawalk.R
import com.momo.furawalk.data.local.room.entity.BokkaEventEntity
import com.momo.furawalk.data.local.room.entity.BokkaItemEntity
import com.momo.furawalk.data.local.room.entity.ShopItemEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BokkaShopScreen(
    event: BokkaEventEntity?,
    inventory: List<BokkaItemEntity>,
    catalogItems: List<ShopItemEntity> = emptyList(), // items.jsonからのデータ
    currentMoney: Long,
    onPurchase: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bokkaCatalogItems = catalogItems.filter { it.shopType == "BOKKA" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ボッカさんの移動販売") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        val configuration = LocalConfiguration.current
        val screenHeight = configuration.screenHeightDp.dp
        val topImageHeight = screenHeight / 5

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ショップヘッダー（背景画像 + 店主アイコン）
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(topImageHeight + 40.dp)) {
                    // 背景画像
                    Image(
                        painter = painterResource(id = R.drawable.shop_image2),
                        contentDescription = "ボッカさんのイラスト",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(topImageHeight)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    
                    // 店主アイコン
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp)
                            .size(80.dp)
                            .border(2.dp, Color.White, CircleShape),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.shop_image2), // 仮
                            contentDescription = "店主",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            
            // 歩荷さんのメッセージセクション
            item {
                PeddlerHeader(
                    name = "ボッカさん",
                    icon = "🎒",
                    message = event?.message ?: "おーい！今日は新鮮なフードを持ってきてるぞ！",
                    color = MaterialTheme.colorScheme.tertiaryContainer
                )
            }

            // GPSイベント限定アイテム (あれば)
            if (inventory.isNotEmpty()) {
                item {
                    Text("限定の掘り出し物", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                items(inventory) { item ->
                    BokkaItemRow(
                        item = item,
                        currentMoney = currentMoney,
                        onPurchase = { onPurchase(item.itemId) }
                    )
                }
            }

            // items.jsonからの常設行商人アイテム
            if (bokkaCatalogItems.isNotEmpty()) {
                item {
                    Text("本日のラインナップ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                items(bokkaCatalogItems) { item ->
                    ShopItemCard(
                        item = item,
                        onPurchase = { onPurchase(item.id) },
                        isPeddler = true
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun BokkaMessageCard(event: BokkaEventEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🎒", fontSize = 48.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "歩荷さん",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "「${event.message}」",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun BokkaItemRow(
    item: BokkaItemEntity,
    currentMoney: Long,
    onPurchase: () -> Unit
) {
    val isSoldOut = item.stock <= 0
    val canAfford = currentMoney >= item.price

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSoldOut) 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // アイテムアイコン（仮）
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("🎁", fontSize = 28.sp)
                if (isSoldOut) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("完売", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "価格: ${item.price} ヘソ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "残り在庫: ${item.stock} 個",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSoldOut) Color.Red else Color.Gray
                )
            }

            Button(
                onClick = onPurchase,
                enabled = !isSoldOut && canAfford,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text(if (isSoldOut) "売り切れ" else "購入")
            }
        }
    }
}
