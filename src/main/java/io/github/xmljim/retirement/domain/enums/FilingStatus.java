package io.github.xmljim.retirement.domain.enums;

/**
 * Federal tax filing status for IRS calculations.
 *
 * <p>Filing status affects many tax-related calculations including:
 * <ul>
 *   <li>Traditional IRA deductibility phase-out ranges</li>
 *   <li>Roth IRA contribution eligibility phase-out ranges</li>
 *   <li>Tax bracket thresholds</li>
 *   <li>Standard deduction amounts</li>
 * </ul>
 *
 * <p>Special considerations:
 * <ul>
 *   <li>{@link #MARRIED_FILING_SEPARATELY} has unique restrictions for Roth IRA
 *       (phase-out starts at $0 if living with spouse)</li>
 *   <li>{@link #QUALIFYING_SURVIVING_SPOUSE} uses MFJ thresholds</li>
 * </ul>
 *
 * @see <a href="https://www.irs.gov/filing/filing-status">IRS Filing Status</a>
 */
public enum FilingStatus {

    /**
     * Single filer - unmarried, divorced, or legally separated.
     */
    SINGLE("Single"),

    /**
     * Married Filing Jointly - married couple files one joint return.
     *
     * <p>This status typically has the highest income thresholds for
     * contribution phase-outs.
     */
    MARRIED_FILING_JOINTLY("Married Filing Jointly"),

    /**
     * Married Filing Separately - married couple files separate returns.
     *
     * <p>This status has significant restrictions for retirement contributions:
     * <ul>
     *   <li>Roth IRA phase-out starts at $0 (if living with spouse)</li>
     *   <li>Traditional IRA deductibility reduced</li>
     * </ul>
     *
     * <p>Note: If not living with spouse during the year, the filer may
     * use SINGLE status thresholds for IRA phase-outs.
     */
    MARRIED_FILING_SEPARATELY("Married Filing Separately"),

    /**
     * Head of Household - unmarried taxpayer who pays more than half
     * the cost of maintaining a home for a qualifying person.
     *
     * <p>Uses same IRA phase-out thresholds as SINGLE for most purposes.
     */
    HEAD_OF_HOUSEHOLD("Head of Household"),

    /**
     * Qualifying Surviving Spouse (Widow/Widower) - surviving spouse
     * with dependent child, for up to 2 years after spouse's death.
     *
     * <p>Uses same thresholds as MARRIED_FILING_JOINTLY.
     */
    QUALIFYING_SURVIVING_SPOUSE("Qualifying Surviving Spouse");

    private final String displayName;

    FilingStatus(String displayName) {
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
     * Returns whether this status uses SINGLE filer thresholds for IRA phase-outs.
     *
     * @return true for SINGLE and HEAD_OF_HOUSEHOLD
     */
    public boolean usesSingleThresholds() {
        return this == SINGLE || this == HEAD_OF_HOUSEHOLD;
    }

    /**
     * Returns whether this status uses MARRIED_FILING_JOINTLY thresholds.
     *
     * @return true for MFJ and QUALIFYING_SURVIVING_SPOUSE
     */
    public boolean usesJointThresholds() {
        return this == MARRIED_FILING_JOINTLY || this == QUALIFYING_SURVIVING_SPOUSE;
    }

    /**
     * Returns whether this status has special restrictions.
     *
     * <p>MARRIED_FILING_SEPARATELY has unique phase-out rules
     * that differ significantly from other statuses.
     *
     * @return true for MARRIED_FILING_SEPARATELY
     */
    public boolean hasSpecialRestrictions() {
        return this == MARRIED_FILING_SEPARATELY;
    }
}
