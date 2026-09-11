package com.hassan.dev.vipcaller.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * أجهزة Honor / Huawei (MagicOS و EMUI) وكذلك Xiaomi و Oppo تقتل الخدمات الخلفية
 * بسياسات خاصة بها. هذه الطبقة تفتح للمستخدم الشاشات المطلوبة لاستثناء التطبيق.
 */
object OemCompat {

    /** هل الجهاز من عائلة Honor/Huawei التي تحتاج "التشغيل التلقائي" يدويًا؟ */
    val isHonorFamily: Boolean
        get() {
            val brand = (Build.BRAND + " " + Build.MANUFACTURER).lowercase()
            return brand.contains("honor") || brand.contains("huawei") || brand.contains("hihonor")
        }

    val deviceLabel: String
        get() = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** يطلب استثناء التطبيق من تحسين البطارية. يعود false إذا لم تقبل الشاشة الفتح. */
    fun requestIgnoreBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        val direct = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:" + context.packageName)
        )
        if (launch(context, direct)) return true
        return launch(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }

    /**
     * يفتح شاشة "التشغيل التلقائي / إدارة بدء التشغيل" الخاصة بالمصنّع.
     * يجرّب المكوّنات بالترتيب وينتهي بصفحة معلومات التطبيق كخيار أخير.
     */
    fun openAutoStartSettings(context: Context): Boolean {
        val candidates = listOf(
            // Honor (MagicOS) و Huawei (EMUI)
            "com.hihonor.systemmanager" to "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
            "com.hihonor.systemmanager" to "com.hihonor.systemmanager.optimize.process.ProtectActivity",
            "com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
            "com.huawei.systemmanager" to "com.huawei.systemmanager.optimize.process.ProtectActivity",
            "com.huawei.systemmanager" to "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity",
            // Xiaomi
            "com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity",
            // Oppo / Realme
            "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
            // Vivo
            "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
            // Samsung
            "com.samsung.android.lool" to "com.samsung.android.sm.ui.battery.BatteryActivity"
        )

        for ((pkg, cls) in candidates) {
            val intent = Intent().apply {
                component = ComponentName(pkg, cls)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (launch(context, intent)) return true
        }

        return openAppDetails(context)
    }

    fun openAppDetails(context: Context): Boolean {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:" + context.packageName)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return launch(context, intent)
    }

    fun openNotificationPolicyAccess(context: Context): Boolean =
        launch(
            context,
            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )

    private fun launch(context: Context, intent: Intent): Boolean =
        runCatching {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        }.getOrDefault(false)
}
