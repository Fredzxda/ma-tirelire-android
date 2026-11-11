package org.matirelire.android

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

/** Blank test activity and synthetic state only: never opens the user's vault. */
class OperationsUiDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Before fun keepOnlyTheSyntheticTestActivityAwake() {
        ui.runOnUiThread { ui.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
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
    @Test fun hierarchySearchCollapseAndCreationFromSettings() {
        val model = model()
        model.ensureAccountNames(listOf("Current", "Savings"))
        model.createReference(true, "Bills", "Food")
        model.createReference(true, "Electricity", "Bills")
        ui.setContent { MaterialTheme { ReferenceManagementDialog(model) {} } }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ui.onNodeWithTag("management-categories-search").performTextInput("Electricity")

        ui.onNodeWithTag("management-categories-tree").performScrollToNode(hasTestTag("management-categories-select-Electricity"))
        ui.onNodeWithTag("management-categories-select-Electricity").performClick()
        ui.onNodeWithTag("management-categories-search").performTextClearance()

        ui.onNodeWithTag("management-categories-tree").performScrollToNode(hasTestTag("management-categories-toggle-Food"))
        ui.onNodeWithTag("management-categories-toggle-Food").performClick()
        ui.onNodeWithTag("management-categories-select-Electricity").assertDoesNotExist()
        ui.onNodeWithTag("management-categories-toggle-Food").performClick()
        ui.onNodeWithText(context.getString(R.string.refs_new_category)).performClick()
        ui.onNodeWithTag("reference-create-name").performTextInput("Gas")

        ui.onNodeWithTag("reference-create-confirm").performScrollTo().performClick()
        ui.runOnIdle {
            assertEquals("Food › Bills › Electricity › Gas", model.categoryPath("Gas"))
            assertEquals(3, model.transactions.size)
        }
        ui.onNodeWithTag("management-categories-search").performTextInput("Gas")

        ui.onNodeWithTag("management-categories-tree").performScrollToNode(hasTestTag("management-categories-select-Gas"))
        ui.onNodeWithTag("management-categories-select-Gas").assertExists()
    }
    @Test fun referenceManagerRenamesWithoutChangingFinancialValues() {
        val model = model()
        model.ensureAccountNames(listOf("Current", "Savings"))
        ui.setContent { MaterialTheme { ReferenceManagementDialog(model) {} } }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ui.onNodeWithText(context.getString(R.string.reference_name)).performScrollTo().performTextClearance()
        ui.onNodeWithText(context.getString(R.string.reference_name)).performTextInput("Groceries")

        ui.runOnIdle {
            assertEquals("Groceries", (model.referenceUiStates["editor-true-Food-name"] as androidx.compose.runtime.MutableState<*>).value)
        }
        ui.onNodeWithText(context.getString(R.string.save)).performScrollTo().performClick()
        ui.runOnIdle {
            assertTrue(model.transactions.all { it.category == "Groceries" })
            assertEquals(BigDecimal("-10.00"), model.transactions.first().amount)
            assertTrue(model.referenceOptions.containsKey("c:Groceries"))
        }
    }
    @Test fun inlineSearchCombinesWithPeriodAndClosingPreservesCriteria() {
        val model = model()
        screen(model)
        val menuHeight = ui.onNodeWithTag("operations-menu").fetchSemanticsNode().layoutInfo.height
        ui.onNodeWithTag("operation-2").assertDoesNotExist()
        ui.onNodeWithTag("menu-search").performClick()
        ui.onNodeWithTag("operations-query").performTextInput("Fixture")
        ui.onNodeWithTag("menu-search-active",useUnmergedTree = true).assertExists()
        assertEquals(menuHeight,ui.onNodeWithTag("operations-menu").fetchSemanticsNode().layoutInfo.height)
        ui.onNodeWithTag("operation-1").assertExists()
        ui.onNodeWithTag("operation-3").assertDoesNotExist()
        ui.onNodeWithTag("menu-filter").performClick()
        ui.onNodeWithTag("operations-query").assertDoesNotExist()
        ui.onNodeWithTag("period-all").performScrollTo().performClick()
        val caption = ui.onNodeWithTag("menu-filter-caption",useUnmergedTree = true)
        caption.assertTextEquals(InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.operations_history_short))
        val layouts = mutableListOf<TextLayoutResult>()
        caption.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        assertTrue(layouts.joinToString { "size=${it.size}, widthOverflow=${it.didOverflowWidth}, heightOverflow=${it.didOverflowHeight}, lines=${it.lineCount}, constraints=${it.layoutInput.constraints}, font=${it.layoutInput.style.fontSize}" }, layouts.all { it.lineCount == 1 && !it.isLineEllipsized(0) && it.getLineRight(0) - it.getLineLeft(0) <= it.size.width + 1f })
        assertEquals(menuHeight,ui.onNodeWithTag("operations-menu").fetchSemanticsNode().layoutInfo.height)
        ui.onNodeWithTag("operation-2").assertExists()
        ui.onNodeWithTag("operation-3").assertDoesNotExist()
        ui.onNodeWithTag("menu-filter").performClick()
        ui.onNodeWithTag("operations-panel").assertDoesNotExist()
        ui.runOnIdle { assertEquals("Fixture", model.searchQuery) }
        ui.onNodeWithTag("menu-search").performClick()
        ui.onNodeWithTag("operations-clear-query").performClick()
        ui.onNodeWithTag("menu-search-active",useUnmergedTree = true).assertDoesNotExist()
        assertEquals(menuHeight,ui.onNodeWithTag("operations-menu").fetchSemanticsNode().layoutInfo.height)
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
