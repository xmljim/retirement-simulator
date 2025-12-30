package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.xmljim.retirement.domain.calculator.SimulationView;
import io.github.xmljim.retirement.domain.enums.FilingStatus;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;

/**
 * Context for spending strategy calculations.
 *
 * <p>Provides all information needed by a {@code SpendingStrategy} to calculate
 * the appropriate withdrawal amount. This includes:
 * <ul>
 *   <li>Simulation state access via {@link SimulationView}</li>
 *   <li>Current period inputs (date, expenses, income)</li>
 *   <li>Person/scenario context (age, retirement date)</li>
 *   <li>Tax context for withdrawal sequencing</li>
 * </ul>
 *
 * <p>This record is immutable. Use the {@link Builder} to create instances.
 *
 * <h2>Simulation Integration</h2>
 *
 * <p>Historical data (prior year spending, returns, etc.) and current portfolio
 * balances are accessed via the {@link SimulationView} interface. This enables:
 * <ul>
 *   <li>Clean separation between simulation state and strategy calculations</li>
 *   <li>Simulation mode agnosticism (deterministic, Monte Carlo, historical)</li>
 *   <li>Easy testing with {@code StubSimulationView}</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * SpendingContext context = SpendingContext.builder()
 *     .simulation(simulationView)
 *     .date(LocalDate.of(2025, 6, 1))
 *     .totalExpenses(new BigDecimal("6000"))
 *     .otherIncome(new BigDecimal("3000"))
 *     .age(68)
 *     .birthYear(1957)
 *     .retirementStartDate(LocalDate.of(2022, 1, 1))
 *     .filingStatus(FilingStatus.MARRIED_FILING_JOINTLY)
 *     .build();
 *
 * // Access simulation state
 * BigDecimal balance = context.simulation().getTotalPortfolioBalance();
 * BigDecimal priorSpending = context.simulation().getPriorYearSpending();
 * }</pre>
 *
 * @param simulation read-only view of simulation state (balances, history)
 * @param date the date for this calculation
 * @param totalExpenses total monthly expenses
 * @param otherIncome income from non-portfolio sources (SS, pension, etc.)
 * @param age the person's current age
 * @param birthYear the person's birth year (for RMD calculations)
 * @param retirementStartDate the date retirement started
 * @param currentTaxableIncome taxable income for the year so far
 * @param filingStatus tax filing status
 * @param strategyParams additional strategy-specific parameters
 * @see SimulationView
 * @see io.github.xmljim.retirement.domain.calculator.SpendingStrategy
 */
@SuppressFBWarnings(value = "EI_EXPOSE_REP",
        justification = "Record makes defensive copy in compact constructor; map is unmodifiable")
public record SpendingContext(
        SimulationView simulation,
        LocalDate date,
        BigDecimal totalExpenses,
        BigDecimal otherIncome,
        int age,
        int birthYear,
        LocalDate retirementStartDate,
        BigDecimal currentTaxableIncome,
        FilingStatus filingStatus,
        Map<String, Object> strategyParams
) {

    /**
     * Compact constructor with validation and defensive copies.
     */
    public SpendingContext {
        MissingRequiredFieldException.requireNonNull(simulation, "simulation");
        MissingRequiredFieldException.requireNonNull(date, "date");
        MissingRequiredFieldException.requireNonNull(retirementStartDate, "retirementStartDate");
        totalExpenses = totalExpenses != null ? totalExpenses : BigDecimal.ZERO;
        otherIncome = otherIncome != null ? otherIncome : BigDecimal.ZERO;
        currentTaxableIncome = currentTaxableIncome != null
                ? currentTaxableIncome
                : BigDecimal.ZERO;
        strategyParams = strategyParams != null
                ? Collections.unmodifiableMap(strategyParams)
                : Collections.emptyMap();
    }

    /**
     * Returns the number of months since retirement started.
     *
     * <p>This is computed from the {@code retirementStartDate} and {@code date} fields,
     * providing accurate monthly granularity for inflation compounding calculations.
     *
     * @return the number of complete months since retirement started
     */
    public long monthsInRetirement() {
        return ChronoUnit.MONTHS.between(retirementStartDate, date);
    }

    /**
     * Returns the number of years since retirement started.
     *
     * <p>This is a convenience method that returns {@code monthsInRetirement() / 12}.
     * For more precise calculations, use {@link #monthsInRetirement()}.
     *
     * @return the number of complete years since retirement started
     */
    public int yearsInRetirement() {
        return (int) (monthsInRetirement() / 12);
    }

    /**
     * Returns the income gap (expenses minus other income).
     *
     * <p>This represents the amount that needs to be withdrawn from
     * the portfolio to cover expenses.
     *
     * @return the income gap, or zero if income covers expenses
     */
    public BigDecimal incomeGap() {
        return totalExpenses.subtract(otherIncome).max(BigDecimal.ZERO);
    }

    /**
     * Returns the current portfolio balance.
     *
     * <p>Delegates to {@code simulation().getTotalPortfolioBalance()}.
     *
     * @return the total portfolio balance
     */
    public BigDecimal currentPortfolioBalance() {
        return simulation.getTotalPortfolioBalance();
    }

    /**
     * Returns the initial portfolio balance at retirement start.
     *
     * <p>Delegates to {@code simulation().getInitialPortfolioBalance()}.
     *
     * @return the initial portfolio balance
     */
    public BigDecimal initialPortfolioBalance() {
        return simulation.getInitialPortfolioBalance();
    }

    /**
     * Returns the current withdrawal rate based on portfolio balance.
     *
     * <p>Formula: priorYearSpending / currentPortfolioBalance
     *
     * <p>Prior year spending is obtained from {@code simulation().getPriorYearSpending()}.
     *
     * @return the current withdrawal rate as a decimal
     */
    public BigDecimal currentWithdrawalRate() {
        BigDecimal balance = currentPortfolioBalance();
        if (balance.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal priorSpending = simulation.getPriorYearSpending();
        return priorSpending.divide(balance, 4, RoundingMode.HALF_UP);
    }

    /**
     * Returns a strategy parameter value.
     *
     * @param key the parameter key
     * @param defaultValue the default if not found
     * @param <T> the value type
     * @return the parameter value or default
     */
    @SuppressWarnings("unchecked")
    public <T> T getStrategyParam(String key, T defaultValue) {
        Object value = strategyParams.get(key);
        if (value == null) {
            return defaultValue;
        }
        return (T) value;
    }

    /**
     * Creates a builder for SpendingContext.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating SpendingContext instances.
     */
    public static class Builder {
        private SimulationView simulation;
        private LocalDate date;
        private BigDecimal totalExpenses = BigDecimal.ZERO;
        private BigDecimal otherIncome = BigDecimal.ZERO;
        private int age;
        private int birthYear;
        private LocalDate retirementStartDate;
        private BigDecimal currentTaxableIncome = BigDecimal.ZERO;
        private FilingStatus filingStatus;
        private Map<String, Object> strategyParams = new HashMap<>();

        /**
         * Sets the simulation view for accessing portfolio state and history.
         *
         * <p>This is required. The simulation view provides read-only access to
         * current balances and historical data.
         *
         * @param simulation the simulation view
         * @return this builder
         */
        public Builder simulation(SimulationView simulation) {
            this.simulation = simulation;
            return this;
        }

        /**
         * Sets the date for this calculation.
         *
         * @param date the date
         * @return this builder
         */
        public Builder date(LocalDate date) {
            this.date = date;
            return this;
        }

        /**
         * Sets the total expenses.
         *
         * @param totalExpenses the expenses
         * @return this builder
         */
        public Builder totalExpenses(BigDecimal totalExpenses) {
            this.totalExpenses = totalExpenses;
            return this;
        }

        /**
         * Sets other income.
         *
         * @param otherIncome the other income
         * @return this builder
         */
        public Builder otherIncome(BigDecimal otherIncome) {
            this.otherIncome = otherIncome;
            return this;
        }

        /**
         * Sets the age.
         *
         * @param age the age
         * @return this builder
         */
        public Builder age(int age) {
            this.age = age;
            return this;
        }

        /**
         * Sets the birth year.
         *
         * @param birthYear the birth year
         * @return this builder
         */
        public Builder birthYear(int birthYear) {
            this.birthYear = birthYear;
            return this;
        }

        /**
         * Sets the retirement start date.
         *
         * <p>This is used to compute months/years in retirement for
         * inflation calculations and dynamic strategy adjustments.
         *
         * @param retirementStartDate the date retirement started
         * @return this builder
         */
        public Builder retirementStartDate(LocalDate retirementStartDate) {
            this.retirementStartDate = retirementStartDate;
            return this;
        }

        /**
         * Sets current taxable income.
         *
         * @param currentTaxableIncome the income
         * @return this builder
         */
        public Builder currentTaxableIncome(BigDecimal currentTaxableIncome) {
            this.currentTaxableIncome = currentTaxableIncome;
            return this;
        }

        /**
         * Sets the filing status.
         *
         * @param filingStatus the status
         * @return this builder
         */
        public Builder filingStatus(FilingStatus filingStatus) {
            this.filingStatus = filingStatus;
            return this;
        }

        /**
         * Sets strategy parameters.
         *
         * @param strategyParams the parameters
         * @return this builder
         */
        public Builder strategyParams(Map<String, Object> strategyParams) {
            this.strategyParams = strategyParams != null ? strategyParams : new HashMap<>();
            return this;
        }

        /**
         * Adds a strategy parameter.
         *
         * @param key the key
         * @param value the value
         * @return this builder
         */
        public Builder addStrategyParam(String key, Object value) {
            this.strategyParams.put(key, value);
            return this;
        }

        /**
         * Builds the SpendingContext instance.
         *
         * @return a new SpendingContext
         * @throws MissingRequiredFieldException if simulation or date is not set
         */
        public SpendingContext build() {
            MissingRequiredFieldException.requireNonNull(simulation, "simulation");
            MissingRequiredFieldException.requireNonNull(date, "date");
            MissingRequiredFieldException.requireNonNull(retirementStartDate, "retirementStartDate");

            return new SpendingContext(
                    simulation,
                    date,
                    totalExpenses,
                    otherIncome,
                    age,
                    birthYear,
                    retirementStartDate,
                    currentTaxableIncome,
                    filingStatus,
                    strategyParams
            );
        }
    }
}
