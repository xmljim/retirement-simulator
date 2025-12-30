package io.github.xmljim.retirement.domain.calculator.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.calculator.StubSimulationView;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.value.SpendingContext;
import io.github.xmljim.retirement.domain.value.SpendingPlan;

@DisplayName("StaticSpendingStrategy Tests")
class StaticSpendingStrategyTest {

    private StaticSpendingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new StaticSpendingStrategy();
    }

    private SpendingContext createContext(BigDecimal initialBalance, BigDecimal currentBalance,
                                           int yearsInRetirement) {
        // Default: $10,000 expenses, $0 other income = $10,000 gap (won't cap typical 4% withdrawals)
        return createContext(initialBalance, currentBalance, yearsInRetirement,
                new BigDecimal("10000"), BigDecimal.ZERO);
    }

    private SpendingContext createContext(BigDecimal initialBalance, BigDecimal currentBalance,
                                           int yearsInRetirement, BigDecimal expenses, BigDecimal otherIncome) {
        LocalDate retirementStart = LocalDate.now().minusYears(yearsInRetirement);
        StubSimulationView simulation = StubSimulationView.builder()
                .addAccount(StubSimulationView.createTestAccount(
                        "Portfolio", AccountType.TRADITIONAL_401K, currentBalance))
                .initialPortfolioBalance(initialBalance)
                .build();

        return SpendingContext.builder()
                .simulation(simulation)
                .date(LocalDate.now())
                .retirementStartDate(retirementStart)
                .totalExpenses(expenses)
                .otherIncome(otherIncome)
                .build();
    }

    @Nested
    @DisplayName("Year 1 Calculations")
    class Year1Tests {

        @Test
        @DisplayName("Should calculate 4% of $1M as $40k/year = $3,333.33/month")
        void calculates4PercentOfOneMillion() {
            SpendingContext context = createContext(
                    new BigDecimal("1000000"),
                    new BigDecimal("1000000"),
                    0  // Year 1 (0 years elapsed)
            );

            SpendingPlan plan = strategy.calculateWithdrawal(context);

            // $1,000,000 * 0.04 / 12 = $3,333.33
            BigDecimal expected = new BigDecimal("3333.3333333333");
            assertEquals(0, expected.compareTo(plan.targetWithdrawal().setScale(10, RoundingMode.HALF_UP)));
            assertTrue(plan.meetsTarget());
        }

        @Test
        @DisplayName("Should calculate 4% of $500k as $20k/year = $1,666.67/month")
        void calculates4PercentOf500k() {
            SpendingContext context = createContext(
                    new BigDecimal("500000"),
                    new BigDecimal("500000"),
                    0
            );

            SpendingPlan plan = strategy.calculateWithdrawal(context);

            // $500,000 * 0.04 / 12 = $1,666.67
            BigDecimal expected = new BigDecimal("1666.67");
            assertEquals(0, expected.compareTo(plan.targetWithdrawal().setScale(2, RoundingMode.HALF_UP)));
        }
    }

    @Nested
    @DisplayName("Inflation Adjustment Tests")
    class InflationAdjustmentTests {

        @Test
        @DisplayName("Year 5 should include 4 years of inflation")
        void year5InflationAdjustment() {
            SpendingContext context = createContext(
                    new BigDecimal("1000000"),
                    new BigDecimal("1100000"),
                    4  // 4 years elapsed = year 5
            );

            SpendingPlan plan = strategy.calculateWithdrawal(context);

            // Year 1: $40,000
            // Year 5: $40,000 * (1.025)^4 = $40,000 * 1.10381... = $44,152.52
            // Monthly: $44,152.52 / 12 = $3,679.38
            BigDecimal year1Annual = new BigDecimal("40000");
            BigDecimal inflationMultiplier = MathUtils.pow(new BigDecimal("1.025"), 4, 10, RoundingMode.HALF_UP);
            BigDecimal expectedAnnual = year1Annual.multiply(inflationMultiplier);
            BigDecimal expectedMonthly = expectedAnnual.divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);

            assertEquals(0, expectedMonthly.compareTo(plan.targetWithdrawal().setScale(2, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Year 20 should include 19 years of inflation")
        void year20InflationAdjustment() {
            SpendingContext context = createContext(
                    new BigDecimal("1000000"),
                    new BigDecimal("800000"),
                    19  // 19 years elapsed = year 20
            );

            SpendingPlan plan = strategy.calculateWithdrawal(context);

            // Year 1: $40,000
            // Year 20: $40,000 * (1.025)^19 = $40,000 * 1.5987... = $63,946.71
            // Monthly: $63,946.71 / 12 = $5,328.89
            BigDecimal year1Annual = new BigDecimal("40000");
            BigDecimal inflationMultiplier = MathUtils.pow(new BigDecimal("1.025"), 19, 10, RoundingMode.HALF_UP);
            BigDecimal expectedAnnual = year1Annual.multiply(inflationMultiplier);
            BigDecimal expectedMonthly = expectedAnnual.divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);

            assertEquals(0, expectedMonthly.compareTo(plan.targetWithdrawal().setScale(2, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should not adjust for inflation when disabled")
        void noInflationAdjustmentWhenDisabled() {
            StaticSpendingStrategy noInflationStrategy = new StaticSpendingStrategy(
                    new BigDecimal("0.04"),
                    new BigDecimal("0.025"),
                    false  // Disable inflation adjustment
            );

            SpendingContext context = createContext(
                    new BigDecimal("1000000"),
                    new BigDecimal("1000000"),
                    10  // Year 11
            );

            SpendingPlan plan = noInflationStrategy.calculateWithdrawal(context);

            // Should still be year 1 amount: $40,000 / 12 = $3,333.33
            BigDecimal expected = new BigDecimal("3333.33");
            assertEquals(0, expected.compareTo(plan.targetWithdrawal().setScale(2, RoundingMode.HALF_UP)));
        }
    }

    @Nested
    @DisplayName("Custom Rate Tests")
    class CustomRateTests {

        @Test
        @DisplayName("Should support 3% withdrawal rate")
        void supports3PercentRate() {
            StaticSpendingStrategy strategy3 = new StaticSpendingStrategy(new BigDecimal("0.03"));

            SpendingContext context = createContext(
                    new BigDecimal("1000000"),
                    new BigDecimal("1000000"),
                    0
            );

            SpendingPlan plan = strategy3.calculateWithdrawal(context);

            // $1,000,000 * 0.03 / 12 = $2,500
            BigDecimal expected = new BigDecimal("2500.00");
            assertEquals(0, expected.compareTo(plan.targetWithdrawal().setScale(2, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should support 5% withdrawal rate")
        void supports5PercentRate() {
            StaticSpendingStrategy strategy5 = new StaticSpendingStrategy(
                    new BigDecimal("0.05"),
                    new BigDecimal("0.03"),  // 3% inflation
                    true
            );

            SpendingContext context = createContext(
                    new BigDecimal("1000000"),
                    new BigDecimal("1000000"),
                    0
            );

            SpendingPlan plan = strategy5.calculateWithdrawal(context);

            // $1,000,000 * 0.05 / 12 = $4,166.67
            BigDecimal expected = new BigDecimal("4166.67");
            assertEquals(0, expected.compareTo(plan.targetWithdrawal().setScale(2, RoundingMode.HALF_UP)));
        }
    }

    @Nested
    @DisplayName("Income Gap Tests")
    class IncomeGapTests {

        @Test
        @DisplayName("Should cap withdrawal at income gap when 4% exceeds need")
        void capsAtIncomeGap() {
            // $1M portfolio = $3,333/month 4% withdrawal
            // But only $5,000 expenses - $3,000 SS = $2,000 gap
            SpendingContext context = createContext(
                    new BigDecimal("1000000"),
                    new BigDecimal("1000000"),
                    0,
                    new BigDecimal("5000"),   // expenses
                    new BigDecimal("3000")    // other income (SS)
            );

            SpendingPlan plan = strategy.calculateWithdrawal(context);

            // Should withdraw $2,000 (the gap), not $3,333 (4% rule)
            assertEquals(0, new BigDecimal("2000").compareTo(
                    plan.targetWithdrawal().setScale(2, RoundingMode.HALF_UP)));
            assertEquals(0, new BigDecimal("2000").compareTo(
                    plan.adjustedWithdrawal().setScale(2, RoundingMode.HALF_UP)));
            assertTrue(plan.meetsTarget());
        }

        @Test
        @DisplayName("Should use 4% rule when gap exceeds it")
        void uses4PercentWhenGapIsLarger() {
            // $500k portfolio = $1,667/month 4% withdrawal
            // Expenses $6,000 - $2,000 SS = $4,000 gap (larger than 4% amount)
            SpendingContext context = createContext(
                    new BigDecimal("500000"),
                    new BigDecimal("500000"),
                    0,
                    new BigDecimal("6000"),
                    new BigDecimal("2000")
            );

            SpendingPlan plan = strategy.calculateWithdrawal(context);

            // Should withdraw $1,666.67 (4% rule), not $4,000 (the gap)
            assertEquals(0, new BigDecimal("1666.67").compareTo(
                    plan.targetWithdrawal().setScale(2, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should withdraw zero when other income covers expenses")
        void zeroWithdrawalWhenIncomeCoversExpenses() {
            // Expenses $3,000, SS $4,000 = gap is 0 (income exceeds expenses)
            SpendingContext context = createContext(
                    new BigDecimal("1000000"),
                    new BigDecimal("1000000"),
                    0,
                    new BigDecimal("3000"),
                    new BigDecimal("4000")
            );

            SpendingPlan plan = strategy.calculateWithdrawal(context);

            assertEquals(0, BigDecimal.ZERO.compareTo(plan.targetWithdrawal()));
            assertEquals(0, BigDecimal.ZERO.compareTo(plan.adjustedWithdrawal()));
            assertTrue(plan.meetsTarget());
        }

        @Test
        @DisplayName("Should include income gap in metadata")
        void includesIncomeGapInMetadata() {
            SpendingContext context = createContext(
                    new BigDecimal("1000000"),
                    new BigDecimal("1000000"),
                    0,
                    new BigDecimal("5000"),
                    new BigDecimal("3000")
            );

            SpendingPlan plan = strategy.calculateWithdrawal(context);

            assertEquals("2000.00", plan.metadata().get("incomeGap"));
            assertEquals("3333.33", plan.metadata().get("ruleBasedMonthly"));
        }
    }

    @Nested
    @DisplayName("Portfolio Constraint Tests")
    class PortfolioConstraintTests {

        @Test
        @DisplayName("Should cap withdrawal at current balance when insufficient")
        void capsAtCurrentBalance() {
            SpendingContext context = createContext(
                    new BigDecimal("1000000"),
                    new BigDecimal("1000"),  // Only $1,000 left
                    0
            );

            SpendingPlan plan = strategy.calculateWithdrawal(context);

            // Target is $3,333.33 but only $1,000 available
            assertEquals(0, new BigDecimal("3333.33").compareTo(
                    plan.targetWithdrawal().setScale(2, RoundingMode.HALF_UP)));
            assertEquals(0, new BigDecimal("1000").compareTo(plan.adjustedWithdrawal()));
            assertFalse(plan.meetsTarget());
        }

        @Test
        @DisplayName("Should return zero when portfolio is empty")
        void returnsZeroForEmptyPortfolio() {
            SpendingContext context = createContext(
                    new BigDecimal("1000000"),
                    BigDecimal.ZERO,
                    0
            );

            SpendingPlan plan = strategy.calculateWithdrawal(context);

            assertEquals(0, BigDecimal.ZERO.compareTo(plan.adjustedWithdrawal()));
            assertFalse(plan.meetsTarget());
        }
    }

    @Nested
    @DisplayName("Metadata Tests")
    class MetadataTests {

        @Test
        @DisplayName("Should include strategy configuration in metadata")
        void includesConfigInMetadata() {
            SpendingContext context = createContext(
                    new BigDecimal("1000000"),
                    new BigDecimal("1000000"),
                    5
            );

            SpendingPlan plan = strategy.calculateWithdrawal(context);

            assertEquals("0.04", plan.metadata().get("withdrawalRate"));
            assertEquals("0.025", plan.metadata().get("inflationRate"));
            assertEquals("true", plan.metadata().get("adjustForInflation"));
            assertEquals("5", plan.metadata().get("yearsInRetirement"));
        }

        @Test
        @DisplayName("Should include calculated amounts in metadata")
        void includesAmountsInMetadata() {
            SpendingContext context = createContext(
                    new BigDecimal("1000000"),
                    new BigDecimal("1000000"),
                    0
            );

            SpendingPlan plan = strategy.calculateWithdrawal(context);

            assertEquals("40000.00", plan.metadata().get("year1AnnualAmount"));
            assertEquals("40000.00", plan.metadata().get("currentAnnualAmount"));
        }
    }

    @Nested
    @DisplayName("Strategy Properties Tests")
    class StrategyPropertiesTests {

        @Test
        @DisplayName("Should have descriptive name")
        void hasDescriptiveName() {
            assertEquals("Static 4%", strategy.getName());
        }

        @Test
        @DisplayName("Should have description with inflation info")
        void hasDescriptionWithInflation() {
            String description = strategy.getDescription();
            assertTrue(description.contains("4.0%"));
            assertTrue(description.contains("2.5%"));
            assertTrue(description.contains("inflation"));
        }

        @Test
        @DisplayName("Should not be dynamic")
        void isNotDynamic() {
            assertFalse(strategy.isDynamic());
        }

        @Test
        @DisplayName("Should not require prior year state")
        void doesNotRequirePriorYearState() {
            assertFalse(strategy.requiresPriorYearState());
        }

        @Test
        @DisplayName("Should expose configuration via getters")
        void exposesConfiguration() {
            assertEquals(0, new BigDecimal("0.04").compareTo(strategy.getWithdrawalRate()));
            assertEquals(0, new BigDecimal("0.025").compareTo(strategy.getInflationRate()));
            assertTrue(strategy.isAdjustForInflation());
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Default constructor uses 4% and 2.5%")
        void defaultConstructorUsesDefaults() {
            StaticSpendingStrategy defaultStrategy = new StaticSpendingStrategy();
            assertEquals(0, new BigDecimal("0.04").compareTo(defaultStrategy.getWithdrawalRate()));
            assertEquals(0, new BigDecimal("0.025").compareTo(defaultStrategy.getInflationRate()));
            assertTrue(defaultStrategy.isAdjustForInflation());
        }

        @Test
        @DisplayName("Single-arg constructor uses default inflation")
        void singleArgUsesDefaultInflation() {
            StaticSpendingStrategy customRate = new StaticSpendingStrategy(new BigDecimal("0.035"));
            assertEquals(0, new BigDecimal("0.035").compareTo(customRate.getWithdrawalRate()));
            assertEquals(0, new BigDecimal("0.025").compareTo(customRate.getInflationRate()));
        }

        @Test
        @DisplayName("Null rates fall back to defaults")
        void nullRatesFallbackToDefaults() {
            StaticSpendingStrategy nullRates = new StaticSpendingStrategy(null, null, true);
            assertEquals(0, new BigDecimal("0.04").compareTo(nullRates.getWithdrawalRate()));
            assertEquals(0, new BigDecimal("0.025").compareTo(nullRates.getInflationRate()));
        }
    }
}
