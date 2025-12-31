package io.github.xmljim.retirement.simulation.config;

import java.math.BigDecimal;

/**
 * Economic assumptions that drive simulation calculations.
 *
 * <p>These rates affect income growth, purchasing power, and
 * present value calculations throughout the simulation.
 *
 * @param generalInflationRate annual CPI inflation rate (e.g., 0.025 for 2.5%)
 * @param wageGrowthRate annual salary/wage growth rate (e.g., 0.03 for 3%)
 * @param interestRate risk-free rate for discounting (e.g., 0.04 for 4%)
 *
 * @see SimulationLevers
 */
public record EconomicLevers(
        BigDecimal generalInflationRate,
        BigDecimal wageGrowthRate,
        BigDecimal interestRate
) {

    /** Default general inflation rate: 2.5%. */
    public static final BigDecimal DEFAULT_INFLATION_RATE = new BigDecimal("0.025");

    /** Default wage growth rate: 3.0%. */
    public static final BigDecimal DEFAULT_WAGE_GROWTH_RATE = new BigDecimal("0.03");

    /** Default risk-free interest rate: 4.0%. */
    public static final BigDecimal DEFAULT_INTEREST_RATE = new BigDecimal("0.04");

    /**
     * Compact constructor with null-safe defaults.
     */
    public EconomicLevers {
        if (generalInflationRate == null) {
            generalInflationRate = DEFAULT_INFLATION_RATE;
        }
        if (wageGrowthRate == null) {
            wageGrowthRate = DEFAULT_WAGE_GROWTH_RATE;
        }
        if (interestRate == null) {
            interestRate = DEFAULT_INTEREST_RATE;
        }
    }

    /**
     * Creates economic levers with default assumptions.
     *
     * <p>Default values:
     * <ul>
     *   <li>General inflation: 2.5%</li>
     *   <li>Wage growth: 3.0%</li>
     *   <li>Interest rate: 4.0%</li>
     * </ul>
     *
     * @return levers with default values
     */
    public static EconomicLevers withDefaults() {
        return new EconomicLevers(
                DEFAULT_INFLATION_RATE,
                DEFAULT_WAGE_GROWTH_RATE,
                DEFAULT_INTEREST_RATE
        );
    }

    /**
     * Creates a builder for constructing EconomicLevers.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for EconomicLevers.
     */
    public static final class Builder {
        private BigDecimal generalInflationRate = DEFAULT_INFLATION_RATE;
        private BigDecimal wageGrowthRate = DEFAULT_WAGE_GROWTH_RATE;
        private BigDecimal interestRate = DEFAULT_INTEREST_RATE;

        private Builder() {
        }

        /**
         * Sets the general inflation rate.
         *
         * @param rate the inflation rate as decimal
         * @return this builder
         */
        public Builder generalInflationRate(BigDecimal rate) {
            this.generalInflationRate = rate;
            return this;
        }

        /**
         * Sets the wage growth rate.
         *
         * @param rate the wage growth rate as decimal
         * @return this builder
         */
        public Builder wageGrowthRate(BigDecimal rate) {
            this.wageGrowthRate = rate;
            return this;
        }

        /**
         * Sets the interest rate.
         *
         * @param rate the interest rate as decimal
         * @return this builder
         */
        public Builder interestRate(BigDecimal rate) {
            this.interestRate = rate;
            return this;
        }

        /**
         * Builds the EconomicLevers.
         *
         * @return the constructed EconomicLevers
         */
        public EconomicLevers build() {
            return new EconomicLevers(
                    generalInflationRate,
                    wageGrowthRate,
                    interestRate
            );
        }
    }
}
