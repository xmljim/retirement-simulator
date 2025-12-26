package io.github.xmljim.retirement.domain.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.config.IraPhaseOutLimits.PhaseOutRange;
import io.github.xmljim.retirement.domain.config.IraPhaseOutLimits.YearPhaseOuts;
import io.github.xmljim.retirement.domain.enums.FilingStatus;

@DisplayName("IraPhaseOutLimits Tests")
class IraPhaseOutLimitsTest {

    private IraPhaseOutLimits limits;

    @BeforeEach
    void setUp() {
        limits = new IraPhaseOutLimits();
        limits.getRothIra().put(2024, new YearPhaseOuts(
            new PhaseOutRange(new BigDecimal("146000"), new BigDecimal("161000")),
            new PhaseOutRange(new BigDecimal("230000"), new BigDecimal("240000")),
            new PhaseOutRange(BigDecimal.ZERO, new BigDecimal("10000"))
        ));
        limits.getRothIra().put(2025, new YearPhaseOuts(
            new PhaseOutRange(new BigDecimal("150000"), new BigDecimal("165000")),
            new PhaseOutRange(new BigDecimal("236000"), new BigDecimal("246000")),
            new PhaseOutRange(BigDecimal.ZERO, new BigDecimal("10000"))
        ));
    }

    @Nested
    @DisplayName("Roth IRA Phase-Out Tests")
    class RothIraTests {

        @Test
        @DisplayName("Should return 2024 Roth IRA limits for single")
        void returns2024SingleLimits() {
            var phaseOuts = limits.getRothIraPhaseOutsForYear(2024);
            var range = phaseOuts.forStatus(FilingStatus.SINGLE);

            assertEquals(0, new BigDecimal("146000").compareTo(range.lowerBound()));
            assertEquals(0, new BigDecimal("161000").compareTo(range.upperBound()));
        }

        @Test
        @DisplayName("Should return 2025 Roth IRA limits for MFJ")
        void returns2025MfjLimits() {
            var phaseOuts = limits.getRothIraPhaseOutsForYear(2025);
            var range = phaseOuts.forStatus(FilingStatus.MARRIED_FILING_JOINTLY);

            assertEquals(0, new BigDecimal("236000").compareTo(range.lowerBound()));
            assertEquals(0, new BigDecimal("246000").compareTo(range.upperBound()));
        }

        @Test
        @DisplayName("Should return MFS limits with restricted range")
        void returnsMfsRestrictedLimits() {
            var phaseOuts = limits.getRothIraPhaseOutsForYear(2024);
            var range = phaseOuts.forStatus(FilingStatus.MARRIED_FILING_SEPARATELY);

            assertEquals(0, BigDecimal.ZERO.compareTo(range.lowerBound()));
            assertEquals(0, new BigDecimal("10000").compareTo(range.upperBound()));
        }

        @Test
        @DisplayName("HEAD_OF_HOUSEHOLD uses single thresholds")
        void hohUsesSingleThresholds() {
            var phaseOuts = limits.getRothIraPhaseOutsForYear(2024);
            var hohRange = phaseOuts.forStatus(FilingStatus.HEAD_OF_HOUSEHOLD);
            var singleRange = phaseOuts.forStatus(FilingStatus.SINGLE);

            assertEquals(singleRange, hohRange);
        }
    }

    @Nested
    @DisplayName("Extrapolation Tests")
    class ExtrapolationTests {

        @Test
        @DisplayName("Should extrapolate future year limits")
        void extrapolatesFutureYear() {
            var phaseOuts = limits.getRothIraPhaseOutsForYear(2026);
            assertNotNull(phaseOuts);

            // 2025 single: $150,000 * 1.02 = $153,000 (rounded to $1000)
            var range = phaseOuts.forStatus(FilingStatus.SINGLE);
            assertEquals(0, new BigDecimal("153000").compareTo(range.lowerBound()));
        }

        @Test
        @DisplayName("Should return empty phase-outs for empty config")
        void returnsEmptyForEmptyConfig() {
            IraPhaseOutLimits empty = new IraPhaseOutLimits();
            var phaseOuts = empty.getRothIraPhaseOutsForYear(2024);

            assertEquals(PhaseOutRange.ZERO, phaseOuts.forStatus(FilingStatus.SINGLE));
        }
    }

    @Nested
    @DisplayName("PhaseOutRange Tests")
    class PhaseOutRangeTests {

        @Test
        @DisplayName("Should calculate range correctly")
        void calculatesRange() {
            var range = new PhaseOutRange(new BigDecimal("146000"), new BigDecimal("161000"));
            assertEquals(0, new BigDecimal("15000").compareTo(range.getRange()));
        }

        @Test
        @DisplayName("Should handle null values")
        void handlesNullValues() {
            var range = new PhaseOutRange(null, null);
            assertEquals(BigDecimal.ZERO, range.lowerBound());
            assertEquals(BigDecimal.ZERO, range.upperBound());
        }
    }
}
