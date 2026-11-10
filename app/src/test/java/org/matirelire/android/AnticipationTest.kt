package org.matirelire.android

import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class AnticipationTest {
    private val today = LocalDate.of(2026, 11, 10)
    private fun model(transfer: Boolean = false) = DemoViewModel().apply {
        initialize(listOf(DemoTransaction(1, 0, "Fixture", BigDecimal("-1.00"), "Food", "Card", "2026-01-01", true)))
        ensureAccountNames(listOf("Current", "Savings")); ensureTransferPayment("Transfer")
        beginEntry(LocalDate.of(2026, 11, 1))
        updateDraft(draft!!.copy(amount = "10", description = "Weekly", repeatFrequency = RepeatFrequency.WEEK,
            payment = if (transfer) "Transfer" else "Card", transferTarget = if (transfer) 1 else null))
        assertTrue(saveEntry(Locale.US, "unused"))
    }

    @Test fun monthEndGeneratesAllThisMonthAndOnlyDueModePreservesOldBehavior() {
        val model = model()
        assertEquals(Anticipation.DUE, model.anticipation)
        model.generateDue(today)
        assertEquals(listOf("2026-11-01", "2026-11-08"), model.transactions.filter { it.seriesId != null }.map { it.date })
        val cleared = model.balance(0, true)
        val total = model.balance(0)
        model.setAnticipation(Anticipation.MONTH_END)
        assertTrue(model.generateDue(today))
        assertEquals(listOf("2026-11-01", "2026-11-08", "2026-11-15", "2026-11-22", "2026-11-29"),
            model.transactions.filter { it.seriesId != null }.map { it.date })
        assertEquals(total - BigDecimal("30.00"), model.balance(0))
        assertEquals(cleared, model.balance(0, true))
        assertFalse(model.generateDue(today))
    }

    @Test fun visualStateAndConfirmationAreIndependentFromClearing() {
        val model = model()
        model.setAnticipation(Anticipation.MONTH_END); model.generateDue(today)
        val future = model.transactions.first { it.date == "2026-11-15" }
        assertEquals(RecurringState.FORECAST, RecurringState.of(future, today))
        assertEquals(RecurringState.PENDING, RecurringState.of(future, LocalDate.of(2026, 11, 15)))
        assertEquals(RecurringState.FORECAST, RecurringState.of(future.copy(cleared = true), today))
        model.editEntry(future.id, Locale.US)
        assertTrue(model.saveEntry(Locale.US, "unused", SeriesEditScope.CURRENT))
        val confirmed = model.transactions.first { it.id == future.id }
        assertEquals(RecurringState.CONFIRMED, RecurringState.of(confirmed, today))
        assertFalse(confirmed.cleared)
    }

    @Test fun reducingHorizonAndRestoringNeverDeleteOrDuplicateForecasts() {
        val model = model()
        model.setAnticipation(Anticipation.MONTH_END); model.generateDue(today)
        val before = model.transactions.toList()
        val archive = SnapshotCodec.encode(model.exportData())
        val restored = DemoViewModel()
        restored.restoreData(SnapshotCodec.decode(archive))
        assertEquals(Anticipation.MONTH_END, restored.anticipation)
        assertFalse(restored.generateDue(today))
        restored.setAnticipation(Anticipation.DUE)
        assertFalse(restored.generateDue(today))
        assertEquals(before, restored.transactions.toList())
    }

    @Test fun transfersStayAtomicAndAreNotDuplicatedInUpcomingView() {
        val model = model(transfer = true)
        val total = model.balance(0) + model.balance(1)
        model.setAnticipation(Anticipation.MONTH_END); model.generateDue(today)
        assertEquals(total, model.balance(0) + model.balance(1))
        val groups = model.transactions.filter { it.seriesId != null }.groupBy { it.occurrence }
        assertTrue(groups.values.all { it.size == 2 && (it[0].amount + it[1].amount).signum() == 0 })
        val upcoming = UpcomingOccurrences.project(model.rules.toList(), 0, today, 30).items
        assertTrue(upcoming.all { it.date > LocalDate.of(2026, 11, 30) })
        SnapshotCodec.decode(SnapshotCodec.encode(model.exportData()))
    }

    @Test fun forecastFlowsAreExcludedFromReportsUntilDueOrConfirmed() {
        val model = model()
        model.setAnticipation(Anticipation.MONTH_END); model.generateDue(today)
        assertEquals(3, model.reportingTransactions().size) // fixture, initial and overdue occurrence
        model.generateDue(LocalDate.of(2026, 11, 15))
        assertEquals(4, model.reportingTransactions().size)
    }

    @Test fun civilHorizonsAndSeriesLimitsAreRespected() {
        assertEquals(LocalDate.of(2024, 2, 29), Anticipation.MONTH_END.horizon(LocalDate.of(2024, 2, 10)))
        assertEquals(LocalDate.of(2026, 11, 25), Anticipation.DAYS_15.horizon(today))
        assertEquals(LocalDate.of(2026, 12, 10), Anticipation.DAYS_30.horizon(today))
        val model = model()
        val data = model.exportData().copy(anticipation = Anticipation.MONTH_END,
            rules = model.rules.map { it.copy(until = "2026-11-22") })
        val generated = Recurrences.generate(data, today)
        assertEquals("2026-11-22", generated.data.transactions.last().date)
        assertFalse(generated.data.rules.single().active)
    }
}
