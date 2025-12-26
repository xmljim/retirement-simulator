package io.github.xmljim.retirement.domain.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CompoundingFunctions Tests")
class CompoundingFunctionsTest {

    private static final BigDecimal PRINCIPAL = new BigDecimal("10000");
    private static final BigDecimal TEN_PERCENT = new BigDecimal("0.10");
    private static final BigDecimal TWELVE_PERCENT = new BigDecimal("0.12");

    @Nested
    @DisplayName("ANNUAL Compounding")
    class AnnualCompoundingTests {

        @Test
        @DisplayName("Should calculate annual compounding for 12 months")
        void annualCompoundingOneYear() {
            // 10000 * (1.10)^(12/12) = 10000 * 1.10 = 11000
            BigDecimal result = CompoundingFunctions.ANNUAL.compound(PRINCIPAL, TEN_PERCENT, 12);

            assertEquals(0, new BigDecimal("11000").compareTo(result.setScale(0, java.math.RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should calculate annual compounding for 6 months")
        void annualCompoundingSixMonths() {
            // 10000 * (1.10)^(6/12) = 10000 * (1.10)^0.5 = 10488.09
            BigDecimal result = CompoundingFunctions.ANNUAL.compound(PRINCIPAL, TEN_PERCENT, 6);

            assertTrue(result.compareTo(new BigDecimal("10480")) > 0);
            assertTrue(result.compareTo(new BigDecimal("10500")) < 0);
        }

        @Test
        @DisplayName("Should calculate annual compounding for 24 months")
        void annualCompoundingTwoYears() {
            // 10000 * (1.10)^(24/12) = 10000 * 1.21 = 12100
            BigDecimal result = CompoundingFunctions.ANNUAL.compound(PRINCIPAL, TEN_PERCENT, 24);

            assertEquals(0, new BigDecimal("12100").compareTo(result.setScale(0, java.math.RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should return principal for zero periods")
        void zeroPeriods() {
            BigDecimal result = CompoundingFunctions.ANNUAL.compound(PRINCIPAL, TEN_PERCENT, 0);
            assertEquals(0, PRINCIPAL.compareTo(result));
        }

        @Test
        @DisplayName("Should return principal for zero rate")
        void zeroRate() {
            BigDecimal result = CompoundingFunctions.ANNUAL.compound(PRINCIPAL, BigDecimal.ZERO, 12);
            assertEquals(0, PRINCIPAL.compareTo(result));
        }

        @Test
        @DisplayName("Should have correct name and formula")
        void nameAndFormula() {
            assertEquals("Annual", CompoundingFunctions.ANNUAL.getName());
            assertTrue(CompoundingFunctions.ANNUAL.getFormula().contains("months/12"));
        }
    }

    @Nested
    @DisplayName("MONTHLY Compounding")
    class MonthlyCompoundingTests {

        @Test
        @DisplayName("Should calculate monthly compounding for 12 months")
        void monthlyCompoundingOneYear() {
            // 10000 * (1 + 0.12/12)^12 = 10000 * (1.01)^12 = 11268.25
            BigDecimal result = CompoundingFunctions.MONTHLY.compound(PRINCIPAL, TWELVE_PERCENT, 12);

            assertTrue(result.compareTo(new BigDecimal("11260")) > 0);
            assertTrue(result.compareTo(new BigDecimal("11280")) < 0);
        }

        @Test
        @DisplayName("Should calculate monthly compounding for 6 months")
        void monthlyCompoundingSixMonths() {
            // 10000 * (1 + 0.12/12)^6 = 10000 * (1.01)^6 = 10615.20
            BigDecimal result = CompoundingFunctions.MONTHLY.compound(PRINCIPAL, TWELVE_PERCENT, 6);

            assertTrue(result.compareTo(new BigDecimal("10610")) > 0);
            assertTrue(result.compareTo(new BigDecimal("10620")) < 0);
        }

        @Test
        @DisplayName("Monthly should compound more than annual for same rate")
        void monthlyMoreThanAnnual() {
            // Monthly compounding yields more than annual for the same nominal rate
            BigDecimal annual = CompoundingFunctions.ANNUAL.compound(PRINCIPAL, TWELVE_PERCENT, 12);
            BigDecimal monthly = CompoundingFunctions.MONTHLY.compound(PRINCIPAL, TWELVE_PERCENT, 12);

            assertTrue(monthly.compareTo(annual) > 0);
        }

        @Test
        @DisplayName("Should have correct name and formula")
        void nameAndFormula() {
            assertEquals("Monthly", CompoundingFunctions.MONTHLY.getName());
            assertTrue(CompoundingFunctions.MONTHLY.getFormula().contains("annualRate/12"));
        }
    }

    @Nested
    @DisplayName("DAILY Compounding")
    class DailyCompoundingTests {

        @Test
        @DisplayName("Should calculate daily compounding for 365 days")
        void dailyCompoundingOneYear() {
            // 10000 * (1 + 0.10/365)^365 = 10000 * 1.10516 = 11051.6
            BigDecimal result = CompoundingFunctions.DAILY.compound(PRINCIPAL, TEN_PERCENT, 365);

            // Should be slightly more than annual compounding (11000) due to more frequent compounding
            assertTrue(result.compareTo(new BigDecimal("11040")) > 0);
            assertTrue(result.compareTo(new BigDecimal("11060")) < 0);
        }

        @Test
        @DisplayName("Should calculate daily compounding for 30 days")
        void dailyCompounding30Days() {
            // 10000 * (1 + 0.18/365)^30 = approximate
            BigDecimal result = CompoundingFunctions.DAILY.compound(
                PRINCIPAL, new BigDecimal("0.18"), 30);

            assertTrue(result.compareTo(new BigDecimal("10140")) > 0);
            assertTrue(result.compareTo(new BigDecimal("10160")) < 0);
        }

        @Test
        @DisplayName("Should have correct name and formula")
        void nameAndFormula() {
            assertEquals("Daily", CompoundingFunctions.DAILY.getName());
            assertTrue(CompoundingFunctions.DAILY.getFormula().contains("365"));
        }
    }

    @Nested
    @DisplayName("CONTINUOUS Compounding")
    class ContinuousCompoundingTests {

        @Test
        @DisplayName("Should calculate continuous compounding for 12 months")
        void continuousCompoundingOneYear() {
            // 10000 * e^(0.10 * 1) = 10000 * 1.10517 = 11051.7
            BigDecimal result = CompoundingFunctions.CONTINUOUS.compound(PRINCIPAL, TEN_PERCENT, 12);

            assertTrue(result.compareTo(new BigDecimal("11040")) > 0);
            assertTrue(result.compareTo(new BigDecimal("11060")) < 0);
        }

        @Test
        @DisplayName("Continuous should compound most of all methods")
        void continuousCompoundsTheMost() {
            // Continuous is the theoretical maximum
            BigDecimal annual = CompoundingFunctions.ANNUAL.compound(PRINCIPAL, TEN_PERCENT, 12);
            BigDecimal monthly = CompoundingFunctions.MONTHLY.compound(PRINCIPAL, TEN_PERCENT, 12);
            BigDecimal continuous = CompoundingFunctions.CONTINUOUS.compound(PRINCIPAL, TEN_PERCENT, 12);

            assertTrue(continuous.compareTo(monthly) > 0);
            assertTrue(monthly.compareTo(annual) > 0);
        }

        @Test
        @DisplayName("Should have correct name and formula")
        void nameAndFormula() {
            assertEquals("Continuous", CompoundingFunctions.CONTINUOUS.getName());
            assertTrue(CompoundingFunctions.CONTINUOUS.getFormula().contains("e^"));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle null principal")
        void nullPrincipal() {
            BigDecimal result = CompoundingFunctions.ANNUAL.compound(null, TEN_PERCENT, 12);
            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        @DisplayName("Should handle null rate")
        void nullRate() {
            BigDecimal result = CompoundingFunctions.ANNUAL.compound(PRINCIPAL, null, 12);
            assertEquals(0, PRINCIPAL.compareTo(result));
        }

        @Test
        @DisplayName("Should handle negative periods")
        void negativePeriods() {
            BigDecimal result = CompoundingFunctions.ANNUAL.compound(PRINCIPAL, TEN_PERCENT, -1);
            assertEquals(0, PRINCIPAL.compareTo(result));
        }

        @Test
        @DisplayName("Should handle zero principal")
        void zeroPrincipal() {
            BigDecimal result = CompoundingFunctions.ANNUAL.compound(BigDecimal.ZERO, TEN_PERCENT, 12);
            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }
    }

    @Nested
    @DisplayName("ReturnCalculator Integration")
    class ReturnCalculatorIntegrationTests {

        @Test
        @DisplayName("Should use compounding function in calculator")
        void calculatorWithCompoundingFunction() {
            ReturnCalculator calculator = CalculatorFactory.returnCalculator();

            // Using ANNUAL (default)
            BigDecimal annual = calculator.calculateAccountGrowth(PRINCIPAL, TEN_PERCENT, 12);

            // Using MONTHLY explicitly
            BigDecimal monthly = calculator.calculateAccountGrowth(
                PRINCIPAL, TEN_PERCENT, 12, CompoundingFunctions.MONTHLY);

            // Monthly should be higher
            assertTrue(monthly.compareTo(annual) > 0);
        }

        @Test
        @DisplayName("Default behavior should match ANNUAL")
        void defaultMatchesAnnual() {
            ReturnCalculator calculator = CalculatorFactory.returnCalculator();

            BigDecimal defaultResult = calculator.calculateAccountGrowth(PRINCIPAL, TEN_PERCENT, 12);
            BigDecimal annualResult = calculator.calculateAccountGrowth(
                PRINCIPAL, TEN_PERCENT, 12, CompoundingFunctions.ANNUAL);

            assertEquals(0, defaultResult.compareTo(annualResult));
        }
    }

    @Nested
    @DisplayName("Custom CompoundingFunction")
    class CustomCompoundingFunctionTests {

        @Test
        @DisplayName("Custom function should use default getName")
        void customFunctionDefaultName() {
            // Create a custom compounding function without overriding getName
            CompoundingFunction custom = (principal, rate, periods) ->
                principal.multiply(BigDecimal.valueOf(2));

            assertEquals("Custom", custom.getName());
        }

        @Test
        @DisplayName("Custom function should use default getFormula")
        void customFunctionDefaultFormula() {
            // Create a custom compounding function without overriding getFormula
            CompoundingFunction custom = (principal, rate, periods) ->
                principal.multiply(BigDecimal.valueOf(2));

            assertEquals("Custom compounding formula", custom.getFormula());
        }

        @Test
        @DisplayName("Custom function should work with calculator")
        void customFunctionWithCalculator() {
            // Double the principal regardless of rate/periods
            CompoundingFunction doubler = (principal, rate, periods) ->
                principal.multiply(BigDecimal.valueOf(2));

            ReturnCalculator calculator = CalculatorFactory.returnCalculator();
            BigDecimal result = calculator.calculateAccountGrowth(
                PRINCIPAL, TEN_PERCENT, 12, doubler);

            assertEquals(0, new BigDecimal("20000").compareTo(
                result.setScale(0, java.math.RoundingMode.HALF_UP)));
        }
    }

    @Nested
    @DisplayName("Edge Cases for All Functions")
    class AllFunctionsEdgeCaseTests {

        @Test
        @DisplayName("MONTHLY should handle null principal")
        void monthlyNullPrincipal() {
            BigDecimal result = CompoundingFunctions.MONTHLY.compound(null, TEN_PERCENT, 12);
            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        @DisplayName("MONTHLY should handle null rate")
        void monthlyNullRate() {
            BigDecimal result = CompoundingFunctions.MONTHLY.compound(PRINCIPAL, null, 12);
            assertEquals(0, PRINCIPAL.compareTo(result));
        }

        @Test
        @DisplayName("DAILY should handle null principal")
        void dailyNullPrincipal() {
            BigDecimal result = CompoundingFunctions.DAILY.compound(null, TEN_PERCENT, 365);
            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        @DisplayName("DAILY should handle null rate")
        void dailyNullRate() {
            BigDecimal result = CompoundingFunctions.DAILY.compound(PRINCIPAL, null, 365);
            assertEquals(0, PRINCIPAL.compareTo(result));
        }

        @Test
        @DisplayName("CONTINUOUS should handle null principal")
        void continuousNullPrincipal() {
            BigDecimal result = CompoundingFunctions.CONTINUOUS.compound(null, TEN_PERCENT, 12);
            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        @DisplayName("CONTINUOUS should handle null rate")
        void continuousNullRate() {
            BigDecimal result = CompoundingFunctions.CONTINUOUS.compound(PRINCIPAL, null, 12);
            assertEquals(0, PRINCIPAL.compareTo(result));
        }

        @Test
        @DisplayName("MONTHLY should handle zero periods")
        void monthlyZeroPeriods() {
            BigDecimal result = CompoundingFunctions.MONTHLY.compound(PRINCIPAL, TEN_PERCENT, 0);
            assertEquals(0, PRINCIPAL.compareTo(result));
        }

        @Test
        @DisplayName("DAILY should handle zero periods")
        void dailyZeroPeriods() {
            BigDecimal result = CompoundingFunctions.DAILY.compound(PRINCIPAL, TEN_PERCENT, 0);
            assertEquals(0, PRINCIPAL.compareTo(result));
        }

        @Test
        @DisplayName("CONTINUOUS should handle zero periods")
        void continuousZeroPeriods() {
            BigDecimal result = CompoundingFunctions.CONTINUOUS.compound(PRINCIPAL, TEN_PERCENT, 0);
            assertEquals(0, PRINCIPAL.compareTo(result));
        }

        @Test
        @DisplayName("MONTHLY should handle zero principal")
        void monthlyZeroPrincipal() {
            BigDecimal result = CompoundingFunctions.MONTHLY.compound(BigDecimal.ZERO, TEN_PERCENT, 12);
            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        @DisplayName("DAILY should handle zero principal")
        void dailyZeroPrincipal() {
            BigDecimal result = CompoundingFunctions.DAILY.compound(BigDecimal.ZERO, TEN_PERCENT, 365);
            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        @DisplayName("CONTINUOUS should handle zero principal")
        void continuousZeroPrincipal() {
            BigDecimal result = CompoundingFunctions.CONTINUOUS.compound(BigDecimal.ZERO, TEN_PERCENT, 12);
            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }
    }
}
