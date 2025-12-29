package io.github.xmljim.retirement.domain.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

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
import io.github.xmljim.retirement.domain.model.InvestmentAccount;
import io.github.xmljim.retirement.domain.model.PersonProfile;
import io.github.xmljim.retirement.domain.model.Portfolio;
import io.github.xmljim.retirement.domain.value.AssetAllocation;
import io.github.xmljim.retirement.domain.value.SpendingContext;
import io.github.xmljim.retirement.domain.value.SpendingPlan;

@DisplayName("DefaultSpendingOrchestrator Tests")
class DefaultSpendingOrchestratorTest {

    private DefaultSpendingOrchestrator orchestrator;
    private RmdCalculator rmdCalculator;
    private PersonProfile owner;
    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        rmdCalculator = new DefaultRmdCalculator();
        orchestrator = new DefaultSpendingOrchestrator(rmdCalculator);

        owner = PersonProfile.builder()
                .name("Test Owner")
                .dateOfBirth(LocalDate.of(1960, 1, 1))
                .retirementDate(LocalDate.of(2025, 1, 1))
                .build();

        InvestmentAccount account401k = InvestmentAccount.builder()
                .name("401(k)")
                .accountType(AccountType.TRADITIONAL_401K)
                .balance(new BigDecimal("200000"))
                .allocation(AssetAllocation.of(60, 35, 5))
                .preRetirementReturnRate(0.07)
                .build();

        InvestmentAccount rothIra = InvestmentAccount.builder()
                .name("Roth IRA")
                .accountType(AccountType.ROTH_IRA)
                .balance(new BigDecimal("100000"))
                .allocation(AssetAllocation.of(70, 25, 5))
                .preRetirementReturnRate(0.08)
                .build();

        InvestmentAccount brokerage = InvestmentAccount.builder()
                .name("Brokerage")
                .accountType(AccountType.TAXABLE_BROKERAGE)
                .balance(new BigDecimal("50000"))
                .allocation(AssetAllocation.of(50, 40, 10))
                .preRetirementReturnRate(0.06)
                .build();

        portfolio = Portfolio.builder()
                .owner(owner)
                .addAccount(account401k)
                .addAccount(rothIra)
                .addAccount(brokerage)
                .build();
    }

    /**
     * Creates a context for the given portfolio.
     * Age and birthYear are derived from the portfolio owner's date of birth.
     */
    private SpendingContext createContext(Portfolio portfolio) {
        return SpendingContext.builder()
                .portfolio(portfolio)
                .totalExpenses(new BigDecimal("5000"))
                .otherIncome(new BigDecimal("2000"))
                .date(LocalDate.now())
                .build();
    }

    /**
     * Creates a context with explicit age and birthYear for RMD testing.
     */
    private SpendingContext createContextWithAge(Portfolio portfolio, int age, int birthYear) {
        return SpendingContext.builder()
                .portfolio(portfolio)
                .totalExpenses(new BigDecimal("5000"))
                .otherIncome(new BigDecimal("2000"))
                .date(LocalDate.now())
                .age(age)
                .birthYear(birthYear)
                .build();
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
            SpendingContext context = createContext(portfolio);
            SpendingStrategy strategy = createFixedStrategy(new BigDecimal("10000"));
            AccountSequencer sequencer = new TaxEfficientSequencer();

            SpendingPlan plan = orchestrator.execute(portfolio, strategy, sequencer, context);

            assertEquals(0, new BigDecimal("10000").compareTo(plan.targetWithdrawal()));
            assertEquals(0, new BigDecimal("10000").compareTo(plan.adjustedWithdrawal()));
            assertTrue(plan.meetsTarget());
            assertEquals(0, BigDecimal.ZERO.compareTo(plan.shortfall()));
            assertFalse(plan.accountWithdrawals().isEmpty());
        }

        @Test
        @DisplayName("Should execute withdrawal across multiple accounts")
        void executesWithdrawalAcrossMultipleAccounts() {
            SpendingContext context = createContext(portfolio);
            // Withdraw more than brokerage has ($50k), requiring multiple accounts
            SpendingStrategy strategy = createFixedStrategy(new BigDecimal("75000"));
            AccountSequencer sequencer = new TaxEfficientSequencer();

            SpendingPlan plan = orchestrator.execute(portfolio, strategy, sequencer, context);

            assertTrue(plan.meetsTarget());
            assertTrue(plan.accountWithdrawals().size() > 1);
            assertEquals(0, new BigDecimal("75000").compareTo(plan.adjustedWithdrawal()));
        }

        @Test
        @DisplayName("Should return no withdrawal when target is zero")
        void noWithdrawalWhenTargetZero() {
            SpendingContext context = createContext(portfolio);
            SpendingStrategy strategy = createFixedStrategy(BigDecimal.ZERO);
            AccountSequencer sequencer = new TaxEfficientSequencer();

            SpendingPlan plan = orchestrator.execute(portfolio, strategy, sequencer, context);

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
            SpendingContext context = createContext(portfolio);
            // Request more than total portfolio ($350k)
            SpendingStrategy strategy = createFixedStrategy(new BigDecimal("500000"));
            AccountSequencer sequencer = new TaxEfficientSequencer();

            SpendingPlan plan = orchestrator.execute(portfolio, strategy, sequencer, context);

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
            SpendingContext context = createContext(portfolio); // Age 65, RMD at 75

            AccountSequencer sequencer = orchestrator.selectDefaultSequencer(context);

            assertTrue(sequencer instanceof TaxEfficientSequencer);
        }

        @Test
        @DisplayName("Should select RmdFirstSequencer when subject to RMD")
        void selectsRmdFirstWhenRmd() {
            SpendingContext context = createContextWithAge(portfolio, 76, 1950); // Age 76, born 1950 → RMD at 72

            AccountSequencer sequencer = orchestrator.selectDefaultSequencer(context);

            assertTrue(sequencer instanceof RmdFirstSequencer);
        }

        @Test
        @DisplayName("Should use overloaded method with default sequencer")
        void usesDefaultSequencerOverload() {
            SpendingContext context = createContext(portfolio);
            SpendingStrategy strategy = createFixedStrategy(new BigDecimal("10000"));

            // Use overloaded method without explicit sequencer
            SpendingPlan plan = orchestrator.execute(portfolio, strategy, context);

            assertTrue(plan.meetsTarget());
            assertEquals("Tax-Efficient", plan.metadata().get("sequencer"));
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw for null portfolio")
        void throwsForNullPortfolio() {
            SpendingContext context = createContext(portfolio);
            SpendingStrategy strategy = createFixedStrategy(new BigDecimal("10000"));
            AccountSequencer sequencer = new TaxEfficientSequencer();

            assertThrows(MissingRequiredFieldException.class, () ->
                    orchestrator.execute(null, strategy, sequencer, context));
        }

        @Test
        @DisplayName("Should throw for null strategy")
        void throwsForNullStrategy() {
            SpendingContext context = createContext(portfolio);
            AccountSequencer sequencer = new TaxEfficientSequencer();

            assertThrows(MissingRequiredFieldException.class, () ->
                    orchestrator.execute(portfolio, null, sequencer, context));
        }

        @Test
        @DisplayName("Should throw for null sequencer")
        void throwsForNullSequencer() {
            SpendingContext context = createContext(portfolio);
            SpendingStrategy strategy = createFixedStrategy(new BigDecimal("10000"));

            assertThrows(MissingRequiredFieldException.class, () ->
                    orchestrator.execute(portfolio, strategy, null, context));
        }

        @Test
        @DisplayName("Should throw for null context")
        void throwsForNullContext() {
            SpendingStrategy strategy = createFixedStrategy(new BigDecimal("10000"));
            AccountSequencer sequencer = new TaxEfficientSequencer();

            assertThrows(MissingRequiredFieldException.class, () ->
                    orchestrator.execute(portfolio, strategy, sequencer, null));
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
            SpendingContext context = createContext(portfolio);
            SpendingStrategy strategy = createFixedStrategy(new BigDecimal("10000"));
            AccountSequencer sequencer = new TaxEfficientSequencer();

            SpendingPlan plan = orchestrator.execute(portfolio, strategy, sequencer, context);

            assertEquals("Tax-Efficient", plan.metadata().get("sequencer"));
        }

        @Test
        @DisplayName("Should include accounts used count in metadata")
        void includesAccountsUsedInMetadata() {
            SpendingContext context = createContext(portfolio);
            SpendingStrategy strategy = createFixedStrategy(new BigDecimal("10000"));
            AccountSequencer sequencer = new TaxEfficientSequencer();

            SpendingPlan plan = orchestrator.execute(portfolio, strategy, sequencer, context);

            assertTrue(plan.metadata().containsKey("accountsUsed"));
        }
    }
}
