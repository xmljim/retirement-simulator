package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;
import java.util.List;

import io.github.xmljim.retirement.domain.enums.FilingStatus;
import io.github.xmljim.retirement.domain.value.TaxBracket;
import io.github.xmljim.retirement.domain.value.TaxCalculationResult;

/**
 * Calculator for federal income tax.
 *
 * <p>Calculates federal tax liability using progressive tax brackets
 * and standard deductions. Supports future year projections using
 * chained CPI indexing.
 *
 * <p>Key features:
 * <ul>
 *   <li>Progressive tax bracket calculation</li>
 *   <li>Standard deduction with age 65+ additional</li>
 *   <li>Chained CPI indexing for future years</li>
 *   <li>Effective and marginal rate calculations</li>
 * </ul>
 *
 * @see TaxCalculationResult
 * @see TaxBracket
 * @see <a href="https://www.irs.gov/publications/p17">IRS Publication 17</a>
 */
public interface FederalTaxCalculator {

    /**
     * Calculates federal income tax liability.
     *
     * @param grossIncome total income before deductions
     * @param filingStatus the filing status
     * @param age the taxpayer's age (for 65+ deduction)
     * @param year the tax year
     * @return the tax calculation result
     */
    TaxCalculationResult calculateTax(
        BigDecimal grossIncome,
        FilingStatus filingStatus,
        int age,
        int year
    );

    /**
     * Calculates federal income tax for married filing jointly.
     *
     * @param grossIncome total income before deductions
     * @param age the primary taxpayer's age
     * @param spouseAge the spouse's age (for additional 65+ deduction)
     * @param year the tax year
     * @return the tax calculation result
     */
    TaxCalculationResult calculateTaxMfj(
        BigDecimal grossIncome,
        int age,
        int spouseAge,
        int year
    );

    /**
     * Gets the standard deduction for a filing status and year.
     *
     * <p>Includes additional deduction for age 65+ if applicable.
     *
     * @param filingStatus the filing status
     * @param age the taxpayer's age
     * @param year the tax year
     * @return the total standard deduction
     */
    BigDecimal getStandardDeduction(
        FilingStatus filingStatus,
        int age,
        int year
    );

    /**
     * Gets the standard deduction for MFJ with both spouse ages.
     *
     * @param age the primary taxpayer's age
     * @param spouseAge the spouse's age
     * @param year the tax year
     * @return the total standard deduction including both 65+ additions
     */
    BigDecimal getStandardDeductionMfj(int age, int spouseAge, int year);

    /**
     * Gets the tax brackets for a filing status and year.
     *
     * <p>Brackets are indexed to the specified year using chained CPI.
     *
     * @param filingStatus the filing status
     * @param year the tax year
     * @return the list of tax brackets
     */
    List<TaxBracket> getBrackets(FilingStatus filingStatus, int year);

    /**
     * Calculates the effective tax rate.
     *
     * <p>Effective rate = Total tax / Gross income
     *
     * @param grossIncome total income before deductions
     * @param filingStatus the filing status
     * @param age the taxpayer's age
     * @param year the tax year
     * @return the effective tax rate as a decimal
     */
    BigDecimal getEffectiveTaxRate(
        BigDecimal grossIncome,
        FilingStatus filingStatus,
        int age,
        int year
    );

    /**
     * Calculates the marginal tax rate.
     *
     * <p>Marginal rate is the tax rate on the next dollar of income.
     *
     * @param taxableIncome income after deductions
     * @param filingStatus the filing status
     * @param year the tax year
     * @return the marginal tax rate as a decimal
     */
    BigDecimal getMarginalTaxRate(
        BigDecimal taxableIncome,
        FilingStatus filingStatus,
        int year
    );

    /**
     * Projects a base-year value to a future year using chained CPI.
     *
     * <p>IRS rounds bracket thresholds and deductions to nearest $50.
     *
     * @param baseValue the value in the base year
     * @param baseYear the year of the base value
     * @param targetYear the year to project to
     * @return the projected value rounded per IRS rules
     */
    BigDecimal projectValueWithChainedCpi(
        BigDecimal baseValue,
        int baseYear,
        int targetYear
    );
}
