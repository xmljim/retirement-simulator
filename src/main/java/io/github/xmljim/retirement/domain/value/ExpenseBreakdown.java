package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.xmljim.retirement.domain.enums.ExpenseCategory;
import io.github.xmljim.retirement.domain.enums.ExpenseCategoryGroup;

/**
 * Breakdown of expenses by category group for a specific date.
 *
 * <p>Provides a structured view of monthly expenses organized by category group,
 * plus one-time expenses and a total. This is useful for reporting, visualization,
 * and budget analysis.
 *
 * <p>Example usage:
 * <pre>{@code
 * ExpenseBreakdown breakdown = budget.getMonthlyBreakdown(date);
 * BigDecimal essentialCosts = breakdown.essential();
 * BigDecimal totalSpend = breakdown.total();
 * }</pre>
 *
 * @param asOfDate the date this breakdown represents
 * @param essential total of essential expenses (housing, food, utilities, transportation)
 * @param healthcare total of healthcare expenses (Medicare, out-of-pocket, LTC)
 * @param discretionary total of discretionary expenses (travel, entertainment, hobbies)
 * @param contingency total of contingency reserves (home repairs, vehicle replacement)
 * @param debt total of debt payments
 * @param other total of other expenses (taxes, miscellaneous)
 * @param oneTime total of one-time expenses occurring in this period
 * @param total sum of all expense categories
 * @param byCategory map of individual category totals
 */
@SuppressFBWarnings(value = "EI_EXPOSE_REP",
        justification = "Record makes defensive copy in compact constructor; map is immutable")
public record ExpenseBreakdown(
        LocalDate asOfDate,
        BigDecimal essential,
        BigDecimal healthcare,
        BigDecimal discretionary,
        BigDecimal contingency,
        BigDecimal debt,
        BigDecimal other,
        BigDecimal oneTime,
        BigDecimal total,
        Map<ExpenseCategory, BigDecimal> byCategory
) {

    /**
     * Creates a new ExpenseBreakdown with immutable category map.
     */
    public ExpenseBreakdown {
        byCategory = byCategory != null
                ? new EnumMap<>(byCategory)
                : new EnumMap<>(ExpenseCategory.class);
    }

    /**
     * Returns the total for a specific category group.
     *
     * @param group the category group
     * @return the total for that group
     */
    public BigDecimal getGroupTotal(ExpenseCategoryGroup group) {
        return switch (group) {
            case ESSENTIAL -> essential;
            case HEALTHCARE -> healthcare;
            case DISCRETIONARY -> discretionary;
            case CONTINGENCY -> contingency;
            case DEBT -> debt;
            case OTHER -> other;
        };
    }

    /**
     * Returns the total recurring expenses (excludes one-time).
     *
     * @return recurring expense total
     */
    public BigDecimal recurringTotal() {
        return total.subtract(oneTime);
    }

    /**
     * Returns the amount for a specific expense category.
     *
     * @param category the expense category
     * @return the amount for that category, or zero if not present
     */
    public BigDecimal getCategoryAmount(ExpenseCategory category) {
        return byCategory.getOrDefault(category, BigDecimal.ZERO);
    }

    /**
     * Creates an empty breakdown for a date.
     *
     * @param asOfDate the date
     * @return an empty breakdown with all zeros
     */
    public static ExpenseBreakdown empty(LocalDate asOfDate) {
        return new ExpenseBreakdown(
                asOfDate,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new EnumMap<>(ExpenseCategory.class)
        );
    }

    /**
     * Creates a new builder for ExpenseBreakdown.
     *
     * @param asOfDate the date for the breakdown
     * @return a new builder
     */
    public static Builder builder(LocalDate asOfDate) {
        return new Builder(asOfDate);
    }

    /**
     * Builder for creating ExpenseBreakdown instances.
     */
    public static final class Builder {
        private final LocalDate asOfDate;
        private BigDecimal essential = BigDecimal.ZERO;
        private BigDecimal healthcare = BigDecimal.ZERO;
        private BigDecimal discretionary = BigDecimal.ZERO;
        private BigDecimal contingency = BigDecimal.ZERO;
        private BigDecimal debt = BigDecimal.ZERO;
        private BigDecimal other = BigDecimal.ZERO;
        private BigDecimal oneTime = BigDecimal.ZERO;
        private final Map<ExpenseCategory, BigDecimal> byCategory = new EnumMap<>(ExpenseCategory.class);

        private Builder(LocalDate asOfDate) {
            this.asOfDate = asOfDate;
        }

        /**
         * Adds an amount to a specific expense category.
         *
         * <p>This method automatically updates both the category-specific amount
         * and the appropriate group total.
         *
         * @param category the expense category
         * @param amount the amount to add
         * @return this builder
         */
        public Builder addExpense(ExpenseCategory category, BigDecimal amount) {
            if (category == null || amount == null) {
                return this;
            }

            // Update category map
            byCategory.merge(category, amount, BigDecimal::add);

            // Update group total
            switch (category.getGroup()) {
                case ESSENTIAL:
                    essential = essential.add(amount);
                    break;
                case HEALTHCARE:
                    healthcare = healthcare.add(amount);
                    break;
                case DISCRETIONARY:
                    discretionary = discretionary.add(amount);
                    break;
                case CONTINGENCY:
                    contingency = contingency.add(amount);
                    break;
                case DEBT:
                    debt = debt.add(amount);
                    break;
                case OTHER:
                    other = other.add(amount);
                    break;
                default:
                    break;
            }

            return this;
        }

        /**
         * Adds a one-time expense amount.
         *
         * @param amount the amount to add
         * @return this builder
         */
        public Builder addOneTimeExpense(BigDecimal amount) {
            if (amount != null) {
                oneTime = oneTime.add(amount);
            }
            return this;
        }

        /**
         * Adds a one-time expense with category tracking.
         *
         * @param category the expense category
         * @param amount the amount to add
         * @return this builder
         */
        public Builder addOneTimeExpense(ExpenseCategory category, BigDecimal amount) {
            addExpense(category, amount);
            addOneTimeExpense(amount);
            return this;
        }

        /**
         * Builds the ExpenseBreakdown instance.
         *
         * @return a new ExpenseBreakdown
         */
        public ExpenseBreakdown build() {
            // Total is sum of all groups (one-time already included in group totals)
            BigDecimal total = essential
                    .add(healthcare)
                    .add(discretionary)
                    .add(contingency)
                    .add(debt)
                    .add(other);

            return new ExpenseBreakdown(
                    asOfDate,
                    essential,
                    healthcare,
                    discretionary,
                    contingency,
                    debt,
                    other,
                    oneTime,
                    total,
                    byCategory
            );
        }
    }
}
