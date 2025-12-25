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

/**
 * IRS contribution limits loaded from external YAML configuration.
 *
 * <p>This class provides access to IRS retirement contribution limits
 * including base limits, catch-up contributions, and ROTH allocation thresholds.
 * Limits are organized by year and can be extrapolated for future years.
 *
 * <p>Configuration is loaded from {@code application.yml} under the
 * {@code irs.contribution} prefix.
 *
 * <p>Example configuration:
 * <pre>
 * irs:
 *   contribution:
 *     default-annual-increase-rate: 0.02
 *     limits:
 *       2025:
 *         base-limit: 23500
 *         catch-up-limit: 7500
 *         super-catch-up-limit: 11250
 *         roth-catch-up-income-threshold: 145000
 * </pre>
 */
@ConfigurationProperties(prefix = "irs.contribution")
@Validated
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP",
    justification = "Spring @ConfigurationProperties requires mutable access for binding"
)
public class IrsContributionLimits {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private Map<Integer, YearLimits> limits = new HashMap<>();
    private BigDecimal defaultAnnualIncreaseRate = new BigDecimal("0.02");
    private Map<Integer, IraLimits> iraLimits = new HashMap<>();

    /**
     * Contribution limits for a specific year.
     *
     * @param baseLimit the base contribution limit (e.g., $23,500)
     * @param catchUpLimit the standard catch-up limit for age 50+ (e.g., $7,500)
     * @param superCatchUpLimit the super catch-up limit for age 60-63 (e.g., $11,250)
     * @param rothCatchUpIncomeThreshold income threshold requiring ROTH catch-up (e.g., $145,000)
     */
    public record YearLimits(
        BigDecimal baseLimit,
        BigDecimal catchUpLimit,
        BigDecimal superCatchUpLimit,
        BigDecimal rothCatchUpIncomeThreshold
    ) {
        /**
         * Creates YearLimits with default values for missing fields.
         */
        public YearLimits {
            if (baseLimit == null) {
                baseLimit = BigDecimal.ZERO;
            }
            if (catchUpLimit == null) {
                catchUpLimit = BigDecimal.ZERO;
            }
            if (superCatchUpLimit == null) {
                superCatchUpLimit = BigDecimal.ZERO;
            }
            if (rothCatchUpIncomeThreshold == null) {
                rothCatchUpIncomeThreshold = BigDecimal.ZERO;
            }
        }
    }

    /**
     * IRA contribution limits for a specific year.
     *
     * @param baseLimit the base IRA contribution limit
     * @param catchUpLimit the catch-up limit for age 50+
     */
    public record IraLimits(
        BigDecimal baseLimit,
        BigDecimal catchUpLimit
    ) {
        /**
         * Creates IraLimits with default values for missing fields.
         */
        public IraLimits {
            if (baseLimit == null) {
                baseLimit = BigDecimal.ZERO;
            }
            if (catchUpLimit == null) {
                catchUpLimit = BigDecimal.ZERO;
            }
        }
    }

    /**
     * Returns the limits for a specific year.
     *
     * <p>If the year is not in the configuration, extrapolates from the
     * most recent known year using the default annual increase rate.
     *
     * @param year the contribution year
     * @return the limits for that year
     */
    public YearLimits getLimitsForYear(int year) {
        if (limits.containsKey(year)) {
            return limits.get(year);
        }

        // Find the most recent year and extrapolate
        NavigableMap<Integer, YearLimits> sortedLimits = new TreeMap<>(limits);
        if (sortedLimits.isEmpty()) {
            return new YearLimits(BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO);
        }

        Map.Entry<Integer, YearLimits> latestEntry = sortedLimits.lastEntry();
        int latestYear = latestEntry.getKey();
        YearLimits latestLimits = latestEntry.getValue();

        if (year < latestYear) {
            // For past years not in config, return the earliest known
            Map.Entry<Integer, YearLimits> earliestEntry = sortedLimits.firstEntry();
            return earliestEntry.getValue();
        }

        // Extrapolate forward
        int yearsAhead = year - latestYear;
        BigDecimal multiplier = BigDecimal.ONE.add(defaultAnnualIncreaseRate)
            .pow(yearsAhead);

        return new YearLimits(
            latestLimits.baseLimit().multiply(multiplier).setScale(SCALE, ROUNDING_MODE),
            latestLimits.catchUpLimit().multiply(multiplier).setScale(SCALE, ROUNDING_MODE),
            latestLimits.superCatchUpLimit().multiply(multiplier).setScale(SCALE, ROUNDING_MODE),
            latestLimits.rothCatchUpIncomeThreshold().multiply(multiplier)
                .setScale(SCALE, ROUNDING_MODE)
        );
    }

    /**
     * Returns the IRA limits for a specific year.
     *
     * @param year the contribution year
     * @return the IRA limits for that year
     */
    public IraLimits getIraLimitsForYear(int year) {
        if (iraLimits.containsKey(year)) {
            return iraLimits.get(year);
        }

        // Find the most recent year
        NavigableMap<Integer, IraLimits> sortedLimits = new TreeMap<>(iraLimits);
        if (sortedLimits.isEmpty()) {
            return new IraLimits(BigDecimal.ZERO, BigDecimal.ZERO);
        }

        Map.Entry<Integer, IraLimits> latestEntry = sortedLimits.lastEntry();
        int latestYear = latestEntry.getKey();
        IraLimits latestLimits = latestEntry.getValue();

        if (year <= latestYear) {
            Map.Entry<Integer, IraLimits> floorEntry = sortedLimits.floorEntry(year);
            return floorEntry != null ? floorEntry.getValue() : latestLimits;
        }

        // Extrapolate forward
        int yearsAhead = year - latestYear;
        BigDecimal multiplier = BigDecimal.ONE.add(defaultAnnualIncreaseRate)
            .pow(yearsAhead);

        return new IraLimits(
            latestLimits.baseLimit().multiply(multiplier).setScale(SCALE, ROUNDING_MODE),
            latestLimits.catchUpLimit().multiply(multiplier).setScale(SCALE, ROUNDING_MODE)
        );
    }

    /**
     * Returns the configured limits map.
     *
     * @return map of year to limits
     */
    public Map<Integer, YearLimits> getLimits() {
        return limits;
    }

    /**
     * Sets the limits map (used by Spring for configuration binding).
     *
     * @param limits the limits map
     */
    public void setLimits(Map<Integer, YearLimits> limits) {
        this.limits = limits;
    }

    /**
     * Returns the default annual increase rate for extrapolation.
     *
     * @return the increase rate as a decimal
     */
    public BigDecimal getDefaultAnnualIncreaseRate() {
        return defaultAnnualIncreaseRate;
    }

    /**
     * Sets the default annual increase rate.
     *
     * @param rate the increase rate as a decimal
     */
    public void setDefaultAnnualIncreaseRate(BigDecimal rate) {
        this.defaultAnnualIncreaseRate = rate;
    }

    /**
     * Returns the IRA limits map.
     *
     * @return map of year to IRA limits
     */
    public Map<Integer, IraLimits> getIraLimits() {
        return iraLimits;
    }

    /**
     * Sets the IRA limits map.
     *
     * @param iraLimits the IRA limits map
     */
    public void setIraLimits(Map<Integer, IraLimits> iraLimits) {
        this.iraLimits = iraLimits;
    }
}
