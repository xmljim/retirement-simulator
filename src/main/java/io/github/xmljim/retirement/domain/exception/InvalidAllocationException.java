package io.github.xmljim.retirement.domain.exception;

import java.math.BigDecimal;

/**
 * Exception thrown when asset allocation percentages are invalid.
 *
 * <p>This exception is thrown when allocation percentages don't sum
 * to 100% or when individual percentages are outside the valid range.
 */
public class InvalidAllocationException extends ValidationException {

    /**
     * Constructs an invalid allocation exception.
     *
     * @param message the detail message
     */
    public InvalidAllocationException(String message) {
        super(message, "allocation");
    }

    /**
     * Creates an exception for allocations that don't sum to 100%.
     *
     * @param actualSum the actual sum of allocations
     * @return a new InvalidAllocationException
     */
    public static InvalidAllocationException invalidSum(BigDecimal actualSum) {
        return new InvalidAllocationException(
            String.format("Asset allocation must sum to 100%%, but was %s%%",
                actualSum.stripTrailingZeros().toPlainString()));
    }

    /**
     * Creates an exception for a percentage outside the valid range.
     *
     * @param assetType the type of asset (stocks, bonds, cash)
     * @param value the invalid percentage value
     * @return a new InvalidAllocationException
     */
    public static InvalidAllocationException outOfRange(String assetType, BigDecimal value) {
        return new InvalidAllocationException(
            String.format("%s percentage must be between 0 and 100, but was %s",
                assetType, value.stripTrailingZeros().toPlainString()));
    }
}
