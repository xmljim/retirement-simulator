package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Optional;

import io.github.xmljim.retirement.domain.calculator.GuardrailsConfiguration;
import io.github.xmljim.retirement.domain.calculator.SpendingStrategy;
import io.github.xmljim.retirement.domain.value.SpendingContext;
import io.github.xmljim.retirement.domain.value.SpendingPlan;

/**
 * Dynamic spending strategy using guardrails-based adjustments.
 *
 * <p>This strategy adjusts spending based on portfolio performance, providing
 * flexibility while protecting against sequence-of-returns risk. Supports three
 * approaches via {@link GuardrailsConfiguration}:
 * <ul>
 *   <li><b>Guyton-Klinger:</b> Rate-based triggers with 10% adjustments</li>
 *   <li><b>Vanguard Dynamic:</b> Ceiling/floor limiting annual changes</li>
 *   <li><b>Kitces Ratcheting:</b> One-way ratchet that only increases spending</li>
 * </ul>
 *
 * <p>Inflation is controlled by the simulation engine (via SpendingContext) to
 * support historical backtests, Monte Carlo scenarios, and stress testing.
 *
 * @see GuardrailsConfiguration
 * @see SpendingStrategy
 */
public class GuardrailsSpendingStrategy implements SpendingStrategy {

    /** Strategy parameter key for inflation rate. */
    public static final String PARAM_INFLATION_RATE = "inflationRate";

    /** Default inflation rate if not provided. */
    public static final BigDecimal DEFAULT_INFLATION = new BigDecimal("0.025");

    private static final BigDecimal TWELVE = new BigDecimal("12");
    private static final int SCALE = 10;

    private final GuardrailsConfiguration config;

    /**
     * Creates a guardrails strategy with the specified configuration.
     *
     * @param config the guardrails configuration
     */
    public GuardrailsSpendingStrategy(GuardrailsConfiguration config) {
        this.config = config != null ? config : GuardrailsConfiguration.guytonKlinger();
    }

    @Override
    public SpendingPlan calculateWithdrawal(SpendingContext context) {
        SpendingPlan.Builder planBuilder = SpendingPlan.builder().strategyUsed(getName());
        BigDecimal inflationRate = getInflationRate(context);

        // Year 1: use initial rate calculation
        BigDecimal priorYearSpending = context.simulation().getPriorYearSpending();
        if (priorYearSpending.compareTo(BigDecimal.ZERO) == 0) {
            return calculateFirstYearWithdrawal(context, planBuilder, inflationRate);
        }

        // Get baseline and apply inflation
        BigDecimal baseSpending = applyInflation(priorYearSpending, context, inflationRate);
        String priorSpendingStr = priorYearSpending.setScale(2, RoundingMode.HALF_UP).toPlainString();
        planBuilder.addMetadata("priorYearSpending", priorSpendingStr);
        planBuilder.addMetadata("baseAfterInflation", baseSpending.setScale(2, RoundingMode.HALF_UP).toPlainString());

        // Calculate current withdrawal rate
        BigDecimal currentBalance = context.currentPortfolioBalance();
        BigDecimal currentRate = baseSpending.divide(currentBalance, SCALE, RoundingMode.HALF_UP);
        planBuilder.addMetadata("currentRate", currentRate.setScale(4, RoundingMode.HALF_UP).toPlainString());

        // Apply guardrail adjustments
        BigDecimal adjustedSpending = applyGuardrails(baseSpending, currentRate, context, planBuilder);

        // Convert to monthly and cap at income gap
        BigDecimal monthlyWithdrawal = adjustedSpending.divide(TWELVE, SCALE, RoundingMode.HALF_UP);
        BigDecimal targetWithdrawal = monthlyWithdrawal.min(context.incomeGap());

        boolean meetsTarget = currentBalance.compareTo(targetWithdrawal) >= 0;
        BigDecimal actualWithdrawal = meetsTarget ? targetWithdrawal : currentBalance.max(BigDecimal.ZERO);

        return planBuilder
                .targetWithdrawal(targetWithdrawal)
                .adjustedWithdrawal(actualWithdrawal)
                .meetsTarget(meetsTarget)
                .build();
    }

    private SpendingPlan calculateFirstYearWithdrawal(SpendingContext context,
            SpendingPlan.Builder planBuilder, BigDecimal inflationRate) {
        BigDecimal initialBalance = context.initialPortfolioBalance();
        BigDecimal annualAmount = initialBalance.multiply(config.initialWithdrawalRate());
        BigDecimal monthlyWithdrawal = annualAmount.divide(TWELVE, SCALE, RoundingMode.HALF_UP);
        BigDecimal targetWithdrawal = monthlyWithdrawal.min(context.incomeGap());

        BigDecimal currentBalance = context.currentPortfolioBalance();
        boolean meetsTarget = currentBalance.compareTo(targetWithdrawal) >= 0;
        BigDecimal actualWithdrawal = meetsTarget ? targetWithdrawal : currentBalance.max(BigDecimal.ZERO);

        return planBuilder
                .targetWithdrawal(targetWithdrawal)
                .adjustedWithdrawal(actualWithdrawal)
                .meetsTarget(meetsTarget)
                .addMetadata("firstYear", "true")
                .addMetadata("initialRate", config.initialWithdrawalRate().toPlainString())
                .addMetadata("inflationRate", inflationRate.toPlainString())
                .build();
    }

    private BigDecimal applyInflation(BigDecimal priorSpending, SpendingContext context,
            BigDecimal inflationRate) {
        // Guyton-Klinger Rule 2: Skip inflation on down years when rate exceeds initial
        if (config.skipInflationOnDownYears()) {
            BigDecimal priorReturn = context.simulation().getPriorYearReturn();
            if (priorReturn.compareTo(BigDecimal.ZERO) < 0) {
                BigDecimal currentRate = context.currentWithdrawalRate();
                if (currentRate.compareTo(config.initialWithdrawalRate()) > 0) {
                    return priorSpending; // Skip inflation adjustment
                }
            }
        }
        return priorSpending.multiply(BigDecimal.ONE.add(inflationRate));
    }

    private BigDecimal applyGuardrails(BigDecimal baseSpending, BigDecimal currentRate,
            SpendingContext context, SpendingPlan.Builder planBuilder) {
        BigDecimal adjustedSpending = baseSpending;
        BigDecimal initialRate = config.initialWithdrawalRate();

        // Check upper guardrail (Prosperity Rule) - rate dropped, may increase spending
        if (config.hasUpperGuardrail()) {
            BigDecimal upperThreshold = initialRate.multiply(config.upperThresholdMultiplier());
            if (currentRate.compareTo(upperThreshold) < 0 && canRatchet(context)) {
                adjustedSpending = baseSpending.multiply(BigDecimal.ONE.add(config.increaseAdjustment()));
                planBuilder.addMetadata("adjustment", "increase");
                planBuilder.addMetadata("reason", "prosperity rule triggered");
            }
        }

        // Check lower guardrail (Capital Preservation) - rate rose, may decrease spending
        if (config.hasLowerGuardrail()) {
            BigDecimal lowerThreshold = initialRate.multiply(config.lowerThresholdMultiplier());
            int yearsInRetirement = context.yearsInRetirement();
            boolean capPreservationActive = config.yearsBeforeCapPreservationEnds() == 0
                    || yearsInRetirement < config.yearsBeforeCapPreservationEnds();

            if (currentRate.compareTo(lowerThreshold) > 0 && capPreservationActive) {
                adjustedSpending = baseSpending.multiply(BigDecimal.ONE.subtract(config.decreaseAdjustment()));
                planBuilder.addMetadata("adjustment", "decrease");
                planBuilder.addMetadata("reason", "capital preservation rule triggered");
            }
        }

        // Apply absolute floor/ceiling constraints
        if (config.hasAbsoluteFloor() && adjustedSpending.compareTo(config.absoluteFloor()) < 0) {
            adjustedSpending = config.absoluteFloor();
            planBuilder.addMetadata("constraint", "floor applied");
        }
        if (config.hasAbsoluteCeiling() && adjustedSpending.compareTo(config.absoluteCeiling()) > 0) {
            adjustedSpending = config.absoluteCeiling();
            planBuilder.addMetadata("constraint", "ceiling applied");
        }

        return adjustedSpending;
    }

    private boolean canRatchet(SpendingContext context) {
        if (config.minimumYearsBetweenRatchets() <= 1) {
            return true;
        }
        Optional<YearMonth> lastRatchet = context.simulation().getLastRatchetMonth();
        if (lastRatchet.isEmpty()) {
            return true;
        }
        YearMonth current = YearMonth.from(context.date());
        long monthsSinceRatchet = lastRatchet.get().until(current, java.time.temporal.ChronoUnit.MONTHS);
        return monthsSinceRatchet >= config.minimumYearsBetweenRatchets() * 12L;
    }

    private BigDecimal getInflationRate(SpendingContext context) {
        return context.getStrategyParam(PARAM_INFLATION_RATE, DEFAULT_INFLATION);
    }

    @Override
    public String getName() {
        return "Guardrails";
    }

    @Override
    public String getDescription() {
        return "Dynamic spending with guardrails-based adjustments for portfolio performance";
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    public boolean requiresPriorYearState() {
        return true;
    }

    /** Returns the configuration. */
    public GuardrailsConfiguration getConfiguration() {
        return config;
    }
}
