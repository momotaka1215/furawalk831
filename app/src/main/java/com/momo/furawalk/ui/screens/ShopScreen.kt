package com.momo.furawalk.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momo.furawalk.R
import com.momo.furawalk.data.local.room.entity.ShopItemEntity
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    shopItems: List<ShopItemEntity>,
    currentMoney: Long,
    onPurchase: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fixedItems = shopItems.filter { it.shopType == "FIXED" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("常設ショップ") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$currentMoney ヘソ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // ショップヘッダー（背景画像 + 店主アイコン）
                Box(modifier = Modifier.fillMaxWidth().height(topImageHeight + 40.dp)) {
                    // 背景画像
                    Image(
                        painter = painterResource(id = R.drawable.shop_imag1),
                        contentDescription = "ショップのイラスト",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(topImageHeight)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    
                    // 店主アイコン（丸型レイヤー）
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
                            painter = painterResource(id = R.drawable.shop_imag1), // 仮で同じ画像を使用
                            contentDescription = "店主",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            item {
                Text(
                    text = "基本のアイテム",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                ShopGrid(items = fixedItems, onPurchase = onPurchase, isPeddler = false)
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun PeddlerHeader(name: String, icon: String, color: Color, message: String = "珍しい掘り出し物があるようです...") {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 40.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun ShopGrid(items: List<ShopItemEntity>, onPurchase: (String) -> Unit, isPeddler: Boolean) {
    val chunked = items.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        chunked.forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { item ->
                    ShopItemCard(
                        item = item,
                        onPurchase = { onPurchase(item.id) },
                        isPeddler = isPeddler,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ShopItemCard(
    item: ShopItemEntity,
    onPurchase: () -> Unit,
    isPeddler: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPeddler) 
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (isPeddler) 
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
        else null
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // 商品画像
            val bitmap = remember(item.localImagePath) {
                item.localImagePath?.let { path ->
                    try {
                        BitmapFactory.decodeFile(path)?.asImageBitmap()
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // 説明文の先頭の絵文字をフォールバックとして使用
                    val emoji = item.description.trim().take(2).ifEmpty { "📦" }
                    Text(emoji, fontSize = 40.sp)
                }

                // 期間限定バッジ
                if (item.isLimited) {
                    Surface(
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "限定",
                            color = Color.White,
                            fontSize = 8.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // メリット表示
            if (item.meritText.isNotEmpty()) {
                Text(
                    text = "✓ ${item.meritText}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4CAF50),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // デメリット表示
            if (item.demeritText.isNotEmpty() && item.demeritText != "特になし") {
                Text(
                    text = "⚠ ${item.demeritText}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp,
                modifier = Modifier.height(28.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.price} ヘソ",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
                Button(
                    onClick = onPurchase,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("購入", fontSize = 11.sp)
                }
            }
        }
    }
}
