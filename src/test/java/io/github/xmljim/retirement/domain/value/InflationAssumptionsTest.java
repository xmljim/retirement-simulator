package io.github.xmljim.retirement.domain.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("InflationAssumptions Tests")
class InflationAssumptionsTest {

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create defaults")
        void createDefaults() {
            InflationAssumptions defaults = InflationAssumptions.defaults();
            assertEquals(0, new BigDecimal("0.03").compareTo(defaults.getGeneralInflation()));
            assertEquals(0, new BigDecimal("0.05").compareTo(defaults.getHealthcareInflation()));
            assertEquals(0, new BigDecimal("0.025").compareTo(defaults.getHousingInflation()));
        }

        @Test
        @DisplayName("Should create uniform rate")
        void createUniform() {
            InflationAssumptions uniform = InflationAssumptions.uniform(0.04);
            assertEquals(0, new BigDecimal("0.04").compareTo(uniform.getGeneralInflation()));
            assertEquals(0, new BigDecimal("0.04").compareTo(uniform.getHealthcareInflation()));
            assertEquals(0, new BigDecimal("0.04").compareTo(uniform.getHousingInflation()));
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should build with custom rates")
        void buildCustomRates() {
            InflationAssumptions custom = InflationAssumptions.builder()
                .generalInflation(0.025)
                .healthcareInflation(0.06)
                .housingInflation(0.02)
                .build();

            assertEquals(0, new BigDecimal("0.025").compareTo(custom.getGeneralInflation()));
            assertEquals(0, new BigDecimal("0.06").compareTo(custom.getHealthcareInflation()));
            assertEquals(0, new BigDecimal("0.02").compareTo(custom.getHousingInflation()));
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should reject rate below -10%")
        void rateTooLow() {
            assertThrows(IllegalArgumentException.class, () ->
                InflationAssumptions.builder().generalInflation(-0.15).build());
        }

        @Test
        @DisplayName("Should reject rate above 20%")
        void rateTooHigh() {
            assertThrows(IllegalArgumentException.class, () ->
                InflationAssumptions.builder().healthcareInflation(0.25).build());
        }
    }

    @Nested
    @DisplayName("With Methods Tests")
    class WithMethodsTests {

        @Test
        @DisplayName("Should create copy with updated general inflation")
        void withGeneralInflation() {
            InflationAssumptions original = InflationAssumptions.defaults();
            InflationAssumptions updated = original.withGeneralInflation(0.04);

            assertEquals(0, new BigDecimal("0.04").compareTo(updated.getGeneralInflation()));
            assertEquals(original.getHealthcareInflation(), updated.getHealthcareInflation());
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsTests {

        @Test
        @DisplayName("Same values should be equal")
        void sameValuesEqual() {
            InflationAssumptions a1 = InflationAssumptions.builder()
                .generalInflation(0.03)
                .healthcareInflation(0.05)
                .housingInflation(0.025)
                .build();
            InflationAssumptions a2 = InflationAssumptions.defaults();

            assertEquals(a1, a2);
            assertEquals(a1.hashCode(), a2.hashCode());
        }

        @Test
        @DisplayName("Different values should not be equal")
        void differentValuesNotEqual() {
            InflationAssumptions a1 = InflationAssumptions.defaults();
            InflationAssumptions a2 = InflationAssumptions.uniform(0.04);
            assertNotEquals(a1, a2);
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should produce readable string")
        void toStringFormat() {
            InflationAssumptions assumptions = InflationAssumptions.defaults();
            String str = assumptions.toString();
            assertTrue(str.contains("general"));
            assertTrue(str.contains("healthcare"));
            assertTrue(str.contains("housing"));
        }
    }
}
