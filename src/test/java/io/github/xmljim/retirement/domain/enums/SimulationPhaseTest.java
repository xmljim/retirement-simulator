package io.github.xmljim.retirement.domain.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("SimulationPhase")
class SimulationPhaseTest {

    @Nested
    @DisplayName("Enum Values")
    class EnumValues {

        @Test
        @DisplayName("should have exactly 4 phases")
        void shouldHaveExactlyFourPhases() {
            assertEquals(4, SimulationPhase.values().length);
        }

        @Test
        @DisplayName("should have expected phases")
        void shouldHaveExpectedPhases() {
            Set<String> phases = Arrays.stream(SimulationPhase.values())
                    .map(Enum::name)
                    .collect(Collectors.toSet());

            assertTrue(phases.contains("ACCUMULATION"));
            assertTrue(phases.contains("TRANSITION"));
            assertTrue(phases.contains("DISTRIBUTION"));
            assertTrue(phases.contains("SURVIVOR"));
        }

        @ParameterizedTest
        @EnumSource(SimulationPhase.class)
        @DisplayName("all phases should have display names")
        void allPhasesShouldHaveDisplayNames(SimulationPhase phase) {
            assertNotNull(phase.getDisplayName());
            assertFalse(phase.getDisplayName().isBlank());
        }

        @ParameterizedTest
        @EnumSource(SimulationPhase.class)
        @DisplayName("all phases should have descriptions")
        void allPhasesShouldHaveDescriptions(SimulationPhase phase) {
            assertNotNull(phase.getDescription());
            assertFalse(phase.getDescription().isBlank());
        }
    }

    @Nested
    @DisplayName("allowsContributions")
    class AllowsContributions {

        @Test
        @DisplayName("ACCUMULATION should allow contributions")
        void accumulationAllowsContributions() {
            assertTrue(SimulationPhase.ACCUMULATION.allowsContributions());
        }

        @Test
        @DisplayName("TRANSITION should not allow contributions")
        void transitionDisallowsContributions() {
            assertFalse(SimulationPhase.TRANSITION.allowsContributions());
        }

        @Test
        @DisplayName("DISTRIBUTION should not allow contributions")
        void distributionDisallowsContributions() {
            assertFalse(SimulationPhase.DISTRIBUTION.allowsContributions());
        }

        @Test
        @DisplayName("SURVIVOR should not allow contributions")
        void survivorDisallowsContributions() {
            assertFalse(SimulationPhase.SURVIVOR.allowsContributions());
        }
    }

    @Nested
    @DisplayName("expectsWithdrawals")
    class ExpectsWithdrawals {

        @Test
        @DisplayName("ACCUMULATION should not expect withdrawals")
        void accumulationDoesNotExpectWithdrawals() {
            assertFalse(SimulationPhase.ACCUMULATION.expectsWithdrawals());
        }

        @Test
        @DisplayName("TRANSITION should not expect withdrawals")
        void transitionDoesNotExpectWithdrawals() {
            assertFalse(SimulationPhase.TRANSITION.expectsWithdrawals());
        }

        @Test
        @DisplayName("DISTRIBUTION should expect withdrawals")
        void distributionExpectsWithdrawals() {
            assertTrue(SimulationPhase.DISTRIBUTION.expectsWithdrawals());
        }

        @Test
        @DisplayName("SURVIVOR should expect withdrawals")
        void survivorExpectsWithdrawals() {
            assertTrue(SimulationPhase.SURVIVOR.expectsWithdrawals());
        }
    }

    @Nested
    @DisplayName("isRetired")
    class IsRetired {

        @Test
        @DisplayName("ACCUMULATION should not be retired")
        void accumulationIsNotRetired() {
            assertFalse(SimulationPhase.ACCUMULATION.isRetired());
        }

        @Test
        @DisplayName("TRANSITION should be retired")
        void transitionIsRetired() {
            assertTrue(SimulationPhase.TRANSITION.isRetired());
        }

        @Test
        @DisplayName("DISTRIBUTION should be retired")
        void distributionIsRetired() {
            assertTrue(SimulationPhase.DISTRIBUTION.isRetired());
        }

        @Test
        @DisplayName("SURVIVOR should be retired")
        void survivorIsRetired() {
            assertTrue(SimulationPhase.SURVIVOR.isRetired());
        }
    }

    @Nested
    @DisplayName("Display and Description")
    class DisplayAndDescription {

        @Test
        @DisplayName("ACCUMULATION display name should be 'Accumulation'")
        void accumulationDisplayName() {
            assertEquals("Accumulation", SimulationPhase.ACCUMULATION.getDisplayName());
        }

        @Test
        @DisplayName("TRANSITION display name should be 'Transition'")
        void transitionDisplayName() {
            assertEquals("Transition", SimulationPhase.TRANSITION.getDisplayName());
        }

        @Test
        @DisplayName("DISTRIBUTION display name should be 'Distribution'")
        void distributionDisplayName() {
            assertEquals("Distribution", SimulationPhase.DISTRIBUTION.getDisplayName());
        }

        @Test
        @DisplayName("SURVIVOR display name should be 'Survivor'")
        void survivorDisplayName() {
            assertEquals("Survivor", SimulationPhase.SURVIVOR.getDisplayName());
        }
    }
}
