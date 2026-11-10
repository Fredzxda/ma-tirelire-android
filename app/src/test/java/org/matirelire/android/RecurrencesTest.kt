package org.matirelire.android

import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class RecurrencesTest {
    private fun model() = DemoViewModel().apply {
        initialize(listOf(DemoTransaction(1, 0, "Fixture", BigDecimal("-1.00"), "Food", "Card", "2026-01-01", true)))
        ensureAccountNames(listOf("Current", "Savings"))
        ensureTransferPayment("Transfer")
    }
    private fun series(model: DemoViewModel, transfer: Boolean = false, count: String = "3", until: String = "") {
        model.beginEntry(LocalDate.of(2026, 1, 31))
        model.updateDraft(model.draft!!.copy(description = "Monthly", amount = "10.00", repeatFrequency = RepeatFrequency.MONTH,
            repeatCount = count, repeatUntil = until, payment = if (transfer) "Transfer" else "Card",
            transferTarget = if (transfer) 1 else null))
        assertTrue(model.saveEntry(Locale.US, "unused"))
    }

    @Test fun monthAnchorsMonthEndAndLeapYearsDoNotDrift() {
        val model = model(); series(model)
        val rule = model.rules.single()
        assertEquals(LocalDate.of(2026, 2, 28), rule.dateAt(1))
        assertEquals(LocalDate.of(2026, 3, 31), rule.dateAt(2))
        assertEquals(LocalDate.of(2026, 4, 30), rule.dateAt(3))
        assertEquals(LocalDate.of(2026, 2, 28), rule.copy(anchor = "2026-01-20", frequency = RepeatFrequency.MONTH_END).dateAt(1))
        val leap = rule.copy(anchor = "2024-02-29", frequency = RepeatFrequency.YEAR)
        assertEquals(LocalDate.of(2025, 2, 28), leap.dateAt(1))
        assertEquals(LocalDate.of(2028, 2, 29), leap.dateAt(4))
        assertEquals(LocalDate.of(2026, 2, 14), rule.copy(frequency = RepeatFrequency.TWO_WEEKS).dateAt(1))
        assertEquals(LocalDate.of(2028, 1, 31), rule.copy(frequency = RepeatFrequency.TWO_YEARS).dateAt(1))
    }

    @Test fun catchUpPersistsOnceAndRestoringCannotDuplicateOccurrences() {
        val model = model(); series(model)
        var writes = 0
        model.onChange = { writes++; SnapshotCodec.decode(SnapshotCodec.encode(model.exportData())) }
        assertTrue(model.generateDue(LocalDate.of(2026, 4, 30)))
        assertEquals(1, writes)
        assertEquals(3, model.transactions.count { it.seriesId != null })
        assertTrue(model.transactions.filter { it.seriesId != null }.all { !it.cleared })
        assertFalse(model.generateDue(LocalDate.of(2026, 4, 30)))
        val restored = DemoViewModel()
        restored.restoreData(SnapshotCodec.decode(SnapshotCodec.encode(model.exportData())))
        assertFalse(restored.generateDue(LocalDate.of(2026, 4, 30)))
        assertEquals(4, restored.transactions.size)
    }

    @Test fun transfersAreAtomicIndependentAndExcludedFromReports() {
        val model = model(); series(model, transfer = true)
        val sum = model.balance(0) + model.balance(1)
        model.toggleCleared(model.transactions.last().id)
        model.generateDue(LocalDate.of(2026, 4, 30))
        val occurrences = model.transactions.filter { it.seriesId != null }.groupBy { it.occurrence }
        assertEquals(3, occurrences.size)
        occurrences.values.forEach { pair ->
            assertEquals(2, pair.size)
            assertEquals(0, (pair[0].amount + pair[1].amount).signum())
            assertEquals(pair[0].peerId, pair[1].id)
            if (pair[0].occurrence != 0) assertFalse(pair.any { it.cleared })
        }
        assertEquals(sum, model.balance(0) + model.balance(1))
        assertEquals(1, model.reportingTransactions().size)
        SnapshotCodec.decode(SnapshotCodec.encode(model.exportData()))
    }

    @Test fun deletionAndStopResumePreserveConsumedOccurrencesAndHistory() {
        val model = model(); series(model)
        val id = model.rules.single().id
        model.setSeriesActive(id, false)
        assertFalse(model.generateDue(LocalDate.of(2026, 4, 30)))
        model.setSeriesActive(id, true)
        assertTrue(model.generateDue(LocalDate.of(2026, 4, 30)))
        val deleted = model.transactions.first { it.occurrence == 1 }
        model.deleteEntry(deleted.id)
        assertFalse(model.generateDue(LocalDate.of(2026, 4, 30)))
        assertFalse(model.transactions.any { it.seriesId == id && it.occurrence == 1 })
        val restored = DemoViewModel()
        restored.restoreData(SnapshotCodec.decode(SnapshotCodec.encode(model.exportData())))
        assertFalse(restored.generateDue(LocalDate.of(2026, 4, 30)))
    }

    @Test fun occurrenceEditAndFutureTemplateEditHaveDifferentScopes() {
        val model = model(); series(model)
        val initial = model.transactions.last()
        model.editEntry(initial.id, Locale.US)
        model.updateDraft(model.draft!!.copy(amount = "20.00", description = "Only this"))
        assertTrue(model.saveEntry(Locale.US, "unused"))
        model.generateDue(LocalDate.of(2026, 2, 28))
        assertEquals(BigDecimal("-10.00"), model.transactions.last().amount)
        model.editEntry(model.transactions.last().id, Locale.US)
        model.updateDraft(model.draft!!.copy(amount = "30.00", description = "Future"))
        assertTrue(model.saveEntry(Locale.US, "unused", SeriesEditScope.SERIES, LocalDate.of(2026, 2, 28)))
        model.generateDue(LocalDate.of(2026, 3, 31))
        assertEquals(BigDecimal("-30.00"), model.transactions.last().amount)
        assertEquals(BigDecimal("-20.00"), model.transactions.first { it.id == initial.id }.amount)
    }

    @Test fun endDateIsInclusiveAndGenerationBudgetIsResumable() {
        val model = model(); series(model, count = "", until = "2026-03-31")
        val first = Recurrences.generate(model.exportData(), LocalDate.of(2026, 5, 31), budget = 1)
        assertEquals(2, first.data.rules.single().next)
        val second = Recurrences.generate(first.data, LocalDate.of(2026, 5, 31), budget = 1)
        assertEquals(3, second.data.transactions.count { it.seriesId != null })
        assertFalse(second.data.rules.single().active)
        assertFalse(Recurrences.generate(second.data, LocalDate.of(2026, 5, 31)).changed)
        SnapshotCodec.decode(SnapshotCodec.encode(second.data))
    }

    @Test fun capacityDoesNotConsumeAnOccurrenceAndInvalidMetadataIsRejected() {
        val model = model(); series(model, transfer = true)
        val good = model.exportData()
        val filler = good.transactions.first()
        val rows = good.transactions + (100L until 10_096L).map { filler.copy(id = it) }
        assertEquals(9_999, rows.size)
        val full = good.copy(transactions = rows)
        val result = Recurrences.generate(full, LocalDate.of(2026, 2, 28))
        assertTrue(result.blocked)
        assertFalse(result.changed)
        assertEquals(full.rules, result.data.rules)
        val orphan = good.copy(rules = emptyList())
        try { SnapshotCodec.encode(orphan); fail("Orphan recurrence accepted") }
        catch (_: IllegalArgumentException) { }
        val duplicate = good.copy(transactions = good.transactions + good.transactions.last().copy(id = 999, peerId = null))
        try { SnapshotCodec.encode(duplicate); fail("Duplicate occurrence accepted") }
        catch (_: IllegalArgumentException) { }
    }
}
