package io.github.xmljim.retirement.simulation.result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.exception.InvalidDateRangeException;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;

@DisplayName("TimeSeries")
class TimeSeriesTest {

    private MonthlySnapshot jan2025;
    private MonthlySnapshot feb2025;
    private MonthlySnapshot mar2025;
    private MonthlySnapshot jan2026;

    @BeforeEach
    void setUp() {
        jan2025 = MonthlySnapshot.builder(YearMonth.of(2025, 1))
                .salaryIncome(new BigDecimal("8000"))
                .totalExpenses(new BigDecimal("5000"))
                .eventsTriggered(List.of("NewYear"))
                .build();
        feb2025 = MonthlySnapshot.builder(YearMonth.of(2025, 2))
                .salaryIncome(new BigDecimal("8000"))
                .totalExpenses(new BigDecimal("5000"))
                .build();
        mar2025 = MonthlySnapshot.builder(YearMonth.of(2025, 3))
                .salaryIncome(new BigDecimal("8000"))
                .totalExpenses(new BigDecimal("5000"))
                .eventsTriggered(List.of("Bonus"))
                .build();
        jan2026 = MonthlySnapshot.builder(YearMonth.of(2026, 1))
                .salaryIncome(new BigDecimal("8500"))
                .totalExpenses(new BigDecimal("5200"))
                .eventsTriggered(List.of("Raise"))
                .build();
    }

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create empty series")
        void shouldCreateEmptySeries() {
            TimeSeries<MonthlySnapshot> series = TimeSeries.<MonthlySnapshot>builder().build();
            assertTrue(series.isEmpty());
            assertEquals(0, series.size());
        }

        @Test
        @DisplayName("should reject null snapshot")
        void shouldRejectNullSnapshot() {
            assertThrows(MissingRequiredFieldException.class, () ->
                    TimeSeries.<MonthlySnapshot>builder().add(null));
        }

        @Test
        @DisplayName("should reject duplicate months")
        void shouldRejectDuplicateMonths() {
            MonthlySnapshot dup = MonthlySnapshot.builder(YearMonth.of(2025, 1)).build();
            assertThrows(ValidationException.class, () ->
                    TimeSeries.<MonthlySnapshot>builder().add(jan2025).add(dup).build());
        }

        @Test
        @DisplayName("should sort snapshots chronologically")
        void shouldSortSnapshots() {
            TimeSeries<MonthlySnapshot> series = TimeSeries.<MonthlySnapshot>builder()
                    .add(mar2025).add(jan2025).add(feb2025).build();
            assertEquals(jan2025, series.getFirst().orElseThrow());
            assertEquals(mar2025, series.getLast().orElseThrow());
        }
    }

    @Nested
    @DisplayName("Monthly Access")
    class MonthlyAccess {

        @Test
        @DisplayName("getSnapshot should return snapshot for month")
        void getSnapshotFound() {
            TimeSeries<MonthlySnapshot> series = TimeSeries.<MonthlySnapshot>builder()
                    .add(jan2025).add(feb2025).build();
            assertEquals(jan2025, series.getSnapshot(YearMonth.of(2025, 1)).orElseThrow());
        }

        @Test
        @DisplayName("getSnapshot should return empty for missing month")
        void getSnapshotNotFound() {
            TimeSeries<MonthlySnapshot> series = TimeSeries.<MonthlySnapshot>builder()
                    .add(jan2025).build();
            assertTrue(series.getSnapshot(YearMonth.of(2025, 12)).isEmpty());
        }

        @Test
        @DisplayName("getRange should return snapshots in range")
        void getRange() {
            TimeSeries<MonthlySnapshot> series = TimeSeries.<MonthlySnapshot>builder()
                    .add(jan2025).add(feb2025).add(mar2025).build();
            List<MonthlySnapshot> range = series.getRange(
                    YearMonth.of(2025, 1), YearMonth.of(2025, 2));
            assertEquals(2, range.size());
        }

        @Test
        @DisplayName("getRange should reject invalid range")
        void getRangeInvalid() {
            TimeSeries<MonthlySnapshot> series = TimeSeries.<MonthlySnapshot>builder()
                    .add(jan2025).build();
            assertThrows(InvalidDateRangeException.class, () ->
                    series.getRange(YearMonth.of(2025, 3), YearMonth.of(2025, 1)));
        }
    }

    @Nested
    @DisplayName("Annual Aggregation")
    class AnnualAggregation {

        @Test
        @DisplayName("getAnnualSummary should aggregate year data")
        void getAnnualSummary() {
            TimeSeries<MonthlySnapshot> series = TimeSeries.<MonthlySnapshot>builder()
                    .add(jan2025).add(feb2025).add(mar2025).build();
            AnnualSummary summary = series.getAnnualSummary(2025).orElseThrow();
            assertEquals(2025, summary.year());
            assertEquals(new BigDecimal("24000.00"), summary.totalIncome());
            assertEquals(new BigDecimal("15000.00"), summary.totalExpenses());
        }

        @Test
        @DisplayName("getAnnualSummary should return empty for missing year")
        void getAnnualSummaryMissing() {
            TimeSeries<MonthlySnapshot> series = TimeSeries.<MonthlySnapshot>builder()
                    .add(jan2025).build();
            assertTrue(series.getAnnualSummary(2030).isEmpty());
        }

        @Test
        @DisplayName("getAllAnnualSummaries should return all years")
        void getAllAnnualSummaries() {
            TimeSeries<MonthlySnapshot> series = TimeSeries.<MonthlySnapshot>builder()
                    .add(jan2025).add(feb2025).add(jan2026).build();
            List<AnnualSummary> summaries = series.getAllAnnualSummaries();
            assertEquals(2, summaries.size());
            assertEquals(2025, summaries.get(0).year());
            assertEquals(2026, summaries.get(1).year());
        }

        @Test
        @DisplayName("getYears should return distinct years")
        void getYears() {
            TimeSeries<MonthlySnapshot> series = TimeSeries.<MonthlySnapshot>builder()
                    .add(jan2025).add(feb2025).add(jan2026).build();
            assertEquals(List.of(2025, 2026), series.getYears());
        }

        @Test
        @DisplayName("annual summary should collect events")
        void annualSummaryEvents() {
            TimeSeries<MonthlySnapshot> series = TimeSeries.<MonthlySnapshot>builder()
                    .add(jan2025).add(mar2025).build();
            AnnualSummary summary = series.getAnnualSummary(2025).orElseThrow();
            assertTrue(summary.significantEvents().contains("NewYear"));
            assertTrue(summary.significantEvents().contains("Bonus"));
        }
    }

    @Nested
    @DisplayName("Iteration")
    class Iteration {

        @Test
        @DisplayName("stream should return all snapshots")
        void stream() {
            TimeSeries<MonthlySnapshot> series = TimeSeries.<MonthlySnapshot>builder()
                    .add(jan2025).add(feb2025).build();
            assertEquals(2, series.stream().count());
        }

        @Test
        @DisplayName("iterator should work in for-each")
        void iterator() {
            TimeSeries<MonthlySnapshot> series = TimeSeries.<MonthlySnapshot>builder()
                    .add(jan2025).add(feb2025).build();
            int count = 0;
            for (MonthlySnapshot s : series) {
                count++;
            }
            assertEquals(2, count);
        }

        @Test
        @DisplayName("toList should return unmodifiable list")
        void toList() {
            TimeSeries<MonthlySnapshot> series = TimeSeries.<MonthlySnapshot>builder()
                    .add(jan2025).build();
            assertThrows(UnsupportedOperationException.class, () ->
                    series.toList().add(feb2025));
        }
    }
}
