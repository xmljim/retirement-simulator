package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;

import io.github.xmljim.retirement.domain.calculator.WithdrawalCalculator;
import io.github.xmljim.retirement.domain.enums.WithdrawalType;
import io.github.xmljim.retirement.domain.exception.CalculationException;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.model.InvestmentAccount;
import io.github.xmljim.retirement.domain.value.WithdrawalResult;
import io.github.xmljim.retirement.domain.value.WithdrawalStrategy;

/**
 * Default implementation of {@link WithdrawalCalculator}.
 *
 * <p>This implementation provides standard withdrawal calculations for
 * retirement savings plans with support for:
 * <ul>
 *   <li>Fixed dollar amount withdrawals</li>
 *   <li>Percentage-based withdrawals (e.g., 4% rule)</li>
 *   <li>Income gap calculations</li>
 *   <li>Inflation adjustments</li>
 *   <li>Partial withdrawals when funds are insufficient</li>
 * </ul>
 */
public class DefaultWithdrawalCalculator implements WithdrawalCalculator {

    private static final int DOLLAR_SCALE = 2;
    private static final int RATE_SCALE = 10;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final int MONTHS_PER_YEAR = 12;

    /**
     * The standard safe withdrawal rate (4% annually).
     */
    private static final BigDecimal FOUR_PERCENT = new BigDecimal("0.04");

    /**
     * Creates a new DefaultWithdrawalCalculator.
     */
    public DefaultWithdrawalCalculator() {
        // Default constructor
    }

    @Override
    public BigDecimal calculateWithdrawalAmount(BigDecimal portfolioBalance, WithdrawalStrategy strategy) {
        MissingRequiredFieldException.requireNonNull(portfolioBalance, "portfolioBalance");
        MissingRequiredFieldException.requireNonNull(strategy, "strategy");

        if (strategy.getWithdrawalType() == WithdrawalType.FIXED) {
            return strategy.getWithdrawalRate().setScale(DOLLAR_SCALE, ROUNDING_MODE);
        }

        // PERCENTAGE type
        return portfolioBalance
            .multiply(strategy.getWithdrawalRate())
            .setScale(DOLLAR_SCALE, ROUNDING_MODE);
    }

    @Override
    public BigDecimal calculateIncomeGap(BigDecimal monthlyExpenses, BigDecimal otherMonthlyIncome) {
        MissingRequiredFieldException.requireNonNull(monthlyExpenses, "monthlyExpenses");

        BigDecimal otherIncome = otherMonthlyIncome != null ? otherMonthlyIncome : BigDecimal.ZERO;

        BigDecimal gap = monthlyExpenses.subtract(otherIncome);

        // Income gap cannot be negative (you have enough income)
        return gap.max(BigDecimal.ZERO).setScale(DOLLAR_SCALE, ROUNDING_MODE);
    }

    @Override
    public BigDecimal adjustForInflation(BigDecimal baseAmount, BigDecimal annualInflationRate, int years) {
        MissingRequiredFieldException.requireNonNull(baseAmount, "baseAmount");

        if (years < 0) {
            throw CalculationException.invalidPeriod(years);
        }

        if (years == 0) {
            return baseAmount.setScale(DOLLAR_SCALE, ROUNDING_MODE);
        }

        BigDecimal inflationRate = annualInflationRate != null ? annualInflationRate : BigDecimal.ZERO;

        // inflationMultiplier = (1 + inflationRate)^years
        BigDecimal base = BigDecimal.ONE.add(inflationRate);
        BigDecimal multiplier = MathUtils.pow(base, years, RATE_SCALE, ROUNDING_MODE);

        return baseAmount.multiply(multiplier).setScale(DOLLAR_SCALE, ROUNDING_MODE);
    }

    @Override
    public WithdrawalResult calculateWithdrawal(InvestmentAccount account, BigDecimal requestedAmount) {
        MissingRequiredFieldException.requireNonNull(account, "account");
        MissingRequiredFieldException.requireNonNull(requestedAmount, "requestedAmount");

        BigDecimal balance = account.getBalance();

        // Handle zero or negative balance
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            return WithdrawalResult.zero(
                requestedAmount,
                account.getId(),
                account.getAccountType()
            );
        }

        // Handle zero or negative requested amount
        if (requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return WithdrawalResult.full(
                BigDecimal.ZERO,
                account.getId(),
                account.getAccountType(),
                balance
            );
        }

        // Full withdrawal available
        if (balance.compareTo(requestedAmount) >= 0) {
            BigDecimal newBalance = balance.subtract(requestedAmount)
                .setScale(DOLLAR_SCALE, ROUNDING_MODE);
            return WithdrawalResult.full(
                requestedAmount.setScale(DOLLAR_SCALE, ROUNDING_MODE),
                account.getId(),
                account.getAccountType(),
                newBalance
            );
        }

        // Partial withdrawal - insufficient funds
        return WithdrawalResult.partial(
            balance.setScale(DOLLAR_SCALE, ROUNDING_MODE),
            requestedAmount.setScale(DOLLAR_SCALE, ROUNDING_MODE),
            account.getId(),
            account.getAccountType()
        );
    }

    @Override
    public InvestmentAccount applyWithdrawal(InvestmentAccount account, BigDecimal withdrawalAmount) {
        MissingRequiredFieldException.requireNonNull(account, "account");
        MissingRequiredFieldException.requireNonNull(withdrawalAmount, "withdrawalAmount");

        BigDecimal balance = account.getBalance();
        BigDecimal actualWithdrawal = withdrawalAmount.min(balance);
        BigDecimal newBalance = balance.subtract(actualWithdrawal)
            .max(BigDecimal.ZERO)
            .setScale(DOLLAR_SCALE, ROUNDING_MODE);

        return account.withBalance(newBalance);
    }

    @Override
    public BigDecimal calculateFourPercentRuleMonthly(BigDecimal portfolioBalance) {
        MissingRequiredFieldException.requireNonNull(portfolioBalance, "portfolioBalance");

        // Annual amount = portfolio * 4%
        // Monthly amount = annual / 12
        return portfolioBalance
            .multiply(FOUR_PERCENT)
            .divide(BigDecimal.valueOf(MONTHS_PER_YEAR), DOLLAR_SCALE, ROUNDING_MODE);
    }

    @Override
    public BigDecimal calculateFourPercentRuleAnnual(
            BigDecimal initialPortfolioBalance,
            BigDecimal annualInflationRate,
            int yearsIntoRetirement) {

        MissingRequiredFieldException.requireNonNull(initialPortfolioBalance, "initialPortfolioBalance");

        // Base amount = 4% of initial portfolio
        BigDecimal baseAmount = initialPortfolioBalance.multiply(FOUR_PERCENT);

        // Adjust for inflation
        return adjustForInflation(baseAmount, annualInflationRate, yearsIntoRetirement);
    }
}
