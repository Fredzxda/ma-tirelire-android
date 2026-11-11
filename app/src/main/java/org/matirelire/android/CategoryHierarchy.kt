package org.matirelire.android

internal fun DemoViewModel.categoryAncestors(name: String): List<String> {
    val result = mutableListOf<String>()
    var current: String? = name.takeIf { it in categories }
    while (current != null && current !in result) {
        result.add(current)
        current = referenceOptions[ReferenceCatalog.key(true, current)]?.parent
    }
    return result.reversed()
}

internal fun DemoViewModel.categoryPath(name: String): String = categoryAncestors(name).joinToString(" › ")

internal fun DemoViewModel.categoryDescendants(name: String): Set<String> = categories.filter {
    it != name && name in categoryAncestors(it)
}.toSet()

/** Validate the complete new reference before a single encrypted persistence callback. */
internal fun DemoViewModel.createReference(category: Boolean, text: String, parent: String? = null): String? {
    val name = text.trim()
    val names = if (category) categories else payments
    if (name.isEmpty() || name.length > 60 || names.size >= 500 || names.any { it.equals(name, true) } ||
        (!category && parent != null) || (parent != null && parent !in categories)) return null
    val metadata = referenceOptions.toMutableMap()
    if (category && parent != null) metadata[ReferenceCatalog.key(true, name)] = ReferenceOptions(parent = parent)
    if (runCatching { ReferenceCatalog.validate(metadata, if (category) categories + name else categories,
            if (category) payments else payments + name, openings.indices, transferPayment) }.isFailure) return null
    names.add(name)
    referenceOptions.clear(); referenceOptions.putAll(metadata)
    onChange?.invoke()
    return name
}

internal data class CategoryTreeRow(val name: String, val depth: Int, val selectable: Boolean, val hasChildren: Boolean)

/** Include disabled ancestors as context without making filtered references selectable. */
internal fun DemoViewModel.categoryTreeRows(allowed: List<String>, expanded: Set<String>, query: String): List<CategoryTreeRow> {
    fun normalize(text: String) = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "").lowercase(java.util.Locale.ROOT)
    val words = normalize(query.trim()).split(Regex("\\s+")).filter { it.isNotBlank() }
    val matches = allowed.filter { name -> words.all { it in normalize(categoryPath(name)) } }.toSet()
    val visible = matches.flatMap { categoryAncestors(it) }.toSet()
    val children = categories.filter { it in visible }.groupBy { referenceOptions[ReferenceCatalog.key(true, it)]?.parent }
    val result = mutableListOf<CategoryTreeRow>()
    fun visit(parent: String?, depth: Int) {
        children[parent].orEmpty().forEach { name ->
            val hasChildren = children[name].orEmpty().isNotEmpty()
            result.add(CategoryTreeRow(name, depth, name in matches, hasChildren))
            if (hasChildren && (words.isNotEmpty() || name in expanded)) visit(name, depth + 1)
        }
    }
    visit(null, 0)
    return result
}
