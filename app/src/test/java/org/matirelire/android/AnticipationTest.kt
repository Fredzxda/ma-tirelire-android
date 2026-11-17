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
        beginEntryWithReferences(LocalDate.of(2026, 11, 1))
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
        assertEquals(RecurringState.PENDING, RecurringState.of(model.transactions.first { it.id == future.id }, today))
        model.editEntry(future.id, Locale.US)
        assertTrue(model.saveEntry(Locale.US, "unused", SeriesEditScope.CURRENT))
        val confirmed = model.transactions.first { it.id == future.id }
        assertEquals(RecurringState.CONFIRMED, RecurringState.of(confirmed, today))
        assertFalse(confirmed.cleared)
    }

    @Test fun reducingHorizonDefersExactForecastsAndRestoringDoesNotDuplicate() {
        val model = model()
        model.setAnticipation(Anticipation.MONTH_END); model.generateDue(today)
        val before = model.transactions.toList()
        val archive = SnapshotCodec.encode(model.exportData())
        val restored = DemoViewModel()
        restored.restoreData(SnapshotCodec.decode(archive))
        assertEquals(Anticipation.MONTH_END, restored.anticipation)
        assertFalse(restored.generateDue(today))
        restored.generateDue(today)
        restored.setAnticipation(Anticipation.DUE)
        assertFalse(restored.generateDue(today))
        assertEquals(before.toSet(), (restored.transactions + restored.deferredForecasts).toSet())
        assertTrue(restored.transactions.none { RecurringState.of(it, today) == RecurringState.FORECAST })
        assertEquals(3, restored.deferredForecasts.size)
        restored.setAnticipation(Anticipation.MONTH_END)
        assertEquals(before.toSet(), restored.transactions.toSet())
        assertTrue(restored.deferredForecasts.isEmpty())
    }

    @Test fun deferredRecordsKeepExactHistoryAndReserveTheirIds() {
        val model = model()
        model.generateDue(today)
        model.setAnticipation(Anticipation.MONTH_END); model.generateDue(today)
        model.setAnticipation(Anticipation.DUE)
        val hidden = model.deferredForecasts.toList()
        model.beginEntryWithReferences(today)
        model.updateDraft(model.draft!!.copy(amount = "2", description = "New", category = "Food", payment = "Card"))
        assertTrue(model.saveEntry(Locale.US, "unused"))
        assertTrue(model.transactions.last().id > hidden.maxOf { it.id })
        val saved = model.exportData()
        val changedTemplate = saved.copy(rules = saved.rules.map { it.copy(amount = BigDecimal("-99.00"), description = "Changed future template") },
            anticipation = Anticipation.MONTH_END)
        val restored = Recurrences.generate(SnapshotCodec.decode(SnapshotCodec.encode(changedTemplate)), today).data
        assertEquals(hidden.toSet(), restored.transactions.filter { row -> row.id in hidden.map { it.id } }.toSet())
        assertEquals(3, UpcomingOccurrences.project(saved.rules, 0, today, 30, deferred = saved.deferredForecasts).items.count { it.index < saved.rules.single().next })
    }

    @Test fun protectedPairsAndPreparedOccurrencesStayVisible() {
        val model = model(transfer = true)
        model.generateDue(today)
        model.setAnticipation(Anticipation.MONTH_END); model.generateDue(today)
        val data = model.exportData()
        val changed = data.copy(transactions = data.transactions.map {
            when (it.date) {
                "2026-11-15" -> it.copy(readyToConfirm = true)
                "2026-11-22" -> if (it.account == 1) it.copy(cleared = true) else it
                else -> it
            }
        }, anticipation = Anticipation.DUE)
        val adjusted = AnticipationForecasts.adjust(changed, today)
        assertEquals(2, adjusted.deferredForecasts.size)
        assertTrue(adjusted.deferredForecasts.all { it.date == "2026-11-29" })
        assertEquals(2, adjusted.transactions.count { it.date == "2026-11-22" })
        SnapshotCodec.decode(SnapshotCodec.encode(adjusted))
    }

    @Test fun hiddenForecastsRestoreWhenDueWithoutRevivingManualDeletion() {
        val model = model()
        model.generateDue(today)
        model.setAnticipation(Anticipation.MONTH_END); model.generateDue(today)
        val deleted = model.transactions.first { it.date == "2026-11-22" }
        model.deleteEntry(deleted.id)
        model.setAnticipation(Anticipation.DUE)
        val persisted = SnapshotCodec.decode(SnapshotCodec.encode(model.exportData()))
        val due = Recurrences.generate(persisted, LocalDate.of(2026, 11, 29)).data
        assertTrue(due.deferredForecasts.isEmpty())
        assertTrue(due.transactions.none { it.date == "2026-11-22" })
        assertEquals(1, due.transactions.count { it.date == "2026-11-29" })
        assertFalse(Recurrences.generate(due, LocalDate.of(2026, 11, 29)).changed)
    }

    @Test fun accountArchivingPreservesHiddenPartitionAndExternalizesFutureTemplate() {
        val model = model(transfer = true)
        model.generateDue(today)
        model.setAnticipation(Anticipation.MONTH_END); model.generateDue(today)
        model.setAnticipation(Anticipation.DUE)
        val hidden = model.deferredForecasts.toList()
        val balance = model.balance(0)
        assertTrue(model.archiveAccount(1))
        assertEquals(balance, model.balance(0))
        assertEquals(hidden, model.deferredForecasts.toList())
        assertNull(model.rules.single().target)
        assertTrue(model.deleteAccount(1))
        assertTrue(model.deferredForecasts.all { it.account == 0 && it.peerId == null })
        SnapshotCodec.decode(SnapshotCodec.encode(model.exportData()))
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
