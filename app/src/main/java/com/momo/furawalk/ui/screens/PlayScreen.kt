package com.momo.furawalk.ui.screens

import android.location.Location
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momo.furawalk.core.domain.model.map.Checkpoint
import com.momo.furawalk.core.domain.model.map.CheckpointType
import com.momo.furawalk.core.domain.model.map.Rewards
import com.momo.furawalk.core.domain.model.event.Event
import com.momo.furawalk.core.domain.provider.LocationData
import com.momo.furawalk.core.domain.provider.VibrationProvider
import com.momo.furawalk.data.local.room.entity.DailyQuestEntity
import com.momo.furawalk.data.local.room.entity.PlayerEntity
import com.momo.furawalk.data.local.room.entity.BokkaEventEntity
import com.momo.furawalk.data.local.room.entity.TortoiseEventStateEntity
import java.util.Calendar
import kotlin.math.roundToInt

enum class DistanceFilter(val label: String) {
    NEAR("200-500m"),
    MEDIUM("500-1000m"),
    FAR("1000m〜"),
    ALL("すべて")
}

@Composable
fun PlayScreen(
    checkpoints: List<Checkpoint> = emptyList(),
    currentLocation: LocationData? = null,
    currentHeading: Float = 0f,
    currentDistance: Double = 0.0,
    currentSteps: Int = 0,
    currentMoney: Long = 0,
    currentExp: Long = 0,
    events: List<Event> = emptyList(),
    checkedInIds: Set<String> = emptySet(),
    playerProfile: PlayerEntity? = null,
    dailyQuest: DailyQuestEntity? = null,
    activeBokkaEvent: BokkaEventEntity? = null,
    tortoiseEventState: TortoiseEventStateEntity? = null,
    isDebugMode: Boolean = false, // 追加
    vibrationProvider: VibrationProvider? = null,
    onCheckIn: (Checkpoint) -> Unit = {},
    onSelectCheckpoint: (Checkpoint) -> Unit = {},
    onCalibrate: (Checkpoint, LocationData) -> Unit = { _, _ -> },
    onRetryGPS: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onOpenBokkaShop: () -> Unit = {},
    onPauseTortoise: () -> Unit = {},
    onResumeTortoise: () -> Unit = {},
    onCancelTortoise: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val scale = (screenWidth / 360f).coerceAtLeast(0.8f)

    var selectedFilter by remember { mutableStateOf(DistanceFilter.NEAR) }

    val bokkaAsCheckpoint = remember(activeBokkaEvent) {
        activeBokkaEvent?.let {
            Checkpoint(
                id = "bokka_target",
                name = "歩荷さん (${it.spotName})",
                latitude = it.latitude,
                longitude = it.longitude,
                radiusMeter = 50f,
                type = CheckpointType.SIGHTSEEING,
                rewards = Rewards(0, 0, null)
            )
        }
    }

    val targetCheckpoint = remember(checkpoints, playerProfile?.activeCheckpointId, currentLocation, checkedInIds, bokkaAsCheckpoint, tortoiseEventState) {
        if (tortoiseEventState?.state == "IN_PROGRESS" && tortoiseEventState.currentDestinationId != null) {
            return@remember Checkpoint(
                id = "tortoise_target",
                name = "逃げた亀 (${tortoiseEventState.currentDestinationName})",
                latitude = tortoiseEventState.currentDestinationLatitude!!,
                longitude = tortoiseEventState.currentDestinationLongitude!!,
                radiusMeter = 30f,
                type = CheckpointType.SIGHTSEEING,
                rewards = Rewards(0, 0, null)
            )
        }

        val selectedId = playerProfile?.activeCheckpointId
        if (selectedId != null) {
            if (selectedId == "bokka_target") bokkaAsCheckpoint 
            else checkpoints.find { it.id == selectedId }
        } else if (currentLocation != null) {
            val normalCandidates = checkpoints.filter { !checkedInIds.contains(it.id) }
            val nearestNormal = normalCandidates.minByOrNull { cp ->
                val results = FloatArray(1)
                Location.distanceBetween(currentLocation.latitude, currentLocation.longitude, cp.latitude, cp.longitude, results)
                results[0]
            }

            if (bokkaAsCheckpoint != null) {
                val bokkaDist = FloatArray(1).also { 
                    Location.distanceBetween(currentLocation.latitude, currentLocation.longitude, bokkaAsCheckpoint.latitude, bokkaAsCheckpoint.longitude, it)
                }[0]
                val nearestNormalDist = nearestNormal?.let { cp ->
                    FloatArray(1).also { Location.distanceBetween(currentLocation.latitude, currentLocation.longitude, cp.latitude, cp.longitude, it) }[0]
                } ?: Float.MAX_VALUE
                if (bokkaDist < nearestNormalDist) bokkaAsCheckpoint else nearestNormal
            } else {
                nearestNormal
            }
        } else {
            checkpoints.firstOrNull()
        }
    }

    val bokkaBearing = remember(activeBokkaEvent, currentLocation) {
        if (activeBokkaEvent != null && currentLocation != null) {
            val results = FloatArray(2)
            Location.distanceBetween(
                currentLocation.latitude, currentLocation.longitude,
                activeBokkaEvent.latitude, activeBokkaEvent.longitude,
                results
            )
            results[1]
        } else null
    }

    val filteredCheckpoints = remember(checkpoints, currentLocation, selectedFilter) {
        if (currentLocation == null) {
            emptyList()
        } else {
            val now = Calendar.getInstance()
            val hour = now.get(Calendar.HOUR_OF_DAY)
            val month = now.get(Calendar.MONTH)
            val isNight = hour >= 18 || hour < 6
            val isWinter = month == Calendar.DECEMBER || month == Calendar.JANUARY || 
                           month == Calendar.FEBRUARY || month == Calendar.MARCH

            checkpoints.filter { cp ->
                if (isDebugMode) return@filter true
                
                if (isNight && !cp.availability.nightSafe) return@filter false
                if (isWinter && !cp.availability.winterAccessible) return@filter false

                val results = FloatArray(1)
                Location.distanceBetween(currentLocation.latitude, currentLocation.longitude, cp.latitude, cp.longitude, results)
                val distance = results[0]
                when (selectedFilter) {
                    DistanceFilter.NEAR -> distance in 200f..500f
                    DistanceFilter.MEDIUM -> distance > 500f && distance <= 1000f
                    DistanceFilter.FAR -> distance > 1000f
                    DistanceFilter.ALL -> true
                }
            }.sortedWith(
                compareByDescending<Checkpoint> { it.priority }
                    .thenBy { cp ->
                        val results = FloatArray(1)
                        Location.distanceBetween(currentLocation.latitude, currentLocation.longitude, cp.latitude, cp.longitude, results)
                        results[0]
                    }
            )
        }
    }

    val allEvents = remember(events, dailyQuest, checkpoints) {
        val dq = dailyQuest?.let { q ->
            val cp = checkpoints.find { it.id == q.checkpointId }
            if (cp != null) {
                Event(
                    id = "daily_${q.date}",
                    title = "【デイリー】${cp.name}を訪れる",
                    description = "現在地から最も近かった目的地です",
                    bonusHeso = cp.rewards.money,
                    bonusExp = cp.rewards.exp,
                    iconEmoji = "📅",
                    startDate = q.date,
                    endDate = q.date,
                    isCompleted = q.isCompleted
                )
            } else null
        }
        if (dq != null) listOf(dq) + events else events
    }

    var destinationBearing: Float? = null
    if (targetCheckpoint != null && currentLocation != null) {
        val results = FloatArray(2)
        Location.distanceBetween(
            currentLocation.latitude, currentLocation.longitude,
            targetCheckpoint.latitude, targetCheckpoint.longitude,
            results
        )
        destinationBearing = results[1]
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = (16 * scale).dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy((12 * scale).dp),
        contentPadding = PaddingValues(top = (16 * scale).dp, bottom = (24 * scale).dp)
    ) {
        item {
            Text(
                text = "Hello Taka",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = (MaterialTheme.typography.headlineMedium.fontSize.value * scale).sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            val dist = if (targetCheckpoint != null && currentLocation != null) {
                val res = FloatArray(1)
                Location.distanceBetween(
                    currentLocation.latitude, currentLocation.longitude,
                    targetCheckpoint.latitude, targetCheckpoint.longitude,
                    res
                )
                res[0]
            } else null

            CompassCard(
                heading = currentHeading,
                destinationBearing = destinationBearing,
                bokkaBearing = bokkaBearing,
                distanceMeters = dist,
                checkpointType = targetCheckpoint?.type,
                scale = scale,
                isLocationAvailable = currentLocation != null
            )
        }

        if (targetCheckpoint != null && currentLocation != null) {
            item {
                val bonus = if (playerProfile?.isDistanceBonusInvalidated == false && playerProfile.startLatitude != null && playerProfile.startLongitude != null) {
                    val res = FloatArray(1)
                    Location.distanceBetween(
                        playerProfile.startLatitude, playerProfile.startLongitude,
                        targetCheckpoint.latitude, targetCheckpoint.longitude,
                        res
                    )
                    val d = res[0]
                    if (d < 300) 0 else ((d / 500).toInt() * 20).coerceAtMost(200)
                } else 0

                NavigationCard(
                    target = targetCheckpoint,
                    current = currentLocation,
                    currentHeading = currentHeading,
                    isAlreadyCheckedIn = checkedInIds.contains(targetCheckpoint.id),
                    distanceBonus = bonus,
                    isBonusInvalidated = playerProfile?.isDistanceBonusInvalidated ?: false,
                    isDebugMode = isDebugMode, // 追加
                    vibrationProvider = vibrationProvider,
                    onCheckIn = { 
                        if (targetCheckpoint.id == "bokka_target") {
                            onOpenBokkaShop()
                        } else {
                            onCheckIn(targetCheckpoint)
                        }
                    },
                    onCalibrate = { onCalibrate(targetCheckpoint, currentLocation) }
                )
            }
        }

        item { MovementTrackerCard(currentSteps, currentDistance) }
        item { CurrencyTrackerCard(currentMoney, currentExp) }

        if (tortoiseEventState != null && tortoiseEventState.state != "NOT_STARTED") {
            item {
                TortoiseTrackerCard(
                    state = tortoiseEventState,
                    onPause = onPauseTortoise,
                    onResume = onResumeTortoise,
                    onCancel = onCancelTortoise
                )
            }
        }

        if (allEvents.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text(text = "進行中のイベント・クエスト", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onNavigateToTasks) { Text(text = "一覧を見る →", style = MaterialTheme.typography.labelMedium) }
                }
            }
            items(allEvents) { event -> EventListItem(event) }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "--- 目的地を探す [全${checkpoints.size}件] ---", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DistanceFilter.values().forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer, selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer)
                    )
                }
            }
        }

        if (currentLocation == null) {
            item { Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { Text(text = "位置情報を取得しています...") } }
        } else if (filteredCheckpoints.isEmpty()) {
            item { Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { Text(text = "該当する目的地が見つかりません") } }
        } else {
            items(filteredCheckpoints) { checkpoint ->
                val isCheckedIn = checkedInIds.contains(checkpoint.id)
                val distance = run {
                    val res = FloatArray(1)
                    Location.distanceBetween(currentLocation.latitude, currentLocation.longitude, checkpoint.latitude, checkpoint.longitude, res)
                    res[0]
                }
                
                CheckpointItem(
                    checkpoint = checkpoint,
                    distance = distance,
                    isSelected = checkpoint.id == targetCheckpoint?.id,
                    isCheckedIn = isCheckedIn,
                    isDebugMode = isDebugMode,
                    onClick = { if (!isCheckedIn) onSelectCheckpoint(checkpoint) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)); LocationStatusCard(currentLocation, currentHeading, onRetryGPS) }
        item {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.1f))) {
                Text(text = "📊 システム情報: DB内の全目的地数: ${checkpoints.size}件", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(8.dp))
            }
        }
    }
}

@Composable
fun CustomCompassNeedle(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val needleWidth = w * 0.08f
        val needleLength = h * 0.42f

        val northLightPath = Path().apply {
            moveTo(cx, cy - needleLength)
            lineTo(cx + needleWidth / 2f, cy)
            lineTo(cx, cy - needleWidth * 0.5f)
            close()
        }
        drawPath(path = northLightPath, brush = Brush.linearGradient(colors = listOf(Color(0xFFFF5252), Color(0xFFFF1744)), start = Offset(cx, cy - needleLength), end = Offset(cx + needleWidth / 2f, cy)))

        val northShadowPath = Path().apply {
            moveTo(cx, cy - needleLength)
            lineTo(cx - needleWidth / 2f, cy)
            lineTo(cx, cy - needleWidth * 0.5f)
            close()
        }
        drawPath(path = northShadowPath, brush = Brush.linearGradient(colors = listOf(Color(0xFFD32F2F), Color(0xFFB71C1C)), start = Offset(cx - needleWidth / 2f, cy), end = Offset(cx, cy - needleWidth * 0.5f)))

        val southLightPath = Path().apply { moveTo(cx, cy + needleLength); lineTo(cx + needleWidth / 2f, cy); lineTo(cx, cy + needleWidth * 0.5f); close() }
        drawPath(path = southLightPath, color = Color(0xFF707070))

        val southShadowPath = Path().apply { moveTo(cx, cy + needleLength); lineTo(cx - needleWidth / 2f, cy); lineTo(cx, cy + needleWidth * 0.5f); close() }
        drawPath(path = southShadowPath, color = Color(0xFF404040))

        val brassRadius = needleWidth * 0.9f
        drawCircle(brush = Brush.radialGradient(colors = listOf(Color(0xFFFFE57F), Color(0xFFC5A059), Color(0xFF7A5C28)), center = Offset(cx, cy), radius = brassRadius), radius = brassRadius, center = Offset(cx, cy))
        drawCircle(brush = Brush.radialGradient(colors = listOf(Color(0xFFFFF9C4), Color(0xFFB8860B)), center = Offset(cx - brassRadius * 0.2f, cy - brassRadius * 0.2f), radius = brassRadius * 0.5f), radius = brassRadius * 0.4f, center = Offset(cx, cy))
    }
}

@Composable
fun CompassCard(heading: Float, destinationBearing: Float? = null, bokkaBearing: Float? = null, distanceMeters: Float? = null, checkpointType: CheckpointType? = null, scale: Float = 1.0f, isLocationAvailable: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "gps_flashing")
    val flashingAlpha by infiniteTransition.animateFloat(initialValue = 0.3f, targetValue = 1.0f, animationSpec = infiniteRepeatable(animation = tween(1000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "alpha")

    Card(modifier = Modifier.fillMaxWidth(0.85f).aspectRatio(1f), shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val dialColor = when {
                distanceMeters != null && distanceMeters <= 50f -> Color(0xFFFFCDD2)
                distanceMeters != null && distanceMeters <= 150f -> Color(0xFFFFF9C4)
                else -> Color.Gray
            }
            Canvas(modifier = Modifier.fillMaxSize(0.9f)) {
                val radius = size.minDimension / 2f
                drawCircle(color = dialColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = (4 * scale).dp.toPx()), alpha = 0.5f)
                for (i in 0 until 12) {
                    val angleDeg = i * 30f
                    val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
                    val outerX = center.x + radius * Math.sin(angleRad.toDouble()).toFloat()
                    val outerY = center.y - radius * Math.cos(angleRad.toDouble()).toFloat()
                    val tickLength = if (i == 0) (15 * scale).dp.toPx() else (10 * scale).dp.toPx()
                    val innerX = center.x + (radius - tickLength) * Math.sin(angleRad.toDouble()).toFloat()
                    val innerY = center.y - (radius - tickLength) * Math.cos(angleRad.toDouble()).toFloat()
                    drawLine(color = if (i == 0) Color.Red else dialColor, start = Offset(outerX, outerY), end = Offset(innerX, innerY), strokeWidth = (if (i == 0) (6 * scale).dp else (4 * scale).dp).toPx(), alpha = 0.8f)
                }
            }

            if (isLocationAvailable && bokkaBearing != null) {
                val relativeBokkaAngle = (bokkaBearing - heading + 360f) % 360f
                Box(modifier = Modifier.fillMaxSize(0.82f).rotate(relativeBokkaAngle), contentAlignment = Alignment.TopCenter) {
                    Text("🎒", fontSize = (22 * scale).sp, modifier = Modifier.rotate(-relativeBokkaAngle))
                }
            }

            if (!isLocationAvailable) {
                Text(text = "GPS情報を取得しています", style = MaterialTheme.typography.labelSmall, color = Color.Red, fontWeight = FontWeight.Bold, fontSize = (12 * scale).sp, modifier = Modifier.alpha(flashingAlpha))
            } else if (destinationBearing != null) {
                val relativeDestinationAngle = (destinationBearing - heading + 360f) % 360f
                Box(modifier = Modifier.fillMaxSize(0.9f).rotate(relativeDestinationAngle), contentAlignment = Alignment.Center) { CustomCompassNeedle(modifier = Modifier.fillMaxSize()) }
            } else {
                Text(text = "目的地がありません", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = (12 * scale).sp)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.TopCenter).padding(top = (4 * scale).dp)) { Text("▲", color = MaterialTheme.colorScheme.primary, fontSize = (16 * scale).sp) }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = (24 * scale).dp)) {
                if (distanceMeters != null) {
                    val distText = if (distanceMeters >= 1000) "%.1fkm".format(distanceMeters / 1000) else "${distanceMeters.roundToInt()}m"
                    Text(text = distText, style = MaterialTheme.typography.headlineSmall.copy(fontSize = (MaterialTheme.typography.headlineSmall.fontSize.value * scale).sp), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                }
                if (checkpointType != null) {
                    Text(text = checkpointType.name, style = MaterialTheme.typography.titleSmall.copy(fontSize = (MaterialTheme.typography.titleSmall.fontSize.value * scale).sp), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun NavigationCard(target: Checkpoint, current: LocationData, currentHeading: Float, isAlreadyCheckedIn: Boolean = false, distanceBonus: Int = 0, isBonusInvalidated: Boolean = false, isDebugMode: Boolean = false, vibrationProvider: VibrationProvider?, onCheckIn: () -> Unit, onCalibrate: () -> Unit) {
    val results = FloatArray(2)
    Location.distanceBetween(current.latitude, current.longitude, target.latitude, target.longitude, results)
    val distance = results[0]
    val initialBearing = results[1]
    val relativeBearing = (initialBearing - currentHeading + 360f) % 360f
    val effectiveAccuracy = current.accuracy
    val isArrived = (distance <= target.radiusMeter) && (distance + (effectiveAccuracy * 0.5f) <= target.radiusMeter + 10f)
    var hasNotifiedArrival by remember(target.id) { mutableStateOf(false) }
    var showCalibrationDialog by remember { mutableStateOf(false) }
    LaunchedEffect(isArrived) { if (isArrived && !hasNotifiedArrival) { vibrationProvider?.vibrateSuccess(); hasNotifiedArrival = true } }

    val displayTargetName = if (isDebugMode || isArrived) {
        target.name
    } else {
        target.name.map { '？' }.joinToString("")
    }

    if (showCalibrationDialog) {
        // ... (no changes to AlertDialog)
        AlertDialog(
            onDismissRequest = { showCalibrationDialog = false },
            title = { Text("座標キャリブレーション") },
            text = { Column { Text("現在のGPS座標で目的地の位置を上書きしますか？"); Spacer(modifier = Modifier.height(8.dp)); Text("現在の緯度: ${current.latitude}", style = MaterialTheme.typography.bodySmall); Text("現在の経度: ${current.longitude}", style = MaterialTheme.typography.bodySmall); Text("精度: ±${current.accuracy}m", style = MaterialTheme.typography.bodySmall) } },
            confirmButton = { TextButton(onClick = { onCalibrate(); showCalibrationDialog = false }) { Text("更新する") } },
            dismissButton = { TextButton(onClick = { showCalibrationDialog = false }) { Text("キャンセル") } }
        )
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (isArrived) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = if (isArrived) "🎉 目的地に到着！ ($displayTargetName)" else "📍 目的地ナビ: $displayTargetName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = "距離: ${if (distance >= 1000) "%.2f km".format(distance / 1000) else "${distance.roundToInt()} m"}", style = MaterialTheme.typography.headlineSmall)
                    Text(text = "方向: ${getDirectionString(initialBearing)} (${initialBearing.roundToInt()}°)", style = MaterialTheme.typography.bodyMedium)
                    if (!isAlreadyCheckedIn) {
                        val rewardText = if (isBonusInvalidated) "予測報酬: ${target.rewards.money} ヘソ (⚠️ボーナス無効)" else "予測報酬: ${target.rewards.money + distanceBonus} ヘソ (ボーナス +$distanceBonus 含む)"
                        Text(text = rewardText, style = MaterialTheme.typography.labelSmall, color = if (isBonusInvalidated) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    }
                }
                if (!isArrived) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isDebugMode) {
                            OutlinedButton(onClick = { showCalibrationDialog = true }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("🔧補正", fontSize = 12.sp) }
                        }
                        Text(text = "⬆️", modifier = Modifier.padding(8.dp).rotate(relativeBearing), style = MaterialTheme.typography.displayMedium)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showCalibrationDialog = true }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("🔧補正", fontSize = 12.sp) }
                        Button(onClick = onCheckIn, enabled = !isAlreadyCheckedIn, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                            val buttonText = when {
                                target.id == "bokka_target" -> "お店を見る"
                                isAlreadyCheckedIn -> "獲得済み"
                                else -> "報酬を受け取る"
                            }
                            Text(buttonText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MovementTrackerCard(steps: Int, distance: Double) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) { Text(text = "👟 今日の歩数", style = MaterialTheme.typography.labelMedium); Text(text = "$steps 歩", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
            Divider(modifier = Modifier.height(40.dp).width(1.dp), color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.3f))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) { Text(text = "📏 移動距離", style = MaterialTheme.typography.labelMedium); Text(text = if (distance >= 1000) "%.2f km".format(distance / 1000) else "${distance.roundToInt()} m", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun CurrencyTrackerCard(money: Long, exp: Long) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) { Text(text = "💰 所持金", style = MaterialTheme.typography.labelMedium); Text(text = "$money ヘソ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            Divider(modifier = Modifier.height(30.dp).width(1.dp), color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.3f))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) { Text(text = "⭐ 経験値", style = MaterialTheme.typography.labelMedium); Text(text = "$exp EXP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun LocationStatusCard(location: LocationData?, currentHeading: Float, onRetryGPS: () -> Unit = {}) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "🛰️ GPS & センサー情報", style = MaterialTheme.typography.titleSmall)
            if (location != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) { Text(text = "緯度: ${"%.5f".format(location.latitude)}", style = MaterialTheme.typography.bodySmall); Text(text = "経度: ${"%.5f".format(location.longitude)}", style = MaterialTheme.typography.bodySmall); Text(text = "高度: ${"%.1f".format(location.altitude)}m", style = MaterialTheme.typography.bodySmall) }
                    Column(modifier = Modifier.weight(1f)) { Text(text = "速度: ${"%.1f".format(location.speed * 3.6)}km/h", style = MaterialTheme.typography.bodySmall); Text(text = "精度: ±${"%.1f".format(location.accuracy)}m", style = MaterialTheme.typography.bodySmall); Text(text = "方位(真北): ${currentHeading.roundToInt()}°", style = MaterialTheme.typography.bodySmall) }
                }
                if (location.verticalAccuracy != null) { Text(text = "垂直精度: ±${"%.1f".format(location.verticalAccuracy)}m", style = MaterialTheme.typography.labelSmall) }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "GPS信号を待機中、または高精度な測位ができません", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                    Button(onClick = onRetryGPS, modifier = Modifier.padding(start = 8.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), shape = MaterialTheme.shapes.small) { Text("再検索", style = MaterialTheme.typography.labelMedium) }
                }
            }
        }
    }
}

@Composable
fun EventListItem(event: Event) {
    Card(modifier = Modifier.fillMaxWidth(), colors = if (event.isCompleted) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant) else CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        ListItem(
            headlineContent = { Row(verticalAlignment = Alignment.CenterVertically) { Text(text = event.iconEmoji, fontSize = 20.sp); Spacer(modifier = Modifier.width(8.dp)); Text(text = event.title, fontWeight = FontWeight.Bold, color = if (event.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface) } },
            supportingContent = { Column { Text(event.description); if (!event.isCompleted) { Text(text = "報酬: ${event.bonusExp} EXP / ${event.bonusHeso} ヘソ" + (event.rewardItemId?.let { " / $it" } ?: ""), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary) } } },
            trailingContent = { Checkbox(checked = event.isCompleted, onCheckedChange = null) }
        )
    }
}

@Composable
fun TortoiseTrackerCard(state: TortoiseEventStateEntity, onPause: () -> Unit, onResume: () -> Unit, onCancel: () -> Unit) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.state) { while (state.state == "IN_PROGRESS") { currentTime = System.currentTimeMillis(); kotlinx.coroutines.delay(1000) } }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = when(state.state) { "COMPLETED" -> Color(0xFFE8F5E9); "FAILED", "ESCAPED" -> Color(0xFFFBE9E7); else -> MaterialTheme.colorScheme.secondaryContainer }), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text("🐢", fontSize = 24.sp); Spacer(modifier = Modifier.width(8.dp)); val stageName = when(state.currentStageIndex) { 0 -> "小"; 1 -> "中"; 2 -> "大"; else -> "？" }; Text(text = "アキレスと亀：ステージ $stageName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.height(8.dp))
            when (state.state) {
                "IN_PROGRESS" -> {
                    val remaining = (state.destinationDeadline - currentTime) / 1000
                    val minutes = (remaining / 60).coerceAtLeast(0); val seconds = (remaining % 60).coerceAtLeast(0)
                    Text("亀が逃げるまで: ${"%02d:%02d".format(minutes, seconds)}", style = MaterialTheme.typography.bodyLarge, color = if (remaining < 60) Color.Red else MaterialTheme.colorScheme.onSecondaryContainer)
                    Text("逃げた回数: ${state.escapeCount} / 3", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = onPause, modifier = Modifier.weight(1f)) { Text("少し休む") }; OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("諦める") } }
                }
                "PAUSED" -> { Text("休憩中...", style = MaterialTheme.typography.bodyLarge); Text("（制限時間は進んでいます！）", style = MaterialTheme.typography.labelSmall); Spacer(modifier = Modifier.height(12.dp)); Button(onClick = onResume, modifier = Modifier.fillMaxWidth()) { Text("追跡を再開") } }
                "COMPLETED" -> { Text("🎉 亀に追いついた！", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold); Text("亀は宝箱を残して消えてしまった...", style = MaterialTheme.typography.bodyMedium); Spacer(modifier = Modifier.height(12.dp)); Button(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("宝箱を開けて終了") } }
                "ESCAPED" -> { Text("💀 亀は遠くへ逃げてしまった...", style = MaterialTheme.typography.bodyLarge); Spacer(modifier = Modifier.height(12.dp)); Button(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("閉じる") } }
            }
        }
    }
}

@Composable
fun CheckpointItem(checkpoint: Checkpoint, distance: Float?, isSelected: Boolean = false, isCheckedIn: Boolean = false, isDebugMode: Boolean = false, onClick: () -> Unit = {}) {
    val displayCheckpointName = if (isDebugMode || isCheckedIn) {
        checkpoint.name
    } else {
        checkpoint.name.map { '？' }.joinToString("")
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Card(modifier = Modifier.fillMaxWidth().alpha(if (isCheckedIn) 0.6f else 1.0f).clickable(onClick = onClick), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface), border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    val icon = if (isDebugMode || isCheckedIn) {
                        when(checkpoint.type) { CheckpointType.SIGHTSEEING -> "📸"; CheckpointType.SHOP -> "🛍️"; CheckpointType.PARK -> "🌳"; CheckpointType.STATION -> "🚉"; CheckpointType.GOVERNMENT -> "🏛️"; CheckpointType.PUBLIC -> "🏢"; CheckpointType.SCHOOL, CheckpointType.ELEMENTARY_SCHOOL, CheckpointType.JUNIOR_HIGH_SCHOOL, CheckpointType.HIGH_SCHOOL, CheckpointType.NURSING_SCHOOL -> "🏫"; CheckpointType.CULTURAL -> "📖"; CheckpointType.SPORT -> "⚽"; CheckpointType.BASEBALL_GROUND -> "⚾"; CheckpointType.PARK_GOLF_COURSE -> "⛳"; CheckpointType.POST -> "📮"; CheckpointType.TOURISM -> "🗺️"; CheckpointType.CONVENIENCE -> "🏪"; CheckpointType.SUPERMARKET -> "🛒"; CheckpointType.DRUGSTORE -> "💊"; CheckpointType.WELFARE -> "🤝"; CheckpointType.HOSPITAL -> "🏥"; CheckpointType.BANK -> "🏦"; CheckpointType.GAS -> "⛽"; CheckpointType.MAINTENANCE -> "🔧"; CheckpointType.LIVE_HOUSE -> "🎸"; CheckpointType.SHRINE -> "⛩️"; CheckpointType.TEMPLE -> "🏯"; CheckpointType.RAMEN -> "🍜"; CheckpointType.SUSHI -> "🍣"; CheckpointType.CURRY -> "🍛"; CheckpointType.MEAT -> "🥩"; CheckpointType.BURGER -> "🍔"; CheckpointType.SOBA_UDON -> "🥢"; CheckpointType.BAKERY -> "🥐"; CheckpointType.IZAKAYA -> "🍺"; CheckpointType.SWEETS -> "🍰"; CheckpointType.RESTAURANT -> "🍴"; CheckpointType.POLICE -> "👮"; CheckpointType.FIRE -> "🚒"; CheckpointType.CAFE -> "☕"; CheckpointType.COMPANY -> "🏢"; CheckpointType.CROSSING -> "🚦" }
                    } else "❓"
                    Text(text = icon, fontSize = 24.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = displayCheckpointName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isSelected || checkpoint.priority >= 5) FontWeight.Bold else FontWeight.Normal,
                            color = if (checkpoint.priority >= 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (checkpoint.priority >= 5 && (isDebugMode || isCheckedIn)) { Text(text = "✨ 重要", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp)) }
                        if (isSelected) { Text(text = "🚩 追跡中", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp)) }
                    }
                    if (distance != null) {
                        val d = if (distance >= 1000) "%.2f km".format(distance / 1000) else "${distance.roundToInt()} m"
                        Text(
                            text = "距離: $d",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Text(text = "報酬: ${checkpoint.rewards.exp} EXP / ${checkpoint.rewards.money} ヘソ", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (isCheckedIn) { Surface(modifier = Modifier.size(24.dp).offset(x = (-4).dp, y = (-4).dp), shape = CircleShape, color = MaterialTheme.colorScheme.error, tonalElevation = 4.dp) { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.padding(4.dp), tint = Color.White) } }
    }
}

fun getDirectionString(bearing: Float): String {
    val b = (bearing + 360f) % 360f
    return when {
        b < 22.5 || b >= 337.5 -> "北"
        b < 67.5 -> "北東"
        b < 112.5 -> "東"
        b < 157.5 -> "南東"
        b < 202.5 -> "南"
        b < 247.5 -> "南西"
        b < 292.5 -> "西"
        else -> "北西"
    }
}
