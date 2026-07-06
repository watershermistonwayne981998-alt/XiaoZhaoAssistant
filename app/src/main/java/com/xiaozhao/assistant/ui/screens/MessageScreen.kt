package com.xiaozhao.assistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaozhao.assistant.data.entity.NotificationEntity
import com.xiaozhao.assistant.ui.theme.ImportantColor
import com.xiaozhao.assistant.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageScreen(vm: AppViewModel) {
    val notifications by vm.notifications.collectAsState()
    val apps by vm.distinctApps.collectAsState()
    val selectedPkg by vm.selectedPackage.collectAsState()
    val keyword by vm.searchKeyword.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // 搜索框
        OutlinedTextField(
            value = keyword,
            onValueChange = { vm.search(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索关键词…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(8.dp))

        // App 筛选条
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedPkg == null,
                    onClick = { vm.selectPackage(null) },
                    label = { Text("全部") }
                )
            }
            items(apps) { app ->
                FilterChip(
                    selected = selectedPkg == app.packageName,
                    onClick = { vm.selectPackage(app.packageName) },
                    label = { Text(app.appName) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // 通知列表
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无通知消息", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications) { notification ->
                    NotificationItem(
                        notification = notification,
                        onConvert = { vm.convertToTodo(notification) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(
    notification: NotificationEntity,
    onConvert: () -> Unit
) {
    val timeFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isImportant)
                ImportantColor.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    notification.appName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    timeFormat.format(Date(notification.postTime)),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Spacer(Modifier.weight(1f))
                if (notification.isImportant) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "重要",
                        tint = ImportantColor,
                        modifier = Modifier.width(16.dp).height(16.dp)
                    )
                }
                if (!notification.isConvertedToTask) {
                    IconButton(onClick = onConvert, modifier = Modifier.width(32.dp).height(32.dp)) {
                        Icon(
                            Icons.Filled.SwapHoriz,
                            contentDescription = "转待办",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            if (notification.title.isNotBlank()) {
                Text(
                    notification.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (notification.text.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    notification.text,
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!notification.bigText.isNullOrBlank() && notification.bigText != notification.text) {
                Spacer(Modifier.height(2.dp))
                Text(
                    notification.bigText,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (notification.isConvertedToTask) {
                Spacer(Modifier.height(4.dp))
                Text("已转待办", fontSize = 11.sp, color = Color(0xFF2E7D32))
            }
        }
    }
}
