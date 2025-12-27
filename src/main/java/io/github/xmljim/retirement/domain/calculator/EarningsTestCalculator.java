package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;

import io.github.xmljim.retirement.domain.value.EarningsTestResult;

/**
 * Calculator for the Social Security earnings test.
 *
 * <p>The earnings test determines how much of a beneficiary's Social Security
 * benefits are withheld when they earn income from work before Full Retirement Age.
 *
 * <p>SSA rules:
 * <ul>
 *   <li>Below FRA all year: $1 reduction per $2 earned over annual limit</li>
 *   <li>Year reaching FRA: $1 per $3 earned over higher limit (only months before FRA count)</li>
 *   <li>At or after FRA: No earnings test - earn unlimited without reduction</li>
 * </ul>
 *
 * <p><b>Important:</b> The earnings test limits are indexed annually by SSA.
 * Benefits withheld are not permanently lost - they're recalculated at FRA.
 *
 * @see <a href="https://www.ssa.gov/benefits/retirement/planner/whileworking.html">SSA Earnings Test</a>
 * @see <a href="https://www.ssa.gov/oact/cola/rtea.html">SSA Earnings Test Limits</a>
 */
public interface EarningsTestCalculator {

    /**
     * Calculates the earnings test result for a Social Security beneficiary.
     *
     * @param annualEarnings the beneficiary's annual earned income from work
     * @param ssAnnualBenefit the annual Social Security benefit before reduction
     * @param ageMonths the beneficiary's age in months
     * @param fraMonths the beneficiary's Full Retirement Age in months
     * @param year the tax year (for looking up indexed limits)
     * @return the earnings test result with any reduction details
     */
    EarningsTestResult calculate(
        BigDecimal annualEarnings,
        BigDecimal ssAnnualBenefit,
        int ageMonths,
        int fraMonths,
        int year
    );

    /**
     * Calculates the earnings test for the year in which the beneficiary reaches FRA.
     *
     * <p>Special rules apply: only earnings in months BEFORE reaching FRA count,
     * and a higher exempt limit and more favorable reduction ratio apply.
     *
     * @param annualEarnings total annual earnings (will be prorated to months before FRA)
     * @param ssAnnualBenefit the annual Social Security benefit
     * @param monthsBeforeFra number of months in the year before reaching FRA
     * @param year the tax year
     * @return the earnings test result
     */
    EarningsTestResult calculateFraYear(
        BigDecimal annualEarnings,
        BigDecimal ssAnnualBenefit,
        int monthsBeforeFra,
        int year
    );

    /**
     * Returns the exempt earnings limit for beneficiaries below FRA.
     *
     * @param year the tax year
     * @return the annual exempt amount
     */
    BigDecimal getBelowFraLimit(int year);

    /**
     * Returns the exempt earnings limit for the year reaching FRA.
     *
     * @param year the tax year
     * @return the annual exempt amount (higher than below-FRA limit)
     */
    BigDecimal getFraYearLimit(int year);

    /**
     * Determines if someone is subject to the earnings test.
     *
     * @param ageMonths the person's age in months
     * @param fraMonths the person's FRA in months
     * @return true if subject to the earnings test (before FRA)
     */
    default boolean isSubjectToTest(int ageMonths, int fraMonths) {
        return ageMonths < fraMonths;
    }
}
