package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import io.github.xmljim.retirement.domain.calculator.InflationCalculator;

/**
 * Default implementation of {@link InflationCalculator}.
 *
 * <p>This implementation uses BigDecimal arithmetic with a precision of 10 decimal
 * places and HALF_UP rounding for financial calculations.
 */
public class DefaultInflationCalculator implements InflationCalculator {

    private static final int SCALE = 10;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final MathContext MATH_CONTEXT = new MathContext(SCALE, ROUNDING_MODE);

    /**
     * Creates a new DefaultInflationCalculator.
     */
    public DefaultInflationCalculator() {
        // Default constructor
    }

    @Override
    public BigDecimal calculateInflationMultiplier(BigDecimal rate, int years) {
        if (years < 0) {
            throw new IllegalArgumentException("Years cannot be negative: " + years);
        }
        if (years == 0) {
            return BigDecimal.ONE;
        }

        // Formula: (1 + rate)^years
        BigDecimal base = BigDecimal.ONE.add(rate);
        return pow(base, years);
    }

    @Override
    public BigDecimal calculatePresentValueFactor(BigDecimal rate, int years) {
        if (years < 0) {
            throw new IllegalArgumentException("Years cannot be negative: " + years);
        }
        if (years == 0) {
            return BigDecimal.ONE;
        }

        // Formula: 1 / (1 + rate)^years
        BigDecimal inflationMultiplier = calculateInflationMultiplier(rate, years);
        return BigDecimal.ONE.divide(inflationMultiplier, SCALE, ROUNDING_MODE);
    }

    @Override
    public BigDecimal applyInflation(BigDecimal value, BigDecimal rate, int years) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        if (years < 0) {
            throw new IllegalArgumentException("Years cannot be negative: " + years);
        }
        if (years == 0) {
            return value;
        }

        // Formula: value * (1 + rate)^years
        BigDecimal multiplier = calculateInflationMultiplier(rate, years);
        return value.multiply(multiplier).setScale(SCALE, ROUNDING_MODE);
    }

    @Override
    public BigDecimal applyCola(BigDecimal salary, BigDecimal colaRate, int years) {
        if (salary == null) {
            throw new IllegalArgumentException("Salary cannot be null");
        }
        // COLA is functionally the same as inflation application
        return applyInflation(salary, colaRate, years);
    }

    private BigDecimal pow(BigDecimal base, int exponent) {
        return MathUtils.pow(base, exponent, SCALE, ROUNDING_MODE);
    }
}
