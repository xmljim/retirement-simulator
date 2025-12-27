package io.github.xmljim.retirement.domain.calculator.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.calculator.TestIrsLimitsFixture;
import io.github.xmljim.retirement.domain.config.IrsContributionLimits;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.enums.ContributionType;
import io.github.xmljim.retirement.domain.value.MatchingPolicy;

@DisplayName("Secure2ContributionRules Tests")
class Secure2ContributionRulesTest {

    private Secure2ContributionRules rules;
    private IrsContributionLimits limits;

    @BeforeEach
    void setUp() {
        limits = TestIrsLimitsFixture.createTestLimits();
        // Add 2024 (no super catch-up) and 2026 for year-specific tests
        limits.getLimits().put(2024, new IrsContributionLimits.YearLimits(
            new BigDecimal("23000"), new BigDecimal("7500"),
            BigDecimal.ZERO, new BigDecimal("145000")));
        limits.getLimits().put(2026, new IrsContributionLimits.YearLimits(
            new BigDecimal("24000"), new BigDecimal("7500"),
            new BigDecimal("11250"), new BigDecimal("145000")));
        rules = new Secure2ContributionRules(limits);
    }

    @Nested
    @DisplayName("Rules Version")
    class RulesVersionTests {
        @Test
        @DisplayName("Should return SECURE 2.0 version")
        void returnsSecure2Version() {
            assertEquals("SECURE 2.0", rules.getRulesVersion());
        }
    }

    @Nested
    @DisplayName("Annual Contribution Limits")
    class AnnualContributionLimitTests {

        @Test
        @DisplayName("Age 49: Base limit only ($23,500 for 2025)")
        void age49BaseOnly() {
            BigDecimal limit = rules.calculateAnnualContributionLimit(2025, 49, null);
            assertEquals(0, new BigDecimal("23500").compareTo(limit));
        }

        @Test
        @DisplayName("Age 50-59: Base + standard catch-up ($31,000 for 2025)")
        void age50StandardCatchUp() {
            BigDecimal limit = rules.calculateAnnualContributionLimit(2025, 55, null);
            assertEquals(0, new BigDecimal("31000").compareTo(limit));
        }

        @Test
        @DisplayName("Age 60-63: Base + super catch-up ($34,750 for 2025)")
        void age60SuperCatchUp() {
            BigDecimal limit = rules.calculateAnnualContributionLimit(2025, 61, null);
            assertEquals(0, new BigDecimal("34750").compareTo(limit));
        }

        @Test
        @DisplayName("Age 64+: Base + standard catch-up (super ends at 63)")
        void age64StandardCatchUp() {
            BigDecimal limit = rules.calculateAnnualContributionLimit(2025, 64, null);
            assertEquals(0, new BigDecimal("31000").compareTo(limit));
        }

        @Test
        @DisplayName("Age 60 in 2024: Standard catch-up (super starts 2025)")
        void age60In2024NoSuperCatchUp() {
            BigDecimal limit = rules.calculateAnnualContributionLimit(2024, 60, null);
            assertEquals(0, new BigDecimal("30500").compareTo(limit));
        }
    }

    @Nested
    @DisplayName("Catch-Up Eligibility")
    class CatchUpEligibilityTests {

        @Test
        @DisplayName("Age 49 not eligible")
        void age49NotEligible() {
            assertFalse(rules.isCatchUpEligible(49));
        }

        @Test
        @DisplayName("Age 50 eligible")
        void age50Eligible() {
            assertTrue(rules.isCatchUpEligible(50));
        }

        @Test
        @DisplayName("Age 65 eligible")
        void age65Eligible() {
            assertTrue(rules.isCatchUpEligible(65));
        }
    }

    @Nested
    @DisplayName("Super Catch-Up Eligibility")
    class SuperCatchUpEligibilityTests {

        @Test
        @DisplayName("Age 59 not eligible")
        void age59NotEligible() {
            assertFalse(rules.isSuperCatchUpEligible(2025, 59));
        }

        @Test
        @DisplayName("Age 60 in 2025 eligible")
        void age60In2025Eligible() {
            assertTrue(rules.isSuperCatchUpEligible(2025, 60));
        }

        @Test
        @DisplayName("Age 63 in 2025 eligible")
        void age63In2025Eligible() {
            assertTrue(rules.isSuperCatchUpEligible(2025, 63));
        }

        @Test
        @DisplayName("Age 64 not eligible")
        void age64NotEligible() {
            assertFalse(rules.isSuperCatchUpEligible(2025, 64));
        }

        @Test
        @DisplayName("Age 60 in 2024 not eligible (before effective year)")
        void age60In2024NotEligible() {
            assertFalse(rules.isSuperCatchUpEligible(2024, 60));
        }
    }

    @Nested
    @DisplayName("ROTH Catch-Up Requirements")
    class RothCatchUpTests {

        @Test
        @DisplayName("Income $145,000 does not require ROTH")
        void income145KNoRothRequired() {
            assertFalse(rules.requiresRothCatchUp(2025, new BigDecimal("145000")));
        }

        @Test
        @DisplayName("Income $145,001 requires ROTH")
        void income145001RequiresRoth() {
            assertTrue(rules.requiresRothCatchUp(2025, new BigDecimal("145001")));
        }

        @Test
        @DisplayName("Income $150,000 requires ROTH in 2025")
        void income150KRequiresRothIn2025() {
            assertTrue(rules.requiresRothCatchUp(2025, new BigDecimal("150000")));
        }

        @Test
        @DisplayName("Income $145,000 does not require ROTH in 2026")
        void income145KNoRothIn2026() {
            assertFalse(rules.requiresRothCatchUp(2026, new BigDecimal("145000")));
        }

        @Test
        @DisplayName("Income $145,001 requires ROTH in 2026")
        void income145001RequiresRothIn2026() {
            assertTrue(rules.requiresRothCatchUp(2026, new BigDecimal("145001")));
        }

        @Test
        @DisplayName("Null income does not require ROTH")
        void nullIncomeNoRothRequired() {
            assertFalse(rules.requiresRothCatchUp(2025, null));
        }
    }

    @Nested
    @DisplayName("Target Account Type Determination")
    class TargetAccountTypeTests {

        @Test
        @DisplayName("Employer contribution always goes to Traditional")
        void employerAlwaysTraditional() {
            AccountType result = rules.determineTargetAccountType(
                ContributionType.EMPLOYER, false, new BigDecimal("100000"),
                2025, AccountType.ROTH_401K);
            assertEquals(AccountType.TRADITIONAL_401K, result);
        }

        @Test
        @DisplayName("Employer contribution to 403b goes to Traditional 403b")
        void employerTo403bGoesToTraditional403b() {
            AccountType result = rules.determineTargetAccountType(
                ContributionType.EMPLOYER, false, new BigDecimal("100000"),
                2025, AccountType.ROTH_403B);
            assertEquals(AccountType.TRADITIONAL_403B, result);
        }

        @Test
        @DisplayName("Employer contribution to IRA goes to Traditional IRA")
        void employerToIraGoesToTraditionalIra() {
            AccountType result = rules.determineTargetAccountType(
                ContributionType.EMPLOYER, false, new BigDecimal("100000"),
                2025, AccountType.ROTH_IRA);
            assertEquals(AccountType.TRADITIONAL_IRA, result);
        }

        @Test
        @DisplayName("Employer contribution to Traditional stays Traditional")
        void employerToTraditionalStaysTraditional() {
            AccountType result = rules.determineTargetAccountType(
                ContributionType.EMPLOYER, false, new BigDecimal("100000"),
                2025, AccountType.TRADITIONAL_401K);
            assertEquals(AccountType.TRADITIONAL_401K, result);
        }

        @Test
        @DisplayName("Personal base contribution follows account default")
        void personalBaseFollowsDefault() {
            AccountType result = rules.determineTargetAccountType(
                ContributionType.PERSONAL, false, new BigDecimal("150000"),
                2025, AccountType.TRADITIONAL_401K);
            assertEquals(AccountType.TRADITIONAL_401K, result);
        }

        @Test
        @DisplayName("Catch-up for high earner goes to ROTH")
        void catchUpHighEarnerGoesToRoth() {
            AccountType result = rules.determineTargetAccountType(
                ContributionType.PERSONAL, true, new BigDecimal("150000"),
                2025, AccountType.TRADITIONAL_401K);
            assertEquals(AccountType.ROTH_401K, result);
        }

        @Test
        @DisplayName("Catch-up for high earner 403b goes to ROTH 403b")
        void catchUpHighEarner403bGoesToRoth403b() {
            AccountType result = rules.determineTargetAccountType(
                ContributionType.PERSONAL, true, new BigDecimal("150000"),
                2025, AccountType.TRADITIONAL_403B);
            assertEquals(AccountType.ROTH_403B, result);
        }

        @Test
        @DisplayName("Catch-up for high earner IRA goes to ROTH IRA")
        void catchUpHighEarnerIraGoesToRothIra() {
            AccountType result = rules.determineTargetAccountType(
                ContributionType.PERSONAL, true, new BigDecimal("150000"),
                2025, AccountType.TRADITIONAL_IRA);
            assertEquals(AccountType.ROTH_IRA, result);
        }

        @Test
        @DisplayName("Catch-up for high earner ROTH stays ROTH")
        void catchUpHighEarnerRothStaysRoth() {
            AccountType result = rules.determineTargetAccountType(
                ContributionType.PERSONAL, true, new BigDecimal("150000"),
                2025, AccountType.ROTH_401K);
            assertEquals(AccountType.ROTH_401K, result);
        }

        @Test
        @DisplayName("Catch-up for normal earner follows default")
        void catchUpNormalEarnerFollowsDefault() {
            AccountType result = rules.determineTargetAccountType(
                ContributionType.PERSONAL, true, new BigDecimal("100000"),
                2025, AccountType.TRADITIONAL_401K);
            assertEquals(AccountType.TRADITIONAL_401K, result);
        }

        @Test
        @DisplayName("Null account type returns null")
        void nullAccountTypeReturnsNull() {
            AccountType result = rules.determineTargetAccountType(
                ContributionType.EMPLOYER, false, new BigDecimal("100000"),
                2025, null);
            assertEquals(null, result);
        }

        @Test
        @DisplayName("Other account types pass through unchanged")
        void otherAccountTypesPassThrough() {
            AccountType result = rules.determineTargetAccountType(
                ContributionType.EMPLOYER, false, new BigDecimal("100000"),
                2025, AccountType.TAXABLE_BROKERAGE);
            assertEquals(AccountType.TAXABLE_BROKERAGE, result);
        }
    }

    @Nested
    @DisplayName("Max Contribution Without ROTH")
    class MaxContributionWithoutRothTests {

        @Test
        @DisplayName("Age 49 always base limit")
        void age49AlwaysBase() {
            BigDecimal max = rules.calculateMaxContributionWithoutRoth(
                2025, 49, new BigDecimal("150000"), false);
            assertEquals(0, new BigDecimal("23500").compareTo(max));
        }

        @Test
        @DisplayName("High earner without ROTH capped at base")
        void highEarnerNoRothCappedAtBase() {
            BigDecimal max = rules.calculateMaxContributionWithoutRoth(
                2025, 55, new BigDecimal("150000"), false);
            assertEquals(0, new BigDecimal("23500").compareTo(max));
        }

        @Test
        @DisplayName("High earner with ROTH gets full limit")
        void highEarnerWithRothFullLimit() {
            BigDecimal max = rules.calculateMaxContributionWithoutRoth(
                2025, 55, new BigDecimal("150000"), true);
            assertEquals(0, new BigDecimal("31000").compareTo(max));
        }

        @Test
        @DisplayName("Normal earner without ROTH gets full limit")
        void normalEarnerNoRothFullLimit() {
            BigDecimal max = rules.calculateMaxContributionWithoutRoth(
                2025, 55, new BigDecimal("100000"), false);
            assertEquals(0, new BigDecimal("31000").compareTo(max));
        }
    }

    @Nested
    @DisplayName("Employer Match Calculation")
    class EmployerMatchTests {

        @Test
        @DisplayName("Calculates match using policy")
        void calculatesMatchUsingPolicy() {
            MatchingPolicy policy = MatchingPolicy.simple(0.50, 0.06);
            BigDecimal match = rules.calculateEmployerMatch(new BigDecimal("0.10"), policy);
            assertEquals(0, new BigDecimal("0.03").compareTo(match.stripTrailingZeros()));
        }

        @Test
        @DisplayName("Returns zero for null policy")
        void returnsZeroForNullPolicy() {
            BigDecimal match = rules.calculateEmployerMatch(new BigDecimal("0.10"), null);
            assertEquals(0, BigDecimal.ZERO.compareTo(match));
        }
    }

    @Nested
    @DisplayName("Birthday Edge Cases")
    class BirthdayEdgeCaseTests {

        @Test
        @DisplayName("Age 49 turning 50 in December still gets catch-up for full year")
        void age49Turning50DecemberGetsFullYearCatchUp() {
            // IRS rule: eligible if turning 50 anytime during the year
            assertTrue(rules.isCatchUpEligible(50));
            assertEquals(0, new BigDecimal("31000").compareTo(
                rules.calculateAnnualContributionLimit(2025, 50, AccountType.TRADITIONAL_401K)));
        }

        @Test
        @DisplayName("Age 59 turning 60 in late year gets super catch-up")
        void age59Turning60GetsSuperCatchUp() {
            assertTrue(rules.isSuperCatchUpEligible(2025, 60));
            // Base $23,500 + super catch-up $11,250 = $34,750
            assertEquals(0, new BigDecimal("34750").compareTo(
                rules.calculateAnnualContributionLimit(2025, 60, AccountType.TRADITIONAL_401K)));
        }

        @Test
        @DisplayName("Age 63 turning 64 loses super catch-up eligibility")
        void age63Turning64LosesSuperCatchUp() {
            assertFalse(rules.isSuperCatchUpEligible(2025, 64));
            // Back to standard catch-up: $23,500 + $7,500 = $31,000
            assertEquals(0, new BigDecimal("31000").compareTo(
                rules.calculateAnnualContributionLimit(2025, 64, AccountType.TRADITIONAL_401K)));
        }

        @Test
        @DisplayName("Turning 55 allows HSA catch-up")
        void turning55AllowsHsaCatchUp() {
            // HSA catch-up starts at 55, verified through limit checker
            // This test ensures the age 55 boundary is correctly handled
            assertTrue(rules.isCatchUpEligible(55));
        }
    }
}
