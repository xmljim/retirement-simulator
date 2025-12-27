package io.github.xmljim.retirement.domain.enums;

import java.math.BigDecimal;

/**
 * Pension payment forms representing survivor benefit options.
 *
 * <p>When electing pension benefits, retirees typically choose a payment form
 * that determines how benefits are paid during their lifetime and to survivors.
 * Each option involves a trade-off between higher initial payments and
 * survivor protection.
 *
 * <p><b>Important:</b> Survivor elections are typically irrevocable once
 * pension payments begin.
 *
 * <p>Typical actuarial reductions vary based on ages of retiree and beneficiary.
 * The default reductions provided are approximate guidelines; actual reductions
 * should be obtained from the pension plan administrator.
 */
public enum PensionPaymentForm {

    /**
     * Single Life Annuity - highest benefit, no survivor protection.
     *
     * <p>Provides the maximum monthly benefit but payments stop at death.
     * No benefit is paid to any survivor.
     */
    SINGLE_LIFE("Single Life", BigDecimal.ZERO, false),

    /**
     * Joint and 100% Survivor - full benefit continues to survivor.
     *
     * <p>Provides a reduced benefit during lifetime, but the same amount
     * continues to the designated beneficiary after death.
     * Typical reduction: 10-15% from single life amount.
     */
    JOINT_100("Joint & 100% Survivor", new BigDecimal("0.12"), true),

    /**
     * Joint and 75% Survivor - 75% of benefit continues to survivor.
     *
     * <p>Provides a reduced benefit during lifetime, with 75% of that
     * amount continuing to the beneficiary after death.
     * Typical reduction: 7-10% from single life amount.
     */
    JOINT_75("Joint & 75% Survivor", new BigDecimal("0.08"), true),

    /**
     * Joint and 50% Survivor - 50% of benefit continues to survivor.
     *
     * <p>Provides a reduced benefit during lifetime, with 50% of that
     * amount continuing to the beneficiary after death.
     * Typical reduction: 5-7% from single life amount.
     */
    JOINT_50("Joint & 50% Survivor", new BigDecimal("0.06"), true),

    /**
     * Period Certain - guarantees payments for a fixed period.
     *
     * <p>If the retiree dies before the period ends, payments continue
     * to the beneficiary for the remainder of the period.
     * Common periods: 10, 15, or 20 years.
     * Typical reduction: 3-8% depending on period length.
     */
    PERIOD_CERTAIN("Period Certain", new BigDecimal("0.05"), true);

    private final String displayName;
    private final BigDecimal typicalReduction;
    private final boolean hasSurvivorBenefit;

    PensionPaymentForm(String displayName, BigDecimal typicalReduction, boolean hasSurvivorBenefit) {
        this.displayName = displayName;
        this.typicalReduction = typicalReduction;
        this.hasSurvivorBenefit = hasSurvivorBenefit;
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
     * Returns the typical actuarial reduction from single life benefit.
     *
     * <p>This is an approximate reduction. Actual reductions vary by plan
     * and are based on the ages of the retiree and beneficiary.
     *
     * @return the typical reduction as a decimal (e.g., 0.10 for 10%)
     */
    public BigDecimal getTypicalReduction() {
        return typicalReduction;
    }

    /**
     * Indicates whether this payment form provides benefits to a survivor.
     *
     * @return true if benefits continue to a survivor after death
     */
    public boolean hasSurvivorBenefit() {
        return hasSurvivorBenefit;
    }

    /**
     * Returns the survivor benefit percentage for joint options.
     *
     * @return the survivor percentage (1.0, 0.75, 0.50) or 0 for non-joint options
     */
    public BigDecimal getSurvivorPercentage() {
        return switch (this) {
            case JOINT_100 -> BigDecimal.ONE;
            case JOINT_75 -> new BigDecimal("0.75");
            case JOINT_50 -> new BigDecimal("0.50");
            default -> BigDecimal.ZERO;
        };
    }

    /**
     * Calculates the adjusted benefit after applying the typical reduction.
     *
     * <p>This uses the default typical reduction. For accurate calculations,
     * use the actual reduction provided by the pension plan.
     *
     * @param singleLifeBenefit the single life (unreduced) benefit amount
     * @return the reduced benefit amount
     */
    public BigDecimal applyTypicalReduction(BigDecimal singleLifeBenefit) {
        if (this == SINGLE_LIFE) {
            return singleLifeBenefit;
        }
        BigDecimal reductionFactor = BigDecimal.ONE.subtract(typicalReduction);
        return singleLifeBenefit.multiply(reductionFactor);
    }
}
