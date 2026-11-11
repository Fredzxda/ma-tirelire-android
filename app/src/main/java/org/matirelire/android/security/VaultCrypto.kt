package org.matirelire.android.security

import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

/**
 * Prototype archive format, independent of Android and device keys.
 * v1 = ASCII MTBK | version byte 1 | salt16 | IV12 | AES-256-GCM ciphertext/tag128.
 * The complete 21-byte header is authenticated as AAD. Argon2id v1.3 parameters
 * are fixed by the archive version: 65536 KiB, three passes, one lane, output32.
 * No unauthenticated archive field controls allocation or KDF cost.
 *
 * Callers own password char arrays and must clear them. Returned BackupKey must
 * only be persisted inside the encrypted vault and cleared on session close.
 * JVM/JCA providers can retain copies; this class cannot promise perfect wiping.
 * References: RFC 9106 and Bouncy Castle Argon2BytesGenerator API.
 */
object VaultCrypto {
    const val MAX_ARCHIVE_BYTES = 40 * 1024 * 1024
    const val SALT_BYTES = 16
    const val KEY_BYTES = 32
    const val MAX_PASSWORD_CHARS = 1024
    private const val IV_BYTES = 12
    private const val TAG_BYTES = 16
    private const val PREFIX_BYTES = 5
    private const val HEADER_BYTES = PREFIX_BYTES + SALT_BYTES
    private const val ENCRYPTED_OVERHEAD = IV_BYTES + TAG_BYTES
    const val MAX_BACKUP_PAYLOAD_BYTES = MAX_ARCHIVE_BYTES - 21 - 12 - 16
    private val prefix = byteArrayOf(0x4d, 0x54, 0x42, 0x4b, 1)
    private val random = SecureRandom()

    data class BackupKey(val salt: ByteArray, val key: ByteArray) {
        override fun toString(): String = "BackupKey([redacted])"
        fun clear() { salt.fill(0); key.fill(0) }
    }

    fun newBackupKey(password: CharArray): BackupKey {
        requireStrongBackupPassword(password)
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        return BackupKey(salt, derive(password, salt))
    }

    /** Creation policy only; decrypting older archives never applies this policy. */
    fun requireStrongBackupPassword(password: CharArray) {
        require(password.size in 16..MAX_PASSWORD_CHARS) { "Backup password must contain 16 to 1024 characters" }
        val lower = CharArray(password.size) { password[it].lowercaseChar() }
        try {
            require(lower.toSet().size >= 6 && lower.any { !it.isWhitespace() }) { "Backup password is too predictable" }
            val obvious = listOf("password", "motdepasse", "qwerty", "azerty", "123456789", "letmein", "changeme")
            require(obvious.none { word -> contains(lower, word) }) { "Backup password is too predictable" }
            val repeats = (1..minOf(12, lower.size / 2)).any { period ->
                lower.indices.all { lower[it] == lower[it % period] }
            }
            val sequential = lower.size > 1 && (1 until lower.size).all { lower[it].code - lower[it - 1].code == 1 }
            val reverseSequential = lower.size > 1 && (1 until lower.size).all { lower[it].code - lower[it - 1].code == -1 }
            require(!repeats && !sequential && !reverseSequential) { "Backup password is too predictable" }
        } finally {
            lower.fill('\u0000')
        }
    }

    private fun contains(chars: CharArray, word: String): Boolean =
        (0..chars.size - word.length).any { start -> word.indices.all { chars[start + it] == word[it] } }

    fun derive(password: CharArray, salt: ByteArray): ByteArray {
        require(password.size in 1..MAX_PASSWORD_CHARS) { "Invalid password length" }
        require(salt.size == SALT_BYTES) { "Invalid salt length" }
        val encoded = StandardCharsets.UTF_8.encode(CharBuffer.wrap(password))
        val passwordBytes = ByteArray(encoded.remaining()).also { encoded.get(it) }
        val parameters = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withMemoryAsKB(64 * 1024)
            .withIterations(3)
            .withParallelism(1)
            .withSalt(salt)
            .build()
        val result = ByteArray(KEY_BYTES)
        try {
            Argon2BytesGenerator().apply { init(parameters) }.generateBytes(passwordBytes, result)
            return result
        } catch (error: Exception) {
            result.fill(0)
            throw error
        } finally {
            passwordBytes.fill(0)
            if (encoded.hasArray()) encoded.array().fill(0)
            parameters.clear()
        }
    }

    fun encryptBackup(payload: ByteArray, material: BackupKey): ByteArray {
        require(payload.size <= MAX_BACKUP_PAYLOAD_BYTES) { "Backup payload too large" }
        require(material.salt.size == SALT_BYTES && material.key.size == KEY_BYTES) { "Invalid backup material" }
        val header = prefix + material.salt
        return header + encrypt(payload, material.key, header)
    }

    fun decryptBackup(archive: ByteArray, password: CharArray): ByteArray {
        require(archive.size in (HEADER_BYTES + ENCRYPTED_OVERHEAD)..MAX_ARCHIVE_BYTES) { "Invalid backup size" }
        require(prefix.indices.all { archive[it] == prefix[it] }) { "Unsupported backup format" }
        val header = archive.copyOfRange(0, HEADER_BYTES)
        val salt = archive.copyOfRange(PREFIX_BYTES, HEADER_BYTES)
        val derived = derive(password, salt)
        try {
            return decrypt(archive.copyOfRange(HEADER_BYTES, archive.size), derived, header)
        } finally {
            derived.fill(0)
            salt.fill(0)
        }
    }

    fun encrypt(payload: ByteArray, key: ByteArray, aad: ByteArray = byteArrayOf()): ByteArray {
        require(key.size == KEY_BYTES) { "Invalid encryption key length" }
        require(payload.size <= MAX_ARCHIVE_BYTES - ENCRYPTED_OVERHEAD) { "Payload too large" }
        require(aad.size <= MAX_ARCHIVE_BYTES) { "Associated data too large" }
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BYTES * 8, iv))
        cipher.updateAAD(aad)
        return iv + cipher.doFinal(payload)
    }

    fun decrypt(encrypted: ByteArray, key: ByteArray, aad: ByteArray = byteArrayOf()): ByteArray {
        require(key.size == KEY_BYTES) { "Invalid decryption key length" }
        require(encrypted.size in ENCRYPTED_OVERHEAD..MAX_ARCHIVE_BYTES) { "Invalid encrypted data size" }
        require(aad.size <= MAX_ARCHIVE_BYTES) { "Associated data too large" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"),
            GCMParameterSpec(TAG_BYTES * 8, encrypted, 0, IV_BYTES))
        cipher.updateAAD(aad)
        try {
            return cipher.doFinal(encrypted, IV_BYTES, encrypted.size - IV_BYTES)
        } catch (_: GeneralSecurityException) {
            throw GeneralSecurityException("Encrypted data authentication failed")
        }
    }
}
