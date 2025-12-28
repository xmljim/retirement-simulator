package io.github.xmljim.retirement.domain.enums;

/**
 * Defines how to handle expense shortfalls when funds are insufficient.
 *
 * <p>When available funds cannot cover all expenses, the allocation
 * strategy must decide how to handle the deficit. This enum defines
 * the available approaches.
 *
 * @see io.github.xmljim.retirement.domain.calculator.ExpenseAllocationStrategy
 */
public enum ShortfallHandling {

    /**
     * Increase the withdrawal from the portfolio to cover the shortfall.
     *
     * <p>This maintains full expense coverage but depletes the
     * portfolio faster. May trigger additional taxes.
     */
    INCREASE_WITHDRAWAL("Increase Withdrawal",
            "Draw more from the portfolio to cover all expenses"),

    /**
     * Reduce discretionary spending first.
     *
     * <p>Cut non-essential expenses before touching essential
     * categories. Travel, entertainment, and hobbies are reduced.
     */
    REDUCE_DISCRETIONARY("Reduce Discretionary",
            "Cut discretionary spending before essential expenses"),

    /**
     * Prorate all expenses proportionally.
     *
     * <p>Reduce all categories by the same percentage.
     * Every category shares the burden equally.
     */
    PRORATE_ALL("Prorate All Categories",
            "Reduce all expense categories proportionally"),

    /**
     * Defer contingency reserve contributions.
     *
     * <p>Skip or reduce contributions to emergency and maintenance
     * reserves when funds are tight. Prioritizes current needs
     * over future reserves.
     */
    DEFER_CONTINGENCY("Defer Contingency",
            "Skip contingency reserve contributions during shortfalls"),

    /**
     * Reduce expenses in reverse priority order.
     *
     * <p>Cut lowest priority categories first, working up the
     * priority chain until the budget balances.
     */
    REDUCE_BY_PRIORITY("Reduce by Priority",
            "Cut lowest priority categories first");

    private final String displayName;
    private final String description;

    ShortfallHandling(String displayName, String description) {
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
     * Returns a description of this handling approach.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }
}
