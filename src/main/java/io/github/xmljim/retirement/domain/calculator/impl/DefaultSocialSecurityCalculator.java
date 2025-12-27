package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import io.github.xmljim.retirement.domain.calculator.SocialSecurityCalculator;

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
 * (Public Law 98-21) and may be updated by future legislation.
 *
 * <p>This calculator is stateless and thread-safe.
 */
public class DefaultSocialSecurityCalculator implements SocialSecurityCalculator {

    private static final int INTERNAL_SCALE = 10;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final MathContext PRECISION = MathContext.DECIMAL128;

    // FRA constants in months
    private static final int FRA_66_YEARS = 792;  // 66 * 12
    private static final int FRA_67_YEARS = 804;  // 67 * 12

    // Birth year thresholds
    private static final int BIRTH_YEAR_1955 = 1955;
    private static final int BIRTH_YEAR_1960 = 1960;

    // Early claiming reduction rates (per month)
    // First 36 months: 5/9 of 1% = 0.00555556
    private static final BigDecimal EARLY_REDUCTION_RATE_FIRST_36 =
        BigDecimal.valueOf(5).divide(BigDecimal.valueOf(900), INTERNAL_SCALE, ROUNDING);

    // Additional months beyond 36: 5/12 of 1% = 0.00416667
    private static final BigDecimal EARLY_REDUCTION_RATE_BEYOND_36 =
        BigDecimal.valueOf(5).divide(BigDecimal.valueOf(1200), INTERNAL_SCALE, ROUNDING);

    // Threshold for switching between reduction rates
    private static final int EARLY_REDUCTION_THRESHOLD_MONTHS = 36;

    // Delayed retirement credit rate (per month)
    // 8% per year = 2/3% per month = 0.00666667
    private static final BigDecimal DELAYED_CREDIT_RATE =
        BigDecimal.valueOf(8).divide(BigDecimal.valueOf(1200), INTERNAL_SCALE, ROUNDING);

    /**
     * Creates a new DefaultSocialSecurityCalculator.
     */
    public DefaultSocialSecurityCalculator() {
        // Stateless - no initialization needed
    }

    @Override
    public int calculateFraMonths(int birthYear) {
        if (birthYear <= 1954) {
            return FRA_66_YEARS;
        }
        if (birthYear >= BIRTH_YEAR_1960) {
            return FRA_67_YEARS;
        }

        // Birth years 1955-1959: FRA increases by 2 months each year
        // 1955 → 66y 2m (794), 1956 → 66y 4m (796), etc.
        int additionalMonths = (birthYear - 1954) * 2;
        return FRA_66_YEARS + additionalMonths;
    }

    @Override
    public BigDecimal calculateEarlyReduction(int monthsEarly) {
        if (monthsEarly <= 0) {
            return BigDecimal.ZERO;
        }

        if (monthsEarly <= EARLY_REDUCTION_THRESHOLD_MONTHS) {
            // First 36 months: 5/9 of 1% per month
            return EARLY_REDUCTION_RATE_FIRST_36
                .multiply(BigDecimal.valueOf(monthsEarly), PRECISION)
                .setScale(INTERNAL_SCALE, ROUNDING);
        }

        // Beyond 36 months: first 36 at higher rate, rest at lower rate
        BigDecimal first36Reduction = EARLY_REDUCTION_RATE_FIRST_36
            .multiply(BigDecimal.valueOf(EARLY_REDUCTION_THRESHOLD_MONTHS), PRECISION);

        int additionalMonths = monthsEarly - EARLY_REDUCTION_THRESHOLD_MONTHS;
        BigDecimal additionalReduction = EARLY_REDUCTION_RATE_BEYOND_36
            .multiply(BigDecimal.valueOf(additionalMonths), PRECISION);

        return first36Reduction.add(additionalReduction).setScale(INTERNAL_SCALE, ROUNDING);
    }

    @Override
    public BigDecimal calculateDelayedCredits(int monthsDelayed) {
        if (monthsDelayed <= 0) {
            return BigDecimal.ZERO;
        }

        // 8% per year (2/3% per month)
        return DELAYED_CREDIT_RATE
            .multiply(BigDecimal.valueOf(monthsDelayed), PRECISION)
            .setScale(INTERNAL_SCALE, ROUNDING);
    }

    @Override
    public BigDecimal calculateAdjustedBenefit(BigDecimal fraBenefit, int fraMonths, int claimingMonths) {
        if (fraBenefit == null || fraBenefit.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (claimingMonths == fraMonths) {
            // Claiming at FRA - no adjustment
            return fraBenefit;
        }

        if (claimingMonths < fraMonths) {
            // Early claiming - apply reduction
            int monthsEarly = fraMonths - claimingMonths;
            BigDecimal reduction = calculateEarlyReduction(monthsEarly);
            BigDecimal multiplier = BigDecimal.ONE.subtract(reduction);
            return fraBenefit.multiply(multiplier, PRECISION).setScale(INTERNAL_SCALE, ROUNDING);
        }

        // Delayed claiming - apply credits
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

        // Compound: benefit * (1 + colaRate)^years
        BigDecimal multiplier = BigDecimal.ONE.add(colaRate).pow(years, PRECISION);
        return benefit.multiply(multiplier, PRECISION).setScale(INTERNAL_SCALE, ROUNDING);
    }
}
