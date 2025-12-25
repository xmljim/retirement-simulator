package io.github.xmljim.retirement.domain.enums;

/**
 * Defines the strategy for distributing withdrawals across accounts.
 *
 * <p>This enum serves as a placeholder for future distribution strategy
 * implementations. The actual withdrawal logic will be implemented in
 * a later milestone.
 */
public enum DistributionStrategy {
    /**
     * Withdraws from accounts in a fixed order based on tax treatment.
     * Typically: taxable first, then pre-tax, then Roth.
     */
    TAX_EFFICIENT("Tax Efficient", "Optimizes for tax efficiency"),

    /**
     * Withdraws proportionally from all accounts based on their balances.
     * Maintains consistent asset allocation across accounts.
     */
    PRO_RATA("Pro Rata", "Proportional withdrawals from all accounts"),

    /**
     * Withdraws from accounts in a user-specified order.
     * Provides maximum control over withdrawal sequence.
     */
    CUSTOM("Custom", "User-defined withdrawal order"),

    /**
     * Automatically selects the optimal strategy based on tax brackets.
     * Uses Roth conversions when beneficial.
     * (Future implementation)
     */
    ROTH_CONVERSION_OPTIMIZER("Roth Optimizer", "Optimizes Roth conversion opportunities");

    private final String displayName;
    private final String description;

    DistributionStrategy(String displayName, String description) {
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
     * Returns a brief description of the distribution strategy.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Indicates whether this strategy is currently implemented.
     *
     * @return true if the strategy is available for use
     */
    public boolean isImplemented() {
        return this == TAX_EFFICIENT || this == PRO_RATA;
    }
}
