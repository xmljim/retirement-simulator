package io.github.xmljim.retirement.simulation.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("SimulationMode")
class SimulationModeTest {

    @Nested
    @DisplayName("Enum Values")
    class EnumValues {

        @Test
        @DisplayName("should have exactly 3 modes")
        void shouldHaveThreeModes() {
            assertEquals(3, SimulationMode.values().length);
        }

        @ParameterizedTest
        @EnumSource(SimulationMode.class)
        @DisplayName("all modes should have display names")
        void allModesShouldHaveDisplayNames(SimulationMode mode) {
            assertNotNull(mode.getDisplayName());
            assertFalse(mode.getDisplayName().isBlank());
        }

        @ParameterizedTest
        @EnumSource(SimulationMode.class)
        @DisplayName("all modes should have descriptions")
        void allModesShouldHaveDescriptions(SimulationMode mode) {
            assertNotNull(mode.getDescription());
            assertFalse(mode.getDescription().isBlank());
        }
    }

    @Nested
    @DisplayName("Mode Properties")
    class ModeProperties {

        @Test
        @DisplayName("DETERMINISTIC should be fixed")
        void deterministicShouldBeFixed() {
            assertTrue(SimulationMode.DETERMINISTIC.isFixed());
            assertFalse(SimulationMode.DETERMINISTIC.isStochastic());
            assertFalse(SimulationMode.DETERMINISTIC.usesHistoricalData());
        }

        @Test
        @DisplayName("MONTE_CARLO should be stochastic")
        void monteCarloShouldBeStochastic() {
            assertFalse(SimulationMode.MONTE_CARLO.isFixed());
            assertTrue(SimulationMode.MONTE_CARLO.isStochastic());
            assertFalse(SimulationMode.MONTE_CARLO.usesHistoricalData());
        }

        @Test
        @DisplayName("HISTORICAL should use historical data")
        void historicalShouldUseHistoricalData() {
            assertFalse(SimulationMode.HISTORICAL.isFixed());
            assertFalse(SimulationMode.HISTORICAL.isStochastic());
            assertTrue(SimulationMode.HISTORICAL.usesHistoricalData());
        }
    }
}
