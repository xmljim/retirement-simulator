/**
 * Calculator interfaces and factory for financial calculations.
 *
 * <p>This package provides a set of calculator interfaces that encapsulate
 * the core financial calculations needed for retirement simulations:
 *
 * <h2>Core Calculators</h2>
 * <ul>
 *   <li>{@link io.github.xmljim.retirement.domain.calculator.InflationCalculator} -
 *       Inflation and cost-of-living adjustment calculations</li>
 *   <li>{@link io.github.xmljim.retirement.domain.calculator.ContributionCalculator} -
 *       Personal and employer contribution rate calculations</li>
 *   <li>{@link io.github.xmljim.retirement.domain.calculator.IncomeCalculator} -
 *       Salary, Social Security, and retirement income calculations</li>
 *   <li>{@link io.github.xmljim.retirement.domain.calculator.ReturnCalculator} -
 *       Investment return and growth calculations</li>
 * </ul>
 *
 * <h2>Distribution Strategy Framework (M6)</h2>
 * <ul>
 *   <li>{@link io.github.xmljim.retirement.domain.calculator.SpendingStrategy} -
 *       Interface for withdrawal calculation strategies</li>
 *   <li>{@link io.github.xmljim.retirement.domain.calculator.SpendingOrchestrator} -
 *       Coordinates strategy, sequencing, and withdrawal execution</li>
 *   <li>{@link io.github.xmljim.retirement.domain.calculator.AccountSequencer} -
 *       Interface for determining account withdrawal order</li>
 *   <li>{@link io.github.xmljim.retirement.domain.calculator.RmdCalculator} -
 *       Required Minimum Distribution calculations</li>
 *   <li>{@link io.github.xmljim.retirement.domain.calculator.SimulationView} -
 *       Read-only interface to simulation state for strategies</li>
 * </ul>
 *
 * <p>Use {@link io.github.xmljim.retirement.domain.calculator.CalculatorFactory}
 * to obtain calculator instances.
 *
 * <p>Default implementations are provided in the
 * {@link io.github.xmljim.retirement.domain.calculator.impl} subpackage.
 *
 * @see io.github.xmljim.retirement.domain.calculator.CalculatorFactory
 * @see io.github.xmljim.retirement.domain.calculator.impl
 */
package io.github.xmljim.retirement.domain.calculator;
