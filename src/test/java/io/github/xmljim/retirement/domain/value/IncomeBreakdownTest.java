package io.github.xmljim.retirement.domain.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("IncomeBreakdown Tests")
class IncomeBreakdownTest {

    private static final LocalDate TEST_DATE = LocalDate.of(2025, 6, 15);

    @Nested
    @DisplayName("Empty Breakdown Tests")
    class EmptyBreakdownTests {
        @Test
        @DisplayName("Empty breakdown should have no income")
        void emptyHasNoIncome() {
            IncomeBreakdown breakdown = IncomeBreakdown.empty(TEST_DATE);
            assertFalse(breakdown.hasIncome());
            assertEquals(TEST_DATE, breakdown.asOfDate());
            assertEquals(0, BigDecimal.ZERO.compareTo(breakdown.total()));
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {
        @Test
        @DisplayName("Should build with all fields")
        void buildWithAllFields() {
            IncomeBreakdown breakdown = IncomeBreakdown.builder()
                .asOfDate(TEST_DATE)
                .salary(new BigDecimal("5000"))
                .socialSecurity(new BigDecimal("2000"))
                .pension(new BigDecimal("1500"))
                .annuity(new BigDecimal("500"))
                .other(new BigDecimal("300"))
                .earnedIncome(new BigDecimal("5000"))
                .passiveIncome(new BigDecimal("4300"))
                .build();

            assertEquals(0, new BigDecimal("5000").compareTo(breakdown.salary()));
            assertEquals(0, new BigDecimal("2000").compareTo(breakdown.socialSecurity()));
            assertEquals(0, new BigDecimal("1500").compareTo(breakdown.pension()));
            assertEquals(0, new BigDecimal("500").compareTo(breakdown.annuity()));
            assertEquals(0, new BigDecimal("300").compareTo(breakdown.other()));
            assertEquals(0, new BigDecimal("9300").compareTo(breakdown.total()));
        }

        @Test
        @DisplayName("Should handle null amounts")
        void handleNullAmounts() {
            IncomeBreakdown breakdown = IncomeBreakdown.builder()
                .asOfDate(TEST_DATE)
                .salary(null)
                .socialSecurity(null)
                .pension(null)
                .annuity(null)
                .other(null)
                .earnedIncome(null)
                .passiveIncome(null)
                .build();

            assertEquals(0, BigDecimal.ZERO.compareTo(breakdown.total()));
        }

        @Test
        @DisplayName("Should accumulate with add methods")
        void accumulateWithAddMethods() {
            IncomeBreakdown breakdown = IncomeBreakdown.builder()
                .asOfDate(TEST_DATE)
                .addToSalary(new BigDecimal("1000"))
                .addToSalary(new BigDecimal("2000"))
                .addToSocialSecurity(new BigDecimal("500"))
                .addToPension(new BigDecimal("300"))
                .addToAnnuity(new BigDecimal("200"))
                .addToOther(new BigDecimal("100"))
                .addToEarnedIncome(new BigDecimal("3000"))
                .addToPassiveIncome(new BigDecimal("1100"))
                .build();

            assertEquals(0, new BigDecimal("3000").compareTo(breakdown.salary()));
            assertEquals(0, new BigDecimal("500").compareTo(breakdown.socialSecurity()));
            assertEquals(0, new BigDecimal("4100").compareTo(breakdown.total()));
        }

        @Test
        @DisplayName("Should handle null in add methods")
        void handleNullInAddMethods() {
            IncomeBreakdown breakdown = IncomeBreakdown.builder()
                .asOfDate(TEST_DATE)
                .addToSalary(null)
                .addToSocialSecurity(null)
                .addToPension(null)
                .addToAnnuity(null)
                .addToOther(null)
                .addToEarnedIncome(null)
                .addToPassiveIncome(null)
                .build();

            assertEquals(0, BigDecimal.ZERO.compareTo(breakdown.total()));
        }
    }

    @Nested
    @DisplayName("ToAnnual Tests")
    class ToAnnualTests {
        @Test
        @DisplayName("Should convert monthly to annual")
        void convertToAnnual() {
            IncomeBreakdown monthly = IncomeBreakdown.builder()
                .asOfDate(TEST_DATE)
                .salary(new BigDecimal("5000"))
                .pension(new BigDecimal("1000"))
                .build();

            IncomeBreakdown annual = monthly.toAnnual();

            assertEquals(0, new BigDecimal("60000").compareTo(annual.salary()));
            assertEquals(0, new BigDecimal("12000").compareTo(annual.pension()));
            assertEquals(0, new BigDecimal("72000").compareTo(annual.total()));
        }
    }

    @Nested
    @DisplayName("HasIncome Tests")
    class HasIncomeTests {
        @Test
        @DisplayName("Should return true when has income")
        void hasIncomeReturnsTrue() {
            IncomeBreakdown breakdown = IncomeBreakdown.builder()
                .asOfDate(TEST_DATE)
                .salary(new BigDecimal("1"))
                .build();

            assertTrue(breakdown.hasIncome());
        }

        @Test
        @DisplayName("Should return false when no income")
        void hasIncomeReturnsFalse() {
            IncomeBreakdown breakdown = IncomeBreakdown.empty(TEST_DATE);
            assertFalse(breakdown.hasIncome());
        }
    }

    @Nested
    @DisplayName("Record Compact Constructor Tests")
    class CompactConstructorTests {
        @Test
        @DisplayName("Should handle null values in constructor")
        void handleNullValues() {
            IncomeBreakdown breakdown = new IncomeBreakdown(
                TEST_DATE, null, null, null, null, null, null, null, null);

            assertEquals(0, BigDecimal.ZERO.compareTo(breakdown.salary()));
            assertEquals(0, BigDecimal.ZERO.compareTo(breakdown.total()));
        }
    }
}
