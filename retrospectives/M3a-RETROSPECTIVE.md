# Milestone 3a Retrospective: Contribution Routing & Tracking

**Sprint Duration:** December 2025
**Status:** Complete
**Date:** December 26, 2025

---

## Summary

Milestone 3a delivered comprehensive contribution routing and year-to-date tracking capabilities for the Retirement Portfolio Simulator. This included contribution routing with overflow handling, IRS limit enforcement for all account types, SECURE 2.0 compliance (super catch-up, Roth requirements), and couple portfolio support.

**Note:** Several M3a features (IRS limits configuration, catch-up rules, SECURE 2.0 Roth requirements) were implemented as part of M1's foundation work. These issues were formally closed in M3a after verification that acceptance criteria were met.

---

## Deliverables

| Issue | Title | PR/Notes |
|-------|-------|----------|
| #17 | Portfolio aggregation across multiple accounts | Completed in M1 (Portfolio class) |
| #18 | Contribution routing logic | #145 |
| #19 | IRS contribution limits configuration model | Completed in M1 (#124) |
| #20 | Age-based catch-up contribution rules | Completed in M1 (#124) |
| #22 | SECURE 2.0 Roth catch-up requirement | Completed in M1 (#124) |
| #23 | YTD contribution tracking and limit enforcement | #146 |
| #24 | Comprehensive test coverage | In develop branch |
| #147 | Documentation updates | In develop branch |

---

## Key Accomplishments

### Contribution Routing
- `ContributionRouter` interface with `DefaultContributionRouter` implementation
- `RoutingConfiguration` and `RoutingRule` for flexible routing rules
- Split contribution support (e.g., 80% Traditional, 20% Roth)
- Overflow handling when primary account hits limit
- Employer match routing (always to Traditional)
- High earner Roth catch-up routing per SECURE 2.0
- `ContributionAllocation` for tracking routed amounts

### YTD Tracking & Limit Enforcement
- `YTDContributionTracker` interface with immutable `DefaultYTDContributionTracker`
- `ContributionRecord` for tracking individual contributions
- `LimitCategory` enum (EMPLOYER_401K, IRA, HSA) for limit grouping
- `ContributionLimitChecker` with `LimitCheckResult` for pre-contribution validation
- Combined IRA limit enforcement (Traditional + Roth share limit)
- HSA family vs individual limit based on spouse linkage
- `YTDSummary` for contribution status reporting

### Couple Portfolio Support
- `CouplePortfolioView` for aggregate view across spouse portfolios
- Combined and individual balance views
- Per-spouse YTD tracking and summaries
- HSA family coverage inference from spouse linkage

### IRS Rules & SECURE 2.0 (from M1)
- `IrsContributionLimits` with YAML configuration for 2024-2026
- `Secure2ContributionRules` implementation
- Age-based catch-up (50+) and super catch-up (60-63, effective 2025)
- Roth-only catch-up for high earners ($145K+)
- HSA contribution limits with age 55+ catch-up
- IRS-style COLA rounding for future year extrapolation

### Test Coverage
- `CouplePortfolioViewTest` - 9 tests for couple portfolio scenarios
- `ContributionIntegrationTest` - 7 tests for full-year and multi-account scenarios
- Birthday edge case tests for catch-up eligibility transitions
- Comprehensive routing and limit checker tests

---

## Metrics

| Metric | Target | Actual |
|--------|--------|--------|
| Issues Closed | 8 | 8 |
| PRs Merged | 2+ | 2 (+ M1 foundation) |
| Code Coverage | 80% | 80%+ |
| Quality Gates | Pass | All Pass |

---

## What Went Well

### Technical Wins

1. **M1's Foundation Paid Off Again** - IRS limits, catch-up rules, and SECURE 2.0 compliance were already in place from M1. M3a was about building on top, not starting from scratch.

2. **Immutable Tracker Pattern** - `YTDContributionTracker.recordContribution()` returns a new instance. This functional approach simplifies reasoning about state and will benefit simulation engine in M7.

3. **HSA Coverage Inference** - Using `PersonProfile.hasSpouse()` to infer family vs individual HSA limits was elegant. Same pattern will work for Social Security spousal benefits in M4.

4. **Strategy Pattern Continues** - `MatchingPolicy` (M1), `CompoundingFunction` (M2), now `RoutingConfiguration` (M3a). Consistent patterns across milestones.

5. **Clean Limit Category Abstraction** - `LimitCategory` enum cleanly maps account types to IRS limit buckets. Handles combined IRA limit naturally.

### Process Wins

1. **M3 Split Was Right Call** - M2 retrospective recommended splitting M3. Phase-out rules (M3b) would have made this milestone too large.

2. **Formal Issue Closure** - Verified M1-completed work against M3a acceptance criteria before closing issues. No ambiguity about completion status.

3. **Test-First Integration Tests** - `ContributionIntegrationTest` validates full scenarios (12-month contributions, couple with different ages) early.

---

## What Didn't Go Well

### Scope Confusion

**M3a issues completed in M1**: Issues #17, #19, #20, #22 were created for M3a but substantially completed during M1 foundation work. This created confusion about what was "new" in M3a.

**Root Cause:** M1 scope included "IRS Contribution Rules - SECURE 2.0" but individual M3a issues duplicated this work.

**Lesson:** When creating milestone issues, verify they don't overlap with work already planned in prior milestones. Better to have issues that build on existing work than duplicate it.

### Test Duplication Pattern

**CPD Warnings**: `createTestLimits()` helper method duplicated across test files:
- `DefaultYTDContributionTrackerTest`
- `DefaultContributionLimitCheckerTest`
- `DefaultContributionRouterTest`
- `ContributionIntegrationTest`

Used `// CPD-OFF` comments to suppress, but this is a code smell.

**Potential Solution:** Create `TestIrsLimits` utility class or test fixtures. Deferred as low priority.

---

## Action Items

### Process Improvements

| Improvement | Description |
|-------------|-------------|
| Issue Scoping | Verify new issues don't overlap with prior milestone deliverables |
| Test Fixtures | Consider shared test utilities for repeated setup (future milestone) |
| Early Verification | Close issues promptly when acceptance criteria met, even if from prior work |

### Carried Forward

| Item | Description | Status |
|------|-------------|--------|
| Branch Creation | Use `gh issue develop` for automatic issue linking | Maintained |
| Milestone Cleanup | Documentation and coverage issues at milestone end | Maintained |
| Quality Gates | Checkstyle, SpotBugs, PMD, JaCoCo all pass | Maintained |

---

## Technical Debt

### Accepted Debt
- **Test Duplication** - `createTestLimits()` duplicated across test files; suppressed with CPD-OFF
- **Legacy Code** - Original contribution types still deprecated from M1

### Debt to Monitor
- **YTD Tracker Memory** - For multi-year simulations, tracker holds all records; may need summarization
- **Limit Checker Parameters** - `check()` method has many parameters; could use a request object

---

## Lessons Learned

1. **Foundation work multiplies** - M1's comprehensive IRS rules setup meant M3a routing "just worked" with existing limits
2. **Immutability is worth the overhead** - Returning new instances from state-changing methods simplifies testing and reasoning
3. **Verify issue scope early** - Several M3a issues were already done; discovered during implementation
4. **Integration tests catch design issues** - Full-year simulation test revealed subtle interactions
5. **HSA spouse inference was a design win** - Simple pattern (hasSpouse → family limit) will reuse for Social Security

---

## Looking Ahead to Milestone 3b / M4

### M3b: Income-Based Phase-Outs

M3b remains as originally scoped:
- Filing status support (Single, MFJ, MFS, HOH, QW)
- MAGI calculator for modified adjusted gross income
- Traditional IRA deductibility phase-out
- Roth IRA contribution eligibility phase-out
- Backdoor Roth awareness flagging

**Risk:** Phase-out calculations are intricate with many edge cases. May need to timebox or split further.

### Alternative: Skip to M4 (Income Modeling)

M3b phase-outs primarily affect IRA contributions. If M4's Social Security modeling is higher priority, could defer M3b. The simulation would work without phase-outs (just wouldn't reduce IRA contributions for high earners).

**Recommendation:** Review priorities with stakeholder before starting M3b.

### Process Goals
- Continue `gh issue develop` workflow
- Create test fixtures if pattern continues
- Verify issue scope against prior milestones before starting

---

## Participants

- **Developer:** @xmljim
- **AI Assistant:** Claude (Anthropic)

---

*This retrospective was conducted on December 26, 2025, following the completion of Milestone 3a.*
