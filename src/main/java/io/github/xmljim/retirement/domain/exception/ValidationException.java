package io.github.xmljim.retirement.domain.exception;

import java.util.function.Predicate;

/**
 * Exception thrown when validation of domain objects fails.
 *
 * <p>This exception serves as the base for all validation-related
 * exceptions in the retirement simulator domain.
 *
 * <p>Example usage with static validate method:
 * <pre>{@code
 * ValidationException.validate("monthlyBenefit", benefit,
 *     v -> v.compareTo(BigDecimal.ZERO) >= 0,
 *     "Monthly benefit cannot be negative");
 * }</pre>
 */
public class ValidationException extends RetirementException {

    private final String fieldName;

    /**
     * Constructs a validation exception with a message.
     *
     * @param message the detail message
     */
    public ValidationException(String message) {
        super(message);
        this.fieldName = null;
    }

    /**
     * Constructs a validation exception with a message and field name.
     *
     * @param message the detail message
     * @param fieldName the name of the field that failed validation
     */
    public ValidationException(String message, String fieldName) {
        super(message);
        this.fieldName = fieldName;
    }

    /**
     * Constructs a validation exception with a message, field name, and cause.
     *
     * @param message the detail message
     * @param fieldName the name of the field that failed validation
     * @param cause the cause of this exception
     */
    public ValidationException(String message, String fieldName, Throwable cause) {
        super(message, cause);
        this.fieldName = fieldName;
    }

    /**
     * Returns the name of the field that failed validation, if available.
     *
     * @return the field name, or null if not specified
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Validates a value against a predicate, throwing if validation fails.
     *
     * <p>Example usage:
     * <pre>{@code
     * ValidationException.validate("age", age,
     *     v -> v >= 0 && v <= 120,
     *     "Age must be between 0 and 120");
     * }</pre>
     *
     * @param fieldName the name of the field being validated
     * @param value the value to validate
     * @param test the predicate that must return true for valid values
     * @param message the error message if validation fails
     * @param <T> the type of the value
     * @return the value if validation passes
     * @throws ValidationException if the predicate returns false
     */
    public static <T> T validate(String fieldName, T value, Predicate<T> test, String message) {
        if (!test.test(value)) {
            throw new ValidationException(message, fieldName);
        }
        return value;
    }

    /**
     * Validates a value against a predicate with a default message.
     *
     * @param fieldName the name of the field being validated
     * @param value the value to validate
     * @param test the predicate that must return true for valid values
     * @param <T> the type of the value
     * @return the value if validation passes
     * @throws ValidationException if the predicate returns false
     */
    public static <T> T validate(String fieldName, T value, Predicate<T> test) {
        return validate(fieldName, value, test, "Invalid value for " + fieldName);
    }
}
