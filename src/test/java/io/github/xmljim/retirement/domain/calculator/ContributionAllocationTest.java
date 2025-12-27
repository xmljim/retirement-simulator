package io.github.xmljim.retirement.domain.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;

@DisplayName("ContributionAllocation Tests")
class ContributionAllocationTest {

    @Nested
    @DisplayName("Construction")
    class ConstructionTests {

        @Test
        @DisplayName("Should create allocation with builder")
        void createWithBuilder() {
            ContributionAllocation allocation = ContributionAllocation.builder()
                .addAllocation("account-1", new BigDecimal("5000"))
                .addAllocation("account-2", new BigDecimal("1000"))
                .build();

            assertEquals(new BigDecimal("6000"), allocation.totalAllocated());
            assertEquals(2, allocation.getAccountCount());
        }

        @Test
        @DisplayName("Should create empty allocation")
        void createEmpty() {
            ContributionAllocation allocation = ContributionAllocation.empty(
                new BigDecimal("5000"));

            assertEquals(BigDecimal.ZERO, allocation.totalAllocated());
            assertEquals(new BigDecimal("5000"), allocation.unallocated());
            assertTrue(allocation.hasWarnings());
        }

        @Test
        @DisplayName("Should throw for null allocations map")
        void nullAllocationsThrows() {
            assertThrows(MissingRequiredFieldException.class, () ->
                new ContributionAllocation(null, List.of(), BigDecimal.ZERO, BigDecimal.ZERO)
            );
        }
    }

    @Nested
    @DisplayName("Allocation Queries")
    class QueryTests {

        @Test
        @DisplayName("Should get amount for existing account")
        void getAmountForExistingAccount() {
            ContributionAllocation allocation = ContributionAllocation.builder()
                .addAllocation("account-1", new BigDecimal("5000"))
                .build();

            assertEquals(new BigDecimal("5000"), allocation.getAmountForAccount("account-1"));
        }

        @Test
        @DisplayName("Should return zero for non-existent account")
        void getAmountForNonExistentAccount() {
            ContributionAllocation allocation = ContributionAllocation.builder()
                .addAllocation("account-1", new BigDecimal("5000"))
                .build();

            assertEquals(BigDecimal.ZERO, allocation.getAmountForAccount("non-existent"));
        }

        @Test
        @DisplayName("Should find amount as Optional")
        void findAmountAsOptional() {
            ContributionAllocation allocation = ContributionAllocation.builder()
                .addAllocation("account-1", new BigDecimal("5000"))
                .build();

            Optional<BigDecimal> found = allocation.findAmountForAccount("account-1");
            Optional<BigDecimal> notFound = allocation.findAmountForAccount("non-existent");

            assertTrue(found.isPresent());
            assertEquals(new BigDecimal("5000"), found.get());
            assertFalse(notFound.isPresent());
        }
    }

    @Nested
    @DisplayName("Status Checks")
    class StatusTests {

        @Test
        @DisplayName("Should report fully allocated when no unallocated and no warnings")
        void fullyAllocated() {
            ContributionAllocation allocation = ContributionAllocation.builder()
                .addAllocation("account-1", new BigDecimal("5000"))
                .build();

            assertTrue(allocation.isFullyAllocated());
            assertFalse(allocation.hasUnallocated());
            assertFalse(allocation.hasWarnings());
        }

        @Test
        @DisplayName("Should report not fully allocated when has unallocated")
        void notFullyAllocatedWhenUnallocated() {
            ContributionAllocation allocation = ContributionAllocation.builder()
                .addAllocation("account-1", new BigDecimal("3000"))
                .unallocated(new BigDecimal("2000"))
                .build();

            assertFalse(allocation.isFullyAllocated());
            assertTrue(allocation.hasUnallocated());
        }

        @Test
        @DisplayName("Should report not fully allocated when has warnings")
        void notFullyAllocatedWhenWarnings() {
            ContributionAllocation allocation = ContributionAllocation.builder()
                .addAllocation("account-1", new BigDecimal("5000"))
                .addWarning("Some warning")
                .build();

            assertFalse(allocation.isFullyAllocated());
            assertTrue(allocation.hasWarnings());
        }

        @Test
        @DisplayName("Should calculate total requested")
        void totalRequested() {
            ContributionAllocation allocation = ContributionAllocation.builder()
                .addAllocation("account-1", new BigDecimal("3000"))
                .unallocated(new BigDecimal("2000"))
                .build();

            assertEquals(new BigDecimal("5000"), allocation.getTotalRequested());
        }
    }

    @Nested
    @DisplayName("Builder Behavior")
    class BuilderTests {

        @Test
        @DisplayName("Should merge allocations to same account")
        void mergeAllocations() {
            ContributionAllocation allocation = ContributionAllocation.builder()
                .addAllocation("account-1", new BigDecimal("3000"))
                .addAllocation("account-1", new BigDecimal("2000"))
                .build();

            assertEquals(new BigDecimal("5000"), allocation.getAmountForAccount("account-1"));
            assertEquals(1, allocation.getAccountCount());
        }

        @Test
        @DisplayName("Should ignore null or zero allocations")
        void ignoreNullOrZeroAllocations() {
            ContributionAllocation allocation = ContributionAllocation.builder()
                .addAllocation("account-1", null)
                .addAllocation("account-2", BigDecimal.ZERO)
                .addAllocation("account-3", new BigDecimal("5000"))
                .build();

            assertEquals(1, allocation.getAccountCount());
            assertEquals(new BigDecimal("5000"), allocation.totalAllocated());
        }

        @Test
        @DisplayName("Should ignore blank warnings")
        void ignoreBlankWarnings() {
            ContributionAllocation allocation = ContributionAllocation.builder()
                .addWarning(null)
                .addWarning("")
                .addWarning("  ")
                .addWarning("Valid warning")
                .build();

            assertEquals(1, allocation.warnings().size());
        }
    }

    @Nested
    @DisplayName("Immutability")
    class ImmutabilityTests {

        @Test
        @DisplayName("Allocations map should be unmodifiable")
        void allocationsUnmodifiable() {
            ContributionAllocation allocation = ContributionAllocation.builder()
                .addAllocation("account-1", new BigDecimal("5000"))
                .build();

            assertThrows(UnsupportedOperationException.class, () ->
                allocation.allocations().put("account-2", new BigDecimal("1000"))
            );
        }

        @Test
        @DisplayName("Warnings list should be unmodifiable")
        void warningsUnmodifiable() {
            ContributionAllocation allocation = ContributionAllocation.builder()
                .addWarning("Warning 1")
                .build();

            assertThrows(UnsupportedOperationException.class, () ->
                allocation.warnings().add("Warning 2")
            );
        }
    }
}
