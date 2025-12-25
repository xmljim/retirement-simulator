package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;

import io.github.xmljim.retirement.domain.model.Scenario;
import io.github.xmljim.retirement.domain.value.AssetAllocation;

/**
 * Calculator for investment return calculations.
 *
 * <p>Provides methods for:
 * <ul>
 *   <li>Calculating blended returns based on asset allocation</li>
 *   <li>Calculating account growth over time</li>
 *   <li>Converting annual returns to monthly returns</li>
 * </ul>
 *
 * @see io.github.xmljim.retirement.domain.calculator.impl.DefaultReturnCalculator
 */
public interface ReturnCalculator {

    /**
     * Calculates the blended annual return rate based on asset allocation
     * and scenario default returns.
     *
     * <p>Formula:
     * <pre>
     * blendedReturn = (stocks% * stockReturn) + (bonds% * bondReturn) + (cash% * cashReturn)
     * </pre>
     *
     * @param allocation the asset allocation (stocks, bonds, cash percentages)
     * @param scenario the scenario containing default return rates
     * @return the blended annual return rate as a decimal (e.g., 0.07 for 7%)
     * @throws IllegalArgumentException if any parameter is null
     */
    BigDecimal calculateBlendedReturn(AssetAllocation allocation, Scenario scenario);

    /**
     * Calculates the blended annual return rate based on asset allocation
     * and explicit return rates.
     *
     * @param allocation the asset allocation (stocks, bonds, cash percentages)
     * @param stockReturn the expected annual return for stocks (decimal)
     * @param bondReturn the expected annual return for bonds (decimal)
     * @param cashReturn the expected annual return for cash (decimal)
     * @return the blended annual return rate as a decimal
     * @throws IllegalArgumentException if allocation is null
     */
    BigDecimal calculateBlendedReturn(
        AssetAllocation allocation,
        BigDecimal stockReturn,
        BigDecimal bondReturn,
        BigDecimal cashReturn);

    /**
     * Calculates the growth of an account balance over a period.
     *
     * <p>Formula for monthly compounding:
     * <pre>
     * growth = balance * (1 + monthlyRate)^months
     * where monthlyRate = annualRate / 12
     * </pre>
     *
     * @param balance the current account balance
     * @param annualReturnRate the annual return rate as a decimal
     * @param months the number of months to calculate growth
     * @return the ending balance after growth
     * @throws IllegalArgumentException if balance is null or months is negative
     */
    BigDecimal calculateAccountGrowth(BigDecimal balance, BigDecimal annualReturnRate, int months);

    /**
     * Converts an annual return rate to a monthly return rate.
     *
     * <p>Uses simple approximation:
     * <pre>
     * monthlyRate = annualRate / 12
     * </pre>
     *
     * <p>Note: For precise compound returns, the exact formula would be
     * {@code (1 + annualRate)^(1/12) - 1}, but for typical return rates
     * the difference is negligible.
     *
     * @param annualRate the annual return rate as a decimal
     * @return the equivalent monthly return rate, or zero if annualRate is null or zero
     */
    BigDecimal toMonthlyRate(BigDecimal annualRate);
}
