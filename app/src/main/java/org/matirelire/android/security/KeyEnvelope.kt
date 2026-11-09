package org.matirelire.android.security

import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** No data/AAD may be sent to a per-use Keystore operation before authentication. */
object KeyEnvelope {
    private val aad = "MaTirelire-vault-key-v1".toByteArray(Charsets.UTF_8)

    fun prepareEncryption(key: SecretKey): Cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
        init(Cipher.ENCRYPT_MODE, key)
    }

    fun prepareDecryption(key: SecretKey, envelope: ByteArray): Cipher {
        require(envelope.size in 29..1024)
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, envelope.copyOfRange(0, 12)))
        }
    }

    fun sealAfterAuthentication(cipher: Cipher, payload: ByteArray): ByteArray {
        require(payload.size in 1..980)
        cipher.updateAAD(aad)
        return cipher.iv + cipher.doFinal(payload)
    }

    fun openAfterAuthentication(cipher: Cipher, envelope: ByteArray): ByteArray {
        require(envelope.size in 29..1024)
        cipher.updateAAD(aad)
        return cipher.doFinal(envelope, 12, envelope.size - 12)
    }
}
