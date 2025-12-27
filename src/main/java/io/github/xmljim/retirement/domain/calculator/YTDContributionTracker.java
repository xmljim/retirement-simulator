package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;
import java.util.List;

import io.github.xmljim.retirement.domain.enums.LimitCategory;
import io.github.xmljim.retirement.domain.value.ContributionRecord;
import io.github.xmljim.retirement.domain.value.YTDSummary;

/**
 * Tracks year-to-date contributions for limit enforcement and reporting.
 *
 * <p>This interface is designed to be immutable - calling {@link #recordContribution}
 * returns a new tracker instance with the contribution added, preserving the
 * original tracker unchanged.
 *
 * <p>Key tracking capabilities:
 * <ul>
 *   <li>Track personal vs employer contributions separately</li>
 *   <li>Group contributions by limit category (401k, IRA, HSA)</li>
 *   <li>Calculate remaining room under IRS limits</li>
 *   <li>Generate summary reports by year</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * YTDContributionTracker tracker = CalculatorFactory.ytdTracker();
 *
 * // Record a contribution (returns new immutable tracker)
 * tracker = tracker.recordContribution(ContributionRecord.builder()
 *     .accountId("401k")
 *     .accountType(AccountType.TRADITIONAL_401K)
 *     .source(ContributionType.PERSONAL)
 *     .amount(new BigDecimal("500"))
 *     .year(2025)
 *     .date(LocalDate.now())
 *     .isCatchUp(false)
 *     .build());
 *
 * // Check YTD totals
 * BigDecimal ytd = tracker.getYTDPersonalContributions(2025, LimitCategory.EMPLOYER_401K);
 *
 * // Get full summary
 * YTDSummary summary = tracker.getSummary(2025, 55, true);  // age 55, has spouse
 * }</pre>
 *
 * @see ContributionRecord
 * @see YTDSummary
 * @see io.github.xmljim.retirement.domain.calculator.impl.DefaultYTDContributionTracker
 */
public interface YTDContributionTracker {

    /**
     * Records a contribution and returns a new tracker with the contribution added.
     *
     * <p>This method is immutable - it does not modify the current tracker.
     *
     * @param record the contribution to record
     * @return a new tracker with the contribution added
     * @throws io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException
     *         if record is null
     */
    YTDContributionTracker recordContribution(ContributionRecord record);

    /**
     * Returns the YTD personal (employee) contributions for a category and year.
     *
     * @param year the tax year
     * @param category the limit category
     * @return the total personal contributions, or zero if none
     */
    BigDecimal getYTDPersonalContributions(int year, LimitCategory category);

    /**
     * Returns the YTD employer contributions for a category and year.
     *
     * <p>Note: Employer contributions do not count against personal contribution limits.
     *
     * @param year the tax year
     * @param category the limit category
     * @return the total employer contributions, or zero if none
     */
    BigDecimal getYTDEmployerContributions(int year, LimitCategory category);

    /**
     * Returns the total YTD contributions (personal + employer) for a category.
     *
     * @param year the tax year
     * @param category the limit category
     * @return the total contributions
     */
    default BigDecimal getYTDTotalContributions(int year, LimitCategory category) {
        return getYTDPersonalContributions(year, category)
            .add(getYTDEmployerContributions(year, category));
    }

    /**
     * Returns all contribution records for a specific year.
     *
     * @param year the tax year
     * @return list of contribution records, may be empty
     */
    List<ContributionRecord> getRecordsForYear(int year);

    /**
     * Returns a summary of YTD contributions and limits for a year.
     *
     * @param year the tax year
     * @param age the person's age (for catch-up eligibility)
     * @param hasSpouse true if person has spouse (for HSA family limit)
     * @return a YTDSummary with contributions, limits, and remaining room
     */
    YTDSummary getSummary(int year, int age, boolean hasSpouse);

    /**
     * Returns a summary using the person's configuration.
     *
     * <p>Convenience method that extracts age and spouse status from parameters.
     *
     * @param year the tax year
     * @param age the person's age
     * @param hasSpouse whether the person has a spouse
     * @param irsRules the IRS rules for calculating limits
     * @return a YTDSummary
     */
    YTDSummary getSummary(int year, int age, boolean hasSpouse, IrsContributionRules irsRules);

    /**
     * Creates an empty tracker with no contributions.
     *
     * @param irsRules the IRS rules for limit calculations
     * @return a new empty tracker
     */
    static YTDContributionTracker empty(IrsContributionRules irsRules) {
        return new io.github.xmljim.retirement.domain.calculator.impl.DefaultYTDContributionTracker(irsRules);
    }
}
