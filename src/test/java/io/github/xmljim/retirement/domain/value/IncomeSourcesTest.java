package io.github.xmljim.retirement.domain.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.AnnuityType;
import io.github.xmljim.retirement.domain.enums.OtherIncomeType;
import io.github.xmljim.retirement.domain.enums.PensionPaymentForm;

@DisplayName("IncomeSources Tests")
class IncomeSourcesTest {

    @Nested
    @DisplayName("Empty Sources Tests")
    class EmptySourcesTests {
        @Test
        @DisplayName("Empty sources should have no income")
        void emptyHasNoIncome() {
            IncomeSources sources = IncomeSources.empty();
            assertFalse(sources.hasIncomeSources());
            assertTrue(sources.getWorkingIncome().isEmpty());
            assertTrue(sources.getSocialSecurityBenefit().isEmpty());
            assertTrue(sources.getPensions().isEmpty());
            assertTrue(sources.getAnnuities().isEmpty());
            assertTrue(sources.getOtherIncomes().isEmpty());
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {
        @Test
        @DisplayName("Should build with working income")
        void withWorkingIncome() {
            WorkingIncome income = WorkingIncome.builder()
                .annualSalary(100000.00)
                .startDate(LocalDate.of(2020, 1, 1))
                .build();

            IncomeSources sources = IncomeSources.builder()
                .workingIncome(income)
                .build();

            assertTrue(sources.hasIncomeSources());
            assertTrue(sources.getWorkingIncome().isPresent());
        }

        @Test
        @DisplayName("Should build with Social Security")
        void withSocialSecurity() {
            SocialSecurityBenefit ss = SocialSecurityBenefit.builder()
                .fraBenefit(2500.00)
                .birthYear(1958)
                .claimingAgeMonths(792)
                .startDate(LocalDate.of(2024, 1, 1))
                .build();

            IncomeSources sources = IncomeSources.builder()
                .socialSecurityBenefit(ss)
                .build();

            assertTrue(sources.hasIncomeSources());
            assertTrue(sources.getSocialSecurityBenefit().isPresent());
        }

        @Test
        @DisplayName("Should build with pensions list")
        void withPensionsList() {
            Pension p1 = createPension("P1", 1000.00);
            Pension p2 = createPension("P2", 500.00);

            IncomeSources sources = IncomeSources.builder()
                .pensions(List.of(p1, p2))
                .build();

            assertTrue(sources.hasIncomeSources());
            assertEquals(2, sources.getPensions().size());
        }

        @Test
        @DisplayName("Should build with annuities list")
        void withAnnuitiesList() {
            Annuity a1 = createAnnuity("A1", 500.00);
            Annuity a2 = createAnnuity("A2", 300.00);

            IncomeSources sources = IncomeSources.builder()
                .annuities(List.of(a1, a2))
                .build();

            assertTrue(sources.hasIncomeSources());
            assertEquals(2, sources.getAnnuities().size());
        }

        @Test
        @DisplayName("Should build with other incomes list")
        void withOtherIncomesList() {
            OtherIncome o1 = createOtherIncome("O1", 1000.00);
            OtherIncome o2 = createOtherIncome("O2", 500.00);

            IncomeSources sources = IncomeSources.builder()
                .otherIncomes(List.of(o1, o2))
                .build();

            assertTrue(sources.hasIncomeSources());
            assertEquals(2, sources.getOtherIncomes().size());
        }

        @Test
        @DisplayName("Should handle null in add methods")
        void handleNullInAddMethods() {
            IncomeSources sources = IncomeSources.builder()
                .addPension(null)
                .addAnnuity(null)
                .addOtherIncome(null)
                .pensions(null)
                .annuities(null)
                .otherIncomes(null)
                .build();

            assertFalse(sources.hasIncomeSources());
        }
    }

    private Pension createPension(String name, double amount) {
        return Pension.builder()
            .name(name)
            .monthlyBenefit(amount)
            .paymentForm(PensionPaymentForm.SINGLE_LIFE)
            .startDate(LocalDate.of(2024, 1, 1))
            .build();
    }

    private Annuity createAnnuity(String name, double amount) {
        return Annuity.builder()
            .name(name)
            .annuityType(AnnuityType.FIXED_IMMEDIATE)
            .monthlyBenefit(amount)
            .purchaseDate(LocalDate.of(2024, 1, 1))
            .paymentStartDate(LocalDate.of(2024, 1, 1))
            .build();
    }

    private OtherIncome createOtherIncome(String name, double amount) {
        return OtherIncome.builder()
            .name(name)
            .incomeType(OtherIncomeType.RENTAL)
            .monthlyAmount(amount)
            .startDate(LocalDate.of(2024, 1, 1))
            .build();
    }
}
