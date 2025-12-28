package io.github.xmljim.retirement.domain.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ContingencyType Tests")
class ContingencyTypeTest {

    @Test
    @DisplayName("HOME_REPAIR has default rate")
    void homeRepairHasDefaultRate() {
        ContingencyType type = ContingencyType.HOME_REPAIR;

        assertTrue(type.hasDefaultRate());
        assertEquals(0, new BigDecimal("0.015").compareTo(type.getDefaultRateOfValue()));
    }

    @Test
    @DisplayName("VEHICLE_REPLACEMENT has no default rate")
    void vehicleReplacementNoDefaultRate() {
        ContingencyType type = ContingencyType.VEHICLE_REPLACEMENT;

        assertFalse(type.hasDefaultRate());
        assertNull(type.getDefaultRateOfValue());
    }

    @Test
    @DisplayName("All types have display names and descriptions")
    void allTypesHaveNames() {
        for (ContingencyType type : ContingencyType.values()) {
            assertNotNull(type.getDisplayName());
            assertNotNull(type.getDescription());
            assertFalse(type.getDisplayName().isEmpty());
            assertFalse(type.getDescription().isEmpty());
        }
    }

    @Test
    @DisplayName("Enum values exist")
    void enumValuesExist() {
        assertEquals(7, ContingencyType.values().length);
        assertNotNull(ContingencyType.HOME_REPAIR);
        assertNotNull(ContingencyType.VEHICLE_REPLACEMENT);
        assertNotNull(ContingencyType.EMERGENCY_FUND);
        assertNotNull(ContingencyType.APPLIANCE_RESERVE);
        assertNotNull(ContingencyType.MEDICAL_EMERGENCY);
        assertNotNull(ContingencyType.FAMILY_SUPPORT);
        assertNotNull(ContingencyType.GENERAL);
    }
}
