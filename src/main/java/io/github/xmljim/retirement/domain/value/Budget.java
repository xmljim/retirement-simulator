package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import io.github.xmljim.retirement.domain.config.InflationRates;
import io.github.xmljim.retirement.domain.enums.ExpenseCategory;

/**
 * Aggregates all expenses for a person or household.
 *
 * <p>A Budget is the central model for expense tracking in retirement planning.
 * It combines recurring expenses, one-time expenses, and inflation configuration
 * to project future costs.
 *
 * <p>Use the {@link Builder} to create instances.
 */
public final class Budget {

    private static final int MONTHS_PER_YEAR = 12;

    private final String ownerId;
    private final Optional<String> secondaryOwnerId;
    private final List<RecurringExpense> recurringExpenses;
    private final List<OneTimeExpense> oneTimeExpenses;
    private final InflationRates inflationRates;
    private final int baseYear;

    private Budget(Builder builder) {
        this.ownerId = builder.ownerId;
        this.secondaryOwnerId = Optional.ofNullable(builder.secondaryOwnerId);
        this.recurringExpenses = new ArrayList<>(builder.recurringExpenses);
        this.oneTimeExpenses = new ArrayList<>(builder.oneTimeExpenses);
        this.inflationRates = builder.inflationRates != null
                ? builder.inflationRates : InflationRates.defaults();
        this.baseYear = builder.baseYear;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public Optional<String> getSecondaryOwnerId() {
        return secondaryOwnerId;
    }

    public boolean isCoupleBudget() {
        return secondaryOwnerId.isPresent();
    }

    public List<RecurringExpense> getRecurringExpenses() {
        return Collections.unmodifiableList(recurringExpenses);
    }

    public List<OneTimeExpense> getOneTimeExpenses() {
        return Collections.unmodifiableList(oneTimeExpenses);
    }

    public InflationRates getInflationRates() {
        return inflationRates;
    }

    public int getBaseYear() {
        return baseYear;
    }

    /**
     * Returns recurring expenses filtered by category.
     */
    public List<RecurringExpense> getExpensesByCategory(ExpenseCategory category) {
        return recurringExpenses.stream()
                .filter(e -> e.getCategory() == category)
                .collect(Collectors.toList());
    }

    /**
     * Returns active recurring expenses for a given date.
     */
    public List<RecurringExpense> getActiveExpenses(LocalDate asOfDate) {
        return recurringExpenses.stream()
                .filter(e -> e.isActive(asOfDate))
                .collect(Collectors.toList());
    }

    /**
     * Calculates total monthly expenses for a given date.
     */
    public BigDecimal getTotalMonthlyExpenses(LocalDate asOfDate) {
        BigDecimal recurring = recurringExpenses.stream()
                .filter(e -> e.isActive(asOfDate))
                .map(e -> e.getMonthlyAmount(asOfDate,
                        inflationRates.getRateForCategory(e.getCategory())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal oneTime = oneTimeExpenses.stream()
                .map(e -> e.getAmountForMonth(asOfDate,
                        inflationRates.getRateForCategory(e.getCategory())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return recurring.add(oneTime);
    }

    /**
     * Returns a detailed expense breakdown by category group.
     */
    public ExpenseBreakdown getMonthlyBreakdown(LocalDate asOfDate) {
        ExpenseBreakdown.Builder builder = ExpenseBreakdown.builder(asOfDate);

        recurringExpenses.stream()
                .filter(e -> e.isActive(asOfDate))
                .forEach(e -> builder.addExpense(e.getCategory(),
                        e.getMonthlyAmount(asOfDate,
                                inflationRates.getRateForCategory(e.getCategory()))));

        oneTimeExpenses.forEach(e -> {
            BigDecimal amount = e.getAmountForMonth(asOfDate,
                    inflationRates.getRateForCategory(e.getCategory()));
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                builder.addOneTimeExpense(e.getCategory(), amount);
            }
        });

        return builder.build();
    }

    /**
     * Projects total annual expenses for a given year.
     */
    public BigDecimal getAnnualExpenses(int year) {
        BigDecimal total = BigDecimal.ZERO;
        for (int month = 1; month <= MONTHS_PER_YEAR; month++) {
            LocalDate date = LocalDate.of(year, month, 1);
            total = total.add(getTotalMonthlyExpenses(date));
        }
        return total;
    }

    /**
     * Creates a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Budget budget = (Budget) o;
        return baseYear == budget.baseYear && Objects.equals(ownerId, budget.ownerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerId, baseYear);
    }

    /**
     * Builder for Budget.
     */
    public static final class Builder {
        private String ownerId;
        private String secondaryOwnerId;
        private final List<RecurringExpense> recurringExpenses = new ArrayList<>();
        private final List<OneTimeExpense> oneTimeExpenses = new ArrayList<>();
        private InflationRates inflationRates;
        private int baseYear = LocalDate.now().getYear();

        /**
         * Sets the primary owner ID.
         *
         * @param ownerId the owner ID
         * @return this builder
         */
        public Builder ownerId(String ownerId) {
            this.ownerId = ownerId;
            return this;
        }

        /**
         * Sets the secondary owner ID for couple budgets.
         *
         * @param secondaryOwnerId the secondary owner ID
         * @return this builder
         */
        public Builder secondaryOwnerId(String secondaryOwnerId) {
            this.secondaryOwnerId = secondaryOwnerId;
            return this;
        }

        /**
         * Adds a recurring expense to the budget.
         *
         * @param expense the expense to add
         * @return this builder
         */
        public Builder addRecurringExpense(RecurringExpense expense) {
            if (expense != null) {
                recurringExpenses.add(expense);
            }
            return this;
        }

        /**
         * Adds a one-time expense to the budget.
         *
         * @param expense the expense to add
         * @return this builder
         */
        public Builder addOneTimeExpense(OneTimeExpense expense) {
            if (expense != null) {
                oneTimeExpenses.add(expense);
            }
            return this;
        }

        /**
         * Sets the inflation rates configuration.
         *
         * @param rates the inflation rates
         * @return this builder
         */
        public Builder inflationRates(InflationRates rates) {
            this.inflationRates = rates;
            return this;
        }

        /**
         * Sets the base year for calculations.
         *
         * @param year the base year
         * @return this builder
         */
        public Builder baseYear(int year) {
            this.baseYear = year;
            return this;
        }

        /**
         * Builds the Budget instance.
         *
         * @return a new Budget
         */
        public Budget build() {
            Objects.requireNonNull(ownerId, "ownerId is required");
            return new Budget(this);
        }
    }
}
