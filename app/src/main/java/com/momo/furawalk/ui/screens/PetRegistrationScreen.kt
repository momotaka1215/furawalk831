package com.momo.furawalk.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetRegistrationScreen(
    onRegistrationComplete: (name: String, species: String, weight: Float, height: Float, color: String, food: String, imageUri: Uri?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var species by remember { mutableStateOf("DOG") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("RED") }
    var selectedFood by remember { mutableStateOf("MEAT") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var isCircleFrame by remember { mutableStateOf(true) }

    // 種別変更時にデフォルトの体格を入力
    LaunchedEffect(species) {
        when (species) {
            "DOG" -> { weight = "3.0"; height = "30.0" }
            "CAT" -> { weight = "1.0"; height = "20.0" }
            "MONKEY" -> { weight = "2.0"; height = "25.0" }
            "OTHER" -> { weight = "1.5"; height = "25.0" }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) selectedImageUri = uri }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success && tempPhotoUri != null) {
                selectedImageUri = tempPhotoUri
            }
        }
    )

    fun launchCamera() {
        val tempFile = File(context.cacheDir, "temp_pet_photo_${System.currentTimeMillis()}.jpg")
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        tempPhotoUri = uri
        cameraLauncher.launch(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ペットを登録する") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 画像プレビュー
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(if (isCircleFrame) CircleShape else RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { isCircleFrame = !isCircleFrame }
                    .border(
                        2.dp,
                        MaterialTheme.colorScheme.primary,
                        if (isCircleFrame) CircleShape else RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "ペットの写真",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🐾", fontSize = 48.sp)
                        Text("No Photo", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            
            Text(
                text = if (isCircleFrame) "丸枠表示 (タップで切替)" else "四角枠表示 (タップで切替)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            // 画像選択ボタン
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { launchCamera() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("撮影")
                }
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("選ぶ")
                }
            }

            HorizontalDivider()

            // 入力フォーム
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("ペットの名前") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 種別選択 (簡易版)
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = when(species) {
                        "DOG" -> "イヌ"
                        "CAT" -> "ネコ"
                        "MONKEY" -> "サル"
                        else -> "その他"
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("種別") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("イヌ") },
                        onClick = { species = "DOG"; expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("ネコ") },
                        onClick = { species = "CAT"; expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("サル") },
                        onClick = { species = "MONKEY"; expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("その他") },
                        onClick = { species = "OTHER"; expanded = false }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { if (it.isEmpty() || it.toFloatOrNull() != null) weight = it },
                    label = { Text("体重 (kg)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = height,
                    onValueChange = { if (it.isEmpty() || it.toFloatOrNull() != null) height = it },
                    label = { Text("体長 (cm)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            // 好きな色選択
            var colorExpanded by remember { mutableStateOf(false) }
            val colors = listOf("RED" to "赤", "BLUE" to "青", "GREEN" to "緑", "YELLOW" to "黄", "PURPLE" to "紫")
            ExposedDropdownMenuBox(
                expanded = colorExpanded,
                onExpandedChange = { colorExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = colors.find { it.first == selectedColor }?.second ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("好きな色") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = colorExpanded) },
                    modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = colorExpanded,
                    onDismissRequest = { colorExpanded = false }
                ) {
                    colors.forEach { (key, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = { selectedColor = key; colorExpanded = false }
                        )
                    }
                }
            }

            // 好きな食べ物選択
            var foodExpanded by remember { mutableStateOf(false) }
            val foods = listOf("MEAT" to "お肉", "FISH" to "お魚", "VEGETABLE" to "お野菜", "SNACK" to "お菓子", "FRUIT" to "果物")
            ExposedDropdownMenuBox(
                expanded = foodExpanded,
                onExpandedChange = { foodExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = foods.find { it.first == selectedFood }?.second ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("好きな食べ物") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = foodExpanded) },
                    modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = foodExpanded,
                    onDismissRequest = { foodExpanded = false }
                ) {
                    foods.forEach { (key, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = { selectedFood = key; foodExpanded = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    onRegistrationComplete(
                        name,
                        species,
                        weight.toFloatOrNull() ?: 0f,
                        height.toFloatOrNull() ?: 0f,
                        selectedColor,
                        selectedFood,
                        selectedImageUri
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = name.isNotBlank() && weight.isNotBlank() && height.isNotBlank()
            ) {
                Text("この内容で登録する", fontWeight = FontWeight.Bold)
            }
        }
    }
}
