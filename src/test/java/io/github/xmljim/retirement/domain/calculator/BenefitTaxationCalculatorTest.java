package io.github.xmljim.retirement.domain.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.FilingStatus;
import io.github.xmljim.retirement.domain.value.TaxationResult;

/**
 * Tests for the BenefitTaxationCalculator.
 *
 * <p>Test scenarios based on IRS Publication 915:
 * <ul>
 *   <li>Single: $25K (50%) / $34K (85%)</li>
 *   <li>MFJ: $32K (50%) / $44K (85%)</li>
 *   <li>MFS living with spouse: $0 - always 85%</li>
 * </ul>
 */
@DisplayName("BenefitTaxationCalculator Tests")
class BenefitTaxationCalculatorTest {

    private BenefitTaxationCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = CalculatorFactory.benefitTaxationCalculator();
    }

    @Nested
    @DisplayName("Single Filer Tests")
    class SingleFilerTests {

        @Test
        @DisplayName("Should have 0% taxable below $25K threshold")
        void zeroTaxableBelowThreshold() {
            BigDecimal ssBenefit = new BigDecimal("20000");
            BigDecimal agi = new BigDecimal("10000");
            BigDecimal interest = BigDecimal.ZERO;

            TaxationResult result = calculator.calculate(
                ssBenefit, agi, interest, FilingStatus.SINGLE);

            // Combined = 10000 + 0 + (20000 * 0.5) = 20000 < 25000
            assertFalse(result.isTaxable());
            assertEquals(TaxationResult.TaxationTier.NONE, result.tier());
        }

        @Test
        @DisplayName("Should have up to 50% taxable between thresholds")
        void fiftyPercentBetweenThresholds() {
            BigDecimal ssBenefit = new BigDecimal("20000");
            BigDecimal agi = new BigDecimal("20000");
            BigDecimal interest = BigDecimal.ZERO;

            TaxationResult result = calculator.calculate(
                ssBenefit, agi, interest, FilingStatus.SINGLE);

            // Combined = 20000 + 0 + 10000 = 30000 (between 25K and 34K)
            assertTrue(result.isTaxable());
            assertEquals(TaxationResult.TaxationTier.FIFTY_PERCENT, result.tier());
        }

        @Test
        @DisplayName("Should have up to 85% taxable above upper threshold")
        void eightyFivePercentAboveThreshold() {
            BigDecimal ssBenefit = new BigDecimal("24000");
            BigDecimal agi = new BigDecimal("50000");
            BigDecimal interest = BigDecimal.ZERO;

            TaxationResult result = calculator.calculate(
                ssBenefit, agi, interest, FilingStatus.SINGLE);

            // Combined = 50000 + 0 + 12000 = 62000 > 34000
            assertTrue(result.isTaxable());
            assertEquals(TaxationResult.TaxationTier.EIGHTY_FIVE_PERCENT, result.tier());
        }
    }

    @Nested
    @DisplayName("MFJ Filer Tests")
    class MfjFilerTests {

        @Test
        @DisplayName("Should have 0% taxable below $32K threshold")
        void zeroTaxableBelowThreshold() {
            BigDecimal ssBenefit = new BigDecimal("20000");
            BigDecimal agi = new BigDecimal("18000");
            BigDecimal interest = BigDecimal.ZERO;

            TaxationResult result = calculator.calculate(
                ssBenefit, agi, interest, FilingStatus.MARRIED_FILING_JOINTLY);

            // Combined = 18000 + 0 + 10000 = 28000 < 32000
            assertFalse(result.isTaxable());
            assertEquals(TaxationResult.TaxationTier.NONE, result.tier());
        }

        @Test
        @DisplayName("Should have up to 50% taxable between thresholds")
        void fiftyPercentBetweenThresholds() {
            BigDecimal ssBenefit = new BigDecimal("30000");
            BigDecimal agi = new BigDecimal("25000");
            BigDecimal interest = BigDecimal.ZERO;

            TaxationResult result = calculator.calculate(
                ssBenefit, agi, interest, FilingStatus.MARRIED_FILING_JOINTLY);

            // Combined = 25000 + 0 + 15000 = 40000 (between 32K and 44K)
            assertTrue(result.isTaxable());
            assertEquals(TaxationResult.TaxationTier.FIFTY_PERCENT, result.tier());
        }
    }

    @Nested
    @DisplayName("MFS Living With Spouse Tests")
    class MfsLivingWithSpouseTests {

        @Test
        @DisplayName("Should always be 85% tier when MFS living with spouse")
        void alwaysEightyFivePercent() {
            BigDecimal ssBenefit = new BigDecimal("20000");
            BigDecimal agi = new BigDecimal("5000");  // Very low income
            BigDecimal interest = BigDecimal.ZERO;

            TaxationResult result = calculator.calculate(
                ssBenefit, agi, interest, FilingStatus.MARRIED_FILING_SEPARATELY, true);

            assertTrue(result.isTaxable());
            assertEquals(TaxationResult.TaxationTier.EIGHTY_FIVE_PERCENT, result.tier());
        }
    }

    @Nested
    @DisplayName("MFS Not Living With Spouse Tests")
    class MfsNotLivingWithSpouseTests {

        @Test
        @DisplayName("Should use Single thresholds when MFS not living with spouse")
        void usesSingleThresholds() {
            BigDecimal ssBenefit = new BigDecimal("20000");
            BigDecimal agi = new BigDecimal("10000");
            BigDecimal interest = BigDecimal.ZERO;

            TaxationResult result = calculator.calculate(
                ssBenefit, agi, interest, FilingStatus.MARRIED_FILING_SEPARATELY, false);

            // Combined = 20000 < 25000 (Single threshold)
            assertFalse(result.isTaxable());
            assertEquals(TaxationResult.TaxationTier.NONE, result.tier());
        }
    }

    @Nested
    @DisplayName("Combined Income Calculation")
    class CombinedIncomeTests {

        @Test
        @DisplayName("Should calculate combined income correctly")
        void calculatesCombinedIncome() {
            BigDecimal ssBenefit = new BigDecimal("24000");
            BigDecimal agi = new BigDecimal("30000");
            BigDecimal interest = new BigDecimal("2000");

            BigDecimal combined = calculator.calculateCombinedIncome(
                ssBenefit, agi, interest);

            // 30000 + 2000 + (24000 * 0.5) = 44000
            assertEquals(0, new BigDecimal("44000").compareTo(combined));
        }
    }

    @Nested
    @DisplayName("Threshold Retrieval")
    class ThresholdTests {

        @Test
        @DisplayName("Should return Single thresholds for SINGLE")
        void singleThresholds() {
            assertEquals(0, new BigDecimal("25000").compareTo(
                calculator.getLowerThreshold(FilingStatus.SINGLE)));
            assertEquals(0, new BigDecimal("34000").compareTo(
                calculator.getUpperThreshold(FilingStatus.SINGLE)));
        }

        @Test
        @DisplayName("Should return Joint thresholds for MFJ")
        void jointThresholds() {
            assertEquals(0, new BigDecimal("32000").compareTo(
                calculator.getLowerThreshold(FilingStatus.MARRIED_FILING_JOINTLY)));
            assertEquals(0, new BigDecimal("44000").compareTo(
                calculator.getUpperThreshold(FilingStatus.MARRIED_FILING_JOINTLY)));
        }
    }
}
