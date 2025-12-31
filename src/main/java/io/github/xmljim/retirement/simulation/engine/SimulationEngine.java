package io.github.xmljim.retirement.simulation.engine;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.github.xmljim.retirement.domain.calculator.ContributionRouter;
import io.github.xmljim.retirement.domain.calculator.SpendingOrchestrator;
import io.github.xmljim.retirement.domain.enums.SimulationPhase;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.model.PersonProfile;
import io.github.xmljim.retirement.simulation.result.AccountMonthlyFlow;
import io.github.xmljim.retirement.simulation.result.MonthlySnapshot;
import io.github.xmljim.retirement.simulation.result.TimeSeries;
import io.github.xmljim.retirement.simulation.state.SimulationState;

/**
 * Core simulation engine that runs the retirement projection.
 *
 * <p>The SimulationEngine executes a month-by-month simulation from
 * the start date through the end date. Each month follows a 7-step
 * process:
 * <ol>
 *   <li><strong>Determine Phase</strong> - Identify simulation phase for each person</li>
 *   <li><strong>Process Income</strong> - Calculate monthly income sources</li>
 *   <li><strong>Process Events</strong> - Handle triggered events (retirement, death, etc.)</li>
 *   <li><strong>Calculate Expenses</strong> - Compute monthly expenses</li>
 *   <li><strong>Execute Financials</strong> - Process contributions or withdrawals</li>
 *   <li><strong>Apply Returns</strong> - Apply monthly investment returns</li>
 *   <li><strong>Record Snapshot</strong> - Capture month-end state</li>
 * </ol>
 *
 * <p>Usage:
 * <pre>{@code
 * SimulationEngine engine = SimulationEngine.builder()
 *     .spendingOrchestrator(orchestrator)
 *     .contributionRouter(router)
 *     .build();
 *
 * TimeSeries<MonthlySnapshot> results = engine.run(config);
 * }</pre>
 *
 * @see SimulationConfig
 * @see MonthlySnapshot
 * @see TimeSeries
 */
public class SimulationEngine {

    private static final int SCALE = 6;
    private static final MathContext MATH_CONTEXT = new MathContext(10, RoundingMode.HALF_UP);
    private static final int MONTHS_PER_YEAR = 12;

    private final SpendingOrchestrator spendingOrchestrator;
    private final ContributionRouter contributionRouter;

    private SimulationState state;

    /**
     * Creates a SimulationEngine with required components.
     *
     * @param spendingOrchestrator the orchestrator for withdrawal operations
     * @param contributionRouter the router for contribution operations
     */
    public SimulationEngine(
            SpendingOrchestrator spendingOrchestrator,
            ContributionRouter contributionRouter) {
        this.spendingOrchestrator = spendingOrchestrator;
        this.contributionRouter = contributionRouter;
    }

    /**
     * Runs the simulation with the given configuration.
     *
     * @param config the simulation configuration
     * @return the time series of monthly snapshots
     */
    public TimeSeries<MonthlySnapshot> run(SimulationConfig config) {
        MissingRequiredFieldException.requireNonNull(config, "config");

        // Initialize state from all portfolio accounts
        this.state = new SimulationState(config.getAllAccounts());

        TimeSeries.Builder<MonthlySnapshot> builder = TimeSeries.builder();

        // Generate list of months to simulate
        List<YearMonth> months = generateMonths(config.startMonth(), config.endMonth());

        for (YearMonth month : months) {
            MonthlySnapshot snapshot = processMonth(month, config);
            builder.add(snapshot);
            state.recordHistory(snapshot);
        }

        return builder.build();
    }

    /**
     * Processes a single month of the simulation.
     *
     * <p>Executes the 7-step monthly loop.
     *
     * @param month the month to process
     * @param config the simulation configuration
     * @return the monthly snapshot
     */
    private MonthlySnapshot processMonth(YearMonth month, SimulationConfig config) {
        // Step 1: Determine phase
        SimulationPhase phase = determinePhase(config.primaryPerson(), month);

        // Step 2: Process income
        BigDecimal income = processIncome(month, phase, config);

        // Step 3: Process events
        List<String> triggeredEvents = processEvents(month, config);

        // Step 4: Calculate expenses
        BigDecimal expenses = calculateExpenses(month, phase, config);

        // Step 5: Execute contributions OR withdrawals
        Map<UUID, AccountMonthlyFlow> flows = executeFinancials(
            phase, income, expenses, month, config);

        // Step 6: Apply monthly returns
        BigDecimal returnRate = calculateMonthlyReturn(config);
        applyMonthlyReturns(flows, returnRate);

        // Step 7: Record snapshot
        return buildSnapshot(month, flows, income, expenses, phase, triggeredEvents);
    }

    // ─── Step 1: Phase Determination ────────────────────────────────────────────

    /**
     * Determines the simulation phase for a person at the given month.
     *
     * @param person the person profile
     * @param month the current month
     * @return the simulation phase
     */
    SimulationPhase determinePhase(PersonProfile person, YearMonth month) {
        LocalDate monthStart = month.atDay(1);

        // Check survivor mode first
        if (state.isSurvivorMode()) {
            return SimulationPhase.SURVIVOR;
        }

        // Check if before retirement
        if (monthStart.isBefore(person.getRetirementDate())) {
            return SimulationPhase.ACCUMULATION;
        }

        // In distribution phase (after retirement)
        return SimulationPhase.DISTRIBUTION;
    }

    // ─── Step 2: Process Income ─────────────────────────────────────────────────

    /**
     * Processes income for the month.
     *
     * <p>Stub implementation - to be enhanced in later milestones.
     *
     * @param month the current month
     * @param phase the simulation phase
     * @param config the configuration
     * @return the total monthly income
     */
    BigDecimal processIncome(YearMonth month, SimulationPhase phase, SimulationConfig config) {
        // Stub: Return zero for now
        // Will be implemented to handle salary, Social Security, pensions, etc.
        return BigDecimal.ZERO;
    }

    // ─── Step 3: Process Events ─────────────────────────────────────────────────

    /**
     * Processes events triggered in the month.
     *
     * <p>Stub implementation - to be enhanced in later milestones.
     *
     * @param month the current month
     * @param config the configuration
     * @return list of triggered event names
     */
    List<String> processEvents(YearMonth month, SimulationConfig config) {
        // Stub: Return empty list for now
        // Will be implemented to handle retirement events, death events, etc.
        return Collections.emptyList();
    }

    // ─── Step 4: Calculate Expenses ─────────────────────────────────────────────

    /**
     * Calculates expenses for the month.
     *
     * <p>Stub implementation - to be enhanced in later milestones.
     *
     * @param month the current month
     * @param phase the simulation phase
     * @param config the configuration
     * @return the total monthly expenses
     */
    BigDecimal calculateExpenses(YearMonth month, SimulationPhase phase, SimulationConfig config) {
        // Stub: Return zero for now
        // Will be implemented to calculate category-based expenses with inflation
        return BigDecimal.ZERO;
    }

    // ─── Step 5: Execute Financials ─────────────────────────────────────────────

    /**
     * Executes financial operations for the month.
     *
     * <p>During accumulation: process contributions.
     * <p>During distribution/survivor: process withdrawals.
     *
     * <p>Stub implementation - to be enhanced in later milestones.
     *
     * @param phase the simulation phase
     * @param income the monthly income
     * @param expenses the monthly expenses
     * @param month the current month
     * @param config the configuration
     * @return map of account flows
     */
    Map<UUID, AccountMonthlyFlow> executeFinancials(
            SimulationPhase phase,
            BigDecimal income,
            BigDecimal expenses,
            YearMonth month,
            SimulationConfig config) {
        // Stub: Return empty map for now
        // Will be implemented to route contributions or execute withdrawals
        return Collections.emptyMap();
    }

    // ─── Step 6: Apply Returns ──────────────────────────────────────────────────

    /**
     * Calculates the monthly return rate from the annual rate.
     *
     * <p>Uses monthly compounding: (1 + annual)^(1/12) - 1
     *
     * @param config the configuration containing levers
     * @return the monthly return rate
     */
    BigDecimal calculateMonthlyReturn(SimulationConfig config) {
        BigDecimal annualReturn = config.levers().market().expectedReturn();

        // Monthly multiplier = (1 + annual)^(1/12)
        double annualDouble = annualReturn.doubleValue();
        double monthlyMultiplier = Math.pow(1.0 + annualDouble, 1.0 / MONTHS_PER_YEAR);
        double monthlyRate = monthlyMultiplier - 1.0;

        return BigDecimal.valueOf(monthlyRate).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Applies monthly returns to all accounts.
     *
     * @param flows the account flows for the month (to be updated with returns)
     * @param monthlyRate the monthly return rate
     */
    void applyMonthlyReturns(Map<UUID, AccountMonthlyFlow> flows, BigDecimal monthlyRate) {
        // Apply returns to all accounts in state
        state.applyReturns(monthlyRate);
    }

    // ─── Step 7: Build Snapshot ─────────────────────────────────────────────────

    /**
     * Builds the monthly snapshot from processed data.
     *
     * @param month the month
     * @param flows the account flows
     * @param income the monthly income
     * @param expenses the monthly expenses
     * @param phase the simulation phase
     * @param triggeredEvents list of triggered event names
     * @return the monthly snapshot
     */
    MonthlySnapshot buildSnapshot(
            YearMonth month,
            Map<UUID, AccountMonthlyFlow> flows,
            BigDecimal income,
            BigDecimal expenses,
            SimulationPhase phase,
            List<String> triggeredEvents) {

        return MonthlySnapshot.builder(month)
            .accountFlows(flows)
            .phase(phase)
            .build();
    }

    // ─── Utility Methods ────────────────────────────────────────────────────────

    /**
     * Generates a list of months from start to end (inclusive).
     *
     * @param start the start month
     * @param end the end month
     * @return list of months
     */
    List<YearMonth> generateMonths(YearMonth start, YearMonth end) {
        List<YearMonth> months = new ArrayList<>();
        YearMonth current = start;

        while (!current.isAfter(end)) {
            months.add(current);
            current = current.plusMonths(1);
        }

        return months;
    }

    /**
     * Returns the current simulation state.
     *
     * <p>Primarily for testing purposes.
     *
     * @return the state, or null if no simulation has been run
     */
    SimulationState getState() {
        return state;
    }

    /**
     * Creates a new builder for SimulationEngine.
     *
     * @return a builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for SimulationEngine.
     */
    public static class Builder {
        private SpendingOrchestrator spendingOrchestrator;
        private ContributionRouter contributionRouter;

        /**
         * Sets the spending orchestrator.
         *
         * @param orchestrator the orchestrator
         * @return this builder
         */
        public Builder spendingOrchestrator(SpendingOrchestrator orchestrator) {
            this.spendingOrchestrator = orchestrator;
            return this;
        }

        /**
         * Sets the contribution router.
         *
         * @param router the router
         * @return this builder
         */
        public Builder contributionRouter(ContributionRouter router) {
            this.contributionRouter = router;
            return this;
        }

        /**
         * Builds the SimulationEngine.
         *
         * @return a new SimulationEngine
         */
        public SimulationEngine build() {
            return new SimulationEngine(spendingOrchestrator, contributionRouter);
        }
    }
}
