package io.github.xmljim.retirement.domain.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.exception.ValidationException;

@DisplayName("WorkingIncome Tests")
class WorkingIncomeTest {

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create with of() method")
        void createWithOf() {
            WorkingIncome income = WorkingIncome.of(100000.00, 0.02);

            assertEquals(0, new BigDecimal("100000").compareTo(income.getAnnualSalary()));
            assertEquals(0, new BigDecimal("0.02").compareTo(income.getColaRate()));
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should build with all fields")
        void buildWithAllFields() {
            WorkingIncome income = WorkingIncome.builder()
                .annualSalary(100000.00)
                .colaRate(0.02)
                .build();

            assertEquals(0, new BigDecimal("100000").compareTo(income.getAnnualSalary()));
            assertEquals(0, new BigDecimal("0.02").compareTo(income.getColaRate()));
        }

        @Test
        @DisplayName("Should use default values")
        void defaultValues() {
            WorkingIncome income = WorkingIncome.builder().build();

            assertEquals(0, BigDecimal.ZERO.compareTo(income.getAnnualSalary()));
            assertEquals(0, BigDecimal.ZERO.compareTo(income.getColaRate()));
        }
    }

    @Nested
    @DisplayName("Calculated Values Tests")
    class CalculatedValuesTests {

        @Test
        @DisplayName("Should calculate monthly salary")
        void monthlySalary() {
            WorkingIncome income = WorkingIncome.of(120000.00, 0.02);

            assertEquals(0, new BigDecimal("10000.00").compareTo(income.getMonthlySalary()));
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw for negative annual salary")
        void negativeAnnualSalary() {
            assertThrows(ValidationException.class, () ->
                WorkingIncome.builder()
                    .annualSalary(-100000.00)
                    .build());
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("Equal incomes should be equal")
        void equalIncomes() {
            WorkingIncome w1 = WorkingIncome.of(100000.00, 0.02);
            WorkingIncome w2 = WorkingIncome.of(100000.00, 0.02);

            assertEquals(w1, w2);
            assertEquals(w1.hashCode(), w2.hashCode());
        }

        @Test
        @DisplayName("Different incomes should not be equal")
        void differentIncomes() {
            WorkingIncome w1 = WorkingIncome.of(100000.00, 0.02);
            WorkingIncome w2 = WorkingIncome.of(120000.00, 0.02);

            assertNotEquals(w1, w2);
        }

        @Test
        @DisplayName("Same object should be equal to itself")
        void sameObject() {
            WorkingIncome w1 = WorkingIncome.of(100000.00, 0.02);
            assertEquals(w1, w1);
        }

        @Test
        @DisplayName("Should not equal null")
        void notEqualNull() {
            WorkingIncome w1 = WorkingIncome.of(100000.00, 0.02);
            assertNotEquals(null, w1);
        }

        @Test
        @DisplayName("Should not equal different class")
        void notEqualDifferentClass() {
            WorkingIncome w1 = WorkingIncome.of(100000.00, 0.02);
            assertNotEquals("string", w1);
        }
    }
}
