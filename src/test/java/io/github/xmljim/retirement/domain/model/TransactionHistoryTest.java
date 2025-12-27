package io.github.xmljim.retirement.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.enums.TransactionType;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;

@DisplayName("TransactionHistory Tests")
class TransactionHistoryTest {

    private static final String ACCOUNT_ID = "test-account-1";

    private Transaction createTransaction(YearMonth period, TransactionType type,
            BigDecimal contribution, BigDecimal returns, BigDecimal withdrawal) {
        return Transaction.builder()
            .accountId(ACCOUNT_ID)
            .accountType(AccountType.TRADITIONAL_401K)
            .period(period)
            .transactionType(type)
            .startBalance(new BigDecimal("100000"))
            .personalContribution(contribution)
            .investmentReturn(returns)
            .withdrawal(withdrawal)
            .build();
    }

    @Nested
    @DisplayName("Creation")
    class CreationTests {

        @Test
        @DisplayName("Should create empty history")
        void createEmpty() {
            TransactionHistory history = TransactionHistory.empty(ACCOUNT_ID);

            assertEquals(ACCOUNT_ID, history.getAccountId());
            assertTrue(history.isEmpty());
            assertEquals(0, history.size());
        }

        @Test
        @DisplayName("Should create from list of transactions")
        void createFromList() {
            Transaction t1 = createTransaction(YearMonth.of(2025, 1),
                TransactionType.CONTRIBUTION, new BigDecimal("500"), BigDecimal.ZERO, BigDecimal.ZERO);
            Transaction t2 = createTransaction(YearMonth.of(2025, 2),
                TransactionType.CONTRIBUTION, new BigDecimal("500"), BigDecimal.ZERO, BigDecimal.ZERO);

            TransactionHistory history = TransactionHistory.of(ACCOUNT_ID, List.of(t2, t1));

            assertEquals(2, history.size());
            // Should be sorted by period
            assertEquals(YearMonth.of(2025, 1), history.getFirst().get().getPeriod());
        }

        @Test
        @DisplayName("Should throw for null accountId")
        void nullAccountId() {
            assertThrows(MissingRequiredFieldException.class, () ->
                TransactionHistory.empty(null));
        }
    }

    @Nested
    @DisplayName("Adding Transactions")
    class AddTransactionTests {

        @Test
        @DisplayName("Should return new history when adding transaction")
        void addReturnsNewHistory() {
            TransactionHistory original = TransactionHistory.empty(ACCOUNT_ID);
            Transaction tx = createTransaction(YearMonth.of(2025, 1),
                TransactionType.CONTRIBUTION, new BigDecimal("500"), BigDecimal.ZERO, BigDecimal.ZERO);

            TransactionHistory updated = original.addTransaction(tx);

            assertNotSame(original, updated);
            assertTrue(original.isEmpty());
            assertEquals(1, updated.size());
        }

        @Test
        @DisplayName("Should maintain sorted order when adding")
        void maintainsSortedOrder() {
            Transaction t1 = createTransaction(YearMonth.of(2025, 3),
                TransactionType.CONTRIBUTION, new BigDecimal("500"), BigDecimal.ZERO, BigDecimal.ZERO);
            Transaction t2 = createTransaction(YearMonth.of(2025, 1),
                TransactionType.CONTRIBUTION, new BigDecimal("500"), BigDecimal.ZERO, BigDecimal.ZERO);

            TransactionHistory history = TransactionHistory.empty(ACCOUNT_ID)
                .addTransaction(t1)
                .addTransaction(t2);

            assertEquals(YearMonth.of(2025, 1), history.getFirst().get().getPeriod());
            assertEquals(YearMonth.of(2025, 3), history.getLatest().get().getPeriod());
        }

        @Test
        @DisplayName("Should throw for null transaction")
        void nullTransaction() {
            TransactionHistory history = TransactionHistory.empty(ACCOUNT_ID);
            assertThrows(MissingRequiredFieldException.class, () ->
                history.addTransaction(null));
        }
    }

    @Nested
    @DisplayName("Query Methods")
    class QueryTests {

        private TransactionHistory history;

        @BeforeEach
        void setUp() {
            Transaction t1 = createTransaction(YearMonth.of(2025, 1),
                TransactionType.CONTRIBUTION, new BigDecimal("500"), new BigDecimal("100"), BigDecimal.ZERO);
            Transaction t2 = createTransaction(YearMonth.of(2025, 2),
                TransactionType.CONTRIBUTION, new BigDecimal("500"), new BigDecimal("110"), BigDecimal.ZERO);
            Transaction t3 = createTransaction(YearMonth.of(2025, 3),
                TransactionType.WITHDRAWAL, BigDecimal.ZERO, new BigDecimal("120"), new BigDecimal("2000"));

            history = TransactionHistory.empty(ACCOUNT_ID)
                .addTransaction(t1)
                .addTransaction(t2)
                .addTransaction(t3);
        }

        @Test
        @DisplayName("Should get all transactions")
        void getAll() {
            List<Transaction> all = history.getAll();
            assertEquals(3, all.size());
        }

        @Test
        @DisplayName("Should get latest transaction")
        void getLatest() {
            assertTrue(history.getLatest().isPresent());
            assertEquals(YearMonth.of(2025, 3), history.getLatest().get().getPeriod());
        }

        @Test
        @DisplayName("Should get first transaction")
        void getFirst() {
            assertTrue(history.getFirst().isPresent());
            assertEquals(YearMonth.of(2025, 1), history.getFirst().get().getPeriod());
        }

        @Test
        @DisplayName("Should get transaction for specific period")
        void getForPeriod() {
            assertTrue(history.getForPeriod(YearMonth.of(2025, 2)).isPresent());
            assertFalse(history.getForPeriod(YearMonth.of(2025, 6)).isPresent());
        }

        @Test
        @DisplayName("Should get transactions in date range")
        void getInRange() {
            List<Transaction> range = history.getInRange(
                YearMonth.of(2025, 1), YearMonth.of(2025, 2));
            assertEquals(2, range.size());
        }

        @Test
        @DisplayName("Should get transactions by type")
        void getByType() {
            List<Transaction> contributions = history.getByType(TransactionType.CONTRIBUTION);
            List<Transaction> withdrawals = history.getByType(TransactionType.WITHDRAWAL);

            assertEquals(2, contributions.size());
            assertEquals(1, withdrawals.size());
        }

        @Test
        @DisplayName("Should get balance at period")
        void getBalanceAt() {
            assertTrue(history.getBalanceAt(YearMonth.of(2025, 2)).isPresent());
            // Balance at a future period should return latest known
            assertTrue(history.getBalanceAt(YearMonth.of(2025, 12)).isPresent());
        }

        @Test
        @DisplayName("Should return empty for queries on empty history")
        void emptyHistoryQueries() {
            TransactionHistory empty = TransactionHistory.empty(ACCOUNT_ID);

            assertFalse(empty.getLatest().isPresent());
            assertFalse(empty.getFirst().isPresent());
            assertTrue(empty.getAll().isEmpty());
        }
    }

    @Nested
    @DisplayName("Aggregation Methods")
    class AggregationTests {

        private TransactionHistory history;

        @BeforeEach
        void setUp() {
            Transaction t1 = Transaction.builder()
                .accountId(ACCOUNT_ID)
                .accountType(AccountType.TRADITIONAL_401K)
                .period(YearMonth.of(2025, 1))
                .transactionType(TransactionType.CONTRIBUTION)
                .startBalance(new BigDecimal("100000"))
                .personalContribution(new BigDecimal("500"))
                .employerContribution(new BigDecimal("150"))
                .investmentReturn(new BigDecimal("583.33"))
                .build();

            Transaction t2 = Transaction.builder()
                .accountId(ACCOUNT_ID)
                .accountType(AccountType.TRADITIONAL_401K)
                .period(YearMonth.of(2025, 2))
                .transactionType(TransactionType.CONTRIBUTION)
                .startBalance(t1.getEndBalance())
                .personalContribution(new BigDecimal("500"))
                .employerContribution(new BigDecimal("150"))
                .investmentReturn(new BigDecimal("590.00"))
                .build();

            Transaction t3 = Transaction.builder()
                .accountId(ACCOUNT_ID)
                .accountType(AccountType.TRADITIONAL_401K)
                .period(YearMonth.of(2025, 3))
                .transactionType(TransactionType.WITHDRAWAL)
                .startBalance(t2.getEndBalance())
                .investmentReturn(new BigDecimal("600.00"))
                .withdrawal(new BigDecimal("2000"))
                .build();

            history = TransactionHistory.of(ACCOUNT_ID, List.of(t1, t2, t3));
        }

        @Test
        @DisplayName("Should calculate total personal contributions")
        void totalPersonalContributions() {
            BigDecimal total = history.getTotalPersonalContributions(
                YearMonth.of(2025, 1), YearMonth.of(2025, 12));

            assertEquals(0, new BigDecimal("1000.00").compareTo(total));
        }

        @Test
        @DisplayName("Should calculate total employer contributions")
        void totalEmployerContributions() {
            BigDecimal total = history.getTotalEmployerContributions(
                YearMonth.of(2025, 1), YearMonth.of(2025, 12));

            assertEquals(0, new BigDecimal("300.00").compareTo(total));
        }

        @Test
        @DisplayName("Should calculate total contributions")
        void totalContributions() {
            BigDecimal total = history.getTotalContributions(
                YearMonth.of(2025, 1), YearMonth.of(2025, 12));

            assertEquals(0, new BigDecimal("1300.00").compareTo(total));
        }

        @Test
        @DisplayName("Should calculate total withdrawals")
        void totalWithdrawals() {
            BigDecimal total = history.getTotalWithdrawals(
                YearMonth.of(2025, 1), YearMonth.of(2025, 12));

            assertEquals(0, new BigDecimal("2000.00").compareTo(total));
        }

        @Test
        @DisplayName("Should calculate total returns")
        void totalReturns() {
            BigDecimal total = history.getTotalReturns(
                YearMonth.of(2025, 1), YearMonth.of(2025, 12));

            // 583.33 + 590.00 + 600.00 = 1773.33
            assertEquals(0, new BigDecimal("1773.33").compareTo(total));
        }

        @Test
        @DisplayName("Should calculate net change")
        void netChange() {
            BigDecimal net = history.getNetChange(
                YearMonth.of(2025, 1), YearMonth.of(2025, 12));

            // Net = contributions + returns - withdrawals
            // 1300 + 1773.33 - 2000 = 1073.33
            assertEquals(0, new BigDecimal("1073.33").compareTo(net));
        }

        @Test
        @DisplayName("Should handle partial date range")
        void partialRange() {
            BigDecimal total = history.getTotalPersonalContributions(
                YearMonth.of(2025, 1), YearMonth.of(2025, 1));

            assertEquals(0, new BigDecimal("500.00").compareTo(total));
        }

        @Test
        @DisplayName("Should return zero for empty range")
        void emptyRange() {
            BigDecimal total = history.getTotalContributions(
                YearMonth.of(2024, 1), YearMonth.of(2024, 12));

            assertEquals(0, BigDecimal.ZERO.compareTo(total));
        }
    }

    @Nested
    @DisplayName("Immutability")
    class ImmutabilityTests {

        @Test
        @DisplayName("Should not allow modification of returned list")
        void returnedListIsUnmodifiable() {
            Transaction tx = createTransaction(YearMonth.of(2025, 1),
                TransactionType.CONTRIBUTION, new BigDecimal("500"), BigDecimal.ZERO, BigDecimal.ZERO);

            TransactionHistory history = TransactionHistory.empty(ACCOUNT_ID)
                .addTransaction(tx);

            List<Transaction> all = history.getAll();
            assertThrows(UnsupportedOperationException.class, () -> all.clear());
        }
    }
}
