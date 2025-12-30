package io.github.xmljim.retirement.domain.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import io.github.xmljim.retirement.domain.calculator.impl.RmdFirstSequencer;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.value.AccountSnapshot;
import io.github.xmljim.retirement.domain.value.SpendingContext;

@DisplayName("RmdFirstSequencer Tests")
class RmdFirstSequencerTest {

    private RmdFirstSequencer sequencer;
    private RmdCalculator rmdCalculator;
    private LocalDate retirementStart;

    @BeforeEach
    void setUp() {
        rmdCalculator = new DefaultRmdCalculator();
        sequencer = new RmdFirstSequencer(rmdCalculator);
        retirementStart = LocalDate.of(2020, 1, 1);
    }

    private SpendingContext createContext(AccountSnapshot... accounts) {
        StubSimulationView simulation = StubSimulationView.withAccounts(List.of(accounts));
        return SpendingContext.builder()
                .simulation(simulation)
                .date(LocalDate.now())
                .age(70)
                .birthYear(1955)
                .retirementStartDate(retirementStart)
                .build();
    }

    private AccountSnapshot createAccount(String name, AccountType type, BigDecimal balance) {
        return StubSimulationView.createTestAccount(name, type, balance);
    }

    @Nested
    @DisplayName("RMD Account Priority Tests")
    class RmdAccountPriorityTests {

        @Test
        @DisplayName("Should put Traditional IRA before Roth IRA")
        void traditionalIraFirst() {
            SpendingContext context = createContext(
                    createAccount("Roth IRA", AccountType.ROTH_IRA, new BigDecimal("100000")),
                    createAccount("Traditional IRA", AccountType.TRADITIONAL_IRA, new BigDecimal("200000")));

            List<AccountSnapshot> ordered = sequencer.sequence(context);

            assertEquals(2, ordered.size());
            assertEquals("Traditional IRA", ordered.get(0).accountName());
            assertEquals("Roth IRA", ordered.get(1).accountName());
        }

        @Test
        @DisplayName("Should put Traditional 401k before taxable brokerage")
        void traditional401kBeforeTaxable() {
            SpendingContext context = createContext(
                    createAccount("Brokerage", AccountType.TAXABLE_BROKERAGE, new BigDecimal("50000")),
                    createAccount("401k", AccountType.TRADITIONAL_401K, new BigDecimal("300000")));

            List<AccountSnapshot> ordered = sequencer.sequence(context);

            assertEquals(2, ordered.size());
            assertEquals("401k", ordered.get(0).accountName());
            assertEquals("Brokerage", ordered.get(1).accountName());
        }

        @Test
        @DisplayName("Should sort RMD accounts by balance descending")
        void rmdAccountsSortedByBalanceDescending() {
            SpendingContext context = createContext(
                    createAccount("Small IRA", AccountType.TRADITIONAL_IRA, new BigDecimal("50000")),
                    createAccount("Large 401k", AccountType.TRADITIONAL_401K, new BigDecimal("500000")),
                    createAccount("Medium IRA", AccountType.TRADITIONAL_IRA, new BigDecimal("150000")));

            List<AccountSnapshot> ordered = sequencer.sequence(context);

            assertEquals(3, ordered.size());
            assertEquals("Large 401k", ordered.get(0).accountName());
            assertEquals("Medium IRA", ordered.get(1).accountName());
            assertEquals("Small IRA", ordered.get(2).accountName());
        }
    }

    @Nested
    @DisplayName("Non-RMD Account Ordering Tests")
    class NonRmdAccountOrderingTests {

        @Test
        @DisplayName("Should order non-RMD accounts tax-efficiently")
        void nonRmdAccountsTaxEfficient() {
            SpendingContext context = createContext(
                    createAccount("HSA", AccountType.HSA, new BigDecimal("30000")),
                    createAccount("Roth IRA", AccountType.ROTH_IRA, new BigDecimal("100000")),
                    createAccount("Brokerage", AccountType.TAXABLE_BROKERAGE, new BigDecimal("50000")));

            List<AccountSnapshot> ordered = sequencer.sequence(context);

            assertEquals(3, ordered.size());
            assertEquals("Brokerage", ordered.get(0).accountName());
            assertEquals("Roth IRA", ordered.get(1).accountName());
            assertEquals("HSA", ordered.get(2).accountName());
        }
    }

    @Nested
    @DisplayName("Mixed Portfolio Tests")
    class MixedPortfolioTests {

        @Test
        @DisplayName("Should sequence mixed portfolio correctly")
        void mixedPortfolioSequence() {
            SpendingContext context = createContext(
                    createAccount("Roth IRA", AccountType.ROTH_IRA, new BigDecimal("100000")),
                    createAccount("HSA", AccountType.HSA, new BigDecimal("30000")),
                    createAccount("401k", AccountType.TRADITIONAL_401K, new BigDecimal("300000")),
                    createAccount("Brokerage", AccountType.TAXABLE_BROKERAGE, new BigDecimal("50000")),
                    createAccount("Traditional IRA", AccountType.TRADITIONAL_IRA, new BigDecimal("150000")));

            List<AccountSnapshot> ordered = sequencer.sequence(context);

            assertEquals(5, ordered.size());
            assertEquals("401k", ordered.get(0).accountName());
            assertEquals("Traditional IRA", ordered.get(1).accountName());
            assertEquals("Brokerage", ordered.get(2).accountName());
            assertEquals("Roth IRA", ordered.get(3).accountName());
            assertEquals("HSA", ordered.get(4).accountName());
        }

        @Test
        @DisplayName("Should handle portfolio with only non-RMD accounts")
        void onlyNonRmdAccounts() {
            SpendingContext context = createContext(
                    createAccount("Roth IRA", AccountType.ROTH_IRA, new BigDecimal("100000")),
                    createAccount("Brokerage", AccountType.TAXABLE_BROKERAGE, new BigDecimal("50000")));

            List<AccountSnapshot> ordered = sequencer.sequence(context);

            assertEquals(2, ordered.size());
            assertEquals("Brokerage", ordered.get(0).accountName());
            assertEquals("Roth IRA", ordered.get(1).accountName());
        }

        @Test
        @DisplayName("Should handle portfolio with only RMD accounts")
        void onlyRmdAccounts() {
            SpendingContext context = createContext(
                    createAccount("401k", AccountType.TRADITIONAL_401K, new BigDecimal("300000")),
                    createAccount("Traditional IRA", AccountType.TRADITIONAL_IRA, new BigDecimal("150000")));

            List<AccountSnapshot> ordered = sequencer.sequence(context);

            assertEquals(2, ordered.size());
            assertEquals("401k", ordered.get(0).accountName());
            assertEquals("Traditional IRA", ordered.get(1).accountName());
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should exclude zero balance accounts")
        void excludesZeroBalance() {
            SpendingContext context = createContext(
                    createAccount("Empty 401k", AccountType.TRADITIONAL_401K, BigDecimal.ZERO),
                    createAccount("Roth IRA", AccountType.ROTH_IRA, new BigDecimal("100000")));

            List<AccountSnapshot> ordered = sequencer.sequence(context);

            assertEquals(1, ordered.size());
            assertEquals("Roth IRA", ordered.get(0).accountName());
        }

        @Test
        @DisplayName("Should handle empty portfolio")
        void handlesEmptyPortfolio() {
            SpendingContext context = createContext();

            List<AccountSnapshot> ordered = sequencer.sequence(context);

            assertTrue(ordered.isEmpty());
        }

        @Test
        @DisplayName("Should throw for null context")
        void throwsForNullContext() {
            assertThrows(MissingRequiredFieldException.class, () ->
                    sequencer.sequence(null));
        }

        @Test
        @DisplayName("Constructor should throw for null calculator")
        void constructorThrowsForNullCalculator() {
            assertThrows(MissingRequiredFieldException.class, () ->
                    new RmdFirstSequencer(null));
        }
    }

    @Nested
    @DisplayName("Metadata Tests")
    class MetadataTests {

        @Test
        @DisplayName("Should return correct name")
        void hasCorrectName() {
            assertEquals("RMD-First", sequencer.getName());
        }

        @Test
        @DisplayName("Should be RMD-aware")
        void isRmdAware() {
            assertTrue(sequencer.isRmdAware());
        }

        @Test
        @DisplayName("Should be tax-aware")
        void isTaxAware() {
            assertTrue(sequencer.isTaxAware());
        }
    }
}
