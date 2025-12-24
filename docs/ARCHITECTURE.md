# Architecture Guide

This document describes the architectural design of the Retirement Portfolio Simulator.

## Table of Contents

- [Overview](#overview)
- [Layered Architecture](#layered-architecture)
- [Package Structure](#package-structure)
- [Domain Model](#domain-model)
- [Key Design Patterns](#key-design-patterns)
- [Data Flow](#data-flow)
- [API Design](#api-design)
- [Dependencies](#dependencies)

---

## Overview

The Retirement Portfolio Simulator follows a **library-first** design:

- Core simulation engine is a standalone Java library
- No UI dependencies in core logic
- Clean separation of concerns
- API layer provides RESTful access
- React UI consumes the API

### Design Principles

1. **Separation of Concerns** - Each layer has distinct responsibilities
2. **Dependency Inversion** - Higher layers depend on abstractions
3. **Single Responsibility** - Each class has one reason to change
4. **Open/Closed** - Extensible without modification
5. **Testability** - All components are independently testable

---

## Layered Architecture

```
┌─────────────────────────────────────┐
│         UI Layer (React)            │  ← Visualization, user interaction
├─────────────────────────────────────┤
│         API Layer (REST/JSON)       │  ← HTTP endpoints, DTOs
├─────────────────────────────────────┤
│       Reporting Module              │  ← Report generation, exports
├─────────────────────────────────────┤
│      Simulation Engine              │  ← Core calculations, scenarios
├─────────────────────────────────────┤
│         Domain Model                │  ← Entities, value objects
└─────────────────────────────────────┘
```

### Layer Rules

| Layer | May Depend On | Must Not Depend On |
|-------|---------------|-------------------|
| UI | API | Domain, Simulation, Reporting |
| API | Simulation, Reporting, Domain | UI |
| Reporting | Simulation, Domain | UI, API |
| Simulation | Domain | UI, API, Reporting |
| Domain | Nothing | All other layers |

---

## Package Structure

```
io.github.xmljim.retirement/
├── domain/                    # Domain Model Layer
│   ├── model/                 # Core entities
│   │   ├── PersonProfile.java
│   │   ├── Portfolio.java
│   │   ├── InvestmentAccount.java
│   │   ├── Scenario.java
│   │   └── Transaction.java
│   ├── enums/                 # Domain enumerations
│   │   ├── AccountType.java
│   │   ├── TransactionType.java
│   │   └── DistributionStrategyType.java
│   └── value/                 # Value objects
│       ├── Money.java
│       └── DateRange.java
│
├── simulation/                # Simulation Engine Layer
│   ├── engine/                # Core simulation
│   │   ├── SimulationEngine.java
│   │   ├── SimulationRunner.java
│   │   └── MonthlyLoop.java
│   ├── calculator/            # Calculation services
│   │   ├── ContributionCalculator.java
│   │   ├── WithdrawalCalculator.java
│   │   ├── ReturnCalculator.java
│   │   └── InflationCalculator.java
│   ├── strategy/              # Distribution strategies
│   │   ├── DistributionStrategy.java
│   │   ├── StaticWithdrawalStrategy.java
│   │   ├── BucketStrategy.java
│   │   ├── SpendingCurveStrategy.java
│   │   └── GuardrailsStrategy.java
│   ├── income/                # Income modeling
│   │   ├── IncomeSource.java
│   │   ├── SalaryIncome.java
│   │   ├── SocialSecurityIncome.java
│   │   └── PensionIncome.java
│   ├── expense/               # Expense modeling
│   │   ├── ExpenseCategory.java
│   │   └── Budget.java
│   ├── rules/                 # IRS rules and limits
│   │   ├── ContributionLimits.java
│   │   └── RMDRules.java
│   └── result/                # Simulation results
│       ├── SimulationResult.java
│       └── MonthlySnapshot.java
│
├── reporting/                 # Reporting Module Layer
│   ├── report/                # Report types
│   │   ├── TransactionReport.java
│   │   ├── TimelineReport.java
│   │   ├── CashFlowReport.java
│   │   └── MonteCarloReport.java
│   └── export/                # Export formats
│       ├── CsvExporter.java
│       ├── JsonExporter.java
│       └── PdfExporter.java
│
├── api/                       # API Layer (Spring Boot)
│   ├── controller/            # REST controllers
│   ├── dto/                   # Data transfer objects
│   ├── mapper/                # Entity-DTO mappers
│   └── config/                # API configuration
│
└── config/                    # Application configuration
    └── ContributionLimitsConfig.java
```

---

## Domain Model

### Core Entities

```
┌──────────────────┐      ┌──────────────────┐
│  PersonProfile   │──────│  PersonProfile   │
│                  │spouse│    (Spouse)      │
├──────────────────┤      └──────────────────┘
│ - dateOfBirth    │
│ - retirementDate │             │
│ - lifeExpectancy │             │
└────────┬─────────┘             │
         │ owns                  │
         ▼                       ▼
┌──────────────────┐      ┌──────────────────┐
│    Portfolio     │      │    Portfolio     │
├──────────────────┤      └──────────────────┘
│ - accounts[]     │
└────────┬─────────┘
         │ contains
         ▼
┌──────────────────┐
│ InvestmentAccount│
├──────────────────┤
│ - type           │
│ - balance        │
│ - allocation     │
│ - returnRate     │
└──────────────────┘
```

### Scenario Configuration

```
┌──────────────────────────────────────────┐
│               Scenario                    │
├──────────────────────────────────────────┤
│ - personProfile(s)                       │
│ - timeHorizon (start, end)               │
│ - simulationMode (deterministic/MC/hist) │
│ - distributionStrategy                   │
│ - assumptions (returns, inflation)       │
│ - expenseBudget                          │
└──────────────────────────────────────────┘
```

---

## Key Design Patterns

### Strategy Pattern - Distribution Strategies

```java
public interface DistributionStrategy {
    MonthlyDistribution calculate(
        Portfolio portfolio,
        Budget budget,
        IncomeAmount totalIncome,
        LocalDate date
    );
}

// Implementations
public class StaticWithdrawalStrategy implements DistributionStrategy { }
public class BucketStrategy implements DistributionStrategy { }
public class SpendingCurveStrategy implements DistributionStrategy { }
public class GuardrailsStrategy implements DistributionStrategy { }
```

### Builder Pattern - Complex Objects

```java
PersonProfile profile = PersonProfile.builder()
    .dateOfBirth(LocalDate.of(1970, 5, 15))
    .retirementDate(LocalDate.of(2035, 1, 1))
    .lifeExpectancy(90)
    .portfolio(portfolio)
    .build();
```

### Factory Pattern - Calculator Creation

```java
public class CalculatorFactory {
    public ContributionCalculator createContributionCalculator(
        ContributionLimits limits,
        int taxYear
    ) {
        return new ContributionCalculator(limits, taxYear);
    }
}
```

### Template Method - Simulation Loop

```java
public abstract class SimulationTemplate {
    public final SimulationResult run(Scenario scenario) {
        initialize(scenario);
        while (hasMoreMonths()) {
            processMonth();
        }
        return finalize();
    }

    protected abstract void processMonth();
}
```

---

## Data Flow

### Simulation Execution Flow

```
┌─────────┐    ┌──────────┐    ┌────────────┐    ┌────────┐
│ Scenario│───▶│Simulation│───▶│  Monthly   │───▶│ Result │
│ Config  │    │  Engine  │    │   Loop     │    │        │
└─────────┘    └──────────┘    └────────────┘    └────────┘
                                     │
                    ┌────────────────┼────────────────┐
                    ▼                ▼                ▼
              ┌──────────┐    ┌──────────┐    ┌──────────┐
              │  Income  │    │ Expense  │    │Distribution│
              │Calculator│    │Calculator│    │ Strategy  │
              └──────────┘    └──────────┘    └──────────┘
```

### Monthly Loop Steps

1. **Check Life Events** - Retirement, SS start, RMD age
2. **Calculate Income** - Salary, SS, pension, annuities
3. **Calculate Expenses** - Apply category-specific inflation
4. **Determine Gap** - Expenses minus income
5. **Execute Distribution** - Apply chosen strategy
6. **Apply Contributions** - Pre-retirement only
7. **Apply Returns** - Monthly investment growth
8. **Record Transaction** - Log all activity

---

## API Design

### RESTful Endpoints

```
Profiles:
  POST   /api/profiles          - Create profile
  GET    /api/profiles/{id}     - Get profile
  PUT    /api/profiles/{id}     - Update profile
  DELETE /api/profiles/{id}     - Delete profile

Portfolio:
  GET    /api/profiles/{id}/portfolio
  POST   /api/profiles/{id}/portfolio/accounts
  PUT    /api/profiles/{id}/portfolio/accounts/{accountId}
  DELETE /api/profiles/{id}/portfolio/accounts/{accountId}

Scenarios:
  POST   /api/scenarios         - Create scenario
  GET    /api/scenarios/{id}    - Get scenario
  PUT    /api/scenarios/{id}    - Update scenario
  POST   /api/scenarios/{id}/clone

Simulations:
  POST   /api/simulations       - Run simulation
  POST   /api/simulations/async - Run async (Monte Carlo)
  GET    /api/simulations/{jobId}/status
  GET    /api/simulations/{jobId}/results

Reports:
  GET    /api/reports/timeline?format={json|csv|pdf}
  GET    /api/reports/cashflow
  GET    /api/reports/transactions
```

### Response Format

```json
{
  "success": true,
  "data": { ... },
  "meta": {
    "timestamp": "2025-01-15T10:30:00Z",
    "requestId": "abc-123"
  }
}
```

### Error Response

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Contribution exceeds annual limit",
    "details": [
      {
        "field": "contributionAmount",
        "message": "Must not exceed $23,000"
      }
    ]
  }
}
```

---

## Dependencies

### Dependency Direction

```
UI ──▶ API ──▶ Simulation ──▶ Domain
                    │
                    └──▶ Reporting ──▶ Domain
```

### External Dependencies

| Dependency | Purpose | Layer |
|------------|---------|-------|
| JUnit 5 | Testing | All |
| AssertJ | Test assertions | All |
| Mockito | Mocking | All |
| Jackson | JSON processing | API, Reporting |
| Spring Boot | REST API | API |
| Apache PDFBox | PDF generation | Reporting |

### Dependency Injection

Use constructor injection for all dependencies:

```java
public class SimulationEngine {
    private final ContributionCalculator contributionCalc;
    private final DistributionStrategy distributionStrategy;

    public SimulationEngine(
        ContributionCalculator contributionCalc,
        DistributionStrategy distributionStrategy
    ) {
        this.contributionCalc = contributionCalc;
        this.distributionStrategy = distributionStrategy;
    }
}
```
