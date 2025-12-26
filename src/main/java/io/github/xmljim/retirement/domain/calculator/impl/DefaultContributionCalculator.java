package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import io.github.xmljim.retirement.domain.calculator.ContributionCalculator;
import io.github.xmljim.retirement.domain.calculator.IrsContributionRules;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.enums.ContributionType;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.value.ContributionConfig;
import io.github.xmljim.retirement.domain.value.ContributionResult;
import io.github.xmljim.retirement.domain.value.MatchingPolicy;

/**
 * Default implementation of {@link ContributionCalculator}.
 *
 * <p>This implementation provides standard contribution calculations for
 * retirement savings plans with support for:
 * <ul>
 *   <li>Personal contributions with annual increment increases</li>
 *   <li>Employer matching contributions</li>
 *   <li>Automatic zero contributions after retirement</li>
 *   <li>IRS annual contribution limit enforcement (when rules provided)</li>
 *   <li>Dollar amount calculations for simulation</li>
 * </ul>
 *
 * <p>When created with {@link IrsContributionRules}, the calculator will
 * enforce annual contribution limits. Without rules, limit checking is skipped.
 */
public class DefaultContributionCalculator implements ContributionCalculator {

    private static final int SCALE = 6;
    private static final int DOLLAR_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final int MONTHS_PER_YEAR = 12;

    private final IrsContributionRules irsRules;

    /**
     * Creates a new DefaultContributionCalculator without IRS limit enforcement.
     *
     * <p>Use this constructor for simple calculations that don't need
     * IRS limit checking.
     */
    public DefaultContributionCalculator() {
        this.irsRules = null;
    }

    /**
     * Creates a new DefaultContributionCalculator with IRS limit enforcement.
     *
     * <p>Use this constructor when you need to enforce IRS annual contribution
     * limits. The rules instance provides year-specific limits and catch-up
     * eligibility calculations.
     *
     * @param irsRules the IRS contribution rules to use for limit enforcement
     */
    public DefaultContributionCalculator(IrsContributionRules irsRules) {
        this.irsRules = irsRules;
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

    // =========================================================================
    // Dollar Amount Calculations (Issue #12)
    // =========================================================================

    @Override
    public ContributionResult calculateMonthlyContribution(
            BigDecimal monthlySalary,
            BigDecimal yearToDateContributions,
            int contributionYear,
            int personAge,
            LocalDate contributionDate,
            LocalDate retirementDate,
            ContributionConfig personalConfig,
            MatchingPolicy matchingPolicy,
            AccountType accountType) {

        MissingRequiredFieldException.requireNonNull(monthlySalary, "monthlySalary");
        MissingRequiredFieldException.requireNonNull(contributionDate, "contributionDate");
        MissingRequiredFieldException.requireNonNull(retirementDate, "retirementDate");
        MissingRequiredFieldException.requireNonNull(personalConfig, "personalConfig");
        MissingRequiredFieldException.requireNonNull(accountType, "accountType");

        BigDecimal ytd = yearToDateContributions != null ? yearToDateContributions : BigDecimal.ZERO;

        // No contributions after retirement
        if (isRetired(contributionDate, retirementDate)) {
            return ContributionResult.zero(ytd, accountType);
        }

        // Calculate contribution rate
        BigDecimal contributionRate = calculatePersonalContributionRate(
            contributionDate, retirementDate, personalConfig);

        // Calculate raw personal contribution amount
        BigDecimal rawPersonalContribution = calculatePersonalContributionAmount(
            monthlySalary, contributionRate);

        // Apply IRS limits if rules are available
        BigDecimal personalContribution = rawPersonalContribution;
        boolean limitApplied = false;

        if (irsRules != null) {
            BigDecimal annualLimit = irsRules.calculateAnnualContributionLimit(
                contributionYear, personAge, accountType);

            BigDecimal remainingLimit = annualLimit.subtract(ytd);

            if (remainingLimit.compareTo(BigDecimal.ZERO) <= 0) {
                // Already at limit
                personalContribution = BigDecimal.ZERO;
                limitApplied = true;
            } else if (rawPersonalContribution.compareTo(remainingLimit) > 0) {
                // Would exceed limit, cap it
                personalContribution = remainingLimit;
                limitApplied = true;
            }
        }

        personalContribution = personalContribution.setScale(DOLLAR_SCALE, ROUNDING_MODE);

        // Calculate employer match
        BigDecimal employerMatch = calculateEmployerMatchAmount(
            contributionRate, monthlySalary, matchingPolicy);

        // Calculate new YTD (only personal contributions count toward IRS limit)
        BigDecimal newYtd = ytd.add(personalContribution);

        // Determine target account
        AccountType targetAccount = accountType;
        if (irsRules != null) {
            // For now, use the simple routing. M3 will add more complex logic.
            targetAccount = irsRules.determineTargetAccountType(
                ContributionType.PERSONAL,
                false, // Not tracking catch-up separately yet
                null,  // No prior year income tracking yet
                contributionYear,
                accountType);
        }

        return ContributionResult.of(
            personalContribution,
            employerMatch,
            newYtd,
            targetAccount,
            limitApplied
        );
    }

    @Override
    public BigDecimal calculatePersonalContributionAmount(
            BigDecimal monthlySalary,
            BigDecimal contributionRate) {

        MissingRequiredFieldException.requireNonNull(monthlySalary, "monthlySalary");

        if (contributionRate == null || contributionRate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return monthlySalary.multiply(contributionRate).setScale(DOLLAR_SCALE, ROUNDING_MODE);
    }

    @Override
    public BigDecimal calculateEmployerMatchAmount(
            BigDecimal employeeContributionRate,
            BigDecimal monthlySalary,
            MatchingPolicy matchingPolicy) {

        MissingRequiredFieldException.requireNonNull(monthlySalary, "monthlySalary");

        if (matchingPolicy == null) {
            return BigDecimal.ZERO;
        }

        if (employeeContributionRate == null
                || employeeContributionRate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Get the employer match rate from the policy
        BigDecimal matchRate = matchingPolicy.calculateEmployerMatch(employeeContributionRate);

        // Calculate dollar amount
        return monthlySalary.multiply(matchRate).setScale(DOLLAR_SCALE, ROUNDING_MODE);
    }
}
