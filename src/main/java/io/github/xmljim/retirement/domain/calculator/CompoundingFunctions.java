package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

import io.github.xmljim.retirement.domain.calculator.impl.MathUtils;

/**
 * Standard compounding function implementations.
 *
 * <p>Provides commonly used compounding strategies:
 * <ul>
 *   <li>{@link #ANNUAL} - True annual compounding (for investments)</li>
 *   <li>{@link #MONTHLY} - Discrete monthly compounding (for savings)</li>
 *   <li>{@link #DAILY} - Daily compounding (for credit cards)</li>
 *   <li>{@link #CONTINUOUS} - Continuous compounding (theoretical maximum)</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * BigDecimal result = CompoundingFunctions.ANNUAL.compound(
 *     new BigDecimal("10000"),  // principal
 *     new BigDecimal("0.10"),   // 10% annual rate
 *     24                        // 24 months
 * );
 * // result = 10000 * (1.10)^(24/12) = 10000 * 1.21 = 12100
 * }</pre>
 */
public final class CompoundingFunctions {

    private static final int SCALE = 10;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final int MONTHS_PER_YEAR = 12;
    private static final int DAYS_PER_YEAR = 365;

    private CompoundingFunctions() {
        // Prevent instantiation
    }

    /**
     * True annual compounding.
     *
     * <p>Formula: {@code principal * (1 + annualRate)^(months/12)}
     *
     * <p>This is the standard for investment growth calculations where
     * the annual rate represents the expected yearly return, compounded
     * once per year but prorated for partial years.
     *
     * <p>Example: $100 at 10% annual rate for 6 months:
     * <pre>
     * 100 * (1.10)^(6/12) = 100 * (1.10)^0.5 = 104.88
     * </pre>
     */
    public static final CompoundingFunction ANNUAL = new CompoundingFunction() {
        @Override
        public BigDecimal compound(BigDecimal principal, BigDecimal annualRate, int periods) {
            if (principal == null || principal.compareTo(BigDecimal.ZERO) == 0) {
                return principal != null ? principal : BigDecimal.ZERO;
            }
            if (annualRate == null || annualRate.compareTo(BigDecimal.ZERO) == 0) {
                return principal;
            }
            if (periods <= 0) {
                return principal;
            }

            // principal * (1 + annualRate)^(months/12)
            BigDecimal base = BigDecimal.ONE.add(annualRate);
            double exponent = periods / (double) MONTHS_PER_YEAR;
            BigDecimal factor = MathUtils.pow(base, exponent, SCALE, ROUNDING_MODE);

            return principal.multiply(factor).setScale(SCALE, ROUNDING_MODE);
        }

        @Override
        public String getName() {
            return "Annual";
        }

        @Override
        public String getFormula() {
            return "principal * (1 + annualRate)^(months/12)";
        }
    };

    /**
     * Discrete monthly compounding.
     *
     * <p>Formula: {@code principal * (1 + annualRate/12)^months}
     *
     * <p>Common for savings accounts and CDs where interest is credited
     * monthly at 1/12th of the annual rate.
     *
     * <p>Example: $100 at 12% annual rate for 6 months:
     * <pre>
     * 100 * (1 + 0.12/12)^6 = 100 * (1.01)^6 = 106.15
     * </pre>
     */
    public static final CompoundingFunction MONTHLY = new CompoundingFunction() {
        @Override
        public BigDecimal compound(BigDecimal principal, BigDecimal annualRate, int periods) {
            if (principal == null || principal.compareTo(BigDecimal.ZERO) == 0) {
                return principal != null ? principal : BigDecimal.ZERO;
            }
            if (annualRate == null || annualRate.compareTo(BigDecimal.ZERO) == 0) {
                return principal;
            }
            if (periods <= 0) {
                return principal;
            }

            // principal * (1 + annualRate/12)^months
            BigDecimal monthlyRate = annualRate.divide(
                BigDecimal.valueOf(MONTHS_PER_YEAR), SCALE, ROUNDING_MODE);
            BigDecimal base = BigDecimal.ONE.add(monthlyRate);
            BigDecimal factor = MathUtils.pow(base, periods, SCALE, ROUNDING_MODE);

            return principal.multiply(factor).setScale(SCALE, ROUNDING_MODE);
        }

        @Override
        public String getName() {
            return "Monthly";
        }

        @Override
        public String getFormula() {
            return "principal * (1 + annualRate/12)^months";
        }
    };

    /**
     * Daily compounding.
     *
     * <p>Formula: {@code principal * (1 + annualRate/365)^days}
     *
     * <p>Common for credit cards and some loans. Note that this function
     * expects the period parameter to be in days, not months.
     *
     * <p>Example: $100 at 18% annual rate for 30 days:
     * <pre>
     * 100 * (1 + 0.18/365)^30 = 100 * 1.01485 = 101.49
     * </pre>
     */
    public static final CompoundingFunction DAILY = new CompoundingFunction() {
        @Override
        public BigDecimal compound(BigDecimal principal, BigDecimal annualRate, int periods) {
            if (principal == null || principal.compareTo(BigDecimal.ZERO) == 0) {
                return principal != null ? principal : BigDecimal.ZERO;
            }
            if (annualRate == null || annualRate.compareTo(BigDecimal.ZERO) == 0) {
                return principal;
            }
            if (periods <= 0) {
                return principal;
            }

            // principal * (1 + annualRate/365)^days
            BigDecimal dailyRate = annualRate.divide(
                BigDecimal.valueOf(DAYS_PER_YEAR), SCALE, ROUNDING_MODE);
            BigDecimal base = BigDecimal.ONE.add(dailyRate);
            BigDecimal factor = MathUtils.pow(base, periods, SCALE, ROUNDING_MODE);

            return principal.multiply(factor).setScale(SCALE, ROUNDING_MODE);
        }

        @Override
        public String getName() {
            return "Daily";
        }

        @Override
        public String getFormula() {
            return "principal * (1 + annualRate/365)^days";
        }
    };

    /**
     * Continuous compounding.
     *
     * <p>Formula: {@code principal * e^(annualRate * years)}
     *
     * <p>Theoretical maximum compounding frequency. Periods are months
     * which are converted to years for the calculation.
     *
     * <p>Example: $100 at 10% annual rate for 12 months (1 year):
     * <pre>
     * 100 * e^(0.10 * 1) = 100 * 1.1052 = 110.52
     * </pre>
     */
    public static final CompoundingFunction CONTINUOUS = new CompoundingFunction() {
        @Override
        public BigDecimal compound(BigDecimal principal, BigDecimal annualRate, int periods) {
            if (principal == null || principal.compareTo(BigDecimal.ZERO) == 0) {
                return principal != null ? principal : BigDecimal.ZERO;
            }
            if (annualRate == null || annualRate.compareTo(BigDecimal.ZERO) == 0) {
                return principal;
            }
            if (periods <= 0) {
                return principal;
            }

            // principal * e^(annualRate * years)
            double years = periods / (double) MONTHS_PER_YEAR;
            double exponent = annualRate.doubleValue() * years;
            double factor = Math.exp(exponent);

            return principal.multiply(BigDecimal.valueOf(factor)).setScale(SCALE, ROUNDING_MODE);
        }

        @Override
        public String getName() {
            return "Continuous";
        }

        @Override
        public String getFormula() {
            return "principal * e^(annualRate * years)";
        }
    };
}
