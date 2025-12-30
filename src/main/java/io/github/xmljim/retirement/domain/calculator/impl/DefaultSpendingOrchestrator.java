package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import io.github.xmljim.retirement.domain.calculator.AccountSequencer;
import io.github.xmljim.retirement.domain.calculator.RmdCalculator;
import io.github.xmljim.retirement.domain.calculator.SpendingOrchestrator;
import io.github.xmljim.retirement.domain.calculator.SpendingStrategy;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.value.AccountSnapshot;
import io.github.xmljim.retirement.domain.value.AccountWithdrawal;
import io.github.xmljim.retirement.domain.value.SpendingContext;
import io.github.xmljim.retirement.domain.value.SpendingPlan;

/**
 * Default implementation of the spending orchestrator.
 *
 * <p>Coordinates the interaction between spending strategy, account sequencing,
 * and withdrawal execution. This implementation:
 * <ul>
 *   <li>Uses the strategy to calculate target withdrawal</li>
 *   <li>Sequences accounts according to the provided sequencer</li>
 *   <li>Withdraws from accounts in order until target is met</li>
 *   <li>Handles partial withdrawals when accounts have insufficient balance</li>
 *   <li>Tracks shortfall when total portfolio cannot meet target</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * RmdCalculator rmdCalc = new DefaultRmdCalculator();
 * SpendingOrchestrator orchestrator = new DefaultSpendingOrchestrator(rmdCalc);
 *
 * SpendingPlan plan = orchestrator.execute(portfolio, strategy, context);
 * }</pre>
 */
public class DefaultSpendingOrchestrator implements SpendingOrchestrator {

    private final RmdCalculator rmdCalculator;

    /**
     * Creates a new DefaultSpendingOrchestrator.
     *
     * @param rmdCalculator the RMD calculator for sequencer selection
     * @throws MissingRequiredFieldException if rmdCalculator is null
     */
    public DefaultSpendingOrchestrator(RmdCalculator rmdCalculator) {
        MissingRequiredFieldException.requireNonNull(rmdCalculator, "rmdCalculator");
        this.rmdCalculator = rmdCalculator;
    }

    @Override
    public SpendingPlan execute(
            SpendingStrategy strategy,
            AccountSequencer sequencer,
            SpendingContext context) {

        MissingRequiredFieldException.requireNonNull(strategy, "strategy");
        MissingRequiredFieldException.requireNonNull(sequencer, "sequencer");
        MissingRequiredFieldException.requireNonNull(context, "context");
        MissingRequiredFieldException.requireNonNull(context.simulation(), "context.simulation()");

        // 1. Calculate target withdrawal from strategy
        SpendingPlan strategyPlan = strategy.calculateWithdrawal(context);
        BigDecimal targetWithdrawal = strategyPlan.targetWithdrawal();

        // If no withdrawal needed, return early
        if (targetWithdrawal.compareTo(BigDecimal.ZERO) <= 0) {
            return SpendingPlan.noWithdrawalNeeded(strategy.getName());
        }

        // 2. Sequence accounts (sequencer gets accounts from context.simulation())
        List<AccountSnapshot> orderedAccounts = sequencer.sequence(context);

        // 3. Execute withdrawals from accounts in sequence using Stream reduce
        WithdrawalState finalState = orderedAccounts.stream()
                .filter(AccountSnapshot::hasBalance)
                .reduce(
                        new WithdrawalState(new ArrayList<>(), targetWithdrawal, BigDecimal.ZERO),
                        (state, account) -> state.processAccount(account),
                        (a, b) -> b // Combiner not used in sequential stream
                );

        // 4. Build and return the spending plan
        boolean meetsTarget = finalState.remaining().compareTo(BigDecimal.ZERO) <= 0;
        BigDecimal shortfall = meetsTarget ? BigDecimal.ZERO : finalState.remaining();

        return SpendingPlan.builder()
                .targetWithdrawal(targetWithdrawal)
                .adjustedWithdrawal(finalState.totalWithdrawn())
                .accountWithdrawals(finalState.withdrawals())
                .meetsTarget(meetsTarget)
                .shortfall(shortfall)
                .strategyUsed(strategy.getName())
                .addMetadata("sequencer", sequencer.getName())
                .addMetadata("accountsUsed", finalState.withdrawals().size())
                .build();
    }

    @Override
    public AccountSequencer selectDefaultSequencer(SpendingContext context) {
        if (rmdCalculator.isRmdRequired(context.age(), context.birthYear())) {
            return new RmdFirstSequencer(rmdCalculator);
        }
        return new TaxEfficientSequencer();
    }

    /**
     * Internal state holder for accumulating withdrawals in the stream reduction.
     *
     * @param withdrawals the list of withdrawals accumulated so far
     * @param remaining the remaining amount to withdraw
     * @param totalWithdrawn the total amount withdrawn so far
     */
    private record WithdrawalState(
            List<AccountWithdrawal> withdrawals,
            BigDecimal remaining,
            BigDecimal totalWithdrawn
    ) {
        /**
         * Processes an account snapshot and returns updated state.
         *
         * @param account the account snapshot to potentially withdraw from
         * @return updated state after processing the account
         */
        WithdrawalState processAccount(AccountSnapshot account) {
            // If we've already withdrawn enough, return unchanged state
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                return this;
            }

            BigDecimal accountBalance = account.balance();
            BigDecimal withdrawalAmount = remaining.min(accountBalance);
            BigDecimal newBalance = accountBalance.subtract(withdrawalAmount);

            AccountWithdrawal withdrawal = AccountWithdrawal.builder()
                    .accountSnapshot(account)
                    .amount(withdrawalAmount)
                    .priorBalance(accountBalance)
                    .newBalance(newBalance)
                    .build();

            List<AccountWithdrawal> updatedWithdrawals = new ArrayList<>(withdrawals);
            updatedWithdrawals.add(withdrawal);

            return new WithdrawalState(
                    updatedWithdrawals,
                    remaining.subtract(withdrawalAmount),
                    totalWithdrawn.add(withdrawalAmount)
            );
        }
    }
}
