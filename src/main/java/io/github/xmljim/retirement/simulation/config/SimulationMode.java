package io.github.xmljim.retirement.simulation.config;

/**
 * Defines how investment returns are calculated during simulation.
 *
 * <p>The simulation mode determines the source of return data:
 * <ul>
 *   <li>{@link #DETERMINISTIC} - Uses a fixed rate for all periods</li>
 *   <li>{@link #MONTE_CARLO} - Draws random returns from a distribution</li>
 *   <li>{@link #HISTORICAL} - Uses actual historical market sequences</li>
 * </ul>
 *
 * @see MarketLevers
 */
public enum SimulationMode {

    /**
     * Fixed rate applied to all periods.
     *
     * <p>Use for baseline projections and sensitivity analysis.
     * The same return rate is applied every month/year.
     */
    DETERMINISTIC("Deterministic", "Fixed rate applied consistently"),

    /**
     * Random returns drawn from a normal distribution.
     *
     * <p>Use for probability analysis and stress testing.
     * Each period samples from N(mean, stdDev) distribution.
     */
    MONTE_CARLO("Monte Carlo", "Random draws from distribution"),

    /**
     * Actual historical market sequences.
     *
     * <p>Use for backtesting against real market conditions.
     * Applies actual historical returns in sequence.
     */
    HISTORICAL("Historical", "Actual market sequences");

    private final String displayName;
    private final String description;

    SimulationMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Returns the human-readable display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns a brief description of this mode.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Indicates whether this mode requires random number generation.
     *
     * @return true if stochastic
     */
    public boolean isStochastic() {
        return this == MONTE_CARLO;
    }

    /**
     * Indicates whether this mode uses a fixed rate.
     *
     * @return true if deterministic
     */
    public boolean isFixed() {
        return this == DETERMINISTIC;
    }

    /**
     * Indicates whether this mode uses historical data.
     *
     * @return true if historical
     */
    public boolean usesHistoricalData() {
        return this == HISTORICAL;
    }
}
