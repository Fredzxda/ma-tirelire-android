package org.matirelire.android

import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

/** Versioned exchange representation. No device keys or backup passwords. */
internal data class LedgerData(
    val transactions: List<DemoTransaction>, val categories: List<String>,
    val payments: List<String>, val openings: List<BigDecimal>, val draft: EntryDraft?,
    val accountNames: List<String> = emptyList(),
)

internal object SnapshotCodec {
    const val MAX_BYTES = 4 * 1024 * 1024

    fun encode(data: LedgerData): ByteArray {
        val rows = JSONArray()
        data.transactions.forEach { t -> rows.put(JSONObject().put("id", t.id).put("account", t.account)
            .put("description", t.description).put("amount", t.amount.toPlainString())
            .put("category", t.category).put("payment", t.payment).put("date", t.date).put("cleared", t.cleared)) }
        val d = data.draft?.let { draft -> JSONObject().put("account", draft.account).put("income", draft.income)
            .put("amount", draft.amount).put("description", draft.description).put("category", draft.category)
            .put("payment", draft.payment).put("addingCategory", draft.addingCategory).put("addingPayment", draft.addingPayment)
            .put("newCategory", draft.newCategory).put("newPayment", draft.newPayment)
            .put("editingId", draft.editingId ?: JSONObject.NULL) }
        val result = JSONObject().put("schema", if (data.accountNames.isEmpty()) 1 else 2)
            .put("accountNames", JSONArray(data.accountNames)).put("transactions", rows)
            .put("categories", JSONArray(data.categories)).put("payments", JSONArray(data.payments))
            .put("openings", JSONArray(data.openings.map { it.toPlainString() })).put("draft", d ?: JSONObject.NULL)
            .toString().toByteArray(Charsets.UTF_8)
        require(result.size <= MAX_BYTES)
        decode(result) // never persist a state that this version cannot reopen
        return result
    }

    fun decode(bytes: ByteArray): LedgerData {
        require(bytes.size in 1..MAX_BYTES)
        val text = Charsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
            .decode(java.nio.ByteBuffer.wrap(bytes)).toString()
        var quoted = false
        var escaped = false
        var depth = 0
        text.forEach { ch ->
            if (quoted) {
                if (escaped) escaped = false else if (ch == '\\') escaped = true else if (ch == '"') quoted = false
            } else when (ch) {
                '"' -> quoted = true
                '{', '[' -> { depth++; require(depth <= 32) }
                '}', ']' -> { depth--; require(depth >= 0) }
            }
        }
        require(!quoted && depth == 0)
        val root = JSONObject(text)
        val schema = root.getInt("schema")
        require(schema in 1..2)
        fun names(key: String): List<String> {
            val list = root.getJSONArray(key)
            require(list.length() in 1..500)
            return (0 until list.length()).map { i -> list.getString(i).also {
                require(it.isNotBlank() && it.length <= 60 && it == it.trim())
            } }.also { values -> require(values.map { it.lowercase(java.util.Locale.ROOT) }.distinct().size == values.size) }
        }
        val categories = names("categories")
        val payments = names("payments")
        fun amount(text: String): BigDecimal {
            require(Regex("-?[0-9]{1,12}([.][0-9]{1,2})?").matches(text))
            return text.toBigDecimal().setScale(2)
        }
        val openingsJson = root.getJSONArray("openings")
        require(openingsJson.length() in 1..50)
        val openings = (0 until openingsJson.length()).map { amount(openingsJson.getString(it)) }
        val accountNames = if (schema == 1) {
            require(openings.size == 2)
            emptyList()
        } else names("accountNames").also { require(it.size == openings.size) }
        val entries = root.getJSONArray("transactions")
        require(entries.length() <= 10_000)
        val rows = (0 until entries.length()).map { i ->
            val t = entries.getJSONObject(i)
            DemoTransaction(t.getLong("id"), t.getInt("account"), t.getString("description"),
                amount(t.getString("amount")), t.getString("category"), t.getString("payment"),
                t.getString("date"), t.getBoolean("cleared")).also {
                require(it.id > 0 && it.id < Long.MAX_VALUE && it.account in openings.indices)
                require(it.description.isNotBlank() && it.description.length <= 100 && it.date.length <= 80)
                require(it.category in categories && it.payment in payments)
            }
        }
        require(rows.map { it.id }.distinct().size == rows.size)
        val d = root.optJSONObject("draft")?.let {
            EntryDraft(it.getInt("account"), it.getBoolean("income"), it.getString("amount"),
                it.getString("description"), it.getString("category"), it.getString("payment"),
                addingCategory = it.optBoolean("addingCategory"), addingPayment = it.optBoolean("addingPayment"),
                newCategory = it.optString("newCategory"), newPayment = it.optString("newPayment"),
                editingId = if (it.isNull("editingId")) null else it.getLong("editingId")).also { draft ->
                require(draft.account in openings.indices && draft.amount.length <= 30 && draft.description.length <= 100)
                require(draft.category in categories && draft.payment in payments)
                require(draft.editingId == null || rows.any { row -> row.id == draft.editingId })
                require(draft.newCategory.length <= 60 && draft.newPayment.length <= 60)
            }
        }
        return LedgerData(rows, categories, payments, openings, d, accountNames)
    }
}
