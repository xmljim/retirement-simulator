package io.github.xmljim.retirement.domain.enums;

/**
 * Identifies which person survives when a spouse passes away.
 *
 * <p>Used in survivor scenario modeling to determine which person's
 * expenses, benefits, and income sources continue after the death event.
 */
public enum SurvivorRole {

    /**
     * The primary account holder survives.
     * Typically the person whose retirement is being planned.
     */
    PRIMARY("Primary", "Primary account holder survives"),

    /**
     * The spouse survives.
     * The secondary person in a couple scenario.
     */
    SPOUSE("Spouse", "Spouse survives");

    private final String displayName;
    private final String description;

    SurvivorRole(String displayName, String description) {
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
     * Returns a brief description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the opposite role (who passed away).
     *
     * @return PRIMARY if this is SPOUSE, SPOUSE if this is PRIMARY
     */
    public SurvivorRole getDeceasedRole() {
        return this == PRIMARY ? SPOUSE : PRIMARY;
    }
}
