package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;
import java.util.Map;

import io.github.xmljim.retirement.domain.enums.ExpenseCategory;
import io.github.xmljim.retirement.domain.enums.SurvivorRole;
import io.github.xmljim.retirement.domain.value.SurvivorAdjustment;

/**
 * Calculates expense adjustments when transitioning from couple to survivor.
 *
 * <p>When a spouse passes away, expenses typically decrease but not uniformly.
 * This calculator applies category-specific multipliers to determine the
 * survivor's adjusted expenses.
 */
public interface SurvivorExpenseCalculator {

    /**
     * Calculate adjusted expenses after spouse death.
     *
     * @param coupleExpenses map of expense category to annual amount
     * @param survivorRole which spouse survived (PRIMARY or SPOUSE)
     * @return map of expense category to adjusted annual amount
     */
    Map<ExpenseCategory, BigDecimal> calculateSurvivorExpenses(
        Map<ExpenseCategory, BigDecimal> coupleExpenses,
        SurvivorRole survivorRole
    );

    /**
     * Applies the survivor adjustment to a single expense amount.
     *
     * @param category the expense category
     * @param coupleAmount the original couple expense
     * @return the adjusted survivor expense
     */
    BigDecimal adjustExpense(ExpenseCategory category, BigDecimal coupleAmount);

    /**
     * Returns the multiplier for a specific category.
     *
     * @param category the expense category
     * @return the adjustment multiplier (e.g., 0.60 for 60%)
     */
    BigDecimal getMultiplier(ExpenseCategory category);

    /**
     * Returns the adjustment record for a specific category.
     *
     * @param category the expense category
     * @return the full adjustment with rationale
     */
    SurvivorAdjustment getAdjustment(ExpenseCategory category);

    /**
     * Calculates the total couple expenses.
     *
     * @param coupleExpenses map of expense category to annual amount
     * @return total annual couple expenses
     */
    BigDecimal getTotalCoupleExpenses(Map<ExpenseCategory, BigDecimal> coupleExpenses);

    /**
     * Calculates the total survivor expenses.
     *
     * @param coupleExpenses map of expense category to annual amount
     * @param survivorRole which spouse survived
     * @return total annual survivor expenses
     */
    BigDecimal getTotalSurvivorExpenses(
        Map<ExpenseCategory, BigDecimal> coupleExpenses,
        SurvivorRole survivorRole
    );

    /**
     * Calculates the overall expense reduction percentage.
     *
     * @param coupleExpenses map of expense category to annual amount
     * @param survivorRole which spouse survived
     * @return the reduction as a decimal (e.g., 0.25 for 25% reduction)
     */
    BigDecimal getOverallReductionPercentage(
        Map<ExpenseCategory, BigDecimal> coupleExpenses,
        SurvivorRole survivorRole
    );
}
