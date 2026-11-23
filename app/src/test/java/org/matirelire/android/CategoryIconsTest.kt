package org.matirelire.android

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class CategoryIconsTest {
    private val icon get() = CategoryIconCatalog.ids.first()
    private fun model() = DemoViewModel().apply {
        initialize(listOf(DemoTransaction(1, 0, "Fixture", BigDecimal("-3.00"), "Home", "Card", "2026-11-11")))
        createReference(true, "Child", "Home", inheritIcon = true)
        createReference(true, "None", "Home")
    }

    @Test fun creationAndValidationAreAtomic() {
        val m = model(); var writes = 0
        m.onChange = { writes++; SnapshotCodec.encode(m.exportData()) }
        assertEquals("Icon", m.createReference(true, "Icon", icon = icon))
        assertEquals(1, writes)
        val before = m.exportData()
        assertNull(m.createReference(true, "Bad", icon = "unknown-icon"))
        assertNull(m.createReference(true, "Bad", icon = icon, inheritIcon = true))
        assertNull(m.createReference(false, "Bad", icon = icon))
        assertNull(m.createReference(false, "Bad", inheritIcon = true))
        assertFalse(m.saveReference(false, "Card", "Card", ReferenceOptions(icon = icon)))
        assertEquals(before, m.exportData()); assertEquals(1, writes)
    }

    @Test fun inheritanceNoneRenamingAndDeletionFollowHierarchy() {
        val m = model()
        assertTrue(m.saveReference(true, "Home", "Housing", ReferenceOptions(icon = icon)))
        assertEquals(icon, m.categoryIconId("Child"))
        assertNull(m.categoryIconId("None"))
        assertEquals("Housing", m.referenceOptions["c:Child"]!!.parent)
        assertTrue(m.removeReference(true, "Housing", "None"))
        assertNull(m.categoryIconId("Child"))
        assertNull(m.referenceOptions["c:Child"]!!.parent)
        m.referenceOptions["c:Child"] = ReferenceOptions(parent = "Child", inheritIcon = true)
        assertNull(m.categoryIconId("Child"))
    }

    @Test fun splitIconsAreDistinctInLineOrderAndTransfersHaveNone() {
        val m = model(); val second = CategoryIconCatalog.ids.first { it != icon }
        m.saveReference(true, "Home", "Home", ReferenceOptions(icon = icon))
        m.saveReference(true, "None", "None", ReferenceOptions(icon = second))
        val row = m.transactions.single().copy(splits = listOf(
            OperationSplit("None", "A", BigDecimal.ONE),
            OperationSplit("Child", "B", BigDecimal.ONE),
            OperationSplit("Home", "C", BigDecimal.ONE)))
        assertEquals(listOf(second, icon), m.operationIconIds(row))
        assertTrue(m.operationIconIds(row.copy(peerId = 2)).isEmpty())
        assertEquals(listOf(icon), m.operationIconIds(m.transactions.single()))
    }

    @Test fun schema17RoundTripPreferenceRestoreAndClear() {
        val m = model(); m.saveReference(true, "Home", "Home", ReferenceOptions(icon = icon))
        var writes = 0; m.onChange = { writes++ }
        m.setShowCategoryIcons(false); m.setShowCategoryIcons(false)
        assertEquals(1, writes)
        val data = m.exportData(); val bytes = SnapshotCodec.encode(data)
        assertEquals(17, JSONObject(bytes.toString(Charsets.UTF_8)).getInt("schema"))
        assertEquals(data, SnapshotCodec.decode(bytes))
        val restored = DemoViewModel(); restored.restoreData(SnapshotCodec.decode(bytes))
        assertFalse(restored.showCategoryIcons)
        assertEquals(icon, restored.categoryIconId("Child"))
        restored.clearSensitive(); assertTrue(restored.showCategoryIcons)
        assertTrue(restored.referenceOptions.isEmpty())
    }

    @Test fun olderSchemasIgnoreFutureIconFields() {
        val m = model()
        val json = JSONObject(SnapshotCodec.encode(m.exportData()).toString(Charsets.UTF_8))
        json.getJSONObject("referenceOptions").getJSONObject("c:Child")
            .put("icon", "unknown-future-icon").put("inheritIcon", true)
        json.put("schema", 12).put("showCategoryIcons", false)
        val decoded = SnapshotCodec.decode(json.toString().toByteArray(Charsets.UTF_8))
        assertTrue(decoded.showCategoryIcons)
        assertNull(decoded.referenceOptions["c:Child"]!!.icon)
        assertFalse(decoded.referenceOptions["c:Child"]!!.inheritIcon)
        assertEquals(decoded, SnapshotCodec.decode(SnapshotCodec.encode(decoded)))
        json.put("schema", 17)
        assertTrue(runCatching { SnapshotCodec.decode(json.toString().toByteArray(Charsets.UTF_8)) }.isFailure)
    }
}
