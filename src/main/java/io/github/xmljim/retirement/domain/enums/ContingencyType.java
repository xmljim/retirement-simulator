package io.github.xmljim.retirement.domain.enums;

import java.math.BigDecimal;

/**
 * Types of contingency reserves for retirement planning.
 *
 * <p>Contingency reserves handle "lumpy" expenses that don't fit
 * into monthly budgets - large, infrequent costs that can significantly
 * impact retirement portfolios if not planned for.
 */
public enum ContingencyType {

    /**
     * Home repair and maintenance reserve.
     * Recommended: 1-2% of home value annually.
     */
    HOME_REPAIR("Home Repair", "Major home repairs and maintenance", new BigDecimal("0.015")),

    /**
     * Vehicle replacement fund.
     * Typically scheduled every 7-10 years.
     */
    VEHICLE_REPLACEMENT("Vehicle Replacement", "Scheduled vehicle purchases", null),

    /**
     * General emergency fund.
     * Recommended: 3-6 months of expenses.
     */
    EMERGENCY_FUND("Emergency Fund", "General emergency reserve", null),

    /**
     * Major appliance replacement reserve.
     * HVAC, water heater, major kitchen appliances.
     */
    APPLIANCE_RESERVE("Appliance Reserve", "Major appliance replacement", null),

    /**
     * Medical emergency fund.
     * Healthcare deductibles, out-of-pocket maximums, emergencies.
     */
    MEDICAL_EMERGENCY("Medical Emergency", "Healthcare emergencies and deductibles", null),

    /**
     * Family support reserve.
     * Helping adult children, parents, or other family members.
     */
    FAMILY_SUPPORT("Family Support", "Financial assistance to family members", null),

    /**
     * General contingency reserve.
     * Catch-all for unspecified contingencies.
     */
    GENERAL("General", "General contingency fund", null);

    private final String displayName;
    private final String description;
    private final BigDecimal defaultRateOfValue;

    ContingencyType(String displayName, String description, BigDecimal defaultRateOfValue) {
        this.displayName = displayName;
        this.description = description;
        this.defaultRateOfValue = defaultRateOfValue;
    }

    /**
     * Returns the human-readable display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns a brief description of this contingency type.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the default rate of asset value for this type, if applicable.
     *
     * <p>For example, HOME_REPAIR returns 0.015 (1.5% of home value).
     *
     * @return the default rate, or null if not applicable
     */
    public BigDecimal getDefaultRateOfValue() {
        return defaultRateOfValue;
    }

    /**
     * Returns whether this type has a default rate based on asset value.
     *
     * @return true if a default rate is defined
     */
    public boolean hasDefaultRate() {
        return defaultRateOfValue != null;
    }
}
