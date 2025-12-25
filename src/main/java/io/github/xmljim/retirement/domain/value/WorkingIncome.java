package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.util.Objects;

import io.github.xmljim.retirement.domain.annotation.Generated;
import io.github.xmljim.retirement.domain.exception.ValidationException;

/**
 * Configuration for working income (salary) before retirement.
 *
 * <p>Represents the annual salary and cost-of-living adjustment (COLA)
 * rate that applies during the accumulation phase.
 *
 * <p>This is an immutable value object. Use the {@link Builder} to create instances.
 *
 * <p>The {@code priorYearIncome} field is used for SECURE 2.0 ROTH catch-up
 * contribution rules. Employees who earned more than $145,000 in the prior
 * year must make catch-up contributions to a ROTH account.
 */
public final class WorkingIncome {

    private final BigDecimal annualSalary;
    private final BigDecimal colaRate;
    private final BigDecimal priorYearIncome;

    private WorkingIncome(Builder builder) {
        this.annualSalary = builder.annualSalary;
        this.colaRate = builder.colaRate;
        this.priorYearIncome = builder.priorYearIncome;
    }

    /**
     * Returns the annual salary amount.
     *
     * @return the annual salary
     */
    public BigDecimal getAnnualSalary() {
        return annualSalary;
    }

    /**
     * Returns the monthly salary amount (annual / 12).
     *
     * @return the monthly salary
     */
    public BigDecimal getMonthlySalary() {
        return annualSalary.divide(BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_UP);
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
        return annualSalary.compareTo(that.annualSalary) == 0
            && colaRate.compareTo(that.colaRate) == 0
            && Objects.equals(priorYearIncome, that.priorYearIncome);
    }

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(annualSalary, colaRate, priorYearIncome);
    }

    @Generated
    @Override
    public String toString() {
        return "WorkingIncome{" +
            "annualSalary=" + annualSalary +
            ", colaRate=" + colaRate +
            ", priorYearIncome=" + priorYearIncome +
            '}';
    }

    /**
     * Builder for creating WorkingIncome instances.
     */
    public static class Builder {
        private BigDecimal annualSalary = BigDecimal.ZERO;
        private BigDecimal colaRate = BigDecimal.ZERO;
        private BigDecimal priorYearIncome;

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
