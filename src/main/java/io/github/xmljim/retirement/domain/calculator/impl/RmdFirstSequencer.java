package io.github.xmljim.retirement.domain.calculator.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import io.github.xmljim.retirement.domain.calculator.AccountSequencer;
import io.github.xmljim.retirement.domain.calculator.RmdCalculator;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.value.AccountSnapshot;
import io.github.xmljim.retirement.domain.value.SpendingContext;

/**
 * Account sequencer that prioritizes RMD-required accounts first.
 *
 * <p>Orders accounts for withdrawal in the following sequence:
 * <ol>
 *   <li><b>RMD accounts first</b> (Traditional IRA, 401k) - sorted by balance
 *       descending to ensure larger RMD obligations are met first</li>
 *   <li><b>Tax-efficient order</b> for remaining accounts (Taxable → Roth → HSA)</li>
 * </ol>
 *
 * <p>This sequencer is the default choice when the account holder has reached
 * RMD age (73 or 75 depending on birth year per SECURE 2.0). It ensures
 * compliance with IRS required distributions while still maintaining
 * tax efficiency for non-RMD accounts.
 *
 * <p>Usage:
 * <pre>{@code
 * RmdCalculator rmdCalc = new DefaultRmdCalculator();
 * AccountSequencer sequencer = new RmdFirstSequencer(rmdCalc);
 *
 * if (context.isSubjectToRmd()) {
 *     List<AccountSnapshot> ordered = sequencer.sequence(context);
 *     // RMD accounts will appear first in the list
 * }
 * }</pre>
 *
 * @see RmdCalculator
 * @see TaxEfficientSequencer
 */
public class RmdFirstSequencer implements AccountSequencer {

    private static final String NAME = "RMD-First";
    private static final String DESCRIPTION =
            "Prioritizes accounts subject to Required Minimum Distributions, "
                    + "then follows tax-efficient ordering for remaining accounts.";

    private final RmdCalculator rmdCalculator;

    /**
     * Creates a new RmdFirstSequencer.
     *
     * @param rmdCalculator the RMD calculator for RMD amount calculations
     * @throws MissingRequiredFieldException if rmdCalculator is null
     */
    public RmdFirstSequencer(RmdCalculator rmdCalculator) {
        MissingRequiredFieldException.requireNonNull(rmdCalculator, "rmdCalculator");
        this.rmdCalculator = rmdCalculator;
    }

    @Override
    public List<AccountSnapshot> sequence(SpendingContext context) {
        MissingRequiredFieldException.requireNonNull(context, "context");
        MissingRequiredFieldException.requireNonNull(context.simulation(), "context.simulation()");

        List<AccountSnapshot> accountsWithBalance = context.simulation().getAccountSnapshots().stream()
                .filter(AccountSnapshot::hasBalance)
                .collect(Collectors.toList());

        // Separate RMD and non-RMD accounts
        List<AccountSnapshot> rmdAccounts = accountsWithBalance.stream()
                .filter(AccountSnapshot::subjectToRmd)
                .sorted(Comparator.comparing(AccountSnapshot::balance).reversed())
                .collect(Collectors.toList());

        List<AccountSnapshot> nonRmdAccounts = accountsWithBalance.stream()
                .filter(account -> !account.subjectToRmd())
                .sorted(taxEfficientComparator())
                .collect(Collectors.toList());

        // Create result: RMD accounts first, then tax-efficient ordering for rest
        List<AccountSnapshot> result = new ArrayList<>(rmdAccounts);
        result.addAll(nonRmdAccounts);

        return result;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public boolean isRmdAware() {
        return true;
    }

    @Override
    public boolean isTaxAware() {
        return true;
    }

    /**
     * Returns the RMD calculator used by this sequencer.
     *
     * @return the RMD calculator
     */
    public RmdCalculator getRmdCalculator() {
        return rmdCalculator;
    }

    /**
     * Creates a comparator for tax-efficient ordering of non-RMD accounts.
     *
     * <p>Orders by tax treatment priority (TAXABLE, PRE_TAX, ROTH, HSA),
     * then by balance ascending.
     *
     * @return the comparator
     */
    private Comparator<AccountSnapshot> taxEfficientComparator() {
        return Comparator
                .comparingInt(this::getTaxTreatmentPriority)
                .thenComparing(AccountSnapshot::balance);
    }

    /**
     * Returns the withdrawal priority for a tax treatment.
     */
    private int getTaxTreatmentPriority(AccountSnapshot account) {
        AccountType.TaxTreatment treatment = account.taxTreatment();
        return switch (treatment) {
            case TAXABLE -> 1;
            case PRE_TAX -> 2;
            case ROTH -> 3;
            case HSA -> 4;
        };
    }
}
