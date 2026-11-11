package org.matirelire.android

import java.math.BigDecimal
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class TransferTest {
    private fun model() = DemoViewModel().apply {
        initialize(listOf(DemoTransaction(1, 0, "Fixture", BigDecimal("-10.00"), "Food", "Card", "2026-11-10", true)))
        ensureAccountNames(listOf("Current", "Savings"))
    }
    private fun transfer(model: DemoViewModel) {
        model.beginEntry()
        model.updateDraft(model.draft!!.copy(transferTarget = 1, amount = "25,50", description = "Transfer", date = "2026-11-09"))
        assertTrue(model.saveEntry(Locale.FRANCE, "unused"))
    }

    @Test fun creationPersistsOneCompletePairAndKeepsConsolidationUnchanged() {
        val model = model()
        val total = model.balance(0) + model.balance(1)
        model.beginEntry()
        model.updateDraft(model.draft!!.copy(transferTarget = 1, amount = "25,50", description = "Transfer"))
        var writes = 0
        model.onChange = {
            writes++
            val data = SnapshotCodec.decode(SnapshotCodec.encode(model.exportData()))
            assertEquals(2, data.transactions.count { it.peerId != null })
        }
        assertTrue(model.saveEntry(Locale.FRANCE, "unused"))
        assertEquals(1, writes)
        assertEquals(total, model.balance(0) + model.balance(1))
        val rows = model.transactions.filter { it.peerId != null }
        assertEquals(BigDecimal("-25.50"), rows[0].amount)
        assertEquals(BigDecimal("25.50"), rows[1].amount)
        assertEquals(rows[0].id, rows[1].peerId)
        assertEquals(rows[1].id, rows[0].peerId)
        assertFalse(rows.any { it.cleared })
        assertFalse(model.saveEntry(Locale.FRANCE, "unused"))
    }

    @Test fun receiverEditUpdatesBothSidesAndPreservesIndependentClearing() {
        val model = model()
        transfer(model)
        val credit = model.transactions.single { it.peerId != null && it.amount.signum() > 0 }
        model.toggleCleared(credit.id)
        assertFalse(model.transactions.single { it.id == credit.peerId }.cleared)
        model.editEntry(credit.id, Locale.FRANCE)
        assertTrue(model.draft!!.income)
        model.updateDraft(model.draft!!.copy(amount = "42,00", description = "Changed", date = "2026-10-01"))
        val restored = DemoViewModel()
        restored.restoreData(SnapshotCodec.decode(SnapshotCodec.encode(model.exportData())))
        var writes = 0
        restored.onChange = { writes++; SnapshotCodec.decode(SnapshotCodec.encode(restored.exportData())) }
        assertTrue(restored.saveEntry(Locale.FRANCE, "unused"))
        assertEquals(1, writes)
        assertEquals(3, restored.transactions.size)
        val pair = restored.transactions.filter { it.peerId != null }
        assertTrue(pair.all { it.date == "2026-10-01" && it.description == "Changed" })
        assertEquals(BigDecimal("42.00"), pair.single { it.id == credit.id }.amount)
        assertTrue(pair.single { it.id == credit.id }.cleared)
        assertFalse(pair.single { it.id == credit.peerId }.cleared)
    }

    @Test fun deletionRemovesBothSidesInOneWriteAndCancelKeepsThem() {
        val model = model()
        transfer(model)
        val credit = model.transactions.last()
        val before = model.transactions.toList()
        model.editEntry(credit.id, Locale.US)
        model.cancelEntry()
        assertEquals(before, model.transactions.toList())
        var writes = 0
        model.onChange = { writes++; SnapshotCodec.decode(SnapshotCodec.encode(model.exportData())) }
        model.deleteEntry(credit.id)
        assertEquals(1, writes)
        assertEquals(1, model.transactions.size)
        assertEquals("Fixture", model.transactions.single().description)
    }

    @Test fun invalidTargetsAndPartialPairsAreRejected() {
        val model = model()
        model.beginEntry()
        model.updateDraft(model.draft!!.copy(transferTarget = 0, amount = "10", description = "Invalid"))
        assertFalse(model.saveEntry(Locale.US, "unused"))
        assertEquals(1, model.transactions.size)
        model.cancelEntry()
        transfer(model)
        val good = model.exportData()
        val credit = good.transactions.last()
        val cases = listOf(
            good.copy(transactions = good.transactions.dropLast(1)),
            good.copy(transactions = good.transactions.map { if (it.id == credit.id) it.copy(peerId = null) else it }),
            good.copy(transactions = good.transactions.map { if (it.id == credit.id) it.copy(amount = BigDecimal("99.00")) else it }),
            good.copy(transactions = good.transactions.map { if (it.id == credit.id) it.copy(account = 0) else it }),
            good.copy(transactions = good.transactions.map { if (it.id == credit.id) it.copy(date = "other") else it }))
        for (bad in cases) {
            try { SnapshotCodec.encode(bad); fail("Inconsistent pair accepted") }
            catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun oldPaymentNamedTransferStaysOrdinaryAndCapacityReservesTwoRows() {
        val model = model()
        val ordinary = model.exportData().transactions.single().copy(payment = "Transfer")
        val legacy = model.exportData().copy(transactions = listOf(ordinary), payments = listOf("Transfer"))
        assertNull(SnapshotCodec.decode(SnapshotCodec.encode(legacy)).transactions.single().peerId)
        val rows = (1L until LedgerLimits.MAX_TRANSACTIONS.toLong()).map { ordinary.copy(id = it) }
        model.restoreData(legacy.copy(transactions = rows))
        model.beginEntry()
        model.updateDraft(model.draft!!.copy(transferTarget = 1, amount = "1", description = "Too many"))
        assertFalse(model.saveEntry(Locale.US, "unused"))
        assertEquals(LedgerLimits.MAX_TRANSACTIONS - 1, model.transactions.size)
    }
    @Test fun editingCannotReverseOrMoveAnExistingPair() {
        val model = model()
        transfer(model)
        val credit = model.transactions.last()
        model.editEntry(credit.id, Locale.US)
        val original = model.draft!!
        model.updateDraft(original.copy(income = false))
        assertFalse(model.saveEntry(Locale.US, "unused"))
        try { SnapshotCodec.encode(model.exportData()); fail("Reversed draft accepted") }
        catch (_: IllegalArgumentException) { }
        model.updateDraft(original.copy(account = 0, transferTarget = 1))
        assertFalse(model.saveEntry(Locale.US, "unused"))
        model.updateDraft(original)
        model.changeDraftAccount(0)
        assertEquals(original, model.draft)
    }
}
