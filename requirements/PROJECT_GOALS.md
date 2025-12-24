That# Retirement Portfolio Simulator - Project Goals

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

## Functional Goals

### 1. Portfolio Modeling

- [ ] Model monthly transactions from a start date through a configurable end date (e.g., age 90, 95, or 100)
- [ ] Track portfolio balance over time with contributions (pre-retirement) and withdrawals (post-retirement)
- [ ] Apply investment returns monthly (different rates pre/post retirement)
- [ ] Support multiple contribution sources (personal, employer)
- [ ] Support incremental contribution rate increases (e.g., annual 1% bump)

### 2. Income Modeling

- [ ] Model working income with cost-of-living adjustments (COLA)
- [ ] Model Social Security benefits with configurable start date and inflation adjustments
- [ ] Model other retirement income (pensions, annuities) with optional adjustments
- [ ] Calculate target retirement income as a percentage of pre-retirement salary

### 3. Withdrawal Strategy

- [ ] Support fixed withdrawal rate strategy
- [ ] Support salary-replacement withdrawal strategy
- [ ] Calculate monthly withdrawal needs based on target income minus other income sources
- [ ] Handle portfolio depletion scenarios gracefully

### 4. Simulation & Analysis

- [ ] Generate a sequence of monthly transactions representing the portfolio lifecycle
- [ ] Calculate key metrics:
  - Portfolio balance at retirement
  - Portfolio longevity (when/if depleted)
  - Total contributions made
  - Total withdrawals taken
  - Total investment gains
- [ ] Support "what-if" scenario comparisons

### 5. Output & Reporting

- [ ] Generate transaction-level detail (monthly breakdown)
- [ ] Generate summary statistics
- [ ] Export capabilities (CSV, JSON, or other formats)

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

### Milestone 1: Complete Transaction Model
**Goal**: A fully functional Transaction class that calculates accurate balances

- Implement `getEndBalance()` with investment returns
- Calculate actual contribution amounts (rate × salary)
- Calculate actual withdrawal amounts
- Support transaction chaining (previous balance → current start balance)
- Full test coverage

### Milestone 2: Portfolio Simulation Engine
**Goal**: Generate a complete sequence of monthly transactions

- Create simulation runner that iterates from start date to end date
- Generate Transaction objects for each month
- Track cumulative statistics
- Handle edge cases (retirement transition, SS start, portfolio depletion)

### Milestone 3: Scenario Analysis
**Goal**: Compare different retirement scenarios

- Support multiple portfolio configurations
- Side-by-side comparison metrics
- Sensitivity analysis (what if returns are lower, inflation higher, etc.)

### Milestone 4: Output & Reporting
**Goal**: Export simulation results in useful formats

- CSV export of transaction history
- Summary report generation
- Visualization data preparation

---

## Open Questions

1. **Simulation granularity**: Monthly is assumed - is this sufficient, or do we need flexibility?
2. **Tax modeling**: Should we account for tax implications (traditional vs Roth, capital gains)?
3. **Multiple accounts**: Should we support modeling multiple accounts with different characteristics?
4. **Rebalancing**: Should we model portfolio rebalancing or assume a single blended return rate?
5. **Spouse/joint modeling**: Single person only, or support for couples?

---

## Notes

_This document will evolve as we refine requirements. Each milestone may spawn detailed epic/story breakdowns._