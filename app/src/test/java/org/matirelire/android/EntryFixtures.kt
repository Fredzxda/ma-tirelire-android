package org.matirelire.android

import java.time.LocalDate

/** Explicit reference selections for fixtures whose subject is not empty-field entry. */
internal fun DemoViewModel.beginEntryWithReferences(today: LocalDate = LocalDate.now()) {
    val alreadyOpen = draft != null
    beginEntry(today)
    if (alreadyOpen) return // Reopening must preserve the exact draft under test.
    val current = draft ?: return // Archived accounts still refuse new entries.
    updateDraft(current.copy(
        category = referenceChoices(true, current.account, current.income).firstOrNull().orEmpty(),
        payment = referenceChoices(false, current.account, current.income).firstOrNull().orEmpty()))
}
