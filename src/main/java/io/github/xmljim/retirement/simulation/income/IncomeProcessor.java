package io.github.xmljim.retirement.simulation.income;

import java.time.YearMonth;
import java.util.List;

import io.github.xmljim.retirement.domain.enums.SimulationPhase;

/**
 * Processes income for a simulation month.
 *
 * <p>The IncomeProcessor aggregates income from all sources for all persons
 * in the simulation. Income sources depend on the simulation phase:
 *
 * <ul>
 *   <li><b>ACCUMULATION:</b> Salary income from working persons</li>
 *   <li><b>DISTRIBUTION:</b> Social Security, pensions, annuities, other income</li>
 *   <li><b>SURVIVOR:</b> Adjusted survivor benefits from applicable sources</li>
 * </ul>
 *
 * <p>Usage in simulation loop:
 * <pre>{@code
 * IncomeProcessor processor = new DefaultIncomeProcessor();
 * MonthlyIncome income = processor.process(incomeProfiles, month, phase);
 * }</pre>
 *
 * @see MonthlyIncome
 * @see IncomeProfile
 * @see io.github.xmljim.retirement.simulation.income.impl.DefaultIncomeProcessor
 */
@FunctionalInterface
public interface IncomeProcessor {

    /**
     * Processes all income sources for the given month and phase.
     *
     * <p>Aggregates income from all income profiles based on their
     * individual income sources and the current simulation phase.
     *
     * @param incomeProfiles the income profiles to process
     * @param month          the simulation month
     * @param phase          the current simulation phase
     * @return the aggregated monthly income
     */
    MonthlyIncome process(List<IncomeProfile> incomeProfiles, YearMonth month, SimulationPhase phase);
}
