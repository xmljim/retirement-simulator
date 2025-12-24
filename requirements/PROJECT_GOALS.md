# Retirement Portfolio Simulator - Project Goals

## Vision

A comprehensive retirement portfolio simulation tool that models the accumulation and distribution phases of retirement savings, enabling users to project portfolio performance under various scenarios and make informed decisions about savings rates, retirement timing, and withdrawal strategies.

## Core Problem Statement

Retirement planning requires understanding how multiple variables interact over time:
- Contribution rates and employer matching
- Investment returns (varying pre/post retirement)
- Inflation and cost-of-living adjustments
- Social Security timing and amounts
- Other retirement income sources (pensions, annuities)
- Withdrawal strategies and rates

This tool aims to model these interactions on a monthly basis, providing visibility into portfolio balance trajectories from the present through retirement.

---

## Core Concepts

### Portfolio of Investments

A user's retirement portfolio consists of one or more **investment accounts**, each with distinct characteristics:

- **Account types**: 401(k), IRA, Roth IRA, Roth 401(k), taxable brokerage, HSA, etc.
- **Asset allocation**: Each account may have different allocations (stocks, bonds, cash)
- **Expected returns**: Different return rates based on asset allocation
- **Contribution rules**: Different contribution limits, employer matching, catch-up provisions
- **Tax treatment**: Pre-tax, post-tax (Roth), or taxable (affects withdrawal sequencing)

The simulator should allow users to define multiple investments and model how they interact during accumulation and distribution phases.

### Distribution Strategies

The method for withdrawing funds in retirement significantly impacts portfolio longevity. The simulator should support multiple distribution strategies:

#### 1. Static Withdrawal (Fixed Rate)
The traditional approach (e.g., the "4% rule"):
- Withdraw a fixed percentage of the initial portfolio balance in year one
- Adjust subsequent withdrawals for inflation
- Simple and predictable, but doesn't adapt to market conditions
- Risk: Portfolio depletion in poor market conditions, or leaving too much unspent in good conditions

#### 2. Bucket Strategy
Segment the portfolio into time-based "buckets" with different risk profiles:
- **Short-term bucket (1-3 years)**: Cash, money market, short-term bonds - covers near-term expenses
- **Medium-term bucket (4-10 years)**: Balanced funds, intermediate bonds - refills short-term bucket
- **Long-term bucket (10+ years)**: Growth stocks, equity funds - refills medium-term bucket

Benefits: Provides psychological comfort during market downturns; short-term needs are protected.
Requires: Periodic rebalancing/refilling of buckets based on rules or market conditions.

#### 3. Retirement Spending Curve
Model the reality that spending patterns change throughout retirement:
- **Go-Go Years (early retirement)**: Higher discretionary spending - travel, hobbies, activities
- **Slow-Go Years (mid-retirement)**: Moderate spending - reduced activity, more time at home
- **No-Go Years (late retirement)**: Lower discretionary spending, but potentially higher healthcare costs

This approach adjusts withdrawal targets based on retirement phase, potentially using age-based multipliers or defined spending curves.

#### 4. Guardrails Strategy (Dynamic Withdrawals)
Dynamically adjust withdrawals based on portfolio performance:
- Define an initial withdrawal rate and annual adjustment for inflation
- Set **upper guardrail**: If portfolio growth pushes withdrawal rate below threshold (e.g., 3%), increase spending
- Set **lower guardrail**: If portfolio decline pushes withdrawal rate above threshold (e.g., 5%), decrease spending
- Examples: Guyton-Klinger guardrails, Vanguard's dynamic spending

Benefits: Balances enjoying retirement with preserving portfolio; adapts to actual market conditions.
Requires: Defining guardrail thresholds, adjustment percentages, and floor/ceiling constraints.

---

## Functional Goals

### 1. Investment Account Modeling

- [ ] Support multiple investment accounts within a portfolio
- [ ] Define account types (401k, IRA, Roth IRA, Roth 401k, taxable brokerage, HSA)
- [ ] Configure asset allocation per account (stocks/bonds/cash percentages)
- [ ] Set expected return rates based on allocation or manual override
- [ ] Track individual account balances and aggregate portfolio balance
- [ ] Model account-specific contribution limits and rules

### 2. Contribution Modeling (Accumulation Phase)

- [ ] Model monthly transactions from a start date through retirement
- [ ] Support multiple contribution sources (personal, employer match, catch-up)
- [ ] Apply contributions to specific accounts based on rules
- [ ] Support incremental contribution rate increases (e.g., annual 1% bump)
- [ ] Respect annual contribution limits by account type
- [ ] Apply investment returns monthly per account

### 3. Income Modeling

- [ ] Model working income with cost-of-living adjustments (COLA)
- [ ] Model Social Security benefits with configurable start date and inflation adjustments
- [ ] Model other retirement income (pensions, annuities) with optional adjustments
- [ ] Calculate target retirement income as a percentage of pre-retirement salary

### 4. Distribution Strategies (Withdrawal Phase)

#### 4.1 Static Withdrawal
- [ ] Support fixed withdrawal rate (e.g., 4% rule)
- [ ] Adjust annual withdrawal for inflation
- [ ] Configure initial withdrawal rate

#### 4.2 Bucket Strategy
- [ ] Define bucket allocations (short/medium/long-term)
- [ ] Configure time horizons for each bucket
- [ ] Model bucket refill rules (when and how to move funds between buckets)
- [ ] Draw from appropriate bucket based on time horizon

#### 4.3 Retirement Spending Curve
- [ ] Define spending phases (go-go, slow-go, no-go) with age ranges
- [ ] Configure spending multipliers or target amounts per phase
- [ ] Model healthcare cost increases in later phases
- [ ] Smooth transitions between phases

#### 4.4 Guardrails Strategy
- [ ] Set initial withdrawal rate and inflation adjustment
- [ ] Define upper guardrail threshold (increase spending trigger)
- [ ] Define lower guardrail threshold (decrease spending trigger)
- [ ] Configure adjustment percentages when guardrails are hit
- [ ] Set floor and ceiling constraints on withdrawals

#### 4.5 Common Withdrawal Features
- [ ] Calculate monthly withdrawal needs based on target income minus other income sources
- [ ] Model withdrawal sequencing across account types (tax-efficient ordering)
- [ ] Handle portfolio depletion scenarios gracefully
- [ ] Support Required Minimum Distributions (RMDs) when applicable

### 5. Simulation & Analysis

- [ ] Generate a sequence of monthly transactions representing the portfolio lifecycle
- [ ] Calculate key metrics:
  - Portfolio balance at retirement
  - Portfolio longevity (when/if depleted)
  - Total contributions made
  - Total withdrawals taken
  - Total investment gains
  - Success rate across scenarios
- [ ] Support "what-if" scenario comparisons
- [ ] Monte Carlo simulation support (variable returns)

### 6. Output & Reporting

- [ ] Generate transaction-level detail (monthly breakdown)
- [ ] Generate summary statistics
- [ ] Export capabilities (CSV, JSON, or other formats)
- [ ] Per-account breakdown in reports

---

## Non-Functional Goals

- **Accuracy**: Financial calculations must be precise and auditable
- **Testability**: Comprehensive unit tests for all calculations
- **Extensibility**: Easy to add new income sources, withdrawal strategies, or calculation methods
- **Usability**: Clear API for constructing portfolios and running simulations

---

## Current State

### Completed
- `PortfolioParameters`: Full parameter model with builders for all configuration
- `Functions`: Core financial calculations (inflation, COLA, contributions, SS, other income)
- `Transaction`: Partial implementation with retirement status, contribution rates, income calculations
- Test coverage for Functions and partial Transaction behavior

### In Progress / Not Started
- `Transaction.getEndBalance()`: Returns hardcoded 0.0
- Actual dollar amount calculations for contributions
- Investment return application
- Portfolio simulation engine (transaction sequencing)
- Withdrawal calculations
- Reporting/output

---

## Proposed Milestones

### Milestone 1: Core Transaction Model
**Goal**: A fully functional Transaction class that calculates accurate balances for a single account

- Implement `getEndBalance()` with investment returns
- Calculate actual contribution amounts (rate × salary)
- Calculate actual withdrawal amounts (static strategy)
- Support transaction chaining (previous balance → current start balance)
- Full test coverage

### Milestone 2: Multi-Account Portfolio
**Goal**: Support multiple investment accounts with different characteristics

- Create InvestmentAccount model with type, allocation, return rates
- Create Portfolio container for multiple accounts
- Model contribution routing to specific accounts
- Aggregate portfolio balance across accounts
- Account-level transaction tracking

### Milestone 3: Distribution Strategies
**Goal**: Implement the four core withdrawal strategies

- **3a**: Static withdrawal (fixed rate with inflation adjustment)
- **3b**: Bucket strategy (time-segmented withdrawals)
- **3c**: Spending curve (phase-based withdrawal targets)
- **3d**: Guardrails (dynamic withdrawal adjustments)
- Strategy interface/abstraction for extensibility
- Withdrawal sequencing across account types

### Milestone 4: Portfolio Simulation Engine
**Goal**: Generate a complete sequence of monthly transactions

- Create simulation runner that iterates from start date to end date
- Generate Transaction objects for each month per account
- Track cumulative statistics (contributions, withdrawals, gains)
- Handle edge cases (retirement transition, SS start, portfolio depletion, RMDs)

### Milestone 5: Scenario Analysis
**Goal**: Compare different retirement scenarios

- Support multiple portfolio/strategy configurations
- Side-by-side comparison metrics
- Sensitivity analysis (variable returns, inflation, contribution rates)
- Monte Carlo simulation support

### Milestone 6: Output & Reporting
**Goal**: Export simulation results in useful formats

- CSV export of transaction history (per-account and aggregate)
- Summary report generation
- Visualization data preparation
- Success/failure metrics for scenario runs

---

## Open Questions

### Resolved
- ~~**Multiple accounts**: Should we support modeling multiple accounts with different characteristics?~~
  **Decision**: Yes - support multiple investment accounts with distinct types, allocations, and rules.
- ~~**Withdrawal strategies**: What strategies should we support?~~
  **Decision**: Support Static, Bucket, Spending Curve, and Guardrails strategies.

### Open
1. **Simulation granularity**: Monthly is assumed - is this sufficient, or do we need flexibility?
2. **Tax modeling depth**: How detailed should tax modeling be?
   - Simple: Just track pre-tax vs post-tax accounts
   - Moderate: Model tax brackets, estimate tax liability on withdrawals
   - Detailed: Capital gains, tax-loss harvesting, state taxes
3. **Rebalancing**: How should we model portfolio rebalancing?
   - Per-account rebalancing to target allocation?
   - Cross-account rebalancing?
   - Frequency (annual, threshold-based)?
4. **Spouse/joint modeling**: Single person only, or support for couples?
   - Joint vs individual Social Security benefits
   - Survivorship scenarios
5. **Withdrawal sequencing**: What's the default order for multi-account withdrawals?
   - Tax-efficient: Taxable → Traditional → Roth
   - User-configurable ordering?
   - Roth conversion strategies?
6. **Historical vs projected returns**: Should we support backtesting against historical data?
7. **Inflation modeling**: Single rate, or separate rates for different expense categories (healthcare, general)?

---

## Notes

_This document will evolve as we refine requirements. Each milestone may spawn detailed epic/story breakdowns._