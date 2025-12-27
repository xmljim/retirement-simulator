package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;

/**
 * Calculator for Social Security benefit adjustments.
 *
 * <p>Provides methods for calculating Full Retirement Age (FRA) based on
 * birth year, early claiming reductions, and delayed retirement credits.
 *
 * <p>Formulas and values sourced from the Social Security Administration:
 * <ul>
 *   <li>FRA by birth year: <a href="https://www.ssa.gov/oact/ProgData/nra.html">SSA Normal Retirement Age</a></li>
 *   <li>Early/Delayed adjustments: <a href="https://www.ssa.gov/oact/ProgData/ar_drc.html">SSA Adjustment Rates</a></li>
 * </ul>
 *
 * <p>These values were established by the Social Security Amendments of 1983
 * (Public Law 98-21) and may be updated by future legislation.
 *
 * @see io.github.xmljim.retirement.domain.calculator.impl.DefaultSocialSecurityCalculator
 */
public interface SocialSecurityCalculator {

    /**
     * Calculates the Full Retirement Age (FRA) in months for a given birth year.
     *
     * <p>FRA varies by birth year:
     * <ul>
     *   <li>1943-1954: 66 years (792 months)</li>
     *   <li>1955: 66 years, 2 months (794 months)</li>
     *   <li>1956: 66 years, 4 months (796 months)</li>
     *   <li>1957: 66 years, 6 months (798 months)</li>
     *   <li>1958: 66 years, 8 months (800 months)</li>
     *   <li>1959: 66 years, 10 months (802 months)</li>
     *   <li>1960+: 67 years (804 months)</li>
     * </ul>
     *
     * @param birthYear the year of birth
     * @return the FRA in months
     */
    int calculateFraMonths(int birthYear);

    /**
     * Calculates the reduction factor for early claiming.
     *
     * <p>Benefits are reduced for claiming before FRA:
     * <ul>
     *   <li>First 36 months early: 5/9 of 1% per month (6.67% per year)</li>
     *   <li>Additional months: 5/12 of 1% per month (5% per year)</li>
     * </ul>
     *
     * <p>For example, claiming 60 months early (at 62 with FRA 67) results in
     * approximately 30% reduction.
     *
     * @param monthsEarly the number of months before FRA (must be non-negative)
     * @return the reduction as a decimal (e.g., 0.30 for 30% reduction),
     *         or zero if monthsEarly is zero or negative
     */
    BigDecimal calculateEarlyReduction(int monthsEarly);

    /**
     * Calculates the credit factor for delayed claiming.
     *
     * <p>Benefits increase by 8% per year (2/3% per month) for each month
     * claiming is delayed past FRA, up to age 70.
     *
     * @param monthsDelayed the number of months after FRA (must be non-negative)
     * @return the credit as a decimal (e.g., 0.24 for 24% increase),
     *         or zero if monthsDelayed is zero or negative
     */
    BigDecimal calculateDelayedCredits(int monthsDelayed);

    /**
     * Calculates the adjusted benefit based on claiming age.
     *
     * <p>This method determines whether claiming is early, at FRA, or delayed,
     * and applies the appropriate adjustment to the FRA benefit amount.
     *
     * @param fraBenefit the monthly benefit amount at Full Retirement Age
     * @param fraMonths the FRA in months (use {@link #calculateFraMonths(int)})
     * @param claimingMonths the claiming age in months
     * @return the adjusted monthly benefit
     */
    BigDecimal calculateAdjustedBenefit(BigDecimal fraBenefit, int fraMonths, int claimingMonths);

    /**
     * Applies COLA (Cost of Living Adjustment) to a benefit amount.
     *
     * <p>Social Security benefits are adjusted annually for inflation.
     * This method compounds the COLA rate over the specified number of years.
     *
     * @param benefit the base benefit amount
     * @param colaRate the annual COLA rate as a decimal (e.g., 0.028 for 2.8%)
     * @param years the number of years of COLA adjustments
     * @return the COLA-adjusted benefit
     */
    BigDecimal applyColaAdjustment(BigDecimal benefit, BigDecimal colaRate, int years);

    // ==================== Survivor Benefit Methods ====================

    /**
     * Returns the minimum age in months at which survivor benefits can be claimed.
     *
     * <p>Per SSA rules, survivors can claim as early as age 60 (720 months),
     * or age 50 (600 months) if disabled.
     *
     * @return the minimum claiming age in months (typically 720)
     */
    int getSurvivorMinimumClaimingAge();

    /**
     * Returns the minimum age in months for disabled survivors.
     *
     * @return the disabled minimum claiming age in months (typically 600)
     */
    int getSurvivorDisabledMinimumClaimingAge();

    /**
     * Calculates the reduction applied to survivor benefits for early claiming.
     *
     * <p>Survivor reduction is different from regular early claiming reduction.
     * The maximum reduction is approximately 28.5% at age 60 (for FRA of 67).
     * The reduction is prorated linearly from age 60 to FRA.
     *
     * @param claimingAgeMonths the age in months when claiming survivor benefit
     * @param fraMonths the survivor's Full Retirement Age in months
     * @return the reduction as a decimal (e.g., 0.285 for 28.5% reduction)
     */
    BigDecimal calculateSurvivorReduction(int claimingAgeMonths, int fraMonths);

    /**
     * Calculates the adjusted survivor benefit based on claiming age.
     *
     * <p>Applies the survivor reduction formula if claiming before FRA.
     *
     * @param deceasedBenefit the deceased spouse's benefit amount
     * @param claimingAgeMonths the survivor's claiming age in months
     * @param fraMonths the survivor's FRA in months
     * @return the adjusted survivor benefit
     */
    BigDecimal calculateAdjustedSurvivorBenefit(
        BigDecimal deceasedBenefit,
        int claimingAgeMonths,
        int fraMonths
    );
}
