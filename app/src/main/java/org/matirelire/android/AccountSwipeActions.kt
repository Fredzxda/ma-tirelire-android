package org.matirelire.android

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import kotlinx.coroutines.launch

@Composable
internal fun AccountSwipeActions(model: DemoViewModel, index: Int, content: @Composable () -> Unit) {
    val state = remember { SwipeToDismissBoxState(initialValue = SwipeToDismissBoxValue.Settled, positionalThreshold = { it * .45f }) }
    val scope = rememberCoroutineScope()
    val archive = stringResource(R.string.account_archive)
    val delete = stringResource(R.string.account_delete)
    SwipeToDismissBox(state = state, modifier = Modifier.testTag("account-swipe-$index").semantics {
        customActions = listOf(
            CustomAccessibilityAction(archive) { model.requestAccountAction(index, AccountActionKind.ARCHIVE); true },
            CustomAccessibilityAction(delete) { model.requestAccountAction(index, AccountActionKind.DELETE); true })
    }, enableDismissFromEndToStart = !model.isArchived(index), onDismiss = { direction ->
        if (direction != SwipeToDismissBoxValue.Settled) {
            model.requestAccountAction(index, if (direction == SwipeToDismissBoxValue.EndToStart) AccountActionKind.ARCHIVE else AccountActionKind.DELETE)
            scope.launch { state.reset() }
        }
    }, backgroundContent = {
        val deleting = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd
        Box(Modifier.fillMaxSize().background(if (deleting) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer)
            .padding(16.dp), contentAlignment = if (deleting) Alignment.CenterStart else Alignment.CenterEnd) {
            Text(if (deleting) "× $delete" else "▣ $archive")
        }
    }) { content() }
}

@Composable
internal fun AccountActionConfirmation(model: DemoViewModel) {
    val action = model.pendingAccountAction ?: return
    val blocked = action.kind == AccountActionKind.DELETE && !model.canDeleteAccount(action.index)
    AlertDialog(onDismissRequest = { model.pendingAccountAction = null },
        modifier = Modifier.testTag("account-action-confirmation"),
        title = { Text(stringResource(if (blocked) R.string.account_delete_blocked_title else if (action.kind == AccountActionKind.DELETE) R.string.account_delete_title else R.string.account_archive_title, action.name)) },
        text = {
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(if (blocked) R.string.account_delete_blocked else if (action.kind == AccountActionKind.DELETE) R.string.account_delete_details else R.string.account_archive_details))
                if (!blocked && action.kind == AccountActionKind.DELETE) {
                    val entries = model.transactions.filter { it.account == action.index }
                    Text(stringResource(R.string.account_delete_impact, entries.size, entries.count { it.peerId != null }, model.rules.count { it.account == action.index || it.target == action.index }))
                    Text(stringResource(R.string.account_delete_balance, money(model.balance(action.index), model.currencyCode(action.index))))
                }
            }
        },
        confirmButton = {
            if (!blocked) TextButton(onClick = { model.confirmAccountAction() }, modifier = Modifier.testTag("account-confirm-action")) {
                Text(stringResource(if (action.kind == AccountActionKind.DELETE) R.string.account_delete else R.string.account_archive))
            } else TextButton(onClick = { model.pendingAccountAction = null }) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = { if (!blocked) TextButton(onClick = { model.pendingAccountAction = null }, modifier = Modifier.testTag("account-cancel-action")) { Text(stringResource(R.string.cancel)) } },
        properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn))
}
