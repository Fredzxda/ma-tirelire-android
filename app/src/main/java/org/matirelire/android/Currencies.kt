package org.matirelire.android

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency
import java.util.Locale

internal object Fx {
    val currencies: List<String> = Currency.getAvailableCurrencies().filter { it.defaultFractionDigits in 0..3 }.map { it.currencyCode }.sorted()
    fun valid(code: String) = code in currencies
    fun digits(code: String): Int = Currency.getInstance(code).defaultFractionDigits.also { require(it in 0..3) }
    fun rate(text: String, locale: Locale): BigDecimal? {
        val separator = java.text.DecimalFormatSymbols.getInstance(locale).decimalSeparator
        val value = text.trim().replace(separator, '.')
        if (!Regex("[0-9]{1,19}([.][0-9]{1,18})?").matches(value)) return null
        return value.toBigDecimalOrNull()?.takeIf { it.signum() > 0 && it <= BigDecimal("1000000000000000000") }
    }
    fun convert(amount: BigDecimal, rate: BigDecimal, code: String): BigDecimal = amount.multiply(rate).setScale(digits(code), RoundingMode.HALF_EVEN)
    fun quotient(to: BigDecimal, from: BigDecimal): BigDecimal = to.divide(from, 18, RoundingMode.HALF_EVEN)
    fun validAmount(amount: BigDecimal, code: String): Boolean = amount.abs() < BigDecimal("1000000000000") &&
        runCatching { amount.setScale(digits(code), RoundingMode.UNNECESSARY) }.isSuccess
}

internal fun DemoViewModel.currencyCode(index: Int): String = accountCurrencies.getOrNull(index) ?: "EUR"
internal fun DemoViewModel.currencyDigits(index: Int): Int = Fx.digits(currencyCode(index))
internal fun DemoViewModel.suggestedRate(from: String, to: String): BigDecimal? {
    if (from == to) return BigDecimal.ONE
    val a = if (from == "EUR") BigDecimal.ONE else localRates[from]?.toBigDecimalOrNull() ?: return null
    val b = if (to == "EUR") BigDecimal.ONE else localRates[to]?.toBigDecimalOrNull() ?: return null
    return Fx.quotient(a, b)
}
internal fun DemoViewModel.setLocalRate(code: String, text: String, locale: Locale): Boolean {
    if (!Fx.valid(code) || code == "EUR") return false
    val value = Fx.rate(text, locale) ?: return false
    localRates[code] = value.toPlainString(); onChange?.invoke(); return true
}
internal fun DemoViewModel.entryRate(draft: EntryDraft, from: String, to: String, locale: Locale): BigDecimal? =
    if (from == to) BigDecimal.ONE else if (draft.fxRate.isNotBlank()) Fx.rate(draft.fxRate, locale) else suggestedRate(from, to)

internal fun DemoViewModel.nativeAmount(draft: EntryDraft, locale: Locale): BigDecimal? {
    val native = currencyCode(draft.account)
    val source = if (draft.transferTarget != null) native else draft.inputCurrency.ifBlank { native }
    if (!Fx.valid(source)) return null
    val original = org.matirelire.android.domain.Money.parseAmount(draft.amount, locale, Fx.digits(source)) ?: return null
    val rate = if (source == native) BigDecimal.ONE else entryRate(draft, source, native, locale) ?: return null
    return Fx.convert(original, rate, native).takeIf { it.signum() > 0 && Fx.validAmount(it, native) }
}
internal fun DemoViewModel.targetAmount(draft: EntryDraft, locale: Locale): BigDecimal? {
    val target = draft.transferTarget ?: return null
    val amount = nativeAmount(draft, locale) ?: return null
    val rate = entryRate(draft, currencyCode(draft.account), currencyCode(target), locale) ?: return null
    return Fx.convert(amount, rate, currencyCode(target)).takeIf { it.signum() > 0 && Fx.validAmount(it, currencyCode(target)) }
}
internal fun DemoViewModel.consolidated(cleared: Boolean = false): BigDecimal? {
    var total = BigDecimal.ZERO
    openings.indices.forEach { index ->
        val rate = suggestedRate(currencyCode(index), "EUR") ?: return null
        total += Fx.convert(balance(index, cleared), rate, "EUR")
    }
    return total.setScale(2)
}

internal fun DemoViewModel.detachTransfer(id: Long): Boolean {
    val row = transactions.firstOrNull { it.id == id } ?: return false
    val peer = row.peerId ?: return false
    val ids = setOf(id, peer)
    val next = transactions.map { if (it.id in ids) it.copy(peerId = null, exchangeAmount = null, seriesId = null, occurrence = null) else it }
    SnapshotCodec.encode(exportData().copy(transactions = next, draft = null)).fill(0)
    transactions.clear(); transactions.addAll(next)
    cancelEntry() // one callback, including the cleared editor
    return true
}
