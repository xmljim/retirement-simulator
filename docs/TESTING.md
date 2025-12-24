# Testing Standards

This document defines testing requirements and best practices for the Retirement Portfolio Simulator.

## Table of Contents

- [Testing Philosophy](#testing-philosophy)
- [Test Categories](#test-categories)
- [Coverage Requirements](#coverage-requirements)
- [Naming Conventions](#naming-conventions)
- [Test Structure](#test-structure)
- [Best Practices](#best-practices)
- [Financial Calculation Testing](#financial-calculation-testing)
- [Running Tests](#running-tests)

---

## Testing Philosophy

1. **Tests are mandatory** - All business logic must have tests
2. **Tests are documentation** - Tests describe expected behavior
3. **Tests enable refactoring** - Confidence to change code safely
4. **Test behavior, not implementation** - Focus on what, not how
5. **Fast feedback** - Tests should run quickly

---

## Test Categories

### Unit Tests

Test individual classes/methods in isolation.

- **Location**: `src/test/java`
- **Naming**: `<ClassName>Test.java`
- **Dependencies**: Mocked or stubbed
- **Speed**: Milliseconds

```java
class ContributionCalculatorTest {
    @Test
    void shouldCalculateMonthlyContribution() {
        // Test single unit of work
    }
}
```

### Integration Tests

Test component interactions.

- **Location**: `src/test/java` with `IT` suffix
- **Naming**: `<ClassName>IT.java`
- **Dependencies**: Real collaborators
- **Speed**: Seconds

```java
class SimulationEngineIT {
    @Test
    void shouldRunCompleteSimulation() {
        // Test multiple components working together
    }
}
```

### Scenario Tests

End-to-end simulation validation.

- **Location**: `src/test/java/scenarios`
- **Purpose**: Validate complete retirement scenarios
- **Speed**: May take longer

---

## Coverage Requirements

### Thresholds

| Metric | Minimum | Target |
|--------|---------|--------|
| Line Coverage | 80% | 90% |
| Branch Coverage | 75% | 85% |
| Method Coverage | 80% | 90% |

### What to Cover

**Must cover:**
- All calculation logic
- Business rules
- Domain model behavior
- Edge cases and boundaries
- Error conditions

**May exclude:**
- Simple getters/setters (if using Lombok)
- Configuration classes
- Main class
- Generated code

---

## Naming Conventions

### Test Class Names

```
<ClassUnderTest>Test.java
<ClassUnderTest>IT.java (integration tests)
```

### Test Method Names

Use descriptive names that explain the scenario:

```java
// Pattern: should<ExpectedBehavior>_when<Condition>
@Test
void shouldReturnZero_whenBalanceIsNegative() { }

@Test
void shouldApplyCatchUpContribution_whenAgeIs50OrOlder() { }

@Test
void shouldThrowException_whenContributionExceedsLimit() { }
```

### Display Names

Use `@DisplayName` for readable test reports:

```java
@Test
@DisplayName("Should calculate 8% delayed credits when claiming SS at age 70")
void shouldCalculateDelayedCredits() { }
```

---

## Test Structure

### Arrange-Act-Assert (AAA)

```java
@Test
void shouldCalculateMonthlyWithdrawal() {
    // Arrange
    Portfolio portfolio = createPortfolioWithBalance(100_000);
    StaticWithdrawalStrategy strategy = new StaticWithdrawalStrategy(0.04);

    // Act
    BigDecimal withdrawal = strategy.calculate(portfolio, LocalDate.now());

    // Assert
    assertThat(withdrawal).isEqualByComparingTo(new BigDecimal("333.33"));
}
```

### Given-When-Then (BDD Style)

```java
@Test
void shouldReduceBenefits_whenClaimingBeforeFRA() {
    // Given
    SocialSecurityCalculator calculator = new SocialSecurityCalculator();
    BigDecimal fraAmount = new BigDecimal("2000");
    int claimingAge = 62;
    int fra = 67;

    // When
    BigDecimal actualBenefit = calculator.calculateBenefit(fraAmount, claimingAge, fra);

    // Then
    assertThat(actualBenefit).isLessThan(fraAmount);
    assertThat(actualBenefit).isEqualByComparingTo(new BigDecimal("1400")); // 30% reduction
}
```

---

## Best Practices

### Use Descriptive Assertions

```java
// Good - Clear failure message
assertThat(account.getBalance())
    .as("Account balance after contribution")
    .isEqualByComparingTo(expectedBalance);

// Bad - Unclear on failure
assertEquals(expectedBalance, account.getBalance());
```

### One Assertion Per Concept

```java
// Good - Test one behavior
@Test
void shouldApplyInflationToExpenses() {
    BigDecimal adjusted = calculator.applyInflation(expense, rate, years);
    assertThat(adjusted).isEqualByComparingTo(expected);
}

@Test
void shouldNotModifyOriginalExpense() {
    calculator.applyInflation(expense, rate, years);
    assertThat(expense).isEqualByComparingTo(originalValue);
}
```

### Use Test Fixtures

```java
class PortfolioTest {
    private Portfolio portfolio;
    private Account account401k;

    @BeforeEach
    void setUp() {
        account401k = Account.builder()
            .type(AccountType.TRADITIONAL_401K)
            .balance(new BigDecimal("100000"))
            .build();

        portfolio = Portfolio.builder()
            .addAccount(account401k)
            .build();
    }
}
```

### Use Parameterized Tests

```java
@ParameterizedTest
@CsvSource({
    "62, 67, 0.70",   // 5 years early = 30% reduction
    "63, 67, 0.75",   // 4 years early = 25% reduction
    "64, 67, 0.80",   // 3 years early = 20% reduction
    "67, 67, 1.00",   // At FRA = full benefit
    "70, 67, 1.24"    // 3 years delayed = 24% increase
})
@DisplayName("Should apply correct SS adjustment factor based on claiming age")
void shouldCalculateSSAdjustmentFactor(int claimingAge, int fra, double expectedFactor) {
    double factor = calculator.getAdjustmentFactor(claimingAge, fra);
    assertThat(factor).isCloseTo(expectedFactor, within(0.01));
}
```

### Test Edge Cases

```java
@Nested
@DisplayName("Edge cases")
class EdgeCases {

    @Test
    void shouldHandleZeroBalance() { }

    @Test
    void shouldHandleNegativeReturns() { }

    @Test
    void shouldHandleMaximumContribution() { }

    @Test
    void shouldHandlePortfolioDepletion() { }

    @Test
    void shouldHandleLeapYear() { }
}
```

### Test Exceptions

```java
@Test
void shouldThrowException_whenContributionExceedsLimit() {
    Account account = createAccountAtLimit();

    assertThatThrownBy(() -> account.contribute(new BigDecimal("1000")))
        .isInstanceOf(ContributionLimitExceededException.class)
        .hasMessageContaining("exceeds annual limit");
}
```

---

## Financial Calculation Testing

### Use BigDecimal Comparisons

```java
// Good - Proper BigDecimal comparison
assertThat(result).isEqualByComparingTo(new BigDecimal("1234.56"));

// Bad - May fail due to scale differences
assertEquals(new BigDecimal("1234.56"), result);
```

### Test with Known Values

```java
@Test
void shouldCalculateCompoundInterest() {
    // Known calculation: $10,000 at 7% for 10 years
    // FV = 10000 * (1.07)^10 = $19,671.51
    BigDecimal result = calculator.futureValue(
        new BigDecimal("10000"),
        new BigDecimal("0.07"),
        10
    );

    assertThat(result).isEqualByComparingTo(new BigDecimal("19671.51"));
}
```

### Test Precision and Rounding

```java
@Test
void shouldRoundToTwoDecimalPlaces() {
    BigDecimal result = calculator.calculate(input);

    assertThat(result.scale()).isEqualTo(2);
}

@Test
void shouldUseHalfUpRounding() {
    // 100.005 should round to 100.01
    BigDecimal result = calculator.round(new BigDecimal("100.005"));
    assertThat(result).isEqualByComparingTo(new BigDecimal("100.01"));
}
```

### Test Time-Based Calculations

```java
@Test
void shouldCalculateCorrectMonthsBetweenDates() {
    LocalDate start = LocalDate.of(2025, 1, 1);
    LocalDate end = LocalDate.of(2035, 6, 15);

    long months = calculator.monthsBetween(start, end);

    assertThat(months).isEqualTo(125);
}
```

---

## Running Tests

### Run All Tests

```bash
mvn test
```

### Run Specific Test Class

```bash
mvn test -Dtest=ContributionCalculatorTest
```

### Run with Coverage Report

```bash
mvn test jacoco:report
```

View report at: `target/site/jacoco/index.html`

### Run Integration Tests

```bash
mvn verify
```

### Skip Tests (Not Recommended)

```bash
mvn install -DskipTests
```

---

## Test Dependencies

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10+</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <version>3.24+</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <version>5.0+</version>
    <scope>test</scope>
</dependency>
```
