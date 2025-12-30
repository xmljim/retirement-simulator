package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;

/**
 * Configuration for guardrails-based dynamic withdrawal strategies.
 *
 * <p>Guardrails strategies adjust spending based on portfolio performance,
 * providing flexibility while protecting against sequence-of-returns risk.
 * This configuration supports three common approaches:
 * <ul>
 *   <li><b>Guyton-Klinger:</b> Rate-based triggers with 10% adjustments</li>
 *   <li><b>Vanguard Dynamic:</b> Ceiling/floor system limiting annual changes</li>
 *   <li><b>Kitces Ratcheting:</b> One-way ratchet that only increases spending</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Use a preset
 * GuardrailsConfiguration config = GuardrailsConfiguration.guytonKlinger();
 *
 * // Or build a custom configuration
 * GuardrailsConfiguration config = GuardrailsConfiguration.builder()
 *     .initialWithdrawalRate(new BigDecimal("0.045"))
 *     .allowSpendingCuts(false)
 *     .build();
 * }</pre>
 *
 * @see GuardrailsSpendingStrategy
 */
public record GuardrailsConfiguration(
        BigDecimal initialWithdrawalRate,
        BigDecimal inflationRate,
        BigDecimal upperThresholdMultiplier,
        BigDecimal increaseAdjustment,
        BigDecimal lowerThresholdMultiplier,
        BigDecimal decreaseAdjustment,
        BigDecimal absoluteFloor,
        BigDecimal absoluteCeiling,
        boolean allowSpendingCuts,
        boolean skipInflationOnDownYears,
        int minimumYearsBetweenRatchets,
        int yearsBeforeCapPreservationEnds
) {

    private static final BigDecimal DEFAULT_INFLATION = new BigDecimal("0.025");

    /**
     * Creates a Guyton-Klinger configuration.
     *
     * <p>Features four decision rules:
     * <ol>
     *   <li>Portfolio Management: Don't sell declining assets</li>
     *   <li>Withdrawal: Skip inflation in down years when rate exceeds initial</li>
     *   <li>Capital Preservation: Cut 10% when rate exceeds 120% of initial</li>
     *   <li>Prosperity: Raise 10% when rate drops below 80% of initial</li>
     * </ol>
     *
     * @return Guyton-Klinger preset configuration
     */
    public static GuardrailsConfiguration guytonKlinger() {
        return new GuardrailsConfiguration(
                new BigDecimal("0.052"),
                DEFAULT_INFLATION,
                new BigDecimal("0.80"),
                new BigDecimal("0.10"),
                new BigDecimal("1.20"),
                new BigDecimal("0.10"),
                null,
                null,
                true,
                true,
                1,
                15
        );
    }

    /**
     * Creates a Vanguard Dynamic Spending configuration.
     *
     * <p>Uses a ceiling-and-floor system that limits annual spending changes:
     * <ul>
     *   <li>Ceiling: Maximum increase of 5% per year</li>
     *   <li>Floor: Maximum decrease of 2.5% per year</li>
     * </ul>
     *
     * @return Vanguard Dynamic preset configuration
     */
    public static GuardrailsConfiguration vanguardDynamic() {
        return new GuardrailsConfiguration(
                new BigDecimal("0.04"),
                DEFAULT_INFLATION,
                null,
                new BigDecimal("0.05"),
                null,
                new BigDecimal("0.025"),
                null,
                null,
                true,
                false,
                1,
                0
        );
    }

    /**
     * Creates a Kitces Ratcheting configuration.
     *
     * <p>One-way ratchet that only increases spending (never decreases):
     * <ul>
     *   <li>Trigger: Portfolio grows to 150% of initial value</li>
     *   <li>Ratchet: Increase spending by 10%</li>
     *   <li>Frequency: Maximum one increase per 3 years</li>
     *   <li>Floor: Never reduce nominal spending</li>
     * </ul>
     *
     * @return Kitces Ratcheting preset configuration
     */
    public static GuardrailsConfiguration kitcesRatcheting() {
        return new GuardrailsConfiguration(
                new BigDecimal("0.04"),
                DEFAULT_INFLATION,
                new BigDecimal("0.667"),
                new BigDecimal("0.10"),
                null,
                BigDecimal.ZERO,
                null,
                null,
                false,
                false,
                3,
                0
        );
    }

    /**
     * Creates a new builder for custom configurations.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns whether this configuration has an upper guardrail.
     */
    public boolean hasUpperGuardrail() {
        return upperThresholdMultiplier != null;
    }

    /**
     * Returns whether this configuration has a lower guardrail.
     */
    public boolean hasLowerGuardrail() {
        return lowerThresholdMultiplier != null && allowSpendingCuts;
    }

    /**
     * Returns whether this configuration has an absolute floor.
     */
    public boolean hasAbsoluteFloor() {
        return absoluteFloor != null;
    }

    /**
     * Returns whether this configuration has an absolute ceiling.
     */
    public boolean hasAbsoluteCeiling() {
        return absoluteCeiling != null;
    }

    /**
     * Builder for creating custom GuardrailsConfiguration instances.
     */
    public static class Builder {
        private BigDecimal initialWithdrawalRate = new BigDecimal("0.04");
        private BigDecimal inflationRate = DEFAULT_INFLATION;
        private BigDecimal upperThresholdMultiplier;
        private BigDecimal increaseAdjustment = new BigDecimal("0.10");
        private BigDecimal lowerThresholdMultiplier;
        private BigDecimal decreaseAdjustment = new BigDecimal("0.10");
        private BigDecimal absoluteFloor;
        private BigDecimal absoluteCeiling;
        private boolean allowSpendingCuts = true;
        private boolean skipInflationOnDownYears = false;
        private int minimumYearsBetweenRatchets = 1;
        private int yearsBeforeCapPreservationEnds = 0;

        /** Sets the initial withdrawal rate (e.g., 0.04 for 4%). */
        public Builder initialWithdrawalRate(BigDecimal rate) {
            this.initialWithdrawalRate = rate;
            return this;
        }

        /** Sets the inflation rate (e.g., 0.025 for 2.5%). */
        public Builder inflationRate(BigDecimal rate) {
            this.inflationRate = rate;
            return this;
        }

        /** Sets the upper guardrail threshold multiplier. */
        public Builder upperThresholdMultiplier(BigDecimal multiplier) {
            this.upperThresholdMultiplier = multiplier;
            return this;
        }

        /** Sets the spending increase adjustment (e.g., 0.10 for 10%). */
        public Builder increaseAdjustment(BigDecimal adjustment) {
            this.increaseAdjustment = adjustment;
            return this;
        }

        /** Sets the lower guardrail threshold multiplier. */
        public Builder lowerThresholdMultiplier(BigDecimal multiplier) {
            this.lowerThresholdMultiplier = multiplier;
            return this;
        }

        /** Sets the spending decrease adjustment (e.g., 0.10 for 10%). */
        public Builder decreaseAdjustment(BigDecimal adjustment) {
            this.decreaseAdjustment = adjustment;
            return this;
        }

        /** Sets the absolute minimum spending floor. */
        public Builder absoluteFloor(BigDecimal floor) {
            this.absoluteFloor = floor;
            return this;
        }

        /** Sets the absolute maximum spending ceiling. */
        public Builder absoluteCeiling(BigDecimal ceiling) {
            this.absoluteCeiling = ceiling;
            return this;
        }

        /** Sets whether spending cuts are allowed. */
        public Builder allowSpendingCuts(boolean allow) {
            this.allowSpendingCuts = allow;
            return this;
        }

        /** Sets whether to skip inflation adjustment in down years. */
        public Builder skipInflationOnDownYears(boolean skip) {
            this.skipInflationOnDownYears = skip;
            return this;
        }

        /** Sets the minimum years between ratchet increases. */
        public Builder minimumYearsBetweenRatchets(int years) {
            this.minimumYearsBetweenRatchets = years;
            return this;
        }

        /** Sets years before cap preservation rule expires. */
        public Builder yearsBeforeCapPreservationEnds(int years) {
            this.yearsBeforeCapPreservationEnds = years;
            return this;
        }

        /** Builds the configuration. */
        public GuardrailsConfiguration build() {
            return new GuardrailsConfiguration(
                    initialWithdrawalRate,
                    inflationRate,
                    upperThresholdMultiplier,
                    increaseAdjustment,
                    lowerThresholdMultiplier,
                    decreaseAdjustment,
                    absoluteFloor,
                    absoluteCeiling,
                    allowSpendingCuts,
                    skipInflationOnDownYears,
                    minimumYearsBetweenRatchets,
                    yearsBeforeCapPreservationEnds
            );
        }
    }
}
