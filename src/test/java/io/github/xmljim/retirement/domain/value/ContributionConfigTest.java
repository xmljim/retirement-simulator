package io.github.xmljim.retirement.domain.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Month;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.ContributionType;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;

@DisplayName("ContributionConfig Tests")
class ContributionConfigTest {

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create personal contribution")
        void createPersonal() {
            ContributionConfig config = ContributionConfig.personal(0.10);

            assertEquals(ContributionType.PERSONAL, config.getContributionType());
            assertEquals(0, new BigDecimal("0.1").compareTo(config.getContributionRate()));
        }

        @Test
        @DisplayName("Should create employer contribution")
        void createEmployer() {
            ContributionConfig config = ContributionConfig.employer(0.04);

            assertEquals(ContributionType.EMPLOYER, config.getContributionType());
            assertEquals(0, new BigDecimal("0.04").compareTo(config.getContributionRate()));
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should build with all fields")
        void buildWithAllFields() {
            ContributionConfig config = ContributionConfig.builder()
                .contributionType(ContributionType.PERSONAL)
                .contributionRate(0.10)
                .incrementRate(0.01)
                .incrementMonth(Month.JUNE)
                .build();

            assertEquals(ContributionType.PERSONAL, config.getContributionType());
            assertEquals(0, new BigDecimal("0.1").compareTo(config.getContributionRate()));
            assertEquals(0, new BigDecimal("0.01").compareTo(config.getIncrementRate()));
            assertEquals(Month.JUNE, config.getIncrementMonth());
        }

        @Test
        @DisplayName("Should use default values")
        void defaultValues() {
            ContributionConfig config = ContributionConfig.builder()
                .contributionType(ContributionType.EMPLOYER)
                .build();

            assertEquals(0, BigDecimal.ZERO.compareTo(config.getContributionRate()));
            assertEquals(0, BigDecimal.ZERO.compareTo(config.getIncrementRate()));
            assertEquals(Month.JANUARY, config.getIncrementMonth());
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw when contribution type is null")
        void nullContributionType() {
            assertThrows(MissingRequiredFieldException.class, () ->
                ContributionConfig.builder()
                    .contributionRate(0.10)
                    .build());
        }

        @Test
        @DisplayName("Should throw for negative contribution rate")
        void negativeContributionRate() {
            assertThrows(ValidationException.class, () ->
                ContributionConfig.builder()
                    .contributionType(ContributionType.PERSONAL)
                    .contributionRate(-0.10)
                    .build());
        }

        @Test
        @DisplayName("Should throw for negative increment rate")
        void negativeIncrementRate() {
            assertThrows(ValidationException.class, () ->
                ContributionConfig.builder()
                    .contributionType(ContributionType.PERSONAL)
                    .contributionRate(0.10)
                    .incrementRate(-0.01)
                    .build());
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("Equal configs should be equal")
        void equalConfigs() {
            ContributionConfig c1 = ContributionConfig.personal(0.10);
            ContributionConfig c2 = ContributionConfig.personal(0.10);

            assertEquals(c1, c2);
            assertEquals(c1.hashCode(), c2.hashCode());
        }

        @Test
        @DisplayName("Different configs should not be equal")
        void differentConfigs() {
            ContributionConfig c1 = ContributionConfig.personal(0.10);
            ContributionConfig c2 = ContributionConfig.employer(0.04);

            assertNotEquals(c1, c2);
        }
    }
}
