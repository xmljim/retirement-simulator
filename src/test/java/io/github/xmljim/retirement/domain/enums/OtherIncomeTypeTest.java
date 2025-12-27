package io.github.xmljim.retirement.domain.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("OtherIncomeType Tests")
class OtherIncomeTypeTest {

    @Nested
    @DisplayName("Display Name Tests")
    class DisplayNameTests {

        @Test
        @DisplayName("RENTAL should have correct display name")
        void rentalDisplayName() {
            assertEquals("Rental Income", OtherIncomeType.RENTAL.getDisplayName());
        }

        @Test
        @DisplayName("PART_TIME_WORK should have correct display name")
        void partTimeWorkDisplayName() {
            assertEquals("Part-Time Work", OtherIncomeType.PART_TIME_WORK.getDisplayName());
        }

        @Test
        @DisplayName("ROYALTIES should have correct display name")
        void royaltiesDisplayName() {
            assertEquals("Royalties", OtherIncomeType.ROYALTIES.getDisplayName());
        }

        @Test
        @DisplayName("DIVIDENDS should have correct display name")
        void dividendsDisplayName() {
            assertEquals("Dividends", OtherIncomeType.DIVIDENDS.getDisplayName());
        }

        @Test
        @DisplayName("BUSINESS should have correct display name")
        void businessDisplayName() {
            assertEquals("Business Income", OtherIncomeType.BUSINESS.getDisplayName());
        }

        @Test
        @DisplayName("OTHER should have correct display name")
        void otherDisplayName() {
            assertEquals("Other Income", OtherIncomeType.OTHER.getDisplayName());
        }
    }

    @Nested
    @DisplayName("Earned Income Tests")
    class EarnedIncomeTests {

        @Test
        @DisplayName("RENTAL should not be earned income (passive)")
        void rentalIsPassive() {
            assertFalse(OtherIncomeType.RENTAL.isEarnedIncome());
        }

        @Test
        @DisplayName("PART_TIME_WORK should be earned income")
        void partTimeWorkIsEarned() {
            assertTrue(OtherIncomeType.PART_TIME_WORK.isEarnedIncome());
        }

        @Test
        @DisplayName("ROYALTIES should not be earned income")
        void royaltiesNotEarned() {
            assertFalse(OtherIncomeType.ROYALTIES.isEarnedIncome());
        }

        @Test
        @DisplayName("DIVIDENDS should not be earned income")
        void dividendsNotEarned() {
            assertFalse(OtherIncomeType.DIVIDENDS.isEarnedIncome());
        }

        @Test
        @DisplayName("BUSINESS should be earned income")
        void businessIsEarned() {
            assertTrue(OtherIncomeType.BUSINESS.isEarnedIncome());
        }

        @Test
        @DisplayName("OTHER should not be earned income")
        void otherNotEarned() {
            assertFalse(OtherIncomeType.OTHER.isEarnedIncome());
        }
    }
}
