package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.xmljim.retirement.domain.calculator.SocialSecurityCalculator;
import io.github.xmljim.retirement.domain.calculator.SpousalBenefitCalculator;
import io.github.xmljim.retirement.domain.config.SocialSecurityRules;
import io.github.xmljim.retirement.domain.enums.MaritalStatus;
import io.github.xmljim.retirement.domain.value.CoupleClaimingStrategy;
import io.github.xmljim.retirement.domain.value.MarriageInfo;
import io.github.xmljim.retirement.domain.value.SocialSecurityBenefit;
import io.github.xmljim.retirement.domain.value.SpousalBenefitResult;
import io.github.xmljim.retirement.domain.value.SurvivorBenefitResult;

/**
 * Default implementation of spousal and survivor benefit calculations.
 *
 * @see <a href="https://www.ssa.gov/benefits/retirement/planner/applying7.html">SSA Spousal</a>
 * @see <a href="https://www.ssa.gov/benefits/survivors/">SSA Survivor Benefits</a>
 */
@Service
public class DefaultSpousalBenefitCalculator implements SpousalBenefitCalculator {

    private static final int MIN_CLAIMING_AGE = 744; // 62 years

    private final SocialSecurityCalculator ssCalculator;
    private final SocialSecurityRules rules;

    @Autowired
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-managed beans")
    public DefaultSpousalBenefitCalculator(
            SocialSecurityCalculator ssCalculator,
            SocialSecurityRules rules) {
        this.ssCalculator = ssCalculator;
        this.rules = rules;
    }

    /**
     * Non-Spring constructor with defaults.
     */
    public DefaultSpousalBenefitCalculator() {
        this.ssCalculator = new DefaultSocialSecurityCalculator();
        this.rules = new SocialSecurityRules();
    }

    @Override
    public SpousalBenefitResult calculateSpousalBenefit(
            SocialSecurityBenefit ownBenefit,
            SocialSecurityBenefit spouseBenefit,
            MarriageInfo marriage,
            int claimingAgeMonths) {

        Optional<String> ineligible = checkSpousalEligibility(marriage, claimingAgeMonths);
        if (ineligible.isPresent()) {
            return SpousalBenefitResult.ineligible(
                ownBenefit.getAdjustedMonthlyBenefit(), ineligible.get());
        }

        BigDecimal spousalAmount = calculateSpousalAmount(
            spouseBenefit.getFraBenefit(), ownBenefit.getBirthYear(), claimingAgeMonths);

        return SpousalBenefitResult.eligible(
            ownBenefit.getAdjustedMonthlyBenefit(), spousalAmount);
    }

    @Override
    public SpousalBenefitResult calculateDivorcedSpouseBenefit(
            SocialSecurityBenefit ownBenefit,
            SocialSecurityBenefit exSpouseBenefit,
            MarriageInfo marriage,
            int claimingAgeMonths) {

        // Ex-spouse must be 62+ (we use MIN_CLAIMING_AGE as proxy)
        Optional<String> ineligible = checkDivorcedSpouseEligibility(
            marriage, claimingAgeMonths, MIN_CLAIMING_AGE);
        if (ineligible.isPresent()) {
            return SpousalBenefitResult.ineligible(
                ownBenefit.getAdjustedMonthlyBenefit(), ineligible.get());
        }

        BigDecimal spousalAmount = calculateSpousalAmount(
            exSpouseBenefit.getFraBenefit(), ownBenefit.getBirthYear(), claimingAgeMonths);

        return SpousalBenefitResult.eligible(
            ownBenefit.getAdjustedMonthlyBenefit(), spousalAmount);
    }

    @Override
    public SurvivorBenefitResult calculateSurvivorBenefit(
            SocialSecurityBenefit survivorOwnBenefit,
            SocialSecurityBenefit deceasedBenefit,
            MarriageInfo marriage,
            int claimingAgeMonths) {

        Optional<String> ineligible = checkSurvivorEligibility(
            marriage, claimingAgeMonths, false);
        if (ineligible.isPresent()) {
            return SurvivorBenefitResult.ineligible(
                survivorOwnBenefit.getAdjustedMonthlyBenefit(), ineligible.get());
        }

        int fraMonths = ssCalculator.calculateFraMonths(survivorOwnBenefit.getBirthYear());
        BigDecimal deceasedAmount = deceasedBenefit.getAdjustedMonthlyBenefit();

        BigDecimal survivorAmount = ssCalculator.calculateAdjustedSurvivorBenefit(
            deceasedAmount, claimingAgeMonths, fraMonths);
        BigDecimal reduction = ssCalculator.calculateSurvivorReduction(
            claimingAgeMonths, fraMonths);

        return SurvivorBenefitResult.eligible(
            survivorOwnBenefit.getAdjustedMonthlyBenefit(), survivorAmount, reduction);
    }

    @Override
    public CoupleClaimingStrategy optimizeClaimingStrategy(
            SocialSecurityBenefit person1Benefit,
            SocialSecurityBenefit person2Benefit,
            MarriageInfo marriage) {

        // Determine higher/lower earner
        boolean person1IsHigher = person1Benefit.getFraBenefit()
            .compareTo(person2Benefit.getFraBenefit()) >= 0;
        SocialSecurityBenefit higher = person1IsHigher ? person1Benefit : person2Benefit;
        SocialSecurityBenefit lower = person1IsHigher ? person2Benefit : person1Benefit;

        // Simple strategy: higher earner delays to 70, lower claims at 62
        int higherAge = 840; // 70 years
        int lowerAge = 744;  // 62 years

        BigDecimal higherBenefit = calculateBenefitAtAge(higher, higherAge);
        BigDecimal lowerBenefit = calculateBenefitAtAge(lower, lowerAge);

        // Check if lower earner should use spousal
        BigDecimal spousalBenefit = calculateSpousalAmount(
            higher.getFraBenefit(), lower.getBirthYear(), lowerAge);
        boolean usingSpousal = spousalBenefit.compareTo(lowerBenefit) > 0;
        if (usingSpousal) {
            lowerBenefit = spousalBenefit;
        }

        List<CoupleClaimingStrategy.AlternativeStrategy> alternatives = buildAlternatives(
            higher, lower, marriage);

        return new CoupleClaimingStrategy(
            higherAge, lowerAge, higherBenefit, lowerBenefit,
            higherBenefit.add(lowerBenefit), usingSpousal,
            "Higher earner delays to 70 to maximize survivor benefit; lower earner claims early",
            alternatives);
    }

    @Override
    public Optional<String> checkSpousalEligibility(MarriageInfo marriage, int claimingAgeMonths) {
        if (marriage.status() != MaritalStatus.MARRIED) {
            return Optional.of("Must be married to claim spousal benefits");
        }
        if (claimingAgeMonths < MIN_CLAIMING_AGE) {
            return Optional.of("Must be at least 62 to claim spousal benefits");
        }
        int minMonths = rules.getSpousal().getMinimumMarriageMonths();
        if (!marriage.meetsMinimumMarriageDuration(minMonths, LocalDate.now())) {
            return Optional.of("Must be married at least 1 year");
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> checkDivorcedSpouseEligibility(
            MarriageInfo marriage, int claimingAgeMonths, int exSpouseAgeMonths) {
        if (marriage.status() != MaritalStatus.DIVORCED) {
            return Optional.of("Must be divorced to claim divorced spouse benefits");
        }
        if (claimingAgeMonths < MIN_CLAIMING_AGE) {
            return Optional.of("Must be at least 62 to claim benefits");
        }
        if (exSpouseAgeMonths < MIN_CLAIMING_AGE) {
            return Optional.of("Ex-spouse must be at least 62");
        }
        if (!marriage.isEligibleForDivorcedSpouseBenefits(LocalDate.now())) {
            return Optional.of("Marriage must have lasted 10+ years and divorced 2+ years");
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> checkSurvivorEligibility(
            MarriageInfo marriage, int claimingAgeMonths, boolean isDisabled) {
        if (marriage.status() != MaritalStatus.WIDOWED) {
            return Optional.of("Must be widowed to claim survivor benefits");
        }
        int minAge = isDisabled
            ? rules.getSurvivor().getDisabledMinimumAgeMonths()
            : rules.getSurvivor().getMinimumClaimingAgeMonths();
        if (claimingAgeMonths < minAge) {
            return Optional.of("Must be at least " + (minAge / 12) + " to claim survivor benefits");
        }
        int minMarriage = rules.getSurvivor().getMinimumMarriageMonths();
        if (!marriage.meetsMinimumMarriageDuration(minMarriage, LocalDate.now())) {
            return Optional.of("Must have been married at least 9 months");
        }
        return Optional.empty();
    }

    private BigDecimal calculateSpousalAmount(
            BigDecimal spouseFraBenefit, int claimantBirthYear, int claimingAgeMonths) {
        BigDecimal percentage = rules.getSpousal().getBenefitPercentage();
        BigDecimal baseSpousal = spouseFraBenefit.multiply(percentage);

        int fraMonths = ssCalculator.calculateFraMonths(claimantBirthYear);
        if (claimingAgeMonths >= fraMonths) {
            return baseSpousal;
        }

        // Reduce spousal benefit for early claiming
        BigDecimal reduction = ssCalculator.calculateEarlyReduction(fraMonths - claimingAgeMonths);
        return baseSpousal.multiply(BigDecimal.ONE.subtract(reduction));
    }

    private BigDecimal calculateBenefitAtAge(SocialSecurityBenefit benefit, int ageMonths) {
        int fraMonths = ssCalculator.calculateFraMonths(benefit.getBirthYear());
        return ssCalculator.calculateAdjustedBenefit(
            benefit.getFraBenefit(), fraMonths, ageMonths);
    }

    private List<CoupleClaimingStrategy.AlternativeStrategy> buildAlternatives(
            SocialSecurityBenefit higher, SocialSecurityBenefit lower, MarriageInfo marriage) {
        List<CoupleClaimingStrategy.AlternativeStrategy> alts = new ArrayList<>();

        // Both at FRA
        int higherFra = ssCalculator.calculateFraMonths(higher.getBirthYear());
        int lowerFra = ssCalculator.calculateFraMonths(lower.getBirthYear());
        BigDecimal h1 = calculateBenefitAtAge(higher, higherFra);
        BigDecimal l1 = calculateBenefitAtAge(lower, lowerFra);
        alts.add(new CoupleClaimingStrategy.AlternativeStrategy(
            higherFra, lowerFra, h1.add(l1), "Both claim at FRA"));

        // Both at 62
        BigDecimal h2 = calculateBenefitAtAge(higher, 744);
        BigDecimal l2 = calculateBenefitAtAge(lower, 744);
        alts.add(new CoupleClaimingStrategy.AlternativeStrategy(
            744, 744, h2.add(l2), "Both claim early at 62"));

        return alts;
    }
}
