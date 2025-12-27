# Milestone 4 Retrospective: Income Modeling

**Sprint Duration:** December 2025
**Status:** Complete
**Date:** December 27, 2025

---

## Summary

Milestone 4 delivered comprehensive income modeling capabilities for retirement simulation. This included working income with COLA, Social Security benefits with early/delayed claiming adjustments, spousal and survivor benefits, earnings test, benefit taxation, pensions with survivor options, annuities, other income sources, and an income aggregation service. The milestone represents the largest feature set delivered to date with 34 story points across 11 issues.

---

## Deliverables

| Issue | Title | PR |
|-------|-------|-----|
| #25 | Implement working income model with COLA | #155 |
| #26 | Implement Social Security benefit model with claiming age adjustments | #158 |
| #27 | Implement Social Security spousal and survivor benefits | #160 |
| #28 | Implement Social Security earnings test and benefit taxation | #162 |
| #29 | Implement pension/defined benefit plan model | #163 |
| #30 | Implement annuity income model | #164 |
| #159 | Tech Debt: Replace null checks with Optional<T> in SocialSecurityRules | #165 |
| #31 | Implement other income sources model | #166 |
| #32 | Create income aggregation service | #167 |
| #33 | Comprehensive test coverage for M4 | #168 |
| #161 | Add M4 documentation to PROJECT_GOALS/README | #169 |

---

## Key Accomplishments

### Working Income (#25)
- `WorkingIncome` value object with salary, COLA rate, start/end dates
- `getAnnualSalary(year)` with COLA compounding from start date
- `getMonthlySalary(date)` for date-specific salary with COLA
- `isActiveOn(date)` for employment period validation
- `colaMonth` field for configurable COLA application timing

### Social Security Benefits (#26)
- `SocialSecurityBenefit` value object with FRA benefit, birth year, claiming age
- `SocialSecurityCalculator` with FRA lookup by birth year (SSA tables 1943-1960+)
- Early claiming reduction: 5/9% per month for first 36 months, 5/12% beyond
- Delayed retirement credits: 8% per year (2/3% per month) up to age 70
- COLA adjustments with configurable rate and compounding
- Parameterized tests covering claiming ages 62-70

### Spousal & Survivor Benefits (#27)
- `SpousalBenefitCalculator` interface with `DefaultSpousalBenefitCalculator`
- Spousal benefits: 50% of higher earner's FRA benefit
- Divorced spouse benefits: requires 10+ year marriage, age 62+, unmarried
- Survivor benefits: 100% of deceased spouse's benefit
- `MarriageInfo` for current marriage with spousal benefits eligibility
- `PastMarriage` for marriage history enabling divorced spouse benefits
- **Critical SSA rule implemented**: Spousal benefits do NOT reduce primary earner's benefit

### Earnings Test & Benefit Taxation (#28)
- `EarningsTestCalculator` - SS benefit reduction when working before FRA
- Below FRA: $1 reduction per $2 earned over limit
- FRA year: $1 reduction per $3 over higher limit
- Year-specific limits from YAML configuration (2024: $22,320/$59,520, 2025: $23,400/$62,160)
- `BenefitTaxationCalculator` - 0%/50%/85% taxation thresholds
- Combined income formula: AGI + non-taxable interest + 50% of SS
- **Important**: Taxation thresholds are NOT indexed (fixed since 1984/1993)

### Pension / Defined Benefit (#29)
- `Pension` value object with name, monthly benefit, start date
- `PensionPaymentForm` enum: SINGLE_LIFE, JOINT_100, JOINT_75, JOINT_50, PERIOD_CERTAIN
- Survivor benefit calculation based on payment form percentage
- Optional COLA with annual compounding
- `getSurvivorBenefit(date)` for survivor benefit amounts

### Annuities (#30)
- `Annuity` value object with builder pattern
- `AnnuityType` enum: FIXED_IMMEDIATE, FIXED_DEFERRED, VARIABLE
- Monthly payment calculation with optional COLA
- Start date and optional end date support
- `ValidationException.validate()` static method with Predicate support

### Other Income Sources (#31)
- `OtherIncome` value object with builder pattern
- `OtherIncomeType` enum with 6 types and earned/passive classification:
  - RENTAL (passive) - per IRS IRC Section 469
  - PART_TIME_WORK (earned)
  - ROYALTIES (passive)
  - DIVIDENDS (passive)
  - BUSINESS (earned)
  - OTHER (passive)
- Earned income flag for SS earnings test integration

### Income Aggregation (#32)
- `IncomeSources` container for all income sources per person
- `IncomeBreakdown` record for income by source type
- `IncomeAggregator` interface with `DefaultIncomeAggregator`
- Earned vs passive income classification
- `combineForCouple()` for household income aggregation

### Technical Debt (#159)
- Updated `SocialSecurityRules.FraEntry` to use `Optional<Integer>` for birth year bounds
- Replaced null checks with `Optional.map().orElse()` pattern
- Cleaner, more idiomatic Java code

---

## Metrics

| Metric | Target | Actual |
|--------|--------|--------|
| Issues Closed | 11 | 11 |
| PRs Merged | 11 | 11 |
| Story Points | 34 | 34 |
| Test Count | 800+ | 850+ |
| Code Coverage | 80% | 80%+ |
| Quality Gates | Pass | All Pass |

---

## What Went Well

### Technical Wins

1. **SSA Research Accuracy** - Researching SSA.gov and IRS Publication 915 before coding ensured accurate benefit calculations. The earnings test limits and taxation thresholds were implemented with proper indexing behavior (earnings test IS indexed, taxation thresholds are NOT).

2. **Marriage History Model** - The `MarriageInfo` and `PastMarriage` design elegantly handles complex spousal benefit scenarios including divorced spouse benefits from multiple ex-spouses. The diagram in the Javadoc makes the relationships clear.

3. **Earned vs Passive Income Classification** - Adding `isEarnedIncome()` to `OtherIncomeType` enables proper SS earnings test integration. Rental income correctly classified as passive per IRS IRC Section 469.

4. **Income Aggregation Architecture** - `IncomeSources` as a standalone container (not embedded in `PersonProfile`) provides flexibility. Scenarios can own income configuration, allowing different income assumptions per scenario.

5. **Parameterized SS Tests** - Adding `@ParameterizedTest` with `@CsvSource` for claiming ages 62-70 provides comprehensive coverage of SSA benefit adjustment formulas.

### Process Wins

1. **Always Pull Develop First** - After experiencing merge conflicts early in the milestone, we established the discipline of always pulling develop before creating feature branches. This prevented conflicts in later issues.

2. **User Domain Expertise** - User correction on rental income classification (passive, not earned) prevented a bug that would have affected SS earnings test calculations.

3. **Comprehensive Documentation** - Creating detailed documentation covering ALL M4 features (not just SS spousal/survivor as originally scoped in #161) provides a complete reference for the milestone.

4. **Incremental PR Strategy** - 11 focused PRs rather than one large PR made code review manageable and allowed quick iteration.

---

## What Didn't Go Well

### Merge Conflict Early On

**Problem:** PR #164 (Annuity) had a merge conflict with develop because the feature branch was created before prior PRs merged.

**Resolution:** Merged develop into the feature branch, kept develop's version of `ValidationException.java` which had better documentation.

**Lesson:** Always `git pull origin develop` before `git checkout -b feature/...`

### Date Confusion

**Problem:** Documentation initially written with "December 2024" instead of "December 2025".

**Resolution:** User caught the error, fixed all 5 occurrences in PROJECT_GOALS.md.

**Lesson:** Pay attention to current date, especially near year boundaries.

### Scope Creep on Documentation Issue

**Problem:** Issue #161 was originally scoped for SS spousal/survivor documentation only, but user correctly noted it should cover ALL M4 features.

**Resolution:** Expanded documentation to comprehensively cover all income modeling features.

**Lesson:** Documentation issues should be scoped to the full milestone, not individual features.

---

## Action Items

### Process Improvements

| Improvement | Description |
|-------------|-------------|
| Pre-branch Pull | Always `git pull origin develop` before creating feature branches |
| Date Awareness | Verify current date in documentation, especially near year boundaries |
| Doc Scope | Milestone documentation issues should cover ALL features in milestone |

### Carried Forward

| Item | Description | Status |
|------|-------------|--------|
| Branch Naming | `feature/<issue>-<description>` convention | Maintained |
| Target Develop | All feature PRs target develop, not main | Maintained |
| Quality Gates | Checkstyle, SpotBugs, PMD, JaCoCo | All Pass |
| Retrospectives | Conduct at end of each milestone | Maintained |

---

## Technical Debt

### Resolved This Milestone
- **Optional<T> in SocialSecurityRules** - Replaced null checks with Optional pattern (#159)

### Deferred
- **SocialSecurityRules Year-Specific Configuration** - Earnings test limits and FRA tables could be moved to YAML configuration for easier updates
- **Income Source Validation** - Could add cross-validation (e.g., SS claiming age must be 62-70)

### Debt to Monitor
- **SSA Limit Updates** - Earnings test limits change annually, need process for updating YAML
- **Taxation Threshold Bracket Creep** - The non-indexed thresholds mean more SS becomes taxable over time; may want to document/warn users

---

## Lessons Learned

1. **IRS/SSA rules are complex** - Earnings test has two different formulas (below FRA vs FRA year). Taxation thresholds have three tiers AND different thresholds by filing status. Research pays off.

2. **Not all limits are indexed** - Earnings test limits ARE indexed annually by SSA. Taxation thresholds are NOT indexed (fixed since 1984/1993). This distinction matters for long-term projections.

3. **Earned vs passive matters** - For SS earnings test, only earned income counts. Getting this classification right (rental = passive per IRC 469) prevents calculation errors.

4. **Marriage history enables optimization** - Supporting divorced spouse benefits from multiple ex-spouses allows users to model complex real-world scenarios and optimize claiming strategies.

5. **Income is per-person, not per-portfolio** - Design decision to keep `IncomeSources` separate from `PersonProfile` provides flexibility for scenario modeling.

---

## Looking Ahead to Milestone 5

### M5: Expense & Budget Modeling

M5 will focus on:
- Expense categories (essential, healthcare, housing, discretionary, debt)
- Category-specific inflation rates (general CPI, healthcare, housing)
- Expense changes over time (mortgage payoff, spending phases)
- One-time expense support
- Budget vs income gap analysis

### Dependencies from M4
- `IncomeAggregator` provides total income for budget gap analysis
- `IncomeBreakdown` shows income sources to compare against expenses
- Filing status affects tax calculations in expense projections

### Process Goals
- Continue parameterized test pattern for expense scenarios
- Create shared test fixtures for expense configuration
- Document inflation rate assumptions from BLS/CPI sources

---

## Files Changed Summary

### New Files (M4)

**Value Objects:**
- `SocialSecurityBenefit.java` - SS benefit with claiming adjustments
- `Pension.java` - Defined benefit pension model
- `Annuity.java` - Annuity income model
- `OtherIncome.java` - Other income sources
- `MarriageInfo.java` - Current marriage information
- `PastMarriage.java` - Marriage history for divorced benefits
- `SpousalBenefitResult.java` - Spousal benefit calculation result
- `SurvivorBenefitResult.java` - Survivor benefit calculation result
- `CoupleClaimingStrategy.java` - Couple SS claiming strategy
- `EarningsTestResult.java` - Earnings test result
- `TaxationResult.java` - Benefit taxation result
- `IncomeSources.java` - Income sources container
- `IncomeBreakdown.java` - Income breakdown by source

**Enums:**
- `AnnuityType.java` - Annuity types (fixed/deferred/variable)
- `OtherIncomeType.java` - Other income types with earned/passive flag
- `PensionPaymentForm.java` - Pension payment forms
- `MaritalStatus.java` - Marital status for spousal benefits
- `MarriageEndReason.java` - Marriage end reasons (divorce/death)

**Calculators:**
- `SpousalBenefitCalculator.java` - Spousal benefit interface
- `DefaultSpousalBenefitCalculator.java` - Spousal benefit implementation
- `EarningsTestCalculator.java` - Earnings test interface
- `DefaultEarningsTestCalculator.java` - Earnings test implementation
- `BenefitTaxationCalculator.java` - Benefit taxation interface
- `DefaultBenefitTaxationCalculator.java` - Benefit taxation implementation
- `IncomeAggregator.java` - Income aggregation interface
- `DefaultIncomeAggregator.java` - Income aggregation implementation

**Configuration:**
- `SocialSecurityRules.java` - Updated with earnings test and taxation rules

### Updated Files
- `WorkingIncome.java` - Enhanced with date ranges and COLA methods
- `SocialSecurityCalculator.java` - Enhanced with claiming adjustments
- `ValidationException.java` - Added `validate()` with Predicate support
- `CalculatorFactory.java` - Added factory methods for new calculators
- `application-social-security.yml` - Added earnings test and taxation config
- `PROJECT_GOALS.md` - M4 completion documentation
- `README.md` - Updated roadmap

---

## Participants

- **Developer:** @xmljim
- **AI Assistant:** Claude (Anthropic)

---

*This retrospective was conducted on December 27, 2025, following the completion of Milestone 4.*
