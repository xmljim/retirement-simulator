package io.github.xmljim.retirement.domain.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Social Security rules loaded from external YAML configuration.
 *
 * <p>This class provides configurable Social Security rules including:
 * <ul>
 *   <li>Full Retirement Age (FRA) table by birth year</li>
 *   <li>Early claiming reduction rates</li>
 *   <li>Delayed retirement credit rates</li>
 *   <li>Claiming age limits</li>
 * </ul>
 *
 * <p>These values are sourced from the Social Security Administration:
 * <ul>
 *   <li>FRA by birth year: <a href="https://www.ssa.gov/oact/ProgData/nra.html">SSA Normal Retirement Age</a></li>
 *   <li>Early/Delayed adjustments: <a href="https://www.ssa.gov/oact/ProgData/ar_drc.html">SSA Adjustment Rates</a></li>
 * </ul>
 *
 * <p>Configuration is loaded from {@code application.yml} under the
 * {@code social-security} prefix.
 *
 * <p>Example configuration:
 * <pre>
 * social-security:
 *   fra-table:
 *     - birth-year-start: 1943
 *       birth-year-end: 1954
 *       fra-months: 792
 *     - birth-year-start: 1960
 *       fra-months: 804
 *   claiming:
 *     minimum-age-months: 744
 *     maximum-age-months: 840
 *   early-reduction:
 *     first-tier-months: 36
 *     first-tier-rate-numerator: 5
 *     first-tier-rate-denominator: 900
 *     second-tier-rate-numerator: 5
 *     second-tier-rate-denominator: 1200
 *   delayed-credits:
 *     rate-numerator: 8
 *     rate-denominator: 1200
 * </pre>
 */
@ConfigurationProperties(prefix = "social-security")
@Validated
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP",
    justification = "Spring @ConfigurationProperties requires mutable access for binding"
)
public class SocialSecurityRules {

    private List<FraEntry> fraTable = new ArrayList<>();
    private ClaimingLimits claiming = new ClaimingLimits();
    private EarlyReduction earlyReduction = new EarlyReduction();
    private DelayedCredits delayedCredits = new DelayedCredits();
    private SpousalRules spousal = new SpousalRules();
    private SurvivorRules survivor = new SurvivorRules();

    /**
     * Full Retirement Age entry for a range of birth years.
     *
     * @param birthYearStart the first birth year in the range
     * @param birthYearEnd the last birth year in the range (null for "and later")
     * @param fraMonths the FRA in months for this range
     */
    public record FraEntry(
        Integer birthYearStart,
        Integer birthYearEnd,
        int fraMonths
    ) { }

    /**
     * Claiming age limits.
     */
    public static class ClaimingLimits {
        private int minimumAgeMonths = 744;  // 62 years
        private int maximumAgeMonths = 840;  // 70 years

        public int getMinimumAgeMonths() {
            return minimumAgeMonths;
        }

        public void setMinimumAgeMonths(int minimumAgeMonths) {
            this.minimumAgeMonths = minimumAgeMonths;
        }

        public int getMaximumAgeMonths() {
            return maximumAgeMonths;
        }

        public void setMaximumAgeMonths(int maximumAgeMonths) {
            this.maximumAgeMonths = maximumAgeMonths;
        }
    }

    /**
     * Early claiming reduction rates.
     *
     * <p>Per SSA rules:
     * <ul>
     *   <li>First 36 months: 5/9 of 1% per month (5/900)</li>
     *   <li>Additional months: 5/12 of 1% per month (5/1200)</li>
     * </ul>
     */
    public static class EarlyReduction {
        private int firstTierMonths = 36;
        private int firstTierRateNumerator = 5;
        private int firstTierRateDenominator = 900;
        private int secondTierRateNumerator = 5;
        private int secondTierRateDenominator = 1200;

        public int getFirstTierMonths() {
            return firstTierMonths;
        }

        public void setFirstTierMonths(int firstTierMonths) {
            this.firstTierMonths = firstTierMonths;
        }

        public int getFirstTierRateNumerator() {
            return firstTierRateNumerator;
        }

        public void setFirstTierRateNumerator(int firstTierRateNumerator) {
            this.firstTierRateNumerator = firstTierRateNumerator;
        }

        public int getFirstTierRateDenominator() {
            return firstTierRateDenominator;
        }

        public void setFirstTierRateDenominator(int firstTierRateDenominator) {
            this.firstTierRateDenominator = firstTierRateDenominator;
        }

        public int getSecondTierRateNumerator() {
            return secondTierRateNumerator;
        }

        public void setSecondTierRateNumerator(int secondTierRateNumerator) {
            this.secondTierRateNumerator = secondTierRateNumerator;
        }

        public int getSecondTierRateDenominator() {
            return secondTierRateDenominator;
        }

        public void setSecondTierRateDenominator(int secondTierRateDenominator) {
            this.secondTierRateDenominator = secondTierRateDenominator;
        }

        /**
         * Calculates the first tier rate as a BigDecimal.
         *
         * @param scale the scale for the result
         * @return the rate as numerator/denominator
         */
        public BigDecimal getFirstTierRate(int scale) {
            return BigDecimal.valueOf(firstTierRateNumerator)
                .divide(BigDecimal.valueOf(firstTierRateDenominator), scale,
                    java.math.RoundingMode.HALF_UP);
        }

        /**
         * Calculates the second tier rate as a BigDecimal.
         *
         * @param scale the scale for the result
         * @return the rate as numerator/denominator
         */
        public BigDecimal getSecondTierRate(int scale) {
            return BigDecimal.valueOf(secondTierRateNumerator)
                .divide(BigDecimal.valueOf(secondTierRateDenominator), scale,
                    java.math.RoundingMode.HALF_UP);
        }
    }

    /**
     * Delayed retirement credit rates.
     *
     * <p>Per SSA rules for those born 1943 or later:
     * 8% per year = 8/1200 per month
     */
    public static class DelayedCredits {
        private int rateNumerator = 8;
        private int rateDenominator = 1200;

        public int getRateNumerator() {
            return rateNumerator;
        }

        public void setRateNumerator(int rateNumerator) {
            this.rateNumerator = rateNumerator;
        }

        public int getRateDenominator() {
            return rateDenominator;
        }

        public void setRateDenominator(int rateDenominator) {
            this.rateDenominator = rateDenominator;
        }

        /**
         * Calculates the delayed credit rate as a BigDecimal.
         *
         * @param scale the scale for the result
         * @return the rate as numerator/denominator
         */
        public BigDecimal getRate(int scale) {
            return BigDecimal.valueOf(rateNumerator)
                .divide(BigDecimal.valueOf(rateDenominator), scale,
                    java.math.RoundingMode.HALF_UP);
        }
    }

    /**
     * Spousal benefit rules.
     *
     * <p>Per SSA rules:
     * <ul>
     *   <li>Spousal benefit is up to 50% of higher earner's FRA benefit</li>
     *   <li>Must be married at least 1 year</li>
     *   <li>Divorced spouse: married 10+ years, divorced 2+ years</li>
     * </ul>
     *
     * @see <a href="https://www.ssa.gov/benefits/retirement/planner/applying7.html">SSA Spousal</a>
     */
    public static class SpousalRules {
        private BigDecimal benefitPercentage = new BigDecimal("0.50");
        private int minimumMarriageMonths = 12;
        private int divorcedMinimumMarriageYears = 10;
        private int divorcedMinimumDivorceYears = 2;

        public BigDecimal getBenefitPercentage() {
            return benefitPercentage;
        }

        public void setBenefitPercentage(BigDecimal benefitPercentage) {
            this.benefitPercentage = benefitPercentage;
        }

        public int getMinimumMarriageMonths() {
            return minimumMarriageMonths;
        }

        public void setMinimumMarriageMonths(int minimumMarriageMonths) {
            this.minimumMarriageMonths = minimumMarriageMonths;
        }

        public int getDivorcedMinimumMarriageYears() {
            return divorcedMinimumMarriageYears;
        }

        public void setDivorcedMinimumMarriageYears(int divorcedMinimumMarriageYears) {
            this.divorcedMinimumMarriageYears = divorcedMinimumMarriageYears;
        }

        public int getDivorcedMinimumDivorceYears() {
            return divorcedMinimumDivorceYears;
        }

        public void setDivorcedMinimumDivorceYears(int divorcedMinimumDivorceYears) {
            this.divorcedMinimumDivorceYears = divorcedMinimumDivorceYears;
        }

        /**
         * Returns the minimum marriage duration in months for divorced spouse benefits.
         *
         * @return the minimum months (divorcedMinimumMarriageYears * 12)
         */
        public int getDivorcedMinimumMarriageMonths() {
            return divorcedMinimumMarriageYears * 12;
        }
    }

    /**
     * Survivor benefit rules.
     *
     * <p>Per SSA rules:
     * <ul>
     *   <li>Survivor receives higher of own or deceased's benefit</li>
     *   <li>Can claim as early as age 60 (reduced)</li>
     *   <li>Must be married at least 9 months</li>
     *   <li>Can remarry after 60 without losing benefits</li>
     * </ul>
     *
     * @see <a href="https://www.ssa.gov/benefits/survivors/">SSA Survivor Benefits</a>
     */
    public static class SurvivorRules {
        private int minimumMarriageMonths = 9;
        private int minimumClaimingAgeMonths = 720;   // 60 years
        private int disabledMinimumAgeMonths = 600;   // 50 years
        private int remarriageAgeMonths = 720;        // Can remarry at 60
        // Survivor reduction: ~28.5% max at age 60 (71.5% of benefit)
        // Different from regular early claiming formula
        private int reductionRateNumerator = 285;
        private int reductionRateDenominator = 10000;

        public int getMinimumMarriageMonths() {
            return minimumMarriageMonths;
        }

        public void setMinimumMarriageMonths(int minimumMarriageMonths) {
            this.minimumMarriageMonths = minimumMarriageMonths;
        }

        public int getMinimumClaimingAgeMonths() {
            return minimumClaimingAgeMonths;
        }

        public void setMinimumClaimingAgeMonths(int minimumClaimingAgeMonths) {
            this.minimumClaimingAgeMonths = minimumClaimingAgeMonths;
        }

        public int getDisabledMinimumAgeMonths() {
            return disabledMinimumAgeMonths;
        }

        public void setDisabledMinimumAgeMonths(int disabledMinimumAgeMonths) {
            this.disabledMinimumAgeMonths = disabledMinimumAgeMonths;
        }

        public int getRemarriageAgeMonths() {
            return remarriageAgeMonths;
        }

        public void setRemarriageAgeMonths(int remarriageAgeMonths) {
            this.remarriageAgeMonths = remarriageAgeMonths;
        }

        public int getReductionRateNumerator() {
            return reductionRateNumerator;
        }

        public void setReductionRateNumerator(int reductionRateNumerator) {
            this.reductionRateNumerator = reductionRateNumerator;
        }

        public int getReductionRateDenominator() {
            return reductionRateDenominator;
        }

        public void setReductionRateDenominator(int reductionRateDenominator) {
            this.reductionRateDenominator = reductionRateDenominator;
        }

        /**
         * Calculates the maximum survivor reduction as a BigDecimal.
         *
         * @param scale the scale for the result
         * @return the max reduction (e.g., 0.285 for 28.5%)
         */
        public BigDecimal getMaxReduction(int scale) {
            return BigDecimal.valueOf(reductionRateNumerator)
                .divide(BigDecimal.valueOf(reductionRateDenominator), scale,
                    java.math.RoundingMode.HALF_UP);
        }
    }

    /**
     * Returns the FRA in months for a given birth year.
     *
     * @param birthYear the birth year
     * @return the FRA in months, or 804 (67 years) if not found
     */
    public int getFraMonthsForBirthYear(int birthYear) {
        return fraTable.stream()
            .filter(entry -> isWithinRange(entry, birthYear))
            .findFirst()
            .map(FraEntry::fraMonths)
            .orElse(804);  // Default to 67 years (804 months) if not found
    }

    /**
     * Checks if a birth year falls within an FRA entry's range.
     *
     * @param entry the FRA entry to check
     * @param birthYear the birth year to test
     * @return true if birth year is within the entry's range
     */
    private boolean isWithinRange(FraEntry entry, int birthYear) {
        // Check if birth year is before range start (if start is defined)
        if (entry.birthYearStart() != null && birthYear < entry.birthYearStart()) {
            return false;
        }
        // Check if birth year is after range end (if end is defined)
        if (entry.birthYearEnd() != null && birthYear > entry.birthYearEnd()) {
            return false;
        }
        // Birth year is within this range (or range is open-ended)
        return true;
    }

    public List<FraEntry> getFraTable() {
        return fraTable;
    }

    public void setFraTable(List<FraEntry> fraTable) {
        this.fraTable = fraTable;
    }

    public ClaimingLimits getClaiming() {
        return claiming;
    }

    public void setClaiming(ClaimingLimits claiming) {
        this.claiming = claiming;
    }

    public EarlyReduction getEarlyReduction() {
        return earlyReduction;
    }

    public void setEarlyReduction(EarlyReduction earlyReduction) {
        this.earlyReduction = earlyReduction;
    }

    public DelayedCredits getDelayedCredits() {
        return delayedCredits;
    }

    public void setDelayedCredits(DelayedCredits delayedCredits) {
        this.delayedCredits = delayedCredits;
    }

    public SpousalRules getSpousal() {
        return spousal;
    }

    public void setSpousal(SpousalRules spousal) {
        this.spousal = spousal;
    }

    public SurvivorRules getSurvivor() {
        return survivor;
    }

    public void setSurvivor(SurvivorRules survivor) {
        this.survivor = survivor;
    }
}
