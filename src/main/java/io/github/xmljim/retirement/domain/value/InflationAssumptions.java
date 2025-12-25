package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import io.github.xmljim.retirement.domain.exception.InvalidRateException;

/**
 * Represents inflation rate assumptions used in retirement projections.
 *
 * <p>This immutable value object contains different inflation rates for
 * various expense categories, allowing more accurate projection of
 * future costs.
 *
 * <p>Use the static factory methods or builder to create instances:
 * <pre>{@code
 * // Using defaults
 * InflationAssumptions defaults = InflationAssumptions.defaults();
 *
 * // Custom rates
 * InflationAssumptions custom = InflationAssumptions.builder()
 *     .generalInflation(0.03)
 *     .healthcareInflation(0.05)
 *     .housingInflation(0.025)
 *     .build();
 * }</pre>
 */
public final class InflationAssumptions {

    /**
     * Default general inflation rate (3.0%).
     */
    public static final BigDecimal DEFAULT_GENERAL_INFLATION = new BigDecimal("0.03");

    /**
     * Default healthcare inflation rate (5.0%).
     */
    public static final BigDecimal DEFAULT_HEALTHCARE_INFLATION = new BigDecimal("0.05");

    /**
     * Default housing inflation rate (2.5%).
     */
    public static final BigDecimal DEFAULT_HOUSING_INFLATION = new BigDecimal("0.025");

    private static final int SCALE = 6;
    private static final BigDecimal MIN_RATE = new BigDecimal("-0.10");
    private static final BigDecimal MAX_RATE = new BigDecimal("0.20");

    private final BigDecimal generalInflation;
    private final BigDecimal healthcareInflation;
    private final BigDecimal housingInflation;

    private InflationAssumptions(BigDecimal general, BigDecimal healthcare, BigDecimal housing) {
        this.generalInflation = general.setScale(SCALE, RoundingMode.HALF_UP);
        this.healthcareInflation = healthcare.setScale(SCALE, RoundingMode.HALF_UP);
        this.housingInflation = housing.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Creates inflation assumptions with default values.
     *
     * @return default inflation assumptions
     */
    public static InflationAssumptions defaults() {
        return new InflationAssumptions(
            DEFAULT_GENERAL_INFLATION,
            DEFAULT_HEALTHCARE_INFLATION,
            DEFAULT_HOUSING_INFLATION
        );
    }

    /**
     * Creates inflation assumptions with a single rate for all categories.
     *
     * @param rate the inflation rate to use (as decimal, e.g., 0.03 for 3%)
     * @return uniform inflation assumptions
     */
    public static InflationAssumptions uniform(double rate) {
        BigDecimal rateDecimal = BigDecimal.valueOf(rate);
        return new InflationAssumptions(rateDecimal, rateDecimal, rateDecimal);
    }

    /**
     * Creates a new builder for InflationAssumptions.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the general inflation rate.
     *
     * @return general inflation rate as decimal (e.g., 0.03 for 3%)
     */
    public BigDecimal getGeneralInflation() {
        return generalInflation;
    }

    /**
     * Returns the healthcare inflation rate.
     *
     * @return healthcare inflation rate as decimal
     */
    public BigDecimal getHealthcareInflation() {
        return healthcareInflation;
    }

    /**
     * Returns the housing inflation rate.
     *
     * @return housing inflation rate as decimal
     */
    public BigDecimal getHousingInflation() {
        return housingInflation;
    }

    /**
     * Creates a copy with the general inflation rate updated.
     *
     * @param rate the new general inflation rate
     * @return new InflationAssumptions with updated rate
     */
    public InflationAssumptions withGeneralInflation(double rate) {
        return builder()
            .generalInflation(rate)
            .healthcareInflation(this.healthcareInflation)
            .housingInflation(this.housingInflation)
            .build();
    }

    /**
     * Creates a copy with the healthcare inflation rate updated.
     *
     * @param rate the new healthcare inflation rate
     * @return new InflationAssumptions with updated rate
     */
    public InflationAssumptions withHealthcareInflation(double rate) {
        return builder()
            .generalInflation(this.generalInflation)
            .healthcareInflation(rate)
            .housingInflation(this.housingInflation)
            .build();
    }

    /**
     * Creates a copy with the housing inflation rate updated.
     *
     * @param rate the new housing inflation rate
     * @return new InflationAssumptions with updated rate
     */
    public InflationAssumptions withHousingInflation(double rate) {
        return builder()
            .generalInflation(this.generalInflation)
            .healthcareInflation(this.healthcareInflation)
            .housingInflation(rate)
            .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InflationAssumptions that = (InflationAssumptions) o;
        return generalInflation.compareTo(that.generalInflation) == 0
            && healthcareInflation.compareTo(that.healthcareInflation) == 0
            && housingInflation.compareTo(that.housingInflation) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            generalInflation.stripTrailingZeros(),
            healthcareInflation.stripTrailingZeros(),
            housingInflation.stripTrailingZeros()
        );
    }

    @Override
    public String toString() {
        return String.format(
            "InflationAssumptions{general=%s%%, healthcare=%s%%, housing=%s%%}",
            generalInflation.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString(),
            healthcareInflation.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString(),
            housingInflation.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString()
        );
    }

    /**
     * Builder for creating InflationAssumptions instances.
     */
    public static class Builder {
        private BigDecimal generalInflation = DEFAULT_GENERAL_INFLATION;
        private BigDecimal healthcareInflation = DEFAULT_HEALTHCARE_INFLATION;
        private BigDecimal housingInflation = DEFAULT_HOUSING_INFLATION;

        /**
         * Sets the general inflation rate.
         *
         * @param rate the inflation rate (as decimal, e.g., 0.03 for 3%)
         * @return this builder
         */
        public Builder generalInflation(double rate) {
            this.generalInflation = BigDecimal.valueOf(rate);
            return this;
        }

        /**
         * Sets the general inflation rate.
         *
         * @param rate the inflation rate
         * @return this builder
         */
        public Builder generalInflation(BigDecimal rate) {
            this.generalInflation = rate;
            return this;
        }

        /**
         * Sets the healthcare inflation rate.
         *
         * @param rate the inflation rate (as decimal)
         * @return this builder
         */
        public Builder healthcareInflation(double rate) {
            this.healthcareInflation = BigDecimal.valueOf(rate);
            return this;
        }

        /**
         * Sets the healthcare inflation rate.
         *
         * @param rate the inflation rate
         * @return this builder
         */
        public Builder healthcareInflation(BigDecimal rate) {
            this.healthcareInflation = rate;
            return this;
        }

        /**
         * Sets the housing inflation rate.
         *
         * @param rate the inflation rate (as decimal)
         * @return this builder
         */
        public Builder housingInflation(double rate) {
            this.housingInflation = BigDecimal.valueOf(rate);
            return this;
        }

        /**
         * Sets the housing inflation rate.
         *
         * @param rate the inflation rate
         * @return this builder
         */
        public Builder housingInflation(BigDecimal rate) {
            this.housingInflation = rate;
            return this;
        }

        /**
         * Builds the InflationAssumptions instance.
         *
         * @return a new InflationAssumptions
         * @throws InvalidRateException if any rate is out of range
         */
        public InflationAssumptions build() {
            validateRate("General inflation", generalInflation);
            validateRate("Healthcare inflation", healthcareInflation);
            validateRate("Housing inflation", housingInflation);
            return new InflationAssumptions(generalInflation, healthcareInflation, housingInflation);
        }

        private void validateRate(String name, BigDecimal rate) {
            if (rate.compareTo(MIN_RATE) < 0 || rate.compareTo(MAX_RATE) > 0) {
                throw InvalidRateException.inflationRateOutOfRange(name, rate);
            }
        }
    }
}
