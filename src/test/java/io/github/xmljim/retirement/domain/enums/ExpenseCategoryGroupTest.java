package io.github.xmljim.retirement.domain.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("ExpenseCategoryGroup Tests")
class ExpenseCategoryGroupTest {

    @ParameterizedTest
    @EnumSource(ExpenseCategoryGroup.class)
    @DisplayName("All groups should have non-null display name")
    void allGroupsHaveDisplayName(ExpenseCategoryGroup group) {
        assertNotNull(group.getDisplayName());
    }

    @ParameterizedTest
    @EnumSource(ExpenseCategoryGroup.class)
    @DisplayName("All groups should have non-null description")
    void allGroupsHaveDescription(ExpenseCategoryGroup group) {
        assertNotNull(group.getDescription());
    }

    @Test
    @DisplayName("ESSENTIAL should have correct display name")
    void essentialDisplayName() {
        assertEquals("Essential", ExpenseCategoryGroup.ESSENTIAL.getDisplayName());
    }

    @Test
    @DisplayName("HEALTHCARE should have correct display name")
    void healthcareDisplayName() {
        assertEquals("Healthcare", ExpenseCategoryGroup.HEALTHCARE.getDisplayName());
    }

    @Test
    @DisplayName("DISCRETIONARY should have correct display name")
    void discretionaryDisplayName() {
        assertEquals("Discretionary", ExpenseCategoryGroup.DISCRETIONARY.getDisplayName());
    }

    @Test
    @DisplayName("CONTINGENCY should have correct display name")
    void contingencyDisplayName() {
        assertEquals("Contingency", ExpenseCategoryGroup.CONTINGENCY.getDisplayName());
    }

    @Test
    @DisplayName("DEBT should have correct display name")
    void debtDisplayName() {
        assertEquals("Debt", ExpenseCategoryGroup.DEBT.getDisplayName());
    }

    @Test
    @DisplayName("OTHER should have correct display name")
    void otherDisplayName() {
        assertEquals("Other", ExpenseCategoryGroup.OTHER.getDisplayName());
    }

    @Test
    @DisplayName("Should have exactly 6 groups")
    void shouldHaveSixGroups() {
        assertEquals(6, ExpenseCategoryGroup.values().length);
    }
}
