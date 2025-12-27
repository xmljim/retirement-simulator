package io.github.xmljim.retirement.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import io.github.xmljim.retirement.domain.annotation.Generated;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.enums.TransactionType;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;

/**
 * Represents a monthly transaction in a retirement portfolio simulation.
 *
 * <p>A Transaction captures all financial activity for an account during
 * a single month, including:
 * <ul>
 *   <li>Starting balance (from previous transaction or initial balance)</li>
 *   <li>Personal and employer contributions</li>
 *   <li>Investment returns for the period</li>
 *   <li>Withdrawals</li>
 *   <li>Ending balance (calculated)</li>
 * </ul>
 *
 * <p>Transactions support chaining where each transaction's end balance
 * becomes the next transaction's start balance. Use the {@link Builder}
 * to create instances.
 *
 * <p>Balance calculation:
 * <pre>
 * endBalance = startBalance + personalContribution + employerContribution
 *            + investmentReturn - withdrawal
 * </pre>
 */
public final class Transaction {

    private static final int DOLLAR_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private final String id;
    private final String accountId;
    private final AccountType accountType;
    private final YearMonth period;
    private final TransactionType transactionType;

    private final BigDecimal startBalance;
    private final BigDecimal personalContribution;
    private final BigDecimal employerContribution;
    private final BigDecimal investmentReturn;
    private final BigDecimal withdrawal;
    private final BigDecimal endBalance;

    private final Transaction previous;

    private Transaction(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.accountId = builder.accountId;
        this.accountType = builder.accountType;
        this.period = builder.period;
        this.transactionType = builder.transactionType;
        this.startBalance = scale(builder.startBalance);
        this.personalContribution = scale(builder.personalContribution);
        this.employerContribution = scale(builder.employerContribution);
        this.investmentReturn = scale(builder.investmentReturn);
        this.withdrawal = scale(builder.withdrawal);
        this.endBalance = calculateEndBalance();
        this.previous = builder.previous;
    }

    private BigDecimal scale(BigDecimal value) {
        return value != null ? value.setScale(DOLLAR_SCALE, ROUNDING_MODE) : BigDecimal.ZERO;
    }

    private BigDecimal calculateEndBalance() {
        return startBalance
            .add(personalContribution)
            .add(employerContribution)
            .add(investmentReturn)
            .subtract(withdrawal)
            .setScale(DOLLAR_SCALE, ROUNDING_MODE);
    }

    /**
     * Returns the unique transaction identifier.
     *
     * @return the transaction ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the account identifier this transaction belongs to.
     *
     * @return the account ID
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Returns the account type.
     *
     * @return the account type
     */
    public AccountType getAccountType() {
        return accountType;
    }

    /**
     * Returns the month this transaction covers.
     *
     * @return the transaction period as YearMonth
     */
    public YearMonth getPeriod() {
        return period;
    }

    /**
     * Returns the transaction type (CONTRIBUTION or WITHDRAWAL).
     *
     * @return the transaction type
     */
    public TransactionType getTransactionType() {
        return transactionType;
    }

    /**
     * Returns the balance at the start of this period.
     *
     * @return the starting balance
     */
    public BigDecimal getStartBalance() {
        return startBalance;
    }

    /**
     * Returns the personal (employee) contribution amount.
     *
     * @return the personal contribution
     */
    public BigDecimal getPersonalContribution() {
        return personalContribution;
    }

    /**
     * Returns the employer contribution/match amount.
     *
     * @return the employer contribution
     */
    public BigDecimal getEmployerContribution() {
        return employerContribution;
    }

    /**
     * Returns the total contribution (personal + employer).
     *
     * @return the total contribution
     */
    public BigDecimal getTotalContribution() {
        return personalContribution.add(employerContribution);
    }

    /**
     * Returns the investment return for this period.
     *
     * @return the investment return amount
     */
    public BigDecimal getInvestmentReturn() {
        return investmentReturn;
    }

    /**
     * Returns the withdrawal amount for this period.
     *
     * @return the withdrawal amount
     */
    public BigDecimal getWithdrawal() {
        return withdrawal;
    }

    /**
     * Returns the balance at the end of this period.
     *
     * <p>Calculated as: startBalance + contributions + investmentReturn - withdrawal
     *
     * @return the ending balance
     */
    public BigDecimal getEndBalance() {
        return endBalance;
    }

    /**
     * Returns the previous transaction in the chain, if any.
     *
     * @return Optional containing the previous transaction
     */
    public Optional<Transaction> getPrevious() {
        return Optional.ofNullable(previous);
    }

    /**
     * Returns true if this is the first transaction in the chain.
     *
     * @return true if no previous transaction exists
     */
    public boolean isFirst() {
        return previous == null;
    }

    /**
     * Returns the net change in balance for this period.
     *
     * @return endBalance - startBalance
     */
    public BigDecimal getNetChange() {
        return endBalance.subtract(startBalance);
    }

    /**
     * Creates a new builder for Transaction.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder for the next transaction in the chain.
     *
     * <p>The new transaction will:
     * <ul>
     *   <li>Use this transaction's end balance as start balance</li>
     *   <li>Link to this transaction as previous</li>
     *   <li>Inherit account ID and type</li>
     * </ul>
     *
     * @param nextPeriod the period for the next transaction
     * @return a builder pre-configured for chaining
     */
    public Builder nextTransaction(YearMonth nextPeriod) {
        return new Builder()
            .accountId(this.accountId)
            .accountType(this.accountType)
            .period(nextPeriod)
            .startBalance(this.endBalance)
            .previous(this);
    }

    @Generated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id);
    }

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Generated
    @Override
    public String toString() {
        return "Transaction{" +
            "id='" + id + '\'' +
            ", accountId='" + accountId + '\'' +
            ", period=" + period +
            ", type=" + transactionType +
            ", start=" + startBalance +
            ", end=" + endBalance +
            '}';
    }

    /**
     * Builder for creating Transaction instances.
     */
    public static class Builder {

        private String id;
        private String accountId;
        private AccountType accountType;
        private YearMonth period;
        private TransactionType transactionType;
        private BigDecimal startBalance = BigDecimal.ZERO;
        private BigDecimal personalContribution = BigDecimal.ZERO;
        private BigDecimal employerContribution = BigDecimal.ZERO;
        private BigDecimal investmentReturn = BigDecimal.ZERO;
        private BigDecimal withdrawal = BigDecimal.ZERO;
        private Transaction previous;

        /**
         * Sets the transaction ID. If not set, a UUID will be generated.
         *
         * @param id the transaction ID
         * @return this builder
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

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
         * Configures from an InvestmentAccount.
         *
         * @param account the investment account
         * @return this builder
         */
        public Builder forAccount(InvestmentAccount account) {
            this.accountId = account.getId();
            this.accountType = account.getAccountType();
            if (this.startBalance.compareTo(BigDecimal.ZERO) == 0) {
                this.startBalance = account.getBalance();
            }
            return this;
        }

        /**
         * Sets the transaction period.
         *
         * @param period the year-month
         * @return this builder
         */
        public Builder period(YearMonth period) {
            this.period = period;
            return this;
        }

        /**
         * Sets the transaction type.
         *
         * @param transactionType the transaction type
         * @return this builder
         */
        public Builder transactionType(TransactionType transactionType) {
            this.transactionType = transactionType;
            return this;
        }

        /**
         * Sets the starting balance.
         *
         * @param startBalance the start balance
         * @return this builder
         */
        public Builder startBalance(BigDecimal startBalance) {
            this.startBalance = startBalance;
            return this;
        }

        /**
         * Sets the personal contribution.
         *
         * @param personalContribution the personal contribution
         * @return this builder
         */
        public Builder personalContribution(BigDecimal personalContribution) {
            this.personalContribution = personalContribution;
            return this;
        }

        /**
         * Sets the employer contribution.
         *
         * @param employerContribution the employer contribution
         * @return this builder
         */
        public Builder employerContribution(BigDecimal employerContribution) {
            this.employerContribution = employerContribution;
            return this;
        }

        /**
         * Sets the investment return.
         *
         * @param investmentReturn the investment return
         * @return this builder
         */
        public Builder investmentReturn(BigDecimal investmentReturn) {
            this.investmentReturn = investmentReturn;
            return this;
        }

        /**
         * Sets the withdrawal amount.
         *
         * @param withdrawal the withdrawal amount
         * @return this builder
         */
        public Builder withdrawal(BigDecimal withdrawal) {
            this.withdrawal = withdrawal;
            return this;
        }

        /**
         * Sets the previous transaction for chaining.
         *
         * @param previous the previous transaction
         * @return this builder
         */
        public Builder previous(Transaction previous) {
            this.previous = previous;
            return this;
        }

        /**
         * Builds the Transaction instance.
         *
         * @return a new Transaction
         * @throws MissingRequiredFieldException if required fields are missing
         */
        public Transaction build() {
            validate();
            return new Transaction(this);
        }

        private void validate() {
            MissingRequiredFieldException.requireNonNull(accountId, "accountId");
            MissingRequiredFieldException.requireNonNull(accountType, "accountType");
            MissingRequiredFieldException.requireNonNull(period, "period");
            MissingRequiredFieldException.requireNonNull(transactionType, "transactionType");
        }
    }
}
