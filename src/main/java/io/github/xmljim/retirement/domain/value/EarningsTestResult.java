package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Result of the Social Security earnings test calculation.
 *
 * <p>The earnings test applies to Social Security beneficiaries who work
 * while receiving benefits before their Full Retirement Age (FRA).
 * Benefits are reduced based on earnings over the annual exempt amount.
 *
 * <p>Key SSA rules:
 * <ul>
 *   <li>Below FRA all year: $1 reduction per $2 earned over limit</li>
 *   <li>Year reaching FRA: $1 per $3 earned over higher limit (months before FRA only)</li>
 *   <li>At/after FRA: No earnings test applies</li>
 * </ul>
 *
 * <p><b>Important:</b> Withheld benefits are NOT lost. They're added back
 * at FRA through benefit recalculation.
 *
 * @param originalBenefit the annual SS benefit before any reduction
 * @param reducedBenefit the annual SS benefit after earnings test reduction
 * @param reductionAmount the total annual reduction applied
 * @param excessEarnings earnings over the applicable exempt limit
 * @param monthsWithheld estimated number of months' benefits withheld
 * @param subjectToTest whether the beneficiary is subject to the earnings test
 * @param exemptReason if not subject to test, the reason for exemption
 *
 * @see <a href="https://www.ssa.gov/benefits/retirement/planner/whileworking.html">SSA Earnings Test</a>
 */
public record EarningsTestResult(
    BigDecimal originalBenefit,
    BigDecimal reducedBenefit,
    BigDecimal reductionAmount,
    BigDecimal excessEarnings,
    int monthsWithheld,
    boolean subjectToTest,
    Optional<String> exemptReason
) {

    /**
     * Creates a result for someone exempt from the earnings test.
     *
     * @param annualBenefit the full annual SS benefit
     * @param reason why they're exempt (e.g., "At or past FRA")
     * @return an exempt result with no reduction
     */
    public static EarningsTestResult exempt(BigDecimal annualBenefit, String reason) {
        return new EarningsTestResult(
            annualBenefit,
            annualBenefit,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            0,
            false,
            Optional.of(reason)
        );
    }

    /**
     * Creates a result for someone subject to the test but under the limit.
     *
     * @param annualBenefit the full annual SS benefit
     * @return a result with no reduction (earnings under limit)
     */
    public static EarningsTestResult noReduction(BigDecimal annualBenefit) {
        return new EarningsTestResult(
            annualBenefit,
            annualBenefit,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            0,
            true,
            Optional.empty()
        );
    }

    /**
     * Creates a result with benefit reduction applied.
     *
     * @param originalBenefit the full annual SS benefit
     * @param reducedBenefit the benefit after reduction
     * @param reductionAmount the amount reduced
     * @param excessEarnings earnings over the exempt limit
     * @param monthsWithheld estimated months' benefits withheld
     * @return a result with reduction details
     */
    public static EarningsTestResult reduced(
            BigDecimal originalBenefit,
            BigDecimal reducedBenefit,
            BigDecimal reductionAmount,
            BigDecimal excessEarnings,
            int monthsWithheld) {
        return new EarningsTestResult(
            originalBenefit,
            reducedBenefit,
            reductionAmount,
            excessEarnings,
            monthsWithheld,
            true,
            Optional.empty()
        );
    }

    /**
     * Returns the effective monthly benefit after any reduction.
     *
     * @return the monthly benefit (annual / 12)
     */
    public BigDecimal getMonthlyBenefit() {
        return reducedBenefit.divide(BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Returns whether benefits were reduced due to excess earnings.
     *
     * @return true if reduction was applied
     */
    public boolean isReduced() {
        return reductionAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Returns the percentage of benefits retained after reduction.
     *
     * @return percentage retained (e.g., 0.75 for 75%)
     */
    public BigDecimal getRetentionPercentage() {
        if (originalBenefit.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ONE;
        }
        return reducedBenefit.divide(originalBenefit, 4, java.math.RoundingMode.HALF_UP);
    }
}
