package io.github.xmljim.retirement.domain.enums;

/**
 * Categories for grouping account types that share IRS contribution limits.
 *
 * <p>IRS limits are applied differently depending on account category:
 * <ul>
 *   <li><strong>EMPLOYER_401K</strong>: 401(k), 403(b), 457(b) plans share limits per employer</li>
 *   <li><strong>IRA</strong>: Traditional and Roth IRA share a combined limit</li>
 *   <li><strong>HSA</strong>: Health Savings Account with individual or family limits</li>
 * </ul>
 *
 * <p>Note: Taxable brokerage accounts have no IRS limits and are not included.
 */
public enum LimitCategory {

    /**
     * Employer-sponsored retirement plans: 401(k), 403(b), 457(b).
     *
     * <p>These plans share limits within each employer plan. Most individuals
     * have only one employer plan, so the limit effectively applies per person.
     * Includes both Traditional and Roth variants.
     */
    EMPLOYER_401K("Employer 401(k)/403(b)/457(b)"),

    /**
     * Individual Retirement Accounts.
     *
     * <p>Traditional IRA and Roth IRA share a combined contribution limit.
     * For example, if the limit is $7,000, contributing $4,000 to Traditional IRA
     * leaves only $3,000 available for Roth IRA.
     */
    IRA("IRA (Traditional + Roth)"),

    /**
     * Health Savings Account.
     *
     * <p>HSA limits depend on coverage type:
     * <ul>
     *   <li>Individual coverage: lower limit (e.g., $4,300 for 2025)</li>
     *   <li>Family coverage: higher limit (e.g., $8,550 for 2025)</li>
     * </ul>
     *
     * <p>Coverage type is inferred from whether the person has a spouse
     * linked in their PersonProfile.
     */
    HSA("Health Savings Account");

    private final String displayName;

    LimitCategory(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns a human-readable display name for this category.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Maps an AccountType to its corresponding LimitCategory.
     *
     * @param accountType the account type to map
     * @return the limit category, or null if the account type has no IRS limits
     */
    public static LimitCategory fromAccountType(AccountType accountType) {
        if (accountType == null) {
            return null;
        }

        return switch (accountType) {
            case TRADITIONAL_401K, ROTH_401K,
                 TRADITIONAL_403B, ROTH_403B,
                 TRADITIONAL_457B -> EMPLOYER_401K;
            case TRADITIONAL_IRA, ROTH_IRA -> IRA;
            case HSA -> HSA;
            case TAXABLE_BROKERAGE -> null;
        };
    }
}
