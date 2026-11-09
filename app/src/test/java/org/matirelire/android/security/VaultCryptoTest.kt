package org.matirelire.android.security

import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.*
import org.junit.Test

class VaultCryptoTest {
    private val phrase get() = "six nuages bleus sur la colline 47".toCharArray()
    private val payload get() = "{\"account\":\"Compte fictif\",\"amount\":\"12.34\"}".toByteArray(Charsets.UTF_8)

    private fun rejects(block: () -> Unit) {
        var failed = false
        try { block() } catch (_: IllegalArgumentException) { failed = true }
        catch (_: GeneralSecurityException) { failed = true }
        assertTrue("Expected invalid data to be rejected", failed)
    }

    @Test fun aesGcmRoundTripUsesFreshNoncesAndAuthenticatesAssociatedData() {
        val key = ByteArray(32) { it.toByte() }
        val aad = "context-v1".toByteArray()
        val first = VaultCrypto.encrypt(payload, key, aad)
        val second = VaultCrypto.encrypt(payload, key, aad)
        assertFalse(first.contentEquals(second))
        assertArrayEquals(payload, VaultCrypto.decrypt(first, key, aad))
        rejects { VaultCrypto.decrypt(first, key, "context-v2".toByteArray()) }
        rejects { VaultCrypto.decrypt(first, ByteArray(32), aad) }
        val damaged = first.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
        rejects { VaultCrypto.decrypt(damaged, key, aad) }
    }

    @Test fun encryptedOutputInteroperatesWithStandardJcaAesGcm() {
        val key = ByteArray(32) { (it + 1).toByte() }
        val aad = byteArrayOf(4, 5, 6)
        val encrypted = VaultCrypto.encrypt(payload, key, aad)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, encrypted, 0, 12))
        cipher.updateAAD(aad)
        assertArrayEquals(payload, cipher.doFinal(encrypted, 12, encrypted.size - 12))
    }

    @Test fun backupMaterialSupportsRepeatedAutomaticExportsAndPortableRestore() {
        val secret = phrase
        val material = VaultCrypto.newBackupKey(secret)
        try {
            val first = VaultCrypto.encryptBackup(payload, material)
            val second = VaultCrypto.encryptBackup(payload, material)
            assertFalse(first.contentEquals(second))
            assertArrayEquals(payload, VaultCrypto.decryptBackup(first, secret))
            assertArrayEquals(payload, VaultCrypto.decryptBackup(second, secret))
            assertFalse(first.toString(Charsets.ISO_8859_1).contains("Compte fictif"))
            assertEquals("BackupKey([redacted])", material.toString())
        } finally { secret.fill('\u0000'); material.clear() }
        assertTrue(material.key.all { it == 0.toByte() })
    }

    @Test fun wrongPasswordAndEveryAuthenticatedBackupRegionAreRejected() {
        val secret = phrase
        val material = VaultCrypto.newBackupKey(secret)
        try {
            val archive = VaultCrypto.encryptBackup(payload, material)
            rejects { VaultCrypto.decryptBackup(archive, "wrong secret".toCharArray()) }
            // Magic/version fail before KDF. Salt, nonce, ciphertext/tag fail authentication.
            for (index in listOf(0, 4, 5, 21, archive.lastIndex)) {
                val damaged = archive.copyOf().also { it[index] = (it[index].toInt() xor 1).toByte() }
                rejects { VaultCrypto.decryptBackup(damaged, secret) }
            }
        } finally { secret.fill('\u0000'); material.clear() }
    }

    @Test fun malformedArchiveAndInvalidMaterialAreBounded() {
        for (length in listOf(0, 4, 20, 48)) rejects { VaultCrypto.decryptBackup(ByteArray(length), charArrayOf()) }
        rejects { VaultCrypto.decryptBackup(ByteArray(VaultCrypto.MAX_ARCHIVE_BYTES + 1), charArrayOf()) }
        rejects { VaultCrypto.encryptBackup(ByteArray(VaultCrypto.MAX_BACKUP_PAYLOAD_BYTES + 1),
            VaultCrypto.BackupKey(ByteArray(16), ByteArray(32))) }
        rejects { VaultCrypto.encryptBackup(payload, VaultCrypto.BackupKey(ByteArray(15), ByteArray(32))) }
        rejects { VaultCrypto.encrypt(payload, ByteArray(31)) }
        rejects { VaultCrypto.decrypt(ByteArray(27), ByteArray(32)) }
        rejects { VaultCrypto.derive(charArrayOf('x'), ByteArray(15)) }
        rejects { VaultCrypto.derive(CharArray(VaultCrypto.MAX_PASSWORD_CHARS + 1), ByteArray(16)) }
    }

    @Test fun policyRejectsObviousSecretsWithoutForcingCompositionRules() {
        for (weak in listOf("short", "aaaaaaaaaaaaaaaa", "1234567890123456", "Password!12345678", "abcdefghijklmnop", "abcdabcdabcdabcd")) {
            rejects { VaultCrypto.requireStrongBackupPassword(weak.toCharArray()) }
        }
        VaultCrypto.requireStrongBackupPassword(phrase)
        VaultCrypto.requireStrongBackupPassword("de longs mots distincts et personnels".toCharArray())
    }

    @Test fun deriveIsDeterministicAndArchiveReadDoesNotEnforceCreationPolicy() {
        val shortPassword = "12345678".toCharArray()
        val salt = ByteArray(16) { it.toByte() }
        val first = VaultCrypto.derive(shortPassword, salt)
        val second = VaultCrypto.derive(shortPassword, salt)
        val other = VaultCrypto.derive(shortPassword, ByteArray(16) { (it + 1).toByte() })
        try {
            assertArrayEquals(first, second)
            assertFalse(first.contentEquals(other))
            val archive = VaultCrypto.encryptBackup(payload, VaultCrypto.BackupKey(salt, first))
            assertArrayEquals(payload, VaultCrypto.decryptBackup(archive, shortPassword))
        } finally { first.fill(0); second.fill(0); other.fill(0); shortPassword.fill('\u0000') }
    }
}
