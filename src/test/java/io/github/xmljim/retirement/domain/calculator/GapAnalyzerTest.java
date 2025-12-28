package io.github.xmljim.retirement.domain.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.calculator.impl.DefaultGapAnalyzer;
import io.github.xmljim.retirement.domain.enums.ExpenseCategory;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.value.ExpenseBreakdown;
import io.github.xmljim.retirement.domain.value.GapAnalysis;
import io.github.xmljim.retirement.domain.value.IncomeBreakdown;

@DisplayName("GapAnalyzer Tests")
class GapAnalyzerTest {

    private static final LocalDate TEST_DATE = LocalDate.of(2025, 6, 15);

    private GapAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new DefaultGapAnalyzer();
    }

    private IncomeBreakdown createIncome(BigDecimal total) {
        return new IncomeBreakdown.Builder()
                .asOfDate(TEST_DATE)
                .salary(total)
                .earnedIncome(total)
                .build();
    }

    private ExpenseBreakdown createExpenses(BigDecimal total) {
        return ExpenseBreakdown.builder(TEST_DATE)
                .addExpense(ExpenseCategory.HOUSING, total)
                .build();
    }

    @Nested
    @DisplayName("Monthly Analysis Tests")
    class MonthlyAnalysisTests {

        @Test
        @DisplayName("Detects deficit when expenses exceed income")
        void detectsDeficit() {
            IncomeBreakdown income = createIncome(new BigDecimal("5000"));
            ExpenseBreakdown expenses = createExpenses(new BigDecimal("7000"));

            GapAnalysis result = analyzer.analyzeMonthly(income, expenses, TEST_DATE);

            assertTrue(result.hasDeficit());
            assertFalse(result.hasSurplus());
            assertEquals(0, new BigDecimal("-2000").compareTo(result.gap()));
            assertEquals(0, new BigDecimal("2000").compareTo(result.withdrawalNeeded()));
            assertEquals(0, BigDecimal.ZERO.compareTo(result.surplusToSave()));
        }

        @Test
        @DisplayName("Detects surplus when income exceeds expenses")
        void detectsSurplus() {
            IncomeBreakdown income = createIncome(new BigDecimal("8000"));
            ExpenseBreakdown expenses = createExpenses(new BigDecimal("5000"));

            GapAnalysis result = analyzer.analyzeMonthly(income, expenses, TEST_DATE);

            assertTrue(result.hasSurplus());
            assertFalse(result.hasDeficit());
            assertEquals(0, new BigDecimal("3000").compareTo(result.gap()));
            assertEquals(0, BigDecimal.ZERO.compareTo(result.withdrawalNeeded()));
            assertEquals(0, new BigDecimal("3000").compareTo(result.surplusToSave()));
        }

        @Test
        @DisplayName("Detects break-even when income equals expenses")
        void detectsBreakEven() {
            IncomeBreakdown income = createIncome(new BigDecimal("5000"));
            ExpenseBreakdown expenses = createExpenses(new BigDecimal("5000"));

            GapAnalysis result = analyzer.analyzeMonthly(income, expenses, TEST_DATE);

            assertTrue(result.isBreakEven());
            assertFalse(result.hasDeficit());
            assertFalse(result.hasSurplus());
        }

        @Test
        @DisplayName("Throws when income is null")
        void throwsWhenIncomeNull() {
            ExpenseBreakdown expenses = createExpenses(new BigDecimal("5000"));

            assertThrows(MissingRequiredFieldException.class, () ->
                    analyzer.analyzeMonthly(null, expenses, TEST_DATE));
        }
    }

    @Nested
    @DisplayName("Annual Analysis Tests")
    class AnnualAnalysisTests {

        @Test
        @DisplayName("Annualizes monthly amounts")
        void annualizesAmounts() {
            IncomeBreakdown income = createIncome(new BigDecimal("5000"));
            ExpenseBreakdown expenses = createExpenses(new BigDecimal("4000"));

            GapAnalysis result = analyzer.analyzeAnnual(income, expenses, TEST_DATE);

            assertEquals(0, new BigDecimal("60000").compareTo(result.totalIncome()));
            assertEquals(0, new BigDecimal("48000").compareTo(result.totalExpenses()));
            assertEquals(0, new BigDecimal("12000").compareTo(result.gap()));
        }
    }

    @Nested
    @DisplayName("Tax-Aware Withdrawal Tests")
    class TaxAwareTests {

        @Test
        @DisplayName("Grosses up withdrawal for taxes")
        void grossesUpForTaxes() {
            BigDecimal netNeeded = new BigDecimal("2000");
            BigDecimal taxRate = new BigDecimal("0.22");

            BigDecimal gross = analyzer.calculateGrossWithdrawal(netNeeded, taxRate);

            // $2000 / (1 - 0.22) = $2564.10
            assertEquals(0, new BigDecimal("2564.10").compareTo(gross));
        }

        @Test
        @DisplayName("Returns net when tax rate is zero")
        void returnsNetWhenNoTax() {
            BigDecimal netNeeded = new BigDecimal("2000");

            BigDecimal gross = analyzer.calculateGrossWithdrawal(netNeeded, BigDecimal.ZERO);

            assertEquals(0, new BigDecimal("2000.00").compareTo(gross));
        }

        @Test
        @DisplayName("Returns zero when net needed is zero")
        void returnsZeroWhenNoNeed() {
            BigDecimal gross = analyzer.calculateGrossWithdrawal(BigDecimal.ZERO, new BigDecimal("0.22"));

            assertEquals(0, BigDecimal.ZERO.compareTo(gross));
        }

        @Test
        @DisplayName("Throws when tax rate >= 100%")
        void throwsWhenTaxRateTooHigh() {
            assertThrows(IllegalArgumentException.class, () ->
                    analyzer.calculateGrossWithdrawal(new BigDecimal("1000"), BigDecimal.ONE));
        }

        @Test
        @DisplayName("GapAnalysis grossWithdrawalNeeded works")
        void gapAnalysisGrossUpWorks() {
            GapAnalysis analysis = GapAnalysis.of(TEST_DATE,
                    new BigDecimal("5000"), new BigDecimal("7000"));

            BigDecimal gross = analysis.grossWithdrawalNeeded(new BigDecimal("0.22"));

            // $2000 / (1 - 0.22) = $2564.10
            assertEquals(0, new BigDecimal("2564.10").compareTo(gross));
        }
    }

    @Nested
    @DisplayName("Year Projection Tests")
    class ProjectionTests {

        @Test
        @DisplayName("Projects 12 months of gap analysis")
        void projectsTwelveMonths() {
            List<GapAnalysis> projections = analyzer.projectYear(
                    date -> createIncome(new BigDecimal("5000")),
                    date -> createExpenses(new BigDecimal("4500")),
                    TEST_DATE
            );

            assertEquals(12, projections.size());

            // Verify dates are consecutive months
            for (int i = 0; i < 12; i++) {
                LocalDate expectedDate = TEST_DATE.plusMonths(i);
                assertEquals(expectedDate, projections.get(i).asOfDate());
            }
        }

        @Test
        @DisplayName("Summarizes year of analyses")
        void summarizesYear() {
            List<GapAnalysis> projections = analyzer.projectYear(
                    date -> createIncome(new BigDecimal("5000")),
                    date -> createExpenses(new BigDecimal("4000")),
                    TEST_DATE
            );

            GapAnalysis summary = analyzer.summarize(projections);

            assertEquals(0, new BigDecimal("60000").compareTo(summary.totalIncome()));
            assertEquals(0, new BigDecimal("48000").compareTo(summary.totalExpenses()));
            assertEquals(0, new BigDecimal("12000").compareTo(summary.surplusToSave()));
        }
    }

    @Nested
    @DisplayName("GapAnalysis Record Tests")
    class GapAnalysisRecordTests {

        @Test
        @DisplayName("Coverage ratio calculated correctly")
        void coverageRatioWorks() {
            GapAnalysis analysis = GapAnalysis.of(TEST_DATE,
                    new BigDecimal("6000"), new BigDecimal("5000"));

            // 6000 / 5000 = 1.2
            assertEquals(0, new BigDecimal("1.2000").compareTo(analysis.coverageRatio()));
        }

        @Test
        @DisplayName("Coverage ratio handles zero expenses")
        void coverageRatioZeroExpenses() {
            GapAnalysis analysis = GapAnalysis.of(TEST_DATE,
                    new BigDecimal("5000"), BigDecimal.ZERO);

            assertTrue(analysis.coverageRatio().compareTo(BigDecimal.valueOf(1000000)) > 0);
        }

        @Test
        @DisplayName("Coverage ratio handles zero income and expenses")
        void coverageRatioZeroBoth() {
            GapAnalysis analysis = GapAnalysis.of(TEST_DATE, BigDecimal.ZERO, BigDecimal.ZERO);

            assertEquals(0, BigDecimal.ZERO.compareTo(analysis.coverageRatio()));
        }

        @Test
        @DisplayName("Empty analysis has all zeros")
        void emptyAnalysis() {
            GapAnalysis empty = GapAnalysis.empty(TEST_DATE);

            assertEquals(0, BigDecimal.ZERO.compareTo(empty.totalIncome()));
            assertEquals(0, BigDecimal.ZERO.compareTo(empty.totalExpenses()));
            assertTrue(empty.isBreakEven());
        }

        @Test
        @DisplayName("grossWithdrawalNeeded with null tax rate returns withdrawal")
        void grossWithdrawalNullTaxRate() {
            GapAnalysis analysis = GapAnalysis.of(TEST_DATE,
                    new BigDecimal("5000"), new BigDecimal("7000"));

            BigDecimal gross = analysis.grossWithdrawalNeeded(null);

            assertEquals(0, new BigDecimal("2000").compareTo(gross));
        }

        @Test
        @DisplayName("grossWithdrawalNeeded with zero tax rate returns withdrawal")
        void grossWithdrawalZeroTaxRate() {
            GapAnalysis analysis = GapAnalysis.of(TEST_DATE,
                    new BigDecimal("5000"), new BigDecimal("7000"));

            BigDecimal gross = analysis.grossWithdrawalNeeded(BigDecimal.ZERO);

            assertEquals(0, new BigDecimal("2000").compareTo(gross));
        }

        @Test
        @DisplayName("grossWithdrawalNeeded throws when tax rate too high")
        void grossWithdrawalThrowsWhenTaxRateTooHigh() {
            GapAnalysis analysis = GapAnalysis.of(TEST_DATE,
                    new BigDecimal("5000"), new BigDecimal("7000"));

            assertThrows(IllegalArgumentException.class, () ->
                    analysis.grossWithdrawalNeeded(BigDecimal.ONE));
        }

        @Test
        @DisplayName("grossWithdrawalNeeded returns zero when no withdrawal needed")
        void grossWithdrawalReturnsZeroWhenNoDeficit() {
            GapAnalysis analysis = GapAnalysis.of(TEST_DATE,
                    new BigDecimal("7000"), new BigDecimal("5000"));

            BigDecimal gross = analysis.grossWithdrawalNeeded(new BigDecimal("0.22"));

            assertEquals(0, BigDecimal.ZERO.compareTo(gross));
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Summarize handles empty list")
        void summarizeEmptyList() {
            GapAnalysis summary = analyzer.summarize(List.of());

            assertEquals(0, BigDecimal.ZERO.compareTo(summary.totalIncome()));
        }

        @Test
        @DisplayName("Summarize handles null list")
        void summarizeNullList() {
            GapAnalysis summary = analyzer.summarize(null);

            assertEquals(0, BigDecimal.ZERO.compareTo(summary.totalIncome()));
        }

        @Test
        @DisplayName("calculateGrossWithdrawal handles null net needed")
        void grossWithdrawalNullNet() {
            BigDecimal gross = analyzer.calculateGrossWithdrawal(null, new BigDecimal("0.22"));

            assertEquals(0, BigDecimal.ZERO.compareTo(gross));
        }

        @Test
        @DisplayName("calculateGrossWithdrawal handles negative net needed")
        void grossWithdrawalNegativeNet() {
            BigDecimal gross = analyzer.calculateGrossWithdrawal(new BigDecimal("-100"), new BigDecimal("0.22"));

            assertEquals(0, BigDecimal.ZERO.compareTo(gross));
        }

        @Test
        @DisplayName("calculateGrossWithdrawal handles null tax rate")
        void grossWithdrawalNullTaxRate() {
            BigDecimal gross = analyzer.calculateGrossWithdrawal(new BigDecimal("2000"), null);

            assertEquals(0, new BigDecimal("2000.00").compareTo(gross));
        }
    }
}
