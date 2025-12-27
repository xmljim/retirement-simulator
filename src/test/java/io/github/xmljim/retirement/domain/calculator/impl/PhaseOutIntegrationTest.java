package io.github.xmljim.retirement.domain.calculator.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.calculator.CalculatorFactory;
import io.github.xmljim.retirement.domain.calculator.MAGICalculator;
import io.github.xmljim.retirement.domain.calculator.PhaseOutCalculator;
import io.github.xmljim.retirement.domain.calculator.PhaseOutResult;
import io.github.xmljim.retirement.domain.calculator.TestPhaseOutFixture;
import io.github.xmljim.retirement.domain.config.IraPhaseOutLimits;
import io.github.xmljim.retirement.domain.config.IrsContributionLimits;
import io.github.xmljim.retirement.domain.enums.FilingStatus;
import io.github.xmljim.retirement.domain.value.IncomeDetails;

/**
 * Integration tests for M3b phase-out flow: MAGI calculation to phase-out determination.
 */
@DisplayName("Phase-Out Integration Tests")
class PhaseOutIntegrationTest {

    private MAGICalculator magiCalculator;
    private PhaseOutCalculator phaseOutCalculator;

    @BeforeEach
    void setUp() {
        magiCalculator = CalculatorFactory.magiCalculator();
        IraPhaseOutLimits phaseOutLimits = TestPhaseOutFixture.createTestPhaseOutLimits();
        IrsContributionLimits contributionLimits = TestPhaseOutFixture.createTestContributionLimits();
        phaseOutCalculator = CalculatorFactory.phaseOutCalculator(phaseOutLimits, contributionLimits);
    }

    @Test
    @DisplayName("High earner with add-backs gets phased out of Roth")
    void highEarnerWithAddBacksPhasedOut() {
        IncomeDetails income = IncomeDetails.builder()
            .adjustedGrossIncome(new BigDecimal("140000"))
            .studentLoanInterest(new BigDecimal("15000"))
            .build();

        BigDecimal magi = magiCalculator.calculate(income);
        assertEquals(0, new BigDecimal("155000").compareTo(magi));

        PhaseOutResult result = phaseOutCalculator.calculateRothIraPhaseOut(
            magi, FilingStatus.SINGLE, 2025, new BigDecimal("7000"), 45);

        assertFalse(result.isFullyPhasedOut());
        assertTrue(result.canContribute());
        assertTrue(result.allowedContribution().compareTo(new BigDecimal("4000")) > 0);
        assertTrue(result.allowedContribution().compareTo(new BigDecimal("5000")) < 0);
    }

    @Test
    @DisplayName("Couple with foreign income exclusion exceeds Roth limit")
    void coupleWithForeignIncomeExceedRoth() {
        IncomeDetails income = IncomeDetails.builder()
            .adjustedGrossIncome(new BigDecimal("200000"))
            .foreignEarnedIncomeExclusion(new BigDecimal("50000"))
            .build();

        BigDecimal magi = magiCalculator.calculate(income);
        assertEquals(0, new BigDecimal("250000").compareTo(magi));

        PhaseOutResult result = phaseOutCalculator.calculateRothIraPhaseOut(
            magi, FilingStatus.MARRIED_FILING_JOINTLY, 2025, new BigDecimal("7000"), 45);

        assertTrue(result.isFullyPhasedOut());
        assertFalse(result.canContribute());
        assertTrue(result.backdoorRothEligible());
    }

    @Test
    @DisplayName("AGI without add-backs uses simple MAGI")
    void agiWithoutAddBacksSimpleMagi() {
        IncomeDetails income = IncomeDetails.ofAgi(new BigDecimal("100000"));

        BigDecimal magi = magiCalculator.calculate(income);
        assertEquals(0, new BigDecimal("100000").compareTo(magi));

        PhaseOutResult result = phaseOutCalculator.calculateRothIraPhaseOut(
            magi, FilingStatus.SINGLE, 2025, new BigDecimal("7000"), 45);

        assertTrue(result.isFullContributionAllowed());
        assertEquals(0, new BigDecimal("7000").compareTo(result.allowedContribution()));
    }

    @Test
    @DisplayName("MFS has restricted phase-out starting at $0")
    void mfsRestrictedPhaseOut() {
        BigDecimal magi = new BigDecimal("5000");

        PhaseOutResult result = phaseOutCalculator.calculateRothIraPhaseOut(
            magi, FilingStatus.MARRIED_FILING_SEPARATELY, 2025, new BigDecimal("7000"), 45);

        assertFalse(result.isFullyPhasedOut());
        assertTrue(result.allowedContribution().compareTo(new BigDecimal("3000")) > 0);
        assertTrue(result.allowedContribution().compareTo(new BigDecimal("4000")) < 0);
    }

    @Test
    @DisplayName("Head of Household uses single thresholds")
    void hohUsesSingleThresholds() {
        BigDecimal magi = new BigDecimal("155000");

        PhaseOutResult singleResult = phaseOutCalculator.calculateRothIraPhaseOut(
            magi, FilingStatus.SINGLE, 2025, new BigDecimal("7000"), 45);

        PhaseOutResult hohResult = phaseOutCalculator.calculateRothIraPhaseOut(
            magi, FilingStatus.HEAD_OF_HOUSEHOLD, 2025, new BigDecimal("7000"), 45);

        assertEquals(0, singleResult.allowedContribution().compareTo(hohResult.allowedContribution()));
    }

    @Test
    @DisplayName("QSS uses MFJ thresholds")
    void qssUsesMfjThresholds() {
        BigDecimal magi = new BigDecimal("240000");

        PhaseOutResult mfjResult = phaseOutCalculator.calculateRothIraPhaseOut(
            magi, FilingStatus.MARRIED_FILING_JOINTLY, 2025, new BigDecimal("7000"), 45);

        PhaseOutResult qssResult = phaseOutCalculator.calculateRothIraPhaseOut(
            magi, FilingStatus.QUALIFYING_SURVIVING_SPOUSE, 2025, new BigDecimal("7000"), 45);

        assertEquals(0, mfjResult.allowedContribution().compareTo(qssResult.allowedContribution()));
    }

    @Test
    @DisplayName("Not covered by plan: full deduction at any income")
    void notCoveredFullDeduction() {
        IncomeDetails income = IncomeDetails.ofAgi(new BigDecimal("500000"));
        BigDecimal magi = magiCalculator.calculate(income);

        PhaseOutResult result = phaseOutCalculator.calculateTraditionalIraPhaseOut(
            magi, FilingStatus.SINGLE, 2025, new BigDecimal("7000"), 45, false, false);

        assertEquals(0, new BigDecimal("7000").compareTo(result.deductiblePortion()));
        assertFalse(result.isFullyPhasedOut());
    }

    @Test
    @DisplayName("Covered and above phase-out: contribution allowed but not deductible")
    void coveredAbovePhaseOut() {
        IncomeDetails income = IncomeDetails.ofAgi(new BigDecimal("100000"));
        BigDecimal magi = magiCalculator.calculate(income);

        PhaseOutResult result = phaseOutCalculator.calculateTraditionalIraPhaseOut(
            magi, FilingStatus.SINGLE, 2025, new BigDecimal("7000"), 45, true, false);

        assertEquals(0, BigDecimal.ZERO.compareTo(result.deductiblePortion()));
        assertTrue(result.isFullyPhasedOut());
    }

    @Test
    @DisplayName("Spouse covered uses higher threshold")
    void spouseCoveredHigherThreshold() {
        BigDecimal magi = new BigDecimal("200000");

        PhaseOutResult result = phaseOutCalculator.calculateTraditionalIraPhaseOut(
            magi, FilingStatus.MARRIED_FILING_JOINTLY, 2025, new BigDecimal("7000"), 45,
            false, true);

        assertEquals(0, new BigDecimal("7000").compareTo(result.deductiblePortion()));
        assertFalse(result.isFullyPhasedOut());
    }

    @Test
    @DisplayName("Age 50+ gets $8,000 limit")
    void age50GetsCatchUp() {
        BigDecimal maxLimit = phaseOutCalculator.getMaxIraContribution(2025, 55);
        assertEquals(0, new BigDecimal("8000").compareTo(maxLimit));
    }

    @Test
    @DisplayName("Age 49 gets $7,000 limit")
    void age49NoCatchUp() {
        BigDecimal maxLimit = phaseOutCalculator.getMaxIraContribution(2025, 49);
        assertEquals(0, new BigDecimal("7000").compareTo(maxLimit));
    }

    @Test
    @DisplayName("Partial phase-out respects catch-up limit")
    void partialPhaseOutWithCatchUp() {
        BigDecimal magi = new BigDecimal("157500");

        PhaseOutResult result = phaseOutCalculator.calculateRothIraPhaseOut(
            magi, FilingStatus.SINGLE, 2025, new BigDecimal("8000"), 55);

        assertTrue(result.allowedContribution().compareTo(new BigDecimal("3500")) > 0);
        assertTrue(result.allowedContribution().compareTo(new BigDecimal("4500")) < 0);
    }
}
