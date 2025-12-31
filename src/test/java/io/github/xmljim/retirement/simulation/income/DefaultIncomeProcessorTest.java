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

    @Nested
    @DisplayName("Staggered Retirement")
    class StaggeredRetirement {

        @Test
        @DisplayName("should include salary for working spouse and SS for retired spouse")
        void shouldHandleStaggeredRetirement() {
            // Spouse 1: Still working
            PersonProfile workingSpouse = PersonProfile.builder()
                .name("Working Spouse")
                .dateOfBirth(LocalDate.of(1965, 1, 1))
                .retirementDate(LocalDate.of(2032, 1, 1))
                .lifeExpectancy(90)
                .build();

            WorkingIncome salary = WorkingIncome.builder()
                .annualSalary(100000)
                .startDate(LocalDate.of(2000, 1, 1))
                .endDate(LocalDate.of(2032, 1, 1))
                .build();

            // Spouse 2: Already retired and collecting SS
            PersonProfile retiredSpouse = PersonProfile.builder()
                .name("Retired Spouse")
                .dateOfBirth(LocalDate.of(1960, 1, 1))
                .retirementDate(LocalDate.of(2025, 1, 1))
                .lifeExpectancy(92)
                .build();

            SocialSecurityBenefit retiredSS = SocialSecurityBenefit.builder()
                .fraBenefit(2200)
                .birthYear(1960)
                .claimingAge(65, 0)
                .startDate(LocalDate.of(2025, 2, 1))
                .build();

            Pension retiredPension = Pension.builder()
                .name("Retired Spouse Pension")
                .monthlyBenefit(1200)
                .startDate(LocalDate.of(2025, 1, 1))
                .paymentForm(PensionPaymentForm.JOINT_100)
                .build();

            IncomeProfile workingProfile = IncomeProfile.builder()
                .person(workingSpouse)
                .workingIncome(salary)
                .build();

            IncomeProfile retiredProfile = IncomeProfile.builder()
                .person(retiredSpouse)
                .socialSecurity(retiredSS)
                .addPension(retiredPension)
                .build();

            // Simulate June 2028: working spouse still working, retired spouse collecting
            MonthlyIncome income = processor.process(
                List.of(workingProfile, retiredProfile),
                YearMonth.of(2028, 6),
                SimulationPhase.ACCUMULATION  // Household still in accumulation
            );

            // Should have salary from working spouse
            assertTrue(income.salaryIncome().compareTo(new BigDecimal("8000")) > 0,
                "Should include working spouse's salary");

            // Should have SS from retired spouse (early claiming at 65 reduces ~13%)
            // $2200 FRA * ~0.867 = ~$1907
            assertTrue(income.socialSecurityIncome().compareTo(new BigDecimal("1800")) > 0,
                "Should include retired spouse's SS");

            // Should have pension from retired spouse
            assertTrue(income.pensionIncome().compareTo(new BigDecimal("1100")) > 0,
                "Should include retired spouse's pension");
        }

        @Test
        @DisplayName("should include retirement income for both when both retired")
        void shouldHandleBothRetired() {
            // Both spouses retired with different income sources
            PersonProfile spouse1 = PersonProfile.builder()
                .name("Spouse 1")
                .dateOfBirth(LocalDate.of(1958, 3, 15))
                .retirementDate(LocalDate.of(2025, 4, 1))
                .lifeExpectancy(88)
                .build();

            PersonProfile spouse2 = PersonProfile.builder()
                .name("Spouse 2")
                .dateOfBirth(LocalDate.of(1960, 7, 20))
                .retirementDate(LocalDate.of(2027, 8, 1))
                .lifeExpectancy(90)
                .build();

            SocialSecurityBenefit ss1 = SocialSecurityBenefit.builder()
                .fraBenefit(2800)
                .birthYear(1958)
                .claimingAge(67, 0)
                .startDate(LocalDate.of(2025, 4, 1))
                .build();

            SocialSecurityBenefit ss2 = SocialSecurityBenefit.builder()
                .fraBenefit(2000)
                .birthYear(1960)
                .claimingAge(67, 0)
                .startDate(LocalDate.of(2027, 8, 1))
                .build();

            Pension pension1 = Pension.builder()
                .name("Spouse 1 Pension")
                .monthlyBenefit(1500)
                .startDate(LocalDate.of(2025, 4, 1))
                .paymentForm(PensionPaymentForm.JOINT_50)
                .build();

            IncomeProfile profile1 = IncomeProfile.builder()
                .person(spouse1)
                .socialSecurity(ss1)
                .addPension(pension1)
                .build();

            IncomeProfile profile2 = IncomeProfile.builder()
                .person(spouse2)
                .socialSecurity(ss2)
                .build();

            // Simulate 2030: both retired
            MonthlyIncome income = processor.process(
                List.of(profile1, profile2),
                YearMonth.of(2030, 6),
                SimulationPhase.DISTRIBUTION
            );

            // No salary
            assertEquals(BigDecimal.ZERO, income.salaryIncome());

            // Both SS benefits
            assertTrue(income.socialSecurityIncome().compareTo(new BigDecimal("4500")) > 0,
                "Should include both SS benefits");

            // Spouse 1's pension
            assertTrue(income.pensionIncome().compareTo(new BigDecimal("1400")) > 0,
                "Should include spouse 1's pension");
        }
    }

    @Nested
    @DisplayName("Negative Tests")
    class NegativeTests {

        @Test
        @DisplayName("should not include salary during DISTRIBUTION phase even if configured")
        void shouldNotIncludeSalaryDuringDistribution() {
            // Person has working income configured but we're in distribution phase
            WorkingIncome salary = WorkingIncome.builder()
                .annualSalary(120000)
                .startDate(LocalDate.of(2020, 1, 1))
                .endDate(LocalDate.of(2035, 1, 1))  // Would still be "active"
                .build();

            IncomeProfile profile = IncomeProfile.builder()
                .person(person)
                .workingIncome(salary)
                .build();

            MonthlyIncome income = processor.process(
                List.of(profile),
                YearMonth.of(2030, 6),
                SimulationPhase.DISTRIBUTION  // Distribution phase
            );

            // Salary should be zero even though WorkingIncome is "active"
            assertEquals(BigDecimal.ZERO, income.salaryIncome(),
                "Salary should not be included during DISTRIBUTION phase");
        }

        @Test
        @DisplayName("should not include salary during SURVIVOR phase even if configured")
        void shouldNotIncludeSalaryDuringSurvivor() {
            WorkingIncome salary = WorkingIncome.builder()
                .annualSalary(120000)
                .startDate(LocalDate.of(2020, 1, 1))
                .endDate(LocalDate.of(2035, 1, 1))
                .build();

            IncomeProfile profile = IncomeProfile.builder()
                .person(person)
                .workingIncome(salary)
                .build();

            MonthlyIncome income = processor.process(
                List.of(profile),
                YearMonth.of(2030, 6),
                SimulationPhase.SURVIVOR  // Survivor phase
            );

            assertEquals(BigDecimal.ZERO, income.salaryIncome(),
                "Salary should not be included during SURVIVOR phase");
        }

        @Test
        @DisplayName("should not include SS before start date even in distribution")
        void shouldNotIncludeSsBeforeStartDate() {
            SocialSecurityBenefit ssBenefit = SocialSecurityBenefit.builder()
                .fraBenefit(2500)
                .birthYear(1960)
                .claimingAge(70, 0)  // Delayed claiming at 70
                .startDate(LocalDate.of(2030, 7, 1))  // Starts July 2030
                .build();

            IncomeProfile profile = IncomeProfile.builder()
                .person(person)
                .socialSecurity(ssBenefit)
                .build();

            // Before SS start date
            MonthlyIncome income = processor.process(
                List.of(profile),
                YearMonth.of(2028, 6),
                SimulationPhase.DISTRIBUTION
            );

            assertEquals(BigDecimal.ZERO, income.socialSecurityIncome(),
                "SS should not be included before start date");
        }

        @Test
        @DisplayName("should not include pension before start date")
        void shouldNotIncludePensionBeforeStartDate() {
            Pension pension = Pension.builder()
                .name("Future Pension")
                .monthlyBenefit(2000)
                .startDate(LocalDate.of(2030, 1, 1))  // Starts 2030
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

            assertEquals(BigDecimal.ZERO, income.pensionIncome(),
                "Pension should not be included before start date");
        }
    }
}
