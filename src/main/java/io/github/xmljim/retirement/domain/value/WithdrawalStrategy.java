package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.util.Objects;

import io.github.xmljim.retirement.domain.annotation.Generated;
import io.github.xmljim.retirement.domain.enums.WithdrawalType;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;

/**
 * Configuration for retirement withdrawal strategy.
 *
 * <p>Represents how withdrawals are calculated during retirement, either
 * as a fixed dollar amount or as a percentage of portfolio/salary.
 *
 * <p>This is an immutable value object. Use the {@link Builder} to create instances.
 */
public final class WithdrawalStrategy {

    private final WithdrawalType withdrawalType;
    private final BigDecimal withdrawalRate;

    private WithdrawalStrategy(Builder builder) {
        this.withdrawalType = builder.withdrawalType;
        this.withdrawalRate = builder.withdrawalRate;
    }

    /**
     * Returns the type of withdrawal strategy.
     *
     * @return the withdrawal type
     */
    public WithdrawalType getWithdrawalType() {
        return withdrawalType;
    }

    /**
     * Returns the withdrawal rate.
     *
     * <p>For FIXED type, this represents a dollar amount.
     * For PERCENTAGE type, this is a decimal (e.g., 0.04 for 4%).
     *
     * @return the withdrawal rate
     */
    public BigDecimal getWithdrawalRate() {
        return withdrawalRate;
    }

    /**
     * Creates a fixed dollar amount withdrawal strategy.
     *
     * @param monthlyAmount the fixed monthly withdrawal amount
     * @return a new WithdrawalStrategy
     */
    public static WithdrawalStrategy fixed(double monthlyAmount) {
        return builder()
            .withdrawalType(WithdrawalType.FIXED)
            .withdrawalRate(monthlyAmount)
            .build();
    }

    /**
     * Creates a percentage-based withdrawal strategy.
     *
     * @param rate the withdrawal rate as a decimal (e.g., 0.04 for 4%)
     * @return a new WithdrawalStrategy
     */
    public static WithdrawalStrategy percentage(double rate) {
        return builder()
            .withdrawalType(WithdrawalType.PERCENTAGE)
            .withdrawalRate(rate)
            .build();
    }

    /**
     * Creates a new builder for WithdrawalStrategy.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
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
        WithdrawalStrategy that = (WithdrawalStrategy) o;
        return withdrawalType == that.withdrawalType
            && withdrawalRate.compareTo(that.withdrawalRate) == 0;
    }

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(withdrawalType, withdrawalRate);
    }

    @Generated
    @Override
    public String toString() {
        return "WithdrawalStrategy{" +
            "type=" + withdrawalType +
            ", rate=" + withdrawalRate +
            '}';
    }

    /**
     * Builder for creating WithdrawalStrategy instances.
     */
    public static class Builder {
        private WithdrawalType withdrawalType;
        private BigDecimal withdrawalRate = BigDecimal.ZERO;

        /**
         * Sets the withdrawal type.
         *
         * @param type the withdrawal type
         * @return this builder
         */
        public Builder withdrawalType(WithdrawalType type) {
            this.withdrawalType = type;
            return this;
        }

        /**
         * Sets the withdrawal rate.
         *
         * @param rate the rate (dollar amount for FIXED, decimal for PERCENTAGE)
         * @return this builder
         */
        public Builder withdrawalRate(BigDecimal rate) {
            this.withdrawalRate = rate;
            return this;
        }

        /**
         * Sets the withdrawal rate.
         *
         * @param rate the rate (dollar amount for FIXED, decimal for PERCENTAGE)
         * @return this builder
         */
        public Builder withdrawalRate(double rate) {
            return withdrawalRate(BigDecimal.valueOf(rate));
        }

        /**
         * Builds the WithdrawalStrategy instance.
         *
         * @return a new WithdrawalStrategy
         * @throws MissingRequiredFieldException if withdrawalType is null
         * @throws ValidationException if withdrawalRate is negative
         */
        public WithdrawalStrategy build() {
            validate();
            return new WithdrawalStrategy(this);
        }

        private void validate() {
            MissingRequiredFieldException.requireNonNull(withdrawalType, "withdrawalType");
            if (withdrawalRate.compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("Withdrawal rate cannot be negative", "withdrawalRate");
            }
        }
    }
}
