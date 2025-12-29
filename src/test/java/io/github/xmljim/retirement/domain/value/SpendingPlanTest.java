package io.github.xmljim.retirement.domain.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.model.InvestmentAccount;

@DisplayName("SpendingPlan Tests")
class SpendingPlanTest {

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should create valid plan with builder")
        void createsValidPlan() {
            SpendingPlan plan = SpendingPlan.builder()
                    .targetWithdrawal(new BigDecimal("5000.00"))
                    .adjustedWithdrawal(new BigDecimal("5000.00"))
                    .meetsTarget(true)
                    .shortfall(BigDecimal.ZERO)
                    .strategyUsed("Static 4%")
                    .build();

            assertEquals(0, new BigDecimal("5000.00").compareTo(plan.targetWithdrawal()));
            assertEquals(0, new BigDecimal("5000.00").compareTo(plan.adjustedWithdrawal()));
            assertTrue(plan.meetsTarget());
            assertEquals(0, BigDecimal.ZERO.compareTo(plan.shortfall()));
            assertEquals("Static 4%", plan.strategyUsed());
        }

        @Test
        @DisplayName("Should default amounts to zero")
        void defaultsAmountsToZero() {
            SpendingPlan plan = SpendingPlan.builder()
                    .strategyUsed("Test")
                    .build();

            assertEquals(0, BigDecimal.ZERO.compareTo(plan.targetWithdrawal()));
            assertEquals(0, BigDecimal.ZERO.compareTo(plan.adjustedWithdrawal()));
            assertEquals(0, BigDecimal.ZERO.compareTo(plan.shortfall()));
        }

        @Test
        @DisplayName("Should default collections to empty")
        void defaultsCollectionsToEmpty() {
            SpendingPlan plan = SpendingPlan.builder()
                    .strategyUsed("Test")
                    .build();

            assertTrue(plan.accountWithdrawals().isEmpty());
            assertTrue(plan.metadata().isEmpty());
        }

        @Test
        @DisplayName("Should support metadata builder methods")
        void supportsMetadataBuilder() {
            SpendingPlan plan = SpendingPlan.builder()
                    .strategyUsed("Guardrails")
                    .addMetadata("adjustment", "increase")
                    .addMetadata("reason", "prosperity rule triggered")
                    .build();

            assertEquals("increase", plan.metadata().get("adjustment"));
            assertEquals("prosperity rule triggered", plan.metadata().get("reason"));
        }
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("noWithdrawalNeeded should create zero withdrawal plan")
        void noWithdrawalNeeded() {
            SpendingPlan plan = SpendingPlan.noWithdrawalNeeded("Income Gap");

            assertEquals(0, BigDecimal.ZERO.compareTo(plan.targetWithdrawal()));
            assertEquals(0, BigDecimal.ZERO.compareTo(plan.adjustedWithdrawal()));
            assertTrue(plan.meetsTarget());
            assertEquals("Income Gap", plan.strategyUsed());
            assertTrue(plan.accountWithdrawals().isEmpty());
        }
    }

    @Nested
    @DisplayName("Tax Calculation Tests")
    class TaxCalculationTests {

        @Test
        @DisplayName("Should calculate total taxable amount")
        void calculatesTotalTaxable() {
            List<AccountWithdrawal> withdrawals = List.of(
                    createWithdrawal(AccountType.TRADITIONAL_401K, new BigDecimal("3000.00")),
                    createWithdrawal(AccountType.TRADITIONAL_IRA, new BigDecimal("2000.00")),
                    createWithdrawal(AccountType.ROTH_IRA, new BigDecimal("1000.00"))
            );

            SpendingPlan plan = SpendingPlan.builder()
                    .accountWithdrawals(withdrawals)
                    .strategyUsed("Test")
                    .build();

            // Traditional 401k + Traditional IRA = $5000 taxable
            assertEquals(0, new BigDecimal("5000.00").compareTo(plan.totalTaxableAmount()));
        }

        @Test
        @DisplayName("Should calculate total tax-free amount")
        void calculatesTotalTaxFree() {
            List<AccountWithdrawal> withdrawals = List.of(
                    createWithdrawal(AccountType.TRADITIONAL_401K, new BigDecimal("3000.00")),
                    createWithdrawal(AccountType.ROTH_IRA, new BigDecimal("2000.00")),
                    createWithdrawal(AccountType.HSA, new BigDecimal("500.00"))
            );

            SpendingPlan plan = SpendingPlan.builder()
                    .accountWithdrawals(withdrawals)
                    .strategyUsed("Test")
                    .build();

            // Roth + HSA = $2500 tax-free
            assertEquals(0, new BigDecimal("2500.00").compareTo(plan.totalTaxFreeAmount()));
        }
    }

    @Nested
    @DisplayName("Depletion Tracking Tests")
    class DepletionTrackingTests {

        @Test
        @DisplayName("Should count depleted accounts")
        void countsDepletedAccounts() {
            List<AccountWithdrawal> withdrawals = List.of(
                    createWithdrawal(AccountType.TRADITIONAL_401K, new BigDecimal("1000.00"),
                            new BigDecimal("1000.00"), BigDecimal.ZERO), // depleted
                    createWithdrawal(AccountType.TRADITIONAL_IRA, new BigDecimal("500.00"),
                            new BigDecimal("500.00"), BigDecimal.ZERO),  // depleted
                    createWithdrawal(AccountType.ROTH_IRA, new BigDecimal("500.00"),
                            new BigDecimal("10000.00"), new BigDecimal("9500.00")) // not depleted
            );

            SpendingPlan plan = SpendingPlan.builder()
                    .accountWithdrawals(withdrawals)
                    .strategyUsed("Test")
                    .build();

            assertEquals(2, plan.depletedAccountCount());
            assertTrue(plan.hasDepletedAccounts());
        }

        @Test
        @DisplayName("Should detect no depleted accounts")
        void detectsNoDepletedAccounts() {
            List<AccountWithdrawal> withdrawals = List.of(
                    createWithdrawal(AccountType.TRADITIONAL_401K, new BigDecimal("1000.00"),
                            new BigDecimal("100000.00"), new BigDecimal("99000.00"))
            );

            SpendingPlan plan = SpendingPlan.builder()
                    .accountWithdrawals(withdrawals)
                    .strategyUsed("Test")
                    .build();

            assertEquals(0, plan.depletedAccountCount());
            assertFalse(plan.hasDepletedAccounts());
        }
    }

    @Nested
    @DisplayName("Immutability Tests")
    class ImmutabilityTests {

        @Test
        @DisplayName("Account withdrawals list should be unmodifiable")
        void withdrawalsUnmodifiable() {
            List<AccountWithdrawal> withdrawals = List.of(
                    createWithdrawal(AccountType.TRADITIONAL_401K, new BigDecimal("1000.00"))
            );

            SpendingPlan plan = SpendingPlan.builder()
                    .accountWithdrawals(withdrawals)
                    .strategyUsed("Test")
                    .build();

            var planWithdrawals = plan.accountWithdrawals();
            org.junit.jupiter.api.Assertions.assertThrows(
                    UnsupportedOperationException.class,
                    () -> planWithdrawals.add(createWithdrawal(AccountType.ROTH_IRA, BigDecimal.TEN))
            );
        }

        @Test
        @DisplayName("Metadata map should be unmodifiable")
        void metadataUnmodifiable() {
            SpendingPlan plan = SpendingPlan.builder()
                    .metadata(Map.of("key", "value"))
                    .strategyUsed("Test")
                    .build();

            var metadata = plan.metadata();
            org.junit.jupiter.api.Assertions.assertThrows(
                    UnsupportedOperationException.class,
                    () -> metadata.put("newKey", "newValue")
            );
        }
    }

    // Helper methods

    private InvestmentAccount createAccount(AccountType type) {
        return InvestmentAccount.builder()
                .name("Test " + type.name())
                .accountType(type)
                .balance(new BigDecimal("100000.00"))
                .allocation(AssetAllocation.of(60, 35, 5))
                .preRetirementReturnRate(0.07)
                .build();
    }

    private AccountWithdrawal createWithdrawal(AccountType type, BigDecimal amount) {
        return AccountWithdrawal.builder()
                .account(createAccount(type))
                .amount(amount)
                .build();
    }

    private AccountWithdrawal createWithdrawal(
            AccountType type,
            BigDecimal amount,
            BigDecimal priorBalance,
            BigDecimal newBalance) {
        return AccountWithdrawal.builder()
                .account(createAccount(type))
                .amount(amount)
                .priorBalance(priorBalance)
                .newBalance(newBalance)
                .build();
    }
}
