package io.github.xmljim.retirement.domain.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("SocialSecurityCalculator Tests")
class SocialSecurityCalculatorTest {

    private SocialSecurityCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = CalculatorFactory.socialSecurityCalculator();
    }

    @Nested
    @DisplayName("FRA Calculation Tests")
    class FraCalculationTests {

        @ParameterizedTest(name = "Birth year {0} should have FRA of {1} months")
        @CsvSource({
            "1943, 792",  // 66 years
            "1950, 792",  // 66 years
            "1954, 792",  // 66 years
            "1955, 794",  // 66 years 2 months
            "1956, 796",  // 66 years 4 months
            "1957, 798",  // 66 years 6 months
            "1958, 800",  // 66 years 8 months
            "1959, 802",  // 66 years 10 months
            "1960, 804",  // 67 years
            "1970, 804",  // 67 years
            "1990, 804"   // 67 years
        })
        void fraByBirthYear(int birthYear, int expectedFraMonths) {
            assertEquals(expectedFraMonths, calculator.calculateFraMonths(birthYear));
        }

        @Test
        @DisplayName("Birth year before 1943 should still return 66 years")
        void birthYearBefore1943() {
            assertEquals(792, calculator.calculateFraMonths(1940));
        }
    }

    @Nested
    @DisplayName("Early Claiming Reduction Tests")
    class EarlyClaimingTests {

        @Test
        @DisplayName("Zero months early should return zero reduction")
        void zeroMonthsEarly() {
            assertEquals(0, BigDecimal.ZERO.compareTo(calculator.calculateEarlyReduction(0)));
        }

        @Test
        @DisplayName("Negative months should return zero reduction")
        void negativeMonths() {
            assertEquals(0, BigDecimal.ZERO.compareTo(calculator.calculateEarlyReduction(-5)));
        }

        @Test
        @DisplayName("12 months early (1 year) should reduce by ~6.67%")
        void oneYearEarly() {
            BigDecimal reduction = calculator.calculateEarlyReduction(12);
            // 12 * (5/9 / 100) = 12 * 0.005556 = 0.0667
            // Allow for small precision differences
            BigDecimal minExpected = new BigDecimal("0.0666");
            BigDecimal maxExpected = new BigDecimal("0.0668");
            assertTrue(reduction.compareTo(minExpected) >= 0,
                "Reduction " + reduction + " should be >= " + minExpected);
            assertTrue(reduction.compareTo(maxExpected) <= 0,
                "Reduction " + reduction + " should be <= " + maxExpected);
        }

        @Test
        @DisplayName("36 months early (3 years) should reduce by 20%")
        void threeYearsEarly() {
            BigDecimal reduction = calculator.calculateEarlyReduction(36);
            // 36 * (5/9 / 100) = 0.20
            BigDecimal expected = new BigDecimal("0.20");
            assertEquals(0, expected.setScale(2, RoundingMode.HALF_UP)
                .compareTo(reduction.setScale(2, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("60 months early (5 years, at 62 for FRA 67) should reduce by ~30%")
        void fiveYearsEarly() {
            BigDecimal reduction = calculator.calculateEarlyReduction(60);
            // First 36: 36 * 5/900 = 0.20
            // Additional 24: 24 * 5/1200 = 0.10
            // Total: 0.30
            BigDecimal expected = new BigDecimal("0.30");
            assertEquals(0, expected.setScale(2, RoundingMode.HALF_UP)
                .compareTo(reduction.setScale(2, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("48 months early should have correct two-tier reduction")
        void fourYearsEarly() {
            BigDecimal reduction = calculator.calculateEarlyReduction(48);
            // First 36: 36 * 5/900 = 0.20
            // Additional 12: 12 * 5/1200 = 0.05
            // Total: 0.25
            BigDecimal expected = new BigDecimal("0.25");
            assertEquals(0, expected.setScale(2, RoundingMode.HALF_UP)
                .compareTo(reduction.setScale(2, RoundingMode.HALF_UP)));
        }
    }

    @Nested
    @DisplayName("Delayed Claiming Credits Tests")
    class DelayedClaimingTests {

        @Test
        @DisplayName("Zero months delayed should return zero credits")
        void zeroMonthsDelayed() {
            assertEquals(0, BigDecimal.ZERO.compareTo(calculator.calculateDelayedCredits(0)));
        }

        @Test
        @DisplayName("Negative months should return zero credits")
        void negativeMonths() {
            assertEquals(0, BigDecimal.ZERO.compareTo(calculator.calculateDelayedCredits(-5)));
        }

        @Test
        @DisplayName("12 months delayed (1 year) should increase by 8%")
        void oneYearDelayed() {
            BigDecimal credit = calculator.calculateDelayedCredits(12);
            // 12 * (8/12 / 100) = 0.08
            BigDecimal expected = new BigDecimal("0.08");
            assertEquals(0, expected.setScale(2, RoundingMode.HALF_UP)
                .compareTo(credit.setScale(2, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("36 months delayed (3 years, to age 70 from FRA 67) should increase by 24%")
        void threeYearsDelayed() {
            BigDecimal credit = calculator.calculateDelayedCredits(36);
            // 36 * (8/12 / 100) = 0.24
            BigDecimal expected = new BigDecimal("0.24");
            assertEquals(0, expected.setScale(2, RoundingMode.HALF_UP)
                .compareTo(credit.setScale(2, RoundingMode.HALF_UP)));
        }
    }

    @Nested
    @DisplayName("Adjusted Benefit Calculation Tests")
    class AdjustedBenefitTests {

        private static final BigDecimal FRA_BENEFIT = new BigDecimal("2000");

        @Test
        @DisplayName("Claiming at FRA should return full benefit")
        void claimingAtFra() {
            BigDecimal adjusted = calculator.calculateAdjustedBenefit(FRA_BENEFIT, 804, 804);
            assertEquals(0, FRA_BENEFIT.compareTo(adjusted));
        }

        @Test
        @DisplayName("Early claiming should reduce benefit")
        void earlyClaiming() {
            // 60 months early = ~30% reduction
            BigDecimal adjusted = calculator.calculateAdjustedBenefit(FRA_BENEFIT, 804, 744);
            BigDecimal expected = new BigDecimal("1400"); // 2000 * 0.70
            assertEquals(0, expected.setScale(0, RoundingMode.HALF_UP)
                .compareTo(adjusted.setScale(0, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Delayed claiming should increase benefit")
        void delayedClaiming() {
            // 36 months delayed = 24% increase
            BigDecimal adjusted = calculator.calculateAdjustedBenefit(FRA_BENEFIT, 804, 840);
            BigDecimal expected = new BigDecimal("2480"); // 2000 * 1.24
            assertEquals(0, expected.setScale(0, RoundingMode.HALF_UP)
                .compareTo(adjusted.setScale(0, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Null benefit should return zero")
        void nullBenefit() {
            BigDecimal adjusted = calculator.calculateAdjustedBenefit(null, 804, 804);
            assertEquals(0, BigDecimal.ZERO.compareTo(adjusted));
        }

        @Test
        @DisplayName("Zero benefit should return zero")
        void zeroBenefit() {
            BigDecimal adjusted = calculator.calculateAdjustedBenefit(BigDecimal.ZERO, 804, 804);
            assertEquals(0, BigDecimal.ZERO.compareTo(adjusted));
        }
    }

    @Nested
    @DisplayName("COLA Adjustment Tests")
    class ColaAdjustmentTests {

        private static final BigDecimal BENEFIT = new BigDecimal("2000");
        private static final BigDecimal COLA_RATE = new BigDecimal("0.028"); // 2.8%

        @Test
        @DisplayName("Zero years should return original benefit")
        void zeroYears() {
            BigDecimal adjusted = calculator.applyColaAdjustment(BENEFIT, COLA_RATE, 0);
            assertEquals(0, BENEFIT.compareTo(adjusted));
        }

        @Test
        @DisplayName("One year COLA should compound correctly")
        void oneYear() {
            BigDecimal adjusted = calculator.applyColaAdjustment(BENEFIT, COLA_RATE, 1);
            // 2000 * 1.028 = 2056
            BigDecimal expected = new BigDecimal("2056");
            assertEquals(0, expected.setScale(0, RoundingMode.HALF_UP)
                .compareTo(adjusted.setScale(0, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Five years COLA should compound correctly")
        void fiveYears() {
            BigDecimal adjusted = calculator.applyColaAdjustment(BENEFIT, COLA_RATE, 5);
            // 2000 * (1.028)^5 = 2000 * 1.1487 = 2297.40
            assertTrue(adjusted.compareTo(new BigDecimal("2295")) > 0);
            assertTrue(adjusted.compareTo(new BigDecimal("2300")) < 0);
        }

        @Test
        @DisplayName("Null benefit should return zero")
        void nullBenefit() {
            BigDecimal adjusted = calculator.applyColaAdjustment(null, COLA_RATE, 5);
            assertEquals(0, BigDecimal.ZERO.compareTo(adjusted));
        }

        @Test
        @DisplayName("Null COLA rate should return original benefit")
        void nullColaRate() {
            BigDecimal adjusted = calculator.applyColaAdjustment(BENEFIT, null, 5);
            assertEquals(0, BENEFIT.compareTo(adjusted));
        }
    }
}
