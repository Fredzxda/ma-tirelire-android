package org.matirelire.android

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.*
import org.junit.Assert.*
import java.math.BigDecimal
import androidx.test.platform.app.InstrumentationRegistry

/** Synthetic activity only; never opens the personal vault. */
class CategoryIconsDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Before fun awake() { ui.runOnUiThread { ui.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) } }
    private fun model() = DemoViewModel().apply { initialize(listOf(
        DemoTransaction(1,0,"Fixture",BigDecimal("-10.00"),"Food","Card","2026-11-23"))) }
    @Test fun settingsSaveIconOnlyAfterExplicitSave() {
        val m=model()
        ui.setContent { MaterialTheme { ReferenceManagementDialog(m) {} } }
        ui.onNodeWithTag("editor-icon-Food-open").performScrollTo().performClick()
        ui.onNodeWithTag("category-icon-grid").performScrollToNode(hasTestTag("category-icon-choice-courses"))
        ui.onNodeWithTag("category-icon-choice-courses").performClick()
        ui.runOnIdle { assertNull(m.categoryIconId("Food")) }
        val text=InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.save)
        ui.onNodeWithText(text).performScrollTo().performClick()
        ui.runOnIdle { assertEquals("courses",m.categoryIconId("Food")); SnapshotCodec.encode(m.exportData()).fill(0) }
    }
    @Test fun landscapePickerReachesLastIconAndCancelDoesNotSelect() {
        ui.runOnUiThread { ui.activity.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        ui.waitUntil(10_000) { ui.activity.resources.configuration.orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE }
        val m=model();var calls=0
        ui.setContent { MaterialTheme { CategoryIconSelectorField(m,null,false,true,"fixture-icon") { _,_-> calls++ } } }
        ui.onNodeWithTag("fixture-icon-open").performClick()
        ui.onNodeWithTag("category-icon-grid").performScrollToNode(hasTestTag("category-icon-choice-autres"))
        ui.onNodeWithTag("category-icon-choice-autres").assertIsDisplayed()
        ui.onNodeWithTag("icon-close").performClick()
        ui.runOnIdle { assertEquals(0,calls) }
        ui.onNodeWithTag("fixture-icon-open").performClick()
        ui.onNodeWithTag("icon-inherit").performClick()
        ui.runOnIdle { assertEquals(1,calls) }
    }
    @Test fun splitIconsAreDistinctBoundedAndCanBeHidden() {
        val m=model()
        val ids=listOf("courses","logement","sante","telephone")
        ids.forEach { m.createReference(true,it,icon=it) }
        val row=m.transactions.single().copy(splits=ids.map { OperationSplit(it,"",BigDecimal("2.50")) })
        ui.setContent { MaterialTheme { CategoryIconStrip(m,row) } }
        ui.onNodeWithTag("operation-icon-courses").assertExists()
        ui.onNodeWithText("+1").assertExists()
        ui.onNodeWithTag("operation-icon-telephone").assertDoesNotExist()
        ui.runOnIdle { m.setShowCategoryIcons(false) }
        ui.onNodeWithTag("operation-icons-1").assertDoesNotExist()
    }
}
