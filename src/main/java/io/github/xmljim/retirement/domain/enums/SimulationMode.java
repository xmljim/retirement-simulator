package io.github.xmljim.retirement.domain.enums;

/**
 * Defines the mode of simulation for retirement projections.
 *
 * <p>This enum serves as a placeholder for future simulation capabilities.
 * Currently, only deterministic simulation is supported.
 */
public enum SimulationMode {
    /**
     * Uses fixed return rates without randomization.
     * Produces a single, predictable projection path.
     */
    DETERMINISTIC("Deterministic", "Fixed return rates"),

    /**
     * Uses random sampling to generate multiple projection paths.
     * Provides probability distributions for outcomes.
     * (Future implementation)
     */
    MONTE_CARLO("Monte Carlo", "Randomized simulations"),

    /**
     * Uses actual historical market data for projections.
     * Shows how portfolio would have performed in past conditions.
     * (Future implementation)
     */
    HISTORICAL("Historical", "Based on historical data");

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
     * Returns a brief description of the simulation mode.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Indicates whether this mode is currently implemented.
     *
     * @return true if the mode is available for use
     */
    public boolean isImplemented() {
        return this == DETERMINISTIC;
    }
}
