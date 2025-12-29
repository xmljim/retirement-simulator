package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;

import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;

/**
 * Represents a withdrawal from a single investment account.
 *
 * <p>This record captures the details of a withdrawal from one account as part
 * of a larger spending plan. It tracks both the amount withdrawn and the
 * resulting balance changes.
 *
 * <p>Tax treatment is included to support tax-efficient withdrawal sequencing
 * and tax liability calculations.
 *
 * @param accountId the unique identifier of the account
 * @param accountName the display name of the account
 * @param accountType the type of account (401K, IRA, ROTH_IRA, etc.)
 * @param amount the withdrawal amount
 * @param priorBalance the account balance before withdrawal
 * @param newBalance the account balance after withdrawal
 * @param taxTreatment the tax treatment of this withdrawal
 * @see io.github.xmljim.retirement.domain.value.SpendingPlan
 */
public record AccountWithdrawal(
        String accountId,
        String accountName,
        AccountType accountType,
        BigDecimal amount,
        BigDecimal priorBalance,
        BigDecimal newBalance,
        AccountType.TaxTreatment taxTreatment
) {

    /**
     * Compact constructor with validation.
     */
    public AccountWithdrawal {
        MissingRequiredFieldException.requireNonNull(accountId, "accountId");
        MissingRequiredFieldException.requireNonNull(accountType, "accountType");
        amount = amount != null ? amount : BigDecimal.ZERO;
        priorBalance = priorBalance != null ? priorBalance : BigDecimal.ZERO;
        newBalance = newBalance != null ? newBalance : BigDecimal.ZERO;
        taxTreatment = taxTreatment != null ? taxTreatment : accountType.getTaxTreatment();
    }

    /**
     * Returns whether this withdrawal is taxable as ordinary income.
     *
     * <p>Withdrawals from PRE_TAX accounts (Traditional IRA, 401k) are
     * taxable as ordinary income. ROTH and HSA qualified withdrawals
     * are tax-free.
     *
     * @return true if the withdrawal is taxable
     */
    public boolean isTaxable() {
        return taxTreatment == AccountType.TaxTreatment.PRE_TAX;
    }

    /**
     * Returns whether the account was depleted by this withdrawal.
     *
     * @return true if the new balance is zero
     */
    public boolean isDepleted() {
        return newBalance.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Returns whether this was a partial withdrawal (less than requested).
     *
     * <p>This can occur when the account balance is less than the
     * requested withdrawal amount.
     *
     * @return true if newBalance is zero and priorBalance was less than amount
     */
    public boolean isPartial() {
        return isDepleted() && priorBalance.compareTo(amount) < 0;
    }

    /**
     * Creates a builder for AccountWithdrawal.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating AccountWithdrawal instances.
     */
    public static class Builder {
        private String accountId;
        private String accountName;
        private AccountType accountType;
        private BigDecimal amount = BigDecimal.ZERO;
        private BigDecimal priorBalance = BigDecimal.ZERO;
        private BigDecimal newBalance = BigDecimal.ZERO;
        private AccountType.TaxTreatment taxTreatment;

        /**
         * Sets the account ID.
         *
         * @param accountId the account ID
         * @return this builder
         */
        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        /**
         * Sets the account name.
         *
         * @param accountName the account name
         * @return this builder
         */
        public Builder accountName(String accountName) {
            this.accountName = accountName;
            return this;
        }

        /**
         * Sets the account type.
         *
         * @param accountType the account type
         * @return this builder
         */
        public Builder accountType(AccountType accountType) {
            this.accountType = accountType;
            return this;
        }

        /**
         * Sets the withdrawal amount.
         *
         * @param amount the amount
         * @return this builder
         */
        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        /**
         * Sets the prior balance.
         *
         * @param priorBalance the balance before withdrawal
         * @return this builder
         */
        public Builder priorBalance(BigDecimal priorBalance) {
            this.priorBalance = priorBalance;
            return this;
        }

        /**
         * Sets the new balance.
         *
         * @param newBalance the balance after withdrawal
         * @return this builder
         */
        public Builder newBalance(BigDecimal newBalance) {
            this.newBalance = newBalance;
            return this;
        }

        /**
         * Sets the tax treatment.
         *
         * @param taxTreatment the tax treatment
         * @return this builder
         */
        public Builder taxTreatment(AccountType.TaxTreatment taxTreatment) {
            this.taxTreatment = taxTreatment;
            return this;
        }

        /**
         * Builds the AccountWithdrawal instance.
         *
         * @return a new AccountWithdrawal
         */
        public AccountWithdrawal build() {
            return new AccountWithdrawal(
                    accountId,
                    accountName,
                    accountType,
                    amount,
                    priorBalance,
                    newBalance,
                    taxTreatment
            );
        }
    }
}
