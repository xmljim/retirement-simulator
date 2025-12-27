package io.github.xmljim.retirement.domain.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.calculator.impl.DefaultSpousalBenefitCalculator;
import io.github.xmljim.retirement.domain.value.CoupleClaimingStrategy;
import io.github.xmljim.retirement.domain.value.MarriageInfo;
import io.github.xmljim.retirement.domain.value.SocialSecurityBenefit;
import io.github.xmljim.retirement.domain.value.SpousalBenefitResult;
import io.github.xmljim.retirement.domain.value.SurvivorBenefitResult;

@DisplayName("SpousalBenefitCalculator")
class SpousalBenefitCalculatorTest {

    private SpousalBenefitCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new DefaultSpousalBenefitCalculator();
    }

    private SocialSecurityBenefit createBenefit(double fraBenefit, int birthYear, int claimingAge) {
        return SocialSecurityBenefit.builder()
            .fraBenefit(fraBenefit)
            .birthYear(birthYear)
            .claimingAgeMonths(claimingAge)
            .startDate(LocalDate.of(2025, 1, 1))
            .build();
    }

    @Nested
    @DisplayName("Spousal Benefits")
    class SpousalBenefitTests {

        @Test
        @DisplayName("calculates 50% of spouse FRA benefit")
        void calculatesFiftyPercent() {
            SocialSecurityBenefit own = createBenefit(1000, 1960, 804); // $1000 at FRA
            SocialSecurityBenefit spouse = createBenefit(3000, 1958, 800); // $3000 at FRA
            MarriageInfo marriage = MarriageInfo.married(LocalDate.of(2010, 1, 1));

            SpousalBenefitResult result = calculator.calculateSpousalBenefit(
                own, spouse, marriage, 804); // claiming at FRA

            assertTrue(result.eligibleForSpousal());
            // Spousal = 50% of $3000 = $1500
            assertTrue(result.spousalBenefit().compareTo(new BigDecimal("1500")) >= 0);
            assertTrue(result.isSpousalBenefitHigher());
            assertEquals(SpousalBenefitResult.BenefitType.SPOUSAL, result.recommendedType());
        }

        @Test
        @DisplayName("recommends own benefit when higher than spousal")
        void recommendsOwnWhenHigher() {
            SocialSecurityBenefit own = createBenefit(2000, 1960, 804); // $2000 at FRA
            SocialSecurityBenefit spouse = createBenefit(3000, 1958, 800); // $3000 FRA -> $1500 spousal
            MarriageInfo marriage = MarriageInfo.married(LocalDate.of(2010, 1, 1));

            SpousalBenefitResult result = calculator.calculateSpousalBenefit(
                own, spouse, marriage, 804);

            assertTrue(result.eligibleForSpousal());
            assertFalse(result.isSpousalBenefitHigher());
            assertEquals(SpousalBenefitResult.BenefitType.OWN, result.recommendedType());
        }

        @Test
        @DisplayName("ineligible if married less than 1 year")
        void ineligibleIfMarriageTooShort() {
            SocialSecurityBenefit own = createBenefit(1000, 1960, 804);
            SocialSecurityBenefit spouse = createBenefit(3000, 1958, 800);
            MarriageInfo marriage = MarriageInfo.married(LocalDate.now().minusMonths(6));

            SpousalBenefitResult result = calculator.calculateSpousalBenefit(
                own, spouse, marriage, 804);

            assertFalse(result.eligibleForSpousal());
            assertTrue(result.ineligibilityReason().isPresent());
        }

        @Test
        @DisplayName("ineligible if not married")
        void ineligibleIfNotMarried() {
            SocialSecurityBenefit own = createBenefit(1000, 1960, 804);
            SocialSecurityBenefit spouse = createBenefit(3000, 1958, 800);
            MarriageInfo marriage = MarriageInfo.single();

            SpousalBenefitResult result = calculator.calculateSpousalBenefit(
                own, spouse, marriage, 804);

            assertFalse(result.eligibleForSpousal());
        }
    }

    @Nested
    @DisplayName("Survivor Benefits")
    class SurvivorBenefitTests {

        @Test
        @DisplayName("calculates full benefit at FRA")
        void calculatesFullBenefitAtFra() {
            SocialSecurityBenefit survivor = createBenefit(1500, 1960, 804);
            SocialSecurityBenefit deceased = createBenefit(2500, 1958, 800);
            MarriageInfo marriage = MarriageInfo.widowed(
                LocalDate.of(2000, 1, 1), LocalDate.of(2024, 1, 1));

            SurvivorBenefitResult result = calculator.calculateSurvivorBenefit(
                survivor, deceased, marriage, 804); // survivor at FRA

            assertTrue(result.eligibleForSurvivor());
            assertFalse(result.reductionApplied());
            assertTrue(result.isSurvivorBenefitHigher());
        }

        @Test
        @DisplayName("applies reduction for early claiming at 60")
        void appliesReductionAtSixty() {
            // Survivor's own benefit at 62 (their FRA benefit is used for comparison)
            SocialSecurityBenefit survivor = createBenefit(1000, 1960, 744);
            SocialSecurityBenefit deceased = createBenefit(2500, 1958, 800);
            MarriageInfo marriage = MarriageInfo.widowed(
                LocalDate.of(2000, 1, 1), LocalDate.of(2024, 1, 1));

            // Survivor can claim survivor benefit at 60 (720 months) even though
            // regular benefits require 62
            SurvivorBenefitResult result = calculator.calculateSurvivorBenefit(
                survivor, deceased, marriage, 720); // claiming survivor at 60

            assertTrue(result.eligibleForSurvivor());
            assertTrue(result.reductionApplied());
            assertTrue(result.reductionPercentage().compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @DisplayName("ineligible if not widowed")
        void ineligibleIfNotWidowed() {
            SocialSecurityBenefit survivor = createBenefit(1500, 1960, 804);
            SocialSecurityBenefit deceased = createBenefit(2500, 1958, 800);
            MarriageInfo marriage = MarriageInfo.married(LocalDate.of(2000, 1, 1));

            SurvivorBenefitResult result = calculator.calculateSurvivorBenefit(
                survivor, deceased, marriage, 804);

            assertFalse(result.eligibleForSurvivor());
        }

        @Test
        @DisplayName("ineligible if married less than 9 months")
        void ineligibleIfMarriageTooShort() {
            SocialSecurityBenefit survivor = createBenefit(1500, 1960, 804);
            SocialSecurityBenefit deceased = createBenefit(2500, 1958, 800);
            MarriageInfo marriage = MarriageInfo.widowed(
                LocalDate.of(2024, 6, 1), LocalDate.of(2024, 12, 1));

            SurvivorBenefitResult result = calculator.calculateSurvivorBenefit(
                survivor, deceased, marriage, 804);

            assertFalse(result.eligibleForSurvivor());
        }
    }

    @Nested
    @DisplayName("Claiming Strategy Optimization")
    class OptimizationTests {

        @Test
        @DisplayName("identifies higher and lower earner correctly")
        void identifiesEarners() {
            SocialSecurityBenefit person1 = createBenefit(2000, 1960, 804);
            SocialSecurityBenefit person2 = createBenefit(3000, 1958, 800);
            MarriageInfo marriage = MarriageInfo.married(LocalDate.of(2010, 1, 1));

            CoupleClaimingStrategy strategy = calculator.optimizeClaimingStrategy(
                person1, person2, marriage);

            // Higher earner ($3000) should delay to 70
            assertEquals(840, strategy.higherEarnerClaimingAgeMonths());
            // Lower earner ($2000) should claim at 62
            assertEquals(744, strategy.lowerEarnerClaimingAgeMonths());
        }

        @Test
        @DisplayName("provides alternative strategies")
        void providesAlternatives() {
            SocialSecurityBenefit person1 = createBenefit(2000, 1960, 804);
            SocialSecurityBenefit person2 = createBenefit(3000, 1958, 800);
            MarriageInfo marriage = MarriageInfo.married(LocalDate.of(2010, 1, 1));

            CoupleClaimingStrategy strategy = calculator.optimizeClaimingStrategy(
                person1, person2, marriage);

            assertFalse(strategy.alternativeStrategies().isEmpty());
        }

        @Test
        @DisplayName("calculates combined benefit")
        void calculatesCombinedBenefit() {
            SocialSecurityBenefit person1 = createBenefit(2000, 1960, 804);
            SocialSecurityBenefit person2 = createBenefit(3000, 1958, 800);
            MarriageInfo marriage = MarriageInfo.married(LocalDate.of(2010, 1, 1));

            CoupleClaimingStrategy strategy = calculator.optimizeClaimingStrategy(
                person1, person2, marriage);

            BigDecimal combined = strategy.higherEarnerMonthlyBenefit()
                .add(strategy.lowerEarnerMonthlyBenefit());
            assertEquals(0, combined.compareTo(strategy.combinedMonthlyBenefit()));
        }
    }

    @Nested
    @DisplayName("Factory Method")
    class FactoryTests {

        @Test
        @DisplayName("CalculatorFactory creates valid instance")
        void factoryCreatesInstance() {
            SpousalBenefitCalculator calc = CalculatorFactory.spousalBenefitCalculator();
            assertNotNull(calc);
        }
    }
}
