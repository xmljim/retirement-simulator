package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A single tier in a tiered employer matching policy.
 *
 * <p>Each tier defines a contribution threshold and the match rate that applies
 * up to that threshold. Tiers are cumulative - a tiered policy applies each tier
 * in sequence until the employee contribution is exhausted.
 *
 * <p>Example: 100% match on first 3%, 50% match on next 2%
 * <pre>
 * Tier 1: threshold=0.03, matchRate=1.0  (100% match on contributions up to 3%)
 * Tier 2: threshold=0.05, matchRate=0.5  (50% match on contributions from 3% to 5%)
 * </pre>
 *
 * <p>For an employee contributing 6%:
 * <ul>
 *   <li>Tier 1: 3% * 100% = 3% employer match</li>
 *   <li>Tier 2: 2% * 50% = 1% employer match</li>
 *   <li>Total employer match: 4%</li>
 * </ul>
 *
 * @param contributionThreshold the cumulative contribution percentage threshold
 *                              (e.g., 0.03 for 3%)
 * @param matchRate the match rate for this tier (e.g., 1.0 for 100% match)
 */
public record MatchTier(
    BigDecimal contributionThreshold,
    BigDecimal matchRate
) {
    /**
     * Creates a new MatchTier with validation.
     *
     * @param contributionThreshold the contribution threshold
     * @param matchRate the match rate
     */
    public MatchTier {
        Objects.requireNonNull(contributionThreshold, "Contribution threshold cannot be null");
        Objects.requireNonNull(matchRate, "Match rate cannot be null");

        if (contributionThreshold.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                "Contribution threshold cannot be negative: " + contributionThreshold);
        }
        if (matchRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Match rate cannot be negative: " + matchRate);
        }
    }

    /**
     * Creates a MatchTier from double values for convenience.
     *
     * @param threshold the contribution threshold as a decimal
     * @param rate the match rate as a decimal
     * @return a new MatchTier
     */
    public static MatchTier of(double threshold, double rate) {
        return new MatchTier(BigDecimal.valueOf(threshold), BigDecimal.valueOf(rate));
    }
}
