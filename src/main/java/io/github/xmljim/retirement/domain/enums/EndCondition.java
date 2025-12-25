package io.github.xmljim.retirement.domain.enums;

/**
 * Defines the condition that determines when a retirement simulation ends.
 *
 * <p>The end condition controls the time horizon of the simulation,
 * allowing flexibility in modeling different retirement planning goals.
 */
public enum EndCondition {
    /**
     * Simulation ends when the primary person reaches their life expectancy age.
     * For couples, uses the longer-lived person's life expectancy.
     */
    LIFE_EXPECTANCY("Life Expectancy", "Ends at projected life expectancy"),

    /**
     * Simulation ends when portfolio balance reaches zero.
     * Useful for determining portfolio longevity.
     */
    PORTFOLIO_DEPLETION("Portfolio Depletion", "Ends when funds are exhausted"),

    /**
     * Simulation ends at whichever occurs first: life expectancy or portfolio depletion.
     * Provides the most comprehensive view of retirement outcomes.
     */
    FIRST_OF_BOTH("First of Both", "Ends at life expectancy or depletion, whichever is first");

    private final String displayName;
    private final String description;

    EndCondition(String displayName, String description) {
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
     * Returns a brief description of the end condition.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }
}
