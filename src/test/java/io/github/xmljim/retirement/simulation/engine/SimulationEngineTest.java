package io.github.xmljim.retirement.simulation.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.SimulationPhase;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.model.PersonProfile;
import io.github.xmljim.retirement.domain.model.Portfolio;
import io.github.xmljim.retirement.simulation.config.MarketLevers;
import io.github.xmljim.retirement.simulation.config.SimulationLevers;
import io.github.xmljim.retirement.simulation.result.MonthlySnapshot;
import io.github.xmljim.retirement.simulation.result.TimeSeries;

@DisplayName("SimulationEngine")
class SimulationEngineTest {

    private SimulationEngine engine;
    private PersonProfile person;
    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        engine = SimulationEngine.builder().build();

        person = PersonProfile.builder()
            .name("Test Person")
            .dateOfBirth(LocalDate.of(1970, 1, 1))
            .retirementDate(LocalDate.of(2035, 1, 1))
            .lifeExpectancy(90)
            .build();

        portfolio = Portfolio.builder()
            .owner(person)
            .build();
    }

    @Nested
    @DisplayName("Phase Determination")
    class PhaseDetermination {

        @Test
        @DisplayName("should return ACCUMULATION before retirement date")
        void shouldReturnAccumulationBeforeRetirement() {
            SimulationConfig config = createConfig(
                YearMonth.of(2025, 1),
                YearMonth.of(2025, 12));

            engine.run(config);

            // Person retires 2035, so 2025 is accumulation
            SimulationPhase phase = engine.determinePhase(person, YearMonth.of(2025, 6));
            assertEquals(SimulationPhase.ACCUMULATION, phase);
        }

        @Test
        @DisplayName("should return DISTRIBUTION after retirement date")
        void shouldReturnDistributionAfterRetirement() {
            SimulationConfig config = createConfig(
                YearMonth.of(2025, 1),
                YearMonth.of(2025, 12));

            engine.run(config);

            // Person retires 2035, so 2040 is distribution
            SimulationPhase phase = engine.determinePhase(person, YearMonth.of(2040, 6));
            assertEquals(SimulationPhase.DISTRIBUTION, phase);
        }

        @Test
        @DisplayName("should return DISTRIBUTION on retirement month")
        void shouldReturnDistributionOnRetirementMonth() {
            SimulationConfig config = createConfig(
                YearMonth.of(2025, 1),
                YearMonth.of(2025, 12));

            engine.run(config);

            // Person retires 2035-01-01
            SimulationPhase phase = engine.determinePhase(person, YearMonth.of(2035, 1));
            assertEquals(SimulationPhase.DISTRIBUTION, phase);
        }
    }

    @Nested
    @DisplayName("Monthly Return Calculation")
    class MonthlyReturnCalculation {

        @Test
        @DisplayName("should calculate monthly return from 7% annual")
        void shouldCalculateMonthlyReturnFromSevenPercent() {
            SimulationConfig config = SimulationConfig.builder()
                .portfolio(portfolio)
                .levers(SimulationLevers.builder()
                    .market(MarketLevers.deterministic(new BigDecimal("0.07")))
                    .build())
                .startMonth(YearMonth.of(2025, 1))
                .endMonth(YearMonth.of(2025, 12))
                .build();

            BigDecimal monthlyRate = engine.calculateMonthlyReturn(config);

            // (1.07)^(1/12) - 1 ≈ 0.005654
            assertTrue(monthlyRate.compareTo(new BigDecimal("0.005")) > 0);
            assertTrue(monthlyRate.compareTo(new BigDecimal("0.006")) < 0);
        }

        @Test
        @DisplayName("should calculate zero return for zero annual rate")
        void shouldCalculateZeroReturnForZeroRate() {
            SimulationConfig config = SimulationConfig.builder()
                .portfolio(portfolio)
                .levers(SimulationLevers.builder()
                    .market(MarketLevers.deterministic(BigDecimal.ZERO))
                    .build())
                .startMonth(YearMonth.of(2025, 1))
                .endMonth(YearMonth.of(2025, 12))
                .build();

            BigDecimal monthlyRate = engine.calculateMonthlyReturn(config);

            assertEquals(0, BigDecimal.ZERO.compareTo(monthlyRate));
        }
    }

    @Nested
    @DisplayName("Month Generation")
    class MonthGeneration {

        @Test
        @DisplayName("should generate correct number of months")
        void shouldGenerateCorrectNumberOfMonths() {
            List<YearMonth> months = engine.generateMonths(
                YearMonth.of(2025, 1),
                YearMonth.of(2025, 12));

            assertEquals(12, months.size());
            assertEquals(YearMonth.of(2025, 1), months.get(0));
            assertEquals(YearMonth.of(2025, 12), months.get(11));
        }

        @Test
        @DisplayName("should generate single month")
        void shouldGenerateSingleMonth() {
            List<YearMonth> months = engine.generateMonths(
                YearMonth.of(2025, 6),
                YearMonth.of(2025, 6));

            assertEquals(1, months.size());
            assertEquals(YearMonth.of(2025, 6), months.get(0));
        }

        @Test
        @DisplayName("should generate months spanning years")
        void shouldGenerateMonthsSpanningYears() {
            List<YearMonth> months = engine.generateMonths(
                YearMonth.of(2024, 11),
                YearMonth.of(2025, 2));

            assertEquals(4, months.size());
            assertEquals(YearMonth.of(2024, 11), months.get(0));
            assertEquals(YearMonth.of(2024, 12), months.get(1));
            assertEquals(YearMonth.of(2025, 1), months.get(2));
            assertEquals(YearMonth.of(2025, 2), months.get(3));
        }
    }

    @Nested
    @DisplayName("Simulation Run")
    class SimulationRun {

        @Test
        @DisplayName("should run empty simulation with no accounts")
        void shouldRunEmptySimulation() {
            SimulationConfig config = createConfig(
                YearMonth.of(2025, 1),
                YearMonth.of(2025, 12));

            TimeSeries<MonthlySnapshot> results = engine.run(config);

            assertNotNull(results);
            assertEquals(12, results.size());
        }

        @Test
        @DisplayName("should iterate correct number of months")
        void shouldIterateCorrectMonths() {
            SimulationConfig config = createConfig(
                YearMonth.of(2025, 1),
                YearMonth.of(2027, 12));

            TimeSeries<MonthlySnapshot> results = engine.run(config);

            // 3 years = 36 months
            assertEquals(36, results.size());
        }

        @Test
        @DisplayName("should create state on run")
        void shouldCreateStateOnRun() {
            SimulationConfig config = createConfig(
                YearMonth.of(2025, 1),
                YearMonth.of(2025, 12));

            engine.run(config);

            assertNotNull(engine.getState());
        }

        @Test
        @DisplayName("should reject null config")
        void shouldRejectNullConfig() {
            assertThrows(MissingRequiredFieldException.class, () ->
                engine.run(null));
        }

        @Test
        @DisplayName("should record snapshots with correct phases")
        void shouldRecordSnapshotsWithCorrectPhases() {
            // Person retires 2035, test months in 2034 (accumulation)
            SimulationConfig config = createConfig(
                YearMonth.of(2034, 1),
                YearMonth.of(2034, 6));

            TimeSeries<MonthlySnapshot> results = engine.run(config);

            // All months should be ACCUMULATION
            results.stream().forEach(snapshot ->
                assertEquals(SimulationPhase.ACCUMULATION, snapshot.phase()));
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should build without components")
        void shouldBuildWithoutComponents() {
            SimulationEngine built = SimulationEngine.builder().build();

            assertNotNull(built);
        }
    }

    // ─── Helper Methods ─────────────────────────────────────────────────────────

    private SimulationConfig createConfig(YearMonth start, YearMonth end) {
        return SimulationConfig.builder()
            .portfolio(portfolio)
            .startMonth(start)
            .endMonth(end)
            .build();
    }
}
