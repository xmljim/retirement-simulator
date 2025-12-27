package io.github.xmljim.retirement.domain.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.MaritalStatus;
import io.github.xmljim.retirement.domain.enums.MarriageEndReason;

@DisplayName("MarriageInfo")
class MarriageInfoTest {

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodTests {

        @Test
        @DisplayName("single() creates SINGLE status with no dates")
        void singleCreatesCorrectly() {
            MarriageInfo info = MarriageInfo.single();

            assertEquals(MaritalStatus.SINGLE, info.status());
            assertTrue(info.marriageDate().isEmpty());
            assertTrue(info.divorceDate().isEmpty());
            assertTrue(info.spouseDeathDate().isEmpty());
            assertFalse(info.livingWithSpouse());
        }

        @Test
        @DisplayName("married() creates MARRIED status with date")
        void marriedCreatesCorrectly() {
            LocalDate weddingDate = LocalDate.of(2015, 6, 20);
            MarriageInfo info = MarriageInfo.married(weddingDate);

            assertEquals(MaritalStatus.MARRIED, info.status());
            assertTrue(info.marriageDate().isPresent());
            assertEquals(weddingDate, info.marriageDate().get());
            assertTrue(info.livingWithSpouse());
        }

        @Test
        @DisplayName("divorced() creates DIVORCED status with both dates")
        void divorcedCreatesCorrectly() {
            LocalDate weddingDate = LocalDate.of(2000, 3, 15);
            LocalDate divorceDate = LocalDate.of(2015, 9, 1);
            MarriageInfo info = MarriageInfo.divorced(weddingDate, divorceDate);

            assertEquals(MaritalStatus.DIVORCED, info.status());
            assertEquals(weddingDate, info.marriageDate().get());
            assertEquals(divorceDate, info.divorceDate().get());
            assertFalse(info.livingWithSpouse());
        }

        @Test
        @DisplayName("widowed() creates WIDOWED status with death date")
        void widowedCreatesCorrectly() {
            LocalDate weddingDate = LocalDate.of(1990, 8, 12);
            LocalDate deathDate = LocalDate.of(2023, 11, 5);
            MarriageInfo info = MarriageInfo.widowed(weddingDate, deathDate);

            assertEquals(MaritalStatus.WIDOWED, info.status());
            assertEquals(weddingDate, info.marriageDate().get());
            assertEquals(deathDate, info.spouseDeathDate().get());
        }
    }

    @Nested
    @DisplayName("Marriage Duration")
    class DurationTests {

        @Test
        @DisplayName("calculates duration for married couple")
        void calculatesMarriedDuration() {
            LocalDate weddingDate = LocalDate.of(2020, 1, 1);
            LocalDate asOf = LocalDate.of(2025, 1, 1);
            MarriageInfo info = MarriageInfo.married(weddingDate);

            assertEquals(60, info.getMarriageDurationMonths(asOf)); // 5 years
        }

        @Test
        @DisplayName("calculates duration for divorced couple")
        void calculatesDivorcedDuration() {
            LocalDate weddingDate = LocalDate.of(2000, 1, 1);
            LocalDate divorceDate = LocalDate.of(2012, 1, 1);
            MarriageInfo info = MarriageInfo.divorced(weddingDate, divorceDate);

            // Duration is from wedding to divorce
            assertEquals(144, info.getMarriageDurationMonths(LocalDate.of(2025, 1, 1)));
        }

        @Test
        @DisplayName("returns 0 for single person")
        void returnsZeroForSingle() {
            MarriageInfo info = MarriageInfo.single();
            assertEquals(0, info.getMarriageDurationMonths(LocalDate.now()));
        }
    }

    @Nested
    @DisplayName("Spousal Eligibility")
    class SpousalEligibilityTests {

        @Test
        @DisplayName("eligible if married 12+ months")
        void eligibleIfMarriedLongEnough() {
            LocalDate weddingDate = LocalDate.of(2023, 1, 1);
            LocalDate asOf = LocalDate.of(2025, 1, 1);
            MarriageInfo info = MarriageInfo.married(weddingDate);

            assertTrue(info.isEligibleForSpousalBenefits(asOf));
        }

        @Test
        @DisplayName("ineligible if married less than 12 months")
        void ineligibleIfMarriedTooShort() {
            LocalDate weddingDate = LocalDate.of(2024, 6, 1);
            LocalDate asOf = LocalDate.of(2025, 1, 1);
            MarriageInfo info = MarriageInfo.married(weddingDate);

            assertFalse(info.isEligibleForSpousalBenefits(asOf));
        }

        @Test
        @DisplayName("ineligible if single")
        void ineligibleIfSingle() {
            assertFalse(MarriageInfo.single().isEligibleForSpousalBenefits(LocalDate.now()));
        }
    }

    @Nested
    @DisplayName("Divorced Spouse Eligibility")
    class DivorcedSpouseEligibilityTests {

        @Test
        @DisplayName("eligible if married 10+ years and divorced 2+ years")
        void eligibleIfMeetsAllRequirements() {
            LocalDate weddingDate = LocalDate.of(2000, 1, 1);
            LocalDate divorceDate = LocalDate.of(2012, 1, 1);
            LocalDate asOf = LocalDate.of(2025, 1, 1);
            MarriageInfo info = MarriageInfo.divorced(weddingDate, divorceDate);

            assertTrue(info.isEligibleForDivorcedSpouseBenefits(asOf));
        }

        @Test
        @DisplayName("ineligible if married less than 10 years")
        void ineligibleIfMarriageTooShort() {
            LocalDate weddingDate = LocalDate.of(2010, 1, 1);
            LocalDate divorceDate = LocalDate.of(2018, 1, 1); // 8 years
            LocalDate asOf = LocalDate.of(2025, 1, 1);
            MarriageInfo info = MarriageInfo.divorced(weddingDate, divorceDate);

            assertFalse(info.isEligibleForDivorcedSpouseBenefits(asOf));
        }

        @Test
        @DisplayName("ineligible if divorced less than 2 years")
        void ineligibleIfDivorcedTooRecently() {
            LocalDate weddingDate = LocalDate.of(2000, 1, 1);
            LocalDate divorceDate = LocalDate.of(2024, 6, 1);
            LocalDate asOf = LocalDate.of(2025, 1, 1);
            MarriageInfo info = MarriageInfo.divorced(weddingDate, divorceDate);

            assertFalse(info.isEligibleForDivorcedSpouseBenefits(asOf));
        }
    }

    @Nested
    @DisplayName("Survivor Eligibility")
    class SurvivorEligibilityTests {

        @Test
        @DisplayName("eligible if widowed and married 9+ months")
        void eligibleIfMeetsRequirements() {
            LocalDate weddingDate = LocalDate.of(2020, 1, 1);
            LocalDate deathDate = LocalDate.of(2024, 1, 1);
            LocalDate asOf = LocalDate.of(2025, 1, 1);
            MarriageInfo info = MarriageInfo.widowed(weddingDate, deathDate);

            assertTrue(info.isEligibleForSurvivorBenefits(asOf));
        }

        @Test
        @DisplayName("ineligible if married less than 9 months")
        void ineligibleIfMarriageTooShort() {
            LocalDate weddingDate = LocalDate.of(2023, 6, 1);
            LocalDate deathDate = LocalDate.of(2024, 1, 1); // 7 months
            LocalDate asOf = LocalDate.of(2025, 1, 1);
            MarriageInfo info = MarriageInfo.widowed(weddingDate, deathDate);

            assertFalse(info.isEligibleForSurvivorBenefits(asOf));
        }

        @Test
        @DisplayName("ineligible if not widowed")
        void ineligibleIfNotWidowed() {
            assertFalse(MarriageInfo.single().isEligibleForSurvivorBenefits(LocalDate.now()));
            assertFalse(MarriageInfo.married(LocalDate.of(2020, 1, 1))
                .isEligibleForSurvivorBenefits(LocalDate.now()));
        }
    }

    @Nested
    @DisplayName("Marriage History")
    class MarriageHistoryTests {

        @Test
        @DisplayName("finds qualifying divorced spouse from history")
        void findsQualifyingDivorcedSpouse() {
            PastMarriage exSpouse = PastMarriage.withBenefit(
                new BigDecimal("3000"),
                LocalDate.of(2000, 1, 1),
                LocalDate.of(2012, 1, 1), // 12 years
                MarriageEndReason.DIVORCED
            );

            MarriageInfo info = MarriageInfo.married(
                LocalDate.of(2015, 1, 1),
                true,
                List.of(exSpouse)
            );

            LocalDate asOf = LocalDate.of(2025, 1, 1);
            assertTrue(info.hasQualifyingDivorcedSpouse(asOf));
            assertEquals(1, info.getQualifyingDivorcedSpouseMarriages(asOf).size());
        }

        @Test
        @DisplayName("finds best divorced spouse option by benefit amount")
        void findsBestDivorcedSpouseOption() {
            PastMarriage exSpouse1 = PastMarriage.withBenefit(
                new BigDecimal("2000"),
                LocalDate.of(1990, 1, 1),
                LocalDate.of(2005, 1, 1), // 15 years
                MarriageEndReason.DIVORCED
            );
            PastMarriage exSpouse2 = PastMarriage.withBenefit(
                new BigDecimal("4000"),
                LocalDate.of(2006, 1, 1),
                LocalDate.of(2020, 1, 1), // 14 years
                MarriageEndReason.DIVORCED
            );

            MarriageInfo info = MarriageInfo.divorced(
                LocalDate.of(2006, 1, 1),
                LocalDate.of(2020, 1, 1),
                List.of(exSpouse1) // exSpouse2 is the current divorce
            );

            LocalDate asOf = LocalDate.of(2025, 1, 1);
            var best = info.findBestDivorcedSpouseOption(asOf);
            assertTrue(best.isPresent());
            assertEquals(new BigDecimal("2000"), best.get().exSpouseFraBenefit().orElse(null));
        }

        @Test
        @DisplayName("empty history returns no qualifying marriages")
        void emptyHistoryReturnsNoQualifying() {
            MarriageInfo info = MarriageInfo.married(LocalDate.of(2020, 1, 1));

            LocalDate asOf = LocalDate.of(2025, 1, 1);
            assertFalse(info.hasQualifyingDivorcedSpouse(asOf));
            assertTrue(info.marriageHistory().isEmpty());
        }

        @Test
        @DisplayName("finds qualifying survivor from history")
        void findsQualifyingSurvivor() {
            PastMarriage deceasedSpouse = PastMarriage.withBenefit(
                new BigDecimal("3500"),
                LocalDate.of(2000, 1, 1),
                LocalDate.of(2022, 1, 1),
                MarriageEndReason.SPOUSE_DIED
            );

            MarriageInfo info = MarriageInfo.married(
                LocalDate.of(2023, 1, 1),
                true,
                List.of(deceasedSpouse)
            );

            assertEquals(1, info.getQualifyingSurvivorMarriages().size());
            var best = info.findBestSurvivorOption();
            assertTrue(best.isPresent());
            assertEquals(new BigDecimal("3500"), best.get().exSpouseFraBenefit().orElse(null));
        }
    }
}
