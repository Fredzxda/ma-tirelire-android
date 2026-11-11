package org.matirelire.android

import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class AccountPeriodTest {
    private fun row(id: Long, date: String) = DemoTransaction(id, 0, "Fixture", BigDecimal("1.00"), "Food", "Card", date)
    private val today = LocalDate.of(2026, 11, 11)
    private val rows = listOf(row(1, "2026-10-31"), row(2, "2026-11-01"), row(3, "30 nov. 2026"),
        row(4, "2026-12-01"), row(5, "01/02/2026"))

    @Test fun currentMonthIncludesItsBoundsAndNamedLegacyDates() {
        assertEquals(listOf(2L,3L), AccountPeriod().filter(rows, today).map { it.id })
        assertEquals(listOf(4L), AccountPeriod().filter(rows, LocalDate.of(2026,12,1)).map { it.id })
    }
    @Test fun navigationCrossesYearsAndStopsAtSupportedBounds() {
        val december = AccountPeriod(YearMonth.of(2026,12), followsCurrent = false)
        assertEquals(YearMonth.of(2027,1), december.move(1, today)!!.month)
        assertEquals(YearMonth.of(2026,11), december.move(-1,today)!!.month)
        assertNull(AccountPeriod(YearMonth.of(1,1), followsCurrent = false).move(-1,today))
        assertNull(AccountPeriod(YearMonth.of(9999,12), followsCurrent = false).move(1,today))
    }
    @Test fun explicitSelectionDoesNotFollowClockButCurrentMonthDoes() {
        val next = LocalDate.of(2026,12,1)
        val selected = AccountPeriod().move(-1,today)!!
        assertEquals(YearMonth.of(2026,10), selected.effectiveMonth(next))
        assertEquals(YearMonth.of(2026,12), AccountPeriod().effectiveMonth(next))
    }
    @Test fun leapDayEmptyMonthAndUnknownDatesStayAccessible() {
        val sample = listOf(row(1,"2024-02-29"),row(2,"2024-03-01"),row(3,"unreadable"))
        assertEquals(listOf(1L), AccountPeriod(YearMonth.of(2024,2), followsCurrent = false).filter(sample,today).map { it.id })
        assertTrue(AccountPeriod(YearMonth.of(2024,1), followsCurrent = false).filter(sample,today).isEmpty())
        assertEquals(listOf(sample[2]), AccountPeriod(scope = PeriodScope.UNDATED).filter(sample,today))
        assertEquals(sample,AccountPeriod(scope = PeriodScope.ALL).filter(sample,today))
    }
    @Test fun filteringPreservesOrderWithoutChangingAccountingOrSearchScope() {
        val ordered = TransactionOrder.NEWEST.arrange(rows)
        val visible = AccountPeriod().filter(ordered,today)
        assertEquals(listOf(3L,2L),visible.map { it.id })
        assertEquals(listOf(5L),AccountPeriod(scope = PeriodScope.UNDATED).filter(rows,today).map { it.id })
        assertEquals(5,TransactionSearch.filter(ordered,"Fixture").size)
        assertEquals(BigDecimal("5.00"),rows.fold(BigDecimal.ZERO) { sum,row -> sum+row.amount })
    }
    @Test fun periodIsSessionOnlyAndLockResetsIt() {
        val model = DemoViewModel().apply { initialize(rows) }
        val before = model.exportData()
        model.accountPeriod = AccountPeriod(YearMonth.of(2020,1),PeriodScope.ALL,false)
        model.operationsMenu = OperationsMenu.SEARCH
        assertEquals(before,model.exportData())
        model.clearSensitive()
        assertEquals(PeriodScope.MONTH,model.accountPeriod.scope)
        assertTrue(model.accountPeriod.followsCurrent)
        assertEquals(OperationsMenu.NONE, model.operationsMenu)
    }
}
