package com.hassan.dev.vipcaller.core

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

/** قراءة دفتر هاتف الجهاز لاستيراد جهات الاتصال إلى قائمة VIP. */
object ContactsRepo {

    /** كل جهات الاتصال التي لديها رقم، بدون تكرار للرقم نفسه، مرتبة بالاسم. */
    fun loadAll(context: Context): List<DeviceContact> {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
            ContactsContract.CommonDataKinds.Phone.CUSTOM_RINGTONE
        )

        val seen = HashSet<String>()
        val result = ArrayList<DeviceContact>()

        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val number = cursor.getString(2) ?: continue
                    val key = PhoneUtils.normalize(number)
                    if (key.isEmpty() || !seen.add(key)) continue

                    val pretty = PhoneUtils.pretty(number)
                    result += DeviceContact(
                        lookupKey = cursor.getString(0) ?: key,
                        // بلا اسم محفوظ نعرض الرقم نفسه، ونطابقه تمامًا حتى لا يتكرر في السطرين.
                        name = cursor.getString(1)?.takeIf { it.isNotBlank() } ?: pretty,
                        number = pretty,
                        photoUri = cursor.getString(3),
                        systemRingtone = cursor.getString(4)?.takeIf { it.isNotBlank() }
                    )
                }
            }
        }

        return result
    }

    /** النغمة المخصصة لجهة الاتصال كما هي معرّفة في النظام، إن وُجدت. */
    fun systemRingtoneFor(context: Context, phone: String): Uri? {
        val lookup = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phone)
        )

        runCatching {
            context.contentResolver.query(
                lookup,
                arrayOf(ContactsContract.PhoneLookup.CUSTOM_RINGTONE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val tone = cursor.getString(0)
                    if (!tone.isNullOrBlank()) return Uri.parse(tone)
                }
            }
        }

        return null
    }

    /** اسم جهة الاتصال في النظام مقابل رقم وارد، للعرض عند الإضافة السريعة. */
    fun displayNameFor(context: Context, phone: String): String? {
        val lookup = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phone)
        )

        runCatching {
            context.contentResolver.query(
                lookup,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) return cursor.getString(0)?.takeIf { it.isNotBlank() }
            }
        }

        return null
    }
}
