package io.github.xmljim.retirement.domain.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GuardrailsConfiguration Tests")
class GuardrailsConfigurationTest {

    @Nested
    @DisplayName("Guyton-Klinger Preset Tests")
    class GuytonKlingerTests {

        @Test
        @DisplayName("Should have correct initial withdrawal rate")
        void correctInitialRate() {
            GuardrailsConfiguration config = GuardrailsConfiguration.guytonKlinger();
            assertEquals(0, new BigDecimal("0.052").compareTo(config.initialWithdrawalRate()));
        }

        @Test
        @DisplayName("Should have upper guardrail at 80%")
        void correctUpperGuardrail() {
            GuardrailsConfiguration config = GuardrailsConfiguration.guytonKlinger();
            assertEquals(0, new BigDecimal("0.80").compareTo(config.upperThresholdMultiplier()));
            assertTrue(config.hasUpperGuardrail());
        }

        @Test
        @DisplayName("Should have lower guardrail at 120%")
        void correctLowerGuardrail() {
            GuardrailsConfiguration config = GuardrailsConfiguration.guytonKlinger();
            assertEquals(0, new BigDecimal("1.20").compareTo(config.lowerThresholdMultiplier()));
            assertTrue(config.hasLowerGuardrail());
        }

        @Test
        @DisplayName("Should have 10% adjustments both directions")
        void correctAdjustments() {
            GuardrailsConfiguration config = GuardrailsConfiguration.guytonKlinger();
            assertEquals(0, new BigDecimal("0.10").compareTo(config.increaseAdjustment()));
            assertEquals(0, new BigDecimal("0.10").compareTo(config.decreaseAdjustment()));
        }

        @Test
        @DisplayName("Should allow spending cuts")
        void allowsSpendingCuts() {
            GuardrailsConfiguration config = GuardrailsConfiguration.guytonKlinger();
            assertTrue(config.allowSpendingCuts());
        }

        @Test
        @DisplayName("Should skip inflation on down years")
        void skipsInflationOnDownYears() {
            GuardrailsConfiguration config = GuardrailsConfiguration.guytonKlinger();
            assertTrue(config.skipInflationOnDownYears());
        }

        @Test
        @DisplayName("Should have 15-year cap preservation rule")
        void correctCapPreservationYears() {
            GuardrailsConfiguration config = GuardrailsConfiguration.guytonKlinger();
            assertEquals(15, config.yearsBeforeCapPreservationEnds());
        }

        @Test
        @DisplayName("Should allow annual adjustments")
        void allowsAnnualAdjustments() {
            GuardrailsConfiguration config = GuardrailsConfiguration.guytonKlinger();
            assertEquals(1, config.minimumYearsBetweenRatchets());
        }
    }

    @Nested
    @DisplayName("Vanguard Dynamic Preset Tests")
    class VanguardDynamicTests {

        @Test
        @DisplayName("Should have correct initial withdrawal rate")
        void correctInitialRate() {
            GuardrailsConfiguration config = GuardrailsConfiguration.vanguardDynamic();
            assertEquals(0, new BigDecimal("0.04").compareTo(config.initialWithdrawalRate()));
        }

        @Test
        @DisplayName("Should have 5% ceiling (max increase)")
        void correctCeiling() {
            GuardrailsConfiguration config = GuardrailsConfiguration.vanguardDynamic();
            assertEquals(0, new BigDecimal("0.05").compareTo(config.increaseAdjustment()));
        }

        @Test
        @DisplayName("Should have 2.5% floor (max decrease)")
        void correctFloor() {
            GuardrailsConfiguration config = GuardrailsConfiguration.vanguardDynamic();
            assertEquals(0, new BigDecimal("0.025").compareTo(config.decreaseAdjustment()));
        }

        @Test
        @DisplayName("Should not have rate-based guardrails")
        void noRateBasedGuardrails() {
            GuardrailsConfiguration config = GuardrailsConfiguration.vanguardDynamic();
            assertNull(config.upperThresholdMultiplier());
            assertNull(config.lowerThresholdMultiplier());
            assertFalse(config.hasUpperGuardrail());
        }

        @Test
        @DisplayName("Should allow spending cuts")
        void allowsSpendingCuts() {
            GuardrailsConfiguration config = GuardrailsConfiguration.vanguardDynamic();
            assertTrue(config.allowSpendingCuts());
        }

        @Test
        @DisplayName("Should not skip inflation on down years")
        void doesNotSkipInflation() {
            GuardrailsConfiguration config = GuardrailsConfiguration.vanguardDynamic();
            assertFalse(config.skipInflationOnDownYears());
        }
    }

    @Nested
    @DisplayName("Kitces Ratcheting Preset Tests")
    class KitcesRatchetingTests {

        @Test
        @DisplayName("Should have correct initial withdrawal rate")
        void correctInitialRate() {
            GuardrailsConfiguration config = GuardrailsConfiguration.kitcesRatcheting();
            assertEquals(0, new BigDecimal("0.04").compareTo(config.initialWithdrawalRate()));
        }

        @Test
        @DisplayName("Should trigger at 50% growth (rate drops to 66.7%)")
        void correctTriggerThreshold() {
            GuardrailsConfiguration config = GuardrailsConfiguration.kitcesRatcheting();
            assertEquals(0, new BigDecimal("0.667").compareTo(config.upperThresholdMultiplier()));
        }

        @Test
        @DisplayName("Should have 10% ratchet increase")
        void correctRatchetIncrease() {
            GuardrailsConfiguration config = GuardrailsConfiguration.kitcesRatcheting();
            assertEquals(0, new BigDecimal("0.10").compareTo(config.increaseAdjustment()));
        }

        @Test
        @DisplayName("Should NOT allow spending cuts")
        void doesNotAllowSpendingCuts() {
            GuardrailsConfiguration config = GuardrailsConfiguration.kitcesRatcheting();
            assertFalse(config.allowSpendingCuts());
            assertFalse(config.hasLowerGuardrail());
        }

        @Test
        @DisplayName("Should have zero decrease adjustment")
        void zeroDecreaseAdjustment() {
            GuardrailsConfiguration config = GuardrailsConfiguration.kitcesRatcheting();
            assertEquals(0, BigDecimal.ZERO.compareTo(config.decreaseAdjustment()));
        }

        @Test
        @DisplayName("Should require 3 years between ratchets")
        void correctRatchetFrequency() {
            GuardrailsConfiguration config = GuardrailsConfiguration.kitcesRatcheting();
            assertEquals(3, config.minimumYearsBetweenRatchets());
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should create configuration with builder")
        void createsWithBuilder() {
            GuardrailsConfiguration config = GuardrailsConfiguration.builder()
                    .initialWithdrawalRate(new BigDecimal("0.045"))
                    .upperThresholdMultiplier(new BigDecimal("0.75"))
                    .increaseAdjustment(new BigDecimal("0.05"))
                    .lowerThresholdMultiplier(new BigDecimal("1.25"))
                    .decreaseAdjustment(new BigDecimal("0.05"))
                    .allowSpendingCuts(true)
                    .minimumYearsBetweenRatchets(2)
                    .build();

            assertEquals(0, new BigDecimal("0.045").compareTo(config.initialWithdrawalRate()));
            assertEquals(0, new BigDecimal("0.75").compareTo(config.upperThresholdMultiplier()));
            assertEquals(2, config.minimumYearsBetweenRatchets());
        }

        @Test
        @DisplayName("Should have sensible defaults")
        void hasSensibleDefaults() {
            GuardrailsConfiguration config = GuardrailsConfiguration.builder().build();

            assertEquals(0, new BigDecimal("0.04").compareTo(config.initialWithdrawalRate()));
            assertTrue(config.allowSpendingCuts());
            assertEquals(1, config.minimumYearsBetweenRatchets());
        }

        @Test
        @DisplayName("Should allow absolute floor and ceiling")
        void allowsAbsoluteConstraints() {
            GuardrailsConfiguration config = GuardrailsConfiguration.builder()
                    .absoluteFloor(new BigDecimal("3000"))
                    .absoluteCeiling(new BigDecimal("8000"))
                    .build();

            assertTrue(config.hasAbsoluteFloor());
            assertTrue(config.hasAbsoluteCeiling());
            assertEquals(0, new BigDecimal("3000").compareTo(config.absoluteFloor()));
            assertEquals(0, new BigDecimal("8000").compareTo(config.absoluteCeiling()));
        }
    }

    @Nested
    @DisplayName("Helper Method Tests")
    class HelperMethodTests {

        @Test
        @DisplayName("hasUpperGuardrail returns false when null")
        void hasUpperGuardrailFalseWhenNull() {
            GuardrailsConfiguration config = GuardrailsConfiguration.vanguardDynamic();
            assertFalse(config.hasUpperGuardrail());
        }

        @Test
        @DisplayName("hasLowerGuardrail returns false when cuts not allowed")
        void hasLowerGuardrailFalseWhenCutsNotAllowed() {
            GuardrailsConfiguration config = GuardrailsConfiguration.kitcesRatcheting();
            assertFalse(config.hasLowerGuardrail());
        }

        @Test
        @DisplayName("All presets are not null")
        void presetsNotNull() {
            assertNotNull(GuardrailsConfiguration.guytonKlinger());
            assertNotNull(GuardrailsConfiguration.vanguardDynamic());
            assertNotNull(GuardrailsConfiguration.kitcesRatcheting());
        }
    }
}
