package io.github.xmljim.retirement.domain.enums;

/**
 * Reason a marriage ended.
 *
 * <p>Used in marriage history to track how past marriages ended,
 * which affects eligibility for different Social Security benefits.
 */
public enum MarriageEndReason {

    /**
     * Marriage ended in divorce.
     *
     * <p>May qualify for divorced spouse benefits if:
     * <ul>
     *   <li>Marriage lasted 10+ years</li>
     *   <li>Divorced at least 2 years</li>
     *   <li>Not currently remarried (or remarried after 60)</li>
     * </ul>
     */
    DIVORCED("Divorced"),

    /**
     * Marriage ended due to spouse's death.
     *
     * <p>May qualify for survivor benefits if:
     * <ul>
     *   <li>Marriage lasted 9+ months</li>
     *   <li>Not remarried before age 60</li>
     * </ul>
     */
    SPOUSE_DIED("Spouse Died");

    private final String displayName;

    MarriageEndReason(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the display name for this reason.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }
}
