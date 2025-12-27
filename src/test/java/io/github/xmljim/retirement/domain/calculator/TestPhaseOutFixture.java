package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import io.github.xmljim.retirement.domain.config.IraPhaseOutLimits;
import io.github.xmljim.retirement.domain.config.IraPhaseOutLimits.PhaseOutRange;
import io.github.xmljim.retirement.domain.config.IraPhaseOutLimits.YearPhaseOuts;
import io.github.xmljim.retirement.domain.config.IrsContributionLimits;
import io.github.xmljim.retirement.domain.config.IrsContributionLimits.IraLimits;

/**
 * Shared test fixtures for phase-out calculator tests.
 *
 * <p>Provides consistent test data across unit and integration tests
 * for IRA phase-out calculations.
 */
public final class TestPhaseOutFixture {

    private TestPhaseOutFixture() {
        // Utility class
    }

    /**
     * Creates test phase-out limits with 2025 thresholds.
     *
     * @return configured IraPhaseOutLimits
     */
    public static IraPhaseOutLimits createTestPhaseOutLimits() {
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

    /**
     * Creates test IRA contribution limits with 2025 values.
     *
     * @return configured IrsContributionLimits
     */
    public static IrsContributionLimits createTestContributionLimits() {
        IrsContributionLimits limits = new IrsContributionLimits();
        Map<Integer, IraLimits> iraLimits = new HashMap<>();
        iraLimits.put(2025, new IraLimits(new BigDecimal("7000"), new BigDecimal("1000")));
        limits.setIraLimits(iraLimits);
        return limits;
    }
}
