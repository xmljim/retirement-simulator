package io.github.xmljim.retirement.domain.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.ExpenseCategory;
import io.github.xmljim.retirement.domain.enums.ExpenseCategoryGroup;
import io.github.xmljim.retirement.domain.enums.ExpenseFrequency;

@DisplayName("Budget Tests")
class BudgetTest {

    private static final LocalDate TEST_DATE = LocalDate.of(2025, 6, 15);

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Creates budget with required fields")
        void createsWithRequiredFields() {
            Budget budget = Budget.builder()
                    .ownerId("person-1")
                    .build();

            assertEquals("person-1", budget.getOwnerId());
            assertFalse(budget.isCoupleBudget());
            assertTrue(budget.getRecurringExpenses().isEmpty());
        }

        @Test
        @DisplayName("Throws when ownerId is missing")
        void throwsWhenOwnerMissing() {
            assertThrows(NullPointerException.class, () ->
                    Budget.builder().build());
        }

        @Test
        @DisplayName("Creates couple budget with secondary owner")
        void createsCouplesBudget() {
            Budget budget = Budget.builder()
                    .ownerId("person-1")
                    .secondaryOwnerId("person-2")
                    .build();

            assertTrue(budget.isCoupleBudget());
            assertEquals("person-2", budget.getSecondaryOwnerId().orElse(null));
        }
    }

    @Nested
    @DisplayName("Expense Calculation Tests")
    class ExpenseCalculationTests {

        private Budget budget;

        @BeforeEach
        void setUp() {
            budget = Budget.builder()
                    .ownerId("test-owner")
                    .baseYear(2025)
                    .addRecurringExpense(RecurringExpense.builder()
                            .name("Mortgage")
                            .category(ExpenseCategory.HOUSING)
                            .amount(2000)
                            .frequency(ExpenseFrequency.MONTHLY)
                            .startDate(LocalDate.of(2020, 1, 1))
                            .build())
                    .addRecurringExpense(RecurringExpense.builder()
                            .name("Groceries")
                            .category(ExpenseCategory.FOOD)
                            .amount(600)
                            .frequency(ExpenseFrequency.MONTHLY)
                            .startDate(LocalDate.of(2020, 1, 1))
                            .build())
                    .addRecurringExpense(RecurringExpense.builder()
                            .name("Medicare")
                            .category(ExpenseCategory.MEDICARE_PREMIUMS)
                            .amount(200)
                            .frequency(ExpenseFrequency.MONTHLY)
                            .startDate(LocalDate.of(2020, 1, 1))
                            .build())
                    .build();
        }

        @Test
        @DisplayName("Calculates total monthly expenses")
        void calculatesMonthlyTotal() {
            BigDecimal total = budget.getTotalMonthlyExpenses(TEST_DATE);
            assertTrue(total.compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @DisplayName("Gets expenses by category")
        void getsByCategory() {
            var housingExpenses = budget.getExpensesByCategory(ExpenseCategory.HOUSING);
            assertEquals(1, housingExpenses.size());
            assertEquals("Mortgage", housingExpenses.get(0).getName());
        }

        @Test
        @DisplayName("Gets active expenses for date")
        void getsActiveExpenses() {
            var active = budget.getActiveExpenses(TEST_DATE);
            assertEquals(3, active.size());
        }
    }

    @Nested
    @DisplayName("ExpenseBreakdown Tests")
    class BreakdownTests {

        @Test
        @DisplayName("Creates breakdown with correct group totals")
        void createsBreakdownWithGroupTotals() {
            Budget budget = Budget.builder()
                    .ownerId("test")
                    .addRecurringExpense(RecurringExpense.builder()
                            .name("Housing")
                            .category(ExpenseCategory.HOUSING)
                            .amount(1500)
                            .frequency(ExpenseFrequency.MONTHLY)
                            .startDate(TEST_DATE.minusYears(1))
                            .noInflation()
                            .build())
                    .addRecurringExpense(RecurringExpense.builder()
                            .name("Travel")
                            .category(ExpenseCategory.TRAVEL)
                            .amount(500)
                            .frequency(ExpenseFrequency.MONTHLY)
                            .startDate(TEST_DATE.minusYears(1))
                            .noInflation()
                            .build())
                    .build();

            ExpenseBreakdown breakdown = budget.getMonthlyBreakdown(TEST_DATE);

            assertNotNull(breakdown);
            assertEquals(0, new BigDecimal("1500").compareTo(breakdown.essential()));
            assertEquals(0, new BigDecimal("500").compareTo(breakdown.discretionary()));
            assertEquals(0, new BigDecimal("2000").compareTo(breakdown.total()));
        }

        @Test
        @DisplayName("Empty breakdown returns zeros")
        void emptyBreakdownReturnsZeros() {
            ExpenseBreakdown empty = ExpenseBreakdown.empty(TEST_DATE);

            assertEquals(0, BigDecimal.ZERO.compareTo(empty.total()));
            assertEquals(0, BigDecimal.ZERO.compareTo(empty.essential()));
        }

        @Test
        @DisplayName("getGroupTotal returns correct values")
        void getGroupTotalWorks() {
            ExpenseBreakdown breakdown = ExpenseBreakdown.builder(TEST_DATE)
                    .addExpense(ExpenseCategory.HOUSING, new BigDecimal("1000"))
                    .addExpense(ExpenseCategory.MEDICARE_PREMIUMS, new BigDecimal("200"))
                    .build();

            assertEquals(0, new BigDecimal("1000").compareTo(
                    breakdown.getGroupTotal(ExpenseCategoryGroup.ESSENTIAL)));
            assertEquals(0, new BigDecimal("200").compareTo(
                    breakdown.getGroupTotal(ExpenseCategoryGroup.HEALTHCARE)));
        }
    }

    @Nested
    @DisplayName("OneTime Expense Tests")
    class OneTimeTests {

        @Test
        @DisplayName("Includes one-time expenses in breakdown")
        void includesOneTimeInBreakdown() {
            Budget budget = Budget.builder()
                    .ownerId("test")
                    .addOneTimeExpense(OneTimeExpense.builder()
                            .name("New Car")
                            .category(ExpenseCategory.VEHICLE_REPLACEMENT)
                            .amount(35000)
                            .targetDate(TEST_DATE)
                            .build())
                    .build();

            ExpenseBreakdown breakdown = budget.getMonthlyBreakdown(TEST_DATE);

            assertEquals(0, new BigDecimal("35000").compareTo(breakdown.oneTime()));
            assertEquals(0, new BigDecimal("35000").compareTo(breakdown.total()));
        }
    }

    @Nested
    @DisplayName("Annual Projection Tests")
    class AnnualTests {

        @Test
        @DisplayName("Projects annual expenses")
        void projectsAnnualExpenses() {
            Budget budget = Budget.builder()
                    .ownerId("test")
                    .addRecurringExpense(RecurringExpense.builder()
                            .name("Rent")
                            .category(ExpenseCategory.HOUSING)
                            .amount(1000)
                            .frequency(ExpenseFrequency.MONTHLY)
                            .startDate(LocalDate.of(2024, 1, 1))
                            .noInflation()
                            .build())
                    .build();

            BigDecimal annual = budget.getAnnualExpenses(2025);
            assertEquals(0, new BigDecimal("12000").compareTo(annual));
        }
    }
}
