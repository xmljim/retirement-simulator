package io.github.xmljim.retirement.simulation.result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TaxSummary")
class TaxSummaryTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create empty summary with all zeros")
        void shouldCreateEmpty() {
            TaxSummary summary = TaxSummary.empty();

            assertEquals(BigDecimal.ZERO, summary.taxableIncome());
            assertEquals(BigDecimal.ZERO, summary.taxableSSIncome());
            assertEquals(BigDecimal.ZERO, summary.taxableWithdrawals());
            assertEquals(BigDecimal.ZERO, summary.taxFreeWithdrawals());
            assertEquals(BigDecimal.ZERO, summary.federalTaxLiability());
            assertEquals(BigDecimal.ZERO, summary.effectiveTaxRate());
            assertEquals(BigDecimal.ZERO, summary.marginalTaxBracket());
            assertEquals(BigDecimal.ZERO, summary.rothConversionAmount());
            assertEquals(BigDecimal.ZERO, summary.rothConversionTax());
        }

        @Test
        @DisplayName("should default null values to zero")
        void shouldDefaultNullsToZero() {
            TaxSummary summary = new TaxSummary(
                    null, null, null, null, null,
                    null, null, null, null
            );

            assertEquals(BigDecimal.ZERO, summary.taxableIncome());
            assertEquals(BigDecimal.ZERO, summary.federalTaxLiability());
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should build with all values")
        void shouldBuildWithAllValues() {
            TaxSummary summary = TaxSummary.builder()
                    .taxableIncome(new BigDecimal("50000"))
                    .taxableSSIncome(new BigDecimal("15000"))
                    .taxableWithdrawals(new BigDecimal("20000"))
                    .taxFreeWithdrawals(new BigDecimal("5000"))
                    .federalTaxLiability(new BigDecimal("8000"))
                    .effectiveTaxRate(new BigDecimal("0.16"))
                    .marginalTaxBracket(new BigDecimal("0.22"))
                    .rothConversionAmount(new BigDecimal("10000"))
                    .rothConversionTax(new BigDecimal("2200"))
                    .build();

            assertEquals(new BigDecimal("50000"), summary.taxableIncome());
            assertEquals(new BigDecimal("15000"), summary.taxableSSIncome());
            assertEquals(new BigDecimal("20000"), summary.taxableWithdrawals());
            assertEquals(new BigDecimal("5000"), summary.taxFreeWithdrawals());
            assertEquals(new BigDecimal("8000"), summary.federalTaxLiability());
            assertEquals(new BigDecimal("0.16"), summary.effectiveTaxRate());
            assertEquals(new BigDecimal("0.22"), summary.marginalTaxBracket());
            assertEquals(new BigDecimal("10000"), summary.rothConversionAmount());
            assertEquals(new BigDecimal("2200"), summary.rothConversionTax());
        }
    }

    @Nested
    @DisplayName("Calculations")
    class Calculations {

        @Test
        @DisplayName("totalWithdrawals should sum taxable and tax-free")
        void totalWithdrawalsCalculation() {
            TaxSummary summary = TaxSummary.builder()
                    .taxableWithdrawals(new BigDecimal("20000"))
                    .taxFreeWithdrawals(new BigDecimal("5000"))
                    .build();

            assertEquals(new BigDecimal("25000.00"), summary.totalWithdrawals());
        }

        @Test
        @DisplayName("totalTaxLiability should sum federal and Roth conversion")
        void totalTaxLiabilityCalculation() {
            TaxSummary summary = TaxSummary.builder()
                    .federalTaxLiability(new BigDecimal("8000"))
                    .rothConversionTax(new BigDecimal("2200"))
                    .build();

            assertEquals(new BigDecimal("10200.00"), summary.totalTaxLiability());
        }

        @Test
        @DisplayName("taxableWithdrawalPercentage should calculate correctly")
        void taxableWithdrawalPercentage() {
            TaxSummary summary = TaxSummary.builder()
                    .taxableWithdrawals(new BigDecimal("15000"))
                    .taxFreeWithdrawals(new BigDecimal("5000"))
                    .build();

            // 15000 / 20000 = 0.75
            assertEquals(new BigDecimal("0.7500"), summary.taxableWithdrawalPercentage());
        }

        @Test
        @DisplayName("taxableWithdrawalPercentage should return zero when no withdrawals")
        void taxableWithdrawalPercentageNoWithdrawals() {
            TaxSummary summary = TaxSummary.empty();

            assertEquals(BigDecimal.ZERO, summary.taxableWithdrawalPercentage());
        }
    }

    @Nested
    @DisplayName("Status Checks")
    class StatusChecks {

        @Test
        @DisplayName("hadRothConversion should return true when amount > 0")
        void hadRothConversionTrue() {
            TaxSummary summary = TaxSummary.builder()
                    .rothConversionAmount(new BigDecimal("10000"))
                    .build();

            assertTrue(summary.hadRothConversion());
        }

        @Test
        @DisplayName("hadRothConversion should return false when amount = 0")
        void hadRothConversionFalse() {
            TaxSummary summary = TaxSummary.empty();

            assertFalse(summary.hadRothConversion());
        }

        @Test
        @DisplayName("hasTaxLiability should return true when tax > 0")
        void hasTaxLiabilityTrue() {
            TaxSummary summary = TaxSummary.builder()
                    .federalTaxLiability(new BigDecimal("5000"))
                    .build();

            assertTrue(summary.hasTaxLiability());
        }

        @Test
        @DisplayName("hasTaxLiability should return false when tax = 0")
        void hasTaxLiabilityFalse() {
            TaxSummary summary = TaxSummary.empty();

            assertFalse(summary.hasTaxLiability());
        }
    }
}
