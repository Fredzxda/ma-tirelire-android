package org.matirelire.android

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Dates are civil ISO values; legacy display labels are never guessed. */
internal object OperationDates {
    fun parse(value: String): LocalDate? {
        if (!Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(value)) return null
        return runCatching { LocalDate.parse(value) }.getOrNull()?.takeIf { it.year in 1..9999 }
    }
    /** Read-only compatibility with named-month dates written before 0.3.5.
     * Numeric formats remain ambiguous and are deliberately not interpreted. */
    fun forOrdering(value: String): LocalDate? {
        parse(value)?.let { return it }
        if (!value.any { it.isLetter() }) return null
        return listOf(Locale.FRANCE, Locale.US, Locale.UK).firstNotNullOfOrNull { locale ->
            runCatching {
                val format = java.time.format.DateTimeFormatterBuilder().parseCaseInsensitive()
                    .appendLocalized(FormatStyle.MEDIUM, null)
                    .parseDefaulting(java.time.temporal.ChronoField.ERA, 1)
                    .toFormatter(locale).withResolverStyle(java.time.format.ResolverStyle.STRICT)
                LocalDate.parse(value, format).takeIf { it.year in 1..9999 }
            }.getOrNull()
        }
    }
    fun display(value: String, locale: Locale): String = parse(value)?.format(
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)) ?: value
}
