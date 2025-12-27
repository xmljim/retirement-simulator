package io.github.xmljim.retirement.domain.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.OtherIncomeType;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;

@DisplayName("OtherIncome Tests")
class OtherIncomeTest {

    private static final LocalDate START_DATE = LocalDate.of(2024, 1, 1);
    private static final LocalDate END_DATE = LocalDate.of(2029, 12, 31);

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should build with required fields")
        void buildWithRequiredFields() {
            OtherIncome income = OtherIncome.builder()
                .name("Rental Property")
                .incomeType(OtherIncomeType.RENTAL)
                .monthlyAmount(2000.00)
                .startDate(START_DATE)
                .build();

            assertEquals("Rental Property", income.getName());
            assertEquals(OtherIncomeType.RENTAL, income.getIncomeType());
            assertEquals(0, new BigDecimal("2000").compareTo(income.getMonthlyAmount()));
            assertEquals(START_DATE, income.getStartDate());
            assertTrue(income.getEndDate().isEmpty());
            assertTrue(income.getInflationRate().isEmpty());
        }

        @Test
        @DisplayName("Should build with all fields")
        void buildWithAllFields() {
            OtherIncome income = OtherIncome.builder()
                .name("Part-time Consulting")
                .incomeType(OtherIncomeType.PART_TIME_WORK)
                .monthlyAmount(3000.00)
                .startDate(START_DATE)
                .endDate(END_DATE)
                .inflationRate(0.03)
                .build();

            assertEquals("Part-time Consulting", income.getName());
            assertTrue(income.getEndDate().isPresent());
            assertEquals(END_DATE, income.getEndDate().get());
            assertTrue(income.getInflationRate().isPresent());
            assertEquals(0, new BigDecimal("0.03").compareTo(income.getInflationRate().get()));
        }

        @Test
        @DisplayName("Should build with annual amount")
        void buildWithAnnualAmount() {
            OtherIncome income = OtherIncome.builder()
                .name("Royalties")
                .incomeType(OtherIncomeType.ROYALTIES)
                .annualAmount(12000.00)
                .startDate(START_DATE)
                .build();

            assertEquals(0, new BigDecimal("1000").compareTo(income.getMonthlyAmount()));
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw for null name")
        void nullName() {
            assertThrows(MissingRequiredFieldException.class, () ->
                OtherIncome.builder()
                    .incomeType(OtherIncomeType.RENTAL)
                    .monthlyAmount(1000.00)
                    .startDate(START_DATE)
                    .build());
        }

        @Test
        @DisplayName("Should throw for blank name")
        void blankName() {
            assertThrows(ValidationException.class, () ->
                OtherIncome.builder()
                    .name("   ")
                    .incomeType(OtherIncomeType.RENTAL)
                    .monthlyAmount(1000.00)
                    .startDate(START_DATE)
                    .build());
        }

        @Test
        @DisplayName("Should throw for null income type")
        void nullIncomeType() {
            assertThrows(MissingRequiredFieldException.class, () ->
                OtherIncome.builder()
                    .name("Test")
                    .monthlyAmount(1000.00)
                    .startDate(START_DATE)
                    .build());
        }

        @Test
        @DisplayName("Should throw for negative monthly amount")
        void negativeAmount() {
            assertThrows(ValidationException.class, () ->
                OtherIncome.builder()
                    .name("Test")
                    .incomeType(OtherIncomeType.RENTAL)
                    .monthlyAmount(-1000.00)
                    .startDate(START_DATE)
                    .build());
        }

        @Test
        @DisplayName("Should throw for end date before start date")
        void endBeforeStart() {
            assertThrows(ValidationException.class, () ->
                OtherIncome.builder()
                    .name("Test")
                    .incomeType(OtherIncomeType.RENTAL)
                    .monthlyAmount(1000.00)
                    .startDate(LocalDate.of(2025, 1, 1))
                    .endDate(LocalDate.of(2024, 1, 1))
                    .build());
        }
    }

    @Nested
    @DisplayName("Income Calculation Tests")
    class IncomeCalculationTests {

        @Test
        @DisplayName("Should return zero before start date")
        void zeroBeforeStart() {
            OtherIncome income = createTestIncome();

            assertEquals(0, BigDecimal.ZERO.compareTo(
                income.getMonthlyIncome(LocalDate.of(2023, 12, 31))));
        }

        @Test
        @DisplayName("Should return base amount on start date")
        void baseAmountOnStart() {
            OtherIncome income = createTestIncome();

            assertEquals(0, new BigDecimal("1000").compareTo(
                income.getMonthlyIncome(START_DATE)));
        }

        @Test
        @DisplayName("Should return zero after end date")
        void zeroAfterEnd() {
            OtherIncome income = OtherIncome.builder()
                .name("Test")
                .incomeType(OtherIncomeType.RENTAL)
                .monthlyAmount(1000.00)
                .startDate(START_DATE)
                .endDate(END_DATE)
                .build();

            assertEquals(0, BigDecimal.ZERO.compareTo(
                income.getMonthlyIncome(LocalDate.of(2030, 1, 1))));
        }

        @Test
        @DisplayName("Should return base amount without inflation")
        void baseAmountWithoutInflation() {
            OtherIncome income = createTestIncome();

            // 5 years later without inflation
            assertEquals(0, new BigDecimal("1000").compareTo(
                income.getMonthlyIncome(LocalDate.of(2029, 1, 1))));
        }

        @Test
        @DisplayName("Should apply inflation after one year")
        void inflationAfterOneYear() {
            OtherIncome income = OtherIncome.builder()
                .name("Test")
                .incomeType(OtherIncomeType.RENTAL)
                .monthlyAmount(1000.00)
                .startDate(START_DATE)
                .inflationRate(0.03)
                .build();

            // After 1 year: 1000 * 1.03 = 1030
            assertEquals(0, new BigDecimal("1030.00").compareTo(
                income.getMonthlyIncome(LocalDate.of(2025, 1, 1))));
        }

        @Test
        @DisplayName("Should compound inflation over multiple years")
        void compoundInflation() {
            OtherIncome income = OtherIncome.builder()
                .name("Test")
                .incomeType(OtherIncomeType.RENTAL)
                .monthlyAmount(1000.00)
                .startDate(START_DATE)
                .inflationRate(0.03)
                .build();

            // After 3 years: 1000 * 1.03^3 = 1092.73
            assertEquals(0, new BigDecimal("1092.73").compareTo(
                income.getMonthlyIncome(LocalDate.of(2027, 1, 1))));
        }

        private OtherIncome createTestIncome() {
            return OtherIncome.builder()
                .name("Test Income")
                .incomeType(OtherIncomeType.RENTAL)
                .monthlyAmount(1000.00)
                .startDate(START_DATE)
                .build();
        }
    }

    @Nested
    @DisplayName("Annual Amount Tests")
    class AnnualAmountTests {

        @Test
        @DisplayName("Should calculate annual amount")
        void annualAmount() {
            OtherIncome income = OtherIncome.builder()
                .name("Test")
                .incomeType(OtherIncomeType.RENTAL)
                .monthlyAmount(1000.00)
                .startDate(START_DATE)
                .build();

            assertEquals(0, new BigDecimal("12000").compareTo(income.getAnnualAmount()));
        }

        @Test
        @DisplayName("Should calculate annual income with inflation")
        void annualIncomeWithInflation() {
            OtherIncome income = OtherIncome.builder()
                .name("Test")
                .incomeType(OtherIncomeType.RENTAL)
                .monthlyAmount(1000.00)
                .startDate(START_DATE)
                .inflationRate(0.03)
                .build();

            // After 1 year: 1030 * 12 = 12360
            assertEquals(0, new BigDecimal("12360.00").compareTo(
                income.getAnnualIncome(LocalDate.of(2025, 1, 1))));
        }
    }

    @Nested
    @DisplayName("Active Status Tests")
    class ActiveStatusTests {

        @Test
        @DisplayName("Should be inactive before start")
        void inactiveBeforeStart() {
            OtherIncome income = createOngoingIncome();

            assertFalse(income.isActive(LocalDate.of(2023, 12, 31)));
        }

        @Test
        @DisplayName("Should be active on start date")
        void activeOnStart() {
            OtherIncome income = createOngoingIncome();

            assertTrue(income.isActive(START_DATE));
        }

        @Test
        @DisplayName("Should be active after start for ongoing income")
        void activeAfterStartOngoing() {
            OtherIncome income = createOngoingIncome();

            assertTrue(income.isActive(LocalDate.of(2050, 1, 1)));
        }

        @Test
        @DisplayName("Should be inactive after end date")
        void inactiveAfterEnd() {
            OtherIncome income = OtherIncome.builder()
                .name("Test")
                .incomeType(OtherIncomeType.PART_TIME_WORK)
                .monthlyAmount(2000.00)
                .startDate(START_DATE)
                .endDate(END_DATE)
                .build();

            assertFalse(income.isActive(LocalDate.of(2030, 1, 1)));
        }

        @Test
        @DisplayName("Should identify ongoing income")
        void isOngoing() {
            OtherIncome ongoing = createOngoingIncome();
            OtherIncome temporary = OtherIncome.builder()
                .name("Temporary")
                .incomeType(OtherIncomeType.PART_TIME_WORK)
                .monthlyAmount(1000.00)
                .startDate(START_DATE)
                .endDate(END_DATE)
                .build();

            assertTrue(ongoing.isOngoing());
            assertFalse(temporary.isOngoing());
        }

        private OtherIncome createOngoingIncome() {
            return OtherIncome.builder()
                .name("Ongoing Income")
                .incomeType(OtherIncomeType.RENTAL)
                .monthlyAmount(1000.00)
                .startDate(START_DATE)
                .build();
        }
    }

    @Nested
    @DisplayName("Earned Income Tests")
    class EarnedIncomeTests {

        @Test
        @DisplayName("Rental income should be earned")
        void rentalIsEarned() {
            OtherIncome income = OtherIncome.builder()
                .name("Rental")
                .incomeType(OtherIncomeType.RENTAL)
                .monthlyAmount(1000.00)
                .startDate(START_DATE)
                .build();

            assertTrue(income.isEarnedIncome());
        }

        @Test
        @DisplayName("Dividend income should not be earned")
        void dividendsNotEarned() {
            OtherIncome income = OtherIncome.builder()
                .name("Dividends")
                .incomeType(OtherIncomeType.DIVIDENDS)
                .monthlyAmount(500.00)
                .startDate(START_DATE)
                .build();

            assertFalse(income.isEarnedIncome());
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("Equal incomes should be equal")
        void equalIncomes() {
            OtherIncome i1 = createTestIncome();
            OtherIncome i2 = createTestIncome();

            assertEquals(i1, i2);
            assertEquals(i1.hashCode(), i2.hashCode());
        }

        @Test
        @DisplayName("Different incomes should not be equal")
        void differentIncomes() {
            OtherIncome i1 = createTestIncome();
            OtherIncome i2 = OtherIncome.builder()
                .name("Different")
                .incomeType(OtherIncomeType.RENTAL)
                .monthlyAmount(1000.00)
                .startDate(START_DATE)
                .build();

            assertNotEquals(i1, i2);
        }

        private OtherIncome createTestIncome() {
            return OtherIncome.builder()
                .name("Test")
                .incomeType(OtherIncomeType.RENTAL)
                .monthlyAmount(1000.00)
                .startDate(START_DATE)
                .build();
        }
    }
}
