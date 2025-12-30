/**
 * Default implementations of calculator interfaces.
 *
 * <p>This package contains the default implementations of the calculator
 * interfaces defined in the parent package. These implementations provide
 * standard financial calculations using BigDecimal for precision.
 *
 * <h2>Core Calculator Implementations</h2>
 * <ul>
 *   <li>{@link io.github.xmljim.retirement.domain.calculator.impl.DefaultInflationCalculator}</li>
 *   <li>{@link io.github.xmljim.retirement.domain.calculator.impl.DefaultContributionCalculator}</li>
 *   <li>{@link io.github.xmljim.retirement.domain.calculator.impl.DefaultIncomeCalculator}</li>
 *   <li>{@link io.github.xmljim.retirement.domain.calculator.impl.DefaultReturnCalculator}</li>
 * </ul>
 *
 * <h2>Distribution Strategy Implementations (M6)</h2>
 * <ul>
 *   <li>{@link io.github.xmljim.retirement.domain.calculator.impl.StaticSpendingStrategy} -
 *       4% rule with inflation adjustment</li>
 *   <li>{@link io.github.xmljim.retirement.domain.calculator.impl.IncomeGapStrategy} -
 *       Withdraw gap between expenses and other income</li>
 *   <li>{@link io.github.xmljim.retirement.domain.calculator.impl.GuardrailsSpendingStrategy} -
 *       Dynamic withdrawals with guardrails (Guyton-Klinger, Vanguard, Kitces)</li>
 *   <li>{@link io.github.xmljim.retirement.domain.calculator.impl.DefaultSpendingOrchestrator} -
 *       Default strategy orchestrator</li>
 *   <li>{@link io.github.xmljim.retirement.domain.calculator.impl.RmdAwareOrchestrator} -
 *       RMD-compliant orchestrator ensuring mandatory distributions</li>
 *   <li>{@link io.github.xmljim.retirement.domain.calculator.impl.TaxEfficientSequencer} -
 *       Taxable → Traditional → Roth account ordering</li>
 *   <li>{@link io.github.xmljim.retirement.domain.calculator.impl.RmdFirstSequencer} -
 *       RMD accounts first, then tax-efficient ordering</li>
 * </ul>
 *
 * <p>These implementations are typically obtained through
 * {@link io.github.xmljim.retirement.domain.calculator.CalculatorFactory}
 * rather than instantiated directly.
 *
 * @see io.github.xmljim.retirement.domain.calculator.CalculatorFactory
 * @see io.github.xmljim.retirement.domain.calculator.SpendingStrategy
 */
package io.github.xmljim.retirement.domain.calculator.impl;
