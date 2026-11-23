package org.matirelire.android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*

@Composable
internal fun CategoryGlyph(model: DemoViewModel, category: String) {
    CategoryIconCatalog.find(model.categoryIconId(category))?.let {
        Icon(painterResource(it.drawableRes), null, Modifier.size(24.dp).padding(end = 4.dp))
    }
}

@Composable
internal fun CategoryIconSelectorField(model: DemoViewModel, icon: String?, inherit: Boolean,
    parentAvailable: Boolean, stateKey: String, onSelect: (String?, Boolean) -> Unit) {
    var open by referenceMemory(model, "$stateKey-open") { false }
    OutlinedButton(onClick = { open = true }, modifier = Modifier.testTag("$stateKey-open")) {
        CategoryIconCatalog.find(icon)?.let { Icon(painterResource(it.drawableRes), null, Modifier.size(24.dp)); Spacer(Modifier.width(8.dp)) }
        Text(if (inherit) stringResource(R.string.icons_inherit) else CategoryIconCatalog.find(icon)?.let { stringResource(it.labelRes) }
            ?: stringResource(R.string.icons_none))
    }
    if (open) CategoryIconPicker(model, stateKey, parentAvailable, { id, inherited ->
        onSelect(id, inherited); open = false
        model.referenceUiStates.keys.removeAll { it.startsWith("$stateKey-picker-") }
    }, { open = false; model.referenceUiStates.keys.removeAll { it.startsWith("$stateKey-picker-") } })
}

@Composable
private fun CategoryIconPicker(model: DemoViewModel, stateKey: String, parentAvailable: Boolean,
    onSelect: (String?, Boolean) -> Unit, onDismiss: () -> Unit) {
    var query by referenceMemory(model, "$stateKey-picker-query") { "" }
    var family by referenceMemory(model, "$stateKey-picker-family") { 0 }
    var familiesOpen by remember { mutableStateOf(false) }
    val families = CategoryIconCatalog.entries.map { it.familyRes }.distinct()
    val choices = CategoryIconCatalog.entries.filter {
        val text = stringResource(it.labelRes) + " " + stringResource(it.familyRes)
        (family == 0 || it.familyRes == family) && text.contains(query.trim(), ignoreCase = true)
    }
    val landscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val actions: @Composable () -> Unit = {
        TextButton(onClick = { onSelect(null, false) }, modifier = Modifier.testTag("icon-none")) { Text(stringResource(R.string.icons_none)) }
        if (parentAvailable) TextButton(onClick = { onSelect(null, true) }, modifier = Modifier.testTag("icon-inherit")) { Text(stringResource(R.string.icons_inherit)) }
        TextButton(onClick = onDismiss, modifier = Modifier.testTag("icon-close")) { Text(stringResource(R.string.cancel)) }
    }
    val controls: @Composable () -> Unit = {
        Text(stringResource(R.string.icons_title), style = MaterialTheme.typography.titleMedium)
        if (landscape) Column { actions() } else Row(Modifier.horizontalScroll(rememberScrollState())) { actions() }
    }
    val content: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PinnedLabelField(query, { query = it }, stringResource(R.string.icons_search), Modifier.weight(1f).testTag("category-icon-search"), maxLength = 100)
                Box {
                    TextButton(onClick = { familiesOpen = true }, Modifier.testTag("icon-family")) {
                        Text(stringResource(if (family == 0) R.string.icons_all else family), modifier = Modifier.widthIn(max = 120.dp))
                    }
                    DropdownMenu(expanded = familiesOpen, onDismissRequest = { familiesOpen = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.icons_all)) }, onClick = { family = 0; familiesOpen = false })
                        families.forEach { id -> DropdownMenuItem(text = { Text(stringResource(id)) }, onClick = { family = id; familiesOpen = false }) }
                    }
                }
            }
            LazyVerticalGrid(columns = GridCells.Adaptive(88.dp), modifier = Modifier.weight(1f).fillMaxWidth().testTag("category-icon-grid"),
                horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(choices, key = { it.id }) { item ->
                    OutlinedCard(onClick = { onSelect(item.id, false) }, modifier = Modifier.testTag("category-icon-choice-${item.id}")) {
                        Column(Modifier.fillMaxWidth().heightIn(min = 80.dp).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(painterResource(item.drawableRes), null, Modifier.size(24.dp))
                            Text(stringResource(item.labelRes), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, securePolicy = SecureFlagPolicy.SecureOn)) {
        Surface(Modifier.padding(8.dp).widthIn(max = 1000.dp).fillMaxWidth().fillMaxHeight(.95f), shape = MaterialTheme.shapes.large) {
            if (landscape) Row(Modifier.padding(12.dp).imePadding(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.width(140.dp).fillMaxHeight().verticalScroll(rememberScrollState())) { controls() }
                content(Modifier.weight(1f).fillMaxHeight())
            } else Column(Modifier.padding(12.dp).imePadding()) {
                Column(Modifier.fillMaxWidth()) { controls() }
                content(Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CategoryIconStrip(model: DemoViewModel, row: DemoTransaction) {
    val ids = if (model.showCategoryIcons) model.operationIconIds(row) else emptyList()
    if (ids.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.testTag("operation-icons-${row.id}")) {
        ids.take(3).forEach { id -> CategoryIconCatalog.find(id)?.let {
            Icon(painterResource(it.drawableRes), null, Modifier.size(22.dp).testTag("operation-icon-$id"))
        } }
        if (ids.size > 3) Text("+${ids.size - 3}", style = MaterialTheme.typography.labelSmall)
    }
}
