package org.matirelire.android

import java.math.BigDecimal
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class SplitOperationsTest {
    @Test fun categoriesAreDerivedAndDistinctWithoutChangingStoredMainCategory() {
        val row = DemoTransaction(1, 0, "Fixture", BigDecimal("-10.00"), "Legacy", "Card", "2026-11-17",
            splits = listOf(OperationSplit("Food", "", BigDecimal("3.00")),
                OperationSplit("House", "", BigDecimal("4.00")), OperationSplit("Food", "", BigDecimal("3.00"))))
        assertEquals("Food + House", Splits.categorySummary(row))
        assertEquals("Legacy", row.category)
        assertEquals("Legacy", Splits.categorySummary(row.copy(splits = emptyList())))
    }
    private fun model() = DemoViewModel().apply { initialize(listOf(
        DemoTransaction(1, 0, "Initial", BigDecimal("-10.00"), "Food", "Card", "2026-11-16"))) }
    private fun lines() = listOf(SplitDraft("Food", "First", "4,00"), SplitDraft("Food", "Second", "6,00"))

    @Test fun sumPrecisionAndDirectionAreStrict() {
        assertNotNull(Splits.parse(lines(), BigDecimal("-10.00"), "EUR", Locale.FRANCE, listOf("Food")))
        assertNull(Splits.parse(lines(), BigDecimal("11.00"), "EUR", Locale.FRANCE, listOf("Food")))
        assertNull(Splits.parse(lines().map { it.copy(amount = "5,001") }, BigDecimal("10.00"), "EUR", Locale.FRANCE, listOf("Food")))
        assertNull(Splits.parse(lines().map { it.copy(amount = "-5") }, BigDecimal("10.00"), "EUR", Locale.FRANCE, listOf("Food")))
        assertNotNull(Splits.parse(lines().map { it.copy(amount = "5") }, BigDecimal("10"), "JPY", Locale.FRANCE, listOf("Food")))
        assertNull(Splits.parse(lines(), BigDecimal("10"), "JPY", Locale.FRANCE, listOf("Food")))
        assertNotNull(Splits.parse(listOf(SplitDraft("Food", "", "4,001"), SplitDraft("Food", "", "5,999")), BigDecimal("10.000"), "KWD", Locale.FRANCE, listOf("Food")))
    }
    @Test fun saveEditAndSnapshotPreserveSharesAndBalance() {
        val model = model()
        val balance = model.balance(0)
        model.editEntry(1, Locale.FRANCE)
        model.updateDraft(model.draft!!.copy(splits = lines()))
        assertTrue(model.saveEntry(Locale.FRANCE, "2026-11-16"))
        assertEquals(balance, model.balance(0))
        val data = SnapshotCodec.decode(SnapshotCodec.encode(model.exportData()))
        assertEquals(listOf(BigDecimal("4.00"), BigDecimal("6.00")), data.transactions.single().splits.map { it.amount })
        model.restoreData(data)
        model.editEntry(1, Locale.FRANCE)
        assertEquals(lines(), model.draft!!.splits)
    }
    @Test fun mismatchAndInternalTransferNeverPartiallySave() {
        val model = model()
        model.ensureTransferPayment("Card")
        model.editEntry(1, Locale.FRANCE)
        model.updateDraft(model.draft!!.copy(splits = lines(), amount = "11"))
        assertFalse(model.saveEntry(Locale.FRANCE, "2026-11-16"))
        assertTrue(model.transactions.single().splits.isEmpty())
        model.updateDraft(model.draft!!.copy(amount = "10", transferTarget = 1))
        assertFalse(model.saveEntry(Locale.FRANCE, "2026-11-16"))
        assertEquals(1, model.transactions.size)
    }
    @Test fun recurringTemplateCopiesSharesAndCurrentEditLeavesTemplateIntact() {
        val model = model()
        model.editEntry(1, Locale.FRANCE)
        model.updateDraft(model.draft!!.copy(splits = lines(), repeatFrequency = RepeatFrequency.MONTH))
        assertTrue(model.saveEntry(Locale.FRANCE, "2026-11-16"))
        val rule = model.rules.single()
        assertEquals(model.transactions.single().splits, rule.splits)
        val generated = Recurrences.generate(model.exportData(), java.time.LocalDate.of(2026, 12, 16)).data
        assertEquals(rule.splits, generated.transactions.last().splits)
        model.editEntry(1, Locale.FRANCE)
        model.updateDraft(model.draft!!.copy(splits = lines().map { it.copy(amount = "5,00") }))
        assertTrue(model.saveEntry(Locale.FRANCE, "2026-11-16", SeriesEditScope.CURRENT))
        assertEquals(rule.splits, model.rules.single().splits)
    }
    @Test fun legacySnapshotDefaultsToNoSplitAndRejectsCorruptShares() {
        val data = model().exportData()
        assertTrue(SnapshotCodec.decode(SnapshotCodec.encode(data)).transactions.single().splits.isEmpty())
        try {
            SnapshotCodec.encode(data.copy(transactions = data.transactions.map { it.copy(splits = listOf(
                OperationSplit("Food", "", BigDecimal("4.00")), OperationSplit("Food", "", BigDecimal("7.00")))) }))
            fail("Invalid shares accepted")
        } catch (_: IllegalArgumentException) { }
    }
    @Test fun searchFindsSplitCategoryAndDescriptionWithoutDuplicatingRows() {
        val row = DemoTransaction(1, 0, "Purchase", BigDecimal("-10.00"), "Misc", "Card", "2026-11-16",
            splits = listOf(OperationSplit("Épicerie", "Pommes", BigDecimal("4.00")), OperationSplit("Loisirs", "Livre", BigDecimal("6.00"))))
        assertEquals(listOf(row), TransactionSearch.filter(listOf(row), "epicerie pommes"))
        assertEquals(listOf(row), TransactionSearch.filter(listOf(row), "livre"))
        assertTrue(TransactionSearch.filter(listOf(row), "missing").isEmpty())
    }
    @Test fun foreignOperationSharesUseNativeAccountAmount() {
        val model = model()
        model.editEntry(1, Locale.FRANCE)
        model.updateDraft(model.draft!!.copy(amount = "100", inputCurrency = "USD", fxRate = "0,9",
            splits = listOf(SplitDraft("Food", "First", "40"), SplitDraft("Food", "Second", "50"))))
        assertTrue(model.saveEntry(Locale.FRANCE, "2026-11-16"))
        val row = model.transactions.single()
        assertEquals(BigDecimal("-90.00"), row.amount)
        assertEquals(BigDecimal("-100.00"), row.originalAmount)
        assertEquals(BigDecimal("90.00"), row.splits.fold(BigDecimal.ZERO) { sum, line -> sum + line.amount })
        assertEquals(row, SnapshotCodec.decode(SnapshotCodec.encode(model.exportData())).transactions.single())
    }
    @Test fun seriesScopeChangesFutureTemplateAndLeavesOtherGeneratedSharesIntact() {
        val model = model()
        model.editEntry(1, Locale.FRANCE)
        model.updateDraft(model.draft!!.copy(splits = lines(), repeatFrequency = RepeatFrequency.MONTH))
        assertTrue(model.saveEntry(Locale.FRANCE, "2026-11-16"))
        model.restoreData(Recurrences.generate(model.exportData(), java.time.LocalDate.of(2026, 12, 16)).data)
        val generated = model.transactions.last()
        model.editEntry(1, Locale.FRANCE)
        model.updateDraft(model.draft!!.copy(splits = lines().map { it.copy(amount = "5,00") }))
        assertTrue(model.saveEntry(Locale.FRANCE, "2026-11-16", SeriesEditScope.SERIES))
        assertEquals(listOf(BigDecimal("5.00"), BigDecimal("5.00")), model.rules.single().splits.map { it.amount })
        assertEquals(generated, model.transactions.first { it.id == generated.id })
    }
}
