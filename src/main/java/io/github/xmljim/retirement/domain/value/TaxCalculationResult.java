package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.xmljim.retirement.domain.enums.FilingStatus;

/**
 * Result of a federal income tax calculation.
 *
 * <p>Contains the complete breakdown of a tax calculation including:
 * <ul>
 *   <li>Gross income and deductions</li>
 *   <li>Taxable income and total tax</li>
 *   <li>Effective and marginal tax rates</li>
 *   <li>Per-bracket tax breakdown</li>
 * </ul>
 *
 * @param grossIncome total income before deductions
 * @param standardDeduction the standard deduction applied
 * @param taxableIncome income subject to tax (gross - deduction)
 * @param federalTax total federal income tax
 * @param effectiveRate effective tax rate (tax / gross income)
 * @param marginalRate marginal tax rate (top bracket rate)
 * @param filingStatus the filing status used
 * @param year the tax year
 * @param bracketBreakdown tax calculated in each bracket
 *
 * @see <a href="https://www.irs.gov/publications/p17">IRS Publication 17</a>
 */
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP",
    justification = "Record is immutable; List is created internally and not modified"
)
public record TaxCalculationResult(
    BigDecimal grossIncome,
    BigDecimal standardDeduction,
    BigDecimal taxableIncome,
    BigDecimal federalTax,
    BigDecimal effectiveRate,
    BigDecimal marginalRate,
    FilingStatus filingStatus,
    int year,
    List<BracketTax> bracketBreakdown
) {

    /**
     * Tax calculated within a single bracket.
     *
     * @param rate the bracket tax rate
     * @param incomeInBracket the income amount taxed at this rate
     * @param taxInBracket the tax generated in this bracket
     */
    public record BracketTax(
        BigDecimal rate,
        BigDecimal incomeInBracket,
        BigDecimal taxInBracket
    ) {
        /**
         * Creates a bracket tax entry.
         *
         * @param rate the tax rate
         * @param incomeInBracket the income in this bracket
         * @return a new BracketTax with calculated tax
         */
        public static BracketTax of(BigDecimal rate, BigDecimal incomeInBracket) {
            BigDecimal tax = incomeInBracket.multiply(rate)
                .setScale(2, RoundingMode.HALF_UP);
            return new BracketTax(rate, incomeInBracket, tax);
        }
    }

    /**
     * Returns the after-tax income.
     *
     * @return gross income minus federal tax
     */
    public BigDecimal getAfterTaxIncome() {
        return grossIncome.subtract(federalTax);
    }

    /**
     * Returns whether any tax is owed.
     *
     * @return true if federal tax is greater than zero
     */
    public boolean hasTaxLiability() {
        return federalTax.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Returns the effective rate as a percentage.
     *
     * @return the effective rate multiplied by 100
     */
    public BigDecimal getEffectiveRatePercentage() {
        return effectiveRate.multiply(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Returns the marginal rate as a percentage.
     *
     * @return the marginal rate multiplied by 100
     */
    public BigDecimal getMarginalRatePercentage() {
        return marginalRate.multiply(BigDecimal.valueOf(100))
            .setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Creates a result for zero tax liability.
     *
     * @param grossIncome the gross income
     * @param standardDeduction the deduction that eliminated tax liability
     * @param filingStatus the filing status
     * @param year the tax year
     * @return a result with zero tax
     */
    public static TaxCalculationResult noTax(
            BigDecimal grossIncome,
            BigDecimal standardDeduction,
            FilingStatus filingStatus,
            int year) {
        return new TaxCalculationResult(
            grossIncome,
            standardDeduction,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            filingStatus,
            year,
            List.of()
        );
    }
}
