package io.github.xmljim.retirement.simulation.config;

import io.github.xmljim.retirement.domain.value.ContributionConfig;
import io.github.xmljim.retirement.domain.value.RoutingConfiguration;

/**
 * Bundles per-person financial configuration for the simulation.
 *
 * <p>Each person in the simulation can have their own contribution
 * and routing settings. This record groups those settings together.
 *
 * <p>Example usage:
 * <pre>{@code
 * PersonFinancialConfig config = PersonFinancialConfig.builder()
 *     .contributionConfig(ContributionConfig.builder()
 *         .contributionType(ContributionType.PERSONAL)
 *         .contributionRate(0.10)  // 10% of salary
 *         .incrementRate(0.01)     // +1% per year
 *         .matchingPolicy(MatchingPolicy.simple(0.50, 0.06))
 *         .build())
 *     .routingConfig(RoutingConfiguration.builder()
 *         .addRule("trad-401k", 0.70, 1)
 *         .addRule("roth-401k", 0.30, 2)
 *         .build())
 *     .build();
 * }</pre>
 *
 * @param contributionConfig the contribution settings (rate, increment, employer match)
 * @param routingConfig      how contributions are routed across accounts
 */
public record PersonFinancialConfig(
    ContributionConfig contributionConfig,
    RoutingConfiguration routingConfig
) {

    /**
     * Creates a PersonFinancialConfig with only contribution config.
     *
     * <p>Use this when routing is handled separately or defaults to single account.
     *
     * @param contributionConfig the contribution configuration
     * @return a new PersonFinancialConfig
     */
    public static PersonFinancialConfig ofContribution(ContributionConfig contributionConfig) {
        return new PersonFinancialConfig(contributionConfig, null);
    }

    /**
     * Creates a PersonFinancialConfig with only routing config.
     *
     * <p>Use this when contribution amounts are calculated externally.
     *
     * @param routingConfig the routing configuration
     * @return a new PersonFinancialConfig
     */
    public static PersonFinancialConfig ofRouting(RoutingConfiguration routingConfig) {
        return new PersonFinancialConfig(null, routingConfig);
    }

    /**
     * Returns true if contribution config is present.
     *
     * @return true if contributionConfig is not null
     */
    public boolean hasContributionConfig() {
        return contributionConfig != null;
    }

    /**
     * Returns true if routing config is present.
     *
     * @return true if routingConfig is not null
     */
    public boolean hasRoutingConfig() {
        return routingConfig != null;
    }

    /**
     * Creates a new builder for PersonFinancialConfig.
     *
     * @return a builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for PersonFinancialConfig.
     */
    public static class Builder {
        private ContributionConfig contributionConfig;
        private RoutingConfiguration routingConfig;

        /**
         * Sets the contribution configuration.
         *
         * @param config the contribution config
         * @return this builder
         */
        public Builder contributionConfig(ContributionConfig config) {
            this.contributionConfig = config;
            return this;
        }

        /**
         * Sets the routing configuration.
         *
         * @param config the routing config
         * @return this builder
         */
        public Builder routingConfig(RoutingConfiguration config) {
            this.routingConfig = config;
            return this;
        }

        /**
         * Builds the PersonFinancialConfig.
         *
         * @return a new PersonFinancialConfig
         */
        public PersonFinancialConfig build() {
            return new PersonFinancialConfig(contributionConfig, routingConfig);
        }
    }
}
