package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import io.github.xmljim.retirement.domain.annotation.Generated;
import io.github.xmljim.retirement.domain.enums.PensionPaymentForm;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;

/**
 * Represents a defined benefit pension plan.
 *
 * <p>A pension provides guaranteed monthly income in retirement, typically
 * based on years of service and final salary. Key characteristics:
 * <ul>
 *   <li>Fixed monthly benefit amount (may be adjusted for COLA)</li>
 *   <li>Start date when payments begin</li>
 *   <li>Payment form determines survivor benefits</li>
 *   <li>Some pensions have COLA, many do not</li>
 * </ul>
 *
 * <p><b>Government vs Private Pensions:</b>
 * <ul>
 *   <li>Government pensions (FERS, state plans) typically have COLA</li>
 *   <li>Private pensions rarely have COLA (benefit is fixed)</li>
 * </ul>
 *
 * <p>This is an immutable value object. Use the {@link Builder} to create instances.
 *
 * @see PensionPaymentForm
 */
public final class Pension {

    private static final int MONTHS_PER_YEAR = 12;
    private static final int SCALE = 10;

    private final String name;
    private final BigDecimal monthlyBenefit;
    private final LocalDate startDate;
    private final PensionPaymentForm paymentForm;
    private final BigDecimal colaRate;
    private final BigDecimal customReduction;
    private final Integer periodCertainYears;

    private Pension(Builder builder) {
        this.name = builder.name;
        this.monthlyBenefit = builder.monthlyBenefit;
        this.startDate = builder.startDate;
        this.paymentForm = builder.paymentForm;
        this.colaRate = builder.colaRate;
        this.customReduction = builder.customReduction;
        this.periodCertainYears = builder.periodCertainYears;
    }

    /**
     * Returns the pension name or identifier.
     *
     * @return the pension name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the base monthly benefit amount.
     *
     * <p>This is the benefit before any COLA adjustments. If a payment form
     * with survivor benefits was selected, this should already reflect any
     * actuarial reduction.
     *
     * @return the monthly benefit
     */
    public BigDecimal getMonthlyBenefit() {
        return monthlyBenefit;
    }

    /**
     * Returns the annual benefit (monthly * 12).
     *
     * @return the annual benefit
     */
    public BigDecimal getAnnualBenefit() {
        return monthlyBenefit.multiply(BigDecimal.valueOf(MONTHS_PER_YEAR));
    }

    /**
     * Returns the date when pension payments begin.
     *
     * @return the start date
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * Returns the payment form (survivor benefit option).
     *
     * @return the payment form
     */
    public PensionPaymentForm getPaymentForm() {
        return paymentForm;
    }

    /**
     * Returns the annual COLA rate, if any.
     *
     * <p>Most private pensions have no COLA (returns empty).
     * Government pensions often have fixed or CPI-linked COLA.
     *
     * @return the COLA rate as a decimal, or empty if no COLA
     */
    public Optional<BigDecimal> getColaRate() {
        return Optional.ofNullable(colaRate);
    }

    /**
     * Returns the custom actuarial reduction, if specified.
     *
     * <p>If provided, this overrides the typical reduction for the payment form.
     * Use this when you have the actual reduction from the pension plan.
     *
     * @return the custom reduction, or empty to use typical reduction
     */
    public Optional<BigDecimal> getCustomReduction() {
        return Optional.ofNullable(customReduction);
    }

    /**
     * Returns the period certain years, if applicable.
     *
     * <p>Only relevant for {@link PensionPaymentForm#PERIOD_CERTAIN}.
     *
     * @return the guaranteed period in years, or empty if not period certain
     */
    public Optional<Integer> getPeriodCertainYears() {
        return Optional.ofNullable(periodCertainYears);
    }

    /**
     * Returns whether this pension has a COLA adjustment.
     *
     * @return true if COLA is applied
     */
    public boolean hasCola() {
        return colaRate != null && colaRate.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Returns whether this pension provides survivor benefits.
     *
     * @return true if survivor benefits are provided
     */
    public boolean hasSurvivorBenefit() {
        return paymentForm.hasSurvivorBenefit();
    }

    /**
     * Calculates the monthly benefit for a specific date, applying COLA.
     *
     * <p>If the date is before the start date, returns zero.
     * COLA is applied annually based on years since start date.
     *
     * @param date the date to calculate benefit for
     * @return the COLA-adjusted monthly benefit, or zero if before start date
     */
    public BigDecimal getMonthlyBenefit(LocalDate date) {
        if (date == null || date.isBefore(startDate)) {
            return BigDecimal.ZERO;
        }

        if (!hasCola()) {
            return monthlyBenefit;
        }

        int yearsElapsed = date.getYear() - startDate.getYear();
        if (date.getMonthValue() < startDate.getMonthValue()
            || (date.getMonthValue() == startDate.getMonthValue()
                && date.getDayOfMonth() < startDate.getDayOfMonth())) {
            yearsElapsed--;
        }

        if (yearsElapsed <= 0) {
            return monthlyBenefit;
        }

        // Apply compound COLA: benefit * (1 + rate)^years
        BigDecimal colaMultiplier = BigDecimal.ONE.add(colaRate)
            .pow(yearsElapsed);

        return monthlyBenefit.multiply(colaMultiplier)
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the annual benefit for a specific year, applying COLA.
     *
     * @param year the year to calculate benefit for
     * @return the COLA-adjusted annual benefit, or zero if before start year
     */
    public BigDecimal getAnnualBenefit(int year) {
        LocalDate dateInYear = LocalDate.of(year, 6, 15);  // Mid-year
        return getMonthlyBenefit(dateInYear)
            .multiply(BigDecimal.valueOf(MONTHS_PER_YEAR));
    }

    /**
     * Calculates the survivor benefit amount.
     *
     * <p>Returns the benefit that would be paid to a survivor based on
     * the payment form's survivor percentage.
     *
     * @param date the date to calculate for (for COLA adjustment)
     * @return the monthly survivor benefit, or zero if no survivor benefit
     */
    public BigDecimal getSurvivorBenefit(LocalDate date) {
        if (!hasSurvivorBenefit()) {
            return BigDecimal.ZERO;
        }

        BigDecimal currentBenefit = getMonthlyBenefit(date);
        BigDecimal survivorPercentage = paymentForm.getSurvivorPercentage();

        return currentBenefit.multiply(survivorPercentage)
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Returns whether the pension is active (paying benefits) on a given date.
     *
     * @param date the date to check
     * @return true if pension is paying on this date
     */
    public boolean isActiveOn(LocalDate date) {
        if (date == null) {
            return false;
        }
        return !date.isBefore(startDate);
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
        Pension pension = (Pension) o;
        return Objects.equals(name, pension.name)
            && monthlyBenefit.compareTo(pension.monthlyBenefit) == 0
            && Objects.equals(startDate, pension.startDate)
            && paymentForm == pension.paymentForm
            && Objects.equals(colaRate, pension.colaRate);
    }

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(name, monthlyBenefit, startDate, paymentForm, colaRate);
    }

    @Generated
    @Override
    public String toString() {
        return "Pension{"
            + "name='" + name + '\''
            + ", monthlyBenefit=" + monthlyBenefit
            + ", startDate=" + startDate
            + ", paymentForm=" + paymentForm
            + ", colaRate=" + colaRate
            + '}';
    }

    /**
     * Builder for creating Pension instances.
     */
    public static class Builder {
        private String name;
        private BigDecimal monthlyBenefit;
        private LocalDate startDate;
        private PensionPaymentForm paymentForm = PensionPaymentForm.SINGLE_LIFE;
        private BigDecimal colaRate;
        private BigDecimal customReduction;
        private Integer periodCertainYears;

        /**
         * Sets the pension name or identifier.
         *
         * @param name the pension name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the monthly benefit amount.
         *
         * @param benefit the monthly benefit
         * @return this builder
         */
        public Builder monthlyBenefit(BigDecimal benefit) {
            this.monthlyBenefit = benefit;
            return this;
        }

        /**
         * Sets the monthly benefit amount.
         *
         * @param benefit the monthly benefit
         * @return this builder
         */
        public Builder monthlyBenefit(double benefit) {
            return monthlyBenefit(BigDecimal.valueOf(benefit));
        }

        /**
         * Sets the start date when benefits begin.
         *
         * @param startDate the start date
         * @return this builder
         */
        public Builder startDate(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        /**
         * Sets the payment form (survivor benefit option).
         *
         * @param paymentForm the payment form
         * @return this builder
         */
        public Builder paymentForm(PensionPaymentForm paymentForm) {
            this.paymentForm = paymentForm;
            return this;
        }

        /**
         * Sets the annual COLA rate.
         *
         * <p>Use null or don't call this method for pensions without COLA.
         *
         * @param rate the COLA rate as a decimal (e.g., 0.02 for 2%)
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
         * Sets a custom actuarial reduction.
         *
         * <p>Use this when you have the actual reduction from the pension plan,
         * rather than using the typical reduction for the payment form.
         *
         * @param reduction the reduction as a decimal (e.g., 0.15 for 15%)
         * @return this builder
         */
        public Builder customReduction(BigDecimal reduction) {
            this.customReduction = reduction;
            return this;
        }

        /**
         * Sets a custom actuarial reduction.
         *
         * @param reduction the reduction as a decimal
         * @return this builder
         */
        public Builder customReduction(double reduction) {
            return customReduction(BigDecimal.valueOf(reduction));
        }

        /**
         * Sets the period certain years for PERIOD_CERTAIN payment form.
         *
         * @param years the guaranteed period (typically 10, 15, or 20)
         * @return this builder
         */
        public Builder periodCertainYears(int years) {
            this.periodCertainYears = years;
            return this;
        }

        /**
         * Builds the Pension instance.
         *
         * @return a new Pension
         * @throws MissingRequiredFieldException if required fields are missing
         * @throws ValidationException if validation fails
         */
        public Pension build() {
            validate();
            return new Pension(this);
        }

        private void validate() {
            if (name == null || name.isBlank()) {
                throw new MissingRequiredFieldException("name");
            }
            if (monthlyBenefit == null) {
                throw new MissingRequiredFieldException("monthlyBenefit");
            }
            if (monthlyBenefit.compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("Monthly benefit cannot be negative", "monthlyBenefit");
            }
            if (startDate == null) {
                throw new MissingRequiredFieldException("startDate");
            }
            if (paymentForm == null) {
                throw new MissingRequiredFieldException("paymentForm");
            }
            if (paymentForm == PensionPaymentForm.PERIOD_CERTAIN && periodCertainYears == null) {
                throw new MissingRequiredFieldException("periodCertainYears",
                    "Period certain years required when using PERIOD_CERTAIN payment form");
            }
            if (colaRate != null && colaRate.compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("COLA rate cannot be negative", "colaRate");
            }
        }
    }
}
