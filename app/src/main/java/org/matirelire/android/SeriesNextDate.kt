package org.matirelire.android

import java.time.LocalDate

/** Reschedules only the not-yet-generated template; consumed indices never rewind. */
internal fun DemoViewModel.nextSeriesMinimumDate(id: String, today: LocalDate = LocalDate.now()): LocalDate? {
    val rule = rules.firstOrNull { it.id == id } ?: return null
    if (isArchived(rule.account) || rule.target?.let(::isArchived) == true || (rule.count != null && rule.next >= rule.count)) return null
    val last = (transactions + deferredForecasts).filter { it.seriesId == id }.mapNotNull { OperationDates.parse(it.date) }.maxOrNull()
    val minimum = if (last != null) runCatching { maxOf(today, last.plusDays(1)) }.getOrNull() ?: return null else today
    return minimum.takeIf { it.year in 1..9999 && (rule.until == null || it <= requireNotNull(OperationDates.parse(rule.until))) }
}

internal fun DemoViewModel.setSeriesNextDate(id: String, date: LocalDate, today: LocalDate = LocalDate.now()): Boolean {
    val minimum = nextSeriesMinimumDate(id, today) ?: return false
    if (date < minimum || date.year !in 1..9999) return false
    val index = rules.indexOfFirst { it.id == id }
    val rule = rules[index]
    if (rule.dateAt(rule.next) == date) return true // Keep the month-end anchor when the date is unchanged.
    if (rule.until != null && date > requireNotNull(OperationDates.parse(rule.until))) return false
    val last = (transactions + deferredForecasts).filter { it.seriesId == id }.mapNotNull { OperationDates.parse(it.date) }.maxOrNull()
    val revised = rule.copy(anchor = date.toString(), scheduleBase = rule.next, scheduleOffset = 0, generatedThrough = last?.toString())
    val nextRules = rules.toMutableList().also { it[index] = revised }
    val checked = try { SnapshotCodec.encode(exportData().copy(rules = nextRules)) } catch (_: IllegalArgumentException) { return false }
    checked.fill(0)
    // restoreData would close the current dialog and discard UI state: update only the rules.
    rules[index] = revised
    notifySeriesScheduleChanged()
    return true
}
