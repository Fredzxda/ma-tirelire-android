package org.matirelire.android.security

import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.*
import org.junit.Test

class KeyEnvelopeTest {
    private val key = SecretKeySpec(ByteArray(32) { (it + 1).toByte() }, "AES")

    @Test fun preparingEncryptionDoesNotSendAnyAuthenticatedData() {
        val prepared = KeyEnvelope.prepareEncryption(key)
        val reference = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, prepared.iv))
        }
        // A premature updateAAD would change this tag and reintroduce the Keystore failure.
        assertArrayEquals(reference.doFinal(byteArrayOf()), prepared.doFinal(byteArrayOf()))
    }

    @Test fun preparingDecryptionDoesNotSendAnyAuthenticatedData() {
        val plain = "fake-key-material".toByteArray()
        val reference = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }
        val raw = reference.iv + reference.doFinal(plain) // deliberately no AAD
        val prepared = KeyEnvelope.prepareDecryption(key, raw)
        assertArrayEquals(plain, prepared.doFinal(raw, 12, raw.size - 12))
    }

    @Test fun authorizedEnvelopeRoundTripsAndRejectsTampering() {
        val plain = ByteArray(77) { it.toByte() }
        val envelope = KeyEnvelope.sealAfterAuthentication(KeyEnvelope.prepareEncryption(key), plain)
        assertArrayEquals(plain, KeyEnvelope.openAfterAuthentication(KeyEnvelope.prepareDecryption(key, envelope), envelope))
        val tampered = envelope.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
        var rejected = false
        try { KeyEnvelope.openAfterAuthentication(KeyEnvelope.prepareDecryption(key, tampered), tampered) }
        catch (_: java.security.GeneralSecurityException) { rejected = true }
        assertTrue(rejected)
    }
}
