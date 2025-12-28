package io.github.xmljim.retirement.domain.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.calculator.impl.PriorityBasedAllocationStrategy;
import io.github.xmljim.retirement.domain.enums.ExpenseCategory;
import io.github.xmljim.retirement.domain.enums.OverflowBehavior;
import io.github.xmljim.retirement.domain.enums.ShortfallHandling;
import io.github.xmljim.retirement.domain.value.AllocationResult;

@DisplayName("ExpenseAllocationStrategy Tests")
class ExpenseAllocationStrategyTest {

    private ExpenseAllocationStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = CalculatorFactory.expenseAllocationStrategy();
    }

    @Nested
    @DisplayName("Adequate Funds Tests")
    class AdequateFundsTests {

        @Test
        @DisplayName("All categories fully funded when funds sufficient")
        void allCategoriesFullyFunded() {
            Map<ExpenseCategory, BigDecimal> expenses = new HashMap<>();
            expenses.put(ExpenseCategory.HOUSING, new BigDecimal("2000"));
            expenses.put(ExpenseCategory.FOOD, new BigDecimal("600"));
            expenses.put(ExpenseCategory.HEALTHCARE_OOP, new BigDecimal("400"));

            AllocationResult result = strategy.allocate(new BigDecimal("5000"), expenses);

            assertTrue(result.isFullyFunded());
            assertFalse(result.hasShortfall());
            assertEquals(0, new BigDecimal("2000").compareTo(result.getAllocatedAmount(ExpenseCategory.HOUSING)));
            assertEquals(0, new BigDecimal("600").compareTo(result.getAllocatedAmount(ExpenseCategory.FOOD)));
            assertEquals(0, new BigDecimal("400").compareTo(result.getAllocatedAmount(ExpenseCategory.HEALTHCARE_OOP)));
        }

        @Test
        @DisplayName("Surplus calculated correctly")
        void surplusCalculated() {
            Map<ExpenseCategory, BigDecimal> expenses = new HashMap<>();
            expenses.put(ExpenseCategory.HOUSING, new BigDecimal("2000"));
            expenses.put(ExpenseCategory.FOOD, new BigDecimal("600"));

            AllocationResult result = strategy.allocate(new BigDecimal("3000"), expenses);

            assertTrue(result.hasSurplus());
            assertEquals(0, new BigDecimal("400").compareTo(result.surplus()));
        }

        @Test
        @DisplayName("Exact funds with no surplus")
        void exactFundsNoSurplus() {
            Map<ExpenseCategory, BigDecimal> expenses = new HashMap<>();
            expenses.put(ExpenseCategory.HOUSING, new BigDecimal("2500"));
            expenses.put(ExpenseCategory.FOOD, new BigDecimal("500"));

            AllocationResult result = strategy.allocate(new BigDecimal("3000"), expenses);

            assertTrue(result.isFullyFunded());
            assertFalse(result.hasSurplus());
            assertEquals(0, BigDecimal.ZERO.compareTo(result.surplus()));
        }
    }

    @Nested
    @DisplayName("Shortfall Tests")
    class ShortfallTests {

        @Test
        @DisplayName("Higher priority categories funded first")
        void higherPriorityFundedFirst() {
            Map<ExpenseCategory, BigDecimal> expenses = new HashMap<>();
            expenses.put(ExpenseCategory.HOUSING, new BigDecimal("2000"));
            expenses.put(ExpenseCategory.FOOD, new BigDecimal("600"));
            expenses.put(ExpenseCategory.TRAVEL, new BigDecimal("800"));

            // Only enough for housing and food
            AllocationResult result = strategy.allocate(new BigDecimal("2600"), expenses);

            // Essential categories should be fully funded
            assertEquals(0, new BigDecimal("2000").compareTo(result.getAllocatedAmount(ExpenseCategory.HOUSING)));
            assertEquals(0, new BigDecimal("600").compareTo(result.getAllocatedAmount(ExpenseCategory.FOOD)));
            // Discretionary should have shortfall
            assertEquals(0, BigDecimal.ZERO.compareTo(result.getAllocatedAmount(ExpenseCategory.TRAVEL)));
            assertTrue(result.hasShortfall());
        }

        @Test
        @DisplayName("Partial funding recorded correctly")
        void partialFundingRecorded() {
            Map<ExpenseCategory, BigDecimal> expenses = new HashMap<>();
            expenses.put(ExpenseCategory.HOUSING, new BigDecimal("2000"));
            expenses.put(ExpenseCategory.FOOD, new BigDecimal("600"));

            // Only enough for housing, partial food
            AllocationResult result = strategy.allocate(new BigDecimal("2300"), expenses);

            assertEquals(0, new BigDecimal("2000").compareTo(result.getAllocatedAmount(ExpenseCategory.HOUSING)));
            assertEquals(0, new BigDecimal("300").compareTo(result.getAllocatedAmount(ExpenseCategory.FOOD)));
            assertEquals(0, new BigDecimal("300").compareTo(result.getShortfallAmount(ExpenseCategory.FOOD)));
        }

        @Test
        @DisplayName("Warnings generated for shortfalls")
        void warningsGeneratedForShortfalls() {
            Map<ExpenseCategory, BigDecimal> expenses = new HashMap<>();
            expenses.put(ExpenseCategory.HOUSING, new BigDecimal("2000"));
            expenses.put(ExpenseCategory.FOOD, new BigDecimal("600"));

            AllocationResult result = strategy.allocate(new BigDecimal("2300"), expenses);

            assertTrue(result.hasWarnings());
            assertFalse(result.warnings().isEmpty());
        }
    }

    @Nested
    @DisplayName("Reserve Target Tests")
    class ReserveTargetTests {

        @Test
        @DisplayName("Reserve target met skips allocation")
        void reserveTargetMetSkipsAllocation() {
            Map<ExpenseCategory, BigDecimal> expenses = new HashMap<>();
            expenses.put(ExpenseCategory.HOUSING, new BigDecimal("2000"));
            expenses.put(ExpenseCategory.EMERGENCY_RESERVE, new BigDecimal("500"));

            Map<ExpenseCategory, Boolean> reserveTargetMet = new HashMap<>();
            reserveTargetMet.put(ExpenseCategory.EMERGENCY_RESERVE, true);

            AllocationResult result = strategy.allocate(
                    new BigDecimal("3000"), expenses, reserveTargetMet);

            // Housing should be funded
            assertEquals(0, new BigDecimal("2000").compareTo(result.getAllocatedAmount(ExpenseCategory.HOUSING)));
            // Reserve should be zero (target met)
            assertEquals(0, BigDecimal.ZERO.compareTo(result.getAllocatedAmount(ExpenseCategory.EMERGENCY_RESERVE)));
            // Should have surplus from skipped reserve
            assertTrue(result.hasSurplus());
        }
    }

    @Nested
    @DisplayName("Builder Configuration Tests")
    class BuilderTests {

        @Test
        @DisplayName("Custom priority order respected")
        void customPriorityOrderRespected() {
            ExpenseAllocationStrategy customStrategy = PriorityBasedAllocationStrategy.builder()
                    .priorityOrder(List.of(
                            ExpenseCategory.HEALTHCARE_OOP,
                            ExpenseCategory.HOUSING,
                            ExpenseCategory.FOOD))
                    .build();

            Map<ExpenseCategory, BigDecimal> expenses = new HashMap<>();
            expenses.put(ExpenseCategory.HOUSING, new BigDecimal("2000"));
            expenses.put(ExpenseCategory.HEALTHCARE_OOP, new BigDecimal("500"));
            expenses.put(ExpenseCategory.FOOD, new BigDecimal("600"));

            // Only enough for healthcare and housing
            AllocationResult result = customStrategy.allocate(new BigDecimal("2500"), expenses);

            // Healthcare first in custom order
            assertEquals(0, new BigDecimal("500").compareTo(result.getAllocatedAmount(ExpenseCategory.HEALTHCARE_OOP)));
            assertEquals(0, new BigDecimal("2000").compareTo(result.getAllocatedAmount(ExpenseCategory.HOUSING)));
            // Food should be unfunded
            assertEquals(0, BigDecimal.ZERO.compareTo(result.getAllocatedAmount(ExpenseCategory.FOOD)));
        }

        @Test
        @DisplayName("Builder creates valid strategy")
        void builderCreatesValidStrategy() {
            ExpenseAllocationStrategy customStrategy = PriorityBasedAllocationStrategy.builder()
                    .name("Custom Strategy")
                    .description("A custom allocation strategy")
                    .overflowBehavior(OverflowBehavior.SAVE_TO_PORTFOLIO)
                    .shortfallHandling(ShortfallHandling.PRORATE_ALL)
                    .build();

            assertEquals("Custom Strategy", customStrategy.getName());
            assertEquals("A custom allocation strategy", customStrategy.getDescription());
        }
    }

    @Nested
    @DisplayName("Default Priority Order Tests")
    class DefaultPriorityOrderTests {

        @Test
        @DisplayName("Default priority order includes all groups")
        void defaultPriorityOrderIncludesAllGroups() {
            List<ExpenseCategory> priorityOrder = strategy.getPriorityOrder();

            assertNotNull(priorityOrder);
            assertFalse(priorityOrder.isEmpty());
            // Should include categories from all groups
            assertTrue(priorityOrder.contains(ExpenseCategory.HOUSING));
            assertTrue(priorityOrder.contains(ExpenseCategory.HEALTHCARE_OOP));
            assertTrue(priorityOrder.contains(ExpenseCategory.DEBT_PAYMENTS));
            assertTrue(priorityOrder.contains(ExpenseCategory.HOME_REPAIRS));
            assertTrue(priorityOrder.contains(ExpenseCategory.TRAVEL));
        }

        @Test
        @DisplayName("Essential categories come before discretionary")
        void essentialBeforeDiscretionary() {
            List<ExpenseCategory> priorityOrder = strategy.getPriorityOrder();

            int housingIndex = priorityOrder.indexOf(ExpenseCategory.HOUSING);
            int travelIndex = priorityOrder.indexOf(ExpenseCategory.TRAVEL);

            assertTrue(housingIndex < travelIndex,
                    "Housing (essential) should come before Travel (discretionary)");
        }
    }

    @Nested
    @DisplayName("AllocationResult Tests")
    class AllocationResultTests {

        @Test
        @DisplayName("Funding percentage calculated correctly")
        void fundingPercentageCalculated() {
            Map<ExpenseCategory, BigDecimal> expenses = new HashMap<>();
            expenses.put(ExpenseCategory.FOOD, new BigDecimal("1000"));

            AllocationResult result = strategy.allocate(new BigDecimal("750"), expenses);

            BigDecimal percentage = result.getFundingPercentage(
                    ExpenseCategory.FOOD, new BigDecimal("1000"));

            assertEquals(0, new BigDecimal("0.7500").compareTo(percentage));
        }

        @Test
        @DisplayName("Builder shortfall tracking")
        void builderShortfallTracking() {
            AllocationResult result = AllocationResult.builder()
                    .allocate(ExpenseCategory.FOOD, new BigDecimal("600"), new BigDecimal("1000"))
                    .allocate(ExpenseCategory.HOUSING, new BigDecimal("2000"), new BigDecimal("2000"))
                    .build();

            assertEquals(0, new BigDecimal("400").compareTo(result.getShortfallAmount(ExpenseCategory.FOOD)));
            assertEquals(0, BigDecimal.ZERO.compareTo(result.getShortfallAmount(ExpenseCategory.HOUSING)));
        }

        @Test
        @DisplayName("Fully funded factory method")
        void fullyFundedFactory() {
            Map<ExpenseCategory, BigDecimal> allocations = new HashMap<>();
            allocations.put(ExpenseCategory.HOUSING, new BigDecimal("2000"));
            allocations.put(ExpenseCategory.FOOD, new BigDecimal("600"));

            AllocationResult result = AllocationResult.fullyFunded(allocations, new BigDecimal("400"));

            assertTrue(result.isFullyFunded());
            assertTrue(result.hasSurplus());
            assertEquals(0, new BigDecimal("400").compareTo(result.surplus()));
        }
    }

    @Nested
    @DisplayName("Enum Tests")
    class EnumTests {

        @Test
        @DisplayName("OverflowBehavior has display names")
        void overflowBehaviorDisplayNames() {
            for (OverflowBehavior behavior : OverflowBehavior.values()) {
                assertNotNull(behavior.getDisplayName());
                assertNotNull(behavior.getDescription());
                assertFalse(behavior.getDisplayName().isEmpty());
            }
        }

        @Test
        @DisplayName("ShortfallHandling has display names")
        void shortfallHandlingDisplayNames() {
            for (ShortfallHandling handling : ShortfallHandling.values()) {
                assertNotNull(handling.getDisplayName());
                assertNotNull(handling.getDescription());
                assertFalse(handling.getDisplayName().isEmpty());
            }
        }
    }
}
