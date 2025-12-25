package io.github.xmljim.retirement.domain.exception;

/**
 * Exception thrown when a calculation fails.
 *
 * <p>This exception is used for errors that occur during retirement
 * projections or other calculations, such as division by zero,
 * overflow, or other mathematical errors.
 */
public class CalculationException extends RetirementException {

    /**
     * Constructs a calculation exception.
     *
     * @param message the detail message
     */
    public CalculationException(String message) {
        super(message);
    }

    /**
     * Constructs a calculation exception with a cause.
     *
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public CalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}
