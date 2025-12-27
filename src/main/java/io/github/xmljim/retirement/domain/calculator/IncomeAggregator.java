package io.github.xmljim.retirement.domain.calculator;

import java.time.LocalDate;

import io.github.xmljim.retirement.domain.value.IncomeBreakdown;
import io.github.xmljim.retirement.domain.value.IncomeSources;

/**
 * Aggregates income from multiple sources for a given date.
 *
 * <p>This service calculates total income by combining all configured income
 * sources and considering their active date ranges. It provides breakdowns
 * by income type and categorizes income as earned or passive for tax and
 * Social Security earnings test purposes.
 *
 * <p>Example usage:
 * <pre>{@code
 * IncomeSources sources = IncomeSources.builder()
 *     .workingIncome(salary)
 *     .socialSecurityBenefit(ssBenefit)
 *     .addPension(companyPension)
 *     .addOtherIncome(rentalIncome)
 *     .build();
 *
 * IncomeBreakdown monthly = aggregator.getMonthlyIncome(sources, date);
 * IncomeBreakdown annual = aggregator.getAnnualIncome(sources, date);
 * }</pre>
 */
public interface IncomeAggregator {

    /**
     * Calculates total monthly income from all sources for the given date.
     *
     * <p>Only includes income sources that are active on the specified date.
     * Applies any COLA/inflation adjustments based on time elapsed since
     * each income source's start date.
     *
     * @param sources the income sources to aggregate
     * @param asOfDate the date for which to calculate income
     * @return breakdown of monthly income by source type
     */
    IncomeBreakdown getMonthlyIncome(IncomeSources sources, LocalDate asOfDate);

    /**
     * Calculates total annual income from all sources for the given date.
     *
     * <p>This is equivalent to monthly income multiplied by 12.
     *
     * @param sources the income sources to aggregate
     * @param asOfDate the date for which to calculate income
     * @return breakdown of annual income by source type
     */
    default IncomeBreakdown getAnnualIncome(IncomeSources sources, LocalDate asOfDate) {
        return getMonthlyIncome(sources, asOfDate).toAnnual();
    }

    /**
     * Combines income breakdowns from two individuals (couple).
     *
     * <p>Creates a combined household breakdown by summing each category.
     *
     * @param primary income breakdown for primary person
     * @param secondary income breakdown for secondary person (spouse)
     * @return combined household income breakdown
     */
    IncomeBreakdown combineForCouple(IncomeBreakdown primary, IncomeBreakdown secondary);
}
