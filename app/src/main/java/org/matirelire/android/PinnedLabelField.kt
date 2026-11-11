package org.matirelire.android

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

/** State is remembered only in memory; the encrypted draft remains authoritative. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PinnedLabelField(value: String, onValueChange: (String) -> Unit, label: String,
    modifier: Modifier = Modifier, readOnly: Boolean = false, placeholder: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default, maxLength: Int = Int.MAX_VALUE) {
    val state = remember { TextFieldState(initialText = value) }
    val currentOnChange by rememberUpdatedState(onValueChange)
    LaunchedEffect(value) {
        if (state.text.toString() != value) state.edit { replace(0, length, value) }
    }
    OutlinedTextField(state = state, modifier = modifier, readOnly = readOnly,
        labelPosition = TextFieldLabelPosition.Attached(alwaysMinimize = true),
        label = { Text(label) }, placeholder = { if (placeholder != null) Text(placeholder, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
        textStyle = MaterialTheme.typography.bodyLarge, lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = keyboardOptions,
        inputTransformation = InputTransformation {
            if (length > maxLength) replace(maxLength, length, "")
            currentOnChange(asCharSequence().toString())
        })
}
