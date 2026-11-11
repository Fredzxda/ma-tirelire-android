package org.matirelire.android

import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

/** Versioned exchange representation. No device keys or backup passwords. */
internal data class LedgerData(
    val transactions: List<DemoTransaction>, val categories: List<String>,
    val payments: List<String>, val openings: List<BigDecimal>, val draft: EntryDraft?,
    val accountNames: List<String> = emptyList(),
    val transferPayment: String? = null,
    val rules: List<RecurringRule> = emptyList(),
    val anticipation: Anticipation = Anticipation.DUE,
    val transactionOrder: TransactionOrder = TransactionOrder.ORIGINAL,
    val dateMigrationVersion: Int = 0,
)

internal object SnapshotCodec {
    const val MAX_BYTES = 4 * 1024 * 1024

    fun encode(data: LedgerData): ByteArray {
        val rows = JSONArray()
        data.transactions.forEach { t -> rows.put(JSONObject().put("id", t.id).put("account", t.account)
            .put("description", t.description).put("amount", t.amount.toPlainString())
            .put("category", t.category).put("payment", t.payment).put("date", t.date).put("cleared", t.cleared).put("peerId", t.peerId ?: JSONObject.NULL).put("seriesId", t.seriesId ?: JSONObject.NULL).put("occurrence", t.occurrence ?: JSONObject.NULL).put("amountConfirmed", t.amountConfirmed).put("readyToConfirm", t.readyToConfirm)) }
        val d = data.draft?.let { draft -> JSONObject().put("account", draft.account).put("income", draft.income)
            .put("amount", draft.amount).put("description", draft.description).put("category", draft.category)
            .put("payment", draft.payment).put("addingCategory", draft.addingCategory).put("addingPayment", draft.addingPayment)
            .put("newCategory", draft.newCategory).put("newPayment", draft.newPayment)
            .put("editingId", draft.editingId ?: JSONObject.NULL).put("date", draft.date).put("transferTarget", draft.transferTarget ?: JSONObject.NULL)
            .put("repeatFrequency", draft.repeatFrequency.name).put("repeatUntil", draft.repeatUntil)
            .put("repeatCount", draft.repeatCount).put("applyToSeries", draft.applyToSeries) }
        val result = JSONObject().put("schema", if (data.dateMigrationVersion > 0) 11 else if (data.transactionOrder != TransactionOrder.ORIGINAL) 10 else if (data.anticipation != Anticipation.DUE) 9 else if (data.rules.isNotEmpty() || data.draft?.repeatFrequency?.let { it != RepeatFrequency.NONE } == true) 9 else if (data.transferPayment != null) 4 else if (data.transactions.any { it.peerId != null } || data.draft?.transferTarget != null) 3 else if (data.accountNames.isEmpty()) 1 else 2)
            .put("dateMigrationVersion", data.dateMigrationVersion).put("transactionOrder", data.transactionOrder.name).put("anticipation", data.anticipation.name).put("rules", JSONArray(data.rules.map(RecurrenceCodec::encode))).put("transferPayment", data.transferPayment ?: JSONObject.NULL).put("accountNames", JSONArray(data.accountNames)).put("transactions", rows)
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
        require(schema in 1..11)
        fun names(key: String): List<String> {
            val list = root.getJSONArray(key)
            require(list.length() in 1..500)
            return (0 until list.length()).map { i -> list.getString(i).also {
                require(it.isNotBlank() && it.length <= 60 && it == it.trim())
            } }.also { values -> require(values.map { it.lowercase(java.util.Locale.ROOT) }.distinct().size == values.size) }
        }
        val categories = names("categories")
        val payments = names("payments")
        val transferPayment = if (schema >= 4 && !root.isNull("transferPayment")) root.getString("transferPayment").also { require(it in payments) } else null
        fun amount(text: String): BigDecimal {
            require(Regex("-?[0-9]{1,12}([.][0-9]{1,2})?").matches(text))
            return text.toBigDecimal().setScale(2)
        }
        val openingsJson = root.getJSONArray("openings")
        require(openingsJson.length() in 1..50)
        val openings = (0 until openingsJson.length()).map { amount(openingsJson.getString(it)) }
        val accountNames = if (schema == 1 || (schema >= 3 && root.getJSONArray("accountNames").length() == 0)) {
            require(openings.size == 2)
            emptyList()
        } else names("accountNames").also { require(it.size == openings.size) }
        val entries = root.getJSONArray("transactions")
        require(entries.length() <= 10_000)
        val parsedRows = (0 until entries.length()).map { i ->
            val t = entries.getJSONObject(i)
            DemoTransaction(t.getLong("id"), t.getInt("account"), t.getString("description"),
                amount(t.getString("amount")), t.getString("category"), t.getString("payment"),
                t.getString("date"), t.getBoolean("cleared"),
                if (t.isNull("peerId")) null else t.getLong("peerId"),
                if (t.isNull("seriesId")) null else t.getString("seriesId"),
                if (t.isNull("occurrence")) null else t.getInt("occurrence"),
                if (schema >= 6) t.getBoolean("amountConfirmed") else
                    (t.isNull("seriesId") || t.optInt("occurrence") == 0 || t.getBoolean("cleared")),
                if (schema >= 9) t.getBoolean("readyToConfirm") else false).also {
                require(it.id > 0 && it.id < Long.MAX_VALUE && it.account in openings.indices)
                require(it.description.isNotBlank() && it.description.length <= 100 && it.date.length <= 80)
                require(it.category in categories && it.payment in payments)
            }
        }
        val legacyById = parsedRows.associateBy { it.id }
        val rows = if (schema < 6) parsedRows.map { row ->
            if (row.seriesId != null && row.peerId != null) row.copy(amountConfirmed = row.amountConfirmed || legacyById[row.peerId]?.amountConfirmed == true)
            else row
        } else parsedRows
        require(rows.map { it.id }.distinct().size == rows.size)
        val byId = rows.associateBy { it.id }
        rows.forEach { row ->
            row.peerId?.let { id ->
                require(schema >= 3)
                if (transferPayment != null) require(row.payment == transferPayment)
                val peer = requireNotNull(byId[id])
                require(peer.id != row.id && peer.peerId == row.id && peer.account != row.account)
                require(row.amount.signum() != 0 && row.amount.add(peer.amount).signum() == 0)
                require(row.date == peer.date && row.description == peer.description &&
                    row.category == peer.category && row.payment == peer.payment &&
                    row.seriesId == peer.seriesId && row.occurrence == peer.occurrence && row.amountConfirmed == peer.amountConfirmed && row.readyToConfirm == peer.readyToConfirm)
            }
        }
        val d = root.optJSONObject("draft")?.let {
            EntryDraft(it.getInt("account"), it.getBoolean("income"), it.getString("amount"),
                it.getString("description"), it.getString("category"), it.getString("payment"),
                addingCategory = it.optBoolean("addingCategory"), addingPayment = it.optBoolean("addingPayment"),
                newCategory = it.optString("newCategory"), newPayment = it.optString("newPayment"),
                editingId = if (it.isNull("editingId")) null else it.getLong("editingId"),
                date = it.optString("date"),
                transferTarget = if (it.isNull("transferTarget")) null else it.getInt("transferTarget"),
                repeatFrequency = RepeatFrequency.valueOf(it.optString("repeatFrequency", "NONE")),
                repeatUntil = it.optString("repeatUntil"), repeatCount = it.optString("repeatCount"),
                applyToSeries = it.optBoolean("applyToSeries")).also { draft ->
                require(draft.account in openings.indices && draft.amount.length <= 30 && draft.description.length <= 100)
                require(draft.category in categories && draft.payment in payments)
                require(draft.editingId == null || rows.any { row -> row.id == draft.editingId })
                draft.transferTarget?.let { target ->
                    require(schema >= 3 && target in openings.indices && target != draft.account)
                    if (transferPayment != null) require(draft.payment == transferPayment)
                }
                draft.editingId?.let { id ->
                    val edited = requireNotNull(byId[id])
                    if (edited.peerId != null) require(draft.transferTarget != null)
                    if (edited.peerId != null) require(draft.account == edited.account &&
                        draft.transferTarget == byId[edited.peerId]?.account && draft.income == (edited.amount.signum() > 0))
                }
                require(draft.date.isEmpty() || OperationDates.parse(draft.date) != null)
                require(draft.repeatUntil.length <= 10 && draft.repeatCount.length <= 5)
                require(draft.newCategory.length <= 60 && draft.newPayment.length <= 60)
            }
        }
        val rulesJson = if (schema >= 5) root.getJSONArray("rules") else JSONArray()
        require(rulesJson.length() <= 500)
        val rules = (0 until rulesJson.length()).map {
            val j = rulesJson.getJSONObject(it)
            val rule = RecurrenceCodec.decode(j)
            if (schema < 6) rule.copy(confirmedThrough = rows.filter { row -> row.seriesId == rule.id && row.amountConfirmed }
                .maxOfOrNull { row -> requireNotNull(row.occurrence) } ?: 0) else rule
        }
        val result = LedgerData(rows, categories, payments, openings, d, accountNames, transferPayment, rules,
            if (schema >= 8) Anticipation.valueOf(root.getString("anticipation")) else Anticipation.DUE,
            if (schema >= 10) TransactionOrder.valueOf(root.getString("transactionOrder")) else TransactionOrder.ORIGINAL,
            if (schema >= 11) root.getInt("dateMigrationVersion").also { require(it == 1) } else 0)
        RecurrenceCodec.validate(result, schema)
        return result
    }
}
