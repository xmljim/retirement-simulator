package io.github.xmljim.retirement.simulation.result;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.xmljim.retirement.domain.enums.ExpenseCategoryGroup;
import io.github.xmljim.retirement.domain.enums.SimulationPhase;

/**
 * Captures the complete state of a simulation for a single month.
 *
 * <p>MonthlySnapshot is the primary output record for each month of simulation.
 * It aggregates all financial activity including:
 * <ul>
 *   <li>Account-level flows (contributions, withdrawals, returns)</li>
 *   <li>Income from all sources</li>
 *   <li>Expenses by category</li>
 *   <li>Tax calculations</li>
 *   <li>Cumulative metrics</li>
 *   <li>Events that triggered during the month</li>
 * </ul>
 *
 * <p>The {@code accountFlows} map is the source of truth for account balances.
 * Aggregate portfolio totals are computed via methods like {@link #totalPortfolioBalance()}.
 *
 * @param month the year-month this snapshot represents
 * @param accountFlows per-account financial flows (source of truth)
 * @param salaryIncome salary/wages income this month
 * @param socialSecurityIncome Social Security benefits this month
 * @param pensionIncome pension income this month
 * @param otherIncome other income (rental, part-time, etc.) this month
 * @param totalExpenses total expenses this month
 * @param expensesByCategory expenses broken down by category group
 * @param taxes tax calculations for this month
 * @param cumulativeContributions running total of all contributions since start
 * @param cumulativeWithdrawals running total of all withdrawals since start
 * @param cumulativeReturns running total of all investment returns since start
 * @param cumulativeTaxesPaid running total of all taxes paid since start
 * @param phase the simulation phase for this month
 * @param eventsTriggered list of events that fired this month
 *
 * @see AccountMonthlyFlow
 * @see TaxSummary
 * @see SimulationPhase
 */
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "Collections are made unmodifiable in compact constructor"
)
public record MonthlySnapshot(
        YearMonth month,
        Map<UUID, AccountMonthlyFlow> accountFlows,
        BigDecimal salaryIncome,
        BigDecimal socialSecurityIncome,
        BigDecimal pensionIncome,
        BigDecimal otherIncome,
        BigDecimal totalExpenses,
        Map<ExpenseCategoryGroup, BigDecimal> expensesByCategory,
        TaxSummary taxes,
        BigDecimal cumulativeContributions,
        BigDecimal cumulativeWithdrawals,
        BigDecimal cumulativeReturns,
        BigDecimal cumulativeTaxesPaid,
        SimulationPhase phase,
        List<String> eventsTriggered
) {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /**
     * Compact constructor with validation and defensive copying.
     */
    public MonthlySnapshot {
        if (month == null) {
            throw new IllegalArgumentException("month cannot be null");
        }
        // Defensive copies for mutable collections
        accountFlows = accountFlows == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(accountFlows);
        expensesByCategory = expensesByCategory == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(expensesByCategory);
        eventsTriggered = eventsTriggered == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(eventsTriggered);

        // Null-safe defaults
        if (salaryIncome == null) {
            salaryIncome = BigDecimal.ZERO;
        }
        if (socialSecurityIncome == null) {
            socialSecurityIncome = BigDecimal.ZERO;
        }
        if (pensionIncome == null) {
            pensionIncome = BigDecimal.ZERO;
        }
        if (otherIncome == null) {
            otherIncome = BigDecimal.ZERO;
        }
        if (totalExpenses == null) {
            totalExpenses = BigDecimal.ZERO;
        }
        if (taxes == null) {
            taxes = TaxSummary.empty();
        }
        if (cumulativeContributions == null) {
            cumulativeContributions = BigDecimal.ZERO;
        }
        if (cumulativeWithdrawals == null) {
            cumulativeWithdrawals = BigDecimal.ZERO;
        }
        if (cumulativeReturns == null) {
            cumulativeReturns = BigDecimal.ZERO;
        }
        if (cumulativeTaxesPaid == null) {
            cumulativeTaxesPaid = BigDecimal.ZERO;
        }
        if (phase == null) {
            phase = SimulationPhase.ACCUMULATION;
        }
    }

    // ─── Portfolio Aggregations ─────────────────────────────────────────────────

    /**
     * Calculates total portfolio balance across all accounts.
     *
     * @return sum of all account ending balances
     */
    public BigDecimal totalPortfolioBalance() {
        return accountFlows.values().stream()
                .map(AccountMonthlyFlow::endingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, ROUNDING);
    }

    /**
     * Calculates total contributions this month across all accounts.
     *
     * @return sum of all account contributions
     */
    public BigDecimal totalContributions() {
        return accountFlows.values().stream()
                .map(AccountMonthlyFlow::contributions)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, ROUNDING);
    }

    /**
     * Calculates total withdrawals this month across all accounts.
     *
     * @return sum of all account withdrawals
     */
    public BigDecimal totalWithdrawals() {
        return accountFlows.values().stream()
                .map(AccountMonthlyFlow::withdrawals)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, ROUNDING);
    }

    /**
     * Calculates total investment returns this month across all accounts.
     *
     * @return sum of all account returns
     */
    public BigDecimal totalReturns() {
        return accountFlows.values().stream()
                .map(AccountMonthlyFlow::returns)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, ROUNDING);
    }

    // ─── Income Aggregations ────────────────────────────────────────────────────

    /**
     * Calculates total income from all sources this month.
     *
     * @return sum of salary, Social Security, pension, and other income
     */
    public BigDecimal totalIncome() {
        return salaryIncome
                .add(socialSecurityIncome)
                .add(pensionIncome)
                .add(otherIncome)
                .setScale(SCALE, ROUNDING);
    }

    /**
     * Calculates total non-salary income this month.
     *
     * <p>This represents retirement income sources (SS, pension, etc.)
     * and is used for gap analysis during distribution phase.
     *
     * @return sum of Social Security, pension, and other income
     */
    public BigDecimal totalNonSalaryIncome() {
        return socialSecurityIncome
                .add(pensionIncome)
                .add(otherIncome)
                .setScale(SCALE, ROUNDING);
    }

    // ─── Cash Flow ──────────────────────────────────────────────────────────────

    /**
     * Calculates net cash flow (income minus expenses).
     *
     * <p>Positive indicates surplus; negative indicates gap requiring withdrawals.
     *
     * @return total income minus total expenses
     */
    public BigDecimal netCashFlow() {
        return totalIncome().subtract(totalExpenses).setScale(SCALE, ROUNDING);
    }

    /**
     * Calculates the income gap (expenses minus non-salary income).
     *
     * <p>This represents the amount needed from portfolio withdrawals
     * during retirement.
     *
     * @return expenses minus non-salary income, or ZERO if no gap
     */
    public BigDecimal incomeGap() {
        return totalExpenses
                .subtract(totalNonSalaryIncome())
                .max(BigDecimal.ZERO)
                .setScale(SCALE, ROUNDING);
    }

    // ─── Convenience Methods ────────────────────────────────────────────────────

    /**
     * Returns the year portion of the month.
     *
     * @return the year
     */
    public int year() {
        return month.getYear();
    }

    /**
     * Returns the month-of-year (1-12).
     *
     * @return the month value
     */
    public int monthValue() {
        return month.getMonthValue();
    }

    /**
     * Indicates whether any events triggered this month.
     *
     * @return true if events list is not empty
     */
    public boolean hadEvents() {
        return !eventsTriggered.isEmpty();
    }

    /**
     * Returns the number of accounts in this snapshot.
     *
     * @return account count
     */
    public int accountCount() {
        return accountFlows.size();
    }

    /**
     * Gets the flow for a specific account.
     *
     * @param accountId the account identifier
     * @return the account flow, or null if not found
     */
    public AccountMonthlyFlow getAccountFlow(UUID accountId) {
        return accountFlows.get(accountId);
    }

    /**
     * Gets expenses for a specific category group.
     *
     * @param group the expense category group
     * @return the expense amount, or ZERO if not present
     */
    public BigDecimal getExpensesByGroup(ExpenseCategoryGroup group) {
        return expensesByCategory.getOrDefault(group, BigDecimal.ZERO);
    }

    /**
     * Creates a builder for constructing MonthlySnapshot instances.
     *
     * @param month the year-month
     * @return a new builder
     */
    public static Builder builder(YearMonth month) {
        return new Builder(month);
    }

    /**
     * Builder for MonthlySnapshot.
     */
    public static final class Builder {
        private final YearMonth month;
        private Map<UUID, AccountMonthlyFlow> accountFlows = Collections.emptyMap();
        private BigDecimal salaryIncome = BigDecimal.ZERO;
        private BigDecimal socialSecurityIncome = BigDecimal.ZERO;
        private BigDecimal pensionIncome = BigDecimal.ZERO;
        private BigDecimal otherIncome = BigDecimal.ZERO;
        private BigDecimal totalExpenses = BigDecimal.ZERO;
        private Map<ExpenseCategoryGroup, BigDecimal> expensesByCategory = Collections.emptyMap();
        private TaxSummary taxes = TaxSummary.empty();
        private BigDecimal cumulativeContributions = BigDecimal.ZERO;
        private BigDecimal cumulativeWithdrawals = BigDecimal.ZERO;
        private BigDecimal cumulativeReturns = BigDecimal.ZERO;
        private BigDecimal cumulativeTaxesPaid = BigDecimal.ZERO;
        private SimulationPhase phase = SimulationPhase.ACCUMULATION;
        private List<String> eventsTriggered = Collections.emptyList();

        private Builder(YearMonth month) {
            this.month = month;
        }

        /**
         * Sets the per-account flows.
         *
         * @param flows the account flows map
         * @return this builder
         */
        public Builder accountFlows(Map<UUID, AccountMonthlyFlow> flows) {
            this.accountFlows = flows == null ? Collections.emptyMap() : new HashMap<>(flows);
            return this;
        }

        /**
         * Sets the salary income.
         *
         * @param amount the salary income
         * @return this builder
         */
        public Builder salaryIncome(BigDecimal amount) {
            this.salaryIncome = amount;
            return this;
        }

        /**
         * Sets the Social Security income.
         *
         * @param amount the Social Security income
         * @return this builder
         */
        public Builder socialSecurityIncome(BigDecimal amount) {
            this.socialSecurityIncome = amount;
            return this;
        }

        /**
         * Sets the pension income.
         *
         * @param amount the pension income
         * @return this builder
         */
        public Builder pensionIncome(BigDecimal amount) {
            this.pensionIncome = amount;
            return this;
        }

        /**
         * Sets the other income.
         *
         * @param amount the other income
         * @return this builder
         */
        public Builder otherIncome(BigDecimal amount) {
            this.otherIncome = amount;
            return this;
        }

        /**
         * Sets the total expenses.
         *
         * @param amount the total expenses
         * @return this builder
         */
        public Builder totalExpenses(BigDecimal amount) {
            this.totalExpenses = amount;
            return this;
        }

        /**
         * Sets the expenses by category.
         *
         * @param expenses the expenses by category group
         * @return this builder
         */
        public Builder expensesByCategory(Map<ExpenseCategoryGroup, BigDecimal> expenses) {
            this.expensesByCategory = expenses == null ? Collections.emptyMap() : new HashMap<>(expenses);
            return this;
        }

        /**
         * Sets the tax summary.
         *
         * @param taxes the tax summary
         * @return this builder
         */
        public Builder taxes(TaxSummary taxes) {
            this.taxes = taxes;
            return this;
        }

        /**
         * Sets the cumulative contributions.
         *
         * @param amount the cumulative contributions
         * @return this builder
         */
        public Builder cumulativeContributions(BigDecimal amount) {
            this.cumulativeContributions = amount;
            return this;
        }

        /**
         * Sets the cumulative withdrawals.
         *
         * @param amount the cumulative withdrawals
         * @return this builder
         */
        public Builder cumulativeWithdrawals(BigDecimal amount) {
            this.cumulativeWithdrawals = amount;
            return this;
        }

        /**
         * Sets the cumulative returns.
         *
         * @param amount the cumulative returns
         * @return this builder
         */
        public Builder cumulativeReturns(BigDecimal amount) {
            this.cumulativeReturns = amount;
            return this;
        }

        /**
         * Sets the cumulative taxes paid.
         *
         * @param amount the cumulative taxes paid
         * @return this builder
         */
        public Builder cumulativeTaxesPaid(BigDecimal amount) {
            this.cumulativeTaxesPaid = amount;
            return this;
        }

        /**
         * Sets the simulation phase.
         *
         * @param phase the simulation phase
         * @return this builder
         */
        public Builder phase(SimulationPhase phase) {
            this.phase = phase;
            return this;
        }

        /**
         * Sets the events triggered.
         *
         * @param events the events list
         * @return this builder
         */
        public Builder eventsTriggered(List<String> events) {
            this.eventsTriggered = events == null ? Collections.emptyList() : new ArrayList<>(events);
            return this;
        }

        /**
         * Builds the MonthlySnapshot.
         *
         * @return the constructed MonthlySnapshot
         */
        public MonthlySnapshot build() {
            return new MonthlySnapshot(
                    month,
                    accountFlows,
                    salaryIncome,
                    socialSecurityIncome,
                    pensionIncome,
                    otherIncome,
                    totalExpenses,
                    expensesByCategory,
                    taxes,
                    cumulativeContributions,
                    cumulativeWithdrawals,
                    cumulativeReturns,
                    cumulativeTaxesPaid,
                    phase,
                    eventsTriggered
            );
        }
    }
}
