package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;

import io.github.xmljim.retirement.domain.value.IncomeDetails;

/**
 * Calculator for Modified Adjusted Gross Income (MAGI).
 *
 * <p>MAGI is used to determine eligibility for various tax benefits including:
 * <ul>
 *   <li>Roth IRA contribution eligibility</li>
 *   <li>Traditional IRA deductibility (when covered by employer plan)</li>
 *   <li>Premium tax credits (ACA)</li>
 *   <li>Medicare premium surcharges (IRMAA)</li>
 * </ul>
 *
 * <p>Note: Different tax provisions may define MAGI slightly differently.
 * This calculator focuses on the IRA-specific definition from IRS Publication 590-A.
 *
 * @see IncomeDetails
 */
public interface MAGICalculator {

    /**
     * Calculates MAGI from income details.
     *
     * <p>MAGI for IRA purposes = AGI + certain deductions added back:
     * <ul>
     *   <li>Student loan interest deduction</li>
     *   <li>Tuition and fees deduction</li>
     *   <li>Foreign earned income exclusion</li>
     *   <li>Foreign housing exclusion or deduction</li>
     *   <li>Excluded Series EE/I savings bond interest</li>
     *   <li>Excluded employer-provided adoption benefits</li>
     * </ul>
     *
     * @param incomeDetails the income components
     * @return the calculated MAGI
     */
    BigDecimal calculate(IncomeDetails incomeDetails);

    /**
     * Calculates MAGI from just AGI (no add-backs).
     *
     * <p>Use this when the taxpayer has no MAGI add-back items,
     * in which case MAGI equals AGI.
     *
     * @param adjustedGrossIncome the AGI
     * @return the MAGI (equal to AGI when no add-backs)
     */
    BigDecimal calculate(BigDecimal adjustedGrossIncome);
}
