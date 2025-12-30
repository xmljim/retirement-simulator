package io.github.xmljim.retirement.domain.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.model.InvestmentAccount;
import io.github.xmljim.retirement.domain.value.AccountSnapshot;
import io.github.xmljim.retirement.domain.value.AssetAllocation;

@DisplayName("StubSimulationView Tests")
class StubSimulationViewTest {

    private InvestmentAccount traditional401k;
    private InvestmentAccount rothIra;
    private AccountSnapshot snapshot401k;
    private AccountSnapshot snapshotRoth;

    @BeforeEach
    void setUp() {
        traditional401k = InvestmentAccount.builder()
                .name("401(k)")
                .accountType(AccountType.TRADITIONAL_401K)
                .balance(new BigDecimal("500000"))
                .allocation(AssetAllocation.of(60, 35, 5))
                .preRetirementReturnRate(0.07)
                .build();

        rothIra = InvestmentAccount.builder()
                .name("Roth IRA")
                .accountType(AccountType.ROTH_IRA)
                .balance(new BigDecimal("200000"))
                .allocation(AssetAllocation.of(70, 25, 5))
                .preRetirementReturnRate(0.08)
                .build();

        snapshot401k = AccountSnapshot.from(traditional401k);
        snapshotRoth = AccountSnapshot.from(rothIra);
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should create stub with accounts via builder")
        void createsWithAccounts() {
            StubSimulationView view = StubSimulationView.builder()
                    .addAccount(snapshot401k)
                    .addAccount(snapshotRoth)
                    .build();

            assertEquals(2, view.getAccountSnapshots().size());
            assertEquals(0, new BigDecimal("700000").compareTo(view.getTotalPortfolioBalance()));
        }

        @Test
        @DisplayName("Should accept InvestmentAccount directly")
        void acceptsInvestmentAccount() {
            StubSimulationView view = StubSimulationView.builder()
                    .addAccount(traditional401k)
                    .build();

            assertEquals(1, view.getAccountSnapshots().size());
            assertEquals("401(k)", view.getAccountSnapshots().getFirst().accountName());
        }

        @Test
        @DisplayName("Should set historical values")
        void setsHistoricalValues() {
            StubSimulationView view = StubSimulationView.builder()
                    .addAccount(snapshot401k)
                    .priorYearSpending(new BigDecimal("48000"))
                    .priorYearReturn(new BigDecimal("0.08"))
                    .cumulativeWithdrawals(new BigDecimal("100000"))
                    .build();

            assertEquals(0, new BigDecimal("48000").compareTo(view.getPriorYearSpending()));
            assertEquals(0, new BigDecimal("0.08").compareTo(view.getPriorYearReturn()));
            assertEquals(0, new BigDecimal("100000").compareTo(view.getCumulativeWithdrawals()));
        }

        @Test
        @DisplayName("Should set last ratchet month")
        void setsLastRatchetMonth() {
            YearMonth ratchetMonth = YearMonth.of(2023, 6);
            StubSimulationView view = StubSimulationView.builder()
                    .addAccount(snapshot401k)
                    .lastRatchetMonth(ratchetMonth)
                    .build();

            assertTrue(view.getLastRatchetMonth().isPresent());
            assertEquals(ratchetMonth, view.getLastRatchetMonth().get());
        }

        @Test
        @DisplayName("Should default last ratchet month to empty")
        void defaultsLastRatchetMonthEmpty() {
            StubSimulationView view = StubSimulationView.builder()
                    .addAccount(snapshot401k)
                    .build();

            assertFalse(view.getLastRatchetMonth().isPresent());
        }

        @Test
        @DisplayName("Should set explicit initial balance")
        void setsExplicitInitialBalance() {
            StubSimulationView view = StubSimulationView.builder()
                    .addAccount(snapshot401k)
                    .initialPortfolioBalance(new BigDecimal("1000000"))
                    .build();

            // Initial balance is explicit
            assertEquals(0, new BigDecimal("1000000").compareTo(view.getInitialPortfolioBalance()));
            // Current balance is from accounts
            assertEquals(0, new BigDecimal("500000").compareTo(view.getTotalPortfolioBalance()));
        }

        @Test
        @DisplayName("Should default initial balance to current total")
        void defaultsInitialBalance() {
            StubSimulationView view = StubSimulationView.builder()
                    .addAccount(snapshot401k)
                    .addAccount(snapshotRoth)
                    .build();

            assertEquals(0, new BigDecimal("700000").compareTo(view.getInitialPortfolioBalance()));
        }

        @Test
        @DisplayName("Should set explicit high water mark")
        void setsHighWaterMark() {
            StubSimulationView view = StubSimulationView.builder()
                    .addAccount(snapshot401k)
                    .highWaterMarkBalance(new BigDecimal("800000"))
                    .build();

            assertEquals(0, new BigDecimal("800000").compareTo(view.getHighWaterMarkBalance()));
        }

        @Test
        @DisplayName("Should default high water mark to current total")
        void defaultsHighWaterMark() {
            StubSimulationView view = StubSimulationView.builder()
                    .addAccount(snapshot401k)
                    .build();

            assertEquals(0, new BigDecimal("500000").compareTo(view.getHighWaterMarkBalance()));
        }

        @Test
        @DisplayName("Should default numeric values to zero")
        void defaultsNumericValues() {
            StubSimulationView view = StubSimulationView.builder()
                    .addAccount(snapshot401k)
                    .build();

            assertEquals(0, BigDecimal.ZERO.compareTo(view.getPriorYearSpending()));
            assertEquals(0, BigDecimal.ZERO.compareTo(view.getPriorYearReturn()));
            assertEquals(0, BigDecimal.ZERO.compareTo(view.getCumulativeWithdrawals()));
        }
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("withAccount should create minimal stub")
        void withAccountCreatesMinimalStub() {
            StubSimulationView view = StubSimulationView.withAccount(traditional401k);

            assertEquals(1, view.getAccountSnapshots().size());
            assertEquals(0, new BigDecimal("500000").compareTo(view.getTotalPortfolioBalance()));
        }

        @Test
        @DisplayName("withAccounts should create stub with list")
        void withAccountsCreatesList() {
            List<AccountSnapshot> accounts = List.of(snapshot401k, snapshotRoth);
            StubSimulationView view = StubSimulationView.withAccounts(accounts);

            assertEquals(2, view.getAccountSnapshots().size());
            assertEquals(0, new BigDecimal("700000").compareTo(view.getTotalPortfolioBalance()));
        }
    }

    @Nested
    @DisplayName("Account Balance Tests")
    class AccountBalanceTests {

        @Test
        @DisplayName("Should get balance by account ID")
        void getBalanceById() {
            StubSimulationView view = StubSimulationView.builder()
                    .addAccount(snapshot401k)
                    .addAccount(snapshotRoth)
                    .build();

            UUID accountId = UUID.fromString(snapshot401k.accountId());
            assertEquals(0, new BigDecimal("500000").compareTo(view.getAccountBalance(accountId)));
        }

        @Test
        @DisplayName("Should return zero for unknown account ID")
        void returnsZeroForUnknownId() {
            StubSimulationView view = StubSimulationView.withAccount(traditional401k);

            assertEquals(0, BigDecimal.ZERO.compareTo(view.getAccountBalance(UUID.randomUUID())));
        }

        @Test
        @DisplayName("Should calculate total from all accounts")
        void calculatesTotalFromAllAccounts() {
            StubSimulationView view = StubSimulationView.builder()
                    .addAccount(snapshot401k)
                    .addAccount(snapshotRoth)
                    .build();

            // 500000 + 200000 = 700000
            assertEquals(0, new BigDecimal("700000").compareTo(view.getTotalPortfolioBalance()));
        }

        @Test
        @DisplayName("Should return zero total for empty accounts")
        void returnsZeroForEmptyAccounts() {
            StubSimulationView view = StubSimulationView.builder().build();

            assertEquals(0, BigDecimal.ZERO.compareTo(view.getTotalPortfolioBalance()));
        }
    }

    @Nested
    @DisplayName("Account Snapshot List Tests")
    class AccountSnapshotListTests {

        @Test
        @DisplayName("Account list should be unmodifiable")
        void accountListUnmodifiable() {
            StubSimulationView view = StubSimulationView.builder()
                    .addAccount(snapshot401k)
                    .build();

            List<AccountSnapshot> accounts = view.getAccountSnapshots();
            org.junit.jupiter.api.Assertions.assertThrows(
                    UnsupportedOperationException.class,
                    () -> accounts.add(snapshotRoth)
            );
        }

        @Test
        @DisplayName("Should return empty list when no accounts")
        void returnsEmptyListWhenNoAccounts() {
            StubSimulationView view = StubSimulationView.builder().build();

            assertTrue(view.getAccountSnapshots().isEmpty());
        }
    }

    @Nested
    @DisplayName("Test Helper Method Tests")
    class TestHelperMethodTests {

        @Test
        @DisplayName("createTestAccount should create valid snapshot")
        void createTestAccountWorks() {
            AccountSnapshot snapshot = StubSimulationView.createTestAccount(
                    "Test Account",
                    AccountType.TRADITIONAL_IRA,
                    new BigDecimal("100000")
            );

            assertEquals("Test Account", snapshot.accountName());
            assertEquals(AccountType.TRADITIONAL_IRA, snapshot.accountType());
            assertEquals(0, new BigDecimal("100000").compareTo(snapshot.balance()));
            assertTrue(snapshot.subjectToRmd());
            assertEquals(AccountType.TaxTreatment.PRE_TAX, snapshot.taxTreatment());
        }

        @Test
        @DisplayName("createTestAccount should set RMD correctly for Roth")
        void createTestAccountRothNotSubjectToRmd() {
            AccountSnapshot snapshot = StubSimulationView.createTestAccount(
                    "Roth",
                    AccountType.ROTH_IRA,
                    new BigDecimal("50000")
            );

            assertFalse(snapshot.subjectToRmd());
        }
    }

    @Nested
    @DisplayName("Typical Usage Pattern Tests")
    class TypicalUsagePatternTests {

        @Test
        @DisplayName("Should support Static 4% strategy test setup")
        void supportsStatic4PercentSetup() {
            // Year 5 of retirement: portfolio grew from $1M to $800K
            StubSimulationView view = StubSimulationView.builder()
                    .addAccount(StubSimulationView.createTestAccount(
                            "401(k)", AccountType.TRADITIONAL_401K, new BigDecimal("600000")))
                    .addAccount(StubSimulationView.createTestAccount(
                            "Roth IRA", AccountType.ROTH_IRA, new BigDecimal("200000")))
                    .initialPortfolioBalance(new BigDecimal("1000000"))
                    .build();

            // Strategy can calculate: initial * 4% = $40,000/year
            assertEquals(0, new BigDecimal("1000000").compareTo(view.getInitialPortfolioBalance()));
            assertEquals(0, new BigDecimal("800000").compareTo(view.getTotalPortfolioBalance()));
        }

        @Test
        @DisplayName("Should support Guardrails strategy test setup")
        void supportsGuardrailsSetup() {
            // Scenario: Good year, portfolio up 15%, should trigger prosperity rule
            StubSimulationView view = StubSimulationView.builder()
                    .addAccount(StubSimulationView.createTestAccount(
                            "Portfolio", AccountType.TRADITIONAL_401K, new BigDecimal("1150000")))
                    .initialPortfolioBalance(new BigDecimal("1000000"))
                    .priorYearSpending(new BigDecimal("40000"))
                    .priorYearReturn(new BigDecimal("0.15"))
                    .highWaterMarkBalance(new BigDecimal("1150000"))
                    .build();

            assertEquals(0, new BigDecimal("0.15").compareTo(view.getPriorYearReturn()));
            assertEquals(0, new BigDecimal("40000").compareTo(view.getPriorYearSpending()));
        }

        @Test
        @DisplayName("Should support Kitces ratcheting test setup")
        void supportsKitcesSetup() {
            // Scenario: 4 years since last ratchet, portfolio grew significantly
            YearMonth lastRatchet = YearMonth.of(2021, 1);
            StubSimulationView view = StubSimulationView.builder()
                    .addAccount(StubSimulationView.createTestAccount(
                            "Portfolio", AccountType.TRADITIONAL_401K, new BigDecimal("1500000")))
                    .initialPortfolioBalance(new BigDecimal("1000000"))
                    .lastRatchetMonth(lastRatchet)
                    .cumulativeWithdrawals(new BigDecimal("160000"))
                    .build();

            assertTrue(view.getLastRatchetMonth().isPresent());
            assertEquals(lastRatchet, view.getLastRatchetMonth().get());
            assertEquals(0, new BigDecimal("160000").compareTo(view.getCumulativeWithdrawals()));
        }
    }
}
