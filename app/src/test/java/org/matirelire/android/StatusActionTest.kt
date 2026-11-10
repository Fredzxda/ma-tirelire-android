package org.matirelire.android

import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class StatusActionTest {
    private val today = LocalDate.of(2026, 11, 10)
    private fun model(transfer: Boolean = false) = DemoViewModel().apply {
        initialize(listOf(DemoTransaction(1, 0, "Fixture", BigDecimal("-1.00"), "Food", "Card", "2026-01-01", true)))
        ensureAccountNames(listOf("Current", "Savings")); ensureTransferPayment("Transfer")
        beginEntry(LocalDate.of(2026, 11, 1))
        updateDraft(draft!!.copy(amount = "10", description = "Weekly", repeatFrequency = RepeatFrequency.WEEK,
            payment = if (transfer) "Transfer" else "Card", transferTarget = if (transfer) 1 else null))
        assertTrue(saveEntry(Locale.US, "unused"))
        setAnticipation(Anticipation.MONTH_END); generateDue(today)
    }

    @Test fun forecastNeedsTwoActionsAndNeitherRequestsScopeOrClearsTheRow() {
        val model = model()
        val row = model.transactions.first { it.date == "2026-11-15" }
        val others = model.transactions.filter { it.id != row.id }
        val reportCount = model.reportingTransactions().size
        model.editEntry(row.id, Locale.FRANCE)
        assertFalse(model.needsSeriesScope(Locale.FRANCE))
        assertTrue(model.saveEntry(Locale.FRANCE, "unused"))
        val prepared = model.transactions.first { it.id == row.id }
        assertEquals(RecurringState.PENDING, RecurringState.of(prepared, today))
        assertFalse(prepared.amountConfirmed)
        assertFalse(prepared.cleared)
        assertEquals(0, model.rules.single().confirmedThrough)
        assertEquals(reportCount, model.reportingTransactions().size)
        val restored = DemoViewModel()
        restored.restoreData(SnapshotCodec.decode(SnapshotCodec.encode(model.exportData())))
        assertEquals(RecurringState.PENDING, RecurringState.of(restored.transactions.first { it.id == row.id }, today))
        restored.editEntry(row.id, Locale.FRANCE)
        assertFalse(restored.needsSeriesScope(Locale.FRANCE))
        assertTrue(restored.saveEntry(Locale.FRANCE, "unused"))
        val confirmed = restored.transactions.first { it.id == row.id }
        assertEquals(RecurringState.CONFIRMED, RecurringState.of(confirmed, today))
        assertFalse(confirmed.cleared)
        assertEquals(others, restored.transactions.filter { it.id != row.id })
    }

    @Test fun dueOccurrenceConfirmsDirectlyButActualEditsStillRequestScope() {
        val model = model()
        val row = model.transactions.first { it.date == "2026-11-08" }
        model.editEntry(row.id, Locale.US)
        assertFalse(model.needsSeriesScope(Locale.US))
        val original = model.draft!!
        model.updateDraft(original.copy(amount = "20"))
        assertTrue(model.needsSeriesScope(Locale.US))
        model.updateDraft(original.copy(description = "Changed"))
        assertTrue(model.needsSeriesScope(Locale.US))
        model.updateDraft(original.copy(date = "2026-11-09"))
        assertTrue(model.needsSeriesScope(Locale.US))
        model.updateDraft(original)
        assertTrue(model.saveEntry(Locale.US, "unused"))
        assertTrue(model.transactions.first { it.id == row.id }.amountConfirmed)
    }

    @Test fun preparingAndConfirmingATransferUpdatesBothStatesAndKeepsClearingIndependent() {
        val model = model(transfer = true)
        val row = model.transactions.first { it.date == "2026-11-15" && it.account == 1 }
        model.toggleCleared(row.id)
        model.editEntry(row.id, Locale.US)
        assertFalse(model.needsSeriesScope(Locale.US))
        assertTrue(model.saveEntry(Locale.US, "unused"))
        val pair = model.transactions.filter { it.id == row.id || it.id == row.peerId }
        assertTrue(pair.all { it.readyToConfirm && !it.amountConfirmed })
        assertEquals(1, pair.count { it.cleared })
        model.editEntry(row.id, Locale.US)
        assertTrue(model.saveEntry(Locale.US, "unused"))
        val confirmedPair = model.transactions.filter { it.id == row.id || it.id == row.peerId }
        assertTrue(confirmedPair.all { it.amountConfirmed })
        assertEquals(1, confirmedPair.count { it.cleared })
        SnapshotCodec.decode(SnapshotCodec.encode(model.exportData()))
    }
}
