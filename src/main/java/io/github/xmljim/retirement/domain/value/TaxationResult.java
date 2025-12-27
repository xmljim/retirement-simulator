package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Result of the Social Security benefit taxation calculation.
 *
 * <p>Determines what percentage of Social Security benefits are taxable
 * based on IRS Publication 915 rules.
 *
 * <p>Combined income formula: AGI + non-taxable interest + 50% of SS benefits
 *
 * <p>Thresholds by filing status (NOT indexed - fixed since 1984/1993):
 * <ul>
 *   <li>Single/HOH/QSS: $25,000 (50% tier) / $34,000 (85% tier)</li>
 *   <li>MFJ: $32,000 (50% tier) / $44,000 (85% tier)</li>
 *   <li>MFS (living with spouse): $0 - always up to 85% taxable</li>
 * </ul>
 *
 * <p><b>Note:</b> The non-indexed thresholds create "bracket creep" where
 * more benefits become taxable over time as incomes rise with inflation.
 *
 * @param ssBenefit the total annual Social Security benefit
 * @param combinedIncome AGI + non-taxable interest + 50% of SS benefits
 * @param taxableAmount the dollar amount of benefits that are taxable
 * @param taxablePercentage the percentage of benefits that are taxable (0.0, 0.50, or 0.85)
 * @param tier the taxation tier (NONE, FIFTY_PERCENT, or EIGHTY_FIVE_PERCENT)
 *
 * @see <a href="https://www.irs.gov/publications/p915">IRS Publication 915</a>
 */
public record TaxationResult(
    BigDecimal ssBenefit,
    BigDecimal combinedIncome,
    BigDecimal taxableAmount,
    BigDecimal taxablePercentage,
    TaxationTier tier
) {

    /**
     * Taxation tier based on combined income thresholds.
     */
    public enum TaxationTier {
        /**
         * Combined income below lower threshold - no benefits taxable.
         */
        NONE,

        /**
         * Combined income between lower and upper thresholds - up to 50% taxable.
         */
        FIFTY_PERCENT,

        /**
         * Combined income above upper threshold - up to 85% taxable.
         */
        EIGHTY_FIVE_PERCENT
    }

    /**
     * Creates a result where no benefits are taxable.
     *
     * @param ssBenefit the annual SS benefit
     * @param combinedIncome the calculated combined income
     * @return a result with zero taxable amount
     */
    public static TaxationResult none(BigDecimal ssBenefit, BigDecimal combinedIncome) {
        return new TaxationResult(
            ssBenefit,
            combinedIncome,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            TaxationTier.NONE
        );
    }

    /**
     * Creates a result where up to 50% of benefits are taxable.
     *
     * @param ssBenefit the annual SS benefit
     * @param combinedIncome the calculated combined income
     * @param taxableAmount the calculated taxable amount
     * @return a result in the 50% tier
     */
    public static TaxationResult fiftyPercent(
            BigDecimal ssBenefit,
            BigDecimal combinedIncome,
            BigDecimal taxableAmount) {
        BigDecimal percentage = calculatePercentage(taxableAmount, ssBenefit);
        return new TaxationResult(
            ssBenefit,
            combinedIncome,
            taxableAmount.setScale(2, RoundingMode.HALF_UP),
            percentage,
            TaxationTier.FIFTY_PERCENT
        );
    }

    /**
     * Creates a result where up to 85% of benefits are taxable.
     *
     * @param ssBenefit the annual SS benefit
     * @param combinedIncome the calculated combined income
     * @param taxableAmount the calculated taxable amount
     * @return a result in the 85% tier
     */
    public static TaxationResult eightyFivePercent(
            BigDecimal ssBenefit,
            BigDecimal combinedIncome,
            BigDecimal taxableAmount) {
        BigDecimal percentage = calculatePercentage(taxableAmount, ssBenefit);
        return new TaxationResult(
            ssBenefit,
            combinedIncome,
            taxableAmount.setScale(2, RoundingMode.HALF_UP),
            percentage,
            TaxationTier.EIGHTY_FIVE_PERCENT
        );
    }

    /**
     * Returns the tax-free portion of benefits.
     *
     * @return the non-taxable amount
     */
    public BigDecimal getNonTaxableAmount() {
        return ssBenefit.subtract(taxableAmount);
    }

    /**
     * Returns whether any benefits are taxable.
     *
     * @return true if taxable amount is greater than zero
     */
    public boolean isTaxable() {
        return taxableAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Returns the effective tax-free percentage.
     *
     * @return 1 minus the taxable percentage
     */
    public BigDecimal getTaxFreePercentage() {
        return BigDecimal.ONE.subtract(taxablePercentage);
    }

    private static BigDecimal calculatePercentage(BigDecimal taxableAmount, BigDecimal ssBenefit) {
        if (ssBenefit.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return taxableAmount.divide(ssBenefit, 4, RoundingMode.HALF_UP);
    }
}
