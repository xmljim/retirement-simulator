package io.github.xmljim.retirement.domain.calculator.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.calculator.TestIrsLimitsFixture;
import io.github.xmljim.retirement.domain.calculator.YTDContributionTracker;
import io.github.xmljim.retirement.domain.config.IrsContributionLimits;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.enums.ContributionType;
import io.github.xmljim.retirement.domain.enums.LimitCategory;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.value.ContributionRecord;
import io.github.xmljim.retirement.domain.value.YTDSummary;

@DisplayName("DefaultYTDContributionTracker Tests")
class DefaultYTDContributionTrackerTest {

    private Secure2ContributionRules irsRules;
    private YTDContributionTracker tracker;

    @BeforeEach
    void setUp() {
        IrsContributionLimits limits = TestIrsLimitsFixture.createTestLimits();
        irsRules = new Secure2ContributionRules(limits);
        tracker = new DefaultYTDContributionTracker(irsRules);
    }

    @Nested
    @DisplayName("Immutability Tests")
    class ImmutabilityTests {

        @Test
        @DisplayName("Should return new tracker on recordContribution")
        void returnsNewTracker() {
            ContributionRecord record = createRecord(
                "401k", AccountType.TRADITIONAL_401K, ContributionType.PERSONAL, 500);

            YTDContributionTracker newTracker = tracker.recordContribution(record);

            assertNotSame(tracker, newTracker);
        }

        @Test
        @DisplayName("Original tracker should be unchanged after recording")
        void originalUnchanged() {
            ContributionRecord record = createRecord(
                "401k", AccountType.TRADITIONAL_401K, ContributionType.PERSONAL, 500);

            tracker.recordContribution(record);

            assertEquals(BigDecimal.ZERO,
                tracker.getYTDPersonalContributions(2025, LimitCategory.EMPLOYER_401K));
        }
    }

    @Nested
    @DisplayName("Personal Contribution Tracking")
    class PersonalContributionTests {

        @Test
        @DisplayName("Should track single personal contribution")
        void tracksSingleContribution() {
            ContributionRecord record = createRecord(
                "401k", AccountType.TRADITIONAL_401K, ContributionType.PERSONAL, 5000);

            tracker = tracker.recordContribution(record);

            assertEquals(0, new BigDecimal("5000").compareTo(
                tracker.getYTDPersonalContributions(2025, LimitCategory.EMPLOYER_401K)));
        }

        @Test
        @DisplayName("Should aggregate multiple personal contributions")
        void aggregatesMultipleContributions() {
            tracker = tracker
                .recordContribution(createRecord("401k", AccountType.TRADITIONAL_401K,
                    ContributionType.PERSONAL, 1000))
                .recordContribution(createRecord("401k", AccountType.TRADITIONAL_401K,
                    ContributionType.PERSONAL, 2000))
                .recordContribution(createRecord("401k", AccountType.TRADITIONAL_401K,
                    ContributionType.PERSONAL, 1500));

            assertEquals(0, new BigDecimal("4500").compareTo(
                tracker.getYTDPersonalContributions(2025, LimitCategory.EMPLOYER_401K)));
        }

        @Test
        @DisplayName("Should separate contributions by category")
        void separatesByCategory() {
            tracker = tracker
                .recordContribution(createRecord("401k", AccountType.TRADITIONAL_401K,
                    ContributionType.PERSONAL, 5000))
                .recordContribution(createRecord("ira", AccountType.TRADITIONAL_IRA,
                    ContributionType.PERSONAL, 3000));

            assertEquals(0, new BigDecimal("5000").compareTo(
                tracker.getYTDPersonalContributions(2025, LimitCategory.EMPLOYER_401K)));
            assertEquals(0, new BigDecimal("3000").compareTo(
                tracker.getYTDPersonalContributions(2025, LimitCategory.IRA)));
        }

        @Test
        @DisplayName("Should combine Traditional and Roth IRA into single category")
        void combinesIraTypes() {
            tracker = tracker
                .recordContribution(createRecord("trad-ira", AccountType.TRADITIONAL_IRA,
                    ContributionType.PERSONAL, 2000))
                .recordContribution(createRecord("roth-ira", AccountType.ROTH_IRA,
                    ContributionType.PERSONAL, 3000));

            assertEquals(0, new BigDecimal("5000").compareTo(
                tracker.getYTDPersonalContributions(2025, LimitCategory.IRA)));
        }
    }

    @Nested
    @DisplayName("Employer Contribution Tracking")
    class EmployerContributionTests {

        @Test
        @DisplayName("Should track employer contributions separately")
        void tracksEmployerSeparately() {
            tracker = tracker
                .recordContribution(createRecord("401k", AccountType.TRADITIONAL_401K,
                    ContributionType.PERSONAL, 5000))
                .recordContribution(createRecord("401k", AccountType.TRADITIONAL_401K,
                    ContributionType.EMPLOYER, 2500));

            assertEquals(0, new BigDecimal("5000").compareTo(
                tracker.getYTDPersonalContributions(2025, LimitCategory.EMPLOYER_401K)));
            assertEquals(0, new BigDecimal("2500").compareTo(
                tracker.getYTDEmployerContributions(2025, LimitCategory.EMPLOYER_401K)));
        }

        @Test
        @DisplayName("Should calculate total contributions")
        void calculatesTotal() {
            tracker = tracker
                .recordContribution(createRecord("401k", AccountType.TRADITIONAL_401K,
                    ContributionType.PERSONAL, 5000))
                .recordContribution(createRecord("401k", AccountType.TRADITIONAL_401K,
                    ContributionType.EMPLOYER, 2500));

            assertEquals(0, new BigDecimal("7500").compareTo(
                tracker.getYTDTotalContributions(2025, LimitCategory.EMPLOYER_401K)));
        }
    }

    @Nested
    @DisplayName("Year Separation Tests")
    class YearSeparationTests {

        @Test
        @DisplayName("Should separate contributions by year")
        void separatesByYear() {
            tracker = tracker
                .recordContribution(createRecordForYear("401k", AccountType.TRADITIONAL_401K,
                    ContributionType.PERSONAL, 5000, 2025))
                .recordContribution(createRecordForYear("401k", AccountType.TRADITIONAL_401K,
                    ContributionType.PERSONAL, 3000, 2024));

            assertEquals(0, new BigDecimal("5000").compareTo(
                tracker.getYTDPersonalContributions(2025, LimitCategory.EMPLOYER_401K)));
            assertEquals(0, new BigDecimal("3000").compareTo(
                tracker.getYTDPersonalContributions(2024, LimitCategory.EMPLOYER_401K)));
        }

        @Test
        @DisplayName("Should return zero for year with no contributions")
        void zeroForEmptyYear() {
            assertEquals(BigDecimal.ZERO,
                tracker.getYTDPersonalContributions(2025, LimitCategory.EMPLOYER_401K));
        }
    }

    @Nested
    @DisplayName("Records Retrieval Tests")
    class RecordsRetrievalTests {

        @Test
        @DisplayName("Should return all records for year")
        void returnsRecordsForYear() {
            ContributionRecord r1 = createRecord("401k", AccountType.TRADITIONAL_401K,
                ContributionType.PERSONAL, 1000);
            ContributionRecord r2 = createRecord("ira", AccountType.TRADITIONAL_IRA,
                ContributionType.PERSONAL, 500);

            tracker = tracker.recordContribution(r1).recordContribution(r2);

            List<ContributionRecord> records = tracker.getRecordsForYear(2025);
            assertEquals(2, records.size());
        }

        @Test
        @DisplayName("Should return empty list for year with no records")
        void emptyListForNoRecords() {
            List<ContributionRecord> records = tracker.getRecordsForYear(2025);
            assertTrue(records.isEmpty());
        }
    }

    @Nested
    @DisplayName("Summary Tests")
    class SummaryTests {

        @Test
        @DisplayName("Should generate valid summary")
        void generatesSummary() {
            tracker = tracker
                .recordContribution(createRecord("401k", AccountType.TRADITIONAL_401K,
                    ContributionType.PERSONAL, 10000))
                .recordContribution(createRecord("401k", AccountType.TRADITIONAL_401K,
                    ContributionType.EMPLOYER, 5000));

            YTDSummary summary = tracker.getSummary(2025, 40, false);

            assertNotNull(summary);
            assertEquals(2025, summary.year());
            assertEquals(0, new BigDecimal("10000").compareTo(
                summary.getPersonalContributions(LimitCategory.EMPLOYER_401K)));
            assertEquals(0, new BigDecimal("5000").compareTo(
                summary.getEmployerContributions(LimitCategory.EMPLOYER_401K)));
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw for null record")
        void throwsForNullRecord() {
            assertThrows(MissingRequiredFieldException.class, () ->
                tracker.recordContribution(null));
        }
    }

    // Helper methods
    private ContributionRecord createRecord(
            String accountId, AccountType type, ContributionType source, double amount) {
        return createRecordForYear(accountId, type, source, amount, 2025);
    }

    private ContributionRecord createRecordForYear(
            String accountId, AccountType type, ContributionType source, double amount, int year) {
        return ContributionRecord.builder()
            .accountId(accountId)
            .accountType(type)
            .source(source)
            .amount(amount)
            .year(year)
            .date(LocalDate.of(year, 1, 15))
            .isCatchUp(false)
            .build();
    }
}
