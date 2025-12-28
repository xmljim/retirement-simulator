package io.github.xmljim.retirement.domain.enums;

import java.math.BigDecimal;

/**
 * Retirement spending phases based on activity level.
 *
 * <p>Research consistently shows that discretionary spending follows a
 * predictable pattern in retirement:
 * <ul>
 *   <li><b>Go-Go Years</b> (early retirement): Active travel, hobbies, entertainment</li>
 *   <li><b>Slow-Go Years</b> (mid retirement): Reduced activity, closer-to-home activities</li>
 *   <li><b>No-Go Years</b> (late retirement): Minimal discretionary, increased healthcare</li>
 * </ul>
 *
 * <p>Default age ranges (configurable):
 * <ul>
 *   <li>Go-Go: 65-74</li>
 *   <li>Slow-Go: 75-84</li>
 *   <li>No-Go: 85+</li>
 * </ul>
 *
 * @see io.github.xmljim.retirement.domain.calculator.impl.SpendingCurveModifier
 */
public enum SpendingPhase {

    /**
     * Early retirement phase with full discretionary spending.
     *
     * <p>Typical ages: 65-74. Characterized by:
     * <ul>
     *   <li>Active travel and vacations</li>
     *   <li>New hobbies and activities</li>
     *   <li>Entertainment and dining out</li>
     *   <li>Highest discretionary spending</li>
     * </ul>
     */
    GO_GO("Go-Go Years", new BigDecimal("1.00"), 65, 74,
            "Active early retirement with full discretionary spending"),

    /**
     * Mid-retirement phase with reduced discretionary spending.
     *
     * <p>Typical ages: 75-84. Characterized by:
     * <ul>
     *   <li>Reduced travel frequency and distance</li>
     *   <li>More time at home</li>
     *   <li>Lower entertainment expenses</li>
     *   <li>Typically 70-80% of Go-Go spending</li>
     * </ul>
     */
    SLOW_GO("Slow-Go Years", new BigDecimal("0.80"), 75, 84,
            "Mid-retirement with reduced activity and spending"),

    /**
     * Late retirement phase with minimal discretionary spending.
     *
     * <p>Typical ages: 85+. Characterized by:
     * <ul>
     *   <li>Limited travel and outside activities</li>
     *   <li>Increased healthcare and assistance needs</li>
     *   <li>Minimal entertainment expenses</li>
     *   <li>Typically 50-60% of Go-Go spending</li>
     * </ul>
     */
    NO_GO("No-Go Years", new BigDecimal("0.50"), 85, Integer.MAX_VALUE,
            "Late retirement with minimal discretionary spending");

    private final String displayName;
    private final BigDecimal defaultMultiplier;
    private final int defaultStartAge;
    private final int defaultEndAge;
    private final String description;

    SpendingPhase(String displayName, BigDecimal defaultMultiplier,
                  int defaultStartAge, int defaultEndAge, String description) {
        this.displayName = displayName;
        this.defaultMultiplier = defaultMultiplier;
        this.defaultStartAge = defaultStartAge;
        this.defaultEndAge = defaultEndAge;
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
     * Returns the default spending multiplier for this phase.
     *
     * <p>The multiplier is applied to discretionary expenses.
     * GO_GO = 1.0 (100%), SLOW_GO = 0.80 (80%), NO_GO = 0.50 (50%)
     *
     * @return the default multiplier
     */
    public BigDecimal getDefaultMultiplier() {
        return defaultMultiplier;
    }

    /**
     * Returns the default starting age for this phase.
     *
     * @return the default start age
     */
    public int getDefaultStartAge() {
        return defaultStartAge;
    }

    /**
     * Returns the default ending age for this phase.
     *
     * @return the default end age (Integer.MAX_VALUE for NO_GO)
     */
    public int getDefaultEndAge() {
        return defaultEndAge;
    }

    /**
     * Returns a description of this spending phase.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Determines the spending phase for a given age using default ranges.
     *
     * @param age the person's age
     * @return the spending phase for that age
     */
    public static SpendingPhase forAge(int age) {
        if (age < SLOW_GO.defaultStartAge) {
            return GO_GO;
        } else if (age < NO_GO.defaultStartAge) {
            return SLOW_GO;
        } else {
            return NO_GO;
        }
    }

    /**
     * Indicates whether the given age falls within this phase's default range.
     *
     * @param age the age to check
     * @return true if age is within the default range
     */
    public boolean isInRange(int age) {
        return age >= defaultStartAge && age <= defaultEndAge;
    }
}
