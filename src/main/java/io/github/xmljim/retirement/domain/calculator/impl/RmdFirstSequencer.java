package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import io.github.xmljim.retirement.domain.calculator.AccountSequencer;
import io.github.xmljim.retirement.domain.calculator.RmdCalculator;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.model.InvestmentAccount;
import io.github.xmljim.retirement.domain.model.Portfolio;
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
 *     List<InvestmentAccount> ordered = sequencer.sequence(portfolio, context);
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
    private final TaxEfficientSequencer fallbackSequencer;

    /**
     * Creates a new RmdFirstSequencer.
     *
     * @param rmdCalculator the RMD calculator for determining RMD-subject accounts
     * @throws MissingRequiredFieldException if rmdCalculator is null
     */
    public RmdFirstSequencer(RmdCalculator rmdCalculator) {
        MissingRequiredFieldException.requireNonNull(rmdCalculator, "rmdCalculator");
        this.rmdCalculator = rmdCalculator;
        this.fallbackSequencer = new TaxEfficientSequencer();
    }

    @Override
    public List<InvestmentAccount> sequence(Portfolio portfolio, SpendingContext context) {
        MissingRequiredFieldException.requireNonNull(portfolio, "portfolio");

        List<InvestmentAccount> accountsWithBalance = portfolio.getAccounts().stream()
                .filter(account -> account.getBalance().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());

        // Separate RMD and non-RMD accounts
        List<InvestmentAccount> rmdAccounts = accountsWithBalance.stream()
                .filter(account -> rmdCalculator.isSubjectToRmd(account.getAccountType()))
                .sorted(Comparator.comparing(InvestmentAccount::getBalance).reversed())
                .collect(Collectors.toList());

        List<InvestmentAccount> nonRmdAccounts = accountsWithBalance.stream()
                .filter(account -> !rmdCalculator.isSubjectToRmd(account.getAccountType()))
                .collect(Collectors.toList());

        // Create result: RMD accounts first, then tax-efficient ordering for rest
        List<InvestmentAccount> result = new ArrayList<>(rmdAccounts);

        // Sort non-RMD accounts using tax-efficient ordering
        if (!nonRmdAccounts.isEmpty()) {
            Portfolio nonRmdPortfolio = createPortfolioSubset(portfolio, nonRmdAccounts);
            List<InvestmentAccount> sortedNonRmd = fallbackSequencer.sequence(nonRmdPortfolio, context);
            result.addAll(sortedNonRmd);
        }

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
     * Creates a temporary portfolio containing only the specified accounts.
     *
     * <p>This is used to delegate non-RMD account sequencing to the
     * TaxEfficientSequencer.
     *
     * @param original the original portfolio
     * @param accounts the accounts to include
     * @return a new portfolio with the subset of accounts
     */
    private Portfolio createPortfolioSubset(Portfolio original, List<InvestmentAccount> accounts) {
        Portfolio.Builder builder = Portfolio.builder().owner(original.getOwner());
        accounts.forEach(builder::addAccount);
        return builder.build();
    }
}
