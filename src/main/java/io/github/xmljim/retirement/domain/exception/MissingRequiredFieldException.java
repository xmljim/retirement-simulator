package io.github.xmljim.retirement.domain.exception;

/**
 * Exception thrown when a required field is null or missing.
 *
 * <p>This exception replaces the use of {@link NullPointerException}
 * for validation of required fields, providing more semantic clarity
 * about the nature of the error.
 *
 * <p>Example usage:
 * <pre>{@code
 * public Builder name(String name) {
 *     MissingRequiredFieldException.requireNonNull(name, "name");
 *     this.name = name;
 *     return this;
 * }
 * }</pre>
 */
public class MissingRequiredFieldException extends ValidationException {

    /**
     * Constructs a missing required field exception.
     *
     * @param fieldName the name of the missing field
     */
    public MissingRequiredFieldException(String fieldName) {
        super(fieldName + " is required", fieldName);
    }

    /**
     * Constructs a missing required field exception with a custom message.
     *
     * @param message the detail message
     * @param fieldName the name of the missing field
     */
    public MissingRequiredFieldException(String message, String fieldName) {
        super(message, fieldName);
    }

    /**
     * Checks that the specified value is not null.
     *
     * @param value the value to check
     * @param fieldName the name of the field being checked
     * @param <T> the type of the value
     * @return the value if not null
     * @throws MissingRequiredFieldException if the value is null
     */
    public static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new MissingRequiredFieldException(fieldName);
        }
        return value;
    }
}
