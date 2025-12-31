package io.github.xmljim.retirement.simulation.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.calculator.SimulationView;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.value.AccountSnapshot;
import io.github.xmljim.retirement.domain.value.AssetAllocation;

@DisplayName("SimulationViewSnapshot")
class SimulationViewSnapshotTest {

    private AccountSnapshot createTestSnapshot(String id, BigDecimal balance) {
        return new AccountSnapshot(
                id,
                "Test Account",
                AccountType.TRADITIONAL_401K,
                balance,
                AccountType.TaxTreatment.PRE_TAX,
                true,
                AssetAllocation.balanced()
        );
    }

    @Nested
    @DisplayName("SimulationView Implementation")
    class SimulationViewImplementation {

        @Test
        @DisplayName("should implement SimulationView interface")
        void shouldImplementSimulationView() {
            SimulationViewSnapshot snapshot = SimulationViewSnapshot.builder().build();
            assertTrue(snapshot instanceof SimulationView);
        }

        @Test
        @DisplayName("getTotalPortfolioBalance should return total")
        void getTotalPortfolioBalanceShouldReturnTotal() {
            SimulationViewSnapshot snapshot = SimulationViewSnapshot.builder()
                    .totalPortfolioBalance(new BigDecimal("500000"))
                    .build();

            assertEquals(new BigDecimal("500000"), snapshot.getTotalPortfolioBalance());
        }

        @Test
        @DisplayName("getAccountBalance should find account by UUID")
        void getAccountBalanceShouldFindByUuid() {
            String accountId = UUID.randomUUID().toString();
            List<AccountSnapshot> accounts = List.of(
                    createTestSnapshot(accountId, new BigDecimal("100000"))
            );

            SimulationViewSnapshot snapshot = SimulationViewSnapshot.builder()
                    .accountSnapshots(accounts)
                    .build();

            assertEquals(new BigDecimal("100000"),
                    snapshot.getAccountBalance(UUID.fromString(accountId)));
        }

        @Test
        @DisplayName("getAccountBalance should return ZERO for unknown account")
        void getAccountBalanceShouldReturnZeroForUnknown() {
            SimulationViewSnapshot snapshot = SimulationViewSnapshot.builder().build();

            assertEquals(BigDecimal.ZERO,
                    snapshot.getAccountBalance(UUID.randomUUID()));
        }

        @Test
        @DisplayName("getAccountBalance should handle null UUID")
        void getAccountBalanceShouldHandleNull() {
            SimulationViewSnapshot snapshot = SimulationViewSnapshot.builder().build();

            assertEquals(BigDecimal.ZERO, snapshot.getAccountBalance(null));
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("builder should create snapshot with all values")
        void builderShouldCreateWithAllValues() {
            String accountId = UUID.randomUUID().toString();
            List<AccountSnapshot> accounts = List.of(
                    createTestSnapshot(accountId, new BigDecimal("100000"))
            );
            YearMonth ratchetMonth = YearMonth.of(2024, 6);

            SimulationViewSnapshot snapshot = SimulationViewSnapshot.builder()
                    .accountSnapshots(accounts)
                    .totalPortfolioBalance(new BigDecimal("500000"))
                    .initialPortfolioBalance(new BigDecimal("400000"))
                    .priorYearSpending(new BigDecimal("40000"))
                    .priorYearReturn(new BigDecimal("0.08"))
                    .lastRatchetMonth(ratchetMonth)
                    .cumulativeWithdrawals(new BigDecimal("120000"))
                    .highWaterMarkBalance(new BigDecimal("550000"))
                    .build();

            assertEquals(1, snapshot.getAccountSnapshots().size());
            assertEquals(new BigDecimal("500000"), snapshot.getTotalPortfolioBalance());
            assertEquals(new BigDecimal("400000"), snapshot.getInitialPortfolioBalance());
            assertEquals(new BigDecimal("40000"), snapshot.getPriorYearSpending());
            assertEquals(new BigDecimal("0.08"), snapshot.getPriorYearReturn());
            assertEquals(Optional.of(ratchetMonth), snapshot.getLastRatchetMonth());
            assertEquals(new BigDecimal("120000"), snapshot.getCumulativeWithdrawals());
            assertEquals(new BigDecimal("550000"), snapshot.getHighWaterMarkBalance());
        }

        @Test
        @DisplayName("builder should use defaults for unset values")
        void builderShouldUseDefaults() {
            SimulationViewSnapshot snapshot = SimulationViewSnapshot.builder().build();

            assertTrue(snapshot.getAccountSnapshots().isEmpty());
            assertEquals(BigDecimal.ZERO, snapshot.getTotalPortfolioBalance());
            assertEquals(BigDecimal.ZERO, snapshot.getInitialPortfolioBalance());
            assertEquals(BigDecimal.ZERO, snapshot.getPriorYearSpending());
            assertEquals(BigDecimal.ZERO, snapshot.getPriorYearReturn());
            assertEquals(Optional.empty(), snapshot.getLastRatchetMonth());
            assertEquals(BigDecimal.ZERO, snapshot.getCumulativeWithdrawals());
            assertEquals(BigDecimal.ZERO, snapshot.getHighWaterMarkBalance());
        }

        @Test
        @DisplayName("lastRatchetMonth can be set as Optional")
        void lastRatchetMonthCanBeSetAsOptional() {
            YearMonth month = YearMonth.of(2024, 3);

            SimulationViewSnapshot snapshot = SimulationViewSnapshot.builder()
                    .lastRatchetMonth(Optional.of(month))
                    .build();

            assertEquals(Optional.of(month), snapshot.getLastRatchetMonth());
        }
    }

    @Nested
    @DisplayName("Null Handling")
    class NullHandling {

        @Test
        @DisplayName("should default null accountSnapshots")
        void shouldDefaultNullAccountSnapshots() {
            SimulationViewSnapshot snapshot = new SimulationViewSnapshot(
                    null, null, null, null, null, null, null, null);

            assertNotNull(snapshot.accountSnapshots());
            assertTrue(snapshot.accountSnapshots().isEmpty());
        }

        @Test
        @DisplayName("should default null BigDecimal values")
        void shouldDefaultNullBigDecimals() {
            SimulationViewSnapshot snapshot = new SimulationViewSnapshot(
                    null, null, null, null, null, null, null, null);

            assertEquals(BigDecimal.ZERO, snapshot.totalPortfolioBalance());
            assertEquals(BigDecimal.ZERO, snapshot.initialPortfolioBalance());
            assertEquals(BigDecimal.ZERO, snapshot.priorYearSpending());
            assertEquals(BigDecimal.ZERO, snapshot.priorYearReturn());
            assertEquals(BigDecimal.ZERO, snapshot.cumulativeWithdrawals());
            assertEquals(BigDecimal.ZERO, snapshot.highWaterMarkBalance());
        }

        @Test
        @DisplayName("should default null lastRatchetMonth")
        void shouldDefaultNullLastRatchetMonth() {
            SimulationViewSnapshot snapshot = new SimulationViewSnapshot(
                    null, null, null, null, null, null, null, null);

            assertEquals(Optional.empty(), snapshot.lastRatchetMonth());
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("accountSnapshots should be unmodifiable")
        void accountSnapshotsShouldBeUnmodifiable() {
            String accountId = UUID.randomUUID().toString();
            List<AccountSnapshot> accounts = List.of(
                    createTestSnapshot(accountId, new BigDecimal("100000"))
            );

            SimulationViewSnapshot snapshot = SimulationViewSnapshot.builder()
                    .accountSnapshots(accounts)
                    .build();

            assertThrows(UnsupportedOperationException.class,
                    () -> snapshot.accountSnapshots().add(
                            createTestSnapshot("new", new BigDecimal("50000"))));
        }
    }
}
