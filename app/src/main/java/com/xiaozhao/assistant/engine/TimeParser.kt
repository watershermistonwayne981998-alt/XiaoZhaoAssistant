package com.xiaozhao.assistant.engine

import java.util.Calendar
import java.util.regex.Pattern

/**
 * 从通知文本中提取截止时间
 *
 * 支持的模式：
 * - 今天 / 明天 / 后天 + 上午/下午/晚上 + 时间
 * - "下午5点前" / "12点前" 等
 * - "本周" / "下周" + 周几
 * - "月底" / "本月底"
 */
object TimeParser {

    private val TIME_REGEX = Pattern.compile(
        "(今天|明天|后天|大后天|本周|下周|月底|本月底)?" +
        "(上午|下午|晚上|下班前)?" +
        "(\\d{1,2})\\s*[点时:]\\s*(\\d{1,2})?" +
        "\\s*(前|之前)?"
    )

    private val WEEKDAY_REGEX = Pattern.compile(
        "(本周|下周)\\s*(周一|周二|周三|周四|周五|周六|周日|周天)"
    )

    private val SIMPLE_DAY_REGEX = Pattern.compile(
        "(今天|明天|后天|大后天|本周|下周|月底|本月底|下班前|上午|下午|晚上)"
    )

    /**
     * 解析文本中的时间表达，返回估算的 epoch 毫秒值
     * 找不到返回 null
     */
    fun parseDueTime(text: String, now: Long = System.currentTimeMillis()): Long? {
        val fullMatch = TIME_REGEX.matcher(text)
        if (fullMatch.find()) {
            return parseFullTime(fullMatch, now)
        }

        val weekdayMatch = WEEKDAY_REGEX.matcher(text)
        if (weekdayMatch.find()) {
            return parseWeekday(weekdayMatch, now)
        }

        val simpleMatch = SIMPLE_DAY_REGEX.matcher(text)
        if (simpleMatch.find()) {
            return parseSimpleDay(simpleMatch.group(1)!!, now)
        }

        return null
    }

    private fun parseFullTime(m: java.util.regex.Matcher, now: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val dayWord = m.group(1)
        val period = m.group(2)
        val hourStr = m.group(3)
        val minStr = m.group(4)
        val beforeWord = m.group(5)

        // 日期偏移
        when (dayWord) {
            "今天" -> { /* 不变 */ }
            "明天" -> cal.add(Calendar.DAY_OF_YEAR, 1)
            "后天" -> cal.add(Calendar.DAY_OF_YEAR, 2)
            "大后天" -> cal.add(Calendar.DAY_OF_YEAR, 3)
            "本周", "下周", "月底", "本月底" -> {
                // 有具体时间，按本周/下周处理，否则按月底
                if (dayWord == "月底" || dayWord == "本月底") {
                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                } else if (dayWord == "下周") {
                    cal.add(Calendar.WEEK_OF_YEAR, 1)
                }
            }
        }

        // 时段
        var hour = hourStr?.toIntOrNull() ?: 9
        val minute = minStr?.toIntOrNull() ?: 0

        if (period == "下午" && hour <= 12) {
            hour += 12
        } else if (period == "晚上" && hour < 12) {
            hour += 12
        } else if (period == "上午" && hour == 12) {
            hour = 0
        } else if (period == "下班前") {
            if (hour == 9 && hourStr == null) {
                hour = 18
            }
        }

        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)

        // "前"字 — 如果解析出的时间已经过去，推到明天
        if (beforeWord != null && cal.timeInMillis < now) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return cal.timeInMillis
    }

    private fun parseWeekday(m: java.util.regex.Matcher, now: Long): Long {
        val weekWord = m.group(1)!!
        val dayWord = m.group(2)!!

        val targetDay = when (dayWord) {
            "周一" -> Calendar.MONDAY
            "周二" -> Calendar.TUESDAY
            "周三" -> Calendar.WEDNESDAY
            "周四" -> Calendar.THURSDAY
            "周五" -> Calendar.FRIDAY
            "周六" -> Calendar.SATURDAY
            "周日", "周天" -> Calendar.SUNDAY
            else -> Calendar.MONDAY
        }

        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, 18)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        if (weekWord == "下周") {
            cal.add(Calendar.WEEK_OF_YEAR, 1)
            cal.set(Calendar.DAY_OF_WEEK, targetDay)
        } else {
            cal.set(Calendar.DAY_OF_WEEK, targetDay)
            // 如果本周该天已过，推到下周
            if (cal.timeInMillis < now) {
                cal.add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        return cal.timeInMillis
    }

    private fun parseSimpleDay(word: String, now: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        when (word) {
            "今天" -> cal.set(Calendar.HOUR_OF_DAY, 18)
            "明天" -> { cal.add(Calendar.DAY_OF_YEAR, 1); cal.set(Calendar.HOUR_OF_DAY, 12) }
            "后天" -> { cal.add(Calendar.DAY_OF_YEAR, 2); cal.set(Calendar.HOUR_OF_DAY, 12) }
            "大后天" -> { cal.add(Calendar.DAY_OF_YEAR, 3); cal.set(Calendar.HOUR_OF_DAY, 12) }
            "本周" -> cal.set(Calendar.HOUR_OF_DAY, 18)
            "下周" -> { cal.add(Calendar.WEEK_OF_YEAR, 1); cal.set(Calendar.HOUR_OF_DAY, 18) }
            "月底", "本月底" -> {
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 18)
            }
            "上午" -> cal.set(Calendar.HOUR_OF_DAY, 11)
            "下午" -> cal.set(Calendar.HOUR_OF_DAY, 14)
            "晚上" -> cal.set(Calendar.HOUR_OF_DAY, 20)
            "下班前" -> cal.set(Calendar.HOUR_OF_DAY, 17)
        }

        // 如果时间已过，推到明天
        if (cal.timeInMillis < now && word != "今天" && word != "下班前") {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return cal.timeInMillis
    }
}
