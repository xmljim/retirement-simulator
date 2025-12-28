package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

import io.github.xmljim.retirement.domain.annotation.Generated;
import io.github.xmljim.retirement.domain.enums.LtcTriggerMode;
import io.github.xmljim.retirement.domain.exception.ValidationException;

/**
 * Long-Term Care (LTC) insurance policy configuration.
 *
 * <p>Models LTC insurance with premium payments and benefit calculations.
 * Premiums are paid during working/early retirement years; benefits are
 * triggered when unable to perform 2+ ADLs or cognitive impairment.
 *
 * @see LtcTriggerMode
 * @see <a href="https://www.aaltci.org">AALTCI</a>
 */
public final class LtcInsurance {

    private static final int DAYS_PER_YEAR = 365;
    private static final int MONTHS_PER_YEAR = 12;
    private static final int SCALE = 10;

    private final String policyName;
    private final BigDecimal annualPremium;
    private final BigDecimal dailyBenefit;
    private final int benefitPeriodYears;
    private final int eliminationDays;
    private final BigDecimal inflationRate;
    private final LocalDate premiumStartDate;
    private final LocalDate premiumEndDate;
    private final LtcTriggerMode triggerMode;
    private final Integer deterministicTriggerAge;

    private LtcInsurance(Builder builder) {
        this.policyName = builder.policyName;
        this.annualPremium = builder.annualPremium;
        this.dailyBenefit = builder.dailyBenefit;
        this.benefitPeriodYears = builder.benefitPeriodYears;
        this.eliminationDays = builder.eliminationDays;
        this.inflationRate = builder.inflationRate;
        this.premiumStartDate = builder.premiumStartDate;
        this.premiumEndDate = builder.premiumEndDate;
        this.triggerMode = builder.triggerMode;
        this.deterministicTriggerAge = builder.deterministicTriggerAge;
    }

    public String getPolicyName() {
        return policyName;
    }

    public BigDecimal getAnnualPremium() {
        return annualPremium;
    }

    public BigDecimal getMonthlyPremium() {
        return annualPremium.divide(BigDecimal.valueOf(MONTHS_PER_YEAR), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getDailyBenefit() {
        return dailyBenefit;
    }

    public int getBenefitPeriodYears() {
        return benefitPeriodYears;
    }

    public boolean isUnlimitedBenefit() {
        return benefitPeriodYears == 0;
    }

    public int getEliminationDays() {
        return eliminationDays;
    }

    public BigDecimal getInflationRate() {
        return inflationRate;
    }

    public LocalDate getPremiumStartDate() {
        return premiumStartDate;
    }

    public Optional<LocalDate> getPremiumEndDate() {
        return Optional.ofNullable(premiumEndDate);
    }

    public boolean isPaidUp() {
        return premiumEndDate != null;
    }

    public LtcTriggerMode getTriggerMode() {
        return triggerMode;
    }

    public Optional<Integer> getDeterministicTriggerAge() {
        return Optional.ofNullable(deterministicTriggerAge);
    }

    /**
     * Returns premium for given date (0 if outside premium period or paid-up).
     */
    public BigDecimal getMonthlyPremium(LocalDate date) {
        if (date.isBefore(premiumStartDate)) {
            return BigDecimal.ZERO;
        }
        if (premiumEndDate != null && date.isAfter(premiumEndDate)) {
            return BigDecimal.ZERO;
        }
        return getMonthlyPremium();
    }

    /**
     * Returns initial benefit pool (dailyBenefit × 365 × years).
     * Returns null for unlimited benefit policies.
     */
    public Optional<BigDecimal> getInitialBenefitPool() {
        if (isUnlimitedBenefit()) {
            return Optional.empty();
        }
        return Optional.of(dailyBenefit
            .multiply(BigDecimal.valueOf(DAYS_PER_YEAR))
            .multiply(BigDecimal.valueOf(benefitPeriodYears)));
    }

    /**
     * Returns daily benefit adjusted for inflation from policy start.
     */
    public BigDecimal getDailyBenefit(LocalDate asOfDate) {
        long yearsElapsed = ChronoUnit.YEARS.between(premiumStartDate, asOfDate);
        if (yearsElapsed <= 0) {
            return dailyBenefit;
        }
        BigDecimal multiplier = BigDecimal.ONE.add(inflationRate)
            .pow((int) yearsElapsed, java.math.MathContext.DECIMAL128);
        return dailyBenefit.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Returns monthly benefit adjusted for inflation.
     */
    public BigDecimal getMonthlyBenefit(LocalDate asOfDate) {
        return getDailyBenefit(asOfDate)
            .multiply(BigDecimal.valueOf(30))
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Returns benefit pool adjusted for inflation.
     */
    public Optional<BigDecimal> getBenefitPool(LocalDate asOfDate) {
        if (isUnlimitedBenefit()) {
            return Optional.empty();
        }
        BigDecimal adjustedDaily = getDailyBenefit(asOfDate);
        return Optional.of(adjustedDaily
            .multiply(BigDecimal.valueOf(DAYS_PER_YEAR))
            .multiply(BigDecimal.valueOf(benefitPeriodYears)));
    }

    /**
     * Returns remaining benefits after claims.
     */
    public Optional<BigDecimal> getRemainingBenefits(BigDecimal claimsPaid, LocalDate asOfDate) {
        return getBenefitPool(asOfDate)
            .map(pool -> pool.subtract(claimsPaid).max(BigDecimal.ZERO));
    }

    /**
     * Returns true if benefits are exhausted.
     */
    public boolean isBenefitExhausted(BigDecimal claimsPaid, LocalDate asOfDate) {
        return getRemainingBenefits(claimsPaid, asOfDate)
            .map(remaining -> remaining.compareTo(BigDecimal.ZERO) <= 0)
            .orElse(false);
    }

    /**
     * Creates a new builder for LtcInsurance.
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
        LtcInsurance that = (LtcInsurance) o;
        return Objects.equals(policyName, that.policyName)
            && annualPremium.compareTo(that.annualPremium) == 0
            && dailyBenefit.compareTo(that.dailyBenefit) == 0;
    }

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(policyName, annualPremium, dailyBenefit);
    }

    @Generated
    @Override
    public String toString() {
        return "LtcInsurance{policyName='" + policyName + "', dailyBenefit=" + dailyBenefit + "}";
    }

    /**
     * Builder for creating LtcInsurance instances.
     */
    public static class Builder {
        private String policyName = "LTC Policy";
        private BigDecimal annualPremium = BigDecimal.ZERO;
        private BigDecimal dailyBenefit = BigDecimal.ZERO;
        private int benefitPeriodYears = 3;
        private int eliminationDays = 90;
        private BigDecimal inflationRate = new BigDecimal("0.03");
        private LocalDate premiumStartDate = LocalDate.now();
        private LocalDate premiumEndDate;
        private LtcTriggerMode triggerMode = LtcTriggerMode.DETERMINISTIC;
        private Integer deterministicTriggerAge;

        /** Sets the policy name. @param name the name @return this builder */
        public Builder policyName(String name) {
            this.policyName = name;
            return this;
        }

        /** Sets the annual premium. @param premium the premium @return this builder */
        public Builder annualPremium(BigDecimal premium) {
            this.annualPremium = premium;
            return this;
        }

        /** Sets the annual premium. @param premium the premium @return this builder */
        public Builder annualPremium(double premium) {
            return annualPremium(BigDecimal.valueOf(premium));
        }

        /** Sets the daily benefit. @param benefit the benefit @return this builder */
        public Builder dailyBenefit(BigDecimal benefit) {
            this.dailyBenefit = benefit;
            return this;
        }

        /** Sets the daily benefit. @param benefit the benefit @return this builder */
        public Builder dailyBenefit(double benefit) {
            return dailyBenefit(BigDecimal.valueOf(benefit));
        }

        /** Sets the benefit period in years. @param years the years @return this builder */
        public Builder benefitPeriodYears(int years) {
            this.benefitPeriodYears = years;
            return this;
        }

        /** Sets unlimited benefit period. @return this builder */
        public Builder unlimitedBenefit() {
            this.benefitPeriodYears = 0;
            return this;
        }

        /** Sets the elimination days. @param days the days @return this builder */
        public Builder eliminationDays(int days) {
            this.eliminationDays = days;
            return this;
        }

        /** Sets the inflation rate. @param rate the rate @return this builder */
        public Builder inflationRate(BigDecimal rate) {
            this.inflationRate = rate;
            return this;
        }

        /** Sets the inflation rate. @param rate the rate @return this builder */
        public Builder inflationRate(double rate) {
            return inflationRate(BigDecimal.valueOf(rate));
        }

        /** Sets the premium start date. @param date the date @return this builder */
        public Builder premiumStartDate(LocalDate date) {
            this.premiumStartDate = date;
            return this;
        }

        /** Sets the premium end date. @param date the date @return this builder */
        public Builder premiumEndDate(LocalDate date) {
            this.premiumEndDate = date;
            return this;
        }

        /** Sets the trigger mode. @param mode the mode @return this builder */
        public Builder triggerMode(LtcTriggerMode mode) {
            this.triggerMode = mode;
            return this;
        }

        /** Sets the deterministic trigger age. @param age the age @return this builder */
        public Builder deterministicTriggerAge(int age) {
            this.deterministicTriggerAge = age;
            return this;
        }

        /** Builds the LtcInsurance. @return the built instance */
        public LtcInsurance build() {
            validate();
            return new LtcInsurance(this);
        }

        private void validate() {
            if (annualPremium.compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("Annual premium cannot be negative", "annualPremium");
            }
            if (dailyBenefit.compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("Daily benefit cannot be negative", "dailyBenefit");
            }
            if (triggerMode == LtcTriggerMode.DETERMINISTIC && deterministicTriggerAge == null) {
                throw new ValidationException(
                    "Deterministic trigger age required for DETERMINISTIC mode", "deterministicTriggerAge");
            }
        }
    }
}
