package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.model.InvestmentAccount;
import io.github.xmljim.retirement.domain.value.AccountSnapshot;
import io.github.xmljim.retirement.domain.value.AssetAllocation;

/**
 * Stub implementation of {@link SimulationView} for testing M6 strategies.
 *
 * <p>This stub allows tests to configure both current portfolio state and
 * historical data without needing a full simulation engine. Use the
 * {@link Builder} to set up test scenarios.
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * // Simple setup with accounts
 * StubSimulationView view = StubSimulationView.builder()
 *     .addAccount(AccountSnapshot.from(traditional401k))
 *     .addAccount(AccountSnapshot.from(rothIra))
 *     .initialPortfolioBalance(new BigDecimal("1000000"))
 *     .build();
 *
 * // With historical data for dynamic strategies
 * StubSimulationView view = StubSimulationView.builder()
 *     .addAccount(AccountSnapshot.from(account))
 *     .priorYearSpending(new BigDecimal("48000"))
 *     .priorYearReturn(new BigDecimal("0.08"))
 *     .lastRatchetMonth(YearMonth.of(2023, 1))
 *     .build();
 * }</pre>
 *
 * @see SimulationView
 * @see io.github.xmljim.retirement.domain.value.SpendingContext
 */
public class StubSimulationView implements SimulationView {

    private final List<AccountSnapshot> accounts;
    private final BigDecimal initialPortfolioBalance;
    private final BigDecimal priorYearSpending;
    private final BigDecimal priorYearReturn;
    private final YearMonth lastRatchetMonth;
    private final BigDecimal cumulativeWithdrawals;
    private final BigDecimal highWaterMarkBalance;

    private StubSimulationView(Builder builder) {
        this.accounts = Collections.unmodifiableList(new ArrayList<>(builder.accounts));
        this.initialPortfolioBalance = builder.initialPortfolioBalance != null
                ? builder.initialPortfolioBalance
                : calculateTotalBalance();
        this.priorYearSpending = builder.priorYearSpending;
        this.priorYearReturn = builder.priorYearReturn;
        this.lastRatchetMonth = builder.lastRatchetMonth;
        this.cumulativeWithdrawals = builder.cumulativeWithdrawals;
        this.highWaterMarkBalance = builder.highWaterMarkBalance != null
                ? builder.highWaterMarkBalance
                : calculateTotalBalance();
    }

    private BigDecimal calculateTotalBalance() {
        return accounts.stream()
                .map(AccountSnapshot::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal getAccountBalance(UUID accountId) {
        String id = accountId.toString();
        return accounts.stream()
                .filter(a -> a.accountId().equals(id))
                .map(AccountSnapshot::balance)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public BigDecimal getTotalPortfolioBalance() {
        return calculateTotalBalance();
    }

    @Override
    public List<AccountSnapshot> getAccountSnapshots() {
        return accounts;
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
        return Optional.ofNullable(lastRatchetMonth);
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
     * Creates a new builder for StubSimulationView.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a minimal stub with a single account.
     *
     * <p>Convenience factory for simple test cases.
     *
     * @param account the account to include
     * @return a StubSimulationView with the single account
     */
    public static StubSimulationView withAccount(InvestmentAccount account) {
        return builder()
                .addAccount(AccountSnapshot.from(account))
                .build();
    }

    /**
     * Creates a minimal stub with the given accounts.
     *
     * @param accounts the accounts to include
     * @return a StubSimulationView with the accounts
     */
    public static StubSimulationView withAccounts(List<AccountSnapshot> accounts) {
        Builder builder = builder();
        accounts.forEach(builder::addAccount);
        return builder.build();
    }

    /**
     * Builder for creating StubSimulationView instances.
     */
    public static class Builder {
        private final List<AccountSnapshot> accounts = new ArrayList<>();
        private BigDecimal initialPortfolioBalance;
        private BigDecimal priorYearSpending = BigDecimal.ZERO;
        private BigDecimal priorYearReturn = BigDecimal.ZERO;
        private YearMonth lastRatchetMonth;
        private BigDecimal cumulativeWithdrawals = BigDecimal.ZERO;
        private BigDecimal highWaterMarkBalance;

        /**
         * Adds an account snapshot to the stub.
         *
         * @param account the account snapshot
         * @return this builder
         */
        public Builder addAccount(AccountSnapshot account) {
            this.accounts.add(account);
            return this;
        }

        /**
         * Adds an investment account to the stub (converts to snapshot).
         *
         * @param account the investment account
         * @return this builder
         */
        public Builder addAccount(InvestmentAccount account) {
            this.accounts.add(AccountSnapshot.from(account));
            return this;
        }

        /**
         * Adds multiple account snapshots to the stub.
         *
         * @param accounts the account snapshots
         * @return this builder
         */
        public Builder accounts(List<AccountSnapshot> accounts) {
            this.accounts.clear();
            this.accounts.addAll(accounts);
            return this;
        }

        /**
         * Sets the initial portfolio balance (at retirement start).
         *
         * <p>If not set, defaults to the sum of current account balances.
         *
         * @param balance the initial balance
         * @return this builder
         */
        public Builder initialPortfolioBalance(BigDecimal balance) {
            this.initialPortfolioBalance = balance;
            return this;
        }

        /**
         * Sets the prior year spending (for Guardrails strategies).
         *
         * @param spending the prior year spending
         * @return this builder
         */
        public Builder priorYearSpending(BigDecimal spending) {
            this.priorYearSpending = spending;
            return this;
        }

        /**
         * Sets the prior year portfolio return (for dynamic strategies).
         *
         * @param returnRate the return as a decimal (e.g., 0.08 for 8%)
         * @return this builder
         */
        public Builder priorYearReturn(BigDecimal returnRate) {
            this.priorYearReturn = returnRate;
            return this;
        }

        /**
         * Sets the month of the last spending ratchet (for Kitces strategy).
         *
         * @param month the month of last ratchet
         * @return this builder
         */
        public Builder lastRatchetMonth(YearMonth month) {
            this.lastRatchetMonth = month;
            return this;
        }

        /**
         * Sets the cumulative withdrawals since retirement start.
         *
         * @param withdrawals the total withdrawals
         * @return this builder
         */
        public Builder cumulativeWithdrawals(BigDecimal withdrawals) {
            this.cumulativeWithdrawals = withdrawals;
            return this;
        }

        /**
         * Sets the high water mark balance.
         *
         * <p>If not set, defaults to the sum of current account balances.
         *
         * @param balance the high water mark
         * @return this builder
         */
        public Builder highWaterMarkBalance(BigDecimal balance) {
            this.highWaterMarkBalance = balance;
            return this;
        }

        /**
         * Builds the StubSimulationView instance.
         *
         * @return a new StubSimulationView
         */
        public StubSimulationView build() {
            return new StubSimulationView(this);
        }
    }

    /**
     * Helper to create a test account snapshot.
     *
     * <p>Convenience method for tests that need to create accounts without
     * setting up full InvestmentAccount instances.
     *
     * @param name the account name
     * @param type the account type
     * @param balance the account balance
     * @return a new AccountSnapshot
     */
    public static AccountSnapshot createTestAccount(String name, AccountType type, BigDecimal balance) {
        return new AccountSnapshot(
                UUID.randomUUID().toString(),
                name,
                type,
                balance,
                type.getTaxTreatment(),
                type.isSubjectToRmd(),
                AssetAllocation.of(60, 35, 5)
        );
    }
}
