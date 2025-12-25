package io.github.xmljim.retirement.domain.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.calculator.impl.DefaultInflationCalculator;

@DisplayName("InflationCalculator Tests")
class InflationCalculatorTest {

    private InflationCalculator calculator;
    private static final BigDecimal TOLERANCE = new BigDecimal("0.0001");

    @BeforeEach
    void setUp() {
        calculator = new DefaultInflationCalculator();
    }

    private void assertApproximatelyEqual(BigDecimal expected, BigDecimal actual) {
        BigDecimal diff = expected.subtract(actual).abs();
        assertTrue(diff.compareTo(TOLERANCE) < 0,
            String.format("Expected %s but got %s (diff: %s)", expected, actual, diff));
    }

    @Nested
    @DisplayName("calculateInflationMultiplier")
    class CalculateInflationMultiplierTests {

        @Test
        @DisplayName("Should calculate inflation multiplier for 3% over 10 years")
        void inflationMultiplier3Percent10Years() {
            BigDecimal rate = new BigDecimal("0.03");
            BigDecimal result = calculator.calculateInflationMultiplier(rate, 10);

            // Expected: (1.03)^10 ≈ 1.3439
            assertApproximatelyEqual(new BigDecimal("1.3439"), result.setScale(4, RoundingMode.HALF_UP));
        }

        @Test
        @DisplayName("Should return 1 for zero years")
        void inflationMultiplierZeroYears() {
            BigDecimal rate = new BigDecimal("0.03");
            BigDecimal result = calculator.calculateInflationMultiplier(rate, 0);

            assertEquals(0, BigDecimal.ONE.compareTo(result));
        }

        @Test
        @DisplayName("Should handle zero rate")
        void inflationMultiplierZeroRate() {
            BigDecimal rate = BigDecimal.ZERO;
            BigDecimal result = calculator.calculateInflationMultiplier(rate, 10);

            assertEquals(0, BigDecimal.ONE.compareTo(result));
        }

        @Test
        @DisplayName("Should throw for negative years")
        void inflationMultiplierNegativeYears() {
            BigDecimal rate = new BigDecimal("0.03");
            assertThrows(IllegalArgumentException.class, () ->
                calculator.calculateInflationMultiplier(rate, -1));
        }
    }

    @Nested
    @DisplayName("calculatePresentValueFactor")
    class CalculatePresentValueFactorTests {

        @Test
        @DisplayName("Should calculate present value factor for 3% over 10 years")
        void presentValueFactor3Percent10Years() {
            BigDecimal rate = new BigDecimal("0.03");
            BigDecimal result = calculator.calculatePresentValueFactor(rate, 10);

            // Expected: 1 / (1.03)^10 ≈ 0.7441
            assertApproximatelyEqual(new BigDecimal("0.7441"), result.setScale(4, RoundingMode.HALF_UP));
        }

        @Test
        @DisplayName("Should return 1 for zero years")
        void presentValueFactorZeroYears() {
            BigDecimal rate = new BigDecimal("0.03");
            BigDecimal result = calculator.calculatePresentValueFactor(rate, 0);

            assertEquals(0, BigDecimal.ONE.compareTo(result));
        }
    }

    @Nested
    @DisplayName("applyInflation")
    class ApplyInflationTests {

        @Test
        @DisplayName("Should apply inflation to value")
        void applyInflationToValue() {
            BigDecimal value = new BigDecimal("1000");
            BigDecimal rate = new BigDecimal("0.03");
            BigDecimal result = calculator.applyInflation(value, rate, 10);

            // Expected: 1000 * (1.03)^10 ≈ 1343.92
            assertApproximatelyEqual(new BigDecimal("1343.92"), result.setScale(2, RoundingMode.HALF_UP));
        }

        @Test
        @DisplayName("Should return same value for zero years")
        void applyInflationZeroYears() {
            BigDecimal value = new BigDecimal("1000");
            BigDecimal rate = new BigDecimal("0.03");
            BigDecimal result = calculator.applyInflation(value, rate, 0);

            assertEquals(0, value.compareTo(result));
        }

        @Test
        @DisplayName("Should throw for null value")
        void applyInflationNullValue() {
            BigDecimal rate = new BigDecimal("0.03");
            assertThrows(IllegalArgumentException.class, () ->
                calculator.applyInflation(null, rate, 10));
        }
    }

    @Nested
    @DisplayName("applyCola")
    class ApplyColaTests {

        @Test
        @DisplayName("Should apply COLA to salary")
        void applyColaToSalary() {
            BigDecimal salary = new BigDecimal("100000");
            BigDecimal colaRate = new BigDecimal("0.02");
            BigDecimal result = calculator.applyCola(salary, colaRate, 10);

            // Expected: 100000 * (1.02)^10 ≈ 121899.44
            assertApproximatelyEqual(new BigDecimal("121899.44"), result.setScale(2, RoundingMode.HALF_UP));
        }

        @Test
        @DisplayName("Should throw for null salary")
        void applyColaNullSalary() {
            BigDecimal colaRate = new BigDecimal("0.02");
            assertThrows(IllegalArgumentException.class, () ->
                calculator.applyCola(null, colaRate, 10));
        }
    }

    @Nested
    @DisplayName("toMonthlyRate")
    class ToMonthlyRateTests {

        @Test
        @DisplayName("Should convert 12% annual rate to 1% monthly rate")
        void toMonthlyRate12Percent() {
            BigDecimal annualRate = new BigDecimal("0.12");
            BigDecimal result = calculator.toMonthlyRate(annualRate);

            // Expected: 0.12 / 12 = 0.01
            assertEquals(0, new BigDecimal("0.01").compareTo(result.setScale(2, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should convert 3% annual rate to 0.25% monthly rate")
        void toMonthlyRate3Percent() {
            BigDecimal annualRate = new BigDecimal("0.03");
            BigDecimal result = calculator.toMonthlyRate(annualRate);

            // Expected: 0.03 / 12 = 0.0025
            assertEquals(0, new BigDecimal("0.0025").compareTo(result.setScale(4, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should return zero for null annual rate")
        void toMonthlyRateNullRate() {
            BigDecimal result = calculator.toMonthlyRate(null);

            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        @DisplayName("Should return zero for zero annual rate")
        void toMonthlyRateZeroRate() {
            BigDecimal result = calculator.toMonthlyRate(BigDecimal.ZERO);

            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should return calculator from factory")
        void factoryReturnsCalculator() {
            InflationCalculator factoryCalculator = CalculatorFactory.inflationCalculator();
            BigDecimal result = factoryCalculator.calculateInflationMultiplier(
                new BigDecimal("0.03"), 10);

            assertApproximatelyEqual(new BigDecimal("1.3439"), result.setScale(4, RoundingMode.HALF_UP));
        }
    }
}
