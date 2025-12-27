package io.github.xmljim.retirement.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.YearMonth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.enums.TransactionType;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.value.AssetAllocation;

@DisplayName("Transaction Tests")
class TransactionTest {

    private static final String ACCOUNT_ID = "test-account-1";
    private static final YearMonth JAN_2025 = YearMonth.of(2025, 1);
    private static final YearMonth FEB_2025 = YearMonth.of(2025, 2);

    private InvestmentAccount createTestAccount(double balance) {
        return InvestmentAccount.builder()
            .name("Test 401k")
            .accountType(AccountType.TRADITIONAL_401K)
            .balance(balance)
            .allocation(AssetAllocation.balanced())
            .preRetirementReturnRate(0.07)
            .build();
    }

    @Nested
    @DisplayName("Balance Calculations")
    class BalanceCalculationTests {

        @Test
        @DisplayName("Should calculate end balance with contributions only")
        void contributionsOnly() {
            Transaction tx = Transaction.builder()
                .accountId(ACCOUNT_ID)
                .accountType(AccountType.TRADITIONAL_401K)
                .period(JAN_2025)
                .transactionType(TransactionType.CONTRIBUTION)
                .startBalance(new BigDecimal("100000"))
                .personalContribution(new BigDecimal("500"))
                .employerContribution(new BigDecimal("150"))
                .build();

            // 100000 + 500 + 150 = 100650
            assertEquals(0, new BigDecimal("100650.00").compareTo(tx.getEndBalance()));
        }

        @Test
        @DisplayName("Should calculate end balance with all components")
        void allComponents() {
            Transaction tx = Transaction.builder()
                .accountId(ACCOUNT_ID)
                .accountType(AccountType.TRADITIONAL_401K)
                .period(JAN_2025)
                .transactionType(TransactionType.CONTRIBUTION)
                .startBalance(new BigDecimal("100000"))
                .personalContribution(new BigDecimal("500"))
                .employerContribution(new BigDecimal("150"))
                .investmentReturn(new BigDecimal("583.33"))
                .build();

            // 100000 + 500 + 150 + 583.33 = 101233.33
            assertEquals(0, new BigDecimal("101233.33").compareTo(tx.getEndBalance()));
        }

        @Test
        @DisplayName("Should calculate end balance with withdrawal")
        void withWithdrawal() {
            Transaction tx = Transaction.builder()
                .accountId(ACCOUNT_ID)
                .accountType(AccountType.TRADITIONAL_401K)
                .period(JAN_2025)
                .transactionType(TransactionType.WITHDRAWAL)
                .startBalance(new BigDecimal("500000"))
                .investmentReturn(new BigDecimal("2083.33"))
                .withdrawal(new BigDecimal("3000"))
                .build();

            // 500000 + 2083.33 - 3000 = 499083.33
            assertEquals(0, new BigDecimal("499083.33").compareTo(tx.getEndBalance()));
        }

        @Test
        @DisplayName("Should calculate total contribution")
        void totalContribution() {
            Transaction tx = Transaction.builder()
                .accountId(ACCOUNT_ID)
                .accountType(AccountType.TRADITIONAL_401K)
                .period(JAN_2025)
                .transactionType(TransactionType.CONTRIBUTION)
                .startBalance(new BigDecimal("100000"))
                .personalContribution(new BigDecimal("500"))
                .employerContribution(new BigDecimal("150"))
                .build();

            assertEquals(0, new BigDecimal("650.00").compareTo(tx.getTotalContribution()));
        }

        @Test
        @DisplayName("Should calculate net change")
        void netChange() {
            Transaction tx = Transaction.builder()
                .accountId(ACCOUNT_ID)
                .accountType(AccountType.TRADITIONAL_401K)
                .period(JAN_2025)
                .transactionType(TransactionType.CONTRIBUTION)
                .startBalance(new BigDecimal("100000"))
                .personalContribution(new BigDecimal("500"))
                .employerContribution(new BigDecimal("150"))
                .investmentReturn(new BigDecimal("583.33"))
                .build();

            // Net change = 101233.33 - 100000 = 1233.33
            assertEquals(0, new BigDecimal("1233.33").compareTo(tx.getNetChange()));
        }

        @Test
        @DisplayName("Should handle zero values gracefully")
        void zeroValues() {
            Transaction tx = Transaction.builder()
                .accountId(ACCOUNT_ID)
                .accountType(AccountType.TRADITIONAL_401K)
                .period(JAN_2025)
                .transactionType(TransactionType.CONTRIBUTION)
                .startBalance(new BigDecimal("100000"))
                .build();

            assertEquals(0, new BigDecimal("100000.00").compareTo(tx.getEndBalance()));
            assertEquals(0, BigDecimal.ZERO.compareTo(tx.getPersonalContribution()));
        }
    }

    @Nested
    @DisplayName("Transaction Chaining")
    class TransactionChainingTests {

        @Test
        @DisplayName("Should chain transactions with end balance becoming start balance")
        void chainTransactions() {
            Transaction first = Transaction.builder()
                .accountId(ACCOUNT_ID)
                .accountType(AccountType.TRADITIONAL_401K)
                .period(JAN_2025)
                .transactionType(TransactionType.CONTRIBUTION)
                .startBalance(new BigDecimal("100000"))
                .personalContribution(new BigDecimal("500"))
                .investmentReturn(new BigDecimal("583.33"))
                .build();

            Transaction second = first.nextTransaction(FEB_2025)
                .transactionType(TransactionType.CONTRIBUTION)
                .personalContribution(new BigDecimal("500"))
                .investmentReturn(new BigDecimal("590.00"))
                .build();

            // First end balance should be second start balance
            assertEquals(0, first.getEndBalance().compareTo(second.getStartBalance()));
            assertTrue(second.getPrevious().isPresent());
            assertEquals(first.getId(), second.getPrevious().get().getId());
        }

        @Test
        @DisplayName("Should identify first transaction in chain")
        void firstTransaction() {
            Transaction first = Transaction.builder()
                .accountId(ACCOUNT_ID)
                .accountType(AccountType.TRADITIONAL_401K)
                .period(JAN_2025)
                .transactionType(TransactionType.CONTRIBUTION)
                .startBalance(new BigDecimal("100000"))
                .build();

            assertTrue(first.isFirst());
            assertFalse(first.getPrevious().isPresent());
        }

        @Test
        @DisplayName("Should maintain chain through multiple transactions")
        void multipleChained() {
            Transaction t1 = Transaction.builder()
                .accountId(ACCOUNT_ID)
                .accountType(AccountType.TRADITIONAL_401K)
                .period(YearMonth.of(2025, 1))
                .transactionType(TransactionType.CONTRIBUTION)
                .startBalance(new BigDecimal("100000"))
                .personalContribution(new BigDecimal("500"))
                .build();

            Transaction t2 = t1.nextTransaction(YearMonth.of(2025, 2))
                .transactionType(TransactionType.CONTRIBUTION)
                .personalContribution(new BigDecimal("500"))
                .build();

            Transaction t3 = t2.nextTransaction(YearMonth.of(2025, 3))
                .transactionType(TransactionType.CONTRIBUTION)
                .personalContribution(new BigDecimal("500"))
                .build();

            // Verify chain integrity
            assertEquals(0, new BigDecimal("101500.00").compareTo(t3.getEndBalance()));
            assertFalse(t3.isFirst());
            assertTrue(t3.getPrevious().isPresent());
            assertEquals(t2.getId(), t3.getPrevious().get().getId());
        }
    }

    @Nested
    @DisplayName("Builder with InvestmentAccount")
    class BuilderWithAccountTests {

        @Test
        @DisplayName("Should configure from InvestmentAccount")
        void forAccount() {
            InvestmentAccount account = createTestAccount(100000);

            Transaction tx = Transaction.builder()
                .forAccount(account)
                .period(JAN_2025)
                .transactionType(TransactionType.CONTRIBUTION)
                .personalContribution(new BigDecimal("500"))
                .build();

            assertEquals(account.getId(), tx.getAccountId());
            assertEquals(account.getAccountType(), tx.getAccountType());
            assertEquals(0, account.getBalance().compareTo(tx.getStartBalance()));
        }

        @Test
        @DisplayName("Should not override explicit start balance")
        void explicitStartBalance() {
            InvestmentAccount account = createTestAccount(100000);

            Transaction tx = Transaction.builder()
                .startBalance(new BigDecimal("50000"))
                .forAccount(account)
                .period(JAN_2025)
                .transactionType(TransactionType.CONTRIBUTION)
                .build();

            // Explicit start balance should be preserved
            assertEquals(0, new BigDecimal("50000.00").compareTo(tx.getStartBalance()));
        }
    }

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        @DisplayName("Should throw for missing accountId")
        void missingAccountId() {
            assertThrows(MissingRequiredFieldException.class, () ->
                Transaction.builder()
                    .accountType(AccountType.TRADITIONAL_401K)
                    .period(JAN_2025)
                    .transactionType(TransactionType.CONTRIBUTION)
                    .build());
        }

        @Test
        @DisplayName("Should throw for missing accountType")
        void missingAccountType() {
            assertThrows(MissingRequiredFieldException.class, () ->
                Transaction.builder()
                    .accountId(ACCOUNT_ID)
                    .period(JAN_2025)
                    .transactionType(TransactionType.CONTRIBUTION)
                    .build());
        }

        @Test
        @DisplayName("Should throw for missing period")
        void missingPeriod() {
            assertThrows(MissingRequiredFieldException.class, () ->
                Transaction.builder()
                    .accountId(ACCOUNT_ID)
                    .accountType(AccountType.TRADITIONAL_401K)
                    .transactionType(TransactionType.CONTRIBUTION)
                    .build());
        }

        @Test
        @DisplayName("Should throw for missing transactionType")
        void missingTransactionType() {
            assertThrows(MissingRequiredFieldException.class, () ->
                Transaction.builder()
                    .accountId(ACCOUNT_ID)
                    .accountType(AccountType.TRADITIONAL_401K)
                    .period(JAN_2025)
                    .build());
        }
    }

    @Nested
    @DisplayName("Properties")
    class PropertyTests {

        @Test
        @DisplayName("Should generate ID if not provided")
        void generatesId() {
            Transaction tx = Transaction.builder()
                .accountId(ACCOUNT_ID)
                .accountType(AccountType.TRADITIONAL_401K)
                .period(JAN_2025)
                .transactionType(TransactionType.CONTRIBUTION)
                .build();

            assertNotNull(tx.getId());
            assertFalse(tx.getId().isEmpty());
        }

        @Test
        @DisplayName("Should use provided ID")
        void usesProvidedId() {
            String customId = "custom-tx-id";
            Transaction tx = Transaction.builder()
                .id(customId)
                .accountId(ACCOUNT_ID)
                .accountType(AccountType.TRADITIONAL_401K)
                .period(JAN_2025)
                .transactionType(TransactionType.CONTRIBUTION)
                .build();

            assertEquals(customId, tx.getId());
        }

        @Test
        @DisplayName("Should scale amounts to 2 decimal places")
        void scalesAmounts() {
            Transaction tx = Transaction.builder()
                .accountId(ACCOUNT_ID)
                .accountType(AccountType.TRADITIONAL_401K)
                .period(JAN_2025)
                .transactionType(TransactionType.CONTRIBUTION)
                .startBalance(new BigDecimal("100000.999"))
                .personalContribution(new BigDecimal("500.555"))
                .build();

            assertEquals(2, tx.getStartBalance().scale());
            assertEquals(2, tx.getPersonalContribution().scale());
            assertEquals(2, tx.getEndBalance().scale());
        }
    }
}
