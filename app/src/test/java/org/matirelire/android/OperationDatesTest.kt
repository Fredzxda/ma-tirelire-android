package org.matirelire.android

import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class OperationDatesTest {
    private fun model(date: String = "2026-11-08") = DemoViewModel().apply {
        initialize(listOf(DemoTransaction(1, 0, "Fixture", BigDecimal("-10.00"), "Food", "Card", date, true)))
    }

    @Test fun dateDefaultsAtDraftStartAndSurvivesReopeningAndSnapshot() {
        val model = model()
        model.beginEntry(LocalDate.of(2026, 11, 10))
        assertEquals("2026-11-10", model.draft!!.date)
        model.beginEntry(LocalDate.of(2026, 11, 11))
        assertEquals("2026-11-10", model.draft!!.date)
        model.updateDraft(model.draft!!.copy(date = "2024-02-29", amount = "12,34", description = "Past expense"))
        val restored = DemoViewModel()
        restored.restoreData(SnapshotCodec.decode(SnapshotCodec.encode(model.exportData())))
        assertEquals("2024-02-29", restored.draft!!.date)
        assertTrue(restored.saveEntry(Locale.FRANCE, "2026-12-01"))
        assertEquals("2024-02-29", restored.transactions.last().date)
        assertEquals(BigDecimal("-12.34"), restored.transactions.last().amount)
    }

    @Test fun editingChangesDateOnlyWhenChosenAndKeepsIdentityAndClearing() {
        val model = model()
        val before = model.transactions.single()
        model.editEntry(1, Locale.US)
        model.updateDraft(model.draft!!.copy(date = "2025-12-31"))
        assertTrue(model.saveEntry(Locale.US, "2026-11-10"))
        assertEquals(before.copy(date = "2025-12-31"), model.transactions.single())
        assertEquals(BigDecimal("490.00"), model.balance(0, true))
    }

    @Test fun legacyTextIsNeverGuessedAndCanBeExplicitlyReplaced() {
        val model = model("08/11/2026")
        model.editEntry(1, Locale.US)
        assertEquals("", model.draft!!.date)
        assertTrue(model.saveEntry(Locale.US, "2026-11-10"))
        assertEquals("08/11/2026", model.transactions.single().date)
        model.editEntry(1, Locale.FRANCE)
        model.updateDraft(model.draft!!.copy(date = "2026-11-08"))
        assertTrue(model.saveEntry(Locale.FRANCE, "unused"))
        assertEquals("2026-11-08", model.transactions.single().date)
        assertEquals("08/11/2026", OperationDates.display("08/11/2026", Locale.US))
    }

    @Test fun strictCivilDateValidationAndLocalizedPresentation() {
        assertNotNull(OperationDates.parse("2024-02-29"))
        for (bad in listOf("2025-02-29", "2026-04-31", "2026-1-01", "0000-01-01", "01/02/2026")) {
            assertNull(OperationDates.parse(bad))
        }
        assertNotEquals(OperationDates.display("2026-11-10", Locale.FRANCE),
            OperationDates.display("2026-11-10", Locale.US))
        val model = model()
        model.editEntry(1, Locale.US)
        model.updateDraft(model.draft!!.copy(date = "2025-02-29"))
        assertFalse(model.saveEntry(Locale.US, "2026-11-10"))
        try { SnapshotCodec.encode(model.exportData()); fail("Invalid draft date accepted") }
        catch (_: IllegalArgumentException) { }
        assertEquals("2026-11-08", model.transactions.single().date)
    }
}
