# Time Series Design Proposal

**Status:** Draft
**Created:** December 26, 2025
**Last Updated:** December 26, 2025
**Target Milestone:** M7 (Simulation Engine)

---

## Overview

This document proposes a time series data model for capturing and querying sequential financial data (transactions, balances, snapshots) over time. This foundation enables visualization, analysis, and scenario comparison capabilities.

### Motivation

The retirement simulator generates monthly data points from start date through end of simulation:
- Account balances changing over time
- Contributions and employer matches
- Investment returns
- Withdrawals and distributions

Users need to:
1. **Visualize** portfolio growth over time (line charts, area charts)
2. **Analyze** performance across periods (year-over-year, pre/post retirement)
3. **Compare** scenarios side-by-side (retire at 62 vs 65)
4. **Query** specific time ranges for reporting

A well-designed time series model provides these capabilities efficiently.

---

## Goals

1. **Sequential Access**: Ordered collection of date/value pairs for graphing
2. **Range Queries**: Efficiently retrieve data for specific time periods
3. **Aggregation**: Sum, average, min/max over periods
4. **Type Safety**: Generic container supporting different entry types
5. **Immutability**: Historical records cannot be modified
6. **Memory Efficiency**: Support 30+ year simulations (360+ monthly entries)

## Non-Goals (Out of Scope)

1. Real-time streaming data
2. Sub-monthly granularity (daily transactions)
3. Database persistence (in-memory for now)
4. Distributed processing

---

## Design Options Explored

### Option A: Simple List of Transactions

```java
List<Transaction> transactions;
```

**Pros**: Simple, familiar
**Cons**: No type safety, no query methods, manual filtering

### Option B: Generic TimeSeries Container

```java
TimeSeries<T extends TimeSeriesEntry> {
    List<T> entries;
    // Query and aggregation methods
}
```

**Pros**: Reusable, type-safe, encapsulates query logic
**Cons**: Slightly more complex

### Option C: Specialized Per-Type Collections

```java
ContributionHistory contributions;
WithdrawalHistory withdrawals;
BalanceHistory balances;
```

**Pros**: Type-specific methods
**Cons**: Code duplication, harder to combine for graphing

### Recommendation: Option B (Generic TimeSeries)

Option B provides the best balance of flexibility and type safety. A generic `TimeSeries<T>` can be reused for different entry types while providing consistent query capabilities.

---

## Proposed Design

### Core Interface: TimeSeriesEntry

All time series entries implement this interface:

```java
package io.github.xmljim.retirement.domain.timeseries;

/**
 * Represents a single entry in a time series.
 * Provides the minimum contract for date-based ordering and value access.
 */
public interface TimeSeriesEntry {

    /**
     * The date of this entry.
     * For monthly data, typically the first day of the month.
     */
    LocalDate getDate();

    /**
     * The primary value of this entry.
     * Interpretation depends on entry type (balance, amount, etc.).
     */
    BigDecimal getValue();

    /**
     * The year-month of this entry (convenience method).
     */
    default YearMonth getYearMonth() {
        return YearMonth.from(getDate());
    }
}
```

### Core Class: TimeSeries<T>

Generic container with query and aggregation capabilities:

```java
package io.github.xmljim.retirement.domain.timeseries;

/**
 * An ordered collection of time series entries.
 * Entries are maintained in chronological order.
 *
 * @param <T> the type of entries in this series
 */
public class TimeSeries<T extends TimeSeriesEntry> implements Iterable<T> {

    private final List<T> entries;  // Immutable, ordered by date

    // Factory methods
    public static <T extends TimeSeriesEntry> TimeSeries<T> of(List<T> entries);
    public static <T extends TimeSeriesEntry> TimeSeries<T> empty();

    // Access methods
    public List<T> getEntries();
    public int size();
    public boolean isEmpty();
    public Optional<T> getFirst();
    public Optional<T> getLast();

    // Query by date
    public Optional<T> getAt(LocalDate date);
    public Optional<T> getAt(YearMonth yearMonth);
    public TimeSeries<T> getRange(LocalDate start, LocalDate end);
    public TimeSeries<T> getRange(YearMonth start, YearMonth end);
    public TimeSeries<T> getBefore(LocalDate date);
    public TimeSeries<T> getAfter(LocalDate date);

    // Aggregation
    public BigDecimal sum();
    public BigDecimal sum(LocalDate start, LocalDate end);
    public Optional<BigDecimal> average();
    public Optional<T> min();  // By value
    public Optional<T> max();  // By value

    // Grouping
    public Map<Year, TimeSeries<T>> groupByYear();
    public Map<YearMonth, T> toYearMonthMap();

    // Transformation (for graphing)
    public List<DataPoint> toDataPoints();
    public <R extends TimeSeriesEntry> TimeSeries<R> map(Function<T, R> mapper);

    // Combination
    public TimeSeries<T> append(T entry);
    public TimeSeries<T> concat(TimeSeries<T> other);
}
```

### Data Point for Graphing

Simple record for chart consumption:

```java
package io.github.xmljim.retirement.domain.timeseries;

/**
 * A simple date/value pair for charting libraries.
 */
public record DataPoint(
    LocalDate date,
    BigDecimal value
) {
    /**
     * Alternative constructor for Unix timestamp (JavaScript compatibility).
     */
    public long getTimestamp() {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }
}
```

### Monthly Snapshot Implementation

Primary entry type for simulation output:

```java
package io.github.xmljim.retirement.domain.timeseries;

/**
 * Captures the complete financial state for a single month.
 * This is the primary output of the monthly simulation loop.
 */
public record MonthlySnapshot(
    YearMonth month,
    BigDecimal startBalance,
    BigDecimal personalContributions,
    BigDecimal employerContributions,
    BigDecimal investmentReturns,
    BigDecimal withdrawals,
    BigDecimal endBalance,
    SimulationPhase phase  // ACCUMULATION or DISTRIBUTION
) implements TimeSeriesEntry {

    @Override
    public LocalDate getDate() {
        return month.atDay(1);
    }

    @Override
    public BigDecimal getValue() {
        return endBalance;
    }

    // Derived calculations
    public BigDecimal totalContributions() {
        return personalContributions.add(employerContributions);
    }

    public BigDecimal netChange() {
        return endBalance.subtract(startBalance);
    }
}
```

### Account Time Series (Specialized)

Account-specific time series with convenience methods:

```java
package io.github.xmljim.retirement.domain.timeseries;

/**
 * Time series for a single investment account.
 */
public class AccountTimeSeries extends TimeSeries<MonthlySnapshot> {

    private final InvestmentAccount account;

    // Account-specific aggregations
    public BigDecimal getTotalContributions();
    public BigDecimal getTotalEmployerMatch();
    public BigDecimal getTotalReturns();
    public BigDecimal getTotalWithdrawals();

    // Phase-specific queries
    public TimeSeries<MonthlySnapshot> getAccumulationPhase();
    public TimeSeries<MonthlySnapshot> getDistributionPhase();

    // Balance queries
    public BigDecimal getBalanceAtRetirement();
    public Optional<YearMonth> getDepletionMonth();
}
```

### Portfolio Time Series (Aggregated)

Combines multiple account time series:

```java
package io.github.xmljim.retirement.domain.timeseries;

/**
 * Aggregated time series across all accounts in a portfolio.
 */
public class PortfolioTimeSeries extends TimeSeries<MonthlySnapshot> {

    private final Map<String, AccountTimeSeries> accountSeries;

    // Get individual account series
    public AccountTimeSeries getAccountSeries(String accountId);
    public List<AccountTimeSeries> getAllAccountSeries();

    // Cross-account aggregations
    public Map<AccountType, BigDecimal> getBalanceByAccountType(YearMonth month);
    public Map<TaxTreatment, BigDecimal> getBalanceByTaxTreatment(YearMonth month);
}
```

---

## Integration with Milestones

### Milestone 2: Core Transaction & Account Operations

**Impact**: Minimal - design M2 components to be *compatible* with TimeSeries

- Ensure `Transaction` or similar can implement `TimeSeriesEntry`
- Use `BigDecimal` for financial values (precision)
- Include date/yearMonth on all financial records

**No new M2 issues required** - just awareness during implementation.

### Milestone 7: Simulation Engine

**Impact**: Primary implementation milestone

The simulation loop will:
1. Generate `MonthlySnapshot` for each month
2. Build `AccountTimeSeries` for each account
3. Build `PortfolioTimeSeries` for the complete simulation

**New Issue**: "Implement TimeSeries data model for simulation output"

### Milestone 9: Output & Reporting

**Impact**: Consumer of TimeSeries data

Reports will use:
- `TimeSeries.toDataPoints()` for chart data
- `TimeSeries.groupByYear()` for annual summaries
- `AccountTimeSeries.getTotalContributions()` for summary stats

### Milestone 11: UI (React)

**Impact**: Consumer of TimeSeries data via API

The API will serialize `DataPoint` lists for charting libraries (Chart.js, Recharts, etc.)

---

## Class Diagram

```
                    ┌─────────────────────┐
                    │  TimeSeriesEntry    │
                    │     <<interface>>   │
                    ├─────────────────────┤
                    │ + getDate()         │
                    │ + getValue()        │
                    │ + getYearMonth()    │
                    └──────────┬──────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
              ▼                ▼                ▼
    ┌─────────────────┐ ┌─────────────┐ ┌─────────────────┐
    │ MonthlySnapshot │ │  DataPoint  │ │ (Future types)  │
    │    <<record>>   │ │  <<record>> │ │                 │
    └─────────────────┘ └─────────────┘ └─────────────────┘
              │
              │ contains
              ▼
    ┌─────────────────────────────────┐
    │      TimeSeries<T>              │
    ├─────────────────────────────────┤
    │ - entries: List<T>              │
    ├─────────────────────────────────┤
    │ + getRange(start, end)          │
    │ + sum()                         │
    │ + toDataPoints()                │
    │ + groupByYear()                 │
    └─────────────────┬───────────────┘
                      │
         ┌────────────┴────────────┐
         │                         │
         ▼                         ▼
┌─────────────────────┐  ┌─────────────────────┐
│  AccountTimeSeries  │  │ PortfolioTimeSeries │
├─────────────────────┤  ├─────────────────────┤
│ - account           │  │ - accountSeries     │
├─────────────────────┤  ├─────────────────────┤
│ + getTotalReturns() │  │ + getAccountSeries()│
│ + getDepletionMonth │  │ + getBalanceByType()│
└─────────────────────┘  └─────────────────────┘
```

---

## Open Questions

These questions will be resolved as we progress through milestones:

1. **Precision**: Is `BigDecimal` necessary everywhere, or can we use `double` for display values?

2. **Memory**: For 30-year Monte Carlo with 10,000 runs, should we store full series or just statistics?

3. **Individual Transactions**: Do we need detailed transaction records in addition to monthly snapshots?

4. **Sparse Data**: How to handle months with no activity (copy forward vs. explicit entry)?

5. **Thread Safety**: Will simulation run in parallel? Does TimeSeries need to be thread-safe?

6. **Serialization**: JSON format for API responses - flat array vs. nested structure?

---

## Revision History

| Date | Version | Author | Changes |
|------|---------|--------|---------|
| 2025-12-26 | 0.1 | Claude/xmljim | Initial draft |

---

## References

- [PROJECT_GOALS.md](../../requirements/PROJECT_GOALS.md) - Section 6.3 (Monthly Simulation Loop)
- [ARCHITECTURE.md](../ARCHITECTURE.md) - Layered Architecture
- M1 Retrospective - Discussion on time series needs
