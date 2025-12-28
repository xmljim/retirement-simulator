package io.github.xmljim.retirement.domain.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.ExpenseCategory;
import io.github.xmljim.retirement.domain.enums.SurvivorRole;
import io.github.xmljim.retirement.domain.value.SurvivorAdjustment;

@DisplayName("SurvivorExpenseCalculator Tests")
class SurvivorExpenseCalculatorTest {

    private SurvivorExpenseCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = CalculatorFactory.survivorExpenseCalculator();
    }

    @Nested
    @DisplayName("Multiplier Tests")
    class MultiplierTests {

        @Test
        @DisplayName("Housing is unchanged (1.0)")
        void housingUnchanged() {
            BigDecimal multiplier = calculator.getMultiplier(ExpenseCategory.HOUSING);
            assertEquals(0, BigDecimal.ONE.compareTo(multiplier));
        }

        @Test
        @DisplayName("Food is reduced to 60%")
        void foodReduced() {
            BigDecimal multiplier = calculator.getMultiplier(ExpenseCategory.FOOD);
            assertEquals(0, new BigDecimal("0.60").compareTo(multiplier));
        }

        @Test
        @DisplayName("Healthcare OOP is halved (50%)")
        void healthcareHalved() {
            BigDecimal multiplier = calculator.getMultiplier(ExpenseCategory.HEALTHCARE_OOP);
            assertEquals(0, new BigDecimal("0.50").compareTo(multiplier));
        }

        @Test
        @DisplayName("Unknown category gets default multiplier")
        void unknownCategoryDefault() {
            BigDecimal multiplier = calculator.getMultiplier(ExpenseCategory.OTHER);
            // Default is 0.75
            assertEquals(0, new BigDecimal("0.75").compareTo(multiplier));
        }
    }

    @Nested
    @DisplayName("Adjustment Tests")
    class AdjustmentTests {

        @Test
        @DisplayName("adjustExpense applies multiplier")
        void adjustExpenseAppliesMultiplier() {
            BigDecimal coupleFood = new BigDecimal("12000");
            BigDecimal survivorFood = calculator.adjustExpense(ExpenseCategory.FOOD, coupleFood);

            // 12000 * 0.60 = 7200
            assertEquals(0, new BigDecimal("7200.00").compareTo(survivorFood));
        }

        @Test
        @DisplayName("getAdjustment returns full record")
        void getAdjustmentReturnsRecord() {
            SurvivorAdjustment adjustment = calculator.getAdjustment(ExpenseCategory.HOUSING);

            assertNotNull(adjustment);
            assertEquals(ExpenseCategory.HOUSING, adjustment.category());
            assertEquals(0, BigDecimal.ONE.compareTo(adjustment.multiplier()));
            assertNotNull(adjustment.rationale());
        }
    }

    @Nested
    @DisplayName("Calculate Survivor Expenses Tests")
    class CalculateSurvivorExpensesTests {

        @Test
        @DisplayName("Calculate all survivor expenses")
        void calculateAllExpenses() {
            Map<ExpenseCategory, BigDecimal> coupleExpenses = new HashMap<>();
            coupleExpenses.put(ExpenseCategory.HOUSING, new BigDecimal("24000"));
            coupleExpenses.put(ExpenseCategory.FOOD, new BigDecimal("12000"));
            coupleExpenses.put(ExpenseCategory.HEALTHCARE_OOP, new BigDecimal("8000"));

            Map<ExpenseCategory, BigDecimal> survivorExpenses =
                calculator.calculateSurvivorExpenses(coupleExpenses, SurvivorRole.PRIMARY);

            assertEquals(3, survivorExpenses.size());
            assertEquals(0, new BigDecimal("24000.00").compareTo(survivorExpenses.get(ExpenseCategory.HOUSING)));
            assertEquals(0, new BigDecimal("7200.00").compareTo(survivorExpenses.get(ExpenseCategory.FOOD)));
            assertEquals(0, new BigDecimal("4000.00").compareTo(survivorExpenses.get(ExpenseCategory.HEALTHCARE_OOP)));
        }

        @Test
        @DisplayName("Total couple expenses")
        void totalCoupleExpenses() {
            Map<ExpenseCategory, BigDecimal> coupleExpenses = new HashMap<>();
            coupleExpenses.put(ExpenseCategory.HOUSING, new BigDecimal("24000"));
            coupleExpenses.put(ExpenseCategory.FOOD, new BigDecimal("12000"));

            BigDecimal total = calculator.getTotalCoupleExpenses(coupleExpenses);

            assertEquals(0, new BigDecimal("36000").compareTo(total));
        }

        @Test
        @DisplayName("Total survivor expenses")
        void totalSurvivorExpenses() {
            Map<ExpenseCategory, BigDecimal> coupleExpenses = new HashMap<>();
            coupleExpenses.put(ExpenseCategory.HOUSING, new BigDecimal("24000"));  // 1.0 -> 24000
            coupleExpenses.put(ExpenseCategory.FOOD, new BigDecimal("12000"));     // 0.6 -> 7200

            BigDecimal total = calculator.getTotalSurvivorExpenses(coupleExpenses, SurvivorRole.PRIMARY);

            // 24000 + 7200 = 31200
            assertEquals(0, new BigDecimal("31200.00").compareTo(total));
        }
    }

    @Nested
    @DisplayName("Reduction Percentage Tests")
    class ReductionPercentageTests {

        @Test
        @DisplayName("Calculate overall reduction percentage")
        void overallReduction() {
            Map<ExpenseCategory, BigDecimal> coupleExpenses = new HashMap<>();
            coupleExpenses.put(ExpenseCategory.HOUSING, new BigDecimal("24000"));       // 1.0 -> 24000
            coupleExpenses.put(ExpenseCategory.FOOD, new BigDecimal("12000"));          // 0.6 -> 7200
            coupleExpenses.put(ExpenseCategory.HEALTHCARE_OOP, new BigDecimal("8000")); // 0.5 -> 4000
            // Total couple: 44000
            // Total survivor: 35200
            // Reduction: 8800 / 44000 = 0.20

            BigDecimal reduction = calculator.getOverallReductionPercentage(
                coupleExpenses, SurvivorRole.PRIMARY);

            assertEquals(0, new BigDecimal("0.2000").compareTo(reduction));
        }

        @Test
        @DisplayName("All fixed costs shows no reduction")
        void allFixedCosts() {
            Map<ExpenseCategory, BigDecimal> coupleExpenses = new HashMap<>();
            coupleExpenses.put(ExpenseCategory.HOUSING, new BigDecimal("24000"));
            coupleExpenses.put(ExpenseCategory.DEBT_PAYMENTS, new BigDecimal("6000"));

            BigDecimal reduction = calculator.getOverallReductionPercentage(
                coupleExpenses, SurvivorRole.PRIMARY);

            assertEquals(0, BigDecimal.ZERO.compareTo(reduction));
        }

        @Test
        @DisplayName("Empty expenses returns zero reduction")
        void emptyExpenses() {
            Map<ExpenseCategory, BigDecimal> coupleExpenses = new HashMap<>();

            BigDecimal reduction = calculator.getOverallReductionPercentage(
                coupleExpenses, SurvivorRole.PRIMARY);

            assertEquals(0, BigDecimal.ZERO.compareTo(reduction));
        }
    }

    @Nested
    @DisplayName("SurvivorAdjustment Record Tests")
    class SurvivorAdjustmentTests {

        @Test
        @DisplayName("apply() multiplies amount")
        void applyMultiplies() {
            SurvivorAdjustment adjustment = SurvivorAdjustment.of(
                ExpenseCategory.FOOD, 0.60, "Food costs reduced");

            BigDecimal result = adjustment.apply(new BigDecimal("10000"));

            assertEquals(0, new BigDecimal("6000.00").compareTo(result));
        }

        @Test
        @DisplayName("unchanged factory method")
        void unchangedFactory() {
            SurvivorAdjustment adjustment = SurvivorAdjustment.unchanged(
                ExpenseCategory.HOUSING, "Fixed cost");

            assertTrue(adjustment.isUnchanged());
            assertFalse(adjustment.isReduction());
        }

        @Test
        @DisplayName("halved factory method")
        void halvedFactory() {
            SurvivorAdjustment adjustment = SurvivorAdjustment.halved(
                ExpenseCategory.HEALTHCARE_OOP, "Single person");

            assertFalse(adjustment.isUnchanged());
            assertTrue(adjustment.isReduction());
            assertEquals(0, new BigDecimal("0.5").compareTo(adjustment.multiplier()));
        }

        @Test
        @DisplayName("getMultiplierPercent formats correctly")
        void multiplierPercent() {
            SurvivorAdjustment adjustment = SurvivorAdjustment.of(
                ExpenseCategory.FOOD, 0.70, "Reduced");

            assertEquals("70%", adjustment.getMultiplierPercent());
        }
    }
}
