package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;

import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.model.InvestmentAccount;

/**
 * Immutable snapshot of an account's state at a point in time.
 *
 * <p>This record provides read-only access to account information needed by
 * spending strategies and account sequencers. It serves as the boundary between
 * the simulation engine (which owns mutable account state) and the strategy layer
 * (which needs to query account information).
 *
 * <p>AccountSnapshot contains all information needed for withdrawal calculations:
 * <ul>
 *   <li>Account identification (id, name, type)</li>
 *   <li>Current balance</li>
 *   <li>Tax treatment for withdrawal sequencing</li>
 *   <li>RMD eligibility for RMD-first sequencing</li>
 *   <li>Asset allocation for bucket strategies</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <p>Create from an InvestmentAccount using the factory method:
 * <pre>{@code
 * InvestmentAccount account = ...;
 * AccountSnapshot snapshot = AccountSnapshot.from(account);
 * }</pre>
 *
 * <p>Access via SimulationView in strategies:
 * <pre>{@code
 * List<AccountSnapshot> accounts = context.simulation().getAccountSnapshots();
 * for (AccountSnapshot account : accounts) {
 *     if (account.subjectToRmd()) {
 *         // Handle RMD account
 *     }
 * }
 * }</pre>
 *
 * @param accountId unique identifier for the account
 * @param accountName human-readable account name
 * @param accountType the type of account (determines tax treatment, RMD rules)
 * @param balance current account balance
 * @param taxTreatment tax treatment for withdrawals
 * @param subjectToRmd whether account is subject to Required Minimum Distributions
 * @param allocation asset allocation (stocks/bonds/cash)
 *
 * @see io.github.xmljim.retirement.domain.calculator.SimulationView
 * @see io.github.xmljim.retirement.domain.calculator.AccountSequencer
 */
public record AccountSnapshot(
        String accountId,
        String accountName,
        AccountType accountType,
        BigDecimal balance,
        AccountType.TaxTreatment taxTreatment,
        boolean subjectToRmd,
        AssetAllocation allocation
) {

    /**
     * Compact constructor with validation.
     */
    public AccountSnapshot {
        MissingRequiredFieldException.requireNonNull(accountId, "accountId");
        MissingRequiredFieldException.requireNonNull(accountName, "accountName");
        MissingRequiredFieldException.requireNonNull(accountType, "accountType");
        MissingRequiredFieldException.requireNonNull(balance, "balance");
        MissingRequiredFieldException.requireNonNull(taxTreatment, "taxTreatment");
        MissingRequiredFieldException.requireNonNull(allocation, "allocation");
    }

    /**
     * Creates an AccountSnapshot from an InvestmentAccount.
     *
     * <p>This is the primary way to create snapshots. The snapshot captures
     * the account's current state at the moment of creation.
     *
     * @param account the investment account to snapshot
     * @return a new AccountSnapshot with the account's current state
     * @throws MissingRequiredFieldException if account is null
     */
    public static AccountSnapshot from(InvestmentAccount account) {
        MissingRequiredFieldException.requireNonNull(account, "account");

        return new AccountSnapshot(
                account.getId(),
                account.getName(),
                account.getAccountType(),
                account.getBalance(),
                account.getAccountType().getTaxTreatment(),
                account.getAccountType().isSubjectToRmd(),
                account.getAllocation()
        );
    }

    /**
     * Returns whether this account has a positive balance.
     *
     * @return true if balance is greater than zero
     */
    public boolean hasBalance() {
        return balance.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Returns whether withdrawals from this account are taxable.
     *
     * <p>Pre-tax accounts (Traditional 401k, Traditional IRA) have taxable
     * withdrawals. Roth and HSA (for qualified expenses) do not.
     *
     * @return true if withdrawals are taxable as ordinary income
     */
    public boolean isTaxable() {
        return taxTreatment == AccountType.TaxTreatment.PRE_TAX;
    }

    /**
     * Returns whether this is an employer-sponsored account.
     *
     * @return true if employer-sponsored (401k, 403b, 457b)
     */
    public boolean isEmployerSponsored() {
        return accountType.isEmployerSponsored();
    }
}
