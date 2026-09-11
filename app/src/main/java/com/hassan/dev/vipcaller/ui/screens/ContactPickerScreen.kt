package com.hassan.dev.vipcaller.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hassan.dev.vipcaller.core.ContactsRepo
import com.hassan.dev.vipcaller.core.DeviceContact
import com.hassan.dev.vipcaller.core.PhoneUtils
import com.hassan.dev.vipcaller.core.SearchUtils
import com.hassan.dev.vipcaller.core.VipContact
import com.hassan.dev.vipcaller.core.VipStore
import com.hassan.dev.vipcaller.ui.HintText
import com.hassan.dev.vipcaller.ui.InitialAvatar
import com.hassan.dev.vipcaller.ui.VipColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactPickerScreen(
    hasContactsPermission: Boolean,
    onRequestPermission: () -> Unit,
    onClose: () -> Unit,
    onImported: (Int) -> Unit
) {
    val context = LocalContext.current
    val existing by VipStore.contacts.collectAsState()

    var loading by remember { mutableStateOf(true) }
    var all by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    val selected = remember { mutableStateListOf<String>() }

    LaunchedEffect(hasContactsPermission) {
        if (!hasContactsPermission) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        all = withContext(Dispatchers.IO) { ContactsRepo.loadAll(context) }
        loading = false
    }

    val alreadyAdded = remember(existing) {
        existing.map { PhoneUtils.normalize(it.number) }.toSet()
    }

    val visible = remember(all, query) { SearchUtils.filter(all, query) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
    ) {
        TopAppBar(
            title = { Text("استيراد جهات الاتصال", fontSize = 18.sp) },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = VipColors.TextPrimary,
                navigationIconContentColor = VipColors.TextPrimary
            )
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("ابحث بالاسم أو الرقم") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "مسح البحث")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
        )

        if (query.isNotBlank() && visible.isNotEmpty()) {
            Text(
                text = "${visible.size} نتيجة",
                color = VipColors.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp)
            )
        } else {
            Spacer(Modifier.height(16.dp))
        }

        when {
            !hasContactsPermission -> PermissionPrompt(onRequestPermission)

            loading -> Box(
                modifier = Modifier.fillMaxWidth().padding(40.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = VipColors.Accent) }

            all.isEmpty() -> Box(
                modifier = Modifier.fillMaxWidth().padding(30.dp),
                contentAlignment = Alignment.Center
            ) { HintText("لا توجد جهات اتصال بأرقام على هذا الجهاز.") }

            visible.isEmpty() -> Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(30.dp),
                contentAlignment = Alignment.TopCenter
            ) { HintText("لا توجد نتائج لـ \"$query\"") }

            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
            ) {
                items(visible, key = { it.lookupKey + it.number }) { contact ->
                    val key = PhoneUtils.normalize(contact.number)
                    val isAdded = key in alreadyAdded
                    ContactRow(
                        contact = contact,
                        checked = isAdded || key in selected,
                        disabled = isAdded,
                        onToggle = {
                            if (key in selected) selected.remove(key) else selected.add(key)
                        }
                    )
                }
            }
        }

        if (hasContactsPermission && !loading && all.isNotEmpty()) {
            Surface(color = VipColors.Surface, tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "محدد: ${selected.size}",
                        color = VipColors.TextSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        enabled = selected.isNotEmpty(),
                        onClick = {
                            val picked = all.filter { PhoneUtils.normalize(it.number) in selected }
                                .map { device ->
                                    VipContact(
                                        id = UUID.randomUUID().toString(),
                                        name = device.name,
                                        number = device.number,
                                        photoUri = device.photoUri,
                                        ringtoneUri = device.systemRingtone
                                    )
                                }
                            val added = VipStore.addAll(context, picked)
                            selected.clear()
                            onImported(added)
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VipColors.Accent)
                    ) {
                        Text("إضافة إلى VIP")
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactRow(
    contact: DeviceContact,
    checked: Boolean,
    disabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !disabled, onClick = onToggle)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InitialAvatar(
            name = contact.name,
            size = 42,
            tint = if (disabled) VipColors.Success else VipColors.Accent
        )
        Spacer(Modifier.width(12.dp))
        val nameless = contact.name == contact.number

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = PhoneUtils.forDisplay(contact.name),
                color = VipColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            // جهة بلا اسم تعرض رقمها كعنوان، فلا داعي لتكراره تحته.
            if (disabled || !nameless) {
                Text(
                    text = if (disabled) "موجود في القائمة" else PhoneUtils.forDisplay(contact.number),
                    color = if (disabled) VipColors.Success else VipColors.TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
        Checkbox(
            checked = checked,
            enabled = !disabled,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = VipColors.Accent)
        )
    }
}

@Composable
private fun PermissionPrompt(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HintText("يحتاج التطبيق إذن قراءة جهات الاتصال حتى يعرض القائمة هنا.")
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRequestPermission,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VipColors.Accent)
        ) { Text("منح الإذن") }
    }
}
