package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.github.xmljim.retirement.domain.calculator.ExpenseModifier;

/**
 * Modifier that drops an expense to zero after a payoff date.
 *
 * <p>Used for expenses that end at a specific date, such as:
 * <ul>
 *   <li>Mortgage payments after payoff</li>
 *   <li>Car loans after final payment</li>
 *   <li>Student loans after payoff</li>
 *   <li>Any fixed-term debt or expense</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Mortgage paid off January 2035
 * ExpenseModifier mortgage = PayoffModifier.onDate(LocalDate.of(2035, 1, 1));
 *
 * // Before payoff: returns full amount
 * mortgage.modify(new BigDecimal("2000"), LocalDate.of(2034, 12, 1), 64);  // 2000
 *
 * // After payoff: returns zero
 * mortgage.modify(new BigDecimal("2000"), LocalDate.of(2035, 2, 1), 65);   // 0
 * }</pre>
 */
public final class PayoffModifier implements ExpenseModifier {

    private final LocalDate payoffDate;
    private final String description;

    private PayoffModifier(LocalDate payoffDate, String description) {
        this.payoffDate = payoffDate;
        this.description = description;
    }

    /**
     * Creates a modifier that drops expense to zero on or after the given date.
     *
     * @param payoffDate the date when the expense ends
     * @return a new PayoffModifier
     * @throws IllegalArgumentException if payoffDate is null
     */
    public static PayoffModifier onDate(LocalDate payoffDate) {
        if (payoffDate == null) {
            throw new IllegalArgumentException("Payoff date cannot be null");
        }
        return new PayoffModifier(payoffDate, "Payoff on " + payoffDate);
    }

    /**
     * Creates a modifier with a custom description.
     *
     * @param payoffDate the date when the expense ends
     * @param description a description of the payoff (e.g., "Mortgage payoff")
     * @return a new PayoffModifier
     */
    public static PayoffModifier onDate(LocalDate payoffDate, String description) {
        if (payoffDate == null) {
            throw new IllegalArgumentException("Payoff date cannot be null");
        }
        return new PayoffModifier(payoffDate, description);
    }

    @Override
    public BigDecimal modify(BigDecimal baseAmount, LocalDate date, int age) {
        if (date.isBefore(payoffDate)) {
            return baseAmount;
        }
        return BigDecimal.ZERO;
    }

    /**
     * Returns the payoff date.
     *
     * @return the date when the expense ends
     */
    public LocalDate getPayoffDate() {
        return payoffDate;
    }

    /**
     * Returns the description of this payoff.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Indicates whether the expense is paid off at the given date.
     *
     * @param date the date to check
     * @return true if the expense is paid off (date is on or after payoff date)
     */
    public boolean isPaidOff(LocalDate date) {
        return !date.isBefore(payoffDate);
    }

    @Override
    public String toString() {
        return "PayoffModifier[" + description + "]";
    }
}
