package io.github.xmljim.retirement.domain.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ExpenseFrequency Tests")
class ExpenseFrequencyTest {

    @Test
    @DisplayName("MONTHLY toMonthly should return same amount")
    void monthlyToMonthly() {
        assertEquals(0, new BigDecimal("100").compareTo(ExpenseFrequency.MONTHLY.toMonthly(new BigDecimal("100"))));
    }

    @Test
    @DisplayName("QUARTERLY toMonthly should divide by 3")
    void quarterlyToMonthly() {
        assertEquals(new BigDecimal("33.33"), ExpenseFrequency.QUARTERLY.toMonthly(new BigDecimal("100")));
    }

    @Test
    @DisplayName("ANNUAL toMonthly should divide by 12")
    void annualToMonthly() {
        assertEquals(new BigDecimal("100.00"), ExpenseFrequency.ANNUAL.toMonthly(new BigDecimal("1200")));
    }

    @Test
    @DisplayName("MONTHLY toAnnual should multiply by 12")
    void monthlyToAnnual() {
        assertEquals(new BigDecimal("1200"), ExpenseFrequency.MONTHLY.toAnnual(new BigDecimal("100")));
    }

    @Test
    @DisplayName("ANNUAL toAnnual should return same amount")
    void annualToAnnual() {
        assertEquals(new BigDecimal("1200"), ExpenseFrequency.ANNUAL.toAnnual(new BigDecimal("1200")));
    }

    @Test
    @DisplayName("Null amount should return zero")
    void nullAmountReturnsZero() {
        assertEquals(BigDecimal.ZERO, ExpenseFrequency.MONTHLY.toMonthly(null));
        assertEquals(BigDecimal.ZERO, ExpenseFrequency.MONTHLY.toAnnual(null));
    }
}
