package io.github.xmljim.retirement.domain.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Scenario Enum Tests")
class ScenarioEnumsTest {

    @Nested
    @DisplayName("SimulationMode Tests")
    class SimulationModeTests {

        @ParameterizedTest
        @EnumSource(SimulationMode.class)
        @DisplayName("All modes should have display name and description")
        void allModesHaveProperties(SimulationMode mode) {
            assertNotNull(mode.getDisplayName());
            assertNotNull(mode.getDescription());
            assertFalse(mode.getDisplayName().isEmpty());
            assertFalse(mode.getDescription().isEmpty());
        }

        @Test
        @DisplayName("Only DETERMINISTIC should be implemented")
        void onlyDeterministicImplemented() {
            assertTrue(SimulationMode.DETERMINISTIC.isImplemented());
            assertFalse(SimulationMode.MONTE_CARLO.isImplemented());
            assertFalse(SimulationMode.HISTORICAL.isImplemented());
        }

        @Test
        @DisplayName("DETERMINISTIC should have correct properties")
        void deterministicProperties() {
            SimulationMode mode = SimulationMode.DETERMINISTIC;
            assertEquals("Deterministic", mode.getDisplayName());
            assertEquals("Fixed return rates", mode.getDescription());
        }
    }

    @Nested
    @DisplayName("EndCondition Tests")
    class EndConditionTests {

        @ParameterizedTest
        @EnumSource(EndCondition.class)
        @DisplayName("All conditions should have display name and description")
        void allConditionsHaveProperties(EndCondition condition) {
            assertNotNull(condition.getDisplayName());
            assertNotNull(condition.getDescription());
            assertFalse(condition.getDisplayName().isEmpty());
            assertFalse(condition.getDescription().isEmpty());
        }

        @Test
        @DisplayName("LIFE_EXPECTANCY should have correct properties")
        void lifeExpectancyProperties() {
            EndCondition condition = EndCondition.LIFE_EXPECTANCY;
            assertEquals("Life Expectancy", condition.getDisplayName());
        }

        @Test
        @DisplayName("PORTFOLIO_DEPLETION should have correct properties")
        void portfolioDepletionProperties() {
            EndCondition condition = EndCondition.PORTFOLIO_DEPLETION;
            assertEquals("Portfolio Depletion", condition.getDisplayName());
        }

        @Test
        @DisplayName("FIRST_OF_BOTH should have correct properties")
        void firstOfBothProperties() {
            EndCondition condition = EndCondition.FIRST_OF_BOTH;
            assertEquals("First of Both", condition.getDisplayName());
        }
    }

    @Nested
    @DisplayName("DistributionStrategy Tests")
    class DistributionStrategyTests {

        @ParameterizedTest
        @EnumSource(DistributionStrategy.class)
        @DisplayName("All strategies should have display name and description")
        void allStrategiesHaveProperties(DistributionStrategy strategy) {
            assertNotNull(strategy.getDisplayName());
            assertNotNull(strategy.getDescription());
            assertFalse(strategy.getDisplayName().isEmpty());
            assertFalse(strategy.getDescription().isEmpty());
        }

        @Test
        @DisplayName("TAX_EFFICIENT and PRO_RATA should be implemented")
        void implementedStrategies() {
            assertTrue(DistributionStrategy.TAX_EFFICIENT.isImplemented());
            assertTrue(DistributionStrategy.PRO_RATA.isImplemented());
            assertFalse(DistributionStrategy.CUSTOM.isImplemented());
            assertFalse(DistributionStrategy.ROTH_CONVERSION_OPTIMIZER.isImplemented());
        }

        @Test
        @DisplayName("TAX_EFFICIENT should have correct properties")
        void taxEfficientProperties() {
            DistributionStrategy strategy = DistributionStrategy.TAX_EFFICIENT;
            assertEquals("Tax Efficient", strategy.getDisplayName());
            assertEquals("Optimizes for tax efficiency", strategy.getDescription());
        }
    }
}
