package io.github.xmljim.retirement.domain.enums;

/**
 * Defines behavior when a reserve (like contingency) reaches its target.
 *
 * <p>When a contingency reserve is fully funded, the allocation strategy
 * needs to determine what to do with funds that would have gone to that
 * reserve. This enum defines the available options.
 *
 * @see io.github.xmljim.retirement.domain.calculator.ExpenseAllocationStrategy
 */
public enum OverflowBehavior {

    /**
     * Redirect excess funds to the next priority category.
     *
     * <p>Follows the priority order, finding the next category
     * that can use additional funds.
     */
    REDIRECT_TO_NEXT_PRIORITY("Redirect to Next Priority",
            "Move excess funds to the next priority category in the allocation order"),

    /**
     * Redirect excess funds specifically to discretionary spending.
     *
     * <p>Increases lifestyle spending when reserves are full.
     */
    REDIRECT_TO_DISCRETIONARY("Redirect to Discretionary",
            "Move excess funds to discretionary spending categories"),

    /**
     * Redirect excess funds to accelerate debt paydown.
     *
     * <p>Uses extra funds to pay down mortgage or other debt faster.
     */
    REDIRECT_TO_DEBT_PAYDOWN("Redirect to Debt Paydown",
            "Move excess funds to accelerate debt repayment"),

    /**
     * Reduce the withdrawal amount from the portfolio.
     *
     * <p>If reserves are full, take less from investments,
     * allowing the portfolio to grow.
     */
    REDUCE_WITHDRAWAL("Reduce Withdrawal",
            "Reduce the withdrawal amount from the portfolio"),

    /**
     * Save excess funds back to the investment portfolio.
     *
     * <p>Treat excess as additional savings rather than spending.
     */
    SAVE_TO_PORTFOLIO("Save to Portfolio",
            "Deposit excess funds back into the investment portfolio");

    private final String displayName;
    private final String description;

    OverflowBehavior(String displayName, String description) {
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
     * Returns a description of this behavior.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }
}
