package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

import io.github.xmljim.retirement.domain.annotation.Generated;
import io.github.xmljim.retirement.domain.enums.ContingencyType;
import io.github.xmljim.retirement.domain.exception.ValidationException;

/**
 * A contingency reserve fund for handling unexpected or large expenses.
 *
 * <p>Contingency reserves help retirees manage "lumpy" spending - large,
 * infrequent costs like home repairs, vehicle replacement, or emergencies.
 * Contributions are treated as expenses; withdrawals offset unexpected costs.
 *
 * <p>This is an immutable value object. Use {@link #withContribution} and
 * {@link #withExpense} to create new instances with updated balances.
 */
public final class ContingencyReserve {

    private static final int MONTHS_PER_YEAR = 12;
    private static final int SCALE = 2;

    private final String name;
    private final ContingencyType type;
    private final BigDecimal targetAmount;
    private final BigDecimal currentBalance;
    private final BigDecimal annualContribution;
    private final LocalDate startDate;

    private ContingencyReserve(Builder builder) {
        this.name = builder.name;
        this.type = builder.type;
        this.targetAmount = builder.targetAmount;
        this.currentBalance = builder.currentBalance;
        this.annualContribution = builder.annualContribution;
        this.startDate = builder.startDate;
    }

    /**
     * Returns the reserve name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the contingency type.
     *
     * @return the type
     */
    public ContingencyType getType() {
        return type;
    }

    /**
     * Returns the target reserve amount.
     *
     * @return the target amount
     */
    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    /**
     * Returns the current funded balance.
     *
     * @return the current balance
     */
    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    /**
     * Returns the annual contribution amount.
     *
     * @return the annual contribution
     */
    public BigDecimal getAnnualContribution() {
        return annualContribution;
    }

    /**
     * Returns the monthly contribution amount.
     *
     * @return annual contribution divided by 12
     */
    public BigDecimal getMonthlyContribution() {
        return annualContribution.divide(BigDecimal.valueOf(MONTHS_PER_YEAR), SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Returns the start date for contributions.
     *
     * @return the start date
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * Returns whether the reserve has reached its target.
     *
     * @return true if current balance >= target amount
     */
    public boolean isFullyFunded() {
        return currentBalance.compareTo(targetAmount) >= 0;
    }

    /**
     * Returns the deficit (target - current), or zero if fully funded.
     *
     * @return the deficit amount
     */
    public BigDecimal getDeficit() {
        BigDecimal deficit = targetAmount.subtract(currentBalance);
        return deficit.max(BigDecimal.ZERO);
    }

    /**
     * Returns the funding percentage (current / target).
     *
     * @return the funding percentage as a decimal (0.0 to 1.0+)
     */
    public BigDecimal getFundingPercentage() {
        if (targetAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ONE;
        }
        return currentBalance.divide(targetAmount, 4, RoundingMode.HALF_UP);
    }

    /**
     * Creates a new reserve with an added contribution.
     *
     * @param amount the contribution amount
     * @return a new ContingencyReserve with updated balance
     */
    public ContingencyReserve withContribution(BigDecimal amount) {
        return new Builder()
            .name(this.name)
            .type(this.type)
            .targetAmount(this.targetAmount)
            .currentBalance(this.currentBalance.add(amount))
            .annualContribution(this.annualContribution)
            .startDate(this.startDate)
            .build();
    }

    /**
     * Creates a new reserve with an expense deducted.
     *
     * <p>If the expense exceeds the current balance, the balance
     * is reduced to zero (the excess must come from elsewhere).
     *
     * @param amount the expense amount
     * @return a new ContingencyReserve with updated balance
     */
    public ContingencyReserve withExpense(BigDecimal amount) {
        BigDecimal newBalance = currentBalance.subtract(amount).max(BigDecimal.ZERO);
        return new Builder()
            .name(this.name)
            .type(this.type)
            .targetAmount(this.targetAmount)
            .currentBalance(newBalance)
            .annualContribution(this.annualContribution)
            .startDate(this.startDate)
            .build();
    }

    /**
     * Returns the amount that can be covered by this reserve for a given expense.
     *
     * @param expenseAmount the total expense amount
     * @return the amount covered (min of expense and current balance)
     */
    public BigDecimal getCoverableAmount(BigDecimal expenseAmount) {
        return expenseAmount.min(currentBalance);
    }

    /**
     * Creates a reserve based on a percentage of asset value.
     *
     * @param name the reserve name
     * @param type the contingency type
     * @param assetValue the asset value (e.g., home value)
     * @param rate the annual reserve rate (e.g., 0.015 for 1.5%)
     * @return a new ContingencyReserve
     */
    public static ContingencyReserve ofAssetValue(
            String name,
            ContingencyType type,
            BigDecimal assetValue,
            BigDecimal rate) {
        BigDecimal annual = assetValue.multiply(rate).setScale(SCALE, RoundingMode.HALF_UP);
        return builder()
            .name(name)
            .type(type)
            .targetAmount(annual)
            .annualContribution(annual)
            .build();
    }

    /**
     * Creates a new builder.
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
        ContingencyReserve that = (ContingencyReserve) o;
        return Objects.equals(name, that.name) && type == that.type;
    }

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @Generated
    @Override
    public String toString() {
        return "ContingencyReserve{name='" + name + "', type=" + type
            + ", balance=" + currentBalance + "/" + targetAmount + "}";
    }

    /**
     * Builder for creating ContingencyReserve instances.
     */
    public static class Builder {
        private String name = "Reserve";
        private ContingencyType type = ContingencyType.GENERAL;
        private BigDecimal targetAmount = BigDecimal.ZERO;
        private BigDecimal currentBalance = BigDecimal.ZERO;
        private BigDecimal annualContribution = BigDecimal.ZERO;
        private LocalDate startDate = LocalDate.now();

        /** Sets the reserve name. @param name the name @return this builder */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /** Sets the contingency type. @param type the type @return this builder */
        public Builder type(ContingencyType type) {
            this.type = type;
            return this;
        }

        /** Sets the target amount. @param amount the target @return this builder */
        public Builder targetAmount(BigDecimal amount) {
            this.targetAmount = amount;
            return this;
        }

        /** Sets the target amount. @param amount the target @return this builder */
        public Builder targetAmount(double amount) {
            return targetAmount(BigDecimal.valueOf(amount));
        }

        /** Sets the current balance. @param balance the balance @return this builder */
        public Builder currentBalance(BigDecimal balance) {
            this.currentBalance = balance;
            return this;
        }

        /** Sets the current balance. @param balance the balance @return this builder */
        public Builder currentBalance(double balance) {
            return currentBalance(BigDecimal.valueOf(balance));
        }

        /** Sets the annual contribution. @param amount the amount @return this builder */
        public Builder annualContribution(BigDecimal amount) {
            this.annualContribution = amount;
            return this;
        }

        /** Sets the annual contribution. @param amount the amount @return this builder */
        public Builder annualContribution(double amount) {
            return annualContribution(BigDecimal.valueOf(amount));
        }

        /** Sets the start date. @param date the date @return this builder */
        public Builder startDate(LocalDate date) {
            this.startDate = date;
            return this;
        }

        /** Builds the ContingencyReserve. @return the built instance */
        public ContingencyReserve build() {
            validate();
            return new ContingencyReserve(this);
        }

        private void validate() {
            if (targetAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("Target amount cannot be negative", "targetAmount");
            }
            if (currentBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("Current balance cannot be negative", "currentBalance");
            }
        }
    }
}
