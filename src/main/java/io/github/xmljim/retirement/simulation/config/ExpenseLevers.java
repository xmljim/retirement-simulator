package io.github.xmljim.retirement.simulation.config;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.xmljim.retirement.domain.calculator.ExpenseModifier;
import io.github.xmljim.retirement.domain.enums.ExpenseCategory.InflationType;

/**
 * Expense-related assumptions for simulation.
 *
 * <p>Controls how expenses evolve over time through:
 * <ul>
 *   <li>Category-specific inflation rates</li>
 *   <li>Healthcare cost trends</li>
 *   <li>Modifiers for spending phases and survivor scenarios</li>
 * </ul>
 *
 * @param inflationRates inflation rates by category type
 * @param healthcareTrend additional healthcare cost increase above inflation
 * @param discretionaryModifier modifier for discretionary spending
 * @param essentialsModifier modifier for essential spending
 * @param healthcareModifier modifier for healthcare spending
 * @param survivorModifier modifier applied in survivor scenarios
 *
 * @see ExpenseModifier
 * @see InflationType
 * @see SimulationLevers
 */
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "Map is made unmodifiable in compact constructor"
)
public record ExpenseLevers(
        Map<InflationType, BigDecimal> inflationRates,
        BigDecimal healthcareTrend,
        ExpenseModifier discretionaryModifier,
        ExpenseModifier essentialsModifier,
        ExpenseModifier healthcareModifier,
        ExpenseModifier survivorModifier
) {

    /** Default general inflation: 2.5%. */
    public static final BigDecimal DEFAULT_GENERAL_INFLATION = new BigDecimal("0.025");

    /** Default healthcare inflation: 5.5%. */
    public static final BigDecimal DEFAULT_HEALTHCARE_INFLATION = new BigDecimal("0.055");

    /** Default housing inflation: 3.0%. */
    public static final BigDecimal DEFAULT_HOUSING_INFLATION = new BigDecimal("0.03");

    /** Default LTC inflation: 4.0%. */
    public static final BigDecimal DEFAULT_LTC_INFLATION = new BigDecimal("0.04");

    /** Default healthcare trend above inflation: 2.0%. */
    public static final BigDecimal DEFAULT_HEALTHCARE_TREND = new BigDecimal("0.02");

    /**
     * Compact constructor with null-safe defaults and defensive copying.
     */
    public ExpenseLevers {
        if (inflationRates == null || inflationRates.isEmpty()) {
            inflationRates = defaultInflationRates();
        } else {
            inflationRates = Collections.unmodifiableMap(new EnumMap<>(inflationRates));
        }
        if (healthcareTrend == null) {
            healthcareTrend = DEFAULT_HEALTHCARE_TREND;
        }
        if (discretionaryModifier == null) {
            discretionaryModifier = ExpenseModifier.identity();
        }
        if (essentialsModifier == null) {
            essentialsModifier = ExpenseModifier.identity();
        }
        if (healthcareModifier == null) {
            healthcareModifier = ExpenseModifier.identity();
        }
        if (survivorModifier == null) {
            survivorModifier = ExpenseModifier.identity();
        }
    }

    /**
     * Creates expense levers with default assumptions.
     *
     * @return levers with default values
     */
    public static ExpenseLevers withDefaults() {
        return new ExpenseLevers(
                defaultInflationRates(),
                DEFAULT_HEALTHCARE_TREND,
                ExpenseModifier.identity(),
                ExpenseModifier.identity(),
                ExpenseModifier.identity(),
                ExpenseModifier.identity()
        );
    }

    /**
     * Gets the inflation rate for a specific category type.
     *
     * @param type the inflation type
     * @return the inflation rate, or ZERO for NONE type
     */
    public BigDecimal getInflationRate(InflationType type) {
        if (type == InflationType.NONE) {
            return BigDecimal.ZERO;
        }
        return inflationRates.getOrDefault(type, DEFAULT_GENERAL_INFLATION);
    }

    /**
     * Creates a builder for constructing ExpenseLevers.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    private static Map<InflationType, BigDecimal> defaultInflationRates() {
        EnumMap<InflationType, BigDecimal> rates = new EnumMap<>(InflationType.class);
        rates.put(InflationType.GENERAL, DEFAULT_GENERAL_INFLATION);
        rates.put(InflationType.HEALTHCARE, DEFAULT_HEALTHCARE_INFLATION);
        rates.put(InflationType.HOUSING, DEFAULT_HOUSING_INFLATION);
        rates.put(InflationType.LTC, DEFAULT_LTC_INFLATION);
        rates.put(InflationType.NONE, BigDecimal.ZERO);
        return Collections.unmodifiableMap(rates);
    }

    /**
     * Builder for ExpenseLevers.
     */
    public static final class Builder {
        private Map<InflationType, BigDecimal> inflationRates;
        private BigDecimal healthcareTrend = DEFAULT_HEALTHCARE_TREND;
        private ExpenseModifier discretionaryModifier = ExpenseModifier.identity();
        private ExpenseModifier essentialsModifier = ExpenseModifier.identity();
        private ExpenseModifier healthcareModifier = ExpenseModifier.identity();
        private ExpenseModifier survivorModifier = ExpenseModifier.identity();

        private Builder() {
            this.inflationRates = new EnumMap<>(InflationType.class);
            this.inflationRates.putAll(defaultInflationRates());
        }

        /**
         * Sets all inflation rates.
         *
         * @param rates the inflation rates map
         * @return this builder
         */
        public Builder inflationRates(Map<InflationType, BigDecimal> rates) {
            this.inflationRates = new EnumMap<>(InflationType.class);
            this.inflationRates.putAll(rates);
            return this;
        }

        /**
         * Sets a specific inflation rate.
         *
         * @param type the inflation type
         * @param rate the rate
         * @return this builder
         */
        public Builder inflationRate(InflationType type, BigDecimal rate) {
            this.inflationRates.put(type, rate);
            return this;
        }

        /**
         * Sets the healthcare trend.
         *
         * @param trend the healthcare trend rate
         * @return this builder
         */
        public Builder healthcareTrend(BigDecimal trend) {
            this.healthcareTrend = trend;
            return this;
        }

        /**
         * Sets the discretionary spending modifier.
         *
         * @param modifier the modifier
         * @return this builder
         */
        public Builder discretionaryModifier(ExpenseModifier modifier) {
            this.discretionaryModifier = modifier;
            return this;
        }

        /**
         * Sets the essentials spending modifier.
         *
         * @param modifier the modifier
         * @return this builder
         */
        public Builder essentialsModifier(ExpenseModifier modifier) {
            this.essentialsModifier = modifier;
            return this;
        }

        /**
         * Sets the healthcare spending modifier.
         *
         * @param modifier the modifier
         * @return this builder
         */
        public Builder healthcareModifier(ExpenseModifier modifier) {
            this.healthcareModifier = modifier;
            return this;
        }

        /**
         * Sets the survivor modifier.
         *
         * @param modifier the modifier
         * @return this builder
         */
        public Builder survivorModifier(ExpenseModifier modifier) {
            this.survivorModifier = modifier;
            return this;
        }

        /**
         * Builds the ExpenseLevers.
         *
         * @return the constructed ExpenseLevers
         */
        public ExpenseLevers build() {
            return new ExpenseLevers(
                    inflationRates,
                    healthcareTrend,
                    discretionaryModifier,
                    essentialsModifier,
                    healthcareModifier,
                    survivorModifier
            );
        }
    }
}
