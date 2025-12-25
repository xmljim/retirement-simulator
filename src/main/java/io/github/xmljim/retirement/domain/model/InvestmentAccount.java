package io.github.xmljim.retirement.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

import io.github.xmljim.retirement.domain.annotation.Generated;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.exception.InvalidRateException;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;
import io.github.xmljim.retirement.domain.value.AssetAllocation;

/**
 * Represents an individual investment account within a portfolio.
 *
 * <p>An InvestmentAccount contains information about a specific investment
 * account including its type, balance, asset allocation, and expected returns.
 * The account can either use explicit return rates or calculate them based
 * on asset allocation.
 *
 * <p>Use the {@link Builder} to create instances:
 * <pre>{@code
 * InvestmentAccount account = InvestmentAccount.builder()
 *     .name("401(k)")
 *     .accountType(AccountType.TRADITIONAL_401K)
 *     .balance(new BigDecimal("250000"))
 *     .allocation(AssetAllocation.of(70, 25, 5))
 *     .preRetirementReturnRate(new BigDecimal("0.07"))
 *     .postRetirementReturnRate(new BigDecimal("0.05"))
 *     .build();
 * }</pre>
 */
public final class InvestmentAccount {

    /**
     * Default expected annual return for stocks (7%).
     */
    public static final BigDecimal DEFAULT_STOCK_RETURN = new BigDecimal("0.07");

    /**
     * Default expected annual return for bonds (4%).
     */
    public static final BigDecimal DEFAULT_BOND_RETURN = new BigDecimal("0.04");

    /**
     * Default expected annual return for cash (2%).
     */
    public static final BigDecimal DEFAULT_CASH_RETURN = new BigDecimal("0.02");

    private static final BigDecimal MIN_RETURN = new BigDecimal("-0.50");
    private static final BigDecimal MAX_RETURN = new BigDecimal("0.50");

    private final String id;
    private final String name;
    private final AccountType accountType;
    private final BigDecimal balance;
    private final AssetAllocation allocation;
    private final BigDecimal preRetirementReturnRate;
    private final BigDecimal postRetirementReturnRate;
    private final boolean useAllocationBasedReturn;

    private InvestmentAccount(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.name = builder.name;
        this.accountType = builder.accountType;
        this.balance = builder.balance.setScale(2, RoundingMode.HALF_UP);
        this.allocation = builder.allocation;
        this.preRetirementReturnRate = builder.preRetirementReturnRate;
        this.postRetirementReturnRate = builder.postRetirementReturnRate;
        this.useAllocationBasedReturn = builder.useAllocationBasedReturn;
    }

    /**
     * Returns the unique identifier for this account.
     *
     * @return the account ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the account name or identifier.
     *
     * @return the account name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the type of investment account.
     *
     * @return the account type
     */
    public AccountType getAccountType() {
        return accountType;
    }

    /**
     * Returns the current account balance.
     *
     * @return the balance
     */
    public BigDecimal getBalance() {
        return balance;
    }

    /**
     * Returns the asset allocation for this account.
     *
     * @return the asset allocation
     */
    public AssetAllocation getAllocation() {
        return allocation;
    }

    /**
     * Returns the expected annual return rate for the pre-retirement phase.
     *
     * <p>If using allocation-based returns, this calculates the blended return.
     * Otherwise, returns the explicitly set rate.
     *
     * @return the pre-retirement return rate as a decimal (e.g., 0.07 for 7%)
     */
    public BigDecimal getPreRetirementReturnRate() {
        if (useAllocationBasedReturn) {
            return calculateAllocationBasedReturn();
        }
        return preRetirementReturnRate;
    }

    /**
     * Returns the expected annual return rate for the post-retirement phase.
     *
     * <p>If using allocation-based returns and no post-retirement rate is set,
     * this defaults to a more conservative allocation calculation.
     *
     * @return the post-retirement return rate as a decimal
     */
    public BigDecimal getPostRetirementReturnRate() {
        if (postRetirementReturnRate != null) {
            return postRetirementReturnRate;
        }
        if (useAllocationBasedReturn) {
            return calculateAllocationBasedReturn();
        }
        return preRetirementReturnRate;
    }

    /**
     * Returns whether this account uses allocation-based return calculation.
     *
     * @return true if using allocation-based returns
     */
    public boolean isUsingAllocationBasedReturn() {
        return useAllocationBasedReturn;
    }

    /**
     * Returns the tax treatment for this account based on its type.
     *
     * @return the tax treatment
     */
    public AccountType.TaxTreatment getTaxTreatment() {
        return accountType.getTaxTreatment();
    }

    /**
     * Indicates whether withdrawals from this account are tax-free.
     *
     * @return true if withdrawals are tax-free
     */
    public boolean isTaxFreeWithdrawal() {
        return accountType.isTaxFreeWithdrawal();
    }

    /**
     * Indicates whether this account is subject to Required Minimum Distributions.
     *
     * @return true if subject to RMD
     */
    public boolean isSubjectToRmd() {
        return accountType.isSubjectToRmd();
    }

    /**
     * Creates a new account with the specified balance.
     *
     * <p>This is useful for projections where the balance changes over time
     * but other properties remain constant.
     *
     * @param newBalance the new balance
     * @return a new InvestmentAccount with the updated balance
     */
    public InvestmentAccount withBalance(BigDecimal newBalance) {
        return toBuilder().balance(newBalance).build();
    }

    /**
     * Calculates the blended return rate based on asset allocation.
     *
     * @return the weighted average return rate
     */
    public BigDecimal calculateAllocationBasedReturn() {
        return allocation.calculateBlendedReturn(
                DEFAULT_STOCK_RETURN,
                DEFAULT_BOND_RETURN,
                DEFAULT_CASH_RETURN
        );
    }

    /**
     * Creates a new builder for InvestmentAccount.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder initialized with values from this account.
     *
     * @return a new builder with copied values
     */
    public Builder toBuilder() {
        Builder builder = new Builder()
            .id(this.id)
            .name(this.name)
            .accountType(this.accountType)
            .balance(this.balance)
            .allocation(this.allocation);

        if (useAllocationBasedReturn) {
            builder.useAllocationBasedReturn();
        } else {
            builder.preRetirementReturnRate(this.preRetirementReturnRate);
            if (this.postRetirementReturnRate != null) {
                builder.postRetirementReturnRate(this.postRetirementReturnRate);
            }
        }

        return builder;
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
        InvestmentAccount that = (InvestmentAccount) o;
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
        return "InvestmentAccount{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", accountType=" + accountType +
                ", balance=" + balance +
                ", allocation=" + allocation +
                '}';
    }

    /**
     * Builder for creating InvestmentAccount instances.
     */
    public static class Builder {
        private String id;
        private String name;
        private AccountType accountType;
        private BigDecimal balance = BigDecimal.ZERO;
        private AssetAllocation allocation = AssetAllocation.balanced();
        private BigDecimal preRetirementReturnRate;
        private BigDecimal postRetirementReturnRate;
        private boolean useAllocationBasedReturn;

        /**
         * Sets the account ID.
         *
         * @param id the account ID
         * @return this builder
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the account name.
         *
         * @param name the account name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
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
         * Sets the account balance.
         *
         * @param balance the current balance
         * @return this builder
         */
        public Builder balance(BigDecimal balance) {
            this.balance = balance;
            return this;
        }

        /**
         * Sets the account balance from a double value.
         *
         * @param balance the current balance
         * @return this builder
         */
        public Builder balance(double balance) {
            this.balance = BigDecimal.valueOf(balance);
            return this;
        }

        /**
         * Sets the asset allocation.
         *
         * @param allocation the asset allocation
         * @return this builder
         */
        public Builder allocation(AssetAllocation allocation) {
            this.allocation = allocation;
            return this;
        }

        /**
         * Sets the pre-retirement expected return rate.
         *
         * @param rate the annual return rate as decimal (e.g., 0.07 for 7%)
         * @return this builder
         */
        public Builder preRetirementReturnRate(BigDecimal rate) {
            this.preRetirementReturnRate = rate;
            this.useAllocationBasedReturn = false;
            return this;
        }

        /**
         * Sets the pre-retirement expected return rate.
         *
         * @param rate the annual return rate as decimal (e.g., 0.07 for 7%)
         * @return this builder
         */
        public Builder preRetirementReturnRate(double rate) {
            return preRetirementReturnRate(BigDecimal.valueOf(rate));
        }

        /**
         * Sets the post-retirement expected return rate.
         *
         * @param rate the annual return rate as decimal
         * @return this builder
         */
        public Builder postRetirementReturnRate(BigDecimal rate) {
            this.postRetirementReturnRate = rate;
            return this;
        }

        /**
         * Sets the post-retirement expected return rate.
         *
         * @param rate the annual return rate as decimal
         * @return this builder
         */
        public Builder postRetirementReturnRate(double rate) {
            return postRetirementReturnRate(BigDecimal.valueOf(rate));
        }

        /**
         * Configures the account to calculate return rates from asset allocation.
         *
         * @return this builder
         */
        public Builder useAllocationBasedReturn() {
            this.useAllocationBasedReturn = true;
            this.preRetirementReturnRate = null;
            return this;
        }

        /**
         * Builds the InvestmentAccount instance.
         *
         * @return a new InvestmentAccount
         * @throws NullPointerException if required fields are missing
         * @throws IllegalArgumentException if validation fails
         */
        public InvestmentAccount build() {
            validate();
            return new InvestmentAccount(this);
        }

        private void validate() {
            MissingRequiredFieldException.requireNonNull(name, "name");
            MissingRequiredFieldException.requireNonNull(accountType, "accountType");
            MissingRequiredFieldException.requireNonNull(allocation, "allocation");

            if (balance.compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("Balance cannot be negative", "balance");
            }

            if (!useAllocationBasedReturn) {
                MissingRequiredFieldException.requireNonNull(preRetirementReturnRate,
                    "preRetirementReturnRate (required unless using allocation-based returns)");
                validateReturnRate("Pre-retirement return rate", preRetirementReturnRate);
                if (postRetirementReturnRate != null) {
                    validateReturnRate("Post-retirement return rate", postRetirementReturnRate);
                }
            }
        }

        private void validateReturnRate(String name, BigDecimal rate) {
            if (rate.compareTo(MIN_RETURN) < 0 || rate.compareTo(MAX_RETURN) > 0) {
                throw InvalidRateException.returnRateOutOfRange(name, rate, MIN_RETURN, MAX_RETURN);
            }
        }
    }
}
