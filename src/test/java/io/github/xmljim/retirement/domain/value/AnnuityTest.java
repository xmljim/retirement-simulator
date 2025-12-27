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

import io.github.xmljim.retirement.domain.enums.AnnuityType;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;

@DisplayName("Annuity Tests")
class AnnuityTest {

    private static final LocalDate PURCHASE_DATE = LocalDate.of(2024, 1, 1);
    private static final LocalDate START_DATE = LocalDate.of(2024, 1, 1);

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should build fixed immediate annuity")
        void buildFixedImmediate() {
            Annuity annuity = Annuity.builder()
                .name("My Annuity")
                .annuityType(AnnuityType.FIXED_IMMEDIATE)
                .purchaseAmount(100000.00)
                .monthlyBenefit(500.00)
                .purchaseDate(PURCHASE_DATE)
                .paymentStartDate(START_DATE)
                .build();

            assertEquals("My Annuity", annuity.getName());
            assertEquals(AnnuityType.FIXED_IMMEDIATE, annuity.getAnnuityType());
            assertEquals(0, new BigDecimal("100000").compareTo(annuity.getPurchaseAmount()));
            assertEquals(0, new BigDecimal("500").compareTo(annuity.getMonthlyBenefit()));
        }

        @Test
        @DisplayName("Should build with COLA rate")
        void buildWithCola() {
            Annuity annuity = Annuity.builder()
                .name("COLA Annuity")
                .annuityType(AnnuityType.FIXED_IMMEDIATE)
                .monthlyBenefit(1000.00)
                .colaRate(0.02)
                .purchaseDate(PURCHASE_DATE)
                .paymentStartDate(START_DATE)
                .build();

            assertTrue(annuity.getColaRate().isPresent());
            assertEquals(0, new BigDecimal("0.02").compareTo(annuity.getColaRate().get()));
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw for null name")
        void nullName() {
            assertThrows(MissingRequiredFieldException.class, () ->
                Annuity.builder()
                    .annuityType(AnnuityType.FIXED_IMMEDIATE)
                    .monthlyBenefit(500.00)
                    .purchaseDate(PURCHASE_DATE)
                    .paymentStartDate(START_DATE)
                    .build());
        }

        @Test
        @DisplayName("Should throw for blank name")
        void blankName() {
            assertThrows(ValidationException.class, () ->
                Annuity.builder()
                    .name("   ")
                    .annuityType(AnnuityType.FIXED_IMMEDIATE)
                    .monthlyBenefit(500.00)
                    .purchaseDate(PURCHASE_DATE)
                    .paymentStartDate(START_DATE)
                    .build());
        }

        @Test
        @DisplayName("Should throw for null annuity type")
        void nullAnnuityType() {
            assertThrows(MissingRequiredFieldException.class, () ->
                Annuity.builder()
                    .name("Test")
                    .monthlyBenefit(500.00)
                    .purchaseDate(PURCHASE_DATE)
                    .paymentStartDate(START_DATE)
                    .build());
        }

        @Test
        @DisplayName("Should throw for negative purchase amount")
        void negativePurchaseAmount() {
            assertThrows(ValidationException.class, () ->
                Annuity.builder()
                    .name("Test")
                    .annuityType(AnnuityType.FIXED_IMMEDIATE)
                    .purchaseAmount(-1000.00)
                    .monthlyBenefit(500.00)
                    .purchaseDate(PURCHASE_DATE)
                    .paymentStartDate(START_DATE)
                    .build());
        }

        @Test
        @DisplayName("Should throw for immediate annuity with zero benefit")
        void immediateWithZeroBenefit() {
            assertThrows(ValidationException.class, () ->
                Annuity.builder()
                    .name("Test")
                    .annuityType(AnnuityType.FIXED_IMMEDIATE)
                    .purchaseDate(PURCHASE_DATE)
                    .paymentStartDate(START_DATE)
                    .build());
        }

        @Test
        @DisplayName("Should throw when payment start before purchase")
        void paymentBeforePurchase() {
            assertThrows(ValidationException.class, () ->
                Annuity.builder()
                    .name("Test")
                    .annuityType(AnnuityType.FIXED_IMMEDIATE)
                    .monthlyBenefit(500.00)
                    .purchaseDate(LocalDate.of(2024, 6, 1))
                    .paymentStartDate(LocalDate.of(2024, 1, 1))
                    .build());
        }
    }

    @Nested
    @DisplayName("COLA Calculation Tests")
    class ColaCalculationTests {

        @Test
        @DisplayName("Should return zero before payment start")
        void zeroBeforeStart() {
            Annuity annuity = Annuity.builder()
                .name("Test")
                .annuityType(AnnuityType.FIXED_IMMEDIATE)
                .monthlyBenefit(1000.00)
                .purchaseDate(PURCHASE_DATE)
                .paymentStartDate(START_DATE)
                .build();

            assertEquals(0, BigDecimal.ZERO.compareTo(
                annuity.getMonthlyBenefitAsOf(LocalDate.of(2023, 12, 1))));
        }

        @Test
        @DisplayName("Should return base benefit without COLA")
        void baseBenefitWithoutCola() {
            Annuity annuity = Annuity.builder()
                .name("Test")
                .annuityType(AnnuityType.FIXED_IMMEDIATE)
                .monthlyBenefit(1000.00)
                .purchaseDate(PURCHASE_DATE)
                .paymentStartDate(START_DATE)
                .build();

            assertEquals(0, new BigDecimal("1000").compareTo(
                annuity.getMonthlyBenefitAsOf(LocalDate.of(2030, 1, 1))));
        }

        @Test
        @DisplayName("Should apply COLA after one year")
        void colaAfterOneYear() {
            Annuity annuity = Annuity.builder()
                .name("Test")
                .annuityType(AnnuityType.FIXED_IMMEDIATE)
                .monthlyBenefit(1000.00)
                .colaRate(0.02)
                .purchaseDate(PURCHASE_DATE)
                .paymentStartDate(START_DATE)
                .build();

            // After 1 year: 1000 * 1.02 = 1020
            assertEquals(0, new BigDecimal("1020.00").compareTo(
                annuity.getMonthlyBenefitAsOf(LocalDate.of(2025, 1, 1))));
        }

        @Test
        @DisplayName("Should compound COLA over multiple years")
        void compoundCola() {
            Annuity annuity = Annuity.builder()
                .name("Test")
                .annuityType(AnnuityType.FIXED_IMMEDIATE)
                .monthlyBenefit(1000.00)
                .colaRate(0.03)
                .purchaseDate(PURCHASE_DATE)
                .paymentStartDate(START_DATE)
                .build();

            // After 3 years: 1000 * 1.03^3 = 1092.73
            BigDecimal expected = new BigDecimal("1092.73");
            assertEquals(0, expected.compareTo(
                annuity.getMonthlyBenefitAsOf(LocalDate.of(2027, 1, 1))));
        }
    }

    @Nested
    @DisplayName("Annual Benefit Tests")
    class AnnualBenefitTests {

        @Test
        @DisplayName("Should calculate annual benefit")
        void annualBenefit() {
            Annuity annuity = Annuity.builder()
                .name("Test")
                .annuityType(AnnuityType.FIXED_IMMEDIATE)
                .monthlyBenefit(1000.00)
                .purchaseDate(PURCHASE_DATE)
                .paymentStartDate(START_DATE)
                .build();

            assertEquals(0, new BigDecimal("12000").compareTo(annuity.getAnnualBenefit()));
        }
    }

    @Nested
    @DisplayName("Payment Active Tests")
    class PaymentActiveTests {

        @Test
        @DisplayName("Should return false before start date")
        void notActiveBeforeStart() {
            Annuity annuity = Annuity.builder()
                .name("Test")
                .annuityType(AnnuityType.FIXED_IMMEDIATE)
                .monthlyBenefit(500.00)
                .purchaseDate(PURCHASE_DATE)
                .paymentStartDate(START_DATE)
                .build();

            assertFalse(annuity.isPaymentActive(LocalDate.of(2023, 12, 31)));
        }

        @Test
        @DisplayName("Should return true on start date")
        void activeOnStartDate() {
            Annuity annuity = Annuity.builder()
                .name("Test")
                .annuityType(AnnuityType.FIXED_IMMEDIATE)
                .monthlyBenefit(500.00)
                .purchaseDate(PURCHASE_DATE)
                .paymentStartDate(START_DATE)
                .build();

            assertTrue(annuity.isPaymentActive(START_DATE));
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("Equal annuities should be equal")
        void equalAnnuities() {
            Annuity a1 = createTestAnnuity();
            Annuity a2 = createTestAnnuity();

            assertEquals(a1, a2);
            assertEquals(a1.hashCode(), a2.hashCode());
        }

        @Test
        @DisplayName("Different annuities should not be equal")
        void differentAnnuities() {
            Annuity a1 = createTestAnnuity();
            Annuity a2 = Annuity.builder()
                .name("Different")
                .annuityType(AnnuityType.FIXED_IMMEDIATE)
                .monthlyBenefit(500.00)
                .purchaseDate(PURCHASE_DATE)
                .paymentStartDate(START_DATE)
                .build();

            assertNotEquals(a1, a2);
        }

        private Annuity createTestAnnuity() {
            return Annuity.builder()
                .name("Test")
                .annuityType(AnnuityType.FIXED_IMMEDIATE)
                .monthlyBenefit(500.00)
                .purchaseDate(PURCHASE_DATE)
                .paymentStartDate(START_DATE)
                .build();
        }
    }
}
