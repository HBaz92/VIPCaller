package com.hassan.dev.vipcaller

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.view.WindowManager
import com.hassan.dev.vipcaller.core.AppLock
import com.hassan.dev.vipcaller.core.VipStore
import com.hassan.dev.vipcaller.ui.VipColors
import com.hassan.dev.vipcaller.ui.rememberRefreshedOnResume
import com.hassan.dev.vipcaller.ui.screens.AboutScreen
import com.hassan.dev.vipcaller.ui.screens.ContactPickerScreen
import com.hassan.dev.vipcaller.ui.screens.ScheduleScreen
import com.hassan.dev.vipcaller.ui.screens.UnlockScreen
import com.hassan.dev.vipcaller.ui.screens.VipListScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // targetSdk 36 يفرض edge-to-edge على أندرويد 15، فنعلنه صراحة ونتولى الحواف بأنفسنا.
        enableEdgeToEdge()
        VipStore.init(this)
        AppLock.init(this)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = VipColors.Accent)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    LockedApp()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // يوائم حالة الخدمة مع المفتاح الرئيسي بعد أي تغيير خارجي (مثل زر الإيقاف في الإشعار).
        VipService.sync(this)
        applySecureScreen()
    }

    override fun onStop() {
        super.onStop()
        LockGate.onLeftApp()
    }

    /** إخفاء المحتوى من معاينة التطبيقات الأخيرة، حين يطلبه المستخدم. */
    private fun applySecureScreen() {
        if (AppLock.secureScreen.value) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

/**
 * يقرر متى يُطلب الرمز. القفل يعود بعد [GRACE_MILLIS] من مغادرة التطبيق،
 * حتى لا يُطلب الرمز عند العودة السريعة من شاشة أذونات أو إعدادات النظام.
 */
object LockGate {

    private const val GRACE_MILLIS = 30_000L

    private var leftAt = 0L
    private var unlocked = false

    fun onLeftApp() {
        leftAt = System.currentTimeMillis()
    }

    fun markUnlocked() {
        unlocked = true
    }

    /** يُستدعى عند بدء كل تكوين للواجهة لتحديد ما إذا كان القفل مطلوبًا الآن. */
    fun isLockRequired(): Boolean {
        if (!AppLock.enabled.value) return false
        if (!unlocked) return true

        val away = System.currentTimeMillis() - leftAt
        if (leftAt != 0L && away > GRACE_MILLIS) {
            unlocked = false
            return true
        }
        return false
    }

    /** يُستدعى عند إلغاء القفل من الإعدادات. */
    fun reset() {
        unlocked = false
        leftAt = 0L
    }
}

private enum class AppTab(val label: String, val icon: ImageVector) {
    Vip("قائمة VIP", Icons.Default.Star),
    Schedule("الإعدادات", Icons.Default.Settings),
    About("حول", Icons.Default.Info)
}

@Composable
private fun LockedApp() {
    val lockEnabled by AppLock.enabled.collectAsState()
    var locked by remember { mutableStateOf(LockGate.isLockRequired()) }

    // العودة من الخلفية قد تستوجب القفل من جديد.
    val shouldLock by rememberRefreshedOnResume { LockGate.isLockRequired() }
    LaunchedEffect(shouldLock) { if (shouldLock) locked = true }
    LaunchedEffect(lockEnabled) { if (!lockEnabled) locked = false }

    if (locked && lockEnabled) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(VipColors.Background, VipColors.BackgroundMid, VipColors.Background)
                    )
                )
        ) {
            UnlockScreen(
                onUnlocked = {
                    LockGate.markUnlocked()
                    locked = false
                }
            )
        }
    } else {
        LaunchedEffect(Unit) { LockGate.markUnlocked() }
        VipCallerApp()
    }
}

@Composable
private fun VipCallerApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    var tab by remember { mutableStateOf(AppTab.Vip) }
    var showPicker by remember { mutableStateOf(false) }
    var contactsGranted by remember { mutableStateOf(hasPermission(context, Manifest.permission.READ_CONTACTS)) }

    val enabled by VipStore.enabled.collectAsState()

    // تغيير المفتاح الرئيسي يشغّل الخدمة الأمامية أو يوقفها فورًا.
    LaunchedEffect(enabled) { VipService.sync(context) }

    val corePermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        contactsGranted = result[Manifest.permission.READ_CONTACTS] ?: contactsGranted
        val phoneDenied = result[Manifest.permission.READ_PHONE_STATE] == false
        if (phoneDenied) {
            scope.launch {
                snackbarHost.showSnackbar("بدون إذن حالة الهاتف لن يتمكن التطبيق من رصد المكالمات")
            }
        }
    }

    val contactsPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> contactsGranted = granted }

    LaunchedEffect(Unit) {
        val needed = buildList {
            add(Manifest.permission.READ_PHONE_STATE)
            add(Manifest.permission.READ_CONTACTS)
            add(Manifest.permission.READ_CALL_LOG)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.filterNot { hasPermission(context, it) }

        if (needed.isNotEmpty()) corePermissions.launch(needed.toTypedArray())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(VipColors.Background, VipColors.BackgroundMid, VipColors.Background)
                )
            )
    ) {
        if (showPicker) {
            // شاشة الاستيراد تملأ الواجهة بالكامل حتى تتسع قائمة جهات الاتصال.
            ContactPickerScreen(
                hasContactsPermission = contactsGranted,
                onRequestPermission = { contactsPermission.launch(Manifest.permission.READ_CONTACTS) },
                onClose = { showPicker = false },
                onImported = { added ->
                    showPicker = false
                    scope.launch {
                        snackbarHost.showSnackbar(
                            if (added > 0) "تمت إضافة $added جهة إلى قائمة VIP"
                            else "كل الجهات المختارة موجودة مسبقًا"
                        )
                    }
                }
            )
        } else {
            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(snackbarHost) },
                topBar = { Header() },
                bottomBar = { BottomBar(tab) { tab = it } }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    when (tab) {
                        AppTab.Vip -> VipListScreen(
                            onImportRequested = {
                                if (!contactsGranted) {
                                    contactsPermission.launch(Manifest.permission.READ_CONTACTS)
                                }
                                showPicker = true
                            },
                            onOpenSchedule = { tab = AppTab.Schedule }
                        )

                        AppTab.Schedule -> ScheduleScreen()
                        AppTab.About -> AboutScreen()
                    }
                }
            }
        }

        // Scaffold يعرض الـ snackbar في التبويبات؛ هنا نغطي حالة شاشة الاستيراد.
        if (showPicker) {
            SnackbarHost(snackbarHost, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun Header() {
    Column(
        modifier = Modifier
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 10.dp)
    ) {
        Text(
            text = "VIP Caller",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = VipColors.TextPrimary
        )
        Text(
            text = "نغمة خاصة للمتصلين المهمين",
            fontSize = 14.sp,
            color = VipColors.TextSecondary
        )
    }
}

@Composable
private fun BottomBar(current: AppTab, onSelect: (AppTab) -> Unit) {
    NavigationBar(containerColor = VipColors.Surface) {
        AppTab.entries.forEach { entry ->
            NavigationBarItem(
                selected = current == entry,
                onClick = { onSelect(entry) },
                icon = { Icon(entry.icon, contentDescription = entry.label) },
                label = { Text(entry.label, fontSize = 12.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = VipColors.Accent,
                    selectedTextColor = VipColors.Accent,
                    unselectedIconColor = VipColors.TextSecondary,
                    unselectedTextColor = VipColors.TextSecondary,
                    indicatorColor = VipColors.SurfaceHigh
                )
            )
        }
    }
}

private fun hasPermission(context: android.content.Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
