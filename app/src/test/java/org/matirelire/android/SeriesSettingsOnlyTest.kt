package org.matirelire.android

import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class SeriesSettingsOnlyTest {
    private fun model() = DemoViewModel().apply {
        initialize(listOf(DemoTransaction(1, 0, "Fixture", BigDecimal("-1.00"), "Food", "Card", "2026-01-01", true)))
        ensureAccountNames(listOf("Current", "Savings")); ensureTransferPayment("Transfer")
        beginEntry(LocalDate.of(2026, 1, 5))
        updateDraft(draft!!.copy(amount = "10", description = "Original", repeatFrequency = RepeatFrequency.MONTH))
        assertTrue(saveEntry(Locale.US, "unused"))
        generateDue(LocalDate.of(2026, 3, 5))
    }

    @Test fun endDateOnlySavesSeriesWithoutConfirmingOrChangingAnyRecordedRow() {
        val model = model()
        val rows = model.transactions.toList()
        val before = model.rules.single()
        val pending = rows.first { it.occurrence == 1 }
        assertFalse(pending.amountConfirmed)
        model.editEntry(pending.id, Locale.FRANCE)
        model.updateDraft(model.draft!!.copy(repeatUntil = "2026-12-31"))
        assertTrue(model.seriesOnlyEdit(Locale.FRANCE))
        assertTrue(model.saveEntry(Locale.FRANCE, "unused"))
        assertEquals(rows, model.transactions.toList())
        assertEquals(before.copy(until = "2026-12-31"), model.rules.single())
    }

    @Test fun limitsDoNotReplaceFutureTemplateWithHistoricalAmountAndDescription() {
        val model = model()
        val data = model.exportData()
        model.restoreData(data.copy(rules = data.rules.map { it.copy(amount = BigDecimal("-99.00"), description = "Future") }))
        val rows = model.transactions.toList()
        model.editEntry(rows.first { it.occurrence == 0 }.id, Locale.US)
        model.updateDraft(model.draft!!.copy(repeatCount = "2"))
        assertTrue(model.seriesOnlyEdit(Locale.US))
        assertTrue(model.saveEntry(Locale.US, "unused"))
        assertEquals(rows, model.transactions.toList())
        assertEquals(BigDecimal("-99.00"), model.rules.single().amount)
        assertEquals("Future", model.rules.single().description)
        assertEquals(model.rules.single().next + 2, model.rules.single().count)
    }

    @Test fun frequencyOnlyRebasesCalendarButLeavesRowsAndTemplateUntouched() {
        val model = model()
        val rows = model.transactions.toList()
        model.editEntry(rows.last().id, Locale.US)
        model.updateDraft(model.draft!!.copy(repeatFrequency = RepeatFrequency.WEEK))
        assertTrue(model.seriesOnlyEdit(Locale.US))
        assertTrue(model.saveEntry(Locale.US, "unused", today = LocalDate.of(2026, 3, 10)))
        assertEquals(rows, model.transactions.toList())
        assertEquals(RepeatFrequency.WEEK, model.rules.single().frequency)
        assertEquals(BigDecimal("-10.00"), model.rules.single().amount)
    }

    @Test fun commonFieldsStillRequireScopeAndInvalidSeriesOnlySettingsNeverSave() {
        val model = model()
        model.editEntry(model.transactions.last().id, Locale.US)
        val original = model.draft!!
        model.updateDraft(original.copy(amount = "20", repeatUntil = "2026-12-31"))
        assertFalse(model.seriesOnlyEdit(Locale.US))
        model.updateDraft(original.copy(date = "2026-03-06"))
        assertFalse(model.seriesOnlyEdit(Locale.US))
        model.updateDraft(original.copy(repeatCount = "bad"))
        assertTrue(model.seriesOnlyEdit(Locale.US))
        val before = model.exportData().copy(draft = null)
        assertFalse(model.saveEntry(Locale.US, "unused"))
        assertEquals(before, model.exportData().copy(draft = null))
    }
}
