package io.github.xmljim.retirement.domain.enums;

/**
 * Types of transactions in a retirement portfolio simulation.
 *
 * <p>Indicates the primary activity for a given transaction period:
 * <ul>
 *   <li>CONTRIBUTION - Accumulation phase (adding to accounts)</li>
 *   <li>WITHDRAWAL - Distribution phase (drawing from accounts)</li>
 * </ul>
 */
public enum TransactionType {

    /**
     * Contribution transaction during accumulation phase.
     * May include personal contributions, employer matches, and investment returns.
     */
    CONTRIBUTION("Contribution", "Adding funds to account"),

    /**
     * Withdrawal transaction during distribution phase.
     * May include withdrawals and investment returns.
     */
    WITHDRAWAL("Withdrawal", "Drawing funds from account");

    private final String displayName;
    private final String description;

    TransactionType(String displayName, String description) {
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
     * Returns a brief description of the transaction type.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }
}
