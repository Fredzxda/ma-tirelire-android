package org.matirelire.android

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy

/** Kept only in the unlocked ViewModel; never serialized into an Android saved-state Bundle. */
@Suppress("UNCHECKED_CAST")
private fun <T> referenceMemory(model: DemoViewModel, key: String, init: () -> T): MutableState<T> =
    model.referenceUiStates.getOrPut(key) { mutableStateOf(init()) } as MutableState<T>

/** Reference management stays outside the compact Operations header. */
@Composable
internal fun ReferenceManagementDialog(model: DemoViewModel, onDismiss: () -> Unit) {
    var category by referenceMemory(model, "dialog-category") { true }
    var selected by referenceMemory(model, "dialog-selected-$category") { "" }
    val names = if (category) model.categories else model.payments
    val current = selected.takeIf { it in names } ?: names.firstOrNull()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(
        usePlatformDefaultWidth = false, securePolicy = SecureFlagPolicy.SecureOn)) {
        Surface(Modifier.padding(12.dp).widthIn(max = 640.dp).fillMaxWidth(),
            shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.refs_title), style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = category, onClick = { category = true },
                        label = { Text(stringResource(R.string.refs_categories)) })
                    FilterChip(selected = !category, onClick = { category = false },
                        label = { Text(stringResource(R.string.refs_payments)) })
                }
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReferenceChoice(stringResource(if (category) R.string.category else R.string.payment),
                        current.orEmpty(), names.map { it to it }, { selected = it })
                    if (current != null) key(category, current) {
                        ReferenceEditor(model, category, current) { selected = it }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.refs_close))
                }
            }
        }
    }
}

@Composable
private fun ReferenceEditor(model: DemoViewModel, category: Boolean, original: String,
    onSelected: (String) -> Unit) {
    val stored = model.referenceOptions[(if (category) "c:" else "p:") + original] ?: ReferenceOptions()
    var name by referenceMemory(model, "editor-$category-$original-name") { original }
    var parent by referenceMemory(model, "editor-$category-$original-parent") { stored.parent.orEmpty() }
    var kindName by referenceMemory(model, "editor-$category-$original-kindName") { stored.kind.name }
    var hidden by referenceMemory(model, "editor-$category-$original-hidden") { stored.hidden }
    var accountIds by referenceMemory(model, "editor-$category-$original-accountIds") { stored.accounts.sorted().joinToString(",") }
    var replacement by referenceMemory(model, "editor-$category-$original-replacement") { "" }
    var error by referenceMemory(model, "editor-$category-$original-error") { false }
    var saved by referenceMemory(model, "editor-$category-$original-saved") { false }
    var confirmDelete by referenceMemory(model, "editor-$category-$original-confirmDelete") { false }
    val dedicatedTransfer = !category && original == model.transferPayment
    val accounts = accountIds.split(',').mapNotNull(String::toIntOrNull).toSet()
    fun edited() { error = false; saved = false }
    PinnedLabelField(name, { name = it; edited() }, stringResource(R.string.reference_name),
        Modifier.fillMaxWidth(), maxLength = 60)
    if (category) {
        val noParent = stringResource(R.string.refs_no_parent)
        ReferenceChoice(stringResource(R.string.refs_parent),
            parent.ifEmpty { noParent },
            listOf("" to noParent) + model.categories.filter { it != original }.map { it to it },
            { parent = it; edited() })
    }
    if (!dedicatedTransfer) {
        ReferenceChoice(stringResource(R.string.refs_kind), stringResource(when (ReferenceKind.valueOf(kindName)) {
            ReferenceKind.ANY -> R.string.refs_any
            ReferenceKind.EXPENSE -> R.string.expense
            ReferenceKind.INCOME -> R.string.income
        }), ReferenceKind.entries.map { kind -> kind.name to stringResource(when (kind) {
            ReferenceKind.ANY -> R.string.refs_any
            ReferenceKind.EXPENSE -> R.string.expense
            ReferenceKind.INCOME -> R.string.income
        }) }, { kindName = it; edited() })
        ReferenceCheck(stringResource(R.string.refs_hidden), hidden) { hidden = it; edited() }
        Text(stringResource(R.string.refs_accounts), style = MaterialTheme.typography.labelLarge)
        Text(stringResource(R.string.refs_accounts_hint), style = MaterialTheme.typography.bodySmall)
        ReferenceCheck(stringResource(R.string.refs_all_accounts), accounts.isEmpty()) {
            accountIds = if (it) "" else model.accountNames.indices.joinToString(",")
            edited()
        }
        model.accountNames.forEachIndexed { index, account ->
            ReferenceCheck(account, accounts.isEmpty() || index in accounts,
                enabled = accounts.size != 1 || index !in accounts) { checked ->
                val explicit = if (accounts.isEmpty()) model.accountNames.indices.toSet() else accounts
                val updated = if (checked) explicit + index else explicit - index
                // Empty has the documented meaning "all accounts"; use hidden to disable everywhere.
                accountIds = updated.sorted().joinToString(",")
                edited()
            }
        }
    } else Text(stringResource(R.string.refs_transfer_protected), style = MaterialTheme.typography.bodySmall)
    if (error) Text(stringResource(R.string.refs_error), color = MaterialTheme.colorScheme.error)
    if (saved) Text(stringResource(R.string.refs_saved), color = MaterialTheme.colorScheme.primary)
    Button(onClick = {
        val options = if (dedicatedTransfer) stored else ReferenceOptions(
            parent = if (category) parent.ifBlank { null } else null,
            kind = ReferenceKind.valueOf(kindName), hidden = hidden, accounts = accounts)
        if (model.saveReference(category, original, name, options)) {
            onSelected(name.trim()); error = false; saved = true
        } else { error = true; saved = false }
    }, enabled = name.isNotBlank()) { Text(stringResource(R.string.save)) }
    if (!dedicatedTransfer) {
        HorizontalDivider()
        Text(stringResource(R.string.refs_remove_hint), style = MaterialTheme.typography.bodySmall)
        val none = stringResource(R.string.refs_no_replacement)
        ReferenceChoice(stringResource(R.string.refs_replacement), replacement.ifEmpty { none },
            listOf("" to none) + (if (category) model.categories else model.payments)
                .filter { it != original && (category || it != model.transferPayment) }.map { it to it },
            { replacement = it; edited() })
        TextButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.refs_remove)) }
    }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false },
        title = { Text(stringResource(R.string.refs_remove_title, original)) },
        text = { Text(stringResource(R.string.refs_remove_confirm)) },
        confirmButton = { TextButton(onClick = {
            confirmDelete = false
            if (model.removeReference(category, original, replacement.ifBlank { null })) {
                onSelected(replacement); error = false
            } else { error = true; saved = false }
        }) { Text(stringResource(R.string.refs_remove)) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn))
}

@Composable
private fun ReferenceChoice(label: String, selected: String, choices: List<Pair<String, String>>,
    onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selected, Modifier.weight(1f))
                Text("▾")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                choices.forEach { (value, caption) ->
                    DropdownMenuItem(text = { Text(caption) }, onClick = {
                        expanded = false; onSelect(value)
                    })
                }
            }
        }
    }
}

@Composable
private fun ReferenceCheck(label: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange, enabled = enabled)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
