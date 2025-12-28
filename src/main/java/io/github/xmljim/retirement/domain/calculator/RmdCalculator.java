package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;

import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.value.RmdProjection;

/**
 * Calculator for Required Minimum Distributions (RMDs).
 *
 * <p>Calculates mandatory withdrawals from tax-deferred retirement accounts
 * based on IRS life expectancy tables and SECURE 2.0 rules.
 *
 * <p>Key features:
 * <ul>
 *   <li>SECURE 2.0 start ages (72/73/75 by birth year)</li>
 *   <li>IRS Uniform Lifetime Table factors</li>
 *   <li>Account type rules (Traditional vs Roth)</li>
 *   <li>First-year deadline handling</li>
 * </ul>
 *
 * @see RmdProjection
 * @see <a href="https://www.irs.gov/publications/p590b">IRS Publication 590-B</a>
 */
public interface RmdCalculator {

    /**
     * Calculates the RMD for a given year.
     *
     * @param priorYearEndBalance the account balance at end of prior year
     * @param age the account owner's age in the RMD year
     * @param birthYear the account owner's birth year
     * @param year the tax year
     * @return the RMD projection with amount and details
     */
    RmdProjection calculate(
        BigDecimal priorYearEndBalance,
        int age,
        int birthYear,
        int year
    );

    /**
     * Calculates just the RMD amount.
     *
     * @param priorYearEndBalance the account balance at end of prior year
     * @param age the account owner's age
     * @return the RMD amount
     */
    BigDecimal calculateRmd(BigDecimal priorYearEndBalance, int age);

    /**
     * Returns the distribution factor for a given age.
     *
     * @param age the account owner's age
     * @return the factor from the Uniform Lifetime Table
     */
    BigDecimal getDistributionFactor(int age);

    /**
     * Returns the RMD start age for a given birth year.
     *
     * <p>Per SECURE 2.0:
     * <ul>
     *   <li>Born 1950 or earlier: Age 72</li>
     *   <li>Born 1951-1959: Age 73</li>
     *   <li>Born 1960 or later: Age 75</li>
     * </ul>
     *
     * @param birthYear the account owner's birth year
     * @return the RMD start age
     */
    int getRmdStartAge(int birthYear);

    /**
     * Returns whether the account type is subject to RMDs.
     *
     * <p>Accounts subject to RMDs:
     * <ul>
     *   <li>Traditional IRA, 401(k), 403(b), 457(b)</li>
     *   <li>SEP IRA, SIMPLE IRA</li>
     * </ul>
     *
     * <p>Accounts NOT subject to RMDs (during owner's lifetime):
     * <ul>
     *   <li>Roth IRA (SECURE 2.0 eliminated Roth 401k RMDs)</li>
     * </ul>
     *
     * @param accountType the account type
     * @return true if subject to RMDs
     */
    boolean isSubjectToRmd(AccountType accountType);

    /**
     * Returns whether RMDs are required for the given age and birth year.
     *
     * @param age the account owner's age
     * @param birthYear the account owner's birth year
     * @return true if RMDs are required
     */
    default boolean isRmdRequired(int age, int birthYear) {
        return age >= getRmdStartAge(birthYear);
    }

    /**
     * Returns the first year RMDs are required.
     *
     * @param birthYear the account owner's birth year
     * @return the first RMD year
     */
    default int getFirstRmdYear(int birthYear) {
        return birthYear + getRmdStartAge(birthYear);
    }
}
