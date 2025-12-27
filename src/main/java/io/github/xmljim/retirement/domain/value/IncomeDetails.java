package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;

import io.github.xmljim.retirement.domain.exception.ValidationException;

/**
 * Captures income components needed for MAGI calculation.
 *
 * <p>MAGI (Modified Adjusted Gross Income) for IRA purposes is calculated as:
 * AGI + certain deductions that were subtracted to get AGI.
 *
 * <p>Example usage:
 * <pre>{@code
 * IncomeDetails income = IncomeDetails.builder()
 *     .adjustedGrossIncome(new BigDecimal("150000"))
 *     .studentLoanInterest(new BigDecimal("2500"))
 *     .build();
 *
 * BigDecimal magi = magiCalculator.calculate(income);
 * }</pre>
 *
 * @param adjustedGrossIncome AGI from tax return (Line 11 of Form 1040)
 * @param studentLoanInterest student loan interest deduction claimed
 * @param tuitionAndFees tuition and fees deduction (if applicable)
 * @param foreignEarnedIncomeExclusion foreign earned income excluded
 * @param foreignHousingExclusion foreign housing exclusion or deduction
 * @param savingsBondInterestExclusion Series EE/I bond interest excluded
 * @param adoptionBenefitsExclusion employer-provided adoption benefits excluded
 */
public record IncomeDetails(
    BigDecimal adjustedGrossIncome,
    BigDecimal studentLoanInterest,
    BigDecimal tuitionAndFees,
    BigDecimal foreignEarnedIncomeExclusion,
    BigDecimal foreignHousingExclusion,
    BigDecimal savingsBondInterestExclusion,
    BigDecimal adoptionBenefitsExclusion
) {
    /**
     * Creates IncomeDetails with validation and null handling.
     */
    public IncomeDetails {
        if (adjustedGrossIncome == null) {
            adjustedGrossIncome = BigDecimal.ZERO;
        }
        if (adjustedGrossIncome.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("adjustedGrossIncome", "AGI cannot be negative");
        }
        if (studentLoanInterest == null) {
            studentLoanInterest = BigDecimal.ZERO;
        }
        if (tuitionAndFees == null) {
            tuitionAndFees = BigDecimal.ZERO;
        }
        if (foreignEarnedIncomeExclusion == null) {
            foreignEarnedIncomeExclusion = BigDecimal.ZERO;
        }
        if (foreignHousingExclusion == null) {
            foreignHousingExclusion = BigDecimal.ZERO;
        }
        if (savingsBondInterestExclusion == null) {
            savingsBondInterestExclusion = BigDecimal.ZERO;
        }
        if (adoptionBenefitsExclusion == null) {
            adoptionBenefitsExclusion = BigDecimal.ZERO;
        }
    }

    /**
     * Returns the total of all MAGI add-backs.
     *
     * @return sum of all deductions to add back to AGI
     */
    public BigDecimal getTotalAddBacks() {
        return studentLoanInterest
            .add(tuitionAndFees)
            .add(foreignEarnedIncomeExclusion)
            .add(foreignHousingExclusion)
            .add(savingsBondInterestExclusion)
            .add(adoptionBenefitsExclusion);
    }

    /**
     * Creates a builder for IncomeDetails.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates IncomeDetails with just AGI (no add-backs).
     *
     * @param agi the adjusted gross income
     * @return IncomeDetails with only AGI set
     */
    public static IncomeDetails ofAgi(BigDecimal agi) {
        return builder().adjustedGrossIncome(agi).build();
    }

    /**
     * Builder for IncomeDetails.
     */
    public static final class Builder {
        private BigDecimal adjustedGrossIncome;
        private BigDecimal studentLoanInterest;
        private BigDecimal tuitionAndFees;
        private BigDecimal foreignEarnedIncomeExclusion;
        private BigDecimal foreignHousingExclusion;
        private BigDecimal savingsBondInterestExclusion;
        private BigDecimal adoptionBenefitsExclusion;

        private Builder() {
        }

        /**
         * Sets the adjusted gross income.
         *
         * @param agi the AGI
         * @return this builder
         */
        public Builder adjustedGrossIncome(BigDecimal agi) {
            this.adjustedGrossIncome = agi;
            return this;
        }

        /**
         * Sets the adjusted gross income from a double.
         *
         * @param agi the AGI
         * @return this builder
         */
        public Builder adjustedGrossIncome(double agi) {
            this.adjustedGrossIncome = BigDecimal.valueOf(agi);
            return this;
        }

        /**
         * Sets the student loan interest deduction.
         *
         * @param amount the deduction amount
         * @return this builder
         */
        public Builder studentLoanInterest(BigDecimal amount) {
            this.studentLoanInterest = amount;
            return this;
        }

        /**
         * Sets the tuition and fees deduction.
         *
         * @param amount the deduction amount
         * @return this builder
         */
        public Builder tuitionAndFees(BigDecimal amount) {
            this.tuitionAndFees = amount;
            return this;
        }

        /**
         * Sets the foreign earned income exclusion.
         *
         * @param amount the exclusion amount
         * @return this builder
         */
        public Builder foreignEarnedIncomeExclusion(BigDecimal amount) {
            this.foreignEarnedIncomeExclusion = amount;
            return this;
        }

        /**
         * Sets the foreign housing exclusion.
         *
         * @param amount the exclusion amount
         * @return this builder
         */
        public Builder foreignHousingExclusion(BigDecimal amount) {
            this.foreignHousingExclusion = amount;
            return this;
        }

        /**
         * Sets the savings bond interest exclusion.
         *
         * @param amount the exclusion amount
         * @return this builder
         */
        public Builder savingsBondInterestExclusion(BigDecimal amount) {
            this.savingsBondInterestExclusion = amount;
            return this;
        }

        /**
         * Sets the adoption benefits exclusion.
         *
         * @param amount the exclusion amount
         * @return this builder
         */
        public Builder adoptionBenefitsExclusion(BigDecimal amount) {
            this.adoptionBenefitsExclusion = amount;
            return this;
        }

        /**
         * Builds the IncomeDetails.
         *
         * @return a new IncomeDetails instance
         */
        public IncomeDetails build() {
            return new IncomeDetails(
                adjustedGrossIncome,
                studentLoanInterest,
                tuitionAndFees,
                foreignEarnedIncomeExclusion,
                foreignHousingExclusion,
                savingsBondInterestExclusion,
                adoptionBenefitsExclusion
            );
        }
    }
}
