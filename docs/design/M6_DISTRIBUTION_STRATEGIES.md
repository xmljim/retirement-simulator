# M6: Distribution Strategies Design

**Status:** Research Complete
**Date:** December 29, 2025
**Milestone:** 6

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
┌─────────────────────────────────────────────────────────────┐
│                    WithdrawalOrchestrator                    │
│  (Coordinates strategy + sequencing + RMD + execution)      │
└─────────────────────────────────────────────────────────────┘
                              │
          ┌───────────────────┼───────────────────┐
          ▼                   ▼                   ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│ WithdrawalStrategy │  │ AccountSequencer │  │  RmdCalculator  │
│   (HOW MUCH)      │  │ (WHICH ACCOUNTS) │  │  (MANDATORY)    │
└─────────────────┘  └─────────────────┘  └─────────────────┘
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

### M6a: Strategy Framework (~13 points)

**Goal:** Establish the strategy pattern foundation.

#### Interfaces

```java
// Core strategy interface
public interface WithdrawalStrategy {
    WithdrawalPlan calculateWithdrawal(WithdrawalContext context);
    String getName();
    String getDescription();
}

// Context passed to strategies (enhanced per research)
public record WithdrawalContext(
    Portfolio portfolio,
    BigDecimal totalExpenses,
    BigDecimal otherIncome,
    LocalDate date,
    int age,
    int birthYear,
    int yearsInRetirement,
    BigDecimal initialPortfolioBalance,

    // State tracking (for Guardrails/Kitces)
    BigDecimal priorYearSpending,
    BigDecimal priorYearPortfolioReturn,
    int yearsSinceLastRatchet,

    // Tax context (for sequencing)
    BigDecimal currentTaxableIncome,
    FilingStatus filingStatus,

    Map<String, Object> strategyParams
) {}

// Result from strategy
public record WithdrawalPlan(
    BigDecimal targetWithdrawal,
    BigDecimal adjustedWithdrawal,  // After RMD/constraints
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
    List<InvestmentAccount> sequence(Portfolio portfolio, WithdrawalContext context);
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
public AccountSequencer selectSequencer(WithdrawalContext ctx, RmdCalculator rmdCalc) {
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
public interface WithdrawalOrchestrator {
    WithdrawalPlan execute(
        Portfolio portfolio,
        WithdrawalStrategy strategy,
        AccountSequencer sequencer,
        WithdrawalContext context
    );
}
```

**Issues:**
1. Create WithdrawalStrategy interface and WithdrawalPlan record (3)
2. Create WithdrawalContext record with state/tax fields (3)
3. Create AccountSequencer interface and AccountWithdrawal record (2)
4. Implement TaxEfficientSequencer (2)
5. Implement RmdFirstSequencer (2)
6. Create WithdrawalOrchestrator interface (2)

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

- **From M5:** GapAnalyzer, Budget, FederalTaxCalculator, RmdCalculator
- **To M7:** WithdrawalOrchestrator feeds into Simulation Engine

---

## Implementation Order

1. **M6a first** - Framework must exist before strategies
2. **M6b second** - Simple strategies validate framework
3. **M6c/M6d parallel** - Independent strategies
4. **M6e last** - Integration requires all strategies
