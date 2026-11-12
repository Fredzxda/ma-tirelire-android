package org.matirelire.android

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy

@Composable
internal fun CategoryTree(model: DemoViewModel, selected: String, selectable: List<String>, stateKey: String,
    onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    var query by referenceMemory(model, "$stateKey-query") { "" }
    var expanded by referenceMemory(model, "$stateKey-expanded") { model.categoryAncestors(selected).dropLast(1).toSet() }
    LaunchedEffect(selected) { expanded = expanded + model.categoryAncestors(selected).dropLast(1) }
    val rows = model.categoryTreeRows(selectable, expanded, query)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PinnedLabelField(query, { query = it }, stringResource(R.string.refs_search),
            Modifier.fillMaxWidth().testTag("$stateKey-search"), maxLength = 100)
        LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("$stateKey-tree")) {
            items(rows, key = { model.categories.indexOf(it.name) }) { row ->
                val color = MaterialTheme.colorScheme.outlineVariant
                val depth = row.depth.coerceAtMost(4)
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .background(if (selected == row.name) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                    .drawBehind {
                        if (depth > 0) {
                            val x = (24 + (depth - 1) * 18).dp.toPx()
                            drawLine(color, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                            drawLine(color, Offset(x, size.height / 2), Offset(x + 16.dp.toPx(), size.height / 2), 1.dp.toPx())
                        }
                    }.padding(start = (depth * 18).dp), verticalAlignment = Alignment.CenterVertically) {
                    if (row.hasChildren) {
                        val open = query.isNotBlank() || row.name in expanded
                        val expandedDescription = stringResource(if (open) R.string.refs_expanded else R.string.refs_collapsed)
                        Box(Modifier.size(48.dp).testTag("$stateKey-toggle-${row.name}")
                            .clickable(enabled = query.isBlank(), role = Role.Button) {
                                expanded = if (row.name in expanded) expanded - row.name else expanded + row.name
                            }.semantics { contentDescription = model.categoryPath(row.name); stateDescription = expandedDescription },
                            contentAlignment = Alignment.Center) { Text(if (open) "▾" else "▸") }
                    } else Box(Modifier.size(48.dp).testTag("$stateKey-leaf-${row.name}").clearAndSetSemantics { },
                        contentAlignment = Alignment.Center) { Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Row(Modifier.weight(1f).heightIn(min = 48.dp).testTag("$stateKey-select-${row.name}")
                        .clickable(enabled = row.selectable, role = Role.Button) { onSelect(row.name) }
                        .semantics { contentDescription = model.categoryPath(row.name); this.selected = selected == row.name }
                        .padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(row.name, Modifier.fillMaxWidth(), fontWeight = if (row.hasChildren) FontWeight.SemiBold else FontWeight.Normal)
                            if ((query.isNotBlank() || row.depth > 4) && row.depth > 0)
                                Text(model.categoryAncestors(row.name).dropLast(1).joinToString(" › "), style = MaterialTheme.typography.bodySmall)
                        }
                        if (selected == row.name) Text(" ✓")
                    }
                    if (model.referenceOptions[ReferenceCatalog.key(true, row.name)]?.hidden == true)
                        Text(stringResource(R.string.refs_hidden_badge), Modifier.padding(4.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
            if (rows.isEmpty()) item { Text(stringResource(R.string.refs_no_results), Modifier.padding(16.dp)) }
        }
    }
}

@Composable
internal fun CategorySelectorField(model: DemoViewModel, selected: String, options: List<String>, stateKey: String,
    onSelect: (String) -> Unit, label: String, allowNone: Boolean = false, onCreate: (() -> Unit)? = null) {
    var open by referenceMemory(model, "$stateKey-open") { false }
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().testTag("$stateKey-open")) {
            Text(model.categoryPath(selected).ifEmpty { stringResource(if (allowNone) R.string.refs_no_parent else R.string.choose_reference) }, Modifier.weight(1f))
            Text("▾")
        }
    }
    if (open) Dialog(onDismissRequest = { open = false }, properties = DialogProperties(
        usePlatformDefaultWidth = false, securePolicy = SecureFlagPolicy.SecureOn)) {
        val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        val actions: @Composable () -> Unit = {
            Text(label, style = MaterialTheme.typography.titleMedium)
            if (allowNone) TextButton(onClick = { onSelect(""); open = false }) { Text(stringResource(R.string.refs_no_parent)) }
            if (onCreate != null) TextButton(onClick = { open = false; onCreate() }) { Text(stringResource(R.string.refs_new_category)) }
            TextButton(onClick = { open = false }) { Text(stringResource(R.string.refs_close)) }
        }
        Surface(Modifier.padding(if (landscape) 6.dp else 12.dp).widthIn(max = if (landscape) 1000.dp else 620.dp)
            .fillMaxWidth().fillMaxHeight(if (landscape) .96f else .9f), shape = MaterialTheme.shapes.large) {
            if (landscape) Row(Modifier.padding(8.dp).imePadding(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.width(156.dp).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) { actions() }
                CategoryTree(model, selected, options, stateKey, { onSelect(it); open = false }, Modifier.weight(1f).fillMaxHeight())
            } else Column(Modifier.padding(16.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                CategoryTree(model, selected, options, stateKey, { onSelect(it); open = false }, Modifier.weight(1f))
                if (allowNone) TextButton(onClick = { onSelect(""); open = false }) { Text(stringResource(R.string.refs_no_parent)) }
                if (onCreate != null) TextButton(onClick = { open = false; onCreate() }) { Text(stringResource(R.string.refs_new_category)) }
                TextButton(onClick = { open = false }) { Text(stringResource(R.string.refs_close)) }
            }
        }
    }
}

@Composable
internal fun ReferenceCreateDialog(model: DemoViewModel, category: Boolean, initialParent: String?, stateKey: String,
    onCreated: (String) -> Unit, onDismiss: () -> Unit) {
    var name by referenceMemory(model, "$stateKey-name") { "" }
    var parent by referenceMemory(model, "$stateKey-parent") { initialParent.orEmpty() }
    var error by referenceMemory(model, "$stateKey-error") { false }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, securePolicy = SecureFlagPolicy.SecureOn)) {
        Surface(Modifier.padding(12.dp).widthIn(max = 500.dp).fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(20.dp).imePadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(if (category) R.string.refs_new_category else R.string.refs_new_payment), style = MaterialTheme.typography.titleLarge)
                PinnedLabelField(name, { name = it; error = false }, stringResource(R.string.reference_name),
                    Modifier.fillMaxWidth().testTag("reference-create-name"), maxLength = 60)
                if (category) CategorySelectorField(model, parent, model.categories, "$stateKey-parent-selector", { parent = it },
                    stringResource(R.string.refs_parent), allowNone = true)
                if (error) Text(stringResource(R.string.refs_create_error), color = MaterialTheme.colorScheme.error)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Button(onClick = {
                        val created = model.createReference(category, name, if (category) parent.ifBlank { null } else null)
                        if (created != null) onCreated(created) else error = true
                    }, enabled = name.isNotBlank(), modifier = Modifier.testTag("reference-create-confirm")) { Text(stringResource(R.string.refs_create)) }
                }
            }
        }
    }
}
