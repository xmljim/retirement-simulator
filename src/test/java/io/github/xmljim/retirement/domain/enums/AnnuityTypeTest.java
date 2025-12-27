package io.github.xmljim.retirement.domain.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AnnuityType Tests")
class AnnuityTypeTest {

    @Nested
    @DisplayName("Display Name Tests")
    class DisplayNameTests {

        @Test
        @DisplayName("Fixed Immediate should have correct display name")
        void fixedImmediateDisplayName() {
            assertEquals("Fixed Immediate", AnnuityType.FIXED_IMMEDIATE.getDisplayName());
        }

        @Test
        @DisplayName("Fixed Deferred should have correct display name")
        void fixedDeferredDisplayName() {
            assertEquals("Fixed Deferred", AnnuityType.FIXED_DEFERRED.getDisplayName());
        }

        @Test
        @DisplayName("Variable should have correct display name")
        void variableDisplayName() {
            assertEquals("Variable", AnnuityType.VARIABLE.getDisplayName());
        }

        @Test
        @DisplayName("Indexed should have correct display name")
        void indexedDisplayName() {
            assertEquals("Indexed", AnnuityType.INDEXED.getDisplayName());
        }
    }

    @Nested
    @DisplayName("Fixed Payment Tests")
    class FixedPaymentTests {

        @Test
        @DisplayName("Fixed Immediate should have fixed payments")
        void fixedImmediateIsFixed() {
            assertTrue(AnnuityType.FIXED_IMMEDIATE.isFixedPayment());
        }

        @Test
        @DisplayName("Fixed Deferred should have fixed payments")
        void fixedDeferredIsFixed() {
            assertTrue(AnnuityType.FIXED_DEFERRED.isFixedPayment());
        }

        @Test
        @DisplayName("Variable should not have fixed payments")
        void variableIsNotFixed() {
            assertFalse(AnnuityType.VARIABLE.isFixedPayment());
        }

        @Test
        @DisplayName("Indexed should not have fixed payments")
        void indexedIsNotFixed() {
            assertFalse(AnnuityType.INDEXED.isFixedPayment());
        }
    }

    @Nested
    @DisplayName("Immediate Payment Tests")
    class ImmediatePaymentTests {

        @Test
        @DisplayName("Fixed Immediate should be immediate")
        void fixedImmediateIsImmediate() {
            assertTrue(AnnuityType.FIXED_IMMEDIATE.isImmediate());
        }

        @Test
        @DisplayName("Fixed Deferred should not be immediate")
        void fixedDeferredIsNotImmediate() {
            assertFalse(AnnuityType.FIXED_DEFERRED.isImmediate());
        }

        @Test
        @DisplayName("Variable should not be immediate")
        void variableIsNotImmediate() {
            assertFalse(AnnuityType.VARIABLE.isImmediate());
        }

        @Test
        @DisplayName("Indexed should not be immediate")
        void indexedIsNotImmediate() {
            assertFalse(AnnuityType.INDEXED.isImmediate());
        }
    }

    @Nested
    @DisplayName("Accumulation Phase Tests")
    class AccumulationPhaseTests {

        @Test
        @DisplayName("Fixed Immediate should not have accumulation phase")
        void fixedImmediateNoAccumulation() {
            assertFalse(AnnuityType.FIXED_IMMEDIATE.hasAccumulationPhase());
        }

        @Test
        @DisplayName("Fixed Deferred should have accumulation phase")
        void fixedDeferredHasAccumulation() {
            assertTrue(AnnuityType.FIXED_DEFERRED.hasAccumulationPhase());
        }

        @Test
        @DisplayName("Variable should have accumulation phase")
        void variableHasAccumulation() {
            assertTrue(AnnuityType.VARIABLE.hasAccumulationPhase());
        }

        @Test
        @DisplayName("Indexed should have accumulation phase")
        void indexedHasAccumulation() {
            assertTrue(AnnuityType.INDEXED.hasAccumulationPhase());
        }
    }

    @Nested
    @DisplayName("Market Exposure Tests")
    class MarketExposureTests {

        @Test
        @DisplayName("Fixed Immediate should not have market exposure")
        void fixedImmediateNoMarketExposure() {
            assertFalse(AnnuityType.FIXED_IMMEDIATE.hasMarketExposure());
        }

        @Test
        @DisplayName("Fixed Deferred should not have market exposure")
        void fixedDeferredNoMarketExposure() {
            assertFalse(AnnuityType.FIXED_DEFERRED.hasMarketExposure());
        }

        @Test
        @DisplayName("Variable should have market exposure")
        void variableHasMarketExposure() {
            assertTrue(AnnuityType.VARIABLE.hasMarketExposure());
        }

        @Test
        @DisplayName("Indexed should have market exposure")
        void indexedHasMarketExposure() {
            assertTrue(AnnuityType.INDEXED.hasMarketExposure());
        }
    }
}
