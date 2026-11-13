package org.matirelire.android

import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test
import org.matirelire.android.domain.Money

class CurrenciesTest {
    private val today = LocalDate.of(2026, 11, 12)
    private fun model() = DemoViewModel().apply {
        initialize(listOf(DemoTransaction(1, 0, "Fixture", BigDecimal("-10.00"), "Food", "Card", today.toString(), true)))
        ensureAccountNames(listOf("Current", "Savings"))
        ensureTransferPayment("Transfer")
    }
    private fun addAccount(model: DemoViewModel, currency: String, opening: String = "0"): Int {
        model.beginAccount(null, Locale.US)
        model.updateAccountDraft(model.accountDraft!!.copy(name = "Account $currency", opening = opening, currency = currency))
        assertTrue(model.saveAccount(Locale.US))
        return model.openings.lastIndex
    }
    private fun beginTransfer(model: DemoViewModel, source: Int, target: Int, amount: String = "10.00", periodic: Boolean = false) {
        model.account.intValue = source
        model.beginEntry(today)
        model.updateDraft(model.draft!!.copy(description = "Fixture transfer", payment = "Transfer", transferTarget = target,
            amount = amount, repeatFrequency = if (periodic) RepeatFrequency.MONTH else RepeatFrequency.NONE,
            repeatCount = if (periodic) "3" else ""))
    }

    @Test fun formattingOnlyOfForeignQuoteDoesNotPromptForSeriesScope() {
        val m = model(); m.setLocalRate("USD", "0.9", Locale.US)
        m.beginEntry(today)
        m.updateDraft(m.draft!!.copy(description = "Foreign series", inputCurrency = "USD", amount = "10",
            repeatFrequency = RepeatFrequency.MONTH, repeatCount = "3"))
        assertTrue(m.saveEntry(Locale.US, "unused", today = today))
        m.editEntry(m.transactions.last().id, Locale.US)
        m.updateDraft(m.draft!!.copy(fxRate = "0.9000"))
        assertFalse(m.operationFieldsChanged(Locale.US))
        assertFalse(m.needsSeriesScope(Locale.US))
        m.updateDraft(m.draft!!.copy(fxRate = "0.8"))
        assertTrue(m.operationFieldsChanged(Locale.US))
        assertTrue(m.needsSeriesScope(Locale.US))
    }
    @Test fun extremeButValidReciprocalRateRemainsEditable() {
        val m = model(); val usd = addAccount(m, "USD")
        beginTransfer(m, usd, 0, amount = "999999999999")
        m.updateDraft(m.draft!!.copy(fxRate = "0.00000000000001"))
        assertTrue(m.saveEntry(Locale.US, "unused", today = today))
        val receiver = m.transactions.last { it.account == 0 }
        assertEquals(BigDecimal("0.01"), receiver.amount)
        m.editEntry(receiver.id, Locale.US)
        assertTrue(m.saveEntry(Locale.US, "unused", today = today))
        assertEquals(BigDecimal("-999999999999.00"), m.transactions.single { it.account == usd }.amount)
    }
    @Test fun malformedOriginalCurrencyInSeriesIsRejectedBeforeGeneration() {
        val m = model(); m.setLocalRate("USD", "0.9", Locale.US)
        m.beginEntry(today)
        m.updateDraft(m.draft!!.copy(description = "Original series", inputCurrency = "USD", amount = "10",
            repeatFrequency = RepeatFrequency.MONTH, repeatCount = "3"))
        assertTrue(m.saveEntry(Locale.US, "unused", today = today))
        val data = m.exportData(); val rule = m.rules.single()
        assertThrows(IllegalArgumentException::class.java) {
            SnapshotCodec.encode(data.copy(rules = listOf(rule.copy(originalCurrency = "EUR", originalAmount = rule.amount, fxRate = BigDecimal.ONE))))
        }
    }
    @Test fun currencyPrecisionIsStrictAndRoundingIsHalfEven() {
        assertEquals(2, Fx.digits("EUR")); assertEquals(0, Fx.digits("JPY")); assertEquals(3, Fx.digits("KWD"))
        assertNull(Money.parseAmount("1.001", Locale.US, Fx.digits("EUR")))
        assertNull(Money.parseAmount("1.0", Locale.US, Fx.digits("JPY")))
        assertEquals(BigDecimal("1.001"), Money.parseAmount("1,001", Locale.FRANCE, Fx.digits("KWD")))
        assertNull(Money.parseAmount("1.0001", Locale.US, Fx.digits("KWD")))
        assertEquals(BigDecimal("1.00"), Fx.convert(BigDecimal("1.005"), BigDecimal.ONE, "EUR"))
        assertEquals(BigDecimal("1.02"), Fx.convert(BigDecimal("1.015"), BigDecimal.ONE, "EUR"))
        assertEquals(BigDecimal("2"), Fx.convert(BigDecimal("2.5"), BigDecimal.ONE, "JPY"))
        assertEquals(BigDecimal("4"), Fx.convert(BigDecimal("3.5"), BigDecimal.ONE, "JPY"))
        assertEquals(BigDecimal("1.234"), Fx.convert(BigDecimal("1.2345"), BigDecimal.ONE, "KWD"))
        assertEquals(BigDecimal("1.236"), Fx.convert(BigDecimal("1.2355"), BigDecimal.ONE, "KWD"))
    }

    @Test fun localQuotesArePositiveStrictAndUseRegionalDecimalSeparator() {
        val m = model()
        assertTrue(m.setLocalRate("USD", "0,9", Locale.FRANCE))
        assertEquals(0, BigDecimal("0.9").compareTo(m.suggestedRate("USD", "EUR")))
        assertTrue(m.setLocalRate("JPY", "0.006", Locale.US))
        assertEquals(0, BigDecimal("150").compareTo(m.suggestedRate("USD", "JPY")))
        val before = m.localRates.toMap()
        for (text in listOf("", "0", "-1", "NaN", "1e2", "1,234", "1.1234567890123456789")) assertFalse(m.setLocalRate("USD", text, Locale.US))
        assertFalse(m.setLocalRate("EUR", "2", Locale.US))
        assertFalse(m.setLocalRate("ZZZ", "2", Locale.US))
        assertEquals(before, m.localRates.toMap())
    }

    @Test fun accountsKeepTheirOwnCurrencyAndAnExistingHistoryCannotBeReinterpreted() {
        val m = model(); val jpy = addAccount(m, "JPY", "120")
        assertEquals("JPY", m.currencyCode(jpy)); assertEquals(BigDecimal("120"), m.balance(jpy))
        assertEquals("EUR", m.currencyCode(0))
        m.beginAccount(0, Locale.US)
        m.updateAccountDraft(m.accountDraft!!.copy(currency = "USD"))
        assertFalse(m.saveAccount(Locale.US)); assertEquals("EUR", m.currencyCode(0))
        m.cancelAccount()
        m.beginAccount(null, Locale.US)
        m.updateAccountDraft(m.accountDraft!!.copy(name = "Bad JPY", currency = "JPY", opening = "1.1"))
        assertFalse(m.saveAccount(Locale.US)); assertEquals(3, m.accountNames.size)
    }

    @Test fun missingQuoteBlocksTheWholeTransferAndConsolidation() {
        val m = model(); val usd = addAccount(m, "USD")
        val rows = m.transactions.toList()
        beginTransfer(m, usd, 0)
        var writes = 0; m.onChange = { writes++ }
        assertNull(m.targetAmount(m.draft!!, Locale.US))
        assertFalse(m.saveEntry(Locale.US, "unused", today = today))
        assertEquals(rows, m.transactions.toList()); assertEquals(0, writes)
        assertNull(m.consolidated())
    }

    @Test fun crossCurrencyPairIsAtomicPortableAndReceiverCanBeEdited() {
        val m = model(); val usd = addAccount(m, "USD")
        m.setLocalRate("USD", "0.9", Locale.US)
        beginTransfer(m, usd, 0)
        var writes = 0
        m.onChange = { writes++; SnapshotCodec.decode(SnapshotCodec.encode(m.exportData())) }
        assertTrue(m.saveEntry(Locale.US, "unused", today = today))
        assertEquals(1, writes)
        val source = m.transactions.single { it.account == usd }
        val receiver = m.transactions.single { it.id == source.peerId }
        assertEquals(BigDecimal("-10.00"), source.amount); assertEquals(BigDecimal("9.00"), receiver.amount)
        assertEquals(receiver.amount, source.exchangeAmount); assertEquals(source.amount, receiver.exchangeAmount)
        m.onChange = null
        m.toggleCleared(receiver.id)
        m.editEntry(receiver.id, Locale.US)
        // The prefilled reciprocal quote has repeating decimals and must remain valid.
        assertTrue(m.saveEntry(Locale.US, "unused", today = today))
        m.editEntry(receiver.id, Locale.US)
        m.updateDraft(m.draft!!.copy(amount = "18.00"))
        assertTrue(m.saveEntry(Locale.US, "unused", today = today))
        assertEquals(BigDecimal("-20.00"), m.transactions.single { it.id == source.id }.amount)
        assertEquals(BigDecimal("18.00"), m.transactions.single { it.id == receiver.id }.amount)
        assertFalse(m.transactions.single { it.id == source.id }.cleared)
        assertTrue(m.transactions.single { it.id == receiver.id }.cleared)
        assertEquals(m.exportData(), SnapshotCodec.decode(SnapshotCodec.encode(m.exportData())))
    }

    @Test fun originalForeignAmountAndAppliedQuoteStayFixedWhenLocalRateChanges() {
        val m = model(); m.setLocalRate("USD", "0.9", Locale.US)
        m.beginEntry(today)
        m.updateDraft(m.draft!!.copy(description = "Foreign purchase", inputCurrency = "USD", amount = "10.00"))
        assertTrue(m.saveEntry(Locale.US, "unused", today = today))
        val row = m.transactions.last()
        assertEquals(BigDecimal("-9.00"), row.amount)
        assertEquals("USD", row.originalCurrency); assertEquals(BigDecimal("-10.00"), row.originalAmount)
        assertEquals(0, BigDecimal("0.9").compareTo(row.fxRate))
        m.setLocalRate("USD", "0.8", Locale.US)
        assertEquals(row, m.transactions.last())
        m.editEntry(row.id, Locale.US)
        assertTrue(m.saveEntry(Locale.US, "unused", today = today))
        val edited = m.transactions.last()
        assertEquals(row.amount, edited.amount); assertEquals(row.originalAmount, edited.originalAmount)
        assertEquals(row.originalCurrency, edited.originalCurrency)
        assertEquals(0, row.fxRate!!.compareTo(edited.fxRate))
    }

    @Test fun zeroAfterRoundingIsRejectedInsteadOfCreatingAZeroValuedTransfer() {
        val m = model(); val usd = addAccount(m, "USD"); val jpy = addAccount(m, "JPY")
        beginTransfer(m, usd, jpy, "0.01")
        m.updateDraft(m.draft!!.copy(fxRate = "0.1"))
        val before = m.transactions.toList()
        assertFalse(m.saveEntry(Locale.US, "unused", today = today)); assertEquals(before, m.transactions.toList())
    }

    @Test fun consolidationConvertsAccountsAndDoesNotAddDifferentCurrenciesDirectly() {
        val m = model(); val usd = addAccount(m, "USD", "100.00")
        assertNull(m.consolidated())
        m.setLocalRate("USD", "0.9", Locale.US)
        assertEquals(BigDecimal("3580.00"), m.consolidated())
        val baseline = m.consolidated()
        beginTransfer(m, usd, 0)
        assertTrue(m.saveEntry(Locale.US, "unused", today = today))
        assertEquals(baseline, m.consolidated())
        assertEquals(listOf(1L), m.reportingTransactions().map { it.id })
    }

    @Test fun recurringTransferKeepsTargetAmountsAndOnlyFutureTemplateFollowsReceiverEdit() {
        val m = model(); val usd = addAccount(m, "USD")
        m.setLocalRate("USD", "0.9", Locale.US)
        beginTransfer(m, usd, 0, periodic = true)
        assertTrue(m.saveEntry(Locale.US, "unused", today = today))
        assertEquals(BigDecimal("9.00"), m.rules.single().targetAmount)
        m.setLocalRate("USD", "0.8", Locale.US)
        assertTrue(m.generateDue(today.plusMonths(1)))
        val generated = m.transactions.filter { it.occurrence == 1 }
        assertEquals(BigDecimal("9.00"), generated.single { it.account == 0 }.amount)
        val receiver = m.transactions.single { it.account == 0 && it.occurrence == 0 }
        m.editEntry(receiver.id, Locale.US)
        m.updateDraft(m.draft!!.copy(amount = "18.00"))
        assertTrue(m.saveEntry(Locale.US, "unused", SeriesEditScope.SERIES, today.plusMonths(1)))
        assertEquals(BigDecimal("-20.00"), m.rules.single().amount)
        assertEquals(BigDecimal("18.00"), m.rules.single().targetAmount)
        assertEquals(generated, m.transactions.filter { it.occurrence == 1 })
        assertTrue(m.generateDue(today.plusMonths(2)))
        assertEquals(BigDecimal("18.00"), m.transactions.single { it.account == 0 && it.occurrence == 2 }.amount)
        SnapshotCodec.decode(SnapshotCodec.encode(m.exportData()))
    }

    @Test fun detachingKeepsBothWritesAndFlagsWithoutChangingTheFutureSeries() {
        val m = model(); val usd = addAccount(m, "USD")
        m.setLocalRate("USD", "0.9", Locale.US)
        beginTransfer(m, usd, 0, periodic = true)
        assertTrue(m.saveEntry(Locale.US, "unused", today = today))
        val source = m.transactions.single { it.account == usd }
        m.toggleCleared(source.id)
        val rows = m.transactions.toList(); val rules = m.rules.toList()
        m.editEntry(source.id, Locale.US)
        var writes = 0; m.onChange = { writes++; SnapshotCodec.decode(SnapshotCodec.encode(m.exportData())) }
        assertTrue(m.detachTransfer(source.id)); assertEquals(1, writes)
        assertEquals(rows.map { if (it.peerId != null) it.copy(peerId = null, exchangeAmount = null, seriesId = null, occurrence = null) else it }, m.transactions.toList())
        assertEquals(rules, m.rules.toList()); assertNull(m.draft)
        assertFalse(m.detachTransfer(source.id)); assertEquals(1, writes)
    }

    @Test fun legacyDefaultsStayEuroAndInvalidCurrencyMetadataIsRejected() {
        val m = model(); val legacy = SnapshotCodec.decode(SnapshotCodec.encode(m.exportData()))
        val restored = DemoViewModel(); restored.restoreData(legacy)
        assertEquals("EUR", restored.currencyCode(0)); assertEquals("EUR", restored.currencyCode(1))
        addAccount(m, "USD"); m.setLocalRate("USD", "0.9", Locale.US)
        val valid = m.exportData()
        assertEquals(valid, SnapshotCodec.decode(SnapshotCodec.encode(valid)))
        assertThrows(IllegalArgumentException::class.java) { SnapshotCodec.encode(valid.copy(accountCurrencies = listOf("EUR"))) }
        assertThrows(IllegalArgumentException::class.java) { SnapshotCodec.encode(valid.copy(accountCurrencies = listOf("EUR", "EUR", "ZZZ"))) }
        assertThrows(IllegalArgumentException::class.java) { SnapshotCodec.encode(valid.copy(localRates = mapOf("USD" to "-1"))) }
    }
}
