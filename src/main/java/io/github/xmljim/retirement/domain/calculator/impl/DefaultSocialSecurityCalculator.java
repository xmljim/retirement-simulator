package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.xmljim.retirement.domain.calculator.SocialSecurityCalculator;
import io.github.xmljim.retirement.domain.config.SocialSecurityRules;

/**
 * Default implementation of Social Security benefit calculations.
 *
 * <p>Formulas and values sourced from the Social Security Administration:
 * <ul>
 *   <li>FRA by birth year: <a href="https://www.ssa.gov/oact/ProgData/nra.html">SSA Normal Retirement Age</a></li>
 *   <li>Early/Delayed adjustments: <a href="https://www.ssa.gov/oact/ProgData/ar_drc.html">SSA Adjustment Rates</a></li>
 * </ul>
 *
 * <p>These values were established by the Social Security Amendments of 1983
 * (Public Law 98-21) and may be updated by future legislation. All values are
 * configurable via {@link SocialSecurityRules}.
 *
 * <p>This calculator is stateless and thread-safe.
 */
@Service
public class DefaultSocialSecurityCalculator implements SocialSecurityCalculator {

    private static final int INTERNAL_SCALE = 10;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final MathContext PRECISION = MathContext.DECIMAL128;

    private final SocialSecurityRules rules;

    /**
     * Creates a new DefaultSocialSecurityCalculator with the given rules.
     *
     * @param rules the Social Security rules configuration
     */
    @Autowired
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "SocialSecurityRules is a Spring-managed singleton bean"
    )
    public DefaultSocialSecurityCalculator(SocialSecurityRules rules) {
        this.rules = rules;
    }

    /**
     * Creates a calculator with default rules (for non-Spring usage).
     *
     * <p>This constructor creates a calculator with hardcoded default values
     * for use in tests or non-Spring contexts. The defaults match current
     * SSA rules as of 2024.
     */
    public DefaultSocialSecurityCalculator() {
        this.rules = createDefaultRules();
    }

    @Override
    public int calculateFraMonths(int birthYear) {
        return rules.getFraMonthsForBirthYear(birthYear);
    }

    @Override
    public BigDecimal calculateEarlyReduction(int monthsEarly) {
        if (monthsEarly <= 0) {
            return BigDecimal.ZERO;
        }

        SocialSecurityRules.EarlyReduction er = rules.getEarlyReduction();
        int firstTierMonths = er.getFirstTierMonths();
        BigDecimal firstTierRate = er.getFirstTierRate(INTERNAL_SCALE);
        BigDecimal secondTierRate = er.getSecondTierRate(INTERNAL_SCALE);

        if (monthsEarly <= firstTierMonths) {
            return firstTierRate
                .multiply(BigDecimal.valueOf(monthsEarly), PRECISION)
                .setScale(INTERNAL_SCALE, ROUNDING);
        }

        // Beyond threshold: first tier at higher rate, rest at lower rate
        BigDecimal firstTierReduction = firstTierRate
            .multiply(BigDecimal.valueOf(firstTierMonths), PRECISION);

        int additionalMonths = monthsEarly - firstTierMonths;
        BigDecimal additionalReduction = secondTierRate
            .multiply(BigDecimal.valueOf(additionalMonths), PRECISION);

        return firstTierReduction.add(additionalReduction).setScale(INTERNAL_SCALE, ROUNDING);
    }

    @Override
    public BigDecimal calculateDelayedCredits(int monthsDelayed) {
        if (monthsDelayed <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal rate = rules.getDelayedCredits().getRate(INTERNAL_SCALE);
        return rate
            .multiply(BigDecimal.valueOf(monthsDelayed), PRECISION)
            .setScale(INTERNAL_SCALE, ROUNDING);
    }

    @Override
    public BigDecimal calculateAdjustedBenefit(BigDecimal fraBenefit, int fraMonths, int claimingMonths) {
        if (fraBenefit == null || fraBenefit.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (claimingMonths == fraMonths) {
            return fraBenefit;
        }

        if (claimingMonths < fraMonths) {
            int monthsEarly = fraMonths - claimingMonths;
            BigDecimal reduction = calculateEarlyReduction(monthsEarly);
            BigDecimal multiplier = BigDecimal.ONE.subtract(reduction);
            return fraBenefit.multiply(multiplier, PRECISION).setScale(INTERNAL_SCALE, ROUNDING);
        }

        int monthsDelayed = claimingMonths - fraMonths;
        BigDecimal credit = calculateDelayedCredits(monthsDelayed);
        BigDecimal multiplier = BigDecimal.ONE.add(credit);
        return fraBenefit.multiply(multiplier, PRECISION).setScale(INTERNAL_SCALE, ROUNDING);
    }

    @Override
    public BigDecimal applyColaAdjustment(BigDecimal benefit, BigDecimal colaRate, int years) {
        if (benefit == null || benefit.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (colaRate == null || years <= 0 || colaRate.compareTo(BigDecimal.ZERO) == 0) {
            return benefit;
        }

        BigDecimal multiplier = BigDecimal.ONE.add(colaRate).pow(years, PRECISION);
        return benefit.multiply(multiplier, PRECISION).setScale(INTERNAL_SCALE, ROUNDING);
    }

    /**
     * Returns the rules configuration.
     *
     * @return the Social Security rules
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "SocialSecurityRules is a Spring-managed singleton bean; returning reference is intentional"
    )
    public SocialSecurityRules getRules() {
        return rules;
    }

    // ==================== Survivor Benefit Methods ====================

    @Override
    public int getSurvivorMinimumClaimingAge() {
        return rules.getSurvivor().getMinimumClaimingAgeMonths();
    }

    @Override
    public int getSurvivorDisabledMinimumClaimingAge() {
        return rules.getSurvivor().getDisabledMinimumAgeMonths();
    }

    @Override
    public BigDecimal calculateSurvivorReduction(int claimingAgeMonths, int fraMonths) {
        if (claimingAgeMonths >= fraMonths) {
            return BigDecimal.ZERO;
        }

        int minimumAge = getSurvivorMinimumClaimingAge();
        if (claimingAgeMonths < minimumAge) {
            // Can't claim before minimum age; return max reduction
            claimingAgeMonths = minimumAge;
        }

        // Survivor reduction is linear from age 60 to FRA
        // Max reduction is ~28.5% at age 60 for FRA 67
        BigDecimal maxReduction = rules.getSurvivor().getMaxReduction(INTERNAL_SCALE);

        int monthsUntilFra = fraMonths - claimingAgeMonths;
        int totalMonthsRange = fraMonths - minimumAge;

        if (totalMonthsRange <= 0) {
            return BigDecimal.ZERO;
        }

        // Prorate reduction linearly
        return maxReduction
            .multiply(BigDecimal.valueOf(monthsUntilFra), PRECISION)
            .divide(BigDecimal.valueOf(totalMonthsRange), INTERNAL_SCALE, ROUNDING);
    }

    @Override
    public BigDecimal calculateAdjustedSurvivorBenefit(
            BigDecimal deceasedBenefit,
            int claimingAgeMonths,
            int fraMonths) {
        if (deceasedBenefit == null || deceasedBenefit.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (claimingAgeMonths >= fraMonths) {
            return deceasedBenefit;
        }

        BigDecimal reduction = calculateSurvivorReduction(claimingAgeMonths, fraMonths);
        BigDecimal multiplier = BigDecimal.ONE.subtract(reduction);
        return deceasedBenefit.multiply(multiplier, PRECISION).setScale(INTERNAL_SCALE, ROUNDING);
    }

    /**
     * Creates default rules for non-Spring usage.
     */
    private static SocialSecurityRules createDefaultRules() {
        SocialSecurityRules defaultRules = new SocialSecurityRules();

        // FRA table per SSA
        defaultRules.getFraTable().add(
            new SocialSecurityRules.FraEntry(null, 1954, 792));  // 66 years
        defaultRules.getFraTable().add(
            new SocialSecurityRules.FraEntry(1955, 1955, 794));  // 66y 2m
        defaultRules.getFraTable().add(
            new SocialSecurityRules.FraEntry(1956, 1956, 796));  // 66y 4m
        defaultRules.getFraTable().add(
            new SocialSecurityRules.FraEntry(1957, 1957, 798));  // 66y 6m
        defaultRules.getFraTable().add(
            new SocialSecurityRules.FraEntry(1958, 1958, 800));  // 66y 8m
        defaultRules.getFraTable().add(
            new SocialSecurityRules.FraEntry(1959, 1959, 802));  // 66y 10m
        defaultRules.getFraTable().add(
            new SocialSecurityRules.FraEntry(1960, null, 804));  // 67 years

        // Claiming limits (62-70)
        defaultRules.getClaiming().setMinimumAgeMonths(744);  // 62 years
        defaultRules.getClaiming().setMaximumAgeMonths(840);  // 70 years

        // Early reduction rates: 5/9 of 1% for first 36, 5/12 of 1% additional
        defaultRules.getEarlyReduction().setFirstTierMonths(36);
        defaultRules.getEarlyReduction().setFirstTierRateNumerator(5);
        defaultRules.getEarlyReduction().setFirstTierRateDenominator(900);
        defaultRules.getEarlyReduction().setSecondTierRateNumerator(5);
        defaultRules.getEarlyReduction().setSecondTierRateDenominator(1200);

        // Delayed credits: 8% per year
        defaultRules.getDelayedCredits().setRateNumerator(8);
        defaultRules.getDelayedCredits().setRateDenominator(1200);

        return defaultRules;
    }
}
