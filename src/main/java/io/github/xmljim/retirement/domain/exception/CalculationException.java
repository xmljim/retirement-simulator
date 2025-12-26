package io.github.xmljim.retirement.domain.exception;

import java.math.BigDecimal;

/**
 * Exception thrown when a calculation fails.
 *
 * <p>This exception is used for errors that occur during retirement
 * projections or other calculations, such as division by zero,
 * overflow, or other mathematical errors.
 *
 * <p>Use the static factory methods for common calculation errors:
 * <pre>{@code
 * throw CalculationException.negativeBalance("return calculation", balance);
 * throw CalculationException.invalidPeriod(-5);
 * }</pre>
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

    /**
     * Creates an exception for a negative balance in calculations.
     *
     * <p>Balances should not be negative for most financial calculations
     * such as return calculations and growth projections.
     *
     * @param context description of the calculation context
     * @param value the invalid negative balance
     * @return a new CalculationException with a descriptive message
     */
    public static CalculationException negativeBalance(String context, BigDecimal value) {
        return new CalculationException(
            String.format("Balance cannot be negative for %s: %s", context, value));
    }

    /**
     * Creates an exception for an invalid calculation period.
     *
     * <p>Calculation periods (in months) cannot be negative.
     *
     * @param months the invalid negative month value
     * @return a new CalculationException with a descriptive message
     */
    public static CalculationException invalidPeriod(int months) {
        return new CalculationException(
            String.format("Calculation period cannot be negative: %d months", months));
    }
}
