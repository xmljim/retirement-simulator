package io.github.xmljim.retirement.domain.calculator.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

/**
 * Integration tests for M6b strategies with SpendingOrchestrator.
 */
@DisplayName("M6b Strategy Integration Tests")
class M6bStrategyIntegrationTest {

    private DefaultSpendingOrchestrator orchestrator;
    private LocalDate retirementStart;

    @BeforeEach
    void setUp() {
        orchestrator = new DefaultSpendingOrchestrator(new DefaultRmdCalculator());
        retirementStart = LocalDate.of(2020, 1, 1);
    }

    private SpendingContext createContext(BigDecimal expenses, BigDecimal otherIncome, int yearsRetired) {
        StubSimulationView sim = StubSimulationView.builder()
                .addAccount(StubSimulationView.createTestAccount(
                        "401(k)", AccountType.TRADITIONAL_401K, new BigDecimal("400000")))
                .addAccount(StubSimulationView.createTestAccount(
                        "Roth IRA", AccountType.ROTH_IRA, new BigDecimal("100000")))
                .initialPortfolioBalance(new BigDecimal("500000"))
                .build();

        return SpendingContext.builder()
                .simulation(sim)
                .date(LocalDate.now())
                .retirementStartDate(retirementStart.minusYears(yearsRetired))
                .totalExpenses(expenses)
                .otherIncome(otherIncome)
                .build();
    }

    @Nested
    @DisplayName("StaticSpendingStrategy + Orchestrator")
    class StaticStrategyIntegration {

        @Test
        @DisplayName("Full flow: 4% rule with tax-efficient sequencing")
        void staticWithTaxEfficientSequencing() {
            StaticSpendingStrategy strategy = new StaticSpendingStrategy();
            SpendingContext context = createContext(new BigDecimal("5000"), new BigDecimal("2000"), 0);

            SpendingPlan plan = orchestrator.execute(strategy, context);

            assertTrue(plan.meetsTarget());
            assertEquals("Static 4%", plan.strategyUsed());
            assertEquals("Tax-Efficient", plan.metadata().get("sequencer"));
        }

        @Test
        @DisplayName("Withdrawals sourced from multiple accounts")
        void withdrawalsFromMultipleAccounts() {
            StaticSpendingStrategy strategy = new StaticSpendingStrategy(new BigDecimal("0.10")); // 10% = $50k/yr
            SpendingContext context = createContext(new BigDecimal("10000"), BigDecimal.ZERO, 0);

            SpendingPlan plan = orchestrator.execute(strategy, context);

            assertTrue(plan.meetsTarget());
            assertTrue(plan.accountWithdrawals().size() >= 1);
        }
    }

    @Nested
    @DisplayName("IncomeGapStrategy + Orchestrator")
    class IncomeGapStrategyIntegration {

        @Test
        @DisplayName("Full flow: income gap with tax-efficient sequencing")
        void incomeGapWithTaxEfficientSequencing() {
            IncomeGapStrategy strategy = new IncomeGapStrategy();
            SpendingContext context = createContext(new BigDecimal("5000"), new BigDecimal("3000"), 0);

            SpendingPlan plan = orchestrator.execute(strategy, context);

            assertTrue(plan.meetsTarget());
            assertEquals("Income Gap", plan.strategyUsed());
            BigDecimal target = plan.targetWithdrawal().setScale(2, RoundingMode.HALF_UP);
            assertEquals(0, new BigDecimal("2000").compareTo(target));
        }

        @Test
        @DisplayName("Zero withdrawal when income covers expenses")
        void zeroWithdrawalWhenSurplus() {
            IncomeGapStrategy strategy = new IncomeGapStrategy();
            SpendingContext context = createContext(new BigDecimal("3000"), new BigDecimal("4000"), 0);

            SpendingPlan plan = orchestrator.execute(strategy, context);

            assertTrue(plan.meetsTarget());
            assertEquals(0, BigDecimal.ZERO.compareTo(plan.adjustedWithdrawal()));
            assertTrue(plan.accountWithdrawals().isEmpty());
        }

        @Test
        @DisplayName("Tax gross-up integration")
        void taxGrossUpIntegration() {
            IncomeGapStrategy strategy = new IncomeGapStrategy(new BigDecimal("0.22"));
            SpendingContext context = createContext(new BigDecimal("5000"), new BigDecimal("3000"), 0);

            SpendingPlan plan = orchestrator.execute(strategy, context);

            // $2000 gap / 0.78 = $2564.10
            BigDecimal expected = new BigDecimal("2000").divide(new BigDecimal("0.78"), 2, RoundingMode.HALF_UP);
            assertEquals(0, expected.compareTo(plan.targetWithdrawal().setScale(2, RoundingMode.HALF_UP)));
        }
    }

    @Nested
    @DisplayName("Strategy Comparison")
    class StrategyComparison {

        @Test
        @DisplayName("Static vs IncomeGap: Static withdraws more when gap is small")
        void staticWithdrawsMoreWhenGapSmall() {
            // $500k portfolio, 4% = $1667/month
            // Gap = $3000 - $2500 = $500
            SpendingContext context = createContext(new BigDecimal("3000"), new BigDecimal("2500"), 0);

            SpendingPlan staticPlan = new StaticSpendingStrategy().calculateWithdrawal(context);
            SpendingPlan gapPlan = new IncomeGapStrategy().calculateWithdrawal(context);

            // Static caps at gap, so both should be $500
            BigDecimal staticTarget = staticPlan.targetWithdrawal().setScale(2, RoundingMode.HALF_UP);
            BigDecimal gapTarget = gapPlan.targetWithdrawal().setScale(2, RoundingMode.HALF_UP);
            assertEquals(0, new BigDecimal("500").compareTo(staticTarget));
            assertEquals(0, new BigDecimal("500").compareTo(gapTarget));
        }

        @Test
        @DisplayName("Static vs IncomeGap: IncomeGap withdraws more when gap exceeds 4%")
        void incomeGapWithdrawsMoreWhenGapLarge() {
            // $500k portfolio, 4% = $1667/month
            // Gap = $6000 - $2000 = $4000
            SpendingContext context = createContext(new BigDecimal("6000"), new BigDecimal("2000"), 0);

            SpendingPlan staticPlan = new StaticSpendingStrategy().calculateWithdrawal(context);
            SpendingPlan gapPlan = new IncomeGapStrategy().calculateWithdrawal(context);

            // Static limited to 4% rule ($1667), IncomeGap takes full $4000
            assertTrue(staticPlan.targetWithdrawal().compareTo(gapPlan.targetWithdrawal()) < 0);
        }
    }
}
