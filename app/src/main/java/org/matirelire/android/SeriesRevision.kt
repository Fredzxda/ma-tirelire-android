package org.matirelire.android

import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal enum class SeriesEditScope { CURRENT, SERIES }

internal object SeriesRevision {
    /** A schedule-only edit never copies historical operation fields into the template. */
    fun updateSettings(rule: RecurringRule, draft: EntryDraft, row: DemoTransaction,
        rows: List<DemoTransaction>, today: LocalDate = LocalDate.now()): RecurringRule? {
        if (draft.repeatFrequency == RepeatFrequency.NONE) return rule.copy(active = false)
        if (draft.repeatFrequency != rule.frequency) {
            val revised = update(rule, draft, row, rows, today) ?: return null
            return revised.copy(account = rule.account, target = rule.target, amount = rule.amount,
                description = rule.description, category = rule.category, payment = rule.payment,
                targetAmount = rule.targetAmount, originalCurrency = rule.originalCurrency, originalAmount = rule.originalAmount, fxRate = rule.fxRate)
        }
        val until = draft.repeatUntil.takeIf { it.isNotBlank() }?.let(OperationDates::parse)
        val remaining = draft.repeatCount.takeIf { it.isNotBlank() }?.toIntOrNull()
        if ((draft.repeatUntil.isNotBlank() && (until == null || until < requireNotNull(OperationDates.parse(rule.anchor)))) ||
            (draft.repeatCount.isNotBlank() && (remaining == null || remaining !in 0..10_000)) ||
            (draft.repeatUntil.isNotBlank() && draft.repeatCount.isNotBlank())) return null
        val exhausted = rule.copy(active = true).nextDate() == null
        return rule.copy(until = until?.toString(), count = remaining?.let { rule.next + it },
            active = (rule.active || exhausted) && remaining != 0)
    }

    /** Rebase only the not-yet-generated schedule; global occurrence numbers never rewind. */
    fun update(rule: RecurringRule, draft: EntryDraft, row: DemoTransaction, rows: List<DemoTransaction>,
        today: LocalDate): RecurringRule? {
        if (draft.repeatFrequency == RepeatFrequency.NONE) return rule.copy(active = false)
        val editedDate = OperationDates.parse(row.date) ?: return null
        val original = rows.firstOrNull { it.id == row.id }
        val calendarChanged = draft.repeatFrequency != rule.frequency || original?.date != row.date ||
            rule.dateAt(row.occurrence ?: 0)?.toString() != row.date
        val anchor = if (calendarChanged) editedDate else requireNotNull(OperationDates.parse(rule.anchor))
        val until = draft.repeatUntil.takeIf { it.isNotBlank() }?.let(OperationDates::parse)
        val remaining = draft.repeatCount.takeIf { it.isNotBlank() }?.toIntOrNull()
        if ((draft.repeatUntil.isNotBlank() && until == null) ||
            (draft.repeatCount.isNotBlank() && (remaining == null || remaining !in 0..10_000)) ||
            (draft.repeatUntil.isNotBlank() && draft.repeatCount.isNotBlank()) || (until != null && until < anchor)) return null
        val receiver = row.peerId != null && rule.target == row.account
        val cutoff = listOfNotNull(today, rule.generatedThrough?.let(OperationDates::parse), rule.dateAt(rule.next - 1),
            rows.filter { it.seriesId == rule.id }.mapNotNull { OperationDates.parse(it.date) }.maxOrNull(), anchor).max()
        val frequency = draft.repeatFrequency
        val probe = rule.copy(anchor = anchor.toString(), frequency = frequency, scheduleBase = 0, scheduleOffset = 0)
        var step = if (frequency.weeks > 0)
            (ChronoUnit.DAYS.between(anchor, cutoff) / (7L * frequency.weeks) + 1).coerceAtLeast(1).toInt()
        else (ChronoUnit.MONTHS.between(anchor.withDayOfMonth(1), cutoff.withDayOfMonth(1)) / frequency.months).coerceAtLeast(1).toInt()
        while (probe.dateAt(step)?.let { it <= cutoff } == true) step++
        if (step !in 1..1_000_000) return null
        // An explicitly stopped series stays stopped; changing an exhausted limit can resume it.
        val wasExhausted = rule.copy(active = true).nextDate() == null
        return rule.copy(anchor = anchor.toString(), frequency = frequency,
            account = if (receiver) requireNotNull(draft.transferTarget) else row.account,
            target = if (receiver) row.account else draft.transferTarget,
            amount = if (receiver) row.exchangeAmount ?: row.amount.negate() else row.amount,
            targetAmount = if (receiver && row.exchangeAmount != null) row.amount else row.exchangeAmount,
            originalCurrency = row.originalCurrency, originalAmount = row.originalAmount, fxRate = row.fxRate,
            description = row.description, category = row.category, payment = row.payment,
            until = until?.toString(), count = remaining?.let { rule.next + it },
            active = (rule.active || wasExhausted) && remaining != 0,
            scheduleBase = rule.next, scheduleOffset = step, generatedThrough = cutoff.toString())
    }
}
