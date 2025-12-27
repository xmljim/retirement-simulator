package io.github.xmljim.retirement.domain.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.MarriageEndReason;

@DisplayName("PastMarriage")
class PastMarriageTest {

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodTests {

        @Test
        @DisplayName("withBenefit creates record with FRA benefit")
        void withBenefitCreatesCorrectly() {
            PastMarriage marriage = PastMarriage.withBenefit(
                new BigDecimal("3000"),
                LocalDate.of(2000, 1, 1),
                LocalDate.of(2012, 1, 1),
                MarriageEndReason.DIVORCED
            );

            assertTrue(marriage.exSpouseFraBenefit().isPresent());
            assertEquals(new BigDecimal("3000"), marriage.exSpouseFraBenefit().get());
            assertTrue(marriage.exSpouse().isEmpty());
        }
    }

    @Nested
    @DisplayName("Duration Calculations")
    class DurationTests {

        @Test
        @DisplayName("calculates duration in months correctly")
        void calculatesDurationMonths() {
            PastMarriage marriage = PastMarriage.withBenefit(
                new BigDecimal("2000"),
                LocalDate.of(2000, 1, 1),
                LocalDate.of(2012, 6, 1),
                MarriageEndReason.DIVORCED
            );

            assertEquals(149, marriage.getDurationMonths()); // 12 years 5 months
        }

        @Test
        @DisplayName("calculates duration in years correctly")
        void calculatesDurationYears() {
            PastMarriage marriage = PastMarriage.withBenefit(
                new BigDecimal("2000"),
                LocalDate.of(2000, 1, 1),
                LocalDate.of(2012, 6, 1),
                MarriageEndReason.DIVORCED
            );

            assertEquals(12, marriage.getDurationYears());
        }
    }

    @Nested
    @DisplayName("Divorced Spouse Eligibility")
    class DivorcedSpouseEligibilityTests {

        @Test
        @DisplayName("qualifies if married 10+ years and divorced")
        void qualifiesIfLongEnough() {
            PastMarriage marriage = PastMarriage.withBenefit(
                new BigDecimal("2500"),
                LocalDate.of(2000, 1, 1),
                LocalDate.of(2012, 1, 1), // 12 years
                MarriageEndReason.DIVORCED
            );

            assertTrue(marriage.qualifiesForDivorcedSpouseBenefits());
        }

        @Test
        @DisplayName("does not qualify if married less than 10 years")
        void doesNotQualifyIfTooShort() {
            PastMarriage marriage = PastMarriage.withBenefit(
                new BigDecimal("2500"),
                LocalDate.of(2010, 1, 1),
                LocalDate.of(2018, 1, 1), // 8 years
                MarriageEndReason.DIVORCED
            );

            assertFalse(marriage.qualifiesForDivorcedSpouseBenefits());
        }

        @Test
        @DisplayName("does not qualify if spouse died (not divorced)")
        void doesNotQualifyIfWidowed() {
            PastMarriage marriage = PastMarriage.withBenefit(
                new BigDecimal("2500"),
                LocalDate.of(2000, 1, 1),
                LocalDate.of(2015, 1, 1),
                MarriageEndReason.SPOUSE_DIED
            );

            assertFalse(marriage.qualifiesForDivorcedSpouseBenefits());
        }

        @Test
        @DisplayName("checks if divorced at least N years ago")
        void checksDivorcedYearsAgo() {
            PastMarriage marriage = PastMarriage.withBenefit(
                new BigDecimal("2500"),
                LocalDate.of(2000, 1, 1),
                LocalDate.of(2020, 1, 1),
                MarriageEndReason.DIVORCED
            );

            assertTrue(marriage.divorcedAtLeastYearsAgo(2, LocalDate.of(2025, 1, 1)));
            assertFalse(marriage.divorcedAtLeastYearsAgo(10, LocalDate.of(2025, 1, 1)));
        }
    }

    @Nested
    @DisplayName("Survivor Eligibility")
    class SurvivorEligibilityTests {

        @Test
        @DisplayName("qualifies if married 9+ months and spouse died")
        void qualifiesIfLongEnough() {
            PastMarriage marriage = PastMarriage.withBenefit(
                new BigDecimal("2500"),
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2024, 1, 1),
                MarriageEndReason.SPOUSE_DIED
            );

            assertTrue(marriage.qualifiesForSurvivorBenefits());
        }

        @Test
        @DisplayName("does not qualify if married less than 9 months")
        void doesNotQualifyIfTooShort() {
            PastMarriage marriage = PastMarriage.withBenefit(
                new BigDecimal("2500"),
                LocalDate.of(2024, 6, 1),
                LocalDate.of(2024, 12, 1), // 6 months
                MarriageEndReason.SPOUSE_DIED
            );

            assertFalse(marriage.qualifiesForSurvivorBenefits());
        }

        @Test
        @DisplayName("does not qualify if divorced (not widowed)")
        void doesNotQualifyIfDivorced() {
            PastMarriage marriage = PastMarriage.withBenefit(
                new BigDecimal("2500"),
                LocalDate.of(2000, 1, 1),
                LocalDate.of(2015, 1, 1),
                MarriageEndReason.DIVORCED
            );

            assertFalse(marriage.qualifiesForSurvivorBenefits());
        }
    }
}
