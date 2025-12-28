package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Represents a federal income tax bracket.
 *
 * <p>Each bracket defines a tax rate and the income range it applies to.
 * Brackets are progressive - only income within the bracket range is taxed
 * at that bracket's rate.
 *
 * @param rate the tax rate as a decimal (e.g., 0.22 for 22%)
 * @param lowerBound the minimum income for this bracket (inclusive)
 * @param upperBound the maximum income for this bracket (exclusive), empty for top bracket
 *
 * @see <a href="https://www.irs.gov/publications/p17">IRS Publication 17</a>
 */
public record TaxBracket(
    BigDecimal rate,
    BigDecimal lowerBound,
    Optional<BigDecimal> upperBound
) {

    /**
     * Creates a bracket with explicit bounds.
     *
     * @param rate the tax rate
     * @param lowerBound the lower bound
     * @param upperBound the upper bound (null for top bracket)
     * @return a new TaxBracket
     */
    public static TaxBracket of(BigDecimal rate, BigDecimal lowerBound, BigDecimal upperBound) {
        return new TaxBracket(rate, lowerBound, Optional.ofNullable(upperBound));
    }

    /**
     * Creates the top bracket (no upper bound).
     *
     * @param rate the tax rate
     * @param lowerBound the lower bound
     * @return a new TaxBracket with no upper bound
     */
    public static TaxBracket topBracket(BigDecimal rate, BigDecimal lowerBound) {
        return new TaxBracket(rate, lowerBound, Optional.empty());
    }

    /**
     * Returns whether this is the top bracket (no upper bound).
     *
     * @return true if this bracket has no upper bound
     */
    public boolean isTopBracket() {
        return upperBound.isEmpty();
    }

    /**
     * Returns the width of this bracket (upper - lower).
     *
     * @return the bracket width, or empty if this is the top bracket
     */
    public Optional<BigDecimal> getWidth() {
        return upperBound.map(upper -> upper.subtract(lowerBound));
    }

    /**
     * Returns whether the given income falls within this bracket.
     *
     * @param income the taxable income
     * @return true if income is within this bracket
     */
    public boolean containsIncome(BigDecimal income) {
        boolean aboveLower = income.compareTo(lowerBound) >= 0;
        boolean belowUpper = upperBound.map(upper -> income.compareTo(upper) < 0).orElse(true);
        return aboveLower && belowUpper;
    }

    /**
     * Calculates the amount of income that falls within this bracket.
     *
     * @param taxableIncome the total taxable income
     * @return the income amount taxed at this bracket's rate
     */
    public BigDecimal getIncomeInBracket(BigDecimal taxableIncome) {
        if (taxableIncome.compareTo(lowerBound) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal incomeAboveLower = taxableIncome.subtract(lowerBound);

        return upperBound
            .map(upper -> {
                BigDecimal bracketWidth = upper.subtract(lowerBound);
                return incomeAboveLower.min(bracketWidth);
            })
            .orElse(incomeAboveLower);
    }

    /**
     * Calculates the tax owed for income in this bracket.
     *
     * @param taxableIncome the total taxable income
     * @return the tax amount for income in this bracket
     */
    public BigDecimal calculateTaxInBracket(BigDecimal taxableIncome) {
        return getIncomeInBracket(taxableIncome).multiply(rate);
    }

    /**
     * Returns the rate as a percentage (e.g., 22 for 22%).
     *
     * @return the rate multiplied by 100
     */
    public BigDecimal getRateAsPercentage() {
        return rate.multiply(BigDecimal.valueOf(100));
    }
}
