package org.matirelire.android

import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Test

class TransactionOrderTest {
    private fun row(id: Long, date: String) = DemoTransaction(id, 0, "Fixture", BigDecimal("1.00"), "Food", "Card", date)
    private val rows = listOf(row(1, "2026-11-11"), row(2, "2026-10-01"), row(3, "old date"), row(4, "2026-11-11"))
    @Test fun sortsDatesWithStableTiesAndUnknownLast() {
        assertEquals(listOf(1L,4L,2L,3L), TransactionOrder.NEWEST.arrange(rows).map { it.id })
        assertEquals(listOf(2L,1L,4L,3L), TransactionOrder.OLDEST.arrange(rows).map { it.id })
        assertEquals(listOf(1L,2L,3L,4L), rows.map { it.id })
        assertEquals(rows, TransactionOrder.ORIGINAL.arrange(rows))
    }
    @Test fun mixedLegacyNamedMonthsAndIsoMatchReportedRegression() {
        val mixed = listOf(row(1, "2026-11-02"), row(2, "2026-11-02"), row(3, "10 nov. 2026"))
        assertEquals(listOf(3L,1L,2L), TransactionOrder.NEWEST.arrange(mixed).map { it.id })
        assertEquals(listOf(1L,2L,3L), TransactionOrder.OLDEST.arrange(mixed).map { it.id })
        assertEquals("10 nov. 2026", mixed.last().date)
        assertNull(OperationDates.parse(mixed.last().date))
        for (locale in listOf(java.util.Locale.FRANCE, java.util.Locale.US, java.util.Locale.UK)) {
            val label = OperationDates.display("2026-11-10", locale)
            assertEquals(java.time.LocalDate.of(2026, 11, 10), OperationDates.forOrdering(label))
        }
    }
    @Test fun invalidOrAmbiguousLegacyDatesAreNotGuessed() {
        for (bad in listOf("01/02/2026", "31 avr. 2026", "29 févr. 2025", "8 novembre · exemple fictif")) {
            assertNull(OperationDates.forOrdering(bad))
        }
        assertEquals(java.time.LocalDate.of(2024, 2, 29), OperationDates.forOrdering("29 févr. 2024"))
    }
    private fun model() = DemoViewModel().apply { initialize(listOf(rows[0], rows[1])) }
    @Test fun encryptedSnapshotRoundTripAndLocking() {
        val model = model()
        model.setTransactionOrder(TransactionOrder.NEWEST)
        val data = SnapshotCodec.decode(SnapshotCodec.encode(model.exportData()))
        model.clearSensitive()
        assertEquals(TransactionOrder.ORIGINAL, model.transactionOrder)
        model.restoreData(data)
        assertEquals(TransactionOrder.NEWEST, model.transactionOrder)
        assertEquals(BigDecimal("502.00"), model.balance(0))
    }
    @Test fun olderSnapshotsKeepOriginalAndInvalidValueIsRejected() {
        val model = model()
        assertEquals(TransactionOrder.ORIGINAL, SnapshotCodec.decode(SnapshotCodec.encode(model.exportData())).transactionOrder)
        model.setTransactionOrder(TransactionOrder.OLDEST)
        val text = SnapshotCodec.encode(model.exportData()).toString(Charsets.UTF_8).replace("OLDEST", "INVALID")
        assertThrows(IllegalArgumentException::class.java) { SnapshotCodec.decode(text.toByteArray(Charsets.UTF_8)) }
    }
}
