package io.github.xmljim.retirement.domain.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.WithdrawalType;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;

@DisplayName("WithdrawalStrategy Tests")
class WithdrawalStrategyTest {

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create fixed strategy")
        void createFixed() {
            WithdrawalStrategy strategy = WithdrawalStrategy.fixed(5000.00);

            assertEquals(WithdrawalType.FIXED, strategy.getWithdrawalType());
            assertEquals(0, new BigDecimal("5000").compareTo(strategy.getWithdrawalRate()));
        }

        @Test
        @DisplayName("Should create percentage strategy")
        void createPercentage() {
            WithdrawalStrategy strategy = WithdrawalStrategy.percentage(0.04);

            assertEquals(WithdrawalType.PERCENTAGE, strategy.getWithdrawalType());
            assertEquals(0, new BigDecimal("0.04").compareTo(strategy.getWithdrawalRate()));
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should build with all fields")
        void buildWithAllFields() {
            WithdrawalStrategy strategy = WithdrawalStrategy.builder()
                .withdrawalType(WithdrawalType.PERCENTAGE)
                .withdrawalRate(0.04)
                .build();

            assertEquals(WithdrawalType.PERCENTAGE, strategy.getWithdrawalType());
            assertEquals(0, new BigDecimal("0.04").compareTo(strategy.getWithdrawalRate()));
        }

        @Test
        @DisplayName("Should use default rate")
        void defaultRate() {
            WithdrawalStrategy strategy = WithdrawalStrategy.builder()
                .withdrawalType(WithdrawalType.FIXED)
                .build();

            assertEquals(0, BigDecimal.ZERO.compareTo(strategy.getWithdrawalRate()));
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw when withdrawal type is null")
        void nullWithdrawalType() {
            assertThrows(MissingRequiredFieldException.class, () ->
                WithdrawalStrategy.builder()
                    .withdrawalRate(0.04)
                    .build());
        }

        @Test
        @DisplayName("Should throw for negative withdrawal rate")
        void negativeWithdrawalRate() {
            assertThrows(ValidationException.class, () ->
                WithdrawalStrategy.builder()
                    .withdrawalType(WithdrawalType.PERCENTAGE)
                    .withdrawalRate(-0.04)
                    .build());
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("Equal strategies should be equal")
        void equalStrategies() {
            WithdrawalStrategy s1 = WithdrawalStrategy.percentage(0.04);
            WithdrawalStrategy s2 = WithdrawalStrategy.percentage(0.04);

            assertEquals(s1, s2);
            assertEquals(s1.hashCode(), s2.hashCode());
        }

        @Test
        @DisplayName("Different strategies should not be equal")
        void differentStrategies() {
            WithdrawalStrategy s1 = WithdrawalStrategy.fixed(5000.00);
            WithdrawalStrategy s2 = WithdrawalStrategy.percentage(0.04);

            assertNotEquals(s1, s2);
        }

        @Test
        @DisplayName("Same object should be equal to itself")
        void sameObject() {
            WithdrawalStrategy s1 = WithdrawalStrategy.percentage(0.04);
            assertEquals(s1, s1);
        }

        @Test
        @DisplayName("Should not equal null")
        void notEqualNull() {
            WithdrawalStrategy s1 = WithdrawalStrategy.percentage(0.04);
            assertNotEquals(null, s1);
        }

        @Test
        @DisplayName("Should not equal different class")
        void notEqualDifferentClass() {
            WithdrawalStrategy s1 = WithdrawalStrategy.percentage(0.04);
            assertNotEquals("string", s1);
        }
    }
}
