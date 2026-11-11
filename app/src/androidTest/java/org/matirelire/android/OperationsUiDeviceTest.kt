package org.matirelire.android

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

/** Blank test activity and synthetic state only: never opens the user's vault. */
class OperationsUiDeviceTest {
    @get:Rule val ui = createComposeRule()
    private val today = LocalDate.now()
    private fun model() = DemoViewModel().apply {
        initialize(listOf(
            DemoTransaction(1,0,"Fixture current",BigDecimal("-10.00"),"Food","Card",today.withDayOfMonth(1).toString()),
            DemoTransaction(2,0,"Fixture previous",BigDecimal("-20.00"),"Food","Card",today.minusMonths(1).withDayOfMonth(1).toString()),
            DemoTransaction(3,0,"Other current",BigDecimal("5.00"),"Food","Card",today.withDayOfMonth(2).toString())))
    }
    private fun screen(model: DemoViewModel, onEdit: (Long) -> Unit = {}, onToggle: (Long) -> Unit = {}) {
        ui.setContent {
            MaterialTheme {
                TransactionsScreen(listOf("Current","Savings"),0,model.balance(0),model.balance(0,true),
                    model.transactionOrder.arrange(model.transactions.toList()),onAccount = {},onToggle = onToggle,
                    onReconcile = {},onEdit = onEdit,onNew = {},model = model)
            }
        }
    }
    @Test fun inlineSearchCombinesWithPeriodAndClosingPreservesCriteria() {
        val model = model()
        screen(model)
        ui.onNodeWithTag("operation-2").assertDoesNotExist()
        ui.onNodeWithTag("menu-search").performClick()
        ui.onNodeWithTag("operations-query").performTextInput("Fixture")
        ui.onNodeWithTag("operation-1").assertExists()
        ui.onNodeWithTag("operation-3").assertDoesNotExist()
        ui.onNodeWithTag("menu-filter").performClick()
        ui.onNodeWithTag("operations-query").assertDoesNotExist()
        ui.onNodeWithTag("period-all").performScrollTo().performClick()
        ui.onNodeWithTag("operation-2").assertExists()
        ui.onNodeWithTag("operation-3").assertDoesNotExist()
        ui.onNodeWithTag("menu-filter").performClick()
        ui.onNodeWithTag("operations-panel").assertDoesNotExist()
        ui.runOnIdle { assertEquals("Fixture", model.searchQuery) }
        ui.onNodeWithTag("menu-search").performClick()
        ui.onNodeWithTag("operations-clear-query").performClick()
        ui.onNodeWithTag("operations-list").performScrollToNode(hasTestTag("operation-3"))
        ui.onNodeWithTag("operation-3").assertExists()
        ui.runOnIdle { assertEquals(PeriodScope.ALL,model.accountPeriod.scope) }
    }
    @Test fun firstOutsideClickEditsOnceAndPointingNeverOpensEntry() {
        val model = model()
        var edits = 0
        var toggles = 0
        screen(model,onEdit = { edits++ },onToggle = { toggles++; model.toggleCleared(it) })
        ui.onNodeWithTag("menu-functions").performClick()
        ui.onNodeWithTag("pointing-1").performTouchInput { click(center) }
        ui.onNodeWithTag("operations-panel").assertDoesNotExist()
        ui.runOnIdle {
            assertEquals(1,toggles); assertEquals(0,edits)
            assertTrue(model.transactions.first { it.id == 1L }.cleared)
        }
        ui.onNodeWithTag("menu-functions").performClick()
        ui.onNodeWithTag("operation-1").performTouchInput { click(Offset(20f,20f)) }
        ui.onNodeWithTag("operations-panel").assertDoesNotExist()
        ui.runOnIdle { assertEquals(1,edits); assertEquals(1,toggles) }
    }
    @Test fun blankOutsideTapDismissesWithoutClearingQuery() {
        val model = model()
        screen(model)
        ui.onNodeWithTag("menu-search").performClick()
        ui.onNodeWithTag("operations-query").performTextInput("No matching fixture")
        ui.onNodeWithTag("operations-list").performTouchInput { click(Offset(8f,8f)) }
        ui.onNodeWithTag("operations-panel").assertDoesNotExist()
        ui.runOnIdle { assertEquals("No matching fixture",model.searchQuery) }
    }
    @Test fun recurrenceFieldFramesKeepTheirHeightForEmptyTypedAndChosenValues() {
        val model = model().apply {
            beginEntry(today)
            updateDraft(draft!!.copy(repeatFrequency = RepeatFrequency.MONTH))
        }
        ui.setContent { MaterialTheme { Column(Modifier.width(380.dp).verticalScroll(rememberScrollState())) { EntryRepeat(model,model.draft!!) } } }
        val emptyEnd = ui.onNodeWithTag("repeat-end").fetchSemanticsNode().layoutInfo.height.toFloat()
        val emptyCount = ui.onNodeWithTag("repeat-count-frame").fetchSemanticsNode().layoutInfo.height.toFloat()
        assertEquals(emptyEnd,emptyCount,0.5f)
        ui.onNodeWithTag("repeat-count").performScrollTo().performTextInput("12")
        ui.runOnIdle { assertEquals("12",model.draft!!.repeatCount) }
        assertEquals(emptyCount,ui.onNodeWithTag("repeat-count-frame").fetchSemanticsNode().layoutInfo.height.toFloat(),0.5f)
        ui.runOnIdle { assertTrue(model.selectRepeatEnd(today.plusMonths(3).toString())) }
        ui.waitForIdle()
        assertEquals(emptyEnd,ui.onNodeWithTag("repeat-end").fetchSemanticsNode().layoutInfo.height.toFloat(),0.5f)
        assertEquals(emptyCount,ui.onNodeWithTag("repeat-count-frame").fetchSemanticsNode().layoutInfo.height.toFloat(),0.5f)
        ui.runOnIdle { assertEquals("",model.draft!!.repeatCount) }
    }
}
