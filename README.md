# Retirement Portfolio Simulator

[![CI](https://github.com/xmljim/retirement-simulator/actions/workflows/ci.yml/badge.svg)](https://github.com/xmljim/retirement-simulator/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/xmljim/retirement-simulator/branch/main/graph/badge.svg)](https://codecov.io/gh/xmljim/retirement-simulator)

A comprehensive retirement portfolio simulation tool that models the accumulation and distribution phases of retirement savings, enabling users to project portfolio performance under various scenarios.

## Overview

Retirement planning requires understanding how multiple variables interact over time:

- Contribution rates and employer matching
- Investment returns (varying pre/post retirement)
- Inflation and cost-of-living adjustments
- Social Security timing and amounts
- Pension and annuity income
- Withdrawal strategies and rates

This tool models these interactions on a monthly basis, providing visibility into portfolio balance trajectories from the present through retirement.

## Features

### Core Capabilities

- **Multi-Account Portfolio Support** - 401(k), IRA, Roth IRA, Roth 401(k), HSA, taxable brokerage
- **IRS Contribution Rules** - Limits, catch-up contributions, SECURE 2.0 compliance
- **Income Modeling** - Salary, Social Security, pensions, annuities, other sources
- **Expense Budgeting** - Category-based with differentiated inflation rates
- **Distribution Strategies** - Static (4% rule), Bucket, Spending Curve, Guardrails

### Simulation Modes

- **Deterministic** - Fixed return rates for baseline planning
- **Monte Carlo** - Probabilistic analysis with configurable runs
- **Historical Backtesting** - Test against actual market history

### Analysis & Reporting

- Portfolio timeline visualization
- Cash flow analysis
- Tax projections
- Scenario comparison
- Export to CSV, JSON, PDF

## Quick Start

### Prerequisites

- Java 25 or higher
- Maven 3.9+

### Build

```bash
mvn clean install
```

### Run Tests

```bash
mvn test
```

### Run with Coverage

```bash
mvn verify
```

## Documentation

| Document | Description |
|----------|-------------|
| [Project Goals](requirements/PROJECT_GOALS.md) | Vision, requirements, and milestones |
| [Architecture](docs/ARCHITECTURE.md) | System design and package structure |
| [Code Style](docs/CODE_STYLE.md) | Java coding standards and conventions |
| [Testing](docs/TESTING.md) | Testing requirements and best practices |
| [Contributing](docs/CONTRIBUTING.md) | How to contribute to the project |

## Project Structure

```
retirement-simulator/
├── src/
│   ├── main/java/           # Application source code
│   └── test/java/           # Test source code
├── docs/                    # Development documentation
│   ├── ARCHITECTURE.md
│   ├── CODE_STYLE.md
│   ├── CONTRIBUTING.md
│   └── TESTING.md
├── requirements/            # Project requirements
│   └── PROJECT_GOALS.md
└── pom.xml                  # Maven configuration
```

## Technology Stack

- **Core Library**: Java 25
- **Build Tool**: Maven
- **Testing**: JUnit 5, AssertJ, Mockito
- **API Layer**: Spring Boot (planned)
- **UI**: React (planned)
- **PDF Generation**: Apache PDFBox (planned)

## Development

### Branch Strategy

- `main` - Production-ready code
- `develop` - Integration branch
- `feature/<issue>-<desc>` - Feature development
- `bugfix/<issue>-<desc>` - Bug fixes

### Quality Gates

All code must pass:

- Checkstyle (code style)
- SpotBugs (static analysis)
- PMD (code smells)
- JaCoCo (80% line coverage minimum)

### Making Changes

1. Create a feature branch from `develop`
2. Make changes following [Code Style](docs/CODE_STYLE.md)
3. Write tests following [Testing Standards](docs/TESTING.md)
4. Submit a pull request

See [Contributing](docs/CONTRIBUTING.md) for detailed guidelines.

## Roadmap

| Milestone | Description | Status |
|-----------|-------------|--------|
| M0 | Project Setup & CI/CD | Complete |
| M1 | Domain Model Foundation | Complete |
| M2 | Core Transaction & Account Operations | Complete |
| M3a | Contribution Routing & Tracking | Complete |
| M3b | Income-Based Phase-Outs | Complete |
| M4 | Income Modeling | Complete |
| M5 | Expense & Budget Modeling | Complete |
| M6 | Distribution Strategies | Planned |
| M7 | Simulation Engine | Planned |
| M8 | Scenario Analysis | Planned |
| M9 | Output & Reporting | Planned |
| M10 | API Layer | Planned |
| M11 | UI (React) | Planned |

See [Project Goals](requirements/PROJECT_GOALS.md) for detailed milestone breakdowns.

## License

[TBD]

## Contact

[TBD]
