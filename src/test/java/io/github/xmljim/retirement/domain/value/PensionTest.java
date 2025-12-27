package io.github.xmljim.retirement.domain.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.PensionPaymentForm;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;

@DisplayName("Pension Tests")
class PensionTest {

    private static final LocalDate START_DATE = LocalDate.of(2025, 1, 1);
    private static final BigDecimal MONTHLY_BENEFIT = new BigDecimal("3000");

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should build with required fields")
        void buildWithRequiredFields() {
            Pension pension = Pension.builder()
                .name("State Pension")
                .monthlyBenefit(MONTHLY_BENEFIT)
                .startDate(START_DATE)
                .build();

            assertEquals("State Pension", pension.getName());
            assertEquals(0, MONTHLY_BENEFIT.compareTo(pension.getMonthlyBenefit()));
            assertEquals(START_DATE, pension.getStartDate());
            assertEquals(PensionPaymentForm.SINGLE_LIFE, pension.getPaymentForm());
        }

        @Test
        @DisplayName("Should build with all fields")
        void buildWithAllFields() {
            Pension pension = Pension.builder()
                .name("FERS Pension")
                .monthlyBenefit(3500)
                .startDate(START_DATE)
                .paymentForm(PensionPaymentForm.JOINT_100)
                .colaRate(0.02)
                .build();

            assertEquals("FERS Pension", pension.getName());
            assertEquals(PensionPaymentForm.JOINT_100, pension.getPaymentForm());
            assertTrue(pension.hasCola());
            assertEquals(0, new BigDecimal("0.02").compareTo(pension.getColaRate().orElseThrow()));
        }

        @Test
        @DisplayName("Should throw for missing name")
        void throwsForMissingName() {
            assertThrows(MissingRequiredFieldException.class, () ->
                Pension.builder()
                    .monthlyBenefit(MONTHLY_BENEFIT)
                    .startDate(START_DATE)
                    .build());
        }

        @Test
        @DisplayName("Should throw for missing monthly benefit")
        void throwsForMissingMonthlyBenefit() {
            assertThrows(MissingRequiredFieldException.class, () ->
                Pension.builder()
                    .name("Test")
                    .startDate(START_DATE)
                    .build());
        }

        @Test
        @DisplayName("Should throw for missing start date")
        void throwsForMissingStartDate() {
            assertThrows(MissingRequiredFieldException.class, () ->
                Pension.builder()
                    .name("Test")
                    .monthlyBenefit(MONTHLY_BENEFIT)
                    .build());
        }

        @Test
        @DisplayName("Should throw for negative monthly benefit")
        void throwsForNegativeMonthlyBenefit() {
            assertThrows(ValidationException.class, () ->
                Pension.builder()
                    .name("Test")
                    .monthlyBenefit(-1000)
                    .startDate(START_DATE)
                    .build());
        }

        @Test
        @DisplayName("Should throw for PERIOD_CERTAIN without years")
        void throwsForPeriodCertainWithoutYears() {
            assertThrows(MissingRequiredFieldException.class, () ->
                Pension.builder()
                    .name("Test")
                    .monthlyBenefit(MONTHLY_BENEFIT)
                    .startDate(START_DATE)
                    .paymentForm(PensionPaymentForm.PERIOD_CERTAIN)
                    .build());
        }

        @Test
        @DisplayName("Should allow PERIOD_CERTAIN with years")
        void allowsPeriodCertainWithYears() {
            Pension pension = Pension.builder()
                .name("Test")
                .monthlyBenefit(MONTHLY_BENEFIT)
                .startDate(START_DATE)
                .paymentForm(PensionPaymentForm.PERIOD_CERTAIN)
                .periodCertainYears(15)
                .build();

            assertEquals(15, pension.getPeriodCertainYears().orElseThrow());
        }
    }

    @Nested
    @DisplayName("Benefit Calculation Tests")
    class BenefitCalculationTests {

        @Test
        @DisplayName("Should return annual benefit")
        void returnsAnnualBenefit() {
            Pension pension = createPension(null);
            // 3000 * 12 = 36000
            assertEquals(0, new BigDecimal("36000").compareTo(pension.getAnnualBenefit()));
        }

        @Test
        @DisplayName("Should return zero before start date")
        void returnsZeroBeforeStartDate() {
            Pension pension = createPension(null);
            LocalDate beforeStart = START_DATE.minusDays(1);
            assertEquals(0, BigDecimal.ZERO.compareTo(pension.getMonthlyBenefit(beforeStart)));
        }

        @Test
        @DisplayName("Should return benefit on start date")
        void returnsBenefitOnStartDate() {
            Pension pension = createPension(null);
            assertEquals(0, MONTHLY_BENEFIT.compareTo(pension.getMonthlyBenefit(START_DATE)));
        }

        @Test
        @DisplayName("Should return benefit after start date without COLA")
        void returnsBenefitWithoutCola() {
            Pension pension = createPension(null);
            LocalDate futureDate = START_DATE.plusYears(5);
            // No COLA - same benefit
            assertEquals(0, MONTHLY_BENEFIT.compareTo(pension.getMonthlyBenefit(futureDate)));
        }
    }

    @Nested
    @DisplayName("COLA Calculation Tests")
    class ColaCalculationTests {

        @Test
        @DisplayName("Should apply COLA after one year")
        void appliesColaAfterOneYear() {
            Pension pension = createPension(0.02);  // 2% COLA
            LocalDate oneYearLater = START_DATE.plusYears(1);

            // 3000 * 1.02 = 3060
            BigDecimal expected = new BigDecimal("3060.00");
            assertEquals(0, expected.compareTo(pension.getMonthlyBenefit(oneYearLater)));
        }

        @Test
        @DisplayName("Should compound COLA over multiple years")
        void compoundsColaOverYears() {
            Pension pension = createPension(0.02);  // 2% COLA
            LocalDate fiveYearsLater = START_DATE.plusYears(5);

            // 3000 * (1.02)^5 = 3312.24...
            BigDecimal expected = MONTHLY_BENEFIT
                .multiply(BigDecimal.valueOf(1.02).pow(5))
                .setScale(2, RoundingMode.HALF_UP);
            assertEquals(0, expected.compareTo(pension.getMonthlyBenefit(fiveYearsLater)));
        }

        @Test
        @DisplayName("Should not apply COLA before anniversary")
        void noColaBeforeAnniversary() {
            Pension pension = createPension(0.02);
            LocalDate beforeAnniversary = START_DATE.plusMonths(11);

            assertEquals(0, MONTHLY_BENEFIT.compareTo(pension.getMonthlyBenefit(beforeAnniversary)));
        }

        @Test
        @DisplayName("Should return annual benefit with COLA")
        void returnsAnnualBenefitWithCola() {
            Pension pension = createPension(0.02);

            // Year 2026 (one year after start)
            // Monthly: 3060, Annual: 36720
            BigDecimal expected = new BigDecimal("36720.00");
            assertEquals(0, expected.compareTo(pension.getAnnualBenefit(2026)));
        }

        @Test
        @DisplayName("hasCola should return true when COLA is set")
        void hasColaWhenSet() {
            Pension withCola = createPension(0.02);
            Pension withoutCola = createPension(null);

            assertTrue(withCola.hasCola());
            assertFalse(withoutCola.hasCola());
        }
    }

    @Nested
    @DisplayName("Survivor Benefit Tests")
    class SurvivorBenefitTests {

        @Test
        @DisplayName("Should have no survivor benefit for SINGLE_LIFE")
        void noSurvivorForSingleLife() {
            Pension pension = Pension.builder()
                .name("Test")
                .monthlyBenefit(MONTHLY_BENEFIT)
                .startDate(START_DATE)
                .paymentForm(PensionPaymentForm.SINGLE_LIFE)
                .build();

            assertFalse(pension.hasSurvivorBenefit());
            assertEquals(0, BigDecimal.ZERO.compareTo(pension.getSurvivorBenefit(START_DATE)));
        }

        @Test
        @DisplayName("Should calculate 100% survivor benefit")
        void calculates100PercentSurvivor() {
            Pension pension = Pension.builder()
                .name("Test")
                .monthlyBenefit(MONTHLY_BENEFIT)
                .startDate(START_DATE)
                .paymentForm(PensionPaymentForm.JOINT_100)
                .build();

            assertTrue(pension.hasSurvivorBenefit());
            assertEquals(0, MONTHLY_BENEFIT.compareTo(pension.getSurvivorBenefit(START_DATE)));
        }

        @Test
        @DisplayName("Should calculate 50% survivor benefit")
        void calculates50PercentSurvivor() {
            Pension pension = Pension.builder()
                .name("Test")
                .monthlyBenefit(MONTHLY_BENEFIT)
                .startDate(START_DATE)
                .paymentForm(PensionPaymentForm.JOINT_50)
                .build();

            // 3000 * 0.50 = 1500
            BigDecimal expected = new BigDecimal("1500.00");
            assertEquals(0, expected.compareTo(pension.getSurvivorBenefit(START_DATE)));
        }

        @Test
        @DisplayName("Should apply COLA to survivor benefit")
        void appliesColaToSurvivorBenefit() {
            Pension pension = Pension.builder()
                .name("Test")
                .monthlyBenefit(MONTHLY_BENEFIT)
                .startDate(START_DATE)
                .paymentForm(PensionPaymentForm.JOINT_50)
                .colaRate(0.02)
                .build();

            LocalDate oneYearLater = START_DATE.plusYears(1);
            // (3000 * 1.02) * 0.50 = 1530
            BigDecimal expected = new BigDecimal("1530.00");
            assertEquals(0, expected.compareTo(pension.getSurvivorBenefit(oneYearLater)));
        }
    }

    @Nested
    @DisplayName("Active Status Tests")
    class ActiveStatusTests {

        @Test
        @DisplayName("Should not be active before start date")
        void notActiveBeforeStart() {
            Pension pension = createPension(null);
            assertFalse(pension.isActiveOn(START_DATE.minusDays(1)));
        }

        @Test
        @DisplayName("Should be active on start date")
        void activeOnStartDate() {
            Pension pension = createPension(null);
            assertTrue(pension.isActiveOn(START_DATE));
        }

        @Test
        @DisplayName("Should be active after start date")
        void activeAfterStartDate() {
            Pension pension = createPension(null);
            assertTrue(pension.isActiveOn(START_DATE.plusYears(10)));
        }

        @Test
        @DisplayName("Should handle null date")
        void handlesNullDate() {
            Pension pension = createPension(null);
            assertFalse(pension.isActiveOn(null));
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("Equal pensions should be equal")
        void equalPensions() {
            Pension p1 = createPension(0.02);
            Pension p2 = createPension(0.02);

            assertEquals(p1, p2);
            assertEquals(p1.hashCode(), p2.hashCode());
        }

        @Test
        @DisplayName("Different pensions should not be equal")
        void differentPensions() {
            Pension p1 = createPension(0.02);
            Pension p2 = Pension.builder()
                .name("Different")
                .monthlyBenefit(MONTHLY_BENEFIT)
                .startDate(START_DATE)
                .build();

            assertNotEquals(p1, p2);
        }

        @Test
        @DisplayName("Same object should be equal to itself")
        void sameObject() {
            Pension p1 = createPension(null);
            assertEquals(p1, p1);
        }

        @Test
        @DisplayName("Should not equal null")
        void notEqualNull() {
            Pension p1 = createPension(null);
            assertNotEquals(null, p1);
        }
    }

    private Pension createPension(Double colaRate) {
        Pension.Builder builder = Pension.builder()
            .name("Test Pension")
            .monthlyBenefit(MONTHLY_BENEFIT)
            .startDate(START_DATE);

        if (colaRate != null) {
            builder.colaRate(colaRate);
        }

        return builder.build();
    }
}
