package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Result of an RMD (Required Minimum Distribution) calculation.
 *
 * <p>Contains the calculated RMD amount and supporting details for a given year.
 * RMDs are mandatory withdrawals from tax-deferred retirement accounts
 * (Traditional IRA, 401k, etc.) that begin at a certain age.
 *
 * <p>Key rules:
 * <ul>
 *   <li>Formula: Prior year-end balance / Distribution factor = RMD</li>
 *   <li>First RMD deadline: April 1 of year following RMD start age</li>
 *   <li>Subsequent RMD deadline: December 31 each year</li>
 *   <li>Penalty for missing: 25% of shortfall (10% if corrected promptly)</li>
 * </ul>
 *
 * @param year the tax year for this RMD
 * @param accountBalance prior year-end account balance (basis for calculation)
 * @param rmdAmount the calculated RMD amount
 * @param distributionFactor the life expectancy factor used
 * @param deadline the deadline for taking this RMD
 * @param isFirstRmd true if this is the first RMD year (April 1 deadline)
 * @param age the account owner's age in this RMD year
 *
 * @see <a href="https://www.irs.gov/publications/p590b">IRS Publication 590-B</a>
 */
public record RmdProjection(
    int year,
    BigDecimal accountBalance,
    BigDecimal rmdAmount,
    BigDecimal distributionFactor,
    LocalDate deadline,
    boolean isFirstRmd,
    int age
) {

    /**
     * Creates an RMD projection for a standard (non-first) year.
     *
     * @param year the tax year
     * @param balance the prior year-end balance
     * @param rmdAmount the calculated RMD
     * @param factor the distribution factor
     * @param age the owner's age
     * @return a new RmdProjection with December 31 deadline
     */
    public static RmdProjection standard(
            int year,
            BigDecimal balance,
            BigDecimal rmdAmount,
            BigDecimal factor,
            int age) {
        return new RmdProjection(
            year,
            balance,
            rmdAmount,
            factor,
            LocalDate.of(year, 12, 31),
            false,
            age
        );
    }

    /**
     * Creates an RMD projection for the first RMD year.
     *
     * <p>The first RMD has a special deadline of April 1 of the following year.
     * However, taking this option means two RMDs in the second year.
     *
     * @param year the tax year
     * @param balance the prior year-end balance
     * @param rmdAmount the calculated RMD
     * @param factor the distribution factor
     * @param age the owner's age
     * @return a new RmdProjection with April 1 deadline of following year
     */
    public static RmdProjection firstYear(
            int year,
            BigDecimal balance,
            BigDecimal rmdAmount,
            BigDecimal factor,
            int age) {
        return new RmdProjection(
            year,
            balance,
            rmdAmount,
            factor,
            LocalDate.of(year + 1, 4, 1),
            true,
            age
        );
    }

    /**
     * Creates a zero RMD projection for years before RMD start age.
     *
     * @param year the tax year
     * @param balance the account balance
     * @param age the owner's age
     * @return a projection with zero RMD
     */
    public static RmdProjection notRequired(int year, BigDecimal balance, int age) {
        return new RmdProjection(
            year,
            balance,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            null,
            false,
            age
        );
    }

    /**
     * Returns whether an RMD is required this year.
     *
     * @return true if rmdAmount is greater than zero
     */
    public boolean isRequired() {
        return rmdAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Returns the RMD as a percentage of the account balance.
     *
     * @return the withdrawal percentage (e.g., 0.0365 for 3.65%)
     */
    public BigDecimal getWithdrawalPercentage() {
        if (accountBalance.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return rmdAmount.divide(accountBalance, 6, java.math.RoundingMode.HALF_UP);
    }
}
