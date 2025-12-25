package io.github.xmljim.retirement.domain.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.config.IrsContributionLimits.HsaLimits;
import io.github.xmljim.retirement.domain.config.IrsContributionLimits.IraLimits;
import io.github.xmljim.retirement.domain.config.IrsContributionLimits.YearLimits;

@DisplayName("IrsContributionLimits Tests")
class IrsContributionLimitsTest {

    private IrsContributionLimits limits;

    @BeforeEach
    void setUp() {
        limits = new IrsContributionLimits();

        // Set up test data
        Map<Integer, YearLimits> yearLimits = new HashMap<>();
        yearLimits.put(2024, new YearLimits(
            new BigDecimal("23000"),
            new BigDecimal("7500"),
            BigDecimal.ZERO,
            new BigDecimal("145000")
        ));
        yearLimits.put(2025, new YearLimits(
            new BigDecimal("23500"),
            new BigDecimal("7500"),
            new BigDecimal("11250"),
            new BigDecimal("145000")
        ));
        yearLimits.put(2026, new YearLimits(
            new BigDecimal("24000"),
            new BigDecimal("7500"),
            new BigDecimal("11250"),
            new BigDecimal("145000")
        ));
        limits.setLimits(yearLimits);

        Map<Integer, IraLimits> iraLimits = new HashMap<>();
        iraLimits.put(2024, new IraLimits(
            new BigDecimal("7000"),
            new BigDecimal("1000")
        ));
        iraLimits.put(2025, new IraLimits(
            new BigDecimal("7000"),
            new BigDecimal("1000")
        ));
        limits.setIraLimits(iraLimits);

        Map<Integer, HsaLimits> hsaLimits = new HashMap<>();
        hsaLimits.put(2024, new HsaLimits(
            new BigDecimal("4150"),
            new BigDecimal("8300"),
            new BigDecimal("1000")
        ));
        hsaLimits.put(2025, new HsaLimits(
            new BigDecimal("4300"),
            new BigDecimal("8550"),
            new BigDecimal("1000")
        ));
        hsaLimits.put(2026, new HsaLimits(
            new BigDecimal("4400"),
            new BigDecimal("8750"),
            new BigDecimal("1000")
        ));
        limits.setHsaLimits(hsaLimits);

        limits.setDefaultAnnualIncreaseRate(new BigDecimal("0.02"));
    }

    @Nested
    @DisplayName("getLimitsForYear")
    class GetLimitsForYearTests {

        @Test
        @DisplayName("Should return exact limits for configured year")
        void returnsExactLimitsForConfiguredYear() {
            YearLimits result = limits.getLimitsForYear(2025);

            assertNotNull(result);
            assertEquals(0, new BigDecimal("23500").compareTo(result.baseLimit()));
            assertEquals(0, new BigDecimal("7500").compareTo(result.catchUpLimit()));
            assertEquals(0, new BigDecimal("11250").compareTo(result.superCatchUpLimit()));
            assertEquals(0, new BigDecimal("145000").compareTo(result.rothCatchUpIncomeThreshold()));
        }

        @Test
        @DisplayName("Should return 2024 limits with zero super catch-up")
        void returns2024LimitsWithZeroSuperCatchUp() {
            YearLimits result = limits.getLimitsForYear(2024);

            assertNotNull(result);
            assertEquals(0, new BigDecimal("23000").compareTo(result.baseLimit()));
            assertEquals(0, BigDecimal.ZERO.compareTo(result.superCatchUpLimit()));
        }

        @Test
        @DisplayName("Should extrapolate limits for future year with IRS-style rounding")
        void extrapolatesLimitsForFutureYear() {
            // 2030 is 4 years after 2026
            YearLimits result = limits.getLimitsForYear(2030);

            assertNotNull(result);
            // Base limit: 24000 * (1.02)^4 = 25,977.67 → rounds to $26,000
            assertEquals(0, new BigDecimal("26000").compareTo(result.baseLimit()));
            // Catch-up: 7500 * (1.02)^4 = 8118.24 → rounds to $8,000
            assertEquals(0, new BigDecimal("8000").compareTo(result.catchUpLimit()));
            // Income threshold: 145000 * (1.02)^4 = 156,952.66 → rounds to $155,000
            assertEquals(0, new BigDecimal("155000").compareTo(result.rothCatchUpIncomeThreshold()));
        }

        @Test
        @DisplayName("Should return earliest year for past year not in config")
        void returnsEarliestYearForPastYear() {
            YearLimits result = limits.getLimitsForYear(2020);

            assertNotNull(result);
            // Should return 2024 limits (earliest configured)
            assertEquals(0, new BigDecimal("23000").compareTo(result.baseLimit()));
        }

        @Test
        @DisplayName("Should return zero limits when no years configured")
        void returnsZeroLimitsWhenEmpty() {
            IrsContributionLimits emptyLimits = new IrsContributionLimits();
            emptyLimits.setLimits(new HashMap<>());

            YearLimits result = emptyLimits.getLimitsForYear(2025);

            assertNotNull(result);
            assertEquals(0, BigDecimal.ZERO.compareTo(result.baseLimit()));
        }
    }

    @Nested
    @DisplayName("getIraLimitsForYear")
    class GetIraLimitsForYearTests {

        @Test
        @DisplayName("Should return exact IRA limits for configured year")
        void returnsExactIraLimitsForConfiguredYear() {
            IraLimits result = limits.getIraLimitsForYear(2025);

            assertNotNull(result);
            assertEquals(0, new BigDecimal("7000").compareTo(result.baseLimit()));
            assertEquals(0, new BigDecimal("1000").compareTo(result.catchUpLimit()));
        }

        @Test
        @DisplayName("Should extrapolate IRA limits for future year")
        void extrapolatesIraLimitsForFutureYear() {
            IraLimits result = limits.getIraLimitsForYear(2030);

            assertNotNull(result);
            // Should be more than 2025 base limit of 7000
            assertTrue(result.baseLimit().compareTo(new BigDecimal("7000")) > 0);
        }
    }

    @Nested
    @DisplayName("getHsaLimitsForYear")
    class GetHsaLimitsForYearTests {

        @Test
        @DisplayName("Should return 2024 HSA limits")
        void returns2024HsaLimits() {
            HsaLimits result = limits.getHsaLimitsForYear(2024);

            assertNotNull(result);
            assertEquals(0, new BigDecimal("4150").compareTo(result.individualLimit()));
            assertEquals(0, new BigDecimal("8300").compareTo(result.familyLimit()));
            assertEquals(0, new BigDecimal("1000").compareTo(result.catchUpLimit()));
        }

        @Test
        @DisplayName("Should return 2025 HSA limits")
        void returns2025HsaLimits() {
            HsaLimits result = limits.getHsaLimitsForYear(2025);

            assertNotNull(result);
            assertEquals(0, new BigDecimal("4300").compareTo(result.individualLimit()));
            assertEquals(0, new BigDecimal("8550").compareTo(result.familyLimit()));
            assertEquals(0, new BigDecimal("1000").compareTo(result.catchUpLimit()));
        }

        @Test
        @DisplayName("Should extrapolate HSA limits for future year with $50 rounding")
        void extrapolatesHsaLimitsForFutureYear() {
            // 2030 is 4 years after 2026
            HsaLimits result = limits.getHsaLimitsForYear(2030);

            assertNotNull(result);
            // Individual: 4400 * (1.02)^4 = 4762.45 → rounds to $4,750
            assertEquals(0, new BigDecimal("4750").compareTo(result.individualLimit()));
            // Family: 8750 * (1.02)^4 = 9470.45 → rounds to $9,450
            assertEquals(0, new BigDecimal("9450").compareTo(result.familyLimit()));
            // Catch-up stays flat at $1,000
            assertEquals(0, new BigDecimal("1000").compareTo(result.catchUpLimit()));
        }

        @Test
        @DisplayName("Should return zero limits when no HSA years configured")
        void returnsZeroLimitsWhenEmpty() {
            IrsContributionLimits emptyLimits = new IrsContributionLimits();
            emptyLimits.setHsaLimits(new HashMap<>());

            HsaLimits result = emptyLimits.getHsaLimitsForYear(2025);

            assertNotNull(result);
            assertEquals(0, BigDecimal.ZERO.compareTo(result.individualLimit()));
        }
    }

    @Nested
    @DisplayName("YearLimits Record")
    class YearLimitsTests {

        @Test
        @DisplayName("Should handle null values with defaults")
        void handlesNullValuesWithDefaults() {
            YearLimits result = new YearLimits(null, null, null, null);

            assertEquals(0, BigDecimal.ZERO.compareTo(result.baseLimit()));
            assertEquals(0, BigDecimal.ZERO.compareTo(result.catchUpLimit()));
            assertEquals(0, BigDecimal.ZERO.compareTo(result.superCatchUpLimit()));
            assertEquals(0, BigDecimal.ZERO.compareTo(result.rothCatchUpIncomeThreshold()));
        }
    }

    @Nested
    @DisplayName("HsaLimits Record")
    class HsaLimitsTests {

        @Test
        @DisplayName("Should handle null values with defaults")
        void handlesNullValuesWithDefaults() {
            HsaLimits result = new HsaLimits(null, null, null);

            assertEquals(0, BigDecimal.ZERO.compareTo(result.individualLimit()));
            assertEquals(0, BigDecimal.ZERO.compareTo(result.familyLimit()));
            assertEquals(0, BigDecimal.ZERO.compareTo(result.catchUpLimit()));
        }
    }

    @Nested
    @DisplayName("Configuration Properties")
    class ConfigurationPropertiesTests {

        @Test
        @DisplayName("Should return default annual increase rate")
        void returnsDefaultAnnualIncreaseRate() {
            assertEquals(0, new BigDecimal("0.02").compareTo(limits.getDefaultAnnualIncreaseRate()));
        }

        @Test
        @DisplayName("Should allow setting annual increase rate")
        void allowsSettingAnnualIncreaseRate() {
            limits.setDefaultAnnualIncreaseRate(new BigDecimal("0.03"));
            assertEquals(0, new BigDecimal("0.03").compareTo(limits.getDefaultAnnualIncreaseRate()));
        }
    }
}
