package io.github.xmljim.retirement.domain.model;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import io.github.xmljim.retirement.domain.exception.InvalidDateRangeException;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;

/**
 * Represents an individual in the retirement simulation.
 *
 * <p>A PersonProfile is the foundational entity containing personal information,
 * retirement planning parameters, and links to financial data. Profiles can be
 * linked to model couples for joint retirement planning.
 *
 * <p>Use the {@link Builder} to create instances:
 * <pre>{@code
 * PersonProfile profile = PersonProfile.builder()
 *     .name("John Doe")
 *     .dateOfBirth(LocalDate.of(1970, 5, 15))
 *     .retirementDate(LocalDate.of(2035, 1, 1))
 *     .lifeExpectancy(90)
 *     .build();
 * }</pre>
 */
public final class PersonProfile {

    private final String id;
    private final String name;
    private final LocalDate dateOfBirth;
    private final LocalDate retirementDate;
    private final int lifeExpectancy;
    private final LocalDate socialSecurityStartDate;
    private final PersonProfile spouse;

    private PersonProfile(Builder builder) {
        // Validation is performed in Builder.build() before this constructor is called,
        // so no exceptions are thrown here (avoiding CT_CONSTRUCTOR_THROW)
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.name = builder.name;
        this.dateOfBirth = builder.dateOfBirth;
        this.retirementDate = builder.retirementDate;
        this.lifeExpectancy = builder.lifeExpectancy;
        this.socialSecurityStartDate = builder.socialSecurityStartDate;
        this.spouse = builder.spouse;
    }

    /**
     * Returns the unique identifier for this profile.
     *
     * @return the profile ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the person's name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the person's date of birth.
     *
     * @return the date of birth
     */
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    /**
     * Returns the planned retirement date.
     *
     * @return the retirement date
     */
    public LocalDate getRetirementDate() {
        return retirementDate;
    }

    /**
     * Returns the life expectancy in years.
     *
     * @return the life expectancy
     */
    public int getLifeExpectancy() {
        return lifeExpectancy;
    }

    /**
     * Returns the Social Security benefits start date, if specified.
     *
     * @return optional containing the Social Security start date
     */
    public Optional<LocalDate> getSocialSecurityStartDate() {
        return Optional.ofNullable(socialSecurityStartDate);
    }

    /**
     * Returns the linked spouse profile, if any.
     *
     * @return optional containing the spouse profile
     */
    public Optional<PersonProfile> getSpouse() {
        return Optional.ofNullable(spouse);
    }

    /**
     * Calculates the person's age as of the given date.
     *
     * @param asOfDate the date to calculate age for
     * @return the age in years
     */
    public int getAge(LocalDate asOfDate) {
        return Period.between(dateOfBirth, asOfDate).getYears();
    }

    /**
     * Calculates the person's current age.
     *
     * @return the current age in years
     */
    public int getCurrentAge() {
        return getAge(LocalDate.now());
    }

    /**
     * Calculates the age at retirement.
     *
     * @return the retirement age in years
     */
    public int getRetirementAge() {
        return getAge(retirementDate);
    }

    /**
     * Determines if the person is retired as of the given date.
     *
     * @param asOfDate the date to check
     * @return true if retired
     */
    public boolean isRetired(LocalDate asOfDate) {
        return !asOfDate.isBefore(retirementDate);
    }

    /**
     * Calculates the projected end date based on life expectancy.
     *
     * @return the projected end date
     */
    public LocalDate getProjectedEndDate() {
        return dateOfBirth.plusYears(lifeExpectancy);
    }

    /**
     * Indicates whether this profile has a linked spouse.
     *
     * @return true if spouse is linked
     */
    public boolean hasSpouse() {
        return spouse != null;
    }

    /**
     * Creates a new builder for PersonProfile.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder initialized with values from this profile.
     *
     * @return a new builder with copied values
     */
    public Builder toBuilder() {
        return new Builder()
            .id(this.id)
            .name(this.name)
            .dateOfBirth(this.dateOfBirth)
            .retirementDate(this.retirementDate)
            .lifeExpectancy(this.lifeExpectancy)
            .socialSecurityStartDate(this.socialSecurityStartDate)
            .spouse(this.spouse);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PersonProfile that = (PersonProfile) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PersonProfile{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", retirementDate=" + retirementDate +
                ", lifeExpectancy=" + lifeExpectancy +
                '}';
    }

    /**
     * Builder for creating PersonProfile instances.
     */
    public static class Builder {
        private String id;
        private String name;
        private LocalDate dateOfBirth;
        private LocalDate retirementDate;
        private int lifeExpectancy = 90;
        private LocalDate socialSecurityStartDate;
        private PersonProfile spouse;

        /**
         * Sets the profile ID.
         *
         * @param id the profile ID
         * @return this builder
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the person's name.
         *
         * @param name the name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the date of birth.
         *
         * @param dateOfBirth the date of birth
         * @return this builder
         */
        public Builder dateOfBirth(LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        /**
         * Sets the planned retirement date.
         *
         * @param retirementDate the retirement date
         * @return this builder
         */
        public Builder retirementDate(LocalDate retirementDate) {
            this.retirementDate = retirementDate;
            return this;
        }

        /**
         * Sets the life expectancy in years.
         *
         * @param lifeExpectancy the life expectancy (default: 90)
         * @return this builder
         */
        public Builder lifeExpectancy(int lifeExpectancy) {
            this.lifeExpectancy = lifeExpectancy;
            return this;
        }

        /**
         * Sets the Social Security benefits start date.
         *
         * @param socialSecurityStartDate the SS start date
         * @return this builder
         */
        public Builder socialSecurityStartDate(LocalDate socialSecurityStartDate) {
            this.socialSecurityStartDate = socialSecurityStartDate;
            return this;
        }

        /**
         * Links a spouse profile.
         *
         * @param spouse the spouse profile
         * @return this builder
         */
        public Builder spouse(PersonProfile spouse) {
            this.spouse = spouse;
            return this;
        }

        /**
         * Builds the PersonProfile instance.
         *
         * @return a new PersonProfile
         * @throws MissingRequiredFieldException if required fields are missing
         * @throws InvalidDateRangeException if date relationships are invalid
         */
        public PersonProfile build() {
            validate();
            return new PersonProfile(this);
        }

        private void validate() {
            MissingRequiredFieldException.requireNonNull(name, "name");
            MissingRequiredFieldException.requireNonNull(dateOfBirth, "dateOfBirth");
            MissingRequiredFieldException.requireNonNull(retirementDate, "retirementDate");

            if (retirementDate.isBefore(dateOfBirth)) {
                throw InvalidDateRangeException.dateMustBeAfter("Retirement date", "date of birth");
            }
            if (socialSecurityStartDate != null && socialSecurityStartDate.isBefore(dateOfBirth)) {
                throw InvalidDateRangeException.dateMustBeAfter("Social Security start date", "date of birth");
            }
        }
    }
}
