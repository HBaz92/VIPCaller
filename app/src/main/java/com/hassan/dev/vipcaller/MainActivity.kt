package com.hassan.dev.vipcaller
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_CALL_LOG
            ),
            1
        )

        setContent {
            VIPCallerUI()
        }
    }
}

@Composable
fun VIPCallerUI() {
    var selectedTab by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B),
                        Color(0xFF020617)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(22.dp)
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "VIP Caller",
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Smart Priority Caller Experience",
                fontSize = 16.sp,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(24.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E293B),
                contentColor = Color.White
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("العربية") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("English") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (selectedTab == 0) {
                ArabicContent()
            } else {
                EnglishContent()
            }
        }
    }
}

@Composable
fun ArabicContent() {
    val context = LocalContext.current

    ModernCard(
        "ما هو التطبيق؟",
        """
        VIP Caller تطبيق ذكي يساعدك على تمييز المكالمات المهمة.
        
        عند ورود اتصال من جهة اتصال لديها نغمة مخصصة داخل النظام، يقوم التطبيق بتشغيل نفس النغمة تلقائيًا حتى تعرف أن المتصل من الأشخاص المهمين لديك.
        """.trimIndent(),
        Icons.Default.Info,
        TextAlign.Right
    )
    ModernCard(
        "طريقة التفعيل",
        """
    بعد تثبيت التطبيق، لا تحتاج إلى أي إعدادات إضافية.
    
    فقط افتح التطبيق أول مرة واسمح بالصلاحيات المطلوبة عند ظهورها.
    
    بعد ذلك يعمل VIP Caller تلقائيًا في الخلفية، ويستخدم النغمات المخصصة الموجودة مسبقًا في جهات الاتصال.
    """.trimIndent(),
        Icons.Default.Settings,
        TextAlign.Right
    )

    ModernCard(
        "كيف يعمل؟",
        """
        • تضع نغمة مخصصة لجهة الاتصال من تطبيق جهات الاتصال.
        • التطبيق يراقب حالة المكالمات الواردة.
        • عند اتصال الشخص، يقرأ التطبيق نغمته المخصصة من النظام.
        • عند الرد أو رفض المكالمة أو انتهائها، تتوقف النغمة مباشرة.
        """.trimIndent(),
        Icons.Default.Call,
        TextAlign.Right
    )

    ModernCard(
        "التوافق",
        """
        يعمل على:
        
        Android 7.0 Nougat وأحدث
        API 24+
        
        تم اختباره مبدئيًا على أجهزة Samsung / One UI.
        قد تختلف النتيجة حسب إعدادات البطارية والصوت في كل جهاز.
        """.trimIndent(),
        Icons.Default.Settings,
        TextAlign.Right
    )

    ModernCard(
        "عن المطور",
        """
        المطور: Hassan Bazarah
        
        مشروع شخصي مفتوح المصدر لتجربة تخصيص تنبيهات المكالمات المهمة بدون روت.
        
        GitHub:
        github.com/HBaz92
        """.trimIndent(),
        Icons.Default.Info,
        TextAlign.Right
    )

    ActionButtons(context)
}

@Composable
fun EnglishContent() {
    val context = LocalContext.current

    ModernCard(
        "About the App",
        """
        VIP Caller is a smart utility app designed to help you identify important incoming calls.
        
        When a contact with a custom Android ringtone calls you, the app plays that same ringtone automatically.
        """.trimIndent(),
        Icons.Default.Info,
        TextAlign.Left
    )
    ModernCard(
        "Activation",
        """
    After installing the app, no extra setup is required.
    
    Just open the app once and allow the required permissions when prompted.
    
    VIP Caller will then work automatically in the background using the custom ringtones already configured in your contacts.
    """.trimIndent(),
        Icons.Default.Settings,
        TextAlign.Left
    )
    ModernCard(
        "How It Works",
        """
        • Set a custom ringtone for a contact in Android Contacts.
        • VIP Caller detects incoming call state.
        • The app reads the contact’s custom ringtone from the system.
        • The sound stops when you answer, reject, or end the call.
        """.trimIndent(),
        Icons.Default.Call,
        TextAlign.Left
    )

    ModernCard(
        "Compatibility",
        """
        Supported:
        
        Android 7.0 Nougat and newer
        API 24+
        
        Initially tested on Samsung / One UI devices.
        Behavior may vary depending on battery and sound restrictions.
        """.trimIndent(),
        Icons.Default.Settings,
        TextAlign.Left
    )

    ModernCard(
        "Developer",
        """
        Developer: Hassan Bazarah
        
        An open-source personal project for experimenting with smart VIP call ringtone behavior without root access.
        
        GitHub:
        github.com/HBaz92
        """.trimIndent(),
        Icons.Default.Info,
        TextAlign.Left
    )

    ActionButtons(context)
}

@Composable
fun ActionButtons(context: android.content.Context) {
    Button(
        onClick = {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF2563EB)
        )
    ) {
        Text("Enable DND Access", fontSize = 17.sp)
    }

    Spacer(modifier = Modifier.height(14.dp))

    OutlinedButton(
        onClick = {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HBaz92"))
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text("Open GitHub", fontSize = 17.sp, color = Color.White)
    }
}

@Composable
fun ModernCard(
    title: String,
    body: String,
    icon: ImageVector,
    textAlign: TextAlign
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFF1E293B)
        )
    ) {
        Column(
            modifier = Modifier.padding(22.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF60A5FA)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = body,
                color = Color(0xFFCBD5E1),
                fontSize = 16.sp,
                lineHeight = 26.sp,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}