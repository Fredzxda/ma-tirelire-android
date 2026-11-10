package org.matirelire.android

import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class RecurrenceConfirmationTest {
    private fun model(transfer: Boolean = false, count: String = "4") = DemoViewModel().apply {
        initialize(listOf(DemoTransaction(1, 0, "Fixture", BigDecimal("-1.00"), "Food", "Card", "2026-01-01", true)))
        ensureAccountNames(listOf("Current", "Savings")); ensureTransferPayment("Transfer")
        beginEntry(LocalDate.of(2026, 1, 1))
        updateDraft(draft!!.copy(amount = "10", description = "Recurring", repeatFrequency = RepeatFrequency.MONTH,
            repeatCount = count, payment = if (transfer) "Transfer" else "Card", transferTarget = if (transfer) 1 else null))
        assertTrue(saveEntry(Locale.US, "unused"))
    }
    private fun confirm(model: DemoViewModel, number: Int, amount: String, account: Int = 0) {
        val row = model.transactions.first { it.occurrence == number && it.account == account }
        model.editEntry(row.id, Locale.US)
        model.updateDraft(model.draft!!.copy(amount = amount))
        assertTrue(model.saveEntry(Locale.US, "unused"))
    }

    @Test fun calendarSelectionStoresIsoClearsCountAndRefusesEarlierEnd() {
        val model = model()
        model.beginEntry(LocalDate.of(2026, 6, 10))
        model.updateDraft(model.draft!!.copy(repeatCount = "3"))
        assertFalse(model.selectRepeatEnd("2026-06-09"))
        assertFalse(model.selectRepeatEnd("10/06/2026"))
        assertEquals("3", model.draft!!.repeatCount)
        assertTrue(model.selectRepeatEnd("2026-06-10"))
        assertEquals("2026-06-10", model.draft!!.repeatUntil)
        assertEquals("", model.draft!!.repeatCount)
        assertNotEquals("2026-06-10", OperationDates.display(model.draft!!.repeatUntil, Locale.FRANCE))
        assertTrue(model.selectRepeatEnd(null))
        assertEquals("", model.draft!!.repeatUntil)
    }

    @Test fun confirmingChangesLaterProposalsAndTemplateButNotEarlierHistoryOrClearing() {
        val model = model()
        model.generateDue(LocalDate.of(2026, 4, 1))
        confirm(model, 1, "20")
        val series = model.transactions.filter { it.seriesId != null }
        assertEquals(BigDecimal("-10.00"), series.single { it.occurrence == 0 }.amount)
        assertTrue(series.single { it.occurrence == 1 }.amountConfirmed)
        assertTrue(series.filter { it.occurrence!! > 1 }.all { !it.amountConfirmed && it.amount == BigDecimal("-20.00") })
        assertTrue(series.all { !it.cleared })
        assertEquals(BigDecimal("-20.00"), model.rules.single().amount)
        assertEquals(1, model.rules.single().confirmedThrough)
        val restored = DemoViewModel()
        restored.restoreData(SnapshotCodec.decode(SnapshotCodec.encode(model.exportData())))
        assertEquals(model.exportData(), restored.exportData())
    }

    @Test fun outOfOrderConfirmationPreservesTheMostRecentConfirmedAmount() {
        val model = model()
        model.generateDue(LocalDate.of(2026, 4, 1))
        confirm(model, 2, "30")
        confirm(model, 1, "20")
        assertEquals(BigDecimal("-30.00"), model.rules.single().amount)
        assertEquals(2, model.rules.single().confirmedThrough)
        assertEquals(BigDecimal("-30.00"), model.transactions.single { it.occurrence == 3 }.amount)
        assertEquals(BigDecimal("-20.00"), model.transactions.single { it.occurrence == 1 }.amount)
    }

    @Test fun receiverConfirmationUpdatesThePairAndProtectsAPointedLaterPair() {
        val model = model(transfer = true)
        model.generateDue(LocalDate.of(2026, 3, 1))
        model.toggleCleared(model.transactions.single { it.occurrence == 2 && it.account == 0 }.id)
        confirm(model, 1, "25", account = 1)
        val confirmedPair = model.transactions.filter { it.occurrence == 1 }
        assertTrue(confirmedPair.all { it.amountConfirmed && !it.cleared })
        assertEquals(0, confirmedPair.fold(BigDecimal.ZERO) { sum, row -> sum + row.amount }.signum())
        assertTrue(model.transactions.filter { it.occurrence == 2 }.all { it.amount.abs() == BigDecimal("10.00") })
        model.generateDue(LocalDate.of(2026, 4, 1))
        assertTrue(model.transactions.filter { it.occurrence == 3 }.all { it.amount.abs() == BigDecimal("25.00") && !it.amountConfirmed })
        SnapshotCodec.decode(SnapshotCodec.encode(model.exportData()))
    }

    @Test fun legacyMigrationAgreesOnBothSidesAndKeepsPendingStateAfterRestore() {
        val model = model(transfer = true)
        model.generateDue(LocalDate.of(2026, 3, 1))
        model.toggleCleared(model.transactions.single { it.occurrence == 1 && it.account == 0 }.id)
        val old = JSONObject(SnapshotCodec.encode(model.exportData()).toString(Charsets.UTF_8)).put("schema", 5)
        val rows = old.getJSONArray("transactions")
        for (i in 0 until rows.length()) rows.getJSONObject(i).remove("amountConfirmed")
        val rules = old.getJSONArray("rules")
        for (i in 0 until rules.length()) rules.getJSONObject(i).remove("confirmedThrough")
        val restored = SnapshotCodec.decode(old.toString().toByteArray())
        assertTrue(restored.transactions.filter { it.occurrence == 1 }.all { it.amountConfirmed })
        assertTrue(restored.transactions.filter { it.occurrence == 2 }.all { !it.amountConfirmed })
        assertEquals(1, restored.rules.single().confirmedThrough)
        assertEquals(restored, SnapshotCodec.decode(SnapshotCodec.encode(restored)))
    }
}
