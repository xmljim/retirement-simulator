package io.github.xmljim.retirement.simulation.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.ExpenseCategory;

@DisplayName("SimulationFlags")
class SimulationFlagsTest {

    @Nested
    @DisplayName("Initial Factory")
    class InitialFactory {

        @Test
        @DisplayName("initial should create default flags")
        void initialShouldCreateDefaultFlags() {
            SimulationFlags flags = SimulationFlags.initial();

            assertFalse(flags.survivorMode());
            assertTrue(flags.contingencyActive().isEmpty());
            assertFalse(flags.refillMode());
            assertTrue(flags.custom().isEmpty());
        }
    }

    @Nested
    @DisplayName("Survivor Mode")
    class SurvivorMode {

        @Test
        @DisplayName("withSurvivorMode should create new instance")
        void withSurvivorModeShouldCreateNewInstance() {
            SimulationFlags original = SimulationFlags.initial();
            SimulationFlags updated = original.withSurvivorMode(true);

            assertNotSame(original, updated);
            assertFalse(original.survivorMode());
            assertTrue(updated.survivorMode());
        }

        @Test
        @DisplayName("withSurvivorMode should return same if unchanged")
        void withSurvivorModeShouldReturnSameIfUnchanged() {
            SimulationFlags flags = SimulationFlags.initial();
            SimulationFlags same = flags.withSurvivorMode(false);

            assertSame(flags, same);
        }
    }

    @Nested
    @DisplayName("Contingency Active")
    class ContingencyActive {

        @Test
        @DisplayName("withContingencyActive should add category")
        void withContingencyActiveShouldAddCategory() {
            SimulationFlags flags = SimulationFlags.initial();
            SimulationFlags updated = flags.withContingencyActive(ExpenseCategory.LTC_CARE, true);

            assertFalse(flags.isContingencyActive(ExpenseCategory.LTC_CARE));
            assertTrue(updated.isContingencyActive(ExpenseCategory.LTC_CARE));
        }

        @Test
        @DisplayName("withContingencyActive should remove category")
        void withContingencyActiveShouldRemoveCategory() {
            SimulationFlags flags = SimulationFlags.initial()
                    .withContingencyActive(ExpenseCategory.LTC_CARE, true);

            SimulationFlags updated = flags.withContingencyActive(ExpenseCategory.LTC_CARE, false);

            assertTrue(flags.isContingencyActive(ExpenseCategory.LTC_CARE));
            assertFalse(updated.isContingencyActive(ExpenseCategory.LTC_CARE));
        }

        @Test
        @DisplayName("withContingencyActive should return same if unchanged")
        void withContingencyActiveShouldReturnSameIfUnchanged() {
            SimulationFlags flags = SimulationFlags.initial();
            SimulationFlags same = flags.withContingencyActive(ExpenseCategory.LTC_CARE, false);

            assertSame(flags, same);
        }

        @Test
        @DisplayName("hasActiveContingency should return true when active")
        void hasActiveContingencyShouldReturnTrueWhenActive() {
            SimulationFlags flags = SimulationFlags.initial()
                    .withContingencyActive(ExpenseCategory.HOME_REPAIRS, true);

            assertTrue(flags.hasActiveContingency());
        }

        @Test
        @DisplayName("should handle null category")
        void shouldHandleNullCategory() {
            SimulationFlags flags = SimulationFlags.initial();
            SimulationFlags same = flags.withContingencyActive(null, true);

            assertSame(flags, same);
            assertFalse(flags.isContingencyActive(null));
        }
    }

    @Nested
    @DisplayName("Refill Mode")
    class RefillMode {

        @Test
        @DisplayName("withRefillMode should create new instance")
        void withRefillModeShouldCreateNewInstance() {
            SimulationFlags original = SimulationFlags.initial();
            SimulationFlags updated = original.withRefillMode(true);

            assertNotSame(original, updated);
            assertFalse(original.refillMode());
            assertTrue(updated.refillMode());
        }

        @Test
        @DisplayName("withRefillMode should return same if unchanged")
        void withRefillModeShouldReturnSameIfUnchanged() {
            SimulationFlags flags = SimulationFlags.initial();
            SimulationFlags same = flags.withRefillMode(false);

            assertSame(flags, same);
        }
    }

    @Nested
    @DisplayName("Custom Flags")
    class CustomFlags {

        @Test
        @DisplayName("withCustomFlag should add flag")
        void withCustomFlagShouldAddFlag() {
            SimulationFlags flags = SimulationFlags.initial();
            SimulationFlags updated = flags.withCustomFlag("testKey", "testValue");

            assertNull(flags.getCustomFlag("testKey"));
            assertEquals("testValue", updated.getCustomFlag("testKey"));
        }

        @Test
        @DisplayName("withCustomFlag should remove flag when null value")
        void withCustomFlagShouldRemoveWhenNull() {
            SimulationFlags flags = SimulationFlags.initial()
                    .withCustomFlag("testKey", "testValue");

            SimulationFlags updated = flags.withCustomFlag("testKey", null);

            assertNotNull(flags.getCustomFlag("testKey"));
            assertNull(updated.getCustomFlag("testKey"));
        }

        @Test
        @DisplayName("getCustomFlag should return default when not set")
        void getCustomFlagShouldReturnDefault() {
            SimulationFlags flags = SimulationFlags.initial();

            assertEquals("default", flags.getCustomFlag("missing", "default"));
        }

        @Test
        @DisplayName("should handle null key")
        void shouldHandleNullKey() {
            SimulationFlags flags = SimulationFlags.initial();
            SimulationFlags same = flags.withCustomFlag(null, "value");

            assertSame(flags, same);
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("contingencyActive should be unmodifiable")
        void contingencyActiveShouldBeUnmodifiable() {
            SimulationFlags flags = SimulationFlags.initial()
                    .withContingencyActive(ExpenseCategory.LTC_CARE, true);

            assertThrows(UnsupportedOperationException.class,
                    () -> flags.contingencyActive().add(ExpenseCategory.TRAVEL));
        }

        @Test
        @DisplayName("custom map should be unmodifiable")
        void customMapShouldBeUnmodifiable() {
            SimulationFlags flags = SimulationFlags.initial()
                    .withCustomFlag("key", "value");

            assertThrows(UnsupportedOperationException.class,
                    () -> flags.custom().put("newKey", "newValue"));
        }
    }

    @Nested
    @DisplayName("Null Handling")
    class NullHandling {

        @Test
        @DisplayName("should handle null contingencyActive in constructor")
        void shouldHandleNullContingencyActive() {
            SimulationFlags flags = new SimulationFlags(false, null, false, null);

            assertNotNull(flags.contingencyActive());
            assertTrue(flags.contingencyActive().isEmpty());
        }

        @Test
        @DisplayName("should handle null custom in constructor")
        void shouldHandleNullCustom() {
            SimulationFlags flags = new SimulationFlags(false, null, false, null);

            assertNotNull(flags.custom());
            assertTrue(flags.custom().isEmpty());
        }
    }
}
