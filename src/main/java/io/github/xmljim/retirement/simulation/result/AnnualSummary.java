package io.github.xmljim.retirement.simulation.result;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Aggregated summary of simulation results for a single calendar year.
 *
 * <p>AnnualSummary is computed from the monthly snapshots within a year,
 * providing key metrics for year-over-year analysis:
 * <ul>
 *   <li>Portfolio balance change (start to end)</li>
 *   <li>Total contributions and withdrawals</li>
 *   <li>Total income and expenses</li>
 *   <li>Investment returns (absolute and percentage)</li>
 *   <li>Significant events that occurred during the year</li>
 * </ul>
 *
 * @param year the calendar year
 * @param startingBalance portfolio balance at start of year (January)
 * @param endingBalance portfolio balance at end of year (December)
 * @param totalContributions sum of all contributions during the year
 * @param totalWithdrawals sum of all withdrawals during the year
 * @param totalIncome sum of all income during the year
 * @param totalExpenses sum of all expenses during the year
 * @param totalTaxesPaid sum of all taxes paid during the year
 * @param annualReturn absolute investment return for the year
 * @param annualReturnPercent return as percentage of average balance
 * @param significantEvents list of notable events during the year
 *
 * @see TimeSeries
 * @see MonthlySnapshot
 */
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "List is made unmodifiable in compact constructor"
)
public record AnnualSummary(
        int year,
        BigDecimal startingBalance,
        BigDecimal endingBalance,
        BigDecimal totalContributions,
        BigDecimal totalWithdrawals,
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal totalTaxesPaid,
        BigDecimal annualReturn,
        BigDecimal annualReturnPercent,
        List<String> significantEvents
) {

    private static final int SCALE = 2;
    private static final int PERCENT_SCALE = 4;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /**
     * Compact constructor with validation and defensive copying.
     */
    public AnnualSummary {
        if (year < 1900 || year > 2200) {
            throw new IllegalArgumentException("year must be between 1900 and 2200");
        }
        if (startingBalance == null) {
            startingBalance = BigDecimal.ZERO;
        }
        if (endingBalance == null) {
            endingBalance = BigDecimal.ZERO;
        }
        if (totalContributions == null) {
            totalContributions = BigDecimal.ZERO;
        }
        if (totalWithdrawals == null) {
            totalWithdrawals = BigDecimal.ZERO;
        }
        if (totalIncome == null) {
            totalIncome = BigDecimal.ZERO;
        }
        if (totalExpenses == null) {
            totalExpenses = BigDecimal.ZERO;
        }
        if (totalTaxesPaid == null) {
            totalTaxesPaid = BigDecimal.ZERO;
        }
        if (annualReturn == null) {
            annualReturn = BigDecimal.ZERO;
        }
        if (annualReturnPercent == null) {
            annualReturnPercent = BigDecimal.ZERO;
        }
        significantEvents = significantEvents == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(significantEvents);
    }

    // ─── Computed Metrics ──────────────────────────────────────────────────────

    /**
     * Calculates the net change in portfolio balance.
     *
     * @return ending balance minus starting balance
     */
    public BigDecimal netBalanceChange() {
        return endingBalance.subtract(startingBalance).setScale(SCALE, ROUNDING);
    }

    /**
     * Calculates the net cash flow (contributions minus withdrawals).
     *
     * @return total contributions minus total withdrawals
     */
    public BigDecimal netContributions() {
        return totalContributions.subtract(totalWithdrawals).setScale(SCALE, ROUNDING);
    }

    /**
     * Calculates the savings rate (income minus expenses).
     *
     * @return total income minus total expenses
     */
    public BigDecimal netSavings() {
        return totalIncome.subtract(totalExpenses).setScale(SCALE, ROUNDING);
    }

    /**
     * Calculates the effective tax rate for the year.
     *
     * @return taxes paid divided by total income, or ZERO if no income
     */
    public BigDecimal effectiveTaxRate() {
        if (totalIncome.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalTaxesPaid
                .divide(totalIncome, PERCENT_SCALE, ROUNDING);
    }

    // ─── Status Checks ─────────────────────────────────────────────────────────

    /**
     * Indicates whether any significant events occurred this year.
     *
     * @return true if events list is not empty
     */
    public boolean hadSignificantEvents() {
        return !significantEvents.isEmpty();
    }

    /**
     * Indicates whether the portfolio grew this year.
     *
     * @return true if ending balance exceeds starting balance
     */
    public boolean hadGrowth() {
        return endingBalance.compareTo(startingBalance) > 0;
    }

    /**
     * Indicates whether there was net accumulation (contributions > withdrawals).
     *
     * @return true if contributions exceed withdrawals
     */
    public boolean isAccumulating() {
        return totalContributions.compareTo(totalWithdrawals) > 0;
    }

    /**
     * Indicates whether there was net distribution (withdrawals > contributions).
     *
     * @return true if withdrawals exceed contributions
     */
    public boolean isDistributing() {
        return totalWithdrawals.compareTo(totalContributions) > 0;
    }

    /**
     * Creates a builder for constructing AnnualSummary instances.
     *
     * @param year the calendar year
     * @return a new builder
     */
    public static Builder builder(int year) {
        return new Builder(year);
    }

    /**
     * Builder for AnnualSummary.
     */
    public static final class Builder {
        private final int year;
        private BigDecimal startingBalance = BigDecimal.ZERO;
        private BigDecimal endingBalance = BigDecimal.ZERO;
        private BigDecimal totalContributions = BigDecimal.ZERO;
        private BigDecimal totalWithdrawals = BigDecimal.ZERO;
        private BigDecimal totalIncome = BigDecimal.ZERO;
        private BigDecimal totalExpenses = BigDecimal.ZERO;
        private BigDecimal totalTaxesPaid = BigDecimal.ZERO;
        private BigDecimal annualReturn = BigDecimal.ZERO;
        private BigDecimal annualReturnPercent = BigDecimal.ZERO;
        private List<String> significantEvents = Collections.emptyList();

        private Builder(int year) {
            this.year = year;
        }

        /**
         * Sets the starting balance.
         *
         * @param balance the starting balance
         * @return this builder
         */
        public Builder startingBalance(BigDecimal balance) {
            this.startingBalance = balance;
            return this;
        }

        /**
         * Sets the ending balance.
         *
         * @param balance the ending balance
         * @return this builder
         */
        public Builder endingBalance(BigDecimal balance) {
            this.endingBalance = balance;
            return this;
        }

        /**
         * Sets the total contributions.
         *
         * @param amount the total contributions
         * @return this builder
         */
        public Builder totalContributions(BigDecimal amount) {
            this.totalContributions = amount;
            return this;
        }

        /**
         * Sets the total withdrawals.
         *
         * @param amount the total withdrawals
         * @return this builder
         */
        public Builder totalWithdrawals(BigDecimal amount) {
            this.totalWithdrawals = amount;
            return this;
        }

        /**
         * Sets the total income.
         *
         * @param amount the total income
         * @return this builder
         */
        public Builder totalIncome(BigDecimal amount) {
            this.totalIncome = amount;
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
         * Sets the total taxes paid.
         *
         * @param amount the total taxes paid
         * @return this builder
         */
        public Builder totalTaxesPaid(BigDecimal amount) {
            this.totalTaxesPaid = amount;
            return this;
        }

        /**
         * Sets the annual return.
         *
         * @param amount the annual return
         * @return this builder
         */
        public Builder annualReturn(BigDecimal amount) {
            this.annualReturn = amount;
            return this;
        }

        /**
         * Sets the annual return percentage.
         *
         * @param percent the return percentage as decimal
         * @return this builder
         */
        public Builder annualReturnPercent(BigDecimal percent) {
            this.annualReturnPercent = percent;
            return this;
        }

        /**
         * Sets the significant events.
         *
         * @param events the list of events
         * @return this builder
         */
        public Builder significantEvents(List<String> events) {
            this.significantEvents = events == null ? Collections.emptyList() : new ArrayList<>(events);
            return this;
        }

        /**
         * Builds the AnnualSummary.
         *
         * @return the constructed AnnualSummary
         */
        public AnnualSummary build() {
            return new AnnualSummary(
                    year,
                    startingBalance,
                    endingBalance,
                    totalContributions,
                    totalWithdrawals,
                    totalIncome,
                    totalExpenses,
                    totalTaxesPaid,
                    annualReturn,
                    annualReturnPercent,
                    significantEvents
            );
        }
    }
}
