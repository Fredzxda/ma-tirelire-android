package org.matirelire.android

import android.view.View
import android.view.ViewGroup
import android.view.inspector.WindowInspector
import android.widget.DatePicker
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.*
import org.junit.Assert.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

class SeriesNextDateDeviceTest {
    private fun descendants(view:View):Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) for (i in 0 until view.childCount) yieldAll(descendants(view.getChildAt(i)))
    }
    @get:Rule val ui=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Test fun nativeCalendarSavesNextDateAndCancelKeepsIt() {
        val today=LocalDate.now()
        val model=DemoViewModel().apply {
            initialize(listOf(DemoTransaction(1,0,"Fixture",BigDecimal("-1.00"),"Food","Card",today.toString())))
            beginEntry(today.minusDays(1))
            updateDraft(draft!!.copy(amount="10",description="Series fixture",category="Food",payment="Card",repeatFrequency=RepeatFrequency.MONTH))
            assertTrue(saveEntry(Locale.US,"unused"))
        }
        val id=model.rules.single().id
        val date=today.plusDays(5)
        ui.runOnUiThread { ui.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        ui.setContent { MaterialTheme { SeriesNextDateButton(model,model.rules.single()) } }
        ui.onNodeWithTag("series-next-date-$id").performClick()
        ui.runOnIdle {
            val views=WindowInspector.getGlobalWindowViews().flatMap { descendants(it).toList() }
            views.filterIsInstance<DatePicker>().single().updateDate(date.year,date.monthValue-1,date.dayOfMonth)
            views.single { it.id==android.R.id.button1 }.performClick()
        }
        ui.runOnIdle { assertEquals(date,model.rules.single().nextDate()) }
        ui.onNodeWithTag("series-next-date-$id").performClick()
        ui.runOnIdle {
            WindowInspector.getGlobalWindowViews().flatMap { descendants(it).toList() }
                .single { it.id==android.R.id.button2 }.performClick()
        }
        ui.runOnIdle { assertEquals(date,model.rules.single().nextDate()) }
    }
}
