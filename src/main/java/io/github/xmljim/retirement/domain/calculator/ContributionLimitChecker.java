package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;

import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.enums.ContributionType;

/**
 * Checks proposed contributions against IRS limits.
 *
 * <p>This interface provides pre-contribution validation to ensure
 * contributions comply with IRS limits. It considers:
 * <ul>
 *   <li>Year-to-date contributions already made</li>
 *   <li>Account type and its limit category</li>
 *   <li>Personal vs employer contribution type</li>
 *   <li>Age-based catch-up eligibility</li>
 *   <li>HSA coverage type (individual vs family)</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * ContributionLimitChecker checker = CalculatorFactory.limitChecker(irsRules);
 *
 * LimitCheckResult result = checker.check(
 *     tracker,                          // YTD tracker with existing contributions
 *     new BigDecimal("5000"),           // proposed amount
 *     AccountType.TRADITIONAL_401K,     // target account
 *     ContributionType.PERSONAL,        // contribution type
 *     2025,                             // year
 *     55,                               // age
 *     new BigDecimal("100000"),         // prior year income
 *     true                              // has spouse (for HSA)
 * );
 *
 * if (result.allowed()) {
 *     // Proceed with contribution of result.allowedAmount()
 * }
 * }</pre>
 *
 * @see LimitCheckResult
 * @see YTDContributionTracker
 * @see io.github.xmljim.retirement.domain.calculator.impl.DefaultContributionLimitChecker
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface ContributionLimitChecker {

    /**
     * Checks a proposed contribution against IRS limits.
     *
     * <p>The check considers:
     * <ol>
     *   <li>Maps account type to limit category</li>
     *   <li>Gets applicable limit (base + catch-up if eligible)</li>
     *   <li>Subtracts YTD personal contributions from limit</li>
     *   <li>Returns allowed amount (may be partial if near limit)</li>
     * </ol>
     *
     * <p>For employer contributions, no limit check is performed since
     * employer contributions do not count against employee limits.
     *
     * @param tracker the YTD contribution tracker
     * @param proposedAmount the amount to contribute
     * @param targetAccount the target account type
     * @param source PERSONAL or EMPLOYER contribution
     * @param year the contribution year
     * @param age the person's age (for catch-up eligibility)
     * @param priorYearIncome prior year income (for SECURE 2.0 Roth rules)
     * @param hasSpouse true if person has spouse (for HSA family limit)
     * @return the result indicating allowed amount and any warnings
     * @throws io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException
     *         if required parameters are null
     * @throws io.github.xmljim.retirement.domain.exception.ValidationException
     *         if proposedAmount is negative
     */
    LimitCheckResult check(
        YTDContributionTracker tracker,
        BigDecimal proposedAmount,
        AccountType targetAccount,
        ContributionType source,
        int year,
        int age,
        BigDecimal priorYearIncome,
        boolean hasSpouse
    );

    /**
     * Convenience method for checking personal contributions without income context.
     *
     * <p>Uses zero for prior year income, which means no SECURE 2.0 Roth requirements.
     *
     * @param tracker the YTD contribution tracker
     * @param proposedAmount the amount to contribute
     * @param targetAccount the target account type
     * @param year the contribution year
     * @param age the person's age
     * @param hasSpouse true if person has spouse
     * @return the result indicating allowed amount and any warnings
     */
    default LimitCheckResult checkPersonal(
            YTDContributionTracker tracker,
            BigDecimal proposedAmount,
            AccountType targetAccount,
            int year,
            int age,
            boolean hasSpouse) {
        return check(
            tracker, proposedAmount, targetAccount, ContributionType.PERSONAL,
            year, age, BigDecimal.ZERO, hasSpouse);
    }

    /**
     * Convenience method for checking employer contributions.
     *
     * <p>Employer contributions are always allowed (no IRS limit applies).
     *
     * @param proposedAmount the employer contribution amount
     * @return a result indicating the full amount is allowed
     */
    default LimitCheckResult checkEmployer(BigDecimal proposedAmount) {
        return LimitCheckResult.allowed(proposedAmount, BigDecimal.ZERO);
    }
}
