package org.matirelire.android

import android.app.DatePickerDialog
import android.view.WindowManager
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import java.time.LocalDate
import java.time.ZoneId

@Composable
internal fun SeriesNextDateButton(model: DemoViewModel, rule: RecurringRule) {
    val context = LocalContext.current
    val title = stringResource(R.string.series_next_date_title)
    var picker by remember { mutableStateOf<DatePickerDialog?>(null) }
    var failed by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { picker?.dismiss() } }
    val minimum = model.nextSeriesMinimumDate(rule.id)
    TextButton(enabled = minimum != null, modifier = Modifier.testTag("series-next-date-${rule.id}"), onClick = {
        val min = model.nextSeriesMinimumDate(rule.id) ?: return@TextButton
        val selected = (rule.dateAt(rule.next) ?: min).coerceAtLeast(min).let { candidate ->
            rule.until?.let(OperationDates::parse)?.let { candidate.coerceAtMost(it) } ?: candidate
        }
        picker?.dismiss()
        picker = DatePickerDialog(context, { _, year, month, day ->
            failed = !model.setSeriesNextDate(rule.id, LocalDate.of(year, month + 1, day))
        }, selected.year, selected.monthValue - 1, selected.dayOfMonth).apply {
            setTitle(title)
            datePicker.minDate = min.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            rule.until?.let(OperationDates::parse)?.let { datePicker.maxDate = it.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }
            show()
            window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }) { Text(stringResource(R.string.series_next_date_change)) }
    if (failed) Text(stringResource(R.string.series_next_date_error), color = MaterialTheme.colorScheme.error)
}
