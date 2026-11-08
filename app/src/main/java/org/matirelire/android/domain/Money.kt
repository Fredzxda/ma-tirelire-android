package org.matirelire.android.domain

import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Strict demo entry: no grouping, exponent, sign or silent rounding. */
object Money {
    fun parseAmount(text: String, locale: Locale): BigDecimal? {
        val separator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
        val pattern = Regex("[0-9]{1,12}(" + Regex.escape(separator.toString()) + "[0-9]{1,2})?")
        val input = text.trim()
        if (!pattern.matches(input)) return null
        val amount = input.replace(separator, '.').toBigDecimalOrNull() ?: return null
        return amount.takeIf { it > BigDecimal.ZERO }?.setScale(2)
    }
}
