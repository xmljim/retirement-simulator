package io.github.xmljim.retirement.domain.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.exception.ValidationException;

@DisplayName("IncomeDetails Tests")
class IncomeDetailsTest {

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should create with all fields")
        void createsWithAllFields() {
            IncomeDetails income = IncomeDetails.builder()
                .adjustedGrossIncome(new BigDecimal("150000"))
                .studentLoanInterest(new BigDecimal("2500"))
                .tuitionAndFees(new BigDecimal("4000"))
                .foreignEarnedIncomeExclusion(new BigDecimal("120000"))
                .foreignHousingExclusion(new BigDecimal("15000"))
                .savingsBondInterestExclusion(new BigDecimal("1000"))
                .adoptionBenefitsExclusion(new BigDecimal("5000"))
                .build();

            assertEquals(0, new BigDecimal("150000").compareTo(income.adjustedGrossIncome()));
            assertEquals(0, new BigDecimal("2500").compareTo(income.studentLoanInterest()));
        }

        @Test
        @DisplayName("Should default null fields to zero")
        void defaultsNullsToZero() {
            IncomeDetails income = IncomeDetails.builder()
                .adjustedGrossIncome(new BigDecimal("100000"))
                .build();

            assertEquals(BigDecimal.ZERO, income.studentLoanInterest());
            assertEquals(BigDecimal.ZERO, income.foreignEarnedIncomeExclusion());
        }

        @Test
        @DisplayName("Should accept AGI from double")
        void acceptsAgiFromDouble() {
            IncomeDetails income = IncomeDetails.builder()
                .adjustedGrossIncome(150000.50)
                .build();

            assertEquals(0, new BigDecimal("150000.5").compareTo(income.adjustedGrossIncome()));
        }
    }

    @Nested
    @DisplayName("Static Factory Tests")
    class StaticFactoryTests {

        @Test
        @DisplayName("ofAgi creates with just AGI")
        void ofAgiCreatesWithJustAgi() {
            IncomeDetails income = IncomeDetails.ofAgi(new BigDecimal("175000"));

            assertEquals(0, new BigDecimal("175000").compareTo(income.adjustedGrossIncome()));
            assertEquals(BigDecimal.ZERO, income.getTotalAddBacks());
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw for negative AGI")
        void throwsForNegativeAgi() {
            assertThrows(ValidationException.class, () ->
                IncomeDetails.builder()
                    .adjustedGrossIncome(new BigDecimal("-1000"))
                    .build());
        }

        @Test
        @DisplayName("Should allow zero AGI")
        void allowsZeroAgi() {
            IncomeDetails income = IncomeDetails.builder()
                .adjustedGrossIncome(BigDecimal.ZERO)
                .build();

            assertEquals(BigDecimal.ZERO, income.adjustedGrossIncome());
        }
    }

    @Nested
    @DisplayName("Total Add-Backs Tests")
    class TotalAddBacksTests {

        @Test
        @DisplayName("Should calculate total add-backs")
        void calculatesTotalAddBacks() {
            IncomeDetails income = IncomeDetails.builder()
                .adjustedGrossIncome(new BigDecimal("100000"))
                .studentLoanInterest(new BigDecimal("2500"))
                .tuitionAndFees(new BigDecimal("1000"))
                .foreignEarnedIncomeExclusion(new BigDecimal("5000"))
                .build();

            // 2500 + 1000 + 5000 = 8500
            assertEquals(0, new BigDecimal("8500").compareTo(income.getTotalAddBacks()));
        }

        @Test
        @DisplayName("Should return zero when no add-backs")
        void returnsZeroWhenNoAddBacks() {
            IncomeDetails income = IncomeDetails.ofAgi(new BigDecimal("100000"));
            assertEquals(BigDecimal.ZERO, income.getTotalAddBacks());
        }
    }
}
