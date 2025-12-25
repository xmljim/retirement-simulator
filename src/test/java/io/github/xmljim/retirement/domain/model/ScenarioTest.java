package io.github.xmljim.retirement.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.DistributionStrategy;
import io.github.xmljim.retirement.domain.enums.EndCondition;
import io.github.xmljim.retirement.domain.enums.SimulationMode;
import io.github.xmljim.retirement.domain.exception.InvalidDateRangeException;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.value.InflationAssumptions;

@DisplayName("Scenario Tests")
class ScenarioTest {

    private PersonProfile primaryPerson;
    private PersonProfile secondaryPerson;

    @BeforeEach
    void setUp() {
        primaryPerson = PersonProfile.builder()
            .name("John Doe")
            .dateOfBirth(LocalDate.of(1970, 1, 1))
            .retirementDate(LocalDate.of(2035, 1, 1))
            .lifeExpectancy(90)
            .build();

        secondaryPerson = PersonProfile.builder()
            .name("Jane Doe")
            .dateOfBirth(LocalDate.of(1972, 6, 15))
            .retirementDate(LocalDate.of(2037, 1, 1))
            .lifeExpectancy(92)
            .build();
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should create scenario with required fields")
        void createWithRequiredFields() {
            Scenario scenario = Scenario.builder()
                .name("Retirement Plan")
                .primaryPerson(primaryPerson)
                .build();

            assertEquals("Retirement Plan", scenario.getName());
            assertEquals(primaryPerson, scenario.getPrimaryPerson());
            assertNotNull(scenario.getId());
            assertNotNull(scenario.getStartDate());
            assertEquals(EndCondition.LIFE_EXPECTANCY, scenario.getEndCondition());
            assertEquals(SimulationMode.DETERMINISTIC, scenario.getSimulationMode());
        }

        @Test
        @DisplayName("Should throw exception when name is missing")
        void missingName() {
            assertThrows(MissingRequiredFieldException.class, () ->
                Scenario.builder().primaryPerson(primaryPerson).build());
        }

        @Test
        @DisplayName("Should throw exception when primary person is missing")
        void missingPrimaryPerson() {
            assertThrows(MissingRequiredFieldException.class, () ->
                Scenario.builder().name("Test").build());
        }

        @Test
        @DisplayName("Should use custom start date")
        void customStartDate() {
            LocalDate startDate = LocalDate.of(2025, 1, 1);
            Scenario scenario = createDefaultScenario()
                .startDate(startDate)
                .build();

            assertEquals(startDate, scenario.getStartDate());
        }
    }

    @Nested
    @DisplayName("Couple Scenario Tests")
    class CoupleTests {

        @Test
        @DisplayName("Should identify single person scenario")
        void singlePersonScenario() {
            Scenario scenario = createDefaultScenario().build();
            assertFalse(scenario.isCoupleScenario());
            assertTrue(scenario.getSecondaryPerson().isEmpty());
        }

        @Test
        @DisplayName("Should identify couple scenario")
        void coupleScenario() {
            Scenario scenario = createDefaultScenario()
                .secondaryPerson(secondaryPerson)
                .build();

            assertTrue(scenario.isCoupleScenario());
            assertTrue(scenario.getSecondaryPerson().isPresent());
            assertEquals(secondaryPerson, scenario.getSecondaryPerson().get());
        }

        @Test
        @DisplayName("Should use longer life expectancy for couple end date")
        void coupleEndDate() {
            Scenario scenario = createDefaultScenario()
                .secondaryPerson(secondaryPerson)
                .build();

            LocalDate expectedEnd = secondaryPerson.getProjectedEndDate();
            assertEquals(expectedEnd, scenario.getProjectedEndDate());
        }
    }

    @Nested
    @DisplayName("Duration Tests")
    class DurationTests {

        @Test
        @DisplayName("Should calculate simulation duration")
        void calculateDuration() {
            Scenario scenario = Scenario.builder()
                .name("Test")
                .primaryPerson(primaryPerson)
                .startDate(LocalDate.of(2025, 1, 1))
                .build();

            LocalDate endDate = primaryPerson.getProjectedEndDate();
            int expectedYears = endDate.getYear() - 2025;
            assertEquals(expectedYears, scenario.getSimulationDurationYears());
        }
    }

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        @Test
        @DisplayName("Should use custom inflation assumptions")
        void customInflation() {
            InflationAssumptions custom = InflationAssumptions.builder()
                .generalInflation(0.04)
                .healthcareInflation(0.06)
                .housingInflation(0.03)
                .build();

            Scenario scenario = createDefaultScenario()
                .inflationAssumptions(custom)
                .build();

            assertEquals(custom, scenario.getInflationAssumptions());
        }

        @Test
        @DisplayName("Should use custom distribution strategy")
        void customDistributionStrategy() {
            Scenario scenario = createDefaultScenario()
                .distributionStrategy(DistributionStrategy.PRO_RATA)
                .build();

            assertEquals(DistributionStrategy.PRO_RATA, scenario.getDistributionStrategy());
        }

        @Test
        @DisplayName("Should use custom end condition")
        void customEndCondition() {
            Scenario scenario = createDefaultScenario()
                .endCondition(EndCondition.PORTFOLIO_DEPLETION)
                .build();

            assertEquals(EndCondition.PORTFOLIO_DEPLETION, scenario.getEndCondition());
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should reject start date after projected end")
        void startDateAfterEnd() {
            assertThrows(InvalidDateRangeException.class, () ->
                Scenario.builder()
                    .name("Test")
                    .primaryPerson(primaryPerson)
                    .startDate(LocalDate.of(2100, 1, 1))
                    .build());
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsTests {

        @Test
        @DisplayName("Scenarios with same ID should be equal")
        void sameIdEquals() {
            Scenario s1 = Scenario.builder()
                .id("same-id")
                .name("Test 1")
                .primaryPerson(primaryPerson)
                .build();
            Scenario s2 = Scenario.builder()
                .id("same-id")
                .name("Test 2")
                .primaryPerson(primaryPerson)
                .build();

            assertEquals(s1, s2);
            assertEquals(s1.hashCode(), s2.hashCode());
        }

        @Test
        @DisplayName("Scenarios with different IDs should not be equal")
        void differentIdNotEquals() {
            Scenario s1 = createDefaultScenario().build();
            Scenario s2 = createDefaultScenario().build();
            assertNotEquals(s1, s2);
        }
    }

    @Nested
    @DisplayName("ToBuilder Tests")
    class ToBuilderTests {

        @Test
        @DisplayName("Should create copy with toBuilder")
        void toBuilderCopiesValues() {
            Scenario original = createDefaultScenario()
                .secondaryPerson(secondaryPerson)
                .endCondition(EndCondition.FIRST_OF_BOTH)
                .build();

            Scenario copy = original.toBuilder().build();

            assertEquals(original.getId(), copy.getId());
            assertEquals(original.getName(), copy.getName());
            assertEquals(original.getPrimaryPerson(), copy.getPrimaryPerson());
            assertEquals(original.getEndCondition(), copy.getEndCondition());
        }
    }

    private Scenario.Builder createDefaultScenario() {
        return Scenario.builder()
            .name("Test Scenario")
            .primaryPerson(primaryPerson);
    }
}
