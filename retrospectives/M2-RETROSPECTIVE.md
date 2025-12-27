# Milestone 2 Retrospective: Core Transaction & Account Operations

**Sprint Duration:** December 2025
**Status:** Complete
**Date:** December 26, 2025

---

## Summary

Milestone 2 delivered core transaction processing capabilities for the Retirement Portfolio Simulator, including a complete transaction model with balance tracking, transaction history, withdrawal calculations, and flexible compounding strategies using the Strategy pattern.

---

## Deliverables

| Issue | Title | PR |
|-------|-------|-----|
| #12 | Contribution Amount Calculations | #135 |
| #13 | Withdrawal Amount Calculations | #136 |
| #14 | Transaction Redesign | #137 |
| #15 | Transaction History | #138 |
| #133 | CompoundingFunction Interface | #139 |
| #16 | Test Coverage & Documentation | #140 |

---

## Key Accomplishments

### Transaction Model
- `Transaction` class with complete balance tracking (start balance → end balance)
- Transaction chaining via `previous` reference for balance continuity
- Builder pattern with fluent API for transaction construction
- New `TransactionType` enum (CONTRIBUTION, WITHDRAWAL, RETURN, FEE, TRANSFER)

### Transaction History
- `TransactionHistory` class for account-level transaction tracking
- Immutable design with `append()` returning new instance
- Query methods: `getByType()`, `getByDateRange()`, `getLatest()`
- Aggregation: `getTotalContributions()`, `getTotalWithdrawals()`, `getNetChange()`
- Balance tracking: `getCurrentBalance()` from latest transaction

### Withdrawal Calculations
- `WithdrawalResult` record capturing withdrawal amount, account details, and remaining balance
- `WithdrawalCalculator` interface for withdrawal strategy abstraction
- `DefaultWithdrawalCalculator` implementation with basic withdrawal logic

### Investment Returns & Compounding
- `CompoundingFunction` functional interface (Strategy pattern)
- `CompoundingFunctions` utility class with standard implementations:
  - `ANNUAL` - True annual compounding: `principal * (1 + rate)^(months/12)`
  - `MONTHLY` - Discrete monthly: `principal * (1 + rate/12)^months`
  - `DAILY` - Daily compounding: `principal * (1 + rate/365)^days`
  - `CONTINUOUS` - Continuous: `principal * e^(rate * years)`
- Updated `ReturnCalculator` with pluggable compounding support
- `MathUtils` made public for BigDecimal power calculations

### Documentation
- Updated README.md with M2 completion status
- Updated PROJECT_GOALS.md with detailed M2 implementation notes
- Updated "Current State" section with all M2 components

---

## Metrics

| Metric | Target | Actual |
|--------|--------|--------|
| Issues Closed | 6 | 6 |
| PRs Merged | 6 | 6 |
| Code Coverage | 80% | 80%+ |
| Quality Gates | Pass | All Pass |

---

## What Went Well

### Technical Wins

1. **M1's Foundation Paid Off** - The domain model, calculator interfaces, and exception hierarchy from M1 made M2 implementation smooth. We weren't fighting the architecture.

2. **Strategy Pattern Continues to Prove Value** - Used for `MatchingPolicy` (M1), now `CompoundingFunction` (M2). This pattern will likely continue into M3 (contribution routing) and M6 (distribution strategies).

3. **Immutability Discipline** - `TransactionHistory.append()` returning a new instance rather than mutating. This will matter when we hit M7's simulation engine where thread-safety could become relevant.

4. **Deprecation Strategy Working** - Legacy code coexists without blocking progress. Migration can happen gradually.

5. **Clean Milestone Execution** - No major blockers, good velocity, solid test coverage throughout.

### Process Wins

1. **Discovered `gh issue develop`** - Creates branches properly linked to issues in the Development section
2. **Project Board Automation** - GraphQL mutations for moving issues between statuses
3. **Milestone Cleanup Issue** - Dedicated issue (#16) for documentation and coverage gaps at milestone end

---

## What Didn't Go Well

### Requirements Gap Identified

**CompoundingFunction (#133)** was added mid-milestone. Initially characterized as "scope creep," but more accurately a **missed requirement**:

- Original M2 scope said: "Calculate investment returns (monthly compounding)"
- Reality: Different financial products compound differently (savings = monthly, investments = annual, credit cards = daily)
- The original requirement was underspecified - the domain requires multiple compounding methods
- Addressed within M2 rather than deferring technical debt

**Lesson:** Requirements for financial calculations need more scrutiny upfront. "Monthly compounding" was too narrow an assumption.

### Process Friction

1. **GitHub PR-Issue Linking** - Initially used "Closes #X" which creates cross-references but not Development section links when merging to `develop`. Resolved by learning `gh issue develop` workflow.

2. **Codecov Drift** - Some coverage gaps accumulated during rapid development. Addressed in cleanup issue (#16).

**Assessment:** These are tooling/observability issues, not code quality issues. GitHub's interfaces aren't the most intuitive, but we adapted. Coverage drift is acceptable during sprints as long as we have cleanup issues at milestone end.

---

## Action Items

### Process Improvements Adopted

| Improvement | Description |
|-------------|-------------|
| Branch Creation | Use `gh issue develop <number>` for automatic issue linking |
| Issue Status | Move to "In Progress" when starting, "Done" after merge |
| Milestone Cleanup | Create dedicated issue for documentation and coverage at milestone end |
| Requirements Review | Scrutinize financial calculation requirements for hidden complexity |

### Carried Forward

| Item | Description | Status |
|------|-------------|--------|
| PR Target Branch | Always PR to `develop` unless release/hotfix | Maintained |
| Issue/PR Metadata | Fill out Project + Milestone on every Issue and PR | Maintained |
| Test Before Commit | Run tests before every commit | Maintained |

---

## Technical Debt

### Accepted Debt
- **Legacy Code** - `TransactionType` in `domain.model` deprecated, new version in `domain.enums`
- **Coverage Gaps** - Addressed in #16, but some edge cases may surface later

### Debt to Monitor
- **Compounding Precision** - Using `Math.pow()` and `Math.exp()` for fractional exponents; sufficient for financial calculations but worth monitoring
- **Transaction History Memory** - For 30+ year simulations, history could grow large; may need pagination or summarization in M7

---

## Lessons Learned

1. **Financial domain requirements need deeper analysis** - "Monthly compounding" seemed simple but masked real complexity
2. **Tooling friction is solvable** - GitHub linking issues were annoying but not blocking; we found `gh issue develop`
3. **Milestone cleanup issues are valuable** - Dedicated time to address drift prevents debt accumulation
4. **Strategy pattern is a workhorse** - Third use case (MatchingPolicy, CompoundingFunction, more to come)
5. **Immutability pays forward** - Building immutable structures now prevents threading issues later

---

## Looking Ahead to Milestone 3

### Scope Concern

M3's current scope is dense:
- Multi-account portfolio aggregation
- Contribution routing to specific accounts
- IRS contribution limits enforcement
- Income-based phase-outs (IRA deductibility, Roth eligibility)
- Year-to-date contribution tracking

**Recommendation:** Review M3 issue breakdown before starting. Income phase-out rules (MAGI calculations, married filing separately, Roth backdoor) could be their own milestone or deferred.

### Risks to Watch
- Phase-out rules are intricate with many edge cases
- Multiple account types interacting with contribution limits
- Need clear separation between limit calculation and limit enforcement

### Process Goals
- Continue using `gh issue develop` for branch creation
- Maintain project board discipline
- Break large stories proactively if scope exceeds 1-2 days

---

## Participants

- **Developer:** @xmljim
- **AI Assistant:** Claude (Anthropic)

---

*This retrospective was conducted on December 26, 2025, following the completion of Milestone 2.*
