package com.hassan.dev.vipcaller.core

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * قفل التطبيق برمز من 4 خانات.
 *
 * الرمز لا يُخزَّن إطلاقًا؛ نحفظ اشتقاقًا منه بـ PBKDF2 مع ملح عشوائي.
 * رمز من 4 خانات له 10000 احتمال فقط، فالتكرار العالي هو ما يجعل
 * تخمينه خارج التطبيق مكلفًا، وتقييد المحاولات هو ما يمنع التخمين داخله.
 */
object AppLock {

    private const val PREFS = "vip_caller_lock"
    private const val KEY_SALT = "salt"
    private const val KEY_HASH = "hash"
    private const val KEY_BIOMETRIC = "biometric_enabled"
    private const val KEY_SECURE_SCREEN = "secure_screen"
    private const val KEY_FAILED = "failed_attempts"
    private const val KEY_LOCKED_UNTIL = "locked_until"

    const val PIN_LENGTH = 4

    private const val ITERATIONS = 150_000
    private const val KEY_BITS = 256

    /** أول تأخير يبدأ بعد هذا العدد من المحاولات الخاطئة. */
    private const val FREE_ATTEMPTS = 4
    private const val FIRST_DELAY_MS = 30_000L
    private const val MAX_DELAY_MS = 5 * 60_000L

    /** 30 ثانية مضروبة في 2^4 تتجاوز السقف، فلا حاجة لإزاحة أكبر. */
    private const val MAX_SHIFT = 4

    private lateinit var prefs: SharedPreferences

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled

    private val _biometricEnabled = MutableStateFlow(true)
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled

    private val _secureScreen = MutableStateFlow(false)
    val secureScreen: StateFlow<Boolean> = _secureScreen

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _enabled.value = prefs.getString(KEY_HASH, null) != null
        _biometricEnabled.value = prefs.getBoolean(KEY_BIOMETRIC, true)
        _secureScreen.value = prefs.getBoolean(KEY_SECURE_SCREEN, false)
    }

    private fun ensure(context: Context) = init(context)

    // -------------------------------------------------------------- pin setup

    fun isValidPin(pin: String): Boolean =
        pin.length == PIN_LENGTH && pin.all { it.isDigit() }

    /** يضبط رمزًا جديدًا ويفعّل القفل. يعيد false إذا كان الرمز غير صالح. */
    fun setPin(context: Context, pin: String): Boolean {
        ensure(context)
        if (!isValidPin(pin)) return false

        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, derive(pin, salt))
            .putInt(KEY_FAILED, 0)
            .putLong(KEY_LOCKED_UNTIL, 0L)
            .apply()

        _enabled.value = true
        return true
    }

    fun disable(context: Context) {
        ensure(context)
        prefs.edit()
            .remove(KEY_SALT)
            .remove(KEY_HASH)
            .putInt(KEY_FAILED, 0)
            .putLong(KEY_LOCKED_UNTIL, 0L)
            .apply()
        _enabled.value = false
    }

    // ---------------------------------------------------------- verification

    /**
     * يتحقق من الرمز. العملية بطيئة عمدًا (PBKDF2) فاستدعها خارج الخيط الرئيسي.
     * تُحدِّث عدّاد المحاولات تلقائيًا.
     */
    fun verify(context: Context, pin: String): Boolean {
        ensure(context)
        val storedHash = prefs.getString(KEY_HASH, null) ?: return false
        val storedSalt = prefs.getString(KEY_SALT, null) ?: return false
        val salt = runCatching { Base64.decode(storedSalt, Base64.NO_WRAP) }.getOrNull() ?: return false

        val candidate = derive(pin, salt)

        // مقارنة ثابتة الزمن حتى لا يسرّب الفارق الزمني معلومات عن الرمز.
        val matches = MessageDigest.isEqual(
            candidate.toByteArray(Charsets.UTF_8),
            storedHash.toByteArray(Charsets.UTF_8)
        )

        if (matches) resetAttempts() else registerFailure()
        return matches
    }

    private fun derive(pin: String, salt: ByteArray): String {
        // PBKDF2WithHmacSHA1 متاح على كل إصدارات أندرويد المدعومة هنا، بخلاف نسخة SHA-256.
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val bytes = factory.generateSecret(spec).encoded
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    // ------------------------------------------------------------- throttling

    /**
     * مدة الحظر بعد [failed] محاولة خاطئة. أول [FREE_ATTEMPTS] محاولات بلا تأخير،
     * ثم يتضاعف التأخير من 30 ثانية حتى سقف 5 دقائق.
     */
    internal fun lockoutMillisFor(failed: Int): Long {
        if (failed <= FREE_ATTEMPTS) return 0L
        // الإزاحة تُقيَّد قبل التنفيذ: إزاحة كبيرة تفيض إلى قيمة سالبة تتجاوز السقف.
        val step = (failed - FREE_ATTEMPTS - 1).coerceIn(0, MAX_SHIFT)
        return (FIRST_DELAY_MS shl step).coerceAtMost(MAX_DELAY_MS)
    }

    private fun registerFailure() {
        val failed = prefs.getInt(KEY_FAILED, 0) + 1
        val editor = prefs.edit().putInt(KEY_FAILED, failed)

        val delay = lockoutMillisFor(failed)
        if (delay > 0L) editor.putLong(KEY_LOCKED_UNTIL, System.currentTimeMillis() + delay)

        editor.apply()
    }

    private fun resetAttempts() {
        prefs.edit().putInt(KEY_FAILED, 0).putLong(KEY_LOCKED_UNTIL, 0L).apply()
    }

    fun failedAttempts(context: Context): Int {
        ensure(context)
        return prefs.getInt(KEY_FAILED, 0)
    }

    /** المدة المتبقية للحظر بالملي ثانية، أو صفر إذا كان الإدخال مسموحًا. */
    fun remainingLockMillis(context: Context): Long {
        ensure(context)
        val until = prefs.getLong(KEY_LOCKED_UNTIL, 0L)
        if (until == 0L) return 0L

        val remaining = until - System.currentTimeMillis()
        return when {
            remaining <= 0L -> 0L
            // ساعة الجهاز قد تُقدَّم للأمام؛ لا نسمح بحظر أطول من الحد الأقصى.
            remaining > MAX_DELAY_MS -> MAX_DELAY_MS
            else -> remaining
        }
    }

    // --------------------------------------------------------------- settings

    fun setBiometricEnabled(context: Context, value: Boolean) {
        ensure(context)
        prefs.edit().putBoolean(KEY_BIOMETRIC, value).apply()
        _biometricEnabled.value = value
    }

    fun setSecureScreen(context: Context, value: Boolean) {
        ensure(context)
        prefs.edit().putBoolean(KEY_SECURE_SCREEN, value).apply()
        _secureScreen.value = value
    }
}
