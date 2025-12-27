package io.github.xmljim.retirement.domain.enums;

/**
 * Types of annuities that provide retirement income.
 *
 * <p>Annuities are insurance contracts that provide guaranteed income,
 * typically in retirement. The type determines how payments are calculated
 * and when they begin.
 *
 * <p><b>Key distinctions:</b>
 * <ul>
 *   <li><b>Immediate vs Deferred:</b> When payments begin</li>
 *   <li><b>Fixed vs Variable:</b> How payments are determined</li>
 * </ul>
 */
public enum AnnuityType {

    /**
     * Fixed Immediate Annuity - payments start immediately with fixed amount.
     *
     * <p>Purchased with a lump sum, payments begin within one year.
     * Provides guaranteed income for life or a specified period.
     * Payment amount is fixed at purchase (may have COLA rider).
     */
    FIXED_IMMEDIATE("Fixed Immediate", true, true),

    /**
     * Fixed Deferred Annuity - payments start later with fixed amount.
     *
     * <p>Accumulates value during the deferral period, then converts
     * to guaranteed payments at annuitization. Payment amount is
     * determined when payments begin based on accumulated value and age.
     */
    FIXED_DEFERRED("Fixed Deferred", true, false),

    /**
     * Variable Annuity - payments vary based on investment performance.
     *
     * <p>Account value is invested in sub-accounts (similar to mutual funds).
     * Payments fluctuate based on investment returns. May include
     * optional guaranteed minimum income benefit (GMIB) rider.
     */
    VARIABLE("Variable", false, false),

    /**
     * Indexed Annuity - payments tied to market index with floor and cap.
     *
     * <p>Returns are linked to a market index (e.g., S&P 500) but with
     * downside protection (floor, typically 0%) and limited upside (cap).
     * Provides market participation with principal protection.
     */
    INDEXED("Indexed", false, false);

    private final String displayName;
    private final boolean fixedPayment;
    private final boolean immediate;

    AnnuityType(String displayName, boolean fixedPayment, boolean immediate) {
        this.displayName = displayName;
        this.fixedPayment = fixedPayment;
        this.immediate = immediate;
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
     * Indicates whether this annuity type has fixed (guaranteed) payments.
     *
     * @return true for fixed annuities, false for variable/indexed
     */
    public boolean isFixedPayment() {
        return fixedPayment;
    }

    /**
     * Indicates whether payments begin immediately.
     *
     * @return true for immediate annuities, false for deferred
     */
    public boolean isImmediate() {
        return immediate;
    }

    /**
     * Indicates whether this annuity has an accumulation phase.
     *
     * @return true for deferred annuities
     */
    public boolean hasAccumulationPhase() {
        return !immediate;
    }

    /**
     * Indicates whether payments vary based on market performance.
     *
     * @return true for variable and indexed annuities
     */
    public boolean hasMarketExposure() {
        return !fixedPayment;
    }
}
