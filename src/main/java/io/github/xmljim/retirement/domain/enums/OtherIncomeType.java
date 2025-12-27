package io.github.xmljim.retirement.domain.enums;

/**
 * Types of other income sources in retirement.
 *
 * <p>These represent income streams outside of traditional retirement
 * accounts, Social Security, pensions, and annuities. Tax treatment
 * varies by type.
 */
public enum OtherIncomeType {

    /**
     * Rental property income.
     *
     * <p>Net income from rental properties after expenses.
     * Generally considered passive income by the IRS (IRC Section 469),
     * not subject to Social Security earnings test or payroll taxes.
     * Taxed as ordinary income, with potential depreciation benefits.
     */
    RENTAL("Rental Income", false),

    /**
     * Part-time employment income.
     *
     * <p>Wages from continued work in retirement.
     * Subject to payroll taxes and may affect Social Security benefits
     * if claimed before Full Retirement Age.
     */
    PART_TIME_WORK("Part-Time Work", true),

    /**
     * Royalty income.
     *
     * <p>Income from intellectual property such as books, music,
     * patents, or mineral rights. Generally taxed as ordinary income.
     */
    ROYALTIES("Royalties", false),

    /**
     * Dividend income outside portfolio.
     *
     * <p>Dividend income from investments held outside retirement
     * accounts (e.g., taxable brokerage accounts, direct stock ownership).
     * May qualify for preferential tax rates if qualified dividends.
     */
    DIVIDENDS("Dividends", false),

    /**
     * Business income.
     *
     * <p>Income from business ownership or self-employment.
     * Subject to self-employment taxes and may allow business deductions.
     */
    BUSINESS("Business Income", true),

    /**
     * Other miscellaneous income.
     *
     * <p>Catch-all category for income not fitting other types,
     * such as alimony (pre-2019 agreements), prizes, or gambling winnings.
     */
    OTHER("Other Income", false);

    private final String displayName;
    private final boolean earnedIncome;

    OtherIncomeType(String displayName, boolean earnedIncome) {
        this.displayName = displayName;
        this.earnedIncome = earnedIncome;
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
     * Indicates whether this income type is considered earned income.
     *
     * <p>Earned income may affect Social Security earnings test
     * and may be subject to payroll/self-employment taxes.
     *
     * @return true if this is earned income
     */
    public boolean isEarnedIncome() {
        return earnedIncome;
    }
}
