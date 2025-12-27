package io.github.xmljim.retirement.domain.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.calculator.impl.DefaultPhaseOutCalculator;
import io.github.xmljim.retirement.domain.config.IraPhaseOutLimits;
import io.github.xmljim.retirement.domain.config.IraPhaseOutLimits.PhaseOutRange;
import io.github.xmljim.retirement.domain.config.IraPhaseOutLimits.YearPhaseOuts;
import io.github.xmljim.retirement.domain.config.IrsContributionLimits;
import io.github.xmljim.retirement.domain.config.IrsContributionLimits.IraLimits;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.enums.FilingStatus;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;

@DisplayName("PhaseOutCalculator Tests")
class PhaseOutCalculatorTest {

    private PhaseOutCalculator calculator;
    private IraPhaseOutLimits phaseOutLimits;
    private IrsContributionLimits contributionLimits;

    @BeforeEach
    void setUp() {
        phaseOutLimits = createTestPhaseOutLimits();
        contributionLimits = createTestContributionLimits();
        calculator = new DefaultPhaseOutCalculator(phaseOutLimits, contributionLimits);
    }

    private IraPhaseOutLimits createTestPhaseOutLimits() {
        IraPhaseOutLimits limits = new IraPhaseOutLimits();

        // Roth IRA 2025 thresholds
        Map<Integer, YearPhaseOuts> rothIra = new HashMap<>();
        rothIra.put(2025, new YearPhaseOuts(
            new PhaseOutRange(new BigDecimal("150000"), new BigDecimal("165000")),
            new PhaseOutRange(new BigDecimal("236000"), new BigDecimal("246000")),
            new PhaseOutRange(BigDecimal.ZERO, new BigDecimal("10000"))
        ));
        limits.setRothIra(rothIra);

        // Traditional IRA covered 2025 thresholds
        Map<Integer, YearPhaseOuts> tradCovered = new HashMap<>();
        tradCovered.put(2025, new YearPhaseOuts(
            new PhaseOutRange(new BigDecimal("79000"), new BigDecimal("89000")),
            new PhaseOutRange(new BigDecimal("126000"), new BigDecimal("146000")),
            new PhaseOutRange(BigDecimal.ZERO, new BigDecimal("10000"))
        ));
        limits.setTraditionalIraCovered(tradCovered);

        // Traditional IRA spouse covered 2025 thresholds
        Map<Integer, YearPhaseOuts> spouseCovered = new HashMap<>();
        spouseCovered.put(2025, new YearPhaseOuts(
            PhaseOutRange.ZERO,
            new PhaseOutRange(new BigDecimal("236000"), new BigDecimal("246000")),
            PhaseOutRange.ZERO
        ));
        limits.setTraditionalIraSpouseCovered(spouseCovered);

        return limits;
    }

    private IrsContributionLimits createTestContributionLimits() {
        IrsContributionLimits limits = new IrsContributionLimits();
        Map<Integer, IraLimits> iraLimits = new HashMap<>();
        iraLimits.put(2025, new IraLimits(new BigDecimal("7000"), new BigDecimal("1000")));
        limits.setIraLimits(iraLimits);
        return limits;
    }

    @Nested
    @DisplayName("Roth IRA Phase-Out Tests")
    class RothIraPhaseOutTests {

        @Test
        @DisplayName("Below phase-out: full contribution allowed")
        void belowPhaseOutFullAllowed() {
            PhaseOutResult result = calculator.calculateRothIraPhaseOut(
                new BigDecimal("100000"), FilingStatus.SINGLE, 2025, new BigDecimal("7000"), 45);

            assertEquals(AccountType.ROTH_IRA, result.accountType());
            assertEquals(0, new BigDecimal("7000").compareTo(result.allowedContribution()));
            assertEquals(0, BigDecimal.ZERO.compareTo(result.phaseOutPercentage()));
            assertFalse(result.isFullyPhasedOut());
            assertFalse(result.backdoorRothEligible());
        }

        @Test
        @DisplayName("Above phase-out: no contribution, backdoor eligible")
        void abovePhaseOutNoContribution() {
            PhaseOutResult result = calculator.calculateRothIraPhaseOut(
                new BigDecimal("170000"), FilingStatus.SINGLE, 2025, new BigDecimal("7000"), 45);

            assertEquals(0, BigDecimal.ZERO.compareTo(result.allowedContribution()));
            assertEquals(0, BigDecimal.ONE.compareTo(result.phaseOutPercentage()));
            assertTrue(result.isFullyPhasedOut());
            assertTrue(result.backdoorRothEligible());
            assertTrue(result.warnings().stream().anyMatch(w -> w.contains("backdoor")));
        }

        @Test
        @DisplayName("Mid phase-out: partial contribution")
        void midPhaseOutPartialContribution() {
            // MAGI at midpoint of $150K-$165K range = $157,500 = 50% phased out
            PhaseOutResult result = calculator.calculateRothIraPhaseOut(
                new BigDecimal("157500"), FilingStatus.SINGLE, 2025, new BigDecimal("7000"), 45);

            assertFalse(result.isFullyPhasedOut());
            assertTrue(result.allowedContribution().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(result.allowedContribution().compareTo(new BigDecimal("7000")) < 0);
        }

        @Test
        @DisplayName("MFJ uses joint thresholds")
        void mfjUsesJointThresholds() {
            // $200K is below MFJ lower bound of $236K
            PhaseOutResult result = calculator.calculateRothIraPhaseOut(
                new BigDecimal("200000"), FilingStatus.MARRIED_FILING_JOINTLY, 2025,
                new BigDecimal("7000"), 45);

            assertEquals(0, new BigDecimal("7000").compareTo(result.allowedContribution()));
            assertFalse(result.isFullyPhasedOut());
        }

        @Test
        @DisplayName("Age 50+ gets catch-up contribution")
        void age50GetsCatchUp() {
            BigDecimal maxLimit = calculator.getMaxIraContribution(2025, 55);
            assertEquals(0, new BigDecimal("8000").compareTo(maxLimit));
        }
    }

    @Nested
    @DisplayName("Traditional IRA Phase-Out Tests")
    class TraditionalIraPhaseOutTests {

        @Test
        @DisplayName("Not covered: full deduction regardless of income")
        void notCoveredFullDeduction() {
            PhaseOutResult result = calculator.calculateTraditionalIraPhaseOut(
                new BigDecimal("500000"), FilingStatus.SINGLE, 2025,
                new BigDecimal("7000"), 45, false, false);

            assertEquals(AccountType.TRADITIONAL_IRA, result.accountType());
            assertEquals(0, new BigDecimal("7000").compareTo(result.allowedContribution()));
            assertEquals(0, new BigDecimal("7000").compareTo(result.deductiblePortion()));
        }

        @Test
        @DisplayName("Covered below phase-out: full deduction")
        void coveredBelowPhaseOut() {
            PhaseOutResult result = calculator.calculateTraditionalIraPhaseOut(
                new BigDecimal("70000"), FilingStatus.SINGLE, 2025,
                new BigDecimal("7000"), 45, true, false);

            assertEquals(0, new BigDecimal("7000").compareTo(result.deductiblePortion()));
            assertFalse(result.isFullyPhasedOut());
        }

        @Test
        @DisplayName("Covered above phase-out: no deduction")
        void coveredAbovePhaseOut() {
            PhaseOutResult result = calculator.calculateTraditionalIraPhaseOut(
                new BigDecimal("100000"), FilingStatus.SINGLE, 2025,
                new BigDecimal("7000"), 45, true, false);

            assertEquals(0, BigDecimal.ZERO.compareTo(result.deductiblePortion()));
            assertTrue(result.isFullyPhasedOut());
        }

        @Test
        @DisplayName("Spouse covered uses higher thresholds")
        void spouseCoveredHigherThresholds() {
            // $200K is below spouse-covered MFJ threshold of $236K
            PhaseOutResult result = calculator.calculateTraditionalIraPhaseOut(
                new BigDecimal("200000"), FilingStatus.MARRIED_FILING_JOINTLY, 2025,
                new BigDecimal("7000"), 45, false, true);

            assertEquals(0, new BigDecimal("7000").compareTo(result.deductiblePortion()));
            assertFalse(result.isFullyPhasedOut());
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Throws for null MAGI")
        void throwsForNullMagi() {
            assertThrows(MissingRequiredFieldException.class, () ->
                calculator.calculateRothIraPhaseOut(null, FilingStatus.SINGLE, 2025,
                    new BigDecimal("7000"), 45));
        }

        @Test
        @DisplayName("Throws for null filing status")
        void throwsForNullFilingStatus() {
            assertThrows(MissingRequiredFieldException.class, () ->
                calculator.calculateRothIraPhaseOut(new BigDecimal("100000"), null, 2025,
                    new BigDecimal("7000"), 45));
        }
    }

    @Nested
    @DisplayName("PhaseOutResult Tests")
    class PhaseOutResultTests {

        @Test
        @DisplayName("Builder creates valid result")
        void builderCreatesValidResult() {
            PhaseOutResult result = PhaseOutResult.builder()
                .accountType(AccountType.ROTH_IRA)
                .requestedContribution(new BigDecimal("7000"))
                .allowedContribution(new BigDecimal("3500"))
                .phaseOutPercentage(new BigDecimal("0.5"))
                .isFullyPhasedOut(false)
                .backdoorRothEligible(false)
                .addWarning("Test warning")
                .build();

            assertNotNull(result);
            assertEquals(AccountType.ROTH_IRA, result.accountType());
            assertFalse(result.isFullContributionAllowed());
            assertTrue(result.canContribute());
        }
    }

    @Nested
    @DisplayName("Factory Tests")
    class FactoryTests {

        @Test
        @DisplayName("Factory creates calculator")
        void factoryCreatesCalculator() {
            PhaseOutCalculator calc = CalculatorFactory.phaseOutCalculator(
                phaseOutLimits, contributionLimits);
            assertNotNull(calc);
        }
    }
}
