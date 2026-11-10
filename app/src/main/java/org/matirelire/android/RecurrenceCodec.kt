package org.matirelire.android

import org.json.JSONObject
import java.util.UUID

internal object RecurrenceCodec {
    fun encode(r: RecurringRule): JSONObject = JSONObject().put("id", r.id).put("anchor", r.anchor)
        .put("frequency", r.frequency.name).put("account", r.account).put("target", r.target ?: JSONObject.NULL)
        .put("amount", r.amount.toPlainString()).put("description", r.description).put("category", r.category)
        .put("payment", r.payment).put("next", r.next).put("until", r.until ?: JSONObject.NULL)
        .put("count", r.count ?: JSONObject.NULL).put("active", r.active)

    private fun validatedAmount(value: String): java.math.BigDecimal {
        require(Regex("-?[0-9]{1,12}([.][0-9]{1,2})?").matches(value))
        return value.toBigDecimal()
    }

    fun decode(j: JSONObject) = RecurringRule(j.getString("id"), j.getString("anchor"),
        RepeatFrequency.valueOf(j.getString("frequency")), j.getInt("account"),
        if (j.isNull("target")) null else j.getInt("target"), validatedAmount(j.getString("amount")),
        j.getString("description"), j.getString("category"), j.getString("payment"), j.getInt("next"),
        if (j.isNull("until")) null else j.getString("until"),
        if (j.isNull("count")) null else j.getInt("count"), j.getBoolean("active"))

    fun validate(data: LedgerData, schema: Int) {
        require(data.rules.size <= 500 && data.rules.map { it.id }.distinct().size == data.rules.size)
        val byId = data.rules.associateBy { it.id }
        data.rules.forEach { r ->
            require(schema == 5 && r.id.length == 36 && UUID.fromString(r.id).toString() == r.id)
            val anchor = requireNotNull(OperationDates.parse(r.anchor))
            require(r.frequency != RepeatFrequency.NONE && r.next in 1..1_000_000)
            require(r.account in data.openings.indices && (r.target == null || (r.target in data.openings.indices && r.target != r.account)))
            require(r.description.isNotBlank() && r.description.length <= 100)
            require(r.category in data.categories && r.payment in data.payments)
            require(Regex("-?[0-9]{1,12}([.][0-9]{1,2})?").matches(r.amount.toPlainString()) && r.amount.signum() != 0)
            if (r.target != null && data.transferPayment != null) require(r.payment == data.transferPayment)
            require(r.count == null || r.count in 1..10_000)
            require(r.until == null || requireNotNull(OperationDates.parse(r.until)) >= anchor)
            require(r.until == null || r.count == null)
        }
        data.transactions.forEach { row ->
            if (row.seriesId == null) require(row.occurrence == null)
            else {
                require(schema == 5)
                val rule = requireNotNull(byId[row.seriesId])
                require(row.occurrence != null && row.occurrence in 0 until rule.next)
            }
        }
        data.transactions.filter { it.seriesId != null }.groupBy { it.seriesId to it.occurrence }.values.forEach { rows ->
            require(rows.size in 1..2)
            if (rows.size == 2) require(rows[0].peerId == rows[1].id && rows[1].peerId == rows[0].id)
        }
        data.draft?.let { d ->
            if (d.repeatFrequency != RepeatFrequency.NONE) require(schema == 5)
        }
    }
}
