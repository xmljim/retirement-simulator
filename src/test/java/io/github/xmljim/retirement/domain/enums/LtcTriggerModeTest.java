package io.github.xmljim.retirement.domain.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LtcTriggerMode Tests")
class LtcTriggerModeTest {

    @Test
    @DisplayName("DETERMINISTIC mode properties")
    void deterministicMode() {
        LtcTriggerMode mode = LtcTriggerMode.DETERMINISTIC;

        assertEquals("Deterministic", mode.getDisplayName());
        assertTrue(mode.canTriggerBenefits());
        assertFalse(mode.requiresMonteCarlo());
    }

    @Test
    @DisplayName("PROBABILISTIC mode properties")
    void probabilisticMode() {
        LtcTriggerMode mode = LtcTriggerMode.PROBABILISTIC;

        assertEquals("Probabilistic", mode.getDisplayName());
        assertTrue(mode.canTriggerBenefits());
        assertTrue(mode.requiresMonteCarlo());
    }

    @Test
    @DisplayName("NONE mode properties")
    void noneMode() {
        LtcTriggerMode mode = LtcTriggerMode.NONE;

        assertEquals("None", mode.getDisplayName());
        assertFalse(mode.canTriggerBenefits());
        assertFalse(mode.requiresMonteCarlo());
    }

    @Test
    @DisplayName("All modes have descriptions")
    void allModesHaveDescriptions() {
        for (LtcTriggerMode mode : LtcTriggerMode.values()) {
            assertNotNull(mode.getDescription());
            assertFalse(mode.getDescription().isEmpty());
        }
    }
}
