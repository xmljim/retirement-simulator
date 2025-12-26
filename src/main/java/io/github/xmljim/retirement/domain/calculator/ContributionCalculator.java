package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.value.ContributionConfig;
import io.github.xmljim.retirement.domain.value.ContributionResult;
import io.github.xmljim.retirement.domain.value.MatchingPolicy;

/**
 * Calculator for retirement contribution calculations.
 *
 * <p>Provides methods for:
 * <ul>
 *   <li>Calculating personal contribution rates with increments</li>
 *   <li>Calculating employer contribution rates</li>
 *   <li>Determining retirement status based on dates</li>
 * </ul>
 *
 * <p>Contributions are calculated based on the contribution date relative
 * to the retirement date. After retirement, contribution rates are zero.
 *
 * @see io.github.xmljim.retirement.domain.calculator.impl.DefaultContributionCalculator
 */
public interface ContributionCalculator {

    /**
     * Determines if a person is retired based on the current date
     * and their retirement date.
     *
     * <p>A person is considered retired if the current date is on or after
     * the retirement date.
     *
     * @param currentDate the current date to check
     * @param retirementDate the planned or actual retirement date
     * @return true if retired (currentDate >= retirementDate), false otherwise
     * @throws IllegalArgumentException if either date is null
     */
    boolean isRetired(LocalDate currentDate, LocalDate retirementDate);

    /**
     * Calculates the personal contribution rate for a given date.
     *
     * <p>The contribution rate starts at the base rate defined in the config
     * and increases by the increment rate each year on the specified increment
     * month. After retirement, the contribution rate is zero.
     *
     * <p>Formula (while working):
     * <pre>
     * rate = baseRate + (incrementRate * yearsOfIncrements)
     * where yearsOfIncrements = floor(monthsSinceStart / 12) +
     *       (currentMonth >= incrementMonth ? 1 : 0)
     * </pre>
     *
     * @param contributionDate the date of the contribution
     * @param retirementDate the planned retirement date
     * @param config the contribution configuration
     * @return the contribution rate as a decimal (e.g., 0.10 for 10%)
     * @throws IllegalArgumentException if any parameter is null
     */
    BigDecimal calculatePersonalContributionRate(
        LocalDate contributionDate,
        LocalDate retirementDate,
        ContributionConfig config);

    /**
     * Calculates the employer contribution rate for a given date.
     *
     * <p>The employer contribution is a fixed rate while the employee is working.
     * After retirement, the employer contribution is zero.
     *
     * <p>Returns zero in these cases:
     * <ul>
     *   <li>The employee is retired (contributionDate >= retirementDate)</li>
     *   <li>The employer match rate in config is zero or null</li>
     *   <li>The contribution type does not include employer matching</li>
     * </ul>
     *
     * @param contributionDate the date of the contribution
     * @param retirementDate the planned retirement date
     * @param config the contribution configuration
     * @return the contribution rate as a decimal (e.g., 0.04 for 4%), or zero if retired
     *         or if no employer match is configured
     * @throws IllegalArgumentException if any parameter is null
     */
    BigDecimal calculateEmployerContributionRate(
        LocalDate contributionDate,
        LocalDate retirementDate,
        ContributionConfig config);

    // =========================================================================
    // Dollar Amount Calculations (Issue #12)
    // =========================================================================

    /**
     * Calculates the complete monthly contribution including personal and employer amounts.
     *
     * <p>This method:
     * <ul>
     *   <li>Calculates personal contribution based on salary and rate</li>
     *   <li>Calculates employer match based on matching policy</li>
     *   <li>Applies IRS annual contribution limits</li>
     *   <li>Tracks year-to-date contributions</li>
     *   <li>Determines target account for routing</li>
     * </ul>
     *
     * <p>Returns a zero contribution result if the person is retired.
     *
     * @param monthlySalary the gross monthly salary
     * @param yearToDateContributions total personal contributions so far this year
     * @param contributionYear the tax year for IRS limits
     * @param personAge the person's age (for catch-up eligibility)
     * @param contributionDate the date of the contribution
     * @param retirementDate the planned retirement date
     * @param personalConfig the personal contribution configuration
     * @param matchingPolicy the employer matching policy (null if no match)
     * @param accountType the default account type for contributions
     * @return a {@link ContributionResult} with all calculated amounts
     * @throws MissingRequiredFieldException if required parameters are null
     */
    ContributionResult calculateMonthlyContribution(
        BigDecimal monthlySalary,
        BigDecimal yearToDateContributions,
        int contributionYear,
        int personAge,
        LocalDate contributionDate,
        LocalDate retirementDate,
        ContributionConfig personalConfig,
        MatchingPolicy matchingPolicy,
        AccountType accountType);

    /**
     * Calculates the personal contribution dollar amount.
     *
     * <p>Simple calculation: {@code monthlySalary * contributionRate}
     *
     * <p>This method does NOT apply IRS limits. For limit-aware calculations,
     * use {@link #calculateMonthlyContribution}.
     *
     * @param monthlySalary the gross monthly salary
     * @param contributionRate the contribution rate as a decimal (e.g., 0.10 for 10%)
     * @return the contribution amount in dollars
     * @throws MissingRequiredFieldException if monthlySalary is null
     */
    BigDecimal calculatePersonalContributionAmount(
        BigDecimal monthlySalary,
        BigDecimal contributionRate);

    /**
     * Calculates the employer match dollar amount.
     *
     * <p>Uses the matching policy to determine the match rate, then applies
     * it to the salary.
     *
     * <p>Example with 50% match up to 6%:
     * <ul>
     *   <li>Employee contributes 4% of $5000 = $200</li>
     *   <li>Employer matches 50% of 4% = 2% of $5000 = $100</li>
     * </ul>
     *
     * @param employeeContributionRate the employee's contribution rate
     * @param monthlySalary the gross monthly salary
     * @param matchingPolicy the employer matching policy
     * @return the employer match amount in dollars, or zero if no policy
     * @throws MissingRequiredFieldException if monthlySalary is null
     */
    BigDecimal calculateEmployerMatchAmount(
        BigDecimal employeeContributionRate,
        BigDecimal monthlySalary,
        MatchingPolicy matchingPolicy);
}
