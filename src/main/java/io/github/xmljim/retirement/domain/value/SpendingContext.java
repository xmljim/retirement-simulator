package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.xmljim.retirement.domain.enums.FilingStatus;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.model.Portfolio;

/**
 * Context for spending strategy calculations.
 *
 * <p>Provides all information needed by a {@code SpendingStrategy} to calculate
 * the appropriate withdrawal amount. This includes:
 * <ul>
 *   <li>Current portfolio and expenses</li>
 *   <li>Other income sources reducing withdrawal need</li>
 *   <li>State tracking for dynamic strategies (Guardrails, Kitces)</li>
 *   <li>Tax context for withdrawal sequencing</li>
 * </ul>
 *
 * <p>This record is immutable. Use the {@link Builder} to create instances.
 *
 * <p>The {@code retirementStartDate} field enables accurate monthly calculations.
 * If not explicitly set, the builder defaults to the portfolio owner's planned
 * retirement date. This can be overridden for scenario analysis (e.g., "what if
 * I retire at 62 vs 65?").
 *
 * @param portfolio the retirement portfolio
 * @param totalExpenses total monthly expenses
 * @param otherIncome income from non-portfolio sources (SS, pension, etc.)
 * @param date the date for this calculation
 * @param age the person's current age
 * @param birthYear the person's birth year (for RMD calculations)
 * @param retirementStartDate the date retirement started (for computing months/years in retirement)
 * @param initialPortfolioBalance portfolio balance at retirement start
 * @param priorYearSpending spending from the prior year (for Guardrails)
 * @param priorYearPortfolioReturn portfolio return from prior year
 * @param yearsSinceLastRatchet years since last spending increase (Kitces)
 * @param currentTaxableIncome taxable income for the year so far
 * @param filingStatus tax filing status
 * @param strategyParams additional strategy-specific parameters
 * @see io.github.xmljim.retirement.domain.calculator.SpendingStrategy
 */
@SuppressFBWarnings(value = "EI_EXPOSE_REP",
        justification = "Record makes defensive copy in compact constructor; map is unmodifiable")
public record SpendingContext(
        Portfolio portfolio,
        BigDecimal totalExpenses,
        BigDecimal otherIncome,
        LocalDate date,
        int age,
        int birthYear,
        LocalDate retirementStartDate,
        BigDecimal initialPortfolioBalance,
        BigDecimal priorYearSpending,
        BigDecimal priorYearPortfolioReturn,
        int yearsSinceLastRatchet,
        BigDecimal currentTaxableIncome,
        FilingStatus filingStatus,
        Map<String, Object> strategyParams
) {

    /**
     * Compact constructor with validation and defensive copies.
     */
    public SpendingContext {
        MissingRequiredFieldException.requireNonNull(portfolio, "portfolio");
        MissingRequiredFieldException.requireNonNull(date, "date");
        MissingRequiredFieldException.requireNonNull(retirementStartDate, "retirementStartDate");
        totalExpenses = totalExpenses != null ? totalExpenses : BigDecimal.ZERO;
        otherIncome = otherIncome != null ? otherIncome : BigDecimal.ZERO;
        initialPortfolioBalance = initialPortfolioBalance != null
                ? initialPortfolioBalance
                : portfolio.getTotalBalance();
        priorYearSpending = priorYearSpending != null ? priorYearSpending : BigDecimal.ZERO;
        priorYearPortfolioReturn = priorYearPortfolioReturn != null
                ? priorYearPortfolioReturn
                : BigDecimal.ZERO;
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
     * @return the total portfolio balance
     */
    public BigDecimal currentPortfolioBalance() {
        return portfolio.getTotalBalance();
    }

    /**
     * Returns the current withdrawal rate based on portfolio balance.
     *
     * <p>Formula: priorYearSpending / currentPortfolioBalance
     *
     * @return the current withdrawal rate as a decimal
     */
    public BigDecimal currentWithdrawalRate() {
        BigDecimal balance = currentPortfolioBalance();
        if (balance.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return priorYearSpending.divide(balance, 4, java.math.RoundingMode.HALF_UP);
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
        private Portfolio portfolio;
        private BigDecimal totalExpenses = BigDecimal.ZERO;
        private BigDecimal otherIncome = BigDecimal.ZERO;
        private LocalDate date;
        private int age;
        private int birthYear;
        private LocalDate retirementStartDate;
        private BigDecimal initialPortfolioBalance;
        private BigDecimal priorYearSpending = BigDecimal.ZERO;
        private BigDecimal priorYearPortfolioReturn = BigDecimal.ZERO;
        private int yearsSinceLastRatchet;
        private BigDecimal currentTaxableIncome = BigDecimal.ZERO;
        private FilingStatus filingStatus;
        private Map<String, Object> strategyParams = new HashMap<>();

        /**
         * Sets the portfolio.
         *
         * @param portfolio the portfolio
         * @return this builder
         */
        public Builder portfolio(Portfolio portfolio) {
            this.portfolio = portfolio;
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
         * Sets the date.
         *
         * @param date the date
         * @return this builder
         */
        public Builder date(LocalDate date) {
            this.date = date;
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
         * <p>If not set, defaults to the portfolio owner's planned retirement date.
         * This can be overridden for scenario analysis (e.g., "what if I retire at 62?").
         *
         * @param retirementStartDate the date retirement started
         * @return this builder
         */
        public Builder retirementStartDate(LocalDate retirementStartDate) {
            this.retirementStartDate = retirementStartDate;
            return this;
        }

        /**
         * Sets the initial portfolio balance.
         *
         * @param initialPortfolioBalance the balance
         * @return this builder
         */
        public Builder initialPortfolioBalance(BigDecimal initialPortfolioBalance) {
            this.initialPortfolioBalance = initialPortfolioBalance;
            return this;
        }

        /**
         * Sets prior year spending.
         *
         * @param priorYearSpending the spending
         * @return this builder
         */
        public Builder priorYearSpending(BigDecimal priorYearSpending) {
            this.priorYearSpending = priorYearSpending;
            return this;
        }

        /**
         * Sets prior year portfolio return.
         *
         * @param priorYearPortfolioReturn the return
         * @return this builder
         */
        public Builder priorYearPortfolioReturn(BigDecimal priorYearPortfolioReturn) {
            this.priorYearPortfolioReturn = priorYearPortfolioReturn;
            return this;
        }

        /**
         * Sets years since last ratchet.
         *
         * @param yearsSinceLastRatchet the years
         * @return this builder
         */
        public Builder yearsSinceLastRatchet(int yearsSinceLastRatchet) {
            this.yearsSinceLastRatchet = yearsSinceLastRatchet;
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
         * <p>If age or birthYear are not explicitly set, they will be derived
         * from the portfolio owner's date of birth. If retirementStartDate is not
         * set, it defaults to the portfolio owner's planned retirement date.
         *
         * @return a new SpendingContext
         * @throws MissingRequiredFieldException if portfolio is not set
         */
        public SpendingContext build() {
            // Validate portfolio early - owner and dateOfBirth are guaranteed by Portfolio/PersonProfile
            MissingRequiredFieldException.requireNonNull(portfolio, "portfolio");

            // Derive age and birthYear from portfolio owner if not set
            int resolvedAge = age;
            int resolvedBirthYear = birthYear;

            LocalDate ownerDob = portfolio.getOwner().getDateOfBirth();
            if (resolvedBirthYear == 0) {
                resolvedBirthYear = ownerDob.getYear();
            }
            if (resolvedAge == 0 && date != null) {
                resolvedAge = portfolio.getOwner().getAge(date);
            }

            // Default retirementStartDate from portfolio owner if not set
            LocalDate resolvedRetirementStartDate = retirementStartDate;
            if (resolvedRetirementStartDate == null) {
                resolvedRetirementStartDate = portfolio.getOwner().getRetirementDate();
            }

            return new SpendingContext(
                    portfolio,
                    totalExpenses,
                    otherIncome,
                    date,
                    resolvedAge,
                    resolvedBirthYear,
                    resolvedRetirementStartDate,
                    initialPortfolioBalance,
                    priorYearSpending,
                    priorYearPortfolioReturn,
                    yearsSinceLastRatchet,
                    currentTaxableIncome,
                    filingStatus,
                    strategyParams
            );
        }
    }
}
