package io.github.xmljim.retirement.domain.exception;

import java.math.BigDecimal;

/**
 * Exception thrown when a rate value is outside the valid range.
 *
 * <p>This exception is used for both return rates and inflation rates
 * that fall outside acceptable bounds.
 */
public class InvalidRateException extends ValidationException {

    /**
     * Constructs an invalid rate exception.
     *
     * @param message the detail message
     * @param fieldName the name of the rate field
     */
    public InvalidRateException(String message, String fieldName) {
        super(message, fieldName);
    }

    /**
     * Creates an exception for a return rate outside the valid range.
     *
     * @param rateName the name of the rate (e.g., "Pre-retirement return rate")
     * @param value the invalid rate value
     * @param minRate the minimum allowed rate
     * @param maxRate the maximum allowed rate
     * @return a new InvalidRateException
     */
    public static InvalidRateException returnRateOutOfRange(
            String rateName, BigDecimal value, BigDecimal minRate, BigDecimal maxRate) {
        BigDecimal hundred = new BigDecimal("100");
        return new InvalidRateException(
            String.format("%s must be between %s%% and %s%%, but was %s%%",
                rateName,
                minRate.multiply(hundred).stripTrailingZeros().toPlainString(),
                maxRate.multiply(hundred).stripTrailingZeros().toPlainString(),
                value.multiply(hundred).stripTrailingZeros().toPlainString()),
            rateName.toLowerCase().replace(" ", "_"));
    }

    /**
     * Creates an exception for an inflation rate outside the valid range.
     *
     * @param rateName the name of the inflation rate
     * @param value the invalid rate value
     * @return a new InvalidRateException
     */
    public static InvalidRateException inflationRateOutOfRange(String rateName, BigDecimal value) {
        BigDecimal hundred = new BigDecimal("100");
        return new InvalidRateException(
            String.format("%s rate must be between -10%% and 20%%, but was %s%%",
                rateName, value.multiply(hundred).stripTrailingZeros().toPlainString()),
            rateName.toLowerCase().replace(" ", "_"));
    }
}
