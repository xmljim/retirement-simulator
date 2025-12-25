package io.github.xmljim.retirement.domain.exception;

/**
 * Exception thrown when date relationships are invalid.
 *
 * <p>This exception is used when dates violate logical constraints,
 * such as a retirement date before birth date or a start date after
 * the projected end date.
 */
public class InvalidDateRangeException extends ValidationException {

    /**
     * Constructs an invalid date range exception.
     *
     * @param message the detail message
     * @param fieldName the name of the date field
     */
    public InvalidDateRangeException(String message, String fieldName) {
        super(message, fieldName);
    }

    /**
     * Creates an exception for a date that cannot be before another date.
     *
     * @param laterDateName the name of the date that should be later
     * @param earlierDateName the name of the date that should be earlier
     * @return a new InvalidDateRangeException
     */
    public static InvalidDateRangeException dateMustBeAfter(
            String laterDateName, String earlierDateName) {
        return new InvalidDateRangeException(
            String.format("%s cannot be before %s", laterDateName, earlierDateName),
            laterDateName.toLowerCase().replace(" ", "_"));
    }

    /**
     * Creates an exception for a date that cannot be after another date.
     *
     * @param earlierDateName the name of the date that should be earlier
     * @param laterDateName the name of the date that should be later
     * @return a new InvalidDateRangeException
     */
    public static InvalidDateRangeException dateMustBeBefore(
            String earlierDateName, String laterDateName) {
        return new InvalidDateRangeException(
            String.format("%s cannot be after %s", earlierDateName, laterDateName),
            earlierDateName.toLowerCase().replace(" ", "_"));
    }
}
