package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;

import io.github.xmljim.retirement.domain.calculator.SpendingStrategy;
import io.github.xmljim.retirement.domain.value.SpendingContext;
import io.github.xmljim.retirement.domain.value.SpendingPlan;

/**
 * Static withdrawal strategy based on the classic "4% rule" approach.
 *
 * <p>This strategy calculates withdrawals as a fixed percentage of the
 * <em>initial</em> portfolio balance at retirement, optionally adjusted
 * for inflation in subsequent years. This approach was popularized by
 * William Bengen's 1994 research.
 *
 * <h2>Formula</h2>
 * <ul>
 *   <li><b>Year 1</b>: {@code initialPortfolioBalance × withdrawalRate ÷ 12}</li>
 *   <li><b>Year N</b>: {@code year1Amount × (1 + inflationRate)^(yearsInRetirement-1) ÷ 12}</li>
 * </ul>
 *
 * <h2>Key Characteristics</h2>
 * <ul>
 *   <li>Based on <em>initial</em> balance, not current balance</li>
 *   <li>Withdrawals remain stable regardless of market performance</li>
 *   <li>Inflation adjustment maintains purchasing power over time</li>
 *   <li>Simple and predictable for budgeting purposes</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Classic 4% rule with 2.5% inflation
 * SpendingStrategy strategy = new StaticSpendingStrategy(
 *     new BigDecimal("0.04"),
 *     new BigDecimal("0.025"),
 *     true
 * );
 *
 * SpendingPlan plan = strategy.calculateWithdrawal(context);
 * BigDecimal monthlyWithdrawal = plan.adjustedWithdrawal();
 * }</pre>
 *
 * <h2>Considerations</h2>
 * <ul>
 *   <li>Does not respond to portfolio performance (may deplete in bad markets)</li>
 *   <li>May leave significant assets in strong markets</li>
 *   <li>Originally designed for 30-year retirement horizons</li>
 * </ul>
 *
 * @see SpendingStrategy
 * @see SpendingContext
 * @see SpendingPlan
 */
public class StaticSpendingStrategy implements SpendingStrategy {

    /** Default withdrawal rate (4%). */
    public static final BigDecimal DEFAULT_WITHDRAWAL_RATE = new BigDecimal("0.04");

    /** Default inflation rate (2.5%). */
    public static final BigDecimal DEFAULT_INFLATION_RATE = new BigDecimal("0.025");

    private static final BigDecimal TWELVE = new BigDecimal("12");
    private static final int SCALE = 10;

    private final BigDecimal withdrawalRate;
    private final BigDecimal inflationRate;
    private final boolean adjustForInflation;

    /**
     * Creates a static spending strategy with default 4% withdrawal and 2.5% inflation.
     */
    public StaticSpendingStrategy() {
        this(DEFAULT_WITHDRAWAL_RATE, DEFAULT_INFLATION_RATE, true);
    }

    /**
     * Creates a static spending strategy with custom rates.
     *
     * @param withdrawalRate the annual withdrawal rate as a decimal (e.g., 0.04 for 4%)
     * @param inflationRate the annual inflation rate as a decimal (e.g., 0.025 for 2.5%)
     * @param adjustForInflation whether to adjust withdrawals for inflation
     */
    public StaticSpendingStrategy(BigDecimal withdrawalRate, BigDecimal inflationRate,
                                   boolean adjustForInflation) {
        this.withdrawalRate = withdrawalRate != null ? withdrawalRate : DEFAULT_WITHDRAWAL_RATE;
        this.inflationRate = inflationRate != null ? inflationRate : DEFAULT_INFLATION_RATE;
        this.adjustForInflation = adjustForInflation;
    }

    /**
     * Creates a static spending strategy with custom withdrawal rate and default inflation.
     *
     * @param withdrawalRate the annual withdrawal rate as a decimal
     */
    public StaticSpendingStrategy(BigDecimal withdrawalRate) {
        this(withdrawalRate, DEFAULT_INFLATION_RATE, true);
    }

    @Override
    public SpendingPlan calculateWithdrawal(SpendingContext context) {
        BigDecimal initialBalance = context.initialPortfolioBalance();

        // Year 1 annual amount: initialBalance * withdrawalRate
        BigDecimal year1Annual = initialBalance.multiply(withdrawalRate);

        // Apply inflation adjustment for years after year 1
        BigDecimal annualAmount;
        int yearsInRetirement = context.yearsInRetirement();

        if (adjustForInflation && yearsInRetirement > 0) {
            // Year N: year1Amount * (1 + inflationRate)^(years-1)
            // Note: yearsInRetirement is 0-indexed (0 = first year)
            BigDecimal inflationMultiplier = MathUtils.pow(
                    BigDecimal.ONE.add(inflationRate),
                    yearsInRetirement,
                    SCALE,
                    RoundingMode.HALF_UP
            );
            annualAmount = year1Annual.multiply(inflationMultiplier);
        } else {
            annualAmount = year1Annual;
        }

        // Convert to monthly
        BigDecimal monthlyWithdrawal = annualAmount.divide(TWELVE, SCALE, RoundingMode.HALF_UP);

        // Cap at income gap - no need to withdraw more than expenses minus other income
        BigDecimal incomeGap = context.incomeGap();
        BigDecimal targetWithdrawal = monthlyWithdrawal.min(incomeGap);

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
                .addMetadata("withdrawalRate", withdrawalRate.toPlainString())
                .addMetadata("inflationRate", inflationRate.toPlainString())
                .addMetadata("adjustForInflation", String.valueOf(adjustForInflation))
                .addMetadata("yearsInRetirement", String.valueOf(yearsInRetirement))
                .addMetadata("year1AnnualAmount", year1Annual.setScale(2, RoundingMode.HALF_UP).toPlainString())
                .addMetadata("currentAnnualAmount", annualAmount.setScale(2, RoundingMode.HALF_UP).toPlainString())
                .addMetadata("ruleBasedMonthly", monthlyWithdrawal.setScale(2, RoundingMode.HALF_UP).toPlainString())
                .addMetadata("incomeGap", incomeGap.setScale(2, RoundingMode.HALF_UP).toPlainString())
                .build();
    }

    @Override
    public String getName() {
        return "Static " + withdrawalRate.multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.HALF_UP) + "%";
    }

    @Override
    public String getDescription() {
        String base = "Withdraws " + withdrawalRate.multiply(new BigDecimal("100"))
                .setScale(1, RoundingMode.HALF_UP) + "% of initial portfolio balance annually";
        if (adjustForInflation) {
            return base + ", adjusted for " + inflationRate.multiply(new BigDecimal("100"))
                    .setScale(1, RoundingMode.HALF_UP) + "% inflation";
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
     * Returns the configured withdrawal rate.
     *
     * @return the withdrawal rate as a decimal
     */
    public BigDecimal getWithdrawalRate() {
        return withdrawalRate;
    }

    /**
     * Returns the configured inflation rate.
     *
     * @return the inflation rate as a decimal
     */
    public BigDecimal getInflationRate() {
        return inflationRate;
    }

    /**
     * Returns whether inflation adjustment is enabled.
     *
     * @return true if adjusting for inflation
     */
    public boolean isAdjustForInflation() {
        return adjustForInflation;
    }
}
