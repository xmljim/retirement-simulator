package io.github.xmljim.retirement.domain.calculator;

import io.github.xmljim.retirement.domain.calculator.impl.DefaultContributionCalculator;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultIncomeCalculator;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultInflationCalculator;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultReturnCalculator;

/**
 * Factory for obtaining calculator instances.
 *
 * <p>This factory provides singleton instances of the default calculator
 * implementations. All calculators are stateless, so sharing instances
 * is safe and efficient.
 *
 * <p>Usage:
 * <pre>{@code
 * InflationCalculator inflation = CalculatorFactory.inflationCalculator();
 * BigDecimal multiplier = inflation.calculateInflationMultiplier(
 *     new BigDecimal("0.03"), 10);
 * }</pre>
 *
 * <p>For testing or alternative implementations, you can instantiate
 * the calculator classes directly or create mock implementations.
 */
public final class CalculatorFactory {

    private static final InflationCalculator INFLATION_CALCULATOR =
        new DefaultInflationCalculator();

    private static final ContributionCalculator CONTRIBUTION_CALCULATOR =
        new DefaultContributionCalculator();

    private static final IncomeCalculator INCOME_CALCULATOR =
        new DefaultIncomeCalculator(INFLATION_CALCULATOR);

    private static final ReturnCalculator RETURN_CALCULATOR =
        new DefaultReturnCalculator();

    private CalculatorFactory() {
        // Prevent instantiation
    }

    /**
     * Returns the inflation calculator instance.
     *
     * @return the inflation calculator
     */
    public static InflationCalculator inflationCalculator() {
        return INFLATION_CALCULATOR;
    }

    /**
     * Returns the contribution calculator instance.
     *
     * @return the contribution calculator
     */
    public static ContributionCalculator contributionCalculator() {
        return CONTRIBUTION_CALCULATOR;
    }

    /**
     * Returns the income calculator instance.
     *
     * @return the income calculator
     */
    public static IncomeCalculator incomeCalculator() {
        return INCOME_CALCULATOR;
    }

    /**
     * Returns the return calculator instance.
     *
     * @return the return calculator
     */
    public static ReturnCalculator returnCalculator() {
        return RETURN_CALCULATOR;
    }
}
