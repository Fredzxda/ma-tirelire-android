package org.matirelire.android

import androidx.test.platform.app.InstrumentationRegistry
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.DatabaseErrorHandler
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.SecureRandom
import java.util.UUID

/** Isolated test DB: never touches the application's vault or Android credentials. */
class VaultStorageDeviceTest {
    @Test fun perUseKeystoreKeyRefusesEncryptionWithoutItsAuthentication() {
        val alias = "matirelire-test-${UUID.randomUUID()}"
        val store = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        try {
            val generator = javax.crypto.KeyGenerator.getInstance("AES", "AndroidKeyStore")
            generator.init(android.security.keystore.KeyGenParameterSpec.Builder(alias,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256).setBlockModes("GCM").setEncryptionPaddings("NoPadding")
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationParameters(0, android.security.keystore.KeyProperties.AUTH_BIOMETRIC_STRONG or
                    android.security.keystore.KeyProperties.AUTH_DEVICE_CREDENTIAL)
                .setInvalidatedByBiometricEnrollment(false).build())
            val key = generator.generateKey()
            var denied = false
            try {
                val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key)
                cipher.doFinal(ByteArray(32)) // never authenticate this operation
            } catch (_: android.security.keystore.UserNotAuthenticatedException) {
                denied = true
            } catch (failure: javax.crypto.IllegalBlockSizeException) {
                denied = generateSequence(failure as Throwable) { it.cause }
                    .any { it is android.security.KeyStoreException }
            }
            assertTrue("A per-use key must not accept a UI-only success", denied)
        } finally {
            if (store.containsAlias(alias)) store.deleteEntry(alias)
        }
    }

    @Test fun sqlcipherIsEncryptedAndWrongKeyNeverReplacesDatabase() {
        System.loadLibrary("sqlcipher")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "cipher-test-${UUID.randomUUID()}.db")
        val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val wrong = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val marker = "FAKE-FINANCIAL-MARKER-DO-NOT-LOG"
        val handler = DatabaseErrorHandler { _, _ -> throw IllegalStateException("Test database unavailable") }
        try {
            SQLiteDatabase.openOrCreateDatabase(file, key, null, handler).use { db ->
                db.execSQL("CREATE TABLE test (value TEXT NOT NULL)")
                db.execSQL("INSERT INTO test VALUES (?)", arrayOf(marker))
                db.beginTransaction()
                db.execSQL("UPDATE test SET value=?", arrayOf("FAKE-ROLLED-BACK"))
                db.endTransaction() // deliberately no success: rollback must preserve initial value
            }
            val before = file.readBytes()
            assertFalse(before.copyOfRange(0, 16).contentEquals("SQLite format 3\u0000".toByteArray()))
            assertFalse(before.toString(Charsets.ISO_8859_1).contains(marker))
            var rejected = false
            try {
                SQLiteDatabase.openOrCreateDatabase(file, wrong, null, handler).use { db ->
                    db.rawQuery("SELECT value FROM test", emptyArray()).use { it.moveToFirst() }
                }
            } catch (_: Exception) { rejected = true }
            assertTrue(rejected)
            assertTrue(file.exists())
            assertArrayEquals(before, file.readBytes())
            SQLiteDatabase.openOrCreateDatabase(file, key, null, handler).use { db ->
                db.rawQuery("SELECT value FROM test", emptyArray()).use { cursor ->
                    assertTrue(cursor.moveToFirst()); assertEquals(marker, cursor.getString(0))
                }
            }
        } finally { key.fill(0); wrong.fill(0); SQLiteDatabase.deleteDatabase(file) }
    }
}
