/**
 * Custom exception hierarchy for the retirement simulator domain.
 *
 * <p>All exceptions in this package extend {@link RetirementException},
 * which is an unchecked exception. This allows for semantic, domain-specific
 * error handling without requiring explicit exception handling at every call site.
 *
 * <h2>Exception Hierarchy</h2>
 * <pre>
 * RetirementException (extends RuntimeException)
 * ├── ValidationException
 * │   ├── MissingRequiredFieldException
 * │   ├── InvalidAllocationException
 * │   ├── InvalidRateException
 * │   └── InvalidDateRangeException
 * ├── ConfigurationException
 * └── CalculationException
 * </pre>
 */
package io.github.xmljim.retirement.domain.exception;
