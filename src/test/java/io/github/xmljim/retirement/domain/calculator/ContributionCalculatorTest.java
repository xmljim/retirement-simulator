package io.github.xmljim.retirement.domain.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.calculator.impl.DefaultContributionCalculator;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.value.ContributionConfig;
import io.github.xmljim.retirement.domain.value.ContributionResult;
import io.github.xmljim.retirement.domain.value.MatchingPolicy;

@DisplayName("ContributionCalculator Tests")
class ContributionCalculatorTest {

    private ContributionCalculator calculator;
    private static final LocalDate RETIREMENT_DATE = LocalDate.of(2033, 1, 1);

    @BeforeEach
    void setUp() {
        calculator = new DefaultContributionCalculator();
    }

    @Nested
    @DisplayName("isRetired")
    class IsRetiredTests {

        @Test
        @DisplayName("Should return false when before retirement date")
        void notRetiredBeforeDate() {
            LocalDate currentDate = LocalDate.of(2025, 6, 15);
            assertFalse(calculator.isRetired(currentDate, RETIREMENT_DATE));
        }

        @Test
        @DisplayName("Should return true on retirement date")
        void retiredOnDate() {
            assertTrue(calculator.isRetired(RETIREMENT_DATE, RETIREMENT_DATE));
        }

        @Test
        @DisplayName("Should return true after retirement date")
        void retiredAfterDate() {
            LocalDate currentDate = LocalDate.of(2035, 1, 1);
            assertTrue(calculator.isRetired(currentDate, RETIREMENT_DATE));
        }

        @Test
        @DisplayName("Should throw for null current date")
        void nullCurrentDate() {
            assertThrows(IllegalArgumentException.class, () ->
                calculator.isRetired(null, RETIREMENT_DATE));
        }

        @Test
        @DisplayName("Should throw for null retirement date")
        void nullRetirementDate() {
            assertThrows(IllegalArgumentException.class, () ->
                calculator.isRetired(LocalDate.now(), null));
        }
    }

    @Nested
    @DisplayName("calculatePersonalContributionRate")
    class PersonalContributionRateTests {

        @Test
        @DisplayName("Should return zero after retirement")
        void zeroAfterRetirement() {
            LocalDate contributionDate = LocalDate.of(2035, 1, 1);
            ContributionConfig config = ContributionConfig.personal(0.10);

            BigDecimal result = calculator.calculatePersonalContributionRate(
                contributionDate, RETIREMENT_DATE, config);

            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        @DisplayName("Should return base rate for current year")
        void baseRateCurrentYear() {
            LocalDate contributionDate = LocalDate.of(LocalDate.now().getYear(), 1, 15);
            ContributionConfig config = ContributionConfig.builder()
                .contributionType(io.github.xmljim.retirement.domain.enums.ContributionType.PERSONAL)
                .contributionRate(0.10)
                .incrementRate(0.01)
                .incrementMonth(Month.JUNE)
                .build();

            BigDecimal result = calculator.calculatePersonalContributionRate(
                contributionDate, RETIREMENT_DATE, config);

            // Before increment month in year 0, should get base rate
            assertTrue(result.compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @DisplayName("Should throw for null contribution date")
        void nullContributionDate() {
            ContributionConfig config = ContributionConfig.personal(0.10);

            assertThrows(IllegalArgumentException.class, () ->
                calculator.calculatePersonalContributionRate(null, RETIREMENT_DATE, config));
        }

        @Test
        @DisplayName("Should throw for null config")
        void nullConfig() {
            LocalDate contributionDate = LocalDate.of(2025, 6, 15);

            assertThrows(IllegalArgumentException.class, () ->
                calculator.calculatePersonalContributionRate(contributionDate, RETIREMENT_DATE, null));
        }
    }

    @Nested
    @DisplayName("calculateEmployerContributionRate")
    class EmployerContributionRateTests {

        @Test
        @DisplayName("Should return contribution rate while working")
        void contributionRateWhileWorking() {
            LocalDate contributionDate = LocalDate.of(2025, 6, 15);
            ContributionConfig config = ContributionConfig.employer(0.04);

            BigDecimal result = calculator.calculateEmployerContributionRate(
                contributionDate, RETIREMENT_DATE, config);

            assertEquals(0, new BigDecimal("0.04").compareTo(
                result.setScale(2, java.math.RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should return zero after retirement")
        void zeroAfterRetirement() {
            LocalDate contributionDate = LocalDate.of(2035, 1, 1);
            ContributionConfig config = ContributionConfig.employer(0.04);

            BigDecimal result = calculator.calculateEmployerContributionRate(
                contributionDate, RETIREMENT_DATE, config);

            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should return calculator from factory")
        void factoryReturnsCalculator() {
            ContributionCalculator factoryCalculator = CalculatorFactory.contributionCalculator();
            assertFalse(factoryCalculator.isRetired(LocalDate.of(2025, 1, 1), RETIREMENT_DATE));
        }
    }

    // =========================================================================
    // Dollar Amount Calculation Tests (Issue #12)
    // =========================================================================

    @Nested
    @DisplayName("calculatePersonalContributionAmount")
    class PersonalContributionAmountTests {

        @Test
        @DisplayName("Should calculate contribution amount from salary and rate")
        void calculateContributionAmount() {
            BigDecimal monthlySalary = new BigDecimal("5000");
            BigDecimal contributionRate = new BigDecimal("0.10"); // 10%

            BigDecimal result = calculator.calculatePersonalContributionAmount(
                monthlySalary, contributionRate);

            // 5000 * 0.10 = 500
            assertEquals(0, new BigDecimal("500.00").compareTo(result));
        }

        @Test
        @DisplayName("Should return zero for zero rate")
        void zeroRate() {
            BigDecimal monthlySalary = new BigDecimal("5000");

            BigDecimal result = calculator.calculatePersonalContributionAmount(
                monthlySalary, BigDecimal.ZERO);

            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        @DisplayName("Should return zero for null rate")
        void nullRate() {
            BigDecimal monthlySalary = new BigDecimal("5000");

            BigDecimal result = calculator.calculatePersonalContributionAmount(
                monthlySalary, null);

            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        @DisplayName("Should throw for null salary")
        void nullSalary() {
            assertThrows(MissingRequiredFieldException.class, () ->
                calculator.calculatePersonalContributionAmount(null, new BigDecimal("0.10")));
        }

        @Test
        @DisplayName("Should round to two decimal places")
        void roundToTwoDecimals() {
            BigDecimal monthlySalary = new BigDecimal("3333.33");
            BigDecimal contributionRate = new BigDecimal("0.10");

            BigDecimal result = calculator.calculatePersonalContributionAmount(
                monthlySalary, contributionRate);

            // 3333.33 * 0.10 = 333.333 -> 333.33
            assertEquals(0, new BigDecimal("333.33").compareTo(result));
        }
    }

    @Nested
    @DisplayName("calculateEmployerMatchAmount")
    class EmployerMatchAmountTests {

        @Test
        @DisplayName("Should calculate match with simple policy")
        void simpleMatchPolicy() {
            BigDecimal employeeRate = new BigDecimal("0.06"); // 6%
            BigDecimal monthlySalary = new BigDecimal("5000");
            // 50% match up to 6%
            MatchingPolicy policy = MatchingPolicy.simple(0.50, 0.06);

            BigDecimal result = calculator.calculateEmployerMatchAmount(
                employeeRate, monthlySalary, policy);

            // Match rate = 50% of 6% = 3%, so 5000 * 0.03 = 150
            assertEquals(0, new BigDecimal("150.00").compareTo(result));
        }

        @Test
        @DisplayName("Should cap match at policy limit")
        void matchCappedAtLimit() {
            BigDecimal employeeRate = new BigDecimal("0.10"); // 10%
            BigDecimal monthlySalary = new BigDecimal("5000");
            // 50% match up to 6% - employee contributes more than cap
            MatchingPolicy policy = MatchingPolicy.simple(0.50, 0.06);

            BigDecimal result = calculator.calculateEmployerMatchAmount(
                employeeRate, monthlySalary, policy);

            // Match capped at 50% of 6% = 3%, so 5000 * 0.03 = 150
            assertEquals(0, new BigDecimal("150.00").compareTo(result));
        }

        @Test
        @DisplayName("Should return zero for null policy")
        void nullPolicy() {
            BigDecimal employeeRate = new BigDecimal("0.06");
            BigDecimal monthlySalary = new BigDecimal("5000");

            BigDecimal result = calculator.calculateEmployerMatchAmount(
                employeeRate, monthlySalary, null);

            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        @DisplayName("Should return zero for no-match policy")
        void noMatchPolicy() {
            BigDecimal employeeRate = new BigDecimal("0.06");
            BigDecimal monthlySalary = new BigDecimal("5000");
            MatchingPolicy policy = MatchingPolicy.none();

            BigDecimal result = calculator.calculateEmployerMatchAmount(
                employeeRate, monthlySalary, policy);

            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        @DisplayName("Should return zero for zero employee contribution")
        void zeroEmployeeContribution() {
            BigDecimal monthlySalary = new BigDecimal("5000");
            MatchingPolicy policy = MatchingPolicy.simple(0.50, 0.06);

            BigDecimal result = calculator.calculateEmployerMatchAmount(
                BigDecimal.ZERO, monthlySalary, policy);

            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        @DisplayName("Should throw for null salary")
        void nullSalary() {
            MatchingPolicy policy = MatchingPolicy.simple(0.50, 0.06);

            assertThrows(MissingRequiredFieldException.class, () ->
                calculator.calculateEmployerMatchAmount(
                    new BigDecimal("0.06"), null, policy));
        }
    }

    @Nested
    @DisplayName("calculateMonthlyContribution (without IRS rules)")
    class MonthlyContributionWithoutRulesTests {

        @Test
        @DisplayName("Should calculate contribution with employer match")
        void contributionWithMatch() {
            BigDecimal monthlySalary = new BigDecimal("5000");
            LocalDate contributionDate = LocalDate.of(2025, 6, 15);
            ContributionConfig config = ContributionConfig.personal(0.10); // 10%
            MatchingPolicy matchPolicy = MatchingPolicy.simple(0.50, 0.06); // 50% up to 6%

            ContributionResult result = calculator.calculateMonthlyContribution(
                monthlySalary,
                BigDecimal.ZERO, // YTD
                2025,
                40, // age
                contributionDate,
                RETIREMENT_DATE,
                config,
                matchPolicy,
                AccountType.TRADITIONAL_401K
            );

            // Personal: 5000 * 0.10 = 500
            assertTrue(result.personalContribution().compareTo(new BigDecimal("400")) > 0);
            // Employer: 50% of 6% = 3%, 5000 * 0.03 = 150
            assertEquals(0, new BigDecimal("150.00").compareTo(result.employerMatch()));
            // Total should be personal + employer
            assertEquals(0, result.totalContribution().compareTo(
                result.personalContribution().add(result.employerMatch())));
            // Should not have limit applied (no IRS rules)
            assertFalse(result.limitApplied());
        }

        @Test
        @DisplayName("Should return zero contribution when retired")
        void zeroWhenRetired() {
            BigDecimal monthlySalary = new BigDecimal("5000");
            LocalDate contributionDate = LocalDate.of(2035, 6, 15); // After retirement
            ContributionConfig config = ContributionConfig.personal(0.10);

            ContributionResult result = calculator.calculateMonthlyContribution(
                monthlySalary,
                BigDecimal.ZERO,
                2035,
                50,
                contributionDate,
                RETIREMENT_DATE,
                config,
                null,
                AccountType.TRADITIONAL_401K
            );

            assertEquals(0, BigDecimal.ZERO.compareTo(result.personalContribution()));
            assertEquals(0, BigDecimal.ZERO.compareTo(result.employerMatch()));
            assertFalse(result.hasContributions());
        }

        @Test
        @DisplayName("Should track year-to-date correctly")
        void tracksYearToDate() {
            BigDecimal monthlySalary = new BigDecimal("5000");
            BigDecimal existingYtd = new BigDecimal("2000");
            LocalDate contributionDate = LocalDate.of(2025, 6, 15);
            ContributionConfig config = ContributionConfig.personal(0.10);

            ContributionResult result = calculator.calculateMonthlyContribution(
                monthlySalary,
                existingYtd,
                2025,
                40,
                contributionDate,
                RETIREMENT_DATE,
                config,
                null,
                AccountType.TRADITIONAL_401K
            );

            // YTD after should be existingYtd + personal contribution
            BigDecimal expectedYtd = existingYtd.add(result.personalContribution());
            assertEquals(0, expectedYtd.compareTo(result.yearToDateAfter()));
        }

        @Test
        @DisplayName("Should throw for null required parameters")
        void throwsForNullParameters() {
            BigDecimal salary = new BigDecimal("5000");
            LocalDate date = LocalDate.of(2025, 6, 15);
            ContributionConfig config = ContributionConfig.personal(0.10);

            assertThrows(MissingRequiredFieldException.class, () ->
                calculator.calculateMonthlyContribution(
                    null, BigDecimal.ZERO, 2025, 40, date, RETIREMENT_DATE,
                    config, null, AccountType.TRADITIONAL_401K));

            assertThrows(MissingRequiredFieldException.class, () ->
                calculator.calculateMonthlyContribution(
                    salary, BigDecimal.ZERO, 2025, 40, null, RETIREMENT_DATE,
                    config, null, AccountType.TRADITIONAL_401K));

            assertThrows(MissingRequiredFieldException.class, () ->
                calculator.calculateMonthlyContribution(
                    salary, BigDecimal.ZERO, 2025, 40, date, null,
                    config, null, AccountType.TRADITIONAL_401K));

            assertThrows(MissingRequiredFieldException.class, () ->
                calculator.calculateMonthlyContribution(
                    salary, BigDecimal.ZERO, 2025, 40, date, RETIREMENT_DATE,
                    null, null, AccountType.TRADITIONAL_401K));

            assertThrows(MissingRequiredFieldException.class, () ->
                calculator.calculateMonthlyContribution(
                    salary, BigDecimal.ZERO, 2025, 40, date, RETIREMENT_DATE,
                    config, null, null));
        }
    }

    @Nested
    @DisplayName("ContributionResult")
    class ContributionResultTests {

        @Test
        @DisplayName("Should create result with calculated total")
        void createWithCalculatedTotal() {
            ContributionResult result = ContributionResult.of(
                new BigDecimal("500"),
                new BigDecimal("150"),
                new BigDecimal("2000"),
                AccountType.TRADITIONAL_401K,
                false
            );

            assertEquals(0, new BigDecimal("650").compareTo(result.totalContribution()));
            assertTrue(result.hasContributions());
        }

        @Test
        @DisplayName("Should create zero result")
        void createZeroResult() {
            ContributionResult result = ContributionResult.zero(
                new BigDecimal("1000"),
                AccountType.TRADITIONAL_401K
            );

            assertEquals(0, BigDecimal.ZERO.compareTo(result.personalContribution()));
            assertEquals(0, BigDecimal.ZERO.compareTo(result.employerMatch()));
            assertEquals(0, BigDecimal.ZERO.compareTo(result.totalContribution()));
            assertEquals(0, new BigDecimal("1000").compareTo(result.yearToDateAfter()));
            assertFalse(result.hasContributions());
            assertFalse(result.limitApplied());
        }

        @Test
        @DisplayName("hasContributions should return false when total is zero")
        void hasContributionsFalseWhenZero() {
            ContributionResult result = new ContributionResult(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                AccountType.TRADITIONAL_401K,
                false
            );

            assertFalse(result.hasContributions());
        }
    }
}
