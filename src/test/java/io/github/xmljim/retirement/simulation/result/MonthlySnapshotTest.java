package io.github.xmljim.retirement.simulation.result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.ExpenseCategoryGroup;
import io.github.xmljim.retirement.domain.enums.SimulationPhase;

@DisplayName("MonthlySnapshot")
class MonthlySnapshotTest {

    private static final YearMonth TEST_MONTH = YearMonth.of(2025, 6);
    private static final UUID ACCOUNT_1_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_2_ID = UUID.randomUUID();

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create with required month")
        void shouldCreateWithMonth() {
            MonthlySnapshot snapshot = MonthlySnapshot.builder(TEST_MONTH).build();

            assertEquals(TEST_MONTH, snapshot.month());
            assertEquals(2025, snapshot.year());
            assertEquals(6, snapshot.monthValue());
        }

        @Test
        @DisplayName("should reject null month")
        void shouldRejectNullMonth() {
            assertThrows(IllegalArgumentException.class, () ->
                    MonthlySnapshot.builder(null).build());
        }

        @Test
        @DisplayName("should default null values appropriately")
        void shouldDefaultNullValues() {
            MonthlySnapshot snapshot = new MonthlySnapshot(
                    TEST_MONTH,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null
            );

            assertTrue(snapshot.accountFlows().isEmpty());
            assertEquals(BigDecimal.ZERO, snapshot.salaryIncome());
            assertEquals(BigDecimal.ZERO, snapshot.totalExpenses());
            assertNotNull(snapshot.taxes());
            assertEquals(SimulationPhase.ACCUMULATION, snapshot.phase());
            assertTrue(snapshot.eventsTriggered().isEmpty());
        }

        @Test
        @DisplayName("should create immutable account flows map")
        void shouldCreateImmutableAccountFlows() {
            AccountMonthlyFlow flow = AccountMonthlyFlow.builder(ACCOUNT_1_ID, "Test Account")
                    .startingBalance(new BigDecimal("100000"))
                    .build();

            MonthlySnapshot snapshot = MonthlySnapshot.builder(TEST_MONTH)
                    .accountFlows(Map.of(ACCOUNT_1_ID, flow))
                    .build();

            assertThrows(UnsupportedOperationException.class, () ->
                    snapshot.accountFlows().put(ACCOUNT_2_ID, flow));
        }

        @Test
        @DisplayName("should create immutable events list")
        void shouldCreateImmutableEventsList() {
            MonthlySnapshot snapshot = MonthlySnapshot.builder(TEST_MONTH)
                    .eventsTriggered(List.of("Event1", "Event2"))
                    .build();

            assertThrows(UnsupportedOperationException.class, () ->
                    snapshot.eventsTriggered().add("Event3"));
        }
    }

    @Nested
    @DisplayName("Portfolio Aggregations")
    class PortfolioAggregations {

        @Test
        @DisplayName("totalPortfolioBalance should sum all account ending balances")
        void totalPortfolioBalance() {
            AccountMonthlyFlow flow1 = AccountMonthlyFlow.builder(ACCOUNT_1_ID, "Account 1")
                    .startingBalance(new BigDecimal("100000"))
                    .returns(new BigDecimal("500"))
                    .build();

            AccountMonthlyFlow flow2 = AccountMonthlyFlow.builder(ACCOUNT_2_ID, "Account 2")
                    .startingBalance(new BigDecimal("50000"))
                    .returns(new BigDecimal("250"))
                    .build();

            MonthlySnapshot snapshot = MonthlySnapshot.builder(TEST_MONTH)
                    .accountFlows(Map.of(ACCOUNT_1_ID, flow1, ACCOUNT_2_ID, flow2))
                    .build();

            // 100500 + 50250 = 150750
            assertEquals(new BigDecimal("150750.00"), snapshot.totalPortfolioBalance());
        }

        @Test
        @DisplayName("totalContributions should sum all account contributions")
        void totalContributions() {
            AccountMonthlyFlow flow1 = AccountMonthlyFlow.builder(ACCOUNT_1_ID, "Account 1")
                    .startingBalance(new BigDecimal("100000"))
                    .contributions(new BigDecimal("1000"))
                    .build();

            AccountMonthlyFlow flow2 = AccountMonthlyFlow.builder(ACCOUNT_2_ID, "Account 2")
                    .startingBalance(new BigDecimal("50000"))
                    .contributions(new BigDecimal("500"))
                    .build();

            MonthlySnapshot snapshot = MonthlySnapshot.builder(TEST_MONTH)
                    .accountFlows(Map.of(ACCOUNT_1_ID, flow1, ACCOUNT_2_ID, flow2))
                    .build();

            assertEquals(new BigDecimal("1500.00"), snapshot.totalContributions());
        }

        @Test
        @DisplayName("totalWithdrawals should sum all account withdrawals")
        void totalWithdrawals() {
            AccountMonthlyFlow flow1 = AccountMonthlyFlow.builder(ACCOUNT_1_ID, "Account 1")
                    .startingBalance(new BigDecimal("100000"))
                    .withdrawals(new BigDecimal("3000"))
                    .build();

            AccountMonthlyFlow flow2 = AccountMonthlyFlow.builder(ACCOUNT_2_ID, "Account 2")
                    .startingBalance(new BigDecimal("50000"))
                    .withdrawals(new BigDecimal("2000"))
                    .build();

            MonthlySnapshot snapshot = MonthlySnapshot.builder(TEST_MONTH)
                    .accountFlows(Map.of(ACCOUNT_1_ID, flow1, ACCOUNT_2_ID, flow2))
                    .build();

            assertEquals(new BigDecimal("5000.00"), snapshot.totalWithdrawals());
        }

        @Test
        @DisplayName("totalReturns should sum all account returns")
        void totalReturns() {
            AccountMonthlyFlow flow1 = AccountMonthlyFlow.builder(ACCOUNT_1_ID, "Account 1")
                    .startingBalance(new BigDecimal("100000"))
                    .returns(new BigDecimal("800"))
                    .build();

            AccountMonthlyFlow flow2 = AccountMonthlyFlow.builder(ACCOUNT_2_ID, "Account 2")
                    .startingBalance(new BigDecimal("50000"))
                    .returns(new BigDecimal("-200"))
                    .build();

            MonthlySnapshot snapshot = MonthlySnapshot.builder(TEST_MONTH)
                    .accountFlows(Map.of(ACCOUNT_1_ID, flow1, ACCOUNT_2_ID, flow2))
                    .build();

            assertEquals(new BigDecimal("600.00"), snapshot.totalReturns());
        }
    }

    @Nested
    @DisplayName("Income Aggregations")
    class IncomeAggregations {

        @Test
        @DisplayName("totalIncome should sum all income sources")
        void totalIncome() {
            MonthlySnapshot snapshot = MonthlySnapshot.builder(TEST_MONTH)
                    .salaryIncome(new BigDecimal("5000"))
                    .socialSecurityIncome(new BigDecimal("2000"))
                    .pensionIncome(new BigDecimal("1500"))
                    .otherIncome(new BigDecimal("500"))
                    .build();

            assertEquals(new BigDecimal("9000.00"), snapshot.totalIncome());
        }

        @Test
        @DisplayName("totalNonSalaryIncome should exclude salary")
        void totalNonSalaryIncome() {
            MonthlySnapshot snapshot = MonthlySnapshot.builder(TEST_MONTH)
                    .salaryIncome(new BigDecimal("5000"))
                    .socialSecurityIncome(new BigDecimal("2000"))
                    .pensionIncome(new BigDecimal("1500"))
                    .otherIncome(new BigDecimal("500"))
                    .build();

            assertEquals(new BigDecimal("4000.00"), snapshot.totalNonSalaryIncome());
        }
    }

    @Nested
    @DisplayName("Cash Flow")
    class CashFlow {

        @Test
        @DisplayName("netCashFlow should be income minus expenses")
        void netCashFlow() {
            MonthlySnapshot snapshot = MonthlySnapshot.builder(TEST_MONTH)
                    .salaryIncome(new BigDecimal("5000"))
                    .socialSecurityIncome(new BigDecimal("2000"))
                    .totalExpenses(new BigDecimal("6000"))
                    .build();

            // 7000 - 6000 = 1000
            assertEquals(new BigDecimal("1000.00"), snapshot.netCashFlow());
        }

        @Test
        @DisplayName("incomeGap should be expenses minus non-salary income")
        void incomeGap() {
            MonthlySnapshot snapshot = MonthlySnapshot.builder(TEST_MONTH)
                    .socialSecurityIncome(new BigDecimal("2000"))
                    .pensionIncome(new BigDecimal("1000"))
                    .totalExpenses(new BigDecimal("5000"))
                    .build();

            // 5000 - 3000 = 2000
            assertEquals(new BigDecimal("2000.00"), snapshot.incomeGap());
        }

        @Test
        @DisplayName("incomeGap should return zero when no gap")
        void incomeGapZero() {
            MonthlySnapshot snapshot = MonthlySnapshot.builder(TEST_MONTH)
                    .socialSecurityIncome(new BigDecimal("3000"))
                    .pensionIncome(new BigDecimal("2000"))
                    .totalExpenses(new BigDecimal("4000"))
                    .build();

            // 4000 - 5000 = -1000, but capped at 0
            assertEquals(new BigDecimal("0.00"), snapshot.incomeGap());
        }
    }

    @Nested
    @DisplayName("Convenience Methods")
    class ConvenienceMethods {

        @Test
        @DisplayName("hadEvents should return true when events exist")
        void hadEventsTrue() {
            MonthlySnapshot snapshot = MonthlySnapshot.builder(TEST_MONTH)
                    .eventsTriggered(List.of("RetirementStart"))
                    .build();

            assertTrue(snapshot.hadEvents());
        }

        @Test
        @DisplayName("hadEvents should return false when no events")
        void hadEventsFalse() {
            MonthlySnapshot snapshot = MonthlySnapshot.builder(TEST_MONTH).build();

            assertFalse(snapshot.hadEvents());
        }

        @Test
        @DisplayName("accountCount should return number of accounts")
        void accountCount() {
            AccountMonthlyFlow flow1 = AccountMonthlyFlow.builder(ACCOUNT_1_ID, "Account 1")
                    .startingBalance(new BigDecimal("100000"))
                    .build();

            AccountMonthlyFlow flow2 = AccountMonthlyFlow.builder(ACCOUNT_2_ID, "Account 2")
                    .startingBalance(new BigDecimal("50000"))
                    .build();

            MonthlySnapshot snapshot = MonthlySnapshot.builder(TEST_MONTH)
                    .accountFlows(Map.of(ACCOUNT_1_ID, flow1, ACCOUNT_2_ID, flow2))
                    .build();

            assertEquals(2, snapshot.accountCount());
        }

        @Test
        @DisplayName("getAccountFlow should return flow for account")
        void getAccountFlow() {
            AccountMonthlyFlow flow = AccountMonthlyFlow.builder(ACCOUNT_1_ID, "Account 1")
                    .startingBalance(new BigDecimal("100000"))
                    .build();

            MonthlySnapshot snapshot = MonthlySnapshot.builder(TEST_MONTH)
                    .accountFlows(Map.of(ACCOUNT_1_ID, flow))
                    .build();

            assertEquals(flow, snapshot.getAccountFlow(ACCOUNT_1_ID));
            assertNull(snapshot.getAccountFlow(ACCOUNT_2_ID));
        }

        @Test
        @DisplayName("getExpensesByGroup should return amount for group")
        void getExpensesByGroup() {
            MonthlySnapshot snapshot = MonthlySnapshot.builder(TEST_MONTH)
                    .expensesByCategory(Map.of(
                            ExpenseCategoryGroup.ESSENTIAL, new BigDecimal("3000"),
                            ExpenseCategoryGroup.HEALTHCARE, new BigDecimal("500")
                    ))
                    .build();

            assertEquals(new BigDecimal("3000"), snapshot.getExpensesByGroup(ExpenseCategoryGroup.ESSENTIAL));
            assertEquals(new BigDecimal("500"), snapshot.getExpensesByGroup(ExpenseCategoryGroup.HEALTHCARE));
            assertEquals(BigDecimal.ZERO, snapshot.getExpensesByGroup(ExpenseCategoryGroup.DISCRETIONARY));
        }
    }
}
