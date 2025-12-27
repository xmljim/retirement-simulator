package io.github.xmljim.retirement.domain.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Exception Tests")
class ExceptionTest {

    @Nested
    @DisplayName("MissingRequiredFieldException Tests")
    class MissingRequiredFieldExceptionTests {

        @Test
        @DisplayName("Should create exception with field name")
        void createWithFieldName() {
            MissingRequiredFieldException ex = new MissingRequiredFieldException("name");
            assertEquals("name is required", ex.getMessage());
            assertEquals("name", ex.getFieldName());
        }

        @Test
        @DisplayName("Should create exception with custom message")
        void createWithCustomMessage() {
            MissingRequiredFieldException ex =
                new MissingRequiredFieldException("Custom message", "fieldName");
            assertEquals("Custom message", ex.getMessage());
            assertEquals("fieldName", ex.getFieldName());
        }

        @Test
        @DisplayName("requireNonNull should throw for null value")
        void requireNonNullThrowsForNull() {
            assertThrows(MissingRequiredFieldException.class, () ->
                MissingRequiredFieldException.requireNonNull(null, "testField"));
        }

        @Test
        @DisplayName("requireNonNull should return value when not null")
        void requireNonNullReturnsValue() {
            String value = "test";
            String result = MissingRequiredFieldException.requireNonNull(value, "testField");
            assertEquals(value, result);
        }
    }

    @Nested
    @DisplayName("InvalidAllocationException Tests")
    class InvalidAllocationExceptionTests {

        @Test
        @DisplayName("Should create exception with message")
        void createWithMessage() {
            InvalidAllocationException ex = new InvalidAllocationException("Test message");
            assertEquals("Test message", ex.getMessage());
            assertEquals("allocation", ex.getFieldName());
        }

        @Test
        @DisplayName("invalidSum should create proper message")
        void invalidSumMessage() {
            InvalidAllocationException ex =
                InvalidAllocationException.invalidSum(new BigDecimal("95.5"));
            assertEquals("Asset allocation must sum to 100%, but was 95.5%", ex.getMessage());
        }

        @Test
        @DisplayName("outOfRange should create proper message")
        void outOfRangeMessage() {
            InvalidAllocationException ex =
                InvalidAllocationException.outOfRange("Stocks", new BigDecimal("110"));
            assertEquals("Stocks percentage must be between 0 and 100, but was 110",
                ex.getMessage());
        }
    }

    @Nested
    @DisplayName("InvalidRateException Tests")
    class InvalidRateExceptionTests {

        @Test
        @DisplayName("Should create exception with message and field")
        void createWithMessageAndField() {
            InvalidRateException ex = new InvalidRateException("Test message", "rate");
            assertEquals("Test message", ex.getMessage());
            assertEquals("rate", ex.getFieldName());
        }

        @Test
        @DisplayName("returnRateOutOfRange should create proper message")
        void returnRateOutOfRangeMessage() {
            InvalidRateException ex = InvalidRateException.returnRateOutOfRange(
                "Pre-retirement return rate",
                new BigDecimal("0.60"),
                new BigDecimal("-0.50"),
                new BigDecimal("0.50"));
            assertEquals("Pre-retirement return rate must be between -50% and 50%, but was 60%",
                ex.getMessage());
            assertEquals("pre-retirement_return_rate", ex.getFieldName());
        }

        @Test
        @DisplayName("inflationRateOutOfRange should create proper message")
        void inflationRateOutOfRangeMessage() {
            InvalidRateException ex = InvalidRateException.inflationRateOutOfRange(
                "General inflation", new BigDecimal("0.25"));
            assertEquals("General inflation rate must be between -10% and 20%, but was 25%",
                ex.getMessage());
            assertEquals("general_inflation", ex.getFieldName());
        }
    }

    @Nested
    @DisplayName("InvalidDateRangeException Tests")
    class InvalidDateRangeExceptionTests {

        @Test
        @DisplayName("dateMustBeAfter should create proper message")
        void dateMustBeAfterMessage() {
            InvalidDateRangeException ex =
                InvalidDateRangeException.dateMustBeAfter("Retirement date", "date of birth");
            assertEquals("Retirement date cannot be before date of birth", ex.getMessage());
            assertEquals("retirement_date", ex.getFieldName());
        }

        @Test
        @DisplayName("dateMustBeBefore should create proper message")
        void dateMustBeBeforeMessage() {
            InvalidDateRangeException ex =
                InvalidDateRangeException.dateMustBeBefore("Start date", "end date");
            assertEquals("Start date cannot be after end date", ex.getMessage());
            assertEquals("start_date", ex.getFieldName());
        }
    }

    @Nested
    @DisplayName("ValidationException Tests")
    class ValidationExceptionTests {

        @Test
        @DisplayName("Should create with message and field")
        void createWithMessageAndField() {
            ValidationException ex = new ValidationException("Test message", "testField");
            assertEquals("Test message", ex.getMessage());
            assertEquals("testField", ex.getFieldName());
        }

        @Test
        @DisplayName("Should return field name")
        void getFieldName() {
            ValidationException ex = new ValidationException("Message", "myField");
            assertEquals("myField", ex.getFieldName());
        }

        @Test
        @DisplayName("validate should return value when predicate passes")
        void validateReturnsValue() {
            Integer value = 10;
            Integer result = ValidationException.validate("age", value, v -> v > 0, "Age must be positive");
            assertEquals(value, result);
        }

        @Test
        @DisplayName("validate should throw when predicate fails")
        void validateThrowsOnFailure() {
            ValidationException ex = assertThrows(ValidationException.class, () ->
                ValidationException.validate("age", -5, v -> v > 0, "Age must be positive"));
            assertEquals("Age must be positive", ex.getMessage());
            assertEquals("age", ex.getFieldName());
        }

        @Test
        @DisplayName("validate with default message should work")
        void validateWithDefaultMessage() {
            ValidationException ex = assertThrows(ValidationException.class, () ->
                ValidationException.validate("amount", BigDecimal.ZERO, v -> v.compareTo(BigDecimal.ZERO) > 0));
            assertEquals("Invalid value for amount", ex.getMessage());
            assertEquals("amount", ex.getFieldName());
        }
    }

    @Nested
    @DisplayName("RetirementException Tests")
    class RetirementExceptionTests {

        @Test
        @DisplayName("Should create with message")
        void createWithMessage() {
            RetirementException ex = new RetirementException("Test message");
            assertEquals("Test message", ex.getMessage());
        }

        @Test
        @DisplayName("Should create with message and cause")
        void createWithMessageAndCause() {
            RuntimeException cause = new RuntimeException("Cause");
            RetirementException ex = new RetirementException("Test message", cause);
            assertEquals("Test message", ex.getMessage());
            assertEquals(cause, ex.getCause());
        }
    }

    @Nested
    @DisplayName("ConfigurationException Tests")
    class ConfigurationExceptionTests {

        @Test
        @DisplayName("Should create with message")
        void createWithMessage() {
            ConfigurationException ex = new ConfigurationException("Config error");
            assertEquals("Config error", ex.getMessage());
        }

        @Test
        @DisplayName("Should create with message and cause")
        void createWithMessageAndCause() {
            RuntimeException cause = new RuntimeException("Cause");
            ConfigurationException ex = new ConfigurationException("Config error", cause);
            assertEquals("Config error", ex.getMessage());
            assertEquals(cause, ex.getCause());
        }
    }

    @Nested
    @DisplayName("CalculationException Tests")
    class CalculationExceptionTests {

        @Test
        @DisplayName("Should create with message")
        void createWithMessage() {
            CalculationException ex = new CalculationException("Calc error");
            assertEquals("Calc error", ex.getMessage());
        }

        @Test
        @DisplayName("Should create with message and cause")
        void createWithMessageAndCause() {
            RuntimeException cause = new RuntimeException("Cause");
            CalculationException ex = new CalculationException("Calc error", cause);
            assertEquals("Calc error", ex.getMessage());
            assertEquals(cause, ex.getCause());
        }

        @Test
        @DisplayName("negativeBalance should create proper message")
        void negativeBalanceMessage() {
            CalculationException ex = CalculationException.negativeBalance(
                "return calculation", new BigDecimal("-100.50"));
            assertEquals("Balance cannot be negative for return calculation: -100.50",
                ex.getMessage());
        }

        @Test
        @DisplayName("invalidPeriod should create proper message")
        void invalidPeriodMessage() {
            CalculationException ex = CalculationException.invalidPeriod(-5);
            assertEquals("Calculation period cannot be negative: -5 months",
                ex.getMessage());
        }
    }
}
