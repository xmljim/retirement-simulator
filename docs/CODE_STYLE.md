# Code Style Guide

This document defines Java coding standards for the Retirement Portfolio Simulator.

## Table of Contents

- [General Principles](#general-principles)
- [Formatting](#formatting)
- [Naming Conventions](#naming-conventions)
- [SOLID Principles](#solid-principles)
- [Design Patterns](#design-patterns)
- [Best Practices](#best-practices)
- [Error Handling](#error-handling)
- [Documentation](#documentation)

---

## General Principles

1. **Clarity over cleverness** - Write code that is easy to understand
2. **Consistency** - Follow established patterns in the codebase
3. **Simplicity** - Prefer simple solutions; avoid over-engineering
4. **Immutability** - Prefer immutable objects where possible
5. **Composition over inheritance** - Favor composition for flexibility

---

## Formatting

### Indentation and Spacing

- **Indentation**: 4 spaces (no tabs)
- **Line length**: Maximum 120 characters
- **Blank lines**: One blank line between methods; two between class sections

### Braces

Use K&R style (opening brace on same line):

```java
public void calculate() {
    if (condition) {
        // code
    } else {
        // code
    }
}
```

### Imports

- No wildcard imports (`import java.util.*`)
- Order: java.*, javax.*, org.*, com.*, then project imports
- Separate groups with blank line
- Remove unused imports

---

## Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| Classes | PascalCase, noun | `PersonProfile`, `SimulationEngine` |
| Interfaces | PascalCase, adjective/noun | `Calculable`, `DistributionStrategy` |
| Methods | camelCase, verb | `calculateBalance()`, `getMonthlyIncome()` |
| Variables | camelCase, descriptive | `monthlyContribution`, `retirementDate` |
| Constants | UPPER_SNAKE_CASE | `MAX_CONTRIBUTION_LIMIT`, `DEFAULT_RATE` |
| Packages | lowercase | `io.github.xmljim.retirement.domain` |
| Type Parameters | Single uppercase | `T`, `E`, `K`, `V` |

### Naming Guidelines

- **Be descriptive**: `calculateMonthlyWithdrawal()` not `calc()`
- **Avoid abbreviations**: `contribution` not `contrib` (except well-known: `id`, `url`)
- **Boolean methods**: Use `is`, `has`, `can`, `should` prefixes
- **Collections**: Use plural names (`accounts`, `transactions`)

---

## SOLID Principles

### Single Responsibility Principle (SRP)

Each class should have one reason to change.

```java
// Good: Separate concerns
public class ContributionCalculator { /* calculates contributions */ }
public class ContributionValidator { /* validates contribution rules */ }

// Bad: Multiple responsibilities
public class ContributionManager { /* calculates, validates, persists, logs */ }
```

### Open/Closed Principle (OCP)

Open for extension, closed for modification. Use interfaces and inheritance.

```java
// Good: Extensible via interface
public interface DistributionStrategy {
    MonthlyDistribution calculate(Portfolio portfolio, LocalDate date);
}

public class StaticWithdrawalStrategy implements DistributionStrategy { }
public class GuardrailsStrategy implements DistributionStrategy { }
```

### Liskov Substitution Principle (LSP)

Subtypes must be substitutable for their base types.

### Interface Segregation Principle (ISP)

Many specific interfaces over one general-purpose interface.

```java
// Good: Focused interfaces
public interface Readable { void read(); }
public interface Writable { void write(); }

// Bad: Fat interface
public interface ReadWriteDeleteUpdateExport { /* too many methods */ }
```

### Dependency Inversion Principle (DIP)

Depend on abstractions, not concretions.

```java
// Good: Depend on interface
public class SimulationEngine {
    private final DistributionStrategy strategy;

    public SimulationEngine(DistributionStrategy strategy) {
        this.strategy = strategy;
    }
}

// Bad: Depend on concrete class
public class SimulationEngine {
    private final StaticWithdrawalStrategy strategy = new StaticWithdrawalStrategy();
}
```

---

## Design Patterns

Apply Gang of Four patterns where appropriate:

### Creational Patterns

- **Builder**: Complex object construction (already used in `PortfolioParameters`)
- **Factory**: Object creation abstraction
- **Singleton**: Use sparingly; prefer dependency injection

### Structural Patterns

- **Adapter**: Interface compatibility
- **Decorator**: Add behavior dynamically
- **Composite**: Tree structures

### Behavioral Patterns

- **Strategy**: Interchangeable algorithms (distribution strategies)
- **Observer**: Event notification
- **Template Method**: Algorithm skeleton with customizable steps
- **Command**: Encapsulate requests

---

## Best Practices

### Immutability

```java
// Good: Immutable class
public final class Money {
    private final BigDecimal amount;
    private final Currency currency;

    public Money(BigDecimal amount, Currency currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount), this.currency);
    }
}
```

### Use Optional for Nullable Returns

```java
// Good
public Optional<Account> findAccount(String id) {
    return Optional.ofNullable(accounts.get(id));
}

// Bad
public Account findAccount(String id) {
    return accounts.get(id); // may return null
}
```

### Constants over Magic Numbers

```java
// Good
private static final double DEFAULT_INFLATION_RATE = 0.03;
private static final int MONTHS_PER_YEAR = 12;

double monthlyRate = DEFAULT_INFLATION_RATE / MONTHS_PER_YEAR;

// Bad
double monthlyRate = 0.03 / 12;
```

### Use BigDecimal for Financial Calculations

```java
// Good
BigDecimal balance = new BigDecimal("10000.00");
BigDecimal rate = new BigDecimal("0.07");
BigDecimal growth = balance.multiply(rate).setScale(2, RoundingMode.HALF_UP);

// Bad - floating point errors
double balance = 10000.00;
double growth = balance * 0.07;
```

### Defensive Copies

```java
public class Portfolio {
    private final List<Account> accounts;

    public Portfolio(List<Account> accounts) {
        this.accounts = new ArrayList<>(accounts); // defensive copy
    }

    public List<Account> getAccounts() {
        return Collections.unmodifiableList(accounts); // immutable view
    }
}
```

---

## Error Handling

### Use Specific Exceptions

```java
// Good
public void withdraw(BigDecimal amount) throws InsufficientFundsException {
    if (amount.compareTo(balance) > 0) {
        throw new InsufficientFundsException("Balance: " + balance + ", Requested: " + amount);
    }
}

// Bad
public void withdraw(BigDecimal amount) throws Exception {
    if (amount.compareTo(balance) > 0) {
        throw new Exception("Not enough money");
    }
}
```

### Don't Swallow Exceptions

```java
// Good
try {
    process();
} catch (ProcessingException e) {
    logger.error("Processing failed", e);
    throw new SimulationException("Failed to process", e);
}

// Bad
try {
    process();
} catch (Exception e) {
    // silently ignored
}
```

### Fail Fast

Validate inputs early and fail with clear messages.

```java
public void setContributionRate(double rate) {
    if (rate < 0 || rate > 1) {
        throw new IllegalArgumentException("Rate must be between 0 and 1, got: " + rate);
    }
    this.contributionRate = rate;
}
```

---

## Documentation

### Javadoc Requirements

- All public classes and interfaces
- All public methods
- Complex algorithms or business logic

### Javadoc Format

```java
/**
 * Calculates the monthly withdrawal amount based on the distribution strategy.
 *
 * <p>The calculation considers current portfolio balance, income sources,
 * and expense requirements to determine the optimal withdrawal.
 *
 * @param portfolio the portfolio to withdraw from
 * @param date the date of the withdrawal
 * @return the calculated withdrawal amount, never negative
 * @throws IllegalStateException if portfolio is depleted
 */
public BigDecimal calculateWithdrawal(Portfolio portfolio, LocalDate date) {
    // implementation
}
```

### Inline Comments

- Use sparingly; prefer self-documenting code
- Explain "why", not "what"
- Keep comments up to date with code

```java
// Good: Explains why
// Using 4.7% instead of 4% to account for historical sequence-of-returns risk
private static final double SAFE_WITHDRAWAL_RATE = 0.047;

// Bad: States the obvious
// Set the rate to 0.047
rate = 0.047;
```

---

## Tools

These standards are enforced by:

- **Checkstyle** - Style and formatting
- **SpotBugs** - Bug detection
- **PMD** - Code smells and best practices

Run locally before committing:

```bash
mvn verify
```
