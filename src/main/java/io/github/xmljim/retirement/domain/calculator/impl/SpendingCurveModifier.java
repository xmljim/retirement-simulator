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
 * in retirement research. Discretionary spending typically:
 * <ul>
 *   <li>Decreases gradually through Go-Go years (active early retirement)</li>
 *   <li>Levels off during Slow-Go years (reduced activity)</li>
 *   <li>Remains low in No-Go years (minimal discretionary, but healthcare rises)</li>
 * </ul>
 *
 * <p>When interpolation is enabled (default), multipliers transition gradually
 * rather than dropping as "cliffs" at phase boundaries.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Gradual transitions (default)
 * ExpenseModifier curve = SpendingCurveModifier.withDefaults();
 *
 * // Cliff transitions (no interpolation)
 * ExpenseModifier stepped = SpendingCurveModifier.builder()
 *     .interpolate(false)
 *     .build();
 * }</pre>
 */
public final class SpendingCurveModifier implements ExpenseModifier {

    private static final int SCALE = 4;
    private static final int RESULT_SCALE = 2;

    private final Map<SpendingPhase, BigDecimal> multipliers;
    private final int goGoStartAge;
    private final int slowGoStartAge;
    private final int noGoStartAge;
    private final boolean interpolate;

    private SpendingCurveModifier(Builder builder) {
        this.multipliers = new EnumMap<>(builder.multipliers);
        this.goGoStartAge = builder.goGoStartAge;
        this.slowGoStartAge = builder.slowGoStartAge;
        this.noGoStartAge = builder.noGoStartAge;
        this.interpolate = builder.interpolate;
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
        BigDecimal multiplier = calculateMultiplier(age);
        return baseAmount.multiply(multiplier).setScale(RESULT_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateMultiplier(int age) {
        if (!interpolate) {
            return multipliers.get(getPhaseForAge(age));
        }

        // Interpolate within Go-Go phase (gradually decrease toward Slow-Go)
        if (age < slowGoStartAge) {
            return interpolateBetween(
                    age, goGoStartAge, slowGoStartAge,
                    multipliers.get(SpendingPhase.GO_GO),
                    multipliers.get(SpendingPhase.SLOW_GO));
        }

        // Slow-Go phase: level off (no interpolation - stable spending)
        if (age < noGoStartAge) {
            return multipliers.get(SpendingPhase.SLOW_GO);
        }

        // No-Go phase: use No-Go multiplier
        return multipliers.get(SpendingPhase.NO_GO);
    }

    private BigDecimal interpolateBetween(int age, int startAge, int endAge,
                                          BigDecimal startValue, BigDecimal endValue) {
        if (age <= startAge) {
            return startValue;
        }
        if (age >= endAge) {
            return endValue;
        }

        int yearsIntoPhase = age - startAge;
        int phaseLength = endAge - startAge;
        BigDecimal progress = BigDecimal.valueOf(yearsIntoPhase)
                .divide(BigDecimal.valueOf(phaseLength), SCALE, RoundingMode.HALF_UP);

        BigDecimal range = endValue.subtract(startValue);
        return startValue.add(range.multiply(progress)).setScale(SCALE, RoundingMode.HALF_UP);
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
        private int goGoStartAge = SpendingPhase.GO_GO.getDefaultStartAge();
        private int slowGoStartAge = SpendingPhase.SLOW_GO.getDefaultStartAge();
        private int noGoStartAge = SpendingPhase.NO_GO.getDefaultStartAge();
        private boolean interpolate = true;

        private Builder() {
            // Initialize with defaults
            for (SpendingPhase phase : SpendingPhase.values()) {
                multipliers.put(phase, phase.getDefaultMultiplier());
            }
        }

        /**
         * Enables or disables interpolation between phases.
         *
         * <p>When enabled (default), spending multipliers transition gradually
         * during the Go-Go phase. When disabled, multipliers change abruptly
         * at phase boundaries ("cliff" transitions).
         *
         * @param interpolate true for gradual transitions, false for step changes
         * @return this builder
         */
        public Builder interpolate(boolean interpolate) {
            this.interpolate = interpolate;
            return this;
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
