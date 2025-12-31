package io.github.xmljim.retirement.simulation.result;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.xmljim.retirement.domain.exception.InvalidDateRangeException;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;

/**
 * Generic container for storing monthly simulation snapshots with query and aggregation methods.
 *
 * <p>TimeSeries provides:
 * <ul>
 *   <li>Efficient monthly access via {@link #getSnapshot(YearMonth)}</li>
 *   <li>Range queries via {@link #getRange(YearMonth, YearMonth)}</li>
 *   <li>Annual aggregation via {@link #getAnnualSummary(int)}</li>
 *   <li>Stream-based iteration for functional processing</li>
 * </ul>
 *
 * <p>The time series is immutable after construction. Use {@link Builder} to create instances.
 *
 * <p>Example usage:
 * <pre>{@code
 * TimeSeries<MonthlySnapshot> series = TimeSeries.<MonthlySnapshot>builder()
 *     .add(snapshot1)
 *     .add(snapshot2)
 *     .build();
 *
 * // Monthly access
 * MonthlySnapshot jan2025 = series.getSnapshot(YearMonth.of(2025, 1));
 *
 * // Annual summary
 * AnnualSummary summary2025 = series.getAnnualSummary(2025);
 * }</pre>
 *
 * @param <T> the snapshot type, must extend MonthlySnapshot
 *
 * @see MonthlySnapshot
 * @see AnnualSummary
 */
public final class TimeSeries<T extends MonthlySnapshot> implements Iterable<T> {

    private static final int SCALE = 2;
    private static final int PERCENT_SCALE = 4;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final NavigableMap<YearMonth, T> snapshotsByMonth;
    private final List<T> snapshotsInOrder;

    /**
     * Private constructor - use {@link #builder()} to create instances.
     *
     * @param snapshots the snapshots to store
     */
    private TimeSeries(List<T> snapshots) {
        NavigableMap<YearMonth, T> map = new TreeMap<>();
        for (T snapshot : snapshots) {
            if (map.containsKey(snapshot.month())) {
                throw new ValidationException(
                        "Duplicate snapshot for month: " + snapshot.month(), "month");
            }
            map.put(snapshot.month(), snapshot);
        }
        this.snapshotsByMonth = Collections.unmodifiableNavigableMap(map);
        this.snapshotsInOrder = Collections.unmodifiableList(new ArrayList<>(map.values()));
    }

    // ─── Monthly Access ────────────────────────────────────────────────────────

    /**
     * Gets the snapshot for a specific month.
     *
     * @param month the year-month to query
     * @return the snapshot, or empty if not found
     */
    public Optional<T> getSnapshot(YearMonth month) {
        return Optional.ofNullable(snapshotsByMonth.get(month));
    }

    /**
     * Gets snapshots within a date range (inclusive).
     *
     * @param start the start month (inclusive)
     * @param end the end month (inclusive)
     * @return unmodifiable list of snapshots in the range
     */
    public List<T> getRange(YearMonth start, YearMonth end) {
        if (start.isAfter(end)) {
            throw InvalidDateRangeException.dateMustBeBefore("start", "end");
        }
        return Collections.unmodifiableList(
                new ArrayList<>(snapshotsByMonth.subMap(start, true, end, true).values())
        );
    }

    /**
     * Gets the first (earliest) snapshot.
     *
     * @return the first snapshot, or empty if series is empty
     */
    public Optional<T> getFirst() {
        return snapshotsInOrder.isEmpty()
                ? Optional.empty()
                : Optional.of(snapshotsInOrder.getFirst());
    }

    /**
     * Gets the last (latest) snapshot.
     *
     * @return the last snapshot, or empty if series is empty
     */
    public Optional<T> getLast() {
        return snapshotsInOrder.isEmpty()
                ? Optional.empty()
                : Optional.of(snapshotsInOrder.getLast());
    }

    /**
     * Returns the number of snapshots in the series.
     *
     * @return the snapshot count
     */
    public int size() {
        return snapshotsInOrder.size();
    }

    /**
     * Checks if the series is empty.
     *
     * @return true if no snapshots
     */
    public boolean isEmpty() {
        return snapshotsInOrder.isEmpty();
    }

    /**
     * Gets all snapshots for a specific year.
     *
     * @param year the calendar year
     * @return unmodifiable list of snapshots for that year
     */
    public List<T> getSnapshotsForYear(int year) {
        YearMonth start = YearMonth.of(year, 1);
        YearMonth end = YearMonth.of(year, 12);
        return getRange(start, end);
    }

    // ─── Annual Aggregation ────────────────────────────────────────────────────

    /**
     * Computes an annual summary for a specific year.
     *
     * <p>The summary aggregates all monthly data within the year.
     *
     * @param year the calendar year
     * @return the annual summary, or empty if no data for that year
     */
    public Optional<AnnualSummary> getAnnualSummary(int year) {
        List<T> yearSnapshots = getSnapshotsForYear(year);
        if (yearSnapshots.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(computeAnnualSummary(year, yearSnapshots));
    }

    /**
     * Computes annual summaries for all years in the series.
     *
     * @return unmodifiable list of annual summaries, ordered by year
     */
    public List<AnnualSummary> getAllAnnualSummaries() {
        if (isEmpty()) {
            return Collections.emptyList();
        }

        Map<Integer, List<T>> byYear = snapshotsInOrder.stream()
                .collect(Collectors.groupingBy(s -> s.month().getYear()));

        return byYear.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> computeAnnualSummary(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * Gets all distinct years present in the series.
     *
     * @return unmodifiable list of years, sorted ascending
     */
    public List<Integer> getYears() {
        return snapshotsInOrder.stream()
                .map(s -> s.month().getYear())
                .distinct()
                .sorted()
                .toList();
    }

    // ─── Iteration ─────────────────────────────────────────────────────────────

    /**
     * Returns a stream of all snapshots in chronological order.
     *
     * @return stream of snapshots
     */
    public Stream<T> stream() {
        return snapshotsInOrder.stream();
    }

    /**
     * Returns an iterator over snapshots in chronological order.
     *
     * @return iterator over snapshots
     */
    @Override
    public Iterator<T> iterator() {
        return snapshotsInOrder.iterator();
    }

    /**
     * Returns all snapshots as an unmodifiable list.
     *
     * @return unmodifiable list of all snapshots in chronological order
     */
    public List<T> toList() {
        return snapshotsInOrder;
    }

    // ─── Private Helpers ───────────────────────────────────────────────────────

    private AnnualSummary computeAnnualSummary(int year, List<T> yearSnapshots) {
        T first = yearSnapshots.getFirst();
        T last = yearSnapshots.getLast();

        BigDecimal startingBalance = first.totalPortfolioBalance()
                .subtract(first.totalContributions())
                .add(first.totalWithdrawals())
                .subtract(first.totalReturns());

        BigDecimal endingBalance = last.totalPortfolioBalance();

        BigDecimal totalContributions = yearSnapshots.stream()
                .map(MonthlySnapshot::totalContributions)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalWithdrawals = yearSnapshots.stream()
                .map(MonthlySnapshot::totalWithdrawals)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalIncome = yearSnapshots.stream()
                .map(MonthlySnapshot::totalIncome)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = yearSnapshots.stream()
                .map(MonthlySnapshot::totalExpenses)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTaxesPaid = yearSnapshots.stream()
                .map(s -> s.taxes().totalTaxLiability())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal annualReturn = yearSnapshots.stream()
                .map(MonthlySnapshot::totalReturns)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate return percentage based on average balance
        BigDecimal avgBalance = startingBalance.add(endingBalance)
                .divide(BigDecimal.valueOf(2), SCALE, ROUNDING);
        BigDecimal annualReturnPercent = avgBalance.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : annualReturn.divide(avgBalance, PERCENT_SCALE, ROUNDING);

        List<String> events = yearSnapshots.stream()
                .flatMap(s -> s.eventsTriggered().stream())
                .distinct()
                .toList();

        return AnnualSummary.builder(year)
                .startingBalance(startingBalance.setScale(SCALE, ROUNDING))
                .endingBalance(endingBalance.setScale(SCALE, ROUNDING))
                .totalContributions(totalContributions.setScale(SCALE, ROUNDING))
                .totalWithdrawals(totalWithdrawals.setScale(SCALE, ROUNDING))
                .totalIncome(totalIncome.setScale(SCALE, ROUNDING))
                .totalExpenses(totalExpenses.setScale(SCALE, ROUNDING))
                .totalTaxesPaid(totalTaxesPaid.setScale(SCALE, ROUNDING))
                .annualReturn(annualReturn.setScale(SCALE, ROUNDING))
                .annualReturnPercent(annualReturnPercent)
                .significantEvents(events)
                .build();
    }

    // ─── Builder ───────────────────────────────────────────────────────────────

    /**
     * Creates a builder for constructing TimeSeries instances.
     *
     * @param <T> the snapshot type
     * @return a new builder
     */
    public static <T extends MonthlySnapshot> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Builder for TimeSeries.
     *
     * @param <T> the snapshot type
     */
    public static final class Builder<T extends MonthlySnapshot> {
        private final List<T> snapshots = new ArrayList<>();

        private Builder() {
        }

        /**
         * Adds a snapshot to the series.
         *
         * @param snapshot the snapshot to add
         * @return this builder
         * @throws MissingRequiredFieldException if snapshot is null
         */
        public Builder<T> add(T snapshot) {
            if (snapshot == null) {
                throw new MissingRequiredFieldException("snapshot");
            }
            snapshots.add(snapshot);
            return this;
        }

        /**
         * Adds multiple snapshots to the series.
         *
         * @param snapshots the snapshots to add
         * @return this builder
         */
        public Builder<T> addAll(List<T> snapshots) {
            if (snapshots != null) {
                for (T snapshot : snapshots) {
                    add(snapshot);
                }
            }
            return this;
        }

        /**
         * Builds the TimeSeries.
         *
         * <p>Snapshots are automatically sorted by month during construction.
         *
         * @return the constructed TimeSeries
         */
        public TimeSeries<T> build() {
            return new TimeSeries<>(snapshots);
        }
    }
}
