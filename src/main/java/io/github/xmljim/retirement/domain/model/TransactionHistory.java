package io.github.xmljim.retirement.domain.model;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.github.xmljim.retirement.domain.enums.TransactionType;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;

/**
 * Immutable history of transactions for an investment account.
 *
 * <p>TransactionHistory provides:
 * <ul>
 *   <li>Ordered storage of transactions by period</li>
 *   <li>Query methods for filtering and retrieval</li>
 *   <li>Aggregation methods for totals over periods</li>
 * </ul>
 *
 * <p>This class is immutable. Adding a transaction returns a new
 * TransactionHistory instance with the added transaction.
 *
 * <p>Usage:
 * <pre>{@code
 * TransactionHistory history = TransactionHistory.empty(accountId);
 * history = history.addTransaction(transaction1);
 * history = history.addTransaction(transaction2);
 *
 * BigDecimal totalContributions = history.getTotalContributions(
 *     YearMonth.of(2025, 1), YearMonth.of(2025, 12));
 * }</pre>
 */
public final class TransactionHistory {

    private final String accountId;
    private final List<Transaction> transactions;

    private TransactionHistory(String accountId, List<Transaction> transactions) {
        this.accountId = accountId;
        this.transactions = Collections.unmodifiableList(new ArrayList<>(transactions));
    }

    /**
     * Creates an empty transaction history for an account.
     *
     * @param accountId the account ID
     * @return a new empty TransactionHistory
     */
    public static TransactionHistory empty(String accountId) {
        MissingRequiredFieldException.requireNonNull(accountId, "accountId");
        return new TransactionHistory(accountId, new ArrayList<>());
    }

    /**
     * Creates a transaction history from an existing list of transactions.
     *
     * @param accountId the account ID
     * @param transactions the list of transactions
     * @return a new TransactionHistory
     */
    public static TransactionHistory of(String accountId, List<Transaction> transactions) {
        MissingRequiredFieldException.requireNonNull(accountId, "accountId");
        List<Transaction> sorted = transactions == null
            ? new ArrayList<>()
            : transactions.stream()
                .sorted(Comparator.comparing(Transaction::getPeriod))
                .collect(Collectors.toList());
        return new TransactionHistory(accountId, sorted);
    }

    /**
     * Returns the account ID this history belongs to.
     *
     * @return the account ID
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Returns the number of transactions in the history.
     *
     * @return the transaction count
     */
    public int size() {
        return transactions.size();
    }

    /**
     * Returns true if the history has no transactions.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return transactions.isEmpty();
    }

    /**
     * Adds a transaction to the history.
     *
     * <p>Returns a new TransactionHistory with the transaction added.
     * The original history is unchanged.
     *
     * @param transaction the transaction to add
     * @return a new TransactionHistory with the transaction
     * @throws MissingRequiredFieldException if transaction is null
     */
    public TransactionHistory addTransaction(Transaction transaction) {
        MissingRequiredFieldException.requireNonNull(transaction, "transaction");

        List<Transaction> newList = new ArrayList<>(transactions);
        newList.add(transaction);
        newList.sort(Comparator.comparing(Transaction::getPeriod));
        return new TransactionHistory(accountId, newList);
    }

    /**
     * Returns all transactions in chronological order.
     *
     * @return unmodifiable list of all transactions
     */
    public List<Transaction> getAll() {
        return transactions;
    }

    /**
     * Returns the most recent transaction.
     *
     * @return Optional containing the latest transaction, or empty if none
     */
    public Optional<Transaction> getLatest() {
        if (transactions.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(transactions.get(transactions.size() - 1));
    }

    /**
     * Returns the first transaction.
     *
     * @return Optional containing the first transaction, or empty if none
     */
    public Optional<Transaction> getFirst() {
        if (transactions.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(transactions.get(0));
    }

    /**
     * Returns the transaction for a specific period.
     *
     * @param period the year-month to find
     * @return Optional containing the transaction, or empty if not found
     */
    public Optional<Transaction> getForPeriod(YearMonth period) {
        return transactions.stream()
            .filter(t -> t.getPeriod().equals(period))
            .findFirst();
    }

    /**
     * Returns transactions within a date range (inclusive).
     *
     * @param from the start period (inclusive)
     * @param to the end period (inclusive)
     * @return list of transactions in the range
     */
    public List<Transaction> getInRange(YearMonth from, YearMonth to) {
        return transactions.stream()
            .filter(t -> !t.getPeriod().isBefore(from) && !t.getPeriod().isAfter(to))
            .collect(Collectors.toList());
    }

    /**
     * Returns transactions of a specific type.
     *
     * @param type the transaction type to filter by
     * @return list of transactions of that type
     */
    public List<Transaction> getByType(TransactionType type) {
        return transactions.stream()
            .filter(t -> t.getTransactionType() == type)
            .collect(Collectors.toList());
    }

    /**
     * Returns the balance at the end of a specific period.
     *
     * <p>If no transaction exists for that period, returns the end balance
     * of the most recent transaction before that period.
     *
     * @param period the period to get balance for
     * @return Optional containing the balance, or empty if no transactions
     */
    public Optional<BigDecimal> getBalanceAt(YearMonth period) {
        return transactions.stream()
            .filter(t -> !t.getPeriod().isAfter(period))
            .max(Comparator.comparing(Transaction::getPeriod))
            .map(Transaction::getEndBalance);
    }

    // =========================================================================
    // Aggregation Methods
    // =========================================================================

    /**
     * Returns total personal contributions over a period.
     *
     * @param from the start period (inclusive)
     * @param to the end period (inclusive)
     * @return total personal contributions
     */
    public BigDecimal getTotalPersonalContributions(YearMonth from, YearMonth to) {
        return getInRange(from, to).stream()
            .map(Transaction::getPersonalContribution)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Returns total employer contributions over a period.
     *
     * @param from the start period (inclusive)
     * @param to the end period (inclusive)
     * @return total employer contributions
     */
    public BigDecimal getTotalEmployerContributions(YearMonth from, YearMonth to) {
        return getInRange(from, to).stream()
            .map(Transaction::getEmployerContribution)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Returns total contributions (personal + employer) over a period.
     *
     * @param from the start period (inclusive)
     * @param to the end period (inclusive)
     * @return total contributions
     */
    public BigDecimal getTotalContributions(YearMonth from, YearMonth to) {
        return getInRange(from, to).stream()
            .map(Transaction::getTotalContribution)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Returns total withdrawals over a period.
     *
     * @param from the start period (inclusive)
     * @param to the end period (inclusive)
     * @return total withdrawals
     */
    public BigDecimal getTotalWithdrawals(YearMonth from, YearMonth to) {
        return getInRange(from, to).stream()
            .map(Transaction::getWithdrawal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Returns total investment returns over a period.
     *
     * @param from the start period (inclusive)
     * @param to the end period (inclusive)
     * @return total investment returns
     */
    public BigDecimal getTotalReturns(YearMonth from, YearMonth to) {
        return getInRange(from, to).stream()
            .map(Transaction::getInvestmentReturn)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Returns the net change in balance over a period.
     *
     * @param from the start period (inclusive)
     * @param to the end period (inclusive)
     * @return net change (may be negative)
     */
    public BigDecimal getNetChange(YearMonth from, YearMonth to) {
        return getInRange(from, to).stream()
            .map(Transaction::getNetChange)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public String toString() {
        return "TransactionHistory{" +
            "accountId='" + accountId + '\'' +
            ", transactionCount=" + transactions.size() +
            '}';
    }
}
