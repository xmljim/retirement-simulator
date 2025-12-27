package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.xmljim.retirement.domain.calculator.EarningsTestCalculator;
import io.github.xmljim.retirement.domain.config.SocialSecurityRules;
import io.github.xmljim.retirement.domain.value.EarningsTestResult;

/**
 * Default implementation of the Social Security earnings test calculator.
 *
 * <p>The earnings test reduces Social Security benefits for beneficiaries who
 * work before reaching Full Retirement Age (FRA).
 *
 * <p>SSA rules:
 * <ul>
 *   <li>Below FRA all year: $1 reduction per $2 earned over annual limit</li>
 *   <li>Year reaching FRA: $1 per $3 earned over higher limit (months before FRA only)</li>
 *   <li>At or after FRA: No earnings test applies</li>
 * </ul>
 *
 * <p><b>Important:</b> Withheld benefits are NOT permanently lost. They're
 * recalculated and added back at FRA.
 *
 * @see <a href="https://www.ssa.gov/benefits/retirement/planner/whileworking.html">SSA Earnings Test</a>
 * @see <a href="https://www.ssa.gov/oact/cola/rtea.html">SSA Earnings Test Limits</a>
 */
@Service
public class DefaultEarningsTestCalculator implements EarningsTestCalculator {

    private static final int MONTHS_PER_YEAR = 12;
    private static final int SCALE = 10;

    private final SocialSecurityRules rules;

    @Autowired
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-managed beans")
    public DefaultEarningsTestCalculator(SocialSecurityRules rules) {
        this.rules = rules;
    }

    /**
     * Non-Spring constructor with defaults.
     */
    public DefaultEarningsTestCalculator() {
        this.rules = new SocialSecurityRules();
    }

    @Override
    public EarningsTestResult calculate(
            BigDecimal annualEarnings,
            BigDecimal ssAnnualBenefit,
            int ageMonths,
            int fraMonths,
            int year) {

        // Not subject to test at or past FRA
        if (!isSubjectToTest(ageMonths, fraMonths)) {
            return EarningsTestResult.exempt(ssAnnualBenefit, "At or past Full Retirement Age");
        }

        BigDecimal limit = getBelowFraLimit(year);
        BigDecimal excessEarnings = annualEarnings.subtract(limit);

        // Under the limit - no reduction
        if (excessEarnings.compareTo(BigDecimal.ZERO) <= 0) {
            return EarningsTestResult.noReduction(ssAnnualBenefit);
        }

        int reductionRatio = rules.getEarningsTest().getBelowFraReductionRatio();
        return calculateReduction(ssAnnualBenefit, excessEarnings, reductionRatio, MONTHS_PER_YEAR);
    }

    @Override
    public EarningsTestResult calculateFraYear(
            BigDecimal annualEarnings,
            BigDecimal ssAnnualBenefit,
            int monthsBeforeFra,
            int year) {

        // If no months before FRA (already at FRA), not subject to test
        if (monthsBeforeFra <= 0) {
            return EarningsTestResult.exempt(ssAnnualBenefit, "Already at Full Retirement Age");
        }

        // Use the higher FRA-year limit
        BigDecimal limit = getFraYearLimit(year);

        // Prorate earnings to months before FRA
        BigDecimal proratedEarnings = annualEarnings
            .multiply(BigDecimal.valueOf(monthsBeforeFra))
            .divide(BigDecimal.valueOf(MONTHS_PER_YEAR), SCALE, RoundingMode.HALF_UP);

        // Also prorate the limit (monthly limit * months before FRA)
        BigDecimal monthlyLimit = limit.divide(BigDecimal.valueOf(MONTHS_PER_YEAR), SCALE, RoundingMode.HALF_UP);
        BigDecimal proratedLimit = monthlyLimit.multiply(BigDecimal.valueOf(monthsBeforeFra));

        BigDecimal excessEarnings = proratedEarnings.subtract(proratedLimit);

        // Under the limit - no reduction
        if (excessEarnings.compareTo(BigDecimal.ZERO) <= 0) {
            return EarningsTestResult.noReduction(ssAnnualBenefit);
        }

        int reductionRatio = rules.getEarningsTest().getFraYearReductionRatio();
        return calculateReduction(ssAnnualBenefit, excessEarnings, reductionRatio, monthsBeforeFra);
    }

    /**
     * Calculates the benefit reduction and creates the result.
     *
     * @param ssAnnualBenefit the annual SS benefit
     * @param excessEarnings earnings over the applicable limit
     * @param reductionRatio the reduction ratio (2 for below FRA, 3 for FRA year)
     * @param maxMonthsWithheld maximum months that can be withheld
     * @return the earnings test result with reduction details
     */
    private EarningsTestResult calculateReduction(
            BigDecimal ssAnnualBenefit,
            BigDecimal excessEarnings,
            int reductionRatio,
            int maxMonthsWithheld) {

        BigDecimal reductionAmount = excessEarnings
            .divide(BigDecimal.valueOf(reductionRatio), SCALE, RoundingMode.HALF_UP);

        // Reduction cannot exceed total benefit
        if (reductionAmount.compareTo(ssAnnualBenefit) > 0) {
            reductionAmount = ssAnnualBenefit;
        }

        BigDecimal reducedBenefit = ssAnnualBenefit.subtract(reductionAmount);

        // Estimate months withheld
        int monthsWithheld = calculateMonthsWithheld(ssAnnualBenefit, reductionAmount, maxMonthsWithheld);

        return EarningsTestResult.reduced(
            ssAnnualBenefit,
            reducedBenefit.setScale(2, RoundingMode.HALF_UP),
            reductionAmount.setScale(2, RoundingMode.HALF_UP),
            excessEarnings.setScale(2, RoundingMode.HALF_UP),
            monthsWithheld);
    }

    /**
     * Calculates the estimated number of months' benefits withheld.
     *
     * @param ssAnnualBenefit the annual SS benefit
     * @param reductionAmount the total reduction amount
     * @param maxMonths maximum months that can be withheld
     * @return estimated months withheld
     */
    private int calculateMonthsWithheld(BigDecimal ssAnnualBenefit, BigDecimal reductionAmount, int maxMonths) {
        BigDecimal monthlyBenefit = ssAnnualBenefit
            .divide(BigDecimal.valueOf(MONTHS_PER_YEAR), SCALE, RoundingMode.HALF_UP);

        if (monthlyBenefit.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        int monthsWithheld = reductionAmount
            .divide(monthlyBenefit, 0, RoundingMode.CEILING)
            .intValue();

        return Math.min(monthsWithheld, maxMonths);
    }

    @Override
    public BigDecimal getBelowFraLimit(int year) {
        return rules.getEarningsTest().getBelowFraLimit(year);
    }

    @Override
    public BigDecimal getFraYearLimit(int year) {
        return rules.getEarningsTest().getFraYearLimit(year);
    }
}
