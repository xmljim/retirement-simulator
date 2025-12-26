package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

import io.github.xmljim.retirement.domain.calculator.ReturnCalculator;
import io.github.xmljim.retirement.domain.exception.CalculationException;
import io.github.xmljim.retirement.domain.exception.InvalidDateRangeException;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.model.InvestmentAccount;
import io.github.xmljim.retirement.domain.model.Scenario;
import io.github.xmljim.retirement.domain.value.AssetAllocation;

/**
 * Default implementation of {@link ReturnCalculator}.
 *
 * <p>This implementation provides investment return calculations with:
 * <ul>
 *   <li>Blended returns based on asset allocation</li>
 *   <li>Monthly compounding for account growth</li>
 *   <li>Precise BigDecimal arithmetic</li>
 * </ul>
 */
public class DefaultReturnCalculator implements ReturnCalculator {

    private static final int SCALE = 10;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final MathContext MATH_CONTEXT = new MathContext(SCALE, ROUNDING_MODE);
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWELVE = new BigDecimal("12");

    /**
     * Creates a new DefaultReturnCalculator.
     */
    public DefaultReturnCalculator() {
        // Default constructor
    }

    @Override
    public BigDecimal calculateBlendedReturn(AssetAllocation allocation, Scenario scenario) {
        if (allocation == null) {
            throw new IllegalArgumentException("Allocation cannot be null");
        }
        if (scenario == null) {
            throw new IllegalArgumentException("Scenario cannot be null");
        }

        return calculateBlendedReturn(
            allocation,
            scenario.getDefaultStockReturn(),
            scenario.getDefaultBondReturn(),
            scenario.getDefaultCashReturn());
    }

    @Override
    public BigDecimal calculateBlendedReturn(
            AssetAllocation allocation,
            BigDecimal stockReturn,
            BigDecimal bondReturn,
            BigDecimal cashReturn) {

        if (allocation == null) {
            throw new IllegalArgumentException("Allocation cannot be null");
        }

        // Convert percentages to decimals and calculate weighted returns
        // allocation.getStocksPercentage() returns values like 60.0000 for 60%
        BigDecimal stocksWeight = allocation.getStocksPercentage()
            .divide(HUNDRED, SCALE, ROUNDING_MODE);
        BigDecimal bondsWeight = allocation.getBondsPercentage()
            .divide(HUNDRED, SCALE, ROUNDING_MODE);
        BigDecimal cashWeight = allocation.getCashPercentage()
            .divide(HUNDRED, SCALE, ROUNDING_MODE);

        // Calculate weighted returns
        BigDecimal stockContribution = stocksWeight.multiply(
            stockReturn != null ? stockReturn : BigDecimal.ZERO);
        BigDecimal bondContribution = bondsWeight.multiply(
            bondReturn != null ? bondReturn : BigDecimal.ZERO);
        BigDecimal cashContribution = cashWeight.multiply(
            cashReturn != null ? cashReturn : BigDecimal.ZERO);

        return stockContribution.add(bondContribution).add(cashContribution)
            .setScale(SCALE, ROUNDING_MODE);
    }

    @Override
    public BigDecimal calculateAccountGrowth(BigDecimal balance, BigDecimal annualReturnRate, int months) {
        MissingRequiredFieldException.requireNonNull(balance, "balance");

        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            throw CalculationException.negativeBalance("account growth calculation", balance);
        }
        if (months < 0) {
            throw CalculationException.invalidPeriod(months);
        }
        if (months == 0 || balance.compareTo(BigDecimal.ZERO) == 0) {
            return balance;
        }

        // Handle zero or null rate
        if (annualReturnRate == null || annualReturnRate.compareTo(BigDecimal.ZERO) == 0) {
            return balance;
        }

        // True annual compounding: balance * (1 + annualRate)^(months/12)
        BigDecimal base = BigDecimal.ONE.add(annualReturnRate);
        double exponent = months / 12.0;
        BigDecimal growthFactor = MathUtils.pow(base, exponent, SCALE, ROUNDING_MODE);

        return balance.multiply(growthFactor).setScale(SCALE, ROUNDING_MODE);
    }

    @Override
    public BigDecimal toMonthlyRate(BigDecimal annualRate) {
        if (annualRate == null || annualRate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // True annual compounding: monthlyRate = (1 + annualRate)^(1/12) - 1
        BigDecimal base = BigDecimal.ONE.add(annualRate);
        BigDecimal monthlyFactor = MathUtils.pow(base, 1.0 / 12.0, SCALE, ROUNDING_MODE);

        return monthlyFactor.subtract(BigDecimal.ONE).setScale(SCALE, ROUNDING_MODE);
    }

    @Override
    public BigDecimal calculateReturn(BigDecimal balance, BigDecimal annualReturnRate,
                                      YearMonth start, YearMonth end) {
        MissingRequiredFieldException.requireNonNull(balance, "balance");
        MissingRequiredFieldException.requireNonNull(start, "start");
        MissingRequiredFieldException.requireNonNull(end, "end");

        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            throw CalculationException.negativeBalance("return calculation", balance);
        }
        if (end.isBefore(start)) {
            throw InvalidDateRangeException.dateMustBeAfter("end", "start");
        }

        // Calculate months between (inclusive of both months)
        int months = (int) ChronoUnit.MONTHS.between(start, end) + 1;

        return calculateAccountGrowth(balance, annualReturnRate, months);
    }

    @Override
    public BigDecimal calculateMonthlyReturn(BigDecimal balance, BigDecimal annualReturnRate) {
        MissingRequiredFieldException.requireNonNull(balance, "balance");

        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            throw CalculationException.negativeBalance("monthly return calculation", balance);
        }
        if (balance.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // monthlyReturn = balance * ((1 + annualRate)^(1/12) - 1)
        BigDecimal monthlyRate = toMonthlyRate(annualReturnRate);
        return balance.multiply(monthlyRate).setScale(SCALE, ROUNDING_MODE);
    }

    @Override
    public BigDecimal calculateMonthlyReturn(InvestmentAccount account, boolean isRetired) {
        MissingRequiredFieldException.requireNonNull(account, "account");

        BigDecimal rate = isRetired
            ? account.getPostRetirementReturnRate()
            : account.getPreRetirementReturnRate();

        return calculateMonthlyReturn(account.getBalance(), rate);
    }

    @Override
    public InvestmentAccount applyMonthlyReturn(InvestmentAccount account, boolean isRetired) {
        MissingRequiredFieldException.requireNonNull(account, "account");

        BigDecimal monthlyReturn = calculateMonthlyReturn(account, isRetired);
        BigDecimal newBalance = account.getBalance().add(monthlyReturn);

        return account.withBalance(newBalance);
    }
}
