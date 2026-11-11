package org.matirelire.android

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class SnapshotCodecTest {
    private fun sample() = LedgerData(
        listOf(DemoTransaction(1, 0, "Fake expense", BigDecimal("-12.34"), "Food", "Card", "2026-11-09", true)),
        listOf("Food"), listOf("Card"), listOf(BigDecimal("1000.00"), BigDecimal("30.00")),
        EntryDraft(0, amount = "20,00", description = "Draft", category = "Food", payment = "Card"))

    @Test fun snapshotRoundTripPreservesLedgerReferencesAndDraft() {
        assertEquals(sample(), SnapshotCodec.decode(SnapshotCodec.encode(sample())))
    }
    @Test fun schemaAndReferencesAreValidatedBeforeImport() {
        val json = SnapshotCodec.encode(sample()).toString(Charsets.UTF_8)
        for (bad in listOf(json.replace("\"schema\":1", "\"schema\":99"),
            json.replace("\"category\":\"Food\"", "\"category\":\"Missing\""))) {
            try { SnapshotCodec.decode(bad.toByteArray()); fail("Invalid snapshot accepted") }
            catch (_: IllegalArgumentException) { }
        }
    }
    @Test fun untrustedSizeDepthAndEncodingAreBounded() {
        for (bytes in listOf(ByteArray(SnapshotCodec.MAX_BYTES + 1),
            ("[".repeat(40) + "0" + "]".repeat(40)).toByteArray(), byteArrayOf(0xc3.toByte(), 0x28))) {
            var rejected = false
            try { SnapshotCodec.decode(bytes) } catch (_: Exception) { rejected = true }
            assertTrue(rejected)
        }
    }
    @Test fun lockClearsSensitiveStateWithoutCallingPersistence() {
        val model = DemoViewModel()
        model.restoreData(sample())
        model.onChange = { fail("Lock should not rewrite the vault") }
        model.clearSensitive()
        assertTrue(model.transactions.isEmpty())
        assertTrue(model.categories.isEmpty())
        assertTrue(model.payments.isEmpty())
        assertNull(model.draft)
        model.restoreData(sample())
        assertEquals(BigDecimal("987.66"), model.balance(0))
        assertNotNull(model.draft)
    }

    @Test fun modelCannotExceedTheLimitsOfTheRestoreFormat() {
        val model = DemoViewModel()
        val manyCategories = listOf("Food") + (1 until 500).map { "Category $it" }
        val rows = (1..LedgerLimits.MAX_TRANSACTIONS).map { sample().transactions.first().copy(id = it.toLong()) }
        model.restoreData(sample().copy(categories = manyCategories, transactions = rows))
        assertNull(model.addCategory("One more"))
        assertEquals(500, model.categories.size)
        assertFalse(model.saveEntry(java.util.Locale.FRANCE, "2026-11-09"))
        assertEquals(LedgerLimits.MAX_TRANSACTIONS, model.transactions.size)
        assertTrue(model.limitReached)
        assertEquals(LedgerLimits.MAX_TRANSACTIONS, SnapshotCodec.decode(SnapshotCodec.encode(model.exportData())).transactions.size)
    }

    @Test fun legacyDraftWithoutEditingIdStillLoadsAndMissingTargetsAreRejected() {
        val data = sample().copy(draft = sample().draft!!.copy(editingId = 1))
        assertEquals(data, SnapshotCodec.decode(SnapshotCodec.encode(data)))
        val oldJson = SnapshotCodec.encode(sample()).toString(Charsets.UTF_8)
            .replace(",\"editingId\":null", "").replace("\"editingId\":null,", "")
        assertNull(SnapshotCodec.decode(oldJson.toByteArray()).draft!!.editingId)
        try {
            SnapshotCodec.encode(data.copy(draft = data.draft!!.copy(editingId = 999)))
            fail("Missing edit target accepted")
        } catch (_: IllegalArgumentException) { }
    }
}
