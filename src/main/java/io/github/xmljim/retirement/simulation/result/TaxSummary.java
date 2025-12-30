package io.github.xmljim.retirement.simulation.result;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Captures tax-related information for a simulation month.
 *
 * <p>This record tracks taxable income components and calculated tax liability.
 * It's designed to support future Roth conversion analysis by tracking
 * marginal tax brackets.
 *
 * <p>Tax components include:
 * <ul>
 *   <li>Taxable withdrawals from pre-tax accounts (Traditional IRA/401k)</li>
 *   <li>Tax-free withdrawals from Roth accounts</li>
 *   <li>Taxable portion of Social Security benefits (0-85%)</li>
 *   <li>Other taxable income (pension, annuity, etc.)</li>
 * </ul>
 *
 * @param taxableIncome total taxable income this month
 * @param taxableSSIncome portion of Social Security that's taxable (up to 85%)
 * @param taxableWithdrawals pre-tax account withdrawals (Traditional IRA/401k)
 * @param taxFreeWithdrawals Roth withdrawals (not taxable)
 * @param federalTaxLiability calculated federal tax for the month
 * @param effectiveTaxRate federal tax divided by taxable income
 * @param marginalTaxBracket current marginal bracket (for Roth conversion decisions)
 * @param rothConversionAmount Roth conversion amount if any this month
 * @param rothConversionTax tax liability from Roth conversion
 *
 * @see MonthlySnapshot
 */
public record TaxSummary(
        BigDecimal taxableIncome,
        BigDecimal taxableSSIncome,
        BigDecimal taxableWithdrawals,
        BigDecimal taxFreeWithdrawals,
        BigDecimal federalTaxLiability,
        BigDecimal effectiveTaxRate,
        BigDecimal marginalTaxBracket,
        BigDecimal rothConversionAmount,
        BigDecimal rothConversionTax
) {

    private static final int SCALE = 2;
    private static final int RATE_SCALE = 4;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /**
     * Compact constructor with null-safe defaults.
     */
    public TaxSummary {
        if (taxableIncome == null) {
            taxableIncome = BigDecimal.ZERO;
        }
        if (taxableSSIncome == null) {
            taxableSSIncome = BigDecimal.ZERO;
        }
        if (taxableWithdrawals == null) {
            taxableWithdrawals = BigDecimal.ZERO;
        }
        if (taxFreeWithdrawals == null) {
            taxFreeWithdrawals = BigDecimal.ZERO;
        }
        if (federalTaxLiability == null) {
            federalTaxLiability = BigDecimal.ZERO;
        }
        if (effectiveTaxRate == null) {
            effectiveTaxRate = BigDecimal.ZERO;
        }
        if (marginalTaxBracket == null) {
            marginalTaxBracket = BigDecimal.ZERO;
        }
        if (rothConversionAmount == null) {
            rothConversionAmount = BigDecimal.ZERO;
        }
        if (rothConversionTax == null) {
            rothConversionTax = BigDecimal.ZERO;
        }
    }

    /**
     * Creates an empty TaxSummary with all zeros.
     *
     * @return a zero-valued TaxSummary
     */
    public static TaxSummary empty() {
        return new TaxSummary(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }

    /**
     * Calculates total withdrawals (taxable + tax-free).
     *
     * @return sum of all withdrawals
     */
    public BigDecimal totalWithdrawals() {
        return taxableWithdrawals.add(taxFreeWithdrawals).setScale(SCALE, ROUNDING);
    }

    /**
     * Calculates the total tax liability (federal + Roth conversion).
     *
     * @return combined tax liability
     */
    public BigDecimal totalTaxLiability() {
        return federalTaxLiability.add(rothConversionTax).setScale(SCALE, ROUNDING);
    }

    /**
     * Indicates whether a Roth conversion occurred this month.
     *
     * @return true if rothConversionAmount is greater than zero
     */
    public boolean hadRothConversion() {
        return rothConversionAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Indicates whether any taxes are owed this month.
     *
     * @return true if total tax liability is greater than zero
     */
    public boolean hasTaxLiability() {
        return totalTaxLiability().compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Calculates the percentage of withdrawals that are taxable.
     *
     * @return taxable withdrawals as a percentage of total, or ZERO if no withdrawals
     */
    public BigDecimal taxableWithdrawalPercentage() {
        BigDecimal total = totalWithdrawals();
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return taxableWithdrawals
                .divide(total, RATE_SCALE, ROUNDING);
    }

    /**
     * Creates a builder for constructing TaxSummary instances.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for TaxSummary.
     */
    public static final class Builder {
        private BigDecimal taxableIncome = BigDecimal.ZERO;
        private BigDecimal taxableSSIncome = BigDecimal.ZERO;
        private BigDecimal taxableWithdrawals = BigDecimal.ZERO;
        private BigDecimal taxFreeWithdrawals = BigDecimal.ZERO;
        private BigDecimal federalTaxLiability = BigDecimal.ZERO;
        private BigDecimal effectiveTaxRate = BigDecimal.ZERO;
        private BigDecimal marginalTaxBracket = BigDecimal.ZERO;
        private BigDecimal rothConversionAmount = BigDecimal.ZERO;
        private BigDecimal rothConversionTax = BigDecimal.ZERO;

        private Builder() {
        }

        /**
         * Sets the taxable income.
         *
         * @param amount the taxable income
         * @return this builder
         */
        public Builder taxableIncome(BigDecimal amount) {
            this.taxableIncome = amount;
            return this;
        }

        /**
         * Sets the taxable Social Security income.
         *
         * @param amount the taxable SS income
         * @return this builder
         */
        public Builder taxableSSIncome(BigDecimal amount) {
            this.taxableSSIncome = amount;
            return this;
        }

        /**
         * Sets the taxable withdrawals.
         *
         * @param amount the taxable withdrawals
         * @return this builder
         */
        public Builder taxableWithdrawals(BigDecimal amount) {
            this.taxableWithdrawals = amount;
            return this;
        }

        /**
         * Sets the tax-free withdrawals.
         *
         * @param amount the tax-free withdrawals
         * @return this builder
         */
        public Builder taxFreeWithdrawals(BigDecimal amount) {
            this.taxFreeWithdrawals = amount;
            return this;
        }

        /**
         * Sets the federal tax liability.
         *
         * @param amount the federal tax
         * @return this builder
         */
        public Builder federalTaxLiability(BigDecimal amount) {
            this.federalTaxLiability = amount;
            return this;
        }

        /**
         * Sets the effective tax rate.
         *
         * @param rate the effective rate as decimal
         * @return this builder
         */
        public Builder effectiveTaxRate(BigDecimal rate) {
            this.effectiveTaxRate = rate;
            return this;
        }

        /**
         * Sets the marginal tax bracket.
         *
         * @param bracket the marginal bracket as decimal
         * @return this builder
         */
        public Builder marginalTaxBracket(BigDecimal bracket) {
            this.marginalTaxBracket = bracket;
            return this;
        }

        /**
         * Sets the Roth conversion amount.
         *
         * @param amount the conversion amount
         * @return this builder
         */
        public Builder rothConversionAmount(BigDecimal amount) {
            this.rothConversionAmount = amount;
            return this;
        }

        /**
         * Sets the Roth conversion tax.
         *
         * @param amount the tax on conversion
         * @return this builder
         */
        public Builder rothConversionTax(BigDecimal amount) {
            this.rothConversionTax = amount;
            return this;
        }

        /**
         * Builds the TaxSummary.
         *
         * @return the constructed TaxSummary
         */
        public TaxSummary build() {
            return new TaxSummary(
                    taxableIncome,
                    taxableSSIncome,
                    taxableWithdrawals,
                    taxFreeWithdrawals,
                    federalTaxLiability,
                    effectiveTaxRate,
                    marginalTaxBracket,
                    rothConversionAmount,
                    rothConversionTax
            );
        }
    }
}
