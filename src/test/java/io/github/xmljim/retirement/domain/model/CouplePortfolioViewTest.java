package io.github.xmljim.retirement.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.calculator.CalculatorFactory;
import io.github.xmljim.retirement.domain.calculator.YTDContributionTracker;
import io.github.xmljim.retirement.domain.calculator.impl.Secure2ContributionRules;
import io.github.xmljim.retirement.domain.config.IrsContributionLimits;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.enums.ContributionType;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.value.AssetAllocation;
import io.github.xmljim.retirement.domain.value.ContributionRecord;

@DisplayName("CouplePortfolioView Tests")
class CouplePortfolioViewTest {

    private Portfolio primaryPortfolio;
    private Portfolio secondaryPortfolio;
    private YTDContributionTracker primaryTracker;
    private YTDContributionTracker secondaryTracker;
    private Secure2ContributionRules irsRules;

    @BeforeEach
    void setUp() {
        IrsContributionLimits limits = createTestLimits();
        irsRules = new Secure2ContributionRules(limits);

        PersonProfile primary = PersonProfile.builder()
            .name("John").dateOfBirth(LocalDate.of(1970, 5, 15))
            .retirementDate(LocalDate.of(2035, 1, 1)).build();

        PersonProfile secondary = PersonProfile.builder()
            .name("Jane").dateOfBirth(LocalDate.of(1972, 8, 20))
            .retirementDate(LocalDate.of(2037, 1, 1)).build();

        primaryPortfolio = Portfolio.builder().owner(primary)
            .addAccount(createAccount("401k", AccountType.TRADITIONAL_401K, 200000))
            .addAccount(createAccount("roth", AccountType.ROTH_IRA, 50000)).build();

        secondaryPortfolio = Portfolio.builder().owner(secondary)
            .addAccount(createAccount("403b", AccountType.TRADITIONAL_403B, 150000))
            .addAccount(createAccount("ira", AccountType.TRADITIONAL_IRA, 30000)).build();

        primaryTracker = CalculatorFactory.ytdTracker(irsRules);
        secondaryTracker = CalculatorFactory.ytdTracker(irsRules);
    }

    private IrsContributionLimits createTestLimits() {
        IrsContributionLimits limits = new IrsContributionLimits();
        limits.getLimits().put(2025, new IrsContributionLimits.YearLimits(
            new BigDecimal("23500"), new BigDecimal("7500"),
            new BigDecimal("11250"), new BigDecimal("145000")));
        limits.getIraLimits().put(2025, new IrsContributionLimits.IraLimits(
            new BigDecimal("7000"), new BigDecimal("1000")));
        limits.getHsaLimits().put(2025, new IrsContributionLimits.HsaLimits(
            new BigDecimal("4300"), new BigDecimal("8550"), new BigDecimal("1000")));
        return limits;
    }

    private InvestmentAccount createAccount(String name, AccountType type, double balance) {
        return InvestmentAccount.builder().name(name).accountType(type)
            .balance(new BigDecimal(String.valueOf(balance)))
            .allocation(AssetAllocation.of(70, 25, 5)).preRetirementReturnRate(0.07).build();
    }

    @Nested
    @DisplayName("Creation Tests")
    class CreationTests {
        @Test
        @DisplayName("Should create couple view with both portfolios")
        void createsCoupleView() {
            CouplePortfolioView view = CouplePortfolioView.of(
                primaryPortfolio, secondaryPortfolio, primaryTracker, secondaryTracker);
            assertTrue(view.isCouple());
            assertNotNull(view.getPrimaryPortfolio());
            assertNotNull(view.getSecondaryPortfolio());
        }

        @Test
        @DisplayName("Should create single-person view")
        void createsSingleView() {
            CouplePortfolioView view = CouplePortfolioView.single(primaryPortfolio, primaryTracker);
            assertFalse(view.isCouple());
            assertNull(view.getSecondaryPortfolio());
        }

        @Test
        @DisplayName("Should throw for null primary portfolio")
        void throwsForNullPrimary() {
            assertThrows(MissingRequiredFieldException.class, () ->
                CouplePortfolioView.of(null, secondaryPortfolio, primaryTracker, secondaryTracker));
        }
    }

    @Nested
    @DisplayName("Balance Aggregation Tests")
    class BalanceTests {
        @Test
        @DisplayName("Should calculate combined balance")
        void combinedBalance() {
            CouplePortfolioView view = CouplePortfolioView.of(
                primaryPortfolio, secondaryPortfolio, primaryTracker, secondaryTracker);
            assertEquals(0, new BigDecimal("430000").compareTo(view.getCombinedBalance()));
        }

        @Test
        @DisplayName("Should return primary and secondary balances separately")
        void separateBalances() {
            CouplePortfolioView view = CouplePortfolioView.of(
                primaryPortfolio, secondaryPortfolio, primaryTracker, secondaryTracker);
            assertEquals(0, new BigDecimal("250000").compareTo(view.getPrimaryBalance()));
            assertEquals(0, new BigDecimal("180000").compareTo(view.getSecondaryBalance()));
        }

        @Test
        @DisplayName("Should merge balances by account type")
        void balancesByType() {
            CouplePortfolioView view = CouplePortfolioView.of(
                primaryPortfolio, secondaryPortfolio, primaryTracker, secondaryTracker);
            Map<AccountType, BigDecimal> byType = view.getBalancesByType();
            assertEquals(0, new BigDecimal("200000").compareTo(byType.get(AccountType.TRADITIONAL_401K)));
            assertEquals(0, new BigDecimal("150000").compareTo(byType.get(AccountType.TRADITIONAL_403B)));
        }

        @Test
        @DisplayName("Single view should return zero for secondary balance")
        void singleSecondaryBalance() {
            CouplePortfolioView view = CouplePortfolioView.single(primaryPortfolio, primaryTracker);
            assertEquals(BigDecimal.ZERO, view.getSecondaryBalance());
        }
    }

    @Nested
    @DisplayName("YTD Summary Tests")
    class YtdSummaryTests {
        @Test
        @DisplayName("Should get primary YTD summary")
        void primarySummary() {
            primaryTracker = primaryTracker.recordContribution(ContributionRecord.builder()
                .accountId("401k").accountType(AccountType.TRADITIONAL_401K)
                .source(ContributionType.PERSONAL).amount(5000).year(2025)
                .date(LocalDate.of(2025, 1, 15)).build());

            CouplePortfolioView view = CouplePortfolioView.of(
                primaryPortfolio, secondaryPortfolio, primaryTracker, secondaryTracker);
            var summary = view.getPrimaryYTDSummary(2025, 55, irsRules);

            assertNotNull(summary);
            assertEquals(2025, summary.year());
        }

        @Test
        @DisplayName("Single view should return null for secondary summary")
        void singleSecondaryYtd() {
            CouplePortfolioView view = CouplePortfolioView.single(primaryPortfolio, primaryTracker);
            assertNull(view.getSecondaryYTDSummary(2025, 50, irsRules));
        }
    }
}
