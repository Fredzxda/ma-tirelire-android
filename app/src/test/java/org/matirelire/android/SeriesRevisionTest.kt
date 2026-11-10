package org.matirelire.android

import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class SeriesRevisionTest {
    private fun model(anchor: String = "2026-01-05", frequency: RepeatFrequency = RepeatFrequency.MONTH, transfer: Boolean = false) = DemoViewModel().apply {
        initialize(listOf(DemoTransaction(1, 0, "Fixture", BigDecimal("-1.00"), "Food", "Card", "2026-01-01", true)))
        ensureAccountNames(listOf("Current", "Savings")); ensureTransferPayment("Transfer")
        beginEntry(LocalDate.parse(anchor))
        updateDraft(draft!!.copy(amount = "10", description = "Recurring", repeatFrequency = frequency,
            payment = if (transfer) "Transfer" else "Card", transferTarget = if (transfer) 1 else null))
        assertTrue(saveEntry(Locale.US, "unused"))
    }

    @Test fun currentScopeChangesOnlyTheEditedRowAndNeverTheTemplate() {
        val model = model()
        model.generateDue(LocalDate.of(2026, 3, 5))
        val before = model.exportData()
        val row = model.transactions.first { it.occurrence == 1 }
        model.editEntry(row.id, Locale.US)
        model.updateDraft(model.draft!!.copy(amount = "99", description = "Only one", date = "2026-02-06", repeatFrequency = RepeatFrequency.WEEK))
        assertTrue(model.saveEntry(Locale.US, "unused", SeriesEditScope.CURRENT))
        assertEquals(before.transactions.filter { it.id != row.id }, model.transactions.filter { it.id != row.id })
        assertEquals(before.rules.single().amount, model.rules.single().amount)
        assertEquals(before.rules.single().frequency, model.rules.single().frequency)
        assertEquals(BigDecimal("-99.00"), model.transactions.single { it.id == row.id }.amount)
    }

    @Test fun monthlyDayChangePreservesAllOtherRecordedRowsAndUsesNewFutureDay() {
        val model = model()
        model.generateDue(LocalDate.of(2026, 3, 5))
        val row = model.transactions.first { it.occurrence == 1 }
        val before = model.transactions.filter { it.id != row.id }
        model.editEntry(row.id, Locale.US)
        model.updateDraft(model.draft!!.copy(date = "2026-02-06", amount = "25", description = "Changed"))
        assertTrue(model.saveEntry(Locale.US, "unused", SeriesEditScope.SERIES, LocalDate.of(2026, 3, 10)))
        assertEquals(before, model.transactions.filter { it.id != row.id })
        assertEquals(LocalDate.of(2026, 4, 6), model.rules.single().nextDate())
        model.generateDue(LocalDate.of(2026, 4, 6))
        val generated = model.transactions.last()
        assertEquals("2026-04-06", generated.date)
        assertEquals(BigDecimal("-25.00"), generated.amount)
        assertEquals("Changed", generated.description)
        val restored = DemoViewModel()
        restored.restoreData(SnapshotCodec.decode(SnapshotCodec.encode(model.exportData())))
        assertFalse(restored.generateDue(LocalDate.of(2026, 4, 6)))
    }

    @Test fun weeklyWeekdayAndMonthlyToWeeklyChangesHaveNoRetroactiveCatchUp() {
        for (original in listOf(RepeatFrequency.WEEK, RepeatFrequency.MONTH)) {
            val model = model(frequency = original)
            model.generateDue(LocalDate.of(2026, 3, 9))
            val row = model.transactions.first { it.occurrence == 0 }
            val before = model.transactions.filter { it.id != row.id }
            model.editEntry(row.id, Locale.US)
            model.updateDraft(model.draft!!.copy(date = "2026-01-07", repeatFrequency = RepeatFrequency.WEEK))
            assertTrue(model.saveEntry(Locale.US, "unused", SeriesEditScope.SERIES, LocalDate.of(2026, 3, 10)))
            assertEquals(LocalDate.of(2026, 3, 11), model.rules.single().nextDate())
            assertEquals(before, model.transactions.filter { it.id != row.id })
            assertFalse(model.generateDue(LocalDate.of(2026, 3, 10)))
            assertTrue(model.generateDue(LocalDate.of(2026, 3, 11)))
        }
    }

    @Test fun remainingCountAndEndDateCanBeAddedWithoutCountingExistingRowsAgain() {
        val model = model()
        model.generateDue(LocalDate.of(2026, 3, 5))
        val row = model.transactions.last()
        model.editEntry(row.id, Locale.US)
        model.updateDraft(model.draft!!.copy(repeatCount = "2"))
        assertTrue(model.saveEntry(Locale.US, "unused", SeriesEditScope.SERIES, LocalDate.of(2026, 3, 10)))
        val next = model.rules.single().next
        assertEquals(next + 2, model.rules.single().count)
        model.generateDue(LocalDate.of(2026, 12, 31))
        assertEquals(5, model.transactions.count { it.seriesId != null })
        assertFalse(model.rules.single().active)
        model.editEntry(row.id, Locale.US)
        model.updateDraft(model.draft!!.copy(repeatCount = "", repeatUntil = "2027-02-05"))
        assertTrue(model.saveEntry(Locale.US, "unused", SeriesEditScope.SERIES, LocalDate.of(2026, 12, 31)))
        assertEquals(LocalDate.of(2026, 6, 5), model.rules.single().nextDate())
        model.generateDue(LocalDate.of(2027, 3, 1))
        assertEquals(14, model.transactions.count { it.seriesId != null })
    }

    @Test fun amountOnlySeriesEditKeepsMonthEndAnchorAndAllHistoricalAmounts() {
        val model = model(anchor = "2026-01-31")
        model.generateDue(LocalDate.of(2026, 2, 28))
        val row = model.transactions.last()
        val before = model.transactions.filter { it.id != row.id }
        model.editEntry(row.id, Locale.US)
        model.updateDraft(model.draft!!.copy(amount = "20"))
        assertTrue(model.saveEntry(Locale.US, "unused", SeriesEditScope.SERIES, LocalDate.of(2026, 3, 1)))
        assertEquals(LocalDate.of(2026, 3, 31), model.rules.single().nextDate())
        assertEquals(before, model.transactions.filter { it.id != row.id })
    }

    @Test fun zeroRemainingAndNeverStopOnlyFutureGeneration() {
        for (never in listOf(false, true)) {
            val model = model()
            val row = model.transactions.last()
            model.editEntry(row.id, Locale.US)
            model.updateDraft(model.draft!!.copy(repeatCount = if (never) "" else "0",
                repeatFrequency = if (never) RepeatFrequency.NONE else RepeatFrequency.MONTH))
            assertTrue(model.saveEntry(Locale.US, "unused", SeriesEditScope.SERIES, LocalDate.of(2026, 3, 10)))
            assertFalse(model.rules.single().active)
            assertFalse(model.generateDue(LocalDate.of(2027, 1, 1)))
            assertEquals(2, model.transactions.size)
        }
    }

    @Test fun descriptionCategoryAndPaymentApplyOnlyToTheFutureTemplate() {
        val model = model()
        model.generateDue(LocalDate.of(2026, 3, 5))
        model.addCategory("Rent"); model.addPayment("Cash")
        val row = model.transactions.first { it.occurrence == 1 }
        val before = model.transactions.filter { it.id != row.id }
        model.editEntry(row.id, Locale.US)
        model.updateDraft(model.draft!!.copy(category = "Rent", payment = "Cash", description = "New template"))
        assertTrue(model.saveEntry(Locale.US, "unused", SeriesEditScope.SERIES, LocalDate.of(2026, 3, 10)))
        assertEquals(before, model.transactions.filter { it.id != row.id })
        model.generateDue(LocalDate.of(2026, 4, 5))
        val next = model.transactions.last()
        assertEquals("Rent", next.category)
        assertEquals("Cash", next.payment)
        assertEquals("New template", next.description)
    }

    @Test fun receiverSeriesEditPreservesOtherRecordedPairsAndFutureDirection() {
        val model = model(transfer = true)
        model.generateDue(LocalDate.of(2026, 3, 5))
        val row = model.transactions.single { it.occurrence == 1 && it.account == 1 }
        val before = model.transactions.filter { it.id != row.id && it.id != row.peerId }
        model.editEntry(row.id, Locale.US)
        model.updateDraft(model.draft!!.copy(amount = "50", date = "2026-02-06"))
        assertTrue(model.saveEntry(Locale.US, "unused", SeriesEditScope.SERIES, LocalDate.of(2026, 3, 10)))
        assertEquals(before, model.transactions.filter { it.id != row.id && it.id != row.peerId })
        assertEquals(BigDecimal("-50.00"), model.rules.single().amount)
        model.generateDue(LocalDate.of(2026, 4, 6))
        val pair = model.transactions.takeLast(2)
        assertEquals(BigDecimal("-50.00"), pair.single { it.account == 0 }.amount)
        assertEquals(BigDecimal("50.00"), pair.single { it.account == 1 }.amount)
        SnapshotCodec.decode(SnapshotCodec.encode(model.exportData()))
    }

    @Test fun deletedLatestOccurrenceDoesNotRewindCalendarOrCursorAfterRevision() {
        val model = model()
        model.generateDue(LocalDate.of(2026, 3, 5))
        model.deleteEntry(model.transactions.single { it.occurrence == 2 }.id)
        val row = model.transactions.single { it.occurrence == 1 }
        model.editEntry(row.id, Locale.US)
        model.updateDraft(model.draft!!.copy(date = "2026-02-06"))
        assertTrue(model.saveEntry(Locale.US, "unused", SeriesEditScope.SERIES, LocalDate.of(2026, 2, 10)))
        assertEquals(LocalDate.of(2026, 3, 6), model.rules.single().nextDate())
        val restored = DemoViewModel()
        restored.restoreData(SnapshotCodec.decode(SnapshotCodec.encode(model.exportData())))
        restored.generateDue(LocalDate.of(2026, 3, 6))
        assertFalse(restored.transactions.any { it.occurrence == 2 })
        assertEquals(3, restored.transactions.last().occurrence)
        assertFalse(restored.generateDue(LocalDate.of(2026, 3, 6)))
    }
}
