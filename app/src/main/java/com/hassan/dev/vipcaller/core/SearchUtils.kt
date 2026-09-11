package com.hassan.dev.vipcaller.core

/**
 * بحث جهات الاتصال بالعربية والإنجليزية.
 *
 * الأسماء العربية تُكتب بصور مختلفة (أحمد/احمد، فاطمة/فاطمه، ليلى/ليلي)،
 * لذلك نوحّد الحروف قبل المقارنة حتى يجد المستخدم النتيجة مهما كتب.
 */
object SearchUtils {

    private const val TATWEEL = 'ـ'

    /** الحركات والتشكيل: فتحة .. سكون، والألف الخنجرية. */
    private fun isDiacritic(c: Char): Boolean =
        c in 'ً'..'ٟ' || c == 'ٰ' || c in 'ۖ'..'ۭ'

    private fun unifyArabicLetter(c: Char): Char = when (c) {
        'أ', 'إ', 'آ', 'ٱ', 'ا' -> 'ا'
        'ى', 'ئ' -> 'ي'
        'ة' -> 'ه'
        'ؤ' -> 'و'
        else -> c
    }

    /** يحوّل النص إلى صورة موحّدة قابلة للمقارنة: حروف صغيرة، بلا تشكيل، ومسافات مضغوطة. */
    fun normalize(text: String?): String {
        if (text.isNullOrBlank()) return ""
        val sb = StringBuilder(text.length)
        var lastWasSpace = false

        for (raw in text.lowercase()) {
            if (isDiacritic(raw) || raw == TATWEEL) continue

            val c = unifyArabicLetter(raw)
            if (c.isWhitespace()) {
                if (sb.isNotEmpty() && !lastWasSpace) {
                    sb.append(' ')
                    lastWasSpace = true
                }
            } else {
                sb.append(c)
                lastWasSpace = false
            }
        }

        return sb.toString().trim()
    }

    /**
     * ترتيب المطابقة: الأصغر أفضل. يعيد null إذا لم تطابق الجهة البحث إطلاقًا.
     *
     * 0 = الاسم يبدأ بالبحث، 1 = كلمة داخل الاسم تبدأ به،
     * 2 = الاسم يحتويه، 3 = الرقم يطابق.
     */
    fun rank(name: String, number: String, query: String): Int? {
        val q = normalize(query)
        val digits = PhoneUtils.normalize(query)

        // بحث رقمي بحت: نطابق الرقم فقط حتى لا تتسرب أسماء غير مرتبطة.
        if (q.isEmpty()) {
            if (digits.isEmpty()) return 0
            return if (PhoneUtils.normalize(number).contains(digits)) 3 else null
        }

        val target = normalize(name)
        when {
            target.startsWith(q) -> return 0
            target.split(' ').any { it.startsWith(q) } -> return 1
            target.contains(q) -> return 2
        }

        // البحث قد يكون رقمًا كُتب بأرقام عربية أو مع فواصل.
        if (digits.isNotEmpty() && PhoneUtils.normalize(number).contains(digits)) return 3

        return null
    }

    /** يصفّي القائمة ويرتّبها: الأفضل مطابقةً أولًا مع الحفاظ على الترتيب الأبجدي داخل كل مرتبة. */
    fun filter(contacts: List<DeviceContact>, query: String): List<DeviceContact> {
        if (query.isBlank()) return contacts
        return contacts
            .mapNotNull { contact ->
                rank(contact.name, contact.number, query)?.let { contact to it }
            }
            .sortedBy { it.second }
            .map { it.first }
    }
}
