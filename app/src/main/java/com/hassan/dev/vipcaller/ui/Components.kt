package com.hassan.dev.vipcaller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object VipColors {
    val Background = Color(0xFF0B1220)
    val BackgroundMid = Color(0xFF111C31)
    val Surface = Color(0xFF18243B)
    val SurfaceHigh = Color(0xFF21314D)
    val Accent = Color(0xFF3B82F6)
    val AccentSoft = Color(0xFF60A5FA)
    val Gold = Color(0xFFFBBF24)
    val Danger = Color(0xFFF87171)
    val Success = Color(0xFF34D399)
    val TextPrimary = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFF94A3B8)
}

@Composable
fun SectionCard(
    title: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = VipColors.Surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            if (title != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (icon != null) {
                        Icon(icon, contentDescription = null, tint = VipColors.AccentSoft)
                    }
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = VipColors.TextPrimary
                    )
                }
                Column(modifier = Modifier.padding(top = 14.dp)) { content() }
            } else {
                content()
            }
        }
    }
}

/** صورة رمزية بحرف الاسم الأول — تتجنب الحاجة لمكتبة تحميل صور. */
@Composable
fun InitialAvatar(name: String, size: Int = 46, tint: Color = VipColors.Accent) {
    // الجهات بلا اسم يكون "اسمها" رقمها، فنتخطى "+" والأرقام بحثًا عن أول حرف.
    val letter = name.firstOrNull { it.isLetter() }?.uppercase() ?: "#"
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(tint.copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            color = tint,
            fontSize = (size / 2.4).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun HintText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = VipColors.TextSecondary,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        modifier = modifier
    )
}

/**
 * قيمة تُحسب من حالة النظام وتُحدَّث كلما عاد المستخدم إلى الشاشة.
 * ضرورية لأذونات تُمنح من شاشات الإعدادات خارج التطبيق.
 */
@Composable
fun <T> rememberRefreshedOnResume(compute: () -> T): State<T> {
    val state = remember { mutableStateOf(compute()) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) state.value = compute()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return state
}
