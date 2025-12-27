package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

import io.github.xmljim.retirement.domain.annotation.Generated;
import io.github.xmljim.retirement.domain.exception.ValidationException;

/**
 * Configuration for working income (salary) during employment.
 *
 * <p>Represents the annual salary, cost-of-living adjustment (COLA) rate,
 * and employment period. Provides methods to calculate COLA-adjusted salary
 * for any date within the employment period.
 *
 * <p>This is an immutable value object. Use the {@link Builder} to create instances.
 *
 * <p>Key features:
 * <ul>
 *   <li>{@link #getAnnualSalary(int)} - Get COLA-adjusted salary for a specific year</li>
 *   <li>{@link #getMonthlySalary(LocalDate)} - Get COLA-adjusted monthly salary for a date</li>
 *   <li>{@link #isActiveOn(LocalDate)} - Check if employed on a given date</li>
 * </ul>
 *
 * <p>The {@code priorYearIncome} field is used for SECURE 2.0 ROTH catch-up
 * contribution rules. Employees who earned more than $145,000 in the prior
 * year must make catch-up contributions to a ROTH account.
 */
public final class WorkingIncome {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);
    private static final MathContext PRECISION = MathContext.DECIMAL128;

    private final BigDecimal annualSalary;
    private final BigDecimal colaRate;
    private final BigDecimal priorYearIncome;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final int colaMonth;

    private WorkingIncome(Builder builder) {
        this.annualSalary = builder.annualSalary;
        this.colaRate = builder.colaRate;
        this.priorYearIncome = builder.priorYearIncome;
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
        this.colaMonth = builder.colaMonth;
    }

    /**
     * Returns the base annual salary amount (without COLA adjustments).
     *
     * @return the base annual salary
     */
    public BigDecimal getBaseSalary() {
        return annualSalary;
    }

    /**
     * Returns the base annual salary amount (without COLA adjustments).
     *
     * <p>This is equivalent to {@link #getBaseSalary()}. For COLA-adjusted
     * salary, use {@link #getAnnualSalary(int)}.
     *
     * @return the base annual salary
     */
    public BigDecimal getAnnualSalary() {
        return annualSalary;
    }

    /**
     * Returns the base monthly salary amount (annual / 12, without COLA).
     *
     * @return the base monthly salary
     */
    public BigDecimal getBaseMonthlySalary() {
        return annualSalary.divide(MONTHS_PER_YEAR, SCALE, ROUNDING);
    }

    /**
     * Returns the base monthly salary amount (annual / 12, without COLA).
     *
     * <p>This is equivalent to {@link #getBaseMonthlySalary()}. For COLA-adjusted
     * monthly salary, use {@link #getMonthlySalary(LocalDate)}.
     *
     * @return the base monthly salary
     */
    public BigDecimal getMonthlySalary() {
        return getBaseMonthlySalary();
    }

    /**
     * Returns the COLA-adjusted annual salary for a specific year.
     *
     * <p>COLA is compounded annually from the start year. The formula is:
     * {@code baseSalary * (1 + colaRate)^yearsOfCola}
     *
     * @param year the year to calculate salary for
     * @return the COLA-adjusted annual salary, or ZERO if not employed that year
     */
    public BigDecimal getAnnualSalary(int year) {
        if (startDate == null) {
            return annualSalary;
        }
        if (!isActiveInYear(year)) {
            return BigDecimal.ZERO;
        }
        int yearsOfCola = calculateYearsOfCola(year);
        return applyColaAdjustment(annualSalary, yearsOfCola);
    }

    /**
     * Returns the COLA-adjusted monthly salary for a specific date.
     *
     * <p>Returns ZERO if not employed on the given date.
     *
     * @param date the date to calculate salary for
     * @return the COLA-adjusted monthly salary, or ZERO if not employed
     */
    public BigDecimal getMonthlySalary(LocalDate date) {
        if (date == null) {
            return BigDecimal.ZERO;
        }
        if (!isActiveOn(date)) {
            return BigDecimal.ZERO;
        }
        BigDecimal adjustedAnnual = getAnnualSalary(date.getYear());
        return adjustedAnnual.divide(MONTHS_PER_YEAR, SCALE, ROUNDING);
    }

    /**
     * Checks if employed on the given date.
     *
     * <p>Returns true if the date is on or after startDate and before endDate
     * (if endDate is specified).
     *
     * @param date the date to check
     * @return true if employed on the date
     */
    public boolean isActiveOn(LocalDate date) {
        if (date == null || startDate == null) {
            return startDate == null;
        }
        if (date.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && !date.isBefore(endDate)) {
            return false;
        }
        return true;
    }

    /**
     * Returns the employment start date.
     *
     * @return the start date, or null if not specified
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * Returns the employment end date (typically retirement date).
     *
     * @return the end date, or null if not specified
     */
    public LocalDate getEndDate() {
        return endDate;
    }

    /**
     * Returns the month when COLA is applied (1 = January, 12 = December).
     *
     * @return the COLA month
     */
    public int getColaMonth() {
        return colaMonth;
    }

    private boolean isActiveInYear(int year) {
        if (startDate == null) {
            return true;
        }
        if (year < startDate.getYear()) {
            return false;
        }
        if (endDate != null && year >= endDate.getYear()) {
            return false;
        }
        return true;
    }

    private int calculateYearsOfCola(int targetYear) {
        if (startDate == null) {
            return 0;
        }
        int startYear = startDate.getYear();
        return Math.max(0, targetYear - startYear);
    }

    private BigDecimal applyColaAdjustment(BigDecimal amount, int years) {
        if (years <= 0 || colaRate.compareTo(BigDecimal.ZERO) == 0) {
            return amount;
        }
        BigDecimal multiplier = BigDecimal.ONE.add(colaRate).pow(years, PRECISION);
        return amount.multiply(multiplier).setScale(SCALE, ROUNDING);
    }

    /**
     * Returns the annual COLA adjustment rate as a decimal.
     *
     * <p>For example, 0.02 represents a 2% annual raise.
     *
     * @return the COLA rate
     */
    public BigDecimal getColaRate() {
        return colaRate;
    }

    /**
     * Returns the prior year income for SECURE 2.0 ROTH catch-up rules.
     *
     * <p>Under SECURE 2.0, employees who earned more than $145,000 in the prior
     * year must make catch-up contributions to a ROTH account. This field stores
     * that prior year income for the calculation.
     *
     * @return the prior year income, or null if not specified
     */
    public BigDecimal getPriorYearIncome() {
        return priorYearIncome;
    }

    /**
     * Creates a WorkingIncome with the specified salary and COLA rate.
     *
     * @param annualSalary the annual salary
     * @param colaRate the annual COLA rate as a decimal
     * @return a new WorkingIncome
     */
    public static WorkingIncome of(double annualSalary, double colaRate) {
        return builder()
            .annualSalary(annualSalary)
            .colaRate(colaRate)
            .build();
    }

    /**
     * Creates a new builder for WorkingIncome.
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
        WorkingIncome that = (WorkingIncome) o;
        return colaMonth == that.colaMonth
            && annualSalary.compareTo(that.annualSalary) == 0
            && colaRate.compareTo(that.colaRate) == 0
            && Objects.equals(priorYearIncome, that.priorYearIncome)
            && Objects.equals(startDate, that.startDate)
            && Objects.equals(endDate, that.endDate);
    }

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(annualSalary, colaRate, priorYearIncome, startDate, endDate, colaMonth);
    }

    @Generated
    @Override
    public String toString() {
        return "WorkingIncome{" +
            "annualSalary=" + annualSalary +
            ", colaRate=" + colaRate +
            ", priorYearIncome=" + priorYearIncome +
            ", startDate=" + startDate +
            ", endDate=" + endDate +
            ", colaMonth=" + colaMonth +
            '}';
    }

    /**
     * Builder for creating WorkingIncome instances.
     */
    public static class Builder {
        private BigDecimal annualSalary = BigDecimal.ZERO;
        private BigDecimal colaRate = BigDecimal.ZERO;
        private BigDecimal priorYearIncome;
        private LocalDate startDate;
        private LocalDate endDate;
        private int colaMonth = 1;

        /**
         * Sets the annual salary amount.
         *
         * @param salary the annual salary
         * @return this builder
         */
        public Builder annualSalary(BigDecimal salary) {
            this.annualSalary = salary;
            return this;
        }

        /**
         * Sets the annual salary amount.
         *
         * @param salary the annual salary
         * @return this builder
         */
        public Builder annualSalary(double salary) {
            return annualSalary(BigDecimal.valueOf(salary));
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
         * Sets the prior year income for SECURE 2.0 ROTH catch-up rules.
         *
         * @param income the prior year income
         * @return this builder
         */
        public Builder priorYearIncome(BigDecimal income) {
            this.priorYearIncome = income;
            return this;
        }

        /**
         * Sets the prior year income for SECURE 2.0 ROTH catch-up rules.
         *
         * @param income the prior year income
         * @return this builder
         */
        public Builder priorYearIncome(double income) {
            return priorYearIncome(BigDecimal.valueOf(income));
        }

        /**
         * Sets the employment start date.
         *
         * @param date the start date
         * @return this builder
         */
        public Builder startDate(LocalDate date) {
            this.startDate = date;
            return this;
        }

        /**
         * Sets the employment end date (typically retirement date).
         *
         * @param date the end date
         * @return this builder
         */
        public Builder endDate(LocalDate date) {
            this.endDate = date;
            return this;
        }

        /**
         * Sets the month when COLA is applied (1 = January, 12 = December).
         *
         * <p>Defaults to January (1) if not specified.
         *
         * @param month the COLA month (1-12)
         * @return this builder
         */
        public Builder colaMonth(int month) {
            this.colaMonth = month;
            return this;
        }

        /**
         * Builds the WorkingIncome instance.
         *
         * @return a new WorkingIncome
         * @throws ValidationException if annualSalary is negative
         */
        public WorkingIncome build() {
            validate();
            return new WorkingIncome(this);
        }

        private void validate() {
            if (annualSalary.compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("Annual salary cannot be negative", "annualSalary");
            }
        }
    }
}
