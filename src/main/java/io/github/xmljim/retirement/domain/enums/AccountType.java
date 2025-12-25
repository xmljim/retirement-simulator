package io.github.xmljim.retirement.domain.enums;

/**
 * Investment account types supported by the retirement simulator.
 *
 * <p>Each account type has distinct tax treatment, contribution limits,
 * and withdrawal rules that affect retirement planning strategies.
 */
public enum AccountType {

    /**
     * Traditional 401(k) employer-sponsored retirement plan.
     *
     * <p>Tax treatment: Pre-tax contributions, taxed on withdrawal.
     * Subject to RMDs starting at age 73 (75 under SECURE 2.0).
     */
    TRADITIONAL_401K("Traditional 401(k)", TaxTreatment.PRE_TAX, true, true),

    /**
     * Roth 401(k) employer-sponsored retirement plan.
     *
     * <p>Tax treatment: Post-tax contributions, tax-free qualified withdrawals.
     * Subject to RMDs (can be avoided by rolling into Roth IRA).
     */
    ROTH_401K("Roth 401(k)", TaxTreatment.ROTH, true, true),

    /**
     * Traditional Individual Retirement Account.
     *
     * <p>Tax treatment: Pre-tax contributions (if deductible), taxed on withdrawal.
     * Subject to RMDs starting at age 73 (75 under SECURE 2.0).
     */
    TRADITIONAL_IRA("Traditional IRA", TaxTreatment.PRE_TAX, false, true),

    /**
     * Roth Individual Retirement Account.
     *
     * <p>Tax treatment: Post-tax contributions, tax-free qualified withdrawals.
     * Not subject to RMDs during owner's lifetime.
     */
    ROTH_IRA("Roth IRA", TaxTreatment.ROTH, false, false),

    /**
     * Health Savings Account.
     *
     * <p>Tax treatment: Triple tax-advantaged - pre-tax contributions,
     * tax-free growth, tax-free withdrawals for qualified medical expenses.
     * After age 65, can withdraw for any purpose (taxed as income).
     */
    HSA("Health Savings Account", TaxTreatment.HSA, false, false),

    /**
     * Taxable brokerage account.
     *
     * <p>Tax treatment: Post-tax contributions, capital gains and dividends
     * taxed annually. No contribution limits or withdrawal restrictions.
     */
    TAXABLE_BROKERAGE("Taxable Brokerage", TaxTreatment.TAXABLE, false, false),

    /**
     * 403(b) retirement plan for public schools and non-profits.
     *
     * <p>Tax treatment: Similar to Traditional 401(k).
     * Subject to RMDs.
     */
    TRADITIONAL_403B("Traditional 403(b)", TaxTreatment.PRE_TAX, true, true),

    /**
     * Roth 403(b) retirement plan.
     *
     * <p>Tax treatment: Similar to Roth 401(k).
     */
    ROTH_403B("Roth 403(b)", TaxTreatment.ROTH, true, true),

    /**
     * 457(b) deferred compensation plan for government employees.
     *
     * <p>Tax treatment: Similar to Traditional 401(k).
     * No early withdrawal penalty (unique among retirement accounts).
     */
    TRADITIONAL_457B("Traditional 457(b)", TaxTreatment.PRE_TAX, true, true);

    private final String displayName;
    private final TaxTreatment taxTreatment;
    private final boolean employerSponsored;
    private final boolean subjectToRmd;

    AccountType(String displayName, TaxTreatment taxTreatment,
                boolean employerSponsored, boolean subjectToRmd) {
        this.displayName = displayName;
        this.taxTreatment = taxTreatment;
        this.employerSponsored = employerSponsored;
        this.subjectToRmd = subjectToRmd;
    }

    /**
     * Returns the human-readable display name for this account type.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the tax treatment for this account type.
     *
     * @return the tax treatment
     */
    public TaxTreatment getTaxTreatment() {
        return taxTreatment;
    }

    /**
     * Indicates whether this is an employer-sponsored account.
     *
     * <p>Employer-sponsored accounts may have employer matching contributions
     * and different contribution limits than individual accounts.
     *
     * @return true if employer-sponsored
     */
    public boolean isEmployerSponsored() {
        return employerSponsored;
    }

    /**
     * Indicates whether this account is subject to Required Minimum Distributions.
     *
     * @return true if subject to RMDs
     */
    public boolean isSubjectToRmd() {
        return subjectToRmd;
    }

    /**
     * Indicates whether contributions to this account are tax-deductible.
     *
     * @return true if contributions are pre-tax/deductible
     */
    public boolean isPreTax() {
        return taxTreatment == TaxTreatment.PRE_TAX || taxTreatment == TaxTreatment.HSA;
    }

    /**
     * Indicates whether qualified withdrawals from this account are tax-free.
     *
     * @return true if withdrawals are tax-free
     */
    public boolean isTaxFreeWithdrawal() {
        return taxTreatment == TaxTreatment.ROTH;
    }

    /**
     * Tax treatment categories for investment accounts.
     */
    public enum TaxTreatment {
        /**
         * Pre-tax contributions, taxed on withdrawal (Traditional accounts).
         */
        PRE_TAX,

        /**
         * Post-tax contributions, tax-free qualified withdrawals (Roth accounts).
         */
        ROTH,

        /**
         * Triple tax-advantaged (HSA only).
         */
        HSA,

        /**
         * Post-tax contributions, taxable gains and dividends (brokerage).
         */
        TAXABLE
    }
}
