package io.github.xmljim.retirement.domain.calculator;

import io.github.xmljim.retirement.domain.model.Portfolio;
import io.github.xmljim.retirement.domain.value.SpendingContext;
import io.github.xmljim.retirement.domain.value.SpendingPlan;

/**
 * Orchestrator that coordinates spending strategy, account sequencing, and execution.
 *
 * <p>The orchestrator is the central coordinator for retirement withdrawals. It:
 * <ol>
 *   <li>Uses the {@link SpendingStrategy} to determine how much to withdraw</li>
 *   <li>Uses the {@link AccountSequencer} to determine account order</li>
 *   <li>Executes withdrawals from accounts in sequence until target is met</li>
 *   <li>Builds and returns a complete {@link SpendingPlan}</li>
 * </ol>
 *
 * <p>Usage example:
 * <pre>{@code
 * SpendingOrchestrator orchestrator = new DefaultSpendingOrchestrator();
 *
 * SpendingStrategy strategy = new StaticSpendingStrategy(0.04, 0.025);
 * AccountSequencer sequencer = context.isSubjectToRmd()
 *     ? new RmdFirstSequencer(rmdCalc)
 *     : new TaxEfficientSequencer();
 *
 * SpendingPlan plan = orchestrator.execute(portfolio, strategy, sequencer, context);
 *
 * if (!plan.meetsTarget()) {
 *     // Handle shortfall
 *     log.warn("Shortfall of {}", plan.shortfall());
 * }
 * }</pre>
 *
 * @see SpendingStrategy
 * @see AccountSequencer
 * @see SpendingPlan
 * @see SpendingContext
 */
public interface SpendingOrchestrator {

    /**
     * Executes a withdrawal operation.
     *
     * <p>This method coordinates the strategy, sequencing, and execution of
     * withdrawals. The process is:
     * <ol>
     *   <li>Call the strategy to calculate target withdrawal amount</li>
     *   <li>Use the sequencer to determine account order</li>
     *   <li>Withdraw from accounts in sequence until target is met</li>
     *   <li>Track any shortfall if accounts are insufficient</li>
     *   <li>Return a complete SpendingPlan with all details</li>
     * </ol>
     *
     * @param portfolio the portfolio to withdraw from
     * @param strategy the spending strategy determining withdrawal amount
     * @param sequencer the account sequencer determining withdrawal order
     * @param context the spending context with expenses, income, and state
     * @return the complete spending plan with account withdrawals
     * @throws io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException
     *         if required parameters are null
     */
    SpendingPlan execute(
            Portfolio portfolio,
            SpendingStrategy strategy,
            AccountSequencer sequencer,
            SpendingContext context
    );

    /**
     * Executes a withdrawal using the default sequencer.
     *
     * <p>Automatically selects the appropriate sequencer based on context:
     * <ul>
     *   <li>If subject to RMDs: Uses RmdFirstSequencer</li>
     *   <li>Otherwise: Uses TaxEfficientSequencer</li>
     * </ul>
     *
     * @param portfolio the portfolio to withdraw from
     * @param strategy the spending strategy
     * @param context the spending context
     * @return the complete spending plan
     */
    default SpendingPlan execute(
            Portfolio portfolio,
            SpendingStrategy strategy,
            SpendingContext context) {
        AccountSequencer sequencer = selectDefaultSequencer(context);
        return execute(portfolio, strategy, sequencer, context);
    }

    /**
     * Selects the appropriate default sequencer based on context.
     *
     * @param context the spending context
     * @return the appropriate sequencer
     */
    AccountSequencer selectDefaultSequencer(SpendingContext context);
}
