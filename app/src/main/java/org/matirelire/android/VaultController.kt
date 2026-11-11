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
import android.hardware.biometrics.BiometricManager.Authenticators.*
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
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
import org.matirelire.android.security.KeyEnvelope
import java.security.KeyStore
import java.security.SecureRandom
import java.util.UUID
import java.io.ByteArrayOutputStream
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class VaultController(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val store = VaultStore(app)
    private lateinit var ledger: DemoViewModel
    var showReleaseNotes by mutableStateOf(false); private set
    var exists by mutableStateOf(store.exists); private set
    var unlocked by mutableStateOf(false); private set
    var needsCode by mutableStateOf(false); private set
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var feedback by mutableStateOf<Int?>(null); private set
    fun dismissFeedback() { feedback = null }
    private fun result(id: Int) { feedback = id }
    var backupConfigured by mutableStateOf(false); private set
    var directorySelected by mutableStateOf(false); private set
    var lastBackupMillis by mutableLongStateOf(0L); private set
    var restorePending by mutableStateOf(false); private set
    private var pendingInner: ByteArray? = null
    private var pendingArchive: ByteArray? = null // ciphertext only; can survive a lock
    private var pendingDirectory: Uri? = null
    private var epoch = 0L
    var unlockGeneration by mutableIntStateOf(0); private set
    private val automaticUnlock = AutomaticUnlock()
    private var job: Job? = null
    private var authenticating = false
    private var authenticationCancellation: CancellationSignal? = null
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
        val keyFailure = generateSequence(error as Throwable) { it.cause }
            .filterIsInstance<android.security.KeyStoreException>().firstOrNull()
        val code = if (android.os.Build.VERSION.SDK_INT >= 33 && keyFailure != null)
            ":code=${keyFailure.numericErrorCode}:authRequired=${keyFailure.requiresUserAuthentication()}" else ""
        Log.w("MaTirelireVault", "failure:$operation:$types$code")
    }

    internal fun bindModel(model: DemoViewModel) { ledger = model }
    private fun message(id: Int) { error = app.getString(id); result(id) }
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

    fun unlockAutomatically(activity: FragmentActivity) {
        if (automaticUnlock.shouldRequest(exists, unlocked, busy, needsCode)) unlock(activity)
    }

    fun onBackground() {
        // Android's credential UI may stop the host; it never exposes an open ledger.
        if (!authenticating) { lock(); automaticUnlock.onLock() }
    }

    fun openReleaseNotes() { if (unlocked) showReleaseNotes = true }

    fun acknowledgeReleaseNotes() {
        if (!unlocked) return
        try {
            val bytes = BuildConfig.VERSION_NAME.toByteArray(Charsets.UTF_8)
            try { store.put("releaseSeen", bytes) } finally { bytes.fill(0) }
            showReleaseNotes = false
        } catch (_: Exception) { lock(); message(R.string.vault_save_error) }
    }

    fun lock() {
        showReleaseNotes = false
        if (unlocked && ::ledger.isInitialized) resumeDestination = ledger.destination.intValue
        epoch++
        automaticUnlock.onLock(allowAutomatic = false)
        unlockGeneration++
        feedback = null
        unlocked = false
        needsCode = false
        job?.cancel(); job = null
        busy = false
        authenticationCancellation?.cancel(); authenticationCancellation = null
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

    private fun authenticate(activity: FragmentActivity, cipher: Cipher,
        onSuccess: (Cipher) -> Unit, onFailure: () -> Unit = {}) {
        val ticket = epoch
        authenticating = true
        busy = true
        val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val creating = !exists
                    activity.lifecycleScope.launch {
                        var completed = false
                        try {
                            // Credential UI can deliver its result before the host resumes.
                            // Wait for visibility instead of discarding a legitimate result.
                            activity.lifecycle.withStateAtLeast(Lifecycle.State.RESUMED) {
                                if (ticket == epoch) {
                                    authenticating = false; busy = false; authenticationCancellation = null
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
                                authenticating = false; busy = false; authenticationCancellation = null
                            }
                        }
                    }
                }
                override fun onAuthenticationError(code: Int, text: CharSequence) {
                    authenticating = false; busy = false; authenticationCancellation = null
                    onFailure()
                    if (ticket == epoch) message(R.string.vault_auth_cancelled)
                }
            }
        // Use the platform API supported by our minimum Android 12. Both choices
        // remain available: the credential fallback is never a simulated success.
        val nativePrompt = BiometricPrompt.Builder(activity)
            .setTitle(app.getString(R.string.brand))
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()
        val cancellation = CancellationSignal()
        authenticationCancellation = cancellation
        try {
            nativePrompt.authenticate(BiometricPrompt.CryptoObject(cipher), cancellation,
                ContextCompat.getMainExecutor(activity), callback)
        } catch (e: Exception) {
            authenticating = false; busy = false; authenticationCancellation = null; onFailure(); throw e
        }
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
                val cipher = KeyEnvelope.prepareEncryption(wrappingKey())
                val wrappedInner = requireNotNull(inner)
                // Auth owns these arrays until its callback; do not put them in saved state.
                authenticate(activity, cipher, onSuccess = { authorized ->
                    try {
                        operation = "create-seal"
                        val envelope = KeyEnvelope.sealAfterAuthentication(authorized, wrappedInner)
                        operation = "create-envelope-write"
                        store.writeEnvelope(envelope)
                        exists = true
                        operation = "create-database-open"
                        openSession(key, restored)
                        if (restored != null) {
                            pendingArchive?.fill(0); pendingArchive = null; restorePending = false
                            result(R.string.vault_restore_success)
                        }
                    } finally { key.fill(0); wrappedInner.fill(0) }
                }, onFailure = { key.fill(0); wrappedInner.fill(0) })
                inner = null // transferred to callback
            } finally { code.fill('\u0000'); inner?.fill(0); if (!authenticating) key.fill(0) }
        }
    }

    fun unlock(activity: FragmentActivity) {
        if (!exists || unlocked || busy || authenticating) return
        error = null
        try {
            val bytes = store.readEnvelope()
            val cipher = KeyEnvelope.prepareDecryption(wrappingKey(), bytes)
            authenticate(activity, cipher, { authorized ->
                operation = "unlock-envelope"
                val inner = KeyEnvelope.openAfterAuthentication(authorized, bytes)
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
        val seenRelease = store.get("releaseSeen")
        try { showReleaseNotes = seenRelease?.toString(Charsets.UTF_8) != BuildConfig.VERSION_NAME }
        finally { seenRelease?.fill(0) }
        unlocked = true
        ledger.destination.intValue = resumeDestination
        needsCode = false
        error = null
        pendingDirectory?.let { uri ->
            val config = config().put("tree", uri.toString()).put("autoFiles", JSONArray()).put("lastAuto", 0)
            saveConfig(config); pendingDirectory = null
            result(R.string.vault_directory_resume)
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
                result(R.string.vault_configure_success)
            } finally { chars.fill('\u0000') }
        }
    }

    fun migrateDates() {
        if (!unlocked) return
        work {
            operation = "date-migration"
            try {
                val before = ledger.exportData()
                if (DateMigration.plan(before).data != before) {
                    val config = config()
                    val verified = runCatching {
                        val archive = withContext(Dispatchers.IO) { readArchive(Uri.parse(config.getString("lastArchiveUri"))) }
                        try { BackupCheckpoint.matches(before, config.getString("lastSnapshotHash"), archive, config.getString("lastArchiveHash")) }
                        finally { archive.fill(0) }
                    }.getOrElse { failure ->
                        if (failure is CancellationException) throw failure
                        false
                    }
                    if (!verified) { message(R.string.date_migration_backup_required); return@work }
                }
                val plan = DateMigration.execute(before,
                    backup = { /* Existing checkpoint verified above; no new backup. */ }, current = { ledger.exportData() }, replace = { data ->
                        val encoded = SnapshotCodec.encode(data)
                        try { store.put("ledger", encoded) } finally { encoded.fill(0) }
                        ledger.restoreData(data)
                        resumeDestination = 2
                        ledger.destination.intValue = 2
                    })
                refreshConfig()
                result(if (plan.unresolved > 0) R.string.date_migration_partial else R.string.date_migration_success)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                diagnostic(failure)
                message(if (!backupConfigured) R.string.vault_backup_required else R.string.date_migration_error)
            }
        }
    }

    fun backupNow() = backupNow(false)
    private fun backupNow(automatic: Boolean) {
        if (!unlocked) return
        work {
            makeBackup(automatic)
            refreshConfig()
            if (!automatic) result(R.string.vault_backup_success)
        }
    }

    private suspend fun makeBackup(automatic: Boolean) {
        val config = config()
        require(config.optString("tree").isNotEmpty() && config.optString("key").isNotEmpty())
        val material = VaultCrypto.BackupKey(un64(config.getString("salt")), un64(config.getString("key")))
        val payload = SnapshotCodec.encode(ledger.exportData())
        val snapshotHash = BackupCheckpoint.hash(payload)
        val archive = try { VaultCrypto.encryptBackup(payload, material) }
            finally { material.clear(); payload.fill(0) }
        val archiveHash = BackupCheckpoint.hash(archive)
        val uri = try { withContext(Dispatchers.IO) { writeArchive(Uri.parse(config.getString("tree")), archive, automatic) } }
            finally { archive.fill(0) }
        val now = System.currentTimeMillis()
        config.put("lastSuccess", now)
        config.put("lastSnapshotHash", snapshotHash).put("lastArchiveHash", archiveHash).put("lastArchiveUri", uri.toString())
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
            operation = "restore-decrypt-validate"
            val chars = password.toCharArray()
            val data = try {
                withContext(Dispatchers.Default) {
                    val bytes = VaultCrypto.decryptBackup(archive, chars)
                    try { SnapshotCodec.decode(bytes) } finally { bytes.fill(0) }
                }
            } finally { chars.fill('\u0000') }
            if (exists) {
                if (!backupConfigured) { message(R.string.vault_backup_required); return@work }
                operation = "restore-safety-backup"
                makeBackup(false) // mandatory, verified recovery archive before replacement
                operation = "restore-replace"
                val encoded = SnapshotCodec.encode(data)
                try { store.put("ledger", encoded); ledger.restoreData(data) } finally { encoded.fill(0) }
                pendingArchive?.fill(0); pendingArchive = null; restorePending = false
                refreshConfig()
                result(R.string.vault_restore_success)
            } else {
                // Start after the current operation has released its busy guard.
                viewModelScope.launch { delay(1); if (!exists) createVault(activity, extraCode, data) }
            }
        }
    }

    override fun onCleared() { lock(); pendingArchive?.fill(0); super.onCleared() }
}
