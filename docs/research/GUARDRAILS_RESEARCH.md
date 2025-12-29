# Research: Guardrails Withdrawal Strategies

**Issue:** #220
**Status:** Complete
**Date:** December 29, 2025

---

## Overview

Guardrails strategies dynamically adjust withdrawals based on portfolio performance, providing flexibility while protecting against sequence-of-returns risk.

---

## 1. Guyton-Klinger Decision Rules (2006)

**Source:** Jonathan Guyton & William Klinger, "Decision Rules and Maximum Initial Withdrawal Rates," Journal of Financial Planning, October 2006

### The Four Decision Rules

#### Rule 1: Portfolio Management Rule
- **Purpose:** Prevent selling assets at a loss during down markets
- **Rule:** In years with negative returns, do not take withdrawals from asset classes that declined
- **Effect:** Forces withdrawals from cash/bonds during equity downturns

#### Rule 2: Withdrawal Rule
- **Purpose:** Maintain stable inflation-adjusted spending
- **Rule:** Increase withdrawals by inflation each year, BUT skip the inflation increase in any year the portfolio has negative returns AND the current withdrawal rate exceeds the initial rate
- **Effect:** Spending stays flat (no inflation bump) during bad years

#### Rule 3: Capital Preservation Rule (Lower Guardrail)
- **Trigger:** Current withdrawal rate exceeds initial rate by 20% (e.g., 5% initial → 6% current)
- **Action:** Reduce withdrawal by 10%
- **Purpose:** Prevent portfolio depletion during extended downturns
- **Example:** Initial 5% rate. If portfolio drops such that withdrawal rate hits 6%, cut spending by 10%

#### Rule 4: Prosperity Rule (Upper Guardrail)
- **Trigger:** Current withdrawal rate falls below initial rate by 20% (e.g., 5% initial → 4% current)
- **Action:** Increase withdrawal by 10%
- **Purpose:** Allow spending increases when portfolio grows significantly
- **Example:** Initial 5% rate. If portfolio grows such that withdrawal rate drops to 4%, raise spending by 10%

### Key Parameters (Guyton-Klinger)

| Parameter | Default Value |
|-----------|---------------|
| Initial withdrawal rate | 5.2% - 5.6% |
| Upper guardrail trigger | Initial rate × 0.80 |
| Lower guardrail trigger | Initial rate × 1.20 |
| Adjustment amount | 10% (both directions) |
| Inflation adjustment | CPI, but skipped per Rule 2 |

### Constraints
- Capital Preservation Rule does NOT apply in final 15 years
- Assumes diversified portfolio (65% equity / 35% fixed)

---

## 2. Vanguard Dynamic Spending

**Source:** Vanguard Research, "From assets to income: A goals-based approach to retirement spending"

### Approach
Ceiling-and-floor system that limits annual spending changes.

### Rules

1. **Base Calculation:** Start with percentage of current portfolio value
2. **Ceiling:** Maximum increase = prior year spending + 5%
3. **Floor:** Maximum decrease = prior year spending - 2.5%

### Formula
```
target_spending = portfolio_balance × withdrawal_rate
ceiling = prior_spending × 1.05
floor = prior_spending × 0.975

if target_spending > ceiling:
    actual_spending = ceiling
elif target_spending < floor:
    actual_spending = floor
else:
    actual_spending = target_spending
```

### Key Parameters (Vanguard)

| Parameter | Default Value |
|-----------|---------------|
| Initial withdrawal rate | 4% - 5% |
| Ceiling (max increase) | +5% |
| Floor (max decrease) | -2.5% |
| Rebalancing | Annual |

### Characteristics
- More frequent but smaller adjustments than Guyton-Klinger
- Asymmetric: easier to increase than decrease
- Smooths spending volatility

---

## 3. Kitces Ratcheting

**Source:** Michael Kitces, "The Ratcheting Safe Withdrawal Rate"

### Approach
One-way ratchet that only increases spending (never decreases).

### Rules

1. **Initial Rate:** Standard 4% of initial portfolio
2. **Ratchet Trigger:** Portfolio grows to 150% of initial value
3. **Ratchet Amount:** Increase spending by 10%
4. **Frequency Cap:** Maximum one increase per 3 years
5. **Floor:** Never reduce nominal spending

### Formula
```
if portfolio_value >= initial_value × 1.50:
    if years_since_last_ratchet >= 3:
        spending = spending × 1.10
        reset ratchet timer
```

### Key Parameters (Kitces)

| Parameter | Default Value |
|-----------|---------------|
| Initial withdrawal rate | 4% |
| Growth trigger | 50% above initial |
| Ratchet increase | 10% |
| Minimum years between ratchets | 3 |
| Spending floor | Initial spending (inflation-adjusted) |

### Characteristics
- Psychologically easier (no cuts)
- More conservative overall
- Good for those with guaranteed income floor

---

## 4. Comparison Table

| Feature | Guyton-Klinger | Vanguard | Kitces |
|---------|----------------|----------|--------|
| **Spending Cuts?** | Yes (10%) | Yes (up to 2.5%) | No |
| **Spending Increases?** | Yes (10%) | Yes (up to 5%) | Yes (10%) |
| **Trigger Type** | Rate-based | Amount-based | Portfolio value |
| **Adjustment Frequency** | Annual check | Annual check | 3-year minimum |
| **Complexity** | High (4 rules) | Medium | Low |
| **Initial Rate** | 5.2-5.6% | 4-5% | 4% |
| **Best For** | Flexible retirees | Balanced approach | Risk-averse |

---

## 5. Implementation Recommendation

### Recommended Approach: Configurable Framework

Rather than implementing a single approach, create a **configurable guardrails system** that can model any of the above:

```java
public record GuardrailsConfiguration(
    // Core parameters
    BigDecimal initialWithdrawalRate,
    BigDecimal inflationRate,

    // Upper guardrail (prosperity)
    BigDecimal upperThresholdMultiplier,  // 0.80 for Guyton-Klinger
    BigDecimal increaseAdjustment,        // 0.10 (10%)

    // Lower guardrail (capital preservation)
    BigDecimal lowerThresholdMultiplier,  // 1.20 for Guyton-Klinger
    BigDecimal decreaseAdjustment,        // 0.10 (10%)

    // Constraints
    BigDecimal absoluteFloor,             // Minimum spending (essential expenses)
    BigDecimal absoluteCeiling,           // Maximum spending (lifestyle cap)

    // Behavior flags
    boolean allowSpendingCuts,            // false for Kitces
    boolean skipInflationOnDownYears,     // Guyton-Klinger Rule 2
    int minimumYearsBetweenRatchets,      // 3 for Kitces, 1 for others
    int yearsBeforeCapPreservationEnds    // 15 for Guyton-Klinger
) {}
```

### Preset Configurations

```java
public static GuardrailsConfiguration guytonKlinger() {
    return new GuardrailsConfiguration(
        new BigDecimal("0.052"),   // 5.2% initial
        new BigDecimal("0.025"),   // 2.5% inflation
        new BigDecimal("0.80"),    // Upper: rate drops to 80% of initial
        new BigDecimal("0.10"),    // Increase by 10%
        new BigDecimal("1.20"),    // Lower: rate rises to 120% of initial
        new BigDecimal("0.10"),    // Decrease by 10%
        null,                      // No absolute floor
        null,                      // No absolute ceiling
        true,                      // Allow cuts
        true,                      // Skip inflation on down years
        1,                         // Can adjust every year
        15                         // No cap preservation in final 15 years
    );
}

public static GuardrailsConfiguration vanguardDynamic() {
    return new GuardrailsConfiguration(
        new BigDecimal("0.04"),    // 4% initial
        new BigDecimal("0.025"),   // 2.5% inflation
        null,                      // Uses ceiling instead
        new BigDecimal("0.05"),    // Max +5% increase
        null,                      // Uses floor instead
        new BigDecimal("0.025"),   // Max -2.5% decrease
        null,                      // No absolute floor
        null,                      // No absolute ceiling
        true,                      // Allow cuts (limited)
        false,                     // Normal inflation
        1,                         // Annual adjustments
        0                          // N/A
    );
}

public static GuardrailsConfiguration kitcesRatcheting() {
    return new GuardrailsConfiguration(
        new BigDecimal("0.04"),    // 4% initial
        new BigDecimal("0.025"),   // 2.5% inflation
        new BigDecimal("0.667"),   // Trigger when rate drops to 2.67% (50% growth)
        new BigDecimal("0.10"),    // Increase by 10%
        null,                      // No lower guardrail
        BigDecimal.ZERO,           // No decreases
        null,                      // Floor is inflation-adjusted initial
        null,                      // No ceiling
        false,                     // NO cuts allowed
        false,                     // Normal inflation
        3,                         // 3 years between ratchets
        0                          // N/A
    );
}
```

---

## 6. Test Scenarios

| Scenario | Guyton-Klinger | Vanguard | Kitces |
|----------|----------------|----------|--------|
| Year 1: 5% return | Normal + inflation | Normal + inflation | Normal + inflation |
| Year 2: -20% return | Skip inflation, maybe cut 10% | Cut up to 2.5% | No change |
| Year 3: +30% return | Maybe raise 10% | Raise up to 5% | Check for 50% trigger |
| Portfolio doubles | Raise 10% | Limited to +5%/year | Raise 10% |
| Portfolio halves | Cut 10% | Limited to -2.5%/year | No cut (floor) |

---

## 7. Design Document Updates

Based on this research, update M6 design:

1. **GuardrailsConfiguration** should be highly configurable
2. **Preset factory methods** for common approaches
3. **State tracking** needed for:
   - Initial portfolio value (Kitces)
   - Prior year spending (all)
   - Years since last ratchet (Kitces)
   - Current withdrawal rate (Guyton-Klinger)
4. **Vanguard approach** uses ceiling/floor differently - may need separate implementation or mode flag

---

## References

1. Guyton, J. & Klinger, W. (2006). "Decision Rules and Maximum Initial Withdrawal Rates." Journal of Financial Planning.
2. Vanguard Research. "From assets to income: A goals-based approach to retirement spending."
3. Kitces, M. "The Ratcheting Safe Withdrawal Rate." kitces.com
4. Bengen, W. (1994). "Determining Withdrawal Rates Using Historical Data." Journal of Financial Planning.
