package io.github.xmljim.retirement.domain.calculator.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for MathUtils internal utility class.
 * These tests ensure edge cases in the power functions are covered.
 */
@DisplayName("MathUtils Tests")
class MathUtilsTest {

    private static final int SCALE = 10;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    @Nested
    @DisplayName("Fractional Power (double exponent)")
    class FractionalPowerTests {

        @Test
        @DisplayName("Should return ONE for exponent 0.0")
        void exponentZero() {
            BigDecimal base = new BigDecimal("1.10");

            BigDecimal result = MathUtils.pow(base, 0.0, SCALE, ROUNDING);

            assertEquals(0, BigDecimal.ONE.compareTo(result));
        }

        @Test
        @DisplayName("Should return base for exponent 1.0")
        void exponentOne() {
            BigDecimal base = new BigDecimal("1.10");

            BigDecimal result = MathUtils.pow(base, 1.0, SCALE, ROUNDING);

            assertEquals(0, base.setScale(SCALE, ROUNDING).compareTo(result));
        }

        @Test
        @DisplayName("Should calculate fractional power correctly")
        void fractionalExponent() {
            BigDecimal base = new BigDecimal("1.10");

            // (1.10)^0.5 = sqrt(1.10) ≈ 1.0488
            BigDecimal result = MathUtils.pow(base, 0.5, SCALE, ROUNDING);

            assertEquals(0, new BigDecimal("1.0488").compareTo(
                result.setScale(4, ROUNDING)));
        }

        @Test
        @DisplayName("Should work with default precision overload")
        void defaultPrecisionOverload() {
            BigDecimal base = new BigDecimal("1.10");

            BigDecimal result = MathUtils.pow(base, 0.5);

            // Should complete without error and return positive value
            assertEquals(0, new BigDecimal("1.0488").compareTo(
                result.setScale(4, ROUNDING)));
        }
    }

    @Nested
    @DisplayName("Integer Power")
    class IntegerPowerTests {

        @Test
        @DisplayName("Should return ONE for exponent 0")
        void exponentZero() {
            BigDecimal base = new BigDecimal("2.0");

            BigDecimal result = MathUtils.pow(base, 0, SCALE, ROUNDING);

            assertEquals(0, BigDecimal.ONE.compareTo(result));
        }

        @Test
        @DisplayName("Should return base for exponent 1")
        void exponentOne() {
            BigDecimal base = new BigDecimal("2.0");

            BigDecimal result = MathUtils.pow(base, 1, SCALE, ROUNDING);

            assertEquals(0, base.compareTo(result));
        }

        @Test
        @DisplayName("Should calculate integer power correctly")
        void integerExponent() {
            BigDecimal base = new BigDecimal("2.0");

            // 2^10 = 1024
            BigDecimal result = MathUtils.pow(base, 10, SCALE, ROUNDING);

            assertEquals(0, new BigDecimal("1024").compareTo(
                result.setScale(0, ROUNDING)));
        }
    }
}
