package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.xmljim.retirement.domain.calculator.PhaseOutCalculator;
import io.github.xmljim.retirement.domain.calculator.PhaseOutResult;
import io.github.xmljim.retirement.domain.config.IraPhaseOutLimits;
import io.github.xmljim.retirement.domain.config.IraPhaseOutLimits.PhaseOutRange;
import io.github.xmljim.retirement.domain.config.IraPhaseOutLimits.YearPhaseOuts;
import io.github.xmljim.retirement.domain.config.IrsContributionLimits;
import io.github.xmljim.retirement.domain.config.IrsContributionLimits.IraLimits;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.enums.FilingStatus;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;

/**
 * Default implementation of IRA phase-out calculations.
 *
 * <p>Uses IRS phase-out thresholds from configuration to determine
 * allowed contributions and deductible portions based on MAGI.
 *
 * <p>Phase-out formula:
 * <pre>
 * phaseOutPercentage = (MAGI - lowerBound) / (upperBound - lowerBound)
 * allowedAmount = limit * (1 - phaseOutPercentage)
 * </pre>
 *
 * <p>IRS rounding: Allowed amounts are rounded UP to the nearest $10
 * (with a $200 minimum if partially phased out).
 */
@Service
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "Spring-managed configuration objects are shared by design"
)
public class DefaultPhaseOutCalculator implements PhaseOutCalculator {

    private static final int CATCH_UP_AGE = 50;
    private static final int SCALE = 10;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final BigDecimal TEN = new BigDecimal("10");
    private static final BigDecimal MINIMUM_PARTIAL = new BigDecimal("200");

    private final IraPhaseOutLimits phaseOutLimits;
    private final IrsContributionLimits contributionLimits;

    /**
     * Creates a new DefaultPhaseOutCalculator.
     *
     * @param phaseOutLimits the IRA phase-out configuration
     * @param contributionLimits the IRS contribution limits
     */
    public DefaultPhaseOutCalculator(
            IraPhaseOutLimits phaseOutLimits,
            IrsContributionLimits contributionLimits) {
        this.phaseOutLimits = phaseOutLimits;
        this.contributionLimits = contributionLimits;
    }

    @Override
    public PhaseOutResult calculateRothIraPhaseOut(
            BigDecimal magi,
            FilingStatus filingStatus,
            int year,
            BigDecimal requestedContribution,
            int age) {

        MissingRequiredFieldException.requireNonNull(magi, "magi");
        MissingRequiredFieldException.requireNonNull(filingStatus, "filingStatus");

        BigDecimal maxContribution = getMaxIraContribution(year, age);
        BigDecimal effectiveRequest = requestedContribution != null
            ? requestedContribution.min(maxContribution)
            : maxContribution;

        YearPhaseOuts yearPhaseOuts = phaseOutLimits.getRothIraPhaseOutsForYear(year);
        PhaseOutRange range = yearPhaseOuts.forStatus(filingStatus);

        PhaseOutResult.Builder builder = PhaseOutResult.builder()
            .accountType(AccountType.ROTH_IRA)
            .requestedContribution(effectiveRequest);

        // Handle special MFS restrictions
        if (filingStatus.hasSpecialRestrictions()) {
            builder.addWarning("Married Filing Separately has reduced phase-out thresholds. "
                + "If not living with spouse, SINGLE thresholds may apply.");
        }

        return applyPhaseOut(builder, magi, range, maxContribution, effectiveRequest, true);
    }

    @Override
    public PhaseOutResult calculateTraditionalIraPhaseOut(
            BigDecimal magi,
            FilingStatus filingStatus,
            int year,
            BigDecimal requestedContribution,
            int age,
            boolean coveredByEmployerPlan,
            boolean spouseCoveredByEmployerPlan) {

        MissingRequiredFieldException.requireNonNull(magi, "magi");
        MissingRequiredFieldException.requireNonNull(filingStatus, "filingStatus");

        BigDecimal maxContribution = getMaxIraContribution(year, age);
        BigDecimal effectiveRequest = requestedContribution != null
            ? requestedContribution.min(maxContribution)
            : maxContribution;

        PhaseOutResult.Builder builder = PhaseOutResult.builder()
            .accountType(AccountType.TRADITIONAL_IRA)
            .requestedContribution(effectiveRequest);

        // Determine applicable phase-out range based on coverage
        PhaseOutRange range;
        if (!coveredByEmployerPlan && !spouseCoveredByEmployerPlan) {
            // Neither covered - full deduction regardless of income
            return builder
                .allowedContribution(effectiveRequest)
                .deductiblePortion(effectiveRequest)
                .phaseOutPercentage(BigDecimal.ZERO)
                .isFullyPhasedOut(false)
                .addWarning("No employer plan coverage - full deduction allowed at any income")
                .build();
        } else if (coveredByEmployerPlan) {
            // Contributor is covered - use covered thresholds
            YearPhaseOuts yearPhaseOuts = phaseOutLimits.getTraditionalIraCoveredPhaseOutsForYear(year);
            range = yearPhaseOuts.forStatus(filingStatus);
        } else {
            // Only spouse is covered - use spouse-covered thresholds (higher limits)
            // Note: spouse-covered only applies to married filers
            if (filingStatus.usesJointThresholds()) {
                YearPhaseOuts yearPhaseOuts =
                    phaseOutLimits.getTraditionalIraSpouseCoveredPhaseOutsForYear(year);
                range = yearPhaseOuts.forStatus(filingStatus);
                builder.addWarning("Spouse is covered by employer plan - using spouse-covered thresholds");
            } else {
                // Single filer with spouse covered doesn't make sense - full deduction
                return builder
                    .allowedContribution(effectiveRequest)
                    .deductiblePortion(effectiveRequest)
                    .phaseOutPercentage(BigDecimal.ZERO)
                    .isFullyPhasedOut(false)
                    .build();
            }
        }

        // Handle special MFS restrictions
        if (filingStatus.hasSpecialRestrictions()) {
            builder.addWarning("Married Filing Separately has very limited deduction phase-out range");
        }

        return applyPhaseOut(builder, magi, range, maxContribution, effectiveRequest, false);
    }

    @Override
    public BigDecimal getMaxIraContribution(int year, int age) {
        IraLimits limits = contributionLimits.getIraLimitsForYear(year);
        BigDecimal base = limits.baseLimit();
        if (age >= CATCH_UP_AGE) {
            return base.add(limits.catchUpLimit());
        }
        return base;
    }

    /**
     * Applies the phase-out calculation and builds the result.
     */
    private PhaseOutResult applyPhaseOut(
            PhaseOutResult.Builder builder,
            BigDecimal magi,
            PhaseOutRange range,
            BigDecimal maxContribution,
            BigDecimal effectiveRequest,
            boolean isRothIra) {

        BigDecimal lowerBound = range.lowerBound();
        BigDecimal upperBound = range.upperBound();

        // Below lower bound - full contribution allowed
        if (magi.compareTo(lowerBound) < 0) {
            return builder
                .allowedContribution(effectiveRequest)
                .deductiblePortion(isRothIra ? BigDecimal.ZERO : effectiveRequest)
                .phaseOutPercentage(BigDecimal.ZERO)
                .isFullyPhasedOut(false)
                .backdoorRothEligible(false)
                .build();
        }

        // At or above upper bound - fully phased out
        if (magi.compareTo(upperBound) >= 0) {
            builder.allowedContribution(BigDecimal.ZERO)
                .deductiblePortion(BigDecimal.ZERO)
                .phaseOutPercentage(BigDecimal.ONE)
                .isFullyPhasedOut(true);

            if (isRothIra) {
                builder.backdoorRothEligible(true)
                    .addWarning("Direct Roth IRA contribution not allowed. "
                        + "Consider backdoor Roth: contribute to Traditional IRA, then convert to Roth.");
            } else {
                builder.addWarning("Traditional IRA contribution allowed but not deductible. "
                    + "Consider Roth IRA or backdoor Roth strategy.");
            }
            return builder.build();
        }

        // Within phase-out range - calculate partial
        BigDecimal rangeSize = range.getRange();
        BigDecimal positionInRange = magi.subtract(lowerBound);
        BigDecimal phaseOutPct = positionInRange.divide(rangeSize, SCALE, ROUNDING_MODE);

        // Calculate allowed amount before IRS rounding
        BigDecimal reductionFactor = BigDecimal.ONE.subtract(phaseOutPct);
        BigDecimal calculatedAllowed = maxContribution.multiply(reductionFactor);

        // Apply IRS rounding (up to nearest $10, minimum $200 if partial)
        BigDecimal allowedAmount = roundUpToTen(calculatedAllowed);
        if (allowedAmount.compareTo(BigDecimal.ZERO) > 0
                && allowedAmount.compareTo(MINIMUM_PARTIAL) < 0) {
            allowedAmount = MINIMUM_PARTIAL;
        }

        // Cap at requested amount
        allowedAmount = allowedAmount.min(effectiveRequest);

        builder.allowedContribution(allowedAmount)
            .phaseOutPercentage(phaseOutPct)
            .isFullyPhasedOut(false);

        if (isRothIra) {
            builder.deductiblePortion(BigDecimal.ZERO)
                .backdoorRothEligible(false);
        } else {
            // For Traditional IRA, deductible = allowed when in phase-out
            builder.deductiblePortion(allowedAmount);
        }

        return builder.build();
    }

    /**
     * Rounds up to the nearest $10 (IRS rule for partial phase-out).
     */
    private BigDecimal roundUpToTen(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        // Ceiling division by 10, then multiply by 10
        return value.divide(TEN, 0, RoundingMode.CEILING).multiply(TEN);
    }
}
