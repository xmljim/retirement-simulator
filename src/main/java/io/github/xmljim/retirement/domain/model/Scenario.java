package io.github.xmljim.retirement.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import io.github.xmljim.retirement.domain.annotation.Generated;
import io.github.xmljim.retirement.domain.enums.DistributionStrategy;
import io.github.xmljim.retirement.domain.enums.EndCondition;
import io.github.xmljim.retirement.domain.enums.SimulationMode;
import io.github.xmljim.retirement.domain.exception.InvalidDateRangeException;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.value.InflationAssumptions;

/**
 * Represents a complete simulation configuration for retirement projections.
 *
 * <p>A Scenario contains all parameters needed to run a retirement simulation,
 * including the person profile(s), time horizon, simulation mode, and various
 * assumption overrides.
 */
public final class Scenario {

    public static final BigDecimal DEFAULT_STOCK_RETURN = new BigDecimal("0.07");
    public static final BigDecimal DEFAULT_BOND_RETURN = new BigDecimal("0.04");
    public static final BigDecimal DEFAULT_CASH_RETURN = new BigDecimal("0.02");

    private final String id;
    private final String name;
    private final PersonProfile primaryPerson;
    private final PersonProfile secondaryPerson;
    private final LocalDate startDate;
    private final EndCondition endCondition;
    private final SimulationMode simulationMode;
    private final DistributionStrategy distributionStrategy;
    private final InflationAssumptions inflationAssumptions;
    private final BigDecimal defaultStockReturn;
    private final BigDecimal defaultBondReturn;
    private final BigDecimal defaultCashReturn;

    private Scenario(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.name = builder.name;
        this.primaryPerson = builder.primaryPerson;
        this.secondaryPerson = builder.secondaryPerson;
        this.startDate = builder.startDate;
        this.endCondition = builder.endCondition;
        this.simulationMode = builder.simulationMode;
        this.distributionStrategy = builder.distributionStrategy;
        this.inflationAssumptions = builder.inflationAssumptions;
        this.defaultStockReturn = builder.defaultStockReturn;
        this.defaultBondReturn = builder.defaultBondReturn;
        this.defaultCashReturn = builder.defaultCashReturn;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public PersonProfile getPrimaryPerson() {
        return primaryPerson;
    }

    public Optional<PersonProfile> getSecondaryPerson() {
        return Optional.ofNullable(secondaryPerson);
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public EndCondition getEndCondition() {
        return endCondition;
    }

    public SimulationMode getSimulationMode() {
        return simulationMode;
    }

    public DistributionStrategy getDistributionStrategy() {
        return distributionStrategy;
    }

    public InflationAssumptions getInflationAssumptions() {
        return inflationAssumptions;
    }

    public BigDecimal getDefaultStockReturn() {
        return defaultStockReturn;
    }

    public BigDecimal getDefaultBondReturn() {
        return defaultBondReturn;
    }

    public BigDecimal getDefaultCashReturn() {
        return defaultCashReturn;
    }

    public boolean isCoupleScenario() {
        return secondaryPerson != null;
    }

    /**
     * Returns the projected end date based on life expectancy.
     * For couples, returns the later of the two projected end dates.
     *
     * @return the projected simulation end date
     */
    public LocalDate getProjectedEndDate() {
        LocalDate primaryEnd = primaryPerson.getProjectedEndDate();
        if (secondaryPerson != null) {
            LocalDate secondaryEnd = secondaryPerson.getProjectedEndDate();
            return primaryEnd.isAfter(secondaryEnd) ? primaryEnd : secondaryEnd;
        }
        return primaryEnd;
    }

    /**
     * Calculates the simulation duration in years.
     *
     * @return the number of years from start to projected end
     */
    public int getSimulationDurationYears() {
        return Period.between(startDate, getProjectedEndDate()).getYears();
    }

    /**
     * Creates a new builder for Scenario.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder initialized with values from this scenario.
     *
     * @return a new builder with copied values
     */
    public Builder toBuilder() {
        return new Builder()
            .id(this.id)
            .name(this.name)
            .primaryPerson(this.primaryPerson)
            .secondaryPerson(this.secondaryPerson)
            .startDate(this.startDate)
            .endCondition(this.endCondition)
            .simulationMode(this.simulationMode)
            .distributionStrategy(this.distributionStrategy)
            .inflationAssumptions(this.inflationAssumptions)
            .defaultStockReturn(this.defaultStockReturn)
            .defaultBondReturn(this.defaultBondReturn)
            .defaultCashReturn(this.defaultCashReturn);
    }

    @Generated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Scenario scenario = (Scenario) o;
        return Objects.equals(id, scenario.id);
    }

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Generated
    @Override
    public String toString() {
        return "Scenario{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", primaryPerson=" + primaryPerson.getName() +
            ", isCoupleScenario=" + isCoupleScenario() +
            ", startDate=" + startDate +
            ", endCondition=" + endCondition +
            '}';
    }

    /**
     * Builder for creating Scenario instances.
     */
    public static class Builder {
        private String id;
        private String name;
        private PersonProfile primaryPerson;
        private PersonProfile secondaryPerson;
        private LocalDate startDate = LocalDate.now();
        private EndCondition endCondition = EndCondition.LIFE_EXPECTANCY;
        private SimulationMode simulationMode = SimulationMode.DETERMINISTIC;
        private DistributionStrategy distributionStrategy = DistributionStrategy.TAX_EFFICIENT;
        private InflationAssumptions inflationAssumptions = InflationAssumptions.defaults();
        private BigDecimal defaultStockReturn = DEFAULT_STOCK_RETURN;
        private BigDecimal defaultBondReturn = DEFAULT_BOND_RETURN;
        private BigDecimal defaultCashReturn = DEFAULT_CASH_RETURN;

        /** Sets the scenario ID. @param id the ID @return this builder */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /** Sets the scenario name. @param name the name @return this builder */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /** Sets the primary person. @param person the person profile @return this builder */
        public Builder primaryPerson(PersonProfile person) {
            this.primaryPerson = person;
            return this;
        }

        /** Sets the secondary person for couples. @param person the person profile @return this builder */
        public Builder secondaryPerson(PersonProfile person) {
            this.secondaryPerson = person;
            return this;
        }

        /** Sets the simulation start date. @param startDate the start date @return this builder */
        public Builder startDate(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        /** Sets the end condition. @param endCondition the end condition @return this builder */
        public Builder endCondition(EndCondition endCondition) {
            this.endCondition = endCondition;
            return this;
        }

        /** Sets the simulation mode. @param mode the mode @return this builder */
        public Builder simulationMode(SimulationMode mode) {
            this.simulationMode = mode;
            return this;
        }

        /** Sets the distribution strategy. @param strategy the strategy @return this builder */
        public Builder distributionStrategy(DistributionStrategy strategy) {
            this.distributionStrategy = strategy;
            return this;
        }

        /** Sets inflation assumptions. @param assumptions the assumptions @return this builder */
        public Builder inflationAssumptions(InflationAssumptions assumptions) {
            this.inflationAssumptions = assumptions;
            return this;
        }

        /** Sets default stock return. @param rate the rate @return this builder */
        public Builder defaultStockReturn(BigDecimal rate) {
            this.defaultStockReturn = rate;
            return this;
        }

        /** Sets default stock return. @param rate the rate @return this builder */
        public Builder defaultStockReturn(double rate) {
            return defaultStockReturn(BigDecimal.valueOf(rate));
        }

        /** Sets default bond return. @param rate the rate @return this builder */
        public Builder defaultBondReturn(BigDecimal rate) {
            this.defaultBondReturn = rate;
            return this;
        }

        /** Sets default bond return. @param rate the rate @return this builder */
        public Builder defaultBondReturn(double rate) {
            return defaultBondReturn(BigDecimal.valueOf(rate));
        }

        /** Sets default cash return. @param rate the rate @return this builder */
        public Builder defaultCashReturn(BigDecimal rate) {
            this.defaultCashReturn = rate;
            return this;
        }

        /** Sets default cash return. @param rate the rate @return this builder */
        public Builder defaultCashReturn(double rate) {
            return defaultCashReturn(BigDecimal.valueOf(rate));
        }

        /** Builds the Scenario. @return a new Scenario @throws MissingRequiredFieldException if missing */
        public Scenario build() {
            validate();
            return new Scenario(this);
        }

        private void validate() {
            MissingRequiredFieldException.requireNonNull(name, "name");
            MissingRequiredFieldException.requireNonNull(primaryPerson, "primaryPerson");
            MissingRequiredFieldException.requireNonNull(startDate, "startDate");
            MissingRequiredFieldException.requireNonNull(endCondition, "endCondition");
            MissingRequiredFieldException.requireNonNull(simulationMode, "simulationMode");
            MissingRequiredFieldException.requireNonNull(distributionStrategy, "distributionStrategy");
            MissingRequiredFieldException.requireNonNull(inflationAssumptions, "inflationAssumptions");

            if (startDate.isAfter(primaryPerson.getProjectedEndDate())) {
                throw InvalidDateRangeException.dateMustBeBefore(
                    "Start date", "primary person's projected end date");
            }
        }
    }
}
