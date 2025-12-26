package io.github.xmljim.retirement.domain.config;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.xmljim.retirement.domain.enums.FilingStatus;

/**
 * IRS IRA phase-out limits loaded from external YAML configuration.
 *
 * <p>Phase-outs determine when Traditional IRA deductions are reduced
 * and when Roth IRA contributions are limited based on MAGI.
 */
@ConfigurationProperties(prefix = "irs.phase-out")
@Validated
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP",
    justification = "Spring @ConfigurationProperties requires mutable access for binding"
)
public class IraPhaseOutLimits {

    private static final BigDecimal INCOME_INCREMENT = new BigDecimal("1000");
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private BigDecimal defaultAnnualIncreaseRate = new BigDecimal("0.02");
    private Map<Integer, YearPhaseOuts> rothIra = new HashMap<>();
    private Map<Integer, YearPhaseOuts> traditionalIraCovered = new HashMap<>();
    private Map<Integer, YearPhaseOuts> traditionalIraSpouseCovered = new HashMap<>();

    /**
     * Phase-out thresholds for a specific year by filing status.
     *
     * @param single thresholds for single/HOH filers
     * @param marriedFilingJointly thresholds for MFJ/QSS filers
     * @param marriedFilingSeparately thresholds for MFS filers
     */
    public record YearPhaseOuts(
        PhaseOutRange single,
        PhaseOutRange marriedFilingJointly,
        PhaseOutRange marriedFilingSeparately
    ) {
        /**
         * Creates YearPhaseOuts with defaults for null values.
         */
        public YearPhaseOuts {
            if (single == null) {
                single = PhaseOutRange.ZERO;
            }
            if (marriedFilingJointly == null) {
                marriedFilingJointly = PhaseOutRange.ZERO;
            }
            if (marriedFilingSeparately == null) {
                marriedFilingSeparately = PhaseOutRange.ZERO;
            }
        }

        /**
         * Returns the phase-out range for the given filing status.
         *
         * @param status the filing status
         * @return the applicable phase-out range
         */
        public PhaseOutRange forStatus(FilingStatus status) {
            return switch (status) {
                case SINGLE, HEAD_OF_HOUSEHOLD -> single;
                case MARRIED_FILING_JOINTLY, QUALIFYING_SURVIVING_SPOUSE -> marriedFilingJointly;
                case MARRIED_FILING_SEPARATELY -> marriedFilingSeparately;
            };
        }
    }

    /**
     * MAGI range where phase-out occurs.
     *
     * @param lowerBound MAGI below this = full contribution allowed
     * @param upperBound MAGI above this = no contribution allowed
     */
    public record PhaseOutRange(BigDecimal lowerBound, BigDecimal upperBound) {
        /** Zero range indicating no phase-out applies. */
        public static final PhaseOutRange ZERO = new PhaseOutRange(BigDecimal.ZERO, BigDecimal.ZERO);

        /**
         * Creates PhaseOutRange with defaults for null values.
         */
        public PhaseOutRange {
            if (lowerBound == null) {
                lowerBound = BigDecimal.ZERO;
            }
            if (upperBound == null) {
                upperBound = BigDecimal.ZERO;
            }
        }

        /**
         * Returns the size of the phase-out range.
         *
         * @return upper minus lower bound
         */
        public BigDecimal getRange() {
            return upperBound.subtract(lowerBound);
        }
    }

    /**
     * Returns Roth IRA phase-outs for the specified year.
     *
     * @param year the tax year
     * @return phase-out thresholds by filing status
     */
    public YearPhaseOuts getRothIraPhaseOutsForYear(int year) {
        return getPhaseOutsForYear(rothIra, year);
    }

    /**
     * Returns Traditional IRA deductibility phase-outs when covered by employer plan.
     *
     * @param year the tax year
     * @return phase-out thresholds by filing status
     */
    public YearPhaseOuts getTraditionalIraCoveredPhaseOutsForYear(int year) {
        return getPhaseOutsForYear(traditionalIraCovered, year);
    }

    /**
     * Returns Traditional IRA deductibility phase-outs when spouse is covered.
     *
     * @param year the tax year
     * @return phase-out thresholds by filing status
     */
    public YearPhaseOuts getTraditionalIraSpouseCoveredPhaseOutsForYear(int year) {
        return getPhaseOutsForYear(traditionalIraSpouseCovered, year);
    }

    private YearPhaseOuts getPhaseOutsForYear(Map<Integer, YearPhaseOuts> map, int year) {
        if (map.containsKey(year)) {
            return map.get(year);
        }
        NavigableMap<Integer, YearPhaseOuts> sorted = new TreeMap<>(map);
        if (sorted.isEmpty()) {
            return new YearPhaseOuts(PhaseOutRange.ZERO, PhaseOutRange.ZERO, PhaseOutRange.ZERO);
        }
        var latest = sorted.lastEntry();
        if (year <= latest.getKey()) {
            var floor = sorted.floorEntry(year);
            return floor != null ? floor.getValue() : latest.getValue();
        }
        return extrapolate(latest.getValue(), year - latest.getKey());
    }

    private YearPhaseOuts extrapolate(YearPhaseOuts base, int years) {
        BigDecimal mult = BigDecimal.ONE.add(defaultAnnualIncreaseRate).pow(years);
        return new YearPhaseOuts(
            extrapolateRange(base.single(), mult),
            extrapolateRange(base.marriedFilingJointly(), mult),
            base.marriedFilingSeparately() // MFS thresholds typically don't change
        );
    }

    private PhaseOutRange extrapolateRange(PhaseOutRange range, BigDecimal multiplier) {
        return new PhaseOutRange(
            roundToIncrement(range.lowerBound().multiply(multiplier)),
            roundToIncrement(range.upperBound().multiply(multiplier))
        );
    }

    private BigDecimal roundToIncrement(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return value.divide(INCOME_INCREMENT, 0, ROUNDING_MODE).multiply(INCOME_INCREMENT);
    }

    /**
     * Returns the Roth IRA phase-out map.
     *
     * @return map of year to phase-outs
     */
    public Map<Integer, YearPhaseOuts> getRothIra() {
        return rothIra;
    }

    /**
     * Sets the Roth IRA phase-out map.
     *
     * @param rothIra the map
     */
    public void setRothIra(Map<Integer, YearPhaseOuts> rothIra) {
        this.rothIra = rothIra;
    }

    /**
     * Returns the Traditional IRA (covered) phase-out map.
     *
     * @return map of year to phase-outs
     */
    public Map<Integer, YearPhaseOuts> getTraditionalIraCovered() {
        return traditionalIraCovered;
    }

    /**
     * Sets the Traditional IRA (covered) phase-out map.
     *
     * @param traditionalIraCovered the map
     */
    public void setTraditionalIraCovered(Map<Integer, YearPhaseOuts> traditionalIraCovered) {
        this.traditionalIraCovered = traditionalIraCovered;
    }

    /**
     * Returns the Traditional IRA (spouse covered) phase-out map.
     *
     * @return map of year to phase-outs
     */
    public Map<Integer, YearPhaseOuts> getTraditionalIraSpouseCovered() {
        return traditionalIraSpouseCovered;
    }

    /**
     * Sets the Traditional IRA (spouse covered) phase-out map.
     *
     * @param traditionalIraSpouseCovered the map
     */
    public void setTraditionalIraSpouseCovered(Map<Integer, YearPhaseOuts> traditionalIraSpouseCovered) {
        this.traditionalIraSpouseCovered = traditionalIraSpouseCovered;
    }

    /**
     * Returns the default annual increase rate for extrapolation.
     *
     * @return the rate as a decimal
     */
    public BigDecimal getDefaultAnnualIncreaseRate() {
        return defaultAnnualIncreaseRate;
    }

    /**
     * Sets the default annual increase rate.
     *
     * @param rate the rate as a decimal
     */
    public void setDefaultAnnualIncreaseRate(BigDecimal rate) {
        this.defaultAnnualIncreaseRate = rate;
    }
}
