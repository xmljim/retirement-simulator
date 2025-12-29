package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import io.github.xmljim.retirement.domain.calculator.AccountSequencer;
import io.github.xmljim.retirement.domain.calculator.RmdCalculator;
import io.github.xmljim.retirement.domain.calculator.SpendingOrchestrator;
import io.github.xmljim.retirement.domain.calculator.SpendingStrategy;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.model.InvestmentAccount;
import io.github.xmljim.retirement.domain.model.Portfolio;
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
            Portfolio portfolio,
            SpendingStrategy strategy,
            AccountSequencer sequencer,
            SpendingContext context) {

        MissingRequiredFieldException.requireNonNull(portfolio, "portfolio");
        MissingRequiredFieldException.requireNonNull(strategy, "strategy");
        MissingRequiredFieldException.requireNonNull(sequencer, "sequencer");
        MissingRequiredFieldException.requireNonNull(context, "context");

        // 1. Calculate target withdrawal from strategy
        SpendingPlan strategyPlan = strategy.calculateWithdrawal(context);
        BigDecimal targetWithdrawal = strategyPlan.targetWithdrawal();

        // If no withdrawal needed, return early
        if (targetWithdrawal.compareTo(BigDecimal.ZERO) <= 0) {
            return SpendingPlan.noWithdrawalNeeded(strategy.getName());
        }

        // 2. Sequence accounts
        List<InvestmentAccount> orderedAccounts = sequencer.sequence(portfolio, context);

        // 3. Execute withdrawals from accounts in sequence
        List<AccountWithdrawal> withdrawals = new ArrayList<>();
        BigDecimal remaining = targetWithdrawal;
        BigDecimal totalWithdrawn = BigDecimal.ZERO;

        for (InvestmentAccount account : orderedAccounts) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal accountBalance = account.getBalance();
            if (accountBalance.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // Withdraw the lesser of remaining need or account balance
            BigDecimal withdrawalAmount = remaining.min(accountBalance);
            BigDecimal newBalance = accountBalance.subtract(withdrawalAmount);

            AccountWithdrawal withdrawal = AccountWithdrawal.builder()
                    .accountId(account.getId())
                    .accountName(account.getName())
                    .accountType(account.getAccountType())
                    .amount(withdrawalAmount)
                    .priorBalance(accountBalance)
                    .newBalance(newBalance)
                    .build();

            withdrawals.add(withdrawal);
            totalWithdrawn = totalWithdrawn.add(withdrawalAmount);
            remaining = remaining.subtract(withdrawalAmount);
        }

        // 4. Build and return the spending plan
        boolean meetsTarget = remaining.compareTo(BigDecimal.ZERO) <= 0;
        BigDecimal shortfall = meetsTarget ? BigDecimal.ZERO : remaining;

        return SpendingPlan.builder()
                .targetWithdrawal(targetWithdrawal)
                .adjustedWithdrawal(totalWithdrawn)
                .accountWithdrawals(withdrawals)
                .meetsTarget(meetsTarget)
                .shortfall(shortfall)
                .strategyUsed(strategy.getName())
                .addMetadata("sequencer", sequencer.getName())
                .addMetadata("accountsUsed", withdrawals.size())
                .build();
    }

    @Override
    public AccountSequencer selectDefaultSequencer(SpendingContext context) {
        if (context.isSubjectToRmd()) {
            return new RmdFirstSequencer(rmdCalculator);
        }
        return new TaxEfficientSequencer();
    }
}
