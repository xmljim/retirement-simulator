package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Result of a survivor benefit calculation.
 *
 * <p>Contains the computed benefit amounts and eligibility information
 * for claiming survivor benefits after a spouse's death. Use
 * {@link #recommendedBenefit()} to get the optimal benefit amount.
 *
 * <p>Survivor benefits can be claimed as early as age 60 (50 if disabled),
 * but are reduced. Full benefits are available at the survivor's FRA.
 *
 * <p>Example usage:
 * <pre>{@code
 * SurvivorBenefitResult result = calculator.calculateSurvivorBenefit(...);
 *
 * if (result.eligibleForSurvivor()) {
 *     BigDecimal benefit = result.recommendedBenefit();
 *     if (result.reductionApplied()) {
 *         BigDecimal reduction = result.reductionPercentage();
 *     }
 * }
 * }</pre>
 *
 * @param ownAdjustedBenefit the survivor's own benefit (with early/delayed adjustment)
 * @param survivorBenefit the calculated survivor benefit (deceased's benefit, possibly reduced)
 * @param recommendedBenefit the higher of own or survivor benefit
 * @param recommendedType which benefit type is recommended
 * @param eligibleForSurvivor whether the claimant meets survivor eligibility requirements
 * @param ineligibilityReason explanation if not eligible (empty if eligible)
 * @param reductionPercentage the reduction applied for early claiming (zero if at FRA)
 *
 * @see <a href="https://www.ssa.gov/benefits/survivors/">SSA Survivor Benefits</a>
 */
public record SurvivorBenefitResult(
    BigDecimal ownAdjustedBenefit,
    BigDecimal survivorBenefit,
    BigDecimal recommendedBenefit,
    BenefitType recommendedType,
    boolean eligibleForSurvivor,
    Optional<String> ineligibilityReason,
    BigDecimal reductionPercentage
) {

    /**
     * Type of benefit recommended.
     */
    public enum BenefitType {
        /** Survivor's own retirement benefit is higher. */
        OWN,
        /** Survivor benefit (deceased's benefit) is higher. */
        SURVIVOR
    }

    /**
     * Compact constructor with validation.
     */
    public SurvivorBenefitResult {
        ownAdjustedBenefit = ownAdjustedBenefit != null ? ownAdjustedBenefit : BigDecimal.ZERO;
        survivorBenefit = survivorBenefit != null ? survivorBenefit : BigDecimal.ZERO;
        recommendedBenefit = recommendedBenefit != null ? recommendedBenefit : BigDecimal.ZERO;
        recommendedType = recommendedType != null ? recommendedType : BenefitType.OWN;
        ineligibilityReason = ineligibilityReason != null ? ineligibilityReason : Optional.empty();
        reductionPercentage = reductionPercentage != null ? reductionPercentage : BigDecimal.ZERO;
    }

    // ==================== Factory Methods ====================

    /**
     * Creates a result for an eligible survivor benefit claim at FRA (no reduction).
     *
     * @param ownBenefit the survivor's own adjusted benefit
     * @param survivorBenefit the full survivor benefit (deceased's benefit)
     * @return SurvivorBenefitResult with recommended benefit selected
     */
    public static SurvivorBenefitResult eligibleAtFra(
            BigDecimal ownBenefit,
            BigDecimal survivorBenefit) {
        return eligible(ownBenefit, survivorBenefit, BigDecimal.ZERO);
    }

    /**
     * Creates a result for an eligible survivor benefit claim with early reduction.
     *
     * @param ownBenefit the survivor's own adjusted benefit
     * @param survivorBenefit the reduced survivor benefit
     * @param reductionPct the reduction percentage applied
     * @return SurvivorBenefitResult with recommended benefit selected
     */
    public static SurvivorBenefitResult eligible(
            BigDecimal ownBenefit,
            BigDecimal survivorBenefit,
            BigDecimal reductionPct) {
        boolean survivorIsHigher = survivorBenefit.compareTo(ownBenefit) > 0;
        return new SurvivorBenefitResult(
            ownBenefit,
            survivorBenefit,
            survivorIsHigher ? survivorBenefit : ownBenefit,
            survivorIsHigher ? BenefitType.SURVIVOR : BenefitType.OWN,
            true,
            Optional.empty(),
            reductionPct
        );
    }

    /**
     * Creates a result for an ineligible survivor benefit claim.
     *
     * @param ownBenefit the survivor's own adjusted benefit
     * @param reason the reason for ineligibility
     * @return SurvivorBenefitResult marked as ineligible
     */
    public static SurvivorBenefitResult ineligible(BigDecimal ownBenefit, String reason) {
        return new SurvivorBenefitResult(
            ownBenefit,
            BigDecimal.ZERO,
            ownBenefit,
            BenefitType.OWN,
            false,
            Optional.of(reason),
            BigDecimal.ZERO
        );
    }

    // ==================== Convenience Methods ====================

    /**
     * Returns true if survivor benefit exceeds own benefit.
     *
     * @return true if survivor benefit is recommended
     */
    public boolean isSurvivorBenefitHigher() {
        return recommendedType == BenefitType.SURVIVOR;
    }

    /**
     * Returns true if an early claiming reduction was applied.
     *
     * @return true if reductionPercentage > 0
     */
    public boolean reductionApplied() {
        return reductionPercentage.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Returns the benefit increase from claiming survivor vs own.
     *
     * @return the difference (survivor - own), may be negative if own is higher
     */
    public BigDecimal getSurvivorBenefitIncrease() {
        return survivorBenefit.subtract(ownAdjustedBenefit);
    }
}
