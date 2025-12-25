package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import io.github.xmljim.retirement.domain.calculator.ContributionCalculator;
import io.github.xmljim.retirement.domain.value.ContributionConfig;

/**
 * Default implementation of {@link ContributionCalculator}.
 *
 * <p>This implementation provides standard contribution calculations for
 * retirement savings plans with support for:
 * <ul>
 *   <li>Personal contributions with annual increment increases</li>
 *   <li>Employer matching contributions</li>
 *   <li>Automatic zero contributions after retirement</li>
 * </ul>
 */
public class DefaultContributionCalculator implements ContributionCalculator {

    private static final int SCALE = 6;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final int MONTHS_PER_YEAR = 12;

    /**
     * Creates a new DefaultContributionCalculator.
     */
    public DefaultContributionCalculator() {
        // Default constructor
    }

    @Override
    public boolean isRetired(LocalDate currentDate, LocalDate retirementDate) {
        if (currentDate == null) {
            throw new IllegalArgumentException("Current date cannot be null");
        }
        if (retirementDate == null) {
            throw new IllegalArgumentException("Retirement date cannot be null");
        }
        return !currentDate.isBefore(retirementDate);
    }

    @Override
    public BigDecimal calculatePersonalContributionRate(
            LocalDate contributionDate,
            LocalDate retirementDate,
            ContributionConfig config) {

        validateInputs(contributionDate, retirementDate, config);

        // No contributions after retirement
        if (isRetired(contributionDate, retirementDate)) {
            return BigDecimal.ZERO;
        }

        BigDecimal baseRate = config.getContributionRate();
        BigDecimal incrementRate = config.getIncrementRate();

        // Calculate months since the reference date (start of current year)
        LocalDate referenceDate = LocalDate.of(LocalDate.now().getYear(), 1, 1);
        long monthsSinceReference = Math.abs(
            ChronoUnit.MONTHS.between(referenceDate, contributionDate));

        // Calculate the number of full years for increments
        int yearsOfIncrements = (int) Math.floor((double) monthsSinceReference / MONTHS_PER_YEAR);

        // Add an extra increment if we've passed the increment month this year
        boolean passedIncrementMonth =
            contributionDate.getMonth().getValue() >= config.getIncrementMonth().getValue();

        BigDecimal incrementMultiplier = BigDecimal.valueOf(yearsOfIncrements);
        if (passedIncrementMonth) {
            incrementMultiplier = incrementMultiplier.add(BigDecimal.ONE);
        }

        // Calculate total rate: baseRate + (incrementRate * incrementMultiplier)
        BigDecimal totalRate = baseRate.add(
            incrementRate.multiply(incrementMultiplier));

        return totalRate.setScale(SCALE, ROUNDING_MODE);
    }

    @Override
    public BigDecimal calculateEmployerContributionRate(
            LocalDate contributionDate,
            LocalDate retirementDate,
            ContributionConfig config) {

        validateInputs(contributionDate, retirementDate, config);

        // No contributions after retirement
        if (isRetired(contributionDate, retirementDate)) {
            return BigDecimal.ZERO;
        }

        // Employer contribution is a fixed rate
        return config.getContributionRate().setScale(SCALE, ROUNDING_MODE);
    }

    private void validateInputs(LocalDate contributionDate, LocalDate retirementDate,
                                ContributionConfig config) {
        if (contributionDate == null) {
            throw new IllegalArgumentException("Contribution date cannot be null");
        }
        if (retirementDate == null) {
            throw new IllegalArgumentException("Retirement date cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("Contribution config cannot be null");
        }
    }
}
