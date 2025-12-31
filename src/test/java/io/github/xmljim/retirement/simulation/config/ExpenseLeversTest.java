package io.github.xmljim.retirement.simulation.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.ExpenseCategory.InflationType;

@DisplayName("ExpenseLevers")
class ExpenseLeversTest {

    @Nested
    @DisplayName("Default Factory")
    class DefaultFactory {

        @Test
        @DisplayName("withDefaults should use standard assumptions")
        void withDefaultsShouldUseStandardAssumptions() {
            ExpenseLevers levers = ExpenseLevers.withDefaults();

            assertEquals(ExpenseLevers.DEFAULT_GENERAL_INFLATION,
                    levers.getInflationRate(InflationType.GENERAL));
            assertEquals(ExpenseLevers.DEFAULT_HEALTHCARE_INFLATION,
                    levers.getInflationRate(InflationType.HEALTHCARE));
            assertEquals(ExpenseLevers.DEFAULT_HOUSING_INFLATION,
                    levers.getInflationRate(InflationType.HOUSING));
            assertEquals(ExpenseLevers.DEFAULT_LTC_INFLATION,
                    levers.getInflationRate(InflationType.LTC));
            assertEquals(BigDecimal.ZERO,
                    levers.getInflationRate(InflationType.NONE));
            assertEquals(ExpenseLevers.DEFAULT_HEALTHCARE_TREND,
                    levers.healthcareTrend());
        }

        @Test
        @DisplayName("withDefaults should provide identity modifiers")
        void withDefaultsShouldProvideIdentityModifiers() {
            ExpenseLevers levers = ExpenseLevers.withDefaults();

            assertNotNull(levers.discretionaryModifier());
            assertNotNull(levers.essentialsModifier());
            assertNotNull(levers.healthcareModifier());
            assertNotNull(levers.survivorModifier());
        }
    }

    @Nested
    @DisplayName("Inflation Rates")
    class InflationRates {

        @Test
        @DisplayName("getInflationRate should return ZERO for NONE type")
        void getInflationRateShouldReturnZeroForNone() {
            ExpenseLevers levers = ExpenseLevers.withDefaults();

            assertEquals(BigDecimal.ZERO, levers.getInflationRate(InflationType.NONE));
        }

        @Test
        @DisplayName("getInflationRate should return default for unknown type")
        void getInflationRateShouldReturnDefaultForUnknown() {
            ExpenseLevers levers = ExpenseLevers.builder()
                    .inflationRates(Map.of())
                    .build();

            // Should fall back to default general inflation
            assertEquals(ExpenseLevers.DEFAULT_GENERAL_INFLATION,
                    levers.getInflationRate(InflationType.GENERAL));
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should build with custom inflation rates")
        void shouldBuildWithCustomInflationRates() {
            ExpenseLevers levers = ExpenseLevers.builder()
                    .inflationRate(InflationType.GENERAL, new BigDecimal("0.03"))
                    .inflationRate(InflationType.HEALTHCARE, new BigDecimal("0.06"))
                    .build();

            assertEquals(new BigDecimal("0.03"),
                    levers.getInflationRate(InflationType.GENERAL));
            assertEquals(new BigDecimal("0.06"),
                    levers.getInflationRate(InflationType.HEALTHCARE));
        }

        @Test
        @DisplayName("should build with custom healthcare trend")
        void shouldBuildWithCustomHealthcareTrend() {
            ExpenseLevers levers = ExpenseLevers.builder()
                    .healthcareTrend(new BigDecimal("0.03"))
                    .build();

            assertEquals(new BigDecimal("0.03"), levers.healthcareTrend());
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("inflationRates map should be unmodifiable")
        void inflationRatesMapShouldBeUnmodifiable() {
            ExpenseLevers levers = ExpenseLevers.withDefaults();

            assertThrows(UnsupportedOperationException.class, () ->
                    levers.inflationRates().put(InflationType.GENERAL, BigDecimal.ONE));
        }
    }

    @Nested
    @DisplayName("Null Handling")
    class NullHandling {

        @Test
        @DisplayName("should default null inflation rates")
        void shouldDefaultNullInflationRates() {
            ExpenseLevers levers = new ExpenseLevers(null, null, null, null, null, null);

            assertNotNull(levers.inflationRates());
            assertEquals(ExpenseLevers.DEFAULT_HEALTHCARE_TREND, levers.healthcareTrend());
            assertNotNull(levers.discretionaryModifier());
        }
    }
}
