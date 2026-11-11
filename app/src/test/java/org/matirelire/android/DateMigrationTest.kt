package org.matirelire.android

import java.math.BigDecimal
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DateMigrationTest {
    private fun data(): LedgerData {
        val model = DemoViewModel().apply {
            initialize(listOf(
                DemoTransaction(1, 0, "Fixture", BigDecimal("-12.00"), "Food", "Card", "10 nov. 2026", true),
                DemoTransaction(2, 0, "Fixture", BigDecimal("2.00"), "Food", "Card", "01/02/2026"),
                DemoTransaction(3, 0, "Fixture", BigDecimal("1.00"), "Food", "Card", "2026-11-02")))
        }
        return model.exportData()
    }

    @Test fun canonicalizesOnlyKnownDatesAndIsIdempotent() {
        val before = data()
        val plan = DateMigration.plan(before)
        assertEquals(1, plan.converted); assertEquals(1, plan.unresolved)
        assertEquals(listOf("2026-11-10", "01/02/2026", "2026-11-02"), plan.data.transactions.map { it.date })
        assertEquals(before.transactions[0].copy(date = "2026-11-10"), plan.data.transactions[0])
        assertEquals(before.copy(transactions = plan.data.transactions, dateMigrationVersion = 1), plan.data)
        assertEquals("10 nov. 2026", before.transactions[0].date)
        assertEquals(plan.data, DateMigration.plan(plan.data).data)
        assertEquals(plan.data, SnapshotCodec.decode(SnapshotCodec.encode(plan.data)))
    }

    @Test fun preservesTransferPairReferencesSignsAndClearing() {
        val a = data().transactions[0].copy(peerId = 4)
        val b = a.copy(id = 4, account = 1, peerId = 1, amount = -a.amount, cleared = false)
        val before = data().copy(transactions = listOf(a, b))
        val after = DateMigration.plan(before).data
        assertEquals(before.transactions.map { it.copy(date = "2026-11-10") }, after.transactions)
        assertEquals(BigDecimal("0.00"), after.transactions.fold(BigDecimal.ZERO) { sum, row -> sum + row.amount })
    }

    @Test fun backupFailurePreventsAnyWrite() = runBlocking<Unit> {
        val before = data()
        var written = false
        try {
            DateMigration.execute(before, backup = { error("fixture backup failure") },
                current = { before }, replace = { written = true })
            fail("Backup failure accepted")
        } catch (_: IllegalStateException) { }
        assertFalse(written)
    }

    @Test fun editsDuringBackupAreNeverOverwrittenAndBackupComesFirst() = runBlocking<Unit> {
        val before = data()
        var current = before
        var written = false
        try {
            DateMigration.execute(before, backup = { current = before.copy(transactionOrder = TransactionOrder.NEWEST) },
                current = { current }, replace = { written = true })
            fail("Concurrent edit overwritten")
        } catch (_: IllegalStateException) { }
        assertFalse(written)
        val events = mutableListOf<String>()
        DateMigration.execute(before, backup = { events.add("backup") }, current = { before },
            replace = { events.add("write") })
        assertEquals(listOf("backup", "write"), events)
        val migrated = DateMigration.plan(before).data
        DateMigration.execute(migrated, backup = { fail("Unnecessary backup") }, current = { migrated },
            replace = { fail("Unnecessary write") })
    }

    @Test fun unknownMigrationVersionIsRejected() {
        val text = SnapshotCodec.encode(DateMigration.plan(data()).data).toString(Charsets.UTF_8)
            .replace("\"dateMigrationVersion\":1", "\"dateMigrationVersion\":2")
        assertThrows(IllegalArgumentException::class.java) { SnapshotCodec.decode(text.toByteArray(Charsets.UTF_8)) }
    }
}
