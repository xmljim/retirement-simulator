package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.util.List;

/**
 * Defines an employer matching contribution policy.
 *
 * <p>Different employers offer different matching structures:
 * <ul>
 *   <li><strong>Simple match</strong>: A fixed percentage match up to a cap
 *       (e.g., 50% match up to 6% of salary)</li>
 *   <li><strong>Tiered match</strong>: Different match rates at different levels
 *       (e.g., 100% on first 3%, 50% on next 2%)</li>
 *   <li><strong>No match</strong>: Employer does not match contributions</li>
 * </ul>
 *
 * <p>Use the static factory methods to create policies:
 * <pre>
 * // 50% match up to 6%
 * MatchingPolicy simple = MatchingPolicy.simple(0.50, 0.06);
 *
 * // 100% on first 3%, 50% on next 2%
 * MatchingPolicy tiered = MatchingPolicy.tiered(List.of(
 *     MatchTier.of(0.03, 1.0),
 *     MatchTier.of(0.05, 0.5)
 * ));
 *
 * // No employer match
 * MatchingPolicy none = MatchingPolicy.none();
 * </pre>
 */
public interface MatchingPolicy {

    /**
     * Calculates the employer match for a given employee contribution rate.
     *
     * @param employeeContributionRate the employee's contribution as a decimal
     *                                  (e.g., 0.06 for 6%)
     * @return the employer match as a decimal (e.g., 0.03 for 3%)
     */
    BigDecimal calculateEmployerMatch(BigDecimal employeeContributionRate);

    /**
     * Indicates whether this policy allows ROTH matching.
     *
     * <p>Under SECURE 2.0, employers can now offer ROTH matching contributions.
     * However, not all employers have implemented this feature.
     *
     * @return true if the employer supports ROTH matching
     */
    boolean allowsRothMatch();

    /**
     * Returns a description of the matching policy.
     *
     * @return a human-readable description
     */
    String getDescription();

    /**
     * Creates a simple matching policy with a fixed match rate up to a cap.
     *
     * <p>Example: 50% match up to 6% of salary
     * <ul>
     *   <li>Employee contributes 4% → Employer matches 2%</li>
     *   <li>Employee contributes 6% → Employer matches 3%</li>
     *   <li>Employee contributes 10% → Employer matches 3% (capped at 6%)</li>
     * </ul>
     *
     * @param matchRate the match rate as a decimal (e.g., 0.50 for 50%)
     * @param maxMatchPercent the maximum employee contribution that will be matched
     * @return a simple matching policy
     */
    static MatchingPolicy simple(BigDecimal matchRate, BigDecimal maxMatchPercent) {
        return new SimpleMatchingPolicy(matchRate, maxMatchPercent, false);
    }

    /**
     * Creates a simple matching policy with a fixed match rate up to a cap.
     *
     * @param matchRate the match rate as a decimal
     * @param maxMatchPercent the maximum contribution that will be matched
     * @return a simple matching policy
     */
    static MatchingPolicy simple(double matchRate, double maxMatchPercent) {
        return simple(BigDecimal.valueOf(matchRate), BigDecimal.valueOf(maxMatchPercent));
    }

    /**
     * Creates a tiered matching policy with different rates at different levels.
     *
     * <p>Tiers must be ordered by threshold (lowest to highest).
     *
     * @param tiers the list of match tiers
     * @return a tiered matching policy
     */
    static MatchingPolicy tiered(List<MatchTier> tiers) {
        return new TieredMatchingPolicy(tiers, false);
    }

    /**
     * Creates a policy with no employer matching.
     *
     * @return a no-match policy
     */
    static MatchingPolicy none() {
        return NoMatchingPolicy.INSTANCE;
    }
}
