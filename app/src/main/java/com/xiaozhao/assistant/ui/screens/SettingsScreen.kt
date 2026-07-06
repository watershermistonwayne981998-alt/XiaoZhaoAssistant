package com.xiaozhao.assistant.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaozhao.assistant.data.entity.KeywordRuleEntity
import com.xiaozhao.assistant.ui.theme.DoneColor
import com.xiaozhao.assistant.ui.theme.OverdueColor
import com.xiaozhao.assistant.util.NotificationUtils
import com.xiaozhao.assistant.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val collectionEnabled by vm.collectionEnabled.collectAsState()
    val whitelist by vm.allWhitelist.collectAsState()
    val keywords by vm.allKeywords.collectAsState()

    var listenerEnabled by remember { mutableStateOf(NotificationUtils.isListenerEnabled(context)) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showBackgroundGuide by remember { mutableStateOf(false) }
    var showWhitelistDialog by remember { mutableStateOf(false) }
    var showKeywordDialog by remember { mutableStateOf(false) }
    var exportMessage by remember { mutableStateOf<String?>(null) }

    // 导出文件 launcher
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = vm.exportData()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray())
                    }
                    exportMessage = "数据已导出到所选位置"
                } catch (e: Exception) {
                    exportMessage = "导出失败: ${e.message}"
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ===== 通知监听权限 =====
        item {
            SectionTitle("通知监听权限")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("通知使用权", modifier = Modifier.weight(1f))
                        Text(
                            if (listenerEnabled) "已开启" else "未开启",
                            color = if (listenerEnabled) DoneColor else OverdueColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            context.startActivity(NotificationUtils.getNotificationListenerSettingsIntent())
                            // 回到页面后会刷新状态
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("前往通知使用权设置")
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Filled.ArrowForward, contentDescription = null)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { listenerEnabled = NotificationUtils.isListenerEnabled(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("刷新权限状态")
                    }
                }
            }
        }

        // ===== 采集开关 =====
        item {
            SectionTitle("采集控制")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("通知采集", fontWeight = FontWeight.Medium)
                        Text(
                            if (collectionEnabled) "采集已开启" else "采集已关闭",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = collectionEnabled,
                        onCheckedChange = { vm.toggleCollection() }
                    )
                }
            }
        }

        // ===== 白名单 =====
        item {
            SectionTitle("App 白名单")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "白名单为空时默认采集所有 App；添加后只采集勾选的 App",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(8.dp))
                    if (whitelist.isEmpty()) {
                        Text("尚未添加白名单 App", color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        whitelist.forEach { app ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(app.appName, fontSize = 14.sp)
                                    Text(app.packageName, fontSize = 11.sp, color = Color.Gray)
                                }
                                Switch(
                                    checked = app.enabled,
                                    onCheckedChange = { vm.setWhitelistEnabled(app.packageName, app.appName, it) }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showWhitelistDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("添加 App 到白名单")
                    }
                }
            }
        }

        // ===== 关键词管理 =====
        item {
            SectionTitle("待办关键词管理")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    KeywordList(vm, keywords)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showKeywordDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("添加关键词")
                    }
                }
            }
        }

        // ===== 数据管理 =====
        item {
            SectionTitle("数据管理")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedButton(
                        onClick = {
                            val fileName = "xiaozhao_export_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
                            exportLauncher.launch(fileName)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("导出数据（JSON）")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = OverdueColor)
                        Spacer(Modifier.width(8.dp))
                        Text("清空所有数据", color = OverdueColor)
                    }
                }
            }
        }

        // ===== 后台运行引导 =====
        item {
            SectionTitle("后台运行设置")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "华为 / HarmonyOS 手机需要手动设置才能保持后台运行，否则通知监听服务会被系统杀死",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showBackgroundGuide = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("查看后台运行设置引导")
                    }
                }
            }
        }

        // ===== 关于 =====
        item {
            SectionTitle("关于")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("小赵手机事务助手 v1.0.0")
                    Spacer(Modifier.height(4.dp))
                    Text("所有数据仅保存在本地，不上传任何通知内容", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(4.dp))
                    Text("无广告、无统计SDK、无第三方埋点", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }

    // ===== 弹窗 =====

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空所有数据") },
            text = { Text("确定清空所有通知和待办数据？此操作不可撤销") },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearAllData()
                    showClearDialog = false
                }) { Text("确定清空", color = OverdueColor) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            }
        )
    }

    if (showBackgroundGuide) {
        BackgroundGuideDialog(
            onDismiss = { showBackgroundGuide = false },
            onOpenBattery = {
                context.startActivity(NotificationUtils.getBatteryOptimizationSettingsIntent())
            },
            onOpenLaunch = {
                context.startActivity(NotificationUtils.getLaunchControlSettingsIntent(context))
            }
        )
    }

    if (showWhitelistDialog) {
        AddWhitelistDialog(
            onAdd = { pkg, name -> vm.addWhitelistApp(pkg, name) },
            onDismiss = { showWhitelistDialog = false }
        )
    }

    if (showKeywordDialog) {
        AddKeywordDialog(
            onAdd = { kw, type -> vm.addKeyword(kw, type) },
            onDismiss = { showKeywordDialog = false }
        )
    }

    exportMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { exportMessage = null },
            title = { Text("导出结果") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { exportMessage = null }) { Text("好的") }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun KeywordList(vm: AppViewModel, keywords: List<KeywordRuleEntity>) {
    val typeLabel = mapOf(
        KeywordRuleEntity.TYPE_TIME to "时间词",
        KeywordRuleEntity.TYPE_ACTION to "动作词",
        KeywordRuleEntity.TYPE_WORK to "工作词",
        KeywordRuleEntity.TYPE_URGENT to "紧急词"
    )
    val typeColor = mapOf(
        KeywordRuleEntity.TYPE_TIME to Color(0xFF1565C0),
        KeywordRuleEntity.TYPE_ACTION to Color(0xFF00897B),
        KeywordRuleEntity.TYPE_WORK to Color(0xFF6A1B9A),
        KeywordRuleEntity.TYPE_URGENT to OverdueColor
    )

    if (keywords.isEmpty()) {
        Text("暂无关键词规则", color = Color.Gray)
        return
    }

    keywords.take(20).forEach { kw ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                typeLabel[kw.type] ?: kw.type,
                fontSize = 11.sp,
                color = typeColor[kw.type] ?: Color.Gray,
                modifier = Modifier.width(50.dp)
            )
            Text(kw.keyword, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Switch(
                checked = kw.enabled,
                onCheckedChange = { vm.toggleKeyword(kw.id, it) }
            )
            IconButton(onClick = { vm.deleteKeyword(kw.id) }) {
                Icon(Icons.Filled.Delete, contentDescription = "删除", tint = OverdueColor)
            }
        }
    }
    if (keywords.size > 20) {
        Text("…还有 ${keywords.size - 20} 条，请通过添加/删除管理", fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
private fun AddWhitelistDialog(
    onAdd: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var pkg by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加 App 到白名单") },
        text = {
            Column {
                OutlinedTextField(
                    value = pkg,
                    onValueChange = { pkg = it },
                    label = { Text("包名 (如 com.tencent.mm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("应用名称 (如 微信)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text("常见包名: 微信=com.tencent.mm, 钉钉=com.alibaba.android.rimet, 企业微信=com.tencent.wework, 短信=com.android.mms",
                    fontSize = 11.sp, color = Color.Gray)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (pkg.isNotBlank() && name.isNotBlank()) {
                    onAdd(pkg.trim(), name.trim())
                    onDismiss()
                }
            }) { Text("添加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun AddKeywordDialog(
    onAdd: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var keyword by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(KeywordRuleEntity.TYPE_ACTION) }
    val types = listOf(
        KeywordRuleEntity.TYPE_TIME to "时间词",
        KeywordRuleEntity.TYPE_ACTION to "动作词",
        KeywordRuleEntity.TYPE_WORK to "工作词",
        KeywordRuleEntity.TYPE_URGENT to "紧急词"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加关键词") },
        text = {
            Column {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("关键词") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text("类型", fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    types.forEach { (type, label) ->
                        val selected = selectedType == type
                        TextButton(
                            onClick = { selectedType = type },
                            modifier = Modifier
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Text(
                                label,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (keyword.isNotBlank()) {
                    onAdd(keyword.trim(), selectedType)
                    onDismiss()
                }
            }) { Text("添加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun BackgroundGuideDialog(
    onDismiss: () -> Unit,
    onOpenBattery: () -> Unit,
    onOpenLaunch: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("华为 / HarmonyOS 后台运行设置") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("1. 打开「设置 → 应用 → 小赵手机事务助手 → 耗电详情」，选择「无限制」")
                Spacer(Modifier.height(8.dp))
                Text("2. 打开「设置 → 应用 → 小赵手机事务助手 → 启动管理」，关闭「自动管理」，手动开启全部三个开关")
                Spacer(Modifier.height(8.dp))
                Text("3. 打开「设置 → 电池 → 更多电池设置」，确保省电模式不限制后台")
                Spacer(Modifier.height(8.dp))
                Text("4. 在最近任务列表中，下拉小赵助手卡片加锁，防止被一键清理")
                Spacer(Modifier.height(8.dp))
                Text("5. 如果通知监听服务被系统杀死，请重新打开 App 并检查通知使用权是否仍然开启")
            }
        },
        confirmButton = {
            Column {
                TextButton(onClick = onOpenBattery) { Text("打开电池优化设置") }
                TextButton(onClick = onOpenLaunch) { Text("打开启动管理设置") }
                TextButton(onClick = onDismiss) { Text("知道了") }
            }
        }
    )
}
