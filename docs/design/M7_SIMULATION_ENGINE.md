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
4. **Point-in-Time Projection** - Simulation projects forward from current state; users update inputs when life changes occur (job change, inheritance, etc.) rather than modeling every life event

## Simulation Model

The simulator is a **projection tool**, not a life modeler:

- Users input current portfolio balances, salary, expenses, etc.
- Simulation projects forward using configured assumptions
- When life changes (new job, salary change, windfall), user updates profile and re-runs
- This keeps the engine simple while giving users accurate projections

**In-scope events:** Death (actuarial), SS start, RMD start, expense lifecycle (mortgage payoff, spending phases), contingency expenses

**Out-of-scope events:** Job changes, salary negotiations, career pivots - user updates profile instead

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

**Structure:**
```java
public record TaxSummary(
    // Income components
    BigDecimal taxableIncome,           // Total taxable income this month
    BigDecimal taxableSSIncome,         // Portion of SS that's taxable (up to 85%)
    BigDecimal taxableWithdrawals,      // Pre-tax withdrawals (Traditional)
    BigDecimal taxFreeWithdrawals,      // Roth withdrawals (not taxable)

    // Tax calculations
    BigDecimal federalTaxLiability,     // From FederalTaxCalculator
    BigDecimal effectiveTaxRate,        // federalTax / taxableIncome
    BigDecimal marginalTaxBracket,      // Current bracket (for Roth conversion decisions)

    // Future: Roth conversion tracking
    BigDecimal rothConversionAmount,    // If any conversion this month
    BigDecimal rothConversionTax        // Tax on conversion
) {}

public record AccountMonthlyFlow(
    UUID accountId,
    String accountName,
    BigDecimal startingBalance,
    BigDecimal contributions,
    BigDecimal withdrawals,
    BigDecimal returns,
    BigDecimal endingBalance
) {
    // Computed
    public BigDecimal netFlow() {
        return contributions.subtract(withdrawals).add(returns);
    }
}

public record MonthlySnapshot(
    YearMonth month,

    // Per-account flows (the source of truth)
    Map<UUID, AccountMonthlyFlow> accountFlows,

    // Income this month
    BigDecimal salaryIncome,
    BigDecimal socialSecurityIncome,
    BigDecimal pensionIncome,
    BigDecimal otherIncome,

    // Expenses this month
    BigDecimal totalExpenses,
    Map<ExpenseCategoryGroup, BigDecimal> expensesByCategory,

    // Taxes this month
    TaxSummary taxes,

    // Cumulative metrics (running totals)
    BigDecimal cumulativeContributions,
    BigDecimal cumulativeWithdrawals,
    BigDecimal cumulativeReturns,
    BigDecimal cumulativeTaxesPaid,

    // Metadata
    SimulationPhase phase,
    List<String> eventsTriggered
) {
    // Aggregated from accountFlows
    public BigDecimal totalPortfolioBalance() {
        return accountFlows.values().stream()
            .map(AccountMonthlyFlow::endingBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalContributions() {
        return accountFlows.values().stream()
            .map(AccountMonthlyFlow::contributions)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalWithdrawals() {
        return accountFlows.values().stream()
            .map(AccountMonthlyFlow::withdrawals)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalReturns() {
        return accountFlows.values().stream()
            .map(AccountMonthlyFlow::returns)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalIncome() {
        return salaryIncome.add(socialSecurityIncome)
            .add(pensionIncome).add(otherIncome);
    }

    public BigDecimal netCashFlow() {
        return totalIncome().subtract(totalExpenses());
    }
}
```

**Decision:** Monthly granularity is correct. Annual snapshots hide important nuance:
- Random events (contingency expenses) in one month ripple to future months
- Market volatility is masked by annual averaging
- Sequence-of-returns risk only visible at monthly level

**Decision:** Net flows per account per month (no individual transactions).

- Monthly is the finest granularity needed
- `AccountMonthlyFlow` per account is the source of truth
- Totals computed via aggregation methods
- Reduces memory over multi-decade simulations

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
    1. Determine phase (accumulation vs distribution)
       - Informs all subsequent steps
       - Per-person: check retirement date, death status

    2. Process income (salary, SS, pension, annuity)
       - Based on current phase and person status
       - SS/pension only if started and person alive

    3. Process events (triggers)
       - Check deterministic events (is trigger date == this month?)
       - Check probabilistic events (roll dice)
       - Execute triggered events → set flags, modify state
       - Affects THIS month's expenses, NEXT month's income

    4. Calculate expenses (budget + inflation)
       - Sees event flags (survivorMode, contingency expenses)
       - Applies modifiers (spending phase, survivor adjustments)

    5. Execute contributions OR withdrawals
       - Accumulation: route contributions to accounts
       - Distribution: execute spending strategy via orchestrator

    6. Apply monthly returns to post-transaction balances
       - balance × (1 + annualRate)^(1/12)
       - Withdrawn money doesn't earn this month's return

    7. Record MonthlySnapshot
       - Capture all account flows, income, expenses, events
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

---

### 3a. Survivor Transition

**Discovered Issue:** #293

When one spouse passes, the simulation must handle three concerns:
1. **Identity** - Track which profile is deceased vs. survivor
2. **Portfolio consolidation** - Merge deceased's accounts into survivor's portfolio
3. **Survivor benefits** - Compute adjusted SS and pension amounts

**SurvivorTransition Class:**

```java
/**
 * Encapsulates the state transition when one spouse passes.
 * Created by SimulationEngine when death event fires.
 */
public class SurvivorTransition {
    private final PersonProfile deceasedProfile;
    private final PersonProfile survivorProfile;
    private final LocalDate transitionDate;
    private final SpousalBenefitCalculator ssCalculator;

    // ─── 1. Identity ───────────────────────────────────────────────
    public PersonProfile getDeceasedProfile() { return deceasedProfile; }
    public PersonProfile getSurvivorProfile() { return survivorProfile; }
    public LocalDate getTransitionDate() { return transitionDate; }

    // ─── 2. Portfolio Consolidation ────────────────────────────────
    /**
     * Merges deceased's accounts into survivor's portfolio.
     * Spouse-to-spouse inheritance has no special rollover rules.
     */
    public Portfolio consolidatePortfolios(Portfolio deceased, Portfolio survivor) {
        List<InvestmentAccount> mergedAccounts = new ArrayList<>(survivor.accounts());

        // Transfer ownership of deceased's accounts to survivor
        deceased.accounts().forEach(account -> {
            InvestmentAccount transferred = account.withOwner(survivorProfile);
            mergedAccounts.add(transferred);
        });

        return Portfolio.builder()
            .owner(survivorProfile)
            .accounts(mergedAccounts)
            .build();
    }

    // ─── 3. Survivor Social Security ───────────────────────────────
    /**
     * Calculates survivor SS benefit: higher of own or deceased's benefit.
     * Uses SpousalBenefitCalculator.calculateSurvivorBenefit().
     */
    public SocialSecurityBenefit calculateSurvivorSS(
            SocialSecurityBenefit deceasedSS,
            SocialSecurityBenefit survivorSS) {

        BigDecimal survivorBenefit = ssCalculator.calculateSurvivorBenefit(
            deceasedSS, survivorSS, transitionDate);

        // Return new SS benefit with survivor amount
        return SocialSecurityBenefit.builder()
            .fraBenefit(survivorBenefit)
            .birthYear(survivorProfile.getBirthYear())
            .claimingAgeMonths(survivorSS.getClaimingAgeMonths())
            .startDate(survivorSS.getStartDate())
            .build();
    }

    // ─── 4. Survivor Pension ───────────────────────────────────────
    /**
     * Calculates survivor pension based on PensionPaymentForm.
     * - SINGLE_LIFE → $0 (benefit ends with deceased)
     * - JOINT_50 → 50% of original
     * - JOINT_75 → 75% of original
     * - JOINT_100 → 100% of original
     */
    public BigDecimal calculateSurvivorPension(Pension pension) {
        return switch (pension.getPaymentForm()) {
            case SINGLE_LIFE -> BigDecimal.ZERO;
            case JOINT_50 -> pension.getMonthlyBenefit().multiply(new BigDecimal("0.50"));
            case JOINT_75 -> pension.getMonthlyBenefit().multiply(new BigDecimal("0.75"));
            case JOINT_100 -> pension.getMonthlyBenefit();
        };
    }

    /**
     * Builds a new IncomeProfile for the survivor with adjusted benefits.
     */
    public IncomeProfile buildSurvivorIncomeProfile(
            IncomeProfile deceasedIncome,
            IncomeProfile survivorIncome) {

        // Calculate survivor SS (higher of own or deceased's)
        SocialSecurityBenefit survivorSS = calculateSurvivorSS(
            deceasedIncome.getSocialSecurity().orElse(null),
            survivorIncome.getSocialSecurity().orElse(null));

        // Adjust pensions based on payment form
        List<Pension> adjustedPensions = new ArrayList<>();

        // Survivor's own pensions continue at full value
        adjustedPensions.addAll(survivorIncome.pensions());

        // Deceased's pensions adjusted by payment form
        deceasedIncome.pensions().stream()
            .filter(p -> p.getPaymentForm() != PensionPaymentForm.SINGLE_LIFE)
            .map(p -> p.withMonthlyBenefit(calculateSurvivorPension(p)))
            .forEach(adjustedPensions::add);

        return IncomeProfile.builder()
            .person(survivorProfile)
            .socialSecurity(survivorSS)
            .pensions(adjustedPensions)
            .annuities(survivorIncome.annuities()) // Survivor's annuities
            .otherIncomes(survivorIncome.otherIncomes())
            .build();
    }
}
```

**SimulationEngine Integration:**

```
┌─────────────────────────────────────────────────────────────────┐
│  SimulationEngine detects death (life expectancy reached)       │
│                           ↓                                     │
│  SpouseDeathEvent fires → creates SurvivorTransition            │
│                           ↓                                     │
│  1. consolidatePortfolios() → survivor has all accounts         │
│  2. buildSurvivorIncomeProfile() → adjusted SS/pensions         │
│                           ↓                                     │
│  Update SimulationState:                                        │
│    - portfolios = [consolidatedPortfolio]                       │
│    - incomeProfiles = [survivorIncomeProfile]                   │
│    - survivorMode = true                                        │
│                           ↓                                     │
│  Continue simulation in SURVIVOR phase with single profile      │
└─────────────────────────────────────────────────────────────────┘
```

**Key Design Decisions:**

| Concern | Decision | Rationale |
|---------|----------|-----------|
| Portfolio transfer | Direct ownership transfer | Spouse-to-spouse has no special IRA/401k rollover rules |
| SS survivor benefit | Use `SpousalBenefitCalculator` | Already implements higher-of logic per SSA rules |
| Pension survivor | Apply `PensionPaymentForm` multiplier | Standard DB plan survivor options |
| Annuities | Only survivor's continue | Simplified model; can enhance later for joint annuities |

---

### 3b. Simulation Termination

The simulation must know when to stop. `SurvivorTransition` helps track deaths, enabling proper termination logic.

**Termination Conditions:**

```
┌─────────────────────────────────────────────────────────────────┐
│  Simulation Termination Conditions                              │
├─────────────────────────────────────────────────────────────────┤
│  Single Person:                                                 │
│    → End when person reaches life expectancy                    │
│                                                                 │
│  Couple:                                                        │
│    → First death: Create SurvivorTransition, continue           │
│    → Second death: END SIMULATION                               │
│                                                                 │
│  Alternative end conditions:                                    │
│    → Portfolio depleted (balance ≤ 0)                           │
│    → Max simulation years reached (configurable cap)            │
└─────────────────────────────────────────────────────────────────┘
```

**Implementation:**

```java
public class SimulationEngine {

    public TimeSeries<MonthlySnapshot> run(SimulationConfig config) {
        TimeSeries<MonthlySnapshot> timeSeries = new TimeSeries<>();

        for (YearMonth month = config.startMonth();
             month.isBefore(config.endMonth());
             month = month.plusMonths(1)) {

            // ... process month (steps 1-7) ...

            // Check termination after recording snapshot
            TerminationReason reason = checkTermination(month);
            if (reason != null) {
                timeSeries.setTerminationReason(reason);
                break;
            }
        }
        return timeSeries;
    }

    private TerminationReason checkTermination(YearMonth month) {
        // All persons deceased
        boolean allDeceased = state.getAllPersons().stream()
            .allMatch(p -> p.isDeceasedAt(month));
        if (allDeceased) {
            return TerminationReason.ALL_PERSONS_DECEASED;
        }

        // Portfolio depleted
        if (state.getTotalBalance().compareTo(BigDecimal.ZERO) <= 0) {
            return TerminationReason.PORTFOLIO_DEPLETED;
        }

        // Max years reached (safety cap)
        if (month.isAfter(config.startMonth().plusYears(config.maxYears()))) {
            return TerminationReason.MAX_YEARS_REACHED;
        }

        return null; // Continue simulation
    }
}

public enum TerminationReason {
    ALL_PERSONS_DECEASED("Simulation ended: all persons have passed"),
    PORTFOLIO_DEPLETED("Simulation ended: portfolio balance exhausted"),
    MAX_YEARS_REACHED("Simulation ended: maximum simulation years reached"),
    COMPLETED("Simulation completed through configured end date");

    private final String description;

    TerminationReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
```

**TimeSeries Enhancement:**

```java
public class TimeSeries<T extends MonthlySnapshot> {
    private final List<T> snapshots;
    private TerminationReason terminationReason;

    public void setTerminationReason(TerminationReason reason) {
        this.terminationReason = reason;
    }

    public TerminationReason getTerminationReason() {
        return terminationReason;
    }

    public boolean wasSuccessful() {
        return terminationReason == TerminationReason.COMPLETED
            || terminationReason == TerminationReason.ALL_PERSONS_DECEASED;
    }

    public boolean ranOutOfMoney() {
        return terminationReason == TerminationReason.PORTFOLIO_DEPLETED;
    }
}
```

**Key Behaviors:**

| Scenario | Behavior |
|----------|----------|
| Single person dies | Simulation ends immediately |
| First spouse dies | `SurvivorTransition` created, simulation continues in SURVIVOR phase |
| Second spouse dies | Simulation ends |
| Portfolio hits $0 | Simulation ends (failure case for Monte Carlo success rate) |
| Max years (e.g., 50) | Safety cap to prevent runaway simulations |

**Monte Carlo Implications:**

The `TerminationReason` is critical for Monte Carlo analysis:
- `PORTFOLIO_DEPLETED` → counts as a **failure** in success rate calculation
- `ALL_PERSONS_DECEASED` with balance > 0 → counts as **success**
- This enables "probability of not running out of money" metrics

---

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
    BigDecimal healthcareTrend,
    ExpenseModifier discretionaryModifier,   // Larger curve for travel/entertainment
    ExpenseModifier essentialsModifier,      // Gentler curve for food
    ExpenseModifier healthcareModifier,      // Age-based INCREASE
    ExpenseModifier survivorModifier         // Applied when survivorMode flag set
) {
    /** Create default levers with standard spending curves. */
    public static ExpenseLevers withDefaults() {
        return new ExpenseLevers(
            InflationRates.defaults().asMap(),
            new BigDecimal("0.05"),
            SpendingCurveModifier.withDefaults(),           // 100% → 80% → 50%
            SpendingCurveModifier.builder()                 // 100% → 90% → 75% (gentler)
                .multiplier(SpendingPhase.SLOW_GO, new BigDecimal("0.90"))
                .multiplier(SpendingPhase.NO_GO, new BigDecimal("0.75"))
                .build(),
            AgeBasedModifier.healthcareDefault(),           // Increases with age
            SurvivorExpenseModifier.withDefaults()
        );
    }
}
```

**Decisions:**
- **Monte Carlo baseline override** → Deferred to research #268
- **Levers are immutable** - set at simulation start, not adjustable mid-run
  - For inflation: choose "fixed rate" OR "historical data" as an immutable option
  - Historical/Monte Carlo modes can use time-varying data, but the *choice* is immutable
  - Aligns with "point-in-time projection" principle

---

### 5a. Expense Model Integration

**Question:** How do expense modifiers (SpendingCurve, Survivor, Age-Based) integrate with the simulation?

**Background:** M5 implemented the expense modifier infrastructure:
- `ExpenseModifier` functional interface with `modify(baseAmount, date, age)`
- `SpendingCurveModifier` - Go-Go/Slow-Go/No-Go discretionary spending
- `SurvivorExpenseModifier` - category-specific adjustments post-death
- `AgeBasedModifier` - healthcare cost increases with age
- `PayoffModifier` - mortgage drops to zero after payoff

The `Budget` class applies inflation but does NOT apply age-based modifiers. This integration happens in M7.

**Decision:** `ExpenseCalculator` wraps Budget and applies modifiers.

```java
/**
 * Calculates monthly expenses with all applicable modifiers.
 * Wraps Budget (M5) and applies expense modifiers from ExpenseLevers.
 */
public interface ExpenseCalculator {
    MonthlyExpenses calculate(Budget budget, YearMonth month, SimulationFlags flags);
}

public record MonthlyExpenses(
    BigDecimal totalExpenses,
    Map<ExpenseCategoryGroup, BigDecimal> byCategory,
    SpendingPhase currentPhase,         // For reporting
    List<String> modifiersApplied       // For audit trail
) {}

public class DefaultExpenseCalculator implements ExpenseCalculator {
    private final ExpenseLevers levers;

    @Override
    public MonthlyExpenses calculate(Budget budget, YearMonth month, SimulationFlags flags) {
        LocalDate date = month.atDay(1);
        int age = budget.getOwner().getAgeAt(date);

        // 1. Get base expenses from Budget (includes inflation)
        ExpenseBreakdown base = budget.getMonthlyBreakdown(date);

        // 2. Build composite modifier based on category and flags
        List<String> appliedModifiers = new ArrayList<>();
        Map<ExpenseCategoryGroup, BigDecimal> adjusted = new EnumMap<>(ExpenseCategoryGroup.class);

        base.getByGroup().forEach((group, amount) -> {
            ExpenseModifier modifier = selectModifier(group, flags, appliedModifiers);
            BigDecimal modifiedAmount = modifier.modify(amount, date, age);
            adjusted.put(group, modifiedAmount);
        });

        BigDecimal total = adjusted.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        SpendingPhase phase = SpendingPhase.forAge(age);

        return new MonthlyExpenses(total, adjusted, phase, appliedModifiers);
    }

    private ExpenseModifier selectModifier(
            ExpenseCategoryGroup group,
            SimulationFlags flags,
            List<String> appliedModifiers) {

        ExpenseModifier modifier = ExpenseModifier.identity();

        // Apply spending curve to discretionary expenses
        if (group == ExpenseCategoryGroup.DISCRETIONARY) {
            modifier = modifier.andThen(levers.discretionaryModifier());
            appliedModifiers.add("SpendingCurve");
        }

        // Apply survivor modifier if flag set
        if (flags.survivorMode()) {
            modifier = modifier.andThen(levers.survivorModifier());
            appliedModifiers.add("Survivor");
        }

        return modifier;
    }
}
```

**Integration Flow:**
```
Budget (M5)                    ExpenseCalculator (M7)              Simulation Loop
┌───────────────────┐          ┌──────────────────────────┐        ┌───────────────┐
│ RecurringExpenses │          │ 1. Get base from Budget  │        │ Step 4        │
│ + Inflation       │ ──────▶  │ 2. Check SimulationFlags │ ─────▶ │ Calculate     │
│ = Base Expenses   │          │ 3. Apply modifiers       │        │ Expenses      │
└───────────────────┘          │ 4. Return MonthlyExpenses│        └───────────────┘
                               └──────────────────────────┘
```

**Category-to-Modifier Mapping:**

The spending curve affects more than just discretionary spending. Research shows:
- **Discretionary** (travel, entertainment) - significant reduction with age
- **Food** - moderate reduction (people eat less as they age)
- **Healthcare** - INCREASES with age (opposite direction!)
- **Housing** - relatively stable
- **Debt** - follows payoff schedules

| Category | Modifier | Multiplier Direction | Applied When |
|----------|----------|---------------------|--------------|
| TRAVEL, ENTERTAINMENT | SpendingCurveModifier | ↓ decreasing | Always during retirement |
| FOOD, DINING | SpendingCurveModifier (gentler) | ↓ decreasing (smaller magnitude) | Always during retirement |
| HEALTHCARE, MEDICAL | AgeBasedModifier | ↑ INCREASING | Always |
| HOUSING, UTILITIES | Identity / Survivor | — stable / ↓ on death | `survivorMode` flag |
| MORTGAGE, DEBT | PayoffModifier | → zero at end | Built into RecurringExpense endDate |

**SpendingCurveModifier Configuration:**
```java
// Standard discretionary curve (larger drops)
SpendingCurveModifier discretionary = SpendingCurveModifier.builder()
    .multiplier(SpendingPhase.GO_GO, new BigDecimal("1.00"))
    .multiplier(SpendingPhase.SLOW_GO, new BigDecimal("0.80"))
    .multiplier(SpendingPhase.NO_GO, new BigDecimal("0.50"))
    .interpolate(true)
    .build();

// Gentler curve for food/essentials (smaller drops)
SpendingCurveModifier essentials = SpendingCurveModifier.builder()
    .multiplier(SpendingPhase.GO_GO, new BigDecimal("1.00"))
    .multiplier(SpendingPhase.SLOW_GO, new BigDecimal("0.90"))
    .multiplier(SpendingPhase.NO_GO, new BigDecimal("0.75"))
    .interpolate(true)
    .build();
```

**Enhanced ExpenseLevers:**
```java
public record ExpenseLevers(
    Map<InflationType, BigDecimal> inflationRates,
    BigDecimal healthcareTrend,
    ExpenseModifier discretionaryModifier,   // Larger curve for travel/entertainment
    ExpenseModifier essentialsModifier,      // Gentler curve for food
    ExpenseModifier healthcareModifier,      // Age-based INCREASE
    ExpenseModifier survivorModifier         // Applied when survivorMode flag set
) {}
```

**SimulationFlags:**
```java
public record SimulationFlags(
    boolean survivorMode,                    // Spouse deceased
    Set<ExpenseCategory> contingencyActive,  // Categories with active contingency
    boolean refillMode,                      // Redirecting to contingency reserve
    Map<String, Object> custom               // Extensibility
) {
    public static SimulationFlags initial() {
        return new SimulationFlags(false, Set.of(), false, Map.of());
    }
}
```

**Why this design?**
1. **Separation of concerns** - Budget owns base expenses + inflation; Calculator owns modifiers
2. **Testable** - Can test modifier logic independently
3. **Configurable** - ExpenseLevers can swap modifiers for different scenarios
4. **Event-driven** - Flags set by events, Calculator checks flags each month

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
    private SimulationState state;
    private final EventRegistry eventRegistry;
    private final IncomeProcessor incomeProcessor;
    private final ExpenseCalculator expenseCalculator;
    private final ContributionRouter contributionRouter;
    private final SpendingOrchestrator spendingOrchestrator;
    private final ReturnCalculator returnCalculator;
    private final TaxCalculator taxCalculator;

    public TimeSeries<MonthlySnapshot> run(SimulationConfig config) {
        TimeSeries<MonthlySnapshot> timeSeries = new TimeSeries<>();

        for (YearMonth month = config.startMonth();
             month.isBefore(config.endMonth());
             month = month.plusMonths(1)) {

            // ─── Step 1: Determine Phase ───────────────────────────────
            SimulationPhase phase = determinePhase(month);

            // ─── Step 2: Process Income ────────────────────────────────
            MonthlyIncome income = incomeProcessor.process(
                state.getPersons(), month, phase);

            // ─── Step 3: Process Events ────────────────────────────────
            List<SimulationEvent> triggered = eventRegistry.check(month, state);
            triggered.forEach(event -> event.execute(state));

            // ─── Step 4: Calculate Expenses ────────────────────────────
            MonthlyExpenses expenses = expenseCalculator.calculate(
                state.getBudget(), month, state.getFlags());

            // ─── Step 5: Execute Contributions OR Withdrawals ──────────
            Map<UUID, AccountMonthlyFlow> accountFlows;
            if (phase == SimulationPhase.ACCUMULATION) {
                accountFlows = executeContributions(income, month);
            } else {
                accountFlows = executeWithdrawals(income, expenses, month);
            }

            // ─── Step 6: Apply Monthly Returns ─────────────────────────
            applyMonthlyReturns(accountFlows, config.getMarketLevers(), month);

            // ─── Step 7: Record MonthlySnapshot ────────────────────────
            TaxSummary taxes = taxCalculator.calculate(income, accountFlows);
            MonthlySnapshot snapshot = buildSnapshot(
                month, accountFlows, income, expenses, taxes, phase, triggered);
            timeSeries.add(snapshot);
            state.recordHistory(snapshot);
        }
        return timeSeries;
    }

    private Map<UUID, AccountMonthlyFlow> executeContributions(
            MonthlyIncome income, YearMonth month) {
        // Route contributions via M3 ContributionRouter
        BigDecimal available = income.salary().subtract(income.expenses());
        return contributionRouter.route(available, state.getAccounts(), month);
    }

    private Map<UUID, AccountMonthlyFlow> executeWithdrawals(
            MonthlyIncome income, MonthlyExpenses expenses, YearMonth month) {
        // Build context with immutable view
        SimulationView view = state.snapshot();
        SpendingContext context = SpendingContext.builder()
            .simulation(view)
            .date(month.atDay(1))
            .totalExpenses(expenses.total())
            .otherIncome(income.totalNonSalary())
            .age(state.getPrimaryPerson().getAgeAt(month))
            .birthYear(state.getPrimaryPerson().getBirthYear())
            .build();

        // Execute via M6 SpendingOrchestrator
        SpendingPlan plan = spendingOrchestrator.execute(
            state.getStrategy(), context);

        // Apply withdrawals to state, return flows
        return state.applyWithdrawals(plan);
    }

    private void applyMonthlyReturns(
            Map<UUID, AccountMonthlyFlow> flows,
            MarketLevers levers,
            YearMonth month) {
        BigDecimal monthlyRate = returnCalculator.getMonthlyReturn(levers, month);

        flows.values().forEach(flow -> {
            var newBalance = flow.endingBalance().multiply(BigDecimal.ONE.add(monthlyRate));
            state.updateAccountBalance(flow.accountId(), newBalance);
            flow.withReturns(newBalance.subtract(flow.endingBalance()));
        });
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
        plan.accountWithdrawals().forEach(withdrawal ->
            accounts.get(withdrawal.accountId()).withdraw(withdrawal.amount()));
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
  - Structured Concurrency for parallelism (Java 25)
  - Seed handling for reproducibility
  - Aggregation model (`MonteCarloResult`)
  - Return distributions
- Returns `MonteCarloResult` containing multiple `TimeSeries` + statistics

```java
// Structured Concurrency for Monte Carlo (Java 25)
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    var simulations = IntStream.range(0, numRuns)
        .mapToObj(i -> scope.fork(() -> runSimulation(config, seeds[i])))
        .toList();

    scope.join();           // Wait for all
    scope.throwIfFailed();  // Propagate errors

    return simulations.stream()
        .map(StructuredTaskScope.Subtask::get)
        .collect(MonteCarloResult.collector());
}
```

Benefits of Structured Concurrency:
- Automatic cleanup if one simulation fails (`ShutdownOnFailure`)
- Clear parent-child task relationship
- No orphaned threads
- Better error propagation

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
