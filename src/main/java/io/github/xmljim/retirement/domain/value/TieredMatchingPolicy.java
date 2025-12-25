package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A tiered employer matching policy with different rates at different levels.
 *
 * <p>This policy applies match rates progressively across tiers. Each tier
 * defines a cumulative contribution threshold and the match rate that applies
 * to contributions within that tier.
 *
 * <p>Example: 100% match on first 3%, 50% match on next 2%
 * <pre>
 * Tiers:
 *   - threshold=0.03, matchRate=1.0  (100% match on 0% to 3%)
 *   - threshold=0.05, matchRate=0.5  (50% match on 3% to 5%)
 *
 * For 6% employee contribution:
 *   - Tier 1: 3% * 100% = 3%
 *   - Tier 2: 2% * 50%  = 1%
 *   - Total match: 4%
 * </pre>
 */
public final class TieredMatchingPolicy implements MatchingPolicy {

    private static final int SCALE = 6;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private final List<MatchTier> tiers;
    private final boolean allowsRothMatch;

    /**
     * Creates a tiered matching policy.
     *
     * @param tiers the list of match tiers (will be sorted by threshold)
     * @param allowsRothMatch whether ROTH matching is allowed
     */
    public TieredMatchingPolicy(List<MatchTier> tiers, boolean allowsRothMatch) {
        Objects.requireNonNull(tiers, "Tiers cannot be null");

        if (tiers.isEmpty()) {
            throw new IllegalArgumentException("At least one tier must be provided");
        }

        // Sort tiers by threshold (lowest to highest)
        this.tiers = tiers.stream()
            .sorted(Comparator.comparing(MatchTier::contributionThreshold))
            .collect(Collectors.toCollection(ArrayList::new));

        this.allowsRothMatch = allowsRothMatch;
    }

    @Override
    public BigDecimal calculateEmployerMatch(BigDecimal employeeContributionRate) {
        if (employeeContributionRate == null
                || employeeContributionRate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalMatch = BigDecimal.ZERO;
        BigDecimal previousThreshold = BigDecimal.ZERO;
        BigDecimal remainingContribution = employeeContributionRate;

        for (MatchTier tier : tiers) {
            if (remainingContribution.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            // Calculate the contribution amount in this tier
            BigDecimal tierWidth = tier.contributionThreshold().subtract(previousThreshold);
            BigDecimal contributionInTier = remainingContribution.min(tierWidth);

            // Apply the tier's match rate
            BigDecimal tierMatch = contributionInTier.multiply(tier.matchRate());
            totalMatch = totalMatch.add(tierMatch);

            // Move to next tier
            remainingContribution = remainingContribution.subtract(contributionInTier);
            previousThreshold = tier.contributionThreshold();
        }

        return totalMatch.setScale(SCALE, ROUNDING_MODE);
    }

    @Override
    public boolean allowsRothMatch() {
        return allowsRothMatch;
    }

    @Override
    public String getDescription() {
        StringBuilder sb = new StringBuilder("Tiered match: ");
        BigDecimal previousThreshold = BigDecimal.ZERO;

        for (int i = 0; i < tiers.size(); i++) {
            MatchTier tier = tiers.get(i);
            if (i > 0) {
                sb.append(", ");
            }

            BigDecimal tierStart = previousThreshold.multiply(BigDecimal.valueOf(100));
            BigDecimal tierEnd = tier.contributionThreshold().multiply(BigDecimal.valueOf(100));
            BigDecimal matchPct = tier.matchRate().multiply(BigDecimal.valueOf(100));

            sb.append(String.format("%.0f%% match on %.0f%%-%.0f%%",
                matchPct, tierStart, tierEnd));

            previousThreshold = tier.contributionThreshold();
        }

        return sb.toString();
    }

    /**
     * Returns the list of tiers.
     *
     * @return unmodifiable list of tiers
     */
    public List<MatchTier> getTiers() {
        return List.copyOf(tiers);
    }

    @Override
    public String toString() {
        return "TieredMatchingPolicy{" +
            "tiers=" + tiers +
            ", allowsRothMatch=" + allowsRothMatch +
            '}';
    }
}
