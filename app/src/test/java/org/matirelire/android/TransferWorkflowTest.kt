package org.matirelire.android

import java.math.BigDecimal
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class TransferWorkflowTest {
    private fun model(income: Boolean = false) = DemoViewModel().apply {
        initialize(listOf(DemoTransaction(1, 0, "Original", BigDecimal(if (income) "10.00" else "-10.00"),
            "Food", "Card", "2026-11-10", true)))
        ensureAccountNames(listOf("Current", "Savings"))
        ensureTransferPayment("Transfer")
    }

    @Test fun transferWithoutTargetIsAnExternalFlowAndAppearsInReports() {
        val model = model()
        model.beginEntry()
        model.selectDraftPayment("Transfer")
        assertNull(model.draft!!.transferTarget)
        model.updateDraft(model.draft!!.copy(amount = "5", description = "External"))
        assertTrue(model.saveEntry(Locale.US, "unused"))
        assertEquals(2, model.transactions.size)
        assertTrue(model.transactions.all { it.peerId == null })
        assertEquals(2, model.reportingTransactions().size)
    }

    @Test fun expenseAndIncomeConversionsKeepIdentityDirectionAndClearingInOneWrite() {
        for (income in listOf(false, true)) {
            val model = model(income)
            val before = model.transactions.single()
            model.editEntry(1, Locale.US)
            model.selectDraftPayment("Transfer")
            model.updateDraft(model.draft!!.copy(transferTarget = 1))
            assertEquals(1, model.transactions.size)
            var writes = 0
            model.onChange = { writes++; SnapshotCodec.decode(SnapshotCodec.encode(model.exportData())) }
            assertTrue(model.saveEntry(Locale.US, "unused"))
            assertEquals(1, writes)
            val original = model.transactions.single { it.id == 1L }
            val other = model.transactions.single { it.id != 1L }
            assertEquals(before.amount, original.amount)
            assertEquals(before.date, original.date)
            assertTrue(original.cleared)
            assertFalse(other.cleared)
            assertEquals(original.amount.negate(), other.amount)
            assertEquals("Transfer", other.payment)
            assertTrue(model.reportingTransactions().isEmpty())
            val restored = DemoViewModel()
            restored.restoreData(SnapshotCodec.decode(SnapshotCodec.encode(model.exportData())))
            assertEquals(model.exportData(), restored.exportData())
        }
    }

    @Test fun cancellationAndChangingMethodDoNotLeaveAHiddenTarget() {
        val model = model()
        val before = model.transactions.single()
        model.editEntry(1, Locale.US)
        model.selectDraftPayment("Transfer")
        model.updateDraft(model.draft!!.copy(transferTarget = 1))
        model.selectDraftPayment("Card")
        assertNull(model.draft!!.transferTarget)
        model.cancelEntry()
        assertEquals(before, model.transactions.single())
    }

    @Test fun migrationNormalizesOnlyLinkedPaymentsAndKeepsTheRoleAcrossLanguages() {
        val model = model()
        val row = model.transactions.single()
        val legacy = model.exportData().copy(transferPayment = null, transactions = listOf(
            row, row.copy(id = 2, peerId = 3, cleared = false),
            row.copy(id = 3, peerId = 2, account = 1, amount = row.amount.negate(), cleared = false)))
        val restored = DemoViewModel()
        restored.restoreData(SnapshotCodec.decode(SnapshotCodec.encode(legacy)))
        restored.ensureTransferPayment("Virement")
        assertEquals("Card", restored.transactions.first().payment)
        assertTrue(restored.transactions.filter { it.peerId != null }.all { it.payment == "Transfer" })
        val reopened = DemoViewModel()
        reopened.restoreData(SnapshotCodec.decode(SnapshotCodec.encode(restored.exportData())))
        reopened.ensureTransferPayment("Virement")
        assertTrue(reopened.isTransferPayment("Transfer"))
        assertEquals(listOf(row), reopened.reportingTransactions())
    }

    @Test fun linkedPairsRefuseCardAndCannotBeDetachedByChangingPayment() {
        val model = model()
        model.editEntry(1, Locale.US)
        model.selectDraftPayment("Transfer")
        model.updateDraft(model.draft!!.copy(transferTarget = 1))
        assertTrue(model.saveEntry(Locale.US, "unused"))
        model.editEntry(1, Locale.US)
        model.selectDraftPayment("Card")
        assertEquals("Transfer", model.draft!!.payment)
        assertEquals(1, model.draft!!.transferTarget)
        val good = model.exportData()
        val bad = good.copy(transactions = good.transactions.map { it.copy(payment = "Card") })
        try { SnapshotCodec.encode(bad); fail("Card allowed on linked pair") }
        catch (_: IllegalArgumentException) { }
    }
}
