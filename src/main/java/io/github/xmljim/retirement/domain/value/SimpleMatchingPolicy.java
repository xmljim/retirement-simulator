package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * A simple employer matching policy with a fixed match rate up to a cap.
 *
 * <p>This is the most common type of employer matching policy. The employer
 * matches a fixed percentage of employee contributions up to a specified
 * percentage of salary.
 *
 * <p>Example: 50% match up to 6% of salary
 * <ul>
 *   <li>Employee contributes 4% → Employer matches 2%</li>
 *   <li>Employee contributes 6% → Employer matches 3%</li>
 *   <li>Employee contributes 10% → Employer matches 3% (capped at 6%)</li>
 * </ul>
 */
public final class SimpleMatchingPolicy implements MatchingPolicy {

    private static final int SCALE = 6;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private final BigDecimal matchRate;
    private final BigDecimal maxMatchPercent;
    private final boolean allowsRothMatch;

    /**
     * Creates a simple matching policy.
     *
     * @param matchRate the match rate as a decimal (e.g., 0.50 for 50%)
     * @param maxMatchPercent the maximum employee contribution that will be matched
     * @param allowsRothMatch whether ROTH matching is allowed
     */
    public SimpleMatchingPolicy(BigDecimal matchRate, BigDecimal maxMatchPercent,
                                boolean allowsRothMatch) {
        Objects.requireNonNull(matchRate, "Match rate cannot be null");
        Objects.requireNonNull(maxMatchPercent, "Max match percent cannot be null");

        if (matchRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Match rate cannot be negative: " + matchRate);
        }
        if (maxMatchPercent.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                "Max match percent cannot be negative: " + maxMatchPercent);
        }

        this.matchRate = matchRate;
        this.maxMatchPercent = maxMatchPercent;
        this.allowsRothMatch = allowsRothMatch;
    }

    @Override
    public BigDecimal calculateEmployerMatch(BigDecimal employeeContributionRate) {
        if (employeeContributionRate == null
                || employeeContributionRate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Cap the employee contribution at the maximum matchable amount
        BigDecimal matchableContribution = employeeContributionRate.min(maxMatchPercent);

        // Apply the match rate
        return matchableContribution.multiply(matchRate).setScale(SCALE, ROUNDING_MODE);
    }

    @Override
    public boolean allowsRothMatch() {
        return allowsRothMatch;
    }

    @Override
    public String getDescription() {
        return String.format("%.0f%% match up to %.0f%% of salary",
            matchRate.multiply(BigDecimal.valueOf(100)),
            maxMatchPercent.multiply(BigDecimal.valueOf(100)));
    }

    /**
     * Returns the match rate.
     *
     * @return the match rate as a decimal
     */
    public BigDecimal getMatchRate() {
        return matchRate;
    }

    /**
     * Returns the maximum employee contribution that will be matched.
     *
     * @return the max match percent as a decimal
     */
    public BigDecimal getMaxMatchPercent() {
        return maxMatchPercent;
    }

    @Override
    public String toString() {
        return "SimpleMatchingPolicy{" +
            "matchRate=" + matchRate +
            ", maxMatchPercent=" + maxMatchPercent +
            ", allowsRothMatch=" + allowsRothMatch +
            '}';
    }
}
