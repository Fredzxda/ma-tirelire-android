package org.matirelire.android

import java.time.LocalDate
import java.time.YearMonth

internal enum class PeriodScope { MONTH, ALL, UNDATED }

/** Session-only navigation; never changes accounting or writes preferences. */
internal data class AccountPeriod(val month: YearMonth = YearMonth.now(),
    val scope: PeriodScope = PeriodScope.MONTH, val followsCurrent: Boolean = true) {
    fun effectiveMonth(today: LocalDate): YearMonth = if (followsCurrent) YearMonth.from(today) else month
    fun move(delta: Long, today: LocalDate): AccountPeriod? {
        val next = runCatching { effectiveMonth(today).plusMonths(delta) }.getOrNull() ?: return null
        return if (next.year in 1..9999) copy(month = next, scope = PeriodScope.MONTH, followsCurrent = false) else null
    }
    fun filter(rows: List<DemoTransaction>, today: LocalDate): List<DemoTransaction> = when (scope) {
        PeriodScope.ALL -> rows
        PeriodScope.UNDATED -> rows.filter { OperationDates.forOrdering(it.date) == null }
        PeriodScope.MONTH -> {
            val selected = effectiveMonth(today)
            rows.filter { row -> OperationDates.forOrdering(row.date)?.let { YearMonth.from(it) == selected } == true }
        }
    }
}
