package io.github.xmljim.retirement.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.value.AssetAllocation;

@DisplayName("Portfolio Tests")
class PortfolioTest {

    private PersonProfile owner;
    private InvestmentAccount account401k;
    private InvestmentAccount rothIra;
    private InvestmentAccount taxable;

    @BeforeEach
    void setUp() {
        owner = PersonProfile.builder()
                .name("John Doe")
                .dateOfBirth(LocalDate.of(1970, 5, 15))
                .retirementDate(LocalDate.of(2035, 1, 1))
                .build();

        account401k = InvestmentAccount.builder()
                .name("Company 401(k)")
                .accountType(AccountType.TRADITIONAL_401K)
                .balance(new BigDecimal("200000"))
                .allocation(AssetAllocation.of(70, 25, 5))
                .preRetirementReturnRate(0.07)
                .build();

        rothIra = InvestmentAccount.builder()
                .name("Roth IRA")
                .accountType(AccountType.ROTH_IRA)
                .balance(new BigDecimal("100000"))
                .allocation(AssetAllocation.of(80, 15, 5))
                .preRetirementReturnRate(0.08)
                .build();

        taxable = InvestmentAccount.builder()
                .name("Brokerage")
                .accountType(AccountType.TAXABLE_BROKERAGE)
                .balance(new BigDecimal("50000"))
                .allocation(AssetAllocation.of(60, 30, 10))
                .preRetirementReturnRate(0.06)
                .build();
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should create portfolio with owner and accounts")
        void createWithOwnerAndAccounts() {
            Portfolio portfolio = Portfolio.builder()
                    .owner(owner)
                    .addAccount(account401k)
                    .addAccount(rothIra)
                    .build();

            assertEquals(owner, portfolio.getOwner());
            assertEquals(2, portfolio.getAccountCount());
            assertNotNull(portfolio.getId());
        }

        @Test
        @DisplayName("Should throw exception when owner is missing")
        void missingOwner() {
            assertThrows(NullPointerException.class, () ->
                    Portfolio.builder().addAccount(account401k).build());
        }

        @Test
        @DisplayName("Should throw exception for duplicate account ID")
        void duplicateAccountId() {
            assertThrows(IllegalArgumentException.class, () ->
                    Portfolio.builder()
                            .owner(owner)
                            .addAccount(account401k)
                            .addAccount(account401k)
                            .build());
        }

        @Test
        @DisplayName("Should allow empty portfolio")
        void emptyPortfolio() {
            Portfolio portfolio = Portfolio.builder().owner(owner).build();
            assertFalse(portfolio.hasAccounts());
            assertEquals(0, portfolio.getAccountCount());
        }
    }

    @Nested
    @DisplayName("Balance Aggregation Tests")
    class BalanceTests {

        @Test
        @DisplayName("Should calculate total balance")
        void totalBalance() {
            Portfolio portfolio = createFullPortfolio();
            assertEquals(0, new BigDecimal("350000").compareTo(portfolio.getTotalBalance()));
        }

        @Test
        @DisplayName("Should calculate balance by account type")
        void balanceByType() {
            Portfolio portfolio = createFullPortfolio();
            assertEquals(0, new BigDecimal("200000")
                    .compareTo(portfolio.getBalanceByType(AccountType.TRADITIONAL_401K)));
        }

        @Test
        @DisplayName("Should calculate balances by tax treatment")
        void balancesByTaxTreatment() {
            Portfolio portfolio = createFullPortfolio();
            Map<AccountType.TaxTreatment, BigDecimal> balances = portfolio.getBalancesByTaxTreatment();

            assertEquals(0, new BigDecimal("200000")
                    .compareTo(balances.get(AccountType.TaxTreatment.PRE_TAX)));
            assertEquals(0, new BigDecimal("100000")
                    .compareTo(balances.get(AccountType.TaxTreatment.ROTH)));
            assertEquals(0, new BigDecimal("50000")
                    .compareTo(balances.get(AccountType.TaxTreatment.TAXABLE)));
        }
    }

    @Nested
    @DisplayName("Allocation Tests")
    class AllocationTests {

        @Test
        @DisplayName("Should calculate weighted overall allocation")
        void overallAllocation() {
            Portfolio portfolio = createFullPortfolio();
            AssetAllocation overall = portfolio.getOverallAllocation();

            // Weighted: (200k*70 + 100k*80 + 50k*60) / 350k = 71.43% stocks
            assertTrue(overall.getStocksPercentage().doubleValue() > 70);
            assertTrue(overall.getStocksPercentage().doubleValue() < 73);
        }

        @Test
        @DisplayName("Should calculate blended return rate")
        void blendedReturnRate() {
            Portfolio portfolio = createFullPortfolio();
            BigDecimal blended = portfolio.getBlendedPreRetirementReturnRate();

            // Weighted: (200k*0.07 + 100k*0.08 + 50k*0.06) / 350k ≈ 0.0714
            assertTrue(blended.doubleValue() > 0.07);
            assertTrue(blended.doubleValue() < 0.075);
        }
    }

    @Nested
    @DisplayName("Account Lookup Tests")
    class LookupTests {

        @Test
        @DisplayName("Should find account by ID")
        void findById() {
            Portfolio portfolio = createFullPortfolio();
            assertTrue(portfolio.findAccountById(account401k.getId()).isPresent());
            assertFalse(portfolio.findAccountById("non-existent").isPresent());
        }

        @Test
        @DisplayName("Should filter accounts by type")
        void filterByType() {
            Portfolio portfolio = createFullPortfolio();
            assertEquals(1, portfolio.getAccountsByType(AccountType.ROTH_IRA).size());
        }
    }

    @Nested
    @DisplayName("Modification Tests")
    class ModificationTests {

        @Test
        @DisplayName("Should create new portfolio with added account")
        void withAccount() {
            Portfolio original = Portfolio.builder().owner(owner).addAccount(account401k).build();
            Portfolio updated = original.withAccount(rothIra);

            assertEquals(1, original.getAccountCount());
            assertEquals(2, updated.getAccountCount());
        }

        @Test
        @DisplayName("Should create new portfolio without account")
        void withoutAccount() {
            Portfolio original = createFullPortfolio();
            Portfolio updated = original.withoutAccount(taxable.getId());

            assertEquals(3, original.getAccountCount());
            assertEquals(2, updated.getAccountCount());
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsTests {

        @Test
        @DisplayName("Portfolios with same ID should be equal")
        void sameIdEquals() {
            Portfolio p1 = Portfolio.builder().id("same-id").owner(owner).build();
            Portfolio p2 = Portfolio.builder().id("same-id").owner(owner).addAccount(account401k).build();

            assertEquals(p1, p2);
            assertEquals(p1.hashCode(), p2.hashCode());
        }

        @Test
        @DisplayName("Portfolios with different IDs should not be equal")
        void differentIdNotEquals() {
            Portfolio p1 = Portfolio.builder().owner(owner).build();
            Portfolio p2 = Portfolio.builder().owner(owner).build();

            assertNotEquals(p1, p2);
        }
    }

    private Portfolio createFullPortfolio() {
        return Portfolio.builder()
                .owner(owner)
                .addAccount(account401k)
                .addAccount(rothIra)
                .addAccount(taxable)
                .build();
    }
}
