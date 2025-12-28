package io.github.xmljim.retirement.domain.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SurvivorRole Tests")
class SurvivorRoleTest {

    @Test
    @DisplayName("PRIMARY role properties")
    void primaryRole() {
        SurvivorRole role = SurvivorRole.PRIMARY;

        assertEquals("Primary", role.getDisplayName());
        assertNotNull(role.getDescription());
        assertEquals(SurvivorRole.SPOUSE, role.getDeceasedRole());
    }

    @Test
    @DisplayName("SPOUSE role properties")
    void spouseRole() {
        SurvivorRole role = SurvivorRole.SPOUSE;

        assertEquals("Spouse", role.getDisplayName());
        assertNotNull(role.getDescription());
        assertEquals(SurvivorRole.PRIMARY, role.getDeceasedRole());
    }

    @Test
    @DisplayName("getDeceasedRole is inverse")
    void deceasedRoleIsInverse() {
        assertEquals(SurvivorRole.SPOUSE, SurvivorRole.PRIMARY.getDeceasedRole());
        assertEquals(SurvivorRole.PRIMARY, SurvivorRole.SPOUSE.getDeceasedRole());
    }
}
