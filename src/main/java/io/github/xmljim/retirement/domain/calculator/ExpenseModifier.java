package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Modifies expense amounts based on various factors.
 *
 * <p>Expense modifiers implement time-based and age-based changes to expenses
 * throughout retirement. Common use cases include:
 * <ul>
 *   <li>Mortgage payoff - expense drops to zero after payoff date</li>
 *   <li>Spending phases - discretionary spending decreases with age</li>
 *   <li>Healthcare aging - medical costs increase with age</li>
 * </ul>
 *
 * <p>Modifiers can be chained using {@link #andThen(ExpenseModifier)} to
 * combine multiple modification rules.
 *
 * <p>Example usage:
 * <pre>{@code
 * ExpenseModifier modifier = PayoffModifier.onDate(LocalDate.of(2035, 1, 1));
 * BigDecimal adjusted = modifier.modify(mortgagePayment, currentDate, age);
 * }</pre>
 *
 * @see PayoffModifier
 * @see SpendingCurveModifier
 * @see AgeBasedModifier
 */
@FunctionalInterface
public interface ExpenseModifier {

    /**
     * Modifies an expense amount based on date and age.
     *
     * @param baseAmount the original expense amount
     * @param date the date for which to calculate the modified amount
     * @param age the person's age at the given date
     * @return the modified expense amount
     */
    BigDecimal modify(BigDecimal baseAmount, LocalDate date, int age);

    /**
     * Returns an identity modifier that returns amounts unchanged.
     *
     * @return a modifier that applies no changes
     */
    static ExpenseModifier identity() {
        return (amount, date, age) -> amount;
    }

    /**
     * Chains this modifier with another, applying both in sequence.
     *
     * <p>The output of this modifier becomes the input to the next.
     *
     * @param after the modifier to apply after this one
     * @return a combined modifier
     */
    default ExpenseModifier andThen(ExpenseModifier after) {
        return (amount, date, age) -> after.modify(this.modify(amount, date, age), date, age);
    }

    /**
     * Combines multiple modifiers into a single modifier.
     *
     * <p>Modifiers are applied in the order provided.
     *
     * @param modifiers the modifiers to combine
     * @return a combined modifier, or identity if none provided
     */
    static ExpenseModifier combine(ExpenseModifier... modifiers) {
        if (modifiers == null || modifiers.length == 0) {
            return identity();
        }

        ExpenseModifier combined = modifiers[0];
        for (int i = 1; i < modifiers.length; i++) {
            combined = combined.andThen(modifiers[i]);
        }
        return combined;
    }

    /**
     * Returns a modifier that multiplies the amount by a fixed factor.
     *
     * @param factor the multiplication factor
     * @return a modifier that applies the factor
     */
    static ExpenseModifier multiplier(BigDecimal factor) {
        return (amount, date, age) -> amount.multiply(factor);
    }

    /**
     * Returns a modifier that sets the amount to zero.
     *
     * @return a modifier that zeros the amount
     */
    static ExpenseModifier zero() {
        return (amount, date, age) -> BigDecimal.ZERO;
    }
}
