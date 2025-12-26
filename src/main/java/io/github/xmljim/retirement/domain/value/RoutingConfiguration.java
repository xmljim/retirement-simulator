package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import io.github.xmljim.retirement.domain.annotation.Generated;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;

/**
 * Configuration for routing contributions across multiple accounts.
 *
 * <p>A routing configuration contains a collection of {@link RoutingRule} objects
 * that define how contributions should be split across accounts. The rules must
 * have percentages that sum to exactly 100%.
 *
 * <p>Example usage:
 * <pre>{@code
 * RoutingConfiguration config = RoutingConfiguration.builder()
 *     .addRule("trad-401k", 0.80, 1)  // 80% to Traditional, priority 1
 *     .addRule("roth-401k", 0.20, 2)  // 20% to Roth, priority 2
 *     .build();
 * }</pre>
 */
public final class RoutingConfiguration {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.0001");

    private final List<RoutingRule> rules;

    private RoutingConfiguration(List<RoutingRule> rules) {
        this.rules = Collections.unmodifiableList(new ArrayList<>(rules));
    }

    /**
     * Returns the routing rules sorted by priority (lowest first).
     *
     * @return an unmodifiable list of routing rules
     */
    public List<RoutingRule> getRules() {
        return rules;
    }

    /**
     * Returns the routing rules sorted by priority.
     *
     * @return a list of rules sorted by priority (lowest number first)
     */
    public List<RoutingRule> getRulesByPriority() {
        return rules.stream()
            .sorted(Comparator.comparingInt(RoutingRule::priority))
            .toList();
    }

    /**
     * Returns the total percentage across all rules.
     *
     * @return the sum of all rule percentages
     */
    public BigDecimal getTotalPercentage() {
        return rules.stream()
            .map(RoutingRule::percentage)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Returns the number of routing rules.
     *
     * @return the rule count
     */
    public int size() {
        return rules.size();
    }

    /**
     * Indicates whether this configuration has any rules.
     *
     * @return true if there are rules
     */
    public boolean hasRules() {
        return !rules.isEmpty();
    }

    /**
     * Creates a single-account routing configuration.
     *
     * @param accountId the account to route 100% of contributions to
     * @return a new RoutingConfiguration
     */
    public static RoutingConfiguration singleAccount(String accountId) {
        return builder()
            .addRule(RoutingRule.singleAccount(accountId))
            .build();
    }

    /**
     * Creates a new builder for RoutingConfiguration.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    @Generated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RoutingConfiguration that = (RoutingConfiguration) o;
        return Objects.equals(rules, that.rules);
    }

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(rules);
    }

    @Generated
    @Override
    public String toString() {
        return "RoutingConfiguration{" +
            "rules=" + rules +
            ", totalPercentage=" + getTotalPercentage() +
            '}';
    }

    /**
     * Builder for creating RoutingConfiguration instances.
     */
    public static class Builder {
        private final List<RoutingRule> rules = new ArrayList<>();

        /**
         * Adds a routing rule.
         *
         * @param rule the routing rule to add
         * @return this builder
         */
        public Builder addRule(RoutingRule rule) {
            MissingRequiredFieldException.requireNonNull(rule, "rule");
            rules.add(rule);
            return this;
        }

        /**
         * Adds a routing rule with the specified parameters.
         *
         * @param accountId the target account ID
         * @param percentage the percentage as a decimal (0.0 to 1.0)
         * @param priority the routing priority
         * @return this builder
         */
        public Builder addRule(String accountId, double percentage, int priority) {
            return addRule(new RoutingRule(
                accountId,
                BigDecimal.valueOf(percentage),
                priority
            ));
        }

        /**
         * Adds a routing rule using percentage value (e.g., 80 for 80%).
         *
         * @param accountId the target account ID
         * @param percentValue the percentage value (0-100)
         * @param priority the routing priority
         * @return this builder
         */
        public Builder addRulePercent(String accountId, double percentValue, int priority) {
            return addRule(RoutingRule.ofPercent(accountId, percentValue, priority));
        }

        /**
         * Builds the RoutingConfiguration instance.
         *
         * @return a new RoutingConfiguration
         * @throws ValidationException if rules are empty or percentages don't sum to 100%
         */
        public RoutingConfiguration build() {
            validate();
            // Sort by priority before creating
            rules.sort(Comparator.comparingInt(RoutingRule::priority));
            return new RoutingConfiguration(rules);
        }

        private void validate() {
            if (rules.isEmpty()) {
                throw new ValidationException(
                    "RoutingConfiguration must have at least one rule", "rules");
            }

            BigDecimal total = rules.stream()
                .map(RoutingRule::percentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal difference = total.subtract(BigDecimal.ONE).abs();
            if (difference.compareTo(TOLERANCE) > 0) {
                throw new ValidationException(
                    "Routing percentages must sum to 100% (got "
                        + total.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
                        + "%)",
                    "rules");
            }
        }
    }
}
