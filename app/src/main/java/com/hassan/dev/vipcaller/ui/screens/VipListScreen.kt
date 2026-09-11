package com.hassan.dev.vipcaller.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hassan.dev.vipcaller.core.PhoneUtils
import com.hassan.dev.vipcaller.core.ScheduleEvaluator
import com.hassan.dev.vipcaller.core.ScheduleMode
import com.hassan.dev.vipcaller.core.VipContact
import com.hassan.dev.vipcaller.core.VipStore
import com.hassan.dev.vipcaller.ui.HintText
import com.hassan.dev.vipcaller.ui.InitialAvatar
import com.hassan.dev.vipcaller.ui.SectionCard
import com.hassan.dev.vipcaller.ui.VipColors

@Composable
fun VipListScreen(
    onImportRequested: () -> Unit,
    onOpenSchedule: () -> Unit
) {
    val context = LocalContext.current
    val contacts by VipStore.contacts.collectAsState()
    val enabled by VipStore.enabled.collectAsState()
    val schedule by VipStore.schedule.collectAsState()

    var pendingDelete by remember { mutableStateOf<VipContact?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp)
    ) {
        item {
            MasterSwitchCard(
                enabled = enabled,
                statusLine = when {
                    !enabled -> "النظام متوقف — لن تُشغَّل أي نغمة"
                    schedule.mode == ScheduleMode.ALWAYS -> "يعمل دائمًا، 24 ساعة"
                    else -> "يعمل من ${ScheduleEvaluator.formatMinutes(schedule.startMinute)}" +
                        " إلى ${ScheduleEvaluator.formatMinutes(schedule.endMinute)}"
                },
                activeNow = VipStore.isActiveNow(context),
                onToggle = { VipStore.setEnabled(context, it) },
                onOpenSchedule = onOpenSchedule
            )
        }

        item {
            Button(
                onClick = onImportRequested,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(bottom = 2.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VipColors.Accent)
            ) {
                Icon(Icons.Default.Person, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("استيراد من جهات الاتصال", fontSize = 16.sp)
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "قائمة VIP",
                    color = VipColors.TextPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                if (contacts.isNotEmpty()) {
                    Text(
                        text = "${contacts.count { it.enabled }} / ${contacts.size}",
                        color = VipColors.TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }

        if (contacts.isEmpty()) {
            item { EmptyState(onImportRequested) }
        } else {
            items(contacts, key = { it.id }) { contact ->
                VipRow(
                    contact = contact,
                    onToggle = { VipStore.updateContact(context, contact.copy(enabled = it)) },
                    onDelete = { pendingDelete = contact }
                )
            }

            item {
                TextButton(
                    onClick = { pendingDelete = null; VipStore.clearContacts(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("مسح القائمة بالكامل", color = VipColors.Danger, fontSize = 14.sp)
                }
            }
        }
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = VipColors.Surface,
            title = { Text("حذف من قائمة VIP", color = VipColors.TextPrimary) },
            text = { HintText("سيتم حذف ${target.name} من القائمة. لن يتأثر دفتر جهات الاتصال في الجهاز.") },
            confirmButton = {
                TextButton(onClick = {
                    VipStore.removeContact(context, target.id)
                    pendingDelete = null
                }) { Text("حذف", color = VipColors.Danger) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("إلغاء", color = VipColors.TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun MasterSwitchCard(
    enabled: Boolean,
    statusLine: String,
    activeNow: Boolean,
    onToggle: (Boolean) -> Unit,
    onOpenSchedule: () -> Unit
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (enabled) "النظام مُفعّل" else "النظام متوقف",
                    color = if (enabled) VipColors.Success else VipColors.Danger,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                HintText(statusLine)
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = VipColors.Success
                )
            )
        }

        if (enabled) {
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (activeNow) VipColors.Success.copy(alpha = 0.12f)
                        else VipColors.Gold.copy(alpha = 0.12f),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable(onClick = onOpenSchedule)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (activeNow) "فعّال الآن" else "خارج وقت العمل حاليًا",
                    color = if (activeNow) VipColors.Success else VipColors.Gold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text("تعديل الأوقات", color = VipColors.AccentSoft, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun VipRow(
    contact: VipContact,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = VipColors.Surface)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InitialAvatar(
                name = contact.name,
                tint = if (contact.enabled) VipColors.Gold else VipColors.TextSecondary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = PhoneUtils.forDisplay(contact.name),
                    color = VipColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = PhoneUtils.forDisplay(contact.number),
                    color = VipColors.TextSecondary,
                    fontSize = 13.sp
                )
            }
            Switch(
                checked = contact.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = VipColors.Accent
                )
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = VipColors.Danger)
            }
        }
    }
}

@Composable
private fun EmptyState(onImportRequested: () -> Unit) {
    SectionCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = VipColors.Gold,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "لا توجد جهات VIP بعد",
                color = VipColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            HintText("استورد جهات الاتصال المهمة من دفتر الهاتف، أو أضِف رقمًا يدويًا.")
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onImportRequested,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VipColors.Accent)
            ) {
                Text("استيراد من جهات الاتصال")
            }
        }
    }
}
