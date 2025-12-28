package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

import io.github.xmljim.retirement.domain.annotation.Generated;
import io.github.xmljim.retirement.domain.enums.ExpenseCategory;
import io.github.xmljim.retirement.domain.enums.ExpenseFrequency;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;

/**
 * Represents a recurring expense in retirement.
 *
 * <p>Models regular expenses such as housing, utilities, insurance, and
 * subscriptions. Supports different frequencies (monthly, quarterly, annual)
 * and inflation adjustments based on expense category.
 *
 * <p>This is an immutable value object. Use the {@link Builder} to create instances.
 */
public final class RecurringExpense {

    private static final MathContext MATH_CONTEXT = new MathContext(10, RoundingMode.HALF_UP);
    private static final int MONTHS_PER_YEAR = 12;

    private final String name;
    private final ExpenseCategory category;
    private final BigDecimal amount;
    private final ExpenseFrequency frequency;
    private final LocalDate startDate;
    private final Optional<LocalDate> endDate;
    private final Optional<BigDecimal> inflationRate;
    private final boolean useDefaultInflation;

    private RecurringExpense(Builder builder) {
        this.name = builder.name;
        this.category = builder.category;
        this.amount = builder.amount;
        this.frequency = builder.frequency;
        this.startDate = builder.startDate;
        this.endDate = Optional.ofNullable(builder.endDate);
        this.inflationRate = Optional.ofNullable(builder.inflationRate);
        this.useDefaultInflation = builder.useDefaultInflation;
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
     * Returns the base amount at the specified frequency.
     *
     * @return the base amount
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Returns the frequency of this expense.
     *
     * @return the frequency
     */
    public ExpenseFrequency getFrequency() {
        return frequency;
    }

    /**
     * Returns the start date of this expense.
     *
     * @return the start date
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * Returns the end date, if applicable.
     *
     * @return optional containing end date, or empty if ongoing
     */
    public Optional<LocalDate> getEndDate() {
        return endDate;
    }

    /**
     * Returns the custom inflation rate, if set.
     *
     * @return optional containing the rate, or empty if using default
     */
    public Optional<BigDecimal> getInflationRate() {
        return inflationRate;
    }

    /**
     * Returns whether this expense uses the default category inflation rate.
     *
     * @return true if using default inflation
     */
    public boolean isUseDefaultInflation() {
        return useDefaultInflation;
    }

    /**
     * Returns the base monthly amount before any inflation adjustment.
     *
     * @return the base monthly amount
     */
    public BigDecimal getBaseMonthlyAmount() {
        return frequency.toMonthly(amount);
    }

    /**
     * Returns the base annual amount before any inflation adjustment.
     *
     * @return the base annual amount
     */
    public BigDecimal getBaseAnnualAmount() {
        return frequency.toAnnual(amount);
    }

    /**
     * Returns the inflation-adjusted monthly amount for a date.
     *
     * @param asOfDate the date to calculate for
     * @return the adjusted monthly amount, or zero if inactive
     */
    public BigDecimal getMonthlyAmount(LocalDate asOfDate) {
        return getMonthlyAmount(asOfDate, null);
    }

    /**
     * Returns the inflation-adjusted monthly amount using a default rate.
     *
     * @param asOfDate the date to calculate for
     * @param defaultRate the default rate to use if configured
     * @return the adjusted monthly amount, or zero if inactive
     */
    public BigDecimal getMonthlyAmount(LocalDate asOfDate, BigDecimal defaultRate) {
        if (!isActive(asOfDate)) {
            return BigDecimal.ZERO;
        }

        BigDecimal baseMonthly = getBaseMonthlyAmount();

        return getEffectiveInflationRate(defaultRate)
                .filter(rate -> rate.compareTo(BigDecimal.ZERO) != 0)
                .map(rate -> {
                    long monthsElapsed = ChronoUnit.MONTHS.between(startDate, asOfDate);
                    int yearsElapsed = (int) (monthsElapsed / MONTHS_PER_YEAR);
                    if (yearsElapsed <= 0) {
                        return baseMonthly;
                    }
                    BigDecimal multiplier = BigDecimal.ONE.add(rate).pow(yearsElapsed, MATH_CONTEXT);
                    return baseMonthly.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
                })
                .orElse(baseMonthly);
    }

    /**
     * Returns the inflation-adjusted annual amount for a date.
     *
     * @param asOfDate the date to calculate for
     * @return the adjusted annual amount
     */
    public BigDecimal getAnnualAmount(LocalDate asOfDate) {
        return getMonthlyAmount(asOfDate).multiply(BigDecimal.valueOf(MONTHS_PER_YEAR));
    }

    /**
     * Returns the inflation-adjusted annual amount using a default rate.
     *
     * @param asOfDate the date to calculate for
     * @param defaultRate the default rate to use if configured
     * @return the adjusted annual amount
     */
    public BigDecimal getAnnualAmount(LocalDate asOfDate, BigDecimal defaultRate) {
        return getMonthlyAmount(asOfDate, defaultRate).multiply(BigDecimal.valueOf(MONTHS_PER_YEAR));
    }

    /**
     * Returns whether this expense is active on the given date.
     *
     * @param asOfDate the date to check
     * @return true if expense is active
     */
    public boolean isActive(LocalDate asOfDate) {
        if (asOfDate.isBefore(startDate)) {
            return false;
        }
        return endDate.map(end -> !asOfDate.isAfter(end)).orElse(true);
    }

    /**
     * Returns whether this expense is ongoing (no end date).
     *
     * @return true if no end date
     */
    public boolean isOngoing() {
        return endDate.isEmpty();
    }

    /**
     * Returns whether this expense is subject to inflation adjustment.
     *
     * @return true if inflation adjusted
     */
    public boolean isInflationAdjusted() {
        if (!useDefaultInflation) {
            return inflationRate
                    .map(rate -> rate.compareTo(BigDecimal.ZERO) != 0)
                    .orElse(false);
        }
        return category.isInflationAdjusted();
    }

    private Optional<BigDecimal> getEffectiveInflationRate(BigDecimal defaultRate) {
        if (!useDefaultInflation) {
            return inflationRate;
        }
        return Optional.ofNullable(defaultRate).or(() -> inflationRate);
    }

    /**
     * Creates a new builder for RecurringExpense.
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
        RecurringExpense that = (RecurringExpense) o;
        return useDefaultInflation == that.useDefaultInflation
            && Objects.equals(name, that.name)
            && category == that.category
            && amount.compareTo(that.amount) == 0
            && frequency == that.frequency
            && Objects.equals(startDate, that.startDate)
            && endDate.equals(that.endDate)
            && inflationRate.equals(that.inflationRate);
    }

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(name, category, amount, frequency, startDate, endDate, inflationRate, useDefaultInflation);
    }

    @Generated
    @Override
    public String toString() {
        return "RecurringExpense{name='" + name + "', category=" + category + ", amount=" + amount
            + ", frequency=" + frequency + ", startDate=" + startDate
            + ", endDate=" + endDate.orElse(null) + '}';
    }

    /**
     * Builder for creating RecurringExpense instances.
     */
    public static class Builder {
        private String name;
        private ExpenseCategory category;
        private BigDecimal amount = BigDecimal.ZERO;
        private ExpenseFrequency frequency = ExpenseFrequency.MONTHLY;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal inflationRate;
        private boolean useDefaultInflation = true;

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
         * Sets the amount.
         *
         * @param amount the amount
         * @return this builder
         */
        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        /**
         * Sets the amount.
         *
         * @param amount the amount
         * @return this builder
         */
        public Builder amount(double amount) {
            return amount(BigDecimal.valueOf(amount));
        }

        /**
         * Sets the frequency.
         *
         * @param frequency the frequency
         * @return this builder
         */
        public Builder frequency(ExpenseFrequency frequency) {
            this.frequency = frequency;
            return this;
        }

        /**
         * Sets the start date.
         *
         * @param date the start date
         * @return this builder
         */
        public Builder startDate(LocalDate date) {
            this.startDate = date;
            return this;
        }

        /**
         * Sets the end date.
         *
         * @param date the end date or null for ongoing
         * @return this builder
         */
        public Builder endDate(LocalDate date) {
            this.endDate = date;
            return this;
        }

        /**
         * Sets a custom inflation rate.
         *
         * @param rate the rate as decimal
         * @return this builder
         */
        public Builder inflationRate(BigDecimal rate) {
            this.inflationRate = rate;
            this.useDefaultInflation = false;
            return this;
        }

        /**
         * Sets a custom inflation rate.
         *
         * @param rate the rate as decimal
         * @return this builder
         */
        public Builder inflationRate(double rate) {
            return inflationRate(BigDecimal.valueOf(rate));
        }

        /**
         * Sets whether to use default category inflation.
         *
         * @param useDefault true to use category default
         * @return this builder
         */
        public Builder useDefaultInflation(boolean useDefault) {
            this.useDefaultInflation = useDefault;
            return this;
        }

        /**
         * Disables inflation adjustment for this expense.
         *
         * @return this builder
         */
        public Builder noInflation() {
            this.inflationRate = BigDecimal.ZERO;
            this.useDefaultInflation = false;
            return this;
        }

        /**
         * Builds the RecurringExpense instance.
         *
         * @return a new RecurringExpense
         * @throws MissingRequiredFieldException if required fields missing
         * @throws ValidationException if validation fails
         */
        public RecurringExpense build() {
            validate();
            return new RecurringExpense(this);
        }

        private void validate() {
            MissingRequiredFieldException.requireNonNull(name, "name");
            ValidationException.validate("name", name, n -> !n.isBlank(), "Name cannot be blank");
            MissingRequiredFieldException.requireNonNull(category, "category");
            MissingRequiredFieldException.requireNonNull(startDate, "startDate");
            ValidationException.validate("amount", amount, v -> v.compareTo(BigDecimal.ZERO) >= 0,
                "Amount cannot be negative");
            if (endDate != null) {
                ValidationException.validate("endDate", endDate, d -> !d.isBefore(startDate),
                    "End date cannot be before start date");
            }
        }
    }
}
