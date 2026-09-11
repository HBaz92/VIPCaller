package com.hassan.dev.vipcaller.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hassan.dev.vipcaller.ui.HintText
import com.hassan.dev.vipcaller.ui.SectionCard
import com.hassan.dev.vipcaller.ui.VipColors

@Composable
fun AboutScreen() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, bottom = 28.dp)
    ) {
        SectionCard("ما هو التطبيق؟", Icons.Default.Info) {
            HintText(
                "VIP Caller يميّز مكالمات الأشخاص المهمين لديك. تضيف جهات الاتصال إلى " +
                    "قائمة VIP، وعند اتصال أحدهم يشغّل التطبيق نغمته الخاصة حتى تعرف " +
                    "من المتصل دون النظر إلى الشاشة."
            )
        }

        SectionCard("كيف يعمل؟", Icons.Default.Call) {
            HintText(
                "• استورد جهات الاتصال المهمة من دفتر هاتفك.\n" +
                    "• التطبيق يراقب المكالمات الواردة في الخلفية.\n" +
                    "• إذا كان المتصل ضمن قائمة VIP والنظام فعّال في هذا الوقت، تُشغَّل نغمته.\n" +
                    "• أولوية النغمة: نغمة جهة الاتصال في النظام، وإلا النغمة الافتراضية.\n" +
                    "• تتوقف النغمة فور الرد أو الرفض أو انتهاء المكالمة."
            )
        }

        SectionCard("التحكم بالتشغيل", Icons.Default.Settings) {
            HintText(
                "المفتاح الرئيسي في الشاشة الأولى يوقف النظام ويشغّله في أي وقت. " +
                    "ومن تبويب الإعدادات تختار بين التشغيل الدائم أو تحديد أيام وساعات معينة. " +
                    "يمكنك أيضًا إيقاف النظام مباشرة من زر \"إيقاف\" في الإشعار الدائم."
            )
        }

        SectionCard("التوافق", Icons.Default.Settings) {
            HintText(
                "Android 7.0 (API 24) وأحدث.\n\n" +
                    "مُهيّأ للعمل على Honor X9d وأجهزة MagicOS عبر خدمة أمامية دائمة، " +
                    "واستثناء البطارية، وإعادة التشغيل التلقائي بعد الإقلاع. " +
                    "راجع قسم \"توافق الجهاز\" في تبويب الإعدادات."
            )
        }

        SectionCard("الخصوصية", Icons.Default.Info) {
            HintText(
                "كل البيانات تبقى على جهازك. لا يرفع التطبيق جهات الاتصال ولا سجل " +
                    "المكالمات ولا أي معلومات شخصية إلى أي خادم.\n\n" +
                    "من تبويب الإعدادات يمكنك قفل التطبيق برمز من 4 خانات أو بالبصمة، " +
                    "فلا يرى قائمة VIP أحد غيرك. الرمز نفسه لا يُحفظ في الجهاز، بل يُحفظ " +
                    "اشتقاق مشفّر منه لا يمكن عكسه."
            )
        }

        SectionCard("عن المطور", Icons.Default.Person) {
            HintText("المطور: Hassan Bazarah\n\nمشروع شخصي مفتوح المصدر بدون روت.")
        }

        OutlinedButton(
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HBaz92"))
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text("فتح GitHub", fontSize = 16.sp, color = VipColors.TextPrimary)
        }
    }
}
