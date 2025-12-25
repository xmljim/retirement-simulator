package io.github.xmljim.retirement.domain.exception;

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
}
