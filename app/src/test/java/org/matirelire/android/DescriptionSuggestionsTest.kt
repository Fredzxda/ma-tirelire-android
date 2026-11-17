package org.matirelire.android

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class DescriptionSuggestionsTest {
    private fun row(id: Long, description: String = "Épicerie", category: String = "Food", payment: String = "Card") =
        DemoTransaction(id, 0, description, BigDecimal("-10.00"), category, payment, "2026-11-17")
    private fun model() = DemoViewModel().apply {
        initialize(listOf(row(1), row(2, category = "Other", payment = "Cash").copy(date = "2025-01-01")))
        ensureAccountNames(listOf("Current", "Savings"))
        ensureTransferPayment("Transfer")
        beginEntry(LocalDate.of(2026, 11, 17))
    }
    @Test fun newEntryIsEmptyAndExactDescriptionUsesLatestEntryDespiteBackdatedOperation() {
        val m = model()
        assertEquals("", m.draft!!.category)
        assertEquals("", m.draft!!.payment)
        m.changeDescription("epicerie"); m.completeDescription()
        assertEquals("Other", m.draft!!.category)
        assertEquals("Cash", m.draft!!.payment)
        assertEquals("", m.draft!!.amount)
        assertEquals("2026-11-17", m.draft!!.date)
        assertNull(m.draft!!.transferTarget)
        assertEquals(RepeatFrequency.NONE, m.draft!!.repeatFrequency)
    }
    @Test fun suggestionsAreUniqueBoundedAndScopedToSameAccountDirectionAndConfirmedHistory() {
        val rows = listOf(row(1), row(2), row(3, "Épicerie other account").copy(account = 1),
            row(4, "Épicerie income").copy(amount = BigDecimal.TEN),
            row(5, "Épicerie forecast").copy(seriesId = "series", amountConfirmed = false)) +
            (6L..15L).map { row(it, "Épicerie $it") }
        val suggestions = DescriptionSuggestions.find(rows, EntryDraft(0), "epi")
        assertEquals(5, suggestions.size)
        assertEquals(listOf(15L, 14L, 13L, 12L, 11L), suggestions.map { it.id })
        assertEquals(1, DescriptionSuggestions.find(rows.take(5), EntryDraft(0), "epi").size)
        assertTrue(DescriptionSuggestions.find(rows, EntryDraft(0), "").isEmpty())
    }
    @Test fun selectingSuggestionNeverOverwritesReferencesOrCopiesFinancialFields() {
        val m = model()
        m.updateDraft(m.draft!!.copy(category = "Food", payment = "Card", amount = "25", repeatFrequency = RepeatFrequency.MONTH))
        m.changeDescription("epi")
        m.selectDescription(2)
        assertEquals("Épicerie", m.draft!!.description)
        assertEquals("Food", m.draft!!.category)
        assertEquals("Card", m.draft!!.payment)
        assertEquals("25", m.draft!!.amount)
        assertEquals(RepeatFrequency.MONTH, m.draft!!.repeatFrequency)
        assertTrue(m.draft!!.splits.isEmpty())
    }
    @Test fun hiddenOrWrongKindReferencesAreNeverSuggestedAndExternalTransferDoesNotSelectTarget() {
        val m = model()
        m.referenceOptions[ReferenceCatalog.key(true, "Other")] = ReferenceOptions(hidden = true)
        m.referenceOptions[ReferenceCatalog.key(false, "Cash")] = ReferenceOptions(kind = ReferenceKind.INCOME)
        m.changeDescription("epicerie"); m.completeDescription()
        assertEquals("", m.draft!!.category)
        assertEquals("", m.draft!!.payment)
        m.transactions.add(row(3, "Internal", payment = "Transfer"))
        m.changeDescription("Internal"); m.completeDescription()
        assertEquals("Transfer", m.draft!!.payment)
        assertNull(m.draft!!.transferTarget)
    }
    @Test fun hiddenForecastsAndSplitTechnicalCategoryAreNotUsedAsDefaults() {
        val m = model()
        m.deferredForecasts.add(row(20, "Hidden"))
        m.changeDescription("Hidden")
        assertTrue(m.descriptionSuggestions().isEmpty())
        m.transactions.add(row(21, "Split").copy(splits = listOf(
            OperationSplit("Food", "", BigDecimal("4.00")), OperationSplit("Other", "", BigDecimal("6.00")))))
        m.changeDescription("Split"); m.completeDescription()
        assertEquals("", m.draft!!.category)
        assertEquals("Card", m.draft!!.payment)
        assertTrue(m.draft!!.splits.isEmpty())
    }
    @Test fun intermediateExactPrefixDoesNotFillBeforeEndOfTyping() {
        val m = model()
        m.transactions.add(row(3, "Car", "Food", "Card"))
        m.transactions.add(row(4, "Carrefour", "Other", "Cash"))
        m.changeDescription("Car")
        assertEquals("", m.draft!!.category)
        m.changeDescription("Carrefour")
        m.completeDescription()
        assertEquals("Other", m.draft!!.category)
        assertEquals("Cash", m.draft!!.payment)
    }

    @Test fun saveCompletesExactDescriptionEvenWithoutFocusChange() {
        val m = model()
        m.changeDescription("epicerie")
        m.updateDraft(m.draft!!.copy(amount = "10"))
        assertTrue(m.saveEntry(java.util.Locale.US, "unused"))
        assertEquals("Other", m.transactions.last().category)
        assertEquals("Cash", m.transactions.last().payment)
    }

    @Test fun splitDoesNotRequireASeparateMainCategory() {
        val m = model()
        m.updateDraft(m.draft!!.copy(description = "New split", amount = "10", payment = "Card", splits = listOf(
            SplitDraft("Other", "", "4"), SplitDraft("Food", "", "6"))))
        assertEquals("", m.draft!!.category)
        assertTrue(m.saveEntry(java.util.Locale.US, "unused"))
        assertEquals("Other", m.transactions.last().category)
        assertEquals(listOf("Other", "Food"), m.transactions.last().splits.map { it.category })
    }

    @Test fun editedOperationCannotSuggestItselfAndExactMatchIsNotHiddenByPrefixLimit() {
        val rows = listOf(row(1, "Shop")) + (2L..30L).map { row(it, "Shop $it") }
        assertEquals(1L, DescriptionSuggestions.exact(rows, EntryDraft(0), "shop")!!.id)
        assertNull(DescriptionSuggestions.exact(rows, EntryDraft(0, editingId = 1), "shop"))
    }
}
