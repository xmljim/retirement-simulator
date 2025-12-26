package io.github.xmljim.retirement.domain.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("FilingStatus Tests")
class FilingStatusTest {

    @Nested
    @DisplayName("Display Name Tests")
    class DisplayNameTests {

        @Test
        @DisplayName("Should return correct display names")
        void displayNames() {
            assertEquals("Single", FilingStatus.SINGLE.getDisplayName());
            assertEquals("Married Filing Jointly", FilingStatus.MARRIED_FILING_JOINTLY.getDisplayName());
            assertEquals("Married Filing Separately", FilingStatus.MARRIED_FILING_SEPARATELY.getDisplayName());
            assertEquals("Head of Household", FilingStatus.HEAD_OF_HOUSEHOLD.getDisplayName());
            assertEquals("Qualifying Surviving Spouse", FilingStatus.QUALIFYING_SURVIVING_SPOUSE.getDisplayName());
        }
    }

    @Nested
    @DisplayName("Threshold Mapping Tests")
    class ThresholdMappingTests {

        @Test
        @DisplayName("SINGLE uses single thresholds")
        void singleUsesSingleThresholds() {
            assertTrue(FilingStatus.SINGLE.usesSingleThresholds());
            assertFalse(FilingStatus.SINGLE.usesJointThresholds());
        }

        @Test
        @DisplayName("HEAD_OF_HOUSEHOLD uses single thresholds")
        void hohUsesSingleThresholds() {
            assertTrue(FilingStatus.HEAD_OF_HOUSEHOLD.usesSingleThresholds());
            assertFalse(FilingStatus.HEAD_OF_HOUSEHOLD.usesJointThresholds());
        }

        @Test
        @DisplayName("MARRIED_FILING_JOINTLY uses joint thresholds")
        void mfjUsesJointThresholds() {
            assertTrue(FilingStatus.MARRIED_FILING_JOINTLY.usesJointThresholds());
            assertFalse(FilingStatus.MARRIED_FILING_JOINTLY.usesSingleThresholds());
        }

        @Test
        @DisplayName("QUALIFYING_SURVIVING_SPOUSE uses joint thresholds")
        void qssUsesJointThresholds() {
            assertTrue(FilingStatus.QUALIFYING_SURVIVING_SPOUSE.usesJointThresholds());
            assertFalse(FilingStatus.QUALIFYING_SURVIVING_SPOUSE.usesSingleThresholds());
        }

        @Test
        @DisplayName("MARRIED_FILING_SEPARATELY uses neither")
        void mfsUsesNeither() {
            assertFalse(FilingStatus.MARRIED_FILING_SEPARATELY.usesSingleThresholds());
            assertFalse(FilingStatus.MARRIED_FILING_SEPARATELY.usesJointThresholds());
        }
    }

    @Nested
    @DisplayName("Special Restrictions Tests")
    class SpecialRestrictionsTests {

        @Test
        @DisplayName("Only MFS has special restrictions")
        void onlyMfsHasSpecialRestrictions() {
            assertTrue(FilingStatus.MARRIED_FILING_SEPARATELY.hasSpecialRestrictions());
            assertFalse(FilingStatus.SINGLE.hasSpecialRestrictions());
            assertFalse(FilingStatus.MARRIED_FILING_JOINTLY.hasSpecialRestrictions());
            assertFalse(FilingStatus.HEAD_OF_HOUSEHOLD.hasSpecialRestrictions());
            assertFalse(FilingStatus.QUALIFYING_SURVIVING_SPOUSE.hasSpecialRestrictions());
        }
    }
}
