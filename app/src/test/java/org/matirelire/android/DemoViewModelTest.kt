package org.matirelire.android

import java.math.BigDecimal
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class DemoViewModelTest {
    private fun model() = DemoViewModel().apply {
        initialize(listOf(
            DemoTransaction(1, 0, "Income", BigDecimal("100.00"), "Salary", "Transfer", "2026-11-08", true),
            DemoTransaction(2, 0, "Food", BigDecimal("-12.34"), "Food", "Card", "2026-11-08", true),
            DemoTransaction(3, 0, "Pending", BigDecimal("-20.00"), "Food", "Card", "2026-11-08"),
            DemoTransaction(4, 1, "Other account", BigDecimal("30.00"), "Salary", "Transfer", "2026-11-08"),
        ))
    }

    @Test fun clearedBalanceIncludesOpeningAndToggleLeavesTotalUnchanged() {
        val model = model()
        assertEquals(BigDecimal("567.66"), model.balance(0))
        assertEquals(BigDecimal("587.66"), model.balance(0, true))
        model.toggleCleared(3)
        assertEquals(BigDecimal("567.66"), model.balance(0))
        assertEquals(BigDecimal("567.66"), model.balance(0, true))
        model.toggleCleared(3)
        assertEquals(BigDecimal("587.66"), model.balance(0, true))
        assertEquals(BigDecimal("3030.00"), model.balance(1))
        assertEquals(BigDecimal("3000.00"), model.balance(1, true))
    }

    @Test fun referencesAreTrimmedReusableAndNotDuplicated() {
        val model = model()
        assertEquals("Transport", model.addCategory("  Transport  "))
        assertEquals("Transport", model.addCategory("transport"))
        assertEquals(1, model.categories.count { it.equals("transport", true) })
        assertNull(model.addCategory("   "))
        assertEquals("Cash", model.addPayment(" Cash "))
        assertEquals("Cash", model.addPayment("CASH"))
        assertEquals(1, model.payments.count { it.equals("cash", true) })
        assertNull(model.addPayment(""))
    }

    @Test fun reopeningDraftPreservesEveryFieldAndSavesExactlyOnce() {
        val model = model()
        model.beginEntry()
        val category = model.addCategory("Transport")!!
        val payment = model.addPayment("Cash")!!
        val edited = model.draft!!.copy(account = 1, amount = "12,34", income = true,
            description = "Test", category = category, payment = payment)
        model.updateDraft(edited)
        model.beginEntry()
        assertEquals(edited, model.draft)
        val count = model.transactions.size
        assertTrue(model.saveEntry(Locale.FRANCE, "2026-11-08"))
        assertFalse(model.saveEntry(Locale.FRANCE, "2026-11-08"))
        assertEquals(count + 1, model.transactions.size)
        assertNull(model.draft)
        assertEquals(BigDecimal("3042.34"), model.balance(1))
        assertEquals(BigDecimal("3000.00"), model.balance(1, true))
        assertEquals(category, model.transactions.last().category)
        assertEquals(payment, model.transactions.last().payment)
        assertEquals(1, model.account.intValue)
        assertEquals(1, model.destination.intValue)
    }

    @Test fun cancellationAndInvalidInputNeverCreateAnOperation() {
        val model = model()
        val count = model.transactions.size
        model.beginEntry()
        model.updateDraft(model.draft!!.copy(amount = "1,234", description = "Test"))
        assertFalse(model.saveEntry(Locale.FRANCE, "2026-11-08"))
        assertTrue(model.draft!!.attempted)
        assertEquals(count, model.transactions.size)
        model.cancelEntry()
        assertNull(model.draft)
        assertEquals(count, model.transactions.size)
        model.beginEntry()
        assertEquals("", model.draft!!.amount)
    }

    @Test fun newExpenseStartsUnclearedAndReferenceCreationDoesNotResetDraft() {
        val model = model()
        model.beginEntry()
        model.updateDraft(model.draft!!.copy(amount = "12.34", description = "Expense",
            addingCategory = true, newCategory = "Travel"))
        val category = model.addCategory(model.draft!!.newCategory)!!
        model.updateDraft(model.draft!!.copy(category = category, addingCategory = false))
        assertEquals("12.34", model.draft!!.amount)
        assertTrue(model.saveEntry(Locale.US, "2026-11-08"))
        assertEquals(BigDecimal("555.32"), model.balance(0))
        assertEquals(BigDecimal("587.66"), model.balance(0, true))
        assertFalse(model.transactions.last().cleared)
        model.beginEntry()
        assertTrue(model.categories.contains("Travel"))
    }
}
