package com.momo.furawalk.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

data class GameTask(val title: String, val description: String, val isCompleted: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tasks = listOf(
        GameTask("山部農協に到着する", "JA山部に行ってチェックインしましょう。", false),
        GameTask("最初の同期", "サーバーから最新のデータを取得しました。", true),
        GameTask("歩いてみよう", "1km以上移動してみましょう。", false)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("クエスト / タスク") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tasks) { task ->
                    TaskItem(task)
                }
            }
        }
    }
}

@Composable
fun TaskItem(task: GameTask) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (task.isCompleted) 
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        else 
            CardDefaults.cardColors()
    ) {
        ListItem(
            headlineContent = { Text(task.title) },
            supportingContent = { Text(task.description) },
            trailingContent = {
                Checkbox(checked = task.isCompleted, onCheckedChange = null)
            }
        )
    }
}
