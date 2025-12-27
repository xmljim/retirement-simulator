package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.xmljim.retirement.domain.calculator.BenefitTaxationCalculator;
import io.github.xmljim.retirement.domain.config.SocialSecurityRules;
import io.github.xmljim.retirement.domain.enums.FilingStatus;
import io.github.xmljim.retirement.domain.value.TaxationResult;

/**
 * Default implementation of Social Security benefit taxation calculator.
 *
 * <p>Implements the IRS Publication 915 worksheet calculation for determining
 * the taxable portion of Social Security benefits.
 *
 * <p>Key rules:
 * <ul>
 *   <li>Combined income = AGI + non-taxable interest + 50% of SS benefits</li>
 *   <li>Below lower threshold: 0% taxable</li>
 *   <li>Between thresholds: Up to 50% taxable (lesser of formula or 50%)</li>
 *   <li>Above upper threshold: Up to 85% taxable (lesser of formula or 85%)</li>
 * </ul>
 *
 * <p><b>Note:</b> Thresholds are NOT indexed for inflation.
 *
 * @see <a href="https://www.irs.gov/publications/p915">IRS Publication 915</a>
 */
@Service
public class DefaultBenefitTaxationCalculator implements BenefitTaxationCalculator {

    private static final int SCALE = 10;
    private static final BigDecimal HALF = new BigDecimal("0.50");
    private static final BigDecimal EIGHTY_FIVE_PERCENT = new BigDecimal("0.85");

    private final SocialSecurityRules rules;

    @Autowired
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-managed beans")
    public DefaultBenefitTaxationCalculator(SocialSecurityRules rules) {
        this.rules = rules;
    }

    /**
     * Non-Spring constructor with defaults.
     */
    public DefaultBenefitTaxationCalculator() {
        this.rules = new SocialSecurityRules();
    }

    @Override
    public TaxationResult calculate(
            BigDecimal ssAnnualBenefit,
            BigDecimal adjustedGrossIncome,
            BigDecimal nonTaxableInterest,
            FilingStatus filingStatus) {
        // Default: assume MFS is living with spouse (worst case)
        return calculate(ssAnnualBenefit, adjustedGrossIncome, nonTaxableInterest,
            filingStatus, true);
    }

    @Override
    public TaxationResult calculate(
            BigDecimal ssAnnualBenefit,
            BigDecimal adjustedGrossIncome,
            BigDecimal nonTaxableInterest,
            FilingStatus filingStatus,
            boolean mfsLivingWithSpouse) {

        BigDecimal combinedIncome = calculateCombinedIncome(
            ssAnnualBenefit, adjustedGrossIncome, nonTaxableInterest);

        // Get effective thresholds (MFS living with spouse uses $0 thresholds)
        BigDecimal lowerThreshold = getEffectiveLowerThreshold(filingStatus, mfsLivingWithSpouse);
        BigDecimal upperThreshold = getEffectiveUpperThreshold(filingStatus, mfsLivingWithSpouse);

        // Below lower threshold: nothing taxable
        if (combinedIncome.compareTo(lowerThreshold) <= 0) {
            return TaxationResult.none(ssAnnualBenefit, combinedIncome);
        }

        // Calculate taxable amount using IRS worksheet logic
        BigDecimal taxableAmount = calculateTaxableAmount(
            ssAnnualBenefit, combinedIncome, lowerThreshold, upperThreshold);

        // Determine tier based on combined income
        if (combinedIncome.compareTo(upperThreshold) <= 0) {
            return TaxationResult.fiftyPercent(ssAnnualBenefit, combinedIncome, taxableAmount);
        } else {
            return TaxationResult.eightyFivePercent(ssAnnualBenefit, combinedIncome, taxableAmount);
        }
    }

    @Override
    public BigDecimal calculateCombinedIncome(
            BigDecimal ssAnnualBenefit,
            BigDecimal adjustedGrossIncome,
            BigDecimal nonTaxableInterest) {
        // Combined income = AGI + non-taxable interest + 50% of SS benefits
        BigDecimal halfSS = ssAnnualBenefit.multiply(HALF);
        return adjustedGrossIncome.add(nonTaxableInterest).add(halfSS);
    }

    @Override
    public BigDecimal getLowerThreshold(FilingStatus filingStatus) {
        if (filingStatus.usesJointThresholds()) {
            return rules.getTaxation().getJointLowerThreshold();
        }
        return rules.getTaxation().getSingleLowerThreshold();
    }

    @Override
    public BigDecimal getUpperThreshold(FilingStatus filingStatus) {
        if (filingStatus.usesJointThresholds()) {
            return rules.getTaxation().getJointUpperThreshold();
        }
        return rules.getTaxation().getSingleUpperThreshold();
    }

    /**
     * Gets the effective lower threshold, handling MFS living with spouse.
     *
     * <p>MFS living with spouse has $0 threshold (always taxable).
     *
     * @param filingStatus the filing status
     * @param mfsLivingWithSpouse true if MFS and living with spouse
     * @return the effective lower threshold
     */
    private BigDecimal getEffectiveLowerThreshold(FilingStatus filingStatus, boolean mfsLivingWithSpouse) {
        if (filingStatus == FilingStatus.MARRIED_FILING_SEPARATELY && mfsLivingWithSpouse) {
            return BigDecimal.ZERO;
        }
        if (filingStatus == FilingStatus.MARRIED_FILING_SEPARATELY) {
            // MFS not living with spouse uses Single thresholds
            return rules.getTaxation().getSingleLowerThreshold();
        }
        return getLowerThreshold(filingStatus);
    }

    /**
     * Gets the effective upper threshold, handling MFS living with spouse.
     *
     * @param filingStatus the filing status
     * @param mfsLivingWithSpouse true if MFS and living with spouse
     * @return the effective upper threshold
     */
    private BigDecimal getEffectiveUpperThreshold(FilingStatus filingStatus, boolean mfsLivingWithSpouse) {
        if (filingStatus == FilingStatus.MARRIED_FILING_SEPARATELY && mfsLivingWithSpouse) {
            return BigDecimal.ZERO;
        }
        if (filingStatus == FilingStatus.MARRIED_FILING_SEPARATELY) {
            // MFS not living with spouse uses Single thresholds
            return rules.getTaxation().getSingleUpperThreshold();
        }
        return getUpperThreshold(filingStatus);
    }

    /**
     * Calculates the taxable amount using IRS Publication 915 worksheet logic.
     *
     * <p>The calculation follows these rules:
     * <ul>
     *   <li>50% tier: Lesser of (50% of excess over lower threshold) or (50% of benefits)</li>
     *   <li>85% tier: Lesser of (85% of benefits) or (previous tier base + 85% of excess over upper)</li>
     * </ul>
     *
     * @param ssAnnualBenefit the annual SS benefit
     * @param combinedIncome the calculated combined income
     * @param lowerThreshold the lower threshold for the filing status
     * @param upperThreshold the upper threshold for the filing status
     * @return the taxable amount
     */
    private BigDecimal calculateTaxableAmount(
            BigDecimal ssAnnualBenefit,
            BigDecimal combinedIncome,
            BigDecimal lowerThreshold,
            BigDecimal upperThreshold) {

        BigDecimal fiftyPercentOfBenefits = ssAnnualBenefit.multiply(HALF);
        BigDecimal eightyFivePercentOfBenefits = ssAnnualBenefit.multiply(EIGHTY_FIVE_PERCENT);

        // If between thresholds (50% tier)
        if (combinedIncome.compareTo(upperThreshold) <= 0) {
            // Taxable = lesser of:
            //   (a) 50% of (combined income - lower threshold)
            //   (b) 50% of SS benefits
            BigDecimal excessOverLower = combinedIncome.subtract(lowerThreshold);
            BigDecimal fiftyPercentOfExcess = excessOverLower.multiply(HALF);

            return fiftyPercentOfExcess.min(fiftyPercentOfBenefits)
                .setScale(SCALE, RoundingMode.HALF_UP);
        }

        // Above upper threshold (85% tier)
        // Taxable = lesser of:
        //   (a) 85% of SS benefits
        //   (b) Sum of:
        //       - 50% of (upper threshold - lower threshold), capped at 50% of benefits
        //       - 85% of (combined income - upper threshold)
        BigDecimal midRangeAmount = upperThreshold.subtract(lowerThreshold);
        BigDecimal fiftyPercentOfMidRange = midRangeAmount.multiply(HALF);
        BigDecimal baseTaxable = fiftyPercentOfMidRange.min(fiftyPercentOfBenefits);

        BigDecimal excessOverUpper = combinedIncome.subtract(upperThreshold);
        BigDecimal eightyFivePercentOfExcess = excessOverUpper.multiply(EIGHTY_FIVE_PERCENT);

        BigDecimal totalTaxable = baseTaxable.add(eightyFivePercentOfExcess);

        return totalTaxable.min(eightyFivePercentOfBenefits)
            .setScale(SCALE, RoundingMode.HALF_UP);
    }
}
