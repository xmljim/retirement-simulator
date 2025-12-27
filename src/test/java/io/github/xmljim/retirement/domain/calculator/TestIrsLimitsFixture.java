package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;

import io.github.xmljim.retirement.domain.config.IrsContributionLimits;

/**
 * Shared test fixtures for IRS contribution limit tests.
 *
 * <p>Provides consistent test data across unit and integration tests
 * for contribution limit and YTD tracking calculations.
 */
public final class TestIrsLimitsFixture {

    private TestIrsLimitsFixture() {
        // Utility class
    }

    /**
     * Creates test IRS contribution limits with 2025 values.
     *
     * <p>Includes:
     * <ul>
     *   <li>401(k)/403(b)/457(b): $23,500 base, $7,500 catch-up, $11,250 super catch-up</li>
     *   <li>IRA: $7,000 base, $1,000 catch-up</li>
     *   <li>HSA: $4,300 individual, $8,550 family, $1,000 catch-up</li>
     * </ul>
     *
     * @return configured IrsContributionLimits
     */
    public static IrsContributionLimits createTestLimits() {
        IrsContributionLimits limits = new IrsContributionLimits();

        limits.getLimits().put(2025, new IrsContributionLimits.YearLimits(
            new BigDecimal("23500"), new BigDecimal("7500"),
            new BigDecimal("11250"), new BigDecimal("145000")));

        limits.getIraLimits().put(2025, new IrsContributionLimits.IraLimits(
            new BigDecimal("7000"), new BigDecimal("1000")));

        limits.getHsaLimits().put(2025, new IrsContributionLimits.HsaLimits(
            new BigDecimal("4300"), new BigDecimal("8550"), new BigDecimal("1000")));

        return limits;
    }
}
