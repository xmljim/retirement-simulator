package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.math.RoundingMode;

import io.github.xmljim.retirement.domain.enums.ExpenseCategory;

/**
 * Expense adjustment applied when transitioning from couple to survivor.
 *
 * <p>When a spouse passes away, expenses typically decrease but not uniformly.
 * Fixed costs (housing, utilities) may stay the same, while variable costs
 * (food, transportation) decrease significantly.
 *
 * <p>Research indicates survivors typically need 70-80% of couple expenses,
 * but the adjustment varies significantly by category.
 *
 * @param category the expense category being adjusted
 * @param multiplier the adjustment factor (e.g., 0.60 for 60% of original)
 * @param rationale explanation for this adjustment
 */
public record SurvivorAdjustment(
    ExpenseCategory category,
    BigDecimal multiplier,
    String rationale
) {

    /**
     * Creates a survivor adjustment.
     *
     * @param category the expense category
     * @param multiplier the adjustment factor (0.0-1.0+)
     * @param rationale the reasoning
     * @return a new SurvivorAdjustment
     */
    public static SurvivorAdjustment of(
            ExpenseCategory category,
            double multiplier,
            String rationale) {
        return new SurvivorAdjustment(category, BigDecimal.valueOf(multiplier), rationale);
    }

    /**
     * Creates an adjustment with no change (multiplier = 1.0).
     *
     * @param category the expense category
     * @param rationale the reasoning
     * @return a new SurvivorAdjustment with multiplier 1.0
     */
    public static SurvivorAdjustment unchanged(ExpenseCategory category, String rationale) {
        return of(category, 1.0, rationale);
    }

    /**
     * Creates an adjustment that reduces to half (multiplier = 0.5).
     *
     * @param category the expense category
     * @param rationale the reasoning
     * @return a new SurvivorAdjustment with multiplier 0.5
     */
    public static SurvivorAdjustment halved(ExpenseCategory category, String rationale) {
        return of(category, 0.5, rationale);
    }

    /**
     * Applies this adjustment to an expense amount.
     *
     * @param coupleAmount the original couple expense
     * @return the adjusted survivor expense
     */
    public BigDecimal apply(BigDecimal coupleAmount) {
        return coupleAmount.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Returns the multiplier as a percentage string.
     *
     * @return e.g., "70%" for multiplier 0.70
     */
    public String getMultiplierPercent() {
        return multiplier.multiply(BigDecimal.valueOf(100))
            .setScale(0, RoundingMode.HALF_UP) + "%";
    }

    /**
     * Returns whether this adjustment keeps the expense unchanged.
     *
     * @return true if multiplier is 1.0
     */
    public boolean isUnchanged() {
        return multiplier.compareTo(BigDecimal.ONE) == 0;
    }

    /**
     * Returns whether this adjustment reduces the expense.
     *
     * @return true if multiplier is less than 1.0
     */
    public boolean isReduction() {
        return multiplier.compareTo(BigDecimal.ONE) < 0;
    }
}
