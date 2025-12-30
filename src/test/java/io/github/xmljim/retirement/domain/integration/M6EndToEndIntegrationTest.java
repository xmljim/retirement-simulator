package io.github.xmljim.retirement.domain.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.calculator.AccountSequencer;
import io.github.xmljim.retirement.domain.calculator.RmdCalculator;
import io.github.xmljim.retirement.domain.calculator.SpendingOrchestrator;
import io.github.xmljim.retirement.domain.calculator.SpendingStrategy;
import io.github.xmljim.retirement.domain.calculator.StubSimulationView;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultRmdCalculator;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultSpendingOrchestrator;
import io.github.xmljim.retirement.domain.calculator.impl.GuardrailsSpendingStrategy;
import io.github.xmljim.retirement.domain.calculator.impl.IncomeGapStrategy;
import io.github.xmljim.retirement.domain.calculator.impl.RmdAwareOrchestrator;
import io.github.xmljim.retirement.domain.calculator.impl.RmdRulesTestLoader;
import io.github.xmljim.retirement.domain.calculator.impl.StaticSpendingStrategy;
import io.github.xmljim.retirement.domain.calculator.impl.TaxEfficientSequencer;
import io.github.xmljim.retirement.domain.config.GuardrailsConfiguration;
import io.github.xmljim.retirement.domain.config.RmdRules;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.value.SpendingContext;
import io.github.xmljim.retirement.domain.value.SpendingPlan;

/**
 * End-to-end integration tests for M6 distribution strategies.
 * Tests complete withdrawal flows across all strategies and scenarios.
 */
@DisplayName("M6 End-to-End Integration Tests")
class M6EndToEndIntegrationTest {

    private static final BigDecimal MILLION = new BigDecimal("1000000");
    private RmdCalculator rmdCalculator;
    private SpendingOrchestrator defaultOrchestrator;
    private SpendingOrchestrator rmdAwareOrchestrator;

    @BeforeEach
    void setUp() {
        RmdRules rules = RmdRulesTestLoader.loadFromYaml();
        rmdCalculator = new DefaultRmdCalculator(rules);
        defaultOrchestrator = new DefaultSpendingOrchestrator(rmdCalculator);
        rmdAwareOrchestrator = new RmdAwareOrchestrator(rmdCalculator);
    }

    @Nested
    @DisplayName("Full Withdrawal Flow Tests")
    class FullWithdrawalFlowTests {

        @Test
        @DisplayName("Early retiree: Static 4% with TaxEfficient sequencer, no RMD")
        void earlyRetireeStaticTaxEfficient() {
            StubSimulationView sim = StubSimulationView.builder()
                    .addAccount(StubSimulationView.createTestAccount(
                            "Brokerage", AccountType.TAXABLE_BROKERAGE, new BigDecimal("300000")))
                    .addAccount(StubSimulationView.createTestAccount(
                            "Traditional 401k", AccountType.TRADITIONAL_401K, new BigDecimal("500000")))
                    .addAccount(StubSimulationView.createTestAccount(
                            "Roth IRA", AccountType.ROTH_IRA, new BigDecimal("200000")))
                    .initialPortfolioBalance(MILLION)
                    .build();

            SpendingContext context = SpendingContext.builder()
                    .simulation(sim)
                    .date(LocalDate.of(2025, 6, 1))
                    .retirementStartDate(LocalDate.of(2025, 1, 1))
                    .totalExpenses(new BigDecimal("5000"))
                    .otherIncome(new BigDecimal("2000"))
                    .age(55)
                    .birthYear(1970)
                    .build();

            SpendingStrategy strategy = new StaticSpendingStrategy();
            AccountSequencer sequencer = new TaxEfficientSequencer();
            SpendingPlan plan = defaultOrchestrator.execute(strategy, sequencer, context);

            assertNotNull(plan);
            assertTrue(plan.meetsTarget());
            assertEquals("Static 4%", plan.strategyUsed());
            // Should withdraw from taxable (brokerage) first
            assertFalse(plan.accountWithdrawals().isEmpty());
            assertEquals("Brokerage", plan.accountWithdrawals().get(0).accountSnapshot().accountName());
        }

        @Test
        @DisplayName("Age 73 Traditional IRA: RMD satisfied first")
        void age73TraditionalIraRmdFirst() {
            StubSimulationView sim = StubSimulationView.builder()
                    .addAccount(StubSimulationView.createTestAccount(
                            "Traditional IRA", AccountType.TRADITIONAL_IRA, new BigDecimal("500000")))
                    .addAccount(StubSimulationView.createTestAccount(
                            "Roth IRA", AccountType.ROTH_IRA, new BigDecimal("200000")))
                    .initialPortfolioBalance(new BigDecimal("700000"))
                    .build();

            SpendingContext context = SpendingContext.builder()
                    .simulation(sim)
                    .date(LocalDate.of(2025, 6, 1))
                    .retirementStartDate(LocalDate.of(2020, 1, 1))
                    .totalExpenses(new BigDecimal("4000"))
                    .otherIncome(new BigDecimal("2000"))
                    .age(73)
                    .birthYear(1952)
                    .build();

            SpendingStrategy strategy = new StaticSpendingStrategy();
            SpendingPlan plan = rmdAwareOrchestrator.execute(strategy, context);

            assertNotNull(plan);
            assertTrue(plan.meetsTarget());
            assertNotNull(plan.metadata().get("rmdRequired"));
            // First withdrawal should be from Traditional IRA (RMD account)
            assertEquals("Traditional IRA", plan.accountWithdrawals().get(0).accountSnapshot().accountName());
        }

        @Test
        @DisplayName("Age 80 large RMD: RMD exceeds spending need")
        void age80LargeRmdExceedsNeed() {
            StubSimulationView sim = StubSimulationView.builder()
                    .addAccount(StubSimulationView.createTestAccount(
                            "Traditional 401k", AccountType.TRADITIONAL_401K, MILLION))
                    .initialPortfolioBalance(MILLION)
                    .build();

            SpendingContext context = SpendingContext.builder()
                    .simulation(sim)
                    .date(LocalDate.of(2025, 6, 1))
                    .retirementStartDate(LocalDate.of(2010, 1, 1))
                    .totalExpenses(new BigDecimal("3000"))
                    .otherIncome(new BigDecimal("2000"))
                    .age(80)
                    .birthYear(1945)
                    .build();

            SpendingStrategy strategy = new IncomeGapStrategy();
            SpendingPlan plan = rmdAwareOrchestrator.execute(strategy, context);

            assertNotNull(plan);
            assertEquals("true", plan.metadata().get("rmdForced"));
            BigDecimal rmdRequired = new BigDecimal((String) plan.metadata().get("rmdRequired"));
            BigDecimal strategyTarget = new BigDecimal((String) plan.metadata().get("strategyTarget"));
            assertTrue(rmdRequired.compareTo(strategyTarget) > 0);
        }
    }

    @Nested
    @DisplayName("RMD Integration Tests")
    class RmdIntegrationTests {

        @Test
        @DisplayName("RMD forces withdrawal even when no spending gap")
        void rmdForcesWithdrawalWhenNoGap() {
            StubSimulationView sim = StubSimulationView.builder()
                    .addAccount(StubSimulationView.createTestAccount(
                            "Traditional 401k", AccountType.TRADITIONAL_401K, new BigDecimal("800000")))
                    .initialPortfolioBalance(new BigDecimal("800000"))
                    .build();

            SpendingContext context = SpendingContext.builder()
                    .simulation(sim)
                    .date(LocalDate.of(2025, 6, 1))
                    .retirementStartDate(LocalDate.of(2015, 1, 1))
                    .totalExpenses(new BigDecimal("3000"))
                    .otherIncome(new BigDecimal("3500"))
                    .age(75)
                    .birthYear(1950)
                    .build();

            SpendingPlan plan = rmdAwareOrchestrator.execute(new IncomeGapStrategy(), context);

            assertNotNull(plan);
            assertTrue(plan.targetWithdrawal().compareTo(BigDecimal.ZERO) > 0);
            assertEquals("true", plan.metadata().get("rmdForced"));
        }

        @Test
        @DisplayName("Multiple RMD accounts aggregate correctly")
        void multipleRmdAccountsAggregate() {
            StubSimulationView sim = StubSimulationView.builder()
                    .addAccount(StubSimulationView.createTestAccount(
                            "Traditional 401k", AccountType.TRADITIONAL_401K, new BigDecimal("400000")))
                    .addAccount(StubSimulationView.createTestAccount(
                            "Traditional IRA", AccountType.TRADITIONAL_IRA, new BigDecimal("300000")))
                    .addAccount(StubSimulationView.createTestAccount(
                            "Roth IRA", AccountType.ROTH_IRA, new BigDecimal("300000")))
                    .initialPortfolioBalance(MILLION)
                    .build();

            SpendingContext context = SpendingContext.builder()
                    .simulation(sim)
                    .date(LocalDate.of(2025, 6, 1))
                    .retirementStartDate(LocalDate.of(2015, 1, 1))
                    .totalExpenses(new BigDecimal("6000"))
                    .otherIncome(new BigDecimal("2000"))
                    .age(76)
                    .birthYear(1949)
                    .build();

            SpendingPlan plan = rmdAwareOrchestrator.execute(new IncomeGapStrategy(), context);

            assertNotNull(plan);
            assertNotNull(plan.metadata().get("rmdRequired"));
        }
    }

    @Nested
    @DisplayName("Strategy Comparison Tests")
    class StrategyComparisonTests {

        private StubSimulationView createStandardScenario() {
            return StubSimulationView.builder()
                    .addAccount(StubSimulationView.createTestAccount(
                            "Traditional 401k", AccountType.TRADITIONAL_401K, new BigDecimal("600000")))
                    .addAccount(StubSimulationView.createTestAccount(
                            "Roth IRA", AccountType.ROTH_IRA, new BigDecimal("200000")))
                    .addAccount(StubSimulationView.createTestAccount(
                            "Brokerage", AccountType.TAXABLE_BROKERAGE, new BigDecimal("200000")))
                    .initialPortfolioBalance(MILLION)
                    .priorYearSpending(new BigDecimal("40000"))
                    .build();
        }

        @Test
        @DisplayName("Compare all strategies: $1M portfolio, $50K expenses, $25K SS")
        void compareAllStrategies() {
            StubSimulationView sim = createStandardScenario();
            SpendingContext context = SpendingContext.builder()
                    .simulation(sim)
                    .date(LocalDate.of(2025, 6, 1))
                    .retirementStartDate(LocalDate.of(2024, 1, 1))
                    .totalExpenses(new BigDecimal("4167"))
                    .otherIncome(new BigDecimal("2083"))
                    .age(65)
                    .birthYear(1960)
                    .build();

            SpendingPlan staticPlan = new StaticSpendingStrategy().calculateWithdrawal(context);
            SpendingPlan gapPlan = new IncomeGapStrategy().calculateWithdrawal(context);
            GuardrailsConfiguration gkConfig = GuardrailsConfiguration.guytonKlinger();
            SpendingPlan guardrailsPlan = new GuardrailsSpendingStrategy(gkConfig)
                    .calculateWithdrawal(context);

            assertNotNull(staticPlan);
            assertNotNull(gapPlan);
            assertNotNull(guardrailsPlan);
            assertEquals("Static 4%", staticPlan.strategyUsed());
            assertEquals("Income Gap", gapPlan.strategyUsed());
            assertTrue(guardrailsPlan.strategyUsed().contains("Guardrails"));
        }

        @Test
        @DisplayName("Static vs IncomeGap: different targets for same scenario")
        void staticVsIncomeGapTargets() {
            StubSimulationView sim = createStandardScenario();
            SpendingContext context = SpendingContext.builder()
                    .simulation(sim)
                    .date(LocalDate.of(2025, 6, 1))
                    .retirementStartDate(LocalDate.of(2024, 1, 1))
                    .totalExpenses(new BigDecimal("6000"))
                    .otherIncome(new BigDecimal("2000"))
                    .build();

            SpendingPlan staticPlan = new StaticSpendingStrategy().calculateWithdrawal(context);
            SpendingPlan gapPlan = new IncomeGapStrategy().calculateWithdrawal(context);

            BigDecimal staticTarget = staticPlan.targetWithdrawal().setScale(2, RoundingMode.HALF_UP);
            BigDecimal gapTarget = gapPlan.targetWithdrawal().setScale(2, RoundingMode.HALF_UP);

            // Static 4% of $1M = $40K/yr = $3,333/mo, capped at gap ($4K)
            // Gap = $6K - $2K = $4K
            assertTrue(gapTarget.compareTo(staticTarget) >= 0);
        }
    }

    @Nested
    @DisplayName("Multi-Year Progression Tests")
    class MultiYearProgressionTests {

        @Test
        @DisplayName("Year 1 to Year 2: portfolio grows 10%")
        void year1ToYear2Growth() {
            // Year 1
            StubSimulationView simY1 = StubSimulationView.builder()
                    .addAccount(StubSimulationView.createTestAccount(
                            "Traditional 401k", AccountType.TRADITIONAL_401K, MILLION))
                    .initialPortfolioBalance(MILLION)
                    .build();

            SpendingContext ctxY1 = SpendingContext.builder()
                    .simulation(simY1)
                    .date(LocalDate.of(2025, 1, 1))
                    .retirementStartDate(LocalDate.of(2025, 1, 1))
                    .totalExpenses(new BigDecimal("5000"))
                    .otherIncome(new BigDecimal("2000"))
                    .build();

            SpendingPlan planY1 = new StaticSpendingStrategy().calculateWithdrawal(ctxY1);

            // Year 2: portfolio grew 10%
            BigDecimal y2Balance = MILLION.multiply(new BigDecimal("1.10"));
            StubSimulationView simY2 = StubSimulationView.builder()
                    .addAccount(StubSimulationView.createTestAccount(
                            "Traditional 401k", AccountType.TRADITIONAL_401K, y2Balance))
                    .initialPortfolioBalance(MILLION)
                    .priorYearReturn(new BigDecimal("0.10"))
                    .build();

            SpendingContext ctxY2 = SpendingContext.builder()
                    .simulation(simY2)
                    .date(LocalDate.of(2026, 1, 1))
                    .retirementStartDate(LocalDate.of(2025, 1, 1))
                    .totalExpenses(new BigDecimal("5000"))
                    .otherIncome(new BigDecimal("2000"))
                    .build();

            SpendingPlan planY2 = new StaticSpendingStrategy().calculateWithdrawal(ctxY2);

            assertNotNull(planY1);
            assertNotNull(planY2);
            assertTrue(planY1.meetsTarget());
            assertTrue(planY2.meetsTarget());
        }

        @Test
        @DisplayName("Guardrails triggers adjustment on portfolio decline")
        void guardrailsTriggersOnDecline() {
            BigDecimal declinedBalance = new BigDecimal("800000");
            StubSimulationView sim = StubSimulationView.builder()
                    .addAccount(StubSimulationView.createTestAccount(
                            "Traditional 401k", AccountType.TRADITIONAL_401K, declinedBalance))
                    .initialPortfolioBalance(MILLION)
                    .priorYearSpending(new BigDecimal("52000"))
                    .priorYearReturn(new BigDecimal("-0.20"))
                    .build();

            SpendingContext context = SpendingContext.builder()
                    .simulation(sim)
                    .date(LocalDate.of(2026, 1, 1))
                    .retirementStartDate(LocalDate.of(2024, 1, 1))
                    .totalExpenses(new BigDecimal("5000"))
                    .otherIncome(new BigDecimal("1000"))
                    .build();

            GuardrailsConfiguration config = GuardrailsConfiguration.guytonKlinger();
            SpendingPlan plan = new GuardrailsSpendingStrategy(config).calculateWithdrawal(context);

            assertNotNull(plan);
            assertNotNull(plan.metadata().get("currentRate"));
        }
    }
}
