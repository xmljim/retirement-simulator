package io.github.xmljim.retirement.domain.calculator;

import java.util.List;

import io.github.xmljim.retirement.domain.value.AccountSnapshot;
import io.github.xmljim.retirement.domain.value.SpendingContext;

/**
 * Interface for determining the order of accounts for withdrawals.
 *
 * <p>Account sequencing determines which accounts to draw from first during
 * retirement withdrawals. Different strategies optimize for different goals:
 * <ul>
 *   <li><b>Tax-Efficient:</b> Taxable → Traditional → Roth (preserve tax-free growth)</li>
 *   <li><b>RMD-First:</b> Accounts requiring RMDs first, then tax-efficient</li>
 *   <li><b>Pro-Rata:</b> Proportional withdrawals from all accounts</li>
 *   <li><b>Custom:</b> User-defined account order</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * AccountSequencer sequencer = new TaxEfficientSequencer();
 * List<AccountSnapshot> orderedAccounts = sequencer.sequence(context);
 *
 * // Withdraw from accounts in order until target met
 * BigDecimal remaining = targetWithdrawal;
 * for (AccountSnapshot account : orderedAccounts) {
 *     if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
 *     BigDecimal withdrawal = remaining.min(account.balance());
 *     remaining = remaining.subtract(withdrawal);
 * }
 * }</pre>
 *
 * <p>Note: This interface determines <em>account order</em>. The
 * {@link SpendingStrategy} interface determines <em>how much</em> to withdraw.
 *
 * @see SpendingStrategy
 * @see SpendingContext
 * @see AccountSnapshot
 */
public interface AccountSequencer {

    /**
     * Returns accounts in withdrawal priority order.
     *
     * <p>The returned list contains account snapshots ordered by withdrawal
     * priority. Accounts earlier in the list should be drawn from before
     * accounts later in the list.
     *
     * <p>Account information is obtained from
     * {@code context.simulation().getAccountSnapshots()}.
     *
     * <p>The list may exclude accounts that should not be used (e.g., accounts
     * with zero balance, or accounts reserved for specific purposes).
     *
     * @param context the spending context containing simulation with accounts
     * @return list of account snapshots in withdrawal priority order
     * @throws io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException
     *         if context or simulation is null
     */
    List<AccountSnapshot> sequence(SpendingContext context);

    /**
     * Returns the sequencer name for identification.
     *
     * <p>This name is used in logging and reporting.
     *
     * @return the sequencer name (e.g., "Tax-Efficient", "RMD-First")
     */
    String getName();

    /**
     * Returns a description of the sequencing strategy.
     *
     * @return the strategy description
     */
    default String getDescription() {
        return getName() + " account sequencing";
    }

    /**
     * Returns whether this sequencer considers RMD requirements.
     *
     * <p>RMD-aware sequencers ensure accounts with Required Minimum Distributions
     * are prioritized appropriately when RMDs apply.
     *
     * @return true if the sequencer handles RMDs
     */
    default boolean isRmdAware() {
        return false;
    }

    /**
     * Returns whether this sequencer considers tax optimization.
     *
     * <p>Tax-aware sequencers order accounts to minimize tax liability,
     * typically by withdrawing from taxable accounts before tax-advantaged.
     *
     * @return true if the sequencer optimizes for taxes
     */
    default boolean isTaxAware() {
        return false;
    }
}
