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

@DisplayName("SocialSecurityIncome Tests")
class SocialSecurityIncomeTest {

    private static final LocalDate START_DATE = LocalDate.of(2035, 9, 1);

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should build with all fields")
        void buildWithAllFields() {
            SocialSecurityIncome ss = SocialSecurityIncome.builder()
                .monthlyBenefit(4018.00)
                .colaRate(0.028)
                .startDate(START_DATE)
                .build();

            assertEquals(0, new BigDecimal("4018").compareTo(ss.getMonthlyBenefit()));
            assertEquals(0, new BigDecimal("0.028").compareTo(ss.getColaRate()));
            assertEquals(START_DATE, ss.getStartDate());
        }

        @Test
        @DisplayName("Should use default values")
        void defaultValues() {
            SocialSecurityIncome ss = SocialSecurityIncome.builder()
                .startDate(START_DATE)
                .build();

            assertEquals(0, BigDecimal.ZERO.compareTo(ss.getMonthlyBenefit()));
            assertEquals(0, BigDecimal.ZERO.compareTo(ss.getColaRate()));
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw when start date is null")
        void nullStartDate() {
            assertThrows(MissingRequiredFieldException.class, () ->
                SocialSecurityIncome.builder()
                    .monthlyBenefit(4018.00)
                    .build());
        }

        @Test
        @DisplayName("Should throw for negative monthly benefit")
        void negativeMonthlyBenefit() {
            assertThrows(ValidationException.class, () ->
                SocialSecurityIncome.builder()
                    .monthlyBenefit(-1000.00)
                    .startDate(START_DATE)
                    .build());
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("Equal configs should be equal")
        void equalConfigs() {
            SocialSecurityIncome s1 = SocialSecurityIncome.builder()
                .monthlyBenefit(4018.00)
                .colaRate(0.028)
                .startDate(START_DATE)
                .build();
            SocialSecurityIncome s2 = SocialSecurityIncome.builder()
                .monthlyBenefit(4018.00)
                .colaRate(0.028)
                .startDate(START_DATE)
                .build();

            assertEquals(s1, s2);
            assertEquals(s1.hashCode(), s2.hashCode());
        }

        @Test
        @DisplayName("Different configs should not be equal")
        void differentConfigs() {
            SocialSecurityIncome s1 = SocialSecurityIncome.builder()
                .monthlyBenefit(4018.00)
                .startDate(START_DATE)
                .build();
            SocialSecurityIncome s2 = SocialSecurityIncome.builder()
                .monthlyBenefit(3000.00)
                .startDate(START_DATE)
                .build();

            assertNotEquals(s1, s2);
        }
    }
}
