package org.matirelire.android.domain

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class MoneyTest {
    @Test fun frenchAndEnglishAmountsAreExact() {
        assertEquals(BigDecimal("12.34"), Money.parseAmount("12,34", Locale.FRANCE))
        assertEquals(BigDecimal("12.34"), Money.parseAmount("12.34", Locale.US))
        assertEquals(BigDecimal("12.30"), Money.parseAmount("12,3", Locale.FRANCE))
        val total = BigDecimal("1000.00")
            .subtract(Money.parseAmount("12,34", Locale.FRANCE)!!)
            .add(Money.parseAmount("100,00", Locale.FRANCE)!!)
        assertEquals(BigDecimal("1087.66"), total)
    }
    @Test fun ambiguousAndUnsafeInputIsRejected() {
        for (input in listOf("", "0", "-1", "+1", "1e3", "1,234", "1.23", "1 000", "1000000000000", "NaN")) {
            assertNull(input, Money.parseAmount(input, Locale.FRANCE))
        }
        assertNull(Money.parseAmount("1,23", Locale.US))
    }
}
