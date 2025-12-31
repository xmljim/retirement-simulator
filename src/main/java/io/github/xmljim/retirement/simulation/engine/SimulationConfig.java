package io.github.xmljim.retirement.simulation.engine;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.xmljim.retirement.domain.calculator.SpendingStrategy;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;
import io.github.xmljim.retirement.domain.model.InvestmentAccount;
import io.github.xmljim.retirement.domain.model.PersonProfile;
import io.github.xmljim.retirement.domain.model.Portfolio;
import io.github.xmljim.retirement.domain.value.Budget;
import io.github.xmljim.retirement.simulation.config.SimulationLevers;

/**
 * Configuration for running a retirement simulation.
 *
 * <p>SimulationConfig contains all inputs required to run a simulation:
 * <ul>
 *   <li>Person profiles (one or two for couple simulations)</li>
 *   <li>Portfolios with investment accounts (one per person)</li>
 *   <li>Budget defining income and expenses</li>
 *   <li>Simulation levers controlling economic assumptions</li>
 *   <li>Time period (start and end months)</li>
 *   <li>Spending strategy for distribution phase</li>
 * </ul>
 *
 * <p>Use the builder for construction:
 * <pre>{@code
 * SimulationConfig config = SimulationConfig.builder()
 *     .persons(List.of(primaryPerson, spouse))
 *     .portfolios(List.of(primaryPortfolio, spousePortfolio))
 *     .budget(budget)
 *     .levers(SimulationLevers.withDefaults())
 *     .startMonth(YearMonth.of(2025, 1))
 *     .endMonth(YearMonth.of(2055, 12))
 *     .strategy(spendingStrategy)
 *     .build();
 * }</pre>
 *
 * @param persons    the person profiles (1 for single, 2 for couple)
 * @param portfolios the investment portfolios (one per person)
 * @param budget     the income and expense budget
 * @param levers     the simulation levers (economic assumptions)
 * @param startMonth the simulation start month
 * @param endMonth   the simulation end month
 * @param strategy   the spending strategy for withdrawals
 */
public record SimulationConfig(
    List<PersonProfile> persons,
    List<Portfolio> portfolios,
    Budget budget,
    SimulationLevers levers,
    YearMonth startMonth,
    YearMonth endMonth,
    SpendingStrategy strategy
) {

    /**
     * Compact constructor with validation and defensive copying.
     */
    public SimulationConfig {
        MissingRequiredFieldException.requireNonNull(persons, "persons");
        MissingRequiredFieldException.requireNonNull(portfolios, "portfolios");
        MissingRequiredFieldException.requireNonNull(startMonth, "startMonth");
        MissingRequiredFieldException.requireNonNull(endMonth, "endMonth");

        ValidationException.validate("persons", persons, p -> !p.isEmpty(),
            "At least one person profile is required");
        ValidationException.validate("portfolios", portfolios, p -> !p.isEmpty(),
            "At least one portfolio is required");

        persons = Collections.unmodifiableList(persons);
        portfolios = Collections.unmodifiableList(portfolios);
        levers = levers != null ? levers : SimulationLevers.withDefaults();
    }

    /**
     * Returns the primary person (first in list).
     *
     * @return the primary person profile
     */
    public PersonProfile primaryPerson() {
        return persons.get(0);
    }

    /**
     * Returns the spouse (second person) if present.
     *
     * @return the spouse profile, or null if single simulation
     */
    public PersonProfile spouse() {
        return persons.size() > 1 ? persons.get(1) : null;
    }

    /**
     * Indicates whether this is a couple simulation.
     *
     * @return true if two persons are configured
     */
    public boolean isCoupleSimulation() {
        return persons.size() > 1;
    }

    /**
     * Returns all investment accounts across all portfolios.
     *
     * @return list of all accounts
     */
    public List<InvestmentAccount> getAllAccounts() {
        List<InvestmentAccount> allAccounts = new ArrayList<>();
        portfolios.forEach(p -> allAccounts.addAll(p.getAccounts()));
        return Collections.unmodifiableList(allAccounts);
    }

    /**
     * Returns the primary portfolio (first in list).
     *
     * @return the primary portfolio
     */
    public Portfolio primaryPortfolio() {
        return portfolios.get(0);
    }

    /**
     * Calculates the total number of months in the simulation.
     *
     * @return the month count
     */
    public int monthCount() {
        int years = endMonth.getYear() - startMonth.getYear();
        int months = endMonth.getMonthValue() - startMonth.getMonthValue();
        return years * 12 + months + 1;
    }

    /**
     * Creates a new builder.
     *
     * @return a builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for SimulationConfig.
     */
    public static class Builder {
        private List<PersonProfile> persons;
        private List<Portfolio> portfolios;
        private Budget budget;
        private SimulationLevers levers;
        private YearMonth startMonth;
        private YearMonth endMonth;
        private SpendingStrategy strategy;

        /**
         * Sets the person profiles.
         *
         * @param persons the profiles (1 or 2)
         * @return this builder
         */
        public Builder persons(List<PersonProfile> persons) {
            this.persons = persons != null ? new ArrayList<>(persons) : null;
            return this;
        }

        /**
         * Sets a single person (convenience method).
         *
         * @param person the person profile
         * @return this builder
         */
        public Builder person(PersonProfile person) {
            this.persons = List.of(person);
            return this;
        }

        /**
         * Sets the portfolios.
         *
         * @param portfolios the investment portfolios
         * @return this builder
         */
        public Builder portfolios(List<Portfolio> portfolios) {
            this.portfolios = portfolios != null ? new ArrayList<>(portfolios) : null;
            return this;
        }

        /**
         * Sets a single portfolio (convenience method).
         *
         * @param portfolio the investment portfolio
         * @return this builder
         */
        public Builder portfolio(Portfolio portfolio) {
            this.portfolios = List.of(portfolio);
            return this;
        }

        /**
         * Sets the budget.
         *
         * @param budget the income/expense budget
         * @return this builder
         */
        public Builder budget(Budget budget) {
            this.budget = budget;
            return this;
        }

        /**
         * Sets the simulation levers.
         *
         * @param levers the economic assumptions
         * @return this builder
         */
        public Builder levers(SimulationLevers levers) {
            this.levers = levers;
            return this;
        }

        /**
         * Sets the simulation start month.
         *
         * @param startMonth the start month
         * @return this builder
         */
        public Builder startMonth(YearMonth startMonth) {
            this.startMonth = startMonth;
            return this;
        }

        /**
         * Sets the simulation end month.
         *
         * @param endMonth the end month
         * @return this builder
         */
        public Builder endMonth(YearMonth endMonth) {
            this.endMonth = endMonth;
            return this;
        }

        /**
         * Sets the spending strategy.
         *
         * @param strategy the strategy for withdrawals
         * @return this builder
         */
        public Builder strategy(SpendingStrategy strategy) {
            this.strategy = strategy;
            return this;
        }

        /**
         * Builds the SimulationConfig.
         *
         * @return a new SimulationConfig
         */
        public SimulationConfig build() {
            return new SimulationConfig(
                persons,
                portfolios,
                budget,
                levers,
                startMonth,
                endMonth,
                strategy
            );
        }
    }
}
