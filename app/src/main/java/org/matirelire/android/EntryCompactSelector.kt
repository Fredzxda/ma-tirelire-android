package org.matirelire.android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp

/** Same fixed label and height as entry fields, with a separate accessible button surface. */
@Composable
internal fun EntryCompactSelector(label: String, value: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true) {
    val prompt = stringResource(R.string.choose_reference)
    Box(modifier.heightIn(min = 56.dp).semantics(mergeDescendants = true) {}) {
        PinnedLabelField(value, {}, label, Modifier.fillMaxWidth().clearAndSetSemantics {},
            readOnly = true, placeholder = prompt)
        Spacer(Modifier.matchParentSize().clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "$label: ${value.ifBlank { prompt }}" })
    }
}
