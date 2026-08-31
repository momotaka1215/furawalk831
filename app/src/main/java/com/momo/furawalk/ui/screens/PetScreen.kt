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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momo.furawalk.data.local.room.entity.InventoryEntity
import com.momo.furawalk.data.local.room.entity.PetEntity
import com.momo.furawalk.data.local.room.entity.PetSpeciesEntity
import com.momo.furawalk.data.local.room.entity.ShopItemEntity
import java.util.Locale

/**
 * ペット用アイテムの表示用モデル
 */
data class PetDisplayItem(
    val id: String,
    val name: String,
    val count: Int,
    val description: String = "",
    val iconEmoji: String = "",
    val meritText: String = "",
    val demeritText: String = ""
)

@Composable
fun PetScreen(
    modifier: Modifier = Modifier,
    inventory: List<InventoryEntity> = emptyList(),
    petStatus: PetEntity? = null,
    petSpecies: List<PetSpeciesEntity> = emptyList(),
    shopCatalog: List<ShopItemEntity> = emptyList(),
    isDebugMode: Boolean = false,
    onUseItem: (String) -> Unit = {},
    onCareAction: (String) -> Unit = {}, // 追加
    onReincarnate: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToCatalog: () -> Unit = {},
    onNavigateToRegistration: () -> Unit = {},
    currentDialogue: com.momo.furawalk.core.engine.conversation.Dialogue? = null,
    onTalkWithPet: () -> Unit = {}
) {
    var showOverlay by remember { mutableStateOf(true) }
    var selectedItemForDialog by remember { mutableStateOf<PetDisplayItem?>(null) }
    
    // 会話の表示制御
    var isDialogueVisible by remember { mutableStateOf(false) }
    LaunchedEffect(currentDialogue) {
        if (currentDialogue != null) {
            isDialogueVisible = true
            kotlinx.coroutines.delay(currentDialogue.displayTimeMillis)
            isDialogueVisible = false
        }
    }

    // ペットが未設定の場合の判定
    val isPetNotSelected = petStatus == null || petStatus.speciesId.isEmpty()

    // DBから取得したステータスを使用
    val health = petStatus?.health ?: 1.0f
    val hunger = petStatus?.hunger ?: 1.0f
    val happiness = petStatus?.happiness ?: 1.0f
    val cleanliness = petStatus?.cleanliness ?: 1.0f
    val fatigue = petStatus?.fatigue ?: 0.0f
    val friendship = petStatus?.friendship ?: 0.0f
    
    val height = petStatus?.height ?: 0f
    val weight = petStatus?.weight ?: 0f
    val bodyType = petStatus?.bodyType ?: 50f
    val generation = petStatus?.generation ?: 1
    
    val petName = petStatus?.name ?: "ペット"
    
    val currentSpecies = petSpecies.find { it.id == petStatus?.speciesId }

    // 性格の判定ロジック
    val personalityText = remember(petStatus) {
        if (petStatus == null) "未知の性格"
        else {
            val traits = mutableListOf<String>()
            if (petStatus.activity > 60) traits.add("活発")
            if (petStatus.affection > 60) traits.add("甘えん坊")
            if (petStatus.bravery > 60) traits.add("勇敢")
            if (petStatus.gentleness > 60) traits.add("おっとり")
            
            if (traits.isEmpty()) "標準的な性格" else traits.joinToString("で")
        }
    }

    // 体型のラベル
    val bodyTypeText = when {
        bodyType < 20 -> "やせすぎ"
        bodyType < 40 -> "やせ型"
        bodyType < 60 -> "標準"
        bodyType < 80 -> "ぽっちゃり"
        else -> "ふっくら"
    }

    // インベントリからお世話アイテムを抽出・分類
    val foodItems = inventory.filter { it.itemId.startsWith("food_") || it.itemId.startsWith("souvenir_") }.map { inv ->
        val master = shopCatalog.find { it.id == inv.itemId }
        PetDisplayItem(
            id = inv.itemId,
            name = master?.name ?: inv.itemId.removePrefix("food_"),
            count = inv.quantity,
            description = master?.description ?: "",
            iconEmoji = master?.description?.trim()?.take(2) ?: "🍲",
            meritText = master?.meritText ?: "",
            demeritText = master?.demeritText ?: ""
        )
    }
    val toyItems = inventory.filter { it.itemId.startsWith("play_") }.map { inv ->
        val master = shopCatalog.find { it.id == inv.itemId }
        PetDisplayItem(
            id = inv.itemId,
            name = master?.name ?: inv.itemId.removePrefix("play_"),
            count = inv.quantity,
            description = master?.description ?: "",
            iconEmoji = master?.description?.trim()?.take(2) ?: "🧸",
            meritText = master?.meritText ?: "",
            demeritText = master?.demeritText ?: ""
        )
    }
    val medicalItems = inventory.filter { it.itemId.startsWith("medical_") || it.itemId == "item_shampoo" }.map { inv ->
        val master = shopCatalog.find { it.id == inv.itemId }
        PetDisplayItem(
            id = inv.itemId,
            name = master?.name ?: inv.itemId.removePrefix("medical_").removePrefix("item_"),
            count = inv.quantity,
            description = master?.description ?: "",
            iconEmoji = master?.description?.trim()?.take(2) ?: "💊",
            meritText = master?.meritText ?: "",
            demeritText = master?.demeritText ?: ""
        )
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(
                    text = "ペットのお世話",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            
            // ... (ペット表示、身体情報、ステータスゲージ、転生ボタンはそのまま) ...
            
            // ペットの表示
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // 吹き出しをペットの上に表示
                    currentDialogue?.let {
                        SpeechBubble(
                            text = it.text,
                            visible = isDialogueVisible,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    val bitmap = remember(currentSpecies?.localImagePath, petStatus?.customImageUri) {
                        val path = petStatus?.customImageUri ?: currentSpecies?.localImagePath
                        path?.let { p ->
                            try {
                                BitmapFactory.decodeFile(p)?.asImageBitmap()
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = petName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = currentSpecies?.iconEmoji ?: "🐾", 
                                style = MaterialTheme.typography.displayLarge
                            )
                        }

                        // 休息中のオーバーレイ (ZZZ)
                        if (petStatus?.isResting == true) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "ZZZ...",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 未選択時のテキストオーバーレイ
                        if (isPetNotSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "ペットを選択してください。",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                    Text(text = petName, style = MaterialTheme.typography.titleLarge)
                    
                    // 健康状態の表示
                    if (petStatus != null) {
                        val healthStatus = petStatus.getHealthStatus()
                        val healthColor = when(healthStatus) {
                            "元気" -> Color(0xFF4CAF50)
                            "元気がない" -> Color(0xFFFFA000)
                            "病気", "弱っている" -> MaterialTheme.colorScheme.error
                            else -> Color.Gray
                        }
                        Surface(
                            color = healthColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, healthColor),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = healthStatus,
                                color = healthColor,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "第 $generation 世代 | $personalityText", 
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 身体情報
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InfoCard(label = "身長", value = String.format(Locale.getDefault(), "%.1f cm", height), modifier = Modifier.weight(1f))
                    InfoCard(label = "体重", value = String.format(Locale.getDefault(), "%.1f kg", weight), modifier = Modifier.weight(1f))
                    InfoCard(label = "世代", value = "第 $generation 代", modifier = Modifier.weight(1f))
                }
            }

            // お世話ボタン (なでる、遊ぶ、休む、話す)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CareActionButton(
                        label = "なでる", 
                        icon = "🖐️", 
                        onClick = { onCareAction("STROKE") },
                        modifier = Modifier.weight(1f)
                    )
                    CareActionButton(
                        label = "遊ぶ", 
                        icon = "🎾", 
                        onClick = { onCareAction("PLAY_GENERIC") },
                        modifier = Modifier.weight(1f)
                    )
                    CareActionButton(
                        label = "休む", 
                        icon = "💤", 
                        onClick = { onCareAction("REST") },
                        modifier = Modifier.weight(1f)
                    )
                    CareActionButton(
                        label = "話す", 
                        icon = "💬", 
                        enabled = !isPetNotSelected && petStatus?.isResting == false,
                        onClick = { onTalkWithPet() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // ステータスゲージ
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        StatusGauge(label = "健康", value = health)
                        StatusGauge(label = "おなか", value = hunger)
                        StatusGauge(label = "ごきげん", value = happiness)
                        StatusGauge(label = "なつき度", value = friendship)
                        StatusGauge(label = "清潔さ", value = cleanliness)
                        StatusGauge(label = "疲労度", value = fatigue, isInverse = true)
                        StatusGauge(label = "体型 ($bodyTypeText)", value = bodyType / 100f)

                        if (isDebugMode && petStatus != null) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                            Text("🛠️ デバッグ：全パラメーター", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // 能力値
                            DebugParamRow("知能", "${petStatus.intelligence}")
                            DebugParamRow("スタミナ", "${petStatus.stamina}")
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // 性格要素
                            DebugParamRow("活発さ", "${petStatus.activity}/100")
                            DebugParamRow("甘えん坊", "${petStatus.affection}/100")
                            DebugParamRow("勇敢さ", "${petStatus.bravery}/100")
                            DebugParamRow("おっとり", "${petStatus.gentleness}/100")
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // 内部係数
                            DebugParamRow("身長傾向", "x${String.format("%.2f", petStatus.innateHeightTrend)}")
                            DebugParamRow("肥満傾向", "x${String.format("%.2f", petStatus.innateBodyTrend)}")
                            DebugParamRow("経験値", "${petStatus.experience}")
                        }
                    }
                }
            }

            // 転生（進化）ボタン
            if (petStatus != null && petStatus.level >= 30) {
                item {
                    Button(
                        onClick = onReincarnate,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("次世代へ転生する (Lv.${petStatus.level})")
                    }
                }
            }

            // ごはんセクション
            item {
                PetItemGrid(
                    title = "ごはん", 
                    items = foodItems,
                    placeholderEmoji = "🍖",
                    onItemClick = { item -> selectedItemForDialog = item }
                )
            }

            // グッズセクション
            item {
                PetItemGrid(
                    title = "グッズ", 
                    items = toyItems,
                    placeholderEmoji = "🧸",
                    onItemClick = { item -> selectedItemForDialog = item }
                )
            }

            // くすりセクション (追加)
            item {
                PetItemGrid(
                    title = "くすり",
                    items = medicalItems,
                    placeholderEmoji = "💊",
                    onItemClick = { item -> selectedItemForDialog = item }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 確認ダイアログ
        selectedItemForDialog?.let { item ->
            AlertDialog(
                onDismissRequest = { selectedItemForDialog = null },
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(text = item.iconEmoji, fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = item.name, style = MaterialTheme.typography.titleLarge)
                    }
                },
                text = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = item.description, 
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        if (item.meritText.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "✅ メリット: ${item.meritText}",
                                color = Color(0xFF4CAF50),
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        if (item.demeritText.isNotEmpty() && item.demeritText != "特になし") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "⚠️ デメリット: ${item.demeritText}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "$petName にあげますか？",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onUseItem(item.id)
                            selectedItemForDialog = null
                        }
                    ) {
                        Text("はい")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedItemForDialog = null }) {
                        Text("いいえ")
                    }
                }
            )
        }

        // ペット未設定時のオーバーレイ
        if (isPetNotSelected && showOverlay) {
            PetSelectionOverlay(
                petSpecies = petSpecies.take(3), // 最初3つを表示
                onDetailClick = onNavigateToDetail,
                onCatalogClick = onNavigateToCatalog,
                onRegistrationClick = onNavigateToRegistration,
                onClose = { showOverlay = false }
            )
        }
    }
}

@Composable
fun PetSelectionOverlay(
    petSpecies: List<PetSpeciesEntity>,
    onDetailClick: (String) -> Unit,
    onCatalogClick: () -> Unit,
    onRegistrationClick: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(top = 64.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "おや？まだペットがきまっていないみたいですね。散歩のお供のペットを決めてください",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Button(
                    onClick = onCatalogClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("ペットカタログから選ぶ", style = MaterialTheme.typography.titleMedium)
                }

                OutlinedButton(
                    onClick = onRegistrationClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                ) {
                    Text("自分のペットを登録する", style = MaterialTheme.typography.titleMedium)
                }

                Text(text = "おすすめのペット", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    petSpecies.forEach { species ->
                        PetSelectionItem(
                            species = species,
                            onDetailClick = { onDetailClick(species.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 閉じるボタン (大きく、白枠線の丸の中に✕)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "閉じる",
                    modifier = Modifier.size(36.dp),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun PetSelectionItem(
    species: PetSpeciesEntity,
    onDetailClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val bitmap = remember(species.localImagePath) {
            species.localImagePath?.let { path ->
                try {
                    BitmapFactory.decodeFile(path)?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }
        }

        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = species.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(text = species.iconEmoji, fontSize = 40.sp)
            }
        }
        
        Text(
            text = species.name,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Button(
            onClick = onDetailClick,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            modifier = Modifier.height(32.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("詳細", fontSize = 12.sp)
        }
    }
}

@Composable
fun PetItemGrid(
    title: String, 
    items: List<PetDisplayItem>, 
    placeholderEmoji: String,
    onItemClick: (PetDisplayItem) -> Unit
) {
    Column {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val totalSlots = 15
            val columns = 5
            val rows = totalSlots / columns
            
            for (r in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (c in 0 until columns) {
                        val index = r * columns + c
                        val item = if (index < items.size) items[index] else null
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .then(
                                    if (item != null) {
                                        Modifier.clickable { onItemClick(item) }
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (item != null) {
                                Text(
                                    text = item.iconEmoji,
                                    fontSize = 24.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(2.dp).padding(bottom = 8.dp)
                                )
                            } else {
                                Text(
                                    text = placeholderEmoji,
                                    fontSize = 16.sp,
                                    modifier = Modifier.alpha(0.3f)
                                )
                            }

                            // 所持戸数表示用のレイヤーボックス (高さ30%)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.3f)
                                    .align(Alignment.BottomCenter)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                val count = item?.count ?: 0
                                Text(
                                    text = count.toString(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DebugParamRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CareActionButton(
    label: String,
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(72.dp),
        contentPadding = PaddingValues(4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = icon, fontSize = 24.sp)
            Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun InfoCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall)
            Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StatusGauge(label: String, value: Float, isInverse: Boolean = false) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = label, style = MaterialTheme.typography.labelMedium)
                Text(text = "${(value * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
            }
            val progressColor = if (isInverse) {
                if (value > 0.8f) Color.Red else MaterialTheme.colorScheme.primary
            } else {
                if (value < 0.2f) Color.Red else MaterialTheme.colorScheme.primary
            }
            LinearProgressIndicator(
                progress = value,
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = progressColor,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}
