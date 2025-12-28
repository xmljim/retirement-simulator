# Milestone 5 Retrospective: Expense & Budget Modeling

**Sprint Duration:** December 2025
**Status:** Complete
**Date:** December 28, 2025

---

## Summary

Milestone 5 delivered comprehensive expense and budget modeling capabilities for retirement simulation. What started as a 7-issue milestone (#34-40) expanded through research (#170) to include Medicare/IRMAA calculations, LTC insurance modeling, contingency reserves, RMD calculations, survivor expense adjustments, and federal tax brackets. The milestone completed with 15 issues closed, representing the most feature-rich milestone to date.

---

## Deliverables

| Issue | Title | PR |
|-------|-------|-----|
| #34 | Create expense category model with differentiated inflation rates | #180 |
| #35 | Implement recurring expense model | #181 |
| #36 | Implement one-time expense support | #182 |
| #37 | Implement expense lifecycle changes (mortgage payoff, spending phases) | #189 |
| #38 | Create budget configuration model | #191 |
| #39 | Implement budget vs income gap analysis | #192 |
| #40 | Comprehensive test coverage for Milestone 5 | #193 |
| #170 | Research: Expense category structure and retirement spending patterns | N/A |
| #171 | Implement Medicare expense calculator with IRMAA | #183 |
| #172 | Implement LTC Insurance model | #184 |
| #173 | Implement contingency and reserve expense model | #185 |
| #174 | Implement expense allocation strategy | #188 |
| #175 | Implement RMD calculator | #186 |
| #176 | Implement survivor scenario expense adjustments | #187 |
| #177 | Implement federal tax calculator with brackets and chained CPI indexing | #178 |

---

## Key Accomplishments

### Expense Categories (#34)
- `ExpenseCategory` enum with 19 categories across 6 groups
- `ExpenseCategoryGroup` enum: ESSENTIAL, HEALTHCARE, DISCRETIONARY, CONTINGENCY, DEBT, OTHER
- `InflationType` per category: GENERAL, HEALTHCARE, HOUSING, LTC, NONE
- `InflationRates` Spring configuration with category-specific rates

### Recurring Expenses (#35)
- `RecurringExpense` value object with builder pattern
- `ExpenseFrequency` enum: MONTHLY, QUARTERLY, SEMI_ANNUAL, ANNUAL
- `getMonthlyAmount(date, inflationRate)` with inflation compounding
- Date range filtering with `isActive(date)`

### One-Time Expenses (#36)
- `OneTimeExpense` value object for single-occurrence expenses
- Target date tracking with `getAmountForMonth(date)`
- Inflation adjustment from base date to target date

### Expense Lifecycle (#37)
- `ExpenseModifier` functional interface with chaining support
- `PayoffModifier` - drops expense to zero after payoff date
- `SpendingCurveModifier` - Go-Go/Slow-Go/No-Go spending phases with interpolation
- `AgeBasedModifier` - age-bracket-based expense scaling (healthcare)

### Budget Model (#38)
- `Budget` class aggregating recurring and one-time expenses
- `ExpenseBreakdown` record with category group totals
- Couple budget support with primary and secondary owners
- Integration with `InflationRates` configuration
- `getMonthlyBreakdown(date)`, `getAnnualExpenses(year)`

### Gap Analysis (#39)
- `GapAnalysis` record with surplus/deficit tracking
- `GapAnalyzer` interface with `DefaultGapAnalyzer`
- Tax-aware gross-up: `grossWithdrawalNeeded(marginalTaxRate)`
- Year projection with 12 monthly analyses
- Coverage ratio calculation

### Medicare Calculator (#171)
- `MedicareCalculator` with Part B and Part D premium calculations
- IRMAA brackets (6 tiers) for income-based surcharges
- Single vs MFJ threshold support
- Future year extrapolation with chained CPI

### LTC Insurance (#172)
- `LtcInsurance` value object with benefit pool and daily benefit
- `LtcInsurancePolicy` with premium periods and elimination days
- Benefit exhaustion tracking
- Deterministic vs probabilistic trigger modes

### Contingency Reserves (#173)
- `ContingencyReserve` for home repairs, vehicle replacement, emergencies
- `ScheduledExpense` for predictable replacement cycles
- `RandomExpenseEvent` for Monte Carlo simulation
- Target balance tracking with "fully funded" detection

### Expense Allocation Strategy (#174)
- `ExpenseAllocationStrategy` interface with priority-based allocation
- `AllocationResult` with shortfall tracking by category
- Overflow/redirect when reserves filled
- Integration with Budget and GapAnalyzer

### RMD Calculator (#175)
- `RmdCalculator` with Uniform Lifetime Table
- SECURE 2.0 age thresholds (73 in 2023, 75 in 2033)
- Prior year balance and distribution date tracking

### Survivor Expenses (#176)
- `SurvivorExpenseCalculator` for post-death expense adjustments
- Category-specific adjustment factors
- Housing (70%), Food (60%), Healthcare (100%), etc.

### Federal Tax Calculator (#177)
- `FederalTaxCalculator` with 7 bracket system
- Chained CPI indexing for bracket inflation
- Filing status support (Single, MFJ, MFS, HOH)

---

## Metrics

| Metric | Target | Actual |
|--------|--------|--------|
| Issues Closed | 7 (original) | 15 |
| PRs Merged | ~15 | 15 |
| Story Points | ~30 | ~55 |
| Test Count | 850+ | 900+ |
| Code Coverage | 80% | 80%+ |
| Quality Gates | Pass | All Pass |

---

## What Went Well

### Technical Wins

1. **Research-Driven Expansion** - Issue #170 research into retirement spending patterns led to discovering critical features needed: Medicare/IRMAA, LTC insurance, RMDs, federal tax brackets. This research-first approach ensured we built what retirees actually need.

2. **PR Review Process** - Code review feedback caught anti-patterns early:
   - Removed `forHealthcare()` static factory in favor of configuration-driven approach
   - Added interpolation to `SpendingCurveModifier` for gradual spending transitions
   - Enforced Stream API usage over for-loops
   - Consistent use of `MissingRequiredFieldException.requireNonNull`

3. **Test Coverage Maintained** - Despite significant scope expansion, maintained 80%+ branch coverage throughout. Parameterized tests for IRMAA brackets and spending phases provided comprehensive coverage.

4. **Builder Pattern Consistency** - All value objects use consistent builder pattern with validation in `build()` method.

### Process Wins

1. **Git Hygiene** - Consistent workflow: create branch → implement → PR → review → merge → pull develop → delete branch → close issue

2. **Target Develop** - All PRs correctly targeted develop branch (caught and fixed one early mistake)

3. **Incremental PRs** - 15 focused PRs rather than large batches made review manageable

---

## What Didn't Go Well

### Documentation Timing

**Problem:** Documentation updates were deferred to end of milestone rather than done incrementally with each feature.

**Impact:** Had to reconstruct all feature details at end rather than documenting while fresh.

**Lesson:** Update documentation as part of each PR, not as a separate end-of-milestone task.

### Scope Growth

**Problem:** M5 grew from 7 issues to 15 issues (over 2x) through research findings.

**Impact:** While the additional features are valuable, the milestone took longer than planned.

**Lesson:** Research issues should inform milestone scoping BEFORE starting implementation, not during.

---

## Action Items

### Process Improvements

| Improvement | Description |
|-------------|-------------|
| Doc-as-you-go | Update PROJECT_GOALS.md with each PR, not at milestone end |
| Research First | Complete research issues before finalizing milestone scope |
| Constant Cleanup | Create global constants file for magic numbers |

### Technical Debt Identified

| Item | Description | Priority |
|------|-------------|----------|
| YAML Config Consistency | Some rules hardcoded (tax brackets) vs YAML config | Medium |
| Global Constants | Many classes have duplicate constants (SCALE, MONTHS_PER_YEAR) | Medium |
| Integration Tests | More end-to-end budget→gap→withdrawal tests | Low |

---

## Technical Debt

### Resolved This Milestone
- None carried from M4

### Identified This Milestone
- **Global Constants** - `SCALE`, `MONTHS_PER_YEAR`, `ROUNDING_MODE` duplicated across classes
- **YAML Configuration** - Tax brackets and some limits hardcoded vs externalized
- **Integration Coverage** - Need more tests combining Budget + GapAnalyzer + AllocationStrategy

---

## Lessons Learned

1. **Research pays dividends** - The #170 research issue identified Medicare IRMAA, LTC, RMDs, and survivor expenses as critical features that would have been missed otherwise.

2. **Interpolation matters** - Spending phases shouldn't be "cliffs" - gradual transitions better model real behavior. User feedback caught this design flaw.

3. **Configuration over hardcoding** - Moving inflation rates, IRMAA brackets, and tax thresholds to YAML enables easier updates without code changes.

4. **Stream API preference** - Consistent use of Stream API with reduce/map/filter produces cleaner, more maintainable code than for-loops.

5. **Builder validation** - Validating required fields in `build()` method using `MissingRequiredFieldException.requireNonNull` provides consistent error handling.

---

## Looking Ahead to Milestone 6

### M6: Distribution Strategies

M6 will focus on:
- Static withdrawal strategies (4% rule, fixed dollar)
- Dynamic strategies (guardrails, spending curve)
- Bucket strategy with segmented portfolios
- Tax-efficient withdrawal sequencing
- RMD integration with withdrawal planning

### Dependencies from M5
- `Budget` provides expense totals for withdrawal calculations
- `GapAnalyzer` determines withdrawal needs
- `RmdCalculator` provides minimum required distributions
- `FederalTaxCalculator` enables tax-efficient sequencing
- `ExpenseAllocationStrategy` guides spending priorities

### Process Goals
- Complete research before implementation starts
- Document features as they're built
- Create global constants utility class
- Externalize remaining hardcoded values to YAML

---

## Files Changed Summary

### New Files (M5)

**Value Objects:**
- `RecurringExpense.java` - Recurring expense model
- `OneTimeExpense.java` - One-time expense model
- `Budget.java` - Budget aggregation
- `ExpenseBreakdown.java` - Category group breakdown
- `GapAnalysis.java` - Income vs expense gap
- `LtcInsurance.java` - LTC insurance model
- `LtcInsurancePolicy.java` - LTC policy details
- `ContingencyReserve.java` - Reserve tracking
- `ScheduledExpense.java` - Scheduled replacements
- `RandomExpenseEvent.java` - Monte Carlo expenses
- `AllocationResult.java` - Allocation outcome

**Enums:**
- `ExpenseCategory.java` - 19 expense categories
- `ExpenseCategoryGroup.java` - 6 category groups
- `ExpenseFrequency.java` - Expense frequencies
- `ContingencyType.java` - Contingency types
- `SpendingPhase.java` - Go-Go/Slow-Go/No-Go

**Calculators:**
- `GapAnalyzer.java` - Gap analysis interface
- `DefaultGapAnalyzer.java` - Gap analysis implementation
- `MedicareCalculator.java` - Medicare/IRMAA interface
- `DefaultMedicareCalculator.java` - Medicare implementation
- `RmdCalculator.java` - RMD interface
- `DefaultRmdCalculator.java` - RMD implementation
- `FederalTaxCalculator.java` - Tax calculation interface
- `DefaultFederalTaxCalculator.java` - Tax implementation
- `SurvivorExpenseCalculator.java` - Survivor adjustments
- `ExpenseAllocationStrategy.java` - Allocation interface
- `PriorityBasedAllocationStrategy.java` - Allocation implementation

**Modifiers:**
- `ExpenseModifier.java` - Modifier interface
- `PayoffModifier.java` - Payoff handling
- `SpendingCurveModifier.java` - Spending phases
- `AgeBasedModifier.java` - Age-based scaling

**Configuration:**
- `InflationRates.java` - Inflation config
- `MedicareRules.java` - Medicare config
- `application-expenses.yml` - Expense config

---

## Participants

- **Developer:** @xmljim
- **AI Assistant:** Claude (Anthropic)

---

*This retrospective was conducted on December 28, 2025, following the completion of Milestone 5.*
