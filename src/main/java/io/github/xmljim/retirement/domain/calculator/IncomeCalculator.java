package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.github.xmljim.retirement.domain.value.RetirementIncome;
import io.github.xmljim.retirement.domain.value.SocialSecurityIncome;
import io.github.xmljim.retirement.domain.value.WorkingIncome;

/**
 * Calculator for income-related calculations during working and retirement years.
 *
 * <p>Provides methods for:
 * <ul>
 *   <li>Calculating monthly salary with COLA adjustments</li>
 *   <li>Calculating Social Security benefits with inflation adjustments</li>
 *   <li>Calculating other retirement income (pensions, annuities)</li>
 * </ul>
 *
 * <p>Income calculations depend on retirement status:
 * <ul>
 *   <li><b>Working:</b> Monthly salary based on annual salary with COLA</li>
 *   <li><b>Retired:</b> Income from Social Security and other retirement sources</li>
 * </ul>
 *
 * @see io.github.xmljim.retirement.domain.calculator.impl.DefaultIncomeCalculator
 */
public interface IncomeCalculator {

    /**
     * Calculates the monthly salary for a given date.
     *
     * <p>While working, the salary is adjusted for cost-of-living increases.
     * After retirement, the salary represents the target retirement income
     * (typically a percentage of pre-retirement income) adjusted for inflation.
     *
     * @param salaryDate the date for which to calculate the salary
     * @param retirementDate the planned retirement date
     * @param workingIncome the working income configuration
     * @param retirementPercentage the percentage of salary to target in retirement
     *                             (e.g., 0.80 for 80%)
     * @param inflationRate the general inflation rate for retirement calculations
     * @return the calculated monthly salary amount
     * @throws IllegalArgumentException if any required parameter is null
     */
    BigDecimal calculateMonthlySalary(
        LocalDate salaryDate,
        LocalDate retirementDate,
        WorkingIncome workingIncome,
        BigDecimal retirementPercentage,
        BigDecimal inflationRate);

    /**
     * Calculates the Social Security benefit for a given distribution date.
     *
     * <p>Benefits are only paid after the Social Security start date. The benefit
     * is adjusted for inflation/COLA from the start date.
     *
     * @param distributionDate the date for which to calculate the benefit
     * @param socialSecurity the Social Security income configuration
     * @return the monthly benefit amount, or zero if before the start date
     * @throws IllegalArgumentException if any required parameter is null
     */
    BigDecimal calculateSocialSecurityBenefit(
        LocalDate distributionDate,
        SocialSecurityIncome socialSecurity);

    /**
     * Calculates other retirement income (pensions, annuities) for a given date.
     *
     * <p>Income is only paid after the start date. The amount is adjusted
     * for the specified adjustment rate from the start date.
     *
     * @param distributionDate the date for which to calculate the income
     * @param retirementIncome the retirement income configuration
     * @return the monthly income amount, or zero if before the start date
     * @throws IllegalArgumentException if any required parameter is null
     */
    BigDecimal calculateOtherRetirementIncome(
        LocalDate distributionDate,
        RetirementIncome retirementIncome);
}
