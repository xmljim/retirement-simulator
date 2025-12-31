package io.github.xmljim.retirement.simulation.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;
import io.github.xmljim.retirement.domain.model.InvestmentAccount;
import io.github.xmljim.retirement.domain.value.AccountSnapshot;
import io.github.xmljim.retirement.domain.value.AssetAllocation;

@DisplayName("AccountState")
class AccountStateTest {

    private InvestmentAccount testAccount;

    @BeforeEach
    void setUp() {
        testAccount = InvestmentAccount.builder()
                .id("test-account-1")
                .name("Test 401k")
                .accountType(AccountType.TRADITIONAL_401K)
                .balance(new BigDecimal("100000.00"))
                .allocation(AssetAllocation.balanced())
                .preRetirementReturnRate(new BigDecimal("0.07"))
                .build();
    }

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create from investment account")
        void shouldCreateFromInvestmentAccount() {
            AccountState state = new AccountState(testAccount);

            assertEquals("test-account-1", state.getAccountId());
            assertEquals("Test 401k", state.getAccountName());
            assertEquals(new BigDecimal("100000.00"), state.getCurrentBalance());
        }

        @Test
        @DisplayName("should reject null account")
        void shouldRejectNullAccount() {
            assertThrows(MissingRequiredFieldException.class,
                    () -> new AccountState(null));
        }

        @Test
        @DisplayName("getAccount should return underlying account")
        void getAccountShouldReturnUnderlyingAccount() {
            AccountState state = new AccountState(testAccount);

            assertNotNull(state.getAccount());
            assertEquals(testAccount, state.getAccount());
        }
    }

    @Nested
    @DisplayName("Deposit")
    class Deposit {

        @Test
        @DisplayName("should add deposit to balance")
        void shouldAddDepositToBalance() {
            AccountState state = new AccountState(testAccount);
            state.deposit(new BigDecimal("5000.00"));

            assertEquals(new BigDecimal("105000.00"), state.getCurrentBalance());
        }

        @Test
        @DisplayName("should handle null deposit")
        void shouldHandleNullDeposit() {
            AccountState state = new AccountState(testAccount);
            state.deposit(null);

            assertEquals(new BigDecimal("100000.00"), state.getCurrentBalance());
        }

        @Test
        @DisplayName("should reject negative deposit")
        void shouldRejectNegativeDeposit() {
            AccountState state = new AccountState(testAccount);

            assertThrows(ValidationException.class,
                    () -> state.deposit(new BigDecimal("-100.00")));
        }
    }

    @Nested
    @DisplayName("Withdraw")
    class Withdraw {

        @Test
        @DisplayName("should subtract withdrawal from balance")
        void shouldSubtractWithdrawalFromBalance() {
            AccountState state = new AccountState(testAccount);
            BigDecimal actual = state.withdraw(new BigDecimal("10000.00"));

            assertEquals(new BigDecimal("10000.00"), actual);
            assertEquals(new BigDecimal("90000.00"), state.getCurrentBalance());
        }

        @Test
        @DisplayName("should return partial withdrawal if insufficient balance")
        void shouldReturnPartialWithdrawal() {
            AccountState state = new AccountState(testAccount);
            BigDecimal actual = state.withdraw(new BigDecimal("150000.00"));

            assertEquals(new BigDecimal("100000.00"), actual);
            assertEquals(BigDecimal.ZERO.setScale(2), state.getCurrentBalance());
        }

        @Test
        @DisplayName("should handle null withdrawal")
        void shouldHandleNullWithdrawal() {
            AccountState state = new AccountState(testAccount);
            BigDecimal actual = state.withdraw(null);

            assertEquals(BigDecimal.ZERO, actual);
            assertEquals(new BigDecimal("100000.00"), state.getCurrentBalance());
        }

        @Test
        @DisplayName("should reject negative withdrawal")
        void shouldRejectNegativeWithdrawal() {
            AccountState state = new AccountState(testAccount);

            assertThrows(ValidationException.class,
                    () -> state.withdraw(new BigDecimal("-100.00")));
        }
    }

    @Nested
    @DisplayName("Apply Return")
    class ApplyReturn {

        @Test
        @DisplayName("should apply positive return")
        void shouldApplyPositiveReturn() {
            AccountState state = new AccountState(testAccount);
            BigDecimal returnAmt = state.applyReturn(new BigDecimal("0.01"));

            assertEquals(new BigDecimal("1000.00"), returnAmt);
            assertEquals(new BigDecimal("101000.00"), state.getCurrentBalance());
        }

        @Test
        @DisplayName("should apply negative return")
        void shouldApplyNegativeReturn() {
            AccountState state = new AccountState(testAccount);
            BigDecimal returnAmt = state.applyReturn(new BigDecimal("-0.05"));

            assertEquals(new BigDecimal("-5000.00"), returnAmt);
            assertEquals(new BigDecimal("95000.00"), state.getCurrentBalance());
        }

        @Test
        @DisplayName("should handle null rate")
        void shouldHandleNullRate() {
            AccountState state = new AccountState(testAccount);
            BigDecimal returnAmt = state.applyReturn(null);

            assertEquals(BigDecimal.ZERO, returnAmt);
            assertEquals(new BigDecimal("100000.00"), state.getCurrentBalance());
        }

        @Test
        @DisplayName("should not go negative from losses")
        void shouldNotGoNegativeFromLosses() {
            AccountState state = new AccountState(testAccount);
            state.applyReturn(new BigDecimal("-1.50")); // 150% loss

            assertEquals(0, BigDecimal.ZERO.compareTo(state.getCurrentBalance()));
        }
    }

    @Nested
    @DisplayName("Balance Status")
    class BalanceStatus {

        @Test
        @DisplayName("hasBalance should return true for positive balance")
        void hasBalanceShouldReturnTrueForPositive() {
            AccountState state = new AccountState(testAccount);
            assertTrue(state.hasBalance());
        }

        @Test
        @DisplayName("isDepleted should return true for zero balance")
        void isDepletedShouldReturnTrueForZero() {
            AccountState state = new AccountState(testAccount);
            state.withdraw(new BigDecimal("100000.00"));

            assertTrue(state.isDepleted());
            assertFalse(state.hasBalance());
        }
    }

    @Nested
    @DisplayName("Set Balance")
    class SetBalance {

        @Test
        @DisplayName("setBalance should update balance")
        void setBalanceShouldUpdateBalance() {
            AccountState state = new AccountState(testAccount);
            state.setBalance(new BigDecimal("50000.00"));

            assertEquals(new BigDecimal("50000.00"), state.getCurrentBalance());
        }

        @Test
        @DisplayName("setBalance should reject negative balance")
        void setBalanceShouldRejectNegative() {
            AccountState state = new AccountState(testAccount);

            assertThrows(ValidationException.class,
                    () -> state.setBalance(new BigDecimal("-100.00")));
        }

        @Test
        @DisplayName("setBalance with null should set to zero")
        void setBalanceWithNullShouldSetToZero() {
            AccountState state = new AccountState(testAccount);
            state.setBalance(null);

            assertEquals(0, BigDecimal.ZERO.compareTo(state.getCurrentBalance()));
        }
    }

    @Nested
    @DisplayName("ToString")
    class ToStringTests {

        @Test
        @DisplayName("toString should contain account info")
        void toStringShouldContainAccountInfo() {
            AccountState state = new AccountState(testAccount);

            String result = state.toString();

            assertTrue(result.contains("test-account-1"));
            assertTrue(result.contains("Test 401k"));
            assertTrue(result.contains("100000.00"));
        }
    }

    @Nested
    @DisplayName("Snapshot")
    class Snapshot {

        @Test
        @DisplayName("should create valid AccountSnapshot")
        void shouldCreateValidSnapshot() {
            AccountState state = new AccountState(testAccount);
            state.withdraw(new BigDecimal("20000.00"));

            AccountSnapshot snapshot = state.toSnapshot();

            assertNotNull(snapshot);
            assertEquals("test-account-1", snapshot.accountId());
            assertEquals("Test 401k", snapshot.accountName());
            assertEquals(AccountType.TRADITIONAL_401K, snapshot.accountType());
            assertEquals(new BigDecimal("80000.00"), snapshot.balance());
        }
    }
}
