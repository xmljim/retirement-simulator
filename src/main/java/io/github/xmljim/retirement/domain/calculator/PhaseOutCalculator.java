package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;

import io.github.xmljim.retirement.domain.enums.FilingStatus;

/**
 * Calculator for IRA contribution phase-out rules.
 *
 * <p>Determines the allowed IRA contribution amount based on Modified
 * Adjusted Gross Income (MAGI) and filing status. Phase-out rules differ
 * between Roth IRA (contribution eligibility) and Traditional IRA
 * (deductibility of contributions).
 *
 * <p>Phase-out calculation:
 * <ul>
 *   <li>MAGI below lower bound: full contribution allowed</li>
 *   <li>MAGI above upper bound: no contribution (Roth) or no deduction (Traditional)</li>
 *   <li>MAGI within range: linear interpolation</li>
 * </ul>
 *
 * @see <a href="https://www.irs.gov/retirement-plans/amount-of-roth-ira-contributions-that-you-can-make-for-2024">
 *     IRS: Roth IRA Contribution Limits</a>
 * @see <a href="https://www.irs.gov/retirement-plans/ira-deduction-limits">
 *     IRS: IRA Deduction Limits</a>
 */
public interface PhaseOutCalculator {

    /**
     * Calculates the Roth IRA contribution phase-out.
     *
     * <p>Determines the maximum Roth IRA contribution allowed based on MAGI.
     * If fully phased out, the result indicates backdoor Roth eligibility.
     *
     * @param magi the Modified Adjusted Gross Income
     * @param filingStatus the tax filing status
     * @param year the tax year
     * @param requestedContribution the contribution amount requested
     * @param age the contributor's age for catch-up eligibility
     * @return the phase-out result with allowed contribution
     */
    PhaseOutResult calculateRothIraPhaseOut(
        BigDecimal magi,
        FilingStatus filingStatus,
        int year,
        BigDecimal requestedContribution,
        int age
    );

    /**
     * Calculates the Traditional IRA deductibility phase-out.
     *
     * <p>Determines the deductible portion of a Traditional IRA contribution.
     * Phase-out only applies if the contributor or spouse is covered by
     * an employer retirement plan.
     *
     * <p>Coverage scenarios:
     * <ul>
     *   <li>Neither covered: full deduction regardless of MAGI</li>
     *   <li>Contributor covered: use "covered" phase-out thresholds</li>
     *   <li>Only spouse covered: use "spouse covered" thresholds (higher)</li>
     * </ul>
     *
     * @param magi the Modified Adjusted Gross Income
     * @param filingStatus the tax filing status
     * @param year the tax year
     * @param requestedContribution the contribution amount requested
     * @param age the contributor's age for catch-up eligibility
     * @param coveredByEmployerPlan true if contributor has employer plan
     * @param spouseCoveredByEmployerPlan true if spouse has employer plan
     * @return the phase-out result with deductible portion
     */
    PhaseOutResult calculateTraditionalIraPhaseOut(
        BigDecimal magi,
        FilingStatus filingStatus,
        int year,
        BigDecimal requestedContribution,
        int age,
        boolean coveredByEmployerPlan,
        boolean spouseCoveredByEmployerPlan
    );

    /**
     * Returns the maximum IRA contribution limit for the year and age.
     *
     * <p>Includes catch-up contributions for age 50+.
     *
     * @param year the tax year
     * @param age the contributor's age
     * @return the maximum contribution limit
     */
    BigDecimal getMaxIraContribution(int year, int age);
}
