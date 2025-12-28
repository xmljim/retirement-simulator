package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.xmljim.retirement.domain.config.InflationRates;
import io.github.xmljim.retirement.domain.enums.ExpenseCategory;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.model.PersonProfile;

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

    private final PersonProfile owner;
    private final Optional<PersonProfile> secondaryOwner;
    private final List<RecurringExpense> recurringExpenses;
    private final List<OneTimeExpense> oneTimeExpenses;
    private final InflationRates inflationRates;
    private final int baseYear;

    private Budget(Builder builder) {
        this.owner = builder.owner;
        this.secondaryOwner = Optional.ofNullable(builder.secondaryOwner);
        this.recurringExpenses = new ArrayList<>(builder.recurringExpenses);
        this.oneTimeExpenses = new ArrayList<>(builder.oneTimeExpenses);
        this.inflationRates = builder.inflationRates != null
                ? builder.inflationRates : InflationRates.defaults();
        this.baseYear = builder.baseYear;
    }

    /**
     * Returns the primary owner of this budget.
     *
     * @return the owner profile
     */
    public PersonProfile getOwner() {
        return owner;
    }

    /**
     * Returns the secondary owner for couple budgets.
     *
     * @return optional containing the secondary owner, or empty if single
     */
    public Optional<PersonProfile> getSecondaryOwner() {
        return secondaryOwner;
    }

    /**
     * Returns whether this is a couple budget.
     *
     * @return true if there is a secondary owner
     */
    public boolean isCoupleBudget() {
        return secondaryOwner.isPresent();
    }

    /**
     * Returns all recurring expenses.
     *
     * @return unmodifiable list of recurring expenses
     */
    public List<RecurringExpense> getRecurringExpenses() {
        return Collections.unmodifiableList(recurringExpenses);
    }

    /**
     * Returns all one-time expenses.
     *
     * @return unmodifiable list of one-time expenses
     */
    public List<OneTimeExpense> getOneTimeExpenses() {
        return Collections.unmodifiableList(oneTimeExpenses);
    }

    /**
     * Returns the inflation rates configuration.
     *
     * @return the inflation rates
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "InflationRates is a Spring configuration bean, intentionally shared")
    public InflationRates getInflationRates() {
        return inflationRates;
    }

    /**
     * Returns the base year for calculations.
     *
     * @return the base year
     */
    public int getBaseYear() {
        return baseYear;
    }

    /**
     * Returns recurring expenses filtered by category.
     *
     * @param category the category to filter by
     * @return list of matching expenses
     */
    public List<RecurringExpense> getExpensesByCategory(ExpenseCategory category) {
        return recurringExpenses.stream()
                .filter(e -> e.getCategory() == category)
                .collect(Collectors.toList());
    }

    /**
     * Returns active recurring expenses for a given date.
     *
     * @param asOfDate the date to check
     * @return list of active expenses
     */
    public List<RecurringExpense> getActiveExpenses(LocalDate asOfDate) {
        return recurringExpenses.stream()
                .filter(e -> e.isActive(asOfDate))
                .collect(Collectors.toList());
    }

    /**
     * Calculates total monthly expenses for a given date.
     *
     * @param asOfDate the date to calculate for
     * @return total monthly expenses
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
     *
     * @param asOfDate the date to calculate for
     * @return expense breakdown
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
     *
     * @param year the year to project
     * @return total annual expenses
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
     *
     * @return a new builder
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
        return baseYear == budget.baseYear && Objects.equals(owner.getId(), budget.owner.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner.getId(), baseYear);
    }

    /**
     * Builder for Budget.
     */
    public static final class Builder {
        private PersonProfile owner;
        private PersonProfile secondaryOwner;
        private final List<RecurringExpense> recurringExpenses = new ArrayList<>();
        private final List<OneTimeExpense> oneTimeExpenses = new ArrayList<>();
        private InflationRates inflationRates;
        private int baseYear = LocalDate.now().getYear();

        /**
         * Sets the primary owner.
         *
         * @param owner the owner profile
         * @return this builder
         */
        public Builder owner(PersonProfile owner) {
            this.owner = owner;
            return this;
        }

        /**
         * Sets the secondary owner for couple budgets.
         *
         * @param secondaryOwner the secondary owner profile
         * @return this builder
         */
        public Builder secondaryOwner(PersonProfile secondaryOwner) {
            this.secondaryOwner = secondaryOwner;
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
        @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
                justification = "InflationRates is a Spring configuration bean, intentionally shared")
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
            MissingRequiredFieldException.requireNonNull(owner, "owner");
            return new Budget(this);
        }
    }
}
