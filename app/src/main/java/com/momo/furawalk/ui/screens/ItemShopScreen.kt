package com.momo.furawalk.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.momo.furawalk.data.local.room.entity.ShopItemEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemShopScreen(
    shopItems: List<ShopItemEntity>,
    currentMoney: Long,
    onPurchase: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 低回復系のフード、おもちゃ、および医療品（注射、シャンプーなど）を抽出
    val filteredItems = shopItems.filter { 
        (it.category == "FOOD" || it.category == "PLAY" || it.category == "MEDICAL") && 
        (it.price <= 300 || it.id == "medical_injection" || it.id == "item_shampoo")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("アイテムショップ") },
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
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "お手頃な育成アイテム",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "日々のペットのお世話に欠かせない基本セットです。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                ShopGrid(items = filteredItems, onPurchase = onPurchase, isPeddler = false)
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
