package io.github.xmljim.retirement.simulation.income;

import java.math.BigDecimal;

/**
 * Aggregates all income sources for a single month.
 *
 * <p>MonthlyIncome captures income from all sources:
 * <ul>
 *   <li>Salary income (working phase only)</li>
 *   <li>Social Security benefits (distribution phase)</li>
 *   <li>Pension income (distribution phase)</li>
 *   <li>Annuity payments (distribution phase)</li>
 *   <li>Other income (any phase - rental, part-time, etc.)</li>
 * </ul>
 *
 * <p>Usage in simulation loop:
 * <pre>{@code
 * MonthlyIncome income = incomeProcessor.process(persons, month, phase);
 * BigDecimal gap = expenses.subtract(income.total());
 * }</pre>
 *
 * @param salaryIncome         income from employment
 * @param socialSecurityIncome Social Security benefits
 * @param pensionIncome        defined benefit pension payments
 * @param annuityIncome        annuity payments
 * @param otherIncome          rental, part-time, royalties, etc.
 */
public record MonthlyIncome(
    BigDecimal salaryIncome,
    BigDecimal socialSecurityIncome,
    BigDecimal pensionIncome,
    BigDecimal annuityIncome,
    BigDecimal otherIncome
) {

    // Compact constructor ensuring non-null values
    public MonthlyIncome {
        salaryIncome = salaryIncome != null ? salaryIncome : BigDecimal.ZERO;
        socialSecurityIncome = socialSecurityIncome != null ? socialSecurityIncome : BigDecimal.ZERO;
        pensionIncome = pensionIncome != null ? pensionIncome : BigDecimal.ZERO;
        annuityIncome = annuityIncome != null ? annuityIncome : BigDecimal.ZERO;
        otherIncome = otherIncome != null ? otherIncome : BigDecimal.ZERO;
    }

    /**
     * Returns the total income from all sources.
     *
     * @return sum of all income components
     */
    public BigDecimal total() {
        return salaryIncome
            .add(socialSecurityIncome)
            .add(pensionIncome)
            .add(annuityIncome)
            .add(otherIncome);
    }

    /**
     * Returns total non-salary income (retirement income sources).
     *
     * <p>This is useful for calculating the "gap" during distribution:
     * <pre>
     * gap = expenses - totalNonSalary()
     * </pre>
     *
     * @return sum of SS, pension, annuity, and other income
     */
    public BigDecimal totalNonSalary() {
        return socialSecurityIncome
            .add(pensionIncome)
            .add(annuityIncome)
            .add(otherIncome);
    }

    /**
     * Returns true if there is any salary income.
     *
     * @return true if salary income is greater than zero
     */
    public boolean hasSalaryIncome() {
        return salaryIncome.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Returns true if there is any retirement income (non-salary).
     *
     * @return true if any retirement income source is greater than zero
     */
    public boolean hasRetirementIncome() {
        return totalNonSalary().compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Creates a MonthlyIncome with only salary.
     *
     * @param salary the salary amount
     * @return a new MonthlyIncome with only salary income
     */
    public static MonthlyIncome ofSalary(BigDecimal salary) {
        return new MonthlyIncome(salary, null, null, null, null);
    }

    /**
     * Creates a MonthlyIncome with zero for all sources.
     *
     * @return a zero income MonthlyIncome
     */
    public static MonthlyIncome zero() {
        return new MonthlyIncome(null, null, null, null, null);
    }

    /**
     * Creates a new builder for MonthlyIncome.
     *
     * @return a builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating MonthlyIncome instances.
     */
    public static class Builder {
        private BigDecimal salaryIncome = BigDecimal.ZERO;
        private BigDecimal socialSecurityIncome = BigDecimal.ZERO;
        private BigDecimal pensionIncome = BigDecimal.ZERO;
        private BigDecimal annuityIncome = BigDecimal.ZERO;
        private BigDecimal otherIncome = BigDecimal.ZERO;

        /**
         * Sets the salary income.
         *
         * @param amount the salary amount
         * @return this builder
         */
        public Builder salaryIncome(BigDecimal amount) {
            this.salaryIncome = amount;
            return this;
        }

        /**
         * Sets the Social Security income.
         *
         * @param amount the SS amount
         * @return this builder
         */
        public Builder socialSecurityIncome(BigDecimal amount) {
            this.socialSecurityIncome = amount;
            return this;
        }

        /**
         * Sets the pension income.
         *
         * @param amount the pension amount
         * @return this builder
         */
        public Builder pensionIncome(BigDecimal amount) {
            this.pensionIncome = amount;
            return this;
        }

        /**
         * Sets the annuity income.
         *
         * @param amount the annuity amount
         * @return this builder
         */
        public Builder annuityIncome(BigDecimal amount) {
            this.annuityIncome = amount;
            return this;
        }

        /**
         * Sets the other income.
         *
         * @param amount the other income amount
         * @return this builder
         */
        public Builder otherIncome(BigDecimal amount) {
            this.otherIncome = amount;
            return this;
        }

        /**
         * Adds to the salary income.
         *
         * @param amount the amount to add
         * @return this builder
         */
        public Builder addSalaryIncome(BigDecimal amount) {
            this.salaryIncome = this.salaryIncome.add(amount);
            return this;
        }

        /**
         * Adds to the Social Security income.
         *
         * @param amount the amount to add
         * @return this builder
         */
        public Builder addSocialSecurityIncome(BigDecimal amount) {
            this.socialSecurityIncome = this.socialSecurityIncome.add(amount);
            return this;
        }

        /**
         * Adds to the pension income.
         *
         * @param amount the amount to add
         * @return this builder
         */
        public Builder addPensionIncome(BigDecimal amount) {
            this.pensionIncome = this.pensionIncome.add(amount);
            return this;
        }

        /**
         * Adds to the annuity income.
         *
         * @param amount the amount to add
         * @return this builder
         */
        public Builder addAnnuityIncome(BigDecimal amount) {
            this.annuityIncome = this.annuityIncome.add(amount);
            return this;
        }

        /**
         * Adds to the other income.
         *
         * @param amount the amount to add
         * @return this builder
         */
        public Builder addOtherIncome(BigDecimal amount) {
            this.otherIncome = this.otherIncome.add(amount);
            return this;
        }

        /**
         * Builds the MonthlyIncome instance.
         *
         * @return a new MonthlyIncome
         */
        public MonthlyIncome build() {
            return new MonthlyIncome(
                salaryIncome,
                socialSecurityIncome,
                pensionIncome,
                annuityIncome,
                otherIncome
            );
        }
    }
}
