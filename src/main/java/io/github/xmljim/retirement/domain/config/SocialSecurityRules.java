package io.github.xmljim.retirement.domain.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private EarningsTestRules earningsTest = new EarningsTestRules();
    private TaxationRules taxation = new TaxationRules();

    /**
     * Full Retirement Age entry for a range of birth years.
     *
     * <p>Uses {@code Optional<Integer>} for range bounds to clearly express
     * that absent values represent open-ended ranges:
     * <ul>
     *   <li>Empty birthYearStart: "birth year X and earlier"</li>
     *   <li>Empty birthYearEnd: "birth year X and later"</li>
     * </ul>
     *
     * @param birthYearStart the first birth year in the range, or empty for open start
     * @param birthYearEnd the last birth year in the range, or empty for open end
     * @param fraMonths the FRA in months for this range
     */
    public record FraEntry(
        Optional<Integer> birthYearStart,
        Optional<Integer> birthYearEnd,
        int fraMonths
    ) {
        /**
         * Canonical constructor ensuring non-null Optionals.
         */
        public FraEntry {
            birthYearStart = birthYearStart != null ? birthYearStart : Optional.empty();
            birthYearEnd = birthYearEnd != null ? birthYearEnd : Optional.empty();
        }
    }

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
     * <p>Uses Optional's map/orElse pattern for clean null handling:
     * <ul>
     *   <li>Empty start bound: any year satisfies the "after start" check</li>
     *   <li>Empty end bound: any year satisfies the "before end" check</li>
     * </ul>
     *
     * @param entry the FRA entry to check
     * @param birthYear the birth year to test
     * @return true if birth year is within the entry's range
     */
    private boolean isWithinRange(FraEntry entry, int birthYear) {
        boolean afterStart = entry.birthYearStart()
            .map(start -> birthYear >= start)
            .orElse(true);

        boolean beforeEnd = entry.birthYearEnd()
            .map(end -> birthYear <= end)
            .orElse(true);

        return afterStart && beforeEnd;
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

    public EarningsTestRules getEarningsTest() {
        return earningsTest;
    }

    public void setEarningsTest(EarningsTestRules earningsTest) {
        this.earningsTest = earningsTest;
    }

    public TaxationRules getTaxation() {
        return taxation;
    }

    public void setTaxation(TaxationRules taxation) {
        this.taxation = taxation;
    }

    /**
     * Year-specific earnings test limits.
     *
     * <p>Limits are indexed annually by SSA based on national wage trends.
     *
     * @param year the tax year
     * @param belowFraLimit annual exempt amount for those below FRA all year
     * @param fraYearLimit annual exempt amount for year reaching FRA
     */
    public record EarningsTestLimits(
        int year,
        BigDecimal belowFraLimit,
        BigDecimal fraYearLimit
    ) { }

    /**
     * Earnings test rules for Social Security benefit reduction.
     *
     * <p>Per SSA rules, benefits are reduced when working before FRA:
     * <ul>
     *   <li>Below FRA all year: $1 reduction per $2 earned over limit</li>
     *   <li>Year reaching FRA (months before): $1 per $3 earned over higher limit</li>
     *   <li>At/after FRA: No earnings test</li>
     * </ul>
     *
     * <p>Limits are indexed annually. Withheld benefits are not lost - they're
     * added back at FRA through benefit recalculation.
     *
     * @see <a href="https://www.ssa.gov/benefits/retirement/planner/whileworking.html">SSA Earnings Test</a>
     * @see <a href="https://www.ssa.gov/oact/cola/rtea.html">SSA Earnings Test Limits</a>
     */
    public static class EarningsTestRules {
        private List<EarningsTestLimits> limits = new ArrayList<>();
        private int belowFraReductionRatio = 2;  // $1 per $2 over
        private int fraYearReductionRatio = 3;   // $1 per $3 over

        public List<EarningsTestLimits> getLimits() {
            return limits;
        }

        public void setLimits(List<EarningsTestLimits> limits) {
            this.limits = limits;
        }

        public int getBelowFraReductionRatio() {
            return belowFraReductionRatio;
        }

        public void setBelowFraReductionRatio(int belowFraReductionRatio) {
            this.belowFraReductionRatio = belowFraReductionRatio;
        }

        public int getFraYearReductionRatio() {
            return fraYearReductionRatio;
        }

        public void setFraYearReductionRatio(int fraYearReductionRatio) {
            this.fraYearReductionRatio = fraYearReductionRatio;
        }

        /**
         * Gets the below-FRA earnings limit for a specific year.
         *
         * @param year the tax year
         * @return the limit, or default of $22,320 (2024) if year not found
         */
        public BigDecimal getBelowFraLimit(int year) {
            return limits.stream()
                .filter(l -> l.year() == year)
                .findFirst()
                .map(EarningsTestLimits::belowFraLimit)
                .orElse(new BigDecimal("22320"));  // 2024 default
        }

        /**
         * Gets the FRA-year earnings limit for a specific year.
         *
         * @param year the tax year
         * @return the limit, or default of $59,520 (2024) if year not found
         */
        public BigDecimal getFraYearLimit(int year) {
            return limits.stream()
                .filter(l -> l.year() == year)
                .findFirst()
                .map(EarningsTestLimits::fraYearLimit)
                .orElse(new BigDecimal("59520"));  // 2024 default
        }
    }

    /**
     * Benefit taxation rules per IRS Publication 915.
     *
     * <p>Determines what percentage of Social Security benefits are taxable.
     * Combined income = AGI + non-taxable interest + 50% of SS benefits.
     *
     * <p><b>IMPORTANT:</b> These thresholds are NOT indexed for inflation.
     * They have remained fixed since 1984/1993, creating "bracket creep"
     * where more benefits become taxable over time as incomes rise.
     *
     * <p>Thresholds by filing status:
     * <ul>
     *   <li>Single/HOH/QSS: $25,000 (50% tier) / $34,000 (85% tier)</li>
     *   <li>MFJ: $32,000 (50% tier) / $44,000 (85% tier)</li>
     *   <li>MFS (living with spouse): $0 - always up to 85% taxable</li>
     * </ul>
     *
     * @see <a href="https://www.irs.gov/publications/p915">IRS Publication 915</a>
     */
    public static class TaxationRules {
        // Single/HOH/QSS thresholds (NOT indexed - fixed since 1984/1993)
        private BigDecimal singleLowerThreshold = new BigDecimal("25000");
        private BigDecimal singleUpperThreshold = new BigDecimal("34000");
        // MFJ thresholds
        private BigDecimal jointLowerThreshold = new BigDecimal("32000");
        private BigDecimal jointUpperThreshold = new BigDecimal("44000");
        // Taxable percentages
        private BigDecimal lowerTierPercentage = new BigDecimal("0.50");
        private BigDecimal upperTierPercentage = new BigDecimal("0.85");

        public BigDecimal getSingleLowerThreshold() {
            return singleLowerThreshold;
        }

        public void setSingleLowerThreshold(BigDecimal singleLowerThreshold) {
            this.singleLowerThreshold = singleLowerThreshold;
        }

        public BigDecimal getSingleUpperThreshold() {
            return singleUpperThreshold;
        }

        public void setSingleUpperThreshold(BigDecimal singleUpperThreshold) {
            this.singleUpperThreshold = singleUpperThreshold;
        }

        public BigDecimal getJointLowerThreshold() {
            return jointLowerThreshold;
        }

        public void setJointLowerThreshold(BigDecimal jointLowerThreshold) {
            this.jointLowerThreshold = jointLowerThreshold;
        }

        public BigDecimal getJointUpperThreshold() {
            return jointUpperThreshold;
        }

        public void setJointUpperThreshold(BigDecimal jointUpperThreshold) {
            this.jointUpperThreshold = jointUpperThreshold;
        }

        public BigDecimal getLowerTierPercentage() {
            return lowerTierPercentage;
        }

        public void setLowerTierPercentage(BigDecimal lowerTierPercentage) {
            this.lowerTierPercentage = lowerTierPercentage;
        }

        public BigDecimal getUpperTierPercentage() {
            return upperTierPercentage;
        }

        public void setUpperTierPercentage(BigDecimal upperTierPercentage) {
            this.upperTierPercentage = upperTierPercentage;
        }
    }
}
