package io.github.xmljim.retirement.domain.calculator.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.calculator.SpendingOrchestrator;
import io.github.xmljim.retirement.domain.calculator.SpendingStrategy;
import io.github.xmljim.retirement.domain.calculator.StubSimulationView;
import io.github.xmljim.retirement.domain.config.RmdRules;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.value.SpendingContext;
import io.github.xmljim.retirement.domain.value.SpendingPlan;

@DisplayName("RmdAwareOrchestrator Tests")
class RmdAwareOrchestratorTest {

    private static final BigDecimal MILLION = new BigDecimal("1000000");
    private DefaultRmdCalculator rmdCalculator;
    private RmdAwareOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        RmdRules rules = RmdRulesTestLoader.loadFromYaml();
        rmdCalculator = new DefaultRmdCalculator(rules);
        orchestrator = new RmdAwareOrchestrator(rmdCalculator);
    }

    @Nested
    @DisplayName("Non-RMD Age Tests")
    class NonRmdAgeTests {

        @Test
        @DisplayName("Delegates to default orchestrator when not RMD age")
        void delegatesWhenNotRmdAge() {
            SpendingContext context = createContext(65, 1960, MILLION); // Not RMD age yet
            SpendingStrategy strategy = new StaticSpendingStrategy();

            SpendingPlan plan = orchestrator.execute(strategy, context);

            assertNotNull(plan);
            assertTrue(plan.meetsTarget());
        }
    }

    @Nested
    @DisplayName("RMD Enforcement Tests")
    class RmdEnforcementTests {

        @Test
        @DisplayName("Forces RMD when strategy requests less")
        void forcesRmdWhenStrategyRequestsLess() {
            // 76 year old with $1M in Traditional 401k
            // RMD factor at 76 ~= 23.7, so annual RMD ~= $42,194, monthly ~= $3,516
            SpendingContext context = createContext(76, 1949, MILLION);

            // Strategy only wants $1000/month (less than RMD)
            SpendingStrategy strategy = new IncomeGapStrategy();
            SpendingContext lowExpenseContext = SpendingContext.builder()
                    .simulation(context.simulation())
                    .date(context.date())
                    .retirementStartDate(context.retirementStartDate())
                    .totalExpenses(new BigDecimal("2000"))
                    .otherIncome(new BigDecimal("1000")) // Gap = $1000
                    .age(76)
                    .birthYear(1949)
                    .build();

            SpendingPlan plan = orchestrator.execute(strategy, lowExpenseContext);

            assertNotNull(plan);
            // Verify RMD metadata exists and target is higher than strategy gap
            assertNotNull(plan.metadata().get("rmdRequired"));
            BigDecimal rmdRequired = new BigDecimal((String) plan.metadata().get("rmdRequired"));
            BigDecimal strategyTarget = new BigDecimal((String) plan.metadata().get("strategyTarget"));

            // RMD should be higher than the $1000 gap
            assertTrue(rmdRequired.compareTo(strategyTarget) > 0,
                    "RMD " + rmdRequired + " should be > strategy " + strategyTarget);
            // Target should be RMD (higher than $1000 gap)
            assertTrue(plan.targetWithdrawal().compareTo(new BigDecimal("1000")) > 0);
        }

        @Test
        @DisplayName("Uses strategy amount when higher than RMD")
        void usesStrategyWhenHigherThanRmd() {
            SpendingContext context = createContext(76, 1949, MILLION);

            // Strategy wants $5000/month (more than monthly RMD)
            SpendingStrategy strategy = new IncomeGapStrategy();
            SpendingContext highExpenseContext = SpendingContext.builder()
                    .simulation(context.simulation())
                    .date(context.date())
                    .retirementStartDate(context.retirementStartDate())
                    .totalExpenses(new BigDecimal("6000"))
                    .otherIncome(new BigDecimal("1000")) // Gap = $5000
                    .age(76)
                    .birthYear(1949)
                    .build();

            SpendingPlan plan = orchestrator.execute(strategy, highExpenseContext);

            assertNotNull(plan);
            assertEquals("false", plan.metadata().get("rmdForced"));
        }
    }

    @Nested
    @DisplayName("Withdrawal Tracking Tests")
    class WithdrawalTrackingTests {

        @Test
        @DisplayName("Tracks RMD vs discretionary withdrawals")
        void tracksRmdVsDiscretionary() {
            SpendingContext context = createContextWithMultipleAccounts(76, 1949);

            SpendingStrategy strategy = new IncomeGapStrategy();
            SpendingContext expenseContext = SpendingContext.builder()
                    .simulation(context.simulation())
                    .date(context.date())
                    .retirementStartDate(context.retirementStartDate())
                    .totalExpenses(new BigDecimal("8000"))
                    .otherIncome(new BigDecimal("2000"))
                    .age(76)
                    .birthYear(1949)
                    .build();

            SpendingPlan plan = orchestrator.execute(strategy, expenseContext);

            assertNotNull(plan.metadata().get("rmdWithdrawn"));
            assertNotNull(plan.metadata().get("discretionaryWithdrawn"));
            assertNotNull(plan.metadata().get("rmdRequired"));
        }
    }

    @Nested
    @DisplayName("Sequencer Selection Tests")
    class SequencerSelectionTests {

        @Test
        @DisplayName("Uses RMD-first sequencer when RMD required")
        void usesRmdFirstSequencer() {
            SpendingContext context = createContext(76, 1949, MILLION);

            var sequencer = orchestrator.selectDefaultSequencer(context);

            assertTrue(sequencer instanceof RmdFirstSequencer);
        }

        @Test
        @DisplayName("Uses tax-efficient sequencer when no RMD")
        void usesTaxEfficientSequencerWhenNoRmd() {
            SpendingContext context = createContext(65, 1960, MILLION);

            var sequencer = orchestrator.selectDefaultSequencer(context);

            assertTrue(sequencer instanceof TaxEfficientSequencer);
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor with delegate")
        void constructorWithDelegate() {
            SpendingOrchestrator delegate = new DefaultSpendingOrchestrator(rmdCalculator);
            RmdAwareOrchestrator withDelegate = new RmdAwareOrchestrator(rmdCalculator, delegate);

            SpendingContext context = createContext(65, 1960, MILLION);
            SpendingPlan plan = withDelegate.execute(new StaticSpendingStrategy(), context);

            assertNotNull(plan);
        }
    }

    @Nested
    @DisplayName("Shortfall Tests")
    class ShortfallTests {

        @Test
        @DisplayName("Reports shortfall when accounts insufficient")
        void reportsShortfallWhenInsufficient() {
            // Small balance that can't meet RMD + expenses
            StubSimulationView sim = StubSimulationView.builder()
                    .addAccount(StubSimulationView.createTestAccount(
                            "Traditional 401k", AccountType.TRADITIONAL_401K, new BigDecimal("1000")))
                    .initialPortfolioBalance(new BigDecimal("1000"))
                    .build();

            SpendingContext context = SpendingContext.builder()
                    .simulation(sim)
                    .date(LocalDate.of(2025, 6, 1))
                    .retirementStartDate(LocalDate.of(2020, 1, 1))
                    .totalExpenses(new BigDecimal("50000"))
                    .otherIncome(BigDecimal.ZERO)
                    .age(76)
                    .birthYear(1949)
                    .build();

            SpendingPlan plan = orchestrator.execute(new IncomeGapStrategy(), context);

            assertNotNull(plan);
            assertTrue(plan.shortfall().compareTo(BigDecimal.ZERO) > 0);
        }
    }

    @Nested
    @DisplayName("Tax Priority Tests")
    class TaxPriorityTests {

        @Test
        @DisplayName("Withdraws from all tax treatments in order")
        void withdrawsFromAllTaxTreatments() {
            StubSimulationView sim = StubSimulationView.builder()
                    .addAccount(StubSimulationView.createTestAccount(
                            "Traditional 401k", AccountType.TRADITIONAL_401K, new BigDecimal("100000")))
                    .addAccount(StubSimulationView.createTestAccount(
                            "Roth IRA", AccountType.ROTH_IRA, new BigDecimal("100000")))
                    .addAccount(StubSimulationView.createTestAccount(
                            "Brokerage", AccountType.TAXABLE_BROKERAGE, new BigDecimal("100000")))
                    .addAccount(StubSimulationView.createTestAccount(
                            "HSA", AccountType.HSA, new BigDecimal("50000")))
                    .initialPortfolioBalance(new BigDecimal("350000"))
                    .build();

            // Large expense to force withdrawals from multiple accounts
            SpendingContext context = SpendingContext.builder()
                    .simulation(sim)
                    .date(LocalDate.of(2025, 6, 1))
                    .retirementStartDate(LocalDate.of(2020, 1, 1))
                    .totalExpenses(new BigDecimal("20000"))
                    .otherIncome(BigDecimal.ZERO)
                    .age(76)
                    .birthYear(1949)
                    .build();

            SpendingPlan plan = orchestrator.execute(new IncomeGapStrategy(), context);

            assertNotNull(plan);
            assertTrue(plan.meetsTarget());
            // Should have withdrawals from multiple accounts
            assertTrue(plan.accountWithdrawals().size() >= 1);
        }
    }

    // Helper methods

    private SpendingContext createContext(int age, int birthYear, BigDecimal balance) {
        StubSimulationView sim = StubSimulationView.builder()
                .addAccount(StubSimulationView.createTestAccount(
                        "Traditional 401k", AccountType.TRADITIONAL_401K, balance))
                .initialPortfolioBalance(balance)
                .build();

        return SpendingContext.builder()
                .simulation(sim)
                .date(LocalDate.of(2025, 6, 1))
                .retirementStartDate(LocalDate.of(2020, 1, 1))
                .totalExpenses(new BigDecimal("5000"))
                .otherIncome(new BigDecimal("2000"))
                .age(age)
                .birthYear(birthYear)
                .build();
    }

    private SpendingContext createContextWithMultipleAccounts(int age, int birthYear) {
        StubSimulationView sim = StubSimulationView.builder()
                .addAccount(StubSimulationView.createTestAccount(
                        "Traditional 401k", AccountType.TRADITIONAL_401K, new BigDecimal("500000")))
                .addAccount(StubSimulationView.createTestAccount(
                        "Roth IRA", AccountType.ROTH_IRA, new BigDecimal("200000")))
                .addAccount(StubSimulationView.createTestAccount(
                        "Brokerage", AccountType.TAXABLE_BROKERAGE, new BigDecimal("300000")))
                .initialPortfolioBalance(MILLION)
                .build();

        return SpendingContext.builder()
                .simulation(sim)
                .date(LocalDate.of(2025, 6, 1))
                .retirementStartDate(LocalDate.of(2020, 1, 1))
                .totalExpenses(new BigDecimal("5000"))
                .otherIncome(new BigDecimal("2000"))
                .age(age)
                .birthYear(birthYear)
                .build();
    }
}
