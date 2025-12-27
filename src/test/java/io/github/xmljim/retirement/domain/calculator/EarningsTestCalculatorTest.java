package io.github.xmljim.retirement.domain.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.value.EarningsTestResult;

/**
 * Tests for the EarningsTestCalculator.
 *
 * <p>Test scenarios based on SSA earnings test rules:
 * <ul>
 *   <li>Below FRA all year: $1 reduction per $2 over limit</li>
 *   <li>Year reaching FRA: $1 per $3 over higher limit</li>
 *   <li>At/after FRA: No earnings test</li>
 * </ul>
 *
 * <p>2025 limits: $23,400 (below FRA) / $62,160 (FRA year)
 */
@DisplayName("EarningsTestCalculator Tests")
class EarningsTestCalculatorTest {

    private static final int FRA_67_MONTHS = 804;  // 67 years
    private static final int AGE_65_MONTHS = 780;  // 65 years
    private static final int AGE_62_MONTHS = 744;  // 62 years
    private static final int YEAR_2024 = 2024;

    // 2024 default limits (used by factory without Spring context)
    private static final BigDecimal BELOW_FRA_LIMIT = new BigDecimal("22320");
    private static final BigDecimal FRA_YEAR_LIMIT = new BigDecimal("59520");

    private EarningsTestCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = CalculatorFactory.earningsTestCalculator();
    }

    @Nested
    @DisplayName("At or Past FRA - No Earnings Test")
    class AtOrPastFraTests {

        @Test
        @DisplayName("Should be exempt when at FRA")
        void exemptAtFra() {
            BigDecimal earnings = new BigDecimal("100000");
            BigDecimal benefit = new BigDecimal("24000");

            EarningsTestResult result = calculator.calculate(
                earnings, benefit, FRA_67_MONTHS, FRA_67_MONTHS, YEAR_2024);

            assertFalse(result.subjectToTest());
            assertEquals(0, benefit.compareTo(result.reducedBenefit()));
            assertEquals(0, BigDecimal.ZERO.compareTo(result.reductionAmount()));
            assertTrue(result.exemptReason().isPresent());
            assertEquals("At or past Full Retirement Age", result.exemptReason().get());
        }

        @Test
        @DisplayName("Should be exempt when past FRA")
        void exemptPastFra() {
            int age68Months = 816;  // 68 years
            BigDecimal earnings = new BigDecimal("150000");
            BigDecimal benefit = new BigDecimal("30000");

            EarningsTestResult result = calculator.calculate(
                earnings, benefit, age68Months, FRA_67_MONTHS, YEAR_2024);

            assertFalse(result.subjectToTest());
            assertEquals(0, benefit.compareTo(result.reducedBenefit()));
            assertFalse(result.isReduced());
        }
    }

    @Nested
    @DisplayName("Below FRA All Year - Under Limit")
    class BelowFraUnderLimitTests {

        @Test
        @DisplayName("Should have no reduction when under limit")
        void noReductionUnderLimit() {
            BigDecimal earnings = new BigDecimal("20000");  // Under $22,320
            BigDecimal benefit = new BigDecimal("24000");

            EarningsTestResult result = calculator.calculate(
                earnings, benefit, AGE_65_MONTHS, FRA_67_MONTHS, YEAR_2024);

            assertTrue(result.subjectToTest());
            assertFalse(result.isReduced());
            assertEquals(0, benefit.compareTo(result.reducedBenefit()));
            assertEquals(0, BigDecimal.ZERO.compareTo(result.reductionAmount()));
        }

        @Test
        @DisplayName("Should have no reduction at exactly the limit")
        void noReductionAtExactLimit() {
            BigDecimal earnings = BELOW_FRA_LIMIT;  // Exactly $22,320
            BigDecimal benefit = new BigDecimal("24000");

            EarningsTestResult result = calculator.calculate(
                earnings, benefit, AGE_65_MONTHS, FRA_67_MONTHS, YEAR_2024);

            assertTrue(result.subjectToTest());
            assertFalse(result.isReduced());
        }
    }

    @Nested
    @DisplayName("Below FRA All Year - Over Limit")
    class BelowFraOverLimitTests {

        @Test
        @DisplayName("Should reduce benefit $1 per $2 over limit")
        void reducesBenefitOverLimit() {
            // 2024 limit: $22,320
            // Earning $32,320 = $10,000 over limit
            // Reduction = $10,000 / 2 = $5,000
            BigDecimal earnings = new BigDecimal("32320");
            BigDecimal benefit = new BigDecimal("24000");

            EarningsTestResult result = calculator.calculate(
                earnings, benefit, AGE_65_MONTHS, FRA_67_MONTHS, YEAR_2024);

            assertTrue(result.subjectToTest());
            assertTrue(result.isReduced());
            assertEquals(0, new BigDecimal("10000.00").compareTo(result.excessEarnings()));
            assertEquals(0, new BigDecimal("5000.00").compareTo(result.reductionAmount()));
            assertEquals(0, new BigDecimal("19000.00").compareTo(result.reducedBenefit()));
        }

        @Test
        @DisplayName("Should cap reduction at total benefit")
        void capsReductionAtTotalBenefit() {
            // 2024 limit: $22,320
            // Earning $100,000 = $77,680 over limit
            // Reduction = $38,840 (more than $12,000 benefit)
            BigDecimal earnings = new BigDecimal("100000");
            BigDecimal benefit = new BigDecimal("12000");

            EarningsTestResult result = calculator.calculate(
                earnings, benefit, AGE_65_MONTHS, FRA_67_MONTHS, YEAR_2024);

            assertTrue(result.isReduced());
            // Reduction capped at benefit amount
            assertEquals(0, benefit.compareTo(result.reductionAmount()));
            assertEquals(0, BigDecimal.ZERO.compareTo(result.reducedBenefit()));
        }

        @Test
        @DisplayName("Should calculate months withheld")
        void calculatesMonthsWithheld() {
            // 2024 limit: $22,320
            // Earning $42,320 = $20,000 over limit
            // Reduction = $10,000 = 5 months of $2,000/month benefit
            BigDecimal earnings = new BigDecimal("42320");
            BigDecimal benefit = new BigDecimal("24000");  // $2,000/month

            EarningsTestResult result = calculator.calculate(
                earnings, benefit, AGE_65_MONTHS, FRA_67_MONTHS, YEAR_2024);

            assertTrue(result.isReduced());
            assertEquals(5, result.monthsWithheld());
        }

        @Test
        @DisplayName("Should calculate retention percentage")
        void calculatesRetentionPercentage() {
            // 2024 limit: $22,320
            // Earning $32,320 = $10,000 over limit
            // $5,000 reduction from $24,000 = $19,000 retained
            // Retention = 19000/24000 = 0.7917
            BigDecimal earnings = new BigDecimal("32320");
            BigDecimal benefit = new BigDecimal("24000");

            EarningsTestResult result = calculator.calculate(
                earnings, benefit, AGE_65_MONTHS, FRA_67_MONTHS, YEAR_2024);

            BigDecimal expectedRetention = new BigDecimal("0.7917");
            assertEquals(0, expectedRetention.compareTo(
                result.getRetentionPercentage().setScale(4, RoundingMode.HALF_UP)));
        }
    }

    @Nested
    @DisplayName("FRA Year Calculation")
    class FraYearCalculationTests {

        @Test
        @DisplayName("Should be exempt when no months before FRA")
        void exemptWhenNoMonthsBeforeFra() {
            BigDecimal earnings = new BigDecimal("100000");
            BigDecimal benefit = new BigDecimal("30000");

            EarningsTestResult result = calculator.calculateFraYear(
                earnings, benefit, 0, YEAR_2024);

            assertFalse(result.subjectToTest());
            assertEquals(0, benefit.compareTo(result.reducedBenefit()));
        }

        @Test
        @DisplayName("Should use $1/$3 formula in FRA year")
        void usesOneThirdFormulaInFraYear() {
            // 2024 FRA year limit: $59,520
            // 6 months before FRA
            // Monthly limit = $59,520 / 12 = $4,960
            // Prorated limit = $4,960 * 6 = $29,760
            // Prorated earnings = $80,000 * 6/12 = $40,000
            // Excess = $40,000 - $29,760 = $10,240
            // Reduction = $10,240 / 3 = $3,413.33
            BigDecimal earnings = new BigDecimal("80000");
            BigDecimal benefit = new BigDecimal("30000");

            EarningsTestResult result = calculator.calculateFraYear(
                earnings, benefit, 6, YEAR_2024);

            assertTrue(result.subjectToTest());
            assertTrue(result.isReduced());
            // Verify reduction uses 1/3 ratio (approximately)
            BigDecimal reductionRatio = result.reductionAmount()
                .divide(result.excessEarnings(), 2, RoundingMode.HALF_UP);
            assertEquals(0, new BigDecimal("0.33").compareTo(reductionRatio));
        }

        @Test
        @DisplayName("Should have no reduction when prorated earnings under prorated limit")
        void noReductionUnderProratedLimit() {
            // 2024 FRA year limit: $59,520
            // 6 months before FRA
            // Monthly limit = $59,520 / 12 = $4,960
            // Prorated limit = $4,960 * 6 = $29,760
            // Prorated earnings = $50,000 * 6/12 = $25,000 (under limit)
            BigDecimal earnings = new BigDecimal("50000");
            BigDecimal benefit = new BigDecimal("30000");

            EarningsTestResult result = calculator.calculateFraYear(
                earnings, benefit, 6, YEAR_2024);

            assertTrue(result.subjectToTest());
            assertFalse(result.isReduced());
        }
    }

    @Nested
    @DisplayName("Limit Retrieval")
    class LimitRetrievalTests {

        @Test
        @DisplayName("Should return correct 2024 below FRA limit")
        void returns2024BelowFraLimit() {
            BigDecimal limit = calculator.getBelowFraLimit(YEAR_2024);
            assertEquals(0, BELOW_FRA_LIMIT.compareTo(limit));
        }

        @Test
        @DisplayName("Should return correct 2024 FRA year limit")
        void returns2024FraYearLimit() {
            BigDecimal limit = calculator.getFraYearLimit(YEAR_2024);
            assertEquals(0, FRA_YEAR_LIMIT.compareTo(limit));
        }

        @Test
        @DisplayName("Should return default for unknown year")
        void returnsDefaultForUnknownYear() {
            // Should fall back to 2024 defaults
            BigDecimal limit = calculator.getBelowFraLimit(2030);
            assertEquals(0, new BigDecimal("22320").compareTo(limit));
        }
    }

    @Nested
    @DisplayName("Subject To Test Check")
    class SubjectToTestTests {

        @Test
        @DisplayName("Should be subject to test before FRA")
        void subjectBeforeFra() {
            assertTrue(calculator.isSubjectToTest(AGE_65_MONTHS, FRA_67_MONTHS));
        }

        @Test
        @DisplayName("Should not be subject to test at FRA")
        void notSubjectAtFra() {
            assertFalse(calculator.isSubjectToTest(FRA_67_MONTHS, FRA_67_MONTHS));
        }

        @Test
        @DisplayName("Should not be subject to test after FRA")
        void notSubjectAfterFra() {
            assertFalse(calculator.isSubjectToTest(816, FRA_67_MONTHS));  // 68 years
        }
    }

    @Nested
    @DisplayName("EarningsTestResult Helper Methods")
    class ResultHelperMethodTests {

        @Test
        @DisplayName("Should calculate monthly benefit")
        void calculatesMonthlyBenefit() {
            // 2024 limit: $22,320
            // Earning $32,320 = $10,000 over limit
            // Reduction = $5,000, reduced benefit = $19,000
            BigDecimal earnings = new BigDecimal("32320");
            BigDecimal benefit = new BigDecimal("24000");

            EarningsTestResult result = calculator.calculate(
                earnings, benefit, AGE_65_MONTHS, FRA_67_MONTHS, YEAR_2024);

            // $19,000 annual / 12 = $1,583.33
            BigDecimal expectedMonthly = new BigDecimal("1583.33");
            assertEquals(0, expectedMonthly.compareTo(result.getMonthlyBenefit()));
        }
    }
}
