package com.hassan.dev.vipcaller.core

/**
 * مقارنة أرقام الهواتف بعد تجريدها من الرموز ومفاتيح الدول،
 * لأن الرقم الوارد من النظام غالبًا يختلف شكله عن المحفوظ في جهات الاتصال.
 */
object PhoneUtils {

    private const val SIGNIFICANT_DIGITS = 9

    fun normalize(raw: String?): String {
        if (raw.isNullOrBlank()) return ""

        // Character.digit يحوّل الأرقام العربية-الهندية (٠١٢٣) إلى قيمتها، فيطابق ما كُتب بلوحة عربية.
        val digits = buildString(raw.length) {
            for (c in raw) {
                val value = Character.digit(c, 10)
                if (value in 0..9) append('0' + value)
            }
        }

        return if (digits.length > SIGNIFICANT_DIGITS) digits.takeLast(SIGNIFICANT_DIGITS) else digits
    }

    fun matches(a: String?, b: String?): Boolean {
        val na = normalize(a)
        val nb = normalize(b)
        return na.isNotEmpty() && na == nb
    }

    fun pretty(raw: String): String = raw.trim().replace(Regex("\\s+"), " ")

    /** عازل اتجاهي: LEFT-TO-RIGHT ISOLATE ثم POP DIRECTIONAL ISOLATE. */
    private const val LTR_ISOLATE = '⁦'
    private const val POP_ISOLATE = '⁩'

    /**
     * داخل واجهة عربية تعيد خوارزمية الاتجاه الثنائي ترتيب مقاطع الرقم،
     * فيظهر "+966 55 123" كأنه "123 55 966+". العزل يبقي الرقم بترتيبه
     * الأصلي دون أن يغيّر محاذاة السطر في التخطيط المحيط.
     */
    fun forDisplay(raw: String): String =
        if (raw.isBlank()) raw else "$LTR_ISOLATE$raw$POP_ISOLATE"
}
