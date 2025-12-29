# Domain Package Refactoring Plan

**Status:** Proposed
**Date:** December 29, 2025
**Target:** Before M10 (Persistence Layer)

---

## Problem Statement

The `domain/value` package has grown to 47 files and become a catch-all. Similarly, `domain/calculator` has 29 interfaces mixing different business concerns. This makes it harder to:
- Find related classes
- Understand domain boundaries
- Create well-organized repositories (M10)
- Design clean API DTOs (M12)

## Current State

| Package | Files | Issue |
|---------|-------|-------|
| `domain/value` | 47 | Too large, mixed concerns |
| `domain/calculator` | 29 | Mixed concerns |
| `domain/calculator/impl` | 26 | Mixed concerns |
| `domain/enums` | 23 | Acceptable |
| `domain/config` | 10 | Acceptable |
| `domain/exception` | 8 | Keep as-is |
| `domain/model` | 7 | Rename to `core` |

---

## Proposed Structure

```
io.github.xmljim.retirement.domain/
├── core/                    # Core entities (7 files)
│   ├── PersonProfile.java
│   ├── Portfolio.java
│   ├── InvestmentAccount.java
│   ├── Transaction.java
│   ├── TransactionHistory.java
│   ├── Scenario.java
│   └── CouplePortfolioView.java
│
├── income/                  # Income modeling (17 files)
│   ├── WorkingIncome.java
│   ├── Pension.java
│   ├── Annuity.java
│   ├── OtherIncome.java
│   ├── RetirementIncome.java
│   ├── IncomeSources.java
│   ├── IncomeBreakdown.java
│   ├── IncomeCalculator.java
│   ├── IncomeAggregator.java
│   ├── impl/
│   │   ├── DefaultIncomeCalculator.java
│   │   └── DefaultIncomeAggregator.java
│   └── enums/
│       ├── AnnuityType.java
│       ├── OtherIncomeType.java
│       └── PensionPaymentForm.java
│
├── socialsecurity/          # Social Security (18 files)
│   ├── SocialSecurityBenefit.java
│   ├── SocialSecurityIncome.java
│   ├── EarningsTestResult.java
│   ├── SpousalBenefitResult.java
│   ├── SurvivorBenefitResult.java
│   ├── CoupleClaimingStrategy.java
│   ├── MarriageInfo.java
│   ├── PastMarriage.java
│   ├── SocialSecurityCalculator.java
│   ├── SpousalBenefitCalculator.java
│   ├── EarningsTestCalculator.java
│   ├── impl/
│   │   ├── DefaultSocialSecurityCalculator.java
│   │   ├── DefaultSpousalBenefitCalculator.java
│   │   └── DefaultEarningsTestCalculator.java
│   ├── config/
│   │   └── SocialSecurityRules.java
│   └── enums/
│       ├── MaritalStatus.java
│       ├── MarriageEndReason.java
│       └── SurvivorRole.java
│
├── expense/                 # Expense & Budget (22 files)
│   ├── Budget.java
│   ├── RecurringExpense.java
│   ├── OneTimeExpense.java
│   ├── ExpenseBreakdown.java
│   ├── ScheduledExpense.java
│   ├── RandomExpenseEvent.java
│   ├── ContingencyReserve.java
│   ├── SurvivorAdjustment.java
│   ├── GapAnalysis.java
│   ├── GapAnalyzer.java
│   ├── ExpenseModifier.java
│   ├── ExpenseAllocationStrategy.java
│   ├── SurvivorExpenseCalculator.java
│   ├── impl/
│   │   ├── DefaultGapAnalyzer.java
│   │   ├── DefaultSurvivorExpenseCalculator.java
│   │   ├── AgeBasedModifier.java
│   │   ├── SpendingCurveModifier.java
│   │   ├── PayoffModifier.java
│   │   └── PriorityBasedAllocationStrategy.java
│   ├── config/
│   │   └── SurvivorExpenseRules.java
│   └── enums/
│       ├── ExpenseCategory.java
│       ├── ExpenseCategoryGroup.java
│       ├── ExpenseFrequency.java
│       ├── ContingencyType.java
│       └── SpendingPhase.java
│
├── contribution/            # Contribution management (22 files)
│   ├── ContributionConfig.java
│   ├── ContributionResult.java
│   ├── ContributionRecord.java
│   ├── RoutingConfiguration.java
│   ├── RoutingRule.java
│   ├── YTDSummary.java
│   ├── MatchingPolicy.java
│   ├── SimpleMatchingPolicy.java
│   ├── TieredMatchingPolicy.java
│   ├── NoMatchingPolicy.java
│   ├── MatchTier.java
│   ├── ContributionCalculator.java
│   ├── ContributionRouter.java
│   ├── ContributionLimitChecker.java
│   ├── ContributionAllocation.java
│   ├── YTDContributionTracker.java
│   ├── IrsContributionRules.java
│   ├── LimitCheckResult.java
│   ├── impl/
│   │   ├── DefaultContributionCalculator.java
│   │   ├── DefaultContributionRouter.java
│   │   ├── DefaultContributionLimitChecker.java
│   │   ├── DefaultYTDContributionTracker.java
│   │   └── Secure2ContributionRules.java
│   ├── config/
│   │   └── IrsContributionLimits.java
│   └── enums/
│       ├── ContributionType.java
│       ├── LimitCategory.java
│       └── OverflowBehavior.java
│
├── tax/                     # Tax calculations (15 files)
│   ├── TaxCalculationResult.java
│   ├── TaxBracket.java
│   ├── TaxationResult.java
│   ├── IncomeDetails.java
│   ├── FederalTaxCalculator.java
│   ├── MAGICalculator.java
│   ├── PhaseOutCalculator.java
│   ├── PhaseOutResult.java
│   ├── BenefitTaxationCalculator.java
│   ├── impl/
│   │   ├── DefaultFederalTaxCalculator.java
│   │   ├── DefaultMAGICalculator.java
│   │   ├── DefaultPhaseOutCalculator.java
│   │   └── DefaultBenefitTaxationCalculator.java
│   ├── config/
│   │   ├── FederalTaxRules.java
│   │   └── IraPhaseOutLimits.java
│   └── enums/
│       └── FilingStatus.java
│
├── healthcare/              # Healthcare & RMD (12 files)
│   ├── MedicarePremiums.java
│   ├── LtcInsurance.java
│   ├── RmdProjection.java
│   ├── MedicareCalculator.java
│   ├── RmdCalculator.java
│   ├── impl/
│   │   ├── DefaultMedicareCalculator.java
│   │   └── DefaultRmdCalculator.java
│   ├── config/
│   │   ├── MedicareRules.java
│   │   ├── LtcRules.java
│   │   └── RmdRules.java
│   └── enums/
│       └── LtcTriggerMode.java
│
├── investment/              # Investment & returns (18 files)
│   ├── AssetAllocation.java
│   ├── WithdrawalResult.java
│   ├── WithdrawalStrategy.java
│   ├── AllocationResult.java
│   ├── InflationAssumptions.java
│   ├── ReturnCalculator.java
│   ├── WithdrawalCalculator.java
│   ├── InflationCalculator.java
│   ├── CompoundingFunction.java
│   ├── CompoundingFunctions.java
│   ├── impl/
│   │   ├── DefaultReturnCalculator.java
│   │   ├── DefaultWithdrawalCalculator.java
│   │   ├── DefaultInflationCalculator.java
│   │   └── MathUtils.java
│   ├── config/
│   │   └── InflationRates.java
│   └── enums/
│       ├── AccountType.java
│       ├── TransactionType.java
│       ├── WithdrawalType.java
│       ├── DistributionStrategy.java
│       └── ShortfallHandling.java
│
├── exception/               # Shared exceptions (keep as-is)
│   ├── RetirementException.java
│   ├── ValidationException.java
│   ├── CalculationException.java
│   ├── ConfigurationException.java
│   ├── InvalidAllocationException.java
│   ├── InvalidDateRangeException.java
│   ├── InvalidRateException.java
│   └── MissingRequiredFieldException.java
│
├── config/                  # Shared config (reduced)
│   └── CalculatorConfig.java
│
├── enums/                   # Shared enums (reduced)
│   ├── EndCondition.java
│   └── SimulationMode.java
│
└── annotation/              # Annotations
    └── Generated.java
```

---

## Migration Summary

| New Package | Files | From |
|-------------|-------|------|
| `core` | 7 | `model` (rename) |
| `income` | 17 | `value`, `calculator`, `enums` |
| `socialsecurity` | 18 | `value`, `calculator`, `config`, `enums` |
| `expense` | 22 | `value`, `calculator`, `config`, `enums` |
| `contribution` | 22 | `value`, `calculator`, `config`, `enums` |
| `tax` | 15 | `value`, `calculator`, `config`, `enums` |
| `healthcare` | 12 | `value`, `calculator`, `config`, `enums` |
| `investment` | 18 | `value`, `calculator`, `config`, `enums` |
| `exception` | 8 | (unchanged) |
| `config` | 1 | (reduced) |
| `enums` | 2 | (reduced) |

---

## Benefits

1. **Domain-Driven Design** - Packages align with business capabilities
2. **Discoverability** - Related classes are together
3. **Repository Alignment** - Clean boundaries for M10 persistence
4. **API Design** - Natural groupings for M12 REST endpoints
5. **Smaller Packages** - Max ~22 files vs current 47

---

## Implementation Steps

### Phase 1: Create New Package Structure
1. Create new packages under `domain/`
2. Add `package-info.java` for each

### Phase 2: Move Files (by domain)
1. `core` - Move from `model`
2. `income` - Extract from `value`, `calculator`
3. `socialsecurity` - Extract SS-related
4. `expense` - Extract budget/expense-related
5. `contribution` - Extract contribution-related
6. `tax` - Extract tax-related
7. `healthcare` - Extract Medicare/LTC/RMD
8. `investment` - Extract investment-related

### Phase 3: Update Imports
1. IDE refactoring handles most imports
2. Verify Spring `@ComponentScan` paths
3. Update YAML config class references

### Phase 4: Cleanup
1. Delete empty old packages
2. Update Javadoc package references
3. Update any documentation

### Phase 5: Verify
1. Run full test suite
2. Verify build succeeds
3. Check code coverage unchanged

---

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Merge conflicts | Do in single PR, coordinate timing |
| Broken imports | Use IDE refactoring, run tests |
| Spring wiring issues | Verify component scan paths |
| Test failures | Run full suite after each domain move |

---

## Effort Estimate

| Phase | Effort |
|-------|--------|
| Create packages | 30 min |
| Move files | 2-3 hours |
| Update imports | 1 hour (mostly automated) |
| Cleanup & verify | 1 hour |
| **Total** | **4-5 hours** |

---

## Recommendation

Create a dedicated milestone **M9.5: Domain Refactoring** between M9 (Output & Reporting) and M10 (Persistence Layer). This ensures:

1. Clean domain boundaries before adding repositories
2. Focused effort without feature pressure
3. Single PR for atomic refactoring

Alternatively, this could be done as the **first issue in M10** since the refactoring directly supports the persistence layer design.
