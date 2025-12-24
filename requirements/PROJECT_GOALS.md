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

### Person Profile

A **Person Profile** is the foundational entity representing an individual in the simulation:

- **Personal information**: Date of birth, planned retirement date, life expectancy
- **Portfolio**: Collection of investment accounts owned by this person
- **Income sources**: Salary, Social Security, pensions, annuities
- **Linked profiles**: Optional link to spouse/partner profile

**Linked Profiles (Couples)**:
- Two Person Profiles can be linked to model couples
- Each person maintains their own portfolio and income sources
- Scenarios can operate on individual portfolios or combined household view
- Enables modeling of:
  - Joint Social Security strategies (spousal benefits, survivor benefits)
  - Coordinated withdrawal strategies across both portfolios
  - Survivorship scenarios (one spouse passes, other continues)
  - Different retirement dates for each spouse

### Portfolio of Investments

A user's retirement portfolio consists of one or more **investment accounts**, each with distinct characteristics:

- **Account types**: 401(k), IRA, Roth IRA, Roth 401(k), taxable brokerage, HSA, etc.
- **Asset allocation**: Each account may have different allocations (stocks, bonds, cash)
- **Expected returns**: Different return rates based on asset allocation
- **Contribution rules**: Different contribution limits, employer matching, catch-up provisions
- **Tax treatment**: Pre-tax, post-tax (Roth), or taxable (affects withdrawal sequencing)

The simulator should allow users to define multiple investments and model how they interact during accumulation and distribution phases.

### Contribution Rules & Limits

Contribution modeling must account for complex IRS rules that vary by:

- **Account type**: 401(k), IRA, Roth IRA, HSA each have different limits
- **Age**: Catch-up contributions available at 50+ (IRA, 401k) and 55+ (HSA)
- **Income**: Phase-outs for IRA deductibility and Roth IRA eligibility
- **Year**: Limits are inflation-adjusted annually by IRS
- **Regulatory changes**: SECURE 2.0 Act introduces new rules effective 2025-2026

**SECURE 2.0 Key Changes (2025-2026)**:
- Ages 60-63 get enhanced "super catch-up" contributions (~50% higher than standard catch-up)
- High earners ($145K+ prior year FICA wages) must make catch-up contributions as Roth
- This requires income lookback to prior year for determining catch-up eligibility

The system should:
- Provide sensible defaults based on a configurable base year
- Auto-project future limits using assumed inflation rate (~2-3%)
- Allow configuration/override of all limits for scenario modeling
- Support effective dates for rule changes (e.g., SECURE 2.0 phased rollout)
- Automatically apply age-based catch-up eligibility
- Track prior year income for SECURE 2.0 Roth catch-up determination

**Out of Scope**: Employer vesting schedules (cliff, graded) require hire date tracking and add significant complexity. Planned as future feature.

### Scenarios

A **Scenario** represents a complete simulation configuration applied to one or more Person Profiles:

**Scenario Components**:
- **Person Profile(s)**: One person or linked couple
- **Time horizon**: Start date, end condition (age or depletion)
- **Simulation mode**: Deterministic, Monte Carlo, or Historical
- **Distribution strategy**: Static, Bucket, Spending Curve, or Guardrails
- **Assumptions**: Return rates, inflation rates (general, healthcare, housing)
- **Expense budget**: Expected spending by category

**Scenario Phases**:
1. **Pre-Retirement (Accumulation)**: From start date until retirement date
   - Apply contributions to accounts
   - Apply employer matches
   - Apply pre-retirement return rates
   - Track income and expenses

2. **Retirement (Distribution)**: From retirement date until end condition
   - Execute chosen distribution strategy
   - Apply post-retirement return rates
   - Model income sources (SS, pensions, annuities)
   - Track expenses with inflation
   - Calculate withdrawal needs

**Scenario Comparison**:
- Users can create multiple scenarios with different parameters
- Compare outcomes side-by-side (e.g., "retire at 62 vs 65", "4% vs 3.5% withdrawal")
- Save/load scenario configurations for reuse

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

#### 2.1 Basic Contribution Tracking
- [ ] Model monthly transactions from a start date through retirement
- [ ] Support multiple contribution sources (personal, employer match, catch-up)
- [ ] Apply contributions to specific accounts based on rules
- [ ] Support incremental contribution rate increases (e.g., annual 1% bump)
- [ ] Apply investment returns monthly per account

#### 2.2 IRS Contribution Limits & Rules
Contribution limits vary by account type, age, and income. The system must model these rules with configurable limits that can be updated as IRS rules change.

**401(k) / 403(b) / 457(b) Plans**:
- [ ] Annual elective deferral limit (employee contributions)
- [ ] Annual total contribution limit (employee + employer)
- [ ] Catch-up contributions for age 50+
- [ ] Enhanced catch-up contributions for ages 60-63 (SECURE 2.0, effective 2025)
- [ ] Employer match rules (percentage match, vesting schedules)

**Traditional & Roth IRA**:
- [ ] Annual contribution limit
- [ ] Catch-up contributions for age 50+
- [ ] Income-based phase-out rules for Traditional IRA deductibility
- [ ] Income-based phase-out rules for Roth IRA contributions
- [ ] Backdoor Roth IRA considerations

**HSA (Health Savings Account)**:
- [ ] Annual contribution limit (individual vs family coverage)
- [ ] Catch-up contributions for age 55+
- [ ] Employer contribution tracking

**2026+ Rule Changes (SECURE 2.0 Act)**:
- [ ] Super catch-up contributions for ages 60-63 (higher limits)
- [ ] Roth catch-up requirement for high earners ($145K+ in prior year)
- [ ] Automatic enrollment requirements impact modeling
- [ ] Configurable rule effective dates for modeling transitions

#### 2.3 Contribution Limit Configuration
- [ ] Provide default IRS limits for a configurable base year
- [ ] Auto-project future limits using inflation rate (~2-3% annually)
- [ ] Allow override of all limits for scenario modeling
- [ ] Track year-to-date contributions against limits
- [ ] Alert/handle when contributions would exceed limits

#### 2.4 SECURE 2.0 Compliance
- [ ] Track prior year income for catch-up contribution rules
- [ ] Determine Roth catch-up requirement based on $145K+ FICA wage threshold
- [ ] Model income lookback for each simulation year
- [ ] Apply enhanced catch-up limits for ages 60-63 (effective 2025)

#### 2.5 Out of Scope (Future Consideration)
- **Employer vesting schedules**: Modeling vested vs unvested employer contributions requires hire date tracking per employer and adds significant complexity. Consider as separate future feature.

### 3. Income Modeling

#### 3.1 Working Income (Pre-Retirement)
- [ ] Model base salary with configurable annual COLA rate
- [ ] Salary COLA independent of general inflation rate
- [ ] Support start/end dates for income (e.g., working until retirement date)

**Out of Scope**: Job changes, promotions, variable compensation - too complex to model reliably.

#### 3.2 Social Security Benefits
User provides their Full Retirement Age (FRA) benefit amount from SSA.gov. The system models timing adjustments:

- [ ] Configurable claiming age (62-70)
- [ ] Model early claiming reduction (before FRA):
  - ~6.67% per year for first 3 years before FRA
  - ~5% per year for years 4-5 before FRA
- [ ] Model delayed claiming credits (after FRA):
  - 8% per year increase up to age 70
- [ ] Annual COLA adjustments (configurable rate, typically ~2-3%)
- [ ] Spousal benefits: up to 50% of higher earner's FRA benefit
- [ ] Survivor benefits: surviving spouse receives higher of two benefits
- [ ] Earnings test: benefit reduction if working before FRA ($1 reduction per $2 earned over limit)
- [ ] Taxation of benefits: model 0%/50%/85% taxable thresholds based on combined income

**Out of Scope (Future)**: Estimating FRA benefit from earnings history.

#### 3.3 Pension / Defined Benefit Plans
- [ ] Configure monthly benefit amount
- [ ] Start date (may differ from retirement date)
- [ ] Optional COLA adjustment (many pensions have none, some have fixed %)
- [ ] Survivor benefit options:
  - Single life (highest benefit, no survivor)
  - 100% joint & survivor
  - 75% joint & survivor
  - 50% joint & survivor
- [ ] Support multiple pensions (e.g., spouse has separate pension)

#### 3.4 Annuities
- [ ] **Fixed immediate annuity**: Known monthly payment starting at purchase
- [ ] **Fixed deferred annuity**: Known payment starting at future date
- [ ] **Variable annuity**: Payment varies based on underlying investments (model with expected return)
- [ ] Optional COLA/inflation riders
- [ ] Survivor/death benefit options

#### 3.5 Other Income Sources
- [ ] Rental income (monthly amount, optional inflation adjustment)
- [ ] Part-time work in retirement (amount, start/end dates)
- [ ] Other recurring income (royalties, dividends outside portfolio, etc.)

### 4. Expense & Budget Modeling

Users need to model expected expenses to determine if income sources cover retirement needs.

#### 4.1 Expense Categories
Expenses should be categorized to apply appropriate inflation rates:

- [ ] **Essential expenses**: Housing, food, utilities, insurance, transportation
- [ ] **Healthcare expenses**: Premiums, out-of-pocket, long-term care (healthcare inflation rate)
- [ ] **Housing expenses**: Property tax, maintenance, HOA (housing inflation rate)
- [ ] **Discretionary expenses**: Travel, entertainment, hobbies (general inflation rate)
- [ ] **Debt payments**: Mortgage (fixed or payoff date), other loans

#### 4.2 Budget Configuration
- [ ] Define monthly/annual amounts per category
- [ ] Assign inflation rate to each category (general, healthcare, housing)
- [ ] Support expense changes over time:
  - Mortgage payoff date
  - Reduced discretionary spending in later retirement (spending curve)
  - Increased healthcare costs with age
- [ ] One-time expenses (new car, home repair, travel goals)

#### 4.3 Budget vs Income Analysis
- [ ] Calculate monthly income need based on total expenses
- [ ] Compare income sources to expenses by period
- [ ] Identify income gaps that require portfolio withdrawals
- [ ] Track surplus/deficit over time

### 5. Distribution Strategies (Withdrawal Phase)

#### 5.1 Static Withdrawal
- [ ] Support fixed withdrawal rate (e.g., 4% rule)
- [ ] Adjust annual withdrawal for inflation
- [ ] Configure initial withdrawal rate

#### 5.2 Bucket Strategy
- [ ] Define bucket allocations (short/medium/long-term)
- [ ] Configure time horizons for each bucket
- [ ] Model bucket refill rules (when and how to move funds between buckets)
- [ ] Draw from appropriate bucket based on time horizon

#### 5.3 Retirement Spending Curve
- [ ] Define spending phases (go-go, slow-go, no-go) with age ranges
- [ ] Configure spending multipliers or target amounts per phase
- [ ] Model healthcare cost increases in later phases
- [ ] Smooth transitions between phases

#### 5.4 Guardrails Strategy
- [ ] Set initial withdrawal rate and inflation adjustment
- [ ] Define upper guardrail threshold (increase spending trigger)
- [ ] Define lower guardrail threshold (decrease spending trigger)
- [ ] Configure adjustment percentages when guardrails are hit
- [ ] Set floor and ceiling constraints on withdrawals

#### 5.5 Common Withdrawal Features
- [ ] Calculate monthly withdrawal needs based on target income minus other income sources
- [ ] Model withdrawal sequencing across account types (tax-efficient ordering)
- [ ] Handle portfolio depletion scenarios gracefully
- [ ] Support Required Minimum Distributions (RMDs) when applicable

### 6. Simulation & Analysis

#### 6.1 Time Horizon Configuration
- [ ] **Start date**: Default to today (current date)
- [ ] **Retirement date**: User-specified per Person Profile
- [ ] **End date options**:
  - Life expectancy tables (default) - configurable by gender, health factors
  - User-specified age (e.g., "plan through age 95")
  - Until portfolio depleted (for stress testing)
  - First of: specified age OR portfolio depletion

#### 6.2 Simulation Modes

**Deterministic (Single Path)**:
- [ ] Fixed return rates (e.g., 7% pre-retirement, 5% post-retirement)
- [ ] Single simulation run, single outcome
- [ ] Best for baseline planning and quick analysis

**Monte Carlo (Probabilistic)**:
- [ ] Variable returns based on statistical distribution (mean + standard deviation)
- [ ] Configurable number of simulation runs (default: 1,000 or 10,000)
- [ ] Calculate success rate (% of runs where portfolio lasts to end date)
- [ ] Show distribution of outcomes:
  - 10th percentile (poor outcome)
  - 50th percentile (median)
  - 90th percentile (good outcome)
- [ ] Configurable return assumptions (mean, std dev, distribution type)

**Historical Backtesting**:
- [ ] Run simulation against actual historical market returns
- [ ] Select historical periods (e.g., "What if I retired in 1966, 2000, 2008?")
- [ ] Rolling period analysis (all possible 30-year periods since X)
- [ ] Compare outcomes across different historical starting points

#### 6.3 Monthly Simulation Loop
Each month, the simulation engine executes:

1. **Check life events**:
   - [ ] Retirement date reached (switch from accumulation to distribution)
   - [ ] Social Security claiming age reached (start SS income)
   - [ ] RMD age reached (73, moving to 75 under SECURE 2.0)
   - [ ] Spouse events (retirement, SS, death/survivorship)
   - [ ] Mortgage payoff date
   - [ ] One-time expense triggers
   - [ ] Portfolio depletion

2. **Calculate income** (if applicable):
   - [ ] Working salary (pre-retirement)
   - [ ] Social Security benefits
   - [ ] Pension payments
   - [ ] Annuity payments
   - [ ] Other income (rental, part-time, etc.)

3. **Calculate expenses**:
   - [ ] Apply category-specific inflation rates
   - [ ] Adjust for spending phase (go-go/slow-go/no-go if using spending curve)
   - [ ] Include any one-time expenses for this month

4. **Determine cash flow gap**:
   - [ ] Gap = Total Expenses - Total Income
   - [ ] If gap > 0: Need withdrawal from portfolio
   - [ ] If gap < 0: Surplus (apply to savings or reduce withdrawal)

5. **Execute distribution strategy** (if gap > 0):
   - [ ] Apply chosen strategy (Static, Bucket, Spending Curve, Guardrails)
   - [ ] Follow withdrawal sequencing rules (account order)
   - [ ] Respect RMD requirements if applicable
   - [ ] Handle insufficient funds scenario

6. **Apply contributions** (pre-retirement):
   - [ ] Employee contributions (respect limits, catch-up rules)
   - [ ] Employer match
   - [ ] Route to appropriate accounts

7. **Apply investment returns**:
   - [ ] Calculate monthly return per account
   - [ ] Apply based on simulation mode (fixed, random, historical)

8. **Record transaction**:
   - [ ] Log all activity for this month
   - [ ] Update running totals and metrics

#### 6.4 Key Metrics & Outputs
- [ ] Portfolio balance at retirement
- [ ] Portfolio longevity (when/if depleted)
- [ ] Total contributions made (by account, by source)
- [ ] Total withdrawals taken (by account)
- [ ] Total investment gains/losses
- [ ] Success rate (Monte Carlo: % of runs lasting to end date)
- [ ] Probability of ruin (inverse of success rate)
- [ ] Safe withdrawal rate achieved
- [ ] Income replacement ratio
- [ ] Real vs nominal values (inflation-adjusted)

#### 6.5 Scenario Comparison
- [ ] Run multiple scenarios with different parameters
- [ ] Side-by-side comparison of key metrics
- [ ] "What-if" analysis (change one variable, compare outcomes)
- [ ] Sensitivity analysis (how sensitive is outcome to return rate, inflation, etc.)

### 7. Output & Reporting

This is a **library/API first** design. The core engine produces structured data that can be consumed by UIs, exported to files, or used programmatically. A React-based UI is planned for visualization.

#### 7.1 Architectural Approach
- [ ] Core library performs calculations and simulation
- [ ] API layer exposes functionality for programmatic access
- [ ] Reporting module generates structured data for consumption
- [ ] UI (React) handles interactive visualization and charting
- [ ] Export module generates file formats (CSV, JSON, PDF)

#### 7.2 Report Types

**Transaction Detail Report**:
- [ ] Month-by-month breakdown of all activity
- [ ] Per-account transactions
- [ ] Income received, expenses paid, contributions, withdrawals
- [ ] Running balances

**Summary Dashboard Data**:
- [ ] Key metrics at a glance (balance at retirement, longevity, success rate)
- [ ] Snapshot of current vs projected state
- [ ] Alerts/warnings (e.g., projected shortfall, high withdrawal rate)

**Timeline Report**:
- [ ] Portfolio balance over time
- [ ] Segmented by phase (accumulation vs distribution)
- [ ] Key milestone markers (retirement, SS start, RMD age)

**Cash Flow Report**:
- [ ] Income vs expenses by period
- [ ] Income breakdown by source (salary, SS, pension, withdrawals)
- [ ] Expense breakdown by category
- [ ] Net cash flow (surplus/deficit)

**Account Breakdown Report**:
- [ ] Per-account balances over time
- [ ] Contribution history by account
- [ ] Withdrawal history by account
- [ ] Growth/returns by account

**Tax Projection Report**:
- [ ] Estimated tax liability by year
- [ ] Taxable income sources
- [ ] Tax-advantaged vs taxable withdrawals
- [ ] Effective tax rate

**Scenario Comparison Report**:
- [ ] Side-by-side metrics for multiple scenarios
- [ ] Highlight differences in outcomes
- [ ] Recommendation indicators

**Monte Carlo Results Report**:
- [ ] Success rate (% of runs meeting goal)
- [ ] Distribution of outcomes (percentiles)
- [ ] Probability of ruin analysis
- [ ] Confidence intervals

#### 7.3 Export Formats

**CSV**:
- [ ] Raw transaction data for spreadsheet analysis
- [ ] Summary metrics export
- [ ] Configurable columns/fields

**JSON**:
- [ ] Structured data for programmatic consumption
- [ ] Full simulation results
- [ ] API response format

**PDF**:
- [ ] Formatted printable reports
- [ ] Embedded charts (using Apache PDFBox or similar charting tools)
- [ ] Professional layout for advisor/client sharing

#### 7.4 Data Structures for UI Consumption

The API should return data structured for easy UI rendering:

- [ ] Time-series data for line charts (portfolio balance, income, expenses)
- [ ] Categorical data for pie/bar charts (account breakdown, expense categories)
- [ ] Distribution data for histograms (Monte Carlo outcomes)
- [ ] Comparison data for side-by-side tables
- [ ] Milestone/event data for timeline markers

#### 7.5 Granularity Options
- [ ] **Monthly**: Full detail for deep analysis
- [ ] **Annual**: Year-by-year summary
- [ ] **Phase**: Accumulation vs distribution totals
- [ ] **Milestone**: Key events (retirement, SS, RMD, depletion)

---

## Non-Functional Goals

- **Accuracy**: Financial calculations must be precise and auditable
- **Testability**: Comprehensive unit tests for all calculations
- **Extensibility**: Easy to add new income sources, withdrawal strategies, or calculation methods
- **Usability**: Clear API for constructing portfolios and running simulations

### Architecture Principles

**Library/API First**:
- Core simulation engine is a standalone Java library
- No UI dependencies in core logic
- Clean separation of concerns (model, calculation, reporting)
- API layer for programmatic access

**Layered Design**:
```
┌─────────────────────────────────────┐
│         UI Layer (React)            │  ← Visualization, user interaction
├─────────────────────────────────────┤
│         API Layer (REST/JSON)       │  ← HTTP endpoints, request/response
├─────────────────────────────────────┤
│       Reporting Module              │  ← Report generation, export formats
├─────────────────────────────────────┤
│      Simulation Engine              │  ← Core calculation, scenario execution
├─────────────────────────────────────┤
│         Domain Model                │  ← Person, Portfolio, Account, Transaction
└─────────────────────────────────────┘
```

**Technology Stack** (Planned):
- **Core Library**: Java (current)
- **API Layer**: Spring Boot (or similar)
- **UI**: React
- **PDF Generation**: Apache PDFBox with charting
- **Data Export**: Jackson (JSON), OpenCSV or similar (CSV)

### Development Standards

All development must adhere to these standards. These are **non-negotiable**.

#### Java Best Practices

**SOLID Principles**:
- **S**ingle Responsibility: Each class has one reason to change
- **O**pen/Closed: Open for extension, closed for modification
- **L**iskov Substitution: Subtypes must be substitutable for base types
- **I**nterface Segregation: Many specific interfaces over one general-purpose
- **D**ependency Inversion: Depend on abstractions, not concretions

**Gang of Four (GoF) Design Patterns**:
Apply appropriate patterns where they add value:
- **Creational**: Builder (already used), Factory, Singleton (sparingly)
- **Structural**: Adapter, Decorator, Composite
- **Behavioral**: Strategy (for distribution strategies), Observer, Command, Template Method

**Code Quality**:
- Meaningful names for classes, methods, variables
- Small, focused methods (single responsibility)
- Prefer composition over inheritance
- Immutable objects where possible
- Proper encapsulation (minimize public APIs)
- No magic numbers - use named constants
- Handle exceptions appropriately (don't swallow)

#### Testing Requirements

**Unit Testing is Mandatory**:
- All business logic must have unit tests
- Test-Driven Development (TDD) encouraged
- Aim for high code coverage on core calculation logic
- Use JUnit 5 (already in project)
- Use descriptive test names (`@DisplayName`)
- Follow Arrange-Act-Assert pattern

**Test Categories**:
- **Unit tests**: Isolated tests of individual classes/methods
- **Integration tests**: Test component interactions
- **Scenario tests**: End-to-end simulation validation

#### Git Workflow

**Branch Strategy**:
- `main`: Production-ready code only
- `develop`: Integration branch for features
- `feature/<issue-number>-<short-description>`: Feature branches
- `bugfix/<issue-number>-<short-description>`: Bug fix branches
- `release/<version>`: Release preparation branches

**Commit Standards**:
- Atomic commits (one logical change per commit)
- Clear, descriptive commit messages
- Reference issue numbers in commits (e.g., "Fixes #123")
- No commits directly to `main` - use Pull Requests

**Pull Request Requirements**:
- Descriptive title and summary
- Link to related issue(s)
- All tests must pass
- Code review required before merge
- Squash or rebase to keep history clean

**Code Review Checklist**:
- [ ] Follows SOLID principles
- [ ] Appropriate design patterns used
- [ ] Unit tests included and passing
- [ ] No commented-out code
- [ ] No TODOs without linked issues
- [ ] Documentation updated if needed

---

## Current State

### Existing Code

**`PortfolioParameters.java`**:
- Full parameter model with builders for investments, contributions, income
- Contains nested classes: Investments, Contribution, WorkingIncome, WithdrawalIncome, MonthlyRetirementIncome
- Uses Builder pattern appropriately
- **Refactoring needed**: May need restructuring to align with new Person Profile / Portfolio concepts

**`Functions.java`**:
- Core financial calculations (inflation, COLA, contributions, SS, other income)
- Functional interfaces with lambda implementations
- **Refactoring needed**: Consider breaking into separate classes by responsibility (InflationCalculator, ContributionCalculator, etc.) to follow Single Responsibility Principle

**`Transaction.java`**:
- Partial implementation with retirement status, contribution rates, income calculations
- `getEndBalance()` returns hardcoded 0.0
- **Refactoring needed**: Significant work needed to complete; may need redesign for multi-account support

**Enums**:
- `TransactionType`: CONTRIBUTION, WITHDRAWAL
- `ContributionType`: PERSONAL, EMPLOYER
- `WithdrawalType`: FIXED, SALARY
- **May need expansion** for new distribution strategies

**Tests**:
- `FunctionsTest.java`: 17 tests covering calculation functions
- `TransactionTest.java`: 16 tests covering Transaction behavior
- **Refactoring needed**: Tests may need updates as code is refactored

### Refactoring Considerations

Based on the expanded requirements, the existing code needs:

1. **Domain Model Restructuring**:
   - Introduce `PersonProfile` as top-level entity
   - Separate `Portfolio` from `PortfolioParameters`
   - Create `InvestmentAccount` for individual accounts
   - Extract `Scenario` as simulation configuration

2. **Functions Decomposition**:
   - Break monolithic Functions class into focused calculators
   - Consider Strategy pattern for different calculation approaches

3. **Transaction Redesign**:
   - Complete `getEndBalance()` implementation
   - Support multi-account transactions
   - Align with monthly simulation loop requirements

### Not Yet Implemented
- Person Profile model
- Multi-account portfolio support
- IRS contribution limits and rules
- Expense/budget modeling
- Distribution strategies (beyond basic)
- Simulation engine
- Reporting module
- API layer

---

## Proposed Milestones

### Milestone 1: Domain Model Foundation
**Goal**: Establish core domain model with clean architecture; refactor existing code

**Domain Entities**:
- Create `PersonProfile` model (DOB, retirement date, life expectancy, linked spouse)
- Create `Portfolio` as container for investment accounts
- Create `InvestmentAccount` model (type, allocation, return rates, balance)
- Create `Scenario` configuration model
- Define account type enum (401k, IRA, Roth IRA, Roth 401k, HSA, Taxable)

**Refactoring**:
- Restructure `PortfolioParameters` to align with new domain model
- Decompose `Functions` class into focused calculator classes (Single Responsibility)
- Apply Strategy pattern where appropriate
- Update existing tests to match refactored code

**Foundation**:
- Establish package structure following layered architecture
- Set up proper interfaces for extensibility
- Full test coverage for domain model

### Milestone 2: Core Transaction & Account Operations
**Goal**: Fully functional transaction processing for individual accounts

- Implement `Transaction` with complete balance calculations
- Calculate investment returns (monthly compounding)
- Calculate actual contribution amounts (rate × salary)
- Calculate actual withdrawal amounts
- Support transaction chaining (previous balance → current start balance)
- Account-level transaction history
- Full test coverage

### Milestone 3: Multi-Account Portfolio & Contribution Rules
**Goal**: Support multiple investment accounts with IRS-compliant contribution modeling

**Multi-Account Support**:
- Portfolio aggregation across accounts
- Contribution routing to specific accounts
- Cross-account balance tracking

**IRS Contribution Rules**:
- Contribution limits by account type (401k, IRA, Roth, HSA)
- Age-based catch-up contributions (50+, 55+ for HSA, 60-63 super catch-up)
- Income-based phase-outs (IRA deductibility, Roth eligibility)
- SECURE 2.0 rule support (effective dates, Roth catch-up for high earners)
- Base year limits with inflation projection
- Year-to-date contribution tracking against limits

### Milestone 4: Income Modeling
**Goal**: Comprehensive income source modeling

**Working Income**:
- Salary with configurable COLA
- Income start/end dates

**Social Security**:
- FRA benefit input from SSA.gov
- Early/delayed claiming adjustments
- Spousal benefits
- Survivor benefits
- Earnings test
- Benefit taxation thresholds

**Pensions & Annuities**:
- Defined benefit pension modeling
- Survivor benefit options
- Fixed/variable annuity support
- COLA adjustments

**Other Income**:
- Rental income
- Part-time retirement work
- Other recurring sources

### Milestone 5: Expense & Budget Modeling
**Goal**: Category-based expense tracking with differentiated inflation

- Expense categories (essential, healthcare, housing, discretionary, debt)
- Category-specific inflation rates
- Expense changes over time (mortgage payoff, spending phases)
- One-time expense support
- Budget vs income gap analysis

### Milestone 6: Distribution Strategies
**Goal**: Implement the four core withdrawal strategies

**Strategies**:
- **6a**: Static withdrawal (fixed rate with inflation adjustment)
- **6b**: Bucket strategy (time-segmented withdrawals)
- **6c**: Spending curve (phase-based withdrawal targets)
- **6d**: Guardrails (dynamic withdrawal adjustments)

**Framework**:
- Strategy interface/abstraction (Strategy pattern)
- Withdrawal sequencing across account types
- Tax-efficient withdrawal ordering
- RMD integration

### Milestone 7: Simulation Engine
**Goal**: Generate complete monthly transaction sequences

**Core Engine**:
- Simulation runner (start date to end date)
- Monthly simulation loop (8-step process)
- Transaction generation per account per month
- Phase handling (accumulation → distribution transition)

**Event Handling**:
- Retirement date trigger
- Social Security start
- RMD age trigger
- Spouse events
- One-time expenses
- Portfolio depletion

**Statistics**:
- Cumulative tracking (contributions, withdrawals, gains)
- Running balances and metrics

### Milestone 8: Scenario Analysis
**Goal**: Compare different retirement scenarios

**Simulation Modes**:
- Deterministic (fixed returns)
- Monte Carlo (probabilistic, N runs, percentiles)
- Historical backtesting (actual market data)

**Analysis Features**:
- Multiple scenario configurations
- Side-by-side comparison
- Sensitivity analysis
- Success rate / probability of ruin

### Milestone 9: Output & Reporting
**Goal**: Export simulation results in useful formats

**Report Types**:
- Transaction detail
- Summary dashboard
- Timeline report
- Cash flow report
- Account breakdown
- Tax projection
- Scenario comparison
- Monte Carlo results

**Export Formats**:
- CSV (raw data)
- JSON (API/programmatic)
- PDF (formatted with charts)

**API**:
- Structured data for UI consumption
- Granularity options (monthly, annual, phase, milestone)

### Milestone 10: API Layer
**Goal**: RESTful API for programmatic access

- Spring Boot application setup
- REST endpoints for all operations
- Request/response DTOs
- API documentation (OpenAPI/Swagger)
- Authentication/authorization (if needed)

### Milestone 11: UI (React)
**Goal**: Interactive web interface

- React application setup
- Person Profile management
- Portfolio configuration
- Scenario builder
- Simulation execution
- Results visualization (charts, tables)
- Report generation

### Future Milestones

**Milestone F1: Roth Conversion Strategies**
- Optimal conversion analysis
- Roth conversion ladder modeling
- Tax liability projections

**Milestone F2: Rebalancing**
- Per-account rebalancing
- Cross-account rebalancing
- Tax-efficient rebalancing

**Milestone F3: Advanced Tax Modeling**
- State tax support
- Capital gains tracking
- Tax-loss harvesting

---

## Open Questions

### Resolved
- ~~**Multiple accounts**: Should we support modeling multiple accounts with different characteristics?~~
  **Decision**: Yes - support multiple investment accounts with distinct types, allocations, and rules.

- ~~**Withdrawal strategies**: What strategies should we support?~~
  **Decision**: Support Static, Bucket, Spending Curve, and Guardrails strategies.

- ~~**Simulation granularity**: Monthly is assumed - is this sufficient, or do we need flexibility?~~
  **Decision**: Monthly granularity is sufficient. Aligns with Social Security payments and provides accurate compounding modeling.

- ~~**Spouse/joint modeling**: Single person only, or support for couples?~~
  **Decision**: Support both single and spouse/joint modeling, including joint Social Security benefits and survivorship scenarios.

- ~~**Withdrawal sequencing**: What's the default order for multi-account withdrawals?~~
  **Decision**: Provide a default tax-efficient sequence (Taxable → Traditional → Roth) with support for custom withdrawal sequences. Allow rules and contingencies for sequencing logic.

- ~~**Historical vs projected returns**: Should we support backtesting against historical data?~~
  **Decision**: Support both fixed projected rates and historical data backtesting.

### Open

#### ~~Tax Modeling~~ (Resolved)

**Decisions**:

1. **Federal tax brackets**: Yes - provide current tax bracket defaults with option to override. Enable custom bracket configuration for users who want to model future tax law changes.

2. **Account type tax treatment**: Model the tax differences between account types:
   - **Traditional 401(k)/IRA**: Pre-tax contributions, taxed on withdrawal as ordinary income
   - **Roth 401(k)/IRA**: Post-tax contributions, tax-free qualified withdrawals
   - **HSA**: Pre-tax contributions, tax-free withdrawals for qualified medical expenses
   - **Taxable Brokerage**: Post-tax contributions, capital gains taxes on growth, dividends taxed annually

3. **Capital gains (taxable accounts)**: Use simple estimation approach - assume a configurable percentage of growth is taxable gains rather than tracking full cost basis. This balances accuracy with complexity.

4. **Roth conversions**: Separate milestone - not part of core distribution strategies but planned as a future enhancement. Requires tax bracket awareness to model effectively.

5. **State taxes**: Out of scope for initial implementation. Could be added as optional module in future.

#### ~~Rebalancing~~ (Future Investigation)
**Decision**: Deferred - requires further research and design.

Rebalancing is complex because it involves simulating variable investment changes within accounts. Questions to explore in the future:
- Per-account rebalancing to target allocation?
- Cross-account rebalancing (tax-efficient asset placement)?
- Frequency: annual, threshold-based (e.g., 5% drift), or both?
- Tax implications of rebalancing in taxable accounts (capital gains triggers)?
- How to model allocation drift between rebalancing events?

**Note**: Initial implementation will assume static allocations per account. Rebalancing may be added as a future enhancement once the core simulation is stable.

#### ~~Inflation Modeling~~ (Resolved)
**Decision**: Support three separate inflation rates:
- **General inflation (CPI)**: Applied to most expenses
- **Healthcare inflation**: Typically higher than CPI (~5-6% historically vs ~2-3% CPI)
- **Housing inflation**: Property taxes, maintenance, insurance

This enables more accurate modeling of expense changes over a 30+ year retirement horizon, where healthcare costs often grow faster than general inflation.

---

## Notes

_This document will evolve as we refine requirements. Each milestone may spawn detailed epic/story breakdowns._