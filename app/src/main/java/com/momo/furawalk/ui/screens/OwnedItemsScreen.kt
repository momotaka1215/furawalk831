package com.momo.furawalk.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momo.furawalk.data.local.room.entity.InventoryEntity
import com.momo.furawalk.data.local.room.entity.ShopItemEntity
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnedItemsScreen(
    inventory: List<InventoryEntity>,
    shopCatalog: List<ShopItemEntity>,
    onSellItem: (String) -> Unit = {}, // 追加
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("所有アイテム") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (inventory.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "アイテムを持っていません", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(inventory) { item ->
                    val shopItem = shopCatalog.find { it.id == item.itemId }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
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
                                        modifier = Modifier.fillMaxSize().padding(8.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Text(text = "🎁", fontSize = 24.sp)
                                }
                                
                                // 所持数バッジ
                                if (item.quantity > 1) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(4.dp)
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
                                Text(text = "???", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        
                        Text(
                            text = shopItem?.name ?: "不明なアイテム",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                        
                        if (shopItem != null && shopItem.sellPrice > 0) {
                            Button(
                                onClick = { onSellItem(shopItem.id) },
                                modifier = Modifier.height(24.dp).padding(0.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Text("${shopItem.sellPrice}H 売却", fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

