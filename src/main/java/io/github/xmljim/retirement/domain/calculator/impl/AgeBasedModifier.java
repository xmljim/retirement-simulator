package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.NavigableMap;
import java.util.TreeMap;

import io.github.xmljim.retirement.domain.calculator.ExpenseModifier;

/**
 * Modifier that adjusts expenses based on age using defined brackets.
 *
 * <p>Useful for expenses that increase with age, particularly healthcare.
 * Research shows healthcare costs typically increase as follows:
 * <ul>
 *   <li>Age 65-74: Base level (1.0x)</li>
 *   <li>Age 75-84: ~1.5x base</li>
 *   <li>Age 85+: ~2.0x base or higher</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Healthcare with increasing costs
 * ExpenseModifier healthcare = AgeBasedModifier.builder()
 *     .addBracket(65, new BigDecimal("1.0"))
 *     .addBracket(75, new BigDecimal("1.5"))
 *     .addBracket(85, new BigDecimal("2.0"))
 *     .build();
 *
 * // At age 70: returns base * 1.0
 * // At age 80: returns base * 1.5
 * // At age 90: returns base * 2.0
 * }</pre>
 */
public final class AgeBasedModifier implements ExpenseModifier {

    private static final int SCALE = 2;

    private final NavigableMap<Integer, BigDecimal> brackets;
    private final BigDecimal defaultMultiplier;

    private AgeBasedModifier(Builder builder) {
        this.brackets = new TreeMap<>(builder.brackets);
        this.defaultMultiplier = builder.defaultMultiplier;
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
        BigDecimal multiplier = getMultiplierForAge(age);
        return baseAmount.multiply(multiplier).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Returns the multiplier for a given age.
     *
     * <p>Uses the highest bracket that the age qualifies for.
     *
     * @param age the person's age
     * @return the multiplier for that age
     */
    public BigDecimal getMultiplierForAge(int age) {
        Integer bracketAge = brackets.floorKey(age);
        if (bracketAge == null) {
            return defaultMultiplier;
        }
        return brackets.get(bracketAge);
    }

    /**
     * Returns all defined brackets.
     *
     * @return map of age to multiplier
     */
    public NavigableMap<Integer, BigDecimal> getBrackets() {
        return new TreeMap<>(brackets);
    }

    /**
     * Builder for AgeBasedModifier.
     */
    public static class Builder {
        private final NavigableMap<Integer, BigDecimal> brackets = new TreeMap<>();
        private BigDecimal defaultMultiplier = BigDecimal.ONE;

        /**
         * Adds an age bracket with a multiplier.
         *
         * <p>The multiplier applies from this age until the next bracket.
         *
         * @param age the starting age for this bracket
         * @param multiplier the multiplier to apply
         * @return this builder
         */
        public Builder addBracket(int age, BigDecimal multiplier) {
            brackets.put(age, multiplier);
            return this;
        }

        /**
         * Adds an age bracket with a double multiplier.
         *
         * @param age the starting age for this bracket
         * @param multiplier the multiplier to apply
         * @return this builder
         */
        public Builder addBracket(int age, double multiplier) {
            brackets.put(age, BigDecimal.valueOf(multiplier));
            return this;
        }

        /**
         * Sets the default multiplier for ages below the first bracket.
         *
         * @param multiplier the default multiplier
         * @return this builder
         */
        public Builder defaultMultiplier(BigDecimal multiplier) {
            this.defaultMultiplier = multiplier;
            return this;
        }

        /**
         * Builds the modifier.
         *
         * @return a new AgeBasedModifier
         */
        public AgeBasedModifier build() {
            return new AgeBasedModifier(this);
        }
    }
}
