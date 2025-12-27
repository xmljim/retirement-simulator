package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;

import io.github.xmljim.retirement.domain.enums.FilingStatus;
import io.github.xmljim.retirement.domain.value.TaxationResult;

/**
 * Calculator for Social Security benefit taxation.
 *
 * <p>Determines what percentage of Social Security benefits are taxable
 * based on IRS Publication 915 rules.
 *
 * <p>Combined income formula: AGI + non-taxable interest + 50% of SS benefits
 *
 * <p>Thresholds by filing status (NOT indexed - fixed since 1984/1993):
 * <ul>
 *   <li>Single/HOH: Combined income below $25,000 = 0% taxable;
 *       $25,000-$34,000 = up to 50%; over $34,000 = up to 85%</li>
 *   <li>MFJ/QSS: Below $32,000 = 0%; $32,000-$44,000 = up to 50%;
 *       over $44,000 = up to 85%</li>
 *   <li>MFS (living with spouse): Always up to 85% taxable (threshold = $0)</li>
 *   <li>MFS (not living with spouse): Uses Single thresholds</li>
 * </ul>
 *
 * <p><b>Note:</b> The non-indexed thresholds create "bracket creep" where
 * more benefits become taxable over time as incomes rise with inflation.
 *
 * @see <a href="https://www.irs.gov/publications/p915">IRS Publication 915</a>
 */
public interface BenefitTaxationCalculator {

    /**
     * Calculates the taxable amount of Social Security benefits.
     *
     * <p>This method assumes MFS filers are living with their spouse
     * (worst case for tax purposes - up to 85% taxable). For MFS filers
     * not living with their spouse, use
     * {@link #calculate(BigDecimal, BigDecimal, BigDecimal, FilingStatus, boolean)}.
     *
     * @param ssAnnualBenefit the annual Social Security benefit
     * @param adjustedGrossIncome AGI from tax return (line 11 of Form 1040)
     * @param nonTaxableInterest tax-exempt interest (e.g., municipal bonds)
     * @param filingStatus the tax filing status
     * @return the taxation calculation result
     */
    TaxationResult calculate(
        BigDecimal ssAnnualBenefit,
        BigDecimal adjustedGrossIncome,
        BigDecimal nonTaxableInterest,
        FilingStatus filingStatus
    );

    /**
     * Calculates the taxable amount of Social Security benefits with MFS detail.
     *
     * <p>For Married Filing Separately, taxation depends on whether the
     * filer lived with their spouse at any time during the year:
     * <ul>
     *   <li>Lived with spouse: Always up to 85% taxable</li>
     *   <li>Did NOT live with spouse: Uses Single filer thresholds</li>
     * </ul>
     *
     * @param ssAnnualBenefit the annual Social Security benefit
     * @param adjustedGrossIncome AGI from tax return
     * @param nonTaxableInterest tax-exempt interest
     * @param filingStatus the tax filing status
     * @param mfsLivingWithSpouse for MFS only: true if lived with spouse during year
     * @return the taxation calculation result
     */
    TaxationResult calculate(
        BigDecimal ssAnnualBenefit,
        BigDecimal adjustedGrossIncome,
        BigDecimal nonTaxableInterest,
        FilingStatus filingStatus,
        boolean mfsLivingWithSpouse
    );

    /**
     * Calculates combined income for benefit taxation.
     *
     * <p>Combined Income = AGI + non-taxable interest + 50% of SS benefits
     *
     * @param ssAnnualBenefit the annual Social Security benefit
     * @param adjustedGrossIncome AGI from tax return
     * @param nonTaxableInterest tax-exempt interest
     * @return the combined income
     */
    BigDecimal calculateCombinedIncome(
        BigDecimal ssAnnualBenefit,
        BigDecimal adjustedGrossIncome,
        BigDecimal nonTaxableInterest
    );

    /**
     * Returns the lower threshold for a given filing status.
     *
     * <p>Combined income below this amount = 0% of benefits taxable.
     *
     * @param filingStatus the filing status
     * @return the lower threshold
     */
    BigDecimal getLowerThreshold(FilingStatus filingStatus);

    /**
     * Returns the upper threshold for a given filing status.
     *
     * <p>Combined income above this amount = up to 85% of benefits taxable.
     *
     * @param filingStatus the filing status
     * @return the upper threshold
     */
    BigDecimal getUpperThreshold(FilingStatus filingStatus);
}
