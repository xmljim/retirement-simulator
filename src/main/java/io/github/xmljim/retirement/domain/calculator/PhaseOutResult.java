package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.xmljim.retirement.domain.enums.AccountType;

/**
 * Result of an IRA phase-out calculation.
 *
 * <p>Contains the allowed contribution amount after applying income-based
 * phase-out rules, along with details about the phase-out status.
 *
 * @param accountType the IRA account type evaluated
 * @param requestedContribution the original contribution amount requested
 * @param allowedContribution the maximum contribution allowed after phase-out
 * @param phaseOutPercentage percentage through the phase-out range (0.0-1.0)
 * @param isFullyPhasedOut true if MAGI exceeds the upper phase-out bound
 * @param backdoorRothEligible true if Roth direct contribution is blocked
 *                              but backdoor Roth conversion is available
 * @param deductiblePortion for Traditional IRA, the deductible portion
 *                          (may be less than allowed contribution)
 * @param warnings any warnings or notes about the calculation
 */
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP",
    justification = "Record component returns immutable List.copyOf() from builder"
)
public record PhaseOutResult(
    AccountType accountType,
    BigDecimal requestedContribution,
    BigDecimal allowedContribution,
    BigDecimal phaseOutPercentage,
    boolean isFullyPhasedOut,
    boolean backdoorRothEligible,
    BigDecimal deductiblePortion,
    List<String> warnings
) {

    /**
     * Creates a PhaseOutResult with defaults for null values.
     */
    public PhaseOutResult {
        if (requestedContribution == null) {
            requestedContribution = BigDecimal.ZERO;
        }
        if (allowedContribution == null) {
            allowedContribution = BigDecimal.ZERO;
        }
        if (phaseOutPercentage == null) {
            phaseOutPercentage = BigDecimal.ZERO;
        }
        if (deductiblePortion == null) {
            deductiblePortion = BigDecimal.ZERO;
        }
        if (warnings == null) {
            warnings = List.of();
        }
    }

    /**
     * Returns true if the full requested contribution is allowed.
     *
     * @return true if no reduction due to phase-out
     */
    public boolean isFullContributionAllowed() {
        return allowedContribution.compareTo(requestedContribution) >= 0;
    }

    /**
     * Returns true if any contribution is allowed.
     *
     * @return true if allowed contribution is greater than zero
     */
    public boolean canContribute() {
        return allowedContribution.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Builder for creating PhaseOutResult instances.
     */
    public static class Builder {
        private AccountType accountType;
        private BigDecimal requestedContribution = BigDecimal.ZERO;
        private BigDecimal allowedContribution = BigDecimal.ZERO;
        private BigDecimal phaseOutPercentage = BigDecimal.ZERO;
        private boolean isFullyPhasedOut;
        private boolean backdoorRothEligible;
        private BigDecimal deductiblePortion = BigDecimal.ZERO;
        private List<String> warnings = new ArrayList<>();

        /**
         * Sets the account type.
         *
         * @param accountType the IRA account type
         * @return this builder
         */
        public Builder accountType(AccountType accountType) {
            this.accountType = accountType;
            return this;
        }

        /**
         * Sets the requested contribution amount.
         *
         * @param amount the requested amount
         * @return this builder
         */
        public Builder requestedContribution(BigDecimal amount) {
            this.requestedContribution = amount;
            return this;
        }

        /**
         * Sets the allowed contribution amount.
         *
         * @param amount the allowed amount after phase-out
         * @return this builder
         */
        public Builder allowedContribution(BigDecimal amount) {
            this.allowedContribution = amount;
            return this;
        }

        /**
         * Sets the phase-out percentage.
         *
         * @param percentage the percentage (0.0 to 1.0)
         * @return this builder
         */
        public Builder phaseOutPercentage(BigDecimal percentage) {
            this.phaseOutPercentage = percentage;
            return this;
        }

        /**
         * Sets whether fully phased out.
         *
         * @param fullyPhasedOut true if MAGI exceeds upper bound
         * @return this builder
         */
        public Builder isFullyPhasedOut(boolean fullyPhasedOut) {
            this.isFullyPhasedOut = fullyPhasedOut;
            return this;
        }

        /**
         * Sets whether backdoor Roth is eligible.
         *
         * @param eligible true if backdoor available
         * @return this builder
         */
        public Builder backdoorRothEligible(boolean eligible) {
            this.backdoorRothEligible = eligible;
            return this;
        }

        /**
         * Sets the deductible portion for Traditional IRA.
         *
         * @param deductible the deductible amount
         * @return this builder
         */
        public Builder deductiblePortion(BigDecimal deductible) {
            this.deductiblePortion = deductible;
            return this;
        }

        /**
         * Adds a warning message.
         *
         * @param warning the warning
         * @return this builder
         */
        public Builder addWarning(String warning) {
            this.warnings.add(warning);
            return this;
        }

        /**
         * Builds the PhaseOutResult.
         *
         * @return the constructed result
         */
        public PhaseOutResult build() {
            return new PhaseOutResult(
                accountType,
                requestedContribution,
                allowedContribution,
                phaseOutPercentage,
                isFullyPhasedOut,
                backdoorRothEligible,
                deductiblePortion,
                List.copyOf(warnings)
            );
        }
    }

    /**
     * Creates a new builder.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}
