package io.github.xmljim.retirement.domain.enums;

/**
 * Defines how Long-Term Care (LTC) benefit needs are triggered in simulations.
 *
 * <p>LTC insurance policies provide benefits when the insured cannot perform
 * Activities of Daily Living (ADLs) or has cognitive impairment. This enum
 * controls how the simulation determines when LTC benefits begin.
 *
 * @see <a href="https://www.aaltci.org">American Association for Long-Term Care Insurance</a>
 */
public enum LtcTriggerMode {

    /**
     * User specifies an exact age when LTC need begins.
     *
     * <p>Useful for scenario analysis where a specific planning assumption
     * is needed, such as "assume LTC need begins at age 80".
     */
    DETERMINISTIC("Deterministic", "LTC need begins at specified age"),

    /**
     * Uses actuarial probability data for Monte Carlo simulations.
     *
     * <p>Each simulation run randomly determines if/when LTC need begins
     * based on age-specific probabilities derived from actuarial data.
     * Provides a distribution of outcomes rather than a single path.
     */
    PROBABILISTIC("Probabilistic", "Monte Carlo using actuarial data"),

    /**
     * No LTC event occurs; premiums are paid but no benefits claimed.
     *
     * <p>Useful for modeling the scenario where LTC insurance is purchased
     * but never used. Approximately 30% of policyholders never file a claim.
     */
    NONE("None", "No LTC event occurs");

    private final String displayName;
    private final String description;

    LtcTriggerMode(String displayName, String description) {
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
     * Returns a brief description of this trigger mode.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Indicates whether this mode triggers LTC benefits at some point.
     *
     * @return true if LTC benefits may be triggered
     */
    public boolean canTriggerBenefits() {
        return this != NONE;
    }

    /**
     * Indicates whether this mode requires Monte Carlo simulation.
     *
     * @return true if probabilistic simulation is needed
     */
    public boolean requiresMonteCarlo() {
        return this == PROBABILISTIC;
    }
}
