package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;

import io.github.xmljim.retirement.domain.enums.AccountType;

/**
 * Result of a contribution calculation containing dollar amounts.
 *
 * <p>This record captures the complete result of calculating contributions
 * for a given period, including:
 * <ul>
 *   <li>Personal (employee) contribution amount</li>
 *   <li>Employer match amount</li>
 *   <li>Year-to-date tracking</li>
 *   <li>Target account for routing</li>
 *   <li>Whether IRS limits were applied</li>
 * </ul>
 *
 * <p>All monetary amounts are in dollars.
 *
 * @param personalContribution the employee's contribution amount for this period
 * @param employerMatch the employer's matching contribution amount
 * @param totalContribution the sum of personal and employer contributions
 * @param yearToDateAfter the cumulative year-to-date contributions after this contribution
 * @param targetAccount the account type where contributions should be deposited
 * @param limitApplied true if the contribution was reduced due to IRS annual limits
 */
public record ContributionResult(
    BigDecimal personalContribution,
    BigDecimal employerMatch,
    BigDecimal totalContribution,
    BigDecimal yearToDateAfter,
    AccountType targetAccount,
    boolean limitApplied
) {

    /**
     * Creates a ContributionResult with calculated total.
     *
     * @param personalContribution the employee's contribution amount
     * @param employerMatch the employer's matching contribution amount
     * @param yearToDateAfter the YTD after this contribution
     * @param targetAccount the target account type
     * @param limitApplied whether limits were applied
     * @return a new ContributionResult
     */
    public static ContributionResult of(
            BigDecimal personalContribution,
            BigDecimal employerMatch,
            BigDecimal yearToDateAfter,
            AccountType targetAccount,
            boolean limitApplied) {

        BigDecimal total = personalContribution.add(employerMatch);
        return new ContributionResult(
            personalContribution,
            employerMatch,
            total,
            yearToDateAfter,
            targetAccount,
            limitApplied
        );
    }

    /**
     * Creates a zero contribution result (e.g., for retired individuals).
     *
     * @param yearToDate the current year-to-date (unchanged)
     * @param targetAccount the account type (for reference)
     * @return a ContributionResult with zero contributions
     */
    public static ContributionResult zero(BigDecimal yearToDate, AccountType targetAccount) {
        return new ContributionResult(
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            yearToDate,
            targetAccount,
            false
        );
    }

    /**
     * Returns true if there are any contributions (personal or employer).
     *
     * @return true if totalContribution is greater than zero
     */
    public boolean hasContributions() {
        return totalContribution.compareTo(BigDecimal.ZERO) > 0;
    }
}
