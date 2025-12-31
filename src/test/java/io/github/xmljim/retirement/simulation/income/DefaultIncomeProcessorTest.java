package io.github.xmljim.retirement.simulation.income;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.PensionPaymentForm;
import io.github.xmljim.retirement.domain.enums.SimulationPhase;
import io.github.xmljim.retirement.domain.model.PersonProfile;
import io.github.xmljim.retirement.domain.value.Pension;
import io.github.xmljim.retirement.domain.value.SocialSecurityBenefit;
import io.github.xmljim.retirement.domain.value.WorkingIncome;
import io.github.xmljim.retirement.simulation.income.impl.DefaultIncomeProcessor;

@DisplayName("DefaultIncomeProcessor")
class DefaultIncomeProcessorTest {

    private DefaultIncomeProcessor processor;
    private PersonProfile person;

    @BeforeEach
    void setUp() {
        processor = new DefaultIncomeProcessor();

        person = PersonProfile.builder()
            .name("Test Person")
            .dateOfBirth(LocalDate.of(1960, 6, 15))
            .retirementDate(LocalDate.of(2027, 1, 1))
            .lifeExpectancy(90)
            .build();
    }

    @Nested
    @DisplayName("Empty Profiles")
    class EmptyProfiles {

        @Test
        @DisplayName("should return zero for null profiles")
        void shouldReturnZeroForNullProfiles() {
            MonthlyIncome income = processor.process(null, YearMonth.of(2025, 6), SimulationPhase.ACCUMULATION);

            assertEquals(BigDecimal.ZERO, income.total());
        }

        @Test
        @DisplayName("should return zero for empty profiles list")
        void shouldReturnZeroForEmptyProfilesList() {
            MonthlyIncome income = processor.process(List.of(), YearMonth.of(2025, 6), SimulationPhase.ACCUMULATION);

            assertEquals(BigDecimal.ZERO, income.total());
        }
    }

    @Nested
    @DisplayName("Salary Income")
    class SalaryIncome {

        @Test
        @DisplayName("should include salary during accumulation")
        void shouldIncludeSalaryDuringAccumulation() {
            WorkingIncome salary = WorkingIncome.builder()
                .annualSalary(120000)
                .startDate(LocalDate.of(2020, 1, 1))
                .endDate(LocalDate.of(2027, 1, 1))
                .build();

            IncomeProfile profile = IncomeProfile.builder()
                .person(person)
                .workingIncome(salary)
                .build();

            MonthlyIncome income = processor.process(
                List.of(profile),
                YearMonth.of(2025, 6),
                SimulationPhase.ACCUMULATION
            );

            // $120,000 / 12 = $10,000 per month
            assertTrue(income.salaryIncome().compareTo(new BigDecimal("9000")) > 0);
            assertTrue(income.hasSalaryIncome());
        }

        @Test
        @DisplayName("should not include salary during distribution")
        void shouldNotIncludeSalaryDuringDistribution() {
            WorkingIncome salary = WorkingIncome.builder()
                .annualSalary(120000)
                .startDate(LocalDate.of(2020, 1, 1))
                .endDate(LocalDate.of(2027, 1, 1))
                .build();

            IncomeProfile profile = IncomeProfile.builder()
                .person(person)
                .workingIncome(salary)
                .build();

            // Even though salary is active, DISTRIBUTION phase doesn't include salary
            MonthlyIncome income = processor.process(
                List.of(profile),
                YearMonth.of(2025, 6),
                SimulationPhase.DISTRIBUTION
            );

            assertEquals(BigDecimal.ZERO, income.salaryIncome());
        }

        @Test
        @DisplayName("should return zero salary after end date")
        void shouldReturnZeroSalaryAfterEndDate() {
            WorkingIncome salary = WorkingIncome.builder()
                .annualSalary(120000)
                .startDate(LocalDate.of(2020, 1, 1))
                .endDate(LocalDate.of(2025, 1, 1))  // Ends Jan 2025
                .build();

            IncomeProfile profile = IncomeProfile.builder()
                .person(person)
                .workingIncome(salary)
                .build();

            MonthlyIncome income = processor.process(
                List.of(profile),
                YearMonth.of(2025, 6),  // After end date
                SimulationPhase.ACCUMULATION
            );

            assertEquals(BigDecimal.ZERO, income.salaryIncome());
        }
    }

    @Nested
    @DisplayName("Social Security Income")
    class SocialSecurityIncome {

        @Test
        @DisplayName("should include SS after start date")
        void shouldIncludeSsAfterStartDate() {
            SocialSecurityBenefit ssBenefit = SocialSecurityBenefit.builder()
                .fraBenefit(2500)
                .birthYear(1960)
                .claimingAge(67, 0)  // FRA for 1960 birth year
                .startDate(LocalDate.of(2027, 7, 1))  // Starts July 2027
                .build();

            IncomeProfile profile = IncomeProfile.builder()
                .person(person)
                .socialSecurity(ssBenefit)
                .build();

            MonthlyIncome income = processor.process(
                List.of(profile),
                YearMonth.of(2028, 1),  // After SS start
                SimulationPhase.DISTRIBUTION
            );

            assertTrue(income.socialSecurityIncome().compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @DisplayName("should not include SS before start date")
        void shouldNotIncludeSsBeforeStartDate() {
            SocialSecurityBenefit ssBenefit = SocialSecurityBenefit.builder()
                .fraBenefit(2500)
                .birthYear(1960)
                .claimingAge(67, 0)
                .startDate(LocalDate.of(2027, 7, 1))
                .build();

            IncomeProfile profile = IncomeProfile.builder()
                .person(person)
                .socialSecurity(ssBenefit)
                .build();

            MonthlyIncome income = processor.process(
                List.of(profile),
                YearMonth.of(2025, 1),  // Before SS start
                SimulationPhase.DISTRIBUTION
            );

            assertEquals(BigDecimal.ZERO, income.socialSecurityIncome());
        }
    }

    @Nested
    @DisplayName("Pension Income")
    class PensionIncome {

        @Test
        @DisplayName("should include pension after start date")
        void shouldIncludePensionAfterStartDate() {
            Pension pension = Pension.builder()
                .name("Company Pension")
                .monthlyBenefit(1500)
                .startDate(LocalDate.of(2027, 1, 1))
                .paymentForm(PensionPaymentForm.SINGLE_LIFE)
                .build();

            IncomeProfile profile = IncomeProfile.builder()
                .person(person)
                .addPension(pension)
                .build();

            MonthlyIncome income = processor.process(
                List.of(profile),
                YearMonth.of(2028, 6),
                SimulationPhase.DISTRIBUTION
            );

            assertTrue(income.pensionIncome().compareTo(new BigDecimal("1400")) > 0);
        }

        @Test
        @DisplayName("should aggregate multiple pensions")
        void shouldAggregateMultiplePensions() {
            Pension pension1 = Pension.builder()
                .name("Pension 1")
                .monthlyBenefit(1000)
                .startDate(LocalDate.of(2027, 1, 1))
                .paymentForm(PensionPaymentForm.SINGLE_LIFE)
                .build();

            Pension pension2 = Pension.builder()
                .name("Pension 2")
                .monthlyBenefit(800)
                .startDate(LocalDate.of(2027, 1, 1))
                .paymentForm(PensionPaymentForm.SINGLE_LIFE)
                .build();

            IncomeProfile profile = IncomeProfile.builder()
                .person(person)
                .addPension(pension1)
                .addPension(pension2)
                .build();

            MonthlyIncome income = processor.process(
                List.of(profile),
                YearMonth.of(2028, 1),
                SimulationPhase.DISTRIBUTION
            );

            // 1000 + 800 = 1800
            assertTrue(income.pensionIncome().compareTo(new BigDecimal("1700")) > 0);
        }
    }

    @Nested
    @DisplayName("Multiple Profiles")
    class MultipleProfiles {

        @Test
        @DisplayName("should aggregate income from multiple profiles")
        void shouldAggregateIncomeFromMultipleProfiles() {
            PersonProfile spouse = PersonProfile.builder()
                .name("Spouse")
                .dateOfBirth(LocalDate.of(1962, 3, 20))
                .retirementDate(LocalDate.of(2029, 1, 1))
                .lifeExpectancy(92)
                .build();

            WorkingIncome salary1 = WorkingIncome.builder()
                .annualSalary(120000)
                .startDate(LocalDate.of(2020, 1, 1))
                .endDate(LocalDate.of(2027, 1, 1))
                .build();

            WorkingIncome salary2 = WorkingIncome.builder()
                .annualSalary(80000)
                .startDate(LocalDate.of(2020, 1, 1))
                .endDate(LocalDate.of(2029, 1, 1))
                .build();

            IncomeProfile profile1 = IncomeProfile.builder()
                .person(person)
                .workingIncome(salary1)
                .build();

            IncomeProfile profile2 = IncomeProfile.builder()
                .person(spouse)
                .workingIncome(salary2)
                .build();

            MonthlyIncome income = processor.process(
                List.of(profile1, profile2),
                YearMonth.of(2025, 6),
                SimulationPhase.ACCUMULATION
            );

            // Both salaries should be included
            // $120,000/12 + $80,000/12 ≈ $10,000 + $6,667 ≈ $16,667
            assertTrue(income.salaryIncome().compareTo(new BigDecimal("15000")) > 0);
        }
    }
}
