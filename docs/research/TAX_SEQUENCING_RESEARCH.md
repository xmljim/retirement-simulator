# Research: Tax-Efficient Withdrawal Sequencing

**Issue:** #222
**Status:** Complete
**Date:** December 29, 2025

---

## Overview

Withdrawal sequencing determines which accounts to draw from and in what order. The goal is to minimize lifetime taxes while meeting income needs.

---

## 1. Traditional Sequencing

### Conventional Wisdom Order
```
1. Taxable accounts (brokerage)
2. Tax-deferred accounts (Traditional IRA, 401k)
3. Tax-free accounts (Roth IRA, Roth 401k)
```

### Rationale
- **Taxable first:** Minimize ongoing dividend/capital gains taxes
- **Tax-deferred second:** Delay tax until required (RMDs)
- **Roth last:** Maximize tax-free compounding

### When This Works
- Stable/consistent income needs
- Tax brackets stay relatively flat
- No specific tax optimization goals
- Simple implementation

---

## 2. Tax Bracket Management

### The Problem with Traditional Sequencing
Traditional sequencing can lead to:
- **Low taxable income early** (only capital gains from taxable)
- **High taxable income later** (large RMDs from tax-deferred)
- **Bracket creep** as RMDs grow
- **Wasted lower brackets** in early retirement

### Tax Bracket Filling Strategy
```
Fill lower tax brackets with tax-deferred withdrawals,
even if not needed for spending.

Strategy:
1. Calculate income from all sources
2. Identify "room" in current bracket
3. Withdraw from Traditional to fill bracket
4. Convert excess to Roth (if not needed for spending)
```

### 2025 Tax Brackets (Single)

| Bracket | Income Range | Marginal Rate |
|---------|--------------|---------------|
| 10% | $0 - $11,925 | 10% |
| 12% | $11,926 - $48,475 | 12% |
| 22% | $48,476 - $103,350 | 22% |
| 24% | $103,351 - $197,300 | 24% |
| 32% | $197,301 - $250,525 | 32% |
| 35% | $250,526 - $626,350 | 35% |
| 37% | $626,351+ | 37% |

### 2025 Tax Brackets (Married Filing Jointly)

| Bracket | Income Range | Marginal Rate |
|---------|--------------|---------------|
| 10% | $0 - $23,850 | 10% |
| 12% | $23,851 - $96,950 | 12% |
| 22% | $96,951 - $206,700 | 22% |
| 24% | $206,701 - $394,600 | 24% |
| 32% | $394,601 - $501,050 | 32% |
| 35% | $501,051 - $751,600 | 35% |
| 37% | $751,601+ | 37% |

### Example: Bracket Filling

```
Scenario: Married couple, $40,000 Social Security, $80,000 expenses

Traditional Approach:
- SS provides $40K (partially taxable)
- Withdraw $40K from taxable account
- Taxable income: ~$20K (SS taxation)
- Tax: ~$2,000
- 12% bracket has $77K unused!

Bracket-Filling Approach:
- SS provides $40K
- Withdraw $20K from taxable
- Withdraw $57K from Traditional (fills 12% bracket)
- Convert $37K to Roth (not needed for spending)
- Tax: ~$9,500 (higher now, but...)
- Future RMDs will be lower
- Roth grows tax-free
```

---

## 3. RMD Considerations

### RMD-First Rule
When subject to RMDs, they must be taken regardless of need:
```
if age >= rmdStartAge:
    rmd = calculateRmd(traditionalBalance, age)
    withdraw(traditionalAccount, min(rmd, traditionalBalance))
    # RMD counts toward spending need
```

### RMD Planning Windows

| Birth Year | RMD Start Age | First RMD Year |
|------------|---------------|----------------|
| ≤ 1950 | 72 | 2022 or earlier |
| 1951-1959 | 73 | 2024-2032 |
| ≥ 1960 | 75 | 2035+ |

### Pre-RMD Strategy
**Goal:** Reduce tax-deferred balance before RMDs start

```
Years until RMD: 10
Traditional balance: $1,000,000
Expected RMD at start: ~$36,500/year (grows with balance)

Strategy: Withdraw/convert to Roth in lower brackets NOW
to reduce future RMD amounts.
```

---

## 4. Special Scenarios

### A. Early Retirement (Before Social Security)

**Situation:** No earned income, no SS yet
**Opportunity:** Very low taxable income years

```
Recommended:
1. Fill standard deduction from Traditional ($29,200 MFJ)
2. Fill 10% bracket from Traditional
3. Fill 12% bracket from Traditional
4. Use Roth for amounts above 12% bracket threshold
5. Consider Roth conversions for future years
```

### B. Social Security Bridge Period

**Situation:** Retired but delaying SS to age 70

```
Years 62-69:
- All spending from portfolio
- Great Roth conversion opportunity
- Fill lower brackets aggressively

Year 70+:
- SS starts ($40-50K typically)
- Less room in lower brackets
- More reliance on Roth for spending flexibility
```

### C. ACA Subsidy Cliff

**Situation:** Need health insurance from ACA marketplace

```
2025 ACA subsidy cliff: 400% FPL
- Single: ~$60,240
- Couple: ~$81,760

If income exceeds cliff → lose ALL subsidies

Strategy:
- Keep MAGI below cliff
- Use Roth for excess needs (not counted in MAGI)
- Be careful with Roth conversions
```

### D. IRMAA Brackets

**Situation:** Medicare Part B/D premium surcharges

```
2025 IRMAA thresholds (Single):
- $106,000+: +$74/month
- $133,000+: +$185/month
- etc.

Strategy:
- Monitor MAGI 2 years ahead (IRMAA uses 2-year lookback)
- May want to avoid large Roth conversions in certain years
```

---

## 5. Sequencing Algorithms

### Algorithm 1: Simple Tax-Efficient

```java
public List<AccountWithdrawal> sequenceSimple(
    Portfolio portfolio,
    BigDecimal amountNeeded
) {
    List<InvestmentAccount> ordered = portfolio.getAccounts().stream()
        .sorted(Comparator.comparing(a ->
            switch (a.getAccountType().getTaxTreatment()) {
                case TAXABLE -> 1;
                case PRE_TAX -> 2;
                case POST_TAX -> 3;  // Roth
                case TAX_FREE -> 3;  // HSA qualified
            }))
        .toList();

    return withdrawInOrder(ordered, amountNeeded);
}
```

### Algorithm 2: RMD-First

```java
public List<AccountWithdrawal> sequenceRmdFirst(
    Portfolio portfolio,
    BigDecimal amountNeeded,
    int age,
    int birthYear,
    RmdCalculator rmdCalculator
) {
    List<AccountWithdrawal> withdrawals = new ArrayList<>();
    BigDecimal remaining = amountNeeded;

    // 1. Satisfy RMDs first
    for (InvestmentAccount account : portfolio.getAccounts()) {
        if (rmdCalculator.isSubjectToRmd(account.getAccountType())
            && rmdCalculator.isRmdRequired(age, birthYear)) {

            BigDecimal rmd = rmdCalculator.calculateRmd(
                account.getBalance(), age);

            withdrawals.add(new AccountWithdrawal(account, rmd));
            remaining = remaining.subtract(rmd);
        }
    }

    // 2. If RMDs satisfied need, done
    if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
        return withdrawals;
    }

    // 3. Otherwise, continue with tax-efficient sequence
    withdrawals.addAll(sequenceSimple(portfolio, remaining));
    return withdrawals;
}
```

### Algorithm 3: Bracket-Aware

```java
public List<AccountWithdrawal> sequenceBracketAware(
    Portfolio portfolio,
    BigDecimal amountNeeded,
    BigDecimal currentTaxableIncome,
    BigDecimal bracketTop,
    FilingStatus filingStatus
) {
    List<AccountWithdrawal> withdrawals = new ArrayList<>();
    BigDecimal remaining = amountNeeded;

    // 1. Calculate bracket room
    BigDecimal bracketRoom = bracketTop.subtract(currentTaxableIncome);

    // 2. Fill bracket from tax-deferred (if room exists)
    if (bracketRoom.compareTo(BigDecimal.ZERO) > 0) {
        BigDecimal fromTraditional = bracketRoom.min(remaining);
        for (InvestmentAccount acct : getPreTaxAccounts(portfolio)) {
            BigDecimal withdrawal = fromTraditional.min(acct.getBalance());
            withdrawals.add(new AccountWithdrawal(acct, withdrawal));
            remaining = remaining.subtract(withdrawal);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
        }
    }

    // 3. Remaining from Roth (avoid higher bracket)
    for (InvestmentAccount acct : getRothAccounts(portfolio)) {
        BigDecimal withdrawal = remaining.min(acct.getBalance());
        withdrawals.add(new AccountWithdrawal(acct, withdrawal));
        remaining = remaining.subtract(withdrawal);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
    }

    // 4. If still remaining, continue to taxable
    // (implies we exceeded Roth capacity)
    return withdrawals;
}
```

---

## 6. Implementation Design

### Sequencer Interface

```java
public interface AccountSequencer {

    /**
     * Orders accounts for withdrawal.
     */
    List<InvestmentAccount> sequence(Portfolio portfolio, WithdrawalContext context);

    /**
     * Returns the sequencer name for logging/display.
     */
    String getName();
}
```

### Sequencer Implementations

```java
// Simple: Taxable → Traditional → Roth
public class TaxEfficientSequencer implements AccountSequencer { }

// RMD accounts first, then tax-efficient
public class RmdFirstSequencer implements AccountSequencer {
    private final RmdCalculator rmdCalculator;
}

// Pro-rata from all accounts
public class ProRataSequencer implements AccountSequencer { }

// User-defined order
public class CustomSequencer implements AccountSequencer {
    private final List<AccountType> customOrder;
}

// Bracket-aware (advanced)
public class BracketAwareSequencer implements AccountSequencer {
    private final FederalTaxCalculator taxCalculator;
    private final BigDecimal targetBracketTop;
}
```

### Withdrawal Context Enhancement

```java
public record WithdrawalContext(
    Portfolio portfolio,
    BigDecimal totalExpenses,
    BigDecimal otherIncome,
    LocalDate date,
    int age,
    int birthYear,
    int yearsInRetirement,
    BigDecimal initialPortfolioBalance,

    // Tax-related context
    BigDecimal currentTaxableIncome,  // Income from SS, pension, etc.
    FilingStatus filingStatus,
    BigDecimal targetBracketTop,      // Optional bracket target
    boolean isSubjectToRmd,

    Map<String, Object> strategyParams
) {}
```

---

## 7. Recommendations

### For M6 Implementation

1. **Start Simple:** Implement `TaxEfficientSequencer` and `RmdFirstSequencer` first
2. **RMD Integration:** Always check RMD requirement before other sequencing
3. **Pro-Rata Option:** Some users prefer proportional withdrawals
4. **Bracket-Aware:** Make this an advanced/optional feature (M6e or future)

### Sequencer Selection Logic

```java
public AccountSequencer selectSequencer(WithdrawalContext ctx) {
    // 1. If RMDs required, use RMD-first
    if (ctx.isSubjectToRmd()) {
        return new RmdFirstSequencer(rmdCalculator);
    }

    // 2. If bracket optimization requested
    if (ctx.targetBracketTop() != null) {
        return new BracketAwareSequencer(taxCalculator, ctx.targetBracketTop());
    }

    // 3. Default: simple tax-efficient
    return new TaxEfficientSequencer();
}
```

---

## 8. Key Findings

1. **Traditional sequencing is a good default** but not optimal
2. **RMDs must be handled first** regardless of strategy
3. **Bracket filling** can significantly reduce lifetime taxes
4. **Early retirement** presents Roth conversion opportunities
5. **ACA/IRMAA cliffs** create income planning constraints
6. **Complexity trade-off:** Bracket-aware sequencing adds value but complexity

---

## 9. Design Document Updates

Based on this research, update M6a/M6e design:

1. **AccountSequencer interface** with multiple implementations
2. **RmdFirstSequencer** as default when RMDs apply
3. **WithdrawalContext** enhanced with tax-related fields
4. **TaxEfficientSequencer** as simple default
5. **BracketAwareSequencer** as optional advanced feature
6. **Tax bracket configuration** from YAML (already have FederalTaxCalculator)

---

## References

1. Kitces, M. "Tax-Efficient Retirement Withdrawal Strategies." kitces.com
2. Pfau, W. "Tax-Aware Retirement Income Planning." Retirement Researcher.
3. Reichenstein, W. "Tax-Efficient Sequencing of Accounts." Journal of Financial Planning.
4. Vanguard Research. "Tax-efficient withdrawal strategies."
5. IRS Publication 590-B. Required Minimum Distributions.
