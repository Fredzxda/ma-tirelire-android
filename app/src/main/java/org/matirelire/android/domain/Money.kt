package org.matirelire.android.domain

import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Strict demo entry: no grouping, exponent, sign or silent rounding. */
object Money {
    fun parseOpening(text: String, locale: Locale, digits: Int = 2): BigDecimal? {
        if (digits !in 0..3) return null
        val separator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
        if (digits == 0 && (text.contains('.') || text.contains(','))) return null
        val input = text.trim()
        if (!Regex("-?[0-9]{1,12}(" + Regex.escape(separator.toString()) + "[0-9]{1," + digits.coerceAtLeast(1) + "})?").matches(input)) return null
        return input.replace(separator, '.').toBigDecimalOrNull()?.setScale(digits)
    }

    fun parseAmount(text: String, locale: Locale, digits: Int = 2): BigDecimal? {
        if (digits !in 0..3) return null
        val separator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
        val pattern = Regex("[0-9]{1,12}(" + Regex.escape(separator.toString()) + "[0-9]{1," + digits.coerceAtLeast(1) + "})?")
        if (digits == 0 && (text.contains('.') || text.contains(','))) return null
        val input = text.trim()
        if (!pattern.matches(input)) return null
        val amount = input.replace(separator, '.').toBigDecimalOrNull() ?: return null
        return amount.takeIf { it > BigDecimal.ZERO }?.setScale(digits)
    }
}
