package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.time.Month;
import java.util.Objects;

import io.github.xmljim.retirement.domain.annotation.Generated;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.enums.ContributionType;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;

/**
 * Configuration for a retirement account contribution.
 *
 * <p>Represents the contribution settings for either personal or employer
 * contributions, including the base rate, annual increment, and the month
 * when the increment is applied.
 *
 * <p>Additional SECURE 2.0 support:
 * <ul>
 *   <li>{@code targetAccountType} - Optional override for ROTH routing. When set,
 *       contributions will be directed to this account type instead of the account's
 *       default. Used for high earner catch-up contributions that must go to ROTH.</li>
 *   <li>{@code matchingPolicy} - For employer contribution configs, defines the
 *       matching rules (simple, tiered, or none).</li>
 * </ul>
 *
 * <p>This is an immutable value object. Use the {@link Builder} to create instances.
 */
public final class ContributionConfig {

    private final ContributionType contributionType;
    private final BigDecimal contributionRate;
    private final BigDecimal incrementRate;
    private final Month incrementMonth;
    private final AccountType targetAccountType;
    private final MatchingPolicy matchingPolicy;

    private ContributionConfig(Builder builder) {
        this.contributionType = builder.contributionType;
        this.contributionRate = builder.contributionRate;
        this.incrementRate = builder.incrementRate;
        this.incrementMonth = builder.incrementMonth;
        this.targetAccountType = builder.targetAccountType;
        this.matchingPolicy = builder.matchingPolicy;
    }

    /**
     * Returns the type of contribution (personal or employer).
     *
     * @return the contribution type
     */
    public ContributionType getContributionType() {
        return contributionType;
    }

    /**
     * Returns the base contribution rate as a decimal (e.g., 0.06 for 6%).
     *
     * @return the contribution rate
     */
    public BigDecimal getContributionRate() {
        return contributionRate;
    }

    /**
     * Returns the annual increment rate as a decimal.
     *
     * <p>This is the amount by which the contribution rate increases each year.
     * For example, 0.01 means the rate increases by 1% each year.
     *
     * @return the increment rate
     */
    public BigDecimal getIncrementRate() {
        return incrementRate;
    }

    /**
     * Returns the month when the annual increment is applied.
     *
     * @return the increment month
     */
    public Month getIncrementMonth() {
        return incrementMonth;
    }

    /**
     * Returns the target account type for ROTH routing.
     *
     * <p>When set, contributions will be directed to this account type instead
     * of the account's default. This is used for SECURE 2.0 high earner catch-up
     * contributions that must go to ROTH.
     *
     * @return the target account type, or null if using account default
     */
    public AccountType getTargetAccountType() {
        return targetAccountType;
    }

    /**
     * Returns the matching policy for employer contributions.
     *
     * <p>For employer contribution configs, this defines how the employer
     * calculates the match (simple, tiered, or none).
     *
     * @return the matching policy, or null if not an employer contribution
     */
    public MatchingPolicy getMatchingPolicy() {
        return matchingPolicy;
    }

    /**
     * Creates a simple personal contribution with no annual increment.
     *
     * @param rate the contribution rate as a decimal
     * @return a new ContributionConfig
     */
    public static ContributionConfig personal(double rate) {
        return builder()
            .contributionType(ContributionType.PERSONAL)
            .contributionRate(rate)
            .build();
    }

    /**
     * Creates an employer contribution with no annual increment.
     *
     * @param rate the contribution rate as a decimal
     * @return a new ContributionConfig
     */
    public static ContributionConfig employer(double rate) {
        return builder()
            .contributionType(ContributionType.EMPLOYER)
            .contributionRate(rate)
            .build();
    }

    /**
     * Creates a new builder for ContributionConfig.
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
        ContributionConfig that = (ContributionConfig) o;
        return contributionType == that.contributionType
            && contributionRate.compareTo(that.contributionRate) == 0
            && incrementRate.compareTo(that.incrementRate) == 0
            && incrementMonth == that.incrementMonth
            && targetAccountType == that.targetAccountType
            && Objects.equals(matchingPolicy, that.matchingPolicy);
    }

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(contributionType, contributionRate, incrementRate, incrementMonth,
            targetAccountType, matchingPolicy);
    }

    @Generated
    @Override
    public String toString() {
        return "ContributionConfig{" +
            "type=" + contributionType +
            ", rate=" + contributionRate +
            ", increment=" + incrementRate +
            ", month=" + incrementMonth +
            ", targetAccountType=" + targetAccountType +
            ", matchingPolicy=" + matchingPolicy +
            '}';
    }

    /**
     * Builder for creating ContributionConfig instances.
     */
    public static class Builder {
        private ContributionType contributionType;
        private BigDecimal contributionRate = BigDecimal.ZERO;
        private BigDecimal incrementRate = BigDecimal.ZERO;
        private Month incrementMonth = Month.JANUARY;
        private AccountType targetAccountType;
        private MatchingPolicy matchingPolicy;

        /**
         * Sets the contribution type.
         *
         * @param type the contribution type
         * @return this builder
         */
        public Builder contributionType(ContributionType type) {
            this.contributionType = type;
            return this;
        }

        /**
         * Sets the contribution rate.
         *
         * @param rate the rate as a decimal
         * @return this builder
         */
        public Builder contributionRate(BigDecimal rate) {
            this.contributionRate = rate;
            return this;
        }

        /**
         * Sets the contribution rate.
         *
         * @param rate the rate as a decimal
         * @return this builder
         */
        public Builder contributionRate(double rate) {
            return contributionRate(BigDecimal.valueOf(rate));
        }

        /**
         * Sets the annual increment rate.
         *
         * @param rate the increment rate as a decimal
         * @return this builder
         */
        public Builder incrementRate(BigDecimal rate) {
            this.incrementRate = rate;
            return this;
        }

        /**
         * Sets the annual increment rate.
         *
         * @param rate the increment rate as a decimal
         * @return this builder
         */
        public Builder incrementRate(double rate) {
            return incrementRate(BigDecimal.valueOf(rate));
        }

        /**
         * Sets the month when the increment is applied.
         *
         * @param month the increment month
         * @return this builder
         */
        public Builder incrementMonth(Month month) {
            this.incrementMonth = month;
            return this;
        }

        /**
         * Sets the target account type for ROTH routing.
         *
         * <p>When set, contributions will be directed to this account type instead
         * of the account's default. This is used for SECURE 2.0 high earner catch-up
         * contributions that must go to ROTH.
         *
         * @param accountType the target account type
         * @return this builder
         */
        public Builder targetAccountType(AccountType accountType) {
            this.targetAccountType = accountType;
            return this;
        }

        /**
         * Sets the matching policy for employer contributions.
         *
         * <p>For employer contribution configs, this defines how the employer
         * calculates the match (simple, tiered, or none).
         *
         * @param policy the matching policy
         * @return this builder
         */
        public Builder matchingPolicy(MatchingPolicy policy) {
            this.matchingPolicy = policy;
            return this;
        }

        /**
         * Builds the ContributionConfig instance.
         *
         * @return a new ContributionConfig
         * @throws MissingRequiredFieldException if contributionType is null
         * @throws ValidationException if rates are negative
         */
        public ContributionConfig build() {
            validate();
            return new ContributionConfig(this);
        }

        private void validate() {
            MissingRequiredFieldException.requireNonNull(contributionType, "contributionType");
            if (contributionRate.compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("Contribution rate cannot be negative", "contributionRate");
            }
            if (incrementRate.compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("Increment rate cannot be negative", "incrementRate");
            }
        }
    }
}
