package org.matirelire.android

import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class UpcomingOccurrencesTest {
    private fun model(transfer: Boolean = false) = DemoViewModel().apply {
        initialize(listOf(DemoTransaction(1, 0, "Fixture", BigDecimal("-1.00"), "Food", "Card", "2026-01-01", true)))
        ensureAccountNames(listOf("Current", "Savings")); ensureTransferPayment("Transfer")
        beginEntry(LocalDate.of(2026, 1, 31))
        updateDraft(draft!!.copy(amount = "10", description = "Monthly", repeatFrequency = RepeatFrequency.MONTH,
            payment = if (transfer) "Transfer" else "Card", transferTarget = if (transfer) 1 else null))
        assertTrue(saveEntry(Locale.US, "unused"))
    }

    @Test fun projectionIsReadOnlyAndPreservesMonthEndWithoutRepeatingRecordedOccurrences() {
        val model = model()
        model.generateDue(LocalDate.of(2026, 2, 28))
        val before = model.exportData()
        repeat(3) {
            val result = UpcomingOccurrences.project(model.rules.toList(), 0, LocalDate.of(2026, 2, 28), 90)
            assertEquals(listOf("2026-03-31", "2026-04-30"), result.items.map { it.date.toString() })
            assertTrue(result.items.all { it.amount == BigDecimal("-10.00") && it.index >= 2 })
            assertFalse(result.truncated)
        }
        assertEquals(before, model.exportData())
    }

    @Test fun transferProjectionHasOppositeAmountsAndNamesTheOtherAccount() {
        val model = model(transfer = true)
        val date = LocalDate.of(2026, 2, 1)
        val source = UpcomingOccurrences.project(model.rules.toList(), 0, date, 30).items.single()
        val target = UpcomingOccurrences.project(model.rules.toList(), 1, date, 30).items.single()
        assertEquals(source.amount.negate(), target.amount)
        assertEquals(1, source.otherAccount)
        assertEquals(0, target.otherAccount)
        assertEquals(source.date, target.date)
        assertEquals(source.index, target.index)
    }

    @Test fun stoppedSeriesAndEndLimitsAreRespected() {
        val rule = model().rules.single()
        val today = LocalDate.of(2026, 2, 1)
        assertTrue(UpcomingOccurrences.project(listOf(rule.copy(active = false)), 0, today, 365).items.isEmpty())
        val end = UpcomingOccurrences.project(listOf(rule.copy(until = "2026-03-31")), 0, today, 365)
        assertEquals(listOf("2026-02-28", "2026-03-31"), end.items.map { it.date.toString() })
        assertEquals(1, UpcomingOccurrences.project(listOf(rule.copy(count = 2)), 0, today, 365).items.size)
        assertTrue(UpcomingOccurrences.project(listOf(rule), 2, today, 365).items.isEmpty())
    }

    @Test fun revisedCalendarAndAmountAreUsedForFutureProjectionOnly() {
        val model = model()
        model.generateDue(LocalDate.of(2026, 2, 28))
        val row = model.transactions.last()
        model.editEntry(row.id, Locale.US)
        model.updateDraft(model.draft!!.copy(date = "2026-03-04", amount = "25", repeatFrequency = RepeatFrequency.WEEK))
        assertTrue(model.saveEntry(Locale.US, "unused", SeriesEditScope.SERIES, LocalDate.of(2026, 3, 10)))
        val before = model.exportData()
        val result = UpcomingOccurrences.project(model.rules.toList(), 0, LocalDate.of(2026, 3, 10), 30)
        assertEquals("2026-03-11", result.items.first().date.toString())
        assertTrue(result.items.all { it.amount == BigDecimal("-25.00") })
        assertEquals(before, model.exportData())
    }

    @Test fun largeBacklogIsSkippedAndGlobalChronologicalLimitIsExplicit() {
        val rule = model().rules.single().copy(anchor = "1900-01-01", frequency = RepeatFrequency.WEEK, count = null)
        val rules = (1..100).map { rule.copy(id = "rule-$it") }
        val result = UpcomingOccurrences.project(rules, 0, LocalDate.of(2026, 11, 10), 365)
        assertEquals(500, result.items.size)
        assertTrue(result.truncated)
        assertEquals(result.items.map { it.date }.sorted(), result.items.map { it.date })
        assertTrue(result.items.all { it.date > LocalDate.of(2026, 11, 10) })
        assertEquals(1, rule.next)
    }

    @Test fun leapYearAndHorizonBoundariesUseCivilDates() {
        val rule = model().rules.single().copy(anchor = "2024-02-29", frequency = RepeatFrequency.YEAR)
        val today = LocalDate.of(2025, 1, 29)
        assertEquals(listOf("2025-02-28"), UpcomingOccurrences.project(listOf(rule), 0, today, 30).items.map { it.date.toString() })
        assertTrue(UpcomingOccurrences.project(listOf(rule), 0, LocalDate.of(2025, 2, 28), 30).items.isEmpty())
    }
}
