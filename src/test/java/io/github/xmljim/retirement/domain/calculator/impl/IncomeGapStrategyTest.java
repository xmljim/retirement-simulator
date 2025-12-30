package io.github.xmljim.retirement.domain.calculator.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.calculator.StubSimulationView;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.value.SpendingContext;
import io.github.xmljim.retirement.domain.value.SpendingPlan;

@DisplayName("IncomeGapStrategy Tests")
class IncomeGapStrategyTest {

    private IncomeGapStrategy strategy;
    private LocalDate retirementStart;

    @BeforeEach
    void setUp() {
        strategy = new IncomeGapStrategy();
        retirementStart = LocalDate.of(2020, 1, 1);
    }

    private SpendingContext createContext(BigDecimal balance, BigDecimal expenses, BigDecimal otherIncome) {
        StubSimulationView simulation = StubSimulationView.builder()
                .addAccount(StubSimulationView.createTestAccount(
                        "Portfolio", AccountType.TRADITIONAL_401K, balance))
                .build();

        return SpendingContext.builder()
                .simulation(simulation)
                .date(LocalDate.now())
                .retirementStartDate(retirementStart)
                .totalExpenses(expenses)
                .otherIncome(otherIncome)
                .build();
    }

    @Nested
    @DisplayName("Basic Income Gap Tests")
    class BasicGapTests {

        @Test
        @DisplayName("Should withdraw exact gap when expenses exceed income")
        void withdrawsExactGap() {
            // $5,000 expenses - $3,000 SS = $2,000 gap
            SpendingContext context = createContext(
                    new BigDecimal("500000"),
                    new BigDecimal("5000"),
                    new BigDecimal("3000")
            );

            SpendingPlan plan = strategy.calculateWithdrawal(context);

            assertEquals(0, new BigDecimal("2000").compareTo(
                    plan.targetWithdrawal().setScale(2, RoundingMode.HALF_UP)));
            assertEquals(0, new BigDecimal("2000").compareTo(
                    plan.adjustedWithdrawal().setScale(2, RoundingMode.HALF_UP)));
            assertTrue(plan.meetsTarget());
        }

        @Test
        @DisplayName("Should withdraw zero when income covers expenses")
        void zeroWhenIncomeCoversExpenses() {
            // $3,000 expenses - $4,000 SS = $0 gap (surplus)
            SpendingContext context = createContext(
                    new BigDecimal("500000"),
                    new BigDecimal("3000"),
                    new BigDecimal("4000")
            );

            SpendingPlan plan = strategy.calculateWithdrawal(context);

            assertEquals(0, BigDecimal.ZERO.compareTo(plan.targetWithdrawal()));
            assertEquals(0, BigDecimal.ZERO.compareTo(plan.adjustedWithdrawal()));
            assertTrue(plan.meetsTarget());
        }

        @Test
        @DisplayName("Should withdraw zero when income exactly equals expenses")
        void zeroWhenIncomeEqualsExpenses() {
            // $4,000 expenses - $4,000 SS = $0 gap
            SpendingContext context = createContext(
                    new BigDecimal("500000"),
                    new BigDecimal("4000"),
                    new BigDecimal("4000")
            );

            SpendingPlan plan = strategy.calculateWithdrawal(context);

            assertEquals(0, BigDecimal.ZERO.compareTo(plan.targetWithdrawal()));
            assertTrue(plan.meetsTarget());
        }

        @Test
        @DisplayName("Should handle no other income (full expenses as gap)")
        void fullExpensesWhenNoOtherIncome() {
            // $5,000 expenses - $0 other income = $5,000 gap
            SpendingContext context = createContext(
                    new BigDecimal("500000"),
                    new BigDecimal("5000"),
                    BigDecimal.ZERO
            );

            SpendingPlan plan = strategy.calculateWithdrawal(context);

            assertEquals(0, new BigDecimal("5000").compareTo(
                    plan.targetWithdrawal().setScale(2, RoundingMode.HALF_UP)));
        }
    }

    @Nested
    @DisplayName("Tax Gross-Up Tests")
    class TaxGrossUpTests {

        @Test
        @DisplayName("Should gross up for 22% tax rate")
        void grossUpFor22Percent() {
            IncomeGapStrategy taxAwareStrategy = new IncomeGapStrategy(new BigDecimal("0.22"));

            // $5,000 expenses - $3,000 SS = $2,000 net gap
            // Gross = $2,000 / (1 - 0.22) = $2,564.10
            SpendingContext context = createContext(
                    new BigDecimal("500000"),
                    new BigDecimal("5000"),
                    new BigDecimal("3000")
            );

            SpendingPlan plan = taxAwareStrategy.calculateWithdrawal(context);

            BigDecimal expected = new BigDecimal("2000")
                    .divide(new BigDecimal("0.78"), 2, RoundingMode.HALF_UP);
            assertEquals(0, expected.compareTo(
                    plan.targetWithdrawal().setScale(2, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should gross up for 12% tax rate")
        void grossUpFor12Percent() {
            IncomeGapStrategy taxAwareStrategy = new IncomeGapStrategy(new BigDecimal("0.12"));

            // $4,000 expenses - $2,500 SS = $1,500 net gap
            // Gross = $1,500 / (1 - 0.12) = $1,704.55
            SpendingContext context = createContext(
                    new BigDecimal("500000"),
                    new BigDecimal("4000"),
                    new BigDecimal("2500")
            );

            SpendingPlan plan = taxAwareStrategy.calculateWithdrawal(context);

            BigDecimal expected = new BigDecimal("1500")
                    .divide(new BigDecimal("0.88"), 2, RoundingMode.HALF_UP);
            assertEquals(0, expected.compareTo(
                    plan.targetWithdrawal().setScale(2, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should not gross up when gap is zero")
        void noGrossUpWhenGapIsZero() {
            IncomeGapStrategy taxAwareStrategy = new IncomeGapStrategy(new BigDecimal("0.22"));

            SpendingContext context = createContext(
                    new BigDecimal("500000"),
                    new BigDecimal("3000"),
                    new BigDecimal("4000")
            );

            SpendingPlan plan = taxAwareStrategy.calculateWithdrawal(context);

            assertEquals(0, BigDecimal.ZERO.compareTo(plan.targetWithdrawal()));
        }

        @Test
        @DisplayName("Should not gross up when tax rate is zero")
        void noGrossUpWhenTaxRateIsZero() {
            IncomeGapStrategy noTaxStrategy = new IncomeGapStrategy(BigDecimal.ZERO);

            SpendingContext context = createContext(
                    new BigDecimal("500000"),
                    new BigDecimal("5000"),
                    new BigDecimal("3000")
            );

            SpendingPlan plan = noTaxStrategy.calculateWithdrawal(context);

            // No gross-up, just the net gap
            assertEquals(0, new BigDecimal("2000").compareTo(
                    plan.targetWithdrawal().setScale(2, RoundingMode.HALF_UP)));
        }
    }

    @Nested
    @DisplayName("Portfolio Constraint Tests")
    class PortfolioConstraintTests {

        @Test
        @DisplayName("Should cap at portfolio balance when insufficient")
        void capsAtPortfolioBalance() {
            SpendingContext context = createContext(
                    new BigDecimal("1000"),  // Only $1,000 in portfolio
                    new BigDecimal("5000"),
                    new BigDecimal("2000")   // Gap = $3,000
            );

            SpendingPlan plan = strategy.calculateWithdrawal(context);

            assertEquals(0, new BigDecimal("3000").compareTo(
                    plan.targetWithdrawal().setScale(2, RoundingMode.HALF_UP)));
            assertEquals(0, new BigDecimal("1000").compareTo(
                    plan.adjustedWithdrawal().setScale(2, RoundingMode.HALF_UP)));
            assertFalse(plan.meetsTarget());
        }

        @Test
        @DisplayName("Should return zero when portfolio is empty")
        void zeroWhenPortfolioEmpty() {
            SpendingContext context = createContext(
                    BigDecimal.ZERO,
                    new BigDecimal("5000"),
                    new BigDecimal("3000")
            );

            SpendingPlan plan = strategy.calculateWithdrawal(context);

            assertEquals(0, BigDecimal.ZERO.compareTo(plan.adjustedWithdrawal()));
            assertFalse(plan.meetsTarget());
        }
    }

    @Nested
    @DisplayName("Metadata Tests")
    class MetadataTests {

        @Test
        @DisplayName("Should include income and expense details in metadata")
        void includesIncomeExpenseDetails() {
            SpendingContext context = createContext(
                    new BigDecimal("500000"),
                    new BigDecimal("5000"),
                    new BigDecimal("3000")
            );

            SpendingPlan plan = strategy.calculateWithdrawal(context);

            assertEquals("5000.00", plan.metadata().get("totalExpenses"));
            assertEquals("3000.00", plan.metadata().get("otherIncome"));
            assertEquals("2000.00", plan.metadata().get("incomeGap"));
        }

        @Test
        @DisplayName("Should include tax configuration in metadata")
        void includesTaxConfig() {
            IncomeGapStrategy taxAwareStrategy = new IncomeGapStrategy(new BigDecimal("0.22"));

            SpendingContext context = createContext(
                    new BigDecimal("500000"),
                    new BigDecimal("5000"),
                    new BigDecimal("3000")
            );

            SpendingPlan plan = taxAwareStrategy.calculateWithdrawal(context);

            assertEquals("true", plan.metadata().get("grossUpForTaxes"));
            assertEquals("0.22", plan.metadata().get("marginalTaxRate"));
        }
    }

    @Nested
    @DisplayName("Strategy Properties Tests")
    class StrategyPropertiesTests {

        @Test
        @DisplayName("Should have descriptive name")
        void hasDescriptiveName() {
            assertEquals("Income Gap", strategy.getName());
        }

        @Test
        @DisplayName("Should have description without tax info when no gross-up")
        void descriptionWithoutTax() {
            String description = strategy.getDescription();
            assertTrue(description.contains("gap between expenses and other income"));
            assertFalse(description.contains("tax"));
        }

        @Test
        @DisplayName("Should have description with tax info when gross-up enabled")
        void descriptionWithTax() {
            IncomeGapStrategy taxAwareStrategy = new IncomeGapStrategy(new BigDecimal("0.22"));
            String description = taxAwareStrategy.getDescription();
            assertTrue(description.contains("22%"));
            assertTrue(description.contains("tax"));
        }

        @Test
        @DisplayName("Should not be dynamic")
        void isNotDynamic() {
            assertFalse(strategy.isDynamic());
        }

        @Test
        @DisplayName("Should not require prior year state")
        void doesNotRequirePriorYearState() {
            assertFalse(strategy.requiresPriorYearState());
        }

        @Test
        @DisplayName("Should expose configuration via getters")
        void exposesConfiguration() {
            IncomeGapStrategy taxAwareStrategy = new IncomeGapStrategy(new BigDecimal("0.22"));
            assertEquals(0, new BigDecimal("0.22").compareTo(taxAwareStrategy.getMarginalTaxRate()));
            assertTrue(taxAwareStrategy.isGrossUpForTaxes());
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Default constructor has no gross-up")
        void defaultConstructorNoGrossUp() {
            IncomeGapStrategy defaultStrategy = new IncomeGapStrategy();
            assertEquals(0, BigDecimal.ZERO.compareTo(defaultStrategy.getMarginalTaxRate()));
            assertFalse(defaultStrategy.isGrossUpForTaxes());
        }

        @Test
        @DisplayName("Null tax rate treated as zero")
        void nullTaxRateTreatedAsZero() {
            IncomeGapStrategy nullTaxStrategy = new IncomeGapStrategy(null);
            assertEquals(0, BigDecimal.ZERO.compareTo(nullTaxStrategy.getMarginalTaxRate()));
            assertFalse(nullTaxStrategy.isGrossUpForTaxes());
        }
    }
}
