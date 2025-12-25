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

import io.github.xmljim.retirement.domain.calculator.impl.DefaultReturnCalculator;
import io.github.xmljim.retirement.domain.value.AssetAllocation;

@DisplayName("ReturnCalculator Tests")
class ReturnCalculatorTest {

    private ReturnCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new DefaultReturnCalculator();
    }

    @Nested
    @DisplayName("calculateBlendedReturn")
    class BlendedReturnTests {

        @Test
        @DisplayName("Should calculate blended return for balanced allocation")
        void blendedReturnBalanced() {
            AssetAllocation allocation = AssetAllocation.balanced(); // 60/40/0
            BigDecimal stockReturn = new BigDecimal("0.07");
            BigDecimal bondReturn = new BigDecimal("0.04");
            BigDecimal cashReturn = new BigDecimal("0.02");

            BigDecimal result = calculator.calculateBlendedReturn(
                allocation, stockReturn, bondReturn, cashReturn);

            // Expected: (0.60 * 0.07) + (0.40 * 0.04) + (0 * 0.02) = 0.042 + 0.016 = 0.058
            assertEquals(0, new BigDecimal("0.058").compareTo(
                result.setScale(3, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should calculate blended return for all stocks")
        void blendedReturnAllStocks() {
            AssetAllocation allocation = AssetAllocation.allStocks();
            BigDecimal stockReturn = new BigDecimal("0.10");
            BigDecimal bondReturn = new BigDecimal("0.05");
            BigDecimal cashReturn = new BigDecimal("0.02");

            BigDecimal result = calculator.calculateBlendedReturn(
                allocation, stockReturn, bondReturn, cashReturn);

            // Expected: 1.0 * 0.10 = 0.10
            assertEquals(0, new BigDecimal("0.10").compareTo(
                result.setScale(2, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should calculate blended return for all bonds")
        void blendedReturnAllBonds() {
            AssetAllocation allocation = AssetAllocation.allBonds();
            BigDecimal stockReturn = new BigDecimal("0.10");
            BigDecimal bondReturn = new BigDecimal("0.05");
            BigDecimal cashReturn = new BigDecimal("0.02");

            BigDecimal result = calculator.calculateBlendedReturn(
                allocation, stockReturn, bondReturn, cashReturn);

            // Expected: 1.0 * 0.05 = 0.05
            assertEquals(0, new BigDecimal("0.05").compareTo(
                result.setScale(2, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should calculate blended return for custom allocation")
        void blendedReturnCustom() {
            AssetAllocation allocation = AssetAllocation.of(70, 20, 10);
            BigDecimal stockReturn = new BigDecimal("0.07");
            BigDecimal bondReturn = new BigDecimal("0.04");
            BigDecimal cashReturn = new BigDecimal("0.02");

            BigDecimal result = calculator.calculateBlendedReturn(
                allocation, stockReturn, bondReturn, cashReturn);

            // Expected: (0.70 * 0.07) + (0.20 * 0.04) + (0.10 * 0.02) = 0.049 + 0.008 + 0.002 = 0.059
            assertEquals(0, new BigDecimal("0.059").compareTo(
                result.setScale(3, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should throw for null allocation")
        void nullAllocation() {
            assertThrows(IllegalArgumentException.class, () ->
                calculator.calculateBlendedReturn(null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Should handle null return rates as zero")
        void nullReturnRates() {
            AssetAllocation allocation = AssetAllocation.balanced();

            BigDecimal result = calculator.calculateBlendedReturn(
                allocation, null, null, null);

            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }
    }

    @Nested
    @DisplayName("calculateAccountGrowth")
    class AccountGrowthTests {

        @Test
        @DisplayName("Should calculate account growth over 12 months")
        void accountGrowth12Months() {
            BigDecimal balance = new BigDecimal("100000");
            BigDecimal annualRate = new BigDecimal("0.06"); // 6% annual

            BigDecimal result = calculator.calculateAccountGrowth(balance, annualRate, 12);

            // With monthly compounding at ~0.5% per month
            // Result should be greater than 100000 but less than 106000
            assertTrue(result.compareTo(balance) > 0);
            assertTrue(result.compareTo(new BigDecimal("107000")) < 0);
        }

        @Test
        @DisplayName("Should return same balance for zero months")
        void accountGrowthZeroMonths() {
            BigDecimal balance = new BigDecimal("100000");
            BigDecimal annualRate = new BigDecimal("0.06");

            BigDecimal result = calculator.calculateAccountGrowth(balance, annualRate, 0);

            assertEquals(0, balance.compareTo(result));
        }

        @Test
        @DisplayName("Should throw for null balance")
        void nullBalance() {
            assertThrows(IllegalArgumentException.class, () ->
                calculator.calculateAccountGrowth(null, new BigDecimal("0.06"), 12));
        }

        @Test
        @DisplayName("Should throw for negative months")
        void negativeMonths() {
            BigDecimal balance = new BigDecimal("100000");

            assertThrows(IllegalArgumentException.class, () ->
                calculator.calculateAccountGrowth(balance, new BigDecimal("0.06"), -1));
        }
    }

    @Nested
    @DisplayName("toMonthlyRate")
    class ToMonthlyRateTests {

        @Test
        @DisplayName("Should convert annual rate to monthly rate")
        void annualToMonthly() {
            BigDecimal annualRate = new BigDecimal("0.12"); // 12% annual

            BigDecimal result = calculator.toMonthlyRate(annualRate);

            // Simple division: 0.12 / 12 = 0.01
            assertEquals(0, new BigDecimal("0.01").compareTo(
                result.setScale(2, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should return zero for null rate")
        void nullRate() {
            BigDecimal result = calculator.toMonthlyRate(null);

            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        @DisplayName("Should return zero for zero rate")
        void zeroRate() {
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
            ReturnCalculator factoryCalculator = CalculatorFactory.returnCalculator();
            AssetAllocation allocation = AssetAllocation.balanced();

            BigDecimal result = factoryCalculator.calculateBlendedReturn(
                allocation,
                new BigDecimal("0.07"),
                new BigDecimal("0.04"),
                new BigDecimal("0.02"));

            assertTrue(result.compareTo(BigDecimal.ZERO) > 0);
        }
    }
}
