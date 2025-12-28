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

### Implemented (Milestones 1, 2, 3a, 3b & 4 Complete)

**Domain Model** (`io.github.xmljim.retirement.domain.model`):
- `PersonProfile` - Individual with DOB, retirement date, life expectancy, linked spouse support
- `Portfolio` - Container for investment accounts with aggregation
- `InvestmentAccount` - Account with type, allocation, return rates, balance
- `Scenario` - Simulation configuration (time horizon, assumptions, strategies)
- `Transaction` - Complete transaction with balance tracking, chaining, and metadata (M2)
- `TransactionHistory` - Immutable collection of transactions with query and aggregation (M2)
- `CouplePortfolioView` - Aggregate view across spouse portfolios with per-spouse YTD tracking (M3a)

**Value Objects** (`io.github.xmljim.retirement.domain.value`):
- `WorkingIncome` - Salary with COLA, includes `priorYearIncome` for SECURE 2.0
- `ContributionConfig` - Contribution settings with `targetAccountType` and `matchingPolicy`
- `AssetAllocation` - Stock/bond/cash allocation with validation
- `RetirementIncome` - Social Security, pension, annuity income sources
- `MatchingPolicy` - Interface with `SimpleMatchingPolicy`, `TieredMatchingPolicy`, `NoMatchingPolicy`
- `MatchTier` - Record for tiered matching configuration
- `WithdrawalResult` - Record capturing withdrawal amount, account details, and remaining balance (M2)
- `ContributionRecord` - Immutable record of a single contribution (M3a)
- `RoutingConfiguration` - Configuration for contribution routing rules (M3a)
- `RoutingRule` - Individual routing rule with account type and percentage (M3a)
- `YTDSummary` - Year-to-date contribution summary with limits and remaining room (M3a)
- `IncomeDetails` - MAGI add-back components value object (M3b)
- `PhaseOutResult` - Phase-out calculation result with warnings (M3b)

**Enums** (`io.github.xmljim.retirement.domain.enums`):
- `AccountType` - 401K, IRA, ROTH variants, HSA, Taxable Brokerage with `TaxTreatment`
- `ContributionType` - PERSONAL, EMPLOYER
- `TransactionType` - CONTRIBUTION, WITHDRAWAL, RETURN, FEE, TRANSFER (M2)
- `TaxTreatment` - PRE_TAX, POST_TAX, TAX_FREE
- `LimitCategory` - EMPLOYER_401K, IRA, HSA for IRS limit grouping (M3a)
- `FilingStatus` - Tax filing status (Single, MFJ, MFS, HOH, QSS) with helper methods (M3b)

**Calculator Framework** (`io.github.xmljim.retirement.domain.calculator`):
- `Calculator` - Base interface with calculation lifecycle
- `InflationCalculator` - Inflation adjustment calculations
- `ContributionCalculator` - Contribution amount calculations
- `SocialSecurityCalculator` - SS benefit calculations with claiming age adjustments
- `IrsContributionRules` - Interface for IRS rule implementations
- `Secure2ContributionRules` - SECURE 2.0 implementation with catch-up rules
- `ReturnCalculator` - Investment return calculations with compounding options (M2)
- `WithdrawalCalculator` - Withdrawal amount calculations with strategy support (M2)
- `CompoundingFunction` - Functional interface for compounding strategies (M2)
- `CompoundingFunctions` - Standard implementations: ANNUAL, MONTHLY, DAILY, CONTINUOUS (M2)
- `ContributionRouter` - Routes contributions to appropriate accounts (M3a)
- `ContributionAllocation` - Tracks allocated contribution amounts (M3a)
- `YTDContributionTracker` - Year-to-date contribution tracking interface (M3a)
- `ContributionLimitChecker` - Pre-contribution limit validation (M3a)
- `LimitCheckResult` - Result of contribution limit check (M3a)
- `MAGICalculator` - Modified Adjusted Gross Income calculation interface (M3b)
- `PhaseOutCalculator` - IRA contribution phase-out calculation interface (M3b)

**Calculator Implementations** (`io.github.xmljim.retirement.domain.calculator.impl`):
- `DefaultInflationCalculator` - Standard inflation adjustments
- `DefaultContributionCalculator` - Standard contribution calculations
- `DefaultSocialSecurityCalculator` - SS benefit with claiming age adjustments
- `DefaultReturnCalculator` - Investment growth with pluggable compounding (M2)
- `DefaultWithdrawalCalculator` - Basic withdrawal logic (M2)
- `MathUtils` - BigDecimal power functions for financial calculations (M2)
- `DefaultContributionRouter` - Standard contribution routing with overflow (M3a)
- `DefaultYTDContributionTracker` - Immutable YTD tracking implementation (M3a)
- `DefaultContributionLimitChecker` - IRS limit enforcement (M3a)
- `DefaultMAGICalculator` - MAGI calculation implementation (M3b)
- `DefaultPhaseOutCalculator` - Phase-out calculation with linear interpolation (M3b)

**Configuration** (`io.github.xmljim.retirement.domain.config`):
- `IrsContributionLimits` - Spring `@ConfigurationProperties` for IRS limits from YAML
- Supports 401(k), IRA, and HSA limits with year-by-year configuration
- IRS-style COLA rounding for future year extrapolation
- `IraPhaseOutLimits` - Phase-out threshold configuration with YAML-driven thresholds (M3b)
- `PhaseOutRange` and `YearPhaseOuts` records for structured phase-out configuration (M3b)

**Exceptions** (`io.github.xmljim.retirement.domain.exception`):
- `ValidationException` - Domain validation errors
- `MissingRequiredFieldException` - Required field validation
- `CalculationException` - Calculation-specific errors (M2)
- `InvalidDateRangeException` - Date range validation (M2)

**Income Sources** (`io.github.xmljim.retirement.domain.value`):
- `WorkingIncome` - Salary with COLA, start/end dates, `priorYearIncome` for SECURE 2.0 (M1, enhanced M4)
- `SocialSecurityBenefit` - SS benefit with FRA, claiming age, early/delayed adjustments, COLA (M4)
- `Pension` - Defined benefit with payment forms, survivor options, COLA (M4)
- `Annuity` - Fixed/deferred/variable annuity support with COLA (M4)
- `OtherIncome` - Rental, part-time work, royalties, dividends, business income (M4)

**Marriage & Spousal Benefits** (`io.github.xmljim.retirement.domain.value`):
- `MarriageInfo` - Current marriage with spousal benefits eligibility (M4)
- `PastMarriage` - Marriage history for divorced spouse benefits (M4)
- `SpousalBenefitResult` - Spousal benefit calculation result (M4)
- `SurvivorBenefitResult` - Survivor benefit calculation result (M4)
- `CoupleClaimingStrategy` - Coordinated SS claiming strategy for couples (M4)

**Income Aggregation** (`io.github.xmljim.retirement.domain.value`, `io.github.xmljim.retirement.domain.calculator`):
- `IncomeSources` - Container for all income sources per person (M4)
- `IncomeBreakdown` - Income breakdown by source type with earned/passive classification (M4)
- `IncomeAggregator` - Aggregates income across sources for a date (M4)

**SS Earnings Test & Taxation** (`io.github.xmljim.retirement.domain.calculator`, `io.github.xmljim.retirement.domain.value`):
- `EarningsTestCalculator` - SS earnings test (benefit reduction when working before FRA) (M4)
- `EarningsTestResult` - Earnings test result with reduction details (M4)
- `BenefitTaxationCalculator` - SS benefit taxation (0%/50%/85% thresholds) (M4)
- `TaxationResult` - Taxation calculation result with combined income (M4)

**Spousal/Survivor Calculator** (`io.github.xmljim.retirement.domain.calculator`):
- `SpousalBenefitCalculator` - Spousal and survivor benefit calculations (M4)
- `DefaultSpousalBenefitCalculator` - Implementation with SSA rules (M4)

**Income-Related Enums** (`io.github.xmljim.retirement.domain.enums`):
- `AnnuityType` - FIXED_IMMEDIATE, FIXED_DEFERRED, VARIABLE (M4)
- `OtherIncomeType` - RENTAL, PART_TIME_WORK, ROYALTIES, DIVIDENDS, BUSINESS, OTHER (M4)
- `PensionPaymentForm` - SINGLE_LIFE, JOINT_100, JOINT_75, JOINT_50, PERIOD_CERTAIN (M4)
- `MaritalStatus` - MARRIED, SINGLE, DIVORCED, WIDOWED (M4)
- `MarriageEndReason` - DIVORCE, DEATH (M4)

### Legacy Code (Deprecated)

The following classes are deprecated and maintained for backwards compatibility:

**`PortfolioParameters.java`** (`@Deprecated`):
- Original parameter model - replaced by domain model classes
- Migration guide in Javadoc

**`Functions.java`** (`@Deprecated`):
- Original calculations - replaced by Calculator framework
- Migration guide in Javadoc

**`model/ContributionType.java`, `model/WithdrawalType.java`, `model/TransactionType.java`** (`@Deprecated`):
- Original enums - replaced by `domain.enums` equivalents

### Not Yet Implemented
- Expense/budget modeling (M5)
- Distribution strategies (M6)
- Simulation engine (M7)
- Scenario analysis (M8)
- Reporting module (M9)
- API layer (M10)
- UI (M11)

---

## Proposed Milestones

### Milestone 1: Domain Model Foundation ✅ COMPLETE
**Goal**: Establish core domain model with clean architecture; refactor existing code

**Status**: Completed December 2025

**Domain Entities** (Completed):
- [x] Created `PersonProfile` model (DOB, retirement date, life expectancy, linked spouse)
- [x] Created `Portfolio` as container for investment accounts
- [x] Created `InvestmentAccount` model (type, allocation, return rates, balance)
- [x] Created `Scenario` configuration model
- [x] Defined `AccountType` enum (401k, IRA, Roth IRA, Roth 401k, HSA, Taxable Brokerage) with `TaxTreatment`

**Calculator Framework** (Completed):
- [x] Created `Calculator` base interface with calculation lifecycle
- [x] Implemented `InflationCalculator`, `ContributionCalculator`, `SocialSecurityCalculator`
- [x] Applied Strategy pattern for extensibility
- [x] Decomposed monolithic `Functions` class (marked `@Deprecated`)

**IRS Contribution Rules - SECURE 2.0** (Completed):
- [x] Created `IrsContributionLimits` with Spring `@ConfigurationProperties`
- [x] Implemented `IrsContributionRules` interface and `Secure2ContributionRules`
- [x] Age-based catch-up (50+) and super catch-up (60-63, effective 2025)
- [x] ROTH-only catch-up for high earners ($145K+, effective 2026)
- [x] HSA contribution limits with age 55+ catch-up
- [x] IRS-style COLA rounding ($500 for limits, $50 for HSA)
- [x] YAML configuration with year-by-year limits through 2026

**Matching Policies** (Completed):
- [x] Created `MatchingPolicy` interface with `SimpleMatchingPolicy`, `TieredMatchingPolicy`, `NoMatchingPolicy`
- [x] Created `MatchTier` record for tiered matching configuration

**Value Objects** (Completed):
- [x] Created `WorkingIncome` with `priorYearIncome` for SECURE 2.0 rules
- [x] Created `ContributionConfig` with `targetAccountType` and `matchingPolicy`
- [x] Created `AssetAllocation`, `RetirementIncome` and related value objects

**Foundation** (Completed):
- [x] Established package structure: `domain.model`, `domain.value`, `domain.enums`, `domain.calculator`, `domain.config`, `domain.exception`
- [x] Set up proper interfaces for extensibility
- [x] Full test coverage with 80%+ line coverage
- [x] Quality gates: Checkstyle, SpotBugs, PMD all passing
- [x] Package-level Javadoc for all packages
- [x] Legacy code marked `@Deprecated` with migration guides

### Milestone 2: Core Transaction & Account Operations ✅ COMPLETE
**Goal**: Fully functional transaction processing for individual accounts

**Status**: Completed December 2025

**Transaction Model** (Completed):
- [x] Created new `Transaction` class (`domain.model`) with complete balance tracking
- [x] Transaction fields: type, amount, description, account type, start balance, end balance, date, previous transaction
- [x] Support transaction chaining via `previous` reference for balance continuity
- [x] Builder pattern with fluent API for transaction construction
- [x] Created new `TransactionType` enum (`domain.enums`) - CONTRIBUTION, WITHDRAWAL, RETURN, FEE, TRANSFER
- [x] Deprecated legacy `TransactionType` in `domain.model` with migration guide

**Transaction History** (Completed):
- [x] Created `TransactionHistory` class for account-level transaction tracking
- [x] Immutable design with `append()` returning new instance
- [x] Query methods: `getByType()`, `getByDateRange()`, `getLatest()`
- [x] Aggregation: `getTotalContributions()`, `getTotalWithdrawals()`, `getNetChange()`
- [x] Balance tracking: `getCurrentBalance()` from latest transaction

**Withdrawal Calculations** (Completed):
- [x] Created `WithdrawalResult` record capturing withdrawal details and remaining balance
- [x] Created `WithdrawalCalculator` interface for withdrawal strategy abstraction
- [x] Implemented `DefaultWithdrawalCalculator` with basic withdrawal logic

**Investment Returns** (Completed):
- [x] Created `CompoundingFunction` functional interface (Strategy pattern)
- [x] Created `CompoundingFunctions` utility class with standard implementations:
  - `ANNUAL` - True annual compounding: `principal * (1 + rate)^(months/12)`
  - `MONTHLY` - Discrete monthly: `principal * (1 + rate/12)^months`
  - `DAILY` - Daily compounding: `principal * (1 + rate/365)^days`
  - `CONTINUOUS` - Continuous: `principal * e^(rate * years)`
- [x] Updated `ReturnCalculator` with `calculateAccountGrowth(balance, rate, periods, compounding)` overload
- [x] Made `MathUtils` public for reuse across calculators

**Test Coverage** (Completed):
- [x] Comprehensive tests for all new classes
- [x] Edge case coverage: null handling, zero values, negative values
- [x] Integration tests verifying calculator interactions
- [x] 80%+ line coverage maintained

### Milestone 3a: Contribution Routing & Tracking ✅ COMPLETE
**Goal**: Route contributions to correct accounts and track against IRS limits

**Status**: Completed December 2025

**Note**: Portfolio aggregation, IRS limits configuration, age-based catch-up, and SECURE 2.0 Roth catch-up were completed in M1.

**Contribution Routing** (Completed):
- [x] `ContributionRouter` interface and `DefaultContributionRouter` implementation
- [x] `RoutingConfiguration` and `RoutingRule` for configurable routing
- [x] Support split contributions (e.g., 80% Traditional, 20% Roth)
- [x] Overflow handling when primary account hits limit
- [x] Employer match routing (always to Traditional)
- [x] High earner ROTH catch-up routing per SECURE 2.0
- [x] `ContributionAllocation` for tracking routed amounts

**YTD Tracking & Limit Enforcement** (Completed):
- [x] `YTDContributionTracker` interface with immutable `DefaultYTDContributionTracker`
- [x] `ContributionRecord` for tracking individual contributions
- [x] `LimitCategory` enum (EMPLOYER_401K, IRA, HSA) for limit grouping
- [x] `ContributionLimitChecker` with `LimitCheckResult` for pre-contribution validation
- [x] Combined IRA limit enforcement (Traditional + Roth share limit)
- [x] HSA family vs individual limit based on spouse linkage
- [x] `YTDSummary` for contribution status reporting
- [x] `CouplePortfolioView` for combined view with per-spouse YTD tracking

**Test Coverage** (Completed):
- [x] Comprehensive tests for routing (single, split, overflow, employer match)
- [x] YTD tracking tests (contributions, year rollover, at-limit scenarios)
- [x] Limit checker tests (all account types, catch-up, HSA family/individual)
- [x] Integration tests (full year, couple with different ages)
- [x] Birthday edge case tests for catch-up eligibility transitions

**Total Points**: 16

### Milestone 3b: Income-Based Phase-Outs ✅ COMPLETE
**Goal**: Implement MAGI calculations and contribution eligibility phase-outs

**Status**: Completed December 2025

**Filing Status Support** (Completed):
- [x] `FilingStatus` enum (Single, MFJ, MFS, HOH, QSS) with helper methods
- [x] `IraPhaseOutLimits` configuration with YAML-driven thresholds
- [x] `PhaseOutRange` and `YearPhaseOuts` records for structured configuration
- [x] Extrapolation support for future years with IRS-style rounding

**MAGI Calculator** (Completed):
- [x] `MAGICalculator` interface with `DefaultMAGICalculator` implementation
- [x] `IncomeDetails` value object with 6 add-back items
- [x] Simple calculation: MAGI = AGI + total add-backs

**Phase-Out Rules** (Completed):
- [x] `PhaseOutCalculator` interface for Roth and Traditional IRA phase-outs
- [x] `DefaultPhaseOutCalculator` with linear interpolation
- [x] `PhaseOutResult` record with builder pattern
- [x] Traditional IRA deductibility phase-out (covered by employer plan, spouse covered, not covered)
- [x] Roth IRA contribution eligibility phase-out
- [x] Backdoor Roth awareness flagging with warning messages
- [x] IRS rounding rules (up to nearest $10, $200 minimum)

**Test Coverage** (Completed):
- [x] `PhaseOutCalculatorTest` - 13 unit tests
- [x] `PhaseOutIntegrationTest` - 12 end-to-end tests
- [x] `MAGICalculatorTest` - 15 tests
- [x] `IncomeDetailsTest` - 9 tests
- [x] `FilingStatusTest` - 6 tests
- [x] `IraPhaseOutLimitsTest` - 8 tests

**Technical Debt Resolution** (Completed):
- [x] Extracted `TestIrsLimitsFixture` shared test utility
- [x] Extracted `TestPhaseOutFixture` shared test utility
- [x] Removed CPD-OFF suppressions from test files
- [x] Created `DEPRECATED_CODE_MIGRATION.md` documenting M1 deprecated types

**Total Points**: 19

### Milestone 4: Income Modeling ✅ COMPLETE
**Goal**: Comprehensive income source modeling

**Status**: Completed December 2025

#### Working Income (Completed)
- [x] `WorkingIncome` value object with salary, COLA rate, start/end dates
- [x] `getAnnualSalary(year)` with COLA compounding from start date
- [x] `getMonthlySalary(date)` for date-specific salary lookup
- [x] `isActiveOn(date)` for employment period validation
- [x] `priorYearIncome` field for SECURE 2.0 catch-up rules

#### Social Security Benefits (Completed)
- [x] `SocialSecurityBenefit` value object with FRA benefit, birth year, claiming age
- [x] `SocialSecurityCalculator` with FRA lookup by birth year (SSA tables)
- [x] Early claiming reduction: 5/9% per month for first 36 months, 5/12% beyond
- [x] Delayed retirement credits: 8% per year (2/3% per month) up to age 70
- [x] COLA adjustments with configurable rate and compounding
- [x] `getMonthlyBenefit(date)` returning adjusted benefit for any date

#### Social Security Spousal & Survivor Benefits (Completed)
- [x] `SpousalBenefitCalculator` interface with `DefaultSpousalBenefitCalculator`
- [x] Spousal benefits: 50% of higher earner's FRA benefit (if greater than own)
- [x] Divorced spouse benefits: requires 10+ year marriage, age 62+, unmarried
- [x] Survivor benefits: 100% of deceased spouse's benefit
- [x] Divorced survivor benefits: requires 10+ year marriage, 9+ month marriage duration
- [x] **Critical SSA rule**: Spousal benefits do NOT reduce primary earner's benefit
- [x] `MarriageInfo` for current marriage (spouse reference, marriage date, status)
- [x] `PastMarriage` for marriage history (ex-spouse DOB, dates, end reason)
- [x] `SpousalBenefitResult` and `SurvivorBenefitResult` records

**Marriage History Model**:
```
                    ┌─────────────┐
                    │  Person B   │ (A's ex)
                    │  (history)  │
                    └──────▲──────┘
                           │ ex-spouse
    ┌──────────────────────┴───────────────────────┐
    │                                              │
┌───┴───────┐                              ┌───────┴───┐
│ Person A  │◄────── current spouse ──────►│ Person C  │
│ (primary) │                              │ (spouse)  │
└───────────┘                              └───────┬───┘
                                                   │ ex-spouse
                                           ┌───────▼──────┐
                                           │   Person D   │ (C's ex)
                                           │   (history)  │
                                           └──────────────┘
```

#### Social Security Earnings Test (Completed)
- [x] `EarningsTestCalculator` interface with `DefaultEarningsTestCalculator`
- [x] Below FRA: $1 reduction per $2 earned over annual limit
- [x] Year reaching FRA (months before FRA): $1 reduction per $3 over higher limit
- [x] At/after FRA: No earnings test applies
- [x] `EarningsTestResult` record with reduction amount, months withheld, exempt reason
- [x] Year-specific limits from SSA (indexed annually via YAML configuration)
- [x] 2024 limits: $22,320 (below FRA) / $59,520 (FRA year)
- [x] 2025 limits: $23,400 (below FRA) / $62,160 (FRA year)

#### Social Security Benefit Taxation (Completed)
- [x] `BenefitTaxationCalculator` interface with `DefaultBenefitTaxationCalculator`
- [x] Combined income formula: AGI + non-taxable interest + 50% of SS benefits
- [x] `TaxationResult` record with taxable amount, percentage, tier
- [x] **Thresholds are NOT indexed** (fixed since 1984/1993):
  - Single/HOH/QSS: $25,000 (50% tier) / $34,000 (85% tier)
  - MFJ: $32,000 (50% tier) / $44,000 (85% tier)
  - MFS (living with spouse): $0 threshold - always up to 85% taxable

#### Pension / Defined Benefit Plans (Completed)
- [x] `Pension` value object with name, monthly benefit, start date
- [x] `PensionPaymentForm` enum: SINGLE_LIFE, JOINT_100, JOINT_75, JOINT_50, PERIOD_CERTAIN
- [x] Survivor benefit calculation based on payment form
- [x] Optional COLA with annual compounding
- [x] `getMonthlyBenefit(date)` with COLA applied
- [x] `getAnnualBenefit(year)` for yearly totals
- [x] `getSurvivorBenefit(date)` for survivor benefit amount
- [x] `isActiveOn(date)` for benefit period validation

#### Annuities (Completed)
- [x] `Annuity` value object with builder pattern
- [x] `AnnuityType` enum: FIXED_IMMEDIATE, FIXED_DEFERRED, VARIABLE
- [x] Monthly payment calculation with optional COLA
- [x] Start date and optional end date support
- [x] `getMonthlyPayment(date)` for date-specific payment lookup
- [x] `isActiveOn(date)` for payout period validation

#### Other Income Sources (Completed)
- [x] `OtherIncome` value object with builder pattern
- [x] `OtherIncomeType` enum with earned/passive classification:
  - RENTAL (passive) - per IRS IRC Section 469
  - PART_TIME_WORK (earned)
  - ROYALTIES (passive)
  - DIVIDENDS (passive)
  - BUSINESS (earned)
  - OTHER (passive)
- [x] Optional inflation/COLA adjustment
- [x] Start date and optional end date support
- [x] `getMonthlyIncome(date)` with inflation applied
- [x] Earned income flag for SS earnings test integration

#### Income Aggregation Service (Completed)
- [x] `IncomeSources` container for all income sources per person
- [x] `IncomeBreakdown` record for income breakdown by source type
- [x] `IncomeAggregator` interface with `DefaultIncomeAggregator`
- [x] `getMonthlyIncome(sources, date)` aggregating all active sources
- [x] `getAnnualIncome(sources, year)` for yearly totals
- [x] Earned vs passive income classification for SS earnings test
- [x] Working income, Social Security, pensions, annuities, other income
- [x] `combineForCouple(sources1, sources2, date)` for household aggregation

**Total Points**: 34

### Milestone 5: Expense & Budget Modeling ✅
**Goal**: Category-based expense tracking with differentiated inflation
**Status**: Complete (December 2025)
**Issues**: #34-#40, #170-#177 (15 issues, ~55 story points)

#### Core Expense Modeling (#34-#40)

**Expense Categories (#34)**:
- `ExpenseCategory` enum with 19 categories across 6 groups
- `ExpenseCategoryGroup`: ESSENTIAL, HEALTHCARE, DISCRETIONARY, CONTINGENCY, DEBT, OTHER
- Category-specific inflation types: GENERAL, HEALTHCARE, HOUSING, LTC, NONE
- `InflationRates` Spring configuration with externalized rates

**Recurring Expenses (#35)**:
- `RecurringExpense` value object with builder pattern
- `ExpenseFrequency`: MONTHLY, QUARTERLY, SEMI_ANNUAL, ANNUAL
- Inflation-adjusted `getMonthlyAmount(date, inflationRate)`
- Date range filtering with `isActive(date)`

**One-Time Expenses (#36)**:
- `OneTimeExpense` for single-occurrence expenses
- Target date tracking with `getAmountForMonth(date)`
- Inflation adjustment from base date to target

**Expense Lifecycle (#37)**:
- `ExpenseModifier` functional interface with `andThen()` chaining
- `PayoffModifier` - drops expense to zero after payoff (mortgages)
- `SpendingCurveModifier` - Go-Go/Slow-Go/No-Go phases with interpolation
- `AgeBasedModifier` - age-bracket scaling (healthcare costs)

**Budget Model (#38)**:
- `Budget` class aggregating recurring and one-time expenses
- `ExpenseBreakdown` record with category group totals
- Couple budget support (primary + secondary owner)
- Integration with `InflationRates` configuration

**Gap Analysis (#39)**:
- `GapAnalyzer` comparing income vs expenses
- `GapAnalysis` record with surplus/deficit tracking
- Tax-aware gross-up: `grossWithdrawalNeeded(marginalTaxRate)`
- Year projection with 12 monthly analyses

#### Research-Driven Extensions (#170-#177)

**Medicare Calculator (#171)**:
- Part B and Part D premium calculations
- IRMAA brackets (6 tiers) for income-based surcharges
- Single vs MFJ threshold support
- Future year extrapolation with chained CPI

**LTC Insurance (#172)**:
- `LtcInsurance` with benefit pool and daily benefit
- Premium periods and elimination days
- Benefit exhaustion tracking
- Deterministic vs probabilistic trigger modes

**Contingency Reserves (#173)**:
- `ContingencyReserve` for home repairs, vehicle replacement
- `ScheduledExpense` for predictable replacement cycles
- `RandomExpenseEvent` for Monte Carlo simulation
- Target balance with "fully funded" detection

**Expense Allocation Strategy (#174)**:
- Priority-based allocation with shortfall tracking
- Overflow/redirect when reserves filled
- Integration with Budget and GapAnalyzer

**RMD Calculator (#175)**:
- Uniform Lifetime Table calculations
- SECURE 2.0 age thresholds (73→75)
- Prior year balance tracking

**Survivor Expenses (#176)**:
- Category-specific adjustment factors post-death
- Housing 70%, Food 60%, Healthcare 100%, etc.

**Federal Tax Calculator (#177)**:
- 7-bracket system with filing status support
- Chained CPI indexing for inflation
- Standard deduction calculations

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
