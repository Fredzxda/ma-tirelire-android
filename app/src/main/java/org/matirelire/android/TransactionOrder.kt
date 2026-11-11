package org.matirelire.android

internal enum class TransactionOrder {
    ORIGINAL, NEWEST, OLDEST;

    /** Stable ties; unparseable historical dates always remain at the end. */
    fun arrange(rows: List<DemoTransaction>): List<DemoTransaction> {
        if (this == ORIGINAL) return rows
        return rows.sortedWith { a, b ->
            val da = OperationDates.parse(a.date)
            val db = OperationDates.parse(b.date)
            when {
                da == null && db == null -> 0
                da == null -> 1
                db == null -> -1
                this == NEWEST -> db.compareTo(da)
                else -> da.compareTo(db)
            }
        }
    }
}
