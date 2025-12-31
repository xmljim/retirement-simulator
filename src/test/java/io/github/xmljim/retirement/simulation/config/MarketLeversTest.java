package io.github.xmljim.retirement.simulation.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.exception.ValidationException;

@DisplayName("MarketLevers")
class MarketLeversTest {

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("deterministic should create fixed rate levers")
        void deterministicShouldCreateFixedRateLevers() {
            MarketLevers levers = MarketLevers.deterministic(new BigDecimal("0.06"));

            assertEquals(SimulationMode.DETERMINISTIC, levers.mode());
            assertEquals(new BigDecimal("0.06"), levers.expectedReturn());
            assertTrue(levers.mode().isFixed());
            assertFalse(levers.isStochastic());
        }

        @Test
        @DisplayName("monteCarlo should create stochastic levers")
        void monteCarloShouldCreateStochasticLevers() {
            MarketLevers levers = MarketLevers.monteCarlo(
                    new BigDecimal("0.07"),
                    new BigDecimal("0.15"));

            assertEquals(SimulationMode.MONTE_CARLO, levers.mode());
            assertEquals(new BigDecimal("0.07"), levers.expectedReturn());
            assertEquals(new BigDecimal("0.15"), levers.returnStdDev());
            assertTrue(levers.isStochastic());
        }

        @Test
        @DisplayName("historical should create historical levers")
        void historicalShouldCreateHistoricalLevers() {
            List<BigDecimal> returns = List.of(
                    new BigDecimal("0.10"),
                    new BigDecimal("-0.05"),
                    new BigDecimal("0.15"));

            MarketLevers levers = MarketLevers.historical(returns);

            assertEquals(SimulationMode.HISTORICAL, levers.mode());
            assertEquals(returns, levers.historicalReturns());
            assertTrue(levers.usesHistoricalData());
        }

        @Test
        @DisplayName("historical should reject empty returns")
        void historicalShouldRejectEmptyReturns() {
            assertThrows(ValidationException.class, () ->
                    MarketLevers.historical(Collections.emptyList()));
        }

        @Test
        @DisplayName("withDefaults should create deterministic 7%")
        void withDefaultsShouldCreateDeterministicSeven() {
            MarketLevers levers = MarketLevers.withDefaults();

            assertEquals(SimulationMode.DETERMINISTIC, levers.mode());
            assertEquals(new BigDecimal("0.07"), levers.expectedReturn());
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("historicalReturns should be unmodifiable")
        void historicalReturnsShouldBeUnmodifiable() {
            List<BigDecimal> returns = List.of(new BigDecimal("0.10"));
            MarketLevers levers = MarketLevers.historical(returns);

            assertThrows(UnsupportedOperationException.class, () ->
                    levers.historicalReturns().add(new BigDecimal("0.05")));
        }
    }

    @Nested
    @DisplayName("Null Handling")
    class NullHandling {

        @Test
        @DisplayName("should default null mode to DETERMINISTIC")
        void shouldDefaultNullMode() {
            MarketLevers levers = new MarketLevers(null, null, null, null);

            assertEquals(SimulationMode.DETERMINISTIC, levers.mode());
            assertEquals(MarketLevers.DEFAULT_EXPECTED_RETURN, levers.expectedReturn());
            assertEquals(MarketLevers.DEFAULT_RETURN_STD_DEV, levers.returnStdDev());
            assertTrue(levers.historicalReturns().isEmpty());
        }
    }
}
