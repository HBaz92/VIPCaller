package com.hassan.dev.vipcaller

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.hassan.dev.vipcaller.core.RingtonePlayer
import com.hassan.dev.vipcaller.core.ScheduleEvaluator
import com.hassan.dev.vipcaller.core.ScheduleMode
import com.hassan.dev.vipcaller.core.VipStore

/**
 * خدمة أمامية خفيفة. لا تنفّذ عملًا دوريًا، لكنها تُبقي العملية حيّة حتى يصل بث
 * PHONE_STATE فورًا على الأجهزة التي تقيّد الخلفية بشدة مثل Honor / MagicOS.
 */
class VipService : Service() {

    companion object {
        private const val CHANNEL_ID = "vip_caller_monitor"
        private const val NOTIFICATION_ID = 4201
        private const val WAKE_TAG = "VIPCaller::ring"

        const val ACTION_START = "com.hassan.dev.vipcaller.START"
        const val ACTION_STOP = "com.hassan.dev.vipcaller.STOP"
        const val ACTION_REFRESH = "com.hassan.dev.vipcaller.REFRESH"
        const val ACTION_RINGING = "com.hassan.dev.vipcaller.RINGING"
        const val ACTION_CALL_ENDED = "com.hassan.dev.vipcaller.CALL_ENDED"

        /** يشغّل الخدمة إذا كان المفتاح الرئيسي مفعّلًا، ويوقفها إن كان مطفأ. */
        fun sync(context: Context) {
            VipStore.init(context)
            if (VipStore.enabled.value) start(context) else stop(context)
        }

        fun start(context: Context) {
            send(context, ACTION_START)
        }

        fun refresh(context: Context) {
            send(context, ACTION_REFRESH)
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, VipService::class.java)) }
        }

        fun notifyRinging(context: Context) = send(context, ACTION_RINGING)

        fun notifyCallEnded(context: Context) = send(context, ACTION_CALL_ENDED)

        private fun send(context: Context, action: String) {
            val intent = Intent(context, VipService::class.java).setAction(action)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        VipStore.init(this)
        // لو قُتلت العملية أثناء مكالمة سابقة، قد يكون رنين النظام ما زال مكتومًا.
        RingtonePlayer.restoreSystemRinger(this)
        createChannel()
        goForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                // زر الإيقاف في الإشعار يطفئ النظام فعليًا، وإلا أعادت MainActivity تشغيله.
                VipStore.setEnabled(this, false)
                RingtonePlayer.stop(this)
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_RINGING -> acquireWakeLock()
            ACTION_CALL_ENDED -> releaseWakeLock()
        }

        goForeground()
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // بعض واجهات Honor تزيل المهمة عند المسح من قائمة التطبيقات الأخيرة؛ نعيد الجدولة.
        if (VipStore.enabled.value) {
            runCatching {
                val restart = Intent(applicationContext, VipService::class.java)
                    .setAction(ACTION_START)
                val pending = PendingIntent.getService(
                    applicationContext,
                    1,
                    restart,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarms = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                alarms.set(
                    android.app.AlarmManager.RTC,
                    System.currentTimeMillis() + 2_000L,
                    pending
                )
            }
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        releaseWakeLock()
        // لا نترك الجوال صامتًا إذا أُوقفت الخدمة بينما النغمة تعمل.
        RingtonePlayer.stop(this)
        super.onDestroy()
    }

    // ---------------------------------------------------------------- helpers

    private fun goForeground() {
        val notification = buildNotification()
        runCatching {
            // نوع specialUse معرّف من API 34 فقط؛ تمريره على إصدار أقدم يرمي استثناء.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun buildNotification(): Notification {
        val schedule = VipStore.schedule.value
        val count = VipStore.contacts.value.count { it.enabled }

        val window = if (schedule.mode == ScheduleMode.ALWAYS) {
            getString(R.string.status_always)
        } else {
            getString(
                R.string.status_window,
                ScheduleEvaluator.formatMinutes(schedule.startMinute),
                ScheduleEvaluator.formatMinutes(schedule.endMinute)
            )
        }

        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stop = PendingIntent.getService(
            this,
            2,
            Intent(this, VipService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_title))
            .setContentText(getString(R.string.service_text, count, window))
            .setSmallIcon(R.drawable.ic_vip_notification)
            .setOngoing(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(open)
            .addAction(0, getString(R.string.action_turn_off), stop)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_monitor),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_monitor_desc)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_TAG).apply {
            setReferenceCounted(false)
            runCatching { acquire(90_000L) }
        }
    }

    private fun releaseWakeLock() {
        runCatching { wakeLock?.takeIf { it.isHeld }?.release() }
        wakeLock = null
    }
}
