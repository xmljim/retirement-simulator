package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import io.github.xmljim.retirement.domain.annotation.Generated;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;

/**
 * Configuration for other retirement income sources.
 *
 * <p>Represents income from pensions, annuities, or other fixed income
 * sources during retirement.
 *
 * <p>This is an immutable value object. Use the {@link Builder} to create instances.
 */
public final class RetirementIncome {

    private final String name;
    private final BigDecimal monthlyAmount;
    private final BigDecimal adjustmentRate;
    private final LocalDate startDate;

    private RetirementIncome(Builder builder) {
        this.name = builder.name;
        this.monthlyAmount = builder.monthlyAmount;
        this.adjustmentRate = builder.adjustmentRate;
        this.startDate = builder.startDate;
    }

    /**
     * Returns the name or description of this income source.
     *
     * @return the income source name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the monthly income amount.
     *
     * @return the monthly amount
     */
    public BigDecimal getMonthlyAmount() {
        return monthlyAmount;
    }

    /**
     * Returns the annual adjustment rate as a decimal.
     *
     * <p>For example, 0.02 represents a 2% annual increase.
     *
     * @return the adjustment rate
     */
    public BigDecimal getAdjustmentRate() {
        return adjustmentRate;
    }

    /**
     * Returns the date when this income begins.
     *
     * @return the start date
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * Creates a fixed pension with no annual adjustment.
     *
     * @param name the pension name
     * @param monthlyAmount the monthly amount
     * @param startDate when payments begin
     * @return a new RetirementIncome
     */
    public static RetirementIncome fixedPension(String name, double monthlyAmount, LocalDate startDate) {
        return builder()
            .name(name)
            .monthlyAmount(monthlyAmount)
            .startDate(startDate)
            .build();
    }

    /**
     * Creates a new builder for RetirementIncome.
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
        RetirementIncome that = (RetirementIncome) o;
        return Objects.equals(name, that.name)
            && monthlyAmount.compareTo(that.monthlyAmount) == 0
            && adjustmentRate.compareTo(that.adjustmentRate) == 0
            && Objects.equals(startDate, that.startDate);
    }

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(name, monthlyAmount, adjustmentRate, startDate);
    }

    @Generated
    @Override
    public String toString() {
        return "RetirementIncome{" +
            "name='" + name + '\'' +
            ", monthlyAmount=" + monthlyAmount +
            ", adjustmentRate=" + adjustmentRate +
            ", startDate=" + startDate +
            '}';
    }

    /**
     * Builder for creating RetirementIncome instances.
     */
    public static class Builder {
        private String name = "Other Income";
        private BigDecimal monthlyAmount = BigDecimal.ZERO;
        private BigDecimal adjustmentRate = BigDecimal.ZERO;
        private LocalDate startDate;

        /**
         * Sets the income source name.
         *
         * @param name the name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the monthly income amount.
         *
         * @param amount the monthly amount
         * @return this builder
         */
        public Builder monthlyAmount(BigDecimal amount) {
            this.monthlyAmount = amount;
            return this;
        }

        /**
         * Sets the monthly income amount.
         *
         * @param amount the monthly amount
         * @return this builder
         */
        public Builder monthlyAmount(double amount) {
            return monthlyAmount(BigDecimal.valueOf(amount));
        }

        /**
         * Sets the annual adjustment rate.
         *
         * @param rate the adjustment rate as a decimal
         * @return this builder
         */
        public Builder adjustmentRate(BigDecimal rate) {
            this.adjustmentRate = rate;
            return this;
        }

        /**
         * Sets the annual adjustment rate.
         *
         * @param rate the adjustment rate as a decimal
         * @return this builder
         */
        public Builder adjustmentRate(double rate) {
            return adjustmentRate(BigDecimal.valueOf(rate));
        }

        /**
         * Sets the income start date.
         *
         * @param date the start date
         * @return this builder
         */
        public Builder startDate(LocalDate date) {
            this.startDate = date;
            return this;
        }

        /**
         * Builds the RetirementIncome instance.
         *
         * @return a new RetirementIncome
         * @throws MissingRequiredFieldException if startDate is null
         * @throws ValidationException if monthlyAmount is negative
         */
        public RetirementIncome build() {
            validate();
            return new RetirementIncome(this);
        }

        private void validate() {
            MissingRequiredFieldException.requireNonNull(startDate, "startDate");
            if (monthlyAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("Monthly amount cannot be negative", "monthlyAmount");
            }
        }
    }
}
