package io.github.xmljim.retirement.domain.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.calculator.impl.DefaultIncomeCalculator;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultInflationCalculator;
import io.github.xmljim.retirement.domain.value.RetirementIncome;
import io.github.xmljim.retirement.domain.value.SocialSecurityIncome;
import io.github.xmljim.retirement.domain.value.WorkingIncome;

@DisplayName("IncomeCalculator Tests")
class IncomeCalculatorTest {

    private IncomeCalculator calculator;
    private static final LocalDate RETIREMENT_DATE = LocalDate.of(2033, 1, 1);
    private static final LocalDate SOCIAL_SECURITY_DATE = LocalDate.of(2035, 1, 1);
    private static final BigDecimal SALARY = new BigDecimal("100000");
    private static final BigDecimal COLA_RATE = new BigDecimal("0.02");
    private static final BigDecimal INFLATION_RATE = new BigDecimal("0.03");
    private static final BigDecimal RETIREMENT_PCT = new BigDecimal("0.80");

    @BeforeEach
    void setUp() {
        InflationCalculator inflationCalculator = new DefaultInflationCalculator();
        calculator = new DefaultIncomeCalculator(inflationCalculator);
    }

    @Nested
    @DisplayName("calculateMonthlySalary")
    class MonthlySalaryTests {

        @Test
        @DisplayName("Should calculate monthly salary for current year while working")
        void monthlySalaryCurrentYear() {
            LocalDate salaryDate = LocalDate.of(LocalDate.now().getYear(), 1, 1);
            WorkingIncome workingIncome = WorkingIncome.of(SALARY.doubleValue(), COLA_RATE.doubleValue());

            BigDecimal result = calculator.calculateMonthlySalary(
                salaryDate, RETIREMENT_DATE, workingIncome, RETIREMENT_PCT, INFLATION_RATE);

            // For current year (year 0), should be close to base monthly salary
            BigDecimal expectedMonthly = SALARY.divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
            assertEquals(0, expectedMonthly.compareTo(result));
        }

        @Test
        @DisplayName("Should apply COLA for next year while working")
        void monthlySalaryNextYear() {
            LocalDate salaryDate = LocalDate.of(LocalDate.now().getYear() + 1, 1, 1);
            WorkingIncome workingIncome = WorkingIncome.of(SALARY.doubleValue(), COLA_RATE.doubleValue());

            BigDecimal result = calculator.calculateMonthlySalary(
                salaryDate, RETIREMENT_DATE, workingIncome, RETIREMENT_PCT, INFLATION_RATE);

            // After 1 year of COLA: (salary/12) * (1.02)^1
            BigDecimal monthlyBase = SALARY.divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);
            BigDecimal expected = monthlyBase.multiply(new BigDecimal("1.02"))
                .setScale(2, RoundingMode.HALF_UP);
            assertEquals(0, expected.compareTo(result));
        }

        @Test
        @DisplayName("Should throw for null salary date")
        void nullSalaryDate() {
            WorkingIncome workingIncome = WorkingIncome.of(SALARY.doubleValue(), COLA_RATE.doubleValue());

            assertThrows(IllegalArgumentException.class, () ->
                calculator.calculateMonthlySalary(null, RETIREMENT_DATE, workingIncome,
                    RETIREMENT_PCT, INFLATION_RATE));
        }

        @Test
        @DisplayName("Should throw for null working income")
        void nullWorkingIncome() {
            LocalDate salaryDate = LocalDate.of(2025, 1, 1);

            assertThrows(IllegalArgumentException.class, () ->
                calculator.calculateMonthlySalary(salaryDate, RETIREMENT_DATE, null,
                    RETIREMENT_PCT, INFLATION_RATE));
        }
    }

    @Nested
    @DisplayName("calculateSocialSecurityBenefit")
    class SocialSecurityTests {

        @Test
        @DisplayName("Should return zero before start date")
        void zeroBenefitBeforeStartDate() {
            LocalDate distributionDate = LocalDate.of(2025, 1, 1);
            SocialSecurityIncome ss = SocialSecurityIncome.builder()
                .monthlyBenefit(1000.00)
                .colaRate(0.03)
                .startDate(SOCIAL_SECURITY_DATE)
                .build();

            BigDecimal result = calculator.calculateSocialSecurityBenefit(distributionDate, ss);

            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        @DisplayName("Should return benefit after start date")
        void benefitAfterStartDate() {
            LocalDate distributionDate = LocalDate.of(2036, 1, 1);
            SocialSecurityIncome ss = SocialSecurityIncome.builder()
                .monthlyBenefit(1000.00)
                .colaRate(0.03)
                .startDate(SOCIAL_SECURITY_DATE)
                .build();

            BigDecimal result = calculator.calculateSocialSecurityBenefit(distributionDate, ss);

            // Should have some positive value with inflation applied
            assertTrue(result.compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @DisplayName("Should throw for null distribution date")
        void nullDistributionDate() {
            SocialSecurityIncome ss = SocialSecurityIncome.builder()
                .monthlyBenefit(1000.00)
                .startDate(SOCIAL_SECURITY_DATE)
                .build();

            assertThrows(IllegalArgumentException.class, () ->
                calculator.calculateSocialSecurityBenefit(null, ss));
        }

        @Test
        @DisplayName("Should throw for null social security income")
        void nullSocialSecurityIncome() {
            LocalDate distributionDate = LocalDate.of(2036, 1, 1);

            assertThrows(IllegalArgumentException.class, () ->
                calculator.calculateSocialSecurityBenefit(distributionDate, null));
        }
    }

    @Nested
    @DisplayName("calculateOtherRetirementIncome")
    class OtherRetirementIncomeTests {

        @Test
        @DisplayName("Should return zero before start date")
        void zeroIncomeBeforeStartDate() {
            LocalDate distributionDate = LocalDate.of(2025, 1, 1);
            RetirementIncome income = RetirementIncome.fixedPension("Pension", 500.00, RETIREMENT_DATE);

            BigDecimal result = calculator.calculateOtherRetirementIncome(distributionDate, income);

            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        @DisplayName("Should return income after start date")
        void incomeAfterStartDate() {
            LocalDate distributionDate = LocalDate.of(2036, 1, 1);
            RetirementIncome income = RetirementIncome.builder()
                .name("Pension")
                .monthlyAmount(500.00)
                .adjustmentRate(0.02)
                .startDate(RETIREMENT_DATE)
                .build();

            BigDecimal result = calculator.calculateOtherRetirementIncome(distributionDate, income);

            // Should have some positive value
            assertTrue(result.compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @DisplayName("Should return zero for zero monthly amount")
        void zeroMonthlyAmount() {
            LocalDate distributionDate = LocalDate.of(2036, 1, 1);
            RetirementIncome income = RetirementIncome.builder()
                .monthlyAmount(0.0)
                .adjustmentRate(0.02)
                .startDate(RETIREMENT_DATE)
                .build();

            BigDecimal result = calculator.calculateOtherRetirementIncome(distributionDate, income);

            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        @DisplayName("Should throw for null retirement income")
        void nullRetirementIncome() {
            LocalDate distributionDate = LocalDate.of(2036, 1, 1);

            assertThrows(IllegalArgumentException.class, () ->
                calculator.calculateOtherRetirementIncome(distributionDate, null));
        }
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should return calculator from factory")
        void factoryReturnsCalculator() {
            IncomeCalculator factoryCalculator = CalculatorFactory.incomeCalculator();

            LocalDate distributionDate = LocalDate.of(2025, 1, 1);
            SocialSecurityIncome ss = SocialSecurityIncome.builder()
                .monthlyBenefit(1000.00)
                .startDate(SOCIAL_SECURITY_DATE)
                .build();

            BigDecimal result = factoryCalculator.calculateSocialSecurityBenefit(distributionDate, ss);
            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }
    }
}
