package com.xiaozhao.assistant.util

import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.service.notification.StatusBarNotification
import android.text.TextUtils

object NotificationUtils {

    /**
     * 检查通知监听权限是否已授予
     */
    fun isListenerEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        if (flat.isEmpty()) return false

        val target = context.packageName
        val components = flat.split(":")
        for (component in components) {
            val parts = component.split("/")
            if (parts.size == 2 && parts[0] == target) {
                return true
            }
        }
        return false
    }

    /**
     * 获取通知使用权设置的 Intent
     */
    fun getNotificationListenerSettingsIntent(): android.content.Intent {
        return android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /**
     * 忽略电池优化设置的 Intent
     */
    fun getBatteryOptimizationSettingsIntent(): android.content.Intent {
        return android.content.Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /**
     * 应用启动管理设置的 Intent（华为/荣耀特有）
     * 如果系统不支持，回退到应用详情页
     */
    fun getLaunchControlSettingsIntent(context: Context): android.content.Intent {
        // 华为启动管理
        val huaweiIntent = android.content.Intent().apply {
            component = android.content.ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            )
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (context.packageManager.resolveActivity(huaweiIntent, 0) != null) {
            return huaweiIntent
        }

        // 回退到应用详情页
        return android.content.Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(android.net.Uri.fromParts("package", context.packageName, null))
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /**
     * 从 StatusBarNotification 提取通知文本信息
     */
    data class NotificationText(
        val title: String,
        val text: String,
        val bigText: String?
    )

    fun extractText(sbn: StatusBarNotification): NotificationText {
        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

        // 如果 title 为空，尝试用 text 的第一行
        val finalTitle = if (title.isBlank() && text.isNotBlank()) {
            text.lines().firstOrNull() ?: ""
        } else {
            title
        }

        return NotificationText(finalTitle, text, bigText)
    }

    /**
     * 根据 packageName 获取应用名称
     */
    fun getAppName(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    /**
     * 生成通知的唯一去重键
     */
    fun dedupKey(packageName: String, notificationId: Int, postTime: Long, title: String, text: String): String {
        return "$packageName|$notificationId|$postTime|${TextUtils.htmlEncode(title)}|${TextUtils.htmlEncode(text)}"
    }
}
