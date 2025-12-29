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
import io.github.xmljim.retirement.domain.model.InvestmentAccount;
import io.github.xmljim.retirement.domain.model.PersonProfile;
import io.github.xmljim.retirement.domain.model.Portfolio;
import io.github.xmljim.retirement.domain.value.AssetAllocation;
import io.github.xmljim.retirement.domain.value.SpendingContext;

@DisplayName("RmdFirstSequencer Tests")
class RmdFirstSequencerTest {

    private RmdFirstSequencer sequencer;
    private RmdCalculator rmdCalculator;
    private PersonProfile owner;

    @BeforeEach
    void setUp() {
        rmdCalculator = new DefaultRmdCalculator();
        sequencer = new RmdFirstSequencer(rmdCalculator);
        owner = PersonProfile.builder()
                .name("Test Owner")
                .dateOfBirth(LocalDate.of(1955, 1, 1))
                .retirementDate(LocalDate.of(2020, 1, 1))
                .build();
    }

    private SpendingContext createContext(Portfolio portfolio) {
        return SpendingContext.builder()
                .portfolio(portfolio)
                .date(LocalDate.now())
                .age(70)
                .birthYear(1955)
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
    @DisplayName("RMD Account Priority Tests")
    class RmdAccountPriorityTests {

        @Test
        @DisplayName("Should put Traditional IRA before Roth IRA")
        void traditionalIraFirst() {
            Portfolio portfolio = Portfolio.builder()
                    .owner(owner)
                    .addAccount(createAccount("Roth IRA", AccountType.ROTH_IRA, new BigDecimal("100000")))
                    .addAccount(createAccount("Traditional IRA", AccountType.TRADITIONAL_IRA, new BigDecimal("200000")))
                    .build();

            List<InvestmentAccount> ordered = sequencer.sequence(portfolio, createContext(portfolio));

            assertEquals(2, ordered.size());
            assertEquals("Traditional IRA", ordered.get(0).getName());
            assertEquals("Roth IRA", ordered.get(1).getName());
        }

        @Test
        @DisplayName("Should put Traditional 401k before taxable brokerage")
        void traditional401kBeforeTaxable() {
            Portfolio portfolio = Portfolio.builder()
                    .owner(owner)
                    .addAccount(createAccount("Brokerage", AccountType.TAXABLE_BROKERAGE, new BigDecimal("50000")))
                    .addAccount(createAccount("401k", AccountType.TRADITIONAL_401K, new BigDecimal("300000")))
                    .build();

            List<InvestmentAccount> ordered = sequencer.sequence(portfolio, createContext(portfolio));

            assertEquals(2, ordered.size());
            assertEquals("401k", ordered.get(0).getName());
            assertEquals("Brokerage", ordered.get(1).getName());
        }

        @Test
        @DisplayName("Should sort RMD accounts by balance descending")
        void rmdAccountsSortedByBalanceDescending() {
            Portfolio portfolio = Portfolio.builder()
                    .owner(owner)
                    .addAccount(createAccount("Small IRA", AccountType.TRADITIONAL_IRA, new BigDecimal("50000")))
                    .addAccount(createAccount("Large 401k", AccountType.TRADITIONAL_401K, new BigDecimal("500000")))
                    .addAccount(createAccount("Medium IRA", AccountType.TRADITIONAL_IRA, new BigDecimal("150000")))
                    .build();

            List<InvestmentAccount> ordered = sequencer.sequence(portfolio, createContext(portfolio));

            assertEquals(3, ordered.size());
            // Sorted by balance descending
            assertEquals("Large 401k", ordered.get(0).getName());
            assertEquals("Medium IRA", ordered.get(1).getName());
            assertEquals("Small IRA", ordered.get(2).getName());
        }
    }

    @Nested
    @DisplayName("Non-RMD Account Ordering Tests")
    class NonRmdAccountOrderingTests {

        @Test
        @DisplayName("Should order non-RMD accounts tax-efficiently")
        void nonRmdAccountsTaxEfficient() {
            Portfolio portfolio = Portfolio.builder()
                    .owner(owner)
                    .addAccount(createAccount("HSA", AccountType.HSA, new BigDecimal("30000")))
                    .addAccount(createAccount("Roth IRA", AccountType.ROTH_IRA, new BigDecimal("100000")))
                    .addAccount(createAccount("Brokerage", AccountType.TAXABLE_BROKERAGE, new BigDecimal("50000")))
                    .build();

            List<InvestmentAccount> ordered = sequencer.sequence(portfolio, createContext(portfolio));

            assertEquals(3, ordered.size());
            // Tax-efficient: TAXABLE → ROTH → HSA
            assertEquals("Brokerage", ordered.get(0).getName());
            assertEquals("Roth IRA", ordered.get(1).getName());
            assertEquals("HSA", ordered.get(2).getName());
        }
    }

    @Nested
    @DisplayName("Mixed Portfolio Tests")
    class MixedPortfolioTests {

        @Test
        @DisplayName("Should sequence mixed portfolio correctly")
        void mixedPortfolioSequence() {
            Portfolio portfolio = Portfolio.builder()
                    .owner(owner)
                    .addAccount(createAccount("Roth IRA", AccountType.ROTH_IRA, new BigDecimal("100000")))
                    .addAccount(createAccount("HSA", AccountType.HSA, new BigDecimal("30000")))
                    .addAccount(createAccount("401k", AccountType.TRADITIONAL_401K, new BigDecimal("300000")))
                    .addAccount(createAccount("Brokerage", AccountType.TAXABLE_BROKERAGE, new BigDecimal("50000")))
                    .addAccount(createAccount("Traditional IRA", AccountType.TRADITIONAL_IRA, new BigDecimal("150000")))
                    .build();

            List<InvestmentAccount> ordered = sequencer.sequence(portfolio, createContext(portfolio));

            assertEquals(5, ordered.size());

            // First: RMD accounts (sorted by balance desc)
            assertEquals("401k", ordered.get(0).getName());
            assertEquals("Traditional IRA", ordered.get(1).getName());

            // Then: Non-RMD accounts (tax-efficient)
            assertEquals("Brokerage", ordered.get(2).getName());
            assertEquals("Roth IRA", ordered.get(3).getName());
            assertEquals("HSA", ordered.get(4).getName());
        }

        @Test
        @DisplayName("Should handle portfolio with only non-RMD accounts")
        void onlyNonRmdAccounts() {
            Portfolio portfolio = Portfolio.builder()
                    .owner(owner)
                    .addAccount(createAccount("Roth IRA", AccountType.ROTH_IRA, new BigDecimal("100000")))
                    .addAccount(createAccount("Brokerage", AccountType.TAXABLE_BROKERAGE, new BigDecimal("50000")))
                    .build();

            List<InvestmentAccount> ordered = sequencer.sequence(portfolio, createContext(portfolio));

            assertEquals(2, ordered.size());
            // Falls back to tax-efficient: TAXABLE first
            assertEquals("Brokerage", ordered.get(0).getName());
            assertEquals("Roth IRA", ordered.get(1).getName());
        }

        @Test
        @DisplayName("Should handle portfolio with only RMD accounts")
        void onlyRmdAccounts() {
            Portfolio portfolio = Portfolio.builder()
                    .owner(owner)
                    .addAccount(createAccount("401k", AccountType.TRADITIONAL_401K, new BigDecimal("300000")))
                    .addAccount(createAccount("Traditional IRA", AccountType.TRADITIONAL_IRA, new BigDecimal("150000")))
                    .build();

            List<InvestmentAccount> ordered = sequencer.sequence(portfolio, createContext(portfolio));

            assertEquals(2, ordered.size());
            // Sorted by balance descending
            assertEquals("401k", ordered.get(0).getName());
            assertEquals("Traditional IRA", ordered.get(1).getName());
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
                    .addAccount(createAccount("Empty 401k", AccountType.TRADITIONAL_401K, BigDecimal.ZERO))
                    .addAccount(createAccount("Roth IRA", AccountType.ROTH_IRA, new BigDecimal("100000")))
                    .build();

            List<InvestmentAccount> ordered = sequencer.sequence(portfolio, createContext(portfolio));

            assertEquals(1, ordered.size());
            assertEquals("Roth IRA", ordered.get(0).getName());
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
        @DisplayName("Should throw for null portfolio")
        void throwsForNullPortfolio() {
            SpendingContext context = createContext(Portfolio.builder().owner(owner).build());
            assertThrows(MissingRequiredFieldException.class, () ->
                    sequencer.sequence(null, context));
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
