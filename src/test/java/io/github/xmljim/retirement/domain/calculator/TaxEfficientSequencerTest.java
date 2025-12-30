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

import io.github.xmljim.retirement.domain.calculator.impl.TaxEfficientSequencer;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.model.InvestmentAccount;
import io.github.xmljim.retirement.domain.model.PersonProfile;
import io.github.xmljim.retirement.domain.model.Portfolio;
import io.github.xmljim.retirement.domain.value.AssetAllocation;
import io.github.xmljim.retirement.domain.value.SpendingContext;

@DisplayName("TaxEfficientSequencer Tests")
class TaxEfficientSequencerTest {

    private TaxEfficientSequencer sequencer;
    private PersonProfile owner;
    private SpendingContext context;
    private LocalDate retirementStart;

    @BeforeEach
    void setUp() {
        sequencer = new TaxEfficientSequencer();
        retirementStart = LocalDate.of(2020, 1, 1);
        owner = PersonProfile.builder()
                .name("Test Owner")
                .dateOfBirth(LocalDate.of(1960, 1, 1))
                .retirementDate(LocalDate.of(2025, 1, 1))
                .build();
    }

    private SpendingContext createContext(Portfolio portfolio) {
        StubSimulationView simulation = StubSimulationView.withAccounts(
                portfolio.getAccounts().stream()
                        .map(a -> StubSimulationView.createTestAccount(
                                a.getName(), a.getAccountType(), a.getBalance()))
                        .toList());
        return SpendingContext.builder()
                .simulation(simulation)
                .date(LocalDate.now())
                .retirementStartDate(retirementStart)
                .build();
    }

    private InvestmentAccount createAccount(String name, AccountType type, BigDecimal balance) {
        return InvestmentAccount.builder()
                .name(name)
                .accountType(type)
                .balance(balance)
                .allocation(AssetAllocation.of(60, 35, 5))
                .preRetirementReturnRate(0.07)
                .build();
    }

    @Nested
    @DisplayName("Tax Treatment Ordering Tests")
    class TaxTreatmentOrderingTests {

        @Test
        @DisplayName("Should order taxable accounts first")
        void taxableFirst() {
            Portfolio portfolio = Portfolio.builder()
                    .owner(owner)
                    .addAccount(createAccount("Roth IRA", AccountType.ROTH_IRA, new BigDecimal("100000")))
                    .addAccount(createAccount("Brokerage", AccountType.TAXABLE_BROKERAGE, new BigDecimal("50000")))
                    .addAccount(createAccount("401k", AccountType.TRADITIONAL_401K, new BigDecimal("200000")))
                    .build();

            List<InvestmentAccount> ordered = sequencer.sequence(portfolio, createContext(portfolio));

            assertEquals(3, ordered.size());
            assertEquals(AccountType.TAXABLE_BROKERAGE, ordered.getFirst().getAccountType());
        }

        @Test
        @DisplayName("Should order pre-tax before Roth")
        void preTaxBeforeRoth() {
            Portfolio portfolio = Portfolio.builder()
                    .owner(owner)
                    .addAccount(createAccount("Roth IRA", AccountType.ROTH_IRA, new BigDecimal("100000")))
                    .addAccount(createAccount("Traditional IRA", AccountType.TRADITIONAL_IRA, new BigDecimal("150000")))
                    .build();

            List<InvestmentAccount> ordered = sequencer.sequence(portfolio, createContext(portfolio));

            assertEquals(2, ordered.size());
            assertEquals(AccountType.TRADITIONAL_IRA, ordered.getFirst().getAccountType());
            assertEquals(AccountType.ROTH_IRA, ordered.getLast().getAccountType());
        }

        @Test
        @DisplayName("Should order HSA last")
        void hsaLast() {
            Portfolio portfolio = Portfolio.builder()
                    .owner(owner)
                    .addAccount(createAccount("HSA", AccountType.HSA, new BigDecimal("30000")))
                    .addAccount(createAccount("Roth IRA", AccountType.ROTH_IRA, new BigDecimal("100000")))
                    .addAccount(createAccount("401k", AccountType.TRADITIONAL_401K, new BigDecimal("200000")))
                    .addAccount(createAccount("Brokerage", AccountType.TAXABLE_BROKERAGE, new BigDecimal("50000")))
                    .build();

            List<InvestmentAccount> ordered = sequencer.sequence(portfolio, createContext(portfolio));

            assertEquals(4, ordered.size());
            assertEquals(AccountType.HSA, ordered.getLast().getAccountType());
        }

        @Test
        @DisplayName("Should sequence all account types correctly")
        void fullSequence() {
            Portfolio portfolio = Portfolio.builder()
                    .owner(owner)
                    .addAccount(createAccount("Roth IRA", AccountType.ROTH_IRA, new BigDecimal("100000")))
                    .addAccount(createAccount("HSA", AccountType.HSA, new BigDecimal("30000")))
                    .addAccount(createAccount("401k", AccountType.TRADITIONAL_401K, new BigDecimal("200000")))
                    .addAccount(createAccount("Brokerage", AccountType.TAXABLE_BROKERAGE, new BigDecimal("50000")))
                    .build();

            List<InvestmentAccount> ordered = sequencer.sequence(portfolio, createContext(portfolio));

            assertEquals(4, ordered.size());
            // TAXABLE → PRE_TAX → ROTH → HSA
            assertEquals(AccountType.TaxTreatment.TAXABLE, ordered.getFirst().getAccountType().getTaxTreatment());
            assertEquals(AccountType.TaxTreatment.PRE_TAX, ordered.get(1).getAccountType().getTaxTreatment());
            assertEquals(AccountType.TaxTreatment.ROTH, ordered.get(2).getAccountType().getTaxTreatment());
            assertEquals(AccountType.TaxTreatment.HSA, ordered.getLast().getAccountType().getTaxTreatment());
        }
    }

    @Nested
    @DisplayName("Balance-Based Secondary Sort Tests")
    class BalanceSortTests {

        @Test
        @DisplayName("Should sort same tax treatment by balance ascending")
        void sortsByBalanceWithinCategory() {
            Portfolio portfolio = Portfolio.builder()
                    .owner(owner)
                    .addAccount(createAccount("Big 401k", AccountType.TRADITIONAL_401K, new BigDecimal("500000")))
                    .addAccount(createAccount("Small 401k", AccountType.TRADITIONAL_401K, new BigDecimal("50000")))
                    .addAccount(createAccount("Medium IRA", AccountType.TRADITIONAL_IRA, new BigDecimal("100000")))
                    .build();

            List<InvestmentAccount> ordered = sequencer.sequence(portfolio, createContext(portfolio));

            assertEquals(3, ordered.size());
            // All PRE_TAX, sorted by balance ascending
            assertEquals("Small 401k", ordered.get(0).getName());
            assertEquals("Medium IRA", ordered.get(1).getName());
            assertEquals("Big 401k", ordered.get(2).getName());
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should exclude zero balance accounts")
        void excludesZeroBalance() {
            Portfolio portfolio = Portfolio.builder()
                    .owner(owner)
                    .addAccount(createAccount("Empty Brokerage", AccountType.TAXABLE_BROKERAGE, BigDecimal.ZERO))
                    .addAccount(createAccount("401k", AccountType.TRADITIONAL_401K, new BigDecimal("200000")))
                    .build();

            List<InvestmentAccount> ordered = sequencer.sequence(portfolio, createContext(portfolio));

            assertEquals(1, ordered.size());
            assertEquals("401k", ordered.getFirst().getName());
        }

        @Test
        @DisplayName("Should handle empty portfolio")
        void handlesEmptyPortfolio() {
            Portfolio portfolio = Portfolio.builder()
                    .owner(owner)
                    .build();

            List<InvestmentAccount> ordered = sequencer.sequence(portfolio, createContext(portfolio));

            assertTrue(ordered.isEmpty());
        }

        @Test
        @DisplayName("Should handle single account")
        void handlesSingleAccount() {
            Portfolio portfolio = Portfolio.builder()
                    .owner(owner)
                    .addAccount(createAccount("Only Account", AccountType.ROTH_IRA, new BigDecimal("100000")))
                    .build();

            List<InvestmentAccount> ordered = sequencer.sequence(portfolio, createContext(portfolio));

            assertEquals(1, ordered.size());
            assertEquals("Only Account", ordered.getFirst().getName());
        }

        @Test
        @DisplayName("Should throw for null portfolio")
        void throwsForNullPortfolio() {
            assertThrows(MissingRequiredFieldException.class, () ->
                    sequencer.sequence(null, context));
        }
    }

    @Nested
    @DisplayName("Metadata Tests")
    class MetadataTests {

        @Test
        @DisplayName("Should return correct name")
        void hasCorrectName() {
            assertEquals("Tax-Efficient", sequencer.getName());
        }

        @Test
        @DisplayName("Should be tax-aware")
        void isTaxAware() {
            assertTrue(sequencer.isTaxAware());
        }

        @Test
        @DisplayName("Should not be RMD-aware")
        void isNotRmdAware() {
            assertTrue(!sequencer.isRmdAware());
        }
    }
}
