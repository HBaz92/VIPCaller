package com.hassan.dev.vipcaller.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale

/**
 * المخزن الوحيد لحالة التطبيق: قائمة VIP + المفتاح الرئيسي + الجدولة.
 * يعتمد على SharedPreferences حتى يقرأه CallReceiver مباشرة دون تهيئة ثقيلة.
 */
object VipStore {

    private const val PREFS = "vip_caller_prefs"
    private const val KEY_CONTACTS = "contacts"
    private const val KEY_ENABLED = "master_enabled"
    private const val KEY_MODE = "schedule_mode"
    private const val KEY_START = "schedule_start"
    private const val KEY_END = "schedule_end"
    private const val KEY_DAYS = "schedule_days"
    private const val KEY_VIBRATE = "vibrate"
    private const val KEY_IGNORE_SILENT = "ignore_silent"
    private const val KEY_MAX_SECONDS = "max_seconds"
    private const val KEY_SILENCE_RINGER = "silence_system_ringer"

    private lateinit var prefs: SharedPreferences

    private val _contacts = MutableStateFlow<List<VipContact>>(emptyList())
    val contacts: StateFlow<List<VipContact>> = _contacts

    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled

    private val _schedule = MutableStateFlow(Schedule())
    val schedule: StateFlow<Schedule> = _schedule

    private val _vibrate = MutableStateFlow(true)
    val vibrate: StateFlow<Boolean> = _vibrate

    private val _ignoreSilent = MutableStateFlow(true)
    val ignoreSilent: StateFlow<Boolean> = _ignoreSilent

    private val _maxSeconds = MutableStateFlow(45)
    val maxSeconds: StateFlow<Int> = _maxSeconds

    private val _silenceSystemRinger = MutableStateFlow(true)
    val silenceSystemRinger: StateFlow<Boolean> = _silenceSystemRinger

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        reload()
    }

    private fun ensure(context: Context) = init(context)

    private fun reload() {
        _contacts.value = parseContacts(prefs.getString(KEY_CONTACTS, null))
        _enabled.value = prefs.getBoolean(KEY_ENABLED, true)
        _schedule.value = Schedule(
            mode = runCatching {
                ScheduleMode.valueOf(prefs.getString(KEY_MODE, null) ?: ScheduleMode.ALWAYS.name)
            }.getOrDefault(ScheduleMode.ALWAYS),
            startMinute = prefs.getInt(KEY_START, 8 * 60),
            endMinute = prefs.getInt(KEY_END, 22 * 60),
            days = prefs.getStringSet(KEY_DAYS, null)
                ?.mapNotNull { it.toIntOrNull() }?.toSet()
                ?: setOf(1, 2, 3, 4, 5, 6, 7)
        )
        _vibrate.value = prefs.getBoolean(KEY_VIBRATE, true)
        _ignoreSilent.value = prefs.getBoolean(KEY_IGNORE_SILENT, true)
        _maxSeconds.value = prefs.getInt(KEY_MAX_SECONDS, 45)
        _silenceSystemRinger.value = prefs.getBoolean(KEY_SILENCE_RINGER, true)
    }

    // ---------------------------------------------------------------- contacts

    fun addContact(context: Context, contact: VipContact) {
        ensure(context)
        val current = _contacts.value
        if (current.any { PhoneUtils.matches(it.number, contact.number) }) return
        persistContacts(current + contact)
    }

    /** يضيف دفعة مستوردة متجاهلًا الأرقام الموجودة مسبقًا، ويعيد عدد المضاف فعليًا. */
    fun addAll(context: Context, incoming: List<VipContact>): Int {
        ensure(context)
        val merged = _contacts.value.toMutableList()
        var added = 0
        incoming.forEach { candidate ->
            if (merged.none { PhoneUtils.matches(it.number, candidate.number) }) {
                merged += candidate
                added++
            }
        }
        if (added > 0) persistContacts(merged)
        return added
    }

    fun removeContact(context: Context, id: String) {
        ensure(context)
        persistContacts(_contacts.value.filterNot { it.id == id })
    }

    fun updateContact(context: Context, contact: VipContact) {
        ensure(context)
        persistContacts(_contacts.value.map { if (it.id == contact.id) contact else it })
    }

    fun clearContacts(context: Context) {
        ensure(context)
        persistContacts(emptyList())
    }

    /** يبحث عن جهة VIP مفعّلة مطابقة للرقم الوارد. */
    fun findByNumber(context: Context, number: String?): VipContact? {
        ensure(context)
        return _contacts.value.firstOrNull { it.enabled && PhoneUtils.matches(it.number, number) }
    }

    private fun persistContacts(list: List<VipContact>) {
        val array = JSONArray()
        list.forEach { c ->
            array.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("number", c.number)
                    put("photoUri", c.photoUri ?: JSONObject.NULL)
                    put("ringtoneUri", c.ringtoneUri ?: JSONObject.NULL)
                    put("enabled", c.enabled)
                }
            )
        }
        prefs.edit().putString(KEY_CONTACTS, array.toString()).apply()
        _contacts.value = list
    }

    private fun parseContacts(raw: String?): List<VipContact> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                VipContact(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    number = o.optString("number"),
                    photoUri = o.optString("photoUri").takeIf { it.isNotBlank() && it != "null" },
                    ringtoneUri = o.optString("ringtoneUri").takeIf { it.isNotBlank() && it != "null" },
                    enabled = o.optBoolean("enabled", true)
                )
            }
        }.getOrDefault(emptyList())
    }

    // ---------------------------------------------------------------- settings

    fun setEnabled(context: Context, value: Boolean) {
        ensure(context)
        prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        _enabled.value = value
    }

    fun setSchedule(context: Context, schedule: Schedule) {
        ensure(context)
        prefs.edit()
            .putString(KEY_MODE, schedule.mode.name)
            .putInt(KEY_START, schedule.startMinute)
            .putInt(KEY_END, schedule.endMinute)
            .putStringSet(KEY_DAYS, schedule.days.map { it.toString() }.toSet())
            .apply()
        _schedule.value = schedule
    }

    fun setVibrate(context: Context, value: Boolean) {
        ensure(context)
        prefs.edit().putBoolean(KEY_VIBRATE, value).apply()
        _vibrate.value = value
    }

    fun setIgnoreSilent(context: Context, value: Boolean) {
        ensure(context)
        prefs.edit().putBoolean(KEY_IGNORE_SILENT, value).apply()
        _ignoreSilent.value = value
    }

    fun setMaxSeconds(context: Context, value: Int) {
        ensure(context)
        prefs.edit().putInt(KEY_MAX_SECONDS, value).apply()
        _maxSeconds.value = value
    }

    fun setSilenceSystemRinger(context: Context, value: Boolean) {
        ensure(context)
        prefs.edit().putBoolean(KEY_SILENCE_RINGER, value).apply()
        _silenceSystemRinger.value = value
    }

    // ---------------------------------------------------------------- schedule

    /** هل النظام فعّال الآن؟ يجمع المفتاح الرئيسي مع نافذة الجدولة. */
    fun isActiveNow(context: Context, now: Calendar = Calendar.getInstance()): Boolean {
        ensure(context)
        if (!_enabled.value) return false
        return ScheduleEvaluator.isWithin(_schedule.value, now)
    }
}

object ScheduleEvaluator {

    fun isWithin(schedule: Schedule, now: Calendar = Calendar.getInstance()): Boolean {
        if (schedule.mode == ScheduleMode.ALWAYS) return true
        if (schedule.days.isEmpty()) return false

        val minutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val today = now.get(Calendar.DAY_OF_WEEK)
        val start = schedule.startMinute
        val end = schedule.endMinute

        return if (start <= end) {
            today in schedule.days && minutes >= start && minutes < end
        } else {
            // نافذة ممتدة عبر منتصف الليل: ما بعد منتصف الليل ينتمي ليوم البدء السابق.
            when {
                minutes >= start -> today in schedule.days
                minutes < end -> previousDay(today) in schedule.days
                else -> false
            }
        }
    }

    private fun previousDay(day: Int): Int =
        if (day == Calendar.SUNDAY) Calendar.SATURDAY else day - 1

    fun formatMinutes(minutes: Int): String =
        String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60)
}
