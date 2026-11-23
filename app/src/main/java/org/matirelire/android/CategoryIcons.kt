package org.matirelire.android

/** Inheritance is explicit at each level; an unassigned parent stops resolution. */
internal fun DemoViewModel.categoryIconId(name: String): String? {
    val seen = mutableSetOf<String>()
    var current: String? = name
    while (current != null && current in categories && seen.add(current)) {
        val options = referenceOptions[ReferenceCatalog.key(true, current)] ?: return null
        options.icon?.let { return it.takeIf { id -> id in CategoryIconCatalog.ids } }
        if (!options.inheritIcon) return null
        current = options.parent
    }
    return null
}

internal fun DemoViewModel.operationIconIds(row: DemoTransaction): List<String> {
    if (row.peerId != null) return emptyList()
    val names = if (row.splits.isEmpty()) listOf(row.category) else row.splits.map { it.category }
    return names.mapNotNull { categoryIconId(it) }.distinct()
}
