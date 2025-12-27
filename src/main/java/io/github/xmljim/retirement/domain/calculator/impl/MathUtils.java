package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Utility class for mathematical operations used by calculator implementations.
 *
 * <p>This class provides high-precision BigDecimal arithmetic operations
 * that are commonly needed across calculator implementations.
 */
public final class MathUtils {

    private static final int DEFAULT_SCALE = 10;
    private static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_UP;
    private static final MathContext DEFAULT_CONTEXT = new MathContext(DEFAULT_SCALE, DEFAULT_ROUNDING);

    private MathUtils() {
        // Prevent instantiation
    }

    /**
     * Calculates base raised to the power of exponent using BigDecimal arithmetic.
     *
     * <p>Uses repeated squaring for efficiency while maintaining precision.
     *
     * @param base the base value
     * @param exponent the exponent (must be non-negative)
     * @param scale the scale for the result
     * @param roundingMode the rounding mode to use
     * @return base^exponent
     */
    public static BigDecimal pow(BigDecimal base, int exponent, int scale, RoundingMode roundingMode) {
        if (exponent == 0) {
            return BigDecimal.ONE;
        }
        if (exponent == 1) {
            return base;
        }

        MathContext context = new MathContext(scale, roundingMode);

        // Use repeated squaring for efficiency
        BigDecimal result = BigDecimal.ONE;
        BigDecimal currentBase = base;
        int exp = exponent;

        while (exp > 0) {
            if ((exp & 1) == 1) {
                result = result.multiply(currentBase, context);
            }
            currentBase = currentBase.multiply(currentBase, context);
            exp >>= 1;
        }

        return result.setScale(scale, roundingMode);
    }

    /**
     * Calculates base raised to the power of exponent with default precision.
     *
     * @param base the base value
     * @param exponent the exponent (must be non-negative)
     * @return base^exponent
     */
    public static BigDecimal pow(BigDecimal base, int exponent) {
        return pow(base, exponent, DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    /**
     * Calculates base raised to a fractional power.
     *
     * <p>Uses {@link Math#pow(double, double)} internally, which provides
     * sufficient precision for financial calculations involving return rates.
     *
     * <p>This is used for true annual compounding:
     * <pre>
     * (1 + annualRate)^(months/12)
     * </pre>
     *
     * @param base the base value
     * @param exponent the fractional exponent
     * @param scale the scale for the result
     * @param roundingMode the rounding mode to use
     * @return base^exponent as a BigDecimal
     */
    public static BigDecimal pow(BigDecimal base, double exponent, int scale, RoundingMode roundingMode) {
        if (exponent == 0.0) {
            return BigDecimal.ONE;
        }
        if (exponent == 1.0) {
            return base.setScale(scale, roundingMode);
        }

        double result = Math.pow(base.doubleValue(), exponent);
        return BigDecimal.valueOf(result).setScale(scale, roundingMode);
    }

    /**
     * Calculates base raised to a fractional power with default precision.
     *
     * @param base the base value
     * @param exponent the fractional exponent
     * @return base^exponent as a BigDecimal
     */
    public static BigDecimal pow(BigDecimal base, double exponent) {
        return pow(base, exponent, DEFAULT_SCALE, DEFAULT_ROUNDING);
    }
}
