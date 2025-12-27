# Milestone 3b Retrospective: Income-Based Phase-Outs

**Sprint Duration:** December 2025
**Status:** Complete
**Date:** December 26, 2025

---

## Summary

Milestone 3b delivered comprehensive income-based phase-out calculations for IRA contributions. This included filing status support, MAGI (Modified Adjusted Gross Income) calculation, Traditional IRA deductibility phase-outs, Roth IRA contribution eligibility phase-outs, and backdoor Roth awareness flagging. The milestone also addressed technical debt carried forward from M3a.

---

## Deliverables

| Issue | Title | PR |
|-------|-------|-----|
| #142 | FilingStatus enum and phase-out configuration | #150 |
| #141 | MAGI calculator | #151 |
| #21 | Phase-out rules for IRA contributions | #152 |
| #143 | Comprehensive test coverage for M3b | #153 |
| #149 | Technical debt cleanup | #154 |

---

## Key Accomplishments

### Filing Status & Configuration
- `FilingStatus` enum with 5 statuses (Single, MFJ, MFS, HOH, QSS)
- Helper methods: `usesSingleThresholds()`, `usesJointThresholds()`, `hasSpecialRestrictions()`
- `IraPhaseOutLimits` configuration with YAML-driven thresholds for 2024-2025
- `PhaseOutRange` and `YearPhaseOuts` records for structured configuration
- Extrapolation support for future years with IRS-style $1,000 rounding

### MAGI Calculator
- `MAGICalculator` interface with `DefaultMAGICalculator` implementation
- `IncomeDetails` value object capturing AGI plus 6 add-back items:
  - Student loan interest deduction
  - Tuition and fees deduction
  - Foreign earned income exclusion
  - Foreign housing exclusion
  - Savings bond interest exclusion
  - Adoption benefits exclusion
- Simple calculation: MAGI = AGI + total add-backs

### Phase-Out Calculator
- `PhaseOutCalculator` interface for Roth and Traditional IRA phase-outs
- `DefaultPhaseOutCalculator` with linear interpolation
- `PhaseOutResult` record with builder pattern
- Key features:
  - Roth IRA contribution phase-out with backdoor Roth awareness
  - Traditional IRA deductibility phase-out:
    - Covered by employer plan
    - Spouse covered by employer plan
    - Not covered (full deduction at any income)
  - IRS rounding rules (up to nearest $10, $200 minimum)
  - Warning messages for special cases (MFS restrictions, backdoor Roth)

### Test Coverage
- `PhaseOutCalculatorTest` - 13 unit tests
- `PhaseOutIntegrationTest` - 12 end-to-end tests
- `MAGICalculatorTest` - 15 tests
- `IncomeDetailsTest` - 9 tests
- `FilingStatusTest` - 6 tests
- `IraPhaseOutLimitsTest` - 8 tests

### Technical Debt Resolution
- Extracted `TestIrsLimitsFixture` shared test utility
- Extracted `TestPhaseOutFixture` shared test utility
- Removed all CPD-OFF suppressions from test files
- Created `DEPRECATED_CODE_MIGRATION.md` documenting M1 deprecated types

---

## Metrics

| Metric | Target | Actual |
|--------|--------|--------|
| Issues Closed | 5 | 5 |
| PRs Merged | 5 | 5 |
| Test Count | 700+ | 721 |
| Code Coverage | 80% | 80%+ |
| Quality Gates | Pass | All Pass |

---

## What Went Well

### Technical Wins

1. **IRS Research Upfront** - Researching phase-out thresholds from official IRS sources (Publication 590-A) before coding ensured accuracy. Configuration-driven approach means easy updates for future years.

2. **Configuration Pattern Consistency** - `IraPhaseOutLimits` follows same pattern as `IrsContributionLimits` (Spring `@ConfigurationProperties`, YAML config, extrapolation). Developers familiar with one will understand the other.

3. **Builder Pattern for Results** - `PhaseOutResult.builder()` makes test setup clean and allows incremental result construction with warnings.

4. **Backdoor Roth Awareness** - Rather than just saying "no contribution allowed," the calculator flags backdoor Roth eligibility with helpful warning messages. User-friendly design.

5. **Technical Debt Payoff** - Extracting `TestIrsLimitsFixture` and `TestPhaseOutFixture` eliminated duplicate code. CPD-OFF suppressions removed. Cleaner codebase going forward.

### Process Wins

1. **Always Target Develop** - Caught targeting `main` instead of `develop` on PR #152. Lesson reinforced - always use `--base develop` for feature PRs.

2. **Feature Branch Creation** - `gh issue develop <number> --checkout` creates properly linked branches. Needed to rebase when branches were created before prior PRs merged.

3. **Comprehensive Issue Closure** - Each PR explicitly `Closes #N` to auto-link and close issues.

4. **Retrospective Planning** - User reminder about retrospective at milestone end ensured we didn't skip this important step.

---

## What Didn't Go Well

### Branch Timing Issues

**Problem:** Feature branches created with `gh issue develop` before prior PRs merged, resulting in merge conflicts when those PRs landed.

**Example:** Branch for #141 (MAGI) was created before PR #150 (FilingStatus) merged. Had to rebase to pick up the merged code.

**Solution:** Wait for dependent PR to merge before creating the next feature branch, OR immediately rebase after checking out the branch.

### Nested Class Compilation Mystery

**Problem:** Integration test with 4 nested classes had unexplained compilation errors where imports weren't visible in the last 2 nested classes. Simplified to flat test class to resolve.

**Root Cause:** Unknown - possibly encoding issue or Maven incremental compilation. Clean rebuild with simplified structure resolved it.

**Lesson:** When facing mysterious compilation errors, simplify structure rather than debugging infinitely.

### CPD Cascading

**Problem:** Fixing CPD issue in `PhaseOutCalculatorTest` revealed another in `CouplePortfolioViewTest`. Had to chase down all instances of the same pattern.

**Solution:** `TestIrsLimitsFixture` now provides single source of truth for test IRS limits.

---

## Action Items

### Process Improvements

| Improvement | Description |
|-------------|-------------|
| Branch Timing | Rebase feature branches immediately after checkout if created from outdated develop |
| Test Structure | Prefer flat test classes over deeply nested for compilation reliability |
| Fixture Extraction | Create shared fixtures proactively when pattern emerges |

### Carried Forward

| Item | Description | Status |
|------|-------------|--------|
| `gh issue develop` | Continue using for automatic issue linking | Maintained |
| Target `develop` | All feature PRs target develop, not main | Reinforced |
| Retrospectives | Conduct at end of each milestone | Maintained |
| Quality Gates | Checkstyle, SpotBugs, PMD, JaCoCo | All Pass |

---

## Technical Debt

### Resolved This Milestone
- **Test Duplication** - Extracted `TestIrsLimitsFixture` and `TestPhaseOutFixture`
- **CPD-OFF Suppressions** - All removed
- **Legacy Code Documentation** - Created `DEPRECATED_CODE_MIGRATION.md`

### Deferred
- **LimitCheckRequest Record** - 8-parameter `check()` method could use request object. Deferred as functional.
- **Deprecated Code Removal** - Tracked for M10 (API layer)

### Debt to Monitor
- **Phase-Out Threshold Updates** - IRS updates thresholds annually. Need process for updating configuration.
- **Future Year Extrapolation** - Current 2% default may drift from actual IRS adjustments over time.

---

## Lessons Learned

1. **Research before code** - IRS phase-out rules are complex. Having official sources (Pub 590-A) prevented mistakes.

2. **Configuration-driven design scales** - Both `IrsContributionLimits` and `IraPhaseOutLimits` use same YAML pattern. Adding new years is just config, not code.

3. **Warnings are documentation** - `PhaseOutResult.warnings()` provides context-aware guidance. Better UX than opaque "not allowed."

4. **Test fixtures pay dividends immediately** - Extracting fixtures solved current CPD issues AND prevents future ones.

5. **Branch hygiene matters** - Rebase early, rebase often. Don't let branches drift from develop.

---

## Looking Ahead to Milestone 4

### M4: Income Modeling & Social Security

M4 will focus on:
- Social Security benefits calculation
- Multiple income sources (salary, pension, rental, dividends)
- Income projection over retirement timeline
- Integration with phase-out calculations from M3b

### Dependencies from M3b
- `FilingStatus` will be used for tax calculations
- `MAGICalculator` feeds Social Security taxation calculations
- Phase-out configuration pattern can inform benefit phase-out configuration

### Process Goals
- Continue test fixture pattern (`TestIncomeSources`?)
- Create issues that don't overlap with prior work
- Rebase feature branches immediately after checkout

---

## Files Changed Summary

### New Files (M3b)
- `FilingStatus.java` - Tax filing status enum
- `IraPhaseOutLimits.java` - Phase-out threshold configuration
- `IncomeDetails.java` - MAGI add-back components
- `MAGICalculator.java` - MAGI calculation interface
- `DefaultMAGICalculator.java` - MAGI implementation
- `PhaseOutCalculator.java` - Phase-out calculation interface
- `PhaseOutResult.java` - Phase-out result record
- `DefaultPhaseOutCalculator.java` - Phase-out implementation
- `TestPhaseOutFixture.java` - Shared test fixture
- `TestIrsLimitsFixture.java` - Shared test fixture
- `DEPRECATED_CODE_MIGRATION.md` - Migration documentation

### Updated Files
- `application.yml` - Added phase-out configuration
- `CalculatorFactory.java` - Added MAGI and phase-out factory methods
- Multiple test files - Use shared fixtures

---

## Participants

- **Developer:** @xmljim
- **AI Assistant:** Claude (Anthropic)

---

*This retrospective was conducted on December 26, 2025, following the completion of Milestone 3b.*
