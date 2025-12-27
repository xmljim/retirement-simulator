package io.github.xmljim.retirement.domain.enums;

/**
 * Marital status for Social Security benefit eligibility calculations.
 *
 * <p>Marital status affects eligibility for spousal and survivor benefits:
 * <ul>
 *   <li>{@link #SINGLE} - No spousal/survivor benefits available</li>
 *   <li>{@link #MARRIED} - Eligible for spousal benefits (if married 1+ year)</li>
 *   <li>{@link #DIVORCED} - May be eligible for divorced spouse benefits (if married 10+ years)</li>
 *   <li>{@link #WIDOWED} - Eligible for survivor benefits (if married 9+ months)</li>
 * </ul>
 *
 * @see <a href="https://www.ssa.gov/benefits/retirement/planner/applying7.html">SSA Spousal Benefits</a>
 * @see <a href="https://www.ssa.gov/benefits/survivors/">SSA Survivor Benefits</a>
 */
public enum MaritalStatus {

    /**
     * Never married or marriage annulled.
     *
     * <p>Not eligible for spousal or survivor benefits.
     */
    SINGLE("Single"),

    /**
     * Currently married.
     *
     * <p>Eligible for spousal benefits if:
     * <ul>
     *   <li>Married at least 1 year</li>
     *   <li>Spouse has filed for benefits (or is 62+)</li>
     * </ul>
     */
    MARRIED("Married"),

    /**
     * Previously married, now divorced.
     *
     * <p>May be eligible for divorced spouse benefits if:
     * <ul>
     *   <li>Marriage lasted at least 10 years</li>
     *   <li>Divorced for at least 2 years</li>
     *   <li>Currently unmarried (or remarried after 60)</li>
     *   <li>Both ex-spouses are at least 62</li>
     * </ul>
     */
    DIVORCED("Divorced"),

    /**
     * Spouse has died.
     *
     * <p>Eligible for survivor benefits if:
     * <ul>
     *   <li>Marriage lasted at least 9 months (exception for accidents)</li>
     *   <li>Age 60 or older (50 if disabled)</li>
     *   <li>Not remarried before age 60</li>
     * </ul>
     */
    WIDOWED("Widowed");

    private final String displayName;

    MaritalStatus(String displayName) {
        this.displayName = displayName;
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
     * Returns whether this status can potentially qualify for spousal benefits.
     *
     * <p>Note: Additional eligibility requirements apply.
     *
     * @return true for MARRIED and DIVORCED statuses
     */
    public boolean canQualifyForSpousalBenefits() {
        return this == MARRIED || this == DIVORCED;
    }

    /**
     * Returns whether this status can potentially qualify for survivor benefits.
     *
     * @return true for WIDOWED status
     */
    public boolean canQualifyForSurvivorBenefits() {
        return this == WIDOWED;
    }
}
