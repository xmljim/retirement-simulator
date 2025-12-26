package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;

import io.github.xmljim.retirement.domain.model.InvestmentAccount;
import io.github.xmljim.retirement.domain.value.WithdrawalResult;
import io.github.xmljim.retirement.domain.value.WithdrawalStrategy;

/**
 * Calculator for retirement withdrawal calculations.
 *
 * <p>Provides methods for:
 * <ul>
 *   <li>Calculating withdrawal amounts based on strategy (fixed or percentage)</li>
 *   <li>Computing income-gap based withdrawals</li>
 *   <li>Adjusting withdrawals for inflation</li>
 *   <li>Processing withdrawals from accounts with balance tracking</li>
 * </ul>
 *
 * <p>This calculator handles basic withdrawal scenarios. Full distribution
 * strategies across multiple accounts will be implemented in Milestone 6.
 *
 * @see io.github.xmljim.retirement.domain.calculator.impl.DefaultWithdrawalCalculator
 */
public interface WithdrawalCalculator {

    /**
     * Calculates the withdrawal amount based on the strategy.
     *
     * <p>For FIXED strategy: Returns the fixed dollar amount from the strategy.
     * For PERCENTAGE strategy: Returns portfolioBalance * withdrawalRate.
     *
     * <p>This method does NOT check account balance limits. Use
     * {@link #calculateWithdrawal} for balance-aware calculations.
     *
     * @param portfolioBalance the total portfolio balance
     * @param strategy the withdrawal strategy (fixed amount or percentage)
     * @return the calculated withdrawal amount
     * @throws io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException
     *         if required parameters are null
     */
    BigDecimal calculateWithdrawalAmount(BigDecimal portfolioBalance, WithdrawalStrategy strategy);

    /**
     * Calculates the income gap withdrawal amount.
     *
     * <p>The income gap is the difference between monthly expenses and other
     * income sources (Social Security, pension, etc.). This represents the
     * amount that needs to be withdrawn from savings.
     *
     * <p>Formula: {@code incomeGap = max(0, monthlyExpenses - otherMonthlyIncome)}
     *
     * @param monthlyExpenses the total monthly living expenses
     * @param otherMonthlyIncome income from non-portfolio sources
     * @return the income gap amount (never negative)
     * @throws io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException
     *         if monthlyExpenses is null
     */
    BigDecimal calculateIncomeGap(BigDecimal monthlyExpenses, BigDecimal otherMonthlyIncome);

    /**
     * Adjusts a withdrawal amount for inflation over time.
     *
     * <p>Calculates the inflation-adjusted withdrawal amount to maintain
     * purchasing power. Uses compound inflation formula:
     * {@code adjustedAmount = baseAmount * (1 + inflationRate)^years}
     *
     * @param baseAmount the original withdrawal amount
     * @param annualInflationRate the expected annual inflation rate as decimal
     * @param years the number of years to project
     * @return the inflation-adjusted withdrawal amount
     * @throws io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException
     *         if baseAmount is null
     * @throws io.github.xmljim.retirement.domain.exception.CalculationException
     *         if years is negative
     */
    BigDecimal adjustForInflation(BigDecimal baseAmount, BigDecimal annualInflationRate, int years);

    /**
     * Calculates a withdrawal from an account with full result details.
     *
     * <p>This method:
     * <ul>
     *   <li>Checks if the requested amount exceeds account balance</li>
     *   <li>Handles partial withdrawals when funds are insufficient</li>
     *   <li>Tracks tax treatment based on account type</li>
     *   <li>Calculates the new balance after withdrawal</li>
     * </ul>
     *
     * @param account the source investment account
     * @param requestedAmount the desired withdrawal amount
     * @return a {@link WithdrawalResult} with full withdrawal details
     * @throws io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException
     *         if required parameters are null
     */
    WithdrawalResult calculateWithdrawal(InvestmentAccount account, BigDecimal requestedAmount);

    /**
     * Applies a withdrawal to an account and returns the updated account.
     *
     * <p>Creates a new InvestmentAccount instance with the updated balance
     * after deducting the withdrawal amount. Does not modify the original account.
     *
     * <p>If the withdrawal amount exceeds the balance, withdraws only the
     * available balance (partial withdrawal).
     *
     * @param account the source investment account
     * @param withdrawalAmount the amount to withdraw
     * @return a new InvestmentAccount with the updated balance
     * @throws io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException
     *         if required parameters are null
     */
    InvestmentAccount applyWithdrawal(InvestmentAccount account, BigDecimal withdrawalAmount);

    /**
     * Calculates monthly withdrawal based on annual 4% rule.
     *
     * <p>The 4% rule suggests withdrawing 4% of your portfolio annually,
     * adjusted for inflation in subsequent years. This method calculates
     * the monthly amount for the first year.
     *
     * <p>Formula: {@code monthlyAmount = portfolioBalance * 0.04 / 12}
     *
     * @param portfolioBalance the total portfolio balance
     * @return the monthly withdrawal amount based on 4% rule
     * @throws io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException
     *         if portfolioBalance is null
     */
    BigDecimal calculateFourPercentRuleMonthly(BigDecimal portfolioBalance);

    /**
     * Calculates annual withdrawal based on 4% rule with inflation adjustment.
     *
     * <p>For year 0, returns 4% of the initial portfolio balance.
     * For subsequent years, adjusts the base amount for inflation.
     *
     * @param initialPortfolioBalance the portfolio balance at retirement start
     * @param annualInflationRate the expected annual inflation rate as decimal
     * @param yearsIntoRetirement the number of years since retirement started (0-based)
     * @return the inflation-adjusted annual withdrawal amount
     * @throws io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException
     *         if initialPortfolioBalance is null
     */
    BigDecimal calculateFourPercentRuleAnnual(
        BigDecimal initialPortfolioBalance,
        BigDecimal annualInflationRate,
        int yearsIntoRetirement);
}
