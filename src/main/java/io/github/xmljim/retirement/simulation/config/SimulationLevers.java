package io.github.xmljim.retirement.simulation.config;

/**
 * Top-level configuration controlling simulation behavior.
 *
 * <p>SimulationLevers aggregates all configuration parameters that affect
 * how a retirement simulation runs. These values are immutable and set
 * at simulation start.
 *
 * <p>Levers are organized into three categories:
 * <ul>
 *   <li>{@link EconomicLevers} - Inflation, wage growth, interest rates</li>
 *   <li>{@link MarketLevers} - Investment return assumptions and mode</li>
 *   <li>{@link ExpenseLevers} - Expense inflation and modifiers</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * SimulationLevers levers = SimulationLevers.builder()
 *     .economic(EconomicLevers.builder()
 *         .generalInflationRate(new BigDecimal("0.03"))
 *         .build())
 *     .market(MarketLevers.monteCarlo(
 *         new BigDecimal("0.07"),
 *         new BigDecimal("0.15")))
 *     .build();
 * }</pre>
 *
 * @param economic economic assumptions (inflation, wage growth, interest)
 * @param market market return assumptions and simulation mode
 * @param expense expense inflation and modifiers
 *
 * @see EconomicLevers
 * @see MarketLevers
 * @see ExpenseLevers
 */
public record SimulationLevers(
        EconomicLevers economic,
        MarketLevers market,
        ExpenseLevers expense
) {

    /**
     * Compact constructor with null-safe defaults.
     */
    public SimulationLevers {
        if (economic == null) {
            economic = EconomicLevers.withDefaults();
        }
        if (market == null) {
            market = MarketLevers.withDefaults();
        }
        if (expense == null) {
            expense = ExpenseLevers.withDefaults();
        }
    }

    /**
     * Creates simulation levers with all default assumptions.
     *
     * <p>Defaults provide a reasonable baseline for initial projections:
     * <ul>
     *   <li>Economic: 2.5% inflation, 3% wage growth, 4% interest</li>
     *   <li>Market: Deterministic 7% return</li>
     *   <li>Expense: Standard inflation rates, no modifiers</li>
     * </ul>
     *
     * @return levers with default values
     */
    public static SimulationLevers withDefaults() {
        return new SimulationLevers(
                EconomicLevers.withDefaults(),
                MarketLevers.withDefaults(),
                ExpenseLevers.withDefaults()
        );
    }

    /**
     * Creates a builder for constructing SimulationLevers.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Indicates whether this configuration uses stochastic returns.
     *
     * @return true if Monte Carlo mode
     */
    public boolean isStochastic() {
        return market.isStochastic();
    }

    /**
     * Gets the simulation mode from market levers.
     *
     * @return the simulation mode
     */
    public SimulationMode mode() {
        return market.mode();
    }

    /**
     * Builder for SimulationLevers.
     */
    public static final class Builder {
        private EconomicLevers economic = EconomicLevers.withDefaults();
        private MarketLevers market = MarketLevers.withDefaults();
        private ExpenseLevers expense = ExpenseLevers.withDefaults();

        private Builder() {
        }

        /**
         * Sets the economic levers.
         *
         * @param economic the economic levers
         * @return this builder
         */
        public Builder economic(EconomicLevers economic) {
            this.economic = economic;
            return this;
        }

        /**
         * Sets the market levers.
         *
         * @param market the market levers
         * @return this builder
         */
        public Builder market(MarketLevers market) {
            this.market = market;
            return this;
        }

        /**
         * Sets the expense levers.
         *
         * @param expense the expense levers
         * @return this builder
         */
        public Builder expense(ExpenseLevers expense) {
            this.expense = expense;
            return this;
        }

        /**
         * Builds the SimulationLevers.
         *
         * @return the constructed SimulationLevers
         */
        public SimulationLevers build() {
            return new SimulationLevers(economic, market, expense);
        }
    }
}
