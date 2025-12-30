package io.github.xmljim.retirement.simulation.result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AccountMonthlyFlow")
class AccountMonthlyFlowTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final String ACCOUNT_NAME = "Traditional 401k";

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create with all values")
        void shouldCreateWithAllValues() {
            AccountMonthlyFlow flow = new AccountMonthlyFlow(
                    ACCOUNT_ID,
                    ACCOUNT_NAME,
                    new BigDecimal("100000.00"),
                    new BigDecimal("1000.00"),
                    BigDecimal.ZERO,
                    new BigDecimal("500.00"),
                    new BigDecimal("101500.00")
            );

            assertEquals(ACCOUNT_ID, flow.accountId());
            assertEquals(ACCOUNT_NAME, flow.accountName());
            assertEquals(new BigDecimal("100000.00"), flow.startingBalance());
            assertEquals(new BigDecimal("1000.00"), flow.contributions());
            assertEquals(BigDecimal.ZERO, flow.withdrawals());
            assertEquals(new BigDecimal("500.00"), flow.returns());
            assertEquals(new BigDecimal("101500.00"), flow.endingBalance());
        }

        @Test
        @DisplayName("should default null amounts to zero")
        void shouldDefaultNullAmountsToZero() {
            AccountMonthlyFlow flow = new AccountMonthlyFlow(
                    ACCOUNT_ID,
                    ACCOUNT_NAME,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            assertEquals(BigDecimal.ZERO, flow.startingBalance());
            assertEquals(BigDecimal.ZERO, flow.contributions());
            assertEquals(BigDecimal.ZERO, flow.withdrawals());
            assertEquals(BigDecimal.ZERO, flow.returns());
            assertEquals(BigDecimal.ZERO, flow.endingBalance());
        }

        @Test
        @DisplayName("should reject null accountId")
        void shouldRejectNullAccountId() {
            assertThrows(IllegalArgumentException.class, () ->
                    new AccountMonthlyFlow(null, ACCOUNT_NAME, BigDecimal.ZERO,
                            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        }

        @Test
        @DisplayName("should reject null accountName")
        void shouldRejectNullAccountName() {
            assertThrows(IllegalArgumentException.class, () ->
                    new AccountMonthlyFlow(ACCOUNT_ID, null, BigDecimal.ZERO,
                            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        }

        @Test
        @DisplayName("should reject blank accountName")
        void shouldRejectBlankAccountName() {
            assertThrows(IllegalArgumentException.class, () ->
                    new AccountMonthlyFlow(ACCOUNT_ID, "  ", BigDecimal.ZERO,
                            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should build with calculated ending balance")
        void shouldBuildWithCalculatedEndingBalance() {
            AccountMonthlyFlow flow = AccountMonthlyFlow.builder(ACCOUNT_ID, ACCOUNT_NAME)
                    .startingBalance(new BigDecimal("100000"))
                    .contributions(new BigDecimal("1000"))
                    .withdrawals(BigDecimal.ZERO)
                    .returns(new BigDecimal("500"))
                    .build();

            assertEquals(new BigDecimal("101500.00"), flow.endingBalance());
        }

        @Test
        @DisplayName("should calculate ending balance with withdrawal")
        void shouldCalculateEndingBalanceWithWithdrawal() {
            AccountMonthlyFlow flow = AccountMonthlyFlow.builder(ACCOUNT_ID, ACCOUNT_NAME)
                    .startingBalance(new BigDecimal("100000"))
                    .withdrawals(new BigDecimal("5000"))
                    .returns(new BigDecimal("400"))
                    .build();

            // 100000 - 5000 + 400 = 95400
            assertEquals(new BigDecimal("95400.00"), flow.endingBalance());
        }
    }

    @Nested
    @DisplayName("Calculations")
    class Calculations {

        @Test
        @DisplayName("netFlow should be contributions - withdrawals + returns")
        void netFlowCalculation() {
            AccountMonthlyFlow flow = AccountMonthlyFlow.builder(ACCOUNT_ID, ACCOUNT_NAME)
                    .startingBalance(new BigDecimal("100000"))
                    .contributions(new BigDecimal("1000"))
                    .withdrawals(new BigDecimal("500"))
                    .returns(new BigDecimal("300"))
                    .build();

            // 1000 - 500 + 300 = 800
            assertEquals(new BigDecimal("800.00"), flow.netFlow());
        }

        @Test
        @DisplayName("netContribution should be contributions - withdrawals")
        void netContributionCalculation() {
            AccountMonthlyFlow flow = AccountMonthlyFlow.builder(ACCOUNT_ID, ACCOUNT_NAME)
                    .startingBalance(new BigDecimal("100000"))
                    .contributions(new BigDecimal("1000"))
                    .withdrawals(new BigDecimal("300"))
                    .returns(new BigDecimal("500"))
                    .build();

            // 1000 - 300 = 700
            assertEquals(new BigDecimal("700.00"), flow.netContribution());
        }

        @Test
        @DisplayName("returnPercentage should calculate correctly")
        void returnPercentageCalculation() {
            AccountMonthlyFlow flow = AccountMonthlyFlow.builder(ACCOUNT_ID, ACCOUNT_NAME)
                    .startingBalance(new BigDecimal("100000"))
                    .contributions(BigDecimal.ZERO)
                    .withdrawals(BigDecimal.ZERO)
                    .returns(new BigDecimal("1000"))
                    .build();

            // 1000 / 100000 = 0.01 (1%)
            assertEquals(new BigDecimal("0.010000"), flow.returnPercentage());
        }

        @Test
        @DisplayName("returnPercentage should return zero when no base balance")
        void returnPercentageZeroBase() {
            AccountMonthlyFlow flow = AccountMonthlyFlow.builder(ACCOUNT_ID, ACCOUNT_NAME)
                    .startingBalance(BigDecimal.ZERO)
                    .build();

            assertEquals(BigDecimal.ZERO, flow.returnPercentage());
        }
    }

    @Nested
    @DisplayName("Activity Detection")
    class ActivityDetection {

        @Test
        @DisplayName("hasActivity should return true with contributions")
        void hasActivityWithContributions() {
            AccountMonthlyFlow flow = AccountMonthlyFlow.builder(ACCOUNT_ID, ACCOUNT_NAME)
                    .startingBalance(new BigDecimal("100000"))
                    .contributions(new BigDecimal("1000"))
                    .build();

            assertTrue(flow.hasActivity());
        }

        @Test
        @DisplayName("hasActivity should return true with withdrawals")
        void hasActivityWithWithdrawals() {
            AccountMonthlyFlow flow = AccountMonthlyFlow.builder(ACCOUNT_ID, ACCOUNT_NAME)
                    .startingBalance(new BigDecimal("100000"))
                    .withdrawals(new BigDecimal("1000"))
                    .build();

            assertTrue(flow.hasActivity());
        }

        @Test
        @DisplayName("hasActivity should return true with returns")
        void hasActivityWithReturns() {
            AccountMonthlyFlow flow = AccountMonthlyFlow.builder(ACCOUNT_ID, ACCOUNT_NAME)
                    .startingBalance(new BigDecimal("100000"))
                    .returns(new BigDecimal("500"))
                    .build();

            assertTrue(flow.hasActivity());
        }

        @Test
        @DisplayName("hasActivity should return false with no activity")
        void hasActivityNoActivity() {
            AccountMonthlyFlow flow = AccountMonthlyFlow.builder(ACCOUNT_ID, ACCOUNT_NAME)
                    .startingBalance(new BigDecimal("100000"))
                    .build();

            assertFalse(flow.hasActivity());
        }

        @Test
        @DisplayName("hadGrowth should return true when ending > starting")
        void hadGrowthTrue() {
            AccountMonthlyFlow flow = AccountMonthlyFlow.builder(ACCOUNT_ID, ACCOUNT_NAME)
                    .startingBalance(new BigDecimal("100000"))
                    .returns(new BigDecimal("500"))
                    .build();

            assertTrue(flow.hadGrowth());
        }

        @Test
        @DisplayName("hadGrowth should return false when ending <= starting")
        void hadGrowthFalse() {
            AccountMonthlyFlow flow = AccountMonthlyFlow.builder(ACCOUNT_ID, ACCOUNT_NAME)
                    .startingBalance(new BigDecimal("100000"))
                    .withdrawals(new BigDecimal("5000"))
                    .build();

            assertFalse(flow.hadGrowth());
        }
    }

    @Nested
    @DisplayName("withReturns")
    class WithReturns {

        @Test
        @DisplayName("should create new instance with updated returns")
        void shouldCreateNewInstance() {
            AccountMonthlyFlow original = AccountMonthlyFlow.builder(ACCOUNT_ID, ACCOUNT_NAME)
                    .startingBalance(new BigDecimal("100000"))
                    .contributions(new BigDecimal("1000"))
                    .build();

            AccountMonthlyFlow updated = original.withReturns(
                    new BigDecimal("800"),
                    new BigDecimal("101800")
            );

            // Original unchanged
            assertEquals(BigDecimal.ZERO, original.returns());
            assertEquals(new BigDecimal("101000.00"), original.endingBalance());

            // Updated has new values
            assertEquals(new BigDecimal("800"), updated.returns());
            assertEquals(new BigDecimal("101800"), updated.endingBalance());

            // Other fields unchanged
            assertEquals(original.accountId(), updated.accountId());
            assertEquals(original.contributions(), updated.contributions());
        }
    }
}
