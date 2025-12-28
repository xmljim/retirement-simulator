package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.xmljim.retirement.domain.calculator.ExpenseAllocationStrategy;
import io.github.xmljim.retirement.domain.enums.ExpenseCategory;
import io.github.xmljim.retirement.domain.enums.ExpenseCategoryGroup;
import io.github.xmljim.retirement.domain.enums.OverflowBehavior;
import io.github.xmljim.retirement.domain.enums.ShortfallHandling;
import io.github.xmljim.retirement.domain.value.AllocationResult;

/**
 * Priority-based expense allocation strategy.
 *
 * <p>Allocates funds to expense categories in priority order.
 * Higher priority categories are fully funded before moving
 * to lower priority categories.
 *
 * <p>Default priority order:
 * <ol>
 *   <li>ESSENTIAL (housing, food, utilities, transportation)</li>
 *   <li>HEALTHCARE (Medicare, out-of-pocket, LTC)</li>
 *   <li>DEBT (fixed debt payments)</li>
 *   <li>CONTINGENCY (reserves for repairs, emergencies)</li>
 *   <li>DISCRETIONARY (travel, entertainment, hobbies)</li>
 *   <li>OTHER (taxes, miscellaneous)</li>
 * </ol>
 */
public class PriorityBasedAllocationStrategy implements ExpenseAllocationStrategy {

    private static final String DEFAULT_NAME = "Priority-Based Allocation";
    private static final String DEFAULT_DESCRIPTION =
            "Allocates funds to expense categories in priority order, "
            + "fully funding higher priorities before lower ones.";

    private final List<ExpenseCategory> priorityOrder;
    private final OverflowBehavior overflowBehavior;
    private final ShortfallHandling shortfallHandling;
    private final String name;
    private final String description;

    /**
     * Creates a strategy with default settings.
     */
    public PriorityBasedAllocationStrategy() {
        this(builder());
    }

    private PriorityBasedAllocationStrategy(Builder builder) {
        this.priorityOrder = List.copyOf(builder.priorityOrder);
        this.overflowBehavior = builder.overflowBehavior;
        this.shortfallHandling = builder.shortfallHandling;
        this.name = builder.name;
        this.description = builder.description;
    }

    @Override
    public AllocationResult allocate(
            BigDecimal availableFunds,
            Map<ExpenseCategory, BigDecimal> expenses,
            Map<ExpenseCategory, Boolean> reserveTargetMet) {

        AllocationResult.Builder result = AllocationResult.builder();
        BigDecimal remainingFunds = availableFunds;

        // Allocate in priority order
        for (ExpenseCategory category : priorityOrder) {
            BigDecimal requested = expenses.getOrDefault(category, BigDecimal.ZERO);
            if (requested.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            // Check if reserve target is met (for contingency categories)
            if (Boolean.TRUE.equals(reserveTargetMet.get(category))) {
                handleOverflow(result, category, requested);
                continue;
            }

            // Allocate what we can
            if (remainingFunds.compareTo(requested) >= 0) {
                // Fully fund this category
                result.allocateFully(category, requested);
                remainingFunds = remainingFunds.subtract(requested);
            } else {
                // Partial or no funding
                result.allocate(category, remainingFunds, requested);
                result.warning(String.format(
                        "Shortfall in %s: needed %s, allocated %s",
                        category.getDisplayName(),
                        requested.toPlainString(),
                        remainingFunds.toPlainString()));
                remainingFunds = BigDecimal.ZERO;
            }
        }

        // Handle any categories not in priority order
        for (Map.Entry<ExpenseCategory, BigDecimal> entry : expenses.entrySet()) {
            ExpenseCategory category = entry.getKey();
            BigDecimal requested = entry.getValue();

            if (!priorityOrder.contains(category) && requested.compareTo(BigDecimal.ZERO) > 0) {
                if (remainingFunds.compareTo(requested) >= 0) {
                    result.allocateFully(category, requested);
                    remainingFunds = remainingFunds.subtract(requested);
                } else {
                    result.allocate(category, remainingFunds, requested);
                    result.warning(String.format(
                            "Shortfall in %s (uncategorized): needed %s, allocated %s",
                            category.getDisplayName(),
                            requested.toPlainString(),
                            remainingFunds.toPlainString()));
                    remainingFunds = BigDecimal.ZERO;
                }
            }
        }

        // Set surplus if any funds remain
        if (remainingFunds.compareTo(BigDecimal.ZERO) > 0) {
            result.surplus(remainingFunds);
        }

        return result.build();
    }

    private void handleOverflow(AllocationResult.Builder result,
                                ExpenseCategory category,
                                BigDecimal amount) {
        // When reserve target is met, record zero allocation with no shortfall
        result.allocateFully(category, BigDecimal.ZERO);
        result.warning(String.format(
                "%s reserve target met - %s overflow handled via %s",
                category.getDisplayName(),
                amount.toPlainString(),
                overflowBehavior.getDisplayName()));
    }

    @Override
    public List<ExpenseCategory> getPriorityOrder() {
        return priorityOrder;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Returns the overflow behavior for this strategy.
     *
     * @return the overflow behavior
     */
    public OverflowBehavior getOverflowBehavior() {
        return overflowBehavior;
    }

    /**
     * Returns the shortfall handling for this strategy.
     *
     * @return the shortfall handling
     */
    public ShortfallHandling getShortfallHandling() {
        return shortfallHandling;
    }

    /**
     * Creates a new builder for this strategy.
     *
     * @return a new builder with default settings
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for PriorityBasedAllocationStrategy.
     */
    public static class Builder {
        private List<ExpenseCategory> priorityOrder = getDefaultPriorityOrder();
        private OverflowBehavior overflowBehavior = OverflowBehavior.REDIRECT_TO_NEXT_PRIORITY;
        private ShortfallHandling shortfallHandling = ShortfallHandling.REDUCE_DISCRETIONARY;
        private String name = DEFAULT_NAME;
        private String description = DEFAULT_DESCRIPTION;

        /**
         * Sets a custom priority order.
         *
         * @param priorities the categories in priority order
         * @return this builder
         */
        public Builder priorityOrder(List<ExpenseCategory> priorities) {
            this.priorityOrder = new ArrayList<>(priorities);
            return this;
        }

        /**
         * Adds a category at the specified priority level.
         *
         * <p>Priority 1 is highest.
         *
         * @param priority the priority level (1-based)
         * @param category the expense category
         * @return this builder
         */
        public Builder priority(int priority, ExpenseCategory category) {
            // Ensure list is large enough
            while (priorityOrder.size() < priority) {
                priorityOrder.add(null);
            }
            priorityOrder.set(priority - 1, category);
            return this;
        }

        /**
         * Sets the overflow behavior.
         *
         * @param behavior the overflow behavior
         * @return this builder
         */
        public Builder overflowBehavior(OverflowBehavior behavior) {
            this.overflowBehavior = behavior;
            return this;
        }

        /**
         * Sets the shortfall handling.
         *
         * @param handling the shortfall handling
         * @return this builder
         */
        public Builder shortfallHandling(ShortfallHandling handling) {
            this.shortfallHandling = handling;
            return this;
        }

        /**
         * Sets a custom name for the strategy.
         *
         * @param name the strategy name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets a custom description for the strategy.
         *
         * @param description the strategy description
         * @return this builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Builds the strategy.
         *
         * @return a new PriorityBasedAllocationStrategy
         */
        public PriorityBasedAllocationStrategy build() {
            // Remove nulls from priority order
            priorityOrder.removeIf(java.util.Objects::isNull);
            return new PriorityBasedAllocationStrategy(this);
        }

        private static List<ExpenseCategory> getDefaultPriorityOrder() {
            List<ExpenseCategory> order = new ArrayList<>();

            // Add categories by group in priority order
            for (ExpenseCategory category : ExpenseCategory.values()) {
                if (category.getGroup() == ExpenseCategoryGroup.ESSENTIAL) {
                    order.add(category);
                }
            }
            for (ExpenseCategory category : ExpenseCategory.values()) {
                if (category.getGroup() == ExpenseCategoryGroup.HEALTHCARE) {
                    order.add(category);
                }
            }
            for (ExpenseCategory category : ExpenseCategory.values()) {
                if (category.getGroup() == ExpenseCategoryGroup.DEBT) {
                    order.add(category);
                }
            }
            for (ExpenseCategory category : ExpenseCategory.values()) {
                if (category.getGroup() == ExpenseCategoryGroup.CONTINGENCY) {
                    order.add(category);
                }
            }
            for (ExpenseCategory category : ExpenseCategory.values()) {
                if (category.getGroup() == ExpenseCategoryGroup.DISCRETIONARY) {
                    order.add(category);
                }
            }
            for (ExpenseCategory category : ExpenseCategory.values()) {
                if (category.getGroup() == ExpenseCategoryGroup.OTHER) {
                    order.add(category);
                }
            }

            return order;
        }
    }
}
