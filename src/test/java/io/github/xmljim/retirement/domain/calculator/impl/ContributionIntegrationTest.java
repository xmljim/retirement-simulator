package io.github.xmljim.retirement.domain.calculator.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.calculator.ContributionLimitChecker;
import io.github.xmljim.retirement.domain.calculator.LimitCheckResult;
import io.github.xmljim.retirement.domain.calculator.YTDContributionTracker;
import io.github.xmljim.retirement.domain.config.IrsContributionLimits;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.enums.ContributionType;
import io.github.xmljim.retirement.domain.enums.LimitCategory;
import io.github.xmljim.retirement.domain.value.ContributionRecord;

/**
 * Integration tests for full contribution scenarios across the M3a components.
 */
@DisplayName("Contribution Integration Tests")
class ContributionIntegrationTest {

    private IrsContributionLimits limits;
    private Secure2ContributionRules rules;
    private ContributionLimitChecker checker;
    private YTDContributionTracker tracker;

    @BeforeEach
    void setUp() {
        limits = createTestLimits();
        rules = new Secure2ContributionRules(limits);
        checker = new DefaultContributionLimitChecker(rules, limits);
        tracker = new DefaultYTDContributionTracker(rules);
    }

    private IrsContributionLimits createTestLimits() {
        IrsContributionLimits l = new IrsContributionLimits();
        l.getLimits().put(2025, new IrsContributionLimits.YearLimits(
            new BigDecimal("23500"), new BigDecimal("7500"),
            new BigDecimal("11250"), new BigDecimal("145000")));
        l.getIraLimits().put(2025, new IrsContributionLimits.IraLimits(
            new BigDecimal("7000"), new BigDecimal("1000")));
        l.getHsaLimits().put(2025, new IrsContributionLimits.HsaLimits(
            new BigDecimal("4300"), new BigDecimal("8550"), new BigDecimal("1000")));
        return l;
    }

    @Nested
    @DisplayName("Full Year Contribution Scenarios")
    class FullYearScenarios {

        @Test
        @DisplayName("Should max out 401k over 12 months")
        void maxOut401kOver12Months() {
            BigDecimal monthlyContrib = new BigDecimal("1958.33"); // ~$23,500 / 12
            int age = 40;

            for (int month = 1; month <= 12; month++) {
                LimitCheckResult result = checker.check(
                    tracker, monthlyContrib, AccountType.TRADITIONAL_401K,
                    ContributionType.PERSONAL, 2025, age, BigDecimal.ZERO, false);

                if (result.allowed() && result.allowedAmount().compareTo(BigDecimal.ZERO) > 0) {
                    tracker = tracker.recordContribution(ContributionRecord.builder()
                        .accountId("401k").accountType(AccountType.TRADITIONAL_401K)
                        .source(ContributionType.PERSONAL).amount(result.allowedAmount())
                        .year(2025).date(LocalDate.of(2025, month, 15)).build());
                }
            }

            BigDecimal ytd = tracker.getYTDPersonalContributions(2025, LimitCategory.EMPLOYER_401K);
            assertTrue(ytd.compareTo(new BigDecimal("23400")) >= 0);
            assertTrue(ytd.compareTo(new BigDecimal("23500")) <= 0);
        }

        @Test
        @DisplayName("Should enforce limit after mid-year max-out")
        void enforceLimitAfterMidYearMaxOut() {
            // Contribute full limit in first 6 months
            tracker = tracker.recordContribution(ContributionRecord.builder()
                .accountId("401k").accountType(AccountType.TRADITIONAL_401K)
                .source(ContributionType.PERSONAL).amount(new BigDecimal("23500"))
                .year(2025).date(LocalDate.of(2025, 6, 15)).build());

            // July contribution should be denied
            LimitCheckResult result = checker.check(
                tracker, new BigDecimal("1000"), AccountType.TRADITIONAL_401K,
                ContributionType.PERSONAL, 2025, 40, BigDecimal.ZERO, false);

            assertFalse(result.allowed());
            assertEquals(BigDecimal.ZERO, result.allowedAmount());
        }
    }

    @Nested
    @DisplayName("Couple Different Ages Scenarios")
    class CoupleDifferentAgesScenarios {

        @Test
        @DisplayName("Spouse age 50 gets catch-up while primary age 45 does not")
        void spouseCatchUpPrimaryNot() {
            int primaryAge = 45;
            int spouseAge = 50;

            // Primary limit: $23,500
            LimitCheckResult primaryResult = checker.check(
                tracker, new BigDecimal("25000"), AccountType.TRADITIONAL_401K,
                ContributionType.PERSONAL, 2025, primaryAge, BigDecimal.ZERO, true);

            assertEquals(0, new BigDecimal("23500").compareTo(primaryResult.allowedAmount()));

            // Spouse limit: $31,000 (base + catch-up)
            YTDContributionTracker spouseTracker = new DefaultYTDContributionTracker(rules);
            LimitCheckResult spouseResult = checker.check(
                spouseTracker, new BigDecimal("32000"), AccountType.TRADITIONAL_401K,
                ContributionType.PERSONAL, 2025, spouseAge, BigDecimal.ZERO, true);

            assertEquals(0, new BigDecimal("31000").compareTo(spouseResult.allowedAmount()));
        }

        @Test
        @DisplayName("Age 61 spouse gets super catch-up, age 52 spouse gets standard")
        void superCatchUpVsStandardCatchUp() {
            int age61 = 61; // Super catch-up eligible
            int age52 = 52; // Standard catch-up only

            // Age 61: $23,500 + $11,250 = $34,750
            LimitCheckResult result61 = checker.check(
                tracker, new BigDecimal("35000"), AccountType.TRADITIONAL_401K,
                ContributionType.PERSONAL, 2025, age61, BigDecimal.ZERO, true);

            assertEquals(0, new BigDecimal("34750").compareTo(result61.allowedAmount()));

            // Age 52: $23,500 + $7,500 = $31,000
            YTDContributionTracker tracker52 = new DefaultYTDContributionTracker(rules);
            LimitCheckResult result52 = checker.check(
                tracker52, new BigDecimal("35000"), AccountType.TRADITIONAL_401K,
                ContributionType.PERSONAL, 2025, age52, BigDecimal.ZERO, true);

            assertEquals(0, new BigDecimal("31000").compareTo(result52.allowedAmount()));
        }
    }

    @Nested
    @DisplayName("Multi-Account Scenarios")
    class MultiAccountScenarios {

        @Test
        @DisplayName("IRA limit shared between Traditional and Roth")
        void iraLimitSharedBetweenTypes() {
            // Contribute $4,000 to Traditional IRA
            tracker = tracker.recordContribution(ContributionRecord.builder()
                .accountId("trad-ira").accountType(AccountType.TRADITIONAL_IRA)
                .source(ContributionType.PERSONAL).amount(4000)
                .year(2025).date(LocalDate.of(2025, 3, 1)).build());

            // Only $3,000 remaining for Roth IRA
            LimitCheckResult result = checker.check(
                tracker, new BigDecimal("5000"), AccountType.ROTH_IRA,
                ContributionType.PERSONAL, 2025, 40, BigDecimal.ZERO, false);

            assertTrue(result.allowed());
            assertEquals(0, new BigDecimal("3000").compareTo(result.allowedAmount()));
        }

        @Test
        @DisplayName("401k and IRA have separate limits")
        void separateLimits401kAndIra() {
            // Max out 401k
            tracker = tracker.recordContribution(ContributionRecord.builder()
                .accountId("401k").accountType(AccountType.TRADITIONAL_401K)
                .source(ContributionType.PERSONAL).amount(23500)
                .year(2025).date(LocalDate.of(2025, 6, 1)).build());

            // IRA still has full limit
            LimitCheckResult iraResult = checker.check(
                tracker, new BigDecimal("7000"), AccountType.TRADITIONAL_IRA,
                ContributionType.PERSONAL, 2025, 40, BigDecimal.ZERO, false);

            assertTrue(iraResult.allowed());
            assertEquals(0, new BigDecimal("7000").compareTo(iraResult.allowedAmount()));
        }
    }
}
