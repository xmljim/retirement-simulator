package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;

/**
 * Result of checking a proposed contribution against IRS limits.
 *
 * <p>Contains information about whether the contribution is allowed,
 * how much can actually be contributed, and any warnings generated.
 *
 * <p>Example usage:
 * <pre>{@code
 * LimitCheckResult result = limitChecker.check(...);
 *
 * if (result.allowed()) {
 *     // Make contribution of result.allowedAmount()
 *     if (result.hasWarnings()) {
 *         result.warnings().forEach(System.out::println);
 *     }
 * } else {
 *     System.out.println("Contribution denied: " + result.warnings().get(0));
 * }
 * }</pre>
 *
 * @param allowed true if any amount of the contribution is allowed
 * @param allowedAmount the amount that can be contributed (may be less than requested)
 * @param remainingRoom room remaining after this contribution would be applied
 * @param warnings list of warning messages (e.g., partial contribution, limit reached)
 */
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP",
    justification = "Record components are made unmodifiable in compact constructor"
)
public record LimitCheckResult(
    boolean allowed,
    BigDecimal allowedAmount,
    BigDecimal remainingRoom,
    List<String> warnings
) {
    // Compact constructor for validation and immutability
    public LimitCheckResult {
        MissingRequiredFieldException.requireNonNull(allowedAmount, "allowedAmount");
        MissingRequiredFieldException.requireNonNull(remainingRoom, "remainingRoom");
        warnings = warnings != null
            ? Collections.unmodifiableList(new ArrayList<>(warnings))
            : Collections.emptyList();
    }

    /**
     * Indicates whether there are any warnings.
     *
     * @return true if warnings list is not empty
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * Indicates whether the full requested amount was allowed.
     *
     * <p>Returns false if the contribution was reduced due to limits.
     *
     * @param requestedAmount the originally requested amount
     * @return true if allowedAmount equals requestedAmount
     */
    public boolean isFullAmountAllowed(BigDecimal requestedAmount) {
        return allowedAmount.compareTo(requestedAmount) >= 0;
    }

    /**
     * Creates a result indicating the full contribution is allowed.
     *
     * @param amount the full amount allowed
     * @param remainingRoom room remaining after contribution
     * @return a new LimitCheckResult
     */
    public static LimitCheckResult allowed(BigDecimal amount, BigDecimal remainingRoom) {
        return new LimitCheckResult(true, amount, remainingRoom, Collections.emptyList());
    }

    /**
     * Creates a result indicating a partial contribution is allowed.
     *
     * @param allowedAmount the amount that can be contributed
     * @param remainingRoom room remaining after contribution (typically zero)
     * @param warning the warning message explaining the reduction
     * @return a new LimitCheckResult
     */
    public static LimitCheckResult partial(BigDecimal allowedAmount, BigDecimal remainingRoom, String warning) {
        return new LimitCheckResult(true, allowedAmount, remainingRoom, List.of(warning));
    }

    /**
     * Creates a result indicating no contribution is allowed.
     *
     * @param reason the reason the contribution was denied
     * @return a new LimitCheckResult
     */
    public static LimitCheckResult denied(String reason) {
        return new LimitCheckResult(false, BigDecimal.ZERO, BigDecimal.ZERO, List.of(reason));
    }

    /**
     * Creates a builder for LimitCheckResult.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating LimitCheckResult instances.
     */
    public static class Builder {
        private boolean allowed = true;
        private BigDecimal allowedAmount = BigDecimal.ZERO;
        private BigDecimal remainingRoom = BigDecimal.ZERO;
        private final List<String> warnings = new ArrayList<>();

        /**
         * Sets whether any contribution is allowed.
         *
         * @param allowed true if allowed
         * @return this builder
         */
        public Builder allowed(boolean allowed) {
            this.allowed = allowed;
            return this;
        }

        /**
         * Sets the allowed contribution amount.
         *
         * @param amount the amount
         * @return this builder
         */
        public Builder allowedAmount(BigDecimal amount) {
            this.allowedAmount = amount;
            return this;
        }

        /**
         * Sets the remaining room after contribution.
         *
         * @param room the remaining room
         * @return this builder
         */
        public Builder remainingRoom(BigDecimal room) {
            this.remainingRoom = room;
            return this;
        }

        /**
         * Adds a warning message.
         *
         * @param warning the warning
         * @return this builder
         */
        public Builder addWarning(String warning) {
            if (warning != null && !warning.isBlank()) {
                warnings.add(warning);
            }
            return this;
        }

        /**
         * Builds the LimitCheckResult.
         *
         * @return a new LimitCheckResult
         */
        public LimitCheckResult build() {
            return new LimitCheckResult(allowed, allowedAmount, remainingRoom, warnings);
        }
    }
}
