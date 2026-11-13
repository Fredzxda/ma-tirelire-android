package org.matirelire.android

import org.json.JSONObject
import java.util.UUID

internal object RecurrenceCodec {
    fun encode(r: RecurringRule): JSONObject = JSONObject().put("id", r.id).put("anchor", r.anchor)
        .put("frequency", r.frequency.name).put("account", r.account).put("target", r.target ?: JSONObject.NULL)
        .put("targetAmount", r.targetAmount?.toPlainString() ?: JSONObject.NULL).put("originalCurrency", r.originalCurrency ?: JSONObject.NULL)
        .put("originalAmount", r.originalAmount?.toPlainString() ?: JSONObject.NULL).put("fxRate", r.fxRate?.toPlainString() ?: JSONObject.NULL)
        .put("amount", r.amount.toPlainString()).put("description", r.description).put("category", r.category)
        .put("payment", r.payment).put("next", r.next).put("until", r.until ?: JSONObject.NULL)
        .put("count", r.count ?: JSONObject.NULL).put("active", r.active).put("confirmedThrough", r.confirmedThrough).put("scheduleBase", r.scheduleBase)
        .put("scheduleOffset", r.scheduleOffset).put("generatedThrough", r.generatedThrough ?: JSONObject.NULL)

    private fun validatedAmount(value: String): java.math.BigDecimal {
        require(Regex("-?[0-9]{1,12}([.][0-9]{1,3})?").matches(value))
        return value.toBigDecimal()
    }

    fun decode(j: JSONObject) = RecurringRule(j.getString("id"), j.getString("anchor"),
        RepeatFrequency.valueOf(j.getString("frequency")), j.getInt("account"),
        if (j.isNull("target")) null else j.getInt("target"), validatedAmount(j.getString("amount")),
        j.getString("description"), j.getString("category"), j.getString("payment"), j.getInt("next"),
        if (j.isNull("until")) null else j.getString("until"),
        if (j.isNull("count")) null else j.getInt("count"), j.getBoolean("active"), j.optInt("confirmedThrough", 0), j.optInt("scheduleBase", 0), j.optInt("scheduleOffset", 0),
        if (j.isNull("generatedThrough")) null else j.getString("generatedThrough"),
        targetAmount = if (j.isNull("targetAmount")) null else validatedAmount(j.getString("targetAmount")),
        originalCurrency = if (j.isNull("originalCurrency")) null else j.getString("originalCurrency"),
        originalAmount = if (j.isNull("originalAmount")) null else validatedAmount(j.getString("originalAmount")),
        fxRate = if (j.isNull("fxRate")) null else requireNotNull(Fx.rate(j.getString("fxRate"), java.util.Locale.US)))

    fun validate(data: LedgerData, schema: Int) {
        require(data.rules.size <= 500 && data.rules.map { it.id }.distinct().size == data.rules.size)
        val byId = data.rules.associateBy { it.id }
        data.rules.forEach { r ->
            require(schema >= 5 && r.id.length == 36 && UUID.fromString(r.id).toString() == r.id)
            val anchor = requireNotNull(OperationDates.parse(r.anchor))
            require(r.confirmedThrough in 0 until r.next)
            require(r.frequency != RepeatFrequency.NONE && r.next in 1..1_000_000)
            require(r.account in data.openings.indices && (r.target == null || (r.target in data.openings.indices && r.target != r.account)))
            fun code(index: Int) = data.accountCurrencies.getOrNull(index) ?: "EUR"
            require(Fx.validAmount(r.amount, code(r.account)))
            if (r.target != null && code(r.target) != code(r.account)) {
                require(schema >= 13 && r.targetAmount != null && r.targetAmount.signum() == -r.amount.signum() && Fx.validAmount(r.targetAmount, code(r.target)))
            } else require(r.targetAmount == null)
            if (r.originalCurrency == null) require(r.originalAmount == null && r.fxRate == null)
            else require(schema >= 13 && r.target == null && r.originalCurrency != code(r.account) && Fx.valid(r.originalCurrency) && r.originalAmount != null && r.fxRate != null &&
                Fx.validAmount(r.originalAmount, r.originalCurrency) && r.originalAmount.signum() == r.amount.signum() &&
                Fx.convert(r.originalAmount, r.fxRate, code(r.account)) == r.amount)
            require(r.description.isNotBlank() && r.description.length <= 100)
            require(r.category in data.categories && r.payment in data.payments)
            require(Regex("-?[0-9]{1,12}([.][0-9]{1,3})?").matches(r.amount.toPlainString()) && r.amount.signum() != 0)
            if (r.target != null && data.transferPayment != null) require(r.payment == data.transferPayment)
            require(r.count == null || r.count in 1..1_010_000)
            require(r.scheduleBase in 0..r.next && r.scheduleOffset in 0..1_000_000)
            require(r.generatedThrough == null || OperationDates.parse(r.generatedThrough) != null)
            require(r.until == null || requireNotNull(OperationDates.parse(r.until)) >= anchor)
            require(r.until == null || r.count == null)
        }
        data.transactions.forEach { row ->
            if (row.seriesId == null) require(row.occurrence == null)
            else {
                require(schema >= 5)
                val rule = requireNotNull(byId[row.seriesId])
                require(row.occurrence != null && row.occurrence in 0 until rule.next)
                if (row.amountConfirmed) require(row.occurrence <= rule.confirmedThrough)
            }
        }
        data.transactions.filter { it.seriesId != null }.groupBy { it.seriesId to it.occurrence }.values.forEach { rows ->
            require(rows.size in 1..2)
            if (rows.size == 2) require(rows[0].peerId == rows[1].id && rows[1].peerId == rows[0].id)
        }
        data.draft?.let { d ->
            if (d.repeatFrequency != RepeatFrequency.NONE) require(schema >= 5)
        }
    }
}
