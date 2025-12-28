package io.github.xmljim.retirement.domain.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.ExpenseCategory;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;

@DisplayName("OneTimeExpense Tests")
class OneTimeExpenseTest {

    private static final LocalDate TARGET_DATE = LocalDate.of(2028, 6, 15);
    private static final LocalDate BASE_DATE = LocalDate.of(2025, 1, 1);

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should build with required fields")
        void buildWithRequiredFields() {
            OneTimeExpense expense = OneTimeExpense.builder()
                .name("New Car")
                .category(ExpenseCategory.VEHICLE_REPLACEMENT)
                .amount(40000)
                .targetDate(TARGET_DATE)
                .build();

            assertEquals("New Car", expense.getName());
            assertEquals(ExpenseCategory.VEHICLE_REPLACEMENT, expense.getCategory());
            assertEquals(0, new BigDecimal("40000").compareTo(expense.getBaseAmount()));
            assertEquals(TARGET_DATE, expense.getTargetDate());
        }

        @Test
        @DisplayName("Should throw on missing name")
        void throwOnMissingName() {
            assertThrows(MissingRequiredFieldException.class, () ->
                OneTimeExpense.builder()
                    .category(ExpenseCategory.TRAVEL)
                    .amount(10000)
                    .targetDate(TARGET_DATE)
                    .build());
        }

        @Test
        @DisplayName("Should throw on blank name")
        void throwOnBlankName() {
            assertThrows(ValidationException.class, () ->
                OneTimeExpense.builder()
                    .name("   ")
                    .category(ExpenseCategory.TRAVEL)
                    .amount(10000)
                    .targetDate(TARGET_DATE)
                    .build());
        }

        @Test
        @DisplayName("Should throw on missing category")
        void throwOnMissingCategory() {
            assertThrows(MissingRequiredFieldException.class, () ->
                OneTimeExpense.builder()
                    .name("Vacation")
                    .amount(10000)
                    .targetDate(TARGET_DATE)
                    .build());
        }

        @Test
        @DisplayName("Should throw on missing target date")
        void throwOnMissingTargetDate() {
            assertThrows(MissingRequiredFieldException.class, () ->
                OneTimeExpense.builder()
                    .name("Vacation")
                    .category(ExpenseCategory.TRAVEL)
                    .amount(10000)
                    .build());
        }

        @Test
        @DisplayName("Should throw on negative amount")
        void throwOnNegativeAmount() {
            assertThrows(ValidationException.class, () ->
                OneTimeExpense.builder()
                    .name("Vacation")
                    .category(ExpenseCategory.TRAVEL)
                    .amount(-1000)
                    .targetDate(TARGET_DATE)
                    .build());
        }
    }

    @Nested
    @DisplayName("Amount Calculation Tests")
    class AmountCalculationTests {

        @Test
        @DisplayName("Should return base amount when no inflation set")
        void baseAmountWithNoInflation() {
            OneTimeExpense expense = OneTimeExpense.builder()
                .name("New Car")
                .category(ExpenseCategory.VEHICLE_REPLACEMENT)
                .amount(40000)
                .targetDate(TARGET_DATE)
                .build();

            assertEquals(0, new BigDecimal("40000").compareTo(expense.getAdjustedAmount()));
        }

        @Test
        @DisplayName("Should inflate amount when rate and base date set")
        void inflatedAmount() {
            OneTimeExpense expense = OneTimeExpense.builder()
                .name("New Car")
                .category(ExpenseCategory.VEHICLE_REPLACEMENT)
                .amount(40000)
                .targetDate(TARGET_DATE)
                .baseDate(BASE_DATE)
                .inflationRate(0.03)
                .build();

            // 3 years from 2025 to 2028, 3% inflation: 40000 * 1.03^3 = 43709.08
            BigDecimal adjusted = expense.getAdjustedAmount();
            assertEquals(0, new BigDecimal("43709.08").compareTo(adjusted));
        }

        @Test
        @DisplayName("Should use default rate when provided")
        void useDefaultRate() {
            OneTimeExpense expense = OneTimeExpense.builder()
                .name("New Car")
                .category(ExpenseCategory.VEHICLE_REPLACEMENT)
                .amount(40000)
                .targetDate(TARGET_DATE)
                .baseDate(BASE_DATE)
                .build();

            // 3 years, 2.5% default rate: 40000 * 1.025^3 = 43075.63
            BigDecimal adjusted = expense.getAdjustedAmount(new BigDecimal("0.025"));
            assertEquals(0, new BigDecimal("43075.63").compareTo(adjusted));
        }

        @Test
        @DisplayName("Should prefer custom rate over default")
        void preferCustomRate() {
            OneTimeExpense expense = OneTimeExpense.builder()
                .name("New Car")
                .category(ExpenseCategory.VEHICLE_REPLACEMENT)
                .amount(40000)
                .targetDate(TARGET_DATE)
                .baseDate(BASE_DATE)
                .inflationRate(0.03)
                .build();

            // Should use 3% not 2.5%
            BigDecimal adjusted = expense.getAdjustedAmount(new BigDecimal("0.025"));
            assertEquals(0, new BigDecimal("43709.08").compareTo(adjusted));
        }
    }

    @Nested
    @DisplayName("Target Month Tests")
    class TargetMonthTests {

        @Test
        @DisplayName("Should return amount in target month")
        void amountInTargetMonth() {
            OneTimeExpense expense = OneTimeExpense.builder()
                .name("Vacation")
                .category(ExpenseCategory.TRAVEL)
                .amount(10000)
                .targetDate(TARGET_DATE)
                .build();

            // Same month as target (June 2028)
            LocalDate sameMonth = LocalDate.of(2028, 6, 1);
            assertEquals(0, new BigDecimal("10000").compareTo(expense.getAmountForMonth(sameMonth)));
        }

        @Test
        @DisplayName("Should return zero outside target month")
        void zeroOutsideTargetMonth() {
            OneTimeExpense expense = OneTimeExpense.builder()
                .name("Vacation")
                .category(ExpenseCategory.TRAVEL)
                .amount(10000)
                .targetDate(TARGET_DATE)
                .build();

            // Month before target
            LocalDate beforeMonth = LocalDate.of(2028, 5, 15);
            assertEquals(BigDecimal.ZERO, expense.getAmountForMonth(beforeMonth));

            // Month after target
            LocalDate afterMonth = LocalDate.of(2028, 7, 1);
            assertEquals(BigDecimal.ZERO, expense.getAmountForMonth(afterMonth));
        }

        @Test
        @DisplayName("isInTargetMonth should be true for same month")
        void isInTargetMonthTrue() {
            OneTimeExpense expense = OneTimeExpense.builder()
                .name("Vacation")
                .category(ExpenseCategory.TRAVEL)
                .amount(10000)
                .targetDate(TARGET_DATE)
                .build();

            assertTrue(expense.isInTargetMonth(LocalDate.of(2028, 6, 1)));
            assertTrue(expense.isInTargetMonth(LocalDate.of(2028, 6, 30)));
        }

        @Test
        @DisplayName("isInTargetMonth should be false for different month")
        void isInTargetMonthFalse() {
            OneTimeExpense expense = OneTimeExpense.builder()
                .name("Vacation")
                .category(ExpenseCategory.TRAVEL)
                .amount(10000)
                .targetDate(TARGET_DATE)
                .build();

            assertFalse(expense.isInTargetMonth(LocalDate.of(2028, 5, 15)));
            assertFalse(expense.isInTargetMonth(LocalDate.of(2029, 6, 15)));
        }
    }

    @Nested
    @DisplayName("Future/Past Tests")
    class FuturePastTests {

        @Test
        @DisplayName("isFuture should be true when target is after reference")
        void isFutureTrue() {
            OneTimeExpense expense = OneTimeExpense.builder()
                .name("New Car")
                .category(ExpenseCategory.VEHICLE_REPLACEMENT)
                .amount(40000)
                .targetDate(TARGET_DATE)
                .build();

            assertTrue(expense.isFuture(LocalDate.of(2025, 1, 1)));
        }

        @Test
        @DisplayName("isFuture should be false when target is before reference")
        void isFutureFalse() {
            OneTimeExpense expense = OneTimeExpense.builder()
                .name("New Car")
                .category(ExpenseCategory.VEHICLE_REPLACEMENT)
                .amount(40000)
                .targetDate(TARGET_DATE)
                .build();

            assertFalse(expense.isFuture(LocalDate.of(2030, 1, 1)));
        }

        @Test
        @DisplayName("isPast should be true when target is before reference")
        void isPastTrue() {
            OneTimeExpense expense = OneTimeExpense.builder()
                .name("New Car")
                .category(ExpenseCategory.VEHICLE_REPLACEMENT)
                .amount(40000)
                .targetDate(TARGET_DATE)
                .build();

            assertTrue(expense.isPast(LocalDate.of(2030, 1, 1)));
        }

        @Test
        @DisplayName("isPast should be false when target is after reference")
        void isPastFalse() {
            OneTimeExpense expense = OneTimeExpense.builder()
                .name("New Car")
                .category(ExpenseCategory.VEHICLE_REPLACEMENT)
                .amount(40000)
                .targetDate(TARGET_DATE)
                .build();

            assertFalse(expense.isPast(LocalDate.of(2025, 1, 1)));
        }
    }
}
