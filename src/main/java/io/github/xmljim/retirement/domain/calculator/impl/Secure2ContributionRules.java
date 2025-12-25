package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.xmljim.retirement.domain.calculator.IrsContributionRules;
import io.github.xmljim.retirement.domain.config.IrsContributionLimits;
import io.github.xmljim.retirement.domain.config.IrsContributionLimits.YearLimits;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.enums.ContributionType;
import io.github.xmljim.retirement.domain.value.MatchingPolicy;

/**
 * Implementation of SECURE 2.0 contribution rules.
 *
 * <p>This implementation handles:
 * <ul>
 *   <li>Standard catch-up contributions for age 50+</li>
 *   <li>Super catch-up contributions for age 60-63 (effective 2025)</li>
 *   <li>ROTH-only catch-up for high earners ($145K+)</li>
 *   <li>Employer contribution routing (always to Traditional)</li>
 * </ul>
 *
 * <p><strong>Important:</strong> All contribution limits are for employee
 * contributions only. Employer matching contributions do NOT count against
 * these limits.
 *
 * <p>ROTH Allocation Rules for High Earners ($145K+ prior year income):
 * <ul>
 *   <li>Base contribution (up to base limit): Can go to Traditional</li>
 *   <li>Catch-up contribution: MUST go to ROTH</li>
 *   <li>If no ROTH account exists: Capped at base limit (no catch-up)</li>
 * </ul>
 */
@Service
public class Secure2ContributionRules implements IrsContributionRules {

    private static final String VERSION = "SECURE 2.0";
    private static final int CATCH_UP_AGE = 50;
    private static final int SUPER_CATCH_UP_MIN_AGE = 60;
    private static final int SUPER_CATCH_UP_MAX_AGE = 63;
    private static final int SUPER_CATCH_UP_EFFECTIVE_YEAR = 2025;

    private final IrsContributionLimits limits;

    /**
     * Creates a new SECURE 2.0 contribution rules calculator.
     *
     * @param limits the IRS contribution limits configuration
     */
    @Autowired
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "IrsContributionLimits is a Spring-managed singleton bean"
    )
    public Secure2ContributionRules(IrsContributionLimits limits) {
        this.limits = limits;
    }

    @Override
    public String getRulesVersion() {
        return VERSION;
    }

    @Override
    public BigDecimal calculateAnnualContributionLimit(
            int contributionYear, int age, AccountType accountType) {

        YearLimits yearLimits = limits.getLimitsForYear(contributionYear);
        BigDecimal baseLimit = yearLimits.baseLimit();
        BigDecimal catchUpLimit = calculateCatchUpLimit(contributionYear, age, accountType);

        return baseLimit.add(catchUpLimit);
    }

    @Override
    public BigDecimal calculateCatchUpLimit(
            int contributionYear, int age, AccountType accountType) {

        if (!isCatchUpEligible(age)) {
            return BigDecimal.ZERO;
        }

        YearLimits yearLimits = limits.getLimitsForYear(contributionYear);

        // Super catch-up for ages 60-63, only starting 2025
        if (isSuperCatchUpEligible(contributionYear, age)) {
            return yearLimits.superCatchUpLimit();
        }

        return yearLimits.catchUpLimit();
    }

    @Override
    public boolean requiresRothCatchUp(int contributionYear, BigDecimal priorYearIncome) {
        if (priorYearIncome == null) {
            return false;
        }

        YearLimits yearLimits = limits.getLimitsForYear(contributionYear);
        return priorYearIncome.compareTo(yearLimits.rothCatchUpIncomeThreshold()) > 0;
    }

    @Override
    public boolean isCatchUpEligible(int age) {
        return age >= CATCH_UP_AGE;
    }

    /**
     * Checks if the person is eligible for super catch-up contributions.
     *
     * <p>Super catch-up is available for ages 60-63, effective starting in 2025.
     *
     * @param contributionYear the tax year
     * @param age the person's age
     * @return true if eligible for super catch-up
     */
    public boolean isSuperCatchUpEligible(int contributionYear, int age) {
        return contributionYear >= SUPER_CATCH_UP_EFFECTIVE_YEAR
            && age >= SUPER_CATCH_UP_MIN_AGE
            && age <= SUPER_CATCH_UP_MAX_AGE;
    }

    @Override
    public BigDecimal calculateEmployerMatch(
            BigDecimal employeeContributionRate, MatchingPolicy policy) {

        if (policy == null) {
            return BigDecimal.ZERO;
        }
        return policy.calculateEmployerMatch(employeeContributionRate);
    }

    @Override
    public AccountType determineTargetAccountType(
            ContributionType contributionType,
            boolean isCatchUp,
            BigDecimal priorYearIncome,
            int contributionYear,
            AccountType accountDefault) {

        // Rule 1: Employer contributions ALWAYS go to Traditional
        if (contributionType == ContributionType.EMPLOYER) {
            return toTraditionalVariant(accountDefault);
        }

        // Rule 2: Catch-up contributions for high earners must go to ROTH
        if (isCatchUp && requiresRothCatchUp(contributionYear, priorYearIncome)) {
            return toRothVariant(accountDefault);
        }

        // Rule 3: All other contributions follow account default
        return accountDefault;
    }

    @Override
    public BigDecimal calculateMaxContributionWithoutRoth(
            int year, int age, BigDecimal priorYearIncome, boolean hasRothEquivalent) {

        YearLimits yearLimits = limits.getLimitsForYear(year);
        BigDecimal baseLimit = yearLimits.baseLimit();

        // If not catch-up eligible, always just base limit
        if (!isCatchUpEligible(age)) {
            return baseLimit;
        }

        // If has ROTH equivalent, full contribution allowed
        if (hasRothEquivalent) {
            return calculateAnnualContributionLimit(year, age, null);
        }

        // High earners without ROTH: capped at base limit (no catch-up)
        if (requiresRothCatchUp(year, priorYearIncome)) {
            return baseLimit;
        }

        // Normal earners without ROTH: full limit including catch-up
        return calculateAnnualContributionLimit(year, age, null);
    }

    /**
     * Converts an account type to its Traditional variant.
     *
     * @param accountType the source account type
     * @return the Traditional variant
     */
    private AccountType toTraditionalVariant(AccountType accountType) {
        if (accountType == null) {
            return null;
        }

        return switch (accountType) {
            case ROTH_401K, TRADITIONAL_401K -> AccountType.TRADITIONAL_401K;
            case ROTH_403B, TRADITIONAL_403B -> AccountType.TRADITIONAL_403B;
            case ROTH_IRA, TRADITIONAL_IRA -> AccountType.TRADITIONAL_IRA;
            default -> accountType;
        };
    }

    /**
     * Converts an account type to its ROTH variant.
     *
     * @param accountType the source account type
     * @return the ROTH variant
     */
    private AccountType toRothVariant(AccountType accountType) {
        if (accountType == null) {
            return null;
        }

        return switch (accountType) {
            case TRADITIONAL_401K, ROTH_401K -> AccountType.ROTH_401K;
            case TRADITIONAL_403B, ROTH_403B -> AccountType.ROTH_403B;
            case TRADITIONAL_IRA, ROTH_IRA -> AccountType.ROTH_IRA;
            default -> accountType;
        };
    }
}
