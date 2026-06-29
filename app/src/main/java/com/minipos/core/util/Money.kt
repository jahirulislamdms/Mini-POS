package com.minipos.core.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/**
 * Money is stored as Long paisa (1 ৳ = 100 paisa). Never use Float/Double for money.
 * The user always sees and types Taka; the currency symbol/label is editable per shop.
 */
object Money {

    const val DEFAULT_SYMBOL = "৳"

    // Fixed English grouping (app is English only) so output is always "1,800".
    private val grouping = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))

    /** 180000 -> "৳ 1,800" · 180050 -> "৳ 1,800.50" · -10000 -> "-৳ 100". */
    fun format(paisa: Long, symbol: String = DEFAULT_SYMBOL): String {
        val negative = paisa < 0
        val absPaisa = abs(paisa)
        val whole = absPaisa / 100
        val frac = (absPaisa % 100).toInt()
        val number = if (frac == 0) {
            grouping.format(whole)
        } else {
            grouping.format(whole) + "." + frac.toString().padStart(2, '0')
        }
        val sign = if (negative) "-" else ""
        return "$sign$symbol $number"
    }

    /** Parse user-typed Taka ("1800", "1,800.50") into paisa. Returns null if not a valid number. */
    fun parseToPaisa(input: String): Long? {
        val cleaned = input.trim()
            .removePrefix(DEFAULT_SYMBOL)
            .replace(",", "")
            .replace(" ", "")
        if (cleaned.isEmpty()) return null
        val value = cleaned.toBigDecimalOrNull() ?: return null
        return value.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toLong()
    }
}
