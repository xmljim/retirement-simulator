package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.xmljim.retirement.domain.enums.LimitCategory;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;

/**
 * Summary of year-to-date contributions and remaining limits.
 *
 * <p>Provides a snapshot of contribution status for a specific year,
 * including:
 * <ul>
 *   <li>Personal (employee) contributions by category</li>
 *   <li>Employer contributions by category</li>
 *   <li>Applicable limits by category</li>
 *   <li>Remaining room under limits</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * YTDSummary summary = tracker.getSummary(2025);
 *
 * BigDecimal contributed = summary.getPersonalContributions(LimitCategory.EMPLOYER_401K);
 * BigDecimal limit = summary.getLimit(LimitCategory.EMPLOYER_401K);
 * BigDecimal remaining = summary.getRemainingRoom(LimitCategory.EMPLOYER_401K);
 *
 * System.out.println("401(k): $" + contributed + " of $" + limit +
 *     " (" + remaining + " remaining)");
 * }</pre>
 *
 * @param year the tax year for this summary
 * @param personalContributions YTD personal contributions by limit category
 * @param employerContributions YTD employer contributions by limit category
 * @param limits applicable limits by category (personal contribution limits only)
 * @param remainingRoom remaining room under limits by category
 */
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP",
    justification = "Record components are made unmodifiable in compact constructor"
)
public record YTDSummary(
    int year,
    Map<LimitCategory, BigDecimal> personalContributions,
    Map<LimitCategory, BigDecimal> employerContributions,
    Map<LimitCategory, BigDecimal> limits,
    Map<LimitCategory, BigDecimal> remainingRoom
) {
    // Compact constructor makes maps immutable
    public YTDSummary {
        if (year < 1900 || year > 2200) {
            throw new MissingRequiredFieldException("year", "Year must be between 1900 and 2200");
        }
        personalContributions = personalContributions != null
            ? Collections.unmodifiableMap(new EnumMap<>(personalContributions))
            : Collections.emptyMap();
        employerContributions = employerContributions != null
            ? Collections.unmodifiableMap(new EnumMap<>(employerContributions))
            : Collections.emptyMap();
        limits = limits != null
            ? Collections.unmodifiableMap(new EnumMap<>(limits))
            : Collections.emptyMap();
        remainingRoom = remainingRoom != null
            ? Collections.unmodifiableMap(new EnumMap<>(remainingRoom))
            : Collections.emptyMap();
    }

    /**
     * Returns the YTD personal contributions for a specific category.
     *
     * @param category the limit category
     * @return the contribution amount, or zero if none
     */
    public BigDecimal getPersonalContributions(LimitCategory category) {
        return personalContributions.getOrDefault(category, BigDecimal.ZERO);
    }

    /**
     * Returns the YTD employer contributions for a specific category.
     *
     * @param category the limit category
     * @return the contribution amount, or zero if none
     */
    public BigDecimal getEmployerContributions(LimitCategory category) {
        return employerContributions.getOrDefault(category, BigDecimal.ZERO);
    }

    /**
     * Returns the total YTD contributions (personal + employer) for a category.
     *
     * @param category the limit category
     * @return the total contribution amount
     */
    public BigDecimal getTotalContributions(LimitCategory category) {
        return getPersonalContributions(category).add(getEmployerContributions(category));
    }

    /**
     * Returns the limit for a specific category.
     *
     * <p>Note: This is the personal contribution limit only. Employer contributions
     * do not count against this limit.
     *
     * @param category the limit category
     * @return the limit amount, or zero if not applicable
     */
    public BigDecimal getLimit(LimitCategory category) {
        return limits.getOrDefault(category, BigDecimal.ZERO);
    }

    /**
     * Returns the remaining room under the limit for a specific category.
     *
     * @param category the limit category
     * @return the remaining room, or zero if at limit
     */
    public BigDecimal getRemainingRoom(LimitCategory category) {
        return remainingRoom.getOrDefault(category, BigDecimal.ZERO);
    }

    /**
     * Returns the percentage of limit used for a specific category.
     *
     * @param category the limit category
     * @return the percentage used (0.0 to 1.0+), or 0 if no limit
     */
    public BigDecimal getPercentageUsed(LimitCategory category) {
        BigDecimal limit = getLimit(category);
        if (limit.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getPersonalContributions(category)
            .divide(limit, 4, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Indicates whether the limit has been reached for a category.
     *
     * @param category the limit category
     * @return true if remaining room is zero or negative
     */
    public boolean isAtLimit(LimitCategory category) {
        return getRemainingRoom(category).compareTo(BigDecimal.ZERO) <= 0;
    }

    /**
     * Creates a builder for YTDSummary.
     *
     * @param year the tax year
     * @return a new builder instance
     */
    public static Builder builder(int year) {
        return new Builder(year);
    }

    /**
     * Builder for creating YTDSummary instances.
     */
    public static final class Builder {
        private final int year;
        private final Map<LimitCategory, BigDecimal> personalContributions = new EnumMap<>(LimitCategory.class);
        private final Map<LimitCategory, BigDecimal> employerContributions = new EnumMap<>(LimitCategory.class);
        private final Map<LimitCategory, BigDecimal> limits = new EnumMap<>(LimitCategory.class);
        private final Map<LimitCategory, BigDecimal> remainingRoom = new EnumMap<>(LimitCategory.class);

        private Builder(int year) {
            this.year = year;
        }

        /**
         * Sets personal contributions for a category.
         *
         * @param category the limit category
         * @param amount the contribution amount
         * @return this builder
         */
        public Builder personalContribution(LimitCategory category, BigDecimal amount) {
            if (amount != null) {
                personalContributions.put(category, amount);
            }
            return this;
        }

        /**
         * Sets employer contributions for a category.
         *
         * @param category the limit category
         * @param amount the contribution amount
         * @return this builder
         */
        public Builder employerContribution(LimitCategory category, BigDecimal amount) {
            if (amount != null) {
                employerContributions.put(category, amount);
            }
            return this;
        }

        /**
         * Sets the limit for a category.
         *
         * @param category the limit category
         * @param amount the limit amount
         * @return this builder
         */
        public Builder limit(LimitCategory category, BigDecimal amount) {
            if (amount != null) {
                limits.put(category, amount);
            }
            return this;
        }

        /**
         * Sets the remaining room for a category.
         *
         * @param category the limit category
         * @param amount the remaining room
         * @return this builder
         */
        public Builder remainingRoom(LimitCategory category, BigDecimal amount) {
            if (amount != null) {
                remainingRoom.put(category, amount);
            }
            return this;
        }

        /**
         * Builds the YTDSummary.
         *
         * @return a new YTDSummary instance
         */
        public YTDSummary build() {
            return new YTDSummary(year, personalContributions, employerContributions, limits, remainingRoom);
        }
    }
}
