package io.github.xmljim.retirement.domain.config;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.xmljim.retirement.domain.enums.ExpenseCategory;
import io.github.xmljim.retirement.domain.enums.ExpenseCategory.InflationType;

/**
 * Inflation rate configuration for expense modeling.
 *
 * <p>Provides category-specific inflation rates for projecting future expenses.
 * Different expense types inflate at different rates:
 * <ul>
 *   <li>General CPI: ~2.5% (food, utilities, discretionary)</li>
 *   <li>Healthcare: ~5.5% (medical, Medicare premiums)</li>
 *   <li>Housing: ~3.0% (property taxes, maintenance)</li>
 *   <li>Long-term care: ~7.0% (nursing homes, assisted living)</li>
 * </ul>
 *
 * <p>Configuration is loaded from YAML with prefix {@code expenses.inflation-rates}.
 *
 * @see ExpenseCategory
 * @see ExpenseCategory.InflationType
 */
@ConfigurationProperties(prefix = "expenses.inflation-rates")
@Validated
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP",
    justification = "Spring @ConfigurationProperties requires mutable access for binding"
)
public class InflationRates {

    /** Default general CPI inflation rate. */
    public static final BigDecimal DEFAULT_GENERAL_CPI = new BigDecimal("0.025");

    /** Default healthcare inflation rate. */
    public static final BigDecimal DEFAULT_HEALTHCARE = new BigDecimal("0.055");

    /** Default housing inflation rate. */
    public static final BigDecimal DEFAULT_HOUSING = new BigDecimal("0.030");

    /** Default long-term care inflation rate. */
    public static final BigDecimal DEFAULT_LTC = new BigDecimal("0.070");

    private BigDecimal generalCpi = DEFAULT_GENERAL_CPI;
    private BigDecimal healthcare = DEFAULT_HEALTHCARE;
    private BigDecimal housing = DEFAULT_HOUSING;
    private BigDecimal ltc = DEFAULT_LTC;
    private Map<ExpenseCategory, BigDecimal> categoryOverrides = new EnumMap<>(ExpenseCategory.class);

    /**
     * Returns the inflation rate for a specific expense category.
     *
     * <p>First checks for a category-specific override, then falls back
     * to the rate for the category's inflation type.
     *
     * @param category the expense category
     * @return the applicable inflation rate (e.g., 0.025 for 2.5%)
     */
    public BigDecimal getRateForCategory(ExpenseCategory category) {
        if (category == null) {
            return generalCpi;
        }

        // Check for category-specific override
        if (categoryOverrides.containsKey(category)) {
            return categoryOverrides.get(category);
        }

        // Fall back to inflation type rate
        return getRateForInflationType(category.getInflationType());
    }

    /**
     * Returns the inflation rate for a specific inflation type.
     *
     * @param type the inflation type
     * @return the applicable inflation rate
     */
    public BigDecimal getRateForInflationType(InflationType type) {
        if (type == null) {
            return generalCpi;
        }

        return switch (type) {
            case GENERAL -> generalCpi;
            case HEALTHCARE -> healthcare;
            case HOUSING -> housing;
            case LTC -> ltc;
            case NONE -> BigDecimal.ZERO;
        };
    }

    /**
     * Returns the general CPI inflation rate.
     *
     * @return the rate as a decimal (e.g., 0.025 for 2.5%)
     */
    public BigDecimal getGeneralCpi() {
        return generalCpi;
    }

    /**
     * Sets the general CPI inflation rate.
     *
     * @param generalCpi the rate as a decimal
     */
    public void setGeneralCpi(BigDecimal generalCpi) {
        this.generalCpi = generalCpi != null ? generalCpi : DEFAULT_GENERAL_CPI;
    }

    /**
     * Returns the healthcare inflation rate.
     *
     * @return the rate as a decimal
     */
    public BigDecimal getHealthcare() {
        return healthcare;
    }

    /**
     * Sets the healthcare inflation rate.
     *
     * @param healthcare the rate as a decimal
     */
    public void setHealthcare(BigDecimal healthcare) {
        this.healthcare = healthcare != null ? healthcare : DEFAULT_HEALTHCARE;
    }

    /**
     * Returns the housing inflation rate.
     *
     * @return the rate as a decimal
     */
    public BigDecimal getHousing() {
        return housing;
    }

    /**
     * Sets the housing inflation rate.
     *
     * @param housing the rate as a decimal
     */
    public void setHousing(BigDecimal housing) {
        this.housing = housing != null ? housing : DEFAULT_HOUSING;
    }

    /**
     * Returns the long-term care inflation rate.
     *
     * @return the rate as a decimal
     */
    public BigDecimal getLtc() {
        return ltc;
    }

    /**
     * Sets the long-term care inflation rate.
     *
     * @param ltc the rate as a decimal
     */
    public void setLtc(BigDecimal ltc) {
        this.ltc = ltc != null ? ltc : DEFAULT_LTC;
    }

    /**
     * Returns category-specific rate overrides.
     *
     * @return map of category to custom rate
     */
    public Map<ExpenseCategory, BigDecimal> getCategoryOverrides() {
        return categoryOverrides;
    }

    /**
     * Sets category-specific rate overrides.
     *
     * @param categoryOverrides map of category to custom rate
     */
    public void setCategoryOverrides(Map<ExpenseCategory, BigDecimal> categoryOverrides) {
        this.categoryOverrides = categoryOverrides != null
                ? new EnumMap<>(categoryOverrides)
                : new EnumMap<>(ExpenseCategory.class);
    }

    /**
     * Sets a custom inflation rate for a specific category.
     *
     * @param category the category to override
     * @param rate the custom rate
     */
    public void setOverride(ExpenseCategory category, BigDecimal rate) {
        if (category != null && rate != null) {
            categoryOverrides.put(category, rate);
        }
    }

    /**
     * Removes a custom inflation rate override for a category.
     *
     * @param category the category to remove override for
     */
    public void removeOverride(ExpenseCategory category) {
        if (category != null) {
            categoryOverrides.remove(category);
        }
    }

    /**
     * Creates default inflation rates without Spring configuration.
     *
     * @return a new InflationRates instance with defaults
     */
    public static InflationRates defaults() {
        return new InflationRates();
    }
}
