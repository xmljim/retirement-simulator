package io.github.xmljim.retirement.domain.enums;

/**
 * Expense categories for retirement budget modeling.
 *
 * <p>Categories are organized into {@link ExpenseCategoryGroup}s for easier
 * management. Each category has an associated default inflation type that
 * determines which inflation rate to apply when projecting future costs.
 *
 * <p>Categories support different inflation rates because costs in retirement
 * do not all grow at the same pace. For example:
 * <ul>
 *   <li>Healthcare typically inflates at 5-6% annually</li>
 *   <li>General expenses follow CPI at ~2.5%</li>
 *   <li>Long-term care costs have risen 7-10% annually</li>
 *   <li>Debt payments are typically fixed (no inflation)</li>
 * </ul>
 *
 * @see ExpenseCategoryGroup
 * @see InflationType
 */
public enum ExpenseCategory {

    // ========== ESSENTIAL ==========

    /**
     * Housing expenses.
     *
     * <p>Includes mortgage or rent, property taxes, homeowner's insurance,
     * HOA fees, and basic home maintenance. Property taxes often outpace
     * general inflation.
     */
    HOUSING("Housing", ExpenseCategoryGroup.ESSENTIAL, InflationType.HOUSING,
            "Mortgage/rent, property tax, insurance, HOA"),

    /**
     * Food expenses.
     *
     * <p>Includes groceries and dining out. Generally follows CPI.
     */
    FOOD("Food", ExpenseCategoryGroup.ESSENTIAL, InflationType.GENERAL,
            "Groceries and dining"),

    /**
     * Utility expenses.
     *
     * <p>Includes electricity, gas, water, sewer, trash, internet, and phone.
     */
    UTILITIES("Utilities", ExpenseCategoryGroup.ESSENTIAL, InflationType.GENERAL,
            "Electric, gas, water, internet, phone"),

    /**
     * Transportation expenses.
     *
     * <p>Includes car payments, auto insurance, fuel, maintenance, and repairs.
     * Does not include vehicle replacement reserves (see {@link #VEHICLE_REPLACEMENT}).
     */
    TRANSPORTATION("Transportation", ExpenseCategoryGroup.ESSENTIAL, InflationType.GENERAL,
            "Car payment, insurance, gas, maintenance"),

    /**
     * Non-health insurance expenses.
     *
     * <p>Includes life insurance, umbrella policies, and other non-health
     * insurance not covered elsewhere.
     */
    INSURANCE("Insurance", ExpenseCategoryGroup.ESSENTIAL, InflationType.GENERAL,
            "Life, umbrella, other non-health insurance"),

    // ========== HEALTHCARE ==========

    /**
     * Medicare premium expenses.
     *
     * <p>Includes Medicare Part B, Part D, and Medigap/Medicare Supplement
     * premiums. May include IRMAA surcharges based on income.
     * Use {@link io.github.xmljim.retirement.domain.calculator.MedicareCalculator}
     * for IRMAA-adjusted premiums.
     */
    MEDICARE_PREMIUMS("Medicare Premiums", ExpenseCategoryGroup.HEALTHCARE, InflationType.HEALTHCARE,
            "Part B, Part D, Medigap premiums (IRMAA-adjusted)"),

    /**
     * Healthcare out-of-pocket expenses.
     *
     * <p>Includes copays, deductibles, prescriptions, dental, vision,
     * and hearing costs not covered by insurance.
     */
    HEALTHCARE_OOP("Healthcare Out-of-Pocket", ExpenseCategoryGroup.HEALTHCARE, InflationType.HEALTHCARE,
            "Copays, prescriptions, dental, vision, hearing"),

    /**
     * Long-term care insurance premiums.
     *
     * <p>Monthly or annual premiums for LTC insurance policies.
     * Premiums may increase over time due to rate adjustments.
     */
    LTC_PREMIUMS("LTC Insurance Premiums", ExpenseCategoryGroup.HEALTHCARE, InflationType.LTC,
            "Long-term care insurance premium payments"),

    /**
     * Long-term care expenses.
     *
     * <p>Actual costs for long-term care services including nursing home,
     * assisted living, home health aides, and adult day care.
     * These costs are reduced by LTC insurance benefit payouts.
     */
    LTC_CARE("Long-Term Care", ExpenseCategoryGroup.HEALTHCARE, InflationType.LTC,
            "Nursing home, assisted living, home health costs"),

    // ========== DISCRETIONARY ==========

    /**
     * Travel expenses.
     *
     * <p>Includes vacations, trips to visit family, and other travel.
     * Often highest in early retirement (Go-Go years) and decreases with age.
     */
    TRAVEL("Travel", ExpenseCategoryGroup.DISCRETIONARY, InflationType.GENERAL,
            "Vacations, trips, visiting family"),

    /**
     * Entertainment expenses.
     *
     * <p>Includes dining out, streaming services, concerts, movies,
     * sports events, and other entertainment.
     */
    ENTERTAINMENT("Entertainment", ExpenseCategoryGroup.DISCRETIONARY, InflationType.GENERAL,
            "Dining out, streaming, concerts, events"),

    /**
     * Hobby expenses.
     *
     * <p>Includes golf, crafts, club memberships, sports equipment,
     * and other hobby-related costs.
     */
    HOBBIES("Hobbies", ExpenseCategoryGroup.DISCRETIONARY, InflationType.GENERAL,
            "Golf, crafts, clubs, sports"),

    /**
     * Gifts and charitable contributions.
     *
     * <p>Includes gifts to family, charitable donations, and support
     * for children/grandchildren.
     */
    GIFTS("Gifts & Charity", ExpenseCategoryGroup.DISCRETIONARY, InflationType.GENERAL,
            "Family gifts, charitable donations"),

    // ========== CONTINGENCY ==========

    /**
     * Home repair and maintenance reserve.
     *
     * <p>Annual set-aside for home repairs and major maintenance.
     * Rule of thumb: 1-2% of home value annually. Covers roof,
     * HVAC, appliances, and other major repairs.
     */
    HOME_REPAIRS("Home Repairs Reserve", ExpenseCategoryGroup.CONTINGENCY, InflationType.HOUSING,
            "Reserve for major repairs (1-2% of home value)"),

    /**
     * Vehicle replacement reserve.
     *
     * <p>Amortized reserve for vehicle replacement. Example: $35,000
     * replacement every 7 years = $5,000/year reserve.
     */
    VEHICLE_REPLACEMENT("Vehicle Replacement", ExpenseCategoryGroup.CONTINGENCY, InflationType.GENERAL,
            "Amortized reserve for car replacement"),

    /**
     * Emergency fund reserve.
     *
     * <p>Contributions to maintain or replenish emergency fund.
     * Target is typically 3-6 months of essential expenses.
     */
    EMERGENCY_RESERVE("Emergency Reserve", ExpenseCategoryGroup.CONTINGENCY, InflationType.GENERAL,
            "Emergency fund contributions"),

    // ========== DEBT ==========

    /**
     * Debt payments.
     *
     * <p>Fixed monthly payments for mortgage principal, car loans,
     * student loans, and other installment debt. These payments
     * are typically fixed and do not inflate.
     */
    DEBT_PAYMENTS("Debt Payments", ExpenseCategoryGroup.DEBT, InflationType.NONE,
            "Mortgage, car loans, other fixed debt"),

    // ========== OTHER ==========

    /**
     * Tax payments.
     *
     * <p>Estimated federal and state income tax payments.
     * Use tax calculators for accurate projections.
     */
    TAXES("Taxes", ExpenseCategoryGroup.OTHER, InflationType.GENERAL,
            "Estimated tax payments"),

    /**
     * Other miscellaneous expenses.
     *
     * <p>Catch-all for expenses not fitting other categories.
     */
    OTHER("Other", ExpenseCategoryGroup.OTHER, InflationType.GENERAL,
            "Miscellaneous expenses");

    /**
     * Type of inflation to apply to this category.
     */
    public enum InflationType {
        /** General CPI inflation (~2.5%). */
        GENERAL,
        /** Healthcare inflation (~5.5%). */
        HEALTHCARE,
        /** Housing-specific inflation (~3.0%). */
        HOUSING,
        /** Long-term care inflation (~7.0%). */
        LTC,
        /** No inflation (fixed payments). */
        NONE
    }

    private final String displayName;
    private final ExpenseCategoryGroup group;
    private final InflationType inflationType;
    private final String description;

    ExpenseCategory(String displayName, ExpenseCategoryGroup group,
                    InflationType inflationType, String description) {
        this.displayName = displayName;
        this.group = group;
        this.inflationType = inflationType;
        this.description = description;
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
     * Returns the category group this expense belongs to.
     *
     * @return the category group
     */
    public ExpenseCategoryGroup getGroup() {
        return group;
    }

    /**
     * Returns the inflation type for this category.
     *
     * @return the inflation type
     */
    public InflationType getInflationType() {
        return inflationType;
    }

    /**
     * Returns a brief description of this category.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Indicates whether this is an essential expense.
     *
     * @return true if this category is in the ESSENTIAL group
     */
    public boolean isEssential() {
        return group == ExpenseCategoryGroup.ESSENTIAL;
    }

    /**
     * Indicates whether this is a healthcare expense.
     *
     * @return true if this category is in the HEALTHCARE group
     */
    public boolean isHealthcare() {
        return group == ExpenseCategoryGroup.HEALTHCARE;
    }

    /**
     * Indicates whether this is a discretionary expense.
     *
     * @return true if this category is in the DISCRETIONARY group
     */
    public boolean isDiscretionary() {
        return group == ExpenseCategoryGroup.DISCRETIONARY;
    }

    /**
     * Indicates whether this expense is subject to inflation.
     *
     * <p>Fixed payments like debt service do not inflate.
     *
     * @return true if inflation should be applied
     */
    public boolean isInflationAdjusted() {
        return inflationType != InflationType.NONE;
    }
}
