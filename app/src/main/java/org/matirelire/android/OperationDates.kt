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
    fun display(value: String, locale: Locale): String = parse(value)?.format(
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)) ?: value
}
