package io.github.xmljim.retirement.simulation.income;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.model.PersonProfile;
import io.github.xmljim.retirement.domain.value.Annuity;
import io.github.xmljim.retirement.domain.value.OtherIncome;
import io.github.xmljim.retirement.domain.value.Pension;
import io.github.xmljim.retirement.domain.value.SocialSecurityBenefit;
import io.github.xmljim.retirement.domain.value.WorkingIncome;

/**
 * Associates income sources with a person for simulation.
 *
 * <p>An IncomeProfile bundles a PersonProfile with all their income sources:
 * <ul>
 *   <li>Working income (salary) - active during accumulation phase</li>
 *   <li>Social Security benefit - starts at claiming age</li>
 *   <li>Pensions - defined benefit plans</li>
 *   <li>Annuities - insurance contracts providing income</li>
 *   <li>Other income - rental, part-time work, royalties, etc.</li>
 * </ul>
 *
 * <p>Use the builder for construction:
 * <pre>{@code
 * IncomeProfile profile = IncomeProfile.builder()
 *     .person(personProfile)
 *     .workingIncome(salary)
 *     .socialSecurity(ssBenefit)
 *     .addPension(pension1)
 *     .addOtherIncome(rentalIncome)
 *     .build();
 * }</pre>
 *
 * @param person          the person profile
 * @param workingIncome   salary/working income (may be null if retired)
 * @param socialSecurity  Social Security benefit (may be null)
 * @param pensions        list of pension income sources
 * @param annuities       list of annuity income sources
 * @param otherIncomes    list of other income sources
 */
@SuppressFBWarnings(value = "EI_EXPOSE_REP",
    justification = "Lists are made unmodifiable in compact constructor")
public record IncomeProfile(
    PersonProfile person,
    WorkingIncome workingIncome,
    SocialSecurityBenefit socialSecurity,
    List<Pension> pensions,
    List<Annuity> annuities,
    List<OtherIncome> otherIncomes
) {

    // Compact constructor with validation and defensive copying
    public IncomeProfile {
        MissingRequiredFieldException.requireNonNull(person, "person");
        pensions = pensions != null ? Collections.unmodifiableList(pensions) : List.of();
        annuities = annuities != null ? Collections.unmodifiableList(annuities) : List.of();
        otherIncomes = otherIncomes != null ? Collections.unmodifiableList(otherIncomes) : List.of();
    }

    /**
     * Returns the working income if present.
     *
     * @return optional containing working income
     */
    public Optional<WorkingIncome> getWorkingIncome() {
        return Optional.ofNullable(workingIncome);
    }

    /**
     * Returns the Social Security benefit if present.
     *
     * @return optional containing SS benefit
     */
    public Optional<SocialSecurityBenefit> getSocialSecurity() {
        return Optional.ofNullable(socialSecurity);
    }

    /**
     * Creates a new builder.
     *
     * @return a builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating IncomeProfile instances.
     */
    public static class Builder {
        private PersonProfile person;
        private WorkingIncome workingIncome;
        private SocialSecurityBenefit socialSecurity;
        private final List<Pension> pensions = new ArrayList<>();
        private final List<Annuity> annuities = new ArrayList<>();
        private final List<OtherIncome> otherIncomes = new ArrayList<>();

        /**
         * Sets the person profile.
         *
         * @param person the person
         * @return this builder
         */
        public Builder person(PersonProfile person) {
            this.person = person;
            return this;
        }

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
         * @param benefit the SS benefit
         * @return this builder
         */
        public Builder socialSecurity(SocialSecurityBenefit benefit) {
            this.socialSecurity = benefit;
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
         * @param pensions the pensions to add
         * @return this builder
         */
        public Builder pensions(List<Pension> pensions) {
            if (pensions != null) {
                this.pensions.addAll(pensions);
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
         * @param annuities the annuities to add
         * @return this builder
         */
        public Builder annuities(List<Annuity> annuities) {
            if (annuities != null) {
                this.annuities.addAll(annuities);
            }
            return this;
        }

        /**
         * Adds an other income source.
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
         * @param incomes the other incomes to add
         * @return this builder
         */
        public Builder otherIncomes(List<OtherIncome> incomes) {
            if (incomes != null) {
                this.otherIncomes.addAll(incomes);
            }
            return this;
        }

        /**
         * Builds the IncomeProfile.
         *
         * @return a new IncomeProfile
         */
        public IncomeProfile build() {
            return new IncomeProfile(
                person,
                workingIncome,
                socialSecurity,
                new ArrayList<>(pensions),
                new ArrayList<>(annuities),
                new ArrayList<>(otherIncomes)
            );
        }
    }
}
