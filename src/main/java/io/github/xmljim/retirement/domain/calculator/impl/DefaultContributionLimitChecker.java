package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.xmljim.retirement.domain.calculator.ContributionLimitChecker;
import io.github.xmljim.retirement.domain.calculator.IrsContributionRules;
import io.github.xmljim.retirement.domain.calculator.LimitCheckResult;
import io.github.xmljim.retirement.domain.calculator.YTDContributionTracker;
import io.github.xmljim.retirement.domain.config.IrsContributionLimits;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.enums.ContributionType;
import io.github.xmljim.retirement.domain.enums.LimitCategory;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;

/**
 * Default implementation of contribution limit checking.
 *
 * <p>This implementation checks proposed contributions against IRS limits
 * using the year-to-date tracker to determine remaining room.
 *
 * <p>Key behaviors:
 * <ul>
 *   <li>Employer contributions are always allowed (no limit check)</li>
 *   <li>Personal contributions checked against category limits</li>
 *   <li>Partial contributions allowed when near limit</li>
 *   <li>Warnings generated for limit reductions</li>
 * </ul>
 */
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "IrsContributionLimits is a Spring-managed singleton bean"
)
public class DefaultContributionLimitChecker implements ContributionLimitChecker {

    private final IrsContributionRules irsRules;
    private final IrsContributionLimits irsLimits;

    /**
     * Creates a new limit checker.
     *
     * @param irsRules the IRS contribution rules
     * @param irsLimits the IRS contribution limits configuration
     */
    public DefaultContributionLimitChecker(IrsContributionRules irsRules, IrsContributionLimits irsLimits) {
        MissingRequiredFieldException.requireNonNull(irsRules, "irsRules");
        MissingRequiredFieldException.requireNonNull(irsLimits, "irsLimits");
        this.irsRules = irsRules;
        this.irsLimits = irsLimits;
    }

    @Override
    public LimitCheckResult check(
            YTDContributionTracker tracker,
            BigDecimal proposedAmount,
            AccountType targetAccount,
            ContributionType source,
            int year,
            int age,
            BigDecimal priorYearIncome,
            boolean hasSpouse) {

        // Validate inputs
        MissingRequiredFieldException.requireNonNull(tracker, "tracker");
        MissingRequiredFieldException.requireNonNull(proposedAmount, "proposedAmount");
        MissingRequiredFieldException.requireNonNull(targetAccount, "targetAccount");
        MissingRequiredFieldException.requireNonNull(source, "source");

        if (proposedAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Proposed amount cannot be negative", "proposedAmount");
        }

        // Zero amount is always allowed
        if (proposedAmount.compareTo(BigDecimal.ZERO) == 0) {
            return LimitCheckResult.allowed(BigDecimal.ZERO, BigDecimal.ZERO);
        }

        // Employer contributions are always allowed (no IRS limit)
        if (source == ContributionType.EMPLOYER) {
            return LimitCheckResult.allowed(proposedAmount, BigDecimal.ZERO);
        }

        // Get limit category for this account type
        LimitCategory category = LimitCategory.fromAccountType(targetAccount);
        if (category == null) {
            // Account type has no limits (e.g., taxable brokerage)
            return LimitCheckResult.allowed(proposedAmount, BigDecimal.ZERO);
        }

        // Calculate applicable limit
        BigDecimal limit = calculateLimit(category, year, age, hasSpouse);
        if (limit.compareTo(BigDecimal.ZERO) == 0) {
            return LimitCheckResult.denied(
                "No contribution limit configured for " + category.getDisplayName() + " in year " + year);
        }

        // Get YTD personal contributions
        BigDecimal ytd = tracker.getYTDPersonalContributions(year, category);
        BigDecimal remainingRoom = limit.subtract(ytd).max(BigDecimal.ZERO);

        // Check if at limit
        if (remainingRoom.compareTo(BigDecimal.ZERO) <= 0) {
            return LimitCheckResult.denied(
                "Annual limit of $" + limit + " already reached for " + category.getDisplayName());
        }

        // Check if partial contribution needed
        if (proposedAmount.compareTo(remainingRoom) > 0) {
            BigDecimal newRemaining = BigDecimal.ZERO;
            String warning = String.format(
                "Contribution reduced from $%s to $%s due to %s annual limit of $%s",
                proposedAmount, remainingRoom, category.getDisplayName(), limit);
            return LimitCheckResult.partial(remainingRoom, newRemaining, warning);
        }

        // Full contribution allowed
        BigDecimal newRemaining = remainingRoom.subtract(proposedAmount);
        return LimitCheckResult.allowed(proposedAmount, newRemaining);
    }

    /**
     * Calculates the applicable limit for a category.
     */
    private BigDecimal calculateLimit(LimitCategory category, int year, int age, boolean hasSpouse) {
        return switch (category) {
            case EMPLOYER_401K -> calculate401kLimit(year, age);
            case IRA -> calculateIraLimit(year, age);
            case HSA -> calculateHsaLimit(year, age, hasSpouse);
        };
    }

    private BigDecimal calculate401kLimit(int year, int age) {
        return irsRules.calculateAnnualContributionLimit(year, age, AccountType.TRADITIONAL_401K);
    }

    private BigDecimal calculateIraLimit(int year, int age) {
        var iraLimits = irsLimits.getIraLimitsForYear(year);
        BigDecimal base = iraLimits.baseLimit();
        // IRA catch-up starts at age 50
        if (age >= 50) {
            return base.add(iraLimits.catchUpLimit());
        }
        return base;
    }

    private BigDecimal calculateHsaLimit(int year, int age, boolean hasSpouse) {
        var hsaLimits = irsLimits.getHsaLimitsForYear(year);
        BigDecimal base = hasSpouse ? hsaLimits.familyLimit() : hsaLimits.individualLimit();
        // HSA catch-up starts at age 55
        if (age >= 55) {
            return base.add(hsaLimits.catchUpLimit());
        }
        return base;
    }
}
