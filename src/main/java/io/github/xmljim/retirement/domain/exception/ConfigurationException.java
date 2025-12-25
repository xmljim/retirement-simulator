package io.github.xmljim.retirement.domain.exception;

/**
 * Exception thrown when configuration of domain objects is invalid.
 *
 * <p>This exception is used for configuration errors that don't fit
 * into the validation category, such as incompatible settings or
 * invalid combinations of options.
 */
public class ConfigurationException extends RetirementException {

    /**
     * Constructs a configuration exception.
     *
     * @param message the detail message
     */
    public ConfigurationException(String message) {
        super(message);
    }

    /**
     * Constructs a configuration exception with a cause.
     *
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
