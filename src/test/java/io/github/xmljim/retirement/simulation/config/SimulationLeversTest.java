package io.github.xmljim.retirement.simulation.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SimulationLevers")
class SimulationLeversTest {

    @Nested
    @DisplayName("Default Factory")
    class DefaultFactory {

        @Test
        @DisplayName("withDefaults should create all default levers")
        void withDefaultsShouldCreateAllDefaultLevers() {
            SimulationLevers levers = SimulationLevers.withDefaults();

            assertNotNull(levers.economic());
            assertNotNull(levers.market());
            assertNotNull(levers.expense());
        }

        @Test
        @DisplayName("withDefaults should use deterministic mode")
        void withDefaultsShouldUseDeterministicMode() {
            SimulationLevers levers = SimulationLevers.withDefaults();

            assertEquals(SimulationMode.DETERMINISTIC, levers.mode());
            assertFalse(levers.isStochastic());
        }
    }

    @Nested
    @DisplayName("Null Handling")
    class NullHandling {

        @Test
        @DisplayName("should default null component levers")
        void shouldDefaultNullComponentLevers() {
            SimulationLevers levers = new SimulationLevers(null, null, null);

            assertNotNull(levers.economic());
            assertNotNull(levers.market());
            assertNotNull(levers.expense());
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should build with custom economic levers")
        void shouldBuildWithCustomEconomicLevers() {
            EconomicLevers custom = EconomicLevers.builder()
                    .generalInflationRate(new BigDecimal("0.03"))
                    .build();

            SimulationLevers levers = SimulationLevers.builder()
                    .economic(custom)
                    .build();

            assertEquals(new BigDecimal("0.03"),
                    levers.economic().generalInflationRate());
        }

        @Test
        @DisplayName("should build with custom market levers")
        void shouldBuildWithCustomMarketLevers() {
            MarketLevers custom = MarketLevers.monteCarlo(
                    new BigDecimal("0.08"),
                    new BigDecimal("0.20"));

            SimulationLevers levers = SimulationLevers.builder()
                    .market(custom)
                    .build();

            assertEquals(SimulationMode.MONTE_CARLO, levers.mode());
            assertTrue(levers.isStochastic());
            assertEquals(new BigDecimal("0.08"),
                    levers.market().expectedReturn());
        }

        @Test
        @DisplayName("should build with custom expense levers")
        void shouldBuildWithCustomExpenseLevers() {
            ExpenseLevers custom = ExpenseLevers.builder()
                    .healthcareTrend(new BigDecimal("0.03"))
                    .build();

            SimulationLevers levers = SimulationLevers.builder()
                    .expense(custom)
                    .build();

            assertEquals(new BigDecimal("0.03"),
                    levers.expense().healthcareTrend());
        }

        @Test
        @DisplayName("builder defaults should match withDefaults")
        void builderDefaultsShouldMatchWithDefaults() {
            SimulationLevers fromBuilder = SimulationLevers.builder().build();
            SimulationLevers fromFactory = SimulationLevers.withDefaults();

            assertEquals(fromFactory.economic().generalInflationRate(),
                    fromBuilder.economic().generalInflationRate());
            assertEquals(fromFactory.market().expectedReturn(),
                    fromBuilder.market().expectedReturn());
            assertEquals(fromFactory.expense().healthcareTrend(),
                    fromBuilder.expense().healthcareTrend());
        }
    }

    @Nested
    @DisplayName("Convenience Methods")
    class ConvenienceMethods {

        @Test
        @DisplayName("mode should delegate to market levers")
        void modeShouldDelegateToMarketLevers() {
            SimulationLevers levers = SimulationLevers.builder()
                    .market(MarketLevers.monteCarlo(
                            new BigDecimal("0.07"),
                            new BigDecimal("0.15")))
                    .build();

            assertEquals(SimulationMode.MONTE_CARLO, levers.mode());
        }

        @Test
        @DisplayName("isStochastic should delegate to market levers")
        void isStochasticShouldDelegateToMarketLevers() {
            SimulationLevers deterministic = SimulationLevers.builder()
                    .market(MarketLevers.deterministic(new BigDecimal("0.07")))
                    .build();

            SimulationLevers monteCarlo = SimulationLevers.builder()
                    .market(MarketLevers.monteCarlo(
                            new BigDecimal("0.07"),
                            new BigDecimal("0.15")))
                    .build();

            assertFalse(deterministic.isStochastic());
            assertTrue(monteCarlo.isStochastic());
        }
    }
}
