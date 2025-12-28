package io.github.xmljim.retirement.domain.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.ContingencyType;

@DisplayName("RandomExpenseEvent Tests")
class RandomExpenseEventTest {

    @Nested
    @DisplayName("Creation Tests")
    class CreationTests {

        @Test
        @DisplayName("Create with factory method")
        void createWithFactory() {
            RandomExpenseEvent event = RandomExpenseEvent.of(
                "HVAC Failure",
                ContingencyType.HOME_REPAIR,
                8000,
                15000,
                0.05
            );

            assertEquals("HVAC Failure", event.name());
            assertEquals(ContingencyType.HOME_REPAIR, event.type());
            assertEquals(0, new BigDecimal("8000").compareTo(event.minAmount()));
            assertEquals(0, new BigDecimal("15000").compareTo(event.maxAmount()));
            assertEquals(0.05, event.annualProbability(), 0.001);
        }

        @Test
        @DisplayName("Pre-configured HVAC event")
        void hvacEvent() {
            RandomExpenseEvent event = RandomExpenseEvent.hvacFailure();

            assertEquals(ContingencyType.HOME_REPAIR, event.type());
            assertEquals(0.05, event.annualProbability(), 0.001);
        }

        @Test
        @DisplayName("Pre-configured roof event")
        void roofEvent() {
            RandomExpenseEvent event = RandomExpenseEvent.roofRepair();

            assertEquals(ContingencyType.HOME_REPAIR, event.type());
            assertEquals(0.03, event.annualProbability(), 0.001);
        }
    }

    @Nested
    @DisplayName("Calculation Tests")
    class CalculationTests {

        @Test
        @DisplayName("Average amount calculation")
        void averageAmount() {
            RandomExpenseEvent event = RandomExpenseEvent.of(
                "Test",
                ContingencyType.HOME_REPAIR,
                8000,
                12000,
                0.10
            );

            assertEquals(0, new BigDecimal("10000.00").compareTo(event.getAverageAmount()));
        }

        @Test
        @DisplayName("Expected annual cost calculation")
        void expectedAnnualCost() {
            RandomExpenseEvent event = RandomExpenseEvent.of(
                "Test",
                ContingencyType.HOME_REPAIR,
                8000,
                12000,
                0.10
            );

            // Average: 10000, probability: 0.10 -> expected: 1000
            assertEquals(0, new BigDecimal("1000.00").compareTo(event.getExpectedAnnualCost()));
        }

        @Test
        @DisplayName("Probability percentage")
        void probabilityPercent() {
            RandomExpenseEvent event = RandomExpenseEvent.of(
                "Test",
                ContingencyType.HOME_REPAIR,
                8000,
                12000,
                0.05
            );

            assertEquals("5.0%", event.getProbabilityPercent());
        }
    }

    @Nested
    @DisplayName("Simulation Tests")
    class SimulationTests {

        @Test
        @DisplayName("generateAmount within range")
        void generateAmountWithinRange() {
            RandomExpenseEvent event = RandomExpenseEvent.of(
                "Test",
                ContingencyType.HOME_REPAIR,
                8000,
                15000,
                1.0
            );

            Random random = new Random(42);
            for (int i = 0; i < 100; i++) {
                BigDecimal amount = event.generateAmount(random);
                assertTrue(amount.compareTo(new BigDecimal("8000")) >= 0);
                assertTrue(amount.compareTo(new BigDecimal("15000")) <= 0);
            }
        }

        @Test
        @DisplayName("simulate returns zero or amount")
        void simulateReturnsZeroOrAmount() {
            RandomExpenseEvent event = RandomExpenseEvent.of(
                "Test",
                ContingencyType.HOME_REPAIR,
                8000,
                15000,
                0.50
            );

            Random random = new Random(42);
            int occurrences = 0;
            for (int i = 0; i < 100; i++) {
                BigDecimal result = event.simulate(random);
                if (result.compareTo(BigDecimal.ZERO) > 0) {
                    occurrences++;
                    assertTrue(result.compareTo(new BigDecimal("8000")) >= 0);
                    assertTrue(result.compareTo(new BigDecimal("15000")) <= 0);
                }
            }
            // With 50% probability, expect roughly 40-60 occurrences
            assertTrue(occurrences > 30 && occurrences < 70);
        }

        @Test
        @DisplayName("100% probability always occurs")
        void alwaysOccurs() {
            RandomExpenseEvent event = RandomExpenseEvent.of(
                "Test",
                ContingencyType.HOME_REPAIR,
                1000,
                1000,
                1.0
            );

            Random random = new Random(42);
            for (int i = 0; i < 10; i++) {
                assertTrue(event.occurs(random));
            }
        }

        @Test
        @DisplayName("0% probability never occurs")
        void neverOccurs() {
            RandomExpenseEvent event = RandomExpenseEvent.of(
                "Test",
                ContingencyType.HOME_REPAIR,
                1000,
                1000,
                0.0
            );

            Random random = new Random(42);
            for (int i = 0; i < 10; i++) {
                BigDecimal result = event.simulate(random);
                assertEquals(0, BigDecimal.ZERO.compareTo(result));
            }
        }
    }

    @Nested
    @DisplayName("Pre-configured Events Tests")
    class PreConfiguredTests {

        @Test
        @DisplayName("All pre-configured events are valid")
        void allPreConfiguredValid() {
            assertNotNull(RandomExpenseEvent.hvacFailure());
            assertNotNull(RandomExpenseEvent.roofRepair());
            assertNotNull(RandomExpenseEvent.majorPlumbing());
            assertNotNull(RandomExpenseEvent.applianceFailure());
        }
    }
}
