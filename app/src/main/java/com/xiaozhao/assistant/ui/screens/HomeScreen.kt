package com.xiaozhao.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaozhao.assistant.data.entity.TaskEntity
import com.xiaozhao.assistant.ui.theme.DoneColor
import com.xiaozhao.assistant.ui.theme.ImportantColor
import com.xiaozhao.assistant.ui.theme.OverdueColor
import com.xiaozhao.assistant.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(vm: AppViewModel) {
    val todayCount by vm.todayTodoCount.collectAsState()
    val unread by vm.unreadCount.collectAsState()
    val important by vm.importantCount.collectAsState()
    val recentTasks by vm.recentTasks.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("小赵手机事务助手", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
        }

        // 统计卡片
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Assignment,
                    iconColor = MaterialTheme.colorScheme.primary,
                    label = "今日待办",
                    count = todayCount
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.MarkEmailUnread,
                    iconColor = OverdueColor,
                    label = "未处理消息",
                    count = unread
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.PriorityHigh,
                    iconColor = ImportantColor,
                    label = "重要消息",
                    count = important
                )
            }
        }

        // 最近待办
        item {
            Spacer(Modifier.height(8.dp))
            Text("最近待办", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
        }

        if (recentTasks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        "暂无待办，享受清净的一天",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = Color.Gray
                    )
                }
            }
        } else {
            items(recentTasks) { task ->
                RecentTaskItem(task)
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    label: String,
    count: Int
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = iconColor, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text("$count", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun RecentTaskItem(task: TaskEntity) {
    val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    val priorityColor = if (task.priority >= 2) ImportantColor else
        if (task.status == TaskEntity.STATUS_DONE) DoneColor else MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(priorityColor, RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2
                )
                if (task.dueTime != null) {
                    Text(
                        "截止: ${dateFormat.format(Date(task.dueTime))}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            if (task.priority >= 2) {
                Text("重要", fontSize = 11.sp, color = ImportantColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}
