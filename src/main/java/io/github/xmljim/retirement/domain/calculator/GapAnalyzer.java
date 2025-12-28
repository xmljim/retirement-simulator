package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import io.github.xmljim.retirement.domain.value.ExpenseBreakdown;
import io.github.xmljim.retirement.domain.value.GapAnalysis;
import io.github.xmljim.retirement.domain.value.IncomeBreakdown;

/**
 * Analyzes the gap between income and expenses to determine withdrawal needs.
 *
 * <p>This service compares income from all sources against expenses to determine
 * whether there is a surplus (can save) or deficit (need to withdraw from portfolio).
 *
 * <p>Key scenarios:
 * <ul>
 *   <li><b>Pre-retirement:</b> Usually surplus - saving for retirement</li>
 *   <li><b>Early retirement:</b> May have deficit until Social Security starts</li>
 *   <li><b>Full retirement:</b> Gap varies based on income sources timing</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * GapAnalysis monthly = analyzer.analyzeMonthly(income, expenses, date);
 * if (monthly.hasDeficit()) {
 *     BigDecimal grossNeeded = monthly.grossWithdrawalNeeded(marginalRate);
 * }
 *
 * // Project for entire year
 * List<GapAnalysis> yearlyProjection = analyzer.projectYear(
 *     incomeSources, budget, startDate);
 * }</pre>
 */
public interface GapAnalyzer {

    /**
     * Analyzes the monthly gap between income and expenses.
     *
     * @param income the income breakdown for the month
     * @param expenses the expense breakdown for the month
     * @param asOfDate the date for the analysis
     * @return the gap analysis result
     */
    GapAnalysis analyzeMonthly(IncomeBreakdown income, ExpenseBreakdown expenses, LocalDate asOfDate);

    /**
     * Analyzes the annual gap between income and expenses.
     *
     * @param income the monthly income breakdown (will be annualized)
     * @param expenses the monthly expense breakdown (will be annualized)
     * @param asOfDate the date for the analysis
     * @return the gap analysis result with annualized amounts
     */
    GapAnalysis analyzeAnnual(IncomeBreakdown income, ExpenseBreakdown expenses, LocalDate asOfDate);

    /**
     * Projects monthly gap analyses for an entire year.
     *
     * <p>Returns a list of 12 monthly analyses starting from the given date.
     * This is useful for identifying when gaps change due to income sources
     * starting or stopping (retirement, Social Security, etc.).
     *
     * @param incomeProvider function to get income breakdown for a given date
     * @param expenseProvider function to get expense breakdown for a given date
     * @param startDate the first month of the projection
     * @return list of 12 monthly gap analyses
     */
    List<GapAnalysis> projectYear(
            java.util.function.Function<LocalDate, IncomeBreakdown> incomeProvider,
            java.util.function.Function<LocalDate, ExpenseBreakdown> expenseProvider,
            LocalDate startDate);

    /**
     * Calculates the gross withdrawal needed after accounting for taxes.
     *
     * <p>When withdrawing from tax-deferred accounts, the withdrawal is taxable.
     * This method grosses up the needed amount to account for taxes.
     *
     * @param netNeeded the net amount needed after taxes
     * @param marginalTaxRate the marginal tax rate (e.g., 0.22 for 22%)
     * @return the gross withdrawal amount
     */
    BigDecimal calculateGrossWithdrawal(BigDecimal netNeeded, BigDecimal marginalTaxRate);

    /**
     * Summarizes a year of gap analyses.
     *
     * @param monthlyAnalyses list of monthly analyses
     * @return summary analysis for the entire period
     */
    GapAnalysis summarize(List<GapAnalysis> monthlyAnalyses);
}
