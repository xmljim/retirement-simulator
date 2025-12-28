package io.github.xmljim.retirement.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.calculator.GapAnalyzer;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultGapAnalyzer;
import io.github.xmljim.retirement.domain.enums.ExpenseCategory;
import io.github.xmljim.retirement.domain.enums.ExpenseFrequency;
import io.github.xmljim.retirement.domain.model.PersonProfile;
import io.github.xmljim.retirement.domain.value.Budget;
import io.github.xmljim.retirement.domain.value.ExpenseBreakdown;
import io.github.xmljim.retirement.domain.value.GapAnalysis;
import io.github.xmljim.retirement.domain.value.IncomeBreakdown;
import io.github.xmljim.retirement.domain.value.OneTimeExpense;
import io.github.xmljim.retirement.domain.value.RecurringExpense;

/**
 * Integration tests for Milestone 5: Expense & Budget Modeling.
 */
@DisplayName("M5 Integration Tests")
class M5IntegrationTest {

    private static final LocalDate TEST_DATE = LocalDate.of(2025, 6, 1);
    private PersonProfile owner;
    private GapAnalyzer gapAnalyzer;

    @BeforeEach
    void setUp() {
        owner = PersonProfile.builder()
                .name("Test Person")
                .dateOfBirth(LocalDate.of(1960, 1, 1))
                .retirementDate(LocalDate.of(2025, 1, 1))
                .lifeExpectancy(90)
                .build();
        gapAnalyzer = new DefaultGapAnalyzer();
    }

    @Test
    @DisplayName("Full budget with multiple expense types")
    void fullBudgetWithMultipleExpenseTypes() {
        Budget budget = Budget.builder()
                .owner(owner)
                .addRecurringExpense(RecurringExpense.builder()
                        .name("Housing").category(ExpenseCategory.HOUSING)
                        .amount(2000).frequency(ExpenseFrequency.MONTHLY)
                        .startDate(TEST_DATE.minusYears(1)).noInflation().build())
                .addRecurringExpense(RecurringExpense.builder()
                        .name("Food").category(ExpenseCategory.FOOD)
                        .amount(800).frequency(ExpenseFrequency.MONTHLY)
                        .startDate(TEST_DATE.minusYears(1)).noInflation().build())
                .addRecurringExpense(RecurringExpense.builder()
                        .name("Medicare").category(ExpenseCategory.MEDICARE_PREMIUMS)
                        .amount(200).frequency(ExpenseFrequency.MONTHLY)
                        .startDate(TEST_DATE.minusYears(1)).noInflation().build())
                .addRecurringExpense(RecurringExpense.builder()
                        .name("Travel").category(ExpenseCategory.TRAVEL)
                        .amount(500).frequency(ExpenseFrequency.MONTHLY)
                        .startDate(TEST_DATE.minusYears(1)).noInflation().build())
                .addOneTimeExpense(OneTimeExpense.builder()
                        .name("New Car").category(ExpenseCategory.VEHICLE_REPLACEMENT)
                        .amount(35000).targetDate(TEST_DATE).build())
                .build();

        ExpenseBreakdown breakdown = budget.getMonthlyBreakdown(TEST_DATE);

        assertNotNull(breakdown);
        assertEquals(0, new BigDecimal("2800").compareTo(breakdown.essential()));
        assertEquals(0, new BigDecimal("200").compareTo(breakdown.healthcare()));
        assertEquals(0, new BigDecimal("500").compareTo(breakdown.discretionary()));
        assertTrue(breakdown.oneTime().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Budget and gap analysis integration")
    void budgetAndGapAnalysisIntegration() {
        Budget budget = Budget.builder()
                .owner(owner)
                .addRecurringExpense(RecurringExpense.builder()
                        .name("Housing").category(ExpenseCategory.HOUSING)
                        .amount(2000).frequency(ExpenseFrequency.MONTHLY)
                        .startDate(TEST_DATE.minusYears(1)).noInflation().build())
                .addRecurringExpense(RecurringExpense.builder()
                        .name("Food").category(ExpenseCategory.FOOD)
                        .amount(1000).frequency(ExpenseFrequency.MONTHLY)
                        .startDate(TEST_DATE.minusYears(1)).noInflation().build())
                .build();

        IncomeBreakdown income = new IncomeBreakdown.Builder()
                .asOfDate(TEST_DATE)
                .socialSecurity(new BigDecimal("2500"))
                .pension(new BigDecimal("1000"))
                .passiveIncome(new BigDecimal("3500"))
                .build();

        ExpenseBreakdown expenses = budget.getMonthlyBreakdown(TEST_DATE);
        GapAnalysis analysis = gapAnalyzer.analyzeMonthly(income, expenses, TEST_DATE);

        assertTrue(analysis.hasSurplus());
        assertEquals(0, new BigDecimal("500").compareTo(analysis.surplusToSave()));
    }

    @Test
    @DisplayName("Year-long projection")
    void yearLongProjection() {
        Budget budget = Budget.builder()
                .owner(owner)
                .addRecurringExpense(RecurringExpense.builder()
                        .name("Expenses").category(ExpenseCategory.HOUSING)
                        .amount(3000).frequency(ExpenseFrequency.MONTHLY)
                        .startDate(TEST_DATE.minusYears(1)).noInflation().build())
                .build();

        List<GapAnalysis> projections = gapAnalyzer.projectYear(
                date -> new IncomeBreakdown.Builder()
                        .asOfDate(date).socialSecurity(new BigDecimal("2500"))
                        .passiveIncome(new BigDecimal("2500")).build(),
                date -> budget.getMonthlyBreakdown(date),
                TEST_DATE);

        assertEquals(12, projections.size());
        GapAnalysis summary = gapAnalyzer.summarize(projections);
        assertEquals(0, new BigDecimal("30000").compareTo(summary.totalIncome()));
    }

    @Test
    @DisplayName("Tax-aware withdrawal calculation")
    void taxAwareWithdrawalCalculation() {
        GapAnalysis deficit = GapAnalysis.of(TEST_DATE,
                new BigDecimal("3000"), new BigDecimal("5000"));

        assertTrue(deficit.hasDeficit());
        BigDecimal grossNeeded = deficit.grossWithdrawalNeeded(new BigDecimal("0.22"));
        assertEquals(0, new BigDecimal("2564.10").compareTo(grossNeeded));
    }
}
