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

import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;

@DisplayName("SocialSecurityBenefit Tests")
class SocialSecurityBenefitTest {

    private static final BigDecimal FRA_BENEFIT = new BigDecimal("2000");
    private static final int BIRTH_YEAR_1960 = 1960;
    private static final int FRA_67_MONTHS = 804;
    private static final int AGE_62_MONTHS = 744;
    private static final int AGE_70_MONTHS = 840;
    private static final LocalDate START_DATE = LocalDate.of(2027, 1, 1);

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should build with all required fields")
        void buildWithRequiredFields() {
            SocialSecurityBenefit benefit = SocialSecurityBenefit.builder()
                .fraBenefit(FRA_BENEFIT)
                .birthYear(BIRTH_YEAR_1960)
                .claimingAgeMonths(FRA_67_MONTHS)
                .startDate(START_DATE)
                .build();

            assertEquals(0, FRA_BENEFIT.compareTo(benefit.getFraBenefit()));
            assertEquals(BIRTH_YEAR_1960, benefit.getBirthYear());
            assertEquals(FRA_67_MONTHS, benefit.getClaimingAgeMonths());
            assertEquals(START_DATE, benefit.getStartDate());
        }

        @Test
        @DisplayName("Should build using claimingAge helper")
        void buildWithClaimingAgeHelper() {
            SocialSecurityBenefit benefit = SocialSecurityBenefit.builder()
                .fraBenefit(2000)
                .birthYear(BIRTH_YEAR_1960)
                .claimingAge(67, 0)
                .startDate(START_DATE)
                .build();

            assertEquals(FRA_67_MONTHS, benefit.getClaimingAgeMonths());
        }

        @Test
        @DisplayName("Should throw when startDate is null")
        void throwsOnNullStartDate() {
            assertThrows(MissingRequiredFieldException.class, () ->
                SocialSecurityBenefit.builder()
                    .fraBenefit(FRA_BENEFIT)
                    .birthYear(BIRTH_YEAR_1960)
                    .claimingAgeMonths(FRA_67_MONTHS)
                    .build());
        }

        @Test
        @DisplayName("Should throw when fraBenefit is negative")
        void throwsOnNegativeBenefit() {
            assertThrows(ValidationException.class, () ->
                SocialSecurityBenefit.builder()
                    .fraBenefit(-1000)
                    .birthYear(BIRTH_YEAR_1960)
                    .claimingAgeMonths(FRA_67_MONTHS)
                    .startDate(START_DATE)
                    .build());
        }

        @Test
        @DisplayName("Should throw when claiming age below 62")
        void throwsOnClaimingAgeTooLow() {
            assertThrows(ValidationException.class, () ->
                SocialSecurityBenefit.builder()
                    .fraBenefit(FRA_BENEFIT)
                    .birthYear(BIRTH_YEAR_1960)
                    .claimingAgeMonths(720) // 60 years
                    .startDate(START_DATE)
                    .build());
        }

        @Test
        @DisplayName("Should throw when claiming age above 70")
        void throwsOnClaimingAgeTooHigh() {
            assertThrows(ValidationException.class, () ->
                SocialSecurityBenefit.builder()
                    .fraBenefit(FRA_BENEFIT)
                    .birthYear(BIRTH_YEAR_1960)
                    .claimingAgeMonths(852) // 71 years
                    .startDate(START_DATE)
                    .build());
        }
    }

    @Nested
    @DisplayName("FRA Calculation Tests")
    class FraCalculationTests {

        @Test
        @DisplayName("Birth year 1960 should have FRA of 67 years (804 months)")
        void fra1960() {
            SocialSecurityBenefit benefit = createBenefit(BIRTH_YEAR_1960, FRA_67_MONTHS);
            assertEquals(804, benefit.getFraAgeMonths());
        }

        @Test
        @DisplayName("Birth year 1955 should have FRA of 66y 2m (794 months)")
        void fra1955() {
            SocialSecurityBenefit benefit = createBenefit(1955, 794);
            assertEquals(794, benefit.getFraAgeMonths());
        }
    }

    @Nested
    @DisplayName("Early/Delayed Claiming Tests")
    class ClaimingTests {

        @Test
        @DisplayName("Claiming at 62 with FRA 67 should be early")
        void isEarlyClaiming() {
            SocialSecurityBenefit benefit = createBenefit(BIRTH_YEAR_1960, AGE_62_MONTHS);
            assertTrue(benefit.isEarlyClaiming());
            assertFalse(benefit.isDelayedClaiming());
        }

        @Test
        @DisplayName("Claiming at FRA should be neither early nor delayed")
        void claimingAtFra() {
            SocialSecurityBenefit benefit = createBenefit(BIRTH_YEAR_1960, FRA_67_MONTHS);
            assertFalse(benefit.isEarlyClaiming());
            assertFalse(benefit.isDelayedClaiming());
        }

        @Test
        @DisplayName("Claiming at 70 should be delayed")
        void isDelayedClaiming() {
            SocialSecurityBenefit benefit = createBenefit(BIRTH_YEAR_1960, AGE_70_MONTHS);
            assertFalse(benefit.isEarlyClaiming());
            assertTrue(benefit.isDelayedClaiming());
        }
    }

    @Nested
    @DisplayName("Adjusted Benefit Tests")
    class AdjustedBenefitTests {

        @Test
        @DisplayName("Claiming at FRA should return full benefit")
        void claimingAtFra() {
            SocialSecurityBenefit benefit = createBenefit(BIRTH_YEAR_1960, FRA_67_MONTHS);
            assertEquals(0, FRA_BENEFIT.compareTo(benefit.getAdjustedMonthlyBenefit()));
        }

        @Test
        @DisplayName("Claiming at 62 with FRA 67 should reduce benefit by ~30%")
        void earlyClaimingAt62() {
            SocialSecurityBenefit benefit = createBenefit(BIRTH_YEAR_1960, AGE_62_MONTHS);
            BigDecimal adjusted = benefit.getAdjustedMonthlyBenefit();
            // 60 months early = 30% reduction = $1400
            BigDecimal expected = new BigDecimal("1400");
            assertEquals(0, expected.setScale(0, RoundingMode.HALF_UP)
                .compareTo(adjusted.setScale(0, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Claiming at 70 should increase benefit by 24%")
        void delayedClaimingAt70() {
            SocialSecurityBenefit benefit = createBenefit(BIRTH_YEAR_1960, AGE_70_MONTHS);
            BigDecimal adjusted = benefit.getAdjustedMonthlyBenefit();
            // 36 months delayed = 24% increase = $2480
            BigDecimal expected = new BigDecimal("2480");
            assertEquals(0, expected.setScale(0, RoundingMode.HALF_UP)
                .compareTo(adjusted.setScale(0, RoundingMode.HALF_UP)));
        }
    }

    @Nested
    @DisplayName("Monthly Benefit with COLA Tests")
    class MonthlyBenefitColaTests {

        @Test
        @DisplayName("Date before start should return zero")
        void beforeStartDate() {
            SocialSecurityBenefit benefit = createBenefitWithCola(0.028);
            BigDecimal monthly = benefit.getMonthlyBenefit(LocalDate.of(2026, 12, 31));
            assertEquals(0, BigDecimal.ZERO.compareTo(monthly));
        }

        @Test
        @DisplayName("Start date should return adjusted benefit (no COLA)")
        void onStartDate() {
            SocialSecurityBenefit benefit = createBenefitWithCola(0.028);
            BigDecimal monthly = benefit.getMonthlyBenefit(START_DATE);
            assertEquals(0, FRA_BENEFIT.compareTo(monthly));
        }

        @Test
        @DisplayName("One year after start should apply one year COLA")
        void oneYearAfterStart() {
            SocialSecurityBenefit benefit = createBenefitWithCola(0.028);
            BigDecimal monthly = benefit.getMonthlyBenefit(LocalDate.of(2028, 1, 1));
            // 2000 * 1.028 = 2056
            BigDecimal expected = new BigDecimal("2056");
            assertEquals(0, expected.setScale(0, RoundingMode.HALF_UP)
                .compareTo(monthly.setScale(0, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Null date should return zero")
        void nullDate() {
            SocialSecurityBenefit benefit = createBenefitWithCola(0.028);
            assertEquals(0, BigDecimal.ZERO.compareTo(benefit.getMonthlyBenefit(null)));
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("Equal benefits should be equal")
        void equalBenefits() {
            SocialSecurityBenefit b1 = createBenefit(BIRTH_YEAR_1960, FRA_67_MONTHS);
            SocialSecurityBenefit b2 = createBenefit(BIRTH_YEAR_1960, FRA_67_MONTHS);
            assertEquals(b1, b2);
            assertEquals(b1.hashCode(), b2.hashCode());
        }

        @Test
        @DisplayName("Different claiming ages should not be equal")
        void differentClaimingAge() {
            SocialSecurityBenefit b1 = createBenefit(BIRTH_YEAR_1960, AGE_62_MONTHS);
            SocialSecurityBenefit b2 = createBenefit(BIRTH_YEAR_1960, FRA_67_MONTHS);
            assertNotEquals(b1, b2);
        }

        @Test
        @DisplayName("Same object should be equal to itself")
        void sameObject() {
            SocialSecurityBenefit b1 = createBenefit(BIRTH_YEAR_1960, FRA_67_MONTHS);
            assertEquals(b1, b1);
        }

        @Test
        @DisplayName("Should not equal null")
        void notEqualNull() {
            SocialSecurityBenefit b1 = createBenefit(BIRTH_YEAR_1960, FRA_67_MONTHS);
            assertNotEquals(null, b1);
        }
    }

    private SocialSecurityBenefit createBenefit(int birthYear, int claimingAgeMonths) {
        return SocialSecurityBenefit.builder()
            .fraBenefit(FRA_BENEFIT)
            .birthYear(birthYear)
            .claimingAgeMonths(claimingAgeMonths)
            .startDate(START_DATE)
            .build();
    }

    private SocialSecurityBenefit createBenefitWithCola(double colaRate) {
        return SocialSecurityBenefit.builder()
            .fraBenefit(FRA_BENEFIT)
            .birthYear(BIRTH_YEAR_1960)
            .claimingAgeMonths(FRA_67_MONTHS)
            .colaRate(colaRate)
            .startDate(START_DATE)
            .build();
    }
}
