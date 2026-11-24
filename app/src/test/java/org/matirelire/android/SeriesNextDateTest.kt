package org.matirelire.android

import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale
import org.junit.Test
import org.junit.Assert.*

class SeriesNextDateTest {
    private val today=LocalDate.of(2026,11,24)
    private fun model(transfer:Boolean=false, frequency:RepeatFrequency=RepeatFrequency.MONTH, anchor:LocalDate=LocalDate.of(2026,11,5))=DemoViewModel().apply {
        initialize(listOf(DemoTransaction(1,0,"Fixture",BigDecimal("-1.00"),"Food","Card","2026-11-01")))
        ensureTransferPayment("Transfer")
        beginEntryWithReferences(anchor)
        updateDraft(draft!!.copy(amount="10",description="Recurring",repeatFrequency=frequency,
            payment=if(transfer) "Transfer" else "Card",transferTarget=if(transfer) 1 else null))
        assertTrue(saveEntry(Locale.US,"unused"))
    }
    @Test fun unchangedFebruaryDateKeepsThirtyFirstAnchor() {
        val m=model(anchor=LocalDate.of(2027,1,31));val before=m.exportData();val rule=m.rules.single()
        assertTrue(m.setSeriesNextDate(rule.id,LocalDate.of(2027,2,28),LocalDate.of(2027,2,1)))
        assertEquals(before,m.exportData())
        assertEquals(LocalDate.of(2027,3,31),m.rules.single().dateAt(rule.next+1))
    }
    @Test fun reschedulePreservesHistoryCountsAndMovesFollowingMonthlyDates() {
        val m=model();val before=m.exportData();val r=m.rules.single();var saves=0
        m.onChange={saves++; SnapshotCodec.encode(m.exportData()).fill(0)}
        assertTrue(m.setSeriesNextDate(r.id,LocalDate.of(2026,12,6),today))
        assertEquals(before.transactions,m.transactions.toList());assertEquals(r.next,m.rules.single().next)
        assertEquals(1,saves)
        m.generateDue(LocalDate.of(2026,12,6))
        assertEquals("2026-12-06",m.transactions.last().date)
        assertEquals(LocalDate.of(2027,1,6),m.rules.single().nextDate())
        SnapshotCodec.decode(SnapshotCodec.encode(m.exportData()))
    }
    @Test fun deferredForecastsAndInvalidDatesArePreservedAtomically() {
        val m=model();m.setAnticipation(Anticipation.DAYS_30);m.generateDue(today)
        m.setAnticipation(Anticipation.DUE)
        val before=m.exportData();assertTrue(before.deferredForecasts.isNotEmpty())
        assertFalse(m.setSeriesNextDate(m.rules.single().id,LocalDate.of(2026,12,5),today))
        assertEquals(before,m.exportData())
        assertTrue(m.setSeriesNextDate(m.rules.single().id,LocalDate.of(2026,12,8),today))
        assertEquals(before.deferredForecasts,m.deferredForecasts.toList())
    }
    @Test fun pausedSeriesAndLimitsAreRespected() {
        val m=model();val id=m.rules.single().id
        m.setSeriesActive(id,false)
        assertTrue(m.setSeriesNextDate(id,LocalDate.of(2026,12,6),today))
        assertFalse(m.rules.single().active)
        m.rules[0]=m.rules[0].copy(until="2026-12-10")
        assertFalse(m.setSeriesNextDate(id,LocalDate.of(2026,12,11),today))
        m.rules[0]=m.rules[0].copy(until=null,count=m.rules[0].next)
        assertNull(m.nextSeriesMinimumDate(id,today))
    }
    @Test fun weeklyTransfersStayPairedAndArchivedSeriesCannotChange() {
        val m=model(true,RepeatFrequency.WEEK);val id=m.rules.single().id
        assertTrue(m.setSeriesNextDate(id,LocalDate.of(2026,11,25),today))
        m.generateDue(LocalDate.of(2026,11,25))
        val pair=m.transactions.filter { it.date=="2026-11-25" }
        assertEquals(2,pair.size);assertEquals(pair[0].id,pair[1].peerId)
        assertEquals(LocalDate.of(2026,12,2),m.rules.single().nextDate())
        m.archivedAccounts.add(m.rules[0].account)
        assertFalse(m.setSeriesNextDate(id,LocalDate.of(2026,12,3),today))
    }
    @Test fun changingAmountLaterDoesNotUndoExplicitNextDate() {
        val m=model();val id=m.rules.single().id
        assertTrue(m.setSeriesNextDate(id,LocalDate.of(2026,12,6),today))
        val row=m.transactions.first { it.seriesId==id }
        m.editEntry(row.id,Locale.US);m.updateDraft(m.draft!!.copy(amount="12"))
        assertTrue(m.saveEntry(Locale.US,"unused",SeriesEditScope.SERIES,today))
        assertEquals(LocalDate.of(2026,12,6),m.rules.single().nextDate())
        assertEquals(BigDecimal("-12.00"),m.rules.single().amount)
    }
}
