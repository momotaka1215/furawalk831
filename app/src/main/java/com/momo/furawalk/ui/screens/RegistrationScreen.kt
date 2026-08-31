package com.momo.furawalk.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.momo.furawalk.core.util.NameValidator
import com.momo.furawalk.data.local.room.entity.AvatarEntity
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    avatars: List<AvatarEntity>,
    onRegistrationComplete: (name: String, birthDate: Long, avatarPath: String?) -> Unit,
    onInsertTestData: () -> Unit = {}, // 追加
    modifier: Modifier = Modifier
) {
    // 開発用デフォルト値を設定
    var name by remember { mutableStateOf("テストユーザー") }
    var selectedAvatar by remember { mutableStateOf<AvatarEntity?>(null) }
    
    val calendar = remember { Calendar.getInstance() }
    val currentYear = calendar.get(Calendar.YEAR)
    
    // デフォルトで1990年1月1日をセット
    var selectedYear by remember { mutableStateOf<Int?>(1990) }
    var selectedMonth by remember { mutableStateOf<Int?>(1) }
    var selectedDay by remember { mutableStateOf<Int?>(1) }
    
    var yearExpanded by remember { mutableStateOf(false) }
    var monthExpanded by remember { mutableStateOf(false) }
    var dayExpanded by remember { mutableStateOf(false) }

    val isNameValid = remember(name) {
        NameValidator.isNameValid(name)
    }
    
    val daysInMonth = remember(selectedYear, selectedMonth) {
        if (selectedYear == null || selectedMonth == null) {
            31
        } else {
            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, selectedYear!!)
            cal.set(Calendar.MONTH, selectedMonth!! - 1)
            cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        }
    }

    // 月変更時に日数が足りなくなった場合の調整
    LaunchedEffect(daysInMonth) {
        if (selectedDay != null && selectedDay!! > daysInMonth) {
            selectedDay = daysInMonth
        }
    }
    
    val isBirthDateValid = selectedYear != null && selectedMonth != null && selectedDay != null

    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "冒険を始める準備",
            style = MaterialTheme.typography.headlineLarge
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("ユーザーネーム") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = name.isNotEmpty() && !isNameValid,
            supportingText = {
                if (name.isNotEmpty() && !isNameValid) {
                    Text(
                        text = "その名前は使用できません",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (avatars.isNotEmpty()) {
            Text(
                text = "アバターを選択",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(avatars) { avatar ->
                    val isSelected = selectedAvatar?.id == avatar.id
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(80.dp)
                            .clickable { 
                                selectedAvatar = avatar
                                focusManager.clearFocus()
                            }
                    ) {
                        val bitmap = remember(avatar.localPath) {
                            avatar.localPath?.let { path ->
                                try {
                                    BitmapFactory.decodeFile(path)?.asImageBitmap()
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = 3.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = avatar.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(text = "👤", style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                        
                        Text(
                            text = avatar.name,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }

        Text(
            text = "生年月日",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 年プルダウン
            ExposedDropdownMenuBox(
                expanded = yearExpanded,
                onExpandedChange = { 
                    yearExpanded = !yearExpanded
                    if (yearExpanded) focusManager.clearFocus()
                },
                modifier = Modifier.weight(1.5f)
            ) {
                OutlinedTextField(
                    value = selectedYear?.let { "${it}年" } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("年") },
                    trailingIcon = { 
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded)
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = yearExpanded,
                    onDismissRequest = { yearExpanded = false }
                ) {
                    ((currentYear - 100)..currentYear).reversed().forEach { year ->
                        DropdownMenuItem(
                            text = { Text("${year}年") },
                            onClick = {
                                selectedYear = year
                                yearExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            // 月プルダウン
            ExposedDropdownMenuBox(
                expanded = monthExpanded,
                onExpandedChange = { 
                    monthExpanded = !monthExpanded
                    if (monthExpanded) focusManager.clearFocus()
                },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedMonth?.let { "%02d月".format(it) } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("月") },
                    trailingIcon = { 
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded)
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = monthExpanded,
                    onDismissRequest = { monthExpanded = false }
                ) {
                    (1..12).forEach { month ->
                        DropdownMenuItem(
                            text = { Text("%02d月".format(month)) },
                            onClick = {
                                selectedMonth = month
                                monthExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            // 日プルダウン
            ExposedDropdownMenuBox(
                expanded = dayExpanded,
                onExpandedChange = { 
                    dayExpanded = !dayExpanded
                    if (dayExpanded) focusManager.clearFocus()
                },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedDay?.let { "%02d日".format(it) } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("日") },
                    trailingIcon = { 
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded)
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = dayExpanded,
                    onDismissRequest = { dayExpanded = false }
                ) {
                    (1..daysInMonth).forEach { day ->
                        DropdownMenuItem(
                            text = { Text("%02d日".format(day)) },
                            onClick = {
                                selectedDay = day
                                dayExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                if (isNameValid && isBirthDateValid) {
                    val birthCal = Calendar.getInstance()
                    birthCal.set(selectedYear!!, selectedMonth!! - 1, selectedDay!!, 0, 0, 0)
                    onRegistrationComplete(name, birthCal.timeInMillis, selectedAvatar?.localPath)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = isNameValid && isBirthDateValid
        ) {
            Text("登録してスタート！")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "※アバター作成は後ほど行えます",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // デバッグ用の一発完了ボタン
        OutlinedButton(
            onClick = onInsertTestData,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("🛠️ 開発者用：一発で開始（テストデータ）")
        }
    }
}
