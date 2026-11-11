package org.matirelire.android

import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Test

class TransactionSearchTest {
    private val rows = listOf(
        DemoTransaction(1, 0, "Électricité été", BigDecimal("-42.50"), "Maison", "Carte", "2026-11-11"),
        DemoTransaction(2, 0, "Courses", BigDecimal("-10.00"), "Alimentation", "Carte", "2026-11-11"),
        DemoTransaction(3, 0, "Salaire", BigDecimal("100.00"), "Revenus", "Virement", "2026-11-11"))

    @Test fun ignoresAccentsCaseAndWhitespace() {
        assertEquals(listOf(rows[0]), TransactionSearch.filter(rows, "  ELECTRICITE   ETE  "))
    }
    @Test fun allWordsMayMatchDifferentFields() {
        assertEquals(listOf(rows[0]), TransactionSearch.filter(rows, "maison carte"))
        assertTrue(TransactionSearch.filter(rows, "maison virement").isEmpty())
    }
    @Test fun blankRestoresAllInSameOrder() {
        assertEquals(rows, TransactionSearch.filter(rows, "  "))
        assertEquals(listOf(rows[0], rows[1]), TransactionSearch.filter(rows, "carte"))
    }
    @Test fun queryIsLiteralAndDoesNotMutateRows() {
        assertTrue(TransactionSearch.filter(rows, ".*").isEmpty())
        assertEquals(BigDecimal("47.50"), rows.fold(BigDecimal.ZERO) { sum, row -> sum + row.amount })
    }
    @Test fun lockingClearsSearchWithoutPersistingIt() {
        val model = DemoViewModel().apply { initialize(rows) }
        model.searchQuery = "Confidential"
        assertFalse(SnapshotCodec.encode(model.exportData()).toString(Charsets.UTF_8).contains("Confidential"))
        model.clearSensitive()
        assertEquals("", model.searchQuery)
    }
}
