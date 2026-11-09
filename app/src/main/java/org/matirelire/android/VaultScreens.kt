package org.matirelire.android

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.text.DateFormat
import java.util.Date

/** The gate is never a cosmetic pause screen: only the controller can open it. */
@Composable
fun VaultGate(controller: VaultController, activity: FragmentActivity, content: @Composable () -> Unit) {
    // Keep registrations alive even when onStop locks and disposes the settings.
    val directoryPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) controller.selectDirectory(uri)
    }
    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) controller.selectRestore(uri)
    }
    SideEffect {
        controller.pickDirectory = { directoryPicker.launch(null) }
        controller.pickRestore = { restorePicker.launch(arrayOf("*/*")) }
    }
    if (controller.unlocked) {
        content()
        return
    }
    var code by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    ClearSecretsOnStop { code = ""; confirmation = "" }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.vault_title), style = MaterialTheme.typography.headlineMedium)
            PrototypeNote()
            if (controller.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            controller.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            when {
                controller.needsCode -> {
                    Text(stringResource(R.string.vault_extra_required))
                    SecretField(code, { code = it }, R.string.vault_extra_code, !controller.busy)
                    Button(onClick = { controller.submitExtraCode(code); code = "" },
                        enabled = !controller.busy && code.isNotBlank()) {
                        Text(stringResource(R.string.vault_continue))
                    }
                }
                !controller.exists && controller.restorePending -> RestoreForm(controller, activity)
                controller.exists -> {
                    Text(stringResource(R.string.vault_locked_description))
                    if (controller.restorePending) Text(stringResource(R.string.vault_restore_unlock_first))
                    Button(onClick = { controller.unlock(activity) }, enabled = !controller.busy) {
                        Text(stringResource(R.string.vault_unlock_phone))
                    }
                }
                else -> {
                    Text(stringResource(R.string.vault_create_description))
                    Text(stringResource(R.string.vault_optional_code_hint), style = MaterialTheme.typography.bodySmall)
                    SecretField(code, { code = it }, R.string.vault_optional_code, !controller.busy)
                    if (code.isNotEmpty()) SecretField(confirmation, { confirmation = it }, R.string.vault_confirm_code, !controller.busy)
                    val codeMatches = code.isEmpty() || code == confirmation
                    if (!codeMatches && confirmation.isNotEmpty()) Text(stringResource(R.string.vault_mismatch), color = MaterialTheme.colorScheme.error)
                    Button(onClick = { controller.create(activity, code); code = ""; confirmation = "" },
                        enabled = !controller.busy && codeMatches) {
                        Text(stringResource(R.string.vault_create))
                    }
                    OutlinedButton(onClick = { controller.pickRestore?.invoke() }, enabled = !controller.busy) {
                        Text(stringResource(R.string.vault_restore_pick))
                    }
                }
            }
        }
    }
}

/** Embedded in the existing settings screen, whose parent owns scrolling. */
@Composable
fun VaultSettings(controller: VaultController, activity: FragmentActivity) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var showLicenses by remember { mutableStateOf(false) }
    ClearSecretsOnStop { password = ""; confirmation = "" }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider()
        Text(stringResource(R.string.vault_settings_title), style = MaterialTheme.typography.titleLarge)
        PrototypeNote()
        if (controller.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        controller.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text(stringResource(R.string.vault_local_directory_hint), style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = { controller.pickDirectory?.invoke() }, enabled = !controller.busy) {
            Text(stringResource(if (controller.directorySelected) R.string.vault_change_directory else R.string.vault_choose_directory))
        }
        if (controller.directorySelected) {
            Text(stringResource(R.string.vault_directory_selected), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(if (controller.backupConfigured) R.string.vault_change_password_hint else R.string.vault_setup_password_hint))
            SecretField(password, { password = it }, R.string.vault_backup_password, !controller.busy)
            SecretField(confirmation, { confirmation = it }, R.string.vault_confirm_password, !controller.busy)
            if (confirmation.isNotEmpty() && password != confirmation) Text(stringResource(R.string.vault_mismatch), color = MaterialTheme.colorScheme.error)
            Button(onClick = { controller.configureBackup(password); password = ""; confirmation = "" },
                enabled = !controller.busy && password.length >= 16 && password == confirmation) {
                Text(stringResource(R.string.vault_configure_backup))
            }
        }
        Text(stringResource(R.string.vault_weekly_hint), style = MaterialTheme.typography.bodySmall)
        val last = if (controller.lastBackupMillis > 0L) DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(controller.lastBackupMillis))
            else stringResource(R.string.vault_no_backup)
        Text(stringResource(R.string.vault_last_backup, last))
        Button(onClick = controller::backupNow, enabled = controller.backupConfigured && !controller.busy) {
            Text(stringResource(R.string.vault_backup_now))
        }
        HorizontalDivider()
        Text(stringResource(R.string.vault_restore_description))
        OutlinedButton(onClick = { controller.pickRestore?.invoke() }, enabled = !controller.busy) {
            Text(stringResource(R.string.vault_restore_pick))
        }
        if (controller.restorePending) RestoreForm(controller, activity)
        HorizontalDivider()
        OutlinedButton(onClick = controller::lock, enabled = !controller.busy) { Text(stringResource(R.string.vault_lock_now)) }
        Text(stringResource(R.string.vault_license_notice), style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = { showLicenses = !showLicenses }) {
            Text(stringResource(R.string.vault_licenses))
        }
        if (showLicenses) {
            val notices = remember(activity) {
                activity.assets.open("licenses.txt").bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
            Text(notices, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun RestoreForm(controller: VaultController, activity: FragmentActivity) {
    var password by remember { mutableStateOf("") }
    var extraCode by remember { mutableStateOf("") }
    var codeConfirmation by remember { mutableStateOf("") }
    var accepted by remember { mutableStateOf(false) }
    ClearSecretsOnStop { password = ""; extraCode = ""; codeConfirmation = ""; accepted = false }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { controller.pickRestore?.invoke() }, enabled = !controller.busy) {
            Text(stringResource(R.string.vault_restore_pick))
        }
        Text(stringResource(R.string.vault_restore_password_hint))
        SecretField(password, { password = it }, R.string.vault_backup_password, !controller.busy)
        if (!controller.exists) {
            Text(stringResource(R.string.vault_optional_code_hint), style = MaterialTheme.typography.bodySmall)
            SecretField(extraCode, { extraCode = it }, R.string.vault_optional_code, !controller.busy)
            if (extraCode.isNotEmpty()) SecretField(codeConfirmation, { codeConfirmation = it }, R.string.vault_confirm_code, !controller.busy)
        }
        Text(stringResource(R.string.vault_restore_warning), color = MaterialTheme.colorScheme.error)
        Row(Modifier.fillMaxWidth()) {
            Checkbox(checked = accepted, onCheckedChange = { accepted = it }, enabled = !controller.busy)
            Text(stringResource(R.string.vault_restore_accept), modifier = Modifier.weight(1f).padding(top = 12.dp))
        }
        Button(onClick = {
            controller.restore(activity, password, extraCode)
            password = ""
            extraCode = ""
            codeConfirmation = ""
            accepted = false
        }, enabled = !controller.busy && accepted && password.isNotEmpty() &&
            (controller.exists || extraCode.isEmpty() || extraCode == codeConfirmation)) {
            Text(stringResource(R.string.vault_restore_execute))
        }
    }
}

@Composable
private fun PrototypeNote() {
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.medium) {
        Text(stringResource(R.string.vault_prototype_note), modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onTertiaryContainer, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SecretField(value: String, onValueChange: (String) -> Unit, label: Int, enabled: Boolean) {
    OutlinedTextField(value = value, onValueChange = { onValueChange(it.take(256)) },
        label = { Text(stringResource(label)) }, visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), enabled = enabled,
        singleLine = true, modifier = Modifier.fillMaxWidth())
}

/** Input secrets are not retained while the app is in the background. */
@Composable
private fun ClearSecretsOnStop(clear: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val currentClear by rememberUpdatedState(clear)
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) currentClear() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}
