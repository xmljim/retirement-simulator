package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import io.github.xmljim.retirement.domain.calculator.MAGICalculator;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.value.IncomeDetails;

/**
 * Default implementation of MAGI calculator for IRA purposes.
 *
 * <p>Calculates Modified Adjusted Gross Income by adding back certain
 * deductions to Adjusted Gross Income per IRS Publication 590-A.
 *
 * <p>Add-back items for IRA MAGI:
 * <ul>
 *   <li>Student loan interest deduction</li>
 *   <li>Tuition and fees deduction (if applicable)</li>
 *   <li>Foreign earned income exclusion</li>
 *   <li>Foreign housing exclusion or deduction</li>
 *   <li>Excluded savings bond interest (Form 8815)</li>
 *   <li>Excluded employer-provided adoption benefits (Form 8839)</li>
 * </ul>
 */
@Service
public class DefaultMAGICalculator implements MAGICalculator {

    @Override
    public BigDecimal calculate(IncomeDetails incomeDetails) {
        MissingRequiredFieldException.requireNonNull(incomeDetails, "incomeDetails");

        return incomeDetails.adjustedGrossIncome()
            .add(incomeDetails.getTotalAddBacks());
    }

    @Override
    public BigDecimal calculate(BigDecimal adjustedGrossIncome) {
        if (adjustedGrossIncome == null) {
            return BigDecimal.ZERO;
        }
        return adjustedGrossIncome;
    }
}
