package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.enums.ContributionType;
import io.github.xmljim.retirement.domain.enums.LimitCategory;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;

/**
 * Immutable record of a single contribution to a retirement account.
 *
 * <p>Tracks all details needed for YTD limit enforcement and reporting:
 * <ul>
 *   <li>Which account received the contribution</li>
 *   <li>Whether it was personal or employer contribution</li>
 *   <li>The contribution amount and date</li>
 *   <li>Whether it counts as a catch-up contribution</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * ContributionRecord record = ContributionRecord.builder()
 *     .accountId("401k-traditional")
 *     .accountType(AccountType.TRADITIONAL_401K)
 *     .source(ContributionType.PERSONAL)
 *     .amount(new BigDecimal("500"))
 *     .year(2025)
 *     .date(LocalDate.of(2025, 1, 15))
 *     .isCatchUp(false)
 *     .build();
 * }</pre>
 *
 * @param accountId the unique identifier of the target account
 * @param accountType the type of account (determines limit category)
 * @param source whether this is a PERSONAL or EMPLOYER contribution
 * @param amount the contribution amount (must be positive)
 * @param year the tax year for this contribution
 * @param date the date the contribution was made
 * @param isCatchUp true if this is a catch-up contribution (age 50+ or 55+ for HSA)
 */
public record ContributionRecord(
    String accountId,
    AccountType accountType,
    ContributionType source,
    BigDecimal amount,
    int year,
    LocalDate date,
    boolean isCatchUp
) {
    // Compact constructor for validation
    public ContributionRecord {
        MissingRequiredFieldException.requireNonNull(accountId, "accountId");
        if (accountId.isBlank()) {
            throw new MissingRequiredFieldException("accountId", "Account ID cannot be blank");
        }
        MissingRequiredFieldException.requireNonNull(accountType, "accountType");
        MissingRequiredFieldException.requireNonNull(source, "source");
        MissingRequiredFieldException.requireNonNull(amount, "amount");
        MissingRequiredFieldException.requireNonNull(date, "date");

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Amount must be non-negative", "amount");
        }

        if (year < 1900 || year > 2200) {
            throw new ValidationException("Year must be between 1900 and 2200", "year");
        }

        if (date.getYear() != year) {
            throw new ValidationException(
                "Date year (" + date.getYear() + ") must match contribution year (" + year + ")",
                "date");
        }
    }

    /**
     * Returns the limit category for this contribution's account type.
     *
     * @return the limit category, or null if no limits apply
     */
    public LimitCategory getLimitCategory() {
        return LimitCategory.fromAccountType(accountType);
    }

    /**
     * Indicates whether this is a personal (employee) contribution.
     *
     * @return true if source is PERSONAL
     */
    public boolean isPersonal() {
        return source == ContributionType.PERSONAL;
    }

    /**
     * Indicates whether this is an employer contribution.
     *
     * @return true if source is EMPLOYER
     */
    public boolean isEmployer() {
        return source == ContributionType.EMPLOYER;
    }

    /**
     * Creates a new builder for ContributionRecord.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating ContributionRecord instances.
     */
    public static class Builder {
        private String accountId;
        private AccountType accountType;
        private ContributionType source;
        private BigDecimal amount;
        private int year;
        private LocalDate date;
        private boolean isCatchUp;

        /**
         * Sets the account ID.
         *
         * @param accountId the account ID
         * @return this builder
         */
        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        /**
         * Sets the account type.
         *
         * @param accountType the account type
         * @return this builder
         */
        public Builder accountType(AccountType accountType) {
            this.accountType = accountType;
            return this;
        }

        /**
         * Sets the contribution source.
         *
         * @param source PERSONAL or EMPLOYER
         * @return this builder
         */
        public Builder source(ContributionType source) {
            this.source = source;
            return this;
        }

        /**
         * Sets the contribution amount.
         *
         * @param amount the amount
         * @return this builder
         */
        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        /**
         * Sets the contribution amount from a double.
         *
         * @param amount the amount
         * @return this builder
         */
        public Builder amount(double amount) {
            this.amount = BigDecimal.valueOf(amount);
            return this;
        }

        /**
         * Sets the contribution year.
         *
         * @param year the tax year
         * @return this builder
         */
        public Builder year(int year) {
            this.year = year;
            return this;
        }

        /**
         * Sets the contribution date.
         *
         * @param date the date
         * @return this builder
         */
        public Builder date(LocalDate date) {
            this.date = date;
            return this;
        }

        /**
         * Sets whether this is a catch-up contribution.
         *
         * @param isCatchUp true if catch-up
         * @return this builder
         */
        public Builder isCatchUp(boolean isCatchUp) {
            this.isCatchUp = isCatchUp;
            return this;
        }

        /**
         * Builds the ContributionRecord.
         *
         * @return a new ContributionRecord
         */
        public ContributionRecord build() {
            return new ContributionRecord(
                accountId, accountType, source, amount, year, date, isCatchUp);
        }
    }
}
