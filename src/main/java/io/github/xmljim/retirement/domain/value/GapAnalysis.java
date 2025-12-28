package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Result of income vs expense gap analysis.
 *
 * <p>Represents the financial gap between income and expenses for a given period,
 * determining whether a surplus exists (can save or reduce withdrawals) or a
 * deficit requires portfolio withdrawal.
 *
 * <p>Example:
 * <pre>{@code
 * GapAnalysis analysis = gapAnalyzer.analyze(income, expenses, date);
 * if (analysis.hasDeficit()) {
 *     BigDecimal needed = analysis.withdrawalNeeded();
 *     BigDecimal grossWithdrawal = analysis.grossWithdrawalNeeded(marginalRate);
 * }
 * }</pre>
 *
 * @param asOfDate the date for which the analysis was performed
 * @param totalIncome total income from all sources
 * @param totalExpenses total expenses
 * @param gap income minus expenses (negative = deficit, positive = surplus)
 * @param withdrawalNeeded amount needed from portfolio (zero if surplus)
 * @param surplusToSave amount available to save/invest (zero if deficit)
 */
public record GapAnalysis(
        LocalDate asOfDate,
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal gap,
        BigDecimal withdrawalNeeded,
        BigDecimal surplusToSave
) {
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /**
     * Compact constructor ensuring non-null values.
     */
    public GapAnalysis {
        Objects.requireNonNull(asOfDate, "asOfDate is required");
        totalIncome = totalIncome != null ? totalIncome : BigDecimal.ZERO;
        totalExpenses = totalExpenses != null ? totalExpenses : BigDecimal.ZERO;
        gap = gap != null ? gap : BigDecimal.ZERO;
        withdrawalNeeded = withdrawalNeeded != null ? withdrawalNeeded : BigDecimal.ZERO;
        surplusToSave = surplusToSave != null ? surplusToSave : BigDecimal.ZERO;
    }

    /**
     * Returns whether there is a deficit (expenses exceed income).
     *
     * @return true if gap is negative
     */
    public boolean hasDeficit() {
        return gap.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Returns whether there is a surplus (income exceeds expenses).
     *
     * @return true if gap is positive
     */
    public boolean hasSurplus() {
        return gap.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Returns whether income exactly matches expenses.
     *
     * @return true if gap is zero
     */
    public boolean isBreakEven() {
        return gap.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Calculates the gross withdrawal needed to net the required amount after taxes.
     *
     * <p>Formula: grossWithdrawal = withdrawalNeeded / (1 - marginalTaxRate)
     *
     * <p>Example: If you need $2000 and marginal rate is 22%:
     * grossWithdrawal = $2000 / (1 - 0.22) = $2564.10
     *
     * @param marginalTaxRate the marginal tax rate (e.g., 0.22 for 22%)
     * @return the gross withdrawal amount needed
     */
    public BigDecimal grossWithdrawalNeeded(BigDecimal marginalTaxRate) {
        if (withdrawalNeeded.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        if (marginalTaxRate == null || marginalTaxRate.compareTo(BigDecimal.ZERO) <= 0) {
            return withdrawalNeeded;
        }

        if (marginalTaxRate.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("Marginal tax rate must be less than 1.0");
        }

        BigDecimal netFactor = BigDecimal.ONE.subtract(marginalTaxRate);
        return withdrawalNeeded.divide(netFactor, SCALE, ROUNDING);
    }

    /**
     * Returns the coverage ratio (income / expenses).
     *
     * <p>A ratio of 1.0 means break-even, greater than 1.0 means surplus,
     * less than 1.0 means deficit.
     *
     * @return the coverage ratio, or zero if no expenses
     */
    public BigDecimal coverageRatio() {
        if (totalExpenses.compareTo(BigDecimal.ZERO) == 0) {
            return totalIncome.compareTo(BigDecimal.ZERO) > 0
                    ? BigDecimal.valueOf(Double.MAX_VALUE)
                    : BigDecimal.ZERO;
        }
        return totalIncome.divide(totalExpenses, 4, ROUNDING);
    }

    /**
     * Creates a GapAnalysis from income and expense totals.
     *
     * @param asOfDate the date
     * @param totalIncome total income
     * @param totalExpenses total expenses
     * @return a new GapAnalysis
     */
    public static GapAnalysis of(LocalDate asOfDate, BigDecimal totalIncome, BigDecimal totalExpenses) {
        BigDecimal gap = totalIncome.subtract(totalExpenses);
        BigDecimal withdrawalNeeded = gap.compareTo(BigDecimal.ZERO) < 0
                ? gap.abs()
                : BigDecimal.ZERO;
        BigDecimal surplusToSave = gap.compareTo(BigDecimal.ZERO) > 0
                ? gap
                : BigDecimal.ZERO;

        return new GapAnalysis(
                asOfDate,
                totalIncome,
                totalExpenses,
                gap,
                withdrawalNeeded,
                surplusToSave
        );
    }

    /**
     * Creates an empty analysis for the given date.
     *
     * @param asOfDate the date
     * @return an empty GapAnalysis with all zeros
     */
    public static GapAnalysis empty(LocalDate asOfDate) {
        return new GapAnalysis(
                asOfDate,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }
}
