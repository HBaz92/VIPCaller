package com.hassan.dev.vipcaller.ui.screens

import android.app.Activity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hassan.dev.vipcaller.core.AppLock
import com.hassan.dev.vipcaller.core.BiometricGate
import com.hassan.dev.vipcaller.ui.HintText
import com.hassan.dev.vipcaller.ui.VipColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** شاشة فتح القفل: رمز من 4 خانات مع بصمة اختيارية. */
@Composable
fun UnlockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activity = context as? Activity

    val biometricEnabled by AppLock.biometricEnabled.collectAsState()
    val biometricUsable = remember { BiometricGate.isAvailable(context) } && biometricEnabled

    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    var lockRemaining by remember { mutableLongStateOf(AppLock.remainingLockMillis(context)) }

    // عدّاد تنازلي للحظر بعد المحاولات الخاطئة.
    LaunchedEffect(lockRemaining > 0) {
        while (lockRemaining > 0) {
            delay(1000)
            lockRemaining = AppLock.remainingLockMillis(context)
        }
    }

    fun submit(candidate: String) {
        if (checking) return
        checking = true
        scope.launch {
            val ok = withContext(Dispatchers.Default) { AppLock.verify(context, candidate) }
            checking = false
            if (ok) {
                onUnlocked()
            } else {
                pin = ""
                lockRemaining = AppLock.remainingLockMillis(context)
                error = if (lockRemaining > 0) null else "رمز غير صحيح"
            }
        }
    }

    fun startBiometric() {
        activity ?: return
        BiometricGate.authenticate(
            activity = activity,
            title = "فتح VIP Caller",
            subtitle = "استخدم بصمتك لعرض قائمة VIP",
            negativeLabel = "استخدم الرمز",
            onSuccess = onUnlocked,
            onFailed = { message -> if (message != null) error = message }
        )
    }

    // البصمة تُعرض تلقائيًا عند فتح الشاشة ما لم يكن هناك حظر.
    LaunchedEffect(biometricUsable) {
        if (biometricUsable && AppLock.remainingLockMillis(context) == 0L) startBiometric()
    }

    LockScaffold(
        title = "VIP Caller مقفل",
        subtitle = if (lockRemaining > 0) {
            "محاولات كثيرة خاطئة — انتظر ${formatCountdown(lockRemaining)}"
        } else {
            "أدخل الرمز لعرض قائمة جهاتك المهمة"
        },
        filled = pin.length,
        error = error != null,
        message = error,
        locked = lockRemaining > 0 || checking,
        biometricLabel = if (biometricUsable && lockRemaining == 0L) "استخدم البصمة" else null,
        onBiometric = ::startBiometric,
        onDigit = { digit ->
            if (pin.length < AppLock.PIN_LENGTH) {
                error = null
                pin += digit
                if (pin.length == AppLock.PIN_LENGTH) submit(pin)
            }
        },
        onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
    )
}

/**
 * ضبط رمز جديد على مرحلتين: إدخال ثم تأكيد.
 * [onDone] يُستدعى بالرمز النهائي بعد تطابق الإدخالين.
 */
@Composable
fun PinSetupScreen(onDone: (String) -> Unit, onCancel: () -> Unit) {
    var first by remember { mutableStateOf<String?>(null) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LockScaffold(
        title = if (first == null) "اختر رمزًا" else "أعد إدخال الرمز",
        subtitle = if (first == null) {
            "رمز من ${AppLock.PIN_LENGTH} خانات يفتح التطبيق"
        } else {
            "للتأكيد فقط"
        },
        filled = pin.length,
        error = error != null,
        message = error,
        locked = false,
        biometricLabel = null,
        onBiometric = {},
        onCancel = onCancel,
        onDigit = { digit ->
            if (pin.length < AppLock.PIN_LENGTH) {
                error = null
                pin += digit

                if (pin.length == AppLock.PIN_LENGTH) {
                    val entered = pin
                    val previous = first
                    pin = ""
                    if (previous == null) {
                        first = entered
                    } else if (previous == entered) {
                        onDone(entered)
                    } else {
                        first = null
                        error = "الرمزان غير متطابقين — ابدأ من جديد"
                    }
                }
            }
        },
        onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
    )
}

// ---------------------------------------------------------------- components

@Composable
private fun LockScaffold(
    title: String,
    subtitle: String,
    filled: Int,
    error: Boolean,
    message: String?,
    locked: Boolean,
    biometricLabel: String?,
    onBiometric: () -> Unit,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            tint = VipColors.AccentSoft,
            modifier = Modifier.size(44.dp)
        )
        Spacer(Modifier.height(18.dp))

        Text(
            text = title,
            color = VipColors.TextPrimary,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        HintText(subtitle, Modifier.fillMaxWidth())

        Spacer(Modifier.height(28.dp))
        PinDots(filled = filled, error = error)

        Spacer(Modifier.height(14.dp))
        Text(
            text = message ?: " ",
            color = VipColors.Danger,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(18.dp))
        Keypad(enabled = !locked, onDigit = onDigit, onBackspace = onBackspace)

        Spacer(Modifier.height(14.dp))

        if (biometricLabel != null) {
            TextButton(onClick = onBiometric) {
                Text(biometricLabel, color = VipColors.AccentSoft, fontSize = 15.sp)
            }
        }

        if (onCancel != null) {
            TextButton(onClick = onCancel) {
                Text("إلغاء", color = VipColors.TextSecondary, fontSize = 15.sp)
            }
        }

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun PinDots(filled: Int, error: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        repeat(AppLock.PIN_LENGTH) { index ->
            val isFilled = index < filled
            val scale by animateFloatAsState(if (isFilled) 1f else 0.7f, label = "dot")
            Box(
                modifier = Modifier
                    .size((17 * scale).dp)
                    .background(
                        when {
                            error -> VipColors.Danger
                            isFilled -> VipColors.Accent
                            else -> VipColors.SurfaceHigh
                        },
                        CircleShape
                    )
            )
        }
    }
}

@Composable
private fun Keypad(enabled: Boolean, onDigit: (Char) -> Unit, onBackspace: () -> Unit) {
    // الأرقام تُقرأ من اليسار لليمين حتى داخل واجهة عربية، كما في لوحة الاتصال.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("123", "456", "789").forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { digit ->
                        KeyButton(label = digit.toString(), enabled = enabled) { onDigit(digit) }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Spacer(Modifier.size(72.dp))
                KeyButton(label = "0", enabled = enabled) { onDigit('0') }
                KeyButton(label = null, enabled = enabled, onClick = onBackspace)
            }
        }
    }
}

@Composable
private fun KeyButton(label: String?, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (label == null) Color.Transparent else VipColors.SurfaceHigh,
        modifier = Modifier.size(72.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (label == null) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = "مسح",
                    tint = if (enabled) VipColors.TextSecondary else VipColors.Surface
                )
            } else {
                Text(
                    text = label,
                    color = if (enabled) VipColors.TextPrimary else VipColors.TextSecondary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun formatCountdown(millis: Long): String {
    val totalSeconds = (millis / 1000).toInt().coerceAtLeast(1)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "$minutes:${seconds.toString().padStart(2, '0')} دقيقة" else "$seconds ثانية"
}
