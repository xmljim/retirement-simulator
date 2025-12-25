package io.github.xmljim.retirement.domain.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;

@DisplayName("RetirementIncome Tests")
class RetirementIncomeTest {

    private static final LocalDate START_DATE = LocalDate.of(2034, 1, 1);

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create fixed pension")
        void createFixedPension() {
            RetirementIncome pension = RetirementIncome.fixedPension("Company Pension", 500.00, START_DATE);

            assertEquals("Company Pension", pension.getName());
            assertEquals(0, new BigDecimal("500").compareTo(pension.getMonthlyAmount()));
            assertEquals(0, BigDecimal.ZERO.compareTo(pension.getAdjustmentRate()));
            assertEquals(START_DATE, pension.getStartDate());
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should build with all fields")
        void buildWithAllFields() {
            RetirementIncome income = RetirementIncome.builder()
                .name("Pension")
                .monthlyAmount(500.00)
                .adjustmentRate(0.02)
                .startDate(START_DATE)
                .build();

            assertEquals("Pension", income.getName());
            assertEquals(0, new BigDecimal("500").compareTo(income.getMonthlyAmount()));
            assertEquals(0, new BigDecimal("0.02").compareTo(income.getAdjustmentRate()));
            assertEquals(START_DATE, income.getStartDate());
        }

        @Test
        @DisplayName("Should use default values")
        void defaultValues() {
            RetirementIncome income = RetirementIncome.builder()
                .startDate(START_DATE)
                .build();

            assertEquals("Other Income", income.getName());
            assertEquals(0, BigDecimal.ZERO.compareTo(income.getMonthlyAmount()));
            assertEquals(0, BigDecimal.ZERO.compareTo(income.getAdjustmentRate()));
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw when start date is null")
        void nullStartDate() {
            assertThrows(MissingRequiredFieldException.class, () ->
                RetirementIncome.builder()
                    .monthlyAmount(500.00)
                    .build());
        }

        @Test
        @DisplayName("Should throw for negative monthly amount")
        void negativeMonthlyAmount() {
            assertThrows(ValidationException.class, () ->
                RetirementIncome.builder()
                    .monthlyAmount(-500.00)
                    .startDate(START_DATE)
                    .build());
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("Equal incomes should be equal")
        void equalIncomes() {
            RetirementIncome r1 = RetirementIncome.fixedPension("Pension", 500.00, START_DATE);
            RetirementIncome r2 = RetirementIncome.fixedPension("Pension", 500.00, START_DATE);

            assertEquals(r1, r2);
            assertEquals(r1.hashCode(), r2.hashCode());
        }

        @Test
        @DisplayName("Different incomes should not be equal")
        void differentIncomes() {
            RetirementIncome r1 = RetirementIncome.fixedPension("Pension A", 500.00, START_DATE);
            RetirementIncome r2 = RetirementIncome.fixedPension("Pension B", 500.00, START_DATE);

            assertNotEquals(r1, r2);
        }
    }
}
