package com.hassan.dev.vipcaller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hassan.dev.vipcaller.core.VipStore

/** يعيد تشغيل خدمة المراقبة بعد إعادة الإقلاع أو تحديث التطبيق. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            // لا نتعامل مع LOCKED_BOOT_COMPLETED: تخزين التطبيق غير متاح قبل فك القفل.
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                VipStore.init(context)
                if (VipStore.enabled.value) VipService.start(context)
            }
        }
    }
}
