package io.github.xmljim.retirement.domain.enums;

/**
 * Types of retirement withdrawal strategies.
 *
 * <p>Determines how withdrawal amounts are calculated during retirement.
 */
public enum WithdrawalType {
    /**
     * Fixed dollar amount withdrawal.
     * The withdrawal amount remains constant (may be adjusted for inflation).
     */
    FIXED("Fixed Amount", "Withdraw a fixed dollar amount"),

    /**
     * Percentage-based withdrawal.
     * Withdrawal is calculated as a percentage of the current portfolio balance
     * or pre-retirement salary.
     */
    PERCENTAGE("Percentage", "Withdraw based on a percentage");

    private final String displayName;
    private final String description;

    WithdrawalType(String displayName, String description) {
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
     * Returns a brief description of the withdrawal type.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }
}
