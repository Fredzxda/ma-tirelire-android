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
