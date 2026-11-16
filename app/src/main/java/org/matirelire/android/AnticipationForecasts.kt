package org.matirelire.android

import java.time.LocalDate

/** Keep consumed cursors intact: deferred rows are exact encrypted records, not new occurrences. */
internal object AnticipationForecasts {
    fun adjust(data: LedgerData, today: LocalDate): LedgerData {
        val horizon = data.anticipation.horizon(today)
        val editing = data.draft?.editingId
        val groups = data.transactions.filter { it.seriesId != null }.groupBy { it.seriesId to it.occurrence }
        val removed = groups.values.filter { group -> group.all { row ->
            !row.amountConfirmed && !row.readyToConfirm && !row.cleared && row.account !in data.archivedAccounts &&
                (editing == null || (row.id != editing && row.peerId != editing)) &&
                OperationDates.parse(row.date)?.let { it > today && it > horizon } == true
        } }.flatten()
        val ids = removed.map { it.id }.toSet()
        return restore(data.copy(transactions = data.transactions.filterNot { it.id in ids },
            deferredForecasts = data.deferredForecasts + removed), today)
    }

    fun restore(data: LedgerData, today: LocalDate): LedgerData {
        val horizon = data.anticipation.horizon(today)
        val ready = data.deferredForecasts.filter { requireNotNull(OperationDates.parse(it.date)) <= horizon }
        if (ready.isEmpty()) return data
        val ids = ready.map { it.id }.toSet()
        return data.copy(transactions = data.transactions + ready,
            deferredForecasts = data.deferredForecasts.filterNot { it.id in ids })
    }

    /** Account lifecycle acts on all already generated history, including hidden forecasts. */
    fun materialize(data: LedgerData): LedgerData = data.copy(
        transactions = data.transactions + data.deferredForecasts, deferredForecasts = emptyList())
}
