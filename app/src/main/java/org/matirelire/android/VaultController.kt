package org.matirelire.android

import android.app.Application
import android.app.KeyguardManager
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStateAtLeast
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import org.matirelire.android.security.VaultCrypto
import java.security.KeyStore
import java.security.SecureRandom
import java.util.UUID
import java.io.ByteArrayOutputStream
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class VaultController(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val store = VaultStore(app)
    private lateinit var ledger: DemoViewModel
    var exists by mutableStateOf(store.exists); private set
    var unlocked by mutableStateOf(false); private set
    var needsCode by mutableStateOf(false); private set
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var backupConfigured by mutableStateOf(false); private set
    var directorySelected by mutableStateOf(false); private set
    var lastBackupMillis by mutableLongStateOf(0L); private set
    var restorePending by mutableStateOf(false); private set
    private var pendingInner: ByteArray? = null
    private var pendingArchive: ByteArray? = null // ciphertext only; can survive a lock
    private var pendingDirectory: Uri? = null
    private var epoch = 0L
    private var job: Job? = null
    private var authenticating = false
    private var prompt: BiometricPrompt? = null
    var pickDirectory: (() -> Unit)? = null
    var pickRestore: (() -> Unit)? = null
    private var resumeDestination = 0
    private val aad = "MaTirelire-vault-key-v1".toByteArray()
    private val alias = "matirelire-vault-wrap-v1"
    private var operation = "idle"
    private fun diagnostic(error: Exception) {
        // Deliberately omit messages, stack traces, payloads and all entered secrets.
        val types = generateSequence(error as Throwable) { it.cause }.take(4)
            .joinToString("/") { it.javaClass.simpleName }
        Log.w("MaTirelireVault", "failure:$operation:$types")
    }

    internal fun bindModel(model: DemoViewModel) { ledger = model }
    private fun message(id: Int) { error = app.getString(id) }
    private fun b64(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun un64(value: String) = Base64.decode(value, Base64.NO_WRAP)

    private fun work(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = null
        val ticket = epoch
        job = viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (failure: Exception) { diagnostic(failure); if (ticket == epoch) message(R.string.vault_error) }
            finally { if (ticket == epoch && !authenticating) busy = false }
        }
    }

    fun onBackground() {
        // Android's credential UI may stop the host; it never exposes an open ledger.
        if (!authenticating) lock()
    }

    fun lock() {
        if (unlocked && ::ledger.isInitialized) resumeDestination = ledger.destination.intValue
        epoch++
        unlocked = false
        needsCode = false
        job?.cancel(); job = null
        busy = false
        prompt?.cancelAuthentication(); prompt = null
        authenticating = false
        pendingInner?.fill(0); pendingInner = null
        if (::ledger.isInitialized) { ledger.onChange = null; ledger.clearSensitive() }
        store.close()
        backupConfigured = false
        lastBackupMillis = 0
        directorySelected = pendingDirectory != null
    }

    private fun wrappingKey(): SecretKey {
        val keys = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keys.containsAlias(alias)) return keys.getKey(alias, null) as SecretKey
        check(!exists) // never silently replace the key of an existing vault
        check(app.getSystemService(KeyguardManager::class.java).isDeviceSecure)
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL)
                .setInvalidatedByBiometricEnrollment(false).build())
            generateKey()
        }
    }

    private fun authenticate(activity: FragmentActivity, cipher: Cipher, biometric: Boolean,
        onSuccess: (Cipher) -> Unit, onFailure: () -> Unit = {}) {
        val ticket = epoch
        authenticating = true
        busy = true
        prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val creating = !exists
                    activity.lifecycleScope.launch {
                        var completed = false
                        try {
                            // Credential UI can deliver its result before the host resumes.
                            // Wait for visibility instead of discarding a legitimate result.
                            activity.lifecycle.withStateAtLeast(Lifecycle.State.RESUMED) {
                                if (ticket == epoch) {
                                    authenticating = false; busy = false; prompt = null
                                    // Some credential implementations omit the returned CryptoObject.
                                    // Reuse the EXACT operation passed to Android: Keystore still
                                    // enforces authentication on doFinal, so no boolean bypass exists.
                                    onSuccess(result.cryptoObject?.cipher ?: cipher)
                                    completed = true
                                }
                            }
                        } catch (failure: CancellationException) {
                            throw failure
                        } catch (failure: Exception) {
                            diagnostic(failure)
                            lock()
                            message(if (creating) R.string.vault_create_error else R.string.vault_unlock_error)
                        } finally {
                            if (!completed) {
                                onFailure()
                                authenticating = false; busy = false; prompt = null
                            }
                        }
                    }
                }
                override fun onAuthenticationError(code: Int, text: CharSequence) {
                    authenticating = false; busy = false; prompt = null
                    onFailure()
                    if (ticket == epoch) message(R.string.vault_auth_cancelled)
                }
            })
        val builder = BiometricPrompt.PromptInfo.Builder().setTitle(app.getString(R.string.brand))
            .setAllowedAuthenticators(if (biometric) BIOMETRIC_STRONG or DEVICE_CREDENTIAL else DEVICE_CREDENTIAL)
        try { prompt!!.authenticate(builder.build(), BiometricPrompt.CryptoObject(cipher)) }
        catch (e: Exception) { authenticating = false; busy = false; prompt = null; onFailure(); throw e }
    }

    fun create(activity: FragmentActivity, extraCode: String) {
        if (exists || authenticating) return
        createVault(activity, extraCode, null)
    }

    private fun createVault(activity: FragmentActivity, extraCode: String, restored: LedgerData?) {
        if (extraCode.isNotEmpty() && (extraCode.length < 8 || extraCode.length > 1024)) {
            message(R.string.vault_code_weak); return
        }
        work {
            val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
            val code = extraCode.toCharArray()
            var inner: ByteArray? = null
            try {
                inner = withContext(Dispatchers.Default) {
                    if (code.isEmpty()) byteArrayOf(0) + key else {
                        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
                        val derived = VaultCrypto.derive(code, salt)
                        try { byteArrayOf(1) + salt + VaultCrypto.encrypt(key, derived, aad) }
                        finally { derived.fill(0); salt.fill(0) }
                    }
                }
                val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                    init(Cipher.ENCRYPT_MODE, wrappingKey()); updateAAD(aad)
                }
                val wrappedInner = requireNotNull(inner)
                // Auth owns these arrays until its callback; do not put them in saved state.
                authenticate(activity, cipher, false, onSuccess = { authorized ->
                    try {
                        operation = "create-seal"
                        val envelope = authorized.iv + authorized.doFinal(wrappedInner)
                        operation = "create-envelope-write"
                        store.writeEnvelope(envelope)
                        exists = true
                        operation = "create-database-open"
                        openSession(key, restored)
                        if (restored != null) {
                            pendingArchive?.fill(0); pendingArchive = null; restorePending = false
                        }
                    } finally { key.fill(0); wrappedInner.fill(0) }
                }, onFailure = { key.fill(0); wrappedInner.fill(0) })
                inner = null // transferred to callback
            } finally { code.fill('\u0000'); inner?.fill(0); if (!authenticating) key.fill(0) }
        }
    }

    fun unlock(activity: FragmentActivity, useBiometrics: Boolean) {
        if (!exists || unlocked || busy || authenticating) return
        error = null
        try {
            val bytes = store.readEnvelope()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, wrappingKey(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
                updateAAD(aad)
            }
            authenticate(activity, cipher, useBiometrics, { authorized ->
                operation = "unlock-envelope"
                val inner = authorized.doFinal(bytes.copyOfRange(12, bytes.size))
                if (inner.firstOrNull() == 0.toByte() && inner.size == 33) {
                    val key = inner.copyOfRange(1, 33)
                    try { openSession(key) } finally { key.fill(0); inner.fill(0) }
                } else {
                    require(inner.size == 77 && inner[0] == 1.toByte())
                    pendingInner?.fill(0); pendingInner = inner; needsCode = true
                }
            })
        } catch (_: Exception) { message(R.string.vault_corrupt_data) }
    }

    fun submitExtraCode(code: String) {
        val inner = pendingInner ?: return
        work {
            val chars = code.toCharArray()
            try {
                val key = withContext(Dispatchers.Default) {
                    val derived = VaultCrypto.derive(chars, inner.copyOfRange(1, 17))
                    try { VaultCrypto.decrypt(inner.copyOfRange(17, inner.size), derived, aad) }
                    finally { derived.fill(0) }
                }
                try { openSession(key) } finally { key.fill(0) }
                pendingInner?.fill(0); pendingInner = null; needsCode = false
            } finally { chars.fill('\u0000') }
        }
    }

    private fun openSession(key: ByteArray, restored: LedgerData? = null) {
        operation = "database-open"
        store.open(key)
        operation = "database-read"
        ledger.onChange = null
        val bytes = store.get("ledger")
        try {
            if (restored != null) {
                ledger.restoreData(restored)
                val encoded = SnapshotCodec.encode(restored)
                try { store.put("ledger", encoded) } finally { encoded.fill(0) }
            } else if (bytes != null) ledger.restoreData(SnapshotCodec.decode(bytes))
        } finally { bytes?.fill(0) }
        ledger.onChange = {
            try {
                val encoded = SnapshotCodec.encode(ledger.exportData())
                try { store.put("ledger", encoded) } finally { encoded.fill(0) }
            } catch (_: Exception) { lock(); message(R.string.vault_save_error) }
        }
        unlocked = true
        ledger.destination.intValue = resumeDestination
        needsCode = false
        error = null
        pendingDirectory?.let { uri ->
            val config = config().put("tree", uri.toString()).put("autoFiles", JSONArray()).put("lastAuto", 0)
            saveConfig(config); pendingDirectory = null
        }
        refreshConfig()
        val config = config()
        if (backupConfigured && System.currentTimeMillis() - config.optLong("lastAuto") >= 7L * 24 * 60 * 60 * 1000) {
            // Wait for the first composition to initialize the demonstration if needed.
            viewModelScope.launch { delay(100); if (unlocked) backupNow(automatic = true) }
        }
    }

    private fun config(): JSONObject {
        val bytes = store.get("backup") ?: return JSONObject()
        return try { JSONObject(bytes.toString(Charsets.UTF_8)) } finally { bytes.fill(0) }
    }
    private fun saveConfig(config: JSONObject) {
        val bytes = config.toString().toByteArray(Charsets.UTF_8)
        try { store.put("backup", bytes) } finally { bytes.fill(0) }
    }
    private fun refreshConfig() {
        val config = config()
        directorySelected = config.optString("tree").isNotEmpty()
        backupConfigured = directorySelected && config.optString("key").isNotEmpty()
        lastBackupMillis = config.optLong("lastSuccess")
    }

    fun selectDirectory(uri: Uri) {
        try {
            require(uri.authority == "com.android.externalstorage.documents")
            app.contentResolver.takePersistableUriPermission(uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            if (unlocked) {
                val config = config().put("tree", uri.toString()).put("autoFiles", JSONArray()).put("lastAuto", 0)
                saveConfig(config); refreshConfig()
            } else { pendingDirectory = uri; directorySelected = true }
        } catch (_: Exception) { message(R.string.vault_directory_error) }
    }

    fun configureBackup(password: String) {
        if (!unlocked) return
        work {
            val chars = password.toCharArray()
            try {
                try { VaultCrypto.requireStrongBackupPassword(chars) }
                catch (_: IllegalArgumentException) { message(R.string.vault_password_weak); return@work }
                val config = config()
                if (config.optString("tree").isEmpty()) { message(R.string.vault_directory_error); return@work }
                val material = withContext(Dispatchers.Default) { VaultCrypto.newBackupKey(chars) }
                try { saveConfig(config.put("key", b64(material.key)).put("salt", b64(material.salt)).put("lastAuto", 0)) }
                finally { material.clear() }
                refreshConfig()
            } finally { chars.fill('\u0000') }
        }
    }

    fun backupNow() = backupNow(false)
    private fun backupNow(automatic: Boolean) {
        if (!unlocked) return
        work { makeBackup(automatic); refreshConfig(); message(R.string.vault_backup_success) }
    }

    private suspend fun makeBackup(automatic: Boolean) {
        val config = config()
        require(config.optString("tree").isNotEmpty() && config.optString("key").isNotEmpty())
        val material = VaultCrypto.BackupKey(un64(config.getString("salt")), un64(config.getString("key")))
        val payload = SnapshotCodec.encode(ledger.exportData())
        val archive = try { VaultCrypto.encryptBackup(payload, material) }
            finally { material.clear(); payload.fill(0) }
        val uri = try { withContext(Dispatchers.IO) { writeArchive(Uri.parse(config.getString("tree")), archive, automatic) } }
            finally { archive.fill(0) }
        val now = System.currentTimeMillis()
        config.put("lastSuccess", now)
        if (automatic) {
            config.put("lastAuto", now)
            val old = config.optJSONArray("autoFiles") ?: JSONArray()
            val files = (0 until old.length()).map { old.getString(it) }.toMutableList()
            files.add(uri.toString())
            config.put("autoFiles", JSONArray(files)); saveConfig(config)
            while (files.size > 8) {
                val oldest = Uri.parse(files.first())
                check(DocumentsContract.deleteDocument(app.contentResolver, oldest))
                files.removeAt(0)
                config.put("autoFiles", JSONArray(files)); saveConfig(config)
            }
        }
        saveConfig(config)
    }

    private fun writeArchive(tree: Uri, bytes: ByteArray, automatic: Boolean): Uri {
        require(tree.authority == "com.android.externalstorage.documents")
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val name = "ma-tirelire-${if (automatic) "auto" else "manual"}-${System.currentTimeMillis()}-${UUID.randomUUID()}.mtbk"
        val uri = requireNotNull(DocumentsContract.createDocument(app.contentResolver, parent, "application/octet-stream", name))
        try {
            requireNotNull(app.contentResolver.openOutputStream(uri, "w")).use { it.write(bytes); it.flush() }
            val verify = readArchive(uri)
            try { check(verify.contentEquals(bytes)) } finally { verify.fill(0) }
            return uri
        } catch (e: Exception) {
            runCatching { DocumentsContract.deleteDocument(app.contentResolver, uri) }
            throw e
        }
    }

    private fun readArchive(uri: Uri): ByteArray {
        require(uri.authority == "com.android.externalstorage.documents")
        return requireNotNull(app.contentResolver.openInputStream(uri)).use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                require(output.size().toLong() + count <= VaultCrypto.MAX_ARCHIVE_BYTES)
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
    }

    fun selectRestore(uri: Uri) {
        work {
            val bytes = withContext(Dispatchers.IO) { readArchive(uri) }
            pendingArchive?.fill(0); pendingArchive = bytes; restorePending = true
        }
    }

    fun restore(activity: FragmentActivity, password: String, extraCode: String) {
        val archive = pendingArchive ?: return
        if (exists && !unlocked) return
        work {
            val chars = password.toCharArray()
            val data = try {
                withContext(Dispatchers.Default) {
                    val bytes = VaultCrypto.decryptBackup(archive, chars)
                    try { SnapshotCodec.decode(bytes) } finally { bytes.fill(0) }
                }
            } finally { chars.fill('\u0000') }
            if (exists) {
                if (!backupConfigured) { message(R.string.vault_backup_required); return@work }
                makeBackup(false) // mandatory, verified recovery archive before replacement
                val encoded = SnapshotCodec.encode(data)
                try { store.put("ledger", encoded); ledger.restoreData(data) } finally { encoded.fill(0) }
                pendingArchive?.fill(0); pendingArchive = null; restorePending = false
            } else {
                // Start after the current operation has released its busy guard.
                viewModelScope.launch { delay(1); if (!exists) createVault(activity, extraCode, data) }
            }
        }
    }

    override fun onCleared() { lock(); pendingArchive?.fill(0); super.onCleared() }
}
