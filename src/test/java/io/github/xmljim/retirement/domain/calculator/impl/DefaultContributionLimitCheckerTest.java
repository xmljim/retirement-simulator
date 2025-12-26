package io.github.xmljim.retirement.domain.calculator.impl;

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

import io.github.xmljim.retirement.domain.calculator.ContributionLimitChecker;
import io.github.xmljim.retirement.domain.calculator.LimitCheckResult;
import io.github.xmljim.retirement.domain.calculator.YTDContributionTracker;
import io.github.xmljim.retirement.domain.config.IrsContributionLimits;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.enums.ContributionType;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;
import io.github.xmljim.retirement.domain.value.ContributionRecord;

@DisplayName("DefaultContributionLimitChecker Tests")
class DefaultContributionLimitCheckerTest {

    private IrsContributionLimits limits;
    private Secure2ContributionRules irsRules;
    private ContributionLimitChecker checker;
    private YTDContributionTracker tracker;

    @BeforeEach
    void setUp() {
        limits = createTestLimits();
        irsRules = new Secure2ContributionRules(limits);
        checker = new DefaultContributionLimitChecker(irsRules, limits);
        tracker = new DefaultYTDContributionTracker(irsRules);
    }

    // CPD-OFF - Test setup duplicated across test files intentionally
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
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
    // CPD-ON

    @Nested
    @DisplayName("401k Limit Tests")
    class Employer401kLimitTests {

        @Test
        @DisplayName("Should allow contribution within limit")
        void allowsWithinLimit() {
            LimitCheckResult result = checker.check(
                tracker,
                new BigDecimal("5000"),
                AccountType.TRADITIONAL_401K,
                ContributionType.PERSONAL,
                2025, 40, BigDecimal.ZERO, false);

            assertTrue(result.allowed());
            assertEquals(0, new BigDecimal("5000").compareTo(result.allowedAmount()));
            assertFalse(result.hasWarnings());
        }

        @Test
        @DisplayName("Should allow full limit for under 50")
        void allowsFullLimitUnder50() {
            LimitCheckResult result = checker.check(
                tracker,
                new BigDecimal("23500"),
                AccountType.TRADITIONAL_401K,
                ContributionType.PERSONAL,
                2025, 40, BigDecimal.ZERO, false);

            assertTrue(result.allowed());
            assertEquals(0, new BigDecimal("23500").compareTo(result.allowedAmount()));
        }

        @Test
        @DisplayName("Should reduce contribution when over limit")
        void reducesOverLimit() {
            LimitCheckResult result = checker.check(
                tracker,
                new BigDecimal("30000"),  // Over $23,500 limit
                AccountType.TRADITIONAL_401K,
                ContributionType.PERSONAL,
                2025, 40, BigDecimal.ZERO, false);

            assertTrue(result.allowed());
            assertEquals(0, new BigDecimal("23500").compareTo(result.allowedAmount()));
            assertTrue(result.hasWarnings());
        }

        @Test
        @DisplayName("Should allow catch-up for age 50+")
        void allowsCatchUpAt50() {
            // Age 50 gets base $23,500 + catch-up $7,500 = $31,000
            LimitCheckResult result = checker.check(
                tracker,
                new BigDecimal("31000"),
                AccountType.TRADITIONAL_401K,
                ContributionType.PERSONAL,
                2025, 50, BigDecimal.ZERO, false);

            assertTrue(result.allowed());
            assertEquals(0, new BigDecimal("31000").compareTo(result.allowedAmount()));
        }

        @Test
        @DisplayName("Should deny when at limit")
        void deniesWhenAtLimit() {
            // First max out the limit
            tracker = tracker.recordContribution(createRecord(
                "401k", AccountType.TRADITIONAL_401K, ContributionType.PERSONAL, 23500));

            LimitCheckResult result = checker.check(
                tracker,
                new BigDecimal("1000"),
                AccountType.TRADITIONAL_401K,
                ContributionType.PERSONAL,
                2025, 40, BigDecimal.ZERO, false);

            assertFalse(result.allowed());
            assertEquals(BigDecimal.ZERO, result.allowedAmount());
        }

        @Test
        @DisplayName("Should allow partial when near limit")
        void allowsPartialNearLimit() {
            // Contribute $20,000, leaving $3,500 room
            tracker = tracker.recordContribution(createRecord(
                "401k", AccountType.TRADITIONAL_401K, ContributionType.PERSONAL, 20000));

            LimitCheckResult result = checker.check(
                tracker,
                new BigDecimal("5000"),  // Requesting more than $3,500 remaining
                AccountType.TRADITIONAL_401K,
                ContributionType.PERSONAL,
                2025, 40, BigDecimal.ZERO, false);

            assertTrue(result.allowed());
            assertEquals(0, new BigDecimal("3500").compareTo(result.allowedAmount()));
            assertTrue(result.hasWarnings());
        }
    }

    @Nested
    @DisplayName("IRA Limit Tests")
    class IraLimitTests {

        @Test
        @DisplayName("Should enforce combined IRA limit")
        void enforcesIraCombinedLimit() {
            // Base IRA limit is $7,000
            LimitCheckResult result = checker.check(
                tracker,
                new BigDecimal("7000"),
                AccountType.TRADITIONAL_IRA,
                ContributionType.PERSONAL,
                2025, 40, BigDecimal.ZERO, false);

            assertTrue(result.allowed());
            assertEquals(0, new BigDecimal("7000").compareTo(result.allowedAmount()));
        }

        @Test
        @DisplayName("Should track Traditional and Roth IRA against same limit")
        void tracksCombinedIra() {
            // Contribute $4,000 to Traditional IRA
            tracker = tracker.recordContribution(createRecord(
                "trad-ira", AccountType.TRADITIONAL_IRA, ContributionType.PERSONAL, 4000));

            // Try to contribute $5,000 to Roth IRA (only $3,000 remaining)
            LimitCheckResult result = checker.check(
                tracker,
                new BigDecimal("5000"),
                AccountType.ROTH_IRA,
                ContributionType.PERSONAL,
                2025, 40, BigDecimal.ZERO, false);

            assertTrue(result.allowed());
            assertEquals(0, new BigDecimal("3000").compareTo(result.allowedAmount()));
        }
    }

    @Nested
    @DisplayName("HSA Limit Tests")
    class HsaLimitTests {

        @Test
        @DisplayName("Should use individual limit for single person")
        void usesIndividualLimit() {
            // Individual limit is $4,300
            LimitCheckResult result = checker.check(
                tracker,
                new BigDecimal("4300"),
                AccountType.HSA,
                ContributionType.PERSONAL,
                2025, 40, BigDecimal.ZERO, false);  // hasSpouse = false

            assertTrue(result.allowed());
            assertEquals(0, new BigDecimal("4300").compareTo(result.allowedAmount()));
        }

        @Test
        @DisplayName("Should use family limit for person with spouse")
        void usesFamilyLimit() {
            // Family limit is $8,550
            LimitCheckResult result = checker.check(
                tracker,
                new BigDecimal("8550"),
                AccountType.HSA,
                ContributionType.PERSONAL,
                2025, 40, BigDecimal.ZERO, true);  // hasSpouse = true

            assertTrue(result.allowed());
            assertEquals(0, new BigDecimal("8550").compareTo(result.allowedAmount()));
        }

        @Test
        @DisplayName("Should add catch-up for age 55+")
        void addsCatchUpAt55() {
            // Individual $4,300 + catch-up $1,000 = $5,300
            LimitCheckResult result = checker.check(
                tracker,
                new BigDecimal("5300"),
                AccountType.HSA,
                ContributionType.PERSONAL,
                2025, 55, BigDecimal.ZERO, false);

            assertTrue(result.allowed());
            assertEquals(0, new BigDecimal("5300").compareTo(result.allowedAmount()));
        }
    }

    @Nested
    @DisplayName("Employer Contribution Tests")
    class EmployerContributionTests {

        @Test
        @DisplayName("Should always allow employer contributions")
        void alwaysAllowsEmployer() {
            LimitCheckResult result = checker.check(
                tracker,
                new BigDecimal("100000"),  // Any amount
                AccountType.TRADITIONAL_401K,
                ContributionType.EMPLOYER,
                2025, 40, BigDecimal.ZERO, false);

            assertTrue(result.allowed());
            assertEquals(0, new BigDecimal("100000").compareTo(result.allowedAmount()));
        }

        @Test
        @DisplayName("Employer contributions should not affect personal limit")
        void employerDoesntAffectPersonalLimit() {
            // Add $10,000 employer contribution
            tracker = tracker.recordContribution(createRecord(
                "401k", AccountType.TRADITIONAL_401K, ContributionType.EMPLOYER, 10000));

            // Personal contribution should still have full limit
            LimitCheckResult result = checker.check(
                tracker,
                new BigDecimal("23500"),
                AccountType.TRADITIONAL_401K,
                ContributionType.PERSONAL,
                2025, 40, BigDecimal.ZERO, false);

            assertTrue(result.allowed());
            assertEquals(0, new BigDecimal("23500").compareTo(result.allowedAmount()));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should allow zero contribution")
        void allowsZeroContribution() {
            LimitCheckResult result = checker.check(
                tracker,
                BigDecimal.ZERO,
                AccountType.TRADITIONAL_401K,
                ContributionType.PERSONAL,
                2025, 40, BigDecimal.ZERO, false);

            assertTrue(result.allowed());
            assertEquals(BigDecimal.ZERO, result.allowedAmount());
        }

        @Test
        @DisplayName("Should throw for negative amount")
        void throwsForNegative() {
            assertThrows(ValidationException.class, () ->
                checker.check(
                    tracker,
                    new BigDecimal("-100"),
                    AccountType.TRADITIONAL_401K,
                    ContributionType.PERSONAL,
                    2025, 40, BigDecimal.ZERO, false));
        }

        @Test
        @DisplayName("Should throw for null tracker")
        void throwsForNullTracker() {
            assertThrows(MissingRequiredFieldException.class, () ->
                checker.check(
                    null,
                    new BigDecimal("1000"),
                    AccountType.TRADITIONAL_401K,
                    ContributionType.PERSONAL,
                    2025, 40, BigDecimal.ZERO, false));
        }

        @Test
        @DisplayName("Should allow unlimited taxable brokerage contributions")
        void allowsUnlimitedTaxable() {
            LimitCheckResult result = checker.check(
                tracker,
                new BigDecimal("1000000"),
                AccountType.TAXABLE_BROKERAGE,
                ContributionType.PERSONAL,
                2025, 40, BigDecimal.ZERO, false);

            assertTrue(result.allowed());
            assertEquals(0, new BigDecimal("1000000").compareTo(result.allowedAmount()));
        }
    }

    // Helper method
    private ContributionRecord createRecord(
            String accountId, AccountType type, ContributionType source, double amount) {
        return ContributionRecord.builder()
            .accountId(accountId)
            .accountType(type)
            .source(source)
            .amount(amount)
            .year(2025)
            .date(LocalDate.of(2025, 1, 15))
            .isCatchUp(false)
            .build();
    }
}
