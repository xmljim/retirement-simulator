package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Result of a spending strategy calculation.
 *
 * <p>Represents the complete withdrawal plan calculated by a {@code SpendingStrategy},
 * including the target withdrawal amount, actual withdrawals from each account,
 * and whether the target was fully met.
 *
 * <p>This record is immutable and all collections are unmodifiable.
 *
 * <p>Example usage:
 * <pre>{@code
 * SpendingPlan plan = strategy.calculateWithdrawal(context);
 * if (!plan.meetsTarget()) {
 *     BigDecimal shortfall = plan.shortfall();
 *     // Handle shortfall scenario
 * }
 *
 * // Process account withdrawals
 * for (AccountWithdrawal withdrawal : plan.accountWithdrawals()) {
 *     processWithdrawal(withdrawal);
 * }
 * }</pre>
 *
 * @param targetWithdrawal the target withdrawal amount based on strategy
 * @param adjustedWithdrawal the actual withdrawal amount after constraints
 * @param accountWithdrawals list of withdrawals from each account
 * @param meetsTarget true if adjusted withdrawal equals or exceeds target
 * @param shortfall the difference between target and adjusted (zero if met)
 * @param strategyUsed the name of the strategy that generated this plan
 * @param metadata additional strategy-specific information
 * @see io.github.xmljim.retirement.domain.calculator.SpendingStrategy
 * @see AccountWithdrawal
 */
@SuppressFBWarnings(value = "EI_EXPOSE_REP",
        justification = "Record makes defensive copy in compact constructor; collections are unmodifiable")
public record SpendingPlan(
        BigDecimal targetWithdrawal,
        BigDecimal adjustedWithdrawal,
        List<AccountWithdrawal> accountWithdrawals,
        boolean meetsTarget,
        BigDecimal shortfall,
        String strategyUsed,
        Map<String, Object> metadata
) {

    /**
     * Compact constructor with validation and defensive copies.
     */
    public SpendingPlan {
        targetWithdrawal = targetWithdrawal != null ? targetWithdrawal : BigDecimal.ZERO;
        adjustedWithdrawal = adjustedWithdrawal != null ? adjustedWithdrawal : BigDecimal.ZERO;
        accountWithdrawals = accountWithdrawals != null
                ? Collections.unmodifiableList(accountWithdrawals)
                : Collections.emptyList();
        shortfall = shortfall != null ? shortfall : BigDecimal.ZERO;
        metadata = metadata != null
                ? Collections.unmodifiableMap(metadata)
                : Collections.emptyMap();
    }

    /**
     * Returns the total taxable amount from all withdrawals.
     *
     * <p>Sums withdrawal amounts from accounts with PRE_TAX treatment.
     *
     * @return the total taxable withdrawal amount
     */
    public BigDecimal totalTaxableAmount() {
        return accountWithdrawals.stream()
                .filter(AccountWithdrawal::isTaxable)
                .map(AccountWithdrawal::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Returns the total tax-free amount from all withdrawals.
     *
     * <p>Sums withdrawal amounts from Roth and qualified HSA accounts.
     *
     * @return the total tax-free withdrawal amount
     */
    public BigDecimal totalTaxFreeAmount() {
        return accountWithdrawals.stream()
                .filter(w -> !w.isTaxable())
                .map(AccountWithdrawal::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Returns the number of accounts that were depleted.
     *
     * @return count of depleted accounts
     */
    public long depletedAccountCount() {
        return accountWithdrawals.stream()
                .filter(AccountWithdrawal::isDepleted)
                .count();
    }

    /**
     * Returns whether any account was depleted by this plan.
     *
     * @return true if at least one account was depleted
     */
    public boolean hasDepletedAccounts() {
        return depletedAccountCount() > 0;
    }

    /**
     * Creates a builder for SpendingPlan.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a spending plan indicating no withdrawal needed.
     *
     * @param strategyUsed the strategy name
     * @return a SpendingPlan with zero withdrawal
     */
    public static SpendingPlan noWithdrawalNeeded(String strategyUsed) {
        return new SpendingPlan(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                Collections.emptyList(),
                true,
                BigDecimal.ZERO,
                strategyUsed,
                Collections.emptyMap()
        );
    }

    /**
     * Builder for creating SpendingPlan instances.
     */
    public static class Builder {
        private BigDecimal targetWithdrawal = BigDecimal.ZERO;
        private BigDecimal adjustedWithdrawal = BigDecimal.ZERO;
        private List<AccountWithdrawal> accountWithdrawals = Collections.emptyList();
        private boolean meetsTarget = true;
        private BigDecimal shortfall = BigDecimal.ZERO;
        private String strategyUsed;
        private Map<String, Object> metadata = new HashMap<>();

        /**
         * Sets the target withdrawal amount.
         *
         * @param targetWithdrawal the target amount
         * @return this builder
         */
        public Builder targetWithdrawal(BigDecimal targetWithdrawal) {
            this.targetWithdrawal = targetWithdrawal;
            return this;
        }

        /**
         * Sets the adjusted withdrawal amount.
         *
         * @param adjustedWithdrawal the adjusted amount
         * @return this builder
         */
        public Builder adjustedWithdrawal(BigDecimal adjustedWithdrawal) {
            this.adjustedWithdrawal = adjustedWithdrawal;
            return this;
        }

        /**
         * Sets the list of account withdrawals.
         *
         * @param accountWithdrawals the withdrawals
         * @return this builder
         */
        public Builder accountWithdrawals(List<AccountWithdrawal> accountWithdrawals) {
            this.accountWithdrawals = accountWithdrawals;
            return this;
        }

        /**
         * Sets whether the target was met.
         *
         * @param meetsTarget true if target met
         * @return this builder
         */
        public Builder meetsTarget(boolean meetsTarget) {
            this.meetsTarget = meetsTarget;
            return this;
        }

        /**
         * Sets the shortfall amount.
         *
         * @param shortfall the shortfall
         * @return this builder
         */
        public Builder shortfall(BigDecimal shortfall) {
            this.shortfall = shortfall;
            return this;
        }

        /**
         * Sets the strategy name.
         *
         * @param strategyUsed the strategy name
         * @return this builder
         */
        public Builder strategyUsed(String strategyUsed) {
            this.strategyUsed = strategyUsed;
            return this;
        }

        /**
         * Sets the metadata map.
         *
         * @param metadata the metadata
         * @return this builder
         */
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata != null ? metadata : new HashMap<>();
            return this;
        }

        /**
         * Adds a single metadata entry.
         *
         * @param key the key
         * @param value the value
         * @return this builder
         */
        public Builder addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        /**
         * Builds the SpendingPlan instance.
         *
         * @return a new SpendingPlan
         */
        public SpendingPlan build() {
            return new SpendingPlan(
                    targetWithdrawal,
                    adjustedWithdrawal,
                    accountWithdrawals,
                    meetsTarget,
                    shortfall,
                    strategyUsed,
                    metadata
            );
        }
    }
}
