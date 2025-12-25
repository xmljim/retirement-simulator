package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;

/**
 * Calculator for inflation and cost-of-living adjustment calculations.
 *
 * <p>Provides methods for:
 * <ul>
 *   <li>Calculating inflation multipliers (future value factors)</li>
 *   <li>Calculating present value factors (discounting)</li>
 *   <li>Applying inflation to monetary values</li>
 *   <li>Applying cost-of-living adjustments to salaries</li>
 * </ul>
 *
 * <p>All calculations use BigDecimal for financial precision.
 *
 * @see io.github.xmljim.retirement.domain.calculator.impl.DefaultInflationCalculator
 */
public interface InflationCalculator {

    /**
     * Calculates the inflation multiplier (future value factor) for a given
     * rate and number of years.
     *
     * <p>Formula: {@code (1 + rate)^years}
     *
     * <p>Example: A 3% annual rate over 10 years yields approximately 1.3439,
     * meaning $1 today would be worth $1.34 in future dollars.
     *
     * @param rate the annual inflation rate as a decimal (e.g., 0.03 for 3%)
     * @param years the number of years to compound
     * @return the inflation multiplier
     * @throws IllegalArgumentException if years is negative
     */
    BigDecimal calculateInflationMultiplier(BigDecimal rate, int years);

    /**
     * Calculates the present value factor (discount factor) for a given
     * rate and number of years.
     *
     * <p>Formula: {@code 1 / (1 + rate)^years}
     *
     * <p>This is the inverse of the inflation multiplier, used to discount
     * future values back to present value.
     *
     * <p>Example: A 3% annual rate over 10 years yields approximately 0.7441,
     * meaning $1 in 10 years is worth about $0.74 today.
     *
     * @param rate the annual discount rate as a decimal (e.g., 0.03 for 3%)
     * @param years the number of years to discount
     * @return the present value factor
     * @throws IllegalArgumentException if years is negative
     */
    BigDecimal calculatePresentValueFactor(BigDecimal rate, int years);

    /**
     * Applies inflation to a monetary value over a number of years.
     *
     * <p>Formula: {@code value * (1 + rate)^years}
     *
     * <p>Example: $1000 with 3% annual inflation over 10 years yields
     * approximately $1343.92.
     *
     * @param value the current monetary value
     * @param rate the annual inflation rate as a decimal
     * @param years the number of years to inflate
     * @return the inflated value
     * @throws IllegalArgumentException if value is null or years is negative
     */
    BigDecimal applyInflation(BigDecimal value, BigDecimal rate, int years);

    /**
     * Applies a cost-of-living adjustment (COLA) to a salary over a number of years.
     *
     * <p>This is functionally equivalent to {@link #applyInflation} but semantically
     * represents salary adjustments rather than general inflation.
     *
     * <p>Formula: {@code salary * (1 + colaRate)^years}
     *
     * @param salary the base annual salary
     * @param colaRate the annual COLA rate as a decimal (e.g., 0.02 for 2%)
     * @param years the number of years to adjust
     * @return the adjusted salary
     * @throws IllegalArgumentException if salary is null or years is negative
     */
    BigDecimal applyCola(BigDecimal salary, BigDecimal colaRate, int years);
}
