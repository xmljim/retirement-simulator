package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;

/**
 * Result of routing a contribution across one or more accounts.
 *
 * <p>Contains the allocation amounts per account, any warnings generated
 * during routing, and tracking of allocated vs unallocated amounts.
 *
 * <p>Example usage:
 * <pre>{@code
 * ContributionAllocation allocation = router.route(...);
 *
 * // Get amount for specific account
 * BigDecimal trad401k = allocation.getAmountForAccount("trad-401k-id");
 *
 * // Check for warnings
 * if (allocation.hasWarnings()) {
 *     allocation.getWarnings().forEach(System.out::println);
 * }
 *
 * // Check if all was allocated
 * if (allocation.hasUnallocated()) {
 *     System.out.println("Unallocated: " + allocation.unallocated());
 * }
 * }</pre>
 *
 * @param allocations map of account ID to allocated amount
 * @param warnings list of warning messages generated during routing
 * @param totalAllocated total amount successfully allocated
 * @param unallocated amount that could not be allocated (e.g., due to limits)
 */
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP",
    justification = "Record components are made unmodifiable in compact constructor"
)
public record ContributionAllocation(
    Map<String, BigDecimal> allocations,
    List<String> warnings,
    BigDecimal totalAllocated,
    BigDecimal unallocated
) {
    // Compact constructor validates and makes components immutable
    public ContributionAllocation {
        MissingRequiredFieldException.requireNonNull(allocations, "allocations");
        allocations = Collections.unmodifiableMap(new HashMap<>(allocations));
        warnings = warnings != null
            ? Collections.unmodifiableList(new ArrayList<>(warnings))
            : Collections.emptyList();
        totalAllocated = totalAllocated != null ? totalAllocated : BigDecimal.ZERO;
        unallocated = unallocated != null ? unallocated : BigDecimal.ZERO;
    }

    /**
     * Returns the amount allocated to a specific account.
     *
     * @param accountId the account ID
     * @return the allocated amount, or zero if not found
     */
    public BigDecimal getAmountForAccount(String accountId) {
        return allocations.getOrDefault(accountId, BigDecimal.ZERO);
    }

    /**
     * Returns the amount allocated to a specific account as an Optional.
     *
     * @param accountId the account ID
     * @return Optional containing the amount, or empty if account not in allocation
     */
    public Optional<BigDecimal> findAmountForAccount(String accountId) {
        return Optional.ofNullable(allocations.get(accountId));
    }

    /**
     * Indicates whether any warnings were generated.
     *
     * @return true if there are warnings
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * Indicates whether there is any unallocated amount.
     *
     * @return true if unallocated amount is greater than zero
     */
    public boolean hasUnallocated() {
        return unallocated.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Indicates whether the allocation was fully successful.
     *
     * <p>Returns true if all amount was allocated and no warnings were generated.
     *
     * @return true if fully successful
     */
    public boolean isFullyAllocated() {
        return !hasUnallocated() && !hasWarnings();
    }

    /**
     * Returns the total amount that was requested for routing.
     *
     * @return totalAllocated + unallocated
     */
    public BigDecimal getTotalRequested() {
        return totalAllocated.add(unallocated);
    }

    /**
     * Returns the number of accounts that received allocations.
     *
     * @return the count of accounts with non-zero allocations
     */
    public int getAccountCount() {
        return (int) allocations.values().stream()
            .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
            .count();
    }

    /**
     * Creates a builder for ContributionAllocation.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates an empty allocation (nothing allocated).
     *
     * @param requestedAmount the amount that was requested
     * @return an allocation with everything unallocated
     */
    public static ContributionAllocation empty(BigDecimal requestedAmount) {
        return new ContributionAllocation(
            Collections.emptyMap(),
            List.of("No accounts available for allocation"),
            BigDecimal.ZERO,
            requestedAmount
        );
    }

    /**
     * Builder for creating ContributionAllocation instances.
     */
    public static class Builder {
        private final Map<String, BigDecimal> allocations = new HashMap<>();
        private final List<String> warnings = new ArrayList<>();
        private BigDecimal totalAllocated = BigDecimal.ZERO;
        private BigDecimal unallocated = BigDecimal.ZERO;

        /**
         * Adds an allocation for an account.
         *
         * @param accountId the account ID
         * @param amount the amount to allocate
         * @return this builder
         */
        public Builder addAllocation(String accountId, BigDecimal amount) {
            if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                allocations.merge(accountId, amount, BigDecimal::add);
                totalAllocated = totalAllocated.add(amount);
            }
            return this;
        }

        /**
         * Adds a warning message.
         *
         * @param warning the warning message
         * @return this builder
         */
        public Builder addWarning(String warning) {
            if (warning != null && !warning.isBlank()) {
                warnings.add(warning);
            }
            return this;
        }

        /**
         * Sets the unallocated amount.
         *
         * @param amount the unallocated amount
         * @return this builder
         */
        public Builder unallocated(BigDecimal amount) {
            this.unallocated = amount != null ? amount : BigDecimal.ZERO;
            return this;
        }

        /**
         * Builds the ContributionAllocation instance.
         *
         * @return a new ContributionAllocation
         */
        public ContributionAllocation build() {
            return new ContributionAllocation(allocations, warnings, totalAllocated, unallocated);
        }
    }
}
