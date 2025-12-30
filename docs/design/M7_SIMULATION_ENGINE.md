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
│                          SIMULATION CONFIGURATION                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                       │
│  │   Economic   │  │    Market    │  │   Expense    │                       │
│  │    Levers    │  │    Levers    │  │    Levers    │                       │
│  └──────────────┘  └──────────────┘  └──────────────┘                       │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           SIMULATION ENGINE                                 │
│                                                                             │
│  ┌────────────────────────┐    ┌────────────────────────────────────────┐   │
│  │   SimulationState      │───▶│  SimulationView (immutable snapshot)   │   │
│  │   (mutable, internal)  │    │  (passed to strategies)                │   │
│  └────────────────────────┘    └────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                      │
│  │   People    │    │  Accounts   │    │   Events    │                      │
│  │   (M1/M4)   │    │   (M2/M3)   │    │  (triggers) │                      │
│  └─────────────┘    └─────────────┘    └─────────────┘                      │
│                                                                             │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                      MONTHLY SIMULATION LOOP                         │   │
│  │                                                                      │   │
│  │  1. Apply market returns to accounts                                 │   │
│  │  2. Process income (salary, SS, pension, annuity)                    │   │
│  │  3. Calculate expenses (budget + inflation)                          │   │
│  │  4. Determine phase (accumulation vs distribution)                   │   │
│  │  5. Execute contributions OR withdrawals                             │   │
│  │  6. Process events (triggers)                                        │   │
│  │  7. Record to time series                                            │   │
│  │  8. Advance to next month                                            │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                     TimeSeries<MonthlySnapshot>                             │
│                                                                             │
│  Month 1 → Month 2 → Month 3 → ... → Month N                                │
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

**Decision:** Monthly granularity is correct. Annual snapshots hide important nuance:
- Random events (contingency expenses) in one month ripple to future months
- Market volatility is masked by annual averaging
- Sequence-of-returns risk only visible at monthly level

**Decision:** Net flows per account per month (no individual transactions).

- Monthly is the finest granularity needed
- Store aggregated flows, not individual transactions
- Reduces memory over multi-decade simulations

```java
public record AccountMonthlyFlow(
    UUID accountId,
    BigDecimal startingBalance,
    BigDecimal contributions,
    BigDecimal withdrawals,
    BigDecimal returns,
    BigDecimal endingBalance
) {}

// In MonthlySnapshot:
Map<UUID, AccountMonthlyFlow> accountFlows;  // replaces List<Transaction>
```

---

### 2. TimeSeries with Annual Aggregation

**Decision:** Monthly simulation with annual aggregation *methods* in TimeSeries.

```java
public class TimeSeries<T extends MonthlySnapshot> {
    private final List<T> snapshots;

    // Monthly access
    public T getSnapshot(YearMonth month) { ... }
    public List<T> getRange(YearMonth start, YearMonth end) { ... }

    // Annual aggregation (computed from monthly data)
    public AnnualSummary getAnnualSummary(int year) { ... }
    public List<AnnualSummary> getAllAnnualSummaries() { ... }
}

public record AnnualSummary(
    int year,
    BigDecimal startingBalance,
    BigDecimal endingBalance,
    BigDecimal totalContributions,
    BigDecimal totalWithdrawals,
    BigDecimal totalIncome,
    BigDecimal totalExpenses,
    BigDecimal annualReturn,
    BigDecimal annualReturnPercent,
    List<String> significantEvents
) {}
```

**Loop Structure:**
```
For each month from start to end:
    1. Process income (salary, SS, pension, annuity)
    2. Calculate expenses (budget + inflation)
    3. Determine phase (accumulation vs distribution)
    4. Execute contributions OR withdrawals
    5. Apply monthly returns to post-transaction balances
    6. Process events (triggers)
    7. Record MonthlySnapshot

    If (month == December):
        - Trigger annual events (RMD calculation, rebalancing)
        - Reset YTD accumulators
```

**Monthly Return Calculation:**

Returns applied AFTER transactions, using monthly compounding:

```java
// Monthly return from annual rate
BigDecimal monthlyMultiplier = BigDecimal.valueOf(
    Math.pow(1 + annualReturn.doubleValue(), 1.0 / 12)
);

// Example: $1M, withdraw $10K, 8% annual return
// 1. Start: $1,000,000
// 2. Withdraw: $1,000,000 - $10,000 = $990,000
// 3. Apply return: $990,000 × (1.08^(1/12)) = $990,000 × 1.00643 ≈ $996,369
```

**Why this order?**
- Transactions first, then returns = conservative modeling
- Withdrawal reduces balance before gains applied
- Contribution increases balance before gains applied
- More realistic: money withdrawn doesn't earn that month's return

**Decision:** All contributions modeled as monthly.

- Real-world pay frequencies vary (bi-weekly, semi-monthly, monthly)
- Simulation assumes monthly for simplicity
- Annual contribution = monthly × 12
- Employer match calculated on monthly contribution

**Decision:** RMDs handled by `RmdAwareOrchestrator` (M6).

- Annual RMD calculated from prior year-end balance
- Orchestrator divides by 12 for monthly minimum
- Each month: `effectiveWithdrawal = max(strategyTarget, monthlyRmd)`
- 12 monthly portions = annual requirement satisfied
- No special annual logic needed in simulation engine

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

**Decision:** Phase is determined *per person*, not per household.

**Staggered Retirement Scenario:**
```
Year 1-3:  Spouse1 = ACCUMULATION, Spouse2 = ACCUMULATION
Year 4-5:  Spouse1 = DISTRIBUTION,  Spouse2 = ACCUMULATION  ← staggered
Year 6+:   Spouse1 = DISTRIBUTION,  Spouse2 = DISTRIBUTION  ← both retired
```

During staggered phase:
- **Contributions** come from working spouse's income → their accounts
- **Distributions** come from retired spouse's accounts (or joint if needed)
- Account selection respects ownership (whose 401k, whose IRA)

When both retired, all accounts available for distribution strategy.

**Social Security Claiming:**
- Each spouse has their own `ssClainingAge` (e.g., 62, 67, 70)
- SS benefit starts **first month after reaching claiming age**
- Example: Spouse1 born March 1960, claims at 67 → SS starts April 2027

```java
YearMonth getSocialSecurityStartMonth(PersonProfile person) {
    LocalDate claimingBirthday = person.getBirthDate()
        .plusYears(person.getSsClaimingAge());
    return YearMonth.from(claimingBirthday).plusMonths(1);
}
```

**Decision:** Part-time work in retirement = "other income", not contributions.

- Once retired, contributions stop (no more 401k/IRA contributions)
- Part-time/consulting income modeled as `OtherIncome` (M4)
- This income reduces the gap, potentially reducing withdrawals needed
- Simplifies phase logic: ACCUMULATION = contributing, DISTRIBUTION = not contributing

```
Retired + part-time work:
  - Phase = DISTRIBUTION
  - OtherIncome += part-time earnings
  - Gap = Expenses - (SS + Pension + OtherIncome)
  - Withdrawal = Gap (if positive)
```

---

### 4. Event System

**Question:** How are events registered and fired?

**Decision:** Events can be **deterministic**, **probabilistic**, or **random**.

| Type | When Known | Example |
|------|------------|---------|
| **Deterministic** | Pre-registered at start | Retirement date, SS claiming, mortgage payoff |
| **Probabilistic** | Derived from tables + variance | Death (actuarial ± 5 years), LTC need |
| **Random** | Monte Carlo draw each run | Market crash, job loss, windfall |

**Event Interface:**
```java
public interface SimulationEvent {
    String getName();
    String getDescription();
    void execute(SimulationContext context);
}

// Deterministic: known trigger date
public interface ScheduledEvent extends SimulationEvent {
    YearMonth getTriggerDate();
}

// Probabilistic: may or may not occur, with probability
public interface ProbabilisticEvent extends SimulationEvent {
    boolean shouldTrigger(SimulationContext context, RandomGenerator random);
    default BigDecimal getBaseProbability() { return BigDecimal.ZERO; }
}
```

**Deterministic Events (pre-registered):**
- `RetirementStartEvent` - from PersonProfile.retirementDate
- `SocialSecurityStartEvent` - from PersonProfile.ssClaimingAge
- `RmdStartEvent` - from birth year + SECURE 2.0 rules
- `MortgagePayoffEvent` - from expense end date
- `SpendingPhaseTransitionEvent` - from age brackets (Go-Go → Slow-Go)

**Probabilistic Events:**
```java
public class SpouseDeathEvent implements ProbabilisticEvent {
    private final PersonProfile spouse;
    private final ActuarialTable table;
    private final int varianceYears; // e.g., ±5 years

    @Override
    public boolean shouldTrigger(SimulationContext ctx, RandomGenerator random) {
        int expectedAge = table.getLifeExpectancy(spouse);
        int actualAge = expectedAge + random.nextInt(-varianceYears, varianceYears + 1);
        return spouse.getAgeAt(ctx.currentMonth()) >= actualAge;
    }
}
```

**Event Execution Flow:**
```
Each month:
    1. Check deterministic events (is trigger date == current month?)
    2. Check probabilistic events (roll dice based on context)
    3. Execute triggered events (may modify future state)
    4. Record triggered events in MonthlySnapshot
```

**Decision:** Events set flags/modify state; calculators react to flags.

**Survivor Event Flow:**
```
SpouseDeathEvent fires
    → Set survivorMode = true in SimulationContext
    → SurvivorExpenseCalculator (M5) applies adjustments when flag set
    → Housing 70%, Food 60%, Healthcare 100%, etc.
```

**Contingency Event Flow:**
```
ContingencyExpenseEvent fires (e.g., "Car repair $2000")
    │
    ├─ Check ContingencyReserve balance for category
    │   ├─ Sufficient → Withdraw from contingency fund
    │   └─ Insufficient → Withdraw available + remainder from portfolio
    │
    └─ Set refillMode for that category
        → Subsequent months redirect cash flow to refill
        → Until target balance reached
```

**Key Principle:** Events modify flags/state, calculators are stateless and check flags each month. This keeps calculators pure and testable.

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

### 6. SimulationEngine and SimulationView

**Question:** How does the engine relate to M6's `SimulationView`?

**Decision:** Engine *produces* views, not *is* a view.

- **SimulationEngine** = orchestrator (runs loop, processes events, calls strategies)
- **SimulationState** = mutable internal state (owned by engine)
- **SimulationView** = immutable snapshot (passed to strategies)

```java
// Engine orchestrates the simulation
public class SimulationEngine {
    private SimulationState state;  // mutable, internal

    public TimeSeries<MonthlySnapshot> run(SimulationConfig config) {
        TimeSeries<MonthlySnapshot> timeSeries = new TimeSeries<>();

        for (YearMonth month = config.startMonth();
             month.isBefore(config.endMonth());
             month = month.plusMonths(1)) {

            // 1. Create immutable view of current state
            SimulationView view = state.snapshot();

            // 2. Build context with view
            SpendingContext context = buildContext(view, month);

            // 3. Execute strategy (receives immutable view)
            SpendingPlan plan = orchestrator.execute(strategy, context);

            // 4. Apply plan to mutable state
            state.apply(plan);

            // 5. Record snapshot
            timeSeries.add(state.toMonthlySnapshot(month));
        }
        return timeSeries;
    }
}

// Mutable state owned by engine
public class SimulationState {
    private final Map<UUID, AccountState> accounts;
    private final List<MonthlySnapshot> history;
    private BigDecimal initialPortfolioBalance;
    private boolean survivorMode;
    private Map<ContingencyType, Boolean> refillModes;

    // Create immutable view for strategies
    public SimulationView snapshot() {
        return new SimulationViewSnapshot(
            copyAccountSnapshots(),
            calculateTotalBalance(),
            initialPortfolioBalance,
            getPriorYearSpending(),
            getPriorYearReturn(),
            getLastRatchetMonth()
        );
    }

    // Mutate state based on plan
    public void apply(SpendingPlan plan) {
        for (AccountWithdrawal withdrawal : plan.accountWithdrawals()) {
            accounts.get(withdrawal.accountId()).withdraw(withdrawal.amount());
        }
    }
}

// Immutable view passed to strategies (implements M6 interface)
public record SimulationViewSnapshot(
    List<AccountSnapshot> accountSnapshots,
    BigDecimal totalPortfolioBalance,
    BigDecimal initialPortfolioBalance,
    BigDecimal priorYearSpending,
    BigDecimal priorYearReturn,
    Optional<YearMonth> lastRatchetMonth
) implements SimulationView {

    @Override
    public List<AccountSnapshot> getAccountSnapshots() {
        return accountSnapshots;  // already immutable
    }

    @Override
    public BigDecimal getTotalPortfolioBalance() {
        return totalPortfolioBalance;
    }

    // ... other SimulationView methods
}
```

**Key Principle:** Strategies receive immutable views. Only the engine mutates state.

---

### 7. Simulation Modes

**Question:** How do different modes affect execution?

| Mode | Return Source | Use Case |
|------|---------------|----------|
| DETERMINISTIC | Fixed rate (e.g., 7%) | Base case planning |
| MONTE_CARLO | Random draw from distribution | Probability analysis |
| HISTORICAL | Actual market sequence | Stress testing |

**Deterministic Mode:**
- Single run with fixed assumptions
- Good for "what if" scenario planning
- Returns `TimeSeries<MonthlySnapshot>`

**Monte Carlo Mode:** → See Research Issue #268
- Requires separate research on:
  - Number of runs (convergence)
  - Virtual threading for parallelism
  - Seed handling for reproducibility
  - Aggregation model (`MonteCarloResult`)
  - Return distributions
- Returns `MonteCarloResult` containing multiple `TimeSeries` + statistics

**Historical Mode:**
- Replay actual market sequences (e.g., 1966 start, 2000 start, 2008 start)
- Tests sequence-of-returns risk with real data

**Decision:** Store historical returns as YAML configuration.

```yaml
# application-historical-returns.yml
historical-returns:
  stocks:
    - { year: 1926, month: 1, return: 0.0012 }
    - { year: 1926, month: 2, return: -0.0034 }
    # ... through present
  bonds:
    - { year: 1976, month: 1, return: 0.0008 }
    # ... through present
  inflation:
    - { year: 1926, month: 1, rate: 0.0015 }
    # ... through present
```

**Data Sources:**
- S&P 500: Shiller data (1926-present)
- Bonds: 10-year Treasury / Barclays Aggregate (1976-present)
- Inflation: BLS CPI data (1913-present)

**Usage:**
```java
public record HistoricalReturns(
    Map<YearMonth, BigDecimal> stockReturns,
    Map<YearMonth, BigDecimal> bondReturns,
    Map<YearMonth, BigDecimal> inflationRates
) {
    public BigDecimal getBlendedReturn(YearMonth month, BigDecimal stockAllocation) {
        BigDecimal stockReturn = stockReturns.get(month);
        BigDecimal bondReturn = bondReturns.get(month);
        return stockReturn.multiply(stockAllocation)
            .add(bondReturn.multiply(BigDecimal.ONE.subtract(stockAllocation)));
    }
}
```

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
