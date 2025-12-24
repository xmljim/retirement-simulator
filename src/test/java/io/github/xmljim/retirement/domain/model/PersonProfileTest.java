package io.github.xmljim.retirement.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PersonProfile Tests")
class PersonProfileTest {

    private static final LocalDate DOB = LocalDate.of(1970, 5, 15);
    private static final LocalDate RETIREMENT_DATE = LocalDate.of(2035, 1, 1);
    private static final String NAME = "John Doe";

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should create profile with required fields")
        void createWithRequiredFields() {
            PersonProfile profile = PersonProfile.builder()
                    .name(NAME)
                    .dateOfBirth(DOB)
                    .retirementDate(RETIREMENT_DATE)
                    .build();

            assertEquals(NAME, profile.getName());
            assertEquals(DOB, profile.getDateOfBirth());
            assertEquals(RETIREMENT_DATE, profile.getRetirementDate());
            assertNotNull(profile.getId());
        }

        @Test
        @DisplayName("Should use default life expectancy of 90")
        void defaultLifeExpectancy() {
            PersonProfile profile = PersonProfile.builder()
                    .name(NAME)
                    .dateOfBirth(DOB)
                    .retirementDate(RETIREMENT_DATE)
                    .build();

            assertEquals(90, profile.getLifeExpectancy());
        }

        @Test
        @DisplayName("Should allow custom life expectancy")
        void customLifeExpectancy() {
            PersonProfile profile = PersonProfile.builder()
                    .name(NAME)
                    .dateOfBirth(DOB)
                    .retirementDate(RETIREMENT_DATE)
                    .lifeExpectancy(95)
                    .build();

            assertEquals(95, profile.getLifeExpectancy());
        }

        @Test
        @DisplayName("Should throw exception when name is missing")
        void missingName() {
            assertThrows(NullPointerException.class, () ->
                    PersonProfile.builder()
                            .dateOfBirth(DOB)
                            .retirementDate(RETIREMENT_DATE)
                            .build()
            );
        }

        @Test
        @DisplayName("Should throw exception when date of birth is missing")
        void missingDateOfBirth() {
            assertThrows(NullPointerException.class, () ->
                    PersonProfile.builder()
                            .name(NAME)
                            .retirementDate(RETIREMENT_DATE)
                            .build()
            );
        }

        @Test
        @DisplayName("Should throw exception when retirement date is missing")
        void missingRetirementDate() {
            assertThrows(NullPointerException.class, () ->
                    PersonProfile.builder()
                            .name(NAME)
                            .dateOfBirth(DOB)
                            .build()
            );
        }

        @Test
        @DisplayName("Should generate unique ID when not provided")
        void generatesUniqueId() {
            PersonProfile profile1 = createDefaultProfile();
            PersonProfile profile2 = createDefaultProfile();

            assertNotEquals(profile1.getId(), profile2.getId());
        }

        @Test
        @DisplayName("Should use provided ID when specified")
        void usesProvidedId() {
            String customId = "custom-id-123";
            PersonProfile profile = PersonProfile.builder()
                    .id(customId)
                    .name(NAME)
                    .dateOfBirth(DOB)
                    .retirementDate(RETIREMENT_DATE)
                    .build();

            assertEquals(customId, profile.getId());
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw exception when retirement date is before birth date")
        void retirementBeforeBirth() {
            assertThrows(IllegalArgumentException.class, () ->
                    PersonProfile.builder()
                            .name(NAME)
                            .dateOfBirth(DOB)
                            .retirementDate(DOB.minusYears(1))
                            .build()
            );
        }

        @Test
        @DisplayName("Should throw exception when SS start date is before birth date")
        void ssStartBeforeBirth() {
            assertThrows(IllegalArgumentException.class, () ->
                    PersonProfile.builder()
                            .name(NAME)
                            .dateOfBirth(DOB)
                            .retirementDate(RETIREMENT_DATE)
                            .socialSecurityStartDate(DOB.minusYears(1))
                            .build()
            );
        }
    }

    @Nested
    @DisplayName("Age Calculation Tests")
    class AgeCalculationTests {

        @Test
        @DisplayName("Should calculate age correctly")
        void calculateAge() {
            PersonProfile profile = createDefaultProfile();
            LocalDate testDate = LocalDate.of(2025, 5, 15);

            assertEquals(55, profile.getAge(testDate));
        }

        @Test
        @DisplayName("Should calculate age before birthday in year")
        void ageBeforeBirthday() {
            PersonProfile profile = createDefaultProfile();
            LocalDate beforeBirthday = LocalDate.of(2025, 3, 1);

            assertEquals(54, profile.getAge(beforeBirthday));
        }

        @Test
        @DisplayName("Should calculate retirement age")
        void retirementAge() {
            PersonProfile profile = createDefaultProfile();

            assertEquals(64, profile.getRetirementAge());
        }
    }

    @Nested
    @DisplayName("Retirement Status Tests")
    class RetirementStatusTests {

        @Test
        @DisplayName("Should not be retired before retirement date")
        void notRetiredBeforeDate() {
            PersonProfile profile = createDefaultProfile();

            assertFalse(profile.isRetired(RETIREMENT_DATE.minusDays(1)));
        }

        @Test
        @DisplayName("Should be retired on retirement date")
        void retiredOnDate() {
            PersonProfile profile = createDefaultProfile();

            assertTrue(profile.isRetired(RETIREMENT_DATE));
        }

        @Test
        @DisplayName("Should be retired after retirement date")
        void retiredAfterDate() {
            PersonProfile profile = createDefaultProfile();

            assertTrue(profile.isRetired(RETIREMENT_DATE.plusDays(1)));
        }
    }

    @Nested
    @DisplayName("Spouse Linking Tests")
    class SpouseLinkingTests {

        @Test
        @DisplayName("Should have no spouse by default")
        void noSpouseByDefault() {
            PersonProfile profile = createDefaultProfile();

            assertFalse(profile.hasSpouse());
            assertTrue(profile.getSpouse().isEmpty());
        }

        @Test
        @DisplayName("Should link spouse correctly")
        void linkSpouse() {
            PersonProfile spouse = PersonProfile.builder()
                    .name("Jane Doe")
                    .dateOfBirth(LocalDate.of(1972, 8, 20))
                    .retirementDate(LocalDate.of(2037, 1, 1))
                    .build();

            PersonProfile profile = PersonProfile.builder()
                    .name(NAME)
                    .dateOfBirth(DOB)
                    .retirementDate(RETIREMENT_DATE)
                    .spouse(spouse)
                    .build();

            assertTrue(profile.hasSpouse());
            assertTrue(profile.getSpouse().isPresent());
            assertEquals("Jane Doe", profile.getSpouse().get().getName());
        }
    }

    @Nested
    @DisplayName("Projected End Date Tests")
    class ProjectedEndDateTests {

        @Test
        @DisplayName("Should calculate projected end date from life expectancy")
        void projectedEndDate() {
            PersonProfile profile = PersonProfile.builder()
                    .name(NAME)
                    .dateOfBirth(DOB)
                    .retirementDate(RETIREMENT_DATE)
                    .lifeExpectancy(85)
                    .build();

            LocalDate expected = DOB.plusYears(85);
            assertEquals(expected, profile.getProjectedEndDate());
        }
    }

    @Nested
    @DisplayName("ToBuilder Tests")
    class ToBuilderTests {

        @Test
        @DisplayName("Should create builder with copied values")
        void toBuilderCopiesValues() {
            PersonProfile original = PersonProfile.builder()
                    .name(NAME)
                    .dateOfBirth(DOB)
                    .retirementDate(RETIREMENT_DATE)
                    .lifeExpectancy(85)
                    .build();

            PersonProfile copy = original.toBuilder().build();

            assertEquals(original.getId(), copy.getId());
            assertEquals(original.getName(), copy.getName());
            assertEquals(original.getDateOfBirth(), copy.getDateOfBirth());
            assertEquals(original.getRetirementDate(), copy.getRetirementDate());
            assertEquals(original.getLifeExpectancy(), copy.getLifeExpectancy());
        }

        @Test
        @DisplayName("Should allow modifying copied values")
        void toBuilderAllowsModification() {
            PersonProfile original = createDefaultProfile();
            PersonProfile modified = original.toBuilder()
                    .name("Modified Name")
                    .build();

            assertEquals("Modified Name", modified.getName());
            assertEquals(original.getDateOfBirth(), modified.getDateOfBirth());
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("Profiles with same ID should be equal")
        void sameIdEquals() {
            PersonProfile profile1 = PersonProfile.builder()
                    .id("same-id")
                    .name(NAME)
                    .dateOfBirth(DOB)
                    .retirementDate(RETIREMENT_DATE)
                    .build();

            PersonProfile profile2 = PersonProfile.builder()
                    .id("same-id")
                    .name("Different Name")
                    .dateOfBirth(DOB)
                    .retirementDate(RETIREMENT_DATE)
                    .build();

            assertEquals(profile1, profile2);
            assertEquals(profile1.hashCode(), profile2.hashCode());
        }

        @Test
        @DisplayName("Profiles with different IDs should not be equal")
        void differentIdNotEquals() {
            PersonProfile profile1 = createDefaultProfile();
            PersonProfile profile2 = createDefaultProfile();

            assertNotEquals(profile1, profile2);
        }
    }

    private PersonProfile createDefaultProfile() {
        return PersonProfile.builder()
                .name(NAME)
                .dateOfBirth(DOB)
                .retirementDate(RETIREMENT_DATE)
                .build();
    }
}
