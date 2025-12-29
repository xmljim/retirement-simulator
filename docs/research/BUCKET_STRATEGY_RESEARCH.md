# Research: Bucket Strategy

**Issue:** #221
**Status:** Complete
**Date:** December 29, 2025

---

## Overview

The bucket strategy segments retirement assets into time-based "buckets" with different risk profiles, providing psychological comfort and protection against sequence-of-returns risk.

**Original Source:** Harold Evensky, financial planner, popularized in the 1980s

---

## 1. Standard Three-Bucket Model

### Bucket 1: Short-Term (Cash Reserve)
| Attribute | Value |
|-----------|-------|
| Time Horizon | 1-2 years |
| Purpose | Cover immediate expenses |
| Asset Allocation | 100% cash, money market, short-term CDs |
| Target Size | 1-2 years of expenses |
| Expected Return | 2-4% |
| Volatility | Very Low |

### Bucket 2: Medium-Term (Income)
| Attribute | Value |
|-----------|-------|
| Time Horizon | 3-7 years |
| Purpose | Refill Bucket 1, moderate growth |
| Asset Allocation | Bonds, bond funds, balanced funds |
| Target Size | 3-5 years of expenses |
| Expected Return | 4-6% |
| Volatility | Low-Medium |

### Bucket 3: Long-Term (Growth)
| Attribute | Value |
|-----------|-------|
| Time Horizon | 8+ years |
| Purpose | Long-term growth, inflation protection |
| Asset Allocation | Stocks, equity funds, REITs |
| Target Size | Remainder of portfolio |
| Expected Return | 7-10% |
| Volatility | High |

---

## 2. Alternative Configurations

### Two-Bucket Model (Simpler)
| Bucket | Horizon | Allocation |
|--------|---------|------------|
| Short-Term | 1-3 years | Cash/Bonds |
| Long-Term | 4+ years | Stocks |

### Four-Bucket Model (More Granular)
| Bucket | Horizon | Allocation |
|--------|---------|------------|
| Immediate | 0-1 year | Cash |
| Short-Term | 2-4 years | Short bonds |
| Medium-Term | 5-10 years | Balanced |
| Long-Term | 11+ years | Equities |

### Christine Benz (Morningstar) Model
| Bucket | Horizon | Allocation | Notes |
|--------|---------|------------|-------|
| Bucket 1 | 1-2 years | Cash | Emergency + near-term |
| Bucket 2 | 3-10 years | Bonds/Balanced | Income focus |
| Bucket 3 | 11+ years | Stocks | Growth focus |

---

## 3. Refill Strategies

### Strategy A: Threshold-Based Refill
```
if bucket1.balance < bucket1.target × 0.50:
    transfer from bucket2 to bucket1 until bucket1 = target
    if bucket2 depleted:
        transfer from bucket3
```

**Parameters:**
- Refill threshold: 50% of target (configurable)
- Refill amount: To full target or partial

### Strategy B: Calendar-Based Refill (Annual)
```
annually:
    withdraw 1 year of expenses from bucket2 to bucket1
    if bucket3 had positive returns:
        rebalance bucket3 gains to bucket2
```

**Parameters:**
- Refill frequency: Annual (or semi-annual)
- Rebalance trigger: Positive returns in growth bucket

### Strategy C: Market-Based Refill
```
if bucket3.yearToDateReturn > 0:
    harvest gains to refill bucket1 and bucket2
else:
    do not refill (let bucket1 deplete naturally)
```

**Parameters:**
- Only refill when markets are up
- Protects against selling low

### Strategy D: Hybrid (Recommended)
```
annually:
    if bucket1.balance < bucket1.target × 0.75:
        if bucket3.yearToDateReturn > 5%:
            transfer gains from bucket3 → bucket2 → bucket1
        else:
            transfer from bucket2 to bucket1
```

**Combines:**
- Threshold trigger (75% depletion)
- Market awareness (prefer gains)
- Annual review cadence

---

## 4. Withdrawal Sequence

### Standard Sequence
1. **Always draw from Bucket 1 first** for monthly expenses
2. **When Bucket 1 < threshold**, trigger refill from Bucket 2
3. **When Bucket 2 < threshold**, trigger refill from Bucket 3
4. **Never skip buckets** - maintain the time-segmented structure

### Emergency Access
- If Bucket 1 depleted AND Bucket 2 depleted:
  - May access Bucket 3 directly
  - Log as "emergency withdrawal"
  - Consider this a failure scenario for analysis

---

## 5. Tax Considerations

### Account Type by Bucket

| Bucket | Preferred Account Type | Reason |
|--------|----------------------|--------|
| Short-Term | Taxable brokerage | Lowest growth, capital gains minimal |
| Medium-Term | Tax-deferred (Traditional IRA/401k) | Interest income sheltered |
| Long-Term | Roth IRA | Tax-free growth on highest return assets |

### Refill Tax Impact
- Refills from tax-deferred → realize ordinary income
- Refills from taxable → realize capital gains
- Refills within Roth → no tax impact

---

## 6. Implementation Design

### Bucket Model

```java
public record Bucket(
    String name,
    BucketType type,
    int minHorizonYears,
    int maxHorizonYears,
    BigDecimal targetBalance,
    BigDecimal currentBalance,
    BigDecimal targetAllocationPercent,
    AssetAllocation allocation
) {
    public BigDecimal getDeficitFromTarget() {
        return targetBalance.subtract(currentBalance).max(BigDecimal.ZERO);
    }

    public BigDecimal getBalanceRatio() {
        return currentBalance.divide(targetBalance, 4, RoundingMode.HALF_UP);
    }

    public boolean needsRefill(BigDecimal threshold) {
        return getBalanceRatio().compareTo(threshold) < 0;
    }
}

public enum BucketType {
    SHORT_TERM(1, 2, "Cash and equivalents"),
    MEDIUM_TERM(3, 7, "Bonds and balanced funds"),
    LONG_TERM(8, 30, "Equities and growth assets");
}
```

### Bucket Configuration

```java
public record BucketConfiguration(
    List<Bucket> buckets,
    RefillStrategy refillStrategy,
    BigDecimal refillThreshold,     // e.g., 0.50 or 0.75
    RefillTrigger refillTrigger,    // THRESHOLD, CALENDAR, MARKET, HYBRID
    int refillFrequencyMonths,      // 12 for annual
    boolean onlyRefillOnPositiveReturns
) {
    public static BucketConfiguration standard() {
        return new BucketConfiguration(
            List.of(
                Bucket.shortTerm(2),   // 2 years expenses
                Bucket.mediumTerm(5),  // 5 years expenses
                Bucket.longTerm()      // Remainder
            ),
            RefillStrategy.CASCADE,
            new BigDecimal("0.50"),
            RefillTrigger.HYBRID,
            12,
            false
        );
    }
}

public enum RefillTrigger {
    THRESHOLD,    // Refill when bucket falls below threshold
    CALENDAR,     // Refill on schedule regardless of balance
    MARKET,       // Refill only when markets positive
    HYBRID        // Threshold + market awareness
}

public enum RefillStrategy {
    CASCADE,      // Bucket 3 → Bucket 2 → Bucket 1
    DIRECT,       // Bucket 3 → Bucket 1 (skip middle)
    PROPORTIONAL  // Refill from all buckets proportionally
}
```

### Bucket Refill Calculator

```java
public interface BucketRefillCalculator {

    /**
     * Calculates transfers needed to rebalance buckets.
     */
    List<BucketTransfer> calculateRefills(
        BucketConfiguration config,
        List<Bucket> currentBuckets,
        BigDecimal bucket3YtdReturn
    );
}

public record BucketTransfer(
    BucketType source,
    BucketType destination,
    BigDecimal amount,
    String reason
) {}
```

### Bucket Withdrawal Strategy

```java
public class BucketWithdrawalStrategy implements WithdrawalStrategy {

    private final BucketConfiguration config;
    private final BucketRefillCalculator refillCalculator;

    @Override
    public WithdrawalPlan calculateWithdrawal(WithdrawalContext ctx) {
        // 1. Determine monthly need
        BigDecimal monthlyNeed = ctx.totalExpenses()
            .subtract(ctx.otherIncome());

        // 2. Check Bucket 1 balance
        Bucket shortTerm = getBucket(BucketType.SHORT_TERM);

        if (shortTerm.currentBalance().compareTo(monthlyNeed) >= 0) {
            // Simple case: withdraw from Bucket 1
            return withdrawFromBucket(shortTerm, monthlyNeed);
        }

        // 3. Bucket 1 insufficient - check refill needed
        List<BucketTransfer> refills = refillCalculator.calculateRefills(
            config, currentBuckets, getYtdReturn());

        // 4. Execute refills, then withdraw
        applyRefills(refills);
        return withdrawFromBucket(shortTerm, monthlyNeed);
    }
}
```

---

## 7. Test Scenarios

| Scenario | Expected Behavior |
|----------|-------------------|
| Year 1, all buckets full | Withdraw from Bucket 1, no refill |
| Bucket 1 at 40% target | Trigger refill from Bucket 2 |
| Bucket 2 depleted | Trigger refill from Bucket 3 |
| Market down 20% | No refill if market-aware, or refill anyway |
| Market up 30% | Harvest gains to refill Buckets 1 & 2 |
| 5 years in, Bucket 1 empty | Cascade refill, flag if Bucket 3 insufficient |

---

## 8. Key Findings

1. **Three buckets is standard** - sufficient granularity without complexity
2. **Hybrid refill strategy** recommended - combines threshold awareness with market timing
3. **Annual refill review** is most common cadence
4. **50-75% threshold** for triggering refills
5. **Tax placement matters** - structure buckets across account types
6. **Psychological benefit** is primary value - actual returns similar to systematic withdrawal

---

## 9. Design Document Updates

Based on this research, update M6c design:

1. **BucketType enum** with standard configurations
2. **Bucket record** with balance tracking and threshold checks
3. **BucketConfiguration** with refill parameters
4. **RefillTrigger and RefillStrategy** enums
5. **BucketRefillCalculator** interface
6. **BucketWithdrawalStrategy** implementation
7. **Standard presets** for common configurations

---

## References

1. Evensky, H. & Katz, D. "The New Wealth Management." McGraw-Hill.
2. Benz, C. "The Bucket Approach to Retirement Allocation." Morningstar.
3. Pfau, W. "Safety-First Retirement Planning." Retirement Researcher.
4. Kitces, M. "The Bucket Approach To Building A Retirement Paycheck." kitces.com
