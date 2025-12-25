package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

import io.github.xmljim.retirement.domain.calculator.IncomeCalculator;
import io.github.xmljim.retirement.domain.calculator.InflationCalculator;
import io.github.xmljim.retirement.domain.value.RetirementIncome;
import io.github.xmljim.retirement.domain.value.SocialSecurityIncome;
import io.github.xmljim.retirement.domain.value.WorkingIncome;

/**
 * Default implementation of {@link IncomeCalculator}.
 *
 * <p>This implementation provides income calculations for:
 * <ul>
 *   <li>Monthly salary with COLA adjustments while working</li>
 *   <li>Retirement income projections based on inflation</li>
 *   <li>Social Security benefits with annual adjustments</li>
 *   <li>Other retirement income sources (pensions, annuities)</li>
 * </ul>
 *
 * <p>Requires an {@link InflationCalculator} for inflation-related calculations.
 */
public final class DefaultIncomeCalculator implements IncomeCalculator {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final BigDecimal MONTHS_PER_YEAR = new BigDecimal("12");

    private final InflationCalculator inflationCalculator;

    /**
     * Creates a new DefaultIncomeCalculator with the specified inflation calculator.
     *
     * @param inflationCalculator the calculator to use for inflation adjustments
     * @throws NullPointerException if inflationCalculator is null
     */
    public DefaultIncomeCalculator(InflationCalculator inflationCalculator) {
        this.inflationCalculator = Objects.requireNonNull(inflationCalculator,
            "Inflation calculator cannot be null");
    }

    @Override
    public BigDecimal calculateMonthlySalary(
            LocalDate salaryDate,
            LocalDate retirementDate,
            WorkingIncome workingIncome,
            BigDecimal retirementPercentage,
            BigDecimal inflationRate) {

        validateSalaryInputs(salaryDate, retirementDate, workingIncome,
            retirementPercentage, inflationRate);

        BigDecimal annualSalary = workingIncome.getAnnualSalary();
        BigDecimal monthlySalary = annualSalary.divide(MONTHS_PER_YEAR, SCALE, ROUNDING_MODE);

        LocalDate baseDate = LocalDate.of(LocalDate.now().getYear(), 1, 1);
        int yearsSinceBase = salaryDate.getYear() - baseDate.getYear();

        boolean isRetired = !salaryDate.isBefore(retirementDate);

        if (isRetired) {
            // After retirement: apply inflation and retirement percentage
            BigDecimal inflatedSalary = inflationCalculator.applyInflation(
                monthlySalary, inflationRate, yearsSinceBase);
            return inflatedSalary.multiply(retirementPercentage)
                .setScale(SCALE, ROUNDING_MODE);
        } else {
            // While working: apply COLA
            return inflationCalculator.applyCola(
                monthlySalary, workingIncome.getColaRate(), yearsSinceBase)
                .setScale(SCALE, ROUNDING_MODE);
        }
    }

    @Override
    public BigDecimal calculateSocialSecurityBenefit(
            LocalDate distributionDate,
            SocialSecurityIncome socialSecurity) {

        if (distributionDate == null) {
            throw new IllegalArgumentException("Distribution date cannot be null");
        }
        if (socialSecurity == null) {
            throw new IllegalArgumentException("Social Security income cannot be null");
        }

        // No benefits before start date
        if (distributionDate.isBefore(socialSecurity.getStartDate())) {
            return BigDecimal.ZERO;
        }

        BigDecimal monthlyBenefit = socialSecurity.getMonthlyBenefit();
        BigDecimal colaRate = socialSecurity.getColaRate();

        // Calculate years since current date (for future projections)
        int yearsSinceNow = distributionDate.getYear() - LocalDate.now().getYear();

        if (yearsSinceNow <= 0) {
            return monthlyBenefit.setScale(SCALE, ROUNDING_MODE);
        }

        return inflationCalculator.applyInflation(monthlyBenefit, colaRate, yearsSinceNow)
            .setScale(SCALE, ROUNDING_MODE);
    }

    @Override
    public BigDecimal calculateOtherRetirementIncome(
            LocalDate distributionDate,
            RetirementIncome retirementIncome) {

        if (distributionDate == null) {
            throw new IllegalArgumentException("Distribution date cannot be null");
        }
        if (retirementIncome == null) {
            throw new IllegalArgumentException("Retirement income cannot be null");
        }

        // No income before start date
        if (distributionDate.isBefore(retirementIncome.getStartDate())) {
            return BigDecimal.ZERO;
        }

        BigDecimal monthlyAmount = retirementIncome.getMonthlyAmount();

        // Zero income returns zero (avoid unnecessary calculations)
        if (monthlyAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal adjustmentRate = retirementIncome.getAdjustmentRate();

        // Calculate years since the start date
        int yearsSinceStart = distributionDate.getYear() - retirementIncome.getStartDate().getYear();

        if (yearsSinceStart <= 0) {
            return monthlyAmount.setScale(SCALE, ROUNDING_MODE);
        }

        return inflationCalculator.applyInflation(monthlyAmount, adjustmentRate, yearsSinceStart)
            .setScale(SCALE, ROUNDING_MODE);
    }

    private void validateSalaryInputs(LocalDate salaryDate, LocalDate retirementDate,
                                      WorkingIncome workingIncome, BigDecimal retirementPercentage,
                                      BigDecimal inflationRate) {
        if (salaryDate == null) {
            throw new IllegalArgumentException("Salary date cannot be null");
        }
        if (retirementDate == null) {
            throw new IllegalArgumentException("Retirement date cannot be null");
        }
        if (workingIncome == null) {
            throw new IllegalArgumentException("Working income cannot be null");
        }
        if (retirementPercentage == null) {
            throw new IllegalArgumentException("Retirement percentage cannot be null");
        }
        if (inflationRate == null) {
            throw new IllegalArgumentException("Inflation rate cannot be null");
        }
    }
}
