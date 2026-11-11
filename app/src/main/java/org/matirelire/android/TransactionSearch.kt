package org.matirelire.android

import java.text.Normalizer
import java.util.Locale

/** Literal words, all required; accents and case do not affect matching. */
internal object TransactionSearch {
    private fun normalized(text: String) = Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)

    fun filter(rows: List<DemoTransaction>, query: String): List<DemoTransaction> {
        val words = normalized(query).trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return rows
        return rows.filter { row ->
            val text = normalized(listOf(row.description, row.category, row.payment).joinToString(" "))
            words.all { it in text }
        }
    }
}
