package io.github.xmljim.retirement.simulation.state;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.xmljim.retirement.domain.calculator.SimulationView;
import io.github.xmljim.retirement.domain.value.AccountSnapshot;

/**
 * Immutable implementation of SimulationView.
 *
 * <p>SimulationViewSnapshot provides a read-only view of simulation state
 * at a point in time. This snapshot is created by {@link SimulationState#snapshot()}
 * and passed to spending strategies for calculations.
 *
 * <p>Because this is immutable, it is safe to pass between threads and
 * can be cached or stored for later analysis.
 *
 * <h2>Design</h2>
 *
 * <p>This record implements the {@link SimulationView} interface defined in M6.
 * Strategies consume SimulationView; they are unaware of whether they receive
 * a snapshot or direct access to state. This separation enables:
 * <ul>
 *   <li>Monte Carlo simulations where state must be snapshotted/restored</li>
 *   <li>Unit testing with mock/stub implementations</li>
 *   <li>Historical backtesting with different data sources</li>
 * </ul>
 *
 * @param accountSnapshots immutable list of account snapshots
 * @param totalPortfolioBalance sum of all account balances
 * @param initialPortfolioBalance portfolio balance at retirement start
 * @param priorYearSpending total spending in the prior calendar year
 * @param priorYearReturn portfolio return percentage for prior year
 * @param lastRatchetMonth month when last spending ratchet occurred
 * @param cumulativeWithdrawals total withdrawals since retirement
 * @param highWaterMarkBalance highest portfolio balance achieved
 *
 * @see SimulationView
 * @see SimulationState
 */
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "List is made unmodifiable in compact constructor"
)
public record SimulationViewSnapshot(
        List<AccountSnapshot> accountSnapshots,
        BigDecimal totalPortfolioBalance,
        BigDecimal initialPortfolioBalance,
        BigDecimal priorYearSpending,
        BigDecimal priorYearReturn,
        Optional<YearMonth> lastRatchetMonth,
        BigDecimal cumulativeWithdrawals,
        BigDecimal highWaterMarkBalance
) implements SimulationView {

    /**
     * Compact constructor with validation and defensive copying.
     */
    public SimulationViewSnapshot {
        accountSnapshots = accountSnapshots == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(accountSnapshots);
        totalPortfolioBalance = totalPortfolioBalance != null
                ? totalPortfolioBalance
                : BigDecimal.ZERO;
        initialPortfolioBalance = initialPortfolioBalance != null
                ? initialPortfolioBalance
                : BigDecimal.ZERO;
        priorYearSpending = priorYearSpending != null
                ? priorYearSpending
                : BigDecimal.ZERO;
        priorYearReturn = priorYearReturn != null
                ? priorYearReturn
                : BigDecimal.ZERO;
        lastRatchetMonth = lastRatchetMonth != null
                ? lastRatchetMonth
                : Optional.empty();
        cumulativeWithdrawals = cumulativeWithdrawals != null
                ? cumulativeWithdrawals
                : BigDecimal.ZERO;
        highWaterMarkBalance = highWaterMarkBalance != null
                ? highWaterMarkBalance
                : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getAccountBalance(UUID accountId) {
        if (accountId == null) {
            return BigDecimal.ZERO;
        }
        String id = accountId.toString();
        return accountSnapshots.stream()
                .filter(a -> a.accountId().equals(id))
                .map(AccountSnapshot::balance)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public BigDecimal getTotalPortfolioBalance() {
        return totalPortfolioBalance;
    }

    @Override
    public List<AccountSnapshot> getAccountSnapshots() {
        return accountSnapshots;
    }

    @Override
    public BigDecimal getInitialPortfolioBalance() {
        return initialPortfolioBalance;
    }

    @Override
    public BigDecimal getPriorYearSpending() {
        return priorYearSpending;
    }

    @Override
    public BigDecimal getPriorYearReturn() {
        return priorYearReturn;
    }

    @Override
    public Optional<YearMonth> getLastRatchetMonth() {
        return lastRatchetMonth;
    }

    @Override
    public BigDecimal getCumulativeWithdrawals() {
        return cumulativeWithdrawals;
    }

    @Override
    public BigDecimal getHighWaterMarkBalance() {
        return highWaterMarkBalance;
    }

    /**
     * Creates a builder for SimulationViewSnapshot.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing SimulationViewSnapshot instances.
     */
    public static class Builder {
        private List<AccountSnapshot> accountSnapshots = Collections.emptyList();
        private BigDecimal totalPortfolioBalance = BigDecimal.ZERO;
        private BigDecimal initialPortfolioBalance = BigDecimal.ZERO;
        private BigDecimal priorYearSpending = BigDecimal.ZERO;
        private BigDecimal priorYearReturn = BigDecimal.ZERO;
        private Optional<YearMonth> lastRatchetMonth = Optional.empty();
        private BigDecimal cumulativeWithdrawals = BigDecimal.ZERO;
        private BigDecimal highWaterMarkBalance = BigDecimal.ZERO;

        /**
         * Sets the account snapshots.
         *
         * @param snapshots the account snapshots
         * @return this builder
         */
        public Builder accountSnapshots(List<AccountSnapshot> snapshots) {
            this.accountSnapshots = snapshots;
            return this;
        }

        /**
         * Sets the total portfolio balance.
         *
         * @param balance the total balance
         * @return this builder
         */
        public Builder totalPortfolioBalance(BigDecimal balance) {
            this.totalPortfolioBalance = balance;
            return this;
        }

        /**
         * Sets the initial portfolio balance.
         *
         * @param balance the initial balance
         * @return this builder
         */
        public Builder initialPortfolioBalance(BigDecimal balance) {
            this.initialPortfolioBalance = balance;
            return this;
        }

        /**
         * Sets the prior year spending.
         *
         * @param spending the prior year spending
         * @return this builder
         */
        public Builder priorYearSpending(BigDecimal spending) {
            this.priorYearSpending = spending;
            return this;
        }

        /**
         * Sets the prior year return.
         *
         * @param returnRate the prior year return
         * @return this builder
         */
        public Builder priorYearReturn(BigDecimal returnRate) {
            this.priorYearReturn = returnRate;
            return this;
        }

        /**
         * Sets the last ratchet month.
         *
         * @param month the last ratchet month
         * @return this builder
         */
        public Builder lastRatchetMonth(YearMonth month) {
            this.lastRatchetMonth = Optional.ofNullable(month);
            return this;
        }

        /**
         * Sets the last ratchet month as optional.
         *
         * @param month the last ratchet month optional
         * @return this builder
         */
        public Builder lastRatchetMonth(Optional<YearMonth> month) {
            this.lastRatchetMonth = month != null ? month : Optional.empty();
            return this;
        }

        /**
         * Sets the cumulative withdrawals.
         *
         * @param withdrawals the cumulative withdrawals
         * @return this builder
         */
        public Builder cumulativeWithdrawals(BigDecimal withdrawals) {
            this.cumulativeWithdrawals = withdrawals;
            return this;
        }

        /**
         * Sets the high water mark balance.
         *
         * @param balance the high water mark
         * @return this builder
         */
        public Builder highWaterMarkBalance(BigDecimal balance) {
            this.highWaterMarkBalance = balance;
            return this;
        }

        /**
         * Builds the SimulationViewSnapshot.
         *
         * @return a new SimulationViewSnapshot instance
         */
        public SimulationViewSnapshot build() {
            return new SimulationViewSnapshot(
                    accountSnapshots,
                    totalPortfolioBalance,
                    initialPortfolioBalance,
                    priorYearSpending,
                    priorYearReturn,
                    lastRatchetMonth,
                    cumulativeWithdrawals,
                    highWaterMarkBalance
            );
        }
    }
}
