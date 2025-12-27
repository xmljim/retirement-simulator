package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import io.github.xmljim.retirement.domain.annotation.Generated;
import io.github.xmljim.retirement.domain.calculator.CalculatorFactory;
import io.github.xmljim.retirement.domain.calculator.SocialSecurityCalculator;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;

/**
 * Social Security benefit configuration with claiming age adjustments.
 *
 * <p>This value object stores the Full Retirement Age (FRA) benefit amount
 * and calculates adjusted benefits based on claiming age. Benefits claimed
 * before FRA are reduced; benefits delayed past FRA receive credits.
 *
 * <p>Key features:
 * <ul>
 *   <li>{@link #getFraBenefit()} - The base benefit at FRA (for spousal calculations)</li>
 *   <li>{@link #getAdjustedMonthlyBenefit()} - Benefit with early/delayed adjustment</li>
 *   <li>{@link #getMonthlyBenefit(LocalDate)} - Benefit with COLA applied</li>
 * </ul>
 *
 * <p>This is an immutable value object. Use the {@link Builder} to create instances.
 */
public final class SocialSecurityBenefit {

    private final BigDecimal fraBenefit;
    private final int birthYear;
    private final int claimingAgeMonths;
    private final BigDecimal colaRate;
    private final LocalDate startDate;

    private final transient SocialSecurityCalculator calculator;
    private final transient int fraMonths;
    private final transient BigDecimal adjustedBenefit;

    private SocialSecurityBenefit(Builder builder) {
        this.fraBenefit = builder.fraBenefit;
        this.birthYear = builder.birthYear;
        this.claimingAgeMonths = builder.claimingAgeMonths;
        this.colaRate = builder.colaRate;
        this.startDate = builder.startDate;
        this.calculator = builder.calculator;

        this.fraMonths = calculator.calculateFraMonths(birthYear);
        this.adjustedBenefit = calculator.calculateAdjustedBenefit(
            fraBenefit, fraMonths, claimingAgeMonths);
    }

    /**
     * Returns the monthly benefit at Full Retirement Age.
     *
     * @return the FRA benefit amount
     */
    public BigDecimal getFraBenefit() {
        return fraBenefit;
    }

    /**
     * Returns the birth year used for FRA calculation.
     *
     * @return the birth year
     */
    public int getBirthYear() {
        return birthYear;
    }

    /**
     * Returns the claiming age in months.
     *
     * @return the claiming age in months
     */
    public int getClaimingAgeMonths() {
        return claimingAgeMonths;
    }

    /**
     * Returns the annual COLA rate as a decimal.
     *
     * @return the COLA rate
     */
    public BigDecimal getColaRate() {
        return colaRate;
    }

    /**
     * Returns the date when benefits begin.
     *
     * @return the start date
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * Returns the Full Retirement Age in months based on birth year.
     *
     * @return the FRA in months
     */
    public int getFraAgeMonths() {
        return fraMonths;
    }

    /**
     * Returns the adjusted monthly benefit (with early/delayed adjustment, no COLA).
     *
     * @return the adjusted benefit
     */
    public BigDecimal getAdjustedMonthlyBenefit() {
        return adjustedBenefit;
    }

    /**
     * Returns true if claiming before FRA.
     *
     * @return true if early claiming
     */
    public boolean isEarlyClaiming() {
        return claimingAgeMonths < fraMonths;
    }

    /**
     * Returns true if claiming after FRA.
     *
     * @return true if delayed claiming
     */
    public boolean isDelayedClaiming() {
        return claimingAgeMonths > fraMonths;
    }

    /**
     * Returns the monthly benefit for a given date, with COLA applied.
     *
     * @param date the date to calculate benefit for
     * @return the COLA-adjusted benefit, or ZERO if before start date
     */
    public BigDecimal getMonthlyBenefit(LocalDate date) {
        if (date == null || startDate == null || date.isBefore(startDate)) {
            return BigDecimal.ZERO;
        }
        int yearsOfCola = date.getYear() - startDate.getYear();
        if (yearsOfCola <= 0) {
            return adjustedBenefit;
        }
        return calculator.applyColaAdjustment(adjustedBenefit, colaRate, yearsOfCola);
    }

    /**
     * Creates a new builder for SocialSecurityBenefit.
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
        SocialSecurityBenefit that = (SocialSecurityBenefit) o;
        return birthYear == that.birthYear
            && claimingAgeMonths == that.claimingAgeMonths
            && fraBenefit.compareTo(that.fraBenefit) == 0
            && colaRate.compareTo(that.colaRate) == 0
            && Objects.equals(startDate, that.startDate);
    }

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(fraBenefit, birthYear, claimingAgeMonths, colaRate, startDate);
    }

    @Generated
    @Override
    public String toString() {
        return "SocialSecurityBenefit{fraBenefit=" + fraBenefit
            + ", birthYear=" + birthYear + ", claimingAgeMonths=" + claimingAgeMonths
            + ", colaRate=" + colaRate + ", startDate=" + startDate + '}';
    }

    /**
     * Builder for creating SocialSecurityBenefit instances.
     */
    public static class Builder {
        private BigDecimal fraBenefit = BigDecimal.ZERO;
        private int birthYear;
        private int claimingAgeMonths;
        private BigDecimal colaRate = BigDecimal.ZERO;
        private LocalDate startDate;
        private SocialSecurityCalculator calculator;

        /**
         * Sets the FRA benefit amount.
         *
         * @param amount the monthly benefit at FRA
         * @return this builder
         */
        public Builder fraBenefit(BigDecimal amount) {
            this.fraBenefit = amount;
            return this;
        }

        /**
         * Sets the FRA benefit amount.
         *
         * @param amount the monthly benefit at FRA
         * @return this builder
         */
        public Builder fraBenefit(double amount) {
            return fraBenefit(BigDecimal.valueOf(amount));
        }

        /**
         * Sets the birth year for FRA calculation.
         *
         * @param year the birth year
         * @return this builder
         */
        public Builder birthYear(int year) {
            this.birthYear = year;
            return this;
        }

        /**
         * Sets the claiming age in months.
         *
         * @param months the claiming age in months (744-840)
         * @return this builder
         */
        public Builder claimingAgeMonths(int months) {
            this.claimingAgeMonths = months;
            return this;
        }

        /**
         * Sets the claiming age in years and months.
         *
         * @param years the years component
         * @param months the months component (0-11)
         * @return this builder
         */
        public Builder claimingAge(int years, int months) {
            return claimingAgeMonths(years * 12 + months);
        }

        /**
         * Sets the annual COLA rate.
         *
         * @param rate the COLA rate as a decimal
         * @return this builder
         */
        public Builder colaRate(BigDecimal rate) {
            this.colaRate = rate;
            return this;
        }

        /**
         * Sets the annual COLA rate.
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
         * Sets a custom calculator (primarily for testing).
         *
         * @param calc the calculator to use
         * @return this builder
         */
        public Builder calculator(SocialSecurityCalculator calc) {
            this.calculator = calc;
            return this;
        }

        /**
         * Builds the SocialSecurityBenefit instance.
         *
         * @return a new SocialSecurityBenefit
         * @throws MissingRequiredFieldException if startDate is null
         * @throws ValidationException if validation fails
         */
        public SocialSecurityBenefit build() {
            if (calculator == null) {
                calculator = CalculatorFactory.socialSecurityCalculator();
            }
            validate();
            return new SocialSecurityBenefit(this);
        }

        private void validate() {
            MissingRequiredFieldException.requireNonNull(startDate, "startDate");
            if (fraBenefit.compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("FRA benefit cannot be negative", "fraBenefit");
            }
            if (birthYear < 1900 || birthYear > 2100) {
                throw new ValidationException(
                    "Birth year must be between 1900 and 2100", "birthYear");
            }
            if (claimingAgeMonths < 744 || claimingAgeMonths > 840) {
                throw new ValidationException(
                    "Claiming age must be between 62 (744 months) and 70 (840 months)",
                    "claimingAgeMonths");
            }
        }
    }
}
