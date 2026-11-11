package org.matirelire.android

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.viewmodel.compose.viewModel
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

internal enum class OperationsMenu { NONE, ACCOUNT, FILTER, SEARCH, FUNCTIONS }

@Composable
internal fun TransactionsScreen(names: List<String>, account: Int, balance: BigDecimal,
    clearedBalance: BigDecimal, transactions: List<DemoTransaction>, onAccount: (Int) -> Unit,
    onToggle: (Long) -> Unit, onReconcile: () -> Unit, onEdit: (Long) -> Unit,
    onNew: () -> Unit, model: DemoViewModel = viewModel()) {
    var showUpcoming by rememberSaveable { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val closeMenu = { model.operationsMenu = OperationsMenu.NONE; focus.clearFocus(); keyboard?.hide(); Unit }
    val currentClose by rememberUpdatedState(closeMenu)
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    var menuBounds by remember { mutableStateOf(Rect.Zero) }
    var panelBounds by remember { mutableStateOf(Rect.Zero) }
    val period = model.accountPeriod
    val periodTitle = when (period.scope) {
        PeriodScope.MONTH -> period.effectiveMonth(model.calendarToday).format(
            DateTimeFormatter.ofPattern("LLL uuuu", LocalConfiguration.current.locales[0]))
        PeriodScope.ALL -> stringResource(R.string.period_all)
        PeriodScope.UNDATED -> stringResource(R.string.date_needs_review)
    }
    val visible = TransactionSearch.filter(period.filter(transactions, model.calendarToday), model.searchQuery)
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (showUpcoming) UpcomingDialog(model, account, names) { showUpcoming = false }
    if (showInfo) AlertDialog(onDismissRequest = { showInfo = false },
        title = { Text(stringResource(R.string.balance_info)) },
        text = { Text(stringResource(R.string.operations_information), modifier = Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = { showInfo = false }) { Text(stringResource(android.R.string.ok)) } },
        properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn))

    Column(Modifier.fillMaxSize().testTag("operations-root")
        .onGloballyPositioned { rootOrigin = it.positionInRoot() }
        .pointerInput(Unit) {
            // Observe after child controls have handled the release. Never consume it
            // or remove the panel on DOWN, which would shift the target before UP.
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Final)
                    val release = event.changes.firstOrNull { it.changedToUpIgnoreConsumed() }
                    if (release != null && model.operationsMenu != OperationsMenu.NONE) {
                        val point = release.position + rootOrigin
                        if (!menuBounds.contains(point) && !panelBounds.contains(point)) currentClose()
                    }
                }
            }
        }) {
        Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
            if (landscape && LocalConfiguration.current.fontScale <= 1.3f) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(names[account], style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    BalanceValue(R.string.balance, balance, Modifier.weight(1f), compact = true)
                    BalanceValue(R.string.cleared_balance, clearedBalance, Modifier.weight(1f), compact = true)
                    HeaderActions(onInfo = { closeMenu(); showInfo = true }, onNew = { closeMenu(); onNew() })
                }
            } else Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(names[account], style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    HeaderActions(onInfo = { closeMenu(); showInfo = true }, onNew = { closeMenu(); onNew() })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                    BalanceValue(R.string.balance, balance, Modifier.weight(1f))
                    BalanceValue(R.string.cleared_balance, clearedBalance, Modifier.weight(1f))
                }
            }
        }
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
            .onGloballyPositioned { menuBounds = it.boundsInRoot() }, verticalAlignment = Alignment.CenterVertically) {
            val currentOrder = stringResource(R.string.operations_sort_action, stringResource(orderResource(model.transactionOrder)))
            IconButton(onClick = {
                closeMenu()
                model.setTransactionOrder(when (model.transactionOrder) {
                    TransactionOrder.ORIGINAL -> TransactionOrder.NEWEST
                    TransactionOrder.NEWEST -> TransactionOrder.OLDEST
                    TransactionOrder.OLDEST -> TransactionOrder.ORIGINAL
                })
            }, modifier = Modifier.testTag("menu-sort").semantics {
                contentDescription = currentOrder
            }) { Text(when (model.transactionOrder) {
                TransactionOrder.ORIGINAL -> "↕"; TransactionOrder.NEWEST -> "↓"; TransactionOrder.OLDEST -> "↑"
            }, style = MaterialTheme.typography.titleLarge) }
            listOf(OperationsMenu.ACCOUNT, OperationsMenu.FILTER, OperationsMenu.SEARCH, OperationsMenu.FUNCTIONS).forEach { entry ->
                val selected = model.operationsMenu == entry
                TextButton(onClick = {
                    if (selected) closeMenu() else {
                        focus.clearFocus(); keyboard?.hide(); model.operationsMenu = entry
                    }
                }, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("menu-${entry.name.lowercase()}")
                    .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(when (entry) {
                            OperationsMenu.ACCOUNT -> R.string.operations_account
                            OperationsMenu.FILTER -> R.string.operations_filter
                            OperationsMenu.SEARCH -> R.string.operations_search
                            else -> R.string.operations_functions
                        }), style = MaterialTheme.typography.labelMedium)
                        if (entry == OperationsMenu.FILTER) Text(periodTitle, style = MaterialTheme.typography.labelSmall)
                        if (entry == OperationsMenu.SEARCH && model.searchQuery.isNotBlank())
                            Text("●", style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.semantics { contentDescription = model.searchQuery })
                    }
                }
            }
        }
        if (model.operationsMenu != OperationsMenu.NONE) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()
                .testTag("operations-panel").onGloballyPositioned { panelBounds = it.boundsInRoot() }) {
                when (model.operationsMenu) {
                    OperationsMenu.SEARCH -> Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = model.searchQuery, onValueChange = { model.searchQuery = it.take(200) },
                            modifier = Modifier.weight(1f).testTag("operations-query"), singleLine = true,
                            placeholder = { Text(stringResource(R.string.operations_search_placeholder), style = MaterialTheme.typography.bodyMedium) })
                        TextButton(onClick = { model.searchQuery = "" }, modifier = Modifier.testTag("operations-clear-query")) {
                            Text(stringResource(R.string.search_clear))
                        }
                    }
                    else -> Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        when (model.operationsMenu) {
                            OperationsMenu.ACCOUNT -> names.forEachIndexed { index, name ->
                                FilterChip(selected = account == index, onClick = { onAccount(index) }, label = { Text(name) })
                            }
                            OperationsMenu.FILTER -> {
                                val previous = period.move(-1, model.calendarToday)
                                val next = period.move(1, model.calendarToday)
                                val previousLabel = stringResource(R.string.period_previous)
                                val nextLabel = stringResource(R.string.period_next)
                                IconButton(onClick = { previous?.let { model.accountPeriod = it } }, enabled = previous != null,
                                    modifier = Modifier.semantics { contentDescription = previousLabel }) { Text("‹", style = MaterialTheme.typography.headlineSmall) }
                                Text(periodTitle, style = MaterialTheme.typography.labelLarge)
                                IconButton(onClick = { next?.let { model.accountPeriod = it } }, enabled = next != null,
                                    modifier = Modifier.semantics { contentDescription = nextLabel }) { Text("›", style = MaterialTheme.typography.headlineSmall) }
                                FilterChip(selected = period.scope == PeriodScope.MONTH && period.followsCurrent,
                                    onClick = { model.accountPeriod = AccountPeriod() }, label = { Text(stringResource(R.string.period_current)) })
                                FilterChip(selected = period.scope == PeriodScope.ALL,
                                    onClick = { model.accountPeriod = period.copy(scope = PeriodScope.ALL) },
                                    modifier = Modifier.testTag("period-all"), label = { Text(stringResource(R.string.period_all)) })
                                val undated = transactions.count { OperationDates.forOrdering(it.date) == null }
                                if (undated > 0) FilterChip(selected = period.scope == PeriodScope.UNDATED,
                                    onClick = { model.accountPeriod = period.copy(scope = PeriodScope.UNDATED) },
                                    label = { Text(stringResource(R.string.period_undated, undated)) })
                            }
                            OperationsMenu.FUNCTIONS -> {
                                OutlinedButton(onClick = { closeMenu(); onReconcile() }) { Text(stringResource(R.string.reconcile_open)) }
                                OutlinedButton(onClick = { closeMenu(); showUpcoming = true }) { Text(stringResource(R.string.upcoming_open)) }
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
        HorizontalDivider()
        LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("operations-list"),
            contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (visible.isEmpty()) item {
                Text(stringResource(R.string.operations_empty), modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            }
            items(visible, key = { it.id }) { row ->
                OperationCard(row, model.calendarToday, onEdit = { closeMenu(); onEdit(row.id) },
                    onToggle = { closeMenu(); onToggle(row.id) })
            }
        }
    }
}

@Composable
private fun HeaderActions(onInfo: () -> Unit, onNew: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val info = stringResource(R.string.balance_info)
        val add = stringResource(R.string.enter_transaction)
        IconButton(onClick = onInfo, modifier = Modifier.testTag("operations-info").semantics { contentDescription = info }) { Text("ⓘ") }
        FilledIconButton(onClick = onNew, modifier = Modifier.testTag("operations-add").semantics { contentDescription = add }) {
            Text("+", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun BalanceValue(label: Int, value: BigDecimal, modifier: Modifier, compact: Boolean = false) {
    Column(modifier) {
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium)
        Text(money(value), style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold)
    }
}

internal fun orderResource(order: TransactionOrder): Int = when (order) {
    TransactionOrder.ORIGINAL -> R.string.order_original
    TransactionOrder.NEWEST -> R.string.order_newest
    TransactionOrder.OLDEST -> R.string.order_oldest
}

@Composable
private fun OperationCard(row: DemoTransaction, today: java.time.LocalDate, onEdit: () -> Unit, onToggle: () -> Unit) {
    Card(onClick = onEdit, modifier = Modifier.fillMaxWidth().testTag("operation-${row.id}")) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) { RecurringBadge(row, today) }
                Text(OperationDates.display(row.date, LocalConfiguration.current.locales[0]),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(row.description, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(money(row.amount), textAlign = androidx.compose.ui.text.style.TextAlign.End, fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(max = 200.dp),
                    color = if (row.amount.signum() < 0) Color(0xFF944C3C) else MaterialTheme.colorScheme.primary)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(if (row.peerId != null) row.payment else "${row.category} · ${row.payment}", style = MaterialTheme.typography.bodySmall)
                    if (row.peerId != null) Text(stringResource(R.string.transfer_linked), style = MaterialTheme.typography.labelSmall)
                    if (OperationDates.forOrdering(row.date) == null) Text(stringResource(R.string.date_needs_review),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
                Row(Modifier.heightIn(min = 48.dp).testTag("pointing-${row.id}")
                    .toggleable(value = row.cleared, role = Role.Checkbox, onValueChange = { onToggle() }),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(if (row.cleared) R.string.marked else R.string.unmarked), style = MaterialTheme.typography.labelMedium)
                    Checkbox(checked = row.cleared, onCheckedChange = null)
                }
            }
        }
    }
}
