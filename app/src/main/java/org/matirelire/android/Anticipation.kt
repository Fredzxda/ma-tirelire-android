package org.matirelire.android

import java.time.LocalDate

internal enum class Anticipation {
    DUE, MONTH_END, DAYS_15, DAYS_30;
    fun horizon(today: LocalDate): LocalDate = when (this) {
        DUE -> today
        MONTH_END -> today.withDayOfMonth(today.lengthOfMonth())
        DAYS_15 -> today.plusDays(15)
        DAYS_30 -> today.plusDays(30)
    }
}

internal enum class RecurringState {
    FORECAST, PENDING, CONFIRMED;
    companion object {
        fun of(row: DemoTransaction, today: LocalDate): RecurringState? {
            if (row.seriesId == null) return null
            if (row.amountConfirmed) return CONFIRMED
            return if (OperationDates.parse(row.date)?.let { it > today } == true) FORECAST else PENDING
        }
    }
}
