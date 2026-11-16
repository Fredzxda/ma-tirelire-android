package org.matirelire.android

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
internal fun EntrySplits(model: DemoViewModel, draft: EntryDraft) {
    if (draft.transferTarget != null && draft.splits.isEmpty()) return
    val locale = LocalConfiguration.current.locales[0]
    val nativeAmount = model.nativeAmount(draft, locale)
    val invalidShares = nativeAmount != null && Splits.parse(draft.splits, nativeAmount, model.currencyCode(draft.account), locale, model.categories) == null
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.splits_title), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            if (draft.splits.isNotEmpty()) TextButton(onClick = { model.updateDraft(draft.copy(splits = emptyList())) }) {
                Text(stringResource(R.string.splits_remove_all))
            }
        }
        if (draft.splits.isNotEmpty()) Text(stringResource(R.string.splits_native, model.currencyCode(draft.account)), style = MaterialTheme.typography.bodySmall)
        draft.splits.forEachIndexed { index, line ->
            fun change(value: SplitDraft) { model.updateDraft(draft.copy(splits = draft.splits.toMutableList().also { it[index] = value })) }
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.splits_line, index + 1), modifier = Modifier.weight(1f))
                        TextButton(onClick = { model.updateDraft(draft.copy(splits = draft.splits.filterIndexed { i, _ -> i != index })) }) {
                            Text(stringResource(R.string.splits_remove))
                        }
                    }
                    CategorySelectorField(model, line.category,
                        model.referenceChoices(true, draft.account, draft.income, line.category), "split-category-$index",
                        { change(line.copy(category = it)) }, stringResource(R.string.category))
                    PinnedLabelField(line.description, { change(line.copy(description = it)) },
                        label = stringResource(R.string.description), modifier = Modifier.fillMaxWidth(), maxLength = 100)
                    PinnedLabelField(line.amount, { change(line.copy(amount = it)) },
                        label = stringResource(R.string.amount), modifier = Modifier.fillMaxWidth().testTag("split_amount_$index"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), maxLength = 30)
                }
            }
        }
        if (draft.splits.size < Splits.MAX_LINES && draft.transferTarget == null) TextButton(onClick = {
            val extra = if (draft.splits.isEmpty()) listOf(SplitDraft(draft.category), SplitDraft(draft.category)) else listOf(SplitDraft(draft.category))
            model.updateDraft(draft.copy(splits = draft.splits + extra))
        }, modifier = Modifier.testTag("split_add")) { Text(stringResource(R.string.splits_add)) }
        if (draft.attempted && invalidShares) Text(stringResource(R.string.splits_invalid), color = MaterialTheme.colorScheme.error)
        if (draft.transferTarget != null && draft.splits.isNotEmpty()) Text(stringResource(R.string.splits_transfer_blocked), color = MaterialTheme.colorScheme.error)
    }
}
