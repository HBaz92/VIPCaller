package com.hassan.dev.vipcaller.core

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

/**
 * بصمة/وجه عبر واجهة النظام مباشرة بدل مكتبة androidx.biometric،
 * لتجنّب اعتمادية إضافية. متاحة من أندرويد 9 (API 28)؛ ما دونه يعتمد الرمز فقط.
 */
object BiometricGate {

    /**
     * رمز خطأ "ضغط الزر السلبي". معرّف في BiometricConstants لكنه مخفي عن
     * واجهة النظام العامة، فنثبّت قيمته هنا بدل جرّ مكتبة androidx.biometric.
     */
    private const val ERROR_NEGATIVE_BUTTON = 13

    /** هل يملك الجهاز بصمة مسجّلة وجاهزة للاستخدام الآن؟ */
    fun isAvailable(context: Context): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
            val manager = context.getSystemService(BiometricManager::class.java)
            manager?.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS
        }

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
            val manager = context.getSystemService(BiometricManager::class.java)
            @Suppress("DEPRECATION")
            manager?.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
        }

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ->
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)

        else -> false
    }

    /**
     * يعرض نافذة البصمة. [onFailed] يُستدعى عند خطأ لا يمكن التعافي منه
     * أو إلغاء المستخدم، ليعود إلى إدخال الرمز.
     */
    fun authenticate(
        activity: Activity,
        title: String,
        subtitle: String,
        negativeLabel: String,
        onSuccess: () -> Unit,
        onFailed: (message: String?) -> Unit
    ): CancellationSignal? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            onFailed(null)
            return null
        }
        return promptFrom(activity, title, subtitle, negativeLabel, onSuccess, onFailed)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun promptFrom(
        activity: Activity,
        title: String,
        subtitle: String,
        negativeLabel: String,
        onSuccess: () -> Unit,
        onFailed: (message: String?) -> Unit
    ): CancellationSignal {
        val executor = ContextCompat.getMainExecutor(activity)
        val cancellation = CancellationSignal()

        val prompt = BiometricPrompt.Builder(activity)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButton(negativeLabel, executor) { _, _ -> onFailed(null) }
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setConfirmationRequired(false)
                }
            }
            .build()

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(code: Int, message: CharSequence?) {
                // الإلغاء اليدوي أو من النظام ليس خطأ يستحق رسالة.
                val silent = code == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED ||
                    code == BiometricPrompt.BIOMETRIC_ERROR_CANCELED ||
                    code == ERROR_NEGATIVE_BUTTON
                onFailed(if (silent) null else message?.toString())
            }
        }

        runCatching { prompt.authenticate(cancellation, executor, callback) }
            .onFailure { onFailed(null) }

        return cancellation
    }
}
