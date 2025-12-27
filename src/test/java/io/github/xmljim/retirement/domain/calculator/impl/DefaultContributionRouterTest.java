package io.github.xmljim.retirement.domain.calculator.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.calculator.ContributionAllocation;
import io.github.xmljim.retirement.domain.calculator.ContributionRouter;
import io.github.xmljim.retirement.domain.calculator.IrsContributionRules;
import io.github.xmljim.retirement.domain.config.IrsContributionLimits;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.enums.ContributionType;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;
import io.github.xmljim.retirement.domain.model.InvestmentAccount;
import io.github.xmljim.retirement.domain.model.PersonProfile;
import io.github.xmljim.retirement.domain.model.Portfolio;
import io.github.xmljim.retirement.domain.value.AssetAllocation;
import io.github.xmljim.retirement.domain.value.RoutingConfiguration;

@DisplayName("DefaultContributionRouter Tests")
class DefaultContributionRouterTest {

    private IrsContributionRules irsRules;
    private ContributionRouter router;
    private Portfolio portfolio;
    private PersonProfile owner;

    private static final String TRAD_401K_ID = "trad-401k";
    private static final String ROTH_401K_ID = "roth-401k";
    private static final String TRAD_IRA_ID = "trad-ira";

    @BeforeEach
    void setUp() {
        irsRules = createTestRules();
        router = new DefaultContributionRouter(irsRules);

        owner = PersonProfile.builder()
            .name("Test User")
            .dateOfBirth(LocalDate.of(1970, 1, 1))
            .retirementDate(LocalDate.of(2035, 1, 1))
            .build();

        InvestmentAccount trad401k = InvestmentAccount.builder()
            .id(TRAD_401K_ID)
            .name("Traditional 401(k)")
            .accountType(AccountType.TRADITIONAL_401K)
            .balance(100000)
            .allocation(AssetAllocation.balanced())
            .preRetirementReturnRate(0.07)
            .build();

        InvestmentAccount roth401k = InvestmentAccount.builder()
            .id(ROTH_401K_ID)
            .name("Roth 401(k)")
            .accountType(AccountType.ROTH_401K)
            .balance(50000)
            .allocation(AssetAllocation.balanced())
            .preRetirementReturnRate(0.07)
            .build();

        InvestmentAccount tradIra = InvestmentAccount.builder()
            .id(TRAD_IRA_ID)
            .name("Traditional IRA")
            .accountType(AccountType.TRADITIONAL_IRA)
            .balance(25000)
            .allocation(AssetAllocation.balanced())
            .preRetirementReturnRate(0.07)
            .build();

        portfolio = Portfolio.builder()
            .owner(owner)
            .addAccount(trad401k)
            .addAccount(roth401k)
            .addAccount(tradIra)
            .build();
    }

    private IrsContributionRules createTestRules() {
        IrsContributionLimits limits = new IrsContributionLimits();
        limits.getLimits().put(2025, new IrsContributionLimits.YearLimits(
            new BigDecimal("23500"), new BigDecimal("7500"),
            new BigDecimal("11250"), new BigDecimal("145000")));
        return new Secure2ContributionRules(limits);
    }

    @Nested
    @DisplayName("Personal Contribution Routing")
    class PersonalContributionTests {

        @Test
        @DisplayName("Should route 100% to single account")
        void singleAccountRouting() {
            RoutingConfiguration config = RoutingConfiguration.singleAccount(TRAD_401K_ID);

            ContributionAllocation allocation = router.route(
                new BigDecimal("5000"),
                ContributionType.PERSONAL,
                portfolio,
                config,
                2025,
                40,  // Not catch-up eligible
                BigDecimal.ZERO
            );

            assertNotNull(allocation);
            assertEquals(new BigDecimal("5000.00"),
                allocation.getAmountForAccount(TRAD_401K_ID));
            assertTrue(allocation.isFullyAllocated());
            assertFalse(allocation.hasWarnings());
        }

        @Test
        @DisplayName("Should split contribution 80/20")
        void splitContributionRouting() {
            RoutingConfiguration config = RoutingConfiguration.builder()
                .addRule(TRAD_401K_ID, 0.80, 1)
                .addRule(ROTH_401K_ID, 0.20, 2)
                .build();

            ContributionAllocation allocation = router.route(
                new BigDecimal("5000"),
                ContributionType.PERSONAL,
                portfolio,
                config,
                2025,
                40,  // Not catch-up eligible
                BigDecimal.ZERO
            );

            assertEquals(new BigDecimal("4000.00"),
                allocation.getAmountForAccount(TRAD_401K_ID));
            assertEquals(new BigDecimal("1000.00"),
                allocation.getAmountForAccount(ROTH_401K_ID));
            assertTrue(allocation.isFullyAllocated());
        }

        @Test
        @DisplayName("Should warn when account not found in portfolio")
        void accountNotFoundWarning() {
            RoutingConfiguration config = RoutingConfiguration.singleAccount("non-existent");

            ContributionAllocation allocation = router.route(
                new BigDecimal("5000"),
                ContributionType.PERSONAL,
                portfolio,
                config,
                2025,
                40,
                BigDecimal.ZERO
            );

            assertTrue(allocation.hasWarnings());
            assertTrue(allocation.hasUnallocated());
            assertEquals(new BigDecimal("5000"), allocation.unallocated());
        }
    }

    @Nested
    @DisplayName("Employer Contribution Routing")
    class EmployerContributionTests {

        @Test
        @DisplayName("Should route employer contribution to Traditional variant")
        void employerRoutesToTraditional() {
            // Config points to Roth, but employer should go to Traditional
            RoutingConfiguration config = RoutingConfiguration.singleAccount(ROTH_401K_ID);

            ContributionAllocation allocation = router.route(
                new BigDecimal("2500"),
                ContributionType.EMPLOYER,
                portfolio,
                config,
                2025,
                40,
                BigDecimal.ZERO
            );

            // Should go to Traditional 401k, not Roth
            assertEquals(0, new BigDecimal("2500").compareTo(
                allocation.getAmountForAccount(TRAD_401K_ID)));
            assertEquals(0, BigDecimal.ZERO.compareTo(
                allocation.getAmountForAccount(ROTH_401K_ID)));
        }
    }

    @Nested
    @DisplayName("High Earner Catch-Up Routing")
    class HighEarnerCatchUpTests {

        @Test
        @DisplayName("Should redirect catch-up to Roth for high earners")
        void highEarnerCatchUpToRoth() {
            RoutingConfiguration config = RoutingConfiguration.singleAccount(TRAD_401K_ID);

            // Age 55 is catch-up eligible
            // Income $200K is above $145K threshold
            ContributionAllocation allocation = router.route(
                new BigDecimal("5000"),
                ContributionType.PERSONAL,
                portfolio,
                config,
                2025,
                55,  // catch-up eligible
                new BigDecimal("200000")  // high earner
            );

            // Should go to Roth 401k instead of Traditional
            assertEquals(new BigDecimal("5000.00"),
                allocation.getAmountForAccount(ROTH_401K_ID));
            assertEquals(BigDecimal.ZERO,
                allocation.getAmountForAccount(TRAD_401K_ID));
            assertTrue(allocation.hasWarnings());
        }

        @Test
        @DisplayName("Should warn when Roth not available for high earner")
        void highEarnerNoRothAvailable() {
            // Portfolio with only Traditional
            Portfolio tradOnlyPortfolio = Portfolio.builder()
                .owner(owner)
                .addAccount(InvestmentAccount.builder()
                    .id(TRAD_401K_ID)
                    .name("Traditional 401(k)")
                    .accountType(AccountType.TRADITIONAL_401K)
                    .balance(100000)
                    .allocation(AssetAllocation.balanced())
                    .preRetirementReturnRate(0.07)
                    .build())
                .build();

            RoutingConfiguration config = RoutingConfiguration.singleAccount(TRAD_401K_ID);

            ContributionAllocation allocation = router.route(
                new BigDecimal("5000"),
                ContributionType.PERSONAL,
                tradOnlyPortfolio,
                config,
                2025,
                55,  // catch-up eligible
                new BigDecimal("200000")  // high earner
            );

            // Should still allocate with warning
            assertEquals(new BigDecimal("5000.00"),
                allocation.getAmountForAccount(TRAD_401K_ID));
            assertTrue(allocation.hasWarnings());
            assertTrue(allocation.warnings().stream()
                .anyMatch(w -> w.contains("Roth")));
        }

        @Test
        @DisplayName("Should route normally for catch-up eligible non-high earner")
        void catchUpEligibleNormalEarner() {
            RoutingConfiguration config = RoutingConfiguration.singleAccount(TRAD_401K_ID);

            // Age 55 is catch-up eligible
            // Income $100K is below $145K threshold
            ContributionAllocation allocation = router.route(
                new BigDecimal("5000"),
                ContributionType.PERSONAL,
                portfolio,
                config,
                2025,
                55,  // catch-up eligible
                new BigDecimal("100000")  // not high earner
            );

            // Should go to Traditional as configured
            assertEquals(new BigDecimal("5000.00"),
                allocation.getAmountForAccount(TRAD_401K_ID));
            assertFalse(allocation.hasWarnings());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle zero amount")
        void zeroAmount() {
            RoutingConfiguration config = RoutingConfiguration.singleAccount(TRAD_401K_ID);

            ContributionAllocation allocation = router.route(
                BigDecimal.ZERO,
                ContributionType.PERSONAL,
                portfolio,
                config,
                2025,
                40,
                BigDecimal.ZERO
            );

            assertEquals(BigDecimal.ZERO, allocation.totalAllocated());
            assertFalse(allocation.hasUnallocated());
        }

        @Test
        @DisplayName("Should handle empty portfolio")
        void emptyPortfolio() {
            Portfolio emptyPortfolio = Portfolio.builder()
                .owner(owner)
                .build();

            RoutingConfiguration config = RoutingConfiguration.singleAccount(TRAD_401K_ID);

            ContributionAllocation allocation = router.route(
                new BigDecimal("5000"),
                ContributionType.PERSONAL,
                emptyPortfolio,
                config,
                2025,
                40,
                BigDecimal.ZERO
            );

            assertTrue(allocation.hasUnallocated());
            assertTrue(allocation.hasWarnings());
        }

        @Test
        @DisplayName("Should throw for null amount")
        void nullAmountThrows() {
            RoutingConfiguration config = RoutingConfiguration.singleAccount(TRAD_401K_ID);

            assertThrows(MissingRequiredFieldException.class, () ->
                router.route(
                    null,
                    ContributionType.PERSONAL,
                    portfolio,
                    config,
                    2025,
                    40,
                    BigDecimal.ZERO
                )
            );
        }

        @Test
        @DisplayName("Should throw for negative amount")
        void negativeAmountThrows() {
            RoutingConfiguration config = RoutingConfiguration.singleAccount(TRAD_401K_ID);

            assertThrows(ValidationException.class, () ->
                router.route(
                    new BigDecimal("-100"),
                    ContributionType.PERSONAL,
                    portfolio,
                    config,
                    2025,
                    40,
                    BigDecimal.ZERO
                )
            );
        }

        @Test
        @DisplayName("Should use convenience method with defaults")
        void convenienceMethod() {
            RoutingConfiguration config = RoutingConfiguration.singleAccount(TRAD_401K_ID);

            ContributionAllocation allocation = router.route(
                new BigDecimal("5000"),
                ContributionType.PERSONAL,
                portfolio,
                config,
                2025
            );

            assertEquals(new BigDecimal("5000.00"),
                allocation.getAmountForAccount(TRAD_401K_ID));
        }
    }
}
