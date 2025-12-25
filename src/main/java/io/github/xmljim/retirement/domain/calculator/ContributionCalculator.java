package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.github.xmljim.retirement.domain.value.ContributionConfig;

/**
 * Calculator for retirement contribution calculations.
 *
 * <p>Provides methods for:
 * <ul>
 *   <li>Calculating personal contribution rates with increments</li>
 *   <li>Calculating employer contribution rates</li>
 *   <li>Determining retirement status based on dates</li>
 * </ul>
 *
 * <p>Contributions are calculated based on the contribution date relative
 * to the retirement date. After retirement, contribution rates are zero.
 *
 * @see io.github.xmljim.retirement.domain.calculator.impl.DefaultContributionCalculator
 */
public interface ContributionCalculator {

    /**
     * Determines if a person is retired based on the current date
     * and their retirement date.
     *
     * <p>A person is considered retired if the current date is on or after
     * the retirement date.
     *
     * @param currentDate the current date to check
     * @param retirementDate the planned or actual retirement date
     * @return true if retired (currentDate >= retirementDate), false otherwise
     * @throws IllegalArgumentException if either date is null
     */
    boolean isRetired(LocalDate currentDate, LocalDate retirementDate);

    /**
     * Calculates the personal contribution rate for a given date.
     *
     * <p>The contribution rate starts at the base rate defined in the config
     * and increases by the increment rate each year on the specified increment
     * month. After retirement, the contribution rate is zero.
     *
     * <p>Formula (while working):
     * <pre>
     * rate = baseRate + (incrementRate * yearsOfIncrements)
     * where yearsOfIncrements = floor(monthsSinceStart / 12) +
     *       (currentMonth >= incrementMonth ? 1 : 0)
     * </pre>
     *
     * @param contributionDate the date of the contribution
     * @param retirementDate the planned retirement date
     * @param config the contribution configuration
     * @return the contribution rate as a decimal (e.g., 0.10 for 10%)
     * @throws IllegalArgumentException if any parameter is null
     */
    BigDecimal calculatePersonalContributionRate(
        LocalDate contributionDate,
        LocalDate retirementDate,
        ContributionConfig config);

    /**
     * Calculates the employer contribution rate for a given date.
     *
     * <p>The employer contribution is a fixed rate while the employee is working.
     * After retirement, the employer contribution is zero.
     *
     * <p>Returns zero in these cases:
     * <ul>
     *   <li>The employee is retired (contributionDate >= retirementDate)</li>
     *   <li>The employer match rate in config is zero or null</li>
     *   <li>The contribution type does not include employer matching</li>
     * </ul>
     *
     * @param contributionDate the date of the contribution
     * @param retirementDate the planned retirement date
     * @param config the contribution configuration
     * @return the contribution rate as a decimal (e.g., 0.04 for 4%), or zero if retired
     *         or if no employer match is configured
     * @throws IllegalArgumentException if any parameter is null
     */
    BigDecimal calculateEmployerContributionRate(
        LocalDate contributionDate,
        LocalDate retirementDate,
        ContributionConfig config);
}
