package io.github.xmljim.retirement.domain.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.calculator.impl.DefaultRmdCalculator;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultSpendingOrchestrator;
import io.github.xmljim.retirement.domain.calculator.impl.RmdFirstSequencer;
import io.github.xmljim.retirement.domain.calculator.impl.TaxEfficientSequencer;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.value.AccountSnapshot;
import io.github.xmljim.retirement.domain.value.SpendingContext;
import io.github.xmljim.retirement.domain.value.SpendingPlan;

@DisplayName("DefaultSpendingOrchestrator Tests")
class DefaultSpendingOrchestratorTest {

    private DefaultSpendingOrchestrator orchestrator;
    private LocalDate retirementStart;

    @BeforeEach
    void setUp() {
        RmdCalculator rmdCalculator = new DefaultRmdCalculator();
        orchestrator = new DefaultSpendingOrchestrator(rmdCalculator);
        retirementStart = LocalDate.of(2020, 1, 1);
    }

    private SpendingContext createContext(AccountSnapshot... accounts) {
        StubSimulationView simulation = StubSimulationView.withAccounts(List.of(accounts));
        return SpendingContext.builder()
                .simulation(simulation)
                .totalExpenses(new BigDecimal("5000"))
                .otherIncome(new BigDecimal("2000"))
                .date(LocalDate.now())
                .retirementStartDate(retirementStart)
                .build();
    }

    private SpendingContext createContextWithAge(int age, int birthYear, AccountSnapshot... accounts) {
        StubSimulationView simulation = StubSimulationView.withAccounts(List.of(accounts));
        return SpendingContext.builder()
                .simulation(simulation)
                .totalExpenses(new BigDecimal("5000"))
                .otherIncome(new BigDecimal("2000"))
                .date(LocalDate.now())
                .age(age)
                .birthYear(birthYear)
                .retirementStartDate(retirementStart)
                .build();
    }

    private AccountSnapshot createAccount(String name, AccountType type, BigDecimal balance) {
        return StubSimulationView.createTestAccount(name, type, balance);
    }

    // Simple test strategy that returns a fixed withdrawal amount
    private SpendingStrategy createFixedStrategy(BigDecimal amount) {
        return new SpendingStrategy() {
            @Override
            public SpendingPlan calculateWithdrawal(SpendingContext context) {
                return SpendingPlan.builder()
                        .targetWithdrawal(amount)
                        .adjustedWithdrawal(amount)
                        .meetsTarget(true)
                        .strategyUsed(getName())
                        .build();
            }

            @Override
            public String getName() {
                return "Fixed Test Strategy";
            }

            @Override
            public String getDescription() {
                return "Test strategy returning fixed amount";
            }
        };
    }

    @Nested
    @DisplayName("Basic Execution Tests")
    class BasicExecutionTests {

        @Test
        @DisplayName("Should execute withdrawal from single account")
        void executesWithdrawalFromSingleAccount() {
            SpendingContext context = createContext(
                    createAccount("401(k)", AccountType.TRADITIONAL_401K, new BigDecimal("200000")),
                    createAccount("Roth IRA", AccountType.ROTH_IRA, new BigDecimal("100000")),
                    createAccount("Brokerage", AccountType.TAXABLE_BROKERAGE, new BigDecimal("50000")));
            SpendingStrategy strategy = createFixedStrategy(new BigDecimal("10000"));
            AccountSequencer sequencer = new TaxEfficientSequencer();

            SpendingPlan plan = orchestrator.execute(strategy, sequencer, context);

            assertEquals(0, new BigDecimal("10000").compareTo(plan.targetWithdrawal()));
            assertEquals(0, new BigDecimal("10000").compareTo(plan.adjustedWithdrawal()));
            assertTrue(plan.meetsTarget());
            assertEquals(0, BigDecimal.ZERO.compareTo(plan.shortfall()));
            assertFalse(plan.accountWithdrawals().isEmpty());
        }

        @Test
        @DisplayName("Should execute withdrawal across multiple accounts")
        void executesWithdrawalAcrossMultipleAccounts() {
            SpendingContext context = createContext(
                    createAccount("401(k)", AccountType.TRADITIONAL_401K, new BigDecimal("200000")),
                    createAccount("Roth IRA", AccountType.ROTH_IRA, new BigDecimal("100000")),
                    createAccount("Brokerage", AccountType.TAXABLE_BROKERAGE, new BigDecimal("50000")));
            SpendingStrategy strategy = createFixedStrategy(new BigDecimal("75000"));
            AccountSequencer sequencer = new TaxEfficientSequencer();

            SpendingPlan plan = orchestrator.execute(strategy, sequencer, context);

            assertTrue(plan.meetsTarget());
            assertTrue(plan.accountWithdrawals().size() > 1);
            assertEquals(0, new BigDecimal("75000").compareTo(plan.adjustedWithdrawal()));
        }

        @Test
        @DisplayName("Should return no withdrawal when target is zero")
        void noWithdrawalWhenTargetZero() {
            SpendingContext context = createContext(
                    createAccount("401(k)", AccountType.TRADITIONAL_401K, new BigDecimal("200000")));
            SpendingStrategy strategy = createFixedStrategy(BigDecimal.ZERO);
            AccountSequencer sequencer = new TaxEfficientSequencer();

            SpendingPlan plan = orchestrator.execute(strategy, sequencer, context);

            assertEquals(0, BigDecimal.ZERO.compareTo(plan.targetWithdrawal()));
            assertEquals(0, BigDecimal.ZERO.compareTo(plan.adjustedWithdrawal()));
            assertTrue(plan.meetsTarget());
            assertTrue(plan.accountWithdrawals().isEmpty());
        }
    }

    @Nested
    @DisplayName("Shortfall Tests")
    class ShortfallTests {

        @Test
        @DisplayName("Should track shortfall when portfolio insufficient")
        void tracksShortfall() {
            SpendingContext context = createContext(
                    createAccount("401(k)", AccountType.TRADITIONAL_401K, new BigDecimal("200000")),
                    createAccount("Roth IRA", AccountType.ROTH_IRA, new BigDecimal("100000")),
                    createAccount("Brokerage", AccountType.TAXABLE_BROKERAGE, new BigDecimal("50000")));
            SpendingStrategy strategy = createFixedStrategy(new BigDecimal("500000"));
            AccountSequencer sequencer = new TaxEfficientSequencer();

            SpendingPlan plan = orchestrator.execute(strategy, sequencer, context);

            assertFalse(plan.meetsTarget());
            assertTrue(plan.shortfall().compareTo(BigDecimal.ZERO) > 0);
            assertEquals(0, new BigDecimal("350000").compareTo(plan.adjustedWithdrawal()));
            assertEquals(0, new BigDecimal("150000").compareTo(plan.shortfall()));
        }
    }

    @Nested
    @DisplayName("Default Sequencer Selection Tests")
    class DefaultSequencerSelectionTests {

        @Test
        @DisplayName("Should select TaxEfficientSequencer when not subject to RMD")
        void selectsTaxEfficientWhenNotRmd() {
            SpendingContext context = createContext(
                    createAccount("401(k)", AccountType.TRADITIONAL_401K, new BigDecimal("200000")));

            AccountSequencer sequencer = orchestrator.selectDefaultSequencer(context);

            assertTrue(sequencer instanceof TaxEfficientSequencer);
        }

        @Test
        @DisplayName("Should select RmdFirstSequencer when subject to RMD")
        void selectsRmdFirstWhenRmd() {
            SpendingContext context = createContextWithAge(76, 1950,
                    createAccount("401(k)", AccountType.TRADITIONAL_401K, new BigDecimal("200000")));

            AccountSequencer sequencer = orchestrator.selectDefaultSequencer(context);

            assertTrue(sequencer instanceof RmdFirstSequencer);
        }

        @Test
        @DisplayName("Should use overloaded method with default sequencer")
        void usesDefaultSequencerOverload() {
            SpendingContext context = createContext(
                    createAccount("401(k)", AccountType.TRADITIONAL_401K, new BigDecimal("200000")),
                    createAccount("Brokerage", AccountType.TAXABLE_BROKERAGE, new BigDecimal("50000")));
            SpendingStrategy strategy = createFixedStrategy(new BigDecimal("10000"));

            SpendingPlan plan = orchestrator.execute(strategy, context);

            assertTrue(plan.meetsTarget());
            assertEquals("Tax-Efficient", plan.metadata().get("sequencer"));
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw for null strategy")
        void throwsForNullStrategy() {
            SpendingContext context = createContext(
                    createAccount("401(k)", AccountType.TRADITIONAL_401K, new BigDecimal("200000")));
            AccountSequencer sequencer = new TaxEfficientSequencer();

            assertThrows(MissingRequiredFieldException.class, () ->
                    orchestrator.execute(null, sequencer, context));
        }

        @Test
        @DisplayName("Should throw for null sequencer")
        void throwsForNullSequencer() {
            SpendingContext context = createContext(
                    createAccount("401(k)", AccountType.TRADITIONAL_401K, new BigDecimal("200000")));
            SpendingStrategy strategy = createFixedStrategy(new BigDecimal("10000"));

            assertThrows(MissingRequiredFieldException.class, () ->
                    orchestrator.execute(strategy, null, context));
        }

        @Test
        @DisplayName("Should throw for null context")
        void throwsForNullContext() {
            SpendingStrategy strategy = createFixedStrategy(new BigDecimal("10000"));
            AccountSequencer sequencer = new TaxEfficientSequencer();

            assertThrows(MissingRequiredFieldException.class, () ->
                    orchestrator.execute(strategy, sequencer, null));
        }

        @Test
        @DisplayName("Constructor should throw for null RMD calculator")
        void constructorThrowsForNullCalculator() {
            assertThrows(MissingRequiredFieldException.class, () ->
                    new DefaultSpendingOrchestrator(null));
        }
    }

    @Nested
    @DisplayName("Metadata Tests")
    class MetadataTests {

        @Test
        @DisplayName("Should include sequencer name in metadata")
        void includesSequencerInMetadata() {
            SpendingContext context = createContext(
                    createAccount("401(k)", AccountType.TRADITIONAL_401K, new BigDecimal("200000")));
            SpendingStrategy strategy = createFixedStrategy(new BigDecimal("10000"));
            AccountSequencer sequencer = new TaxEfficientSequencer();

            SpendingPlan plan = orchestrator.execute(strategy, sequencer, context);

            assertEquals("Tax-Efficient", plan.metadata().get("sequencer"));
        }

        @Test
        @DisplayName("Should include accounts used count in metadata")
        void includesAccountsUsedInMetadata() {
            SpendingContext context = createContext(
                    createAccount("401(k)", AccountType.TRADITIONAL_401K, new BigDecimal("200000")));
            SpendingStrategy strategy = createFixedStrategy(new BigDecimal("10000"));
            AccountSequencer sequencer = new TaxEfficientSequencer();

            SpendingPlan plan = orchestrator.execute(strategy, sequencer, context);

            assertTrue(plan.metadata().containsKey("accountsUsed"));
        }
    }
}
