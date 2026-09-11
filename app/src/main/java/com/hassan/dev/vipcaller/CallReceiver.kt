package com.hassan.dev.vipcaller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.TelephonyManager
import android.util.Log
import com.hassan.dev.vipcaller.core.ContactsRepo
import com.hassan.dev.vipcaller.core.PhoneUtils
import com.hassan.dev.vipcaller.core.RingtonePlayer
import com.hassan.dev.vipcaller.core.VipStore

/**
 * يراقب حالة المكالمات. يشغّل النغمة فقط إذا كان المتصل ضمن قائمة VIP
 * والنظام فعّال الآن حسب المفتاح الرئيسي والجدولة.
 */
class CallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "VipCallReceiver"

        /**
         * أندرويد يرسل الرقم في بث واحد فقط وقد يصل لاحقًا فارغًا،
         * لذلك نحتفظ بآخر رقم معروف حتى نهاية المكالمة.
         */
        @Volatile
        private var lastNumber: String? = null

        /**
         * الأجهزة ثنائية الشريحة تبثّ PHONE_STATE مرة لكل شريحة، فيصل RINGING مرتين
         * لنفس المكالمة. نحتفظ بالرقم الذي نرنّ له حاليًا حتى لا نعيد تشغيل النغمة.
         */
        @Volatile
        private var ringingFor: String? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        VipStore.init(context)

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val incoming = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        if (!incoming.isNullOrBlank()) lastNumber = incoming

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> handleRinging(context, incoming ?: lastNumber)
            TelephonyManager.EXTRA_STATE_OFFHOOK,
            TelephonyManager.EXTRA_STATE_IDLE -> handleCallEnded(context)
        }
    }

    private fun handleRinging(context: Context, number: String?) {
        if (!VipStore.isActiveNow(context)) {
            Log.d(TAG, "النظام غير فعّال الآن — تجاهل المكالمة")
            return
        }

        if (number.isNullOrBlank()) {
            Log.d(TAG, "لم يصل رقم المتصل — لا يمكن مطابقته بقائمة VIP")
            return
        }

        val normalized = PhoneUtils.normalize(number)
        if (normalized == ringingFor && RingtonePlayer.isPlaying) {
            Log.d(TAG, "بث مكرر لنفس المكالمة (شريحة ثانية) — تجاهل")
            return
        }

        val vip = VipStore.findByNumber(context, number)
        if (vip == null) {
            Log.d(TAG, "المتصل ${PhoneUtils.normalize(number)} ليس ضمن قائمة VIP")
            return
        }

        // أولوية النغمة: المختارة داخل التطبيق، ثم نغمة جهة الاتصال في النظام، ثم الافتراضية.
        val tone: Uri? = vip.ringtoneUri?.let { Uri.parse(it) }
            ?: ContactsRepo.systemRingtoneFor(context, number)

        ringingFor = normalized
        VipService.notifyRinging(context)
        RingtonePlayer.play(
            context = context,
            uri = tone,
            vibrate = VipStore.vibrate.value,
            ignoreSilent = VipStore.ignoreSilent.value,
            maxSeconds = VipStore.maxSeconds.value,
            silenceSystemRinger = VipStore.silenceSystemRinger.value
        )
    }

    private fun handleCallEnded(context: Context) {
        lastNumber = null
        ringingFor = null
        RingtonePlayer.stop(context)
        VipService.notifyCallEnded(context)
    }
}
