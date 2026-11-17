package org.matirelire.android

import java.text.Normalizer
import java.util.Locale

/** Suggestions come only from this unlocked ledger, never Android's persisted state. */
internal object DescriptionSuggestions {
    private fun normalized(value: String): String = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)

    fun find(rows: List<DemoTransaction>, draft: EntryDraft, query: String, limit: Int = 5): List<DemoTransaction> {
        require(limit in 1..20)
        val term = normalized(query)
        if (term.isEmpty()) return emptyList()
        return rows.asSequence().filter {
            it.id != draft.editingId && it.account == draft.account &&
                (it.amount.signum() > 0) == draft.income && it.amount.signum() != 0 &&
                (it.seriesId == null || it.amountConfirmed) && normalized(it.description).startsWith(term)
        }.sortedByDescending { it.id }.distinctBy { normalized(it.description) }.take(limit).toList()
    }

    fun exact(rows: List<DemoTransaction>, draft: EntryDraft, text: String): DemoTransaction? =
        rows.asSequence().filter { it.id != draft.editingId && it.account == draft.account &&
            (it.amount.signum() > 0) == draft.income && it.amount.signum() != 0 &&
            (it.seriesId == null || it.amountConfirmed) && normalized(it.description) == normalized(text) }
            .maxByOrNull { it.id }
}

internal fun DemoViewModel.descriptionSuggestions(text: String = draft?.description.orEmpty()): List<DemoTransaction> =
    draft?.let { DescriptionSuggestions.find(transactions, it, text) }.orEmpty()

internal fun DemoViewModel.changeDescription(text: String) {
    val current = draft ?: return
    updateDraft(current.copy(description = text.take(100)))
}

internal fun DemoViewModel.completedDescriptionDraft(value: EntryDraft): EntryDraft {
    val example = DescriptionSuggestions.exact(transactions, value, value.description) ?: return value
    return descriptionDefaults(value, example)
}

internal fun DemoViewModel.completeDescription() {
    val current = draft ?: return
    val next = completedDescriptionDraft(current)
    if (next != current) updateDraft(next)
}

internal fun DemoViewModel.selectDescription(id: Long) {
    val current = draft ?: return
    val example = descriptionSuggestions().firstOrNull { it.id == id } ?: return
    updateDraft(descriptionDefaults(current.copy(description = example.description), example))
}

private fun DemoViewModel.descriptionDefaults(value: EntryDraft, example: DemoTransaction): EntryDraft {
    if (value.transferTarget != null) return value
    val category = if (value.category.isBlank() && value.splits.isEmpty() && example.splits.isEmpty() && example.peerId == null &&
        example.category in referenceChoices(true, value.account, value.income)) example.category else value.category
    val payment = if (value.payment.isBlank() &&
        example.payment in referenceChoices(false, value.account, value.income)) example.payment else value.payment
    return value.copy(category = category, payment = payment)
}
