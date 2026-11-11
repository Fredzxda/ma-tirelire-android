package org.matirelire.android

/** Fingerprints live only in the encrypted configuration, never in logs. */
internal object BackupCheckpoint {
    fun hash(bytes: ByteArray): String = java.security.MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
    fun snapshotHash(data: LedgerData): String {
        val bytes = SnapshotCodec.encode(data)
        return try { hash(bytes) } finally { bytes.fill(0) }
    }
    fun matches(data: LedgerData, expectedSnapshotHash: String, archive: ByteArray, archiveHash: String): Boolean =
        expectedSnapshotHash.length == 64 && archiveHash.length == 64 &&
            snapshotHash(data) == expectedSnapshotHash && hash(archive) == archiveHash
}
