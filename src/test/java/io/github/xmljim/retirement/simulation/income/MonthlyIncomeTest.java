package io.github.xmljim.retirement.simulation.income;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("MonthlyIncome")
class MonthlyIncomeTest {

    @Nested
    @DisplayName("Record Construction")
    class RecordConstruction {

        @Test
        @DisplayName("should default null values to zero")
        void shouldDefaultNullValuesToZero() {
            MonthlyIncome income = new MonthlyIncome(null, null, null, null, null);

            assertEquals(BigDecimal.ZERO, income.salaryIncome());
            assertEquals(BigDecimal.ZERO, income.socialSecurityIncome());
            assertEquals(BigDecimal.ZERO, income.pensionIncome());
            assertEquals(BigDecimal.ZERO, income.annuityIncome());
            assertEquals(BigDecimal.ZERO, income.otherIncome());
        }

        @Test
        @DisplayName("should preserve non-null values")
        void shouldPreserveNonNullValues() {
            MonthlyIncome income = new MonthlyIncome(
                new BigDecimal("5000"),
                new BigDecimal("2000"),
                new BigDecimal("1500"),
                new BigDecimal("500"),
                new BigDecimal("300")
            );

            assertEquals(new BigDecimal("5000"), income.salaryIncome());
            assertEquals(new BigDecimal("2000"), income.socialSecurityIncome());
            assertEquals(new BigDecimal("1500"), income.pensionIncome());
            assertEquals(new BigDecimal("500"), income.annuityIncome());
            assertEquals(new BigDecimal("300"), income.otherIncome());
        }
    }

    @Nested
    @DisplayName("Totals")
    class Totals {

        @Test
        @DisplayName("should calculate total of all sources")
        void shouldCalculateTotalOfAllSources() {
            MonthlyIncome income = new MonthlyIncome(
                new BigDecimal("5000"),
                new BigDecimal("2000"),
                new BigDecimal("1500"),
                new BigDecimal("500"),
                new BigDecimal("300")
            );

            assertEquals(new BigDecimal("9300"), income.total());
        }

        @Test
        @DisplayName("should calculate total non-salary income")
        void shouldCalculateTotalNonSalaryIncome() {
            MonthlyIncome income = new MonthlyIncome(
                new BigDecimal("5000"),
                new BigDecimal("2000"),
                new BigDecimal("1500"),
                new BigDecimal("500"),
                new BigDecimal("300")
            );

            // 2000 + 1500 + 500 + 300 = 4300
            assertEquals(new BigDecimal("4300"), income.totalNonSalary());
        }

        @Test
        @DisplayName("should return zero for empty income")
        void shouldReturnZeroForEmptyIncome() {
            MonthlyIncome income = MonthlyIncome.zero();

            assertEquals(BigDecimal.ZERO, income.total());
            assertEquals(BigDecimal.ZERO, income.totalNonSalary());
        }
    }

    @Nested
    @DisplayName("Has Income Methods")
    class HasIncomeMethods {

        @Test
        @DisplayName("should detect salary income")
        void shouldDetectSalaryIncome() {
            MonthlyIncome withSalary = MonthlyIncome.ofSalary(new BigDecimal("5000"));
            MonthlyIncome withoutSalary = MonthlyIncome.zero();

            assertTrue(withSalary.hasSalaryIncome());
            assertFalse(withoutSalary.hasSalaryIncome());
        }

        @Test
        @DisplayName("should detect retirement income")
        void shouldDetectRetirementIncome() {
            MonthlyIncome withRetirement = MonthlyIncome.builder()
                .socialSecurityIncome(new BigDecimal("2000"))
                .build();
            MonthlyIncome withoutRetirement = MonthlyIncome.ofSalary(new BigDecimal("5000"));

            assertTrue(withRetirement.hasRetirementIncome());
            assertFalse(withoutRetirement.hasRetirementIncome());
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should build with all fields")
        void shouldBuildWithAllFields() {
            MonthlyIncome income = MonthlyIncome.builder()
                .salaryIncome(new BigDecimal("5000"))
                .socialSecurityIncome(new BigDecimal("2000"))
                .pensionIncome(new BigDecimal("1500"))
                .annuityIncome(new BigDecimal("500"))
                .otherIncome(new BigDecimal("300"))
                .build();

            assertEquals(new BigDecimal("5000"), income.salaryIncome());
            assertEquals(new BigDecimal("9300"), income.total());
        }

        @Test
        @DisplayName("should accumulate with add methods")
        void shouldAccumulateWithAddMethods() {
            MonthlyIncome income = MonthlyIncome.builder()
                .addSalaryIncome(new BigDecimal("3000"))
                .addSalaryIncome(new BigDecimal("2000"))
                .addSocialSecurityIncome(new BigDecimal("1000"))
                .addSocialSecurityIncome(new BigDecimal("800"))
                .build();

            assertEquals(new BigDecimal("5000"), income.salaryIncome());
            assertEquals(new BigDecimal("1800"), income.socialSecurityIncome());
        }
    }

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("should create salary-only income")
        void shouldCreateSalaryOnlyIncome() {
            MonthlyIncome income = MonthlyIncome.ofSalary(new BigDecimal("6000"));

            assertEquals(new BigDecimal("6000"), income.salaryIncome());
            assertEquals(BigDecimal.ZERO, income.socialSecurityIncome());
            assertEquals(new BigDecimal("6000"), income.total());
        }

        @Test
        @DisplayName("should create zero income")
        void shouldCreateZeroIncome() {
            MonthlyIncome income = MonthlyIncome.zero();

            assertEquals(BigDecimal.ZERO, income.total());
            assertFalse(income.hasSalaryIncome());
            assertFalse(income.hasRetirementIncome());
        }
    }
}
