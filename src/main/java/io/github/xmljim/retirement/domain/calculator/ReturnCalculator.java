package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;
import java.time.YearMonth;

import io.github.xmljim.retirement.domain.model.InvestmentAccount;
import io.github.xmljim.retirement.domain.model.Scenario;
import io.github.xmljim.retirement.domain.value.AssetAllocation;

/**
 * Calculator for investment return calculations.
 *
 * <p>Provides methods for:
 * <ul>
 *   <li>Calculating blended returns based on asset allocation</li>
 *   <li>Calculating account growth over time using true annual compounding</li>
 *   <li>Calculating monthly returns for simulation loops</li>
 *   <li>Integrating with {@link InvestmentAccount} for pre/post retirement rates</li>
 * </ul>
 *
 * <p>All growth calculations use true annual compounding:
 * <pre>
 * endBalance = principal * (1 + annualRate)^(months/12)
 * </pre>
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
     * <p>Uses true annual compounding formula:
     * <pre>
     * endBalance = balance * (1 + annualRate)^(months/12)
     * </pre>
     *
     * <p>Example: $100 at 10% annual rate for 2 months:
     * <pre>
     * 100 * (1.10)^(2/12) = 100 * 1.0160 = $101.60
     * </pre>
     *
     * @param balance the current account balance (must be non-negative)
     * @param annualReturnRate the annual return rate as a decimal (e.g., 0.10 for 10%)
     * @param months the number of months to calculate growth (must be non-negative)
     * @return the ending balance after growth
     * @throws MissingRequiredFieldException if balance is null
     * @throws CalculationException if balance is negative or months is negative
     */
    BigDecimal calculateAccountGrowth(BigDecimal balance, BigDecimal annualReturnRate, int months);

    /**
     * Converts an annual return rate to a monthly return rate.
     *
     * <p>Uses the true annual compounding formula:
     * <pre>
     * monthlyRate = (1 + annualRate)^(1/12) - 1
     * </pre>
     *
     * <p>Example: 10% annual rate:
     * <pre>
     * (1.10)^(1/12) - 1 = 0.00797 (approximately 0.797% monthly)
     * </pre>
     *
     * @param annualRate the annual return rate as a decimal
     * @return the equivalent monthly return rate, or zero if annualRate is null or zero
     */
    BigDecimal toMonthlyRate(BigDecimal annualRate);

    /**
     * Calculates the investment return between two year-months.
     *
     * <p>Uses true annual compounding for the period between start and end.
     *
     * @param balance the starting balance (must be non-negative)
     * @param annualReturnRate the annual return rate as a decimal
     * @param start the start year-month (inclusive)
     * @param end the end year-month (inclusive)
     * @return the ending balance after growth
     * @throws MissingRequiredFieldException if any parameter is null
     * @throws CalculationException if balance is negative
     * @throws InvalidDateRangeException if end is before start
     */
    BigDecimal calculateReturn(BigDecimal balance, BigDecimal annualReturnRate,
                               YearMonth start, YearMonth end);

    /**
     * Calculates the dollar amount of return for a single month.
     *
     * <p>This is a convenience method for simulation loops that process
     * one month at a time. Returns the growth amount, not the ending balance.
     *
     * <p>Formula:
     * <pre>
     * monthlyReturn = balance * ((1 + annualRate)^(1/12) - 1)
     * </pre>
     *
     * @param balance the current balance (must be non-negative)
     * @param annualReturnRate the annual return rate as a decimal
     * @return the dollar amount of return for one month
     * @throws MissingRequiredFieldException if balance is null
     * @throws CalculationException if balance is negative
     */
    BigDecimal calculateMonthlyReturn(BigDecimal balance, BigDecimal annualReturnRate);

    /**
     * Calculates the dollar amount of return for a single month on an account.
     *
     * <p>Uses the account's pre-retirement or post-retirement return rate
     * based on the retirement status. Supports both explicit rates and
     * allocation-based returns.
     *
     * @param account the investment account
     * @param isRetired true if the account owner is retired
     * @return the dollar amount of return for one month
     * @throws MissingRequiredFieldException if account is null
     */
    BigDecimal calculateMonthlyReturn(InvestmentAccount account, boolean isRetired);

    /**
     * Applies one month of investment return to an account.
     *
     * <p>Returns a new {@link InvestmentAccount} with the updated balance
     * after applying one month of growth. The original account is not modified.
     *
     * @param account the investment account
     * @param isRetired true if the account owner is retired
     * @return a new InvestmentAccount with the updated balance
     * @throws MissingRequiredFieldException if account is null
     */
    InvestmentAccount applyMonthlyReturn(InvestmentAccount account, boolean isRetired);
}
