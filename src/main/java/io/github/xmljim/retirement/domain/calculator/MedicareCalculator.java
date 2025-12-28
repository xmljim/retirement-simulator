package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;

import io.github.xmljim.retirement.domain.enums.FilingStatus;
import io.github.xmljim.retirement.domain.value.MedicarePremiums;
import io.github.xmljim.retirement.domain.value.MedicarePremiums.IrmaaBracket;

/**
 * Calculator for Medicare premiums with IRMAA adjustments.
 *
 * <p>Calculates Medicare Part A, Part B, and Part D premiums based on
 * income (MAGI) and filing status. Higher-income beneficiaries pay
 * additional premiums through IRMAA (Income-Related Monthly Adjustment Amount).
 *
 * <p>Key features:
 * <ul>
 *   <li>Part B premium calculation with IRMAA brackets</li>
 *   <li>Part D IRMAA surcharge calculation</li>
 *   <li>Support for different filing statuses</li>
 *   <li>Year-specific premium tables</li>
 * </ul>
 *
 * <p><b>Important:</b> IRMAA is based on MAGI from 2 years prior.
 * For example, 2025 premiums are based on 2023 MAGI.
 *
 * @see MedicarePremiums
 * @see <a href="https://www.medicare.gov/basics/costs/medicare-costs">Medicare Costs</a>
 */
public interface MedicareCalculator {

    /**
     * Calculates all Medicare premiums based on income.
     *
     * <p>Determines the appropriate IRMAA bracket based on MAGI and
     * filing status, then calculates Part A, Part B, and Part D costs.
     *
     * @param magi Modified Adjusted Gross Income (from 2 years prior)
     * @param filingStatus the tax filing status
     * @param year the Medicare year (e.g., 2025)
     * @param premiumFreePartA true if the beneficiary has premium-free Part A
     * @return the calculated Medicare premiums
     */
    MedicarePremiums calculatePremiums(
        BigDecimal magi,
        FilingStatus filingStatus,
        int year,
        boolean premiumFreePartA
    );

    /**
     * Calculates Medicare premiums assuming premium-free Part A.
     *
     * <p>Most beneficiaries have premium-free Part A if they (or their spouse)
     * paid Medicare taxes for at least 40 quarters (10 years).
     *
     * @param magi Modified Adjusted Gross Income (from 2 years prior)
     * @param filingStatus the tax filing status
     * @param year the Medicare year
     * @return the calculated Medicare premiums with Part A premium of $0
     */
    default MedicarePremiums calculatePremiums(
            BigDecimal magi,
            FilingStatus filingStatus,
            int year) {
        return calculatePremiums(magi, filingStatus, year, true);
    }

    /**
     * Returns the Part B base premium for a given year.
     *
     * @param year the Medicare year
     * @return the Part B base monthly premium (before IRMAA)
     */
    BigDecimal getPartBBasePremium(int year);

    /**
     * Returns the total Part B premium including IRMAA.
     *
     * @param magi Modified Adjusted Gross Income (from 2 years prior)
     * @param filingStatus the tax filing status
     * @param year the Medicare year
     * @return the total Part B monthly premium
     */
    BigDecimal getPartBPremium(
        BigDecimal magi,
        FilingStatus filingStatus,
        int year
    );

    /**
     * Returns the Part D IRMAA surcharge.
     *
     * <p>Note: This is the IRMAA surcharge only, not the full Part D
     * plan premium which varies by plan.
     *
     * @param magi Modified Adjusted Gross Income (from 2 years prior)
     * @param filingStatus the tax filing status
     * @param year the Medicare year
     * @return the Part D IRMAA monthly surcharge
     */
    BigDecimal getPartDIrmaa(
        BigDecimal magi,
        FilingStatus filingStatus,
        int year
    );

    /**
     * Determines the IRMAA bracket for a given MAGI and filing status.
     *
     * @param magi Modified Adjusted Gross Income (from 2 years prior)
     * @param filingStatus the tax filing status
     * @param year the Medicare year
     * @return the IRMAA bracket (0-5)
     */
    IrmaaBracket getIrmaaBracket(
        BigDecimal magi,
        FilingStatus filingStatus,
        int year
    );

    /**
     * Returns the Part A premium for non-premium-free beneficiaries.
     *
     * <p>Part A premium depends on work history:
     * <ul>
     *   <li>40+ quarters: $0 (premium-free)</li>
     *   <li>30-39 quarters: reduced premium (~$285/month in 2025)</li>
     *   <li>Under 30 quarters: full premium (~$518/month in 2025)</li>
     * </ul>
     *
     * @param year the Medicare year
     * @param workQuarters the number of Medicare-covered work quarters
     * @return the monthly Part A premium
     */
    BigDecimal getPartAPremium(int year, int workQuarters);
}
