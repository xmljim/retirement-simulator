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

import io.github.xmljim.retirement.domain.enums.ExpenseCategory.InflationType;

@DisplayName("ExpenseCategory Tests")
class ExpenseCategoryTest {

    @Nested
    @DisplayName("Basic Property Tests")
    class BasicPropertyTests {

        @ParameterizedTest
        @EnumSource(ExpenseCategory.class)
        @DisplayName("All categories should have non-null display name")
        void allCategoriesHaveDisplayName(ExpenseCategory category) {
            assertNotNull(category.getDisplayName());
        }

        @ParameterizedTest
        @EnumSource(ExpenseCategory.class)
        @DisplayName("All categories should have non-null group")
        void allCategoriesHaveGroup(ExpenseCategory category) {
            assertNotNull(category.getGroup());
        }

        @ParameterizedTest
        @EnumSource(ExpenseCategory.class)
        @DisplayName("All categories should have non-null inflation type")
        void allCategoriesHaveInflationType(ExpenseCategory category) {
            assertNotNull(category.getInflationType());
        }

        @ParameterizedTest
        @EnumSource(ExpenseCategory.class)
        @DisplayName("All categories should have non-null description")
        void allCategoriesHaveDescription(ExpenseCategory category) {
            assertNotNull(category.getDescription());
        }

        @Test
        @DisplayName("Should have 19 expense categories")
        void shouldHave19Categories() {
            assertEquals(19, ExpenseCategory.values().length);
        }
    }

    @Nested
    @DisplayName("Group Mapping Tests")
    class GroupMappingTests {

        @Test
        @DisplayName("Essential categories should be in ESSENTIAL group")
        void essentialCategoriesInEssentialGroup() {
            Set<ExpenseCategory> essentialCategories = Set.of(
                    ExpenseCategory.HOUSING,
                    ExpenseCategory.FOOD,
                    ExpenseCategory.UTILITIES,
                    ExpenseCategory.TRANSPORTATION,
                    ExpenseCategory.INSURANCE
            );

            for (ExpenseCategory category : essentialCategories) {
                assertEquals(ExpenseCategoryGroup.ESSENTIAL, category.getGroup(),
                        category + " should be ESSENTIAL");
            }
        }

        @Test
        @DisplayName("Healthcare categories should be in HEALTHCARE group")
        void healthcareCategoriesInHealthcareGroup() {
            Set<ExpenseCategory> healthcareCategories = Set.of(
                    ExpenseCategory.MEDICARE_PREMIUMS,
                    ExpenseCategory.HEALTHCARE_OOP,
                    ExpenseCategory.LTC_PREMIUMS,
                    ExpenseCategory.LTC_CARE
            );

            for (ExpenseCategory category : healthcareCategories) {
                assertEquals(ExpenseCategoryGroup.HEALTHCARE, category.getGroup(),
                        category + " should be HEALTHCARE");
            }
        }

        @Test
        @DisplayName("Discretionary categories should be in DISCRETIONARY group")
        void discretionaryCategoriesInDiscretionaryGroup() {
            Set<ExpenseCategory> discretionaryCategories = Set.of(
                    ExpenseCategory.TRAVEL,
                    ExpenseCategory.ENTERTAINMENT,
                    ExpenseCategory.HOBBIES,
                    ExpenseCategory.GIFTS
            );

            for (ExpenseCategory category : discretionaryCategories) {
                assertEquals(ExpenseCategoryGroup.DISCRETIONARY, category.getGroup(),
                        category + " should be DISCRETIONARY");
            }
        }

        @Test
        @DisplayName("Contingency categories should be in CONTINGENCY group")
        void contingencyCategoriesInContingencyGroup() {
            Set<ExpenseCategory> contingencyCategories = Set.of(
                    ExpenseCategory.HOME_REPAIRS,
                    ExpenseCategory.VEHICLE_REPLACEMENT,
                    ExpenseCategory.EMERGENCY_RESERVE
            );

            for (ExpenseCategory category : contingencyCategories) {
                assertEquals(ExpenseCategoryGroup.CONTINGENCY, category.getGroup(),
                        category + " should be CONTINGENCY");
            }
        }

        @Test
        @DisplayName("DEBT_PAYMENTS should be in DEBT group")
        void debtPaymentsInDebtGroup() {
            assertEquals(ExpenseCategoryGroup.DEBT, ExpenseCategory.DEBT_PAYMENTS.getGroup());
        }

        @Test
        @DisplayName("TAXES and OTHER should be in OTHER group")
        void otherCategoriesInOtherGroup() {
            assertEquals(ExpenseCategoryGroup.OTHER, ExpenseCategory.TAXES.getGroup());
            assertEquals(ExpenseCategoryGroup.OTHER, ExpenseCategory.OTHER.getGroup());
        }
    }

    @Nested
    @DisplayName("Inflation Type Tests")
    class InflationTypeTests {

        @Test
        @DisplayName("HOUSING should use HOUSING inflation type")
        void housingUsesHousingInflation() {
            assertEquals(InflationType.HOUSING, ExpenseCategory.HOUSING.getInflationType());
        }

        @Test
        @DisplayName("HOME_REPAIRS should use HOUSING inflation type")
        void homeRepairsUsesHousingInflation() {
            assertEquals(InflationType.HOUSING, ExpenseCategory.HOME_REPAIRS.getInflationType());
        }

        @Test
        @DisplayName("Healthcare categories should use HEALTHCARE inflation type")
        void healthcareCategoriesUseHealthcareInflation() {
            assertEquals(InflationType.HEALTHCARE, ExpenseCategory.MEDICARE_PREMIUMS.getInflationType());
            assertEquals(InflationType.HEALTHCARE, ExpenseCategory.HEALTHCARE_OOP.getInflationType());
        }

        @Test
        @DisplayName("LTC categories should use LTC inflation type")
        void ltcCategoriesUseLtcInflation() {
            assertEquals(InflationType.LTC, ExpenseCategory.LTC_PREMIUMS.getInflationType());
            assertEquals(InflationType.LTC, ExpenseCategory.LTC_CARE.getInflationType());
        }

        @Test
        @DisplayName("DEBT_PAYMENTS should use NONE inflation type")
        void debtPaymentsUsesNoInflation() {
            assertEquals(InflationType.NONE, ExpenseCategory.DEBT_PAYMENTS.getInflationType());
        }

        @Test
        @DisplayName("General categories should use GENERAL inflation type")
        void generalCategoriesUseGeneralInflation() {
            Set<ExpenseCategory> generalCategories = Set.of(
                    ExpenseCategory.FOOD,
                    ExpenseCategory.UTILITIES,
                    ExpenseCategory.TRANSPORTATION,
                    ExpenseCategory.INSURANCE,
                    ExpenseCategory.TRAVEL,
                    ExpenseCategory.ENTERTAINMENT,
                    ExpenseCategory.HOBBIES,
                    ExpenseCategory.GIFTS,
                    ExpenseCategory.VEHICLE_REPLACEMENT,
                    ExpenseCategory.EMERGENCY_RESERVE,
                    ExpenseCategory.TAXES,
                    ExpenseCategory.OTHER
            );

            for (ExpenseCategory category : generalCategories) {
                assertEquals(InflationType.GENERAL, category.getInflationType(),
                        category + " should use GENERAL inflation");
            }
        }
    }

    @Nested
    @DisplayName("Helper Method Tests")
    class HelperMethodTests {

        @Test
        @DisplayName("isEssential should return true for ESSENTIAL group")
        void isEssentialForEssentialGroup() {
            assertTrue(ExpenseCategory.HOUSING.isEssential());
            assertTrue(ExpenseCategory.FOOD.isEssential());
            assertTrue(ExpenseCategory.UTILITIES.isEssential());
            assertTrue(ExpenseCategory.TRANSPORTATION.isEssential());
            assertTrue(ExpenseCategory.INSURANCE.isEssential());
        }

        @Test
        @DisplayName("isEssential should return false for non-ESSENTIAL groups")
        void isNotEssentialForOtherGroups() {
            assertFalse(ExpenseCategory.TRAVEL.isEssential());
            assertFalse(ExpenseCategory.MEDICARE_PREMIUMS.isEssential());
            assertFalse(ExpenseCategory.DEBT_PAYMENTS.isEssential());
        }

        @Test
        @DisplayName("isHealthcare should return true for HEALTHCARE group")
        void isHealthcareForHealthcareGroup() {
            assertTrue(ExpenseCategory.MEDICARE_PREMIUMS.isHealthcare());
            assertTrue(ExpenseCategory.HEALTHCARE_OOP.isHealthcare());
            assertTrue(ExpenseCategory.LTC_PREMIUMS.isHealthcare());
            assertTrue(ExpenseCategory.LTC_CARE.isHealthcare());
        }

        @Test
        @DisplayName("isHealthcare should return false for non-HEALTHCARE groups")
        void isNotHealthcareForOtherGroups() {
            assertFalse(ExpenseCategory.HOUSING.isHealthcare());
            assertFalse(ExpenseCategory.TRAVEL.isHealthcare());
        }

        @Test
        @DisplayName("isDiscretionary should return true for DISCRETIONARY group")
        void isDiscretionaryForDiscretionaryGroup() {
            assertTrue(ExpenseCategory.TRAVEL.isDiscretionary());
            assertTrue(ExpenseCategory.ENTERTAINMENT.isDiscretionary());
            assertTrue(ExpenseCategory.HOBBIES.isDiscretionary());
            assertTrue(ExpenseCategory.GIFTS.isDiscretionary());
        }

        @Test
        @DisplayName("isDiscretionary should return false for non-DISCRETIONARY groups")
        void isNotDiscretionaryForOtherGroups() {
            assertFalse(ExpenseCategory.HOUSING.isDiscretionary());
            assertFalse(ExpenseCategory.MEDICARE_PREMIUMS.isDiscretionary());
        }

        @Test
        @DisplayName("isInflationAdjusted should return true for most categories")
        void isInflationAdjustedForMostCategories() {
            assertTrue(ExpenseCategory.HOUSING.isInflationAdjusted());
            assertTrue(ExpenseCategory.FOOD.isInflationAdjusted());
            assertTrue(ExpenseCategory.MEDICARE_PREMIUMS.isInflationAdjusted());
            assertTrue(ExpenseCategory.TRAVEL.isInflationAdjusted());
        }

        @Test
        @DisplayName("isInflationAdjusted should return false for DEBT_PAYMENTS")
        void isNotInflationAdjustedForDebtPayments() {
            assertFalse(ExpenseCategory.DEBT_PAYMENTS.isInflationAdjusted());
        }
    }

    @Nested
    @DisplayName("All Groups Covered Tests")
    class AllGroupsCoveredTests {

        @Test
        @DisplayName("Every category group should have at least one category")
        void everyGroupHasAtLeastOneCategory() {
            Set<ExpenseCategoryGroup> groupsWithCategories = Arrays.stream(ExpenseCategory.values())
                    .map(ExpenseCategory::getGroup)
                    .collect(Collectors.toSet());

            for (ExpenseCategoryGroup group : ExpenseCategoryGroup.values()) {
                assertTrue(groupsWithCategories.contains(group),
                        "Group " + group + " should have at least one category");
            }
        }
    }
}
