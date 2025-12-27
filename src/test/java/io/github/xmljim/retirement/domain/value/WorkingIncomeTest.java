package io.github.xmljim.retirement.domain.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

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
    @DisplayName("Prior Year Income Tests")
    class PriorYearIncomeTests {

        @Test
        @DisplayName("Should set and get prior year income")
        void setAndGetPriorYearIncome() {
            WorkingIncome income = WorkingIncome.builder()
                .annualSalary(100000.00)
                .colaRate(0.02)
                .priorYearIncome(150000.00)
                .build();

            assertEquals(0, new BigDecimal("150000").compareTo(income.getPriorYearIncome()));
        }

        @Test
        @DisplayName("Should allow null prior year income")
        void allowNullPriorYearIncome() {
            WorkingIncome income = WorkingIncome.builder()
                .annualSalary(100000.00)
                .colaRate(0.02)
                .build();

            assertNull(income.getPriorYearIncome());
        }

        @Test
        @DisplayName("Should include prior year income in equals")
        void includesInEquals() {
            WorkingIncome w1 = WorkingIncome.builder()
                .annualSalary(100000.00)
                .colaRate(0.02)
                .priorYearIncome(150000.00)
                .build();

            WorkingIncome w2 = WorkingIncome.builder()
                .annualSalary(100000.00)
                .colaRate(0.02)
                .priorYearIncome(150000.00)
                .build();

            WorkingIncome w3 = WorkingIncome.builder()
                .annualSalary(100000.00)
                .colaRate(0.02)
                .priorYearIncome(100000.00)
                .build();

            assertEquals(w1, w2);
            assertNotEquals(w1, w3);
        }

        @Test
        @DisplayName("Should include prior year income in toString")
        void includesInToString() {
            WorkingIncome income = WorkingIncome.builder()
                .annualSalary(100000.00)
                .priorYearIncome(150000.00)
                .build();

            String result = income.toString();
            assertEquals(true, result.contains("priorYearIncome=150000"));
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

    @Nested
    @DisplayName("Date Range Tests")
    class DateRangeTests {

        @Test
        @DisplayName("Should set and get start date")
        void setAndGetStartDate() {
            LocalDate startDate = LocalDate.of(2020, 1, 1);
            WorkingIncome income = WorkingIncome.builder()
                .annualSalary(100000.00)
                .colaRate(0.03)
                .startDate(startDate)
                .build();

            assertEquals(startDate, income.getStartDate());
        }

        @Test
        @DisplayName("Should set and get end date")
        void setAndGetEndDate() {
            LocalDate endDate = LocalDate.of(2035, 1, 1);
            WorkingIncome income = WorkingIncome.builder()
                .annualSalary(100000.00)
                .colaRate(0.03)
                .endDate(endDate)
                .build();

            assertEquals(endDate, income.getEndDate());
        }

        @Test
        @DisplayName("Should allow null start and end dates")
        void allowNullDates() {
            WorkingIncome income = WorkingIncome.builder()
                .annualSalary(100000.00)
                .colaRate(0.03)
                .build();

            assertNull(income.getStartDate());
            assertNull(income.getEndDate());
        }

        @Test
        @DisplayName("Should default colaMonth to January")
        void defaultColaMonth() {
            WorkingIncome income = WorkingIncome.builder()
                .annualSalary(100000.00)
                .build();

            assertEquals(1, income.getColaMonth());
        }

        @Test
        @DisplayName("Should set custom colaMonth")
        void customColaMonth() {
            WorkingIncome income = WorkingIncome.builder()
                .annualSalary(100000.00)
                .colaMonth(7)
                .build();

            assertEquals(7, income.getColaMonth());
        }
    }

    @Nested
    @DisplayName("COLA-Adjusted Annual Salary Tests")
    class ColaAdjustedAnnualSalaryTests {

        @Test
        @DisplayName("Should return base salary in start year")
        void baseSalaryInStartYear() {
            WorkingIncome income = WorkingIncome.builder()
                .annualSalary(100000.00)
                .colaRate(0.03)
                .startDate(LocalDate.of(2020, 1, 1))
                .endDate(LocalDate.of(2035, 1, 1))
                .build();

            assertEquals(0, new BigDecimal("100000").compareTo(income.getAnnualSalary(2020)));
        }

        @Test
        @DisplayName("Should apply COLA for one year")
        void colaAfterOneYear() {
            WorkingIncome income = WorkingIncome.builder()
                .annualSalary(100000.00)
                .colaRate(0.03)
                .startDate(LocalDate.of(2020, 1, 1))
                .endDate(LocalDate.of(2035, 1, 1))
                .build();

            // 100000 * 1.03 = 103000
            assertEquals(0, new BigDecimal("103000.00").compareTo(income.getAnnualSalary(2021)));
        }

        @Test
        @DisplayName("Should compound COLA over multiple years")
        void colaAfterFiveYears() {
            WorkingIncome income = WorkingIncome.builder()
                .annualSalary(100000.00)
                .colaRate(0.03)
                .startDate(LocalDate.of(2020, 1, 1))
                .endDate(LocalDate.of(2035, 1, 1))
                .build();

            // 100000 * (1.03)^5 = 115927.407... (high precision)
            BigDecimal actual = income.getAnnualSalary(2025);
            // Compare to 2 decimal places for validation
            BigDecimal rounded = actual.setScale(2, java.math.RoundingMode.HALF_UP);
            assertEquals(0, new BigDecimal("115927.41").compareTo(rounded));
        }

        @Test
        @DisplayName("Should return zero before start date")
        void zeroBeforeStartDate() {
            WorkingIncome income = WorkingIncome.builder()
                .annualSalary(100000.00)
                .colaRate(0.03)
                .startDate(LocalDate.of(2020, 1, 1))
                .endDate(LocalDate.of(2035, 1, 1))
                .build();

            assertEquals(0, BigDecimal.ZERO.compareTo(income.getAnnualSalary(2019)));
        }

        @Test
        @DisplayName("Should return zero after end date")
        void zeroAfterEndDate() {
            WorkingIncome income = WorkingIncome.builder()
                .annualSalary(100000.00)
                .colaRate(0.03)
                .startDate(LocalDate.of(2020, 1, 1))
                .endDate(LocalDate.of(2035, 1, 1))
                .build();

            assertEquals(0, BigDecimal.ZERO.compareTo(income.getAnnualSalary(2035)));
        }

        @Test
        @DisplayName("Should handle zero COLA (salary freeze)")
        void zeroCola() {
            WorkingIncome income = WorkingIncome.builder()
                .annualSalary(100000.00)
                .colaRate(0.0)
                .startDate(LocalDate.of(2020, 1, 1))
                .endDate(LocalDate.of(2035, 1, 1))
                .build();

            assertEquals(0, new BigDecimal("100000").compareTo(income.getAnnualSalary(2025)));
        }

        @Test
        @DisplayName("Should return base salary when no start date")
        void noStartDate() {
            WorkingIncome income = WorkingIncome.builder()
                .annualSalary(100000.00)
                .colaRate(0.03)
                .build();

            assertEquals(0, new BigDecimal("100000").compareTo(income.getAnnualSalary(2025)));
        }
    }

    @Nested
    @DisplayName("COLA-Adjusted Monthly Salary Tests")
    class ColaAdjustedMonthlySalaryTests {

        @Test
        @DisplayName("Should return monthly salary for date in employment period")
        void monthlySalaryDuringEmployment() {
            WorkingIncome income = WorkingIncome.builder()
                .annualSalary(120000.00)
                .colaRate(0.03)
                .startDate(LocalDate.of(2020, 1, 1))
                .endDate(LocalDate.of(2035, 1, 1))
                .build();

            // 2020: 120000/12 = 10000
            LocalDate date = LocalDate.of(2020, 6, 15);
            assertEquals(0, new BigDecimal("10000.00").compareTo(income.getMonthlySalary(date)));
        }

        @Test
        @DisplayName("Should apply COLA to monthly salary")
        void monthlySalaryWithCola() {
            WorkingIncome income = WorkingIncome.builder()
                .annualSalary(120000.00)
                .colaRate(0.03)
                .startDate(LocalDate.of(2020, 1, 1))
                .endDate(LocalDate.of(2035, 1, 1))
                .build();

            // 2021: 120000 * 1.03 / 12 = 10300
            LocalDate date = LocalDate.of(2021, 6, 15);
            assertEquals(0, new BigDecimal("10300.00").compareTo(income.getMonthlySalary(date)));
        }

        @Test
        @DisplayName("Should return zero before start date")
        void zeroBeforeStartDate() {
            WorkingIncome income = WorkingIncome.builder()
                .annualSalary(120000.00)
                .colaRate(0.03)
                .startDate(LocalDate.of(2020, 1, 1))
                .endDate(LocalDate.of(2035, 1, 1))
                .build();

            LocalDate date = LocalDate.of(2019, 6, 15);
            assertEquals(0, BigDecimal.ZERO.compareTo(income.getMonthlySalary(date)));
        }

        @Test
        @DisplayName("Should return zero after end date")
        void zeroAfterEndDate() {
            WorkingIncome income = WorkingIncome.builder()
                .annualSalary(120000.00)
                .colaRate(0.03)
                .startDate(LocalDate.of(2020, 1, 1))
                .endDate(LocalDate.of(2035, 1, 1))
                .build();

            LocalDate date = LocalDate.of(2035, 6, 15);
            assertEquals(0, BigDecimal.ZERO.compareTo(income.getMonthlySalary(date)));
        }

        @Test
        @DisplayName("Should return zero for null date")
        void zeroForNullDate() {
            WorkingIncome income = WorkingIncome.builder()
                .annualSalary(120000.00)
                .colaRate(0.03)
                .startDate(LocalDate.of(2020, 1, 1))
                .build();

            assertEquals(0, BigDecimal.ZERO.compareTo(income.getMonthlySalary(null)));
        }
    }

    @Nested
    @DisplayName("isActiveOn Tests")
    class IsActiveOnTests {

        @Test
        @DisplayName("Should be active on start date")
        void activeOnStartDate() {
            LocalDate startDate = LocalDate.of(2020, 1, 1);
            WorkingIncome income = WorkingIncome.builder()
                .annualSalary(100000.00)
                .startDate(startDate)
                .endDate(LocalDate.of(2035, 1, 1))
                .build();

            assertTrue(income.isActiveOn(startDate));
        }

        @Test
        @DisplayName("Should not be active before start date")
        void notActiveBeforeStartDate() {
            WorkingIncome income = WorkingIncome.builder()
                .annualSalary(100000.00)
                .startDate(LocalDate.of(2020, 1, 1))
                .endDate(LocalDate.of(2035, 1, 1))
                .build();

            assertFalse(income.isActiveOn(LocalDate.of(2019, 12, 31)));
        }

        @Test
        @DisplayName("Should not be active on end date")
        void notActiveOnEndDate() {
            LocalDate endDate = LocalDate.of(2035, 1, 1);
            WorkingIncome income = WorkingIncome.builder()
                .annualSalary(100000.00)
                .startDate(LocalDate.of(2020, 1, 1))
                .endDate(endDate)
                .build();

            assertFalse(income.isActiveOn(endDate));
        }

        @Test
        @DisplayName("Should be active day before end date")
        void activeBeforeEndDate() {
            WorkingIncome income = WorkingIncome.builder()
                .annualSalary(100000.00)
                .startDate(LocalDate.of(2020, 1, 1))
                .endDate(LocalDate.of(2035, 1, 1))
                .build();

            assertTrue(income.isActiveOn(LocalDate.of(2034, 12, 31)));
        }

        @Test
        @DisplayName("Should be active when no end date")
        void activeWhenNoEndDate() {
            WorkingIncome income = WorkingIncome.builder()
                .annualSalary(100000.00)
                .startDate(LocalDate.of(2020, 1, 1))
                .build();

            assertTrue(income.isActiveOn(LocalDate.of(2050, 6, 15)));
        }

        @Test
        @DisplayName("Should be active when no start date")
        void activeWhenNoStartDate() {
            WorkingIncome income = WorkingIncome.builder()
                .annualSalary(100000.00)
                .endDate(LocalDate.of(2035, 1, 1))
                .build();

            assertTrue(income.isActiveOn(LocalDate.of(2020, 6, 15)));
        }

        @Test
        @DisplayName("Should handle null date")
        void handleNullDate() {
            WorkingIncome income = WorkingIncome.builder()
                .annualSalary(100000.00)
                .startDate(LocalDate.of(2020, 1, 1))
                .build();

            assertFalse(income.isActiveOn(null));
        }
    }
}
