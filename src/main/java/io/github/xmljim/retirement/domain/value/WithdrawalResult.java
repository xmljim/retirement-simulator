package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;

import io.github.xmljim.retirement.domain.enums.AccountType;

/**
 * Result of a withdrawal calculation containing amounts and tax information.
 *
 * <p>This record captures the complete result of calculating a withdrawal
 * from an investment account, including:
 * <ul>
 *   <li>Actual withdrawal amount (may be less than requested)</li>
 *   <li>Tax treatment information for reporting</li>
 *   <li>Account balance after withdrawal</li>
 *   <li>Partial withdrawal and depletion flags</li>
 * </ul>
 *
 * <p>All monetary amounts are in dollars.
 *
 * @param withdrawalAmount the actual amount withdrawn (may be less than requested)
 * @param requestedAmount the originally requested withdrawal amount
 * @param accountId the source account identifier
 * @param accountType the type of account for tax treatment
 * @param taxTreatment the tax treatment of the withdrawal
 * @param isTaxable true if this withdrawal is subject to income tax
 * @param newBalance the account balance after the withdrawal
 * @param isPartialWithdrawal true if only part of the requested amount was available
 * @param isDepleted true if the account balance is now zero
 */
public record WithdrawalResult(
    BigDecimal withdrawalAmount,
    BigDecimal requestedAmount,
    String accountId,
    AccountType accountType,
    AccountType.TaxTreatment taxTreatment,
    boolean isTaxable,
    BigDecimal newBalance,
    boolean isPartialWithdrawal,
    boolean isDepleted
) {

    /**
     * Creates a WithdrawalResult for a successful full withdrawal.
     *
     * @param withdrawalAmount the amount withdrawn
     * @param accountId the source account ID
     * @param accountType the account type
     * @param newBalance the balance after withdrawal
     * @return a new WithdrawalResult
     */
    public static WithdrawalResult full(
            BigDecimal withdrawalAmount,
            String accountId,
            AccountType accountType,
            BigDecimal newBalance) {

        boolean isDepleted = newBalance.compareTo(BigDecimal.ZERO) == 0;
        boolean isTaxable = determineTaxability(accountType.getTaxTreatment());

        return new WithdrawalResult(
            withdrawalAmount,
            withdrawalAmount,
            accountId,
            accountType,
            accountType.getTaxTreatment(),
            isTaxable,
            newBalance,
            false,
            isDepleted
        );
    }

    /**
     * Creates a WithdrawalResult for a partial withdrawal (insufficient funds).
     *
     * @param actualAmount the amount actually withdrawn
     * @param requestedAmount the originally requested amount
     * @param accountId the source account ID
     * @param accountType the account type
     * @return a new WithdrawalResult with zero balance and depletion flag
     */
    public static WithdrawalResult partial(
            BigDecimal actualAmount,
            BigDecimal requestedAmount,
            String accountId,
            AccountType accountType) {

        boolean isTaxable = determineTaxability(accountType.getTaxTreatment());

        return new WithdrawalResult(
            actualAmount,
            requestedAmount,
            accountId,
            accountType,
            accountType.getTaxTreatment(),
            isTaxable,
            BigDecimal.ZERO,
            true,
            true
        );
    }

    /**
     * Creates a zero withdrawal result (e.g., account already depleted).
     *
     * @param requestedAmount the amount that was requested
     * @param accountId the source account ID
     * @param accountType the account type
     * @return a WithdrawalResult with zero withdrawal
     */
    public static WithdrawalResult zero(
            BigDecimal requestedAmount,
            String accountId,
            AccountType accountType) {

        return new WithdrawalResult(
            BigDecimal.ZERO,
            requestedAmount,
            accountId,
            accountType,
            accountType.getTaxTreatment(),
            false,
            BigDecimal.ZERO,
            true,
            true
        );
    }

    /**
     * Returns true if there was any withdrawal amount.
     *
     * @return true if withdrawalAmount is greater than zero
     */
    public boolean hasWithdrawal() {
        return withdrawalAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Returns the shortfall amount if this was a partial withdrawal.
     *
     * @return the difference between requested and actual, or zero if full withdrawal
     */
    public BigDecimal getShortfall() {
        return requestedAmount.subtract(withdrawalAmount).max(BigDecimal.ZERO);
    }

    /**
     * Determines if a withdrawal from an account with the given tax treatment is taxable.
     *
     * <p>Tax treatment rules:
     * <ul>
     *   <li>PRE_TAX: Withdrawals are taxable as ordinary income</li>
     *   <li>ROTH: Qualified withdrawals are tax-free</li>
     *   <li>HSA: Withdrawals for qualified medical expenses are tax-free</li>
     *   <li>TAXABLE: Only gains are taxable (simplified to false for basic calculation)</li>
     * </ul>
     *
     * @param taxTreatment the account's tax treatment
     * @return true if withdrawals are generally taxable
     */
    private static boolean determineTaxability(AccountType.TaxTreatment taxTreatment) {
        return taxTreatment == AccountType.TaxTreatment.PRE_TAX;
    }
}
