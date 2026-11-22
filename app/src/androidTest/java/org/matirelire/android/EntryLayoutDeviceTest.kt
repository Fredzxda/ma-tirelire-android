package org.matirelire.android

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

/** Isolated form components; never opens the user's vault or changes Android settings. */
class EntryLayoutDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Before fun awake() { ui.runOnUiThread {
        ui.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } }
    private fun model() = DemoViewModel().apply {
        initialize(listOf(DemoTransaction(1, 0, "Fixture", BigDecimal("-5.00"), "Food", "Card", "2026-11-22")))
        beginEntry(LocalDate.of(2026, 11, 22))
        updateDraft(draft!!.copy(amount = "50", description = "Fixture", category = "Food", payment = "Card", repeatFrequency = RepeatFrequency.MONTH))
    }
    @Test fun portraitCompanionFieldsKeepMostWidthForAmountAndDescription() = companionRows(false)
    @Test fun landscapeCompanionFieldsAndLastActionRemainReachable() = companionRows(true)
    private fun companionRows(landscape: Boolean) {
        ui.runOnUiThread { ui.activity.requestedOrientation = if (landscape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
        ui.waitUntil(10_000) { ui.activity.resources.configuration.orientation == if (landscape) Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT }
        val m = model()
        ui.setContent { MaterialTheme { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            EntryFinancialFields(m, m.draft!!)
            EntryRepeat(m, m.draft!!)
            Button(onClick = {}, modifier = Modifier.testTag("entry-last-action")) { Text("Fixture action") }
        } } }
        val amount = ui.onNodeWithTag("entry-amount").fetchSemanticsNode().boundsInRoot
        val currency = ui.onNodeWithTag("currency-selector").fetchSemanticsNode().boundsInRoot
        assertTrue(amount.left < currency.left)
        assertTrue(amount.width > currency.width)
        assertTrue(kotlin.math.abs(amount.top - currency.top) < 4)
        ui.onNodeWithTag("description-payment-row").performScrollTo()
        val description = ui.onNodeWithTag("entry-description").fetchSemanticsNode().boundsInRoot
        val payment = ui.onNodeWithTag("entry-payment").fetchSemanticsNode().boundsInRoot
        assertTrue(description.left < payment.left)
        assertTrue(description.width > payment.width)
        ui.onNodeWithTag("entry-last-action").performScrollTo().assertIsDisplayed()
    }
    @Test fun recurrenceHintsOnlyAppearInInformationPopup() {
        val m = model()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ui.setContent { MaterialTheme { Column(Modifier.verticalScroll(rememberScrollState())) { EntryRepeat(m, m.draft!!) } } }
        ui.onNodeWithText(context.getString(R.string.repeat_limits_hint)).assertDoesNotExist()
        ui.onNodeWithTag("repeat-information").performClick()
        ui.onNodeWithText(context.getString(R.string.repeat_limits_hint)).assertExists()
        ui.onNodeWithTag("repeat-information-content").assertExists()
    }
    @Test fun creatingExistingPaymentSelectsItWithoutDuplicate() {
        val m = model()
        val before = m.payments.size
        m.updateDraft(m.draft!!.copy(payment = ""))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ui.setContent { MaterialTheme {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) { EntryFinancialFields(m, m.draft!!) }
        } }
        ui.onNodeWithTag("entry-payment").performScrollTo().performClick()
        ui.onNodeWithText(context.getString(R.string.add_reference, context.getString(R.string.payment))).performClick()
        ui.onNodeWithTag("entry-payment-create-name").performTextInput("  card  ")
        ui.onNodeWithTag("entry-payment-create-confirm").performClick()
        ui.runOnIdle {
            assertEquals(before, m.payments.size)
            assertEquals("Card", m.draft!!.payment)
        }
    }
    @Test fun enlargedTextStacksSelectorsAndStillReachesFinalAction() {
        val m = model()
        ui.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 1.6f)) {
                MaterialTheme { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    EntryFinancialFields(m, m.draft!!)
                    EntryRepeat(m, m.draft!!)
                    Button(onClick = {}, modifier = Modifier.testTag("entry-last-action")) { Text("Fixture action") }
                } }
            }
        }
        ui.onNodeWithTag("amount-currency-row").assertDoesNotExist()
        ui.onNodeWithTag("description-payment-row").assertDoesNotExist()
        ui.onNodeWithTag("entry-last-action").performScrollTo().assertIsDisplayed()
    }
}
