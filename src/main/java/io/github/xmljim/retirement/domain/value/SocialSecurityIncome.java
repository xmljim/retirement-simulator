package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;

/**
 * Configuration for Social Security retirement income.
 *
 * <p>Represents the Social Security benefit amount, start date, and
 * cost-of-living adjustment (COLA) rate.
 *
 * <p>This is an immutable value object. Use the {@link Builder} to create instances.
 */
public final class SocialSecurityIncome {

    private final BigDecimal monthlyBenefit;
    private final BigDecimal colaRate;
    private final LocalDate startDate;

    private SocialSecurityIncome(Builder builder) {
        this.monthlyBenefit = builder.monthlyBenefit;
        this.colaRate = builder.colaRate;
        this.startDate = builder.startDate;
    }

    /**
     * Returns the monthly Social Security benefit amount.
     *
     * @return the monthly benefit
     */
    public BigDecimal getMonthlyBenefit() {
        return monthlyBenefit;
    }

    /**
     * Returns the annual COLA adjustment rate as a decimal.
     *
     * <p>For example, 0.028 represents a 2.8% annual increase.
     *
     * @return the COLA rate
     */
    public BigDecimal getColaRate() {
        return colaRate;
    }

    /**
     * Returns the date when Social Security benefits begin.
     *
     * @return the start date
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * Creates a new builder for SocialSecurityIncome.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SocialSecurityIncome that = (SocialSecurityIncome) o;
        return monthlyBenefit.compareTo(that.monthlyBenefit) == 0
            && colaRate.compareTo(that.colaRate) == 0
            && Objects.equals(startDate, that.startDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(monthlyBenefit, colaRate, startDate);
    }

    @Override
    public String toString() {
        return "SocialSecurityIncome{" +
            "monthlyBenefit=" + monthlyBenefit +
            ", colaRate=" + colaRate +
            ", startDate=" + startDate +
            '}';
    }

    /**
     * Builder for creating SocialSecurityIncome instances.
     */
    public static class Builder {
        private BigDecimal monthlyBenefit = BigDecimal.ZERO;
        private BigDecimal colaRate = BigDecimal.ZERO;
        private LocalDate startDate;

        /**
         * Sets the monthly benefit amount.
         *
         * @param amount the monthly benefit
         * @return this builder
         */
        public Builder monthlyBenefit(BigDecimal amount) {
            this.monthlyBenefit = amount;
            return this;
        }

        /**
         * Sets the monthly benefit amount.
         *
         * @param amount the monthly benefit
         * @return this builder
         */
        public Builder monthlyBenefit(double amount) {
            return monthlyBenefit(BigDecimal.valueOf(amount));
        }

        /**
         * Sets the annual COLA adjustment rate.
         *
         * @param rate the COLA rate as a decimal
         * @return this builder
         */
        public Builder colaRate(BigDecimal rate) {
            this.colaRate = rate;
            return this;
        }

        /**
         * Sets the annual COLA adjustment rate.
         *
         * @param rate the COLA rate as a decimal
         * @return this builder
         */
        public Builder colaRate(double rate) {
            return colaRate(BigDecimal.valueOf(rate));
        }

        /**
         * Sets the benefit start date.
         *
         * @param date the start date
         * @return this builder
         */
        public Builder startDate(LocalDate date) {
            this.startDate = date;
            return this;
        }

        /**
         * Builds the SocialSecurityIncome instance.
         *
         * @return a new SocialSecurityIncome
         * @throws MissingRequiredFieldException if startDate is null
         * @throws ValidationException if monthlyBenefit is negative
         */
        public SocialSecurityIncome build() {
            validate();
            return new SocialSecurityIncome(this);
        }

        private void validate() {
            MissingRequiredFieldException.requireNonNull(startDate, "startDate");
            if (monthlyBenefit.compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("Monthly benefit cannot be negative", "monthlyBenefit");
            }
        }
    }
}
