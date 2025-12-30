package io.github.xmljim.retirement.domain.calculator;

import io.github.xmljim.retirement.domain.value.SpendingContext;
import io.github.xmljim.retirement.domain.value.SpendingPlan;

/**
 * Strategy interface for retirement spending calculations.
 *
 * <p>Implementations of this interface determine how much to withdraw from
 * a retirement portfolio based on the chosen spending approach. This is
 * the Strategy pattern applied to retirement distribution planning.
 *
 * <p>Available strategy implementations (Milestone 6):
 * <ul>
 *   <li><b>Static (4% Rule)</b>: Fixed percentage of initial balance, adjusted for inflation</li>
 *   <li><b>Income Gap</b>: Withdraw exactly the gap between expenses and other income</li>
 *   <li><b>Bucket Strategy</b>: Time-segmented approach with short/medium/long-term buckets</li>
 *   <li><b>Guardrails</b>: Dynamic adjustments based on portfolio performance
 *       (Guyton-Klinger, Vanguard, Kitces)</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * SpendingStrategy strategy = new StaticSpendingStrategy(0.04, 0.025);
 * SpendingContext context = SpendingContext.builder()
 *     .simulation(simulationView)
 *     .totalExpenses(monthlyExpenses)
 *     .otherIncome(ssIncome.add(pensionIncome))
 *     .date(LocalDate.now())
 *     .retirementStartDate(retirementStart)
 *     .build();
 *
 * SpendingPlan plan = strategy.calculateWithdrawal(context);
 * }</pre>
 *
 * <p>Note: This interface determines <em>how much</em> to withdraw. The
 * {@code AccountSequencer} interface determines <em>from which accounts</em>
 * to withdraw.
 *
 * @see SpendingContext
 * @see SpendingPlan
 * @see AccountSequencer
 */
public interface SpendingStrategy {

    /**
     * Calculates the withdrawal plan based on the spending context.
     *
     * <p>This method determines how much should be withdrawn from the portfolio
     * to meet spending needs. The calculation depends on the specific strategy
     * implementation.
     *
     * <p>The returned {@link SpendingPlan} includes:
     * <ul>
     *   <li>Target withdrawal amount based on strategy rules</li>
     *   <li>Actual withdrawal after applying constraints</li>
     *   <li>Whether the target was fully met</li>
     *   <li>Strategy-specific metadata</li>
     * </ul>
     *
     * @param context the spending context with portfolio, expenses, and state
     * @return the calculated spending plan
     * @throws io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException
     *         if required context fields are missing
     * @throws io.github.xmljim.retirement.domain.exception.CalculationException
     *         if calculation fails
     */
    SpendingPlan calculateWithdrawal(SpendingContext context);

    /**
     * Returns the strategy name for identification.
     *
     * <p>This name is used in logging, reporting, and the {@link SpendingPlan#strategyUsed()}
     * field.
     *
     * @return the strategy name (e.g., "Static 4%", "Guyton-Klinger Guardrails")
     */
    String getName();

    /**
     * Returns a description of the strategy.
     *
     * <p>This description explains how the strategy works for user display.
     *
     * @return the strategy description
     */
    String getDescription();

    /**
     * Returns whether this strategy supports dynamic adjustments.
     *
     * <p>Dynamic strategies (like Guardrails) adjust spending based on
     * portfolio performance and may require state tracking between periods.
     * Static strategies maintain consistent inflation-adjusted spending.
     *
     * @return true if the strategy adjusts dynamically
     */
    default boolean isDynamic() {
        return false;
    }

    /**
     * Returns whether this strategy requires prior year state.
     *
     * <p>Some strategies (Guardrails, Kitces) need to know prior year spending
     * and portfolio returns to make adjustments. This method helps the
     * orchestrator ensure required state is provided.
     *
     * @return true if prior year state is required
     */
    default boolean requiresPriorYearState() {
        return false;
    }
}
