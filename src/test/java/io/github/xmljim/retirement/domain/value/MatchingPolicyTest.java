package io.github.xmljim.retirement.domain.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("MatchingPolicy Tests")
class MatchingPolicyTest {

    @Nested
    @DisplayName("SimpleMatchingPolicy")
    class SimpleMatchingPolicyTests {

        @Test
        @DisplayName("Should calculate 50% match up to 6%")
        void calculates50PercentMatchUpTo6Percent() {
            MatchingPolicy policy = MatchingPolicy.simple(0.50, 0.06);

            // Employee contributes 4% → 2% match
            BigDecimal result = policy.calculateEmployerMatch(new BigDecimal("0.04"));
            assertEquals(0, new BigDecimal("0.02").compareTo(result.stripTrailingZeros()));
        }

        @Test
        @DisplayName("Should cap match at maximum matchable percentage")
        void capsMatchAtMaximumMatchablePercentage() {
            MatchingPolicy policy = MatchingPolicy.simple(0.50, 0.06);

            // Employee contributes 10% → 3% match (capped at 6% * 50%)
            BigDecimal result = policy.calculateEmployerMatch(new BigDecimal("0.10"));
            assertEquals(0, new BigDecimal("0.03").compareTo(result.stripTrailingZeros()));
        }

        @Test
        @DisplayName("Should return zero for null contribution")
        void returnsZeroForNullContribution() {
            MatchingPolicy policy = MatchingPolicy.simple(0.50, 0.06);

            BigDecimal result = policy.calculateEmployerMatch(null);
            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        @DisplayName("Should return zero for zero contribution")
        void returnsZeroForZeroContribution() {
            MatchingPolicy policy = MatchingPolicy.simple(0.50, 0.06);

            BigDecimal result = policy.calculateEmployerMatch(BigDecimal.ZERO);
            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        @DisplayName("Should throw for negative match rate")
        void throwsForNegativeMatchRate() {
            assertThrows(IllegalArgumentException.class, () ->
                MatchingPolicy.simple(-0.50, 0.06));
        }

        @Test
        @DisplayName("Should return description")
        void returnsDescription() {
            MatchingPolicy policy = MatchingPolicy.simple(0.50, 0.06);

            assertNotNull(policy.getDescription());
            assertTrue(policy.getDescription().contains("50%"));
            assertTrue(policy.getDescription().contains("6%"));
        }
    }

    @Nested
    @DisplayName("TieredMatchingPolicy")
    class TieredMatchingPolicyTests {

        @Test
        @DisplayName("Should calculate tiered match: 100% on first 3%, 50% on next 2%")
        void calculatesTieredMatch() {
            // 100% match on first 3%, 50% match on contributions from 3% to 5%
            MatchingPolicy policy = MatchingPolicy.tiered(List.of(
                MatchTier.of(0.03, 1.0),
                MatchTier.of(0.05, 0.5)
            ));

            // Employee contributes 6%
            // Tier 1: 3% * 100% = 3%
            // Tier 2: 2% * 50% = 1%
            // Total: 4%
            BigDecimal result = policy.calculateEmployerMatch(new BigDecimal("0.06"));
            assertEquals(0, new BigDecimal("0.04").compareTo(result.stripTrailingZeros()));
        }

        @Test
        @DisplayName("Should handle contribution within first tier only")
        void handlesContributionWithinFirstTierOnly() {
            MatchingPolicy policy = MatchingPolicy.tiered(List.of(
                MatchTier.of(0.03, 1.0),
                MatchTier.of(0.05, 0.5)
            ));

            // Employee contributes 2% → 2% match (within first tier)
            BigDecimal result = policy.calculateEmployerMatch(new BigDecimal("0.02"));
            assertEquals(0, new BigDecimal("0.02").compareTo(result.stripTrailingZeros()));
        }

        @Test
        @DisplayName("Should handle contribution exactly at tier boundary")
        void handlesContributionExactlyAtTierBoundary() {
            MatchingPolicy policy = MatchingPolicy.tiered(List.of(
                MatchTier.of(0.03, 1.0),
                MatchTier.of(0.05, 0.5)
            ));

            // Employee contributes exactly 3% → 3% match
            BigDecimal result = policy.calculateEmployerMatch(new BigDecimal("0.03"));
            assertEquals(0, new BigDecimal("0.03").compareTo(result.stripTrailingZeros()));
        }

        @Test
        @DisplayName("Should return zero for null contribution")
        void returnsZeroForNullContribution() {
            MatchingPolicy policy = MatchingPolicy.tiered(List.of(
                MatchTier.of(0.03, 1.0)
            ));

            BigDecimal result = policy.calculateEmployerMatch(null);
            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        @DisplayName("Should throw for empty tier list")
        void throwsForEmptyTierList() {
            assertThrows(IllegalArgumentException.class, () ->
                MatchingPolicy.tiered(List.of()));
        }

        @Test
        @DisplayName("Should sort tiers by threshold")
        void sortsTiersByThreshold() {
            // Provide tiers out of order - should still work correctly
            MatchingPolicy policy = MatchingPolicy.tiered(List.of(
                MatchTier.of(0.05, 0.5),
                MatchTier.of(0.03, 1.0)
            ));

            // Should calculate correctly despite out-of-order input
            BigDecimal result = policy.calculateEmployerMatch(new BigDecimal("0.06"));
            assertEquals(0, new BigDecimal("0.04").compareTo(result.stripTrailingZeros()));
        }
    }

    @Nested
    @DisplayName("NoMatchingPolicy")
    class NoMatchingPolicyTests {

        @Test
        @DisplayName("Should return zero for any contribution")
        void returnsZeroForAnyContribution() {
            MatchingPolicy policy = MatchingPolicy.none();

            BigDecimal result = policy.calculateEmployerMatch(new BigDecimal("0.10"));
            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        @DisplayName("Should not allow ROTH match")
        void doesNotAllowRothMatch() {
            MatchingPolicy policy = MatchingPolicy.none();

            assertFalse(policy.allowsRothMatch());
        }
    }

    @Nested
    @DisplayName("MatchTier Record")
    class MatchTierTests {

        @Test
        @DisplayName("Should create valid tier")
        void createsValidTier() {
            MatchTier tier = MatchTier.of(0.03, 1.0);

            assertEquals(0, new BigDecimal("0.03").compareTo(tier.contributionThreshold()));
            assertEquals(0, new BigDecimal("1.0").compareTo(tier.matchRate()));
        }

        @Test
        @DisplayName("Should throw for null threshold")
        void throwsForNullThreshold() {
            assertThrows(NullPointerException.class, () ->
                new MatchTier(null, BigDecimal.ONE));
        }

        @Test
        @DisplayName("Should throw for negative threshold")
        void throwsForNegativeThreshold() {
            assertThrows(IllegalArgumentException.class, () ->
                MatchTier.of(-0.03, 1.0));
        }

        @Test
        @DisplayName("Should throw for negative match rate")
        void throwsForNegativeMatchRate() {
            assertThrows(IllegalArgumentException.class, () ->
                MatchTier.of(0.03, -1.0));
        }
    }
}
