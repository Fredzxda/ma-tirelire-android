package org.matirelire.android

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class CategoryHierarchyTest {
    private fun model() = DemoViewModel().apply {
        initialize(listOf(DemoTransaction(1, 0, "Fixture", BigDecimal("-1.00"), "Home", "Card", "2026-11-11")))
        createReference(true, "Bills", "Home")
        createReference(true, "Électricité", "Bills")
        createReference(true, "Water", "Bills")
    }
    @Test fun creationIsAtomicAndPortable() {
        val m = model(); val original = m.transactions.toList()
        var callbacks = 0
        m.onChange = { callbacks++; SnapshotCodec.decode(SnapshotCodec.encode(m.exportData())) }
        assertEquals("Gas", m.createReference(true, " Gas ", "Bills"))
        assertEquals(1, callbacks)
        assertEquals("Home › Bills › Gas", m.categoryPath("Gas"))
        assertEquals(original, m.transactions.toList())
        assertEquals(m.exportData(), SnapshotCodec.decode(SnapshotCodec.encode(m.exportData())))
    }
    @Test fun invalidOrDuplicateCreationDoesNotChangeAnything() {
        val m = model(); val before = m.exportData()
        assertNull(m.createReference(true, "bills"))
        assertNull(m.createReference(true, " "))
        assertNull(m.createReference(true, "x".repeat(61)))
        assertNull(m.createReference(true, "Gas", "Missing"))
        assertNull(m.createReference(false, "Cash", "Home"))
        assertEquals(before, m.exportData())
        assertEquals("Cash", m.createReference(false, "Cash"))
        assertEquals("Travel", m.createReference(true, "Travel"))
        assertNull(m.referenceOptions["c:Travel"]?.parent)
    }
    @Test fun treeKeepsDisabledAncestorsOfAllowedChildren() {
        val m = model()
        val rows = m.categoryTreeRows(listOf("Électricité"), setOf("Home", "Bills"), "")
        assertEquals(listOf("Home", "Bills", "Électricité"), rows.map { it.name })
        assertEquals(listOf(0, 1, 2), rows.map { it.depth })
        assertEquals(listOf(false, false, true), rows.map { it.selectable })
    }
    @Test fun searchIgnoresAccentsExpandsContextAndCollapseIsIndependent() {
        val m = model()
        assertEquals(listOf("Home"), m.categoryTreeRows(m.categories, emptySet(), "").map { it.name })
        val result = m.categoryTreeRows(m.categories, emptySet(), "electri")
        assertEquals(listOf("Home", "Bills", "Électricité"), result.map { it.name })
        assertEquals(listOf(false, false, true), result.map { it.selectable })
        assertTrue(m.categoryTreeRows(m.categories, emptySet(), "unknown").isEmpty())
        assertEquals(listOf("Home"), m.categoryTreeRows(m.categories, emptySet(), "").map { it.name })
    }
    @Test fun descendantsAndPathsFollowRenamingAndReparenting() {
        val m = model()
        assertEquals(setOf("Bills", "Électricité", "Water"), m.categoryDescendants("Home"))
        assertTrue(m.saveReference(true, "Home", "Housing", ReferenceOptions()))
        assertEquals("Housing › Bills › Water", m.categoryPath("Water"))
        assertFalse(m.saveReference(true, "Housing", "Housing", ReferenceOptions(parent = "Water")))
        assertTrue(m.saveReference(true, "Water", "Water", ReferenceOptions()))
        assertEquals("Water", m.categoryPath("Water"))
        assertEquals(setOf("Bills", "Électricité"), m.categoryDescendants("Housing"))
    }
}
