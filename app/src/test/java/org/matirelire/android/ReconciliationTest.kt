package org.matirelire.android

import java.math.BigDecimal
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class ReconciliationTest {
    private fun model() = DemoViewModel().apply {
        initialize(listOf(
            DemoTransaction(1, 0, "Cleared", BigDecimal("-10.00"), "Food", "Card", "date", true),
            DemoTransaction(2, 0, "Pending", BigDecimal("-20.00"), "Food", "Card", "date"),
            DemoTransaction(3, 1, "Other", BigDecimal("50.00"), "Food", "Card", "date", true)))
    }

    @Test fun provisionalSelectionDoesNotWriteAndConfirmationCommitsOnlyThisAccount() {
        val model = model()
        var writes = 0
        model.onChange = { writes++ }
        val before = model.transactions.toList()
        val total = model.balance(0)
        model.beginReconciliation(0)
        model.reconciliationTarget("470,00")
        assertEquals(BigDecimal("-20.00"), model.reconciliationDifference(Locale.FRANCE))
        assertFalse(model.confirmReconciliation(Locale.FRANCE))
        model.reconciliationToggle(2)
        assertEquals(BigDecimal("0.00"), model.reconciliationDifference(Locale.FRANCE))
        assertEquals(before, model.transactions.toList())
        assertEquals(0, writes)
        assertTrue(model.confirmReconciliation(Locale.FRANCE))
        assertEquals(1, writes)
        assertEquals(BigDecimal("470.00"), model.balance(0, true))
        assertEquals(total, model.balance(0))
        assertEquals(before.last(), model.transactions.last())
        val restored = SnapshotCodec.decode(SnapshotCodec.encode(model.exportData()))
        assertTrue(restored.transactions.first { it.id == 2L }.cleared)
        assertFalse(model.confirmReconciliation(Locale.FRANCE))
        assertEquals(1, writes)
    }

    @Test fun cancellationLockAndForeignIdsNeverChangeClearing() {
        val model = model()
        val before = model.transactions.toList()
        model.beginReconciliation(0)
        val session = model.reconciliation
        model.reconciliationToggle(3)
        assertEquals(session, model.reconciliation)
        model.reconciliationToggle(1)
        model.cancelReconciliation()
        assertEquals(before, model.transactions.toList())
        model.beginReconciliation(0)
        model.clearSensitive()
        assertNull(model.reconciliation)
    }

    @Test fun invalidOrStaleSessionCannotBeConfirmed() {
        val model = model()
        model.beginReconciliation(0)
        for (target in listOf("", "NaN", "1,234")) {
            model.reconciliationTarget(target)
            assertNull(model.reconciliationDifference(Locale.FRANCE))
            assertFalse(model.confirmReconciliation(Locale.FRANCE))
        }
        model.reconciliationTarget("490.00")
        model.toggleCleared(2)
        assertFalse(model.confirmReconciliation(Locale.US))
    }

    @Test fun zeroAndNegativeStatementBalancesUseExactDecimalArithmetic() {
        val model = model()
        model.restoreData(model.exportData().copy(openings = listOf(BigDecimal("10.00"), BigDecimal("0.00"))))
        model.beginReconciliation(0)
        model.reconciliationTarget("0")
        assertTrue(model.confirmReconciliation(Locale.US))
        model.beginReconciliation(0)
        model.reconciliationToggle(2)
        model.reconciliationTarget("-20,00")
        assertTrue(model.confirmReconciliation(Locale.FRANCE))
        assertEquals(BigDecimal("-20.00"), model.balance(0, true))
    }
}
