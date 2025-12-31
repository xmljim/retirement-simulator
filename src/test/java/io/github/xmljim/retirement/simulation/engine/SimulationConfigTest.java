package io.github.xmljim.retirement.simulation.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;
import io.github.xmljim.retirement.domain.model.PersonProfile;
import io.github.xmljim.retirement.domain.model.Portfolio;
import io.github.xmljim.retirement.simulation.config.SimulationLevers;

@DisplayName("SimulationConfig")
class SimulationConfigTest {

    private PersonProfile primaryPerson;
    private PersonProfile spouse;
    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        primaryPerson = PersonProfile.builder()
            .name("John Doe")
            .dateOfBirth(LocalDate.of(1970, 1, 1))
            .retirementDate(LocalDate.of(2035, 1, 1))
            .lifeExpectancy(90)
            .build();

        spouse = PersonProfile.builder()
            .name("Jane Doe")
            .dateOfBirth(LocalDate.of(1972, 6, 15))
            .retirementDate(LocalDate.of(2037, 6, 1))
            .lifeExpectancy(92)
            .build();

        portfolio = Portfolio.builder()
            .owner(primaryPerson)
            .build();
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should build with all required fields")
        void shouldBuildWithRequiredFields() {
            SimulationConfig config = SimulationConfig.builder()
                .persons(List.of(primaryPerson))
                .portfolio(portfolio)
                .startMonth(YearMonth.of(2025, 1))
                .endMonth(YearMonth.of(2055, 12))
                .build();

            assertNotNull(config);
            assertEquals(1, config.persons().size());
            assertNotNull(config.levers()); // Should default
        }

        @Test
        @DisplayName("should build with single person convenience method")
        void shouldBuildWithSinglePerson() {
            SimulationConfig config = SimulationConfig.builder()
                .person(primaryPerson)
                .portfolio(portfolio)
                .startMonth(YearMonth.of(2025, 1))
                .endMonth(YearMonth.of(2055, 12))
                .build();

            assertEquals(1, config.persons().size());
            assertEquals(primaryPerson, config.primaryPerson());
        }

        @Test
        @DisplayName("should default levers when not provided")
        void shouldDefaultLevers() {
            SimulationConfig config = SimulationConfig.builder()
                .person(primaryPerson)
                .portfolio(portfolio)
                .startMonth(YearMonth.of(2025, 1))
                .endMonth(YearMonth.of(2055, 12))
                .build();

            assertNotNull(config.levers());
        }

        @Test
        @DisplayName("should use custom levers when provided")
        void shouldUseCustomLevers() {
            SimulationLevers customLevers = SimulationLevers.withDefaults();

            SimulationConfig config = SimulationConfig.builder()
                .person(primaryPerson)
                .portfolio(portfolio)
                .levers(customLevers)
                .startMonth(YearMonth.of(2025, 1))
                .endMonth(YearMonth.of(2055, 12))
                .build();

            assertEquals(customLevers, config.levers());
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("should reject null persons")
        void shouldRejectNullPersons() {
            assertThrows(MissingRequiredFieldException.class, () ->
                SimulationConfig.builder()
                    .persons(null)
                    .portfolio(portfolio)
                    .startMonth(YearMonth.of(2025, 1))
                    .endMonth(YearMonth.of(2055, 12))
                    .build());
        }

        @Test
        @DisplayName("should reject empty persons list")
        void shouldRejectEmptyPersons() {
            assertThrows(ValidationException.class, () ->
                SimulationConfig.builder()
                    .persons(List.of())
                    .portfolio(portfolio)
                    .startMonth(YearMonth.of(2025, 1))
                    .endMonth(YearMonth.of(2055, 12))
                    .build());
        }

        @Test
        @DisplayName("should reject null portfolio")
        void shouldRejectNullPortfolio() {
            assertThrows(MissingRequiredFieldException.class, () ->
                SimulationConfig.builder()
                    .person(primaryPerson)
                    .portfolio(null)
                    .startMonth(YearMonth.of(2025, 1))
                    .endMonth(YearMonth.of(2055, 12))
                    .build());
        }

        @Test
        @DisplayName("should reject null startMonth")
        void shouldRejectNullStartMonth() {
            assertThrows(MissingRequiredFieldException.class, () ->
                SimulationConfig.builder()
                    .person(primaryPerson)
                    .portfolio(portfolio)
                    .startMonth(null)
                    .endMonth(YearMonth.of(2055, 12))
                    .build());
        }

        @Test
        @DisplayName("should reject null endMonth")
        void shouldRejectNullEndMonth() {
            assertThrows(MissingRequiredFieldException.class, () ->
                SimulationConfig.builder()
                    .person(primaryPerson)
                    .portfolio(portfolio)
                    .startMonth(YearMonth.of(2025, 1))
                    .endMonth(null)
                    .build());
        }
    }

    @Nested
    @DisplayName("Couple Simulation")
    class CoupleSimulation {

        @Test
        @DisplayName("should identify couple simulation")
        void shouldIdentifyCoupleSimulation() {
            SimulationConfig config = SimulationConfig.builder()
                .persons(List.of(primaryPerson, spouse))
                .portfolio(portfolio)
                .startMonth(YearMonth.of(2025, 1))
                .endMonth(YearMonth.of(2055, 12))
                .build();

            assertTrue(config.isCoupleSimulation());
            assertEquals(primaryPerson, config.primaryPerson());
            assertEquals(spouse, config.spouse());
        }

        @Test
        @DisplayName("should identify single simulation")
        void shouldIdentifySingleSimulation() {
            SimulationConfig config = SimulationConfig.builder()
                .person(primaryPerson)
                .portfolio(portfolio)
                .startMonth(YearMonth.of(2025, 1))
                .endMonth(YearMonth.of(2055, 12))
                .build();

            assertFalse(config.isCoupleSimulation());
            assertEquals(primaryPerson, config.primaryPerson());
            assertNull(config.spouse());
        }
    }

    @Nested
    @DisplayName("Month Calculation")
    class MonthCalculation {

        @Test
        @DisplayName("should calculate month count for full years")
        void shouldCalculateMonthCountForFullYears() {
            SimulationConfig config = SimulationConfig.builder()
                .person(primaryPerson)
                .portfolio(portfolio)
                .startMonth(YearMonth.of(2025, 1))
                .endMonth(YearMonth.of(2025, 12))
                .build();

            assertEquals(12, config.monthCount());
        }

        @Test
        @DisplayName("should calculate month count spanning years")
        void shouldCalculateMonthCountSpanningYears() {
            SimulationConfig config = SimulationConfig.builder()
                .person(primaryPerson)
                .portfolio(portfolio)
                .startMonth(YearMonth.of(2025, 1))
                .endMonth(YearMonth.of(2055, 12))
                .build();

            // 31 years = 372 months
            assertEquals(372, config.monthCount());
        }

        @Test
        @DisplayName("should calculate month count for single month")
        void shouldCalculateMonthCountForSingleMonth() {
            SimulationConfig config = SimulationConfig.builder()
                .person(primaryPerson)
                .portfolio(portfolio)
                .startMonth(YearMonth.of(2025, 6))
                .endMonth(YearMonth.of(2025, 6))
                .build();

            assertEquals(1, config.monthCount());
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("persons list should be unmodifiable")
        void personsListShouldBeUnmodifiable() {
            SimulationConfig config = SimulationConfig.builder()
                .persons(List.of(primaryPerson))
                .portfolio(portfolio)
                .startMonth(YearMonth.of(2025, 1))
                .endMonth(YearMonth.of(2055, 12))
                .build();

            assertThrows(UnsupportedOperationException.class, () ->
                config.persons().add(spouse));
        }
    }
}
