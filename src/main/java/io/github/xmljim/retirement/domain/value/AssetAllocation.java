package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Represents the asset allocation of an investment account.
 *
 * <p>Asset allocation defines how investments are distributed across
 * three major asset classes: stocks, bonds, and cash. The percentages
 * must sum to exactly 100%.
 *
 * <p>This is an immutable value object. Use the static factory methods
 * or builder to create instances:
 * <pre>{@code
 * // Using factory method
 * AssetAllocation balanced = AssetAllocation.of(60, 30, 10);
 *
 * // Using builder
 * AssetAllocation custom = AssetAllocation.builder()
 *     .stocks(70)
 *     .bonds(25)
 *     .cash(5)
 *     .build();
 * }</pre>
 */
public final class AssetAllocation {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int SCALE = 4;

    private final BigDecimal stocksPercentage;
    private final BigDecimal bondsPercentage;
    private final BigDecimal cashPercentage;

    private AssetAllocation(BigDecimal stocks, BigDecimal bonds, BigDecimal cash) {
        this.stocksPercentage = stocks.setScale(SCALE, RoundingMode.HALF_UP);
        this.bondsPercentage = bonds.setScale(SCALE, RoundingMode.HALF_UP);
        this.cashPercentage = cash.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Creates an asset allocation with the specified percentages.
     *
     * @param stocks percentage allocated to stocks (0-100)
     * @param bonds percentage allocated to bonds (0-100)
     * @param cash percentage allocated to cash (0-100)
     * @return a new AssetAllocation instance
     * @throws IllegalArgumentException if percentages don't sum to 100 or are out of range
     */
    public static AssetAllocation of(double stocks, double bonds, double cash) {
        return builder()
                .stocks(stocks)
                .bonds(bonds)
                .cash(cash)
                .build();
    }

    /**
     * Creates a 100% stocks allocation.
     *
     * @return an all-stocks allocation
     */
    public static AssetAllocation allStocks() {
        return new AssetAllocation(HUNDRED, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /**
     * Creates a 100% bonds allocation.
     *
     * @return an all-bonds allocation
     */
    public static AssetAllocation allBonds() {
        return new AssetAllocation(BigDecimal.ZERO, HUNDRED, BigDecimal.ZERO);
    }

    /**
     * Creates a 100% cash allocation.
     *
     * @return an all-cash allocation
     */
    public static AssetAllocation allCash() {
        return new AssetAllocation(BigDecimal.ZERO, BigDecimal.ZERO, HUNDRED);
    }

    /**
     * Creates a balanced 60/40 allocation (60% stocks, 40% bonds).
     *
     * @return a balanced allocation
     */
    public static AssetAllocation balanced() {
        return new AssetAllocation(
                new BigDecimal("60"),
                new BigDecimal("40"),
                BigDecimal.ZERO
        );
    }

    /**
     * Creates a new builder for AssetAllocation.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the percentage allocated to stocks.
     *
     * @return stocks percentage (0-100)
     */
    public BigDecimal getStocksPercentage() {
        return stocksPercentage;
    }

    /**
     * Returns the percentage allocated to bonds.
     *
     * @return bonds percentage (0-100)
     */
    public BigDecimal getBondsPercentage() {
        return bondsPercentage;
    }

    /**
     * Returns the percentage allocated to cash.
     *
     * @return cash percentage (0-100)
     */
    public BigDecimal getCashPercentage() {
        return cashPercentage;
    }

    /**
     * Calculates a blended return rate based on this allocation.
     *
     * @param stockReturn expected annual return for stocks (as decimal, e.g., 0.07 for 7%)
     * @param bondReturn expected annual return for bonds (as decimal)
     * @param cashReturn expected annual return for cash (as decimal)
     * @return the weighted average return rate
     */
    public BigDecimal calculateBlendedReturn(BigDecimal stockReturn, BigDecimal bondReturn,
                                             BigDecimal cashReturn) {
        BigDecimal stockContribution = stocksPercentage
                .multiply(stockReturn)
                .divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
        BigDecimal bondContribution = bondsPercentage
                .multiply(bondReturn)
                .divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
        BigDecimal cashContribution = cashPercentage
                .multiply(cashReturn)
                .divide(HUNDRED, SCALE, RoundingMode.HALF_UP);

        return stockContribution.add(bondContribution).add(cashContribution);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AssetAllocation that = (AssetAllocation) o;
        return stocksPercentage.compareTo(that.stocksPercentage) == 0
                && bondsPercentage.compareTo(that.bondsPercentage) == 0
                && cashPercentage.compareTo(that.cashPercentage) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                stocksPercentage.stripTrailingZeros(),
                bondsPercentage.stripTrailingZeros(),
                cashPercentage.stripTrailingZeros()
        );
    }

    @Override
    public String toString() {
        return String.format("AssetAllocation{stocks=%s%%, bonds=%s%%, cash=%s%%}",
                stocksPercentage.stripTrailingZeros().toPlainString(),
                bondsPercentage.stripTrailingZeros().toPlainString(),
                cashPercentage.stripTrailingZeros().toPlainString());
    }

    /**
     * Builder for creating AssetAllocation instances.
     */
    public static class Builder {
        private BigDecimal stocks = BigDecimal.ZERO;
        private BigDecimal bonds = BigDecimal.ZERO;
        private BigDecimal cash = BigDecimal.ZERO;

        /**
         * Sets the stocks percentage.
         *
         * @param percentage stocks allocation (0-100)
         * @return this builder
         */
        public Builder stocks(double percentage) {
            this.stocks = BigDecimal.valueOf(percentage);
            return this;
        }

        /**
         * Sets the bonds percentage.
         *
         * @param percentage bonds allocation (0-100)
         * @return this builder
         */
        public Builder bonds(double percentage) {
            this.bonds = BigDecimal.valueOf(percentage);
            return this;
        }

        /**
         * Sets the cash percentage.
         *
         * @param percentage cash allocation (0-100)
         * @return this builder
         */
        public Builder cash(double percentage) {
            this.cash = BigDecimal.valueOf(percentage);
            return this;
        }

        /**
         * Builds the AssetAllocation instance.
         *
         * @return a new AssetAllocation
         * @throws IllegalArgumentException if validation fails
         */
        public AssetAllocation build() {
            validate();
            return new AssetAllocation(stocks, bonds, cash);
        }

        private void validate() {
            validateRange("Stocks", stocks);
            validateRange("Bonds", bonds);
            validateRange("Cash", cash);

            BigDecimal total = stocks.add(bonds).add(cash);
            if (total.compareTo(HUNDRED) != 0) {
                throw new IllegalArgumentException(
                        String.format("Asset allocation must sum to 100%%, but was %s%%",
                                total.stripTrailingZeros().toPlainString()));
            }
        }

        private void validateRange(String name, BigDecimal value) {
            if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(HUNDRED) > 0) {
                throw new IllegalArgumentException(
                        String.format("%s percentage must be between 0 and 100, but was %s",
                                name, value.stripTrailingZeros().toPlainString()));
            }
        }
    }
}
