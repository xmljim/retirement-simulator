package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

import io.github.xmljim.retirement.domain.annotation.Generated;
import io.github.xmljim.retirement.domain.enums.AnnuityType;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;

/**
 * Represents an annuity that provides retirement income.
 *
 * <p>Annuities are insurance contracts that provide guaranteed income,
 * typically in retirement. This class models various types of annuities
 * including fixed immediate, fixed deferred, variable, and indexed annuities.
 *
 * <p>This is an immutable value object. Use the {@link Builder} to create instances.
 */
public final class Annuity {

    private static final MathContext MATH_CONTEXT = new MathContext(10, RoundingMode.HALF_UP);
    private static final int MONTHS_PER_YEAR = 12;

    private final String name;
    private final AnnuityType annuityType;
    private final BigDecimal purchaseAmount;
    private final BigDecimal monthlyBenefit;
    private final BigDecimal colaRate;
    private final LocalDate purchaseDate;
    private final LocalDate paymentStartDate;

    private Annuity(Builder builder) {
        this.name = builder.name;
        this.annuityType = builder.annuityType;
        this.purchaseAmount = builder.purchaseAmount;
        this.monthlyBenefit = builder.monthlyBenefit;
        this.colaRate = builder.colaRate;
        this.purchaseDate = builder.purchaseDate;
        this.paymentStartDate = builder.paymentStartDate;
    }

    /**
     * Returns the name or description of this annuity.
     *
     * @return the annuity name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the type of annuity.
     *
     * @return the annuity type
     */
    public AnnuityType getAnnuityType() {
        return annuityType;
    }

    /**
     * Returns the initial purchase amount (premium paid).
     *
     * @return the purchase amount
     */
    public BigDecimal getPurchaseAmount() {
        return purchaseAmount;
    }

    /**
     * Returns the monthly benefit amount at the start of payments.
     *
     * @return the initial monthly benefit
     */
    public BigDecimal getMonthlyBenefit() {
        return monthlyBenefit;
    }

    /**
     * Returns the annual COLA adjustment rate, if applicable.
     *
     * @return optional containing the COLA rate, or empty if no COLA
     */
    public Optional<BigDecimal> getColaRate() {
        return Optional.ofNullable(colaRate);
    }

    /**
     * Returns the date the annuity was purchased.
     *
     * @return the purchase date
     */
    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    /**
     * Returns the date when payments begin.
     *
     * <p>For immediate annuities, this is typically the same as or shortly
     * after the purchase date. For deferred annuities, this is the
     * annuitization date.
     *
     * @return the payment start date
     */
    public LocalDate getPaymentStartDate() {
        return paymentStartDate;
    }

    /**
     * Calculates the annual benefit amount at the start of payments.
     *
     * @return the annual benefit (monthly * 12)
     */
    public BigDecimal getAnnualBenefit() {
        return monthlyBenefit.multiply(BigDecimal.valueOf(MONTHS_PER_YEAR));
    }

    /**
     * Calculates the monthly benefit for a given date, accounting for COLA adjustments.
     *
     * <p>If the date is before the payment start date, returns zero.
     * If no COLA rate is set, returns the base monthly benefit.
     *
     * @param asOfDate the date for which to calculate the benefit
     * @return the COLA-adjusted monthly benefit
     */
    public BigDecimal getMonthlyBenefitAsOf(LocalDate asOfDate) {
        if (asOfDate.isBefore(paymentStartDate)) {
            return BigDecimal.ZERO;
        }

        if (colaRate == null || colaRate.compareTo(BigDecimal.ZERO) == 0) {
            return monthlyBenefit;
        }

        long monthsElapsed = ChronoUnit.MONTHS.between(paymentStartDate, asOfDate);
        int yearsElapsed = (int) (monthsElapsed / MONTHS_PER_YEAR);

        if (yearsElapsed <= 0) {
            return monthlyBenefit;
        }

        // Apply compound COLA: benefit * (1 + colaRate)^years
        BigDecimal colaMultiplier = BigDecimal.ONE.add(colaRate).pow(yearsElapsed, MATH_CONTEXT);
        return monthlyBenefit.multiply(colaMultiplier).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the annual benefit for a given date, accounting for COLA adjustments.
     *
     * @param asOfDate the date for which to calculate the benefit
     * @return the COLA-adjusted annual benefit
     */
    public BigDecimal getAnnualBenefitAsOf(LocalDate asOfDate) {
        return getMonthlyBenefitAsOf(asOfDate).multiply(BigDecimal.valueOf(MONTHS_PER_YEAR));
    }

    /**
     * Returns whether payments have started as of the given date.
     *
     * @param asOfDate the date to check
     * @return true if payments have started
     */
    public boolean isPaymentActive(LocalDate asOfDate) {
        return !asOfDate.isBefore(paymentStartDate);
    }

    /**
     * Creates a new builder for Annuity.
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
        Annuity annuity = (Annuity) o;
        return Objects.equals(name, annuity.name)
            && annuityType == annuity.annuityType
            && purchaseAmount.compareTo(annuity.purchaseAmount) == 0
            && monthlyBenefit.compareTo(annuity.monthlyBenefit) == 0
            && Objects.equals(colaRate, annuity.colaRate)
            && Objects.equals(purchaseDate, annuity.purchaseDate)
            && Objects.equals(paymentStartDate, annuity.paymentStartDate);
    }

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(name, annuityType, purchaseAmount, monthlyBenefit,
            colaRate, purchaseDate, paymentStartDate);
    }

    @Generated
    @Override
    public String toString() {
        return "Annuity{"
            + "name='" + name + '\''
            + ", annuityType=" + annuityType
            + ", purchaseAmount=" + purchaseAmount
            + ", monthlyBenefit=" + monthlyBenefit
            + ", colaRate=" + colaRate
            + ", purchaseDate=" + purchaseDate
            + ", paymentStartDate=" + paymentStartDate
            + '}';
    }

    /**
     * Builder for creating Annuity instances.
     */
    public static class Builder {
        private String name;
        private AnnuityType annuityType;
        private BigDecimal purchaseAmount = BigDecimal.ZERO;
        private BigDecimal monthlyBenefit = BigDecimal.ZERO;
        private BigDecimal colaRate;
        private LocalDate purchaseDate;
        private LocalDate paymentStartDate;

        /**
         * Sets the name or description of this annuity.
         *
         * @param name the annuity name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the annuity type.
         *
         * @param type the annuity type
         * @return this builder
         */
        public Builder annuityType(AnnuityType type) {
            this.annuityType = type;
            return this;
        }

        /**
         * Sets the initial purchase amount (premium paid).
         *
         * @param amount the purchase amount
         * @return this builder
         */
        public Builder purchaseAmount(BigDecimal amount) {
            this.purchaseAmount = amount;
            return this;
        }

        /**
         * Sets the initial purchase amount (premium paid).
         *
         * @param amount the purchase amount
         * @return this builder
         */
        public Builder purchaseAmount(double amount) {
            return purchaseAmount(BigDecimal.valueOf(amount));
        }

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
         * @param rate the COLA rate as a decimal (e.g., 0.02 for 2%)
         * @return this builder
         */
        public Builder colaRate(BigDecimal rate) {
            this.colaRate = rate;
            return this;
        }

        /**
         * Sets the annual COLA adjustment rate.
         *
         * @param rate the COLA rate as a decimal (e.g., 0.02 for 2%)
         * @return this builder
         */
        public Builder colaRate(double rate) {
            return colaRate(BigDecimal.valueOf(rate));
        }

        /**
         * Sets the date the annuity was purchased.
         *
         * @param date the purchase date
         * @return this builder
         */
        public Builder purchaseDate(LocalDate date) {
            this.purchaseDate = date;
            return this;
        }

        /**
         * Sets the date when payments begin.
         *
         * @param date the payment start date
         * @return this builder
         */
        public Builder paymentStartDate(LocalDate date) {
            this.paymentStartDate = date;
            return this;
        }

        /**
         * Builds the Annuity instance.
         *
         * @return a new Annuity
         * @throws MissingRequiredFieldException if required fields are null
         * @throws ValidationException if validation fails
         */
        public Annuity build() {
            validate();
            return new Annuity(this);
        }

        private void validate() {
            MissingRequiredFieldException.requireNonNull(name, "name");
            ValidationException.validate("name", name, n -> !n.isBlank(),
                "Name cannot be blank");

            MissingRequiredFieldException.requireNonNull(annuityType, "annuityType");
            MissingRequiredFieldException.requireNonNull(purchaseDate, "purchaseDate");
            MissingRequiredFieldException.requireNonNull(paymentStartDate, "paymentStartDate");

            ValidationException.validate("purchaseAmount", purchaseAmount,
                v -> v.compareTo(BigDecimal.ZERO) >= 0,
                "Purchase amount cannot be negative");

            ValidationException.validate("monthlyBenefit", monthlyBenefit,
                v -> v.compareTo(BigDecimal.ZERO) >= 0,
                "Monthly benefit cannot be negative");

            ValidationException.validate("paymentStartDate", paymentStartDate,
                d -> !d.isBefore(purchaseDate),
                "Payment start date cannot be before purchase date");

            if (annuityType.isImmediate()) {
                ValidationException.validate("monthlyBenefit", monthlyBenefit,
                    v -> v.compareTo(BigDecimal.ZERO) > 0,
                    "Immediate annuity must have a positive monthly benefit");
            }
        }
    }
}
