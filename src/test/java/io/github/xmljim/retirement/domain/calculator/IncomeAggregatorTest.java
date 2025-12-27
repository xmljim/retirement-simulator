package io.github.xmljim.retirement.domain.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.calculator.impl.DefaultIncomeAggregator;
import io.github.xmljim.retirement.domain.enums.AnnuityType;
import io.github.xmljim.retirement.domain.enums.OtherIncomeType;
import io.github.xmljim.retirement.domain.enums.PensionPaymentForm;
import io.github.xmljim.retirement.domain.value.Annuity;
import io.github.xmljim.retirement.domain.value.IncomeBreakdown;
import io.github.xmljim.retirement.domain.value.IncomeSources;
import io.github.xmljim.retirement.domain.value.OtherIncome;
import io.github.xmljim.retirement.domain.value.Pension;
import io.github.xmljim.retirement.domain.value.SocialSecurityBenefit;
import io.github.xmljim.retirement.domain.value.WorkingIncome;

@DisplayName("IncomeAggregator Tests")
class IncomeAggregatorTest {

    private IncomeAggregator aggregator;
    private static final LocalDate TEST_DATE = LocalDate.of(2025, 6, 15);

    @BeforeEach
    void setUp() {
        aggregator = new DefaultIncomeAggregator();
    }

    @Nested
    @DisplayName("Empty Sources Tests")
    class EmptySourcesTests {
        @Test
        @DisplayName("Should return empty breakdown for empty sources")
        void emptySourcesReturnsZero() {
            IncomeBreakdown result = aggregator.getMonthlyIncome(IncomeSources.empty(), TEST_DATE);
            assertFalse(result.hasIncome());
            assertEquals(0, BigDecimal.ZERO.compareTo(result.total()));
        }
    }

    @Nested
    @DisplayName("Working Income Tests")
    class WorkingIncomeTests {
        @Test
        @DisplayName("Should aggregate working income")
        void aggregatesWorkingIncome() {
            WorkingIncome salary = WorkingIncome.builder()
                .annualSalary(120000.00)
                .startDate(LocalDate.of(2020, 1, 1))
                .build();

            IncomeSources sources = IncomeSources.builder()
                .workingIncome(salary)
                .build();

            IncomeBreakdown result = aggregator.getMonthlyIncome(sources, TEST_DATE);
            assertEquals(0, new BigDecimal("10000").compareTo(result.salary()));
            assertEquals(0, result.salary().compareTo(result.earnedIncome()));
        }
    }

    @Nested
    @DisplayName("Social Security Tests")
    class SocialSecurityTests {
        @Test
        @DisplayName("Should aggregate Social Security")
        void aggregatesSocialSecurity() {
            SocialSecurityBenefit ss = SocialSecurityBenefit.builder()
                .fraBenefit(2500.00)
                .birthYear(1958)
                .claimingAgeMonths(792)
                .startDate(LocalDate.of(2024, 1, 1))
                .build();

            IncomeSources sources = IncomeSources.builder()
                .socialSecurityBenefit(ss)
                .build();

            IncomeBreakdown result = aggregator.getMonthlyIncome(sources, TEST_DATE);
            assertTrue(result.socialSecurity().compareTo(BigDecimal.ZERO) > 0);
            assertEquals(0, result.socialSecurity().compareTo(result.passiveIncome()));
        }
    }

    @Nested
    @DisplayName("Pension Tests")
    class PensionTests {
        @Test
        @DisplayName("Should aggregate pension income")
        void aggregatesPension() {
            Pension pension = Pension.builder()
                .name("Company Pension")
                .monthlyBenefit(1500.00)
                .paymentForm(PensionPaymentForm.SINGLE_LIFE)
                .startDate(LocalDate.of(2024, 1, 1))
                .build();

            IncomeSources sources = IncomeSources.builder()
                .addPension(pension)
                .build();

            IncomeBreakdown result = aggregator.getMonthlyIncome(sources, TEST_DATE);
            assertEquals(0, new BigDecimal("1500").compareTo(result.pension()));
        }

        @Test
        @DisplayName("Should aggregate multiple pensions")
        void aggregatesMultiplePensions() {
            Pension p1 = Pension.builder()
                .name("Pension 1").monthlyBenefit(1000.00)
                .paymentForm(PensionPaymentForm.SINGLE_LIFE)
                .startDate(LocalDate.of(2024, 1, 1)).build();
            Pension p2 = Pension.builder()
                .name("Pension 2").monthlyBenefit(500.00)
                .paymentForm(PensionPaymentForm.SINGLE_LIFE)
                .startDate(LocalDate.of(2024, 1, 1)).build();

            IncomeSources sources = IncomeSources.builder()
                .addPension(p1).addPension(p2).build();

            IncomeBreakdown result = aggregator.getMonthlyIncome(sources, TEST_DATE);
            assertEquals(0, new BigDecimal("1500").compareTo(result.pension()));
        }
    }

    @Nested
    @DisplayName("Annuity Tests")
    class AnnuityTests {
        @Test
        @DisplayName("Should aggregate annuity income")
        void aggregatesAnnuity() {
            Annuity annuity = Annuity.builder()
                .name("Fixed Annuity")
                .annuityType(AnnuityType.FIXED_IMMEDIATE)
                .monthlyBenefit(500.00)
                .purchaseDate(LocalDate.of(2024, 1, 1))
                .paymentStartDate(LocalDate.of(2024, 1, 1))
                .build();

            IncomeSources sources = IncomeSources.builder()
                .addAnnuity(annuity).build();

            IncomeBreakdown result = aggregator.getMonthlyIncome(sources, TEST_DATE);
            assertEquals(0, new BigDecimal("500").compareTo(result.annuity()));
        }
    }

    @Nested
    @DisplayName("Other Income Tests")
    class OtherIncomeTests {
        @Test
        @DisplayName("Should aggregate other income and classify correctly")
        void aggregatesOtherIncome() {
            OtherIncome rental = OtherIncome.builder()
                .name("Rental").incomeType(OtherIncomeType.RENTAL)
                .monthlyAmount(1000.00).startDate(LocalDate.of(2024, 1, 1)).build();
            OtherIncome partTime = OtherIncome.builder()
                .name("Consulting").incomeType(OtherIncomeType.PART_TIME_WORK)
                .monthlyAmount(2000.00).startDate(LocalDate.of(2024, 1, 1)).build();

            IncomeSources sources = IncomeSources.builder()
                .addOtherIncome(rental).addOtherIncome(partTime).build();

            IncomeBreakdown result = aggregator.getMonthlyIncome(sources, TEST_DATE);
            assertEquals(0, new BigDecimal("3000").compareTo(result.other()));
            assertEquals(0, new BigDecimal("2000").compareTo(result.earnedIncome()));
            assertEquals(0, new BigDecimal("1000").compareTo(result.passiveIncome()));
        }
    }

    @Nested
    @DisplayName("Combined Income Tests")
    class CombinedIncomeTests {
        @Test
        @DisplayName("Should calculate total from all sources")
        void calculatesTotal() {
            WorkingIncome salary = WorkingIncome.builder()
                .annualSalary(60000.00).startDate(LocalDate.of(2020, 1, 1)).build();
            Pension pension = Pension.builder()
                .name("Pension").monthlyBenefit(1000.00)
                .paymentForm(PensionPaymentForm.SINGLE_LIFE)
                .startDate(LocalDate.of(2024, 1, 1)).build();
            OtherIncome rental = OtherIncome.builder()
                .name("Rental").incomeType(OtherIncomeType.RENTAL)
                .monthlyAmount(500.00).startDate(LocalDate.of(2024, 1, 1)).build();

            IncomeSources sources = IncomeSources.builder()
                .workingIncome(salary).addPension(pension).addOtherIncome(rental).build();

            IncomeBreakdown result = aggregator.getMonthlyIncome(sources, TEST_DATE);
            // 5000 salary + 1000 pension + 500 rental = 6500
            assertEquals(0, new BigDecimal("6500").compareTo(result.total()));
        }
    }

    @Nested
    @DisplayName("Couple Aggregation Tests")
    class CoupleAggregationTests {
        @Test
        @DisplayName("Should combine couple income")
        void combinesCoupleIncome() {
            IncomeBreakdown p1 = IncomeBreakdown.builder()
                .asOfDate(TEST_DATE).salary(new BigDecimal("5000"))
                .pension(new BigDecimal("1000")).earnedIncome(new BigDecimal("5000"))
                .passiveIncome(new BigDecimal("1000")).build();
            IncomeBreakdown p2 = IncomeBreakdown.builder()
                .asOfDate(TEST_DATE).salary(new BigDecimal("4000"))
                .socialSecurity(new BigDecimal("2000")).earnedIncome(new BigDecimal("4000"))
                .passiveIncome(new BigDecimal("2000")).build();

            IncomeBreakdown combined = aggregator.combineForCouple(p1, p2);
            assertEquals(0, new BigDecimal("9000").compareTo(combined.salary()));
            assertEquals(0, new BigDecimal("2000").compareTo(combined.socialSecurity()));
            assertEquals(0, new BigDecimal("1000").compareTo(combined.pension()));
            assertEquals(0, new BigDecimal("12000").compareTo(combined.total()));
        }
    }

    @Nested
    @DisplayName("Annual Income Tests")
    class AnnualIncomeTests {
        @Test
        @DisplayName("Should convert to annual")
        void convertsToAnnual() {
            Pension pension = Pension.builder()
                .name("Pension").monthlyBenefit(1000.00)
                .paymentForm(PensionPaymentForm.SINGLE_LIFE)
                .startDate(LocalDate.of(2024, 1, 1)).build();

            IncomeSources sources = IncomeSources.builder().addPension(pension).build();
            IncomeBreakdown annual = aggregator.getAnnualIncome(sources, TEST_DATE);
            assertEquals(0, new BigDecimal("12000").compareTo(annual.pension()));
        }
    }
}
