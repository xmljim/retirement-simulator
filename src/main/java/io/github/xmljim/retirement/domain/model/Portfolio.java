package io.github.xmljim.retirement.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;
import io.github.xmljim.retirement.domain.value.AssetAllocation;

/**
 * Represents a portfolio containing multiple investment accounts.
 *
 * <p>A Portfolio is owned by a {@link PersonProfile} and contains a collection
 * of {@link InvestmentAccount} objects. It provides aggregation methods for
 * calculating total balances, balances by account type or tax treatment,
 * and overall asset allocation across all accounts.
 *
 * <p>Use the {@link Builder} to create instances:
 * <pre>{@code
 * Portfolio portfolio = Portfolio.builder()
 *     .owner(personProfile)
 *     .addAccount(account401k)
 *     .addAccount(rothIra)
 *     .build();
 * }</pre>
 */
public final class Portfolio {

    private static final int SCALE = 6;

    private final String id;
    private final PersonProfile owner;
    private final List<InvestmentAccount> accounts;

    private Portfolio(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.owner = builder.owner;
        this.accounts = Collections.unmodifiableList(new ArrayList<>(builder.accounts));
    }

    /**
     * Returns the unique identifier for this portfolio.
     *
     * @return the portfolio ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the owner of this portfolio.
     *
     * @return the owner's PersonProfile
     */
    public PersonProfile getOwner() {
        return owner;
    }

    /**
     * Returns an unmodifiable list of accounts in this portfolio.
     *
     * @return the list of investment accounts
     */
    public List<InvestmentAccount> getAccounts() {
        return accounts;
    }

    /**
     * Returns the number of accounts in this portfolio.
     *
     * @return the account count
     */
    public int getAccountCount() {
        return accounts.size();
    }

    /**
     * Indicates whether this portfolio has any accounts.
     *
     * @return true if the portfolio has at least one account
     */
    public boolean hasAccounts() {
        return !accounts.isEmpty();
    }

    /**
     * Finds an account by its ID.
     *
     * @param accountId the account ID to find
     * @return an Optional containing the account, or empty if not found
     */
    public Optional<InvestmentAccount> findAccountById(String accountId) {
        return accounts.stream()
            .filter(a -> a.getId().equals(accountId))
            .findFirst();
    }

    /**
     * Returns all accounts of a specific type.
     *
     * @param accountType the account type to filter by
     * @return list of accounts matching the type
     */
    public List<InvestmentAccount> getAccountsByType(AccountType accountType) {
        return accounts.stream()
            .filter(a -> a.getAccountType() == accountType)
            .collect(Collectors.toList());
    }

    /**
     * Returns all accounts with a specific tax treatment.
     *
     * @param taxTreatment the tax treatment to filter by
     * @return list of accounts matching the tax treatment
     */
    public List<InvestmentAccount> getAccountsByTaxTreatment(AccountType.TaxTreatment taxTreatment) {
        return accounts.stream()
            .filter(a -> a.getTaxTreatment() == taxTreatment)
            .collect(Collectors.toList());
    }

    // ==================== Balance Aggregation ====================

    /**
     * Calculates the total balance across all accounts.
     *
     * @return the total portfolio balance
     */
    public BigDecimal getTotalBalance() {
        return accounts.stream()
            .map(InvestmentAccount::getBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates the total balance for accounts of a specific type.
     *
     * @param accountType the account type to sum
     * @return the total balance for that account type
     */
    public BigDecimal getBalanceByType(AccountType accountType) {
        return accounts.stream()
            .filter(a -> a.getAccountType() == accountType)
            .map(InvestmentAccount::getBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates the total balance grouped by account type.
     *
     * @return a map of account type to total balance
     */
    public Map<AccountType, BigDecimal> getBalancesByType() {
        return accounts.stream()
            .collect(Collectors.groupingBy(
                InvestmentAccount::getAccountType,
                Collectors.reducing(
                    BigDecimal.ZERO,
                    InvestmentAccount::getBalance,
                    BigDecimal::add
                )
            ));
    }

    /**
     * Calculates the total balance for accounts with a specific tax treatment.
     *
     * @param taxTreatment the tax treatment to sum
     * @return the total balance for that tax treatment
     */
    public BigDecimal getBalanceByTaxTreatment(AccountType.TaxTreatment taxTreatment) {
        return accounts.stream()
            .filter(a -> a.getTaxTreatment() == taxTreatment)
            .map(InvestmentAccount::getBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates the total balance grouped by tax treatment.
     *
     * @return a map of tax treatment to total balance
     */
    public Map<AccountType.TaxTreatment, BigDecimal> getBalancesByTaxTreatment() {
        return accounts.stream()
            .collect(Collectors.groupingBy(
                InvestmentAccount::getTaxTreatment,
                Collectors.reducing(
                    BigDecimal.ZERO,
                    InvestmentAccount::getBalance,
                    BigDecimal::add
                )
            ));
    }

    // ==================== Allocation Methods ====================

    /**
     * Calculates the overall asset allocation weighted by account balances.
     *
     * <p>Each account's allocation is weighted by its balance relative to
     * the total portfolio balance.
     *
     * @return the weighted average asset allocation, or a zero allocation if empty
     */
    public AssetAllocation getOverallAllocation() {
        BigDecimal totalBalance = getTotalBalance();

        if (totalBalance.compareTo(BigDecimal.ZERO) == 0) {
            return AssetAllocation.of(0, 0, 100);
        }

        BigDecimal weightedStocks = BigDecimal.ZERO;
        BigDecimal weightedBonds = BigDecimal.ZERO;
        BigDecimal weightedCash = BigDecimal.ZERO;

        for (InvestmentAccount account : accounts) {
            BigDecimal weight = account.getBalance()
                    .divide(totalBalance, SCALE, RoundingMode.HALF_UP);

            weightedStocks = weightedStocks.add(
                    account.getAllocation().getStocksPercentage().multiply(weight));
            weightedBonds = weightedBonds.add(
                    account.getAllocation().getBondsPercentage().multiply(weight));
            weightedCash = weightedCash.add(
                    account.getAllocation().getCashPercentage().multiply(weight));
        }

        // Round stocks and bonds, cash gets remainder to ensure sum is exactly 100
        BigDecimal hundred = new BigDecimal("100");
        BigDecimal stocksRounded = weightedStocks.setScale(2, RoundingMode.HALF_UP);
        BigDecimal bondsRounded = weightedBonds.setScale(2, RoundingMode.HALF_UP);
        BigDecimal cashRounded = hundred.subtract(stocksRounded).subtract(bondsRounded);

        // Ensure no negative values from rounding errors
        if (cashRounded.compareTo(BigDecimal.ZERO) < 0) {
            cashRounded = BigDecimal.ZERO;
            bondsRounded = hundred.subtract(stocksRounded);
        }

        return AssetAllocation.of(
                stocksRounded.doubleValue(),
                bondsRounded.doubleValue(),
                cashRounded.doubleValue());
    }

    /**
     * Calculates the blended pre-retirement return rate weighted by balances.
     *
     * @return the weighted average pre-retirement return rate
     */
    public BigDecimal getBlendedPreRetirementReturnRate() {
        return calculateBlendedReturnRate(true);
    }

    /**
     * Calculates the blended post-retirement return rate weighted by balances.
     *
     * @return the weighted average post-retirement return rate
     */
    public BigDecimal getBlendedPostRetirementReturnRate() {
        return calculateBlendedReturnRate(false);
    }

    private BigDecimal calculateBlendedReturnRate(boolean preRetirement) {
        BigDecimal totalBalance = getTotalBalance();

        if (totalBalance.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal weightedReturn = BigDecimal.ZERO;

        for (InvestmentAccount account : accounts) {
            BigDecimal weight = account.getBalance()
                    .divide(totalBalance, SCALE, RoundingMode.HALF_UP);

            BigDecimal accountReturn = preRetirement
                    ? account.getPreRetirementReturnRate()
                    : account.getPostRetirementReturnRate();

            weightedReturn = weightedReturn.add(accountReturn.multiply(weight));
        }

        return weightedReturn.setScale(SCALE, RoundingMode.HALF_UP);
    }

    // ==================== Portfolio Modification ====================

    /**
     * Creates a new portfolio with an additional account.
     *
     * @param account the account to add
     * @return a new Portfolio with the account added
     * @throws IllegalArgumentException if account ID already exists
     */
    public Portfolio withAccount(InvestmentAccount account) {
        return toBuilder().addAccount(account).build();
    }

    /**
     * Creates a new portfolio without the specified account.
     *
     * @param accountId the ID of the account to remove
     * @return a new Portfolio without the account
     */
    public Portfolio withoutAccount(String accountId) {
        List<InvestmentAccount> filtered = accounts.stream()
            .filter(a -> !a.getId().equals(accountId))
            .collect(Collectors.toList());

        return toBuilder()
            .clearAccounts()
            .addAccounts(filtered)
            .build();
    }

    /**
     * Creates a new builder for Portfolio.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder initialized with values from this portfolio.
     *
     * @return a new builder with copied values
     */
    public Builder toBuilder() {
        return new Builder()
            .id(this.id)
            .owner(this.owner)
            .addAccounts(this.accounts);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Portfolio portfolio = (Portfolio) o;
        return Objects.equals(id, portfolio.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Portfolio{" +
                "id='" + id + '\'' +
                ", owner=" + owner.getName() +
                ", accounts=" + accounts.size() +
                ", totalBalance=" + getTotalBalance() +
                '}';
    }

    /**
     * Builder for creating Portfolio instances.
     */
    public static class Builder {
        private String id;
        private PersonProfile owner;
        private final List<InvestmentAccount> accounts = new ArrayList<>();
        private final Set<String> accountIds = new HashSet<>();

        /**
         * Sets the portfolio ID.
         *
         * @param id the portfolio ID
         * @return this builder
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the portfolio owner.
         *
         * @param owner the owner's PersonProfile
         * @return this builder
         */
        public Builder owner(PersonProfile owner) {
            this.owner = owner;
            return this;
        }

        /**
         * Adds an investment account to the portfolio.
         *
         * @param account the account to add
         * @return this builder
         * @throws MissingRequiredFieldException if account is null
         * @throws ValidationException if account ID already exists
         */
        public Builder addAccount(InvestmentAccount account) {
            MissingRequiredFieldException.requireNonNull(account, "account");

            if (accountIds.contains(account.getId())) {
                throw new ValidationException(
                    "Duplicate account ID: " + account.getId(), "account");
            }

            accounts.add(account);
            accountIds.add(account.getId());
            return this;
        }

        /**
         * Adds multiple investment accounts to the portfolio.
         *
         * @param accounts the accounts to add
         * @return this builder
         */
        public Builder addAccounts(List<InvestmentAccount> accounts) {
            accounts.forEach(this::addAccount);
            return this;
        }

        /**
         * Clears all accounts from the builder.
         *
         * @return this builder
         */
        public Builder clearAccounts() {
            this.accounts.clear();
            this.accountIds.clear();
            return this;
        }

        /**
         * Builds the Portfolio instance.
         *
         * @return a new Portfolio
         * @throws MissingRequiredFieldException if owner is not set
         */
        public Portfolio build() {
            validate();
            return new Portfolio(this);
        }

        private void validate() {
            MissingRequiredFieldException.requireNonNull(owner, "owner");
        }
    }
}
