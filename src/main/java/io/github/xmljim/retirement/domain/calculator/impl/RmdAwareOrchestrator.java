package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
 * Orchestrator that ensures RMD compliance before applying spending strategies.
 *
 * <p>This orchestrator guarantees that Required Minimum Distributions are satisfied,
 * preventing IRS penalties (25% of missed RMD under SECURE 2.0). It:
 * <ol>
 *   <li>Calculates required RMDs for all RMD-subject accounts</li>
 *   <li>Forces RMD withdrawals even if strategy requests less</li>
 *   <li>Applies strategy for any additional withdrawal needed beyond RMD</li>
 *   <li>Tracks RMD vs discretionary withdrawals separately</li>
 * </ol>
 *
 * <h2>RMD Logic</h2>
 * <pre>
 * If (Total RMD >= Strategy Withdrawal):
 *   - Withdraw full RMD (forced)
 *   - Excess RMD available for spending or reinvestment
 *
 * If (Total RMD < Strategy Withdrawal):
 *   - Withdraw full RMD first
 *   - Apply strategy for remaining (need - RMD)
 * </pre>
 *
 * @see SpendingOrchestrator
 * @see RmdCalculator
 */
public class RmdAwareOrchestrator implements SpendingOrchestrator {

    private static final BigDecimal TWELVE = new BigDecimal("12");
    private static final int SCALE = 2;

    private final RmdCalculator rmdCalculator;
    private final SpendingOrchestrator delegate;

    /**
     * Creates an RMD-aware orchestrator.
     *
     * @param rmdCalculator the RMD calculator
     * @param delegate the delegate orchestrator for non-RMD withdrawals
     */
    public RmdAwareOrchestrator(RmdCalculator rmdCalculator, SpendingOrchestrator delegate) {
        MissingRequiredFieldException.requireNonNull(rmdCalculator, "rmdCalculator");
        MissingRequiredFieldException.requireNonNull(delegate, "delegate");
        this.rmdCalculator = rmdCalculator;
        this.delegate = delegate;
    }

    /**
     * Creates an RMD-aware orchestrator with default delegate.
     *
     * @param rmdCalculator the RMD calculator
     */
    public RmdAwareOrchestrator(RmdCalculator rmdCalculator) {
        MissingRequiredFieldException.requireNonNull(rmdCalculator, "rmdCalculator");
        this.rmdCalculator = rmdCalculator;
        this.delegate = new DefaultSpendingOrchestrator(rmdCalculator);
    }

    @Override
    public SpendingPlan execute(
            SpendingStrategy strategy,
            AccountSequencer sequencer,
            SpendingContext context) {

        MissingRequiredFieldException.requireNonNull(strategy, "strategy");
        MissingRequiredFieldException.requireNonNull(sequencer, "sequencer");
        MissingRequiredFieldException.requireNonNull(context, "context");

        // Check if RMDs apply
        if (!rmdCalculator.isRmdRequired(context.age(), context.birthYear())) {
            return delegate.execute(strategy, sequencer, context);
        }

        // Calculate strategy withdrawal target
        SpendingPlan strategyPlan = strategy.calculateWithdrawal(context);
        BigDecimal strategyTarget = strategyPlan.targetWithdrawal();

        // Calculate total RMD requirement (monthly portion)
        RmdRequirement rmdRequirement = calculateRmdRequirement(context);
        BigDecimal monthlyRmd = rmdRequirement.totalMonthlyRmd();

        // Determine effective withdrawal target (max of strategy and RMD)
        BigDecimal effectiveTarget = strategyTarget.max(monthlyRmd);

        // Execute withdrawals with RMD accounts first
        List<AccountSnapshot> accounts = context.simulation().getAccountSnapshots();
        WithdrawalResult result = executeWithdrawals(
                accounts, effectiveTarget, rmdRequirement, context);

        // Build spending plan with RMD metadata
        boolean meetsTarget = result.totalWithdrawn().compareTo(effectiveTarget) >= 0;
        BigDecimal shortfall = meetsTarget ? BigDecimal.ZERO
                : effectiveTarget.subtract(result.totalWithdrawn());

        return SpendingPlan.builder()
                .targetWithdrawal(effectiveTarget)
                .adjustedWithdrawal(result.totalWithdrawn())
                .accountWithdrawals(result.withdrawals())
                .meetsTarget(meetsTarget)
                .shortfall(shortfall)
                .strategyUsed(strategy.getName())
                .addMetadata("sequencer", sequencer.getName())
                .addMetadata("rmdRequired", monthlyRmd.setScale(SCALE, RoundingMode.HALF_UP).toPlainString())
                .addMetadata("strategyTarget", strategyTarget.setScale(SCALE, RoundingMode.HALF_UP).toPlainString())
                .addMetadata("rmdForced", String.valueOf(monthlyRmd.compareTo(strategyTarget) > 0))
                .addMetadata("rmdWithdrawn",
                        result.rmdWithdrawn().setScale(SCALE, RoundingMode.HALF_UP).toPlainString())
                .addMetadata("discretionaryWithdrawn",
                        result.discretionaryWithdrawn().setScale(SCALE, RoundingMode.HALF_UP).toPlainString())
                .build();
    }

    @Override
    public AccountSequencer selectDefaultSequencer(SpendingContext context) {
        // Always use RMD-first when RMDs are required
        if (rmdCalculator.isRmdRequired(context.age(), context.birthYear())) {
            return new RmdFirstSequencer(rmdCalculator);
        }
        return delegate.selectDefaultSequencer(context);
    }

    private RmdRequirement calculateRmdRequirement(SpendingContext context) {
        List<AccountSnapshot> accounts = context.simulation().getAccountSnapshots();
        int age = context.age();

        BigDecimal totalAnnualRmd = BigDecimal.ZERO;
        List<AccountRmd> accountRmds = new ArrayList<>();

        for (AccountSnapshot account : accounts) {
            if (account.subjectToRmd() && account.hasBalance()) {
                BigDecimal annualRmd = rmdCalculator.calculateRmd(account.balance(), age);
                totalAnnualRmd = totalAnnualRmd.add(annualRmd);
                accountRmds.add(new AccountRmd(account, annualRmd));
            }
        }

        BigDecimal monthlyRmd = totalAnnualRmd.divide(TWELVE, SCALE, RoundingMode.HALF_UP);
        return new RmdRequirement(monthlyRmd, totalAnnualRmd, accountRmds);
    }

    private WithdrawalResult executeWithdrawals(
            List<AccountSnapshot> accounts,
            BigDecimal target,
            RmdRequirement rmdRequirement,
            SpendingContext context) {

        List<AccountWithdrawal> withdrawals = new ArrayList<>();
        BigDecimal remaining = target;
        BigDecimal totalWithdrawn = BigDecimal.ZERO;
        BigDecimal rmdWithdrawn = BigDecimal.ZERO;
        BigDecimal discretionaryWithdrawn = BigDecimal.ZERO;

        // First, withdraw from RMD accounts (up to their monthly RMD portion)
        for (AccountRmd accountRmd : rmdRequirement.accountRmds()) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            AccountSnapshot account = accountRmd.account();
            BigDecimal monthlyRmd = accountRmd.annualRmd().divide(TWELVE, SCALE, RoundingMode.HALF_UP);
            BigDecimal withdrawAmount = remaining.min(monthlyRmd).min(account.balance());

            if (withdrawAmount.compareTo(BigDecimal.ZERO) > 0) {
                AccountWithdrawal withdrawal = createWithdrawal(account, withdrawAmount);
                withdrawals.add(withdrawal);
                remaining = remaining.subtract(withdrawAmount);
                totalWithdrawn = totalWithdrawn.add(withdrawAmount);
                rmdWithdrawn = rmdWithdrawn.add(withdrawAmount);
            }
        }

        // If more needed, withdraw from remaining accounts (tax-efficient order)
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            List<AccountSnapshot> nonRmdAccounts = accounts.stream()
                    .filter(a -> !a.subjectToRmd() && a.hasBalance())
                    .sorted((a, b) -> {
                        // Tax-efficient: Roth last
                        int taxOrder = Integer.compare(
                                getTaxPriority(a), getTaxPriority(b));
                        if (taxOrder != 0) {
                            return taxOrder;
                        }
                        return b.balance().compareTo(a.balance());
                    })
                    .toList();

            for (AccountSnapshot account : nonRmdAccounts) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }

                BigDecimal withdrawAmount = remaining.min(account.balance());
                if (withdrawAmount.compareTo(BigDecimal.ZERO) > 0) {
                    AccountWithdrawal withdrawal = createWithdrawal(account, withdrawAmount);
                    withdrawals.add(withdrawal);
                    remaining = remaining.subtract(withdrawAmount);
                    totalWithdrawn = totalWithdrawn.add(withdrawAmount);
                    discretionaryWithdrawn = discretionaryWithdrawn.add(withdrawAmount);
                }
            }
        }

        return new WithdrawalResult(withdrawals, totalWithdrawn, rmdWithdrawn, discretionaryWithdrawn);
    }

    private AccountWithdrawal createWithdrawal(AccountSnapshot account, BigDecimal amount) {
        return AccountWithdrawal.builder()
                .accountSnapshot(account)
                .amount(amount)
                .priorBalance(account.balance())
                .newBalance(account.balance().subtract(amount))
                .build();
    }

    private int getTaxPriority(AccountSnapshot account) {
        return switch (account.taxTreatment()) {
            case PRE_TAX -> 1;      // Traditional - withdraw early
            case TAXABLE -> 2;       // Brokerage
            case ROTH -> 3;          // Roth - preserve tax-free growth
            case HSA -> 4;           // HSA - preserve for healthcare
        };
    }

    private record RmdRequirement(
            BigDecimal totalMonthlyRmd,
            BigDecimal totalAnnualRmd,
            List<AccountRmd> accountRmds
    ) {}

    private record AccountRmd(
            AccountSnapshot account,
            BigDecimal annualRmd
    ) {}

    private record WithdrawalResult(
            List<AccountWithdrawal> withdrawals,
            BigDecimal totalWithdrawn,
            BigDecimal rmdWithdrawn,
            BigDecimal discretionaryWithdrawn
    ) {}
}
