package org.matirelire.android

import java.math.BigDecimal
import java.time.LocalDate

internal enum class RepeatFrequency(val weeks: Int = 0, val months: Int = 0, val endOfMonth: Boolean = false) {
    NONE, WEEK(weeks = 1), TWO_WEEKS(weeks = 2), MONTH(months = 1), MONTH_END(months = 1, endOfMonth = true),
    TWO_MONTHS(months = 2), THREE_MONTHS(months = 3), FOUR_MONTHS(months = 4), SIX_MONTHS(months = 6),
    YEAR(months = 12), TWO_YEARS(months = 24);
}

internal data class RecurringRule(
    val id: String, val anchor: String, val frequency: RepeatFrequency,
    val account: Int, val target: Int?, val amount: BigDecimal, val description: String,
    val category: String, val payment: String, val next: Int = 1,
    val until: String? = null, val count: Int? = null, val active: Boolean = true,
    val confirmedThrough: Int = 0,
    val scheduleBase: Int = 0, val scheduleOffset: Int = 0, val generatedThrough: String? = null,
) {
    fun dateAt(index: Int): LocalDate? = runCatching {
        val start = requireNotNull(OperationDates.parse(anchor))
        val step = index.toLong() - scheduleBase + scheduleOffset
        val date = if (frequency.weeks > 0) start.plusWeeks(step * frequency.weeks)
            else start.plusMonths(step * frequency.months)
        (if (frequency.endOfMonth && step > 0) date.withDayOfMonth(date.lengthOfMonth()) else date)
            .takeIf { it.year in 1..9999 }
    }.getOrNull()
    fun nextDate(): LocalDate? {
        if (!active || (count != null && next >= count)) return null
        val date = dateAt(next) ?: return null
        return date.takeIf { until == null || it <= requireNotNull(OperationDates.parse(until)) }
    }
}

internal data class RepeatGeneration(val data: LedgerData, val changed: Boolean, val blocked: Boolean)

internal object Recurrences {
    /** Cursor and complete occurrences are returned together for one atomic snapshot. */
    fun generate(data: LedgerData, today: LocalDate, budget: Int = 256): RepeatGeneration {
        require(budget in 1..1000)
        val horizon = data.anticipation.horizon(today)
        val rows = data.transactions.toMutableList()
        val rules = data.rules.toMutableList()
        val recorded = rows.mapNotNull { row -> row.seriesId?.let { it to row.occurrence } }.toHashSet()
        var nextId = (rows.maxOfOrNull { it.id } ?: 0) + 1
        var made = 0
        var changed = false
        var blocked = false
        for (i in rules.indices) {
            var rule = rules[i]
            while (rule.active) {
                val due = rule.nextDate()
                if (due == null) { rule = rule.copy(active = false); changed = true; break }
                if (due > horizon || made >= budget) break
                val needed = if (rule.target == null) 1 else 2
                if (rows.size + needed > LedgerLimits.MAX_TRANSACTIONS || nextId <= 0 || nextId >= Long.MAX_VALUE - 1 || rule.next >= 1_000_000) {
                    blocked = true; break
                }
                // Deleted occurrences remain consumed by the cursor and cannot reappear.
                if (recorded.add(rule.id to rule.next)) {
                    val first = DemoTransaction(nextId++, rule.account, rule.description, rule.amount,
                        rule.category, rule.payment, due.toString(), false,
                        if (rule.target == null) null else nextId, rule.id, rule.next, false)
                    rows.add(first)
                    if (rule.target != null) rows.add(first.copy(id = nextId++, account = rule.target,
                        amount = rule.amount.negate(), peerId = first.id))
                }
                rule = rule.copy(next = rule.next + 1, generatedThrough = due.toString())
                changed = true; made++
            }
            rules[i] = rule
        }
        return RepeatGeneration(data.copy(transactions = rows, rules = rules), changed, blocked)
    }
}
