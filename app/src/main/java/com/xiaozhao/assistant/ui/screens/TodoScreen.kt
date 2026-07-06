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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaozhao.assistant.data.entity.TaskEntity
import com.xiaozhao.assistant.ui.theme.DoneColor
import com.xiaozhao.assistant.ui.theme.ImportantColor
import com.xiaozhao.assistant.ui.theme.OverdueColor
import com.xiaozhao.assistant.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(vm: AppViewModel) {
    val pending by vm.pendingTasks.collectAsState()
    val today by vm.todayTasks.collectAsState()
    val overdue by vm.overdueTasks.collectAsState()
    val done by vm.doneTasks.collectAsState()

    var tabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("待确认(${pending.size})", "今日待办(${today.size})", "已逾期(${overdue.size})", "已完成(${done.size})")

    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var deletingTask by remember { mutableStateOf<TaskEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = tabIndex == index,
                    onClick = { tabIndex = index },
                    text = { Text(title, fontSize = 13.sp) }
                )
            }
        }

        val list = when (tabIndex) {
            0 -> pending
            1 -> today
            2 -> overdue
            else -> done
        }

        if (list.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无待办", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(list) { task ->
                    TaskItem(
                        task = task,
                        onConfirm = { vm.confirmTask(task.id) },
                        onComplete = { vm.completeTask(task.id) },
                        onDelete = { deletingTask = task },
                        onEditDue = { editingTask = task }
                    )
                }
            }
        }
    }

    // 修改截止时间弹窗
    editingTask?.let { task ->
        DueTimePickerDialog(
            onConfirm = { time ->
                vm.updateTaskDueTime(task.id, time)
                editingTask = null
            },
            onDismiss = { editingTask = null }
        )
    }

    // 删除确认弹窗
    deletingTask?.let { task ->
        AlertDialog(
            onDismissRequest = { deletingTask = null },
            title = { Text("删除待办") },
            text = { Text("确定删除「${task.title}」？此操作不可撤销") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteTask(task.id)
                    deletingTask = null
                }) { Text("删除", color = OverdueColor) }
            },
            dismissButton = {
                TextButton(onClick = { deletingTask = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun TaskItem(
    task: TaskEntity,
    onConfirm: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    onEditDue: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    val accentColor = when {
        task.status == TaskEntity.STATUS_DONE -> DoneColor
        task.priority >= 2 -> ImportantColor
        task.dueTime != null && task.dueTime < System.currentTimeMillis() -> OverdueColor
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 状态色条
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(40.dp)
                        .padding(end = 0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(accentColor, RoundedCornerShape(2.dp))
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        task.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (task.dueTime != null) {
                        val overdueText = if (task.dueTime < System.currentTimeMillis() && task.status != TaskEntity.STATUS_DONE)
                            " (已逾期)" else ""
                        Text(
                            "截止: ${dateFormat.format(Date(task.dueTime))}$overdueText",
                            fontSize = 12.sp,
                            color = if (overdueText.isNotEmpty()) OverdueColor else Color.Gray
                        )
                    }
                    if (task.sourceApp != null) {
                        Text("来源: ${task.sourceApp}", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (task.priority >= 2) {
                    Text("重要", fontSize = 11.sp, color = ImportantColor, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                }

                if (task.status == TaskEntity.STATUS_PENDING) {
                    TextButton(onClick = onConfirm) { Text("确认") }
                }
                if (task.status != TaskEntity.STATUS_DONE) {
                    IconButton(onClick = onComplete) {
                        Icon(Icons.Filled.Check, contentDescription = "完成", tint = DoneColor)
                    }
                    IconButton(onClick = onEditDue) {
                        Icon(Icons.Filled.Edit, contentDescription = "修改时间", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除", tint = OverdueColor)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DueTimePickerDialog(
    onConfirm: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var pickedDate by remember { mutableStateOf<Long?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }

    if (!showTimePicker) {
        val dateState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    pickedDate = dateState.selectedDateMillis
                    showTimePicker = true
                }) { Text("下一步") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        ) {
            DatePicker(state = dateState)
        }
    } else {
        val timeState = rememberTimePickerState(initialHour = 18, initialMinute = 0, is24Hour = true)
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("选择时间") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    val date = pickedDate ?: System.currentTimeMillis()
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = date
                    cal.set(Calendar.HOUR_OF_DAY, timeState.hour)
                    cal.set(Calendar.MINUTE, timeState.minute)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    onConfirm(cal.timeInMillis)
                }) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        )
    }
}
