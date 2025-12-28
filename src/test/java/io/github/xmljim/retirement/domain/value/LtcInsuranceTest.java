package io.github.xmljim.retirement.domain.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.LtcTriggerMode;
import io.github.xmljim.retirement.domain.exception.ValidationException;

@DisplayName("LtcInsurance Tests")
class LtcInsuranceTest {

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Build with required fields")
        void buildWithRequiredFields() {
            LtcInsurance ltc = LtcInsurance.builder()
                .dailyBenefit(200)
                .annualPremium(2400)
                .triggerMode(LtcTriggerMode.DETERMINISTIC)
                .deterministicTriggerAge(80)
                .build();

            assertEquals(0, new BigDecimal("200").compareTo(ltc.getDailyBenefit()));
            assertEquals(0, new BigDecimal("2400").compareTo(ltc.getAnnualPremium()));
            assertEquals(LtcTriggerMode.DETERMINISTIC, ltc.getTriggerMode());
            assertEquals(80, ltc.getDeterministicTriggerAge().orElse(0));
        }

        @Test
        @DisplayName("Deterministic mode requires trigger age")
        void deterministicRequiresTriggerAge() {
            assertThrows(ValidationException.class, () ->
                LtcInsurance.builder()
                    .dailyBenefit(200)
                    .triggerMode(LtcTriggerMode.DETERMINISTIC)
                    .build()
            );
        }

        @Test
        @DisplayName("NONE mode does not require trigger age")
        void noneModeNoTriggerAge() {
            LtcInsurance ltc = LtcInsurance.builder()
                .dailyBenefit(200)
                .triggerMode(LtcTriggerMode.NONE)
                .build();

            assertTrue(ltc.getDeterministicTriggerAge().isEmpty());
        }
    }

    @Nested
    @DisplayName("Premium Tests")
    class PremiumTests {

        @Test
        @DisplayName("Monthly premium calculation")
        void monthlyPremium() {
            LtcInsurance ltc = LtcInsurance.builder()
                .annualPremium(2400)
                .triggerMode(LtcTriggerMode.NONE)
                .build();

            assertEquals(0, new BigDecimal("200.00").compareTo(ltc.getMonthlyPremium()));
        }

        @Test
        @DisplayName("Premium before start date returns zero")
        void premiumBeforeStart() {
            LocalDate startDate = LocalDate.of(2025, 1, 1);
            LtcInsurance ltc = LtcInsurance.builder()
                .annualPremium(2400)
                .premiumStartDate(startDate)
                .triggerMode(LtcTriggerMode.NONE)
                .build();

            BigDecimal premium = ltc.getMonthlyPremium(LocalDate.of(2024, 6, 1));
            assertEquals(0, BigDecimal.ZERO.compareTo(premium));
        }

        @Test
        @DisplayName("Paid-up policy returns zero after end date")
        void paidUpPolicy() {
            LocalDate startDate = LocalDate.of(2020, 1, 1);
            LocalDate endDate = LocalDate.of(2030, 1, 1);

            LtcInsurance ltc = LtcInsurance.builder()
                .annualPremium(2400)
                .premiumStartDate(startDate)
                .premiumEndDate(endDate)
                .triggerMode(LtcTriggerMode.NONE)
                .build();

            assertTrue(ltc.isPaidUp());
            assertEquals(0, BigDecimal.ZERO.compareTo(ltc.getMonthlyPremium(LocalDate.of(2031, 1, 1))));
        }
    }

    @Nested
    @DisplayName("Benefit Pool Tests")
    class BenefitPoolTests {

        @Test
        @DisplayName("Initial benefit pool calculation")
        void initialBenefitPool() {
            LtcInsurance ltc = LtcInsurance.builder()
                .dailyBenefit(200)
                .benefitPeriodYears(3)
                .triggerMode(LtcTriggerMode.NONE)
                .build();

            // 200 * 365 * 3 = 219,000
            BigDecimal expected = new BigDecimal("219000");
            assertEquals(0, expected.compareTo(ltc.getInitialBenefitPool().orElse(BigDecimal.ZERO)));
        }

        @Test
        @DisplayName("Unlimited benefit returns empty")
        void unlimitedBenefit() {
            LtcInsurance ltc = LtcInsurance.builder()
                .dailyBenefit(200)
                .unlimitedBenefit()
                .triggerMode(LtcTriggerMode.NONE)
                .build();

            assertTrue(ltc.isUnlimitedBenefit());
            assertTrue(ltc.getInitialBenefitPool().isEmpty());
        }

        @Test
        @DisplayName("Daily benefit with inflation")
        void dailyBenefitWithInflation() {
            LocalDate startDate = LocalDate.of(2020, 1, 1);
            LtcInsurance ltc = LtcInsurance.builder()
                .dailyBenefit(200)
                .inflationRate(0.03)
                .premiumStartDate(startDate)
                .triggerMode(LtcTriggerMode.NONE)
                .build();

            // After 5 years: 200 * 1.03^5 ≈ 231.85
            BigDecimal adjustedBenefit = ltc.getDailyBenefit(LocalDate.of(2025, 1, 1));
            assertTrue(adjustedBenefit.compareTo(new BigDecimal("230")) > 0);
            assertTrue(adjustedBenefit.compareTo(new BigDecimal("235")) < 0);
        }

        @Test
        @DisplayName("Remaining benefits calculation")
        void remainingBenefits() {
            LocalDate startDate = LocalDate.of(2020, 1, 1);
            LtcInsurance ltc = LtcInsurance.builder()
                .dailyBenefit(200)
                .benefitPeriodYears(3)
                .inflationRate(BigDecimal.ZERO)
                .premiumStartDate(startDate)
                .triggerMode(LtcTriggerMode.NONE)
                .build();

            BigDecimal claimsPaid = new BigDecimal("50000");
            BigDecimal remaining = ltc.getRemainingBenefits(claimsPaid, startDate).orElse(BigDecimal.ZERO);

            // 219000 - 50000 = 169000
            assertEquals(0, new BigDecimal("169000").compareTo(remaining));
        }

        @Test
        @DisplayName("Benefit exhaustion check")
        void benefitExhaustion() {
            LocalDate startDate = LocalDate.of(2020, 1, 1);
            LtcInsurance ltc = LtcInsurance.builder()
                .dailyBenefit(200)
                .benefitPeriodYears(3)
                .inflationRate(BigDecimal.ZERO)
                .premiumStartDate(startDate)
                .triggerMode(LtcTriggerMode.NONE)
                .build();

            assertFalse(ltc.isBenefitExhausted(new BigDecimal("50000"), startDate));
            assertTrue(ltc.isBenefitExhausted(new BigDecimal("250000"), startDate));
        }
    }

    @Nested
    @DisplayName("Trigger Mode Tests")
    class TriggerModeTests {

        @Test
        @DisplayName("Deterministic mode")
        void deterministicMode() {
            LtcInsurance ltc = LtcInsurance.builder()
                .triggerMode(LtcTriggerMode.DETERMINISTIC)
                .deterministicTriggerAge(80)
                .build();

            assertEquals(LtcTriggerMode.DETERMINISTIC, ltc.getTriggerMode());
            assertEquals(80, ltc.getDeterministicTriggerAge().orElse(0));
        }

        @Test
        @DisplayName("Probabilistic mode")
        void probabilisticMode() {
            LtcInsurance ltc = LtcInsurance.builder()
                .triggerMode(LtcTriggerMode.PROBABILISTIC)
                .build();

            assertEquals(LtcTriggerMode.PROBABILISTIC, ltc.getTriggerMode());
            assertTrue(ltc.getTriggerMode().requiresMonteCarlo());
        }
    }
}
