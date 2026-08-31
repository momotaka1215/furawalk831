package com.momo.furawalk.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momo.furawalk.R
import com.momo.furawalk.data.local.room.entity.ShopItemEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToraShopScreen(
    shopItems: List<ShopItemEntity>,
    currentMoney: Long,
    onPurchase: (String) -> Unit,
    onBack: () -> Unit
) {
    val toraItems = shopItems.filter { it.shopType == "TORA" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("トラさんの珍品堂") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        }
    ) { innerPadding ->
        val configuration = LocalConfiguration.current
        val screenHeight = configuration.screenHeightDp.dp
        val topImageHeight = screenHeight / 5

        LazyColumn(
            modifier = Modifier
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
                        painter = painterResource(id = R.drawable.shop_image3),
                        contentDescription = "トラさんのイラスト",
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
                            painter = painterResource(id = R.drawable.shop_image3), // 仮
                            contentDescription = "店主",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            item {
                PeddlerHeader(
                    name = "トラさん",
                    icon = "🐫",
                    message = "やあ！今日はとっておきのおもちゃを持ってきたよ。",
                    color = MaterialTheme.colorScheme.secondaryContainer
                )
            }

            items(toraItems) { item ->
                ShopItemCard(
                    item = item,
                    onPurchase = { onPurchase(item.id) },
                    isPeddler = true
                )
            }
        }
    }
}
