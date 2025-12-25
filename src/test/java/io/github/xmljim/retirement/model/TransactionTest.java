package io.github.xmljim.retirement.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDate;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.*;

import static io.github.xmljim.retirement.model.PortfolioParameters.MonthlyRetirementIncome;
import static io.github.xmljim.retirement.model.PortfolioParameters.Investments;
import static io.github.xmljim.retirement.model.PortfolioParameters.Contribution;
import static io.github.xmljim.retirement.functions.Functions.*;

/**
 * Tests for Transaction class.
 * Uses deprecated PortfolioParameters - will be updated in Issue #10.
 */
@SuppressWarnings("deprecation")
class TransactionTest {

    @Test
    @DisplayName("Should return not retired when transaction date is before planned retirement date")
    void testIsRetiredBeforeRetirementDate() {

        final PortfolioParameters parameters = PortfolioParameters.builder()
            .dateOfBirth(LocalDate.of(1968, 8, 29))
            .plannedRetirementDate(LocalDate.of(2034, 1, 1))
            .investments(Investments.builder()
                .startingBalance(1_140_000.00)
                .build())
            .build();

        final LocalDate transactionDate = LocalDate.of(LocalDate.now().getYear(), LocalDate.now().getMonthValue(), 1);

        final Transaction transaction = new Transaction(parameters, transactionDate);
        assertFalse(transaction.isRetired());
    }

    @Test
    @DisplayName("Should return retired when transaction date is after planned retirement date")
    void testIsRetiredAfterRetirementDate() {
        final PortfolioParameters parameters = PortfolioParameters.builder()
            .dateOfBirth(LocalDate.of(1968, 8, 29))
            .plannedRetirementDate(LocalDate.of(2034, 1, 1))
            .investments(Investments.builder()
                .startingBalance(1_140_000.00)
                .build())
            .build();

        final LocalDate transactionDate = LocalDate.of(2035, LocalDate.now().getMonthValue(), 1);

        final Transaction transaction = new Transaction(parameters, transactionDate);
        assertTrue(transaction.isRetired());

    }

    @Test
    @DisplayName("Should return starting balance equal to investments starting balance")
    void getStartBalance() {
        final PortfolioParameters parameters = PortfolioParameters.builder()
            .dateOfBirth(LocalDate.of(1968, 8, 29))
            .plannedRetirementDate(LocalDate.of(2034, 1, 1))
            .investments(Investments.builder()
                .startingBalance(1_140_000.00)
                .build())
            .build();

        final LocalDate transactionDate = LocalDate.of(LocalDate.now().getYear(), LocalDate.now().getMonthValue(), 1);

        final Transaction transaction = new Transaction(parameters, transactionDate);
        assertEquals(parameters.getInvestments().getStartingBalance(), transaction.getStartBalance());
    }

    @Test
    @DisplayName("Should return CONTRIBUTION type before retirement date")
    void testGetTypeBeforeRetirementDate() {
        final PortfolioParameters parameters = PortfolioParameters.builder()
            .dateOfBirth(LocalDate.of(1968, 8, 29))
            .plannedRetirementDate(LocalDate.of(2034, 1, 1))
            .investments(Investments.builder()
                .startingBalance(1_140_000.00)
                .build())
            .build();

        final LocalDate transactionDate = LocalDate.of(LocalDate.now().getYear(), LocalDate.now().getMonthValue(), 1);

        final Transaction transaction = new Transaction(parameters, transactionDate);
        assertEquals(TransactionType.CONTRIBUTION, transaction.getType());
    }

    @Test
    @DisplayName("Should return WITHDRAWAL type on or after retirement date")
    void testGetTypeAfterRetirementDate() {
        final PortfolioParameters parameters = PortfolioParameters.builder()
            .dateOfBirth(LocalDate.of(1968, 8, 29))
            .plannedRetirementDate(LocalDate.of(2034, 1, 1))
            .investments(Investments.builder()
                .startingBalance(1_140_000.00)
                .build())
            .build();

        final LocalDate transactionDate = LocalDate.of(2034, 1, 1);

        final Transaction transaction = new Transaction(parameters, transactionDate);
        assertEquals(TransactionType.WITHDRAWAL, transaction.getType());
    }

    @Test
    @DisplayName("Should return correct age at transaction date")
    void testGetAge() {
        final PortfolioParameters parameters = PortfolioParameters.builder()
            .dateOfBirth(LocalDate.of(1968, 8, 29))
            .plannedRetirementDate(LocalDate.of(2034, 1, 1))
            .investments(Investments.builder()
                .startingBalance(1_140_000.00)
                .build())
            .build();

        final LocalDate transactionDate = LocalDate.of(LocalDate.now().getYear(), LocalDate.now().getMonthValue(), 1);
        final Transaction transaction = new Transaction(parameters, transactionDate);
        assertEquals(57, transaction.getAge());
    }

    @Test
    @DisplayName("Should return 0.0 for Social Security before Social Security start date")
    void testGetSocialSecurityBeforeRetirementDate() {
        final PortfolioParameters parameters = PortfolioParameters.builder()
            .dateOfBirth(LocalDate.of(1968, 8, 29))
            .plannedRetirementDate(LocalDate.of(2034, 1, 1))
            .investments(Investments.builder()
                .startingBalance(1_140_000.00)
                .build())
            .monthlyRetirementIncome(MonthlyRetirementIncome.builder()
                .socialSecurity(4_018.00)
                .socialSecurityAdjustmentRate(0.028)
                .startSocialSecurity(LocalDate.of(2035, 9, 1))
                .build())
            .build();

        final LocalDate transactionDate = LocalDate.of(LocalDate.now().getYear(), LocalDate.now().getMonthValue(), 1);
        final Transaction transaction = new Transaction(parameters, transactionDate);
        assertEquals(0.0, transaction.getSocialSecurity());
    }

    @Test
    @DisplayName("Should return 0.0 for Social Security after retirement but before Social Security start date")
    void testGetSocialSecurityAfterRetirementDateAndBeforeSocialSecurity() {
        final PortfolioParameters parameters = PortfolioParameters.builder()
            .dateOfBirth(LocalDate.of(1968, 8, 29))
            .plannedRetirementDate(LocalDate.of(2034, 1, 1))
            .investments(Investments.builder()
                .startingBalance(1_140_000.00)
                .build())
            .monthlyRetirementIncome(MonthlyRetirementIncome.builder()
                .socialSecurity(4_018.00)
                .socialSecurityAdjustmentRate(0.028)
                .startSocialSecurity(LocalDate.of(2035, 9, 1))
                .build())
            .build();

        final LocalDate transactionDate = LocalDate.of(2034, 1, 1);
        final Transaction transaction = new Transaction(parameters, transactionDate);
        assertTrue(transaction.isRetired());
        assertEquals(0.0, transaction.getSocialSecurity());
    }

    @Test
    @DisplayName("Should return adjusted Social Security after Social Security start date and after retirement")
    void testGetSocialSecurityAfterRetirementDateAndAfterSocialSecurity() {
        final PortfolioParameters parameters = PortfolioParameters.builder()
            .dateOfBirth(LocalDate.of(1968, 8, 29))
            .plannedRetirementDate(LocalDate.of(2034, 1, 1))
            .investments(Investments.builder()
                .startingBalance(100_000)
                .build())
            .monthlyRetirementIncome(MonthlyRetirementIncome.builder()
                .socialSecurity(4_018.00)
                .socialSecurityAdjustmentRate(0.028)
                .startSocialSecurity(LocalDate.of(2035, 9, 1))
                .build())
            .build();

        final LocalDate transactionDate = LocalDate.of(2035, 9, 1);
        final Transaction transaction = new Transaction(parameters, transactionDate);

        final double expectedSocialSecurity = INFLATED.apply(parameters.getMonthlyRetirementIncome().getSocialSecurity(),
            parameters.getMonthlyRetirementIncome().getSocialSecurityAdjustmentRate(),
            transactionDate.getYear() - LocalDate.now().getYear());

        assertTrue(transaction.isRetired());
        assertEquals(expectedSocialSecurity, transaction.getSocialSecurity());
    }

    @Test
    @DisplayName("Should return adjusted other retirement income after retirement date and after other income start date")
    void testGetOtherRetirementIncomeAfterRetirementDate() {
        final PortfolioParameters parameters = PortfolioParameters.builder()
            .dateOfBirth(LocalDate.of(1968, 8, 29))
            .plannedRetirementDate(LocalDate.of(2034, 1, 1))
            .investments(Investments.builder()
                .startingBalance(100_000)
                .build())
            .monthlyRetirementIncome(MonthlyRetirementIncome.builder()
                .socialSecurity(4_018.00)
                .socialSecurityAdjustmentRate(0.028)
                .startSocialSecurity(LocalDate.of(2035, 9, 1))
                .otherMonthlyIncome(300.00)
                .otherMonthlyIncomeAdjustmentRate(0.0)
                .startOtherMonthlyIncome(LocalDate.of(2034, 1, 1))
                .build())
            .build();

        final LocalDate transactionDate = LocalDate.of(2035, 9, 1);
        final Transaction transaction = new Transaction(parameters, transactionDate);

        final double expectedOtherIncome =
            INFLATED.apply(parameters.getMonthlyRetirementIncome().getOtherMonthlyIncome(),
                parameters.getMonthlyRetirementIncome().getOtherMonthlyIncomeAdjustmentRate(),
                transactionDate.getYear() - LocalDate.now().getYear());

        assertTrue(transaction.isRetired());
        assertEquals(expectedOtherIncome, transaction.getOtherRetirement());
    }

    @Test
    @DisplayName("Should return 0.0 for other retirement income before retirement date")
    void testGetOtherRetirementIncomeBeforeRetirementDate() {
        final PortfolioParameters parameters = PortfolioParameters.builder()
            .dateOfBirth(LocalDate.of(1968, 8, 29))
            .plannedRetirementDate(LocalDate.of(2034, 1, 1))
            .investments(Investments.builder()
                .startingBalance(100_000)
                .build())
            .monthlyRetirementIncome(MonthlyRetirementIncome.builder()
                .socialSecurity(4_018.00)
                .socialSecurityAdjustmentRate(0.028)
                .startSocialSecurity(LocalDate.of(2035, 9, 1))
                .otherMonthlyIncome(300.00)
                .otherMonthlyIncomeAdjustmentRate(0.0)
                .startOtherMonthlyIncome(LocalDate.of(2034, 1, 1))
                .build())
            .build();

        final LocalDate transactionDate = LocalDate.of(2025, 9, 1);
        final Transaction transaction = new Transaction(parameters, transactionDate);



        assertFalse(transaction.isRetired());
        assertEquals(0.0, transaction.getOtherRetirement());
    }

    @Test
    @DisplayName("Should return base personal contribution rate before increment month and before retirement date")
    void testGetPersonalContributionRateBeforeRetirementDateBeforeMonth() {
        final PortfolioParameters parameters = PortfolioParameters.builder()
            .dateOfBirth(LocalDate.of(1968, 8, 29))
            .plannedRetirementDate(LocalDate.of(2034, 1, 1))
            .investments(Investments.builder()
                .startingBalance(100_000)
                .build())
            .monthlyRetirementIncome(MonthlyRetirementIncome.builder()
                .socialSecurity(4_018.00)
                .socialSecurityAdjustmentRate(0.028)
                .startSocialSecurity(LocalDate.of(2035, 9, 1))
                .otherMonthlyIncome(300.00)
                .otherMonthlyIncomeAdjustmentRate(0.0)
                .startOtherMonthlyIncome(LocalDate.of(2034, 1, 1))
                .build())
            .addContribution(Contribution.builder()
                .contributionType(ContributionType.PERSONAL)
                .contributionRate(0.10)
                .incrementContributionRate(0.01)
                .incrementMonth(Month.JUNE)
                .build())
            .addContribution(Contribution.builder()
                .contributionType(ContributionType.EMPLOYER)
                .contributionRate(0.04)
                .build())
            .build();

        final LocalDate transactionDate = LocalDate.of(2025, 5, 1);

        final Transaction transaction = new Transaction(parameters, transactionDate);
        assertFalse(transaction.isRetired());
        assertEquals(0.1, transaction.getPersonalContributionRate());
    }

    @Test
    @DisplayName("Should return incremented personal contribution rate after increment month and before retirement date")
    void testGetPersonalContributionRateBeforeRetirementDateAfterMonth() {
        final PortfolioParameters parameters = PortfolioParameters.builder()
            .dateOfBirth(LocalDate.of(1968, 8, 29))
            .plannedRetirementDate(LocalDate.of(2034, 1, 1))
            .investments(Investments.builder()
                .startingBalance(100_000)
                .build())
            .monthlyRetirementIncome(MonthlyRetirementIncome.builder()
                .socialSecurity(4_018.00)
                .socialSecurityAdjustmentRate(0.028)
                .startSocialSecurity(LocalDate.of(2035, 9, 1))
                .otherMonthlyIncome(300.00)
                .otherMonthlyIncomeAdjustmentRate(0.0)
                .startOtherMonthlyIncome(LocalDate.of(2034, 1, 1))
                .build())
            .addContribution(Contribution.builder()
                .contributionType(ContributionType.PERSONAL)
                .contributionRate(0.10)
                .incrementContributionRate(0.01)
                .incrementMonth(Month.JUNE)
                .build())
            .addContribution(Contribution.builder()
                .contributionType(ContributionType.EMPLOYER)
                .contributionRate(0.04)
                .build())
            .build();

        final LocalDate transactionDate = LocalDate.of(2025, 10, 1);

        final Transaction transaction = new Transaction(parameters, transactionDate);
        assertFalse(transaction.isRetired());
        assertEquals(0.11, transaction.getPersonalContributionRate());
    }

    @Test
    @DisplayName("Should return 0.0 for personal contribution rate after retirement date")
    void testGetPersonalContributionRateAfterRetirementDate() {
        final PortfolioParameters parameters = PortfolioParameters.builder()
            .dateOfBirth(LocalDate.of(1968, 8, 29))
            .plannedRetirementDate(LocalDate.of(2034, 1, 1))
            .investments(Investments.builder()
                .startingBalance(100_000)
                .build())
            .monthlyRetirementIncome(MonthlyRetirementIncome.builder()
                .socialSecurity(4_018.00)
                .socialSecurityAdjustmentRate(0.028)
                .startSocialSecurity(LocalDate.of(2035, 9, 1))
                .otherMonthlyIncome(300.00)
                .otherMonthlyIncomeAdjustmentRate(0.0)
                .startOtherMonthlyIncome(LocalDate.of(2034, 1, 1))
                .build())
            .addContribution(Contribution.builder()
                .contributionType(ContributionType.PERSONAL)
                .contributionRate(0.10)
                .incrementContributionRate(0.01)
                .incrementMonth(Month.JUNE)
                .build())
            .addContribution(Contribution.builder()
                .contributionType(ContributionType.EMPLOYER)
                .contributionRate(0.04)
                .build())
            .build();

        final LocalDate transactionDate = LocalDate.of(2034, 1, 1);

        final Transaction transaction = new Transaction(parameters, transactionDate);
        assertTrue(transaction.isRetired());
        assertEquals(0.0, transaction.getPersonalContributionRate());
    }

    @Test
    @DisplayName("Should return employer contribution rate before retirement date")
    void testGetEmployerContributionRateBeforeRetirementDate() {
        final PortfolioParameters parameters = PortfolioParameters.builder()
            .dateOfBirth(LocalDate.of(1968, 8, 29))
            .plannedRetirementDate(LocalDate.of(2034, 1, 1))
            .investments(Investments.builder()
                .startingBalance(100_000)
                .build())
            .monthlyRetirementIncome(MonthlyRetirementIncome.builder()
                .socialSecurity(4_018.00)
                .socialSecurityAdjustmentRate(0.028)
                .startSocialSecurity(LocalDate.of(2035, 9, 1))
                .otherMonthlyIncome(300.00)
                .otherMonthlyIncomeAdjustmentRate(0.0)
                .startOtherMonthlyIncome(LocalDate.of(2034, 1, 1))
                .build())
            .addContribution(Contribution.builder()
                .contributionType(ContributionType.PERSONAL)
                .contributionRate(0.10)
                .incrementContributionRate(0.01)
                .incrementMonth(Month.JUNE)
                .build())
            .addContribution(Contribution.builder()
                .contributionType(ContributionType.EMPLOYER)
                .contributionRate(0.04)
                .build())
            .build();

        final LocalDate transactionDate = LocalDate.of(2025, 10, 1);

        final Transaction transaction = new Transaction(parameters, transactionDate);
        assertFalse(transaction.isRetired());
        assertEquals(0.04, transaction.getEmployerContributionRate());
    }

    @Test
    @DisplayName("Should return 0.0 for employer contribution rate after retirement date")
    void testGetEmployerContributionRateAfterRetirementDate() {
        final PortfolioParameters parameters = PortfolioParameters.builder()
            .dateOfBirth(LocalDate.of(1968, 8, 29))
            .plannedRetirementDate(LocalDate.of(2034, 1, 1))
            .investments(Investments.builder()
                .startingBalance(100_000)
                .build())
            .monthlyRetirementIncome(MonthlyRetirementIncome.builder()
                .socialSecurity(4_018.00)
                .socialSecurityAdjustmentRate(0.028)
                .startSocialSecurity(LocalDate.of(2035, 9, 1))
                .otherMonthlyIncome(300.00)
                .otherMonthlyIncomeAdjustmentRate(0.0)
                .startOtherMonthlyIncome(LocalDate.of(2034, 1, 1))
                .build())
            .addContribution(Contribution.builder()
                .contributionType(ContributionType.PERSONAL)
                .contributionRate(0.10)
                .incrementContributionRate(0.01)
                .incrementMonth(Month.JUNE)
                .build())
            .addContribution(Contribution.builder()
                .contributionType(ContributionType.EMPLOYER)
                .contributionRate(0.04)
                .build())
            .build();

        final LocalDate transactionDate = LocalDate.of(2034, 1, 1);

        final Transaction transaction = new Transaction(parameters, transactionDate);
        assertTrue(transaction.isRetired());
        assertEquals(0.0, transaction.getEmployerContributionRate());
    }
}