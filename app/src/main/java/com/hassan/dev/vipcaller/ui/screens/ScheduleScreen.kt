package com.hassan.dev.vipcaller.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.app.NotificationManager
import android.content.Context
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hassan.dev.vipcaller.LockGate
import com.hassan.dev.vipcaller.core.AppLock
import com.hassan.dev.vipcaller.core.BiometricGate
import com.hassan.dev.vipcaller.core.OemCompat
import com.hassan.dev.vipcaller.core.Schedule
import com.hassan.dev.vipcaller.core.ScheduleEvaluator
import com.hassan.dev.vipcaller.core.ScheduleMode
import com.hassan.dev.vipcaller.core.VipStore
import com.hassan.dev.vipcaller.ui.HintText
import com.hassan.dev.vipcaller.ui.SectionCard
import com.hassan.dev.vipcaller.ui.rememberRefreshedOnResume
import com.hassan.dev.vipcaller.ui.VipColors
import java.util.Calendar

private val DAY_LABELS = listOf(
    Calendar.SATURDAY to "السبت",
    Calendar.SUNDAY to "الأحد",
    Calendar.MONDAY to "الإثنين",
    Calendar.TUESDAY to "الثلاثاء",
    Calendar.WEDNESDAY to "الأربعاء",
    Calendar.THURSDAY to "الخميس",
    Calendar.FRIDAY to "الجمعة"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScheduleScreen() {
    val context = LocalContext.current
    val schedule by VipStore.schedule.collectAsState()
    val vibrate by VipStore.vibrate.collectAsState()
    val ignoreSilent by VipStore.ignoreSilent.collectAsState()
    val maxSeconds by VipStore.maxSeconds.collectAsState()
    val silenceRinger by VipStore.silenceSystemRinger.collectAsState()
    val dndGranted by rememberRefreshedOnResume { hasDndAccess(context) }

    var editingStart by remember { mutableStateOf(false) }
    var editingEnd by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, bottom = 28.dp)
    ) {
        SectionCard("وقت عمل النظام", Icons.Default.DateRange) {
            ModeOption(
                title = "يعمل دائمًا",
                subtitle = "يراقب المكالمات 24 ساعة طوال الأسبوع",
                selected = schedule.mode == ScheduleMode.ALWAYS,
                onSelect = { VipStore.setSchedule(context, schedule.copy(mode = ScheduleMode.ALWAYS)) }
            )
            Spacer(Modifier.height(8.dp))
            ModeOption(
                title = "أوقات محددة",
                subtitle = "يعمل فقط ضمن الأيام والساعات التي تختارها",
                selected = schedule.mode == ScheduleMode.WINDOW,
                onSelect = { VipStore.setSchedule(context, schedule.copy(mode = ScheduleMode.WINDOW)) }
            )

            if (schedule.mode == ScheduleMode.WINDOW) {
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TimeBox(
                        label = "من",
                        value = ScheduleEvaluator.formatMinutes(schedule.startMinute),
                        modifier = Modifier.weight(1f),
                        onClick = { editingStart = true }
                    )
                    TimeBox(
                        label = "إلى",
                        value = ScheduleEvaluator.formatMinutes(schedule.endMinute),
                        modifier = Modifier.weight(1f),
                        onClick = { editingEnd = true }
                    )
                }

                if (schedule.startMinute > schedule.endMinute) {
                    Spacer(Modifier.height(10.dp))
                    HintText("النافذة تمتد عبر منتصف الليل إلى اليوم التالي.")
                }

                Spacer(Modifier.height(18.dp))
                Text("أيام العمل", color = VipColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DAY_LABELS.forEach { (day, label) ->
                        FilterChip(
                            selected = day in schedule.days,
                            onClick = {
                                val days = schedule.days.toMutableSet()
                                if (!days.add(day)) days.remove(day)
                                VipStore.setSchedule(context, schedule.copy(days = days))
                            },
                            label = { Text(label, fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VipColors.Accent,
                                selectedLabelColor = VipColors.TextPrimary,
                                labelColor = VipColors.TextSecondary
                            )
                        )
                    }
                }

                if (schedule.days.isEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("لم تختر أي يوم — لن يعمل النظام.", color = VipColors.Danger, fontSize = 13.sp)
                }
            }
        }

        SectionCard("سلوك التنبيه", Icons.Default.Notifications) {
            ToggleRow(
                title = "كتم رنين النظام",
                subtitle = "أندرويد يشغّل نغمة جهة الاتصال بنفسه؛ بدون الكتم تُسمع نغمتان معًا",
                checked = silenceRinger,
                onChange = { VipStore.setSilenceSystemRinger(context, it) }
            )

            if (silenceRinger && !dndGranted) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = VipColors.Danger.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "إذن عدم الإزعاج مطلوب",
                            color = VipColors.Danger,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        HintText("بدون هذا الإذن لا يستطيع التطبيق كتم رنين النظام، وستسمع نغمتين.")
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { OemCompat.openNotificationPolicyAccess(context) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VipColors.Accent)
                        ) { Text("منح الإذن", fontSize = 14.sp) }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            ToggleRow(
                title = "الاهتزاز مع النغمة",
                subtitle = "اهتزاز متكرر أثناء رنين جهة VIP",
                checked = vibrate,
                onChange = { VipStore.setVibrate(context, it) }
            )
            Spacer(Modifier.height(12.dp))
            ToggleRow(
                title = "تجاوز الوضع الصامت",
                subtitle = "تشغيل النغمة عبر مسار المنبّه حتى لو كان الجهاز صامتًا",
                checked = ignoreSilent,
                onChange = { VipStore.setIgnoreSilent(context, it) }
            )

            Spacer(Modifier.height(18.dp))
            Text(
                "أقصى مدة للنغمة: $maxSeconds ثانية",
                color = VipColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Slider(
                value = maxSeconds.toFloat(),
                onValueChange = { VipStore.setMaxSeconds(context, it.toInt()) },
                valueRange = 10f..120f,
                steps = 10,
                colors = SliderDefaults.colors(
                    thumbColor = VipColors.Accent,
                    activeTrackColor = VipColors.Accent
                )
            )
            HintText("تتوقف النغمة تلقائيًا عند الرد أو الرفض، وهذا حد أمان إضافي.")
        }

        SecurityCard()

        DeviceCompatibilityCard()
    }

    if (editingStart) {
        TimePickerDialog(
            initialMinutes = schedule.startMinute,
            onDismiss = { editingStart = false },
            onConfirm = {
                VipStore.setSchedule(context, schedule.copy(startMinute = it))
                editingStart = false
            }
        )
    }

    if (editingEnd) {
        TimePickerDialog(
            initialMinutes = schedule.endMinute,
            onDismiss = { editingEnd = false },
            onConfirm = {
                VipStore.setSchedule(context, schedule.copy(endMinute = it))
                editingEnd = false
            }
        )
    }
}

@Composable
private fun SecurityCard() {
    val context = LocalContext.current
    val lockEnabled by AppLock.enabled.collectAsState()
    val biometricOn by AppLock.biometricEnabled.collectAsState()
    val secureScreen by AppLock.secureScreen.collectAsState()
    val biometricAvailable = remember { BiometricGate.isAvailable(context) }

    var setupVisible by remember { mutableStateOf(false) }
    var changing by remember { mutableStateOf(false) }

    SectionCard("قفل التطبيق", Icons.Default.Lock) {
        ToggleRow(
            title = "طلب رمز عند الفتح",
            subtitle = "رمز من ${AppLock.PIN_LENGTH} خانات يمنع من يمسك جهازك من رؤية قائمة VIP",
            checked = lockEnabled,
            onChange = { wanted ->
                if (wanted) {
                    changing = false
                    setupVisible = true
                } else {
                    AppLock.disable(context)
                    LockGate.reset()
                }
            }
        )

        if (lockEnabled) {
            Spacer(Modifier.height(14.dp))
            ToggleRow(
                title = "فتح بالبصمة",
                subtitle = if (biometricAvailable) {
                    "أسرع من إدخال الرمز، والرمز يبقى بديلًا دائمًا"
                } else {
                    "لا توجد بصمة مسجّلة على هذا الجهاز"
                },
                checked = biometricOn && biometricAvailable,
                enabled = biometricAvailable,
                onChange = { AppLock.setBiometricEnabled(context, it) }
            )

            Spacer(Modifier.height(14.dp))
            ToggleRow(
                title = "إخفاء المحتوى من التطبيقات الأخيرة",
                subtitle = "يمنع ظهور الأسماء في معاينة التطبيقات — لكنه يعطّل لقطات الشاشة أيضًا",
                checked = secureScreen,
                onChange = { AppLock.setSecureScreen(context, it) }
            )

            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = { changing = true; setupVisible = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text("تغيير الرمز", color = VipColors.TextPrimary, fontSize = 14.sp)
            }
        }
    }

    if (setupVisible) {
        Dialog(onDismissRequest = { setupVisible = false }) {
            Surface(color = VipColors.Background, shape = RoundedCornerShape(24.dp)) {
                Box(modifier = Modifier.height(620.dp)) {
                    PinSetupScreen(
                        onDone = { pin ->
                            AppLock.setPin(context, pin)
                            // ضبط الرمز لا يجب أن يقفل الشاشة الحالية في وجه المستخدم.
                            LockGate.markUnlocked()
                            setupVisible = false
                        },
                        onCancel = { setupVisible = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceCompatibilityCard() {
    val context = LocalContext.current
    val batteryExempt by rememberRefreshedOnResume { OemCompat.isIgnoringBatteryOptimizations(context) }

    SectionCard("توافق الجهاز", Icons.Default.Build) {
        HintText("الجهاز الحالي: ${OemCompat.deviceLabel}")

        if (OemCompat.isHonorFamily) {
            Spacer(Modifier.height(12.dp))
            Surface(
                color = VipColors.Gold.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "أجهزة Honor / MagicOS",
                        color = VipColors.Gold,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    HintText(
                        "نظام MagicOS يغلق التطبيقات الخلفية تلقائيًا. لكي يعمل VIP Caller " +
                            "على Honor X9d بثبات، فعّل الخيارين أدناه، ثم من إعدادات البطارية " +
                            "اختر VIP Caller واضبطه على \"عدم التقييد\"، وثبّت التطبيق في " +
                            "قائمة التطبيقات الأخيرة بسحبه للأسفل."
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        CompatAction(
            title = "استثناء من تحسين البطارية",
            state = if (batteryExempt) "مفعّل" else "غير مفعّل",
            positive = batteryExempt,
            onClick = { OemCompat.requestIgnoreBatteryOptimizations(context) }
        )
        Spacer(Modifier.height(10.dp))
        CompatAction(
            title = "التشغيل التلقائي / بدء التشغيل",
            state = "افتح الإعداد",
            positive = false,
            onClick = { OemCompat.openAutoStartSettings(context) }
        )
        Spacer(Modifier.height(10.dp))
        CompatAction(
            title = "الوصول إلى وضع عدم الإزعاج",
            state = "افتح الإعداد",
            positive = false,
            onClick = { OemCompat.openNotificationPolicyAccess(context) }
        )
        Spacer(Modifier.height(10.dp))
        CompatAction(
            title = "صلاحيات التطبيق",
            state = "افتح الإعداد",
            positive = false,
            onClick = { OemCompat.openAppDetails(context) }
        )
    }
}

@Composable
private fun CompatAction(title: String, state: String, positive: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(title, color = VipColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(
            state,
            color = if (positive) VipColors.Success else VipColors.AccentSoft,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun ModeOption(title: String, subtitle: String, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        color = if (selected) VipColors.Accent.copy(alpha = 0.15f) else VipColors.SurfaceHigh,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(selectedColor = VipColors.Accent)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = VipColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = VipColors.TextSecondary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = VipColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = VipColors.TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = VipColors.Accent)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeBox(label: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        color = VipColors.SurfaceHigh,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = VipColors.TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, color = VipColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VipColors.Surface,
        title = { Text("اختر الوقت", color = VipColors.TextPrimary, fontSize = 17.sp) },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) {
                Text("حفظ", color = VipColors.AccentSoft)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = VipColors.TextSecondary) }
        }
    )
}

/** إذن الوصول لسياسة الإشعارات — شرط كتم مسار الرنين. */
private fun hasDndAccess(context: Context): Boolean {
    val manager = context.getSystemService(NotificationManager::class.java)
    return manager?.isNotificationPolicyAccessGranted == true
}
