package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.util.List;

/**
 * Optimal Social Security claiming strategy for a married couple.
 *
 * <p>This record provides recommendations for when each spouse should claim
 * their Social Security benefits to maximize total lifetime benefits. It
 * considers spousal benefits and the break-even analysis.
 *
 * <p>Common strategies include:
 * <ul>
 *   <li><strong>Higher earner delays</strong>: Maximizes survivor benefit</li>
 *   <li><strong>Lower earner claims early</strong>: Provides income bridge</li>
 *   <li><strong>Both delay to 70</strong>: Maximum benefits if both long-lived</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * CoupleClaimingStrategy strategy = calculator.optimizeClaimingStrategy(
 *     person1Benefit, person2Benefit, marriage);
 *
 * int higherEarnerAge = strategy.higherEarnerClaimingAgeMonths();
 * int lowerEarnerAge = strategy.lowerEarnerClaimingAgeMonths();
 * BigDecimal monthlyIncome = strategy.combinedMonthlyBenefit();
 * }</pre>
 *
 * @param higherEarnerClaimingAgeMonths recommended claiming age for higher earner (months)
 * @param lowerEarnerClaimingAgeMonths recommended claiming age for lower earner (months)
 * @param higherEarnerMonthlyBenefit the higher earner's benefit at recommended age
 * @param lowerEarnerMonthlyBenefit the lower earner's benefit at recommended age
 * @param combinedMonthlyBenefit total monthly benefit for the couple
 * @param lowerEarnerUsingSpousal whether lower earner should claim spousal benefit
 * @param strategyRationale explanation of why this strategy is recommended
 * @param alternativeStrategies other strategies considered with trade-offs
 *
 * @see <a href="https://www.ssa.gov/benefits/retirement/planner/applying7.html">SSA Spousal</a>
 */
public record CoupleClaimingStrategy(
    int higherEarnerClaimingAgeMonths,
    int lowerEarnerClaimingAgeMonths,
    BigDecimal higherEarnerMonthlyBenefit,
    BigDecimal lowerEarnerMonthlyBenefit,
    BigDecimal combinedMonthlyBenefit,
    boolean lowerEarnerUsingSpousal,
    String strategyRationale,
    List<AlternativeStrategy> alternativeStrategies
) {

    /**
     * An alternative claiming strategy with its trade-offs.
     *
     * @param higherEarnerAge claiming age in months for higher earner
     * @param lowerEarnerAge claiming age in months for lower earner
     * @param combinedBenefit total monthly benefit
     * @param description brief description of the trade-off
     */
    public record AlternativeStrategy(
        int higherEarnerAge,
        int lowerEarnerAge,
        BigDecimal combinedBenefit,
        String description
    ) { }

    /**
     * Compact constructor with validation.
     */
    public CoupleClaimingStrategy {
        higherEarnerMonthlyBenefit = higherEarnerMonthlyBenefit != null
            ? higherEarnerMonthlyBenefit : BigDecimal.ZERO;
        lowerEarnerMonthlyBenefit = lowerEarnerMonthlyBenefit != null
            ? lowerEarnerMonthlyBenefit : BigDecimal.ZERO;
        combinedMonthlyBenefit = combinedMonthlyBenefit != null
            ? combinedMonthlyBenefit : BigDecimal.ZERO;
        strategyRationale = strategyRationale != null
            ? strategyRationale : "No rationale provided";
        alternativeStrategies = alternativeStrategies != null
            ? List.copyOf(alternativeStrategies) : List.of();
    }

    // ==================== Factory Methods ====================

    /**
     * Creates a simple strategy with both spouses claiming at the same age.
     *
     * @param claimingAgeMonths the claiming age for both
     * @param higherEarnerBenefit the higher earner's benefit
     * @param lowerEarnerBenefit the lower earner's benefit
     * @param rationale explanation for this strategy
     * @return CoupleClaimingStrategy with same claiming age
     */
    public static CoupleClaimingStrategy bothAtSameAge(
            int claimingAgeMonths,
            BigDecimal higherEarnerBenefit,
            BigDecimal lowerEarnerBenefit,
            String rationale) {
        return new CoupleClaimingStrategy(
            claimingAgeMonths,
            claimingAgeMonths,
            higherEarnerBenefit,
            lowerEarnerBenefit,
            higherEarnerBenefit.add(lowerEarnerBenefit),
            false,
            rationale,
            List.of()
        );
    }

    /**
     * Creates a staggered strategy where higher earner delays.
     *
     * @param higherEarnerAge claiming age for higher earner
     * @param lowerEarnerAge claiming age for lower earner
     * @param higherEarnerBenefit the higher earner's benefit
     * @param lowerEarnerBenefit the lower earner's benefit
     * @param usingSpousal whether lower earner uses spousal benefit
     * @param rationale explanation for this strategy
     * @return CoupleClaimingStrategy with staggered claiming
     */
    public static CoupleClaimingStrategy staggered(
            int higherEarnerAge,
            int lowerEarnerAge,
            BigDecimal higherEarnerBenefit,
            BigDecimal lowerEarnerBenefit,
            boolean usingSpousal,
            String rationale) {
        return new CoupleClaimingStrategy(
            higherEarnerAge,
            lowerEarnerAge,
            higherEarnerBenefit,
            lowerEarnerBenefit,
            higherEarnerBenefit.add(lowerEarnerBenefit),
            usingSpousal,
            rationale,
            List.of()
        );
    }

    // ==================== Convenience Methods ====================

    /**
     * Returns the higher earner's claiming age in years.
     *
     * @return age in years
     */
    public int higherEarnerClaimingAgeYears() {
        return higherEarnerClaimingAgeMonths / 12;
    }

    /**
     * Returns the lower earner's claiming age in years.
     *
     * @return age in years
     */
    public int lowerEarnerClaimingAgeYears() {
        return lowerEarnerClaimingAgeMonths / 12;
    }

    /**
     * Returns the annual combined benefit.
     *
     * @return monthly benefit * 12
     */
    public BigDecimal annualCombinedBenefit() {
        return combinedMonthlyBenefit.multiply(BigDecimal.valueOf(12));
    }

    /**
     * Returns the difference in claiming ages between spouses.
     *
     * @return months difference (positive if higher earner claims later)
     */
    public int claimingAgeDifferenceMonths() {
        return higherEarnerClaimingAgeMonths - lowerEarnerClaimingAgeMonths;
    }
}
