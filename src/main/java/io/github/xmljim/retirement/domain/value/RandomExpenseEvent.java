package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

import io.github.xmljim.retirement.domain.enums.ContingencyType;

/**
 * A random expense event for Monte Carlo simulation.
 *
 * <p>Models unexpected expenses with a probability of occurring
 * and a range of possible costs. Used in probabilistic simulations
 * to model "lumpy" spending that varies between scenarios.
 *
 * <p>Examples:
 * <ul>
 *   <li>HVAC failure: $8K-$15K, 5% annual probability</li>
 *   <li>Roof repair: $10K-$25K, 3% annual probability</li>
 *   <li>Major plumbing: $5K-$15K, 4% annual probability</li>
 * </ul>
 *
 * @param name the event name
 * @param type the contingency type
 * @param minAmount the minimum expense amount
 * @param maxAmount the maximum expense amount
 * @param annualProbability the probability of occurring in a given year (0.0-1.0)
 */
public record RandomExpenseEvent(
    String name,
    ContingencyType type,
    BigDecimal minAmount,
    BigDecimal maxAmount,
    double annualProbability
) {

    /**
     * Creates a random expense event.
     *
     * @param name the event name
     * @param type the contingency type
     * @param minAmount minimum cost
     * @param maxAmount maximum cost
     * @param annualProbability probability per year (0.0-1.0)
     * @return a new RandomExpenseEvent
     */
    public static RandomExpenseEvent of(
            String name,
            ContingencyType type,
            double minAmount,
            double maxAmount,
            double annualProbability) {
        return new RandomExpenseEvent(
            name,
            type,
            BigDecimal.valueOf(minAmount),
            BigDecimal.valueOf(maxAmount),
            annualProbability
        );
    }

    /**
     * Returns the expected annual cost (probability × average amount).
     *
     * @return the expected value
     */
    public BigDecimal getExpectedAnnualCost() {
        BigDecimal average = minAmount.add(maxAmount)
            .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        return average.multiply(BigDecimal.valueOf(annualProbability))
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Returns the average expense amount.
     *
     * @return (min + max) / 2
     */
    public BigDecimal getAverageAmount() {
        return minAmount.add(maxAmount)
            .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    /**
     * Determines if this event occurs in a simulation run.
     *
     * @param random the random number generator
     * @return true if the event occurs
     */
    public boolean occurs(Random random) {
        return random.nextDouble() < annualProbability;
    }

    /**
     * Generates a random expense amount within the range.
     *
     * @param random the random number generator
     * @return a random amount between min and max
     */
    public BigDecimal generateAmount(Random random) {
        BigDecimal range = maxAmount.subtract(minAmount);
        BigDecimal randomFactor = BigDecimal.valueOf(random.nextDouble());
        return minAmount.add(range.multiply(randomFactor))
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Simulates this event for one year.
     *
     * @param random the random number generator
     * @return the expense amount (0 if event doesn't occur)
     */
    public BigDecimal simulate(Random random) {
        if (occurs(random)) {
            return generateAmount(random);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Returns the probability as a percentage string.
     *
     * @return e.g., "5.0%"
     */
    public String getProbabilityPercent() {
        return String.format("%.1f%%", annualProbability * 100);
    }

    /**
     * Common random expense: HVAC system failure/replacement.
     *
     * @return a pre-configured HVAC expense event
     */
    public static RandomExpenseEvent hvacFailure() {
        return of("HVAC Replacement", ContingencyType.HOME_REPAIR, 8000, 15000, 0.05);
    }

    /**
     * Common random expense: Roof repair/replacement.
     *
     * @return a pre-configured roof expense event
     */
    public static RandomExpenseEvent roofRepair() {
        return of("Roof Repair", ContingencyType.HOME_REPAIR, 10000, 25000, 0.03);
    }

    /**
     * Common random expense: Major plumbing issue.
     *
     * @return a pre-configured plumbing expense event
     */
    public static RandomExpenseEvent majorPlumbing() {
        return of("Major Plumbing", ContingencyType.HOME_REPAIR, 5000, 15000, 0.04);
    }

    /**
     * Common random expense: Major appliance failure.
     *
     * @return a pre-configured appliance expense event
     */
    public static RandomExpenseEvent applianceFailure() {
        return of("Appliance Failure", ContingencyType.APPLIANCE_RESERVE, 2000, 8000, 0.10);
    }
}
