package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;

import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;

/**
 * Represents a routing rule for directing contributions to a specific account.
 *
 * <p>A routing rule specifies what percentage of a contribution should go to
 * a particular account and the priority order when multiple accounts are configured.
 *
 * <p>Example usage:
 * <pre>{@code
 * // 80% to Traditional 401(k), priority 1 (highest)
 * RoutingRule traditional = new RoutingRule("trad-401k-id", new BigDecimal("0.80"), 1);
 *
 * // 20% to Roth 401(k), priority 2
 * RoutingRule roth = new RoutingRule("roth-401k-id", new BigDecimal("0.20"), 2);
 * }</pre>
 *
 * @param accountId the unique identifier of the target account
 * @param percentage the percentage of contribution to route (0.0 to 1.0, e.g., 0.80 for 80%)
 * @param priority the routing priority (lower number = higher priority)
 */
public record RoutingRule(
    String accountId,
    BigDecimal percentage,
    int priority
) {
    // Compact constructor validates all parameters
    public RoutingRule {
        MissingRequiredFieldException.requireNonNull(accountId, "accountId");
        if (accountId.isBlank()) {
            throw new MissingRequiredFieldException("accountId", "Account ID cannot be blank");
        }
        MissingRequiredFieldException.requireNonNull(percentage, "percentage");

        if (percentage.compareTo(BigDecimal.ZERO) < 0
                || percentage.compareTo(BigDecimal.ONE) > 0) {
            throw new ValidationException(
                "Percentage must be between 0 and 1 (got " + percentage + ")", "percentage");
        }

        if (priority < 0) {
            throw new ValidationException(
                "Priority must be non-negative (got " + priority + ")", "priority");
        }
    }

    /**
     * Creates a routing rule from a percentage value.
     *
     * @param accountId the unique identifier of the target account
     * @param percentageValue the percentage value (e.g., 80.0 for 80%)
     * @param priority the routing priority
     * @return a new RoutingRule
     */
    public static RoutingRule ofPercent(String accountId, double percentageValue, int priority) {
        return new RoutingRule(
            accountId,
            BigDecimal.valueOf(percentageValue / 100.0),
            priority
        );
    }

    /**
     * Creates a routing rule with 100% allocation at priority 1.
     *
     * <p>Use this when routing all contributions to a single account.
     *
     * @param accountId the unique identifier of the target account
     * @return a new RoutingRule with 100% allocation
     */
    public static RoutingRule singleAccount(String accountId) {
        return new RoutingRule(accountId, BigDecimal.ONE, 1);
    }
}
