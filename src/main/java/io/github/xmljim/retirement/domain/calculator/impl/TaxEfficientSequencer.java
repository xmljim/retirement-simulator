package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import io.github.xmljim.retirement.domain.calculator.AccountSequencer;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.model.InvestmentAccount;
import io.github.xmljim.retirement.domain.model.Portfolio;
import io.github.xmljim.retirement.domain.value.SpendingContext;

/**
 * Account sequencer implementing the tax-efficient withdrawal order.
 *
 * <p>Orders accounts for withdrawal in the following sequence:
 * <ol>
 *   <li><b>Taxable accounts</b> (brokerage) - withdraw first to minimize
 *       ongoing dividend/capital gains taxes</li>
 *   <li><b>Pre-tax accounts</b> (Traditional IRA, 401k) - delay tax
 *       until required by RMDs</li>
 *   <li><b>Roth accounts</b> (Roth IRA, Roth 401k) - withdraw last to
 *       maximize tax-free compounding</li>
 *   <li><b>HSA</b> - preserve for qualified medical expenses</li>
 * </ol>
 *
 * <p>Within each tax category, accounts are sorted by balance (smallest first)
 * to consolidate accounts and simplify portfolio management.
 *
 * <p>This is the conventional wisdom for retirement withdrawals, though
 * tax bracket management may warrant different ordering in specific situations.
 *
 * <p>Usage:
 * <pre>{@code
 * AccountSequencer sequencer = new TaxEfficientSequencer();
 * List<InvestmentAccount> ordered = sequencer.sequence(portfolio, context);
 * // Process accounts in order
 * }</pre>
 *
 * @see <a href="docs/research/TAX_SEQUENCING_RESEARCH.md">Tax Sequencing Research</a>
 */
public class TaxEfficientSequencer implements AccountSequencer {

    private static final String NAME = "Tax-Efficient";
    private static final String DESCRIPTION =
            "Withdraws from taxable accounts first, then pre-tax, then Roth. "
                    + "Optimizes for tax-deferred growth.";

    /**
     * Creates a new TaxEfficientSequencer.
     */
    public TaxEfficientSequencer() {
        // Default constructor
    }

    @Override
    public List<InvestmentAccount> sequence(Portfolio portfolio, SpendingContext context) {
        MissingRequiredFieldException.requireNonNull(portfolio, "portfolio");

        return portfolio.getAccounts().stream()
                .filter(account -> account.getBalance().compareTo(BigDecimal.ZERO) > 0)
                .sorted(taxEfficientComparator())
                .collect(Collectors.toList());
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
    public boolean isTaxAware() {
        return true;
    }

    /**
     * Creates a comparator for tax-efficient ordering.
     *
     * <p>Primary sort: Tax treatment priority (TAXABLE, PRE_TAX, ROTH, HSA)
     * <p>Secondary sort: Balance ascending (deplete smaller accounts first)
     *
     * @return the comparator
     */
    private Comparator<InvestmentAccount> taxEfficientComparator() {
        return Comparator
                .comparingInt(this::getTaxTreatmentPriority)
                .thenComparing(InvestmentAccount::getBalance);
    }

    /**
     * Returns the withdrawal priority for a tax treatment.
     *
     * <p>Lower numbers = withdraw first:
     * <ul>
     *   <li>1 = TAXABLE (brokerage)</li>
     *   <li>2 = PRE_TAX (Traditional IRA/401k)</li>
     *   <li>3 = ROTH (Roth IRA/401k)</li>
     *   <li>4 = HSA (preserve for medical)</li>
     * </ul>
     *
     * @param account the investment account
     * @return the priority number
     */
    private int getTaxTreatmentPriority(InvestmentAccount account) {
        AccountType.TaxTreatment treatment = account.getAccountType().getTaxTreatment();
        return switch (treatment) {
            case TAXABLE -> 1;
            case PRE_TAX -> 2;
            case ROTH -> 3;
            case HSA -> 4;
        };
    }
}
