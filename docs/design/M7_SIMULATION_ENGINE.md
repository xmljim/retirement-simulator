# M7: Simulation Engine Design

**Status:** Research In Progress
**Date:** December 30, 2025
**Milestone:** 7
**Research Issue:** #267

---

## Overview

The Simulation Engine is the orchestration core that ties together all previous milestones. It manages the progression of time, coordinates contributions (accumulation) and withdrawals (distribution), applies market returns, and records everything to a time series.

---

## Design Principles (Lessons from M6)

1. **Architecture First** - Define integration points before implementation
2. **Clear Ownership** - Engine owns mutable state; calculators are pure functions
3. **Separation of Concerns** - Levers (config) vs Engine (execution) vs Output (time series)

---

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          SIMULATION CONFIGURATION                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                       │
│  │   Economic   │  │    Market    │  │   Expense    │                       │
│  │    Levers    │  │    Levers    │  │    Levers    │                       │
│  └──────────────┘  └──────────────┘  └──────────────┘                       │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           SIMULATION ENGINE                                  │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      implements SimulationView                       │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                     │
│  │   People    │    │  Accounts   │    │   Events    │                     │
│  │   (M1/M4)   │    │   (M2/M3)   │    │  (triggers) │                     │
│  └─────────────┘    └─────────────┘    └─────────────┘                     │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      MONTHLY SIMULATION LOOP                         │   │
│  │                                                                      │   │
│  │  1. Apply market returns to accounts                                 │   │
│  │  2. Process income (salary, SS, pension, annuity)                   │   │
│  │  3. Calculate expenses (budget + inflation)                          │   │
│  │  4. Determine phase (accumulation vs distribution)                   │   │
│  │  5. Execute contributions OR withdrawals                             │   │
│  │  6. Process events (triggers)                                        │   │
│  │  7. Record to time series                                            │   │
│  │  8. Advance to next month                                            │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                     TimeSeries<MonthlySnapshot>                              │
│                                                                             │
│  Month 1 → Month 2 → Month 3 → ... → Month N                               │
│  (Full transaction history with account balances)                           │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Research Questions

### 1. State Management

**Question:** What's in `MonthlySnapshot`?

**Proposed Structure:**
```java
public record MonthlySnapshot(
    YearMonth month,

    // Account state
    Map<UUID, BigDecimal> accountBalances,
    BigDecimal totalPortfolioBalance,

    // Transactions this month
    List<Transaction> transactions,

    // Income this month
    BigDecimal salaryIncome,
    BigDecimal socialSecurityIncome,
    BigDecimal pensionIncome,
    BigDecimal otherIncome,
    BigDecimal totalIncome,

    // Expenses this month
    BigDecimal totalExpenses,
    Map<ExpenseCategoryGroup, BigDecimal> expensesByCategory,

    // Flows this month
    BigDecimal totalContributions,
    BigDecimal totalWithdrawals,
    BigDecimal netCashFlow,

    // Cumulative metrics
    BigDecimal cumulativeContributions,
    BigDecimal cumulativeWithdrawals,
    BigDecimal cumulativeReturns,

    // Market this month
    BigDecimal monthlyReturn,
    BigDecimal ytdReturn,

    // Metadata
    SimulationPhase phase,
    List<String> eventsTriggered
) {}
```

**Open Questions:**
- [ ] Is this too granular? Should some fields be computed on-demand?
- [ ] How do we handle per-account transactions vs aggregated view?

---

### 2. Loop Structure

**Question:** Monthly or annual granularity?

**Recommendation:** Monthly simulation with annual aggregation points

```
For each month from start to end:
    1. Apply monthly return (annual / 12 or monthly draw)
    2. Process monthly income
    3. Process monthly expenses
    4. Execute monthly contributions/withdrawals
    5. Record MonthlySnapshot

    If (month == December):
        - Calculate annual summary
        - Trigger annual events (RMD, rebalancing)
        - Reset YTD accumulators
```

**Open Questions:**
- [ ] Some contributions are per-paycheck (bi-weekly). Aggregate to monthly?
- [ ] RMDs are annual but can be taken monthly. How to model?

---

### 3. Phase Management

**Question:** How does the engine detect accumulation → distribution transition?

**Proposed Approach:**
```java
public enum SimulationPhase {
    ACCUMULATION,      // Working, contributing
    TRANSITION,        // Retired but not yet withdrawing (bridge period)
    DISTRIBUTION,      // Withdrawing from portfolio
    SURVIVOR           // One spouse deceased
}

// Phase determination per person
SimulationPhase determinePhase(PersonProfile person, YearMonth month) {
    if (person.isDeceased(month)) return SURVIVOR;
    if (month.isBefore(person.getRetirementDate())) return ACCUMULATION;
    if (month.isBefore(person.getWithdrawalStartDate())) return TRANSITION;
    return DISTRIBUTION;
}
```

**Open Questions:**
- [ ] Couple with different retirement dates - how to handle?
- [ ] Part-time work in early retirement (contributions + withdrawals)?

---

### 4. Event System

**Question:** How are events registered and fired?

**Proposed Approach:**
```java
public interface SimulationEvent {
    YearMonth getTriggerDate();
    void execute(SimulationContext context);
    String getDescription();
}

// Examples
- RetirementStartEvent
- SocialSecurityStartEvent
- RmdStartEvent
- MortgagePayoffEvent
- SpouseDeathEvent
- SpendingPhaseTransitionEvent
```

**Open Questions:**
- [ ] Are events pre-registered or detected dynamically?
- [ ] How do events affect future months (e.g., expense changes)?

---

### 5. Variable Levers

**Question:** How are configuration levers structured?

**Proposed Structure:**
```java
public record SimulationLevers(
    EconomicLevers economic,
    MarketLevers market,
    ExpenseLevers expense
) {}

public record EconomicLevers(
    BigDecimal generalInflationRate,
    BigDecimal wageGrowthRate,
    BigDecimal interestRate
) {}

public record MarketLevers(
    SimulationMode mode,
    BigDecimal expectedReturn,        // For deterministic
    BigDecimal returnStdDev,          // For Monte Carlo
    List<BigDecimal> historicalReturns // For backtesting
) {}

public record ExpenseLevers(
    Map<InflationType, BigDecimal> inflationRates,
    BigDecimal healthcareTrend
) {}
```

**Open Questions:**
- [ ] How do Monte Carlo runs override baseline?
- [ ] Should levers be immutable or adjustable mid-simulation?

---

### 6. SimulationView Implementation

**Question:** How does the engine implement M6's `SimulationView`?

```java
public class SimulationEngine implements SimulationView {
    private final Map<UUID, InvestmentAccount> accounts;
    private final TimeSeries<MonthlySnapshot> history;
    private final BigDecimal initialPortfolioBalance;

    @Override
    public BigDecimal getTotalPortfolioBalance() {
        return accounts.values().stream()
            .map(InvestmentAccount::getBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal getPriorYearSpending() {
        // Query history for prior year's total withdrawals
    }

    @Override
    public List<AccountSnapshot> getAccountSnapshots() {
        return accounts.values().stream()
            .map(AccountSnapshot::from)
            .toList();
    }
}
```

---

### 7. Simulation Modes

**Question:** How do different modes affect execution?

| Mode | Return Source | Use Case |
|------|---------------|----------|
| DETERMINISTIC | Fixed rate (e.g., 7%) | Base case planning |
| MONTE_CARLO | Random draw from distribution | Probability analysis |
| HISTORICAL | Actual market sequence | Stress testing |

**Open Questions:**
- [ ] Monte Carlo: how many runs? How to aggregate results?
- [ ] Historical: what data source? How far back?

---

### 8. Integration Points

| Milestone | Integration |
|-----------|-------------|
| M1: Person | `PersonProfile` for ages, dates, life expectancy |
| M2: Account | `InvestmentAccount` for balances, contributions |
| M3: Contributions | `ContributionRouter` during accumulation |
| M4: Income | `SocialSecurityBenefit`, `Pension`, `Annuity` |
| M5: Expenses | `Budget`, `GapAnalyzer`, expense modifiers |
| M6: Distribution | `SpendingStrategy`, `SpendingOrchestrator` |

---

## Proposed Sub-Milestones

### M7a: Core Engine Framework (~15 points)
- `SimulationEngine` class implementing `SimulationView`
- `MonthlySnapshot` and `TimeSeries` structures
- `SimulationLevers` configuration
- Basic monthly loop (no contributions/withdrawals yet)

### M7b: Accumulation Phase (~10 points)
- Integration with M3 `ContributionRouter`
- Salary income processing
- Employer match calculations
- Account growth with returns

### M7c: Distribution Phase (~10 points)
- Integration with M6 `SpendingOrchestrator`
- Income source processing (SS, pension, annuity)
- Expense calculation with inflation
- Withdrawal execution

### M7d: Event System (~8 points)
- Event interface and registry
- Standard event implementations
- Phase transition handling

### M7e: Simulation Modes (~12 points)
- Deterministic mode
- Monte Carlo mode with aggregation
- Historical backtesting mode

### M7f: Integration & Testing (~8 points)
- End-to-end simulation tests
- Multi-decade scenario validation
- Performance optimization

**Total:** ~63 story points

---

## Next Steps

1. [ ] Review and refine this design document
2. [ ] Answer open questions in each section
3. [ ] Create milestone and issues once design approved
4. [ ] Implement M7a first (core framework)

---

## References

- [M6 Design: SimulationView interface](M6_DISTRIBUTION_STRATEGIES.md)
- [PROJECT_GOALS.md: M7 section](../../requirements/PROJECT_GOALS.md)
- [M6 Retrospective: Architecture lessons](../../retrospectives/M6-RETROSPECTIVE.md)
