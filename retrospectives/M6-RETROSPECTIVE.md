# Milestone 6 Retrospective: Distribution Strategies

**Sprint Duration:** December 2025
**Status:** Complete
**Date:** December 30, 2025

---

## Summary

Milestone 6 delivered comprehensive distribution strategy capabilities for retirement simulation. The milestone implemented three spending strategies (Static 4%, Income Gap, Guardrails), two account sequencers (Tax-Efficient, RMD-First), and RMD-aware orchestration. The bucket strategy (M6c) was deferred after analysis determined it's an asset allocation model rather than a spending strategy. The milestone completed with ~45 story points across 22 issues (excluding deferred M6c).

---

## Deliverables

### Research & Planning
| Issue | Title | PR |
|-------|-------|-----|
| #220 | Research: Guardrails withdrawal strategies | N/A |
| #221 | Research: Bucket strategy implementation patterns | N/A |
| #222 | Research: Tax-efficient withdrawal sequencing | N/A |

### M6a: Strategy Framework
| Issue | Title | PR |
|-------|-------|-----|
| #223 | Create SpendingStrategy interface and SpendingPlan record | #246 |
| #224 | Create SpendingContext record | #246, #248, #256 |
| #225 | Create AccountSequencer interface and AccountWithdrawal record | #246 |
| #226 | Implement TaxEfficientSequencer | #246 |
| #227 | Implement RmdFirstSequencer | #246 |
| #228 | Create SpendingOrchestrator interface | #246 |
| #249 | Create SimulationView interface | #254 |
| #250 | Create StubSimulationView for testing | #255 |
| #251 | Refactor SpendingContext to use SimulationView | #256 |
| #260 | Remove Portfolio parameter from Orchestrator | #261 |

### M6b: Static & Income-Gap Strategies
| Issue | Title | PR |
|-------|-------|-----|
| #229 | Implement StaticSpendingStrategy | #257 |
| #230 | Implement IncomeGapStrategy | #258 |
| #231 | Comprehensive strategy integration tests | #259 |

### M6c: Bucket Strategy (Deferred)
| Issue | Title | Status |
|-------|-------|--------|
| #232-236 | Bucket strategy implementation | Deferred |

### M6d: Guardrails Strategy
| Issue | Title | PR |
|-------|-------|-----|
| #237 | Create GuardrailsConfiguration with preset factories | #263 |
| #238 | Implement GuardrailsSpendingStrategy | #263 |
| #239 | Implement Guyton-Klinger 4-rule logic | #263 |
| #240 | Implement Kitces ratchet-only logic | #263 |
| #241 | Guardrails tests for all presets | #263 |

### M6e: RMD Integration
| Issue | Title | PR |
|-------|-------|-----|
| #242 | Implement RmdAwareOrchestrator | #264 |
| #243 | End-to-end integration tests | #265 |
| #244 | M6 documentation and usage examples | #266 |

---

## Key Accomplishments

### Strategy Framework (M6a)
- `SpendingStrategy` interface with Strategy pattern
- `SpendingContext` record with `SimulationView` reference
- `SpendingPlan` result record with account-level details and metadata
- `AccountWithdrawal` record for per-account withdrawal tracking
- `AccountSequencer` interface for withdrawal ordering
- `TaxEfficientSequencer` (Taxable → Traditional → Roth)
- `RmdFirstSequencer` (RMD accounts first, then tax-efficient)
- `SpendingOrchestrator` interface and `DefaultSpendingOrchestrator`
- `SimulationView` interface for read-only simulation state access
- `AccountSnapshot` record for immutable account state
- `StubSimulationView` for testing without M7

### Static & Income-Gap Strategies (M6b)
- `StaticSpendingStrategy` - 4% rule with configurable rate and inflation
- `IncomeGapStrategy` - Withdraw gap with optional tax gross-up
- Integration tests with orchestrator

### Guardrails Strategy (M6d)
- `GuardrailsConfiguration` record with builder pattern
- Three preset factories: `guytonKlinger()`, `vanguardDynamic()`, `kitcesRatcheting()`
- `GuardrailsSpendingStrategy` with dynamic adjustments
- Inflation skipping on down years (Guyton-Klinger Rule 2)
- Capital preservation and prosperity rule triggers
- Absolute floor/ceiling constraints
- Minimum years between ratchets (Kitces)

### RMD Integration (M6e)
- `RmdAwareOrchestrator` ensuring RMD compliance (prevents 25% IRS penalty)
- RMD-first withdrawal sequencing with forced withdrawals
- Tracking of RMD vs discretionary withdrawals in metadata
- `RmdRulesTestLoader` utility to load test config from YAML
- End-to-end integration tests covering all strategies

---

## Metrics

| Metric | Target | Actual |
|--------|--------|--------|
| Issues Closed | 22 (M6c deferred) | 17 active + 5 deferred |
| PRs Merged | ~15 | 16 |
| Story Points | ~60 | ~45 (excluding M6c) |
| Test Count | 900+ | 950+ |
| Code Coverage | 80% | 80%+ |
| Quality Gates | Pass | All Pass |

---

## What Went Well

### Technical Wins

1. **Architecture Revision Mid-Milestone** - Early design had strategies directly managing state. Discussion revealed the need for clean separation: simulation engine owns state, strategies are pure calculators. The `SimulationView` interface solved this elegantly.

2. **Research-First Approach** - Three research issues (#220-222) investigated guardrails, bucket strategies, and tax sequencing BEFORE implementation. This prevented wasted effort on M6c when analysis showed bucket strategy is allocation, not spending.

3. **Test Configuration from YAML** - User feedback caught hardcoded life table values in `StubRmdCalculator`. Created `RmdRulesTestLoader` to load from `application-rmd.yml`, ensuring test data matches production configuration.

4. **Integration Test Consolidation** - Moved 5 scattered integration tests into dedicated `domain.integration` package with `package-info.java` documenting the distinction between unit and integration tests.

5. **PR Review Effectiveness** - Code reviews caught:
   - Missing `name` field in `GuardrailsConfiguration`
   - Package location issue (`GuardrailsConfiguration` moved to `domain.config`)
   - Hardcoded values that should come from YAML
   - Accidentally committed IDE settings

### Process Wins

1. **Incremental PRs** - 16 focused PRs rather than large batches made review manageable

2. **Deferral Decision** - M6c (Bucket Strategy) was deferred rather than forced through. This showed maturity in scope management.

3. **Documentation Improved** - Updated PROJECT_GOALS.md, README.md, M6 design doc, and package-info files as part of the milestone (not deferred to later)

---

## What Didn't Go Well

### Insufficient Upfront Architecture Planning

**Problem:** M6 was built "bottom-up" without a clear "top-down" view of how M7 (Simulation Engine) would consume it. The `SimulationView` interface emerged *reactively* when we discovered strategies couldn't own state - it should have been designed *proactively* before implementation began.

**Impact:** Required mid-milestone architectural revision. `SpendingContext` was refactored twice (once to add fields, once to remove them and use `SimulationView`). The Portfolio parameter was added to orchestrator, then removed.

**Root Cause:** M6 was treated as an isolated feature rather than a component in a larger system. Even a rough sketch of "simulation loop calls strategy → strategy returns plan → simulation executes plan" would have clarified the interface contract upfront.

**Lesson:** For orchestration milestones, sketch the integration architecture BEFORE implementation. Ask: "Who owns the state? Who mutates it? What's read-only?"

### Conceptual Confusion: Strategy vs Allocation vs Expense Models

**Problem:** Retirement planning literature blurs lines between spending strategies, allocation strategies, and expense models. We didn't clearly distinguish these upfront:

| Concern | Question | Milestone |
|---------|----------|-----------|
| **Expense/Budget** | "How much do I need?" | M5 |
| **Spending Strategy** | "How much should I withdraw?" | M6 |
| **Allocation** | "Where should my money be positioned?" | NOT M6 |

**Impact:**
- Wasted design effort on M6c (Bucket Strategy) before recognizing it's an allocation model, not a spending strategy
- Confusion about where spending curve (Go-Go/Slow-Go/No-Go) belongs - it's an expense modifier (#44), not a distribution strategy
- Churn in issue scoping and PR focus

**Lesson:** For future milestones, explicitly define what's IN scope and OUT of scope based on the *type of concern*, not just feature names.

### Context Window Exhaustion

**Problem:** Multiple sessions hit context limits requiring conversation continuation.

**Impact:** Had to summarize and restart conversations, potentially losing context nuance.

**Lesson:** Break complex milestones into smaller chunks. Use task handoff documents earlier.

### Output Token Limits

**Problem:** Encountered "API Error: Claude's response exceeded the 4096 output token maximum" multiple times during E2E test creation.

**Impact:** Had to break responses into smaller pieces, slowing down delivery.

**Lesson:** When creating large files, write incrementally rather than attempting complete files at once.

### Deferred Items Without Clear Tracking

**Problem:** M6c (Bucket Strategy) deferred, but issues #232-236 left in limbo.

**Impact:** Unclear whether these will be revisited or closed.

**Recommendation:** Close deferred issues with "won't fix" or move to "Deferred" milestone.

### Old Milestone Stragglers

**Problem:** Found orphaned issues (#41-48) in old "Milestone 6" that predated the current M6.

**Impact:** Confusion about what was actually in scope.

**Lesson:** Clean up old milestones before starting new ones with similar names.

---

## Technical Debt

### Resolved This Milestone
- Integration tests consolidated into `domain.integration` package
- Test configuration loaded from YAML rather than hardcoded

### Identified This Milestone

| Item | Description | Priority |
|------|-------------|----------|
| Constants Duplication | `SCALE`, `TWELVE`, `ROUNDING_MODE` duplicated across strategy classes | Medium |
| SpendingPhase Integration | #44 (Go-Go/Slow-Go/No-Go) moved to M7 - expense modifier exists but not integrated with strategies | Medium |
| Inflation Parameter | Inflation rate passed via `strategyParams` map; consider dedicated field | Low |
| RMD Excess Handling | When RMD exceeds spending need, excess handling undefined | Low |

### Deferred from M6
- **Bucket Strategy (M6c)** - Issues #232-236 deferred. Determined to be asset allocation model, not spending strategy. If revisited, should be separate feature outside distribution strategies.

---

## Lessons Learned

1. **Interface Design First** - The `SimulationView` interface was the key architectural insight. Strategies need read-only access to simulation state; simulation engine owns mutable state.

2. **Preset Factories Aid Adoption** - `GuardrailsConfiguration.guytonKlinger()`, `vanguardDynamic()`, `kitcesRatcheting()` make complex configuration accessible.

3. **RMD is Non-Negotiable** - RMDs override strategy calculations. The `RmdAwareOrchestrator` ensures compliance regardless of what strategy requests.

4. **Single Source of Truth** - Test data should come from the same configuration files as production. `RmdRulesTestLoader` exemplifies this.

5. **Deferred != Failed** - Not implementing bucket strategy was the RIGHT decision. It would have added complexity without proportional benefit.

---

## Why M6 Was the Hardest Milestone

M6 represented a fundamental shift in complexity from previous milestones:

| Milestones 1-5 | Milestone 6 |
|----------------|-------------|
| Data models and calculations | Orchestration and coordination |
| Clear inputs → outputs | Stateful interactions over time |
| Self-contained components | Components that integrate with future M7 |
| "What is this thing?" | "How do things work together?" |

**M1-M5** were largely about defining *what* things are: accounts, contributions, income sources, expenses. They have clear boundaries - a `SocialSecurityBenefit` doesn't need to know about `Budget`.

**M6** introduced *how* things interact: strategies query state, sequencers order accounts, orchestrators coordinate execution, and all of it must integrate with a simulation engine that doesn't exist yet. This is **orchestration complexity** - a fundamentally different kind of problem.

The lesson for M7 and beyond: when a milestone involves *coordinating* other milestones' outputs, invest heavily in integration architecture upfront. The cost of mid-stream architectural changes is high.

---

## Looking Ahead to Milestone 7

### M7: Simulation Engine

M7 will focus on:
- `SimulationEngine` implementing `SimulationView` interface
- Monthly simulation loop (8-step process)
- Transaction generation per account per month
- Phase handling (accumulation → distribution transition)
- Event triggers (retirement, SS start, RMD age)
- `TimeSeries<MonthlySnapshot>` for historical tracking

### Dependencies from M6
- `SpendingStrategy` implementations provide withdrawal calculations
- `SpendingOrchestrator` coordinates withdrawal execution
- `AccountSequencer` implementations determine withdrawal order
- `SimulationView` interface must be implemented by simulation engine

### Process Goals
- Complete M7 planning before implementation
- Create smaller, focused PRs
- Document incrementally with each feature
- Resolve M6c deferred issues (close or rescope)

---

## Files Changed Summary

### New Files (M6)

**Interfaces:**
- `SpendingStrategy.java` - Strategy interface
- `SpendingOrchestrator.java` - Orchestrator interface
- `AccountSequencer.java` - Sequencer interface
- `SimulationView.java` - Simulation state access

**Implementations:**
- `StaticSpendingStrategy.java` - 4% rule
- `IncomeGapStrategy.java` - Gap-based withdrawals
- `GuardrailsSpendingStrategy.java` - Dynamic guardrails
- `DefaultSpendingOrchestrator.java` - Default orchestrator
- `RmdAwareOrchestrator.java` - RMD-compliant orchestrator
- `TaxEfficientSequencer.java` - Tax-efficient ordering
- `RmdFirstSequencer.java` - RMD-first ordering

**Value Objects:**
- `SpendingContext.java` - Spending calculation context
- `SpendingPlan.java` - Spending result
- `AccountWithdrawal.java` - Per-account withdrawal
- `AccountSnapshot.java` - Immutable account state

**Configuration:**
- `GuardrailsConfiguration.java` - Guardrails config with presets

**Test Support:**
- `StubSimulationView.java` - Testing stub
- `RmdRulesTestLoader.java` - YAML config loader for tests

**Integration Tests:**
- `M6EndToEndIntegrationTest.java` - E2E tests
- `M6bStrategyIntegrationTest.java` - Strategy integration tests
- `domain/integration/package-info.java` - Package documentation

---

## Participants

- **Developer:** @xmljim
- **AI Assistant:** Claude (Anthropic)

---

*This retrospective was conducted on December 30, 2025, following the completion of Milestone 6.*
