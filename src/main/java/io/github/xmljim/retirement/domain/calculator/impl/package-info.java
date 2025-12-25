/**
 * Default implementations of calculator interfaces.
 *
 * <p>This package contains the default implementations of the calculator
 * interfaces defined in the parent package. These implementations provide
 * standard financial calculations using BigDecimal for precision.
 *
 * <p>Implementations in this package:
 * <ul>
 *   <li>{@link io.github.xmljim.retirement.domain.calculator.impl.DefaultInflationCalculator}</li>
 *   <li>{@link io.github.xmljim.retirement.domain.calculator.impl.DefaultContributionCalculator}</li>
 *   <li>{@link io.github.xmljim.retirement.domain.calculator.impl.DefaultIncomeCalculator}</li>
 *   <li>{@link io.github.xmljim.retirement.domain.calculator.impl.DefaultReturnCalculator}</li>
 * </ul>
 *
 * <p>These implementations are typically obtained through
 * {@link io.github.xmljim.retirement.domain.calculator.CalculatorFactory}
 * rather than instantiated directly.
 *
 * @see io.github.xmljim.retirement.domain.calculator.CalculatorFactory
 */
package io.github.xmljim.retirement.domain.calculator.impl;
