package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;

import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.model.InvestmentAccount;

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
 * @param account the investment account the withdrawal is from
 * @param amount the withdrawal amount
 * @param priorBalance the account balance before withdrawal
 * @param newBalance the account balance after withdrawal
 * @param taxTreatment the tax treatment of this withdrawal (defaults from account type)
 * @see io.github.xmljim.retirement.domain.value.SpendingPlan
 */
public record AccountWithdrawal(
        InvestmentAccount account,
        BigDecimal amount,
        BigDecimal priorBalance,
        BigDecimal newBalance,
        AccountType.TaxTreatment taxTreatment
) {

    /**
     * Compact constructor with validation.
     */
    public AccountWithdrawal {
        MissingRequiredFieldException.requireNonNull(account, "account");
        amount = amount != null ? amount : BigDecimal.ZERO;
        priorBalance = priorBalance != null ? priorBalance : BigDecimal.ZERO;
        newBalance = newBalance != null ? newBalance : BigDecimal.ZERO;
        taxTreatment = taxTreatment != null ? taxTreatment : account.getAccountType().getTaxTreatment();
    }

    /**
     * Returns the account ID.
     *
     * @return the account's unique identifier
     */
    public String accountId() {
        return account.getId();
    }

    /**
     * Returns the account name.
     *
     * @return the account's display name
     */
    public String accountName() {
        return account.getName();
    }

    /**
     * Returns the account type.
     *
     * @return the type of account
     */
    public AccountType accountType() {
        return account.getAccountType();
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
        private InvestmentAccount account;
        private BigDecimal amount = BigDecimal.ZERO;
        private BigDecimal priorBalance = BigDecimal.ZERO;
        private BigDecimal newBalance = BigDecimal.ZERO;
        private AccountType.TaxTreatment taxTreatment;

        /**
         * Sets the investment account.
         *
         * @param account the investment account
         * @return this builder
         */
        public Builder account(InvestmentAccount account) {
            this.account = account;
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
                    account,
                    amount,
                    priorBalance,
                    newBalance,
                    taxTreatment
            );
        }
    }
}
