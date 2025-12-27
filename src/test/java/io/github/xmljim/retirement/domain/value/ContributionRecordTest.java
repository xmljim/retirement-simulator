package io.github.xmljim.retirement.domain.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.enums.ContributionType;
import io.github.xmljim.retirement.domain.enums.LimitCategory;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;

@DisplayName("ContributionRecord Tests")
class ContributionRecordTest {

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should create valid record with builder")
        void createsValidRecord() {
            ContributionRecord record = ContributionRecord.builder()
                .accountId("401k-trad")
                .accountType(AccountType.TRADITIONAL_401K)
                .source(ContributionType.PERSONAL)
                .amount(new BigDecimal("500.00"))
                .year(2025)
                .date(LocalDate.of(2025, 1, 15))
                .isCatchUp(false)
                .build();

            assertEquals("401k-trad", record.accountId());
            assertEquals(AccountType.TRADITIONAL_401K, record.accountType());
            assertEquals(ContributionType.PERSONAL, record.source());
            assertEquals(0, new BigDecimal("500.00").compareTo(record.amount()));
            assertEquals(2025, record.year());
            assertEquals(LocalDate.of(2025, 1, 15), record.date());
            assertFalse(record.isCatchUp());
        }

        @Test
        @DisplayName("Should allow amount from double")
        void amountFromDouble() {
            ContributionRecord record = ContributionRecord.builder()
                .accountId("ira")
                .accountType(AccountType.TRADITIONAL_IRA)
                .source(ContributionType.PERSONAL)
                .amount(1000.50)
                .year(2025)
                .date(LocalDate.of(2025, 6, 1))
                .build();

            assertEquals(0, new BigDecimal("1000.5").compareTo(record.amount()));
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw for null accountId")
        void nullAccountIdThrows() {
            assertThrows(MissingRequiredFieldException.class, () ->
                ContributionRecord.builder()
                    .accountType(AccountType.TRADITIONAL_401K)
                    .source(ContributionType.PERSONAL)
                    .amount(100)
                    .year(2025)
                    .date(LocalDate.of(2025, 1, 1))
                    .build());
        }

        @Test
        @DisplayName("Should throw for blank accountId")
        void blankAccountIdThrows() {
            assertThrows(MissingRequiredFieldException.class, () ->
                ContributionRecord.builder()
                    .accountId("  ")
                    .accountType(AccountType.TRADITIONAL_401K)
                    .source(ContributionType.PERSONAL)
                    .amount(100)
                    .year(2025)
                    .date(LocalDate.of(2025, 1, 1))
                    .build());
        }

        @Test
        @DisplayName("Should throw for negative amount")
        void negativeAmountThrows() {
            assertThrows(ValidationException.class, () ->
                ContributionRecord.builder()
                    .accountId("401k")
                    .accountType(AccountType.TRADITIONAL_401K)
                    .source(ContributionType.PERSONAL)
                    .amount(-100)
                    .year(2025)
                    .date(LocalDate.of(2025, 1, 1))
                    .build());
        }

        @Test
        @DisplayName("Should throw when date year doesn't match")
        void dateMismatchThrows() {
            assertThrows(ValidationException.class, () ->
                ContributionRecord.builder()
                    .accountId("401k")
                    .accountType(AccountType.TRADITIONAL_401K)
                    .source(ContributionType.PERSONAL)
                    .amount(100)
                    .year(2025)
                    .date(LocalDate.of(2024, 12, 31))  // Wrong year
                    .build());
        }
    }

    @Nested
    @DisplayName("Helper Method Tests")
    class HelperMethodTests {

        @Test
        @DisplayName("Should return correct limit category")
        void getLimitCategory() {
            ContributionRecord record = ContributionRecord.builder()
                .accountId("401k")
                .accountType(AccountType.TRADITIONAL_401K)
                .source(ContributionType.PERSONAL)
                .amount(100)
                .year(2025)
                .date(LocalDate.of(2025, 1, 1))
                .build();

            assertEquals(LimitCategory.EMPLOYER_401K, record.getLimitCategory());
        }

        @Test
        @DisplayName("Should identify personal contributions")
        void isPersonal() {
            ContributionRecord personal = ContributionRecord.builder()
                .accountId("401k")
                .accountType(AccountType.TRADITIONAL_401K)
                .source(ContributionType.PERSONAL)
                .amount(100)
                .year(2025)
                .date(LocalDate.of(2025, 1, 1))
                .build();

            assertTrue(personal.isPersonal());
            assertFalse(personal.isEmployer());
        }

        @Test
        @DisplayName("Should identify employer contributions")
        void isEmployer() {
            ContributionRecord employer = ContributionRecord.builder()
                .accountId("401k")
                .accountType(AccountType.TRADITIONAL_401K)
                .source(ContributionType.EMPLOYER)
                .amount(100)
                .year(2025)
                .date(LocalDate.of(2025, 1, 1))
                .build();

            assertTrue(employer.isEmployer());
            assertFalse(employer.isPersonal());
        }
    }
}
