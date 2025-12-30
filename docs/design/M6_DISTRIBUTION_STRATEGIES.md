# M6: Distribution Strategies Design

**Status:** Research Complete → Architecture Revised
**Date:** December 29, 2025
**Last Revised:** December 29, 2025
**Milestone:** 6

---

## Revision History

| Date | Change | Reason |
|------|--------|--------|
| 2025-12-29 | Added Simulation Integration Architecture | Design discussion revealed need for clean separation between simulation state and strategy calculations |
| 2025-12-29 | Introduced `SimulationView` interface | Strategies need read-only access to both current balances and historical data |
| 2025-12-29 | Refactored `SpendingContext` | Removed embedded historical fields; now uses `SimulationView` |

---

## Research References

| Topic | Document | Key Findings |
|-------|----------|--------------|
| Guardrails | [GUARDRAILS_RESEARCH.md](../research/GUARDRAILS_RESEARCH.md) | 3 approaches: Guyton-Klinger, Vanguard, Kitces |
| Bucket Strategy | [BUCKET_STRATEGY_RESEARCH.md](../research/BUCKET_STRATEGY_RESEARCH.md) | 3-bucket model, hybrid refill |
| Tax Sequencing | [TAX_SEQUENCING_RESEARCH.md](../research/TAX_SEQUENCING_RESEARCH.md) | RMD-first, bracket-aware |

---

## Overview

M6 implements retirement withdrawal strategies that determine how much to withdraw and from which accounts. This builds on M5's expense/budget modeling and integrates with existing calculators.

---

## Simulation Integration Architecture

> **Key Architectural Decision:** The simulation engine owns all mutable state (account balances, time progression, market returns). Strategies are pure calculators that receive read-only views of simulation state and return withdrawal plans. This separation enables clean support for deterministic, Monte Carlo, and historical backtesting simulation modes.

### The Problem

Early design had strategies receiving a `SpendingContext` with embedded historical fields (`priorYearSpending`, `priorYearPortfolioReturn`, `yearsSinceLastRatchet`). This created several issues:

1. **State ownership ambiguity:** Who updates these fields between periods?
2. **Strategy-specific leakage:** `yearsSinceLastRatchet` is Kitces-specific but was in the general context
3. **Simulation mode coupling:** Strategies couldn't be agnostic to whether they're in Monte Carlo run #847 or deterministic mode
4. **Testing complexity:** Hard to test strategies in isolation without simulating full history

### The Solution: SimulationView

The simulation engine exposes a **read-only interface** that strategies use to query both current state and historical data:

```java
/**
 * Read-only view of simulation state for strategy calculations.
 *
 * The simulation engine implements this interface, backed by its internal
 * state and TimeSeries history. Strategies can query what they need without
 * coupling to simulation internals.
 */
public interface SimulationView {

    // ─── Current State (as of this simulation step) ───────────────────────

    /** Current balance for a specific account */
    BigDecimal getAccountBalance(UUID accountId);

    /** Total portfolio balance across all accounts */
    BigDecimal getTotalPortfolioBalance();

    /** All accounts with current balances (read-only snapshots) */
    List<AccountSnapshot> getAccountSnapshots();

    /** Portfolio balance at retirement start (for 4% rule calculations) */
    BigDecimal getInitialPortfolioBalance();

    // ─── Historical Queries (for dynamic strategies) ──────────────────────

    /** Total spending in the prior calendar year */
    BigDecimal getPriorYearSpending();

    /** Portfolio return (%) in the prior calendar year */
    BigDecimal getPriorYearReturn();

    /** Find when a spending ratchet last occurred (for Kitces) */
    Optional<YearMonth> getLastRatchetMonth();

    /** Cumulative withdrawals since retirement start */
    BigDecimal getCumulativeWithdrawals();

    /** Highest portfolio balance achieved (for drawdown calculations) */
    BigDecimal getHighWaterMarkBalance();

    /** Access to full history if needed (optional, for complex strategies) */
    Optional<TimeSeries<MonthlySnapshot>> getHistory();
}

/**
 * Immutable snapshot of an account's state at a point in time.
 * Contains all info needed by strategies and sequencers without exposing mutable Portfolio.
 */
public record AccountSnapshot(
    UUID accountId,
    String accountName,
    AccountType accountType,
    BigDecimal balance,
    AccountType.TaxTreatment taxTreatment,
    boolean subjectToRmd,           // For RMD sequencing
    AssetAllocation allocation      // For bucket strategy, rebalancing
) {
    /** Factory method to create from InvestmentAccount */
    public static AccountSnapshot from(InvestmentAccount account) { ... }
}
```

### Updated SpendingContext

With `SimulationView` handling state access, `SpendingContext` becomes a lean container for **current period inputs**:

```java
/**
 * Context for a single period's spending calculation.
 *
 * Contains current-period inputs (what's happening NOW) and a reference
 * to SimulationView for historical/balance queries.
 */
public record SpendingContext(
    // ─── Simulation State Access ──────────────────────────────────────────
    SimulationView simulation,

    // ─── Current Period Inputs ────────────────────────────────────────────
    LocalDate date,
    BigDecimal totalExpenses,
    BigDecimal otherIncome,          // SS, pension, etc.

    // ─── Person/Scenario Context ──────────────────────────────────────────
    int age,
    int birthYear,
    LocalDate retirementStartDate,

    // ─── Tax Context ──────────────────────────────────────────────────────
    FilingStatus filingStatus,
    BigDecimal currentTaxableIncome,

    // ─── Strategy Configuration ───────────────────────────────────────────
    Map<String, Object> strategyParams
) {
    // Convenience methods delegate to SimulationView
    public BigDecimal currentPortfolioBalance() {
        return simulation.getTotalPortfolioBalance();
    }

    public long monthsInRetirement() {
        return ChronoUnit.MONTHS.between(retirementStartDate, date);
    }

    public int yearsInRetirement() {
        return (int) (monthsInRetirement() / 12);
    }

    public BigDecimal incomeGap() {
        return totalExpenses.subtract(otherIncome).max(BigDecimal.ZERO);
    }
}
```

### Execution Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         SIMULATION ENGINE (M7)                              │
│                                                                             │
│  Owns: Account Balances, TimeSeries History, Time Progression               │
│  Implements: SimulationView interface                                       │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ Each simulation step:
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  1. Apply market returns to account balances                                │
│     (Deterministic: fixed rate | Monte Carlo: random draw | Historical)     │
│                                                                             │
│  2. Build SpendingContext with SimulationView reference                     │
│     ┌───────────────────────────────────────────────────────────────────┐   │
│     │ SpendingContext {                                                 │   │
│     │   simulation: this,        // SimulationView                      │   │
│     │   date: currentDate,                                              │   │
│     │   totalExpenses: 6000,                                            │   │
│     │   otherIncome: 3000,       // SS started                          │   │
│     │   age: 68,                                                        │   │
│     │   ...                                                             │   │
│     │ }                                                                 │   │
│     └───────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│                                    ▼                                        │
│  3. Call Orchestrator                                                       │
│     ┌──────────────────────────────────────────────────────────────────┐    │
│     │ orchestrator.execute(strategy, sequencer, context)               │    │
│     │                                                                  │    │
│     │   Strategy: "I need $3,000 this month"                           │    │
│     │     └─ may call context.simulation.getPriorYearSpending()        │    │
│     │     └─ may call context.simulation.getTotalPortfolioBalance()    │    │
│     │                                                                  │    │
│     │   Sequencer: "Withdraw from: 401k → IRA → Roth"                  │    │
│     │     └─ queries account balances via SimulationView               │    │
│     │                                                                  │    │
│     │   Returns: SpendingPlan with AccountWithdrawals                  │    │
│     └──────────────────────────────────────────────────────────────────┘    │
│                                    │                                        │
│                                    ▼                                        │
│  4. Execute SpendingPlan                                                    │
│     - Update account balances (authoritative state)                         │
│     - Record to TimeSeries history                                          │
│                                                                             │
│  5. Advance to next period                                                  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Simulation Mode Agnosticism

Strategies don't know or care which simulation mode is running. They see "reality" through `SimulationView`:

| Mode | How Returns Are Applied | What Strategy Sees |
|------|------------------------|-------------------|
| **Deterministic** | Fixed rate (e.g., 7% annually) | Steady balance growth |
| **Monte Carlo** | Random draw from return distribution | Variable balances per run |
| **Historical** | Actual market data (e.g., 2008 crash) | Historical sequence of returns |

Example: Monte Carlo run #847 has a -10% year:
```
Before: Portfolio = $1,000,000
After returns: Portfolio = $900,000
Strategy calls: context.simulation.getTotalPortfolioBalance() → $900,000
Guardrails strategy: "Withdrawal rate too high! Cut spending 10%"
```

The strategy reacts to current reality. The simulation controls what that reality is.

### RMD Interaction

RMDs are **mandatory minimums** that override strategy calculations:

```
Strategy says: "Withdraw $3,000/month"
RMD requires:  "$5,000 from Traditional IRA this month"

Orchestrator ensures:
  1. At least $5,000 comes from IRA (satisfies RMD)
  2. If $5,000 > $3,000 spending need, excess goes to:
     - Taxable brokerage (reinvest)
     - Cash reserve
     - Or increases spending if lifestyle allows
```

The `RmdFirstSequencer` prioritizes RMD-subject accounts so RMDs are naturally satisfied during normal withdrawal sequencing.

### Benefits of This Architecture

1. **Clean separation of concerns**
   - Simulation: owns state, controls time, applies returns
   - Strategy: pure calculation given current state
   - SimulationView: read-only bridge between them

2. **Testable strategies**
   - Mock `SimulationView` interface
   - No need to simulate full history for unit tests

3. **Simulation mode flexibility**
   - Same strategy code works for all modes
   - Mode selection is purely a simulation concern

4. **Monte Carlo support**
   - Each run starts with fresh `TimeSeries`
   - No strategy state to snapshot/restore
   - Strategies query history naturally scoped to current run

5. **Historical data access**
   - Dynamic strategies (Guardrails, Kitces) query what they need
   - No need to pre-compute all possible historical metrics

---

## Existing Foundation

| Component | Purpose |
|-----------|---------|
| `WithdrawalCalculator` | 4% rule, income gap, inflation adjustment |
| `GapAnalyzer` | Income vs expense gap analysis |
| `RmdCalculator` | Required minimum distributions |
| `FederalTaxCalculator` | Tax bracket calculations |
| `SpendingCurveModifier` | Go-Go/Slow-Go/No-Go expense adjustment |
| `DistributionStrategy` enum | TAX_EFFICIENT, PRO_RATA, CUSTOM |
| `AccountType.TaxTreatment` | PRE_TAX, POST_TAX, TAX_FREE |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         SIMULATION ENGINE (M7)                              │
│                                                                             │
│  Owns: Account Balances, TimeSeries History, Market Returns                 │
│  Implements: SimulationView interface                                       │
└─────────────────────────────────────────────────────────────────────────────┘
         │
         │ provides read-only view
         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           SimulationView                                    │
│  (Read-only interface to current balances + historical queries)             │
└─────────────────────────────────────────────────────────────────────────────┘
         │
         │ injected into
         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           SpendingContext                                   │
│  (Current period: date, expenses, income, age + SimulationView reference)   │
└─────────────────────────────────────────────────────────────────────────────┘
         │
         │ passed to
         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        SpendingOrchestrator                                 │
│  (Coordinates strategy + sequencing + RMD + execution)                      │
└─────────────────────────────────────────────────────────────────────────────┘
                              │
          ┌───────────────────┼───────────────────┐
          ▼                   ▼                   ▼
┌──────────────────┐  ┌──────────────────┐  ┌─────────────────┐
│ SpendingStrategy │  │ AccountSequencer │  │  RmdCalculator  │
│   (HOW MUCH)     │  │ (WHICH ACCOUNTS) │  │  (MANDATORY)    │
└──────────────────┘  └──────────────────┘  └─────────────────┘
          │
    ┌─────┴─────┬─────────┬───────────┐
    ▼           ▼         ▼           ▼
┌───────┐  ┌────────┐  ┌───────┐  ┌──────────┐
│Static │  │ Bucket │  │Income │  │Guardrails│
│  4%   │  │Strategy│  │  Gap  │  │ Dynamic  │
└───────┘  └────────┘  └───────┘  └──────────┘
```

---

## Sub-Milestones

### M6a: Strategy Framework (~13 points) ✅ COMPLETE (with revisions pending)

**Goal:** Establish the strategy pattern foundation.

**Status:** Core interfaces implemented. Requires refactoring per Simulation Integration Architecture.

#### Implemented (Current)

| Component | Status | Notes |
|-----------|--------|-------|
| `SpendingStrategy` | ✅ Done | Interface for withdrawal calculations |
| `SpendingContext` | ⚠️ Needs Refactor | Must add `SimulationView`, remove historical fields |
| `SpendingPlan` | ✅ Done | Withdrawal result record |
| `AccountWithdrawal` | ✅ Done | Per-account withdrawal details |
| `AccountSequencer` | ✅ Done | Interface for account ordering |
| `TaxEfficientSequencer` | ✅ Done | Standard tax-efficient order |
| `RmdFirstSequencer` | ✅ Done | RMD accounts prioritized |
| `SpendingOrchestrator` | ✅ Done | Coordinates strategy + sequencing |
| `DefaultSpendingOrchestrator` | ✅ Done | Default implementation |

#### New Components Needed (per Architecture Revision)

```java
/**
 * Read-only view of simulation state for strategy calculations.
 * Implemented by the simulation engine (M7).
 *
 * For M6 testing, a mock/stub implementation will be provided.
 */
public interface SimulationView {
    // Current state
    BigDecimal getAccountBalance(UUID accountId);
    BigDecimal getTotalPortfolioBalance();
    List<AccountSnapshot> getAccountSnapshots();
    BigDecimal getInitialPortfolioBalance();

    // Historical queries (for dynamic strategies)
    BigDecimal getPriorYearSpending();
    BigDecimal getPriorYearReturn();
    Optional<YearMonth> getLastRatchetMonth();
    BigDecimal getCumulativeWithdrawals();
    BigDecimal getHighWaterMarkBalance();
    Optional<TimeSeries<MonthlySnapshot>> getHistory();
}

/**
 * Immutable snapshot of account state for strategy queries.
 */
public record AccountSnapshot(
    UUID accountId,
    String accountName,
    AccountType accountType,
    BigDecimal balance,
    AccountType.TaxTreatment taxTreatment
) {}

/**
 * Stub implementation for M6 testing (before M7 exists).
 */
public class StubSimulationView implements SimulationView {
    private final Portfolio portfolio;
    private final BigDecimal initialBalance;
    // ... stub implementations for historical queries
}
```

#### SpendingContext Refactoring

**Remove these fields** (now queried via SimulationView):
- `Portfolio portfolio` → use `simulation.getAccountSnapshots()`
- `BigDecimal priorYearSpending` → use `simulation.getPriorYearSpending()`
- `BigDecimal priorYearPortfolioReturn` → use `simulation.getPriorYearReturn()`
- `int yearsSinceLastRatchet` → derive from `simulation.getLastRatchetMonth()`
- `BigDecimal initialPortfolioBalance` → use `simulation.getInitialPortfolioBalance()`

**Keep these fields** (current period inputs):
- `SimulationView simulation` (NEW)
- `LocalDate date`
- `BigDecimal totalExpenses`
- `BigDecimal otherIncome`
- `int age`
- `int birthYear`
- `LocalDate retirementStartDate`
- `FilingStatus filingStatus`
- `BigDecimal currentTaxableIncome`
- `Map<String, Object> strategyParams`

#### Existing Interfaces (Already Implemented)

```java
// Core strategy interface
public interface SpendingStrategy {
    SpendingPlan calculateWithdrawal(SpendingContext context);
    String getName();
    String getDescription();
    default boolean isDynamic() { return false; }
    default boolean requiresPriorYearState() { return false; }
}

// Result from strategy (already implemented)
public record SpendingPlan(
    BigDecimal targetWithdrawal,
    BigDecimal adjustedWithdrawal,
    List<AccountWithdrawal> accountWithdrawals,
    boolean meetsTarget,
    BigDecimal shortfall,
    String strategyUsed,
    Map<String, Object> metadata
) {}

public record AccountWithdrawal(
    UUID accountId,
    String accountName,
    AccountType accountType,
    BigDecimal amount,
    BigDecimal priorBalance,
    BigDecimal newBalance,
    TaxTreatment taxTreatment
) {}
```

#### Account Sequencing (Enhanced per Research)

```java
public interface AccountSequencer {
    /**
     * Sequences accounts for withdrawal order.
     * Account balances are accessed via context.simulation().getAccountSnapshots()
     */
    List<AccountSnapshot> sequence(SpendingContext context);
    String getName();
}

// Standard: Taxable → Traditional → Roth
public class TaxEfficientSequencer implements AccountSequencer { }

// Proportional from all accounts
public class ProRataSequencer implements AccountSequencer { }

// RMD accounts first, then tax-efficient (DEFAULT when RMDs apply)
public class RmdFirstSequencer implements AccountSequencer {
    private final RmdCalculator rmdCalculator;
}

// User-defined order
public class CustomSequencer implements AccountSequencer {
    private final List<AccountType> customOrder;
}
```

#### Sequencer Selection Logic (from Research)

```java
public AccountSequencer selectSequencer(SpendingContext ctx, RmdCalculator rmdCalc) {
    // 1. If RMDs required, use RMD-first
    if (rmdCalc.isRmdRequired(ctx.age(), ctx.birthYear())) {
        return new RmdFirstSequencer(rmdCalc);
    }
    // 2. Default: simple tax-efficient
    return new TaxEfficientSequencer();
}
```

#### Orchestrator

```java
public interface SpendingOrchestrator {
    /**
     * Executes withdrawal using strategy and sequencer.
     * All state accessed via context.simulation() (SimulationView)
     */
    SpendingPlan execute(
        SpendingStrategy strategy,
        AccountSequencer sequencer,
        SpendingContext context
    );
}
```

**Issues:**
1. ~~Create SpendingStrategy interface and SpendingPlan record~~ ✅ #223
2. ~~Create SpendingContext record~~ ✅ #224 (⚠️ needs refactor)
3. ~~Create AccountSequencer interface and AccountWithdrawal record~~ ✅ #225
4. ~~Implement TaxEfficientSequencer~~ ✅ #226
5. ~~Implement RmdFirstSequencer~~ ✅ #227
6. ~~Create SpendingOrchestrator interface~~ ✅ #228

**New Issues (Architecture Revision):**
7. Create SimulationView interface and AccountSnapshot record (3)
8. Create StubSimulationView for M6 testing (2)
9. Refactor SpendingContext to use SimulationView (3)
10. Update existing tests to use StubSimulationView (2)

---

### M6b: Static & Income-Gap Strategies (~8 points)

**Goal:** Implement basic withdrawal strategies.

#### Static Strategy (4% Rule)

```java
public class StaticWithdrawalStrategy implements WithdrawalStrategy {
    private final BigDecimal withdrawalRate;      // e.g., 0.04
    private final BigDecimal inflationRate;
    private final boolean adjustForInflation;

    @Override
    public WithdrawalPlan calculateWithdrawal(WithdrawalContext ctx) {
        // Year 1: initialBalance * rate / 12
        // Year N: year1Amount * (1 + inflation)^(N-1) / 12
    }
}
```

#### Income-Gap Strategy

```java
public class IncomeGapStrategy implements WithdrawalStrategy {
    private final GapAnalyzer gapAnalyzer;
    private final BigDecimal marginalTaxRate;

    @Override
    public WithdrawalPlan calculateWithdrawal(WithdrawalContext ctx) {
        // Gap = expenses - otherIncome
        // Gross up for taxes if withdrawing from pre-tax
    }
}
```

**Issues:**
1. Implement StaticWithdrawalStrategy (3)
2. Implement IncomeGapStrategy with GapAnalyzer integration (3)
3. Tests for static/income-gap strategies (2)

---

### M6c: Bucket Strategy (~15 points)

**Goal:** Time-segmented withdrawal approach.

**Research:** Standard 3-bucket model with hybrid refill strategy.

#### Model (Enhanced per Research)

```java
public record Bucket(
    String name,
    BucketType type,
    int minHorizonYears,
    int maxHorizonYears,
    BigDecimal targetBalance,      // In years of expenses
    BigDecimal currentBalance,
    AssetAllocation allocation
) {
    public boolean needsRefill(BigDecimal threshold) {
        return currentBalance.divide(targetBalance).compareTo(threshold) < 0;
    }
}

public enum BucketType {
    SHORT_TERM(1, 2, "Cash, money market", new BigDecimal("0.03")),
    MEDIUM_TERM(3, 7, "Bonds, balanced", new BigDecimal("0.05")),
    LONG_TERM(8, 30, "Equities, growth", new BigDecimal("0.08"));
}

public record BucketConfiguration(
    List<Bucket> buckets,
    BigDecimal refillThreshold,     // 0.50 or 0.75
    RefillTrigger refillTrigger,
    int refillFrequencyMonths,
    boolean onlyRefillOnPositiveReturns
) {
    public static BucketConfiguration standard() { ... }
}

public enum RefillTrigger {
    THRESHOLD,    // When bucket < threshold
    CALENDAR,     // On schedule
    MARKET,       // Only when markets up
    HYBRID        // Threshold + market awareness (recommended)
}
```

#### Refill Calculator

```java
public interface BucketRefillCalculator {
    List<BucketTransfer> calculateRefills(
        BucketConfiguration config,
        List<Bucket> currentBuckets,
        BigDecimal longTermYtdReturn
    );
}

public record BucketTransfer(
    BucketType source,
    BucketType destination,
    BigDecimal amount,
    String reason
) {}
```

**Issues:**
1. Create Bucket, BucketType, BucketConfiguration models (3)
2. Create RefillTrigger enum and BucketTransfer record (2)
3. Implement BucketWithdrawalStrategy (5)
4. Implement BucketRefillCalculator with hybrid logic (3)
5. Bucket strategy tests (2)

---

### M6d: Guardrails Strategy (~15 points)

**Goal:** Dynamic withdrawals that adjust based on portfolio performance.

**Research:** Supports Guyton-Klinger, Vanguard Dynamic, and Kitces Ratcheting approaches.

#### Configuration (Enhanced per Research)

```java
public record GuardrailsConfiguration(
    // Core parameters
    BigDecimal initialWithdrawalRate,
    BigDecimal inflationRate,

    // Upper guardrail (prosperity) - triggers spending INCREASE
    BigDecimal upperThresholdMultiplier,  // 0.80 = rate drops to 80% of initial
    BigDecimal increaseAdjustment,        // 0.10 = increase by 10%

    // Lower guardrail (capital preservation) - triggers spending DECREASE
    BigDecimal lowerThresholdMultiplier,  // 1.20 = rate rises to 120% of initial
    BigDecimal decreaseAdjustment,        // 0.10 = decrease by 10%

    // Constraints
    BigDecimal absoluteFloor,             // Minimum (essential expenses)
    BigDecimal absoluteCeiling,           // Maximum (lifestyle cap)

    // Behavior flags
    boolean allowSpendingCuts,            // false for Kitces
    boolean skipInflationOnDownYears,     // Guyton-Klinger Rule 2
    int minimumYearsBetweenRatchets,      // 3 for Kitces
    int yearsBeforeCapPreservationEnds    // 15 for Guyton-Klinger
) {
    // Preset factory methods
    public static GuardrailsConfiguration guytonKlinger() { ... }
    public static GuardrailsConfiguration vanguardDynamic() { ... }
    public static GuardrailsConfiguration kitcesRatcheting() { ... }
}
```

#### Preset Comparison (from Research)

| Parameter | Guyton-Klinger | Vanguard | Kitces |
|-----------|----------------|----------|--------|
| Initial Rate | 5.2% | 4-5% | 4% |
| Spending Cuts? | Yes (10%) | Yes (2.5% max) | No |
| Spending Increases? | Yes (10%) | Yes (5% max) | Yes (10%) |
| Trigger | Rate-based | Amount-based | Portfolio growth |
| Min Years Between | 1 | 1 | 3 |

**Issues:**
1. Create GuardrailsConfiguration with preset factories (3)
2. Implement GuardrailsWithdrawalStrategy core logic (5)
3. Implement Guyton-Klinger 4-rule logic (3)
4. Implement Kitces ratchet-only logic (2)
5. Guardrails tests for all presets (2)

---

### M6e: Integration & RMD Coordination (~8 points)

**Goal:** Integrate RMDs and test end-to-end flows.

#### RMD Integration

```java
public class RmdAwareOrchestrator implements WithdrawalOrchestrator {
    private final RmdCalculator rmdCalculator;

    @Override
    public WithdrawalPlan execute(...) {
        // 1. Calculate RMD for each applicable account
        // 2. Satisfy RMD first
        // 3. Apply strategy for remaining need
        // 4. Combine into final plan
    }
}
```

**Issues:**
1. Implement RmdAwareOrchestrator (3)
2. End-to-end integration tests (3)
3. Documentation and examples (2)

---

## Test Scenarios

| Scenario | Strategy | Expected Behavior |
|----------|----------|-------------------|
| Early retiree, no SS yet | Income Gap | Full gap from portfolio |
| Age 73, Traditional IRA | Any + RMD | RMD satisfied first |
| Portfolio up 30% | Guardrails | May trigger increase |
| Portfolio down 25% | Guardrails | Decrease to floor |
| Year 1 retirement | Static 4% | Initial balance × 4% ÷ 12 |
| Year 5 retirement | Static 4% | Inflation-adjusted |
| Short-term bucket low | Bucket | Trigger refill from medium |

---

## Summary (Revised per Research)

| Sub-Milestone | Points | Focus |
|---------------|--------|-------|
| M6a | 14 | Framework, Sequencing, Context |
| M6b | 8 | Static & Income-Gap (simpler) |
| M6c | 15 | Bucket Strategy + Refill Logic |
| M6d | 15 | Guardrails (3 presets) |
| M6e | 8 | RMD Integration |
| **Total** | **60** | |

**Research Issues (completed):**
- #220: Guardrails Research (3 pts)
- #221: Bucket Strategy Research (3 pts)
- #222: Tax Sequencing Research (3 pts)

---

## Dependencies

### Incoming Dependencies (M6 uses)
- **From M5:** GapAnalyzer, Budget, FederalTaxCalculator, RmdCalculator

### Outgoing Dependencies (M7 uses M6)
- **To M7:** SpendingOrchestrator, SpendingStrategy implementations, AccountSequencer implementations

### Cross-Milestone Interface: SimulationView

The `SimulationView` interface is the **contract between M6 and M7**:

| Milestone | Responsibility |
|-----------|----------------|
| **M6** | Defines the interface; strategies consume it |
| **M6** | Provides `StubSimulationView` for testing strategies before M7 exists |
| **M7** | Implements `SimulationView` backed by actual simulation state + TimeSeries |

This allows M6 to be fully developed and tested independently, with M7 providing the real implementation later.

---

## Implementation Order

### Revised Order (per Architecture Update)

1. **Architecture Refactoring** (blocking)
   - Create `SimulationView` interface
   - Create `AccountSnapshot` record
   - Create `StubSimulationView` for testing
   - Refactor `SpendingContext` to use `SimulationView`
   - Update existing M6a tests

2. **M6b: Static & Income-Gap Strategies**
   - Validate refactored framework with simple strategies
   - Static strategy uses `simulation.getInitialPortfolioBalance()`
   - Income-Gap strategy uses current period inputs only

3. **M6c/M6d parallel** - Independent strategies
   - Bucket Strategy (M6c)
   - Guardrails (M6d) - validates `SimulationView` historical queries

4. **M6e: Integration** - End-to-end flows with RMD coordination

### Original Order (for reference)
1. ~~M6a first~~ - Framework ✅ (needs refactoring)
2. M6b second - Simple strategies validate framework
3. M6c/M6d parallel - Independent strategies
4. M6e last - Integration requires all strategies

---

## Open Questions

These questions will be resolved during implementation:

1. **Portfolio vs AccountSnapshots**: Should `SimulationView` expose `Portfolio` or only `List<AccountSnapshot>`?
   - `AccountSnapshot` is cleaner (read-only, no mutation risk)
   - But some operations may need richer account info (asset allocation, RMD eligibility)
   - **Decision**: Use `List<AccountSnapshot>` exclusively. If additional account info is needed (e.g., RMD eligibility), extend `AccountSnapshot` to include it. This maintains clean read-only access and prevents mutation risk. Portfolio is no longer passed to orchestrator or sequencer.

2. **RMD Excess Handling**: When RMDs exceed spending needs, where does excess go?
   - Reinvest in taxable brokerage?
   - Cash reserve bucket?
   - Just record as "excess withdrawal" and let user decide?
   - **Deferred to M6e**

3. **Inflation in Context**: Should `SpendingContext` include inflation rate, or should strategies get it from configuration?
   - Currently in `strategyParams` map
   - May warrant a dedicated field for clarity
   - **Decide during M6b implementation**

4. **StubSimulationView Scope**: How sophisticated should the stub be?
   - Minimal: just returns configured values
   - Medium: tracks withdrawals and computes derived values
   - Full: maintains actual TimeSeries for testing
   - **Start minimal, expand as needed**
