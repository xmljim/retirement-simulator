package io.github.xmljim.retirement.domain.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.ExpenseCategory;
import io.github.xmljim.retirement.domain.enums.ExpenseFrequency;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;

@DisplayName("RecurringExpense Tests")
class RecurringExpenseTest {

    private static final LocalDate START_DATE = LocalDate.of(2025, 1, 1);

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should build with required fields")
        void buildWithRequiredFields() {
            RecurringExpense expense = RecurringExpense.builder()
                .name("Rent")
                .category(ExpenseCategory.HOUSING)
                .amount(2000)
                .startDate(START_DATE)
                .build();

            assertEquals("Rent", expense.getName());
            assertEquals(ExpenseCategory.HOUSING, expense.getCategory());
            assertEquals(0, new BigDecimal("2000").compareTo(expense.getAmount()));
            assertEquals(ExpenseFrequency.MONTHLY, expense.getFrequency());
        }

        @Test
        @DisplayName("Should throw on missing name")
        void throwOnMissingName() {
            assertThrows(MissingRequiredFieldException.class, () ->
                RecurringExpense.builder()
                    .category(ExpenseCategory.HOUSING)
                    .amount(2000)
                    .startDate(START_DATE)
                    .build());
        }

        @Test
        @DisplayName("Should throw on blank name")
        void throwOnBlankName() {
            assertThrows(ValidationException.class, () ->
                RecurringExpense.builder()
                    .name("   ")
                    .category(ExpenseCategory.HOUSING)
                    .amount(2000)
                    .startDate(START_DATE)
                    .build());
        }

        @Test
        @DisplayName("Should throw on missing category")
        void throwOnMissingCategory() {
            assertThrows(MissingRequiredFieldException.class, () ->
                RecurringExpense.builder()
                    .name("Rent")
                    .amount(2000)
                    .startDate(START_DATE)
                    .build());
        }

        @Test
        @DisplayName("Should throw on end date before start date")
        void throwOnEndBeforeStart() {
            assertThrows(ValidationException.class, () ->
                RecurringExpense.builder()
                    .name("Rent")
                    .category(ExpenseCategory.HOUSING)
                    .amount(2000)
                    .startDate(START_DATE)
                    .endDate(START_DATE.minusDays(1))
                    .build());
        }
    }

    @Nested
    @DisplayName("Amount Calculation Tests")
    class AmountCalculationTests {

        @Test
        @DisplayName("Monthly frequency should return same amount")
        void monthlyFrequency() {
            RecurringExpense expense = RecurringExpense.builder()
                .name("Rent")
                .category(ExpenseCategory.HOUSING)
                .amount(2000)
                .frequency(ExpenseFrequency.MONTHLY)
                .startDate(START_DATE)
                .noInflation()
                .build();

            assertEquals(0, new BigDecimal("2000").compareTo(expense.getBaseMonthlyAmount()));
        }

        @Test
        @DisplayName("Annual frequency should convert to monthly")
        void annualFrequency() {
            RecurringExpense expense = RecurringExpense.builder()
                .name("Insurance")
                .category(ExpenseCategory.INSURANCE)
                .amount(1200)
                .frequency(ExpenseFrequency.ANNUAL)
                .startDate(START_DATE)
                .noInflation()
                .build();

            assertEquals(0, new BigDecimal("100").compareTo(expense.getBaseMonthlyAmount()));
        }

        @Test
        @DisplayName("Should return zero before start date")
        void zeroBeforeStartDate() {
            RecurringExpense expense = RecurringExpense.builder()
                .name("Rent")
                .category(ExpenseCategory.HOUSING)
                .amount(2000)
                .startDate(START_DATE)
                .build();

            assertEquals(BigDecimal.ZERO, expense.getMonthlyAmount(START_DATE.minusDays(1)));
        }

        @Test
        @DisplayName("Should return zero after end date")
        void zeroAfterEndDate() {
            LocalDate endDate = START_DATE.plusYears(1);
            RecurringExpense expense = RecurringExpense.builder()
                .name("Car Payment")
                .category(ExpenseCategory.DEBT_PAYMENTS)
                .amount(500)
                .startDate(START_DATE)
                .endDate(endDate)
                .build();

            assertEquals(BigDecimal.ZERO, expense.getMonthlyAmount(endDate.plusDays(1)));
        }
    }

    @Nested
    @DisplayName("Inflation Tests")
    class InflationTests {

        @Test
        @DisplayName("Should apply custom inflation rate")
        void customInflationRate() {
            RecurringExpense expense = RecurringExpense.builder()
                .name("Rent")
                .category(ExpenseCategory.HOUSING)
                .amount(2000)
                .startDate(START_DATE)
                .inflationRate(0.03)
                .build();

            LocalDate oneYearLater = START_DATE.plusYears(1);
            BigDecimal expected = new BigDecimal("2060.00");
            assertEquals(expected, expense.getMonthlyAmount(oneYearLater));
        }

        @Test
        @DisplayName("noInflation should prevent adjustment")
        void noInflation() {
            RecurringExpense expense = RecurringExpense.builder()
                .name("Car Payment")
                .category(ExpenseCategory.DEBT_PAYMENTS)
                .amount(500)
                .startDate(START_DATE)
                .noInflation()
                .build();

            LocalDate fiveYearsLater = START_DATE.plusYears(5);
            assertEquals(0, new BigDecimal("500").compareTo(expense.getMonthlyAmount(fiveYearsLater)));
        }

        @Test
        @DisplayName("Should use default rate when configured")
        void useDefaultInflation() {
            RecurringExpense expense = RecurringExpense.builder()
                .name("Groceries")
                .category(ExpenseCategory.FOOD)
                .amount(800)
                .startDate(START_DATE)
                .useDefaultInflation(true)
                .build();

            LocalDate oneYearLater = START_DATE.plusYears(1);
            BigDecimal defaultRate = new BigDecimal("0.025");
            BigDecimal expected = new BigDecimal("820.00");
            assertEquals(expected, expense.getMonthlyAmount(oneYearLater, defaultRate));
        }
    }

    @Nested
    @DisplayName("Active Period Tests")
    class ActivePeriodTests {

        @Test
        @DisplayName("Should be active on start date")
        void activeOnStartDate() {
            RecurringExpense expense = RecurringExpense.builder()
                .name("Rent")
                .category(ExpenseCategory.HOUSING)
                .amount(2000)
                .startDate(START_DATE)
                .build();

            assertTrue(expense.isActive(START_DATE));
        }

        @Test
        @DisplayName("Should be active on end date")
        void activeOnEndDate() {
            LocalDate endDate = START_DATE.plusYears(1);
            RecurringExpense expense = RecurringExpense.builder()
                .name("Car Payment")
                .category(ExpenseCategory.DEBT_PAYMENTS)
                .amount(500)
                .startDate(START_DATE)
                .endDate(endDate)
                .build();

            assertTrue(expense.isActive(endDate));
        }

        @Test
        @DisplayName("Ongoing expense should have no end date")
        void ongoingExpense() {
            RecurringExpense expense = RecurringExpense.builder()
                .name("Rent")
                .category(ExpenseCategory.HOUSING)
                .amount(2000)
                .startDate(START_DATE)
                .build();

            assertTrue(expense.isOngoing());
            assertTrue(expense.getEndDate().isEmpty());
        }
    }
}
