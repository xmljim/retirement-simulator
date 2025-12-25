package retirement.functions;

import io.github.xmljim.retirement.model.PortfolioParameters;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import static io.github.xmljim.retirement.functions.Functions.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Functions class.
 * Uses deprecated PortfolioParameters - will be updated in Issue #10.
 */
@SuppressWarnings("deprecation")
class FunctionsTest {


   

    private static final double COLA_PCT = 0.02;
    private static final double INFLATION_PCT = 0.03;
    private static final double SALARY = 100_000.00;
    private static final double RETIREMENT_PCT = 0.80;
    private static final LocalDate RETIREMENT_DATE = LocalDate.of(2033, 1, 1);
    private static final LocalDate SOCIAL_SECURITY_DATE = LocalDate.of(2035, 1, 1);

    @Test
    void testMonthSalaryCurrent() {
        final LocalDate salaryDate = LocalDate.of(2025, 1, 1);

        final Double calculated = MONTHLY_SALARY.apply(SALARY, COLA_PCT, INFLATION_PCT, RETIREMENT_PCT, salaryDate,
            RETIREMENT_DATE);
        assertEquals(SALARY/12, calculated);
    }

    @Test
    void testMonthSalaryNextYear() {
        final LocalDate salaryDate = LocalDate.of(2026, 1, 1);
        final Double nextYearSalary = COLA.apply(SALARY / 12, COLA_PCT, 1);
        final Double calcNextYearSalary = MONTHLY_SALARY.apply(SALARY, COLA_PCT, INFLATION_PCT, RETIREMENT_PCT,
            salaryDate, RETIREMENT_DATE);
        assertEquals(calcNextYearSalary, nextYearSalary);
    }

    @Test
    void testMonthSalaryRetired() {
        final LocalDate salaryDate = LocalDate.of(2036, 1, 1);
        final long years = salaryDate.getYear() - LocalDate.now().getYear();
        final Double expected = INFLATED.apply(SALARY / 12, INFLATION_PCT, years) * RETIREMENT_PCT;

        final Double actual = MONTHLY_SALARY.apply(SALARY, COLA_PCT, INFLATION_PCT, RETIREMENT_PCT,
            salaryDate, RETIREMENT_DATE);

        assertEquals(expected, actual);
    }

    @Test
    void testInflation() {
        final double expected = 1.3439;
        final double actual = INFLATION.apply(INFLATION_PCT, 10);
        assertEquals(expected, actual, 0.0001);
    }

    @Test
    void testAdjusted() {
        final double expected = 1 / 1.3439;
        final double actual = ADJUSTED.apply(INFLATION_PCT, 10);
        assertEquals(expected, actual, 0.0001);
    }

    @Test
    void testCola() {
        final double expected = Math.pow(1 + COLA_PCT, 10) * SALARY;
        final double actual = COLA.apply(SALARY, COLA_PCT, 10);
        assertEquals(expected, actual, 0.0001);
    }

    @Test
    void testSocialSecurityNotRetired() {
        final LocalDate distributionDate = LocalDate.of(2025, 1, 1);
        final Double monthlySocialSecurity = 1000.00;

        final Double actual = SOCIAL_SECURITY.apply(monthlySocialSecurity, INFLATION_PCT, distributionDate,
            SOCIAL_SECURITY_DATE);
        assertEquals(0, actual, 0.0001);
    }

    @Test
    void testSocialSecurityRetired() {
        final LocalDate distributionDate = LocalDate.of(2036, 1, 1);
        final Double monthlySocialSecurity = 1000.00;
        final long years = distributionDate.getYear() - LocalDate.now().getYear();
        final Double expected = INFLATED.apply(monthlySocialSecurity, INFLATION_PCT, years);

        final Double actual = SOCIAL_SECURITY.apply(monthlySocialSecurity, INFLATION_PCT, distributionDate,
            SOCIAL_SECURITY_DATE);
        assertEquals(expected, actual, 0.0001);
    }

    @Test
    void testSocialSecurityPortfolioParameters() {
        PortfolioParameters portfolioParameters = PortfolioParameters.builder()
            .monthlyRetirementIncome(PortfolioParameters.MonthlyRetirementIncome.builder()
                .socialSecurity(1000.00)
                .socialSecurityAdjustmentRate(INFLATION_PCT)
                .startSocialSecurity(LocalDate.of(2035, 1, 1))
                .build()
            )
            .build();

        final LocalDate distributionDate = LocalDate.of(2036, 1, 1);
        final Double monthlySocialSecurity = 1000.00;
        final long years = distributionDate.getYear() - LocalDate.now().getYear();
        final Double expected = INFLATED.apply(monthlySocialSecurity, INFLATION_PCT, years);

        final Double actual = SocialSecurity.getDistribution(portfolioParameters, distributionDate);

        assertEquals(expected, actual, 0.0001);

    }

     @Test
    void testOtherRetirementIncomeNotRetired() {
        final LocalDate distributionDate = LocalDate.of(2025, 1, 1);
        final Double otherIncome = 500.00;
        final Double incrementPct = 0.02;
        final Double actual = OTHER_RETIREMENT_INCOME.apply(otherIncome, incrementPct, distributionDate, RETIREMENT_DATE);
        assertEquals(0.0, actual, 0.0001);
    }

    @Test
    void testOtherRetirementIncomeRetiredPositive() {
        final LocalDate distributionDate = LocalDate.of(2036, 1, 1);
        final Double otherIncome = 500.00;
        final Double incrementPct = 0.02;
        final long years = distributionDate.getYear() - RETIREMENT_DATE.getYear();
        final Double expected = INFLATED.apply(otherIncome, incrementPct, years);
        final Double actual = OTHER_RETIREMENT_INCOME.apply(otherIncome, incrementPct, distributionDate, RETIREMENT_DATE);
        assertEquals(expected, actual, 0.0001);
    }

    @Test
    void testOtherRetirementIncomeRetiredZero() {
        final LocalDate distributionDate = LocalDate.of(2036, 1, 1);
        final Double otherIncome = 0.0;
        final Double incrementPct = 0.02;
        final Double actual = OTHER_RETIREMENT_INCOME.apply(otherIncome, incrementPct, distributionDate, RETIREMENT_DATE);
        assertEquals(0.0, actual, 0.0001);
    }

    @Test
    void testOtherRetirementIncomePortfolioParameters() {
        PortfolioParameters portfolioParameters = PortfolioParameters.builder()
            .monthlyRetirementIncome(PortfolioParameters.MonthlyRetirementIncome.builder()
                .otherMonthlyIncome(500.00)
                .otherMonthlyIncomeAdjustmentRate(0.02)
                .startOtherMonthlyIncome(RETIREMENT_DATE)
                .build())
            .build();

        final LocalDate distributionDate = LocalDate.of(2036, 1, 1);
        final Double otherIncome = 500.00;
        final Double incrementPct = 0.02;
        final long years = distributionDate.getYear() - RETIREMENT_DATE.getYear();
        final Double expected = INFLATED.apply(otherIncome, incrementPct, years);
        final Double actual = OtherRetirementIncome.getDistribution(portfolioParameters, distributionDate);
        assertEquals(expected, actual, 0.0001);
    }

    @Test
    void testOtherRetirementIncomeRetiredNoIncrement() {
        final LocalDate distributionDate = LocalDate.of(2036, 1, 1);
        final double otherIncome = 500.00;
        final Double incrementPct = 0.0;
        final long years = distributionDate.getYear() - RETIREMENT_DATE.getYear();
        final Double expected = INFLATED.apply(otherIncome, incrementPct, years);
        final Double actual = OTHER_RETIREMENT_INCOME.apply(otherIncome, incrementPct, distributionDate, RETIREMENT_DATE);
        assertEquals(expected, actual, 0.0001);
        assertEquals(otherIncome, actual, 0.0001);
    }

    @Test
    void testOtherRetirementIncomePortfolioParametersNoIncrement() {
        PortfolioParameters portfolioParameters = PortfolioParameters.builder()
            .monthlyRetirementIncome(PortfolioParameters.MonthlyRetirementIncome.builder()
                .otherMonthlyIncome(500.00)
                .otherMonthlyIncomeAdjustmentRate(0.0)
                .startOtherMonthlyIncome(RETIREMENT_DATE)
                .build())
            .build();

        final LocalDate distributionDate = LocalDate.of(2036, 1, 1);
        final double otherIncome = 500.00;
        final Double incrementPct = 0.0;
        final long years = distributionDate.getYear() - RETIREMENT_DATE.getYear();
        final Double expected = INFLATED.apply(otherIncome, incrementPct, years);
        final Double actual = OtherRetirementIncome.getDistribution(portfolioParameters, distributionDate);
        assertEquals(expected, actual, 0.0001);
        assertEquals(otherIncome, actual, 0.0001);
    }

}