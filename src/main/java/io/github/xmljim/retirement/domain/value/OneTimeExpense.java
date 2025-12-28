package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

import io.github.xmljim.retirement.domain.annotation.Generated;
import io.github.xmljim.retirement.domain.enums.ExpenseCategory;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;

/**
 * Represents a one-time expense in retirement planning.
 *
 * <p>Models planned, known expenses with specific dates such as:
 * <ul>
 *   <li>New car purchase in 5 years</li>
 *   <li>Home renovation next year</li>
 *   <li>Special vacation</li>
 *   <li>Wedding expenses</li>
 *   <li>Major appliance replacement</li>
 * </ul>
 *
 * <p>For future expenses, the amount can optionally be adjusted for inflation
 * from a base date. The expense only applies in the month of the target date.
 *
 * <p>This is an immutable value object. Use the {@link Builder} to create instances.
 *
 * @see RecurringExpense
 */
public final class OneTimeExpense {

    private static final MathContext MATH_CONTEXT = new MathContext(10, RoundingMode.HALF_UP);
    private static final int MONTHS_PER_YEAR = 12;

    private final String name;
    private final ExpenseCategory category;
    private final BigDecimal baseAmount;
    private final LocalDate targetDate;
    private final Optional<LocalDate> baseDate;
    private final Optional<BigDecimal> inflationRate;

    private OneTimeExpense(Builder builder) {
        this.name = builder.name;
        this.category = builder.category;
        this.baseAmount = builder.baseAmount;
        this.targetDate = builder.targetDate;
        this.baseDate = Optional.ofNullable(builder.baseDate);
        this.inflationRate = Optional.ofNullable(builder.inflationRate);
    }

    /**
     * Returns the name or description of this expense.
     *
     * @return the expense name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the expense category.
     *
     * @return the category
     */
    public ExpenseCategory getCategory() {
        return category;
    }

    /**
     * Returns the base amount before any inflation adjustment.
     *
     * @return the base amount
     */
    public BigDecimal getBaseAmount() {
        return baseAmount;
    }

    /**
     * Returns the target date when this expense occurs.
     *
     * @return the target date
     */
    public LocalDate getTargetDate() {
        return targetDate;
    }

    /**
     * Returns the base date for inflation calculations, if set.
     *
     * @return optional containing base date, or empty if not set
     */
    public Optional<LocalDate> getBaseDate() {
        return baseDate;
    }

    /**
     * Returns the custom inflation rate, if set.
     *
     * @return optional containing the rate, or empty if not set
     */
    public Optional<BigDecimal> getInflationRate() {
        return inflationRate;
    }

    /**
     * Returns the inflation-adjusted amount at the target date.
     *
     * <p>If inflation rate and base date are set, calculates the
     * inflated amount from base date to target date.
     *
     * @return the adjusted amount
     */
    public BigDecimal getAdjustedAmount() {
        return getAdjustedAmount(null);
    }

    /**
     * Returns the inflation-adjusted amount using a default rate.
     *
     * @param defaultRate the default inflation rate to use if not set
     * @return the adjusted amount
     */
    public BigDecimal getAdjustedAmount(BigDecimal defaultRate) {
        return getEffectiveInflationRate(defaultRate)
                .filter(rate -> rate.compareTo(BigDecimal.ZERO) != 0)
                .flatMap(rate -> baseDate.map(base -> calculateInflatedAmount(base, rate)))
                .orElse(baseAmount);
    }

    /**
     * Returns the expense amount for a given month.
     *
     * <p>Returns the adjusted amount only if the given date falls in the
     * same month as the target date. Returns zero for all other months.
     *
     * @param asOfDate the date to check
     * @return the expense amount if in target month, zero otherwise
     */
    public BigDecimal getAmountForMonth(LocalDate asOfDate) {
        return getAmountForMonth(asOfDate, null);
    }

    /**
     * Returns the expense amount for a given month using a default rate.
     *
     * @param asOfDate the date to check
     * @param defaultRate the default inflation rate
     * @return the expense amount if in target month, zero otherwise
     */
    public BigDecimal getAmountForMonth(LocalDate asOfDate, BigDecimal defaultRate) {
        if (isInTargetMonth(asOfDate)) {
            return getAdjustedAmount(defaultRate);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Returns whether the given date is in the same month as the target date.
     *
     * @param asOfDate the date to check
     * @return true if in target month
     */
    public boolean isInTargetMonth(LocalDate asOfDate) {
        YearMonth target = YearMonth.from(targetDate);
        YearMonth check = YearMonth.from(asOfDate);
        return target.equals(check);
    }

    /**
     * Returns whether this expense is in the future relative to the given date.
     *
     * @param asOfDate the reference date
     * @return true if target date is after the given date
     */
    public boolean isFuture(LocalDate asOfDate) {
        return targetDate.isAfter(asOfDate);
    }

    /**
     * Returns whether this expense is in the past relative to the given date.
     *
     * @param asOfDate the reference date
     * @return true if target date is before the given date
     */
    public boolean isPast(LocalDate asOfDate) {
        return targetDate.isBefore(asOfDate);
    }

    private BigDecimal calculateInflatedAmount(LocalDate fromDate, BigDecimal rate) {
        long monthsElapsed = ChronoUnit.MONTHS.between(fromDate, targetDate);
        int yearsElapsed = (int) (monthsElapsed / MONTHS_PER_YEAR);

        if (yearsElapsed <= 0) {
            return baseAmount;
        }

        BigDecimal multiplier = BigDecimal.ONE.add(rate).pow(yearsElapsed, MATH_CONTEXT);
        return baseAmount.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    private Optional<BigDecimal> getEffectiveInflationRate(BigDecimal defaultRate) {
        return inflationRate.or(() -> Optional.ofNullable(defaultRate));
    }

    /**
     * Creates a new builder for OneTimeExpense.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    @Generated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OneTimeExpense that = (OneTimeExpense) o;
        return Objects.equals(name, that.name)
            && category == that.category
            && baseAmount.compareTo(that.baseAmount) == 0
            && Objects.equals(targetDate, that.targetDate)
            && baseDate.equals(that.baseDate)
            && inflationRate.equals(that.inflationRate);
    }

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(name, category, baseAmount, targetDate, baseDate, inflationRate);
    }

    @Generated
    @Override
    public String toString() {
        return "OneTimeExpense{name='" + name + "', category=" + category
            + ", baseAmount=" + baseAmount + ", targetDate=" + targetDate + '}';
    }

    /**
     * Builder for creating OneTimeExpense instances.
     */
    public static class Builder {
        private String name;
        private ExpenseCategory category;
        private BigDecimal baseAmount = BigDecimal.ZERO;
        private LocalDate targetDate;
        private LocalDate baseDate;
        private BigDecimal inflationRate;

        /**
         * Sets the expense name.
         *
         * @param name the name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the expense category.
         *
         * @param category the category
         * @return this builder
         */
        public Builder category(ExpenseCategory category) {
            this.category = category;
            return this;
        }

        /**
         * Sets the base amount.
         *
         * @param amount the amount
         * @return this builder
         */
        public Builder amount(BigDecimal amount) {
            this.baseAmount = amount;
            return this;
        }

        /**
         * Sets the base amount.
         *
         * @param amount the amount
         * @return this builder
         */
        public Builder amount(double amount) {
            return amount(BigDecimal.valueOf(amount));
        }

        /**
         * Sets the target date when the expense occurs.
         *
         * @param date the target date
         * @return this builder
         */
        public Builder targetDate(LocalDate date) {
            this.targetDate = date;
            return this;
        }

        /**
         * Sets the base date for inflation calculations.
         *
         * <p>If set along with inflation rate, the amount will be
         * adjusted from this date to the target date.
         *
         * @param date the base date
         * @return this builder
         */
        public Builder baseDate(LocalDate date) {
            this.baseDate = date;
            return this;
        }

        /**
         * Sets the inflation rate for future expense adjustment.
         *
         * @param rate the rate as decimal (e.g., 0.03 for 3%)
         * @return this builder
         */
        public Builder inflationRate(BigDecimal rate) {
            this.inflationRate = rate;
            return this;
        }

        /**
         * Sets the inflation rate for future expense adjustment.
         *
         * @param rate the rate as decimal
         * @return this builder
         */
        public Builder inflationRate(double rate) {
            return inflationRate(BigDecimal.valueOf(rate));
        }

        /**
         * Builds the OneTimeExpense instance.
         *
         * @return a new OneTimeExpense
         * @throws MissingRequiredFieldException if required fields missing
         * @throws ValidationException if validation fails
         */
        public OneTimeExpense build() {
            validate();
            return new OneTimeExpense(this);
        }

        private void validate() {
            MissingRequiredFieldException.requireNonNull(name, "name");
            ValidationException.validate("name", name, n -> !n.isBlank(), "Name cannot be blank");
            MissingRequiredFieldException.requireNonNull(category, "category");
            MissingRequiredFieldException.requireNonNull(targetDate, "targetDate");
            ValidationException.validate("baseAmount", baseAmount,
                v -> v.compareTo(BigDecimal.ZERO) >= 0, "Amount cannot be negative");
        }
    }
}
