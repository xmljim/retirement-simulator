package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

import io.github.xmljim.retirement.domain.annotation.Generated;
import io.github.xmljim.retirement.domain.enums.OtherIncomeType;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;

/**
 * Represents other income sources in retirement.
 *
 * <p>Models flexible income sources such as rental income, part-time work,
 * royalties, dividends, and business income. Supports optional end dates
 * for temporary income and inflation/COLA adjustments.
 *
 * <p>This is an immutable value object. Use the {@link Builder} to create instances.
 */
public final class OtherIncome {

    private static final MathContext MATH_CONTEXT = new MathContext(10, RoundingMode.HALF_UP);
    private static final int MONTHS_PER_YEAR = 12;

    private final String name;
    private final OtherIncomeType incomeType;
    private final BigDecimal monthlyAmount;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final BigDecimal inflationRate;

    private OtherIncome(Builder builder) {
        this.name = builder.name;
        this.incomeType = builder.incomeType;
        this.monthlyAmount = builder.monthlyAmount;
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
        this.inflationRate = builder.inflationRate;
    }

    /**
     * Returns the name or description of this income source.
     *
     * @return the income name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the type of income.
     *
     * @return the income type
     */
    public OtherIncomeType getIncomeType() {
        return incomeType;
    }

    /**
     * Returns the base monthly amount at the start date.
     *
     * @return the monthly amount
     */
    public BigDecimal getMonthlyAmount() {
        return monthlyAmount;
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
     * Returns the date when this income ends, if applicable.
     *
     * @return optional containing the end date, or empty if ongoing
     */
    public Optional<LocalDate> getEndDate() {
        return Optional.ofNullable(endDate);
    }

    /**
     * Returns the annual inflation/COLA adjustment rate, if applicable.
     *
     * @return optional containing the inflation rate, or empty if no adjustment
     */
    public Optional<BigDecimal> getInflationRate() {
        return Optional.ofNullable(inflationRate);
    }

    /**
     * Returns the annual income amount at the start date.
     *
     * @return the annual amount (monthly * 12)
     */
    public BigDecimal getAnnualAmount() {
        return monthlyAmount.multiply(BigDecimal.valueOf(MONTHS_PER_YEAR));
    }

    /**
     * Calculates the monthly income for a given date.
     *
     * <p>Returns zero if:
     * <ul>
     *   <li>The date is before the start date</li>
     *   <li>The date is after the end date (if specified)</li>
     * </ul>
     *
     * <p>If an inflation rate is set, the amount is adjusted annually
     * using compound growth from the start date.
     *
     * @param asOfDate the date for which to calculate income
     * @return the adjusted monthly income, or zero if outside the active period
     */
    public BigDecimal getMonthlyIncome(LocalDate asOfDate) {
        if (!isActive(asOfDate)) {
            return BigDecimal.ZERO;
        }

        if (inflationRate == null || inflationRate.compareTo(BigDecimal.ZERO) == 0) {
            return monthlyAmount;
        }

        long monthsElapsed = ChronoUnit.MONTHS.between(startDate, asOfDate);
        int yearsElapsed = (int) (monthsElapsed / MONTHS_PER_YEAR);

        if (yearsElapsed <= 0) {
            return monthlyAmount;
        }

        // Apply compound inflation: amount * (1 + rate)^years
        BigDecimal inflationMultiplier = BigDecimal.ONE.add(inflationRate)
            .pow(yearsElapsed, MATH_CONTEXT);
        return monthlyAmount.multiply(inflationMultiplier).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the annual income for a given date.
     *
     * @param asOfDate the date for which to calculate income
     * @return the adjusted annual income
     */
    public BigDecimal getAnnualIncome(LocalDate asOfDate) {
        return getMonthlyIncome(asOfDate).multiply(BigDecimal.valueOf(MONTHS_PER_YEAR));
    }

    /**
     * Returns whether this income is active (receiving payments) on the given date.
     *
     * @param asOfDate the date to check
     * @return true if income is active on this date
     */
    public boolean isActive(LocalDate asOfDate) {
        if (asOfDate.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && asOfDate.isAfter(endDate)) {
            return false;
        }
        return true;
    }

    /**
     * Returns whether this income source is ongoing (no end date).
     *
     * @return true if there is no end date
     */
    public boolean isOngoing() {
        return endDate == null;
    }

    /**
     * Returns whether this income type is considered earned income.
     *
     * <p>Earned income may affect Social Security earnings test.
     *
     * @return true if this is earned income
     */
    public boolean isEarnedIncome() {
        return incomeType.isEarnedIncome();
    }

    /**
     * Creates a new builder for OtherIncome.
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
        OtherIncome that = (OtherIncome) o;
        return Objects.equals(name, that.name)
            && incomeType == that.incomeType
            && monthlyAmount.compareTo(that.monthlyAmount) == 0
            && Objects.equals(startDate, that.startDate)
            && Objects.equals(endDate, that.endDate)
            && Objects.equals(inflationRate, that.inflationRate);
    }

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(name, incomeType, monthlyAmount, startDate, endDate, inflationRate);
    }

    @Generated
    @Override
    public String toString() {
        return "OtherIncome{"
            + "name='" + name + '\''
            + ", incomeType=" + incomeType
            + ", monthlyAmount=" + monthlyAmount
            + ", startDate=" + startDate
            + ", endDate=" + endDate
            + ", inflationRate=" + inflationRate
            + '}';
    }

    /**
     * Builder for creating OtherIncome instances.
     */
    public static class Builder {
        private String name;
        private OtherIncomeType incomeType;
        private BigDecimal monthlyAmount = BigDecimal.ZERO;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal inflationRate;

        /**
         * Sets the name or description.
         *
         * @param name the income name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the income type.
         *
         * @param type the income type
         * @return this builder
         */
        public Builder incomeType(OtherIncomeType type) {
            this.incomeType = type;
            return this;
        }

        /**
         * Sets the monthly amount.
         *
         * @param amount the monthly amount
         * @return this builder
         */
        public Builder monthlyAmount(BigDecimal amount) {
            this.monthlyAmount = amount;
            return this;
        }

        /**
         * Sets the monthly amount.
         *
         * @param amount the monthly amount
         * @return this builder
         */
        public Builder monthlyAmount(double amount) {
            return monthlyAmount(BigDecimal.valueOf(amount));
        }

        /**
         * Sets the annual amount (will be converted to monthly).
         *
         * @param amount the annual amount
         * @return this builder
         */
        public Builder annualAmount(BigDecimal amount) {
            this.monthlyAmount = amount.divide(BigDecimal.valueOf(MONTHS_PER_YEAR),
                2, RoundingMode.HALF_UP);
            return this;
        }

        /**
         * Sets the annual amount (will be converted to monthly).
         *
         * @param amount the annual amount
         * @return this builder
         */
        public Builder annualAmount(double amount) {
            return annualAmount(BigDecimal.valueOf(amount));
        }

        /**
         * Sets the start date.
         *
         * @param date the start date
         * @return this builder
         */
        public Builder startDate(LocalDate date) {
            this.startDate = date;
            return this;
        }

        /**
         * Sets the end date (optional).
         *
         * @param date the end date, or null for ongoing income
         * @return this builder
         */
        public Builder endDate(LocalDate date) {
            this.endDate = date;
            return this;
        }

        /**
         * Sets the annual inflation/COLA adjustment rate.
         *
         * @param rate the inflation rate as a decimal (e.g., 0.02 for 2%)
         * @return this builder
         */
        public Builder inflationRate(BigDecimal rate) {
            this.inflationRate = rate;
            return this;
        }

        /**
         * Sets the annual inflation/COLA adjustment rate.
         *
         * @param rate the inflation rate as a decimal (e.g., 0.02 for 2%)
         * @return this builder
         */
        public Builder inflationRate(double rate) {
            return inflationRate(BigDecimal.valueOf(rate));
        }

        /**
         * Builds the OtherIncome instance.
         *
         * @return a new OtherIncome
         * @throws MissingRequiredFieldException if required fields are null
         * @throws ValidationException if validation fails
         */
        public OtherIncome build() {
            validate();
            return new OtherIncome(this);
        }

        private void validate() {
            MissingRequiredFieldException.requireNonNull(name, "name");
            ValidationException.validate("name", name, n -> !n.isBlank(),
                "Name cannot be blank");

            MissingRequiredFieldException.requireNonNull(incomeType, "incomeType");
            MissingRequiredFieldException.requireNonNull(startDate, "startDate");

            ValidationException.validate("monthlyAmount", monthlyAmount,
                v -> v.compareTo(BigDecimal.ZERO) >= 0,
                "Monthly amount cannot be negative");

            if (endDate != null) {
                ValidationException.validate("endDate", endDate,
                    d -> !d.isBefore(startDate),
                    "End date cannot be before start date");
            }
        }
    }
}
