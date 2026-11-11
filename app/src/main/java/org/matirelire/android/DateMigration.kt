package org.matirelire.android

/** Versioned normalization. Never guesses numeric dates or mutates the input. */
internal object DateMigration {
    data class Plan(val data: LedgerData, val converted: Int, val unresolved: Int)
    fun plan(before: LedgerData): Plan {
        var converted = 0
        var unresolved = 0
        val rows = before.transactions.map { row ->
            if (OperationDates.parse(row.date) != null) row
            else {
                val date = OperationDates.forOrdering(row.date)
                if (date == null) { unresolved++; row }
                else { converted++; row.copy(date = date.toString()) }
            }
        }
        val data = before.copy(transactions = rows, dateMigrationVersion = 1)
        // Checks references, transfer pairs, recurrence cursors, bounds and draft.
        SnapshotCodec.encode(data).fill(0)
        return Plan(data, converted, unresolved)
    }

    suspend fun execute(before: LedgerData, backup: suspend () -> Unit,
        current: () -> LedgerData, replace: (LedgerData) -> Unit): Plan {
        val plan = plan(before)
        if (plan.data == before) return plan
        backup() // must succeed before any replacement
        check(current() == before) // never overwrite edits made while backing up
        replace(plan.data)
        return plan
    }
}
