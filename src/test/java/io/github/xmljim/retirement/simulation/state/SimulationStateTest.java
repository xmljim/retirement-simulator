package io.github.xmljim.retirement.simulation.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.calculator.SimulationView;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.model.InvestmentAccount;
import io.github.xmljim.retirement.domain.value.AccountSnapshot;
import io.github.xmljim.retirement.domain.value.AccountWithdrawal;
import io.github.xmljim.retirement.domain.value.AssetAllocation;
import io.github.xmljim.retirement.domain.value.SpendingPlan;
import io.github.xmljim.retirement.simulation.result.AccountMonthlyFlow;
import io.github.xmljim.retirement.simulation.result.MonthlySnapshot;

@DisplayName("SimulationState")
class SimulationStateTest {

    private List<InvestmentAccount> testAccounts;
    private String account1Id;
    private String account2Id;

    @BeforeEach
    void setUp() {
        account1Id = UUID.randomUUID().toString();
        account2Id = UUID.randomUUID().toString();

        InvestmentAccount account1 = InvestmentAccount.builder()
                .id(account1Id)
                .name("Traditional 401k")
                .accountType(AccountType.TRADITIONAL_401K)
                .balance(new BigDecimal("300000.00"))
                .allocation(AssetAllocation.balanced())
                .preRetirementReturnRate(new BigDecimal("0.07"))
                .build();

        InvestmentAccount account2 = InvestmentAccount.builder()
                .id(account2Id)
                .name("Roth IRA")
                .accountType(AccountType.ROTH_IRA)
                .balance(new BigDecimal("200000.00"))
                .allocation(AssetAllocation.balanced())
                .preRetirementReturnRate(new BigDecimal("0.07"))
                .build();

        testAccounts = List.of(account1, account2);
    }

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should initialize with accounts")
        void shouldInitializeWithAccounts() {
            SimulationState state = new SimulationState(testAccounts);

            assertEquals(2, state.getAccountCount());
            assertEquals(new BigDecimal("500000.00"), state.calculateTotalBalance());
        }

        @Test
        @DisplayName("should set initial portfolio balance")
        void shouldSetInitialPortfolioBalance() {
            SimulationState state = new SimulationState(testAccounts);

            assertEquals(new BigDecimal("500000.00"), state.getInitialPortfolioBalance());
        }

        @Test
        @DisplayName("should initialize high water mark")
        void shouldInitializeHighWaterMark() {
            SimulationState state = new SimulationState(testAccounts);

            assertEquals(new BigDecimal("500000.00"), state.getHighWaterMarkBalance());
        }

        @Test
        @DisplayName("should handle null accounts list")
        void shouldHandleNullAccountsList() {
            SimulationState state = new SimulationState(null);

            assertEquals(0, state.getAccountCount());
            assertEquals(BigDecimal.ZERO.setScale(2), state.calculateTotalBalance());
        }

        @Test
        @DisplayName("should initialize empty history")
        void shouldInitializeEmptyHistory() {
            SimulationState state = new SimulationState(testAccounts);

            assertTrue(state.getHistory().isEmpty());
        }

        @Test
        @DisplayName("should initialize default flags")
        void shouldInitializeDefaultFlags() {
            SimulationState state = new SimulationState(testAccounts);

            assertFalse(state.isSurvivorMode());
            assertNotNull(state.getFlags());
        }
    }

    @Nested
    @DisplayName("Account Operations")
    class AccountOperations {

        @Test
        @DisplayName("getAccountBalance should return balance")
        void getAccountBalanceShouldReturnBalance() {
            SimulationState state = new SimulationState(testAccounts);

            assertEquals(new BigDecimal("300000.00"), state.getAccountBalance(account1Id));
        }

        @Test
        @DisplayName("getAccountBalance should return ZERO for unknown account")
        void getAccountBalanceShouldReturnZeroForUnknown() {
            SimulationState state = new SimulationState(testAccounts);

            assertEquals(BigDecimal.ZERO, state.getAccountBalance("unknown-id"));
        }

        @Test
        @DisplayName("withdrawFromAccount should reduce balance")
        void withdrawFromAccountShouldReduceBalance() {
            SimulationState state = new SimulationState(testAccounts);

            BigDecimal actual = state.withdrawFromAccount(account1Id, new BigDecimal("50000"));

            assertEquals(new BigDecimal("50000"), actual);
            assertEquals(new BigDecimal("250000.00"), state.getAccountBalance(account1Id));
        }

        @Test
        @DisplayName("withdrawFromAccount should return ZERO for unknown account")
        void withdrawFromAccountShouldReturnZeroForUnknown() {
            SimulationState state = new SimulationState(testAccounts);

            BigDecimal actual = state.withdrawFromAccount("unknown-id", new BigDecimal("1000"));

            assertEquals(BigDecimal.ZERO, actual);
        }

        @Test
        @DisplayName("withdrawFromAccount should update cumulative withdrawals")
        void withdrawFromAccountShouldUpdateCumulative() {
            SimulationState state = new SimulationState(testAccounts);

            state.withdrawFromAccount(account1Id, new BigDecimal("25000"));
            state.withdrawFromAccount(account2Id, new BigDecimal("15000"));

            assertEquals(0, new BigDecimal("40000").compareTo(state.getCumulativeWithdrawals()));
        }

        @Test
        @DisplayName("depositToAccount should increase balance")
        void depositToAccountShouldIncreaseBalance() {
            SimulationState state = new SimulationState(testAccounts);

            state.depositToAccount(account1Id, new BigDecimal("10000"));

            assertEquals(new BigDecimal("310000.00"), state.getAccountBalance(account1Id));
        }

        @Test
        @DisplayName("depositToAccount should do nothing for unknown account")
        void depositToAccountShouldDoNothingForUnknown() {
            SimulationState state = new SimulationState(testAccounts);

            state.depositToAccount("unknown-id", new BigDecimal("10000"));

            // Total balance unchanged
            assertEquals(new BigDecimal("500000.00"), state.calculateTotalBalance());
        }

        @Test
        @DisplayName("depositToAccount should update high water mark")
        void depositToAccountShouldUpdateHighWaterMark() {
            SimulationState state = new SimulationState(testAccounts);

            state.depositToAccount(account1Id, new BigDecimal("100000"));

            assertEquals(new BigDecimal("600000.00"), state.getHighWaterMarkBalance());
        }

        @Test
        @DisplayName("updateAccountBalance should set balance directly")
        void updateAccountBalanceShouldSetBalanceDirectly() {
            SimulationState state = new SimulationState(testAccounts);

            state.updateAccountBalance(account1Id, new BigDecimal("400000"));

            assertEquals(new BigDecimal("400000.00"), state.getAccountBalance(account1Id));
        }

        @Test
        @DisplayName("updateAccountBalance should do nothing for unknown account")
        void updateAccountBalanceShouldDoNothingForUnknown() {
            SimulationState state = new SimulationState(testAccounts);

            state.updateAccountBalance("unknown-id", new BigDecimal("100000"));

            // Total balance unchanged
            assertEquals(new BigDecimal("500000.00"), state.calculateTotalBalance());
        }

        @Test
        @DisplayName("updateAccountBalance should update high water mark")
        void updateAccountBalanceShouldUpdateHighWaterMark() {
            SimulationState state = new SimulationState(testAccounts);

            state.updateAccountBalance(account1Id, new BigDecimal("500000"));

            assertEquals(new BigDecimal("700000.00"), state.getHighWaterMarkBalance());
        }
    }

    @Nested
    @DisplayName("Apply Returns")
    class ApplyReturns {

        @Test
        @DisplayName("applyReturns should apply to all accounts")
        void applyReturnsShouldApplyToAllAccounts() {
            SimulationState state = new SimulationState(testAccounts);

            Map<String, BigDecimal> returns = state.applyReturns(new BigDecimal("0.01"));

            assertEquals(new BigDecimal("3000.00"), returns.get(account1Id));
            assertEquals(new BigDecimal("2000.00"), returns.get(account2Id));
            assertEquals(new BigDecimal("505000.00"), state.calculateTotalBalance());
        }

        @Test
        @DisplayName("applyReturns should update high water mark")
        void applyReturnsShouldUpdateHighWaterMark() {
            SimulationState state = new SimulationState(testAccounts);

            state.applyReturns(new BigDecimal("0.10"));

            assertEquals(new BigDecimal("550000.00"), state.getHighWaterMarkBalance());
        }
    }

    @Nested
    @DisplayName("Apply Withdrawals from SpendingPlan")
    class ApplyWithdrawalsFromPlan {

        @Test
        @DisplayName("applyWithdrawals should process spending plan")
        void applyWithdrawalsShouldProcessPlan() {
            SimulationState state = new SimulationState(testAccounts);

            AccountSnapshot snapshot1 = new AccountSnapshot(
                    account1Id, "Traditional 401k", AccountType.TRADITIONAL_401K,
                    new BigDecimal("300000"), AccountType.TaxTreatment.PRE_TAX,
                    true, AssetAllocation.balanced());

            AccountWithdrawal withdrawal = AccountWithdrawal.builder()
                    .accountSnapshot(snapshot1)
                    .amount(new BigDecimal("10000"))
                    .priorBalance(new BigDecimal("300000"))
                    .newBalance(new BigDecimal("290000"))
                    .build();

            SpendingPlan plan = SpendingPlan.builder()
                    .targetWithdrawal(new BigDecimal("10000"))
                    .adjustedWithdrawal(new BigDecimal("10000"))
                    .accountWithdrawals(List.of(withdrawal))
                    .meetsTarget(true)
                    .strategyUsed("Test")
                    .build();

            Map<UUID, AccountMonthlyFlow> flows = state.applyWithdrawals(plan);

            assertEquals(1, flows.size());
            assertEquals(new BigDecimal("290000.00"), state.getAccountBalance(account1Id));
        }
    }

    @Nested
    @DisplayName("History Operations")
    class HistoryOperations {

        @Test
        @DisplayName("recordHistory should add snapshot to history")
        void recordHistoryShouldAddSnapshot() {
            SimulationState state = new SimulationState(testAccounts);
            MonthlySnapshot snapshot = MonthlySnapshot.builder(YearMonth.of(2025, 1)).build();

            state.recordHistory(snapshot);

            assertEquals(1, state.getHistory().size());
        }

        @Test
        @DisplayName("recordHistory should handle null")
        void recordHistoryShouldHandleNull() {
            SimulationState state = new SimulationState(testAccounts);

            state.recordHistory(null);

            assertTrue(state.getHistory().isEmpty());
        }

        @Test
        @DisplayName("getPriorYearSpending should return ZERO for empty history")
        void getPriorYearSpendingShouldReturnZeroForEmptyHistory() {
            SimulationState state = new SimulationState(testAccounts);

            assertEquals(BigDecimal.ZERO, state.getPriorYearSpending(YearMonth.of(2026, 1)));
        }

        @Test
        @DisplayName("getPriorYearSpending should return ZERO for null month")
        void getPriorYearSpendingShouldReturnZeroForNullMonth() {
            SimulationState state = new SimulationState(testAccounts);
            state.recordHistory(MonthlySnapshot.builder(YearMonth.of(2025, 1)).build());

            assertEquals(BigDecimal.ZERO, state.getPriorYearSpending(null));
        }

        @Test
        @DisplayName("getPriorYearSpending should sum prior year withdrawals")
        void getPriorYearSpendingShouldSumPriorYearWithdrawals() {
            SimulationState state = new SimulationState(testAccounts);

            // Add some 2025 history with withdrawals
            UUID uuid = UUID.fromString(account1Id);
            Map<UUID, AccountMonthlyFlow> flows = Map.of(
                    uuid, AccountMonthlyFlow.builder(uuid, "Test")
                            .startingBalance(new BigDecimal("100000"))
                            .withdrawals(new BigDecimal("5000"))
                            .build()
            );
            state.recordHistory(MonthlySnapshot.builder(YearMonth.of(2025, 1))
                    .accountFlows(flows)
                    .build());
            state.recordHistory(MonthlySnapshot.builder(YearMonth.of(2025, 6))
                    .accountFlows(flows)
                    .build());

            // Query from 2026
            BigDecimal spending = state.getPriorYearSpending(YearMonth.of(2026, 1));

            assertEquals(0, new BigDecimal("10000").compareTo(spending));
        }

        @Test
        @DisplayName("getPriorYearReturn should return ZERO for empty history")
        void getPriorYearReturnShouldReturnZeroForEmptyHistory() {
            SimulationState state = new SimulationState(testAccounts);

            assertEquals(BigDecimal.ZERO, state.getPriorYearReturn(YearMonth.of(2026, 1)));
        }

        @Test
        @DisplayName("getPriorYearReturn should return ZERO for null month")
        void getPriorYearReturnShouldReturnZeroForNullMonth() {
            SimulationState state = new SimulationState(testAccounts);
            state.recordHistory(MonthlySnapshot.builder(YearMonth.of(2025, 1)).build());

            assertEquals(BigDecimal.ZERO, state.getPriorYearReturn(null));
        }

        @Test
        @DisplayName("getPriorYearReturn should return ZERO if no prior year data")
        void getPriorYearReturnShouldReturnZeroIfNoPriorYearData() {
            SimulationState state = new SimulationState(testAccounts);
            // Add 2025 data but query for prior to 2024 (2023)
            state.recordHistory(MonthlySnapshot.builder(YearMonth.of(2025, 1)).build());

            assertEquals(BigDecimal.ZERO, state.getPriorYearReturn(YearMonth.of(2024, 1)));
        }

        @Test
        @DisplayName("getPriorYearReturn should calculate return percentage")
        void getPriorYearReturnShouldCalculateReturnPercentage() {
            SimulationState state = new SimulationState(testAccounts);

            // Add 2025 history with returns
            UUID uuid = UUID.fromString(account1Id);
            Map<UUID, AccountMonthlyFlow> flows = Map.of(
                    uuid, AccountMonthlyFlow.builder(uuid, "Test")
                            .startingBalance(new BigDecimal("500000"))
                            .returns(new BigDecimal("35000")) // 7% return
                            .build()
            );
            state.recordHistory(MonthlySnapshot.builder(YearMonth.of(2025, 1))
                    .accountFlows(flows)
                    .build());

            // Query from 2026 - should use initial portfolio balance
            BigDecimal returnRate = state.getPriorYearReturn(YearMonth.of(2026, 1));

            // 35000 / 500000 = 0.07
            assertEquals(0, new BigDecimal("0.07").compareTo(returnRate));
        }
    }

    @Nested
    @DisplayName("Flags")
    class FlagsTests {

        @Test
        @DisplayName("setSurvivorMode should update flags")
        void setSurvivorModeShouldUpdateFlags() {
            SimulationState state = new SimulationState(testAccounts);

            state.setSurvivorMode(true);

            assertTrue(state.isSurvivorMode());
            assertTrue(state.getFlags().survivorMode());
        }

        @Test
        @DisplayName("setFlags should replace flags")
        void setFlagsShouldReplaceFlags() {
            SimulationState state = new SimulationState(testAccounts);
            SimulationFlags newFlags = SimulationFlags.initial().withRefillMode(true);

            state.setFlags(newFlags);

            assertTrue(state.getFlags().refillMode());
        }

        @Test
        @DisplayName("setFlags with null should use initial")
        void setFlagsWithNullShouldUseInitial() {
            SimulationState state = new SimulationState(testAccounts);
            state.setSurvivorMode(true);

            state.setFlags(null);

            assertFalse(state.isSurvivorMode());
        }
    }

    @Nested
    @DisplayName("Ratchet Tracking")
    class RatchetTracking {

        @Test
        @DisplayName("recordRatchet should store month")
        void recordRatchetShouldStoreMonth() {
            SimulationState state = new SimulationState(testAccounts);
            YearMonth ratchetMonth = YearMonth.of(2025, 6);

            state.recordRatchet(ratchetMonth);

            assertEquals(Optional.of(ratchetMonth), state.getLastRatchetMonth());
        }

        @Test
        @DisplayName("getLastRatchetMonth should return empty initially")
        void getLastRatchetMonthShouldReturnEmptyInitially() {
            SimulationState state = new SimulationState(testAccounts);

            assertEquals(Optional.empty(), state.getLastRatchetMonth());
        }
    }

    @Nested
    @DisplayName("Snapshot")
    class SnapshotTests {

        @Test
        @DisplayName("snapshot should create SimulationView")
        void snapshotShouldCreateSimulationView() {
            SimulationState state = new SimulationState(testAccounts);

            SimulationView view = state.snapshot();

            assertNotNull(view);
            assertEquals(new BigDecimal("500000.00"), view.getTotalPortfolioBalance());
            assertEquals(2, view.getAccountSnapshots().size());
        }

        @Test
        @DisplayName("snapshot should include initial balance")
        void snapshotShouldIncludeInitialBalance() {
            SimulationState state = new SimulationState(testAccounts);

            SimulationView view = state.snapshot();

            assertEquals(new BigDecimal("500000.00"), view.getInitialPortfolioBalance());
        }

        @Test
        @DisplayName("snapshot should include cumulative withdrawals")
        void snapshotShouldIncludeCumulativeWithdrawals() {
            SimulationState state = new SimulationState(testAccounts);
            state.withdrawFromAccount(account1Id, new BigDecimal("25000"));

            SimulationView view = state.snapshot();

            assertEquals(0, new BigDecimal("25000").compareTo(view.getCumulativeWithdrawals()));
        }

        @Test
        @DisplayName("snapshot should include high water mark")
        void snapshotShouldIncludeHighWaterMark() {
            SimulationState state = new SimulationState(testAccounts);
            state.depositToAccount(account1Id, new BigDecimal("50000"));

            SimulationView view = state.snapshot();

            assertEquals(new BigDecimal("550000.00"), view.getHighWaterMarkBalance());
        }

        @Test
        @DisplayName("snapshot should include last ratchet month")
        void snapshotShouldIncludeLastRatchetMonth() {
            SimulationState state = new SimulationState(testAccounts);
            YearMonth ratchetMonth = YearMonth.of(2025, 3);
            state.recordRatchet(ratchetMonth);

            SimulationView view = state.snapshot();

            assertEquals(Optional.of(ratchetMonth), view.getLastRatchetMonth());
        }
    }

    @Nested
    @DisplayName("Account Snapshots")
    class AccountSnapshotsTests {

        @Test
        @DisplayName("getAccountSnapshots should return all accounts")
        void getAccountSnapshotsShouldReturnAll() {
            SimulationState state = new SimulationState(testAccounts);

            List<AccountSnapshot> snapshots = state.getAccountSnapshots();

            assertEquals(2, snapshots.size());
        }

        @Test
        @DisplayName("getAccountSnapshots should reflect current balances")
        void getAccountSnapshotsShouldReflectCurrentBalances() {
            SimulationState state = new SimulationState(testAccounts);
            state.withdrawFromAccount(account1Id, new BigDecimal("100000"));

            List<AccountSnapshot> snapshots = state.getAccountSnapshots();
            AccountSnapshot account1Snapshot = snapshots.stream()
                    .filter(s -> s.accountId().equals(account1Id))
                    .findFirst()
                    .orElseThrow();

            assertEquals(new BigDecimal("200000.00"), account1Snapshot.balance());
        }
    }
}
