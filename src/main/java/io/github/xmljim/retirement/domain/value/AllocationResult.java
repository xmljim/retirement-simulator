package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.xmljim.retirement.domain.enums.ExpenseCategory;

/**
 * Result of an expense allocation operation.
 *
 * <p>Contains the allocated amounts per category, any shortfalls,
 * surplus funds, and warnings generated during allocation.
 *
 * @param allocatedAmounts amounts allocated to each category
 * @param shortfalls amounts not covered for each category (if any)
 * @param totalAllocated sum of all allocated amounts
 * @param totalShortfall sum of all shortfalls
 * @param surplus funds remaining after all allocations
 * @param warnings any warnings generated during allocation
 *
 * @see io.github.xmljim.retirement.domain.calculator.ExpenseAllocationStrategy
 */
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "Record accessors return unmodifiable collections created in constructor"
)
public record AllocationResult(
        Map<ExpenseCategory, BigDecimal> allocatedAmounts,
        Map<ExpenseCategory, BigDecimal> shortfalls,
        BigDecimal totalAllocated,
        BigDecimal totalShortfall,
        BigDecimal surplus,
        List<String> warnings
) {

    /**
     * Canonical constructor with defensive copies.
     *
     * @param allocatedAmounts amounts allocated to each category
     * @param shortfalls amounts not covered for each category
     * @param totalAllocated sum of all allocated amounts
     * @param totalShortfall sum of all shortfalls
     * @param surplus funds remaining after all allocations
     * @param warnings any warnings generated during allocation
     */
    public AllocationResult {
        allocatedAmounts = allocatedAmounts != null
                ? Collections.unmodifiableMap(new HashMap<>(allocatedAmounts))
                : Collections.emptyMap();
        shortfalls = shortfalls != null
                ? Collections.unmodifiableMap(new HashMap<>(shortfalls))
                : Collections.emptyMap();
        totalAllocated = totalAllocated != null ? totalAllocated : BigDecimal.ZERO;
        totalShortfall = totalShortfall != null ? totalShortfall : BigDecimal.ZERO;
        surplus = surplus != null ? surplus : BigDecimal.ZERO;
        warnings = warnings != null
                ? Collections.unmodifiableList(new ArrayList<>(warnings))
                : Collections.emptyList();
    }

    /**
     * Indicates whether all expenses were fully funded.
     *
     * @return true if no shortfalls exist
     */
    public boolean isFullyFunded() {
        return totalShortfall.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Indicates whether there is a shortfall.
     *
     * @return true if any category has a shortfall
     */
    public boolean hasShortfall() {
        return totalShortfall.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Indicates whether there is surplus funds.
     *
     * @return true if there are excess funds after allocation
     */
    public boolean hasSurplus() {
        return surplus.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Indicates whether warnings were generated.
     *
     * @return true if there are warnings
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * Returns the allocated amount for a specific category.
     *
     * @param category the expense category
     * @return the allocated amount, or zero if not allocated
     */
    public BigDecimal getAllocatedAmount(ExpenseCategory category) {
        return allocatedAmounts.getOrDefault(category, BigDecimal.ZERO);
    }

    /**
     * Returns the shortfall amount for a specific category.
     *
     * @param category the expense category
     * @return the shortfall amount, or zero if fully funded
     */
    public BigDecimal getShortfallAmount(ExpenseCategory category) {
        return shortfalls.getOrDefault(category, BigDecimal.ZERO);
    }

    /**
     * Returns the funding percentage for a category.
     *
     * @param category the expense category
     * @param requestedAmount the originally requested amount
     * @return the percentage funded (0.0 to 1.0)
     */
    public BigDecimal getFundingPercentage(ExpenseCategory category, BigDecimal requestedAmount) {
        if (requestedAmount == null || requestedAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ONE;
        }
        BigDecimal allocated = getAllocatedAmount(category);
        return allocated.divide(requestedAmount, 4, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Creates a builder for constructing AllocationResult instances.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a fully funded result with no shortfalls.
     *
     * @param allocatedAmounts the allocated amounts
     * @param surplus any surplus funds
     * @return a new AllocationResult
     */
    public static AllocationResult fullyFunded(
            Map<ExpenseCategory, BigDecimal> allocatedAmounts,
            BigDecimal surplus) {

        BigDecimal total = allocatedAmounts.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new AllocationResult(
                allocatedAmounts,
                Collections.emptyMap(),
                total,
                BigDecimal.ZERO,
                surplus,
                Collections.emptyList()
        );
    }

    /**
     * Builder for AllocationResult.
     */
    public static class Builder {
        private final Map<ExpenseCategory, BigDecimal> allocatedAmounts = new HashMap<>();
        private final Map<ExpenseCategory, BigDecimal> shortfalls = new HashMap<>();
        private final List<String> warnings = new ArrayList<>();
        private BigDecimal surplus = BigDecimal.ZERO;

        /**
         * Records an allocation for a category.
         *
         * @param category the expense category
         * @param allocated the amount allocated
         * @param requested the amount originally requested
         * @return this builder
         */
        public Builder allocate(ExpenseCategory category, BigDecimal allocated, BigDecimal requested) {
            allocatedAmounts.put(category, allocated);
            BigDecimal shortfall = requested.subtract(allocated);
            if (shortfall.compareTo(BigDecimal.ZERO) > 0) {
                shortfalls.put(category, shortfall);
            }
            return this;
        }

        /**
         * Records a full allocation for a category (no shortfall).
         *
         * @param category the expense category
         * @param amount the amount allocated
         * @return this builder
         */
        public Builder allocateFully(ExpenseCategory category, BigDecimal amount) {
            allocatedAmounts.put(category, amount);
            return this;
        }

        /**
         * Records a shortfall for a category.
         *
         * @param category the expense category
         * @param shortfall the shortfall amount
         * @return this builder
         */
        public Builder shortfall(ExpenseCategory category, BigDecimal shortfall) {
            shortfalls.put(category, shortfall);
            return this;
        }

        /**
         * Sets the surplus amount.
         *
         * @param surplus the surplus
         * @return this builder
         */
        public Builder surplus(BigDecimal surplus) {
            this.surplus = surplus;
            return this;
        }

        /**
         * Adds a warning message.
         *
         * @param warning the warning message
         * @return this builder
         */
        public Builder warning(String warning) {
            warnings.add(warning);
            return this;
        }

        /**
         * Builds the AllocationResult.
         *
         * @return a new AllocationResult
         */
        public AllocationResult build() {
            BigDecimal totalAllocated = allocatedAmounts.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalShortfall = shortfalls.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return new AllocationResult(
                    allocatedAmounts,
                    shortfalls,
                    totalAllocated,
                    totalShortfall,
                    surplus,
                    warnings
            );
        }
    }
}
