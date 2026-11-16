package org.matirelire.android

import java.math.BigDecimal
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject
import org.matirelire.android.domain.Money

/** Positive shares of the operation's native account amount; direction belongs to the operation. */
internal data class OperationSplit(val category: String, val description: String, val amount: BigDecimal)
internal data class SplitDraft(val category: String = "", val description: String = "", val amount: String = "")

internal object Splits {
    const val MAX_LINES = 50
    fun valid(lines: List<OperationSplit>, amount: BigDecimal, currency: String, categories: List<String>): Boolean =
        lines.isEmpty() || (lines.size in 2..MAX_LINES && lines.all {
            it.category in categories && it.description.length <= 100 && it.amount.signum() > 0 && Fx.validAmount(it.amount, currency)
        } && lines.fold(BigDecimal.ZERO) { sum, line -> sum + line.amount }.compareTo(amount.abs()) == 0)

    fun parse(lines: List<SplitDraft>, amount: BigDecimal, currency: String, locale: Locale, categories: List<String>): List<OperationSplit>? {
        if (lines.isEmpty()) return emptyList()
        if (lines.size !in 2..MAX_LINES) return null
        val result = lines.map { line -> OperationSplit(line.category, line.description.trim(),
            Money.parseAmount(line.amount, locale, Fx.digits(currency)) ?: return null) }
        return result.takeIf { valid(it, amount, currency, categories) }
    }
    fun encode(lines: List<OperationSplit>) = JSONArray(lines.map { JSONObject().put("category", it.category)
        .put("description", it.description).put("amount", it.amount.toPlainString()) })
    fun decode(json: JSONArray?): List<OperationSplit> {
        if (json == null) return emptyList()
        require(json.length() <= MAX_LINES)
        return (0 until json.length()).map { index -> val j = json.getJSONObject(index)
            val text = j.getString("amount")
            require(Regex("[0-9]{1,12}([.][0-9]{1,3})?").matches(text))
            OperationSplit(j.getString("category"), j.getString("description"), text.toBigDecimal()) }
    }
    fun encodeDraft(lines: List<SplitDraft>) = JSONArray(lines.map { JSONObject().put("category", it.category)
        .put("description", it.description).put("amount", it.amount) })
    fun decodeDraft(json: JSONArray?): List<SplitDraft> {
        if (json == null) return emptyList()
        require(json.length() <= MAX_LINES)
        return (0 until json.length()).map { index -> val j = json.getJSONObject(index)
            SplitDraft(j.getString("category"), j.getString("description"), j.getString("amount")) }
    }
}
