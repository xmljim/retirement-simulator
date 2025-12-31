package io.github.xmljim.retirement.simulation.income;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.AnnuityType;
import io.github.xmljim.retirement.domain.enums.OtherIncomeType;
import io.github.xmljim.retirement.domain.enums.PensionPaymentForm;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.model.PersonProfile;
import io.github.xmljim.retirement.domain.value.Annuity;
import io.github.xmljim.retirement.domain.value.OtherIncome;
import io.github.xmljim.retirement.domain.value.Pension;
import io.github.xmljim.retirement.domain.value.SocialSecurityBenefit;
import io.github.xmljim.retirement.domain.value.WorkingIncome;

@DisplayName("IncomeProfile")
class IncomeProfileTest {

    private PersonProfile person;
    private WorkingIncome workingIncome;
    private SocialSecurityBenefit socialSecurity;
    private Pension pension;
    private Annuity annuity;
    private OtherIncome otherIncome;

    @BeforeEach
    void setUp() {
        person = PersonProfile.builder()
            .name("Test Person")
            .dateOfBirth(LocalDate.of(1960, 6, 15))
            .retirementDate(LocalDate.of(2027, 1, 1))
            .lifeExpectancy(90)
            .build();

        workingIncome = WorkingIncome.builder()
            .annualSalary(120000)
            .startDate(LocalDate.of(2020, 1, 1))
            .endDate(LocalDate.of(2027, 1, 1))
            .build();

        socialSecurity = SocialSecurityBenefit.builder()
            .fraBenefit(2500)
            .birthYear(1960)
            .claimingAge(67, 0)
            .startDate(LocalDate.of(2027, 7, 1))
            .build();

        pension = Pension.builder()
            .name("Company Pension")
            .monthlyBenefit(1500)
            .startDate(LocalDate.of(2027, 1, 1))
            .paymentForm(PensionPaymentForm.SINGLE_LIFE)
            .build();

        annuity = Annuity.builder()
            .name("Fixed Annuity")
            .annuityType(AnnuityType.FIXED_IMMEDIATE)
            .purchaseAmount(100000)
            .monthlyBenefit(500)
            .purchaseDate(LocalDate.of(2025, 1, 1))
            .paymentStartDate(LocalDate.of(2027, 1, 1))
            .build();

        otherIncome = OtherIncome.builder()
            .name("Rental Income")
            .incomeType(OtherIncomeType.RENTAL)
            .monthlyAmount(800)
            .startDate(LocalDate.of(2020, 1, 1))
            .build();
    }

    @Nested
    @DisplayName("Record Construction")
    class RecordConstruction {

        @Test
        @DisplayName("should require person")
        void shouldRequirePerson() {
            assertThrows(MissingRequiredFieldException.class, () ->
                new IncomeProfile(null, workingIncome, socialSecurity, List.of(), List.of(), List.of()));
        }

        @Test
        @DisplayName("should default null lists to empty")
        void shouldDefaultNullListsToEmpty() {
            IncomeProfile profile = new IncomeProfile(person, null, null, null, null, null);

            assertTrue(profile.pensions().isEmpty());
            assertTrue(profile.annuities().isEmpty());
            assertTrue(profile.otherIncomes().isEmpty());
        }

        @Test
        @DisplayName("should make lists unmodifiable")
        void shouldMakeListsUnmodifiable() {
            IncomeProfile profile = IncomeProfile.builder()
                .person(person)
                .addPension(pension)
                .build();

            assertThrows(UnsupportedOperationException.class, () ->
                profile.pensions().add(pension));
        }
    }

    @Nested
    @DisplayName("Optional Accessors")
    class OptionalAccessors {

        @Test
        @DisplayName("should return empty Optional when working income is null")
        void shouldReturnEmptyOptionalWhenWorkingIncomeNull() {
            IncomeProfile profile = IncomeProfile.builder()
                .person(person)
                .build();

            assertTrue(profile.getWorkingIncome().isEmpty());
        }

        @Test
        @DisplayName("should return present Optional when working income exists")
        void shouldReturnPresentOptionalWhenWorkingIncomeExists() {
            IncomeProfile profile = IncomeProfile.builder()
                .person(person)
                .workingIncome(workingIncome)
                .build();

            assertTrue(profile.getWorkingIncome().isPresent());
            assertEquals(workingIncome, profile.getWorkingIncome().orElseThrow());
        }

        @Test
        @DisplayName("should return empty Optional when social security is null")
        void shouldReturnEmptyOptionalWhenSocialSecurityNull() {
            IncomeProfile profile = IncomeProfile.builder()
                .person(person)
                .build();

            assertTrue(profile.getSocialSecurity().isEmpty());
        }

        @Test
        @DisplayName("should return present Optional when social security exists")
        void shouldReturnPresentOptionalWhenSocialSecurityExists() {
            IncomeProfile profile = IncomeProfile.builder()
                .person(person)
                .socialSecurity(socialSecurity)
                .build();

            assertTrue(profile.getSocialSecurity().isPresent());
            assertEquals(socialSecurity, profile.getSocialSecurity().orElseThrow());
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should build with all income sources")
        void shouldBuildWithAllIncomeSources() {
            IncomeProfile profile = IncomeProfile.builder()
                .person(person)
                .workingIncome(workingIncome)
                .socialSecurity(socialSecurity)
                .addPension(pension)
                .addAnnuity(annuity)
                .addOtherIncome(otherIncome)
                .build();

            assertEquals(person, profile.person());
            assertTrue(profile.getWorkingIncome().isPresent());
            assertTrue(profile.getSocialSecurity().isPresent());
            assertEquals(1, profile.pensions().size());
            assertEquals(1, profile.annuities().size());
            assertEquals(1, profile.otherIncomes().size());
        }

        @Test
        @DisplayName("should add multiple pensions")
        void shouldAddMultiplePensions() {
            Pension pension2 = Pension.builder()
                .name("Second Pension")
                .monthlyBenefit(800)
                .startDate(LocalDate.of(2027, 1, 1))
                .paymentForm(PensionPaymentForm.JOINT_50)
                .build();

            IncomeProfile profile = IncomeProfile.builder()
                .person(person)
                .addPension(pension)
                .addPension(pension2)
                .build();

            assertEquals(2, profile.pensions().size());
        }

        @Test
        @DisplayName("should add pensions as list")
        void shouldAddPensionsAsList() {
            Pension pension2 = Pension.builder()
                .name("Second Pension")
                .monthlyBenefit(800)
                .startDate(LocalDate.of(2027, 1, 1))
                .paymentForm(PensionPaymentForm.JOINT_50)
                .build();

            IncomeProfile profile = IncomeProfile.builder()
                .person(person)
                .pensions(List.of(pension, pension2))
                .build();

            assertEquals(2, profile.pensions().size());
        }

        @Test
        @DisplayName("should add multiple annuities")
        void shouldAddMultipleAnnuities() {
            Annuity annuity2 = Annuity.builder()
                .name("Second Annuity")
                .annuityType(AnnuityType.FIXED_DEFERRED)
                .purchaseAmount(50000)
                .monthlyBenefit(300)
                .purchaseDate(LocalDate.of(2020, 1, 1))
                .paymentStartDate(LocalDate.of(2030, 1, 1))
                .build();

            IncomeProfile profile = IncomeProfile.builder()
                .person(person)
                .addAnnuity(annuity)
                .addAnnuity(annuity2)
                .build();

            assertEquals(2, profile.annuities().size());
        }

        @Test
        @DisplayName("should add annuities as list")
        void shouldAddAnnuitiesAsList() {
            IncomeProfile profile = IncomeProfile.builder()
                .person(person)
                .annuities(List.of(annuity))
                .build();

            assertEquals(1, profile.annuities().size());
        }

        @Test
        @DisplayName("should add multiple other incomes")
        void shouldAddMultipleOtherIncomes() {
            OtherIncome otherIncome2 = OtherIncome.builder()
                .name("Part-time Work")
                .incomeType(OtherIncomeType.PART_TIME_WORK)
                .monthlyAmount(1000)
                .startDate(LocalDate.of(2027, 1, 1))
                .endDate(LocalDate.of(2030, 1, 1))
                .build();

            IncomeProfile profile = IncomeProfile.builder()
                .person(person)
                .addOtherIncome(otherIncome)
                .addOtherIncome(otherIncome2)
                .build();

            assertEquals(2, profile.otherIncomes().size());
        }

        @Test
        @DisplayName("should add other incomes as list")
        void shouldAddOtherIncomesAsList() {
            IncomeProfile profile = IncomeProfile.builder()
                .person(person)
                .otherIncomes(List.of(otherIncome))
                .build();

            assertEquals(1, profile.otherIncomes().size());
        }

        @Test
        @DisplayName("should ignore null items in add methods")
        void shouldIgnoreNullItemsInAddMethods() {
            IncomeProfile profile = IncomeProfile.builder()
                .person(person)
                .addPension(null)
                .addAnnuity(null)
                .addOtherIncome(null)
                .build();

            assertTrue(profile.pensions().isEmpty());
            assertTrue(profile.annuities().isEmpty());
            assertTrue(profile.otherIncomes().isEmpty());
        }

        @Test
        @DisplayName("should ignore null lists in list methods")
        void shouldIgnoreNullListsInListMethods() {
            IncomeProfile profile = IncomeProfile.builder()
                .person(person)
                .pensions(null)
                .annuities(null)
                .otherIncomes(null)
                .build();

            assertTrue(profile.pensions().isEmpty());
            assertTrue(profile.annuities().isEmpty());
            assertTrue(profile.otherIncomes().isEmpty());
        }
    }
}
