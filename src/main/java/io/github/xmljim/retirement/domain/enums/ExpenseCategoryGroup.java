package io.github.xmljim.retirement.domain.enums;

/**
 * High-level groupings for expense categories.
 *
 * <p>Expense categories are organized into groups for easier management
 * and to apply group-level defaults (such as inflation rates). Each
 * {@link ExpenseCategory} belongs to exactly one group.
 *
 * @see ExpenseCategory
 */
public enum ExpenseCategoryGroup {

    /**
     * Essential living expenses.
     *
     * <p>Covers basic needs that cannot easily be reduced:
     * housing, food, utilities, transportation, and insurance.
     * These expenses should be prioritized in budget allocation.
     */
    ESSENTIAL("Essential", "Basic living expenses that are difficult to reduce"),

    /**
     * Healthcare-related expenses.
     *
     * <p>Includes Medicare premiums, out-of-pocket medical costs,
     * long-term care insurance premiums, and actual long-term care costs.
     * Healthcare typically inflates faster than general CPI (5-6% vs 2-3%).
     */
    HEALTHCARE("Healthcare", "Medical and long-term care expenses"),

    /**
     * Discretionary spending.
     *
     * <p>Non-essential lifestyle expenses such as travel, entertainment,
     * hobbies, and gifts. These can be reduced during market downturns
     * or when income is constrained.
     */
    DISCRETIONARY("Discretionary", "Lifestyle expenses that can be adjusted"),

    /**
     * Contingency and reserve expenses.
     *
     * <p>Funds set aside for unexpected expenses such as home repairs,
     * vehicle replacement, and emergency reserves. These represent
     * planned savings for irregular large expenses.
     */
    CONTINGENCY("Contingency", "Reserves for unexpected expenses"),

    /**
     * Debt payments.
     *
     * <p>Fixed debt obligations such as mortgage payments, car loans,
     * and other installment debt. These typically do not inflate and
     * have defined end dates.
     */
    DEBT("Debt", "Fixed debt payments"),

    /**
     * Other expenses.
     *
     * <p>Catch-all category for expenses not fitting other groups,
     * including estimated tax payments and miscellaneous costs.
     */
    OTHER("Other", "Miscellaneous expenses");

    private final String displayName;
    private final String description;

    ExpenseCategoryGroup(String displayName, String description) {
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
     * Returns a brief description of this group.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }
}
