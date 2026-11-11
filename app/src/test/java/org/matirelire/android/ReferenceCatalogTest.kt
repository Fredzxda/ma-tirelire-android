package org.matirelire.android

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class ReferenceCatalogTest {
    private fun model() = DemoViewModel().apply {
        initialize(listOf(DemoTransaction(1, 0, "Test", BigDecimal("-10.00"), "Food", "Card", "2026-11-11")))
        addCategory("Other"); addPayment("Cash"); ensureTransferPayment("Transfer")
    }
    @Test fun renameAndReplacePreserveSeriesPairsAndIndependentClearing() {
        val m = model()
        m.beginEntry(java.time.LocalDate.of(2026, 11, 11))
        m.updateDraft(m.draft!!.copy(description = "Monthly transfer", amount = "25.00", payment = "Transfer",
            transferTarget = 1, repeatFrequency = RepeatFrequency.MONTH, repeatCount = "3"))
        assertTrue(m.saveEntry(Locale.US, "unused", today = java.time.LocalDate.of(2026, 11, 11)))
        m.toggleCleared(m.transactions.last().id)
        val before = m.transactions.toList()
        val rule = m.rules.single()
        var writes = 0
        m.onChange = { writes++; SnapshotCodec.decode(SnapshotCodec.encode(m.exportData())) }
        assertTrue(m.saveReference(false, "Transfer", "Wire", ReferenceOptions()))
        assertTrue(m.removeReference(true, "Food", "Other"))
        assertEquals(2, writes)
        assertEquals(before.map { it.copy(category = "Other", payment = if (it.payment == "Transfer") "Wire" else it.payment) }, m.transactions.toList())
        assertEquals(rule.copy(category = "Other", payment = "Wire"), m.rules.single())
        m.onChange = null
        assertTrue(m.generateDue(java.time.LocalDate.of(2026, 12, 11)))
        assertTrue(m.transactions.filter { it.seriesId != null }.all { it.category == "Other" && it.payment == "Wire" })
    }
    @Test fun newEntryChangesDirectionAndAccountWithoutKeepingForbiddenReferences() {
        val m = model()
        m.saveReference(true, "Food", "Food", ReferenceOptions(kind = ReferenceKind.EXPENSE, accounts = setOf(0)))
        m.beginEntry()
        assertEquals("Food", m.draft!!.category)
        m.updateDraft(m.draft!!.copy(income = true, amount = "10.00", description = "Test"))
        assertEquals("Other", m.draft!!.category)
        m.updateDraft(m.draft!!.copy(category = "Food"))
        assertFalse(m.saveEntry(Locale.US, "unused"))
        m.updateDraft(m.draft!!.copy(income = false))
        m.changeDraftAccount(1)
        assertEquals("Other", m.draft!!.category)
        assertTrue(m.saveEntry(Locale.US, "unused"))
    }
    @Test fun referenceEditorMemoryIsDiscardedAtLock() {
        val m = model()
        m.referenceUiStates["private"] = "sensitive synthetic label"
        m.clearSensitive()
        assertTrue(m.referenceUiStates.isEmpty())
        assertTrue(m.referenceOptions.isEmpty())
    }
    @Test fun renameRewritesHistoryDraftAndPersistsOnce() {
        val m = model(); m.editEntry(1, Locale.US)
        var calls = 0
        m.onChange = { calls++; SnapshotCodec.encode(m.exportData()) }
        assertTrue(m.saveReference(true, "Food", "Groceries", ReferenceOptions()))
        assertEquals(1, calls)
        assertEquals("Groceries", m.transactions.single().category)
        assertEquals("Groceries", m.draft!!.category)
        assertEquals(BigDecimal("-10.00"), m.transactions.single().amount)
        assertFalse(m.transactions.single().cleared)
    }
    @Test fun cyclesAndDuplicateNamesAreRejectedWithoutMutation() {
        val m = model()
        assertTrue(m.saveReference(true, "Other", "Other", ReferenceOptions(parent = "Food")))
        val before = m.exportData()
        assertFalse(m.saveReference(true, "Food", "Food", ReferenceOptions(parent = "Other")))
        assertFalse(m.saveReference(true, "Food", "other", ReferenceOptions()))
        assertEquals(before, m.exportData())
    }
    @Test fun removeRequiresReplacementWhenUsedAndReparentsChildren() {
        val m = model()
        m.saveReference(true, "Other", "Other", ReferenceOptions(parent = "Food"))
        assertFalse(m.removeReference(true, "Food", null))
        assertTrue(m.removeReference(true, "Food", "Other"))
        assertEquals("Other", m.transactions.single().category)
        assertNull(m.referenceOptions["c:Other"]!!.parent)
        SnapshotCodec.decode(SnapshotCodec.encode(m.exportData()))
    }
    @Test fun selectionHonorsVisibilityKindAndRetainsEditedValue() {
        val m = model()
        assertTrue(m.saveReference(true, "Food", "Food", ReferenceOptions(kind = ReferenceKind.EXPENSE, accounts = setOf(1))))
        assertFalse("Food" in m.referenceChoices(true, 0, false))
        assertTrue("Food" in m.referenceChoices(true, 1, false))
        assertFalse("Food" in m.referenceChoices(true, 1, true))
        assertTrue("Food" in m.referenceChoices(true, 0, true, "Food"))
        assertTrue(m.saveReference(false, "Cash", "Cash", ReferenceOptions(hidden = true)))
        assertFalse("Cash" in m.referenceChoices(false, 0, false))
    }
    @Test fun transferMethodCannotBeRemovedHiddenRestrictedOrUsedAsReplacement() {
        val m = model(); val before = m.exportData()
        assertFalse(m.removeReference(false, "Transfer", "Card"))
        assertFalse(m.removeReference(false, "Card", "Transfer"))
        assertFalse(m.saveReference(false, "Transfer", "Transfer", ReferenceOptions(hidden = true)))
        assertEquals(before, m.exportData())
        assertTrue(m.saveReference(false, "Transfer", "Internal", ReferenceOptions()))
        assertEquals("Internal", m.transferPayment)
    }
    @Test fun newMetadataSurvivesPortableRoundTripAndLegacyDefaultsRemain() {
        val m = model()
        m.saveReference(true, "Other", "Other", ReferenceOptions(parent = "Food", kind = ReferenceKind.INCOME, accounts = setOf(1)))
        val data = m.exportData()
        assertEquals(data, SnapshotCodec.decode(SnapshotCodec.encode(data)))
        val legacy = data.copy(referenceOptions = emptyMap())
        assertTrue(SnapshotCodec.decode(SnapshotCodec.encode(legacy)).referenceOptions.isEmpty())
    }
}
