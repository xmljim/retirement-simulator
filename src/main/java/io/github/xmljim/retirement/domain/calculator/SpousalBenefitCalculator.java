package io.github.xmljim.retirement.domain.calculator;

import java.util.Optional;

import io.github.xmljim.retirement.domain.value.CoupleClaimingStrategy;
import io.github.xmljim.retirement.domain.value.MarriageInfo;
import io.github.xmljim.retirement.domain.value.SocialSecurityBenefit;
import io.github.xmljim.retirement.domain.value.SpousalBenefitResult;
import io.github.xmljim.retirement.domain.value.SurvivorBenefitResult;

/**
 * Calculator for Social Security spousal and survivor benefits.
 *
 * <p>Provides methods for calculating:
 * <ul>
 *   <li><strong>Spousal benefits</strong>: Up to 50% of higher earner's FRA benefit</li>
 *   <li><strong>Divorced spouse benefits</strong>: For marriages lasting 10+ years</li>
 *   <li><strong>Survivor benefits</strong>: Higher of own or deceased's benefit</li>
 *   <li><strong>Optimal claiming strategy</strong>: Maximize lifetime couple benefits</li>
 * </ul>
 *
 * <p>Key SSA rules implemented:
 * <ul>
 *   <li>Spousal benefit is based on higher earner's FRA benefit (not adjusted)</li>
 *   <li>Spousal benefit reduced if claimed before claimant's own FRA</li>
 *   <li>Survivor can claim at 60 (reduced) or FRA (full)</li>
 *   <li>Marriage duration requirements: 1 year (spousal), 10 years (divorced), 9 months (survivor)</li>
 * </ul>
 *
 * @see <a href="https://www.ssa.gov/benefits/retirement/planner/applying7.html">SSA Spousal</a>
 * @see <a href="https://www.ssa.gov/benefits/retirement/planner/applying6.html">SSA Divorced</a>
 * @see <a href="https://www.ssa.gov/benefits/survivors/">SSA Survivor Benefits</a>
 */
public interface SpousalBenefitCalculator {

    /**
     * Calculates spousal benefit for a married person.
     *
     * <p>The spousal benefit is up to 50% of the higher-earning spouse's
     * FRA benefit. The result indicates whether spousal or own benefit is
     * recommended.
     *
     * <p>Requirements:
     * <ul>
     *   <li>Marriage duration of at least 1 year</li>
     *   <li>Higher earner must have filed for benefits (or be 62+)</li>
     * </ul>
     *
     * @param ownBenefit the claimant's own Social Security benefit
     * @param spouseBenefit the spouse's Social Security benefit
     * @param marriage the marriage information
     * @param claimingAgeMonths the age in months when claiming
     * @return SpousalBenefitResult with recommendation
     */
    SpousalBenefitResult calculateSpousalBenefit(
        SocialSecurityBenefit ownBenefit,
        SocialSecurityBenefit spouseBenefit,
        MarriageInfo marriage,
        int claimingAgeMonths
    );

    /**
     * Calculates divorced spouse benefit.
     *
     * <p>Divorced persons may be eligible for spousal benefits if:
     * <ul>
     *   <li>Marriage lasted at least 10 years</li>
     *   <li>Divorced for at least 2 years</li>
     *   <li>Currently unmarried (or remarried after 60)</li>
     *   <li>Both ex-spouses are at least 62</li>
     * </ul>
     *
     * @param ownBenefit the claimant's own Social Security benefit
     * @param exSpouseBenefit the ex-spouse's Social Security benefit
     * @param marriage the marriage information (must be DIVORCED status)
     * @param claimingAgeMonths the age in months when claiming
     * @return SpousalBenefitResult with recommendation
     */
    SpousalBenefitResult calculateDivorcedSpouseBenefit(
        SocialSecurityBenefit ownBenefit,
        SocialSecurityBenefit exSpouseBenefit,
        MarriageInfo marriage,
        int claimingAgeMonths
    );

    /**
     * Calculates survivor benefit after spouse's death.
     *
     * <p>The survivor receives the higher of:
     * <ul>
     *   <li>Their own retirement benefit</li>
     *   <li>The deceased spouse's benefit (possibly reduced for early claiming)</li>
     * </ul>
     *
     * <p>Requirements:
     * <ul>
     *   <li>Marriage duration of at least 9 months (exception for accidents)</li>
     *   <li>Age 60+ (50 if disabled)</li>
     *   <li>Not remarried before age 60</li>
     * </ul>
     *
     * @param survivorOwnBenefit the survivor's own Social Security benefit
     * @param deceasedBenefit the deceased spouse's Social Security benefit
     * @param marriage the marriage information (must be WIDOWED status)
     * @param claimingAgeMonths the age in months when claiming survivor benefit
     * @return SurvivorBenefitResult with recommendation
     */
    SurvivorBenefitResult calculateSurvivorBenefit(
        SocialSecurityBenefit survivorOwnBenefit,
        SocialSecurityBenefit deceasedBenefit,
        MarriageInfo marriage,
        int claimingAgeMonths
    );

    /**
     * Determines optimal claiming strategy for a married couple.
     *
     * <p>Analyzes various claiming age combinations to maximize total
     * lifetime benefits, considering:
     * <ul>
     *   <li>Each spouse's FRA benefit</li>
     *   <li>Early claiming reductions / delayed credits</li>
     *   <li>Spousal benefit availability</li>
     *   <li>Survivor benefit implications</li>
     * </ul>
     *
     * @param person1Benefit first person's Social Security benefit
     * @param person2Benefit second person's Social Security benefit
     * @param marriage the marriage information
     * @return CoupleClaimingStrategy with recommendations
     */
    CoupleClaimingStrategy optimizeClaimingStrategy(
        SocialSecurityBenefit person1Benefit,
        SocialSecurityBenefit person2Benefit,
        MarriageInfo marriage
    );

    /**
     * Checks eligibility for spousal benefits.
     *
     * @param marriage the marriage information
     * @param claimingAgeMonths the claiming age in months
     * @return Optional containing ineligibility reason, or empty if eligible
     */
    Optional<String> checkSpousalEligibility(
        MarriageInfo marriage,
        int claimingAgeMonths
    );

    /**
     * Checks eligibility for divorced spouse benefits.
     *
     * @param marriage the marriage information
     * @param claimingAgeMonths the claiming age in months
     * @param exSpouseAgeMonths the ex-spouse's current age in months
     * @return Optional containing ineligibility reason, or empty if eligible
     */
    Optional<String> checkDivorcedSpouseEligibility(
        MarriageInfo marriage,
        int claimingAgeMonths,
        int exSpouseAgeMonths
    );

    /**
     * Checks eligibility for survivor benefits.
     *
     * @param marriage the marriage information
     * @param claimingAgeMonths the claiming age in months
     * @param isDisabled whether the survivor is disabled
     * @return Optional containing ineligibility reason, or empty if eligible
     */
    Optional<String> checkSurvivorEligibility(
        MarriageInfo marriage,
        int claimingAgeMonths,
        boolean isDisabled
    );
}
