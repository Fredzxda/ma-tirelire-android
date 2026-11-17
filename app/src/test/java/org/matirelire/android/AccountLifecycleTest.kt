package org.matirelire.android

import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class AccountLifecycleTest {
    private val today = LocalDate.now()
    private fun model() = DemoViewModel().apply {
        initialize(listOf(DemoTransaction(1, 0, "Fixture", BigDecimal("-10.00"), "Food", "Card", today.toString(), true)))
        ensureAccountNames(listOf("Current", "Savings")); ensureTransferPayment("Transfer")
    }
    private fun addUsd(m: DemoViewModel): Int {
        m.beginAccount(null, Locale.US)
        m.updateAccountDraft(m.accountDraft!!.copy(name = "USD account", currency = "USD", opening = "0"))
        assertTrue(m.saveAccount(Locale.US)); return m.openings.lastIndex
    }
    private fun series(m: DemoViewModel, target: Int = 1, income: Boolean = false) {
        m.account.intValue = 0; m.beginEntryWithReferences(today)
        m.updateDraft(m.draft!!.copy(income = income, amount = "10", description = "Fixture monthly", payment = "Transfer", transferTarget = target,
            repeatFrequency = RepeatFrequency.MONTH, repeatCount = "3"))
        assertTrue(m.saveEntry(Locale.US, "unused", today = today))
    }
    @Test fun requestCancelAndStaleIdentityNeverChangeLedger() {
        val m = model(); val before = m.exportData(); var writes = 0; m.onChange = { writes++ }
        m.requestAccountAction(0, AccountActionKind.ARCHIVE)
        assertEquals(before, m.exportData()); assertEquals(0, writes)
        m.pendingAccountAction = null
        assertEquals(before, m.exportData())
        m.requestAccountAction(0, AccountActionKind.DELETE)
        m.accountNames[0] = "Different account"
        assertFalse(m.confirmAccountAction()); assertEquals(0, writes)
    }
    @Test fun archiveKeepsRowsAndBalancesExternalizesTransferAndDoesNotResumeVoluntaryPause() {
        val m = model(); series(m)
        val rows = m.transactions.toList(); val balance = m.balance(0); var writes = 0
        m.onChange = { writes++; SnapshotCodec.encode(m.exportData()) }
        assertTrue(m.archiveAccount(0)); assertEquals(1, writes)
        assertEquals(rows, m.transactions.toList()); assertEquals(balance, m.balance(0))
        assertTrue(m.rules.single().active); assertNull(m.rules.single().target)
        assertEquals(1, m.rules.single().account); assertEquals(m.balance(1), m.consolidated())
        m.setSeriesActive(m.rules.single().id, false)
        m.beginAccount(0, Locale.US)
        m.updateAccountDraft(m.accountDraft!!.copy(name = "Pending rename"))
        assertTrue(m.unarchiveAccount(0)); assertFalse(m.rules.single().active)
        assertEquals("Pending rename", m.accountDraft!!.name); assertEquals("Current", m.accountNames[0])
        assertEquals(rows, m.transactions.toList())
    }
    @Test fun archivedRowsCannotBeEditedDeletedOrPointedAndActiveCounterpartKeepsIndependentPointing() {
        val m = model(); series(m); m.archiveAccount(1)
        val archived = m.transactions.single { it.account == 1 }; val source = m.transactions.single { it.id == archived.peerId }
        val before = m.transactions.toList()
        m.editEntry(archived.id, Locale.US); assertNull(m.draft)
        m.editEntry(source.id, Locale.US); assertNull(m.draft)
        m.deleteEntry(source.id); m.toggleCleared(archived.id); assertEquals(before, m.transactions.toList())
        m.toggleCleared(source.id); assertTrue(m.transactions.single { it.id == source.id }.cleared)
        assertFalse(m.transactions.single { it.id == archived.id }.cleared)
        m.account.intValue = 1; m.beginEntryWithReferences(today); assertNull(m.draft)
    }
    @Test fun allAccountsCanBeArchivedWithoutInvalidSelectionOrTotal() {
        val m = model(); series(m); assertTrue(m.archiveAccount(0)); assertTrue(m.archiveAccount(1))
        assertTrue(m.activeAccountIndices().isEmpty()); assertEquals(BigDecimal("0.00"), m.consolidated())
        assertTrue(m.account.intValue in m.openings.indices)
        assertEquals(m.exportData(), SnapshotCodec.decode(SnapshotCodec.encode(m.exportData())))
        m.unarchiveAccount(0); m.unarchiveAccount(1)
        assertNull(m.rules.single().target); assertFalse(m.rules.single().active)
    }
    @Test fun deletingAccountKeepsOtherCounterpartsAndForecastStatesAndContinuesExternalSeries() {
        val m = model(); val usd = addUsd(m); m.setLocalRate("USD", "0.8", Locale.US); series(m, usd)
        assertTrue(m.generateDue(today.plusMonths(1)))
        val survivorRows = m.transactions.filter { it.account == usd }; val usdBalance = m.balance(usd)
        m.toggleCleared(survivorRows.first().id)
        val original = m.transactions.filter { it.account == usd }; var writes = 0
        m.onChange = { writes++; SnapshotCodec.encode(m.exportData()) }
        assertTrue(m.deleteAccount(0)); assertEquals(1, writes)
        assertEquals(usdBalance, m.balance(usd - 1)); assertEquals("USD", m.currencyCode(usd - 1))
        assertEquals(original.map { it.copy(account = usd - 1, peerId = null, exchangeAmount = null) }, m.transactions.toList())
        assertTrue(m.rules.single().active); assertNull(m.rules.single().target)
        assertEquals(usd - 1, m.rules.single().account)
        assertFalse(m.transactions.single { it.occurrence == 1 }.amountConfirmed)
        assertTrue(m.generateDue(today.plusMonths(2)))
        val future = m.transactions.single { it.occurrence == 2 }
        assertEquals(usd - 1, future.account); assertNull(future.peerId)
        assertEquals(BigDecimal("12.50"), future.amount)
        assertEquals(m.exportData(), SnapshotCodec.decode(SnapshotCodec.encode(m.exportData())))
    }
    @Test fun deletingReceiverKeepsSourceBalanceAndDoesNotLeaveInvalidRuleReferences() {
        val m = model(); series(m); val before = m.balance(0)
        assertTrue(m.deleteAccount(1)); assertEquals(before, m.balance(0))
        assertTrue(m.transactions.all { it.account == 0 && it.peerId == null })
        assertTrue(m.rules.single().active); assertNull(m.rules.single().target)
        assertFalse(m.deleteAccount(0)); assertEquals(1, m.openings.size)
        SnapshotCodec.decode(SnapshotCodec.encode(m.exportData()))
    }
    @Test fun deletionRemapsVisibilityDraftCurrencyAndArchivedIndices() {
        val m = model(); val usd = addUsd(m)
        m.saveReference(true, "Food", "Food", ReferenceOptions(accounts = setOf(1)))
        m.archiveAccount(usd)
        m.account.intValue = 0; m.beginEntryWithReferences(today)
        m.updateDraft(m.draft!!.copy(amount = "1", description = "Draft"))
        assertTrue(m.deleteAccount(1))
        assertEquals(setOf(1), m.archivedAccounts.toSet()); assertEquals("USD", m.currencyCode(1))
        assertTrue(m.referenceOptions["c:Food"]!!.hidden)
        assertNotNull(m.draft); assertEquals(0, m.draft!!.account)
        SnapshotCodec.decode(SnapshotCodec.encode(m.exportData()))
    }
    @Test fun editingOldOrdinaryOccurrenceContinuesOnlyOnActiveAccountAfterExternalization() {
        val m = model(); series(m)
        val pair = m.transactions.filter { it.seriesId != null }
        val source = pair.single { it.account == 0 }
        val data = m.exportData()
        m.restoreData(data.copy(transactions = data.transactions.filter { it.id != source.peerId }.map {
            if (it.id == source.id) it.copy(peerId = null, exchangeAmount = null) else it
        }, rules = listOf(m.rules.single().copy(count = 1))))
        m.archiveAccount(1)
        m.editEntry(source.id, Locale.US)
        m.updateDraft(m.draft!!.copy(repeatCount = "2"))
        assertTrue(m.seriesOnlyEdit(Locale.US))
        assertTrue(m.saveEntry(Locale.US, "unused", today = today))
        assertNull(m.rules.single().target)
        assertTrue(m.generateDue(today.plusMonths(2)))
        assertTrue(m.transactions.filter { it.occurrence != null }.all { it.account == 0 && it.peerId == null })
        SnapshotCodec.decode(SnapshotCodec.encode(m.exportData()))
    }
    @Test fun archivalUsesCurrentFutureNativeAmountAndPreservesIncomingTransferSign() {
        val m = model(); val usd = addUsd(m); m.setLocalRate("USD", "0.8", Locale.US)
        series(m, usd, income = true)
        m.rules[0] = m.rules.single().copy(amount = BigDecimal("20.00"), targetAmount = BigDecimal("-25.00"))
        val historical = m.transactions.toList()
        assertTrue(m.archiveAccount(0)); assertEquals(historical, m.transactions.toList())
        assertEquals(BigDecimal("-25.00"), m.rules.single().amount)
        assertTrue(m.rules.single().active); assertEquals(usd, m.rules.single().account); assertNull(m.rules.single().target)
        assertTrue(m.generateDue(today.plusMonths(1)))
        val future = m.transactions.single { it.occurrence == 1 }
        assertEquals(usd, future.account); assertEquals(BigDecimal("-25.00"), future.amount); assertNull(future.peerId)
        assertTrue(m.unarchiveAccount(0)); assertNull(m.rules.single().target)
        assertTrue(m.rules.single().active)
        assertFalse(m.generateDue(today.plusMonths(1)))
    }
    @Test fun deletionUsesFutureTemplateRatherThanFirstHistoricalAmountAndKeepsPausedState() {
        val m = model(); val usd = addUsd(m); m.setLocalRate("USD", "0.8", Locale.US); series(m, usd)
        m.rules[0] = m.rules.single().copy(amount = BigDecimal("-30.00"), targetAmount = BigDecimal("37.50"))
        m.setSeriesActive(m.rules.single().id, false)
        assertTrue(m.deleteAccount(0))
        assertEquals(BigDecimal("37.50"), m.rules.single().amount)
        assertEquals(usd - 1, m.rules.single().account); assertNull(m.rules.single().target)
        assertFalse(m.rules.single().active); assertFalse(m.generateDue(today.plusMonths(1)))
        m.setSeriesActive(m.rules.single().id, true)
        assertTrue(m.generateDue(today.plusMonths(1)))
        assertEquals(BigDecimal("37.50"), m.transactions.single { it.occurrence == 1 }.amount)
    }
    @Test fun archivingThenDeletingCurrencyTargetKeepsSourceFutureAmountAndDoesNotDuplicate() {
        val m = model(); val usd = addUsd(m); m.setLocalRate("USD", "0.8", Locale.US); series(m, usd, income = true)
        m.rules[0] = m.rules.single().copy(amount = BigDecimal("20.00"), targetAmount = BigDecimal("-25.00"))
        val balance = m.balance(0)
        m.archiveAccount(usd)
        assertEquals(BigDecimal("20.00"), m.rules.single().amount); assertEquals(0, m.rules.single().account)
        assertNull(m.rules.single().target); assertTrue(m.rules.single().active)
        assertTrue(m.deleteAccount(usd)); assertEquals(balance, m.balance(0))
        assertTrue(m.generateDue(today.plusMonths(1)))
        assertEquals(BigDecimal("20.00"), m.transactions.single { it.occurrence == 1 }.amount)
        assertFalse(m.generateDue(today.plusMonths(1)))
        assertEquals(1, m.transactions.count { it.occurrence == 1 })
    }
    @Test fun oldArchivedInternalSeriesIsMigratedWithoutAutomaticReactivation() {
        val m = model(); series(m); val data = m.exportData()
        val old = data.copy(archivedAccounts = setOf(1), rules = data.rules.map { it.copy(active = false) })
        val migrated = SnapshotCodec.decode(SnapshotCodec.encode(old))
        assertEquals(old.transactions, migrated.transactions)
        assertEquals(0, migrated.rules.single().account); assertNull(migrated.rules.single().target)
        assertFalse(migrated.rules.single().active)
        m.restoreData(migrated); m.setSeriesActive(m.rules.single().id, true)
        assertTrue(m.rules.single().active)
        assertTrue(m.generateDue(today.plusMonths(1)))
        assertEquals(1, m.transactions.count { it.occurrence == 1 })
        assertEquals(0, m.transactions.single { it.occurrence == 1 }.account)
        assertEquals(m.exportData(), SnapshotCodec.decode(SnapshotCodec.encode(m.exportData())))
    }
    @Test fun ordinarySeriesStillStopsWhenItsOnlyAccountIsArchived() {
        val m = model(); m.beginEntryWithReferences(today)
        m.updateDraft(m.draft!!.copy(amount = "10", description = "Ordinary", repeatFrequency = RepeatFrequency.MONTH, repeatCount = "3"))
        assertTrue(m.saveEntry(Locale.US, "unused", today = today))
        m.archiveAccount(0)
        assertFalse(m.rules.single().active); assertFalse(m.generateDue(today.plusMonths(1)))
        m.unarchiveAccount(0); assertFalse(m.rules.single().active)
    }
    @Test fun archiveMetadataRoundTripsAndMalformedReferencesAreRejected() {
        val m = model(); val old = m.exportData()
        assertTrue(SnapshotCodec.decode(SnapshotCodec.encode(old)).archivedAccounts.isEmpty())
        series(m); val active = m.exportData()
        assertThrows(IllegalArgumentException::class.java) { SnapshotCodec.encode(active.copy(archivedAccounts = setOf(0))) }
        assertThrows(IllegalArgumentException::class.java) { SnapshotCodec.encode(old.copy(archivedAccounts = setOf(9))) }
        m.archiveAccount(1)
        assertEquals(m.exportData(), SnapshotCodec.decode(SnapshotCodec.encode(m.exportData())))
    }
}
