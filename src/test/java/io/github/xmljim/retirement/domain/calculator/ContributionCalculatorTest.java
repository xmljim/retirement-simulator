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
import io.github.xmljim.retirement.domain.value.ContributionConfig;

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
}
