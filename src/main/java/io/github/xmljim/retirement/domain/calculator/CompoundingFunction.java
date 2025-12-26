package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;

/**
 * Functional interface for compounding interest calculations.
 *
 * <p>Different financial instruments use different compounding formulas:
 * <ul>
 *   <li><b>Annual</b>: Investments typically use true annual compounding</li>
 *   <li><b>Monthly</b>: Savings accounts often compound monthly</li>
 *   <li><b>Daily</b>: Credit cards and some loans compound daily</li>
 *   <li><b>Continuous</b>: Theoretical maximum compounding frequency</li>
 * </ul>
 *
 * <p>This interface follows the Strategy Pattern, allowing different
 * compounding strategies to be plugged into calculators.
 *
 * <p>Standard implementations are available in {@link CompoundingFunctions}.
 *
 * @see CompoundingFunctions
 */
@FunctionalInterface
public interface CompoundingFunction {

    /**
     * Calculates the ending balance after applying compound interest.
     *
     * <p>The interpretation of {@code periods} depends on the implementation:
     * <ul>
     *   <li>ANNUAL compounding: periods are months (compounds annually)</li>
     *   <li>MONTHLY compounding: periods are months</li>
     *   <li>DAILY compounding: periods are days</li>
     *   <li>CONTINUOUS compounding: periods are months</li>
     * </ul>
     *
     * @param principal the starting balance (must be non-negative)
     * @param annualRate the annual interest rate as decimal (0.10 for 10%)
     * @param periods the number of periods (interpretation depends on implementation)
     * @return the ending balance after compounding
     */
    BigDecimal compound(BigDecimal principal, BigDecimal annualRate, int periods);

    /**
     * Returns the name of this compounding function.
     *
     * @return a descriptive name
     */
    default String getName() {
        return "Custom";
    }

    /**
     * Returns the formula description for documentation purposes.
     *
     * @return the mathematical formula
     */
    default String getFormula() {
        return "Custom compounding formula";
    }
}
