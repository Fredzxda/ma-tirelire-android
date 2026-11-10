package org.matirelire.android

import java.math.BigDecimal
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test
import org.matirelire.android.domain.Money

class AccountsTest {
    private fun model() = DemoViewModel().apply {
        initialize(listOf(DemoTransaction(1, 0, "Fixture", BigDecimal("-12.00"), "Food", "Card", "date", true)))
        ensureAccountNames(listOf("Current", "Savings"))
    }

    @Test fun createAndEditAnAccountPreservesTransactionsAndBothBalances() {
        val model = model()
        model.beginAccount(null, Locale.FRANCE)
        model.updateAccountDraft(model.accountDraft!!.copy(name = "  New account  ", opening = "-25,50"))
        assertTrue(model.saveAccount(Locale.FRANCE))
        assertEquals("New account", model.accountNames[2])
        assertEquals(BigDecimal("-25.50"), model.balance(2))
        model.account.intValue = 2
        model.beginEntry()
        model.updateDraft(model.draft!!.copy(amount = "10,00", description = "Expense"))
        assertTrue(model.saveEntry(Locale.FRANCE, "date"))
        model.toggleCleared(model.transactions.last().id)
        val rows = model.transactions.toList()
        model.beginAccount(2, Locale.FRANCE)
        model.updateAccountDraft(model.accountDraft!!.copy(name = "Renamed", opening = "0"))
        assertTrue(model.saveAccount(Locale.FRANCE))
        assertEquals(rows, model.transactions.toList())
        assertEquals(BigDecimal("-10.00"), model.balance(2))
        assertEquals(BigDecimal("-10.00"), model.balance(2, true))
        val reopened = DemoViewModel()
        reopened.restoreData(SnapshotCodec.decode(SnapshotCodec.encode(model.exportData())))
        assertEquals(model.exportData(), reopened.exportData())
        assertEquals(BigDecimal("-10.00"), reopened.balance(2))
    }

    @Test fun invalidAccountsCannotChangeTheLedgerAndLockClearsTheForm() {
        val model = model()
        val before = model.exportData()
        for ((name, value) in listOf("current" to "0", "" to "0", "Other" to "1,234", "Other" to "NaN")) {
            model.beginAccount(null, Locale.FRANCE)
            model.updateAccountDraft(model.accountDraft!!.copy(name = name, opening = value))
            assertFalse(model.saveAccount(Locale.FRANCE))
            assertEquals(before, model.exportData())
        }
        model.clearSensitive()
        assertNull(model.accountDraft)
        assertTrue(model.accountNames.isEmpty())
    }

    @Test fun oldArchivesMigrateAndInvalidReferencesAndNamesAreRefused() {
        val old = model().exportData().copy(accountNames = emptyList())
        val oldArchive = SnapshotCodec.encode(old)
        assertTrue(oldArchive.toString(Charsets.UTF_8).contains("\"schema\":1"))
        val restored = DemoViewModel()
        restored.restoreData(SnapshotCodec.decode(oldArchive))
        restored.ensureAccountNames(listOf("Current", "Savings"))
        assertEquals(old.transactions, restored.transactions.toList())
        val data = restored.exportData()
        assertTrue(SnapshotCodec.encode(data).toString(Charsets.UTF_8).contains("\"schema\":2"))
        for (bad in listOf(data.copy(accountNames = listOf("Same", "same")),
            data.copy(accountNames = listOf("Only one")),
            data.copy(transactions = data.transactions.map { it.copy(account = 99) }))) {
            try { SnapshotCodec.encode(bad); fail("Invalid account state accepted") }
            catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun accountLimitAndSignedLocaleAmountsAreStrict() {
        val model = model()
        for (i in 2 until 50) {
            model.beginAccount(null, Locale.US)
            model.updateAccountDraft(model.accountDraft!!.copy(name = "Account $i"))
            assertTrue(model.saveAccount(Locale.US))
        }
        model.beginAccount(null, Locale.US)
        model.updateAccountDraft(model.accountDraft!!.copy(name = "Too many"))
        assertFalse(model.saveAccount(Locale.US))
        assertEquals(50, model.accountNames.size)
        assertEquals(BigDecimal("-12.34"), Money.parseOpening("-12,34", Locale.FRANCE))
        assertEquals(BigDecimal("0.00"), Money.parseOpening("0", Locale.US))
        assertNull(Money.parseOpening("12,34", Locale.US))
        assertNull(Money.parseOpening("1e3", Locale.US))
        assertNull(Money.parseOpening("1000000000000", Locale.US))
    }
}
