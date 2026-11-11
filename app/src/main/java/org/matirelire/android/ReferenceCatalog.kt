package org.matirelire.android

import org.json.JSONArray
import org.json.JSONObject

internal enum class ReferenceKind { ANY, EXPENSE, INCOME }
internal data class ReferenceOptions(val parent: String? = null, val kind: ReferenceKind = ReferenceKind.ANY,
    val hidden: Boolean = false, val accounts: Set<Int> = emptySet())

internal object ReferenceCatalog {
    fun key(category: Boolean, name: String) = (if (category) "c:" else "p:") + name
    fun encode(options: Map<String, ReferenceOptions>) = JSONObject().apply {
        options.forEach { (key, value) -> put(key, JSONObject().put("parent", value.parent ?: JSONObject.NULL)
            .put("kind", value.kind.name).put("hidden", value.hidden).put("accounts", JSONArray(value.accounts.sorted()))) }
    }
    fun validate(options: Map<String, ReferenceOptions>, categories: List<String>, payments: List<String>, accounts: IntRange, transfer: String?) {
        require(options.size <= categories.size + payments.size)
        options.forEach { (key, value) ->
            require(key.startsWith("c:") || key.startsWith("p:"))
            val category = key.startsWith("c:")
            require(key.drop(2) in if (category) categories else payments)
            require(value.accounts.all { it in accounts })
            if (!category) require(value.parent == null)
            value.parent?.let { require(it in categories) }
            if (key == key(false, transfer.orEmpty())) require(value == ReferenceOptions())
            val seen = mutableSetOf(key.drop(2))
            var parent = value.parent
            while (parent != null) {
                require(seen.add(parent))
                parent = options[key(true, parent)]?.parent
            }
        }
    }
    fun decode(json: JSONObject, categories: List<String>, payments: List<String>, accounts: IntRange, transfer: String?): Map<String, ReferenceOptions> {
        require(json.length() <= 1000)
        return json.keys().asSequence().associateWith { key ->
            val obj = json.getJSONObject(key)
            val list = obj.getJSONArray("accounts")
            require(list.length() <= accounts.count())
            val values = (0 until list.length()).map { list.getInt(it) }
            require(values.distinct().size == values.size)
            ReferenceOptions(if (obj.isNull("parent")) null else obj.getString("parent"),
                ReferenceKind.valueOf(obj.getString("kind")), obj.getBoolean("hidden"), values.toSet())
        }.also { validate(it, categories, payments, accounts, transfer) }
    }
}

internal fun DemoViewModel.referenceChoices(category: Boolean, account: Int, income: Boolean, current: String = ""): List<String> {
    val names = if (category) categories else payments
    val filtered = names.filter { name ->
        val options = referenceOptions[ReferenceCatalog.key(category, name)] ?: ReferenceOptions()
        name == current || (!options.hidden && (options.accounts.isEmpty() || account in options.accounts) &&
            (options.kind == ReferenceKind.ANY || (options.kind == ReferenceKind.INCOME) == income))
    }
    // A depth-first order keeps each category beside its children without changing stored references.
    if (!category) return filtered
    val ordered = mutableListOf<String>()
    fun visit(parent: String?) {
        names.filter { referenceOptions[ReferenceCatalog.key(true, it)]?.parent == parent }.forEach {
            if (it in filtered) ordered.add(it)
            visit(it)
        }
    }
    visit(null)
    return ordered
}

internal fun DemoViewModel.saveReference(category: Boolean, old: String, name: String, options: ReferenceOptions): Boolean {
    val names = if (category) categories else payments
    val next = name.trim()
    if (old !in names || next.isBlank() || next.length > 60 || names.any { it != old && it.equals(next, true) }) return false
    val nextNames = names.map { if (it == old) next else it }
    val metadata = referenceOptions.toMutableMap()
    metadata.remove(ReferenceCatalog.key(category, old))
    metadata[ReferenceCatalog.key(category, next)] = options
    if (category) metadata.replaceAll { _, value -> if (value.parent == old) value.copy(parent = next) else value }
    val transfer = if (!category && transferPayment == old) next else transferPayment
    if (runCatching { ReferenceCatalog.validate(metadata, if (category) nextNames else categories,
            if (category) payments else nextNames, openings.indices, transfer) }.isFailure) return false
    rewriteReference(category, old, next)
    names[names.indexOf(old)] = next
    if (!category) renameTransferPayment(old, next)
    referenceOptions.clear(); referenceOptions.putAll(metadata)
    onChange?.invoke()
    return true
}

internal fun DemoViewModel.removeReference(category: Boolean, name: String, replacement: String?): Boolean {
    val names = if (category) categories else payments
    if (name !in names || names.size <= 1 || (!category && name == transferPayment)) return false
    if (replacement != null && (replacement !in names || replacement == name || (!category && replacement == transferPayment))) return false
    val used = transactions.any { (if (category) it.category else it.payment) == name } ||
        rules.any { (if (category) it.category else it.payment) == name } ||
        draft?.let { (if (category) it.category else it.payment) == name } == true
    if (used && replacement == null) return false
    // Children are reparented to the removed category's parent, independently of replacement.
    val metadata = referenceOptions.toMutableMap()
    val parent = metadata.remove(ReferenceCatalog.key(category, name))?.parent
    if (category) metadata.replaceAll { _, value -> if (value.parent == name) value.copy(parent = parent) else value }
    if (replacement != null) rewriteReference(category, name, replacement)
    names.remove(name)
    referenceOptions.clear(); referenceOptions.putAll(metadata)
    onChange?.invoke()
    return true
}

private fun DemoViewModel.rewriteReference(category: Boolean, old: String, next: String) {
    transactions.indices.forEach { i ->
        val row = transactions[i]
        if (category && row.category == old) transactions[i] = row.copy(category = next)
        if (!category && row.payment == old) transactions[i] = row.copy(payment = next)
    }
    rules.indices.forEach { i ->
        val rule = rules[i]
        if (category && rule.category == old) rules[i] = rule.copy(category = next)
        if (!category && rule.payment == old) rules[i] = rule.copy(payment = next)
    }
    draft?.let { value ->
        // updateDraft persists; postpone that callback until references are changed atomically.
        val callback = onChange; onChange = null
        try {
            if (category && value.category == old) updateDraft(value.copy(category = next))
            if (!category && value.payment == old) updateDraft(value.copy(payment = next))
        } finally { onChange = callback }
    }
}
