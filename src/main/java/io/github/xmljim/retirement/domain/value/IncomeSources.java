package io.github.xmljim.retirement.domain.value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Container for all income sources associated with a person.
 *
 * <p>Aggregates various income types for use in retirement simulations:
 * <ul>
 *   <li>Working income (salary)</li>
 *   <li>Social Security benefits</li>
 *   <li>Pensions (defined benefit plans)</li>
 *   <li>Annuities</li>
 *   <li>Other income (rental, dividends, etc.)</li>
 * </ul>
 *
 * <p>This is an immutable value object. Use the {@link Builder} to create instances.
 */
public final class IncomeSources {

    private final WorkingIncome workingIncome;
    private final SocialSecurityBenefit socialSecurityBenefit;
    private final List<Pension> pensions;
    private final List<Annuity> annuities;
    private final List<OtherIncome> otherIncomes;

    private IncomeSources(Builder builder) {
        this.workingIncome = builder.workingIncome;
        this.socialSecurityBenefit = builder.socialSecurityBenefit;
        this.pensions = Collections.unmodifiableList(new ArrayList<>(builder.pensions));
        this.annuities = Collections.unmodifiableList(new ArrayList<>(builder.annuities));
        this.otherIncomes = Collections.unmodifiableList(new ArrayList<>(builder.otherIncomes));
    }

    /**
     * Returns the working income, if any.
     *
     * @return optional containing working income
     */
    public Optional<WorkingIncome> getWorkingIncome() {
        return Optional.ofNullable(workingIncome);
    }

    /**
     * Returns the Social Security benefit, if any.
     *
     * @return optional containing Social Security benefit
     */
    public Optional<SocialSecurityBenefit> getSocialSecurityBenefit() {
        return Optional.ofNullable(socialSecurityBenefit);
    }

    /**
     * Returns the list of pensions.
     *
     * @return unmodifiable list of pensions (may be empty)
     */
    public List<Pension> getPensions() {
        return pensions;
    }

    /**
     * Returns the list of annuities.
     *
     * @return unmodifiable list of annuities (may be empty)
     */
    public List<Annuity> getAnnuities() {
        return annuities;
    }

    /**
     * Returns the list of other income sources.
     *
     * @return unmodifiable list of other incomes (may be empty)
     */
    public List<OtherIncome> getOtherIncomes() {
        return otherIncomes;
    }

    /**
     * Returns whether there are any income sources configured.
     *
     * @return true if at least one income source is present
     */
    public boolean hasIncomeSources() {
        return workingIncome != null
            || socialSecurityBenefit != null
            || !pensions.isEmpty()
            || !annuities.isEmpty()
            || !otherIncomes.isEmpty();
    }

    /**
     * Creates an empty IncomeSources with no income configured.
     *
     * @return empty income sources
     */
    public static IncomeSources empty() {
        return new Builder().build();
    }

    /**
     * Creates a new builder.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating IncomeSources instances.
     */
    public static class Builder {
        private WorkingIncome workingIncome;
        private SocialSecurityBenefit socialSecurityBenefit;
        private final List<Pension> pensions = new ArrayList<>();
        private final List<Annuity> annuities = new ArrayList<>();
        private final List<OtherIncome> otherIncomes = new ArrayList<>();

        /**
         * Sets the working income.
         *
         * @param income the working income
         * @return this builder
         */
        public Builder workingIncome(WorkingIncome income) {
            this.workingIncome = income;
            return this;
        }

        /**
         * Sets the Social Security benefit.
         *
         * @param benefit the Social Security benefit
         * @return this builder
         */
        public Builder socialSecurityBenefit(SocialSecurityBenefit benefit) {
            this.socialSecurityBenefit = benefit;
            return this;
        }

        /**
         * Adds a pension.
         *
         * @param pension the pension to add
         * @return this builder
         */
        public Builder addPension(Pension pension) {
            if (pension != null) {
                this.pensions.add(pension);
            }
            return this;
        }

        /**
         * Adds multiple pensions.
         *
         * @param pensionList the pensions to add
         * @return this builder
         */
        public Builder pensions(List<Pension> pensionList) {
            if (pensionList != null) {
                this.pensions.addAll(pensionList);
            }
            return this;
        }

        /**
         * Adds an annuity.
         *
         * @param annuity the annuity to add
         * @return this builder
         */
        public Builder addAnnuity(Annuity annuity) {
            if (annuity != null) {
                this.annuities.add(annuity);
            }
            return this;
        }

        /**
         * Adds multiple annuities.
         *
         * @param annuityList the annuities to add
         * @return this builder
         */
        public Builder annuities(List<Annuity> annuityList) {
            if (annuityList != null) {
                this.annuities.addAll(annuityList);
            }
            return this;
        }

        /**
         * Adds other income.
         *
         * @param income the other income to add
         * @return this builder
         */
        public Builder addOtherIncome(OtherIncome income) {
            if (income != null) {
                this.otherIncomes.add(income);
            }
            return this;
        }

        /**
         * Adds multiple other income sources.
         *
         * @param incomeList the other incomes to add
         * @return this builder
         */
        public Builder otherIncomes(List<OtherIncome> incomeList) {
            if (incomeList != null) {
                this.otherIncomes.addAll(incomeList);
            }
            return this;
        }

        /**
         * Builds the IncomeSources instance.
         *
         * @return a new IncomeSources
         */
        public IncomeSources build() {
            return new IncomeSources(this);
        }
    }
}
