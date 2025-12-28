package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

import io.github.xmljim.retirement.domain.calculator.ExpenseModifier;
import io.github.xmljim.retirement.domain.enums.SpendingPhase;

/**
 * Modifier that adjusts discretionary spending based on retirement phase.
 *
 * <p>Implements the Go-Go/Slow-Go/No-Go spending curve model documented
 * in retirement research. Discretionary spending typically decreases as
 * retirees age and become less active.
 *
 * <p>Default multipliers:
 * <ul>
 *   <li>Go-Go (65-74): 100% - full discretionary spending</li>
 *   <li>Slow-Go (75-84): 80% - reduced activity</li>
 *   <li>No-Go (85+): 50% - minimal discretionary</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Using default settings
 * ExpenseModifier curve = SpendingCurveModifier.withDefaults();
 *
 * // Custom age ranges and multipliers
 * ExpenseModifier custom = SpendingCurveModifier.builder()
 *     .phaseStartAge(SpendingPhase.SLOW_GO, 72)
 *     .phaseStartAge(SpendingPhase.NO_GO, 82)
 *     .multiplier(SpendingPhase.SLOW_GO, new BigDecimal("0.75"))
 *     .build();
 * }</pre>
 */
public final class SpendingCurveModifier implements ExpenseModifier {

    private static final int SCALE = 2;

    private final Map<SpendingPhase, BigDecimal> multipliers;
    private final int slowGoStartAge;
    private final int noGoStartAge;

    private SpendingCurveModifier(Builder builder) {
        this.multipliers = new EnumMap<>(builder.multipliers);
        this.slowGoStartAge = builder.slowGoStartAge;
        this.noGoStartAge = builder.noGoStartAge;
    }

    /**
     * Creates a modifier with default settings.
     *
     * @return a new SpendingCurveModifier with defaults
     */
    public static SpendingCurveModifier withDefaults() {
        return builder().build();
    }

    /**
     * Creates a new builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public BigDecimal modify(BigDecimal baseAmount, LocalDate date, int age) {
        SpendingPhase phase = getPhaseForAge(age);
        BigDecimal multiplier = multipliers.get(phase);
        return baseAmount.multiply(multiplier).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Determines the spending phase for a given age.
     *
     * @param age the person's age
     * @return the spending phase
     */
    public SpendingPhase getPhaseForAge(int age) {
        if (age >= noGoStartAge) {
            return SpendingPhase.NO_GO;
        } else if (age >= slowGoStartAge) {
            return SpendingPhase.SLOW_GO;
        } else {
            return SpendingPhase.GO_GO;
        }
    }

    /**
     * Returns the multiplier for a specific phase.
     *
     * @param phase the spending phase
     * @return the multiplier for that phase
     */
    public BigDecimal getMultiplier(SpendingPhase phase) {
        return multipliers.get(phase);
    }

    /**
     * Returns the age when Slow-Go phase begins.
     *
     * @return the Slow-Go start age
     */
    public int getSlowGoStartAge() {
        return slowGoStartAge;
    }

    /**
     * Returns the age when No-Go phase begins.
     *
     * @return the No-Go start age
     */
    public int getNoGoStartAge() {
        return noGoStartAge;
    }

    /**
     * Builder for SpendingCurveModifier.
     */
    public static final class Builder {
        private final Map<SpendingPhase, BigDecimal> multipliers = new EnumMap<>(SpendingPhase.class);
        private int slowGoStartAge = SpendingPhase.SLOW_GO.getDefaultStartAge();
        private int noGoStartAge = SpendingPhase.NO_GO.getDefaultStartAge();

        private Builder() {
            // Initialize with defaults
            for (SpendingPhase phase : SpendingPhase.values()) {
                multipliers.put(phase, phase.getDefaultMultiplier());
            }
        }

        /**
         * Sets a custom multiplier for a spending phase.
         *
         * @param phase the spending phase
         * @param multiplier the multiplier (0.0 to 1.0 typically)
         * @return this builder
         */
        public Builder multiplier(SpendingPhase phase, BigDecimal multiplier) {
            multipliers.put(phase, multiplier);
            return this;
        }

        /**
         * Sets the start age for the Slow-Go phase.
         *
         * @param age the start age
         * @return this builder
         */
        public Builder slowGoStartAge(int age) {
            this.slowGoStartAge = age;
            return this;
        }

        /**
         * Sets the start age for the No-Go phase.
         *
         * @param age the start age
         * @return this builder
         */
        public Builder noGoStartAge(int age) {
            this.noGoStartAge = age;
            return this;
        }

        /**
         * Sets the start age for a specific phase.
         *
         * @param phase the phase (SLOW_GO or NO_GO)
         * @param age the start age
         * @return this builder
         */
        public Builder phaseStartAge(SpendingPhase phase, int age) {
            if (phase == SpendingPhase.SLOW_GO) {
                this.slowGoStartAge = age;
            } else if (phase == SpendingPhase.NO_GO) {
                this.noGoStartAge = age;
            }
            return this;
        }

        /**
         * Builds the modifier.
         *
         * @return a new SpendingCurveModifier
         */
        public SpendingCurveModifier build() {
            return new SpendingCurveModifier(this);
        }
    }
}
