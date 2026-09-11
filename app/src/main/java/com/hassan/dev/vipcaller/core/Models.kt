package com.hassan.dev.vipcaller.core

/** جهة اتصال مهمة (VIP) محفوظة داخل التطبيق. */
data class VipContact(
    val id: String,
    val name: String,
    val number: String,
    val photoUri: String? = null,
    /** نغمة مخصصة من داخل التطبيق. null = استخدم نغمة جهة الاتصال في النظام. */
    val ringtoneUri: String? = null,
    val enabled: Boolean = true
)

/** جهة اتصال كما هي في دفتر هاتف الجهاز. */
data class DeviceContact(
    val lookupKey: String,
    val name: String,
    val number: String,
    val photoUri: String?,
    val systemRingtone: String?
)

enum class ScheduleMode { ALWAYS, WINDOW }

/**
 * نافذة العمل. [startMinute] و [endMinute] دقائق من منتصف الليل (0..1439).
 * إذا كانت البداية أكبر من النهاية تُعتبر النافذة ممتدة عبر منتصف الليل.
 * [days] أيام الأسبوع بترميز java.util.Calendar.DAY_OF_WEEK (الأحد = 1).
 */
data class Schedule(
    val mode: ScheduleMode = ScheduleMode.ALWAYS,
    val startMinute: Int = 8 * 60,
    val endMinute: Int = 22 * 60,
    val days: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7)
)
