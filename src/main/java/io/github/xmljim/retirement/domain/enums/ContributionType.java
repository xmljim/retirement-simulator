package io.github.xmljim.retirement.domain.enums;

/**
 * Types of retirement account contributions.
 *
 * <p>Contributions can come from the individual (personal) or from
 * their employer as part of a matching or profit-sharing program.
 */
public enum ContributionType {
    /**
     * Personal contributions made by the individual from their salary.
     * These may be pre-tax (Traditional) or post-tax (Roth).
     */
    PERSONAL("Personal", "Individual salary contribution"),

    /**
     * Employer contributions such as matching or profit-sharing.
     * These are typically pre-tax.
     */
    EMPLOYER("Employer", "Employer matching or profit-sharing");

    private final String displayName;
    private final String description;

    ContributionType(String displayName, String description) {
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
     * Returns a brief description of the contribution type.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }
}
