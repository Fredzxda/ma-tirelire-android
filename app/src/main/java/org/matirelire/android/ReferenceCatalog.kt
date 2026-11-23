package org.matirelire.android

import org.json.JSONArray
import org.json.JSONObject

internal enum class ReferenceKind { ANY, EXPENSE, INCOME }
internal data class ReferenceOptions(val parent: String? = null, val kind: ReferenceKind = ReferenceKind.ANY,
    val hidden: Boolean = false, val accounts: Set<Int> = emptySet(),
    val icon: String? = null, val inheritIcon: Boolean = false)

internal object ReferenceCatalog {
    fun key(category: Boolean, name: String) = (if (category) "c:" else "p:") + name
    fun encode(options: Map<String, ReferenceOptions>) = JSONObject().apply {
        options.forEach { (key, value) -> put(key, JSONObject().put("parent", value.parent ?: JSONObject.NULL)
            .put("icon", value.icon ?: JSONObject.NULL).put("inheritIcon", value.inheritIcon)
            .put("kind", value.kind.name).put("hidden", value.hidden).put("accounts", JSONArray(value.accounts.sorted()))) }
    }
    fun validate(options: Map<String, ReferenceOptions>, categories: List<String>, payments: List<String>, accounts: IntRange, transfer: String?) {
        require(options.size <= categories.size + payments.size)
        options.forEach { (key, value) ->
            require(key.startsWith("c:") || key.startsWith("p:"))
            val category = key.startsWith("c:")
            require(key.drop(2) in if (category) categories else payments)
            require(value.accounts.all { it in accounts })
            require(value.icon == null || value.icon in CategoryIconCatalog.ids)
            require(value.icon == null || !value.inheritIcon)
            if (!category) require(value.parent == null && value.icon == null && !value.inheritIcon)
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
    fun decode(json: JSONObject, categories: List<String>, payments: List<String>, accounts: IntRange, transfer: String?, includeIcons: Boolean = true): Map<String, ReferenceOptions> {
        require(json.length() <= 1000)
        return json.keys().asSequence().associateWith { key ->
            val obj = json.getJSONObject(key)
            val list = obj.getJSONArray("accounts")
            require(list.length() <= accounts.count())
            val values = (0 until list.length()).map { list.getInt(it) }
            require(values.distinct().size == values.size)
            ReferenceOptions(if (obj.isNull("parent")) null else obj.getString("parent"),
                ReferenceKind.valueOf(obj.getString("kind")), obj.getBoolean("hidden"), values.toSet(),
                if (includeIcons && !obj.isNull("icon")) obj.getString("icon") else null,
                if (includeIcons) obj.optBoolean("inheritIcon", false) else false)
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
    val used = (transactions + deferredForecasts).any { (if (category) it.category else it.payment) == name || (category && it.splits.any { line -> line.category == name }) } ||
        rules.any { (if (category) it.category else it.payment) == name || (category && it.splits.any { line -> line.category == name }) } ||
        draft?.let { (if (category) it.category else it.payment) == name || (category && it.splits.any { line -> line.category == name }) } == true
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
    fun rewriteRow(row: DemoTransaction): DemoTransaction = if (category) row.copy(
        category = if (row.category == old) next else row.category,
        splits = row.splits.map { if (it.category == old) it.copy(category = next) else it })
        else if (row.payment == old) row.copy(payment = next) else row
    transactions.indices.forEach { i -> transactions[i] = rewriteRow(transactions[i]) }
    deferredForecasts.indices.forEach { i -> deferredForecasts[i] = rewriteRow(deferredForecasts[i]) }
    rules.indices.forEach { i ->
        val rule = rules[i]
        rules[i] = if (category) rule.copy(category = if (rule.category == old) next else rule.category,
            splits = rule.splits.map { if (it.category == old) it.copy(category = next) else it })
            else if (rule.payment == old) rule.copy(payment = next) else rule
    }
    draft?.let { value ->
        val callback = onChange; onChange = null
        try {
            if (category) updateDraft(value.copy(category = if (value.category == old) next else value.category,
                splits = value.splits.map { if (it.category == old) it.copy(category = next) else it }))
            else if (value.payment == old) updateDraft(value.copy(payment = next))
        } finally { onChange = callback }
    }
}
