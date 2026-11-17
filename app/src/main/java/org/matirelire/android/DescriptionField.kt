package org.matirelire.android

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties

@Composable
internal fun EntryDescriptionSuggestions(model: DemoViewModel, draft: EntryDraft, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf(false) }
    val choices = model.descriptionSuggestions(draft.description)
    Box(modifier) {
        PinnedLabelField(draft.description, {
            dismissed = false
            model.changeDescription(it)
        }, stringResource(R.string.description), Modifier.fillMaxWidth().testTag("entry-description")
            .onFocusChanged {
                val lostFocus = focused && !it.isFocused
                focused = it.isFocused
                if (lostFocus) model.completeDescription()
            }, maxLength = 100)
        DropdownMenu(expanded = focused && !dismissed && choices.isNotEmpty(),
            onDismissRequest = { dismissed = true }, modifier = Modifier.heightIn(max = 240.dp),
            properties = PopupProperties(focusable = false)) {
            choices.forEach { example ->
                DropdownMenuItem(text = { Text(example.description) },
                    modifier = Modifier.testTag("description-suggestion-${example.id}"),
                    onClick = { model.selectDescription(example.id); dismissed = true })
            }
        }
    }
}
