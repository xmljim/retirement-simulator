package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Breakdown of income by source type for a given date.
 *
 * <p>Provides a detailed view of all income sources and their amounts,
 * used for cash flow analysis and tax planning in retirement simulations.
 *
 * @param asOfDate the date for which income is calculated
 * @param salary working income (pre-retirement)
 * @param socialSecurity Social Security benefits
 * @param pension defined benefit pension income
 * @param annuity annuity income
 * @param other other income sources (rental, dividends, etc.)
 * @param total total of all income sources
 * @param earnedIncome income subject to Social Security earnings test
 * @param passiveIncome income not subject to earnings test
 */
public record IncomeBreakdown(
    LocalDate asOfDate,
    BigDecimal salary,
    BigDecimal socialSecurity,
    BigDecimal pension,
    BigDecimal annuity,
    BigDecimal other,
    BigDecimal total,
    BigDecimal earnedIncome,
    BigDecimal passiveIncome
) {
    /**
     * Compact constructor ensuring non-null values.
     */
    public IncomeBreakdown {
        Objects.requireNonNull(asOfDate, "asOfDate is required");
        salary = salary != null ? salary : BigDecimal.ZERO;
        socialSecurity = socialSecurity != null ? socialSecurity : BigDecimal.ZERO;
        pension = pension != null ? pension : BigDecimal.ZERO;
        annuity = annuity != null ? annuity : BigDecimal.ZERO;
        other = other != null ? other : BigDecimal.ZERO;
        total = total != null ? total : BigDecimal.ZERO;
        earnedIncome = earnedIncome != null ? earnedIncome : BigDecimal.ZERO;
        passiveIncome = passiveIncome != null ? passiveIncome : BigDecimal.ZERO;
    }

    /**
     * Creates an empty breakdown for the given date.
     *
     * @param date the date
     * @return an empty breakdown with all amounts at zero
     */
    public static IncomeBreakdown empty(LocalDate date) {
        return new IncomeBreakdown(
            date,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO
        );
    }

    /**
     * Returns whether there is any income.
     *
     * @return true if total is greater than zero
     */
    public boolean hasIncome() {
        return total.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Returns annual amounts by multiplying monthly by 12.
     *
     * @return a new breakdown with annual amounts
     */
    public IncomeBreakdown toAnnual() {
        BigDecimal twelve = BigDecimal.valueOf(12);
        return new IncomeBreakdown(
            asOfDate,
            salary.multiply(twelve),
            socialSecurity.multiply(twelve),
            pension.multiply(twelve),
            annuity.multiply(twelve),
            other.multiply(twelve),
            total.multiply(twelve),
            earnedIncome.multiply(twelve),
            passiveIncome.multiply(twelve)
        );
    }

    /**
     * Builder for creating IncomeBreakdown instances.
     */
    public static class Builder {
        private LocalDate asOfDate;
        private BigDecimal salary = BigDecimal.ZERO;
        private BigDecimal socialSecurity = BigDecimal.ZERO;
        private BigDecimal pension = BigDecimal.ZERO;
        private BigDecimal annuity = BigDecimal.ZERO;
        private BigDecimal other = BigDecimal.ZERO;
        private BigDecimal earnedIncome = BigDecimal.ZERO;
        private BigDecimal passiveIncome = BigDecimal.ZERO;

        /** Sets the date. @param date the date @return this builder */
        public Builder asOfDate(LocalDate date) {
            this.asOfDate = date;
            return this;
        }

        /** Sets salary amount. @param amount the amount @return this builder */
        public Builder salary(BigDecimal amount) {
            this.salary = amount != null ? amount : BigDecimal.ZERO;
            return this;
        }

        /** Sets Social Security amount. @param amount the amount @return this builder */
        public Builder socialSecurity(BigDecimal amount) {
            this.socialSecurity = amount != null ? amount : BigDecimal.ZERO;
            return this;
        }

        /** Sets pension amount. @param amount the amount @return this builder */
        public Builder pension(BigDecimal amount) {
            this.pension = amount != null ? amount : BigDecimal.ZERO;
            return this;
        }

        /** Sets annuity amount. @param amount the amount @return this builder */
        public Builder annuity(BigDecimal amount) {
            this.annuity = amount != null ? amount : BigDecimal.ZERO;
            return this;
        }

        /** Sets other income amount. @param amount the amount @return this builder */
        public Builder other(BigDecimal amount) {
            this.other = amount != null ? amount : BigDecimal.ZERO;
            return this;
        }

        /** Sets earned income amount. @param amount the amount @return this builder */
        public Builder earnedIncome(BigDecimal amount) {
            this.earnedIncome = amount != null ? amount : BigDecimal.ZERO;
            return this;
        }

        /** Sets passive income amount. @param amount the amount @return this builder */
        public Builder passiveIncome(BigDecimal amount) {
            this.passiveIncome = amount != null ? amount : BigDecimal.ZERO;
            return this;
        }

        /** Adds to salary. @param amount the amount to add @return this builder */
        public Builder addToSalary(BigDecimal amount) {
            if (amount != null) {
                this.salary = this.salary.add(amount);
            }
            return this;
        }

        /** Adds to Social Security. @param amount the amount to add @return this builder */
        public Builder addToSocialSecurity(BigDecimal amount) {
            if (amount != null) {
                this.socialSecurity = this.socialSecurity.add(amount);
            }
            return this;
        }

        /** Adds to pension. @param amount the amount to add @return this builder */
        public Builder addToPension(BigDecimal amount) {
            if (amount != null) {
                this.pension = this.pension.add(amount);
            }
            return this;
        }

        /** Adds to annuity. @param amount the amount to add @return this builder */
        public Builder addToAnnuity(BigDecimal amount) {
            if (amount != null) {
                this.annuity = this.annuity.add(amount);
            }
            return this;
        }

        /** Adds to other income. @param amount the amount to add @return this builder */
        public Builder addToOther(BigDecimal amount) {
            if (amount != null) {
                this.other = this.other.add(amount);
            }
            return this;
        }

        /** Adds to earned income. @param amount the amount to add @return this builder */
        public Builder addToEarnedIncome(BigDecimal amount) {
            if (amount != null) {
                this.earnedIncome = this.earnedIncome.add(amount);
            }
            return this;
        }

        /** Adds to passive income. @param amount the amount to add @return this builder */
        public Builder addToPassiveIncome(BigDecimal amount) {
            if (amount != null) {
                this.passiveIncome = this.passiveIncome.add(amount);
            }
            return this;
        }

        /** Builds the IncomeBreakdown. @return a new IncomeBreakdown */
        public IncomeBreakdown build() {
            BigDecimal total = salary
                .add(socialSecurity)
                .add(pension)
                .add(annuity)
                .add(other);

            return new IncomeBreakdown(
                asOfDate,
                salary,
                socialSecurity,
                pension,
                annuity,
                other,
                total,
                earnedIncome,
                passiveIncome
            );
        }
    }

    /**
     * Creates a new builder.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}
