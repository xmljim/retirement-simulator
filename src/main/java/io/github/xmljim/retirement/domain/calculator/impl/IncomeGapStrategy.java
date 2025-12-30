package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;

import io.github.xmljim.retirement.domain.calculator.SpendingStrategy;
import io.github.xmljim.retirement.domain.value.SpendingContext;
import io.github.xmljim.retirement.domain.value.SpendingPlan;

/**
 * Spending strategy that withdraws exactly the gap between expenses and other income.
 *
 * <p>This strategy is the simplest approach: withdraw only what you need to cover
 * expenses after accounting for other income sources (Social Security, pensions, etc.).
 *
 * <h2>Formula</h2>
 * <ul>
 *   <li><b>Net Gap</b>: {@code totalExpenses - otherIncome}</li>
 *   <li><b>Gross Withdrawal</b>: {@code netGap / (1 - marginalTaxRate)}</li>
 * </ul>
 *
 * <h2>Tax Gross-Up</h2>
 *
 * <p>When withdrawing from tax-deferred accounts (Traditional IRA, 401k), the
 * withdrawal is taxable income. To end up with the needed net amount, the
 * gross withdrawal must be "grossed up" to account for taxes.
 *
 * <p>Example: If you need $1,000 net and your marginal rate is 22%:
 * <pre>
 * Gross needed = $1,000 / (1 - 0.22) = $1,282.05
 * Taxes = $1,282.05 × 0.22 = $282.05
 * Net received = $1,282.05 - $282.05 = $1,000
 * </pre>
 *
 * <h2>When to Use</h2>
 * <ul>
 *   <li>Retirees who want to minimize portfolio withdrawals</li>
 *   <li>Those with significant guaranteed income (SS, pensions)</li>
 *   <li>Preserving portfolio for legacy or late-life expenses</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Simple: withdraw net gap
 * SpendingStrategy strategy = new IncomeGapStrategy();
 *
 * // With tax gross-up for pre-tax withdrawals
 * SpendingStrategy strategy = new IncomeGapStrategy(new BigDecimal("0.22"));
 *
 * SpendingPlan plan = strategy.calculateWithdrawal(context);
 * }</pre>
 *
 * @see SpendingStrategy
 * @see SpendingContext#incomeGap()
 */
public class IncomeGapStrategy implements SpendingStrategy {

    private static final int SCALE = 10;

    private final BigDecimal marginalTaxRate;
    private final boolean grossUpForTaxes;

    /**
     * Creates an income gap strategy without tax gross-up.
     *
     * <p>Use this when withdrawing from Roth or taxable accounts,
     * or when tax handling is done elsewhere.
     */
    public IncomeGapStrategy() {
        this.marginalTaxRate = BigDecimal.ZERO;
        this.grossUpForTaxes = false;
    }

    /**
     * Creates an income gap strategy with tax gross-up.
     *
     * <p>Use this when primarily withdrawing from tax-deferred accounts
     * and you want the withdrawal to cover taxes as well.
     *
     * @param marginalTaxRate the marginal tax rate as a decimal (e.g., 0.22 for 22%)
     */
    public IncomeGapStrategy(BigDecimal marginalTaxRate) {
        this.marginalTaxRate = marginalTaxRate != null ? marginalTaxRate : BigDecimal.ZERO;
        this.grossUpForTaxes = marginalTaxRate != null && marginalTaxRate.compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public SpendingPlan calculateWithdrawal(SpendingContext context) {
        BigDecimal incomeGap = context.incomeGap();

        // Apply tax gross-up if configured
        BigDecimal targetWithdrawal;
        if (grossUpForTaxes && incomeGap.compareTo(BigDecimal.ZERO) > 0) {
            // Gross = Net / (1 - taxRate)
            BigDecimal divisor = BigDecimal.ONE.subtract(marginalTaxRate);
            targetWithdrawal = incomeGap.divide(divisor, SCALE, RoundingMode.HALF_UP);
        } else {
            targetWithdrawal = incomeGap;
        }

        // Check if portfolio can support this withdrawal
        BigDecimal currentBalance = context.currentPortfolioBalance();
        boolean meetsTarget = currentBalance.compareTo(targetWithdrawal) >= 0;
        BigDecimal actualWithdrawal = meetsTarget ? targetWithdrawal
                : currentBalance.max(BigDecimal.ZERO);

        return SpendingPlan.builder()
                .targetWithdrawal(targetWithdrawal)
                .adjustedWithdrawal(actualWithdrawal)
                .meetsTarget(meetsTarget)
                .strategyUsed(getName())
                .addMetadata("incomeGap", incomeGap.setScale(2, RoundingMode.HALF_UP).toPlainString())
                .addMetadata("totalExpenses", context.totalExpenses().setScale(2, RoundingMode.HALF_UP).toPlainString())
                .addMetadata("otherIncome", context.otherIncome().setScale(2, RoundingMode.HALF_UP).toPlainString())
                .addMetadata("grossUpForTaxes", String.valueOf(grossUpForTaxes))
                .addMetadata("marginalTaxRate", marginalTaxRate.toPlainString())
                .build();
    }

    @Override
    public String getName() {
        return "Income Gap";
    }

    @Override
    public String getDescription() {
        String base = "Withdraws exactly the gap between expenses and other income";
        if (grossUpForTaxes) {
            return base + ", grossed up for " + marginalTaxRate.multiply(new BigDecimal("100"))
                    .setScale(0, RoundingMode.HALF_UP) + "% taxes";
        }
        return base;
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    @Override
    public boolean requiresPriorYearState() {
        return false;
    }

    /**
     * Returns the configured marginal tax rate.
     *
     * @return the marginal tax rate as a decimal
     */
    public BigDecimal getMarginalTaxRate() {
        return marginalTaxRate;
    }

    /**
     * Returns whether tax gross-up is enabled.
     *
     * @return true if grossing up for taxes
     */
    public boolean isGrossUpForTaxes() {
        return grossUpForTaxes;
    }
}
