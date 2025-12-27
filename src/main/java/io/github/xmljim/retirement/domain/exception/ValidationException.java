package io.github.xmljim.retirement.domain.exception;

import java.util.function.Predicate;

/**
 * Exception thrown when validation of domain objects fails.
 *
 * <p>This exception serves as the base for all validation-related
 * exceptions in the retirement simulator domain.
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
     * Validates a value using a predicate, throwing an exception if validation fails.
     *
     * <p>This utility method provides a fluent way to validate values and return
     * them if valid, enabling inline validation in builders and constructors.
     *
     * @param <T> the type of value being validated
     * @param fieldName the name of the field for error messages
     * @param value the value to validate
     * @param test the predicate that returns true if the value is valid
     * @param message the error message if validation fails
     * @return the value if validation passes
     * @throws ValidationException if the predicate returns false
     */
    public static <T> T validate(String fieldName, T value, Predicate<T> test, String message) {
        if (!test.test(value)) {
            throw new ValidationException(message, fieldName);
        }
        return value;
    }
}
