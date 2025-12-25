package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;

import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.enums.ContributionType;
import io.github.xmljim.retirement.domain.value.MatchingPolicy;

/**
 * Base interface for IRS contribution rule calculators.
 *
 * <p>Different implementations handle different versions of IRS rules
 * (e.g., SECURE Act, SECURE 2.0, future legislation). This interface
 * provides a common contract for calculating contribution limits and
 * determining ROTH allocation requirements.
 *
 * <p><strong>Important:</strong> All limits calculated by this interface are for
 * <em>employee contributions only</em>. Employer matching contributions do NOT
 * count against these limits - they are tracked separately.
 *
 * <p>Key SECURE 2.0 rules (effective 2025):
 * <ul>
 *   <li>Standard catch-up: Age 50+ can contribute additional $7,500</li>
 *   <li>Super catch-up: Age 60-63 can contribute additional $11,250</li>
 *   <li>High earners ($145K+): Catch-up must go to ROTH</li>
 *   <li>Employer contributions: Always go to Traditional account</li>
 * </ul>
 *
 * @see io.github.xmljim.retirement.domain.calculator.impl.Secure2ContributionRules
 */
public interface IrsContributionRules {

    /**
     * Returns the name/version of the rules.
     *
     * @return the rules version (e.g., "SECURE 2.0")
     */
    String getRulesVersion();

    /**
     * Calculates the total annual employee contribution limit for an individual.
     *
     * <p>This includes the base limit plus any applicable catch-up contribution.
     * The limit depends on the person's age and the year.
     *
     * <p><strong>Note:</strong> This is the employee contribution limit only.
     * Employer matching contributions do not count against this limit.
     *
     * @param contributionYear the tax year
     * @param age the person's age during the contribution year
     * @param accountType the type of account (may affect limits for some account types)
     * @return the maximum annual employee contribution
     */
    BigDecimal calculateAnnualContributionLimit(
        int contributionYear,
        int age,
        AccountType accountType);

    /**
     * Calculates the catch-up contribution limit if eligible.
     *
     * <p>Returns zero if the person is not eligible for catch-up contributions.
     * For SECURE 2.0:
     * <ul>
     *   <li>Age 50-59, 64+: Standard catch-up ($7,500 for 2025)</li>
     *   <li>Age 60-63 (starting 2025): Super catch-up ($11,250 for 2025)</li>
     *   <li>Under 50: $0 (not eligible)</li>
     * </ul>
     *
     * @param contributionYear the tax year
     * @param age the person's age during the contribution year
     * @param accountType the type of account
     * @return the catch-up contribution limit
     */
    BigDecimal calculateCatchUpLimit(
        int contributionYear,
        int age,
        AccountType accountType);

    /**
     * Determines if catch-up contributions must go to ROTH.
     *
     * <p>Under SECURE 2.0, employees who earned more than $145,000 in the prior
     * year must make catch-up contributions to a ROTH account.
     *
     * @param contributionYear the tax year
     * @param priorYearIncome the employee's prior year income (W-2 wages)
     * @return true if catch-up must be ROTH
     */
    boolean requiresRothCatchUp(
        int contributionYear,
        BigDecimal priorYearIncome);

    /**
     * Checks if the person is eligible for catch-up contributions.
     *
     * @param age the person's age
     * @return true if eligible for catch-up (age 50+)
     */
    boolean isCatchUpEligible(int age);

    /**
     * Calculates employer match using the specified policy.
     *
     * <p><strong>Note:</strong> Employer contributions do NOT count against
     * the employee's contribution limits. They are tracked separately.
     *
     * @param employeeContributionRate the employee's contribution rate
     * @param policy the employer's matching policy
     * @return the employer match rate
     */
    BigDecimal calculateEmployerMatch(
        BigDecimal employeeContributionRate,
        MatchingPolicy policy);

    /**
     * Determines the target account type for a contribution.
     *
     * <p>SECURE 2.0 Rules:
     * <ul>
     *   <li>Employer contributions: Always go to Traditional</li>
     *   <li>Base employee contributions: Follow account default</li>
     *   <li>Catch-up for high earners ($145K+): Must go to ROTH equivalent</li>
     *   <li>Catch-up for normal earners: Follow account default</li>
     * </ul>
     *
     * @param contributionType PERSONAL or EMPLOYER
     * @param isCatchUp true if this is a catch-up contribution
     * @param priorYearIncome the employee's prior year income
     * @param contributionYear the tax year
     * @param accountDefault the account's default type
     * @return the target AccountType for this contribution
     */
    AccountType determineTargetAccountType(
        ContributionType contributionType,
        boolean isCatchUp,
        BigDecimal priorYearIncome,
        int contributionYear,
        AccountType accountDefault);

    /**
     * Calculates the maximum allowed contribution when a ROTH equivalent is not available.
     *
     * <p>High earners ($145K+) without a ROTH equivalent account are capped
     * at the base limit because they cannot make catch-up contributions
     * to a Traditional account.
     *
     * @param year the contribution year
     * @param age the person's age
     * @param priorYearIncome the prior year income
     * @param hasRothEquivalent true if portfolio has a ROTH version of the account
     * @return the maximum allowed employee contribution
     */
    BigDecimal calculateMaxContributionWithoutRoth(
        int year,
        int age,
        BigDecimal priorYearIncome,
        boolean hasRothEquivalent);
}
