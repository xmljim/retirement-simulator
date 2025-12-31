package io.github.xmljim.retirement.simulation.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("EconomicLevers")
class EconomicLeversTest {

    @Nested
    @DisplayName("Default Factory")
    class DefaultFactory {

        @Test
        @DisplayName("withDefaults should use standard assumptions")
        void withDefaultsShouldUseStandardAssumptions() {
            EconomicLevers levers = EconomicLevers.withDefaults();

            assertEquals(new BigDecimal("0.025"), levers.generalInflationRate());
            assertEquals(new BigDecimal("0.03"), levers.wageGrowthRate());
            assertEquals(new BigDecimal("0.04"), levers.interestRate());
        }
    }

    @Nested
    @DisplayName("Null Handling")
    class NullHandling {

        @Test
        @DisplayName("should default null values")
        void shouldDefaultNullValues() {
            EconomicLevers levers = new EconomicLevers(null, null, null);

            assertEquals(EconomicLevers.DEFAULT_INFLATION_RATE, levers.generalInflationRate());
            assertEquals(EconomicLevers.DEFAULT_WAGE_GROWTH_RATE, levers.wageGrowthRate());
            assertEquals(EconomicLevers.DEFAULT_INTEREST_RATE, levers.interestRate());
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should build with custom values")
        void shouldBuildWithCustomValues() {
            EconomicLevers levers = EconomicLevers.builder()
                    .generalInflationRate(new BigDecimal("0.03"))
                    .wageGrowthRate(new BigDecimal("0.04"))
                    .interestRate(new BigDecimal("0.05"))
                    .build();

            assertEquals(new BigDecimal("0.03"), levers.generalInflationRate());
            assertEquals(new BigDecimal("0.04"), levers.wageGrowthRate());
            assertEquals(new BigDecimal("0.05"), levers.interestRate());
        }

        @Test
        @DisplayName("builder should use defaults when not set")
        void builderShouldUseDefaults() {
            EconomicLevers levers = EconomicLevers.builder().build();

            assertEquals(EconomicLevers.DEFAULT_INFLATION_RATE, levers.generalInflationRate());
            assertEquals(EconomicLevers.DEFAULT_WAGE_GROWTH_RATE, levers.wageGrowthRate());
            assertEquals(EconomicLevers.DEFAULT_INTEREST_RATE, levers.interestRate());
        }
    }
}
