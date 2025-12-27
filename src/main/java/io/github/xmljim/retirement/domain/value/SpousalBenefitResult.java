package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Result of a spousal benefit calculation.
 *
 * <p>Contains the computed benefit amounts and eligibility information
 * for claiming spousal benefits. Use {@link #recommendedBenefit()} to
 * get the optimal benefit amount.
 *
 * <p>Example usage:
 * <pre>{@code
 * SpousalBenefitResult result = calculator.calculateSpousalBenefit(...);
 *
 * if (result.eligibleForSpousal()) {
 *     BigDecimal benefit = result.recommendedBenefit();
 *     BenefitType type = result.recommendedType();
 * } else {
 *     String reason = result.ineligibilityReason().orElse("Unknown");
 * }
 * }</pre>
 *
 * @param ownAdjustedBenefit the claimant's own benefit (with early/delayed adjustment)
 * @param spousalBenefit the calculated spousal benefit (50% of spouse's FRA, adjusted)
 * @param recommendedBenefit the higher of own or spousal benefit
 * @param recommendedType which benefit type is recommended
 * @param eligibleForSpousal whether the claimant meets spousal eligibility requirements
 * @param ineligibilityReason explanation if not eligible (empty if eligible)
 *
 * @see <a href="https://www.ssa.gov/benefits/retirement/planner/applying7.html">SSA Spousal</a>
 */
public record SpousalBenefitResult(
    BigDecimal ownAdjustedBenefit,
    BigDecimal spousalBenefit,
    BigDecimal recommendedBenefit,
    BenefitType recommendedType,
    boolean eligibleForSpousal,
    Optional<String> ineligibilityReason
) {

    /**
     * Type of benefit recommended.
     */
    public enum BenefitType {
        /** Claimant's own retirement benefit is higher. */
        OWN,
        /** Spousal benefit (50% of spouse's FRA) is higher. */
        SPOUSAL
    }

    /**
     * Compact constructor with validation.
     */
    public SpousalBenefitResult {
        ownAdjustedBenefit = ownAdjustedBenefit != null ? ownAdjustedBenefit : BigDecimal.ZERO;
        spousalBenefit = spousalBenefit != null ? spousalBenefit : BigDecimal.ZERO;
        recommendedBenefit = recommendedBenefit != null ? recommendedBenefit : BigDecimal.ZERO;
        recommendedType = recommendedType != null ? recommendedType : BenefitType.OWN;
        ineligibilityReason = ineligibilityReason != null ? ineligibilityReason : Optional.empty();
    }

    // ==================== Factory Methods ====================

    /**
     * Creates a result for an eligible spousal benefit claim.
     *
     * @param ownBenefit the claimant's own adjusted benefit
     * @param spousalBenefit the calculated spousal benefit
     * @return SpousalBenefitResult with recommended benefit selected
     */
    public static SpousalBenefitResult eligible(BigDecimal ownBenefit, BigDecimal spousalBenefit) {
        boolean spousalIsHigher = spousalBenefit.compareTo(ownBenefit) > 0;
        return new SpousalBenefitResult(
            ownBenefit,
            spousalBenefit,
            spousalIsHigher ? spousalBenefit : ownBenefit,
            spousalIsHigher ? BenefitType.SPOUSAL : BenefitType.OWN,
            true,
            Optional.empty()
        );
    }

    /**
     * Creates a result for an ineligible spousal benefit claim.
     *
     * @param ownBenefit the claimant's own adjusted benefit
     * @param reason the reason for ineligibility
     * @return SpousalBenefitResult marked as ineligible
     */
    public static SpousalBenefitResult ineligible(BigDecimal ownBenefit, String reason) {
        return new SpousalBenefitResult(
            ownBenefit,
            BigDecimal.ZERO,
            ownBenefit,
            BenefitType.OWN,
            false,
            Optional.of(reason)
        );
    }

    // ==================== Convenience Methods ====================

    /**
     * Returns true if spousal benefit exceeds own benefit.
     *
     * @return true if spousal benefit is recommended
     */
    public boolean isSpousalBenefitHigher() {
        return recommendedType == BenefitType.SPOUSAL;
    }

    /**
     * Returns the benefit increase from claiming spousal vs own.
     *
     * @return the difference (spousal - own), may be negative if own is higher
     */
    public BigDecimal getSpousalBenefitIncrease() {
        return spousalBenefit.subtract(ownAdjustedBenefit);
    }
}
