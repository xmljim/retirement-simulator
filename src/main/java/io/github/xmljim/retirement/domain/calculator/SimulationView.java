package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.xmljim.retirement.domain.value.AccountSnapshot;

/**
 * Read-only view of simulation state for strategy calculations.
 *
 * <p>This interface is the <b>contract between M6 (Distribution Strategies) and M7
 * (Simulation Engine)</b>. Strategies consume this interface to access current
 * portfolio state and historical data without coupling to simulation internals.
 *
 * <p>The simulation engine implements this interface, backed by its internal
 * account balances and TimeSeries history. Strategies can query what they need
 * without knowing how the data is stored or managed.
 *
 * <h2>Design Rationale</h2>
 *
 * <p>By separating state ownership (simulation) from state consumption (strategies),
 * we achieve:
 * <ul>
 *   <li><b>Clean separation of concerns</b> - Simulation owns state, strategies are pure calculators</li>
 *   <li><b>Simulation mode agnosticism</b> - Same strategy code works for deterministic,
 *       Monte Carlo, and historical backtesting modes</li>
 *   <li><b>Testability</b> - Strategies can be unit tested with mock/stub implementations</li>
 *   <li><b>Monte Carlo support</b> - Each run has fresh history; no strategy state to snapshot/restore</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <p>Strategies access this view through {@code SpendingContext.simulation()}:
 * <pre>{@code
 * public SpendingPlan calculateWithdrawal(SpendingContext context) {
 *     BigDecimal currentBalance = context.simulation().getTotalPortfolioBalance();
 *     BigDecimal priorSpending = context.simulation().getPriorYearSpending();
 *     // ... calculate withdrawal
 * }
 * }</pre>
 *
 * @see SpendingStrategy
 * @see io.github.xmljim.retirement.domain.value.SpendingContext
 * @see AccountSnapshot
 */
public interface SimulationView {

    // ─────────────────────────────────────────────────────────────────────────
    // Current State (as of this simulation step)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the current balance of a specific account.
     *
     * @param accountId the account's unique identifier
     * @return the current balance, or {@link BigDecimal#ZERO} if account not found
     */
    BigDecimal getAccountBalance(UUID accountId);

    /**
     * Returns the total portfolio balance across all accounts.
     *
     * @return the sum of all account balances
     */
    BigDecimal getTotalPortfolioBalance();

    /**
     * Returns read-only snapshots of all accounts with current balances.
     *
     * <p>This is the primary method for strategies and sequencers to access
     * account information. The snapshots are immutable and contain all info
     * needed for withdrawal calculations.
     *
     * @return unmodifiable list of account snapshots
     */
    List<AccountSnapshot> getAccountSnapshots();

    /**
     * Returns the portfolio balance at retirement start.
     *
     * <p>Used by Static (4%) strategy to calculate base withdrawal amount.
     *
     * @return the initial portfolio balance
     */
    BigDecimal getInitialPortfolioBalance();

    // ─────────────────────────────────────────────────────────────────────────
    // Historical Queries (for dynamic strategies)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns total spending (withdrawals) in the prior calendar year.
     *
     * <p>Used by Guardrails strategies to calculate spending adjustments.
     *
     * @return prior year spending, or {@link BigDecimal#ZERO} if first year
     */
    BigDecimal getPriorYearSpending();

    /**
     * Returns the portfolio return percentage for the prior calendar year.
     *
     * <p>Used by dynamic strategies to determine if spending adjustments
     * are warranted.
     *
     * @return prior year return as a decimal (e.g., 0.08 for 8%), or
     *         {@link BigDecimal#ZERO} if first year
     */
    BigDecimal getPriorYearReturn();

    /**
     * Finds the month when a spending ratchet last occurred.
     *
     * <p>Used by Kitces Ratcheting strategy to enforce minimum years between
     * spending increases.
     *
     * @return the month of last ratchet, or empty if no ratchet has occurred
     */
    Optional<YearMonth> getLastRatchetMonth();

    /**
     * Returns cumulative withdrawals since retirement start.
     *
     * @return total withdrawals across all periods
     */
    BigDecimal getCumulativeWithdrawals();

    /**
     * Returns the highest portfolio balance achieved (high water mark).
     *
     * <p>Used for drawdown calculations and some dynamic strategies.
     *
     * @return the maximum portfolio balance observed
     */
    BigDecimal getHighWaterMarkBalance();
}
