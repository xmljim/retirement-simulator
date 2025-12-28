package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;
import java.util.Map;

import io.github.xmljim.retirement.domain.enums.ExpenseCategory;
import io.github.xmljim.retirement.domain.value.AllocationResult;

/**
 * Strategy for allocating available funds to expense categories.
 *
 * <p>When monthly income and withdrawals are limited, expenses must be
 * prioritized. This strategy determines:
 * <ul>
 *   <li>Which categories get funded first</li>
 *   <li>How to handle shortfalls when funds are insufficient</li>
 *   <li>What to do with surplus when reserves are full</li>
 * </ul>
 *
 * <p>Implementations can provide different allocation approaches:
 * <ul>
 *   <li>Priority-based: Fund categories in priority order</li>
 *   <li>Proportional: Prorate all categories equally</li>
 *   <li>Essential-first: Always fund essentials, then others</li>
 * </ul>
 *
 * @see io.github.xmljim.retirement.domain.value.AllocationResult
 * @see io.github.xmljim.retirement.domain.enums.OverflowBehavior
 * @see io.github.xmljim.retirement.domain.enums.ShortfallHandling
 */
public interface ExpenseAllocationStrategy {

    /**
     * Allocates available funds to expense categories.
     *
     * @param availableFunds total funds available for allocation
     * @param expenses map of expense category to required amount
     * @param reserveTargetMet map of categories where reserve targets are met
     * @return the allocation result with amounts, shortfalls, and warnings
     */
    AllocationResult allocate(
            BigDecimal availableFunds,
            Map<ExpenseCategory, BigDecimal> expenses,
            Map<ExpenseCategory, Boolean> reserveTargetMet
    );

    /**
     * Allocates available funds to expense categories.
     *
     * <p>Convenience method when no reserves are tracked.
     *
     * @param availableFunds total funds available for allocation
     * @param expenses map of expense category to required amount
     * @return the allocation result
     */
    default AllocationResult allocate(
            BigDecimal availableFunds,
            Map<ExpenseCategory, BigDecimal> expenses) {
        return allocate(availableFunds, expenses, Map.of());
    }

    /**
     * Returns the priority order for expense categories.
     *
     * <p>Categories earlier in the list are funded before those later.
     * This is used by priority-based allocation strategies.
     *
     * @return list of categories in priority order
     */
    java.util.List<ExpenseCategory> getPriorityOrder();

    /**
     * Returns the name of this allocation strategy.
     *
     * @return the strategy name
     */
    String getName();

    /**
     * Returns a description of how this strategy allocates funds.
     *
     * @return the strategy description
     */
    String getDescription();
}
