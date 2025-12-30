package io.github.xmljim.retirement.simulation.result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AnnualSummary")
class AnnualSummaryTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create with valid year")
        void shouldCreateWithValidYear() {
            AnnualSummary summary = AnnualSummary.builder(2025).build();

            assertEquals(2025, summary.year());
        }

        @Test
        @DisplayName("should reject year before 1900")
        void shouldRejectYearBefore1900() {
            assertThrows(IllegalArgumentException.class, () ->
                    AnnualSummary.builder(1899).build());
        }

        @Test
        @DisplayName("should reject year after 2200")
        void shouldRejectYearAfter2200() {
            assertThrows(IllegalArgumentException.class, () ->
                    AnnualSummary.builder(2201).build());
        }

        @Test
        @DisplayName("should default null values to zero")
        void shouldDefaultNullValues() {
            AnnualSummary summary = new AnnualSummary(
                    2025, null, null, null, null, null, null, null, null, null, null);

            assertEquals(BigDecimal.ZERO, summary.startingBalance());
            assertEquals(BigDecimal.ZERO, summary.endingBalance());
            assertEquals(BigDecimal.ZERO, summary.totalContributions());
            assertEquals(BigDecimal.ZERO, summary.totalWithdrawals());
            assertEquals(BigDecimal.ZERO, summary.totalIncome());
            assertEquals(BigDecimal.ZERO, summary.totalExpenses());
            assertEquals(BigDecimal.ZERO, summary.totalTaxesPaid());
            assertEquals(BigDecimal.ZERO, summary.annualReturn());
            assertEquals(BigDecimal.ZERO, summary.annualReturnPercent());
            assertTrue(summary.significantEvents().isEmpty());
        }

        @Test
        @DisplayName("should create immutable events list")
        void shouldCreateImmutableEventsList() {
            AnnualSummary summary = AnnualSummary.builder(2025)
                    .significantEvents(List.of("Retirement", "SS Start"))
                    .build();

            assertThrows(UnsupportedOperationException.class, () ->
                    summary.significantEvents().add("Event3"));
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should build with all values")
        void shouldBuildWithAllValues() {
            AnnualSummary summary = AnnualSummary.builder(2025)
                    .startingBalance(new BigDecimal("500000"))
                    .endingBalance(new BigDecimal("550000"))
                    .totalContributions(new BigDecimal("24000"))
                    .totalWithdrawals(BigDecimal.ZERO)
                    .totalIncome(new BigDecimal("100000"))
                    .totalExpenses(new BigDecimal("75000"))
                    .totalTaxesPaid(new BigDecimal("15000"))
                    .annualReturn(new BigDecimal("26000"))
                    .annualReturnPercent(new BigDecimal("0.0495"))
                    .significantEvents(List.of("MaxContribution"))
                    .build();

            assertEquals(2025, summary.year());
            assertEquals(new BigDecimal("500000"), summary.startingBalance());
            assertEquals(new BigDecimal("550000"), summary.endingBalance());
            assertEquals(new BigDecimal("24000"), summary.totalContributions());
            assertEquals(BigDecimal.ZERO, summary.totalWithdrawals());
            assertEquals(new BigDecimal("100000"), summary.totalIncome());
            assertEquals(new BigDecimal("75000"), summary.totalExpenses());
            assertEquals(new BigDecimal("15000"), summary.totalTaxesPaid());
            assertEquals(new BigDecimal("26000"), summary.annualReturn());
            assertEquals(new BigDecimal("0.0495"), summary.annualReturnPercent());
            assertEquals(List.of("MaxContribution"), summary.significantEvents());
        }
    }

    @Nested
    @DisplayName("Computed Metrics")
    class ComputedMetrics {

        @Test
        @DisplayName("netBalanceChange should be ending minus starting")
        void netBalanceChange() {
            AnnualSummary summary = AnnualSummary.builder(2025)
                    .startingBalance(new BigDecimal("500000"))
                    .endingBalance(new BigDecimal("550000"))
                    .build();

            assertEquals(new BigDecimal("50000.00"), summary.netBalanceChange());
        }

        @Test
        @DisplayName("netContributions should be contributions minus withdrawals")
        void netContributions() {
            AnnualSummary summary = AnnualSummary.builder(2025)
                    .totalContributions(new BigDecimal("24000"))
                    .totalWithdrawals(new BigDecimal("5000"))
                    .build();

            assertEquals(new BigDecimal("19000.00"), summary.netContributions());
        }

        @Test
        @DisplayName("netSavings should be income minus expenses")
        void netSavings() {
            AnnualSummary summary = AnnualSummary.builder(2025)
                    .totalIncome(new BigDecimal("100000"))
                    .totalExpenses(new BigDecimal("75000"))
                    .build();

            assertEquals(new BigDecimal("25000.00"), summary.netSavings());
        }

        @Test
        @DisplayName("effectiveTaxRate should be taxes divided by income")
        void effectiveTaxRate() {
            AnnualSummary summary = AnnualSummary.builder(2025)
                    .totalIncome(new BigDecimal("100000"))
                    .totalTaxesPaid(new BigDecimal("22000"))
                    .build();

            assertEquals(new BigDecimal("0.2200"), summary.effectiveTaxRate());
        }

        @Test
        @DisplayName("effectiveTaxRate should return zero when no income")
        void effectiveTaxRateNoIncome() {
            AnnualSummary summary = AnnualSummary.builder(2025).build();

            assertEquals(BigDecimal.ZERO, summary.effectiveTaxRate());
        }
    }

    @Nested
    @DisplayName("Status Checks")
    class StatusChecks {

        @Test
        @DisplayName("hadSignificantEvents should return true when events exist")
        void hadSignificantEventsTrue() {
            AnnualSummary summary = AnnualSummary.builder(2025)
                    .significantEvents(List.of("Retirement"))
                    .build();

            assertTrue(summary.hadSignificantEvents());
        }

        @Test
        @DisplayName("hadSignificantEvents should return false when no events")
        void hadSignificantEventsFalse() {
            AnnualSummary summary = AnnualSummary.builder(2025).build();

            assertFalse(summary.hadSignificantEvents());
        }

        @Test
        @DisplayName("hadGrowth should return true when ending > starting")
        void hadGrowthTrue() {
            AnnualSummary summary = AnnualSummary.builder(2025)
                    .startingBalance(new BigDecimal("500000"))
                    .endingBalance(new BigDecimal("550000"))
                    .build();

            assertTrue(summary.hadGrowth());
        }

        @Test
        @DisplayName("hadGrowth should return false when ending <= starting")
        void hadGrowthFalse() {
            AnnualSummary summary = AnnualSummary.builder(2025)
                    .startingBalance(new BigDecimal("500000"))
                    .endingBalance(new BigDecimal("450000"))
                    .build();

            assertFalse(summary.hadGrowth());
        }

        @Test
        @DisplayName("isAccumulating should return true when contributions > withdrawals")
        void isAccumulatingTrue() {
            AnnualSummary summary = AnnualSummary.builder(2025)
                    .totalContributions(new BigDecimal("24000"))
                    .totalWithdrawals(BigDecimal.ZERO)
                    .build();

            assertTrue(summary.isAccumulating());
        }

        @Test
        @DisplayName("isAccumulating should return false when contributions <= withdrawals")
        void isAccumulatingFalse() {
            AnnualSummary summary = AnnualSummary.builder(2025)
                    .totalContributions(BigDecimal.ZERO)
                    .totalWithdrawals(new BigDecimal("50000"))
                    .build();

            assertFalse(summary.isAccumulating());
        }

        @Test
        @DisplayName("isDistributing should return true when withdrawals > contributions")
        void isDistributingTrue() {
            AnnualSummary summary = AnnualSummary.builder(2025)
                    .totalContributions(BigDecimal.ZERO)
                    .totalWithdrawals(new BigDecimal("50000"))
                    .build();

            assertTrue(summary.isDistributing());
        }

        @Test
        @DisplayName("isDistributing should return false when withdrawals <= contributions")
        void isDistributingFalse() {
            AnnualSummary summary = AnnualSummary.builder(2025)
                    .totalContributions(new BigDecimal("24000"))
                    .totalWithdrawals(BigDecimal.ZERO)
                    .build();

            assertFalse(summary.isDistributing());
        }
    }
}
