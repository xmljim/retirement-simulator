package io.github.xmljim.retirement.simulation.state;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import io.github.xmljim.retirement.domain.calculator.SimulationView;
import io.github.xmljim.retirement.domain.model.InvestmentAccount;
import io.github.xmljim.retirement.domain.value.AccountSnapshot;
import io.github.xmljim.retirement.domain.value.AccountWithdrawal;
import io.github.xmljim.retirement.domain.value.SpendingPlan;
import io.github.xmljim.retirement.simulation.result.AccountMonthlyFlow;
import io.github.xmljim.retirement.simulation.result.MonthlySnapshot;

/**
 * Mutable internal state for the simulation engine.
 *
 * <p>SimulationState owns all mutable state during a simulation run:
 * <ul>
 *   <li>Account balances via {@link AccountState}</li>
 *   <li>Simulation history via {@link MonthlySnapshot} list</li>
 *   <li>Event flags via {@link SimulationFlags}</li>
 *   <li>Cumulative metrics (withdrawals, high water mark, etc.)</li>
 * </ul>
 *
 * <p>The simulation engine creates one SimulationState per run. Strategies
 * receive read-only {@link SimulationView} snapshots via {@link #snapshot()}.
 *
 * <p>Thread safety: This class is NOT thread-safe. Each simulation run
 * should use its own SimulationState instance.
 *
 * @see SimulationView
 * @see SimulationViewSnapshot
 * @see AccountState
 */
public final class SimulationState {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final Map<String, AccountState> accounts;
    private final List<MonthlySnapshot> history;
    private BigDecimal initialPortfolioBalance;
    private BigDecimal highWaterMarkBalance;
    private BigDecimal cumulativeWithdrawals;
    private Optional<YearMonth> lastRatchetMonth;
    private SimulationFlags flags;

    /**
     * Creates a new SimulationState with the given accounts.
     *
     * @param investmentAccounts the accounts to track
     */
    public SimulationState(List<InvestmentAccount> investmentAccounts) {
        this.accounts = new HashMap<>();
        this.history = new ArrayList<>();
        this.cumulativeWithdrawals = BigDecimal.ZERO;
        this.lastRatchetMonth = Optional.empty();
        this.flags = SimulationFlags.initial();

        Optional.ofNullable(investmentAccounts)
            .ifPresent(list -> list.forEach(account ->
                accounts.put(account.getId(), new AccountState(account))));

        // Calculate initial balances
        this.initialPortfolioBalance = calculateTotalBalance();
        this.highWaterMarkBalance = initialPortfolioBalance;
    }

    // ─── Account Operations ─────────────────────────────────────────────────────

    /**
     * Returns the current balance of a specific account.
     *
     * @param accountId the account ID
     * @return the current balance, or ZERO if not found
     */
    public BigDecimal getAccountBalance(String accountId) {
        return Optional.ofNullable(accounts.get(accountId))
            .map(AccountState::getCurrentBalance)
            .orElse(BigDecimal.ZERO);
    }

    /**
     * Updates an account's balance directly.
     *
     * @param accountId  the account ID
     * @param newBalance the new balance
     */
    public void updateAccountBalance(String accountId, BigDecimal newBalance) {
        Optional.ofNullable(accounts.get(accountId))
            .ifPresent(state -> {
                state.setBalance(newBalance);
                updateHighWaterMark();
            });
    }

    /**
     * Applies a withdrawal to an account.
     *
     * @param accountId the account ID
     * @param amount    the withdrawal amount
     * @return the actual amount withdrawn
     */
    public BigDecimal withdrawFromAccount(String accountId, BigDecimal amount) {
        return Optional.ofNullable(accounts.get(accountId))
            .map(state -> {
                BigDecimal withdrawal = state.withdraw(amount);
                cumulativeWithdrawals = cumulativeWithdrawals.add(withdrawal);
                return withdrawal;
            })
            .orElse(BigDecimal.ZERO);
    }

    /**
     * Applies a deposit to an account.
     *
     * @param accountId the account ID
     * @param amount    the deposit amount
     */
    public void depositToAccount(String accountId, BigDecimal amount) {
        Optional.ofNullable(accounts.get(accountId))
            .ifPresent(state -> {
                state.deposit(amount);
                updateHighWaterMark();
            });
    }

    /**
     * Applies investment returns to all accounts.
     *
     * @param rate the return rate as a decimal
     * @return map of account ID to return amount
     */
    public Map<String, BigDecimal> applyReturns(BigDecimal rate) {
        Map<String, BigDecimal> returns = accounts.entrySet().stream()
            .collect(HashMap::new,
                (map, entry) -> map.put(entry.getKey(), entry.getValue().applyReturn(rate)),
                HashMap::putAll);
        updateHighWaterMark();
        return returns;
    }

    /**
     * Applies withdrawals from a spending plan.
     *
     * @param plan the spending plan with account withdrawals
     * @return map of account ID to AccountMonthlyFlow
     */
    public Map<UUID, AccountMonthlyFlow> applyWithdrawals(SpendingPlan plan) {
        Map<UUID, AccountMonthlyFlow> flows = new HashMap<>();

        plan.accountWithdrawals().forEach(withdrawal -> {
            String accountId = withdrawal.accountId();

            Optional.ofNullable(accounts.get(accountId))
                .ifPresent(state -> {
                    BigDecimal startingBalance = state.getCurrentBalance();
                    BigDecimal actualWithdrawn = state.withdraw(withdrawal.amount());
                    cumulativeWithdrawals = cumulativeWithdrawals.add(actualWithdrawn);

                    UUID uuid = UUID.fromString(accountId);
                    flows.put(uuid, AccountMonthlyFlow.builder(uuid, state.getAccountName())
                        .startingBalance(startingBalance)
                        .withdrawals(actualWithdrawn)
                        .build());
                });
        });

        return flows;
    }

    // ─── Balance Queries ────────────────────────────────────────────────────────

    /**
     * Calculates the total portfolio balance across all accounts.
     *
     * @return the sum of all account balances
     */
    public BigDecimal calculateTotalBalance() {
        return accounts.values().stream()
            .map(AccountState::getCurrentBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(SCALE, ROUNDING);
    }

    /**
     * Returns the initial portfolio balance at simulation start.
     *
     * @return the initial balance
     */
    public BigDecimal getInitialPortfolioBalance() {
        return initialPortfolioBalance;
    }

    /**
     * Returns the highest portfolio balance achieved.
     *
     * @return the high water mark balance
     */
    public BigDecimal getHighWaterMarkBalance() {
        return highWaterMarkBalance;
    }

    /**
     * Returns total cumulative withdrawals.
     *
     * @return cumulative withdrawals since start
     */
    public BigDecimal getCumulativeWithdrawals() {
        return cumulativeWithdrawals;
    }

    // ─── History Operations ─────────────────────────────────────────────────────

    /**
     * Records a monthly snapshot to history.
     *
     * @param snapshot the snapshot to record
     */
    public void recordHistory(MonthlySnapshot snapshot) {
        if (snapshot != null) {
            history.add(snapshot);
        }
    }

    /**
     * Returns the simulation history.
     *
     * @return unmodifiable list of monthly snapshots
     */
    public List<MonthlySnapshot> getHistory() {
        return Collections.unmodifiableList(history);
    }

    /**
     * Returns total spending in the prior calendar year.
     *
     * @param currentMonth the current simulation month
     * @return prior year spending, or ZERO if first year
     */
    public BigDecimal getPriorYearSpending(YearMonth currentMonth) {
        if (history.isEmpty() || currentMonth == null) {
            return BigDecimal.ZERO;
        }

        int priorYear = currentMonth.getYear() - 1;
        return history.stream()
            .filter(s -> s.year() == priorYear)
            .map(MonthlySnapshot::totalWithdrawals)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(SCALE, ROUNDING);
    }

    /**
     * Returns the portfolio return for the prior calendar year.
     *
     * @param currentMonth the current simulation month
     * @return prior year return as decimal, or ZERO if first year
     */
    public BigDecimal getPriorYearReturn(YearMonth currentMonth) {
        if (history.isEmpty() || currentMonth == null) {
            return BigDecimal.ZERO;
        }

        int priorYear = currentMonth.getYear() - 1;
        List<MonthlySnapshot> priorYearSnapshots = history.stream()
            .filter(s -> s.year() == priorYear)
            .toList();

        if (priorYearSnapshots.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalReturns = priorYearSnapshots.stream()
            .map(MonthlySnapshot::totalReturns)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get starting balance for prior year (end of year before)
        BigDecimal startBalance = getYearEndBalance(priorYear - 1);
        if (startBalance.compareTo(BigDecimal.ZERO) == 0) {
            startBalance = initialPortfolioBalance;
        }

        if (startBalance.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return totalReturns.divide(startBalance, 6, ROUNDING);
    }

    private BigDecimal getYearEndBalance(int year) {
        return history.stream()
            .filter(s -> s.year() == year && s.monthValue() == 12)
            .map(MonthlySnapshot::totalPortfolioBalance)
            .findFirst()
            .orElse(BigDecimal.ZERO);
    }

    // ─── Ratchet Tracking ───────────────────────────────────────────────────────

    /**
     * Returns the month when the last spending ratchet occurred.
     *
     * @return the last ratchet month, or empty if none
     */
    public Optional<YearMonth> getLastRatchetMonth() {
        return lastRatchetMonth;
    }

    /**
     * Records a spending ratchet event.
     *
     * @param month the month when ratchet occurred
     */
    public void recordRatchet(YearMonth month) {
        this.lastRatchetMonth = Optional.ofNullable(month);
    }

    // ─── Flags ──────────────────────────────────────────────────────────────────

    /**
     * Returns the current simulation flags.
     *
     * @return the flags
     */
    public SimulationFlags getFlags() {
        return flags;
    }

    /**
     * Updates the simulation flags.
     *
     * @param flags the new flags
     */
    public void setFlags(SimulationFlags flags) {
        this.flags = flags != null ? flags : SimulationFlags.initial();
    }

    /**
     * Returns whether survivor mode is active.
     *
     * @return true if in survivor mode
     */
    public boolean isSurvivorMode() {
        return flags.survivorMode();
    }

    /**
     * Sets survivor mode.
     *
     * @param survivorMode the new survivor mode state
     */
    public void setSurvivorMode(boolean survivorMode) {
        this.flags = flags.withSurvivorMode(survivorMode);
    }

    // ─── Snapshot Creation ──────────────────────────────────────────────────────

    /**
     * Creates an immutable snapshot for strategy consumption.
     *
     * @param currentMonth the current simulation month (for historical queries)
     * @return a SimulationView snapshot
     */
    public SimulationView snapshot(YearMonth currentMonth) {
        List<AccountSnapshot> snapshots = accounts.values().stream()
            .map(AccountState::toSnapshot)
            .toList();

        return SimulationViewSnapshot.builder()
            .accountSnapshots(snapshots)
            .totalPortfolioBalance(calculateTotalBalance())
            .initialPortfolioBalance(initialPortfolioBalance)
            .priorYearSpending(getPriorYearSpending(currentMonth))
            .priorYearReturn(getPriorYearReturn(currentMonth))
            .lastRatchetMonth(lastRatchetMonth)
            .cumulativeWithdrawals(cumulativeWithdrawals)
            .highWaterMarkBalance(highWaterMarkBalance)
            .build();
    }

    /**
     * Creates an immutable snapshot without month context.
     *
     * @return a SimulationView snapshot
     */
    public SimulationView snapshot() {
        return snapshot(null);
    }

    // ─── Internal Helpers ───────────────────────────────────────────────────────

    private void updateHighWaterMark() {
        BigDecimal current = calculateTotalBalance();
        if (current.compareTo(highWaterMarkBalance) > 0) {
            highWaterMarkBalance = current;
        }
    }

    /**
     * Returns the number of accounts.
     *
     * @return account count
     */
    public int getAccountCount() {
        return accounts.size();
    }

    /**
     * Returns account snapshots for all accounts.
     *
     * @return list of account snapshots
     */
    public List<AccountSnapshot> getAccountSnapshots() {
        return accounts.values().stream()
            .map(AccountState::toSnapshot)
            .toList();
    }
}
