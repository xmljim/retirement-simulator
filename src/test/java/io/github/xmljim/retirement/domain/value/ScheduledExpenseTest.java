package io.github.xmljim.retirement.domain.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.ContingencyType;

@DisplayName("ScheduledExpense Tests")
class ScheduledExpenseTest {

    @Nested
    @DisplayName("Creation Tests")
    class CreationTests {

        @Test
        @DisplayName("Create with last occurrence")
        void createWithLastOccurrence() {
            LocalDate lastPurchase = LocalDate.of(2020, 1, 1);
            ScheduledExpense expense = ScheduledExpense.of(
                "Vehicle",
                ContingencyType.VEHICLE_REPLACEMENT,
                new BigDecimal("40000"),
                8,
                lastPurchase
            );

            assertEquals("Vehicle", expense.name());
            assertEquals(ContingencyType.VEHICLE_REPLACEMENT, expense.type());
            assertEquals(0, new BigDecimal("40000").compareTo(expense.amount()));
            assertEquals(8, expense.intervalYears());
            assertEquals(lastPurchase, expense.lastOccurrence());
            assertEquals(LocalDate.of(2028, 1, 1), expense.nextOccurrence());
        }

        @Test
        @DisplayName("Create starting now")
        void createStartingNow() {
            ScheduledExpense expense = ScheduledExpense.startingNow(
                "Vehicle",
                ContingencyType.VEHICLE_REPLACEMENT,
                new BigDecimal("40000"),
                8
            );

            assertEquals(LocalDate.now(), expense.lastOccurrence());
            assertEquals(LocalDate.now().plusYears(8), expense.nextOccurrence());
        }
    }

    @Nested
    @DisplayName("Set-Aside Tests")
    class SetAsideTests {

        @Test
        @DisplayName("Monthly set-aside calculation")
        void monthlySetAside() {
            ScheduledExpense expense = ScheduledExpense.of(
                "Vehicle",
                ContingencyType.VEHICLE_REPLACEMENT,
                new BigDecimal("48000"),
                8,
                LocalDate.now()
            );

            // 48000 / (8 * 12) = 500
            assertEquals(0, new BigDecimal("500.00").compareTo(expense.getMonthlySetAside()));
        }

        @Test
        @DisplayName("Annual set-aside calculation")
        void annualSetAside() {
            ScheduledExpense expense = ScheduledExpense.of(
                "Vehicle",
                ContingencyType.VEHICLE_REPLACEMENT,
                new BigDecimal("40000"),
                8,
                LocalDate.now()
            );

            // 40000 / 8 = 5000
            assertEquals(0, new BigDecimal("5000.00").compareTo(expense.getAnnualSetAside()));
        }
    }

    @Nested
    @DisplayName("Due Date Tests")
    class DueDateTests {

        @Test
        @DisplayName("isDueWithin date range")
        void isDueWithinRange() {
            ScheduledExpense expense = ScheduledExpense.of(
                "Vehicle",
                ContingencyType.VEHICLE_REPLACEMENT,
                new BigDecimal("40000"),
                8,
                LocalDate.of(2020, 6, 15)
            );
            // Next occurrence: 2028-06-15

            assertTrue(expense.isDueWithin(LocalDate.of(2028, 1, 1), LocalDate.of(2028, 12, 31)));
            assertFalse(expense.isDueWithin(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31)));
        }

        @Test
        @DisplayName("isDueInYear")
        void isDueInYear() {
            ScheduledExpense expense = ScheduledExpense.of(
                "Vehicle",
                ContingencyType.VEHICLE_REPLACEMENT,
                new BigDecimal("40000"),
                8,
                LocalDate.of(2020, 6, 15)
            );

            assertTrue(expense.isDueInYear(2028));
            assertFalse(expense.isDueInYear(2027));
        }
    }

    @Nested
    @DisplayName("After Occurrence Tests")
    class AfterOccurrenceTests {

        @Test
        @DisplayName("afterOccurrence updates dates")
        void afterOccurrence() {
            ScheduledExpense expense = ScheduledExpense.of(
                "Vehicle",
                ContingencyType.VEHICLE_REPLACEMENT,
                new BigDecimal("40000"),
                8,
                LocalDate.of(2020, 1, 1)
            );

            ScheduledExpense updated = expense.afterOccurrence();

            assertEquals(LocalDate.of(2028, 1, 1), updated.lastOccurrence());
            assertEquals(LocalDate.of(2036, 1, 1), updated.nextOccurrence());
        }

        @Test
        @DisplayName("withAmount updates amount")
        void withAmount() {
            ScheduledExpense expense = ScheduledExpense.of(
                "Vehicle",
                ContingencyType.VEHICLE_REPLACEMENT,
                new BigDecimal("40000"),
                8,
                LocalDate.now()
            );

            ScheduledExpense updated = expense.withAmount(new BigDecimal("45000"));

            assertEquals(0, new BigDecimal("45000").compareTo(updated.amount()));
        }
    }
}
