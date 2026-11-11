package org.matirelire.android

import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Test

class BackupCheckpointTest {
    private fun data() = DemoViewModel().apply {
        initialize(listOf(DemoTransaction(1,0,"Fixture",BigDecimal("1.00"),"Food","Card","10 nov. 2026")))
    }.exportData()

    @Test fun verifiedArchiveMustMatchCurrentSnapshot() {
        val before = data()
        val archive = byteArrayOf(1,2,3,4) // synthetic ciphertext for fingerprint tests
        val snapshotHash = BackupCheckpoint.snapshotHash(before)
        val archiveHash = BackupCheckpoint.hash(archive)
        assertTrue(BackupCheckpoint.matches(before,snapshotHash,archive,archiveHash))
        assertFalse(BackupCheckpoint.matches(before.copy(transactionOrder = TransactionOrder.NEWEST),snapshotHash,archive,archiveHash))
        assertFalse(BackupCheckpoint.matches(before.copy(transactions = before.transactions.map { it.copy(amount = BigDecimal("2.00")) }),snapshotHash,archive,archiveHash))
    }
    @Test fun alteredArchiveAndMissingCheckpointAreRejected() {
        val before = data()
        val archive = byteArrayOf(1,2,3,4)
        val snapshotHash = BackupCheckpoint.snapshotHash(before)
        val archiveHash = BackupCheckpoint.hash(archive)
        assertFalse(BackupCheckpoint.matches(before,snapshotHash,byteArrayOf(1,2,3,5),archiveHash))
        assertFalse(BackupCheckpoint.matches(before,"",archive,archiveHash))
        assertFalse(BackupCheckpoint.matches(before,snapshotHash,archive,""))
    }
}
