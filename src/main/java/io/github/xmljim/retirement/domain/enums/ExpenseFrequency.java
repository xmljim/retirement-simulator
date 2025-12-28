package io.github.xmljim.retirement.domain.enums;

import java.math.BigDecimal;

/**
 * Frequency at which an expense occurs.
 *
 * <p>Used to convert between monthly and annual amounts for
 * expense tracking and projection.
 */
public enum ExpenseFrequency {

    /**
     * Expense occurs monthly (12 times per year).
     */
    MONTHLY("Monthly", 12),

    /**
     * Expense occurs quarterly (4 times per year).
     */
    QUARTERLY("Quarterly", 4),

    /**
     * Expense occurs semi-annually (2 times per year).
     */
    SEMI_ANNUAL("Semi-Annual", 2),

    /**
     * Expense occurs annually (once per year).
     */
    ANNUAL("Annual", 1);

    private final String displayName;
    private final int periodsPerYear;

    ExpenseFrequency(String displayName, int periodsPerYear) {
        this.displayName = displayName;
        this.periodsPerYear = periodsPerYear;
    }

    /**
     * Returns the human-readable display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the number of occurrences per year.
     *
     * @return periods per year (e.g., 12 for MONTHLY)
     */
    public int getPeriodsPerYear() {
        return periodsPerYear;
    }

    /**
     * Converts an amount at this frequency to a monthly amount.
     *
     * <p>For example:
     * <ul>
     *   <li>MONTHLY: amount stays the same</li>
     *   <li>QUARTERLY: amount / 3</li>
     *   <li>ANNUAL: amount / 12</li>
     * </ul>
     *
     * @param amount the amount at this frequency
     * @return the equivalent monthly amount
     */
    public BigDecimal toMonthly(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (this == MONTHLY) {
            return amount;
        }
        // Monthly amount = (amount * periodsPerYear) / 12
        return amount.multiply(BigDecimal.valueOf(periodsPerYear))
                .divide(BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Converts an amount at this frequency to an annual amount.
     *
     * @param amount the amount at this frequency
     * @return the equivalent annual amount
     */
    public BigDecimal toAnnual(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(BigDecimal.valueOf(periodsPerYear));
    }
}
