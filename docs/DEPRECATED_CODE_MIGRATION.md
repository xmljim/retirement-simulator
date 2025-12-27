# Deprecated Code Migration Plan

This document tracks deprecated code from M1 and the plan for removal in M10 (API Layer).

## Overview

During M1, several classes were marked as deprecated after being replaced by the new domain model. These remain for backwards compatibility but should be removed when the API layer is implemented.

## Deprecated Types

### 1. `Functions.java` (`@Deprecated(forRemoval = true)`)
**Location**: `io.github.xmljim.retirement.functions.Functions`
**Replaced by**: Calculator framework (`CalculatorFactory`, individual calculators)

**Current Usage**:
- `Main.java` - CLI demo application

**Migration Tasks for M10**:
- [ ] Update `Main.java` to use new Calculator framework
- [ ] Remove `Functions.java`
- [ ] Remove `FunctionsTest.java` (or migrate to test new calculators)

### 2. `PortfolioParameters.java` (`@Deprecated`)
**Location**: `io.github.xmljim.retirement.model.PortfolioParameters`
**Replaced by**: Domain model classes (`PersonProfile`, `Portfolio`, `Scenario`)

**Current Usage**:
- `Functions.java` - as input parameter

**Migration Tasks for M10**:
- [ ] Remove when `Functions.java` is removed
- [ ] No external API should expose this type

### 3. `ContributionType.java` (model) (`@Deprecated`)
**Location**: `io.github.xmljim.retirement.model.ContributionType`
**Replaced by**: `io.github.xmljim.retirement.domain.enums.ContributionType`

**Current Usage**: None (orphaned)

**Migration Tasks for M10**:
- [ ] Delete - no longer used

### 4. `TransactionType.java` (`@Deprecated`)
**Location**: `io.github.xmljim.retirement.model.TransactionType`
**Replaced by**: Transaction handling in domain model

**Current Usage**: None (orphaned)

**Migration Tasks for M10**:
- [ ] Delete - no longer used

### 5. `WithdrawalType.java` (model) (`@Deprecated`)
**Location**: `io.github.xmljim.retirement.model.WithdrawalType`
**Replaced by**: `io.github.xmljim.retirement.domain.enums.WithdrawalType`

**Current Usage**: None (orphaned)

**Migration Tasks for M10**:
- [ ] Delete - no longer used

## Recommended Approach

1. **M10 Planning**: Include deprecated code removal as acceptance criteria
2. **API Design**: Ensure new REST API uses only domain model types
3. **Cleanup Order**:
   - First: Remove orphaned enums (ContributionType, TransactionType, WithdrawalType in model package)
   - Second: Update Main.java to use Calculator framework
   - Third: Remove Functions.java and PortfolioParameters.java
4. **Testing**: Ensure all functionality is covered by new calculator tests before removal

## Notes

- The deprecated code has comprehensive Javadoc pointing to replacements
- Test coverage exists via new calculator tests, making removal safe
- No production code depends on these deprecated types
