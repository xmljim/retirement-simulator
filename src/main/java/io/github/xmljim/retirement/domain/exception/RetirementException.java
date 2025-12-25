package io.github.xmljim.retirement.domain.exception;

/**
 * Base exception for all retirement simulator domain exceptions.
 *
 * <p>This is an unchecked exception that serves as the root of the
 * exception hierarchy for the retirement simulator. All domain-specific
 * exceptions should extend this class.
 *
 * <p>Using unchecked exceptions allows for cleaner code without
 * requiring explicit exception handling at every call site, while
 * still providing semantic, domain-specific error information.
 */
public class RetirementException extends RuntimeException {

    /**
     * Constructs a new retirement exception with the specified message.
     *
     * @param message the detail message
     */
    public RetirementException(String message) {
        super(message);
    }

    /**
     * Constructs a new retirement exception with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public RetirementException(String message, Throwable cause) {
        super(message, cause);
    }
}
