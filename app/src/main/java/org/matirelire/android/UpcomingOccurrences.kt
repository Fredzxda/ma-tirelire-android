package org.matirelire.android

import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal data class UpcomingOccurrence(val seriesId: String, val index: Int, val date: LocalDate,
    val description: String, val amount: BigDecimal, val otherAccount: Int?)
internal data class UpcomingResult(val items: List<UpcomingOccurrence>, val truncated: Boolean)

internal object UpcomingOccurrences {
    /** Read-only projection: no generation, cursor update, clearing or persistence. */
    fun project(rules: List<RecurringRule>, account: Int, today: LocalDate, days: Int, limit: Int = 500): UpcomingResult {
        require(days in listOf(30, 90, 365) && limit in 1..500)
        val end = today.plusDays(days.toLong())
        val items = mutableListOf<UpcomingOccurrence>()
        for (rule in rules) {
            if (!rule.active || (rule.account != account && rule.target != account)) continue
            var index = rule.next
            val first = rule.nextDate() ?: continue
            // Jump over a large backlog rather than looping through every missed period.
            if (first <= today) {
                val jump = if (rule.frequency.weeks > 0)
                    ChronoUnit.DAYS.between(first, today) / (7L * rule.frequency.weeks)
                else ChronoUnit.MONTHS.between(first.withDayOfMonth(1), today.withDayOfMonth(1)) / rule.frequency.months
                index += jump.coerceAtLeast(0).coerceAtMost(1_000_000).toInt()
            }
            while (index < 1_000_000) {
                val date = rule.copy(next = index).nextDate() ?: break
                if (date > end) break
                if (date > today) {
                    val receiver = rule.target == account
                    items.add(UpcomingOccurrence(rule.id, index, date, rule.description,
                        if (receiver) rule.targetAmount ?: rule.amount.negate() else rule.amount,
                        if (receiver) rule.account else rule.target))
                }
                index++
            }
        }
        val sorted = items.sortedWith(compareBy<UpcomingOccurrence> { it.date }.thenBy { it.seriesId }.thenBy { it.index })
        return UpcomingResult(sorted.take(limit), sorted.size > limit)
    }
}
