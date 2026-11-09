package org.matirelire.android

import android.content.ContentValues
import android.content.Context
import android.util.AtomicFile
import net.zetetic.database.Logger
import net.zetetic.database.NoopTarget
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File

/** SQLCipher holds both ledger and sensitive settings; no plaintext SQLite fallback. */
internal class VaultStore(context: Context) {
    private val directory = File(context.noBackupFilesDir, "vault-v1").apply { mkdirs() }
    private val databaseFile = File(directory, "ledger.db")
    private val envelope = AtomicFile(File(directory, "key.wrap"))
    private var db: SQLiteDatabase? = null
    val exists: Boolean get() = envelope.baseFile.exists()

    fun readEnvelope(): ByteArray = envelope.readFully().also { require(it.size in 29..1024) }
    fun writeEnvelope(bytes: ByteArray) {
        val stream = envelope.startWrite()
        try { stream.write(bytes); envelope.finishWrite(stream) }
        catch (e: Exception) { envelope.failWrite(stream); throw e }
    }

    @Synchronized fun open(key: ByteArray) {
        check(db == null)
        System.loadLibrary("sqlcipher")
        Logger.setTarget(NoopTarget())
        // Preserve a damaged database instead of the library's destructive default handler.
        val opened = SQLiteDatabase.openOrCreateDatabase(databaseFile, key, null,
            net.zetetic.database.DatabaseErrorHandler { _, _ -> throw IllegalStateException("Vault unavailable") })
        try {
            opened.rawQuery("PRAGMA cipher_version", emptyArray()).use { check(it.moveToFirst()) }
            opened.rawQuery("PRAGMA journal_mode=DELETE", emptyArray()).use { it.moveToFirst() }
            opened.execSQL("PRAGMA temp_store=MEMORY")
            opened.execSQL("PRAGMA secure_delete=ON")
            opened.execSQL("PRAGMA cipher_memory_security=ON")
            opened.execSQL("CREATE TABLE IF NOT EXISTS state (name TEXT PRIMARY KEY, value BLOB NOT NULL)")
            db = opened
        } catch (e: Exception) { opened.close(); throw e }
    }

    @Synchronized fun get(name: String): ByteArray? = requireNotNull(db).rawQuery(
        "SELECT value FROM state WHERE name=?", arrayOf(name)).use { if (it.moveToFirst()) it.getBlob(0) else null }

    @Synchronized fun put(name: String, value: ByteArray) {
        val database = requireNotNull(db)
        database.beginTransaction()
        try {
            val row = ContentValues().apply { put("name", name); put("value", value) }
            check(database.insertWithOnConflict("state", null, row, SQLiteDatabase.CONFLICT_REPLACE) != -1L)
            database.setTransactionSuccessful()
        } finally { database.endTransaction() }
    }

    @Synchronized fun close() { db?.close(); db = null }
}
