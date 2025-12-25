package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import io.github.xmljim.retirement.domain.calculator.ReturnCalculator;
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
        if (balance == null) {
            throw new IllegalArgumentException("Balance cannot be null");
        }
        if (months < 0) {
            throw new IllegalArgumentException("Months cannot be negative: " + months);
        }
        if (months == 0) {
            return balance;
        }

        // Convert annual rate to monthly rate
        BigDecimal monthlyRate = toMonthlyRate(annualReturnRate);

        // Calculate growth: balance * (1 + monthlyRate)^months
        BigDecimal growthFactor = pow(BigDecimal.ONE.add(monthlyRate), months);
        return balance.multiply(growthFactor).setScale(SCALE, ROUNDING_MODE);
    }

    @Override
    public BigDecimal toMonthlyRate(BigDecimal annualRate) {
        if (annualRate == null || annualRate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // For compound interest: monthlyRate = (1 + annualRate)^(1/12) - 1
        // Using approximation: monthlyRate ≈ annualRate / 12 for small rates
        // More accurate formula requires nth root which we approximate

        // Simple approximation for typical return rates
        return annualRate.divide(TWELVE, SCALE, ROUNDING_MODE);
    }

    private BigDecimal pow(BigDecimal base, int exponent) {
        return MathUtils.pow(base, exponent, SCALE, ROUNDING_MODE);
    }
}
