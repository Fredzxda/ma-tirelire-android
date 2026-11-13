package org.matirelire.android

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

private fun localRateText(value: BigDecimal, locale: Locale): String =
    value.stripTrailingZeros().toPlainString().replace('.', DecimalFormatSymbols.getInstance(locale).decimalSeparator)

/** A bounded, searchable currency chooser; no Android saved-state payload. */
@Composable
internal fun CurrencySelector(code: String, onSelect: (String) -> Unit, enabled: Boolean = true, model: DemoViewModel? = null, stateKey: String = "currency-selector") {
    val locale = LocalConfiguration.current.locales[0]
    val openState = if (model == null) remember { mutableStateOf(false) } else referenceMemory(model, "$stateKey-open") { false }
    val queryState = if (model == null) remember { mutableStateOf("") } else referenceMemory(model, "$stateKey-query") { "" }
    var open by openState
    var query by queryState
    val title = stringResource(R.string.currency_label)
    Column(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { query = ""; open = true }, enabled = enabled,
            modifier = Modifier.fillMaxWidth().testTag("currency-selector")) {
            Text("$code · ${runCatching { Currency.getInstance(code).getDisplayName(locale) }.getOrDefault(code)}",
                Modifier.weight(1f))
            Text("▾")
        }
    }
    val landscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val choices: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PinnedLabelField(query, { query = it }, stringResource(R.string.currency_search),
                Modifier.fillMaxWidth().testTag("currency-search"), maxLength = 80)
            val currencies = Fx.currencies.filter {
                it.contains(query.trim(), ignoreCase = true) || Currency.getInstance(it).getDisplayName(locale).contains(query.trim(), ignoreCase = true)
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("currency-list")) {
                items(currencies, key = { it }) { value ->
                    TextButton(onClick = { onSelect(value); open = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("currency-$value")) {
                        Text("$value · ${Currency.getInstance(value).getDisplayName(locale)}", Modifier.weight(1f))
                        if (value == code) Text(" ✓")
                    }
                }
                if (currencies.isEmpty()) item { Text(stringResource(R.string.currency_no_results), Modifier.padding(16.dp)) }
            }
        }
    }
    if (open) Dialog(onDismissRequest = { open = false }, properties = DialogProperties(
        usePlatformDefaultWidth = false, securePolicy = SecureFlagPolicy.SecureOn)) {
        Surface(Modifier.padding(if (landscape) 6.dp else 12.dp).widthIn(max = if (landscape) 1000.dp else 620.dp)
            .fillMaxWidth().fillMaxHeight(.94f), shape = MaterialTheme.shapes.large) {
            if (landscape) Row(Modifier.padding(12.dp).imePadding(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.width(128.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { open = false }) { Text(stringResource(R.string.refs_close)) }
                }
                choices(Modifier.weight(1f).fillMaxHeight())
            } else Column(Modifier.padding(16.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { open = false }) { Text(stringResource(R.string.refs_close)) }
                }
                choices(Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

/** Quotes are user-entered, encrypted with the ledger, and never fetched from a service. */
@Composable
internal fun CurrencySettings(model: DemoViewModel) {
    val locale = LocalConfiguration.current.locales[0]
    var code by referenceMemory(model, "currency-settings-code") { model.localRates.keys.firstOrNull { it != "EUR" } ?: "USD" }
    var text by referenceMemory(model, "currency-settings-rate-$code") {
        model.localRates[code]?.toBigDecimalOrNull()?.let { localRateText(it, locale) }.orEmpty()
    }
    var failed by referenceMemory(model, "currency-settings-error") { false }
    var saved by referenceMemory(model, "currency-settings-saved") { false }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.currency_settings_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.currency_local_only), style = MaterialTheme.typography.bodySmall)
            CurrencySelector(code, { code = it; failed = false; saved = false }, model = model, stateKey = "settings-currency-selector")
            if (code == "EUR") Text(stringResource(R.string.currency_eur_fixed)) else {
                PinnedLabelField(text, { text = it; failed = false; saved = false },
                    stringResource(R.string.currency_local_rate_label, code), Modifier.fillMaxWidth().testTag("currency-local-rate"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), maxLength = 48)
                Button(onClick = {
                    saved = model.setLocalRate(code, text, locale)
                    failed = !saved
                }, modifier = Modifier.testTag("currency-save-rate")) { Text(stringResource(R.string.save)) }
            }
            if (failed) Text(stringResource(R.string.currency_invalid_rate), color = MaterialTheme.colorScheme.error)
            if (saved) Text(stringResource(R.string.currency_rate_saved), color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Fields stay in the host form's scroll area, never adding a permanent Operations toolbar. */
@Composable
internal fun EntryCurrencyFields(model: DemoViewModel, draft: EntryDraft) {
    val locale = LocalConfiguration.current.locales[0]
    val native = model.currencyCode(draft.account)
    val target = draft.transferTarget?.let(model::currencyCode)
    val original = if (target == null) draft.inputCurrency.ifBlank { native } else native
    val from = original
    val to = target ?: native
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (target == null) CurrencySelector(original, {
            model.updateDraft(draft.copy(inputCurrency = if (it == native) "" else it, fxRate = ""))
        }, model = model, stateKey = "entry-original-currency")
        if (from != to) {
            val proposed = model.suggestedRate(from, to)
            PinnedLabelField(draft.fxRate, { model.updateDraft(draft.copy(fxRate = it.take(48))) },
                stringResource(R.string.currency_conversion_rate_label, from, to),
                Modifier.fillMaxWidth().testTag("entry-fx-rate"),
                placeholder = proposed?.let { localRateText(it, locale) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), maxLength = 48)
            if (proposed != null) TextButton(onClick = { model.updateDraft(draft.copy(fxRate = localRateText(proposed, locale))) },
                modifier = Modifier.testTag("entry-use-local-rate")) {
                Text(stringResource(R.string.currency_use_local_rate, localRateText(proposed, locale)))
            }
            val converted = if (target == null) model.nativeAmount(draft, locale) else model.targetAmount(draft, locale)
            if (converted != null && draft.amount.isNotBlank()) Text(stringResource(
                if (target == null) R.string.currency_account_amount else R.string.currency_target_amount,
                money(converted.abs(), to)), style = MaterialTheme.typography.bodyMedium)
            if (draft.attempted && converted == null) Text(stringResource(R.string.currency_invalid_rate), color = MaterialTheme.colorScheme.error)
        }
    }
}
