package org.matirelire.android

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
import org.matirelire.android.security.VaultCrypto

class LongHistoryTest {
    private fun ledger(count: Int): LedgerData = LedgerData(
        (1..count).map { id -> DemoTransaction(id.toLong(), 0, "Fictive entry $id",
            BigDecimal("-1.00"), "Food", "Card", "2026-11-01", false) },
        listOf("Food"), listOf("Card"), listOf(BigDecimal("0.00"), BigDecimal("0.00")), null)

    @Test fun historyBeyondPreviousLimitRoundTripsWithoutLosingEntriesOrBalances() {
        val source = ledger(12_345)
        val restored = SnapshotCodec.decode(SnapshotCodec.encode(source))
        assertEquals(source, restored)
        assertEquals(BigDecimal("-12345.00"), restored.transactions.sumOf { it.amount })
    }

    @Test fun fiftyThousandRowsRemainWithinSnapshotAndArchiveBudgets() {
        val source = ledger(LedgerLimits.MAX_TRANSACTIONS)
        val bytes = SnapshotCodec.encode(source)
        assertTrue(bytes.size < SnapshotCodec.MAX_BYTES)
        assertTrue(SnapshotCodec.MAX_BYTES <= VaultCrypto.MAX_BACKUP_PAYLOAD_BYTES)
        assertEquals(source, SnapshotCodec.decode(bytes))
        val password = "fifty thousand fictive entries 2026".toCharArray()
        val material = VaultCrypto.newBackupKey(password)
        try {
            val archive = VaultCrypto.encryptBackup(bytes, material)
            assertTrue(archive.size > 8 * 1024 * 1024)
            val restored = VaultCrypto.decryptBackup(archive, password)
            try { assertArrayEquals(bytes, restored) } finally { restored.fill(0) }
        } finally { password.fill('\u0000'); material.clear(); bytes.fill(0) }
    }

    @Test fun generationSupportsLongHistoriesAndStopsBeforeSplittingTransferPair() {
        val source = ledger(LedgerLimits.MAX_TRANSACTIONS - 1)
        val rule = RecurringRule("00000000-0000-0000-0000-000000000001", "2026-10-01",
            RepeatFrequency.MONTH, 0, 1, BigDecimal("-1.00"), "Fictive transfer", "Food", "Card")
        val result = Recurrences.generate(source.copy(rules = listOf(rule)), LocalDate.of(2026, 11, 1))
        assertTrue(result.blocked)
        assertFalse(result.changed)
        assertEquals(source.transactions, result.data.transactions)
        assertEquals(rule, result.data.rules.single())
    }

    @Test fun oversizedSnapshotsAreRejectedBeforeParsing() {
        try {
            SnapshotCodec.decode(ByteArray(SnapshotCodec.MAX_BYTES + 1))
            fail("Oversized history accepted")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun archiveSizeLimitIncludesAuthenticatedEnvelopeOverhead() {
        assertEquals(40 * 1024 * 1024, VaultCrypto.MAX_ARCHIVE_BYTES)
        assertEquals(VaultCrypto.MAX_ARCHIVE_BYTES - 49, VaultCrypto.MAX_BACKUP_PAYLOAD_BYTES)
        try {
            VaultCrypto.encryptBackup(ByteArray(VaultCrypto.MAX_BACKUP_PAYLOAD_BYTES + 1),
                VaultCrypto.BackupKey(ByteArray(16), ByteArray(32)))
            fail("Oversized archive accepted")
        } catch (_: IllegalArgumentException) { }
    }
}