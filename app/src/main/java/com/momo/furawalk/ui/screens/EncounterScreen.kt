package com.momo.furawalk.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momo.furawalk.core.domain.provider.MetPlayer
import com.momo.furawalk.data.local.room.entity.EncounterHistoryEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EncounterScreen(
    metPlayers: List<MetPlayer>,
    encounterHistory: List<EncounterHistoryEntity>,
    isScanning: Boolean,
    onNavigateToSetGreeting: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayMetPlayers = if (metPlayers.isEmpty() && encounterHistory.isEmpty()) {
        // デバッグ用モックデータ10件
        List(10) { i ->
            MetPlayer(
                id = "mock_$i",
                name = "冒険者 ${i + 1}",
                level = (i * 3) + 1,
                totalDistance = (i * 1.5) + 0.5,
                message = "富良野の${i + 1}番目の絶景を探しています！",
                timestamp = System.currentTimeMillis() - (i * 3600000L),
                avatarPath = null
            )
        }
    } else {
        metPlayers
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "すれ違い履歴",
                style = MaterialTheme.typography.headlineMedium
            )
            Button(onClick = onNavigateToSetGreeting) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("あいさつ設定")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            color = if (isScanning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isScanning) "📡 すれ違い通信：探索中" else "⚠️ すれ違い通信：停止中",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (displayMetPlayers.isNotEmpty()) {
                item {
                    Text(
                        text = if (metPlayers.isEmpty()) "モックデータ表示中" else "近くにいるプレイヤー",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(displayMetPlayers) { player ->
                    EncounterItem(
                        name = player.name,
                        level = player.level,
                        totalDistance = player.totalDistance,
                        message = player.message,
                        avatarPath = player.avatarPath,
                        timestamp = player.timestamp
                    )
                }
            }

            if (encounterHistory.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "これまでの履歴", style = MaterialTheme.typography.titleSmall)
                }
                items(encounterHistory) { history ->
                    EncounterItem(
                        name = history.peerName,
                        level = history.peerLevel,
                        totalDistance = history.peerTotalDistance,
                        message = history.greetingMessage,
                        avatarPath = history.avatarPath,
                        timestamp = history.metAt
                    )
                }
            }
        }
    }
}

@Composable
fun EncounterItem(
    name: String,
    level: Int,
    totalDistance: Double,
    message: String,
    avatarPath: String?,
    timestamp: Long
) {
    val dateStr = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(timestamp))
    val bitmap = remember(avatarPath) {
        avatarPath?.let { path ->
            try {
                BitmapFactory.decodeFile(path)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // アバター表示
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(text = "👤", fontSize = 24.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = "Lv.$level",
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "累積: ${"%.1f".format(totalDistance)}km",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "「$message」",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
