package io.github.xmljim.retirement.domain.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.calculator.impl.DefaultWithdrawalCalculator;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.exception.CalculationException;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.model.InvestmentAccount;
import io.github.xmljim.retirement.domain.value.AssetAllocation;
import io.github.xmljim.retirement.domain.value.WithdrawalResult;
import io.github.xmljim.retirement.domain.value.WithdrawalStrategy;

@DisplayName("WithdrawalCalculator Tests")
class WithdrawalCalculatorTest {

    private WithdrawalCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new DefaultWithdrawalCalculator();
    }

    private InvestmentAccount createAccount(String name, AccountType type, double balance) {
        return InvestmentAccount.builder()
            .name(name)
            .accountType(type)
            .balance(balance)
            .allocation(AssetAllocation.balanced())
            .preRetirementReturnRate(0.07)
            .build();
    }

    @Nested
    @DisplayName("calculateWithdrawalAmount")
    class CalculateWithdrawalAmountTests {

        @Test
        @DisplayName("Should return fixed amount for FIXED strategy")
        void fixedStrategy() {
            BigDecimal portfolio = new BigDecimal("100000");
            WithdrawalStrategy strategy = WithdrawalStrategy.fixed(3000);

            BigDecimal result = calculator.calculateWithdrawalAmount(portfolio, strategy);

            assertEquals(0, new BigDecimal("3000.00").compareTo(result));
        }

        @Test
        @DisplayName("Should calculate percentage for PERCENTAGE strategy")
        void percentageStrategy() {
            BigDecimal portfolio = new BigDecimal("100000");
            WithdrawalStrategy strategy = WithdrawalStrategy.percentage(0.04); // 4%

            BigDecimal result = calculator.calculateWithdrawalAmount(portfolio, strategy);

            assertEquals(0, new BigDecimal("4000.00").compareTo(result));
        }

        @Test
        @DisplayName("Should throw for null portfolio balance")
        void nullPortfolio() {
            WithdrawalStrategy strategy = WithdrawalStrategy.fixed(1000);
            assertThrows(MissingRequiredFieldException.class, () ->
                calculator.calculateWithdrawalAmount(null, strategy));
        }

        @Test
        @DisplayName("Should throw for null strategy")
        void nullStrategy() {
            assertThrows(MissingRequiredFieldException.class, () ->
                calculator.calculateWithdrawalAmount(new BigDecimal("100000"), null));
        }
    }

    @Nested
    @DisplayName("calculateIncomeGap")
    class CalculateIncomeGapTests {

        @Test
        @DisplayName("Should calculate gap when expenses exceed income")
        void gapWhenExpensesExceedIncome() {
            BigDecimal expenses = new BigDecimal("5000");
            BigDecimal income = new BigDecimal("3000");

            BigDecimal result = calculator.calculateIncomeGap(expenses, income);

            assertEquals(0, new BigDecimal("2000.00").compareTo(result));
        }

        @Test
        @DisplayName("Should return zero when income exceeds expenses")
        void noGapWhenIncomeExceedsExpenses() {
            BigDecimal expenses = new BigDecimal("3000");
            BigDecimal income = new BigDecimal("5000");

            BigDecimal result = calculator.calculateIncomeGap(expenses, income);

            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        @DisplayName("Should handle null other income as zero")
        void nullOtherIncome() {
            BigDecimal expenses = new BigDecimal("5000");

            BigDecimal result = calculator.calculateIncomeGap(expenses, null);

            assertEquals(0, new BigDecimal("5000.00").compareTo(result));
        }

        @Test
        @DisplayName("Should throw for null expenses")
        void nullExpenses() {
            assertThrows(MissingRequiredFieldException.class, () ->
                calculator.calculateIncomeGap(null, new BigDecimal("1000")));
        }
    }

    @Nested
    @DisplayName("adjustForInflation")
    class AdjustForInflationTests {

        @Test
        @DisplayName("Should adjust amount for inflation")
        void adjustForInflation() {
            BigDecimal baseAmount = new BigDecimal("40000");
            BigDecimal inflationRate = new BigDecimal("0.03"); // 3%

            BigDecimal result = calculator.adjustForInflation(baseAmount, inflationRate, 5);

            // 40000 * (1.03)^5 = 46371.00 (approximately)
            assertTrue(result.compareTo(new BigDecimal("46000")) > 0);
            assertTrue(result.compareTo(new BigDecimal("47000")) < 0);
        }

        @Test
        @DisplayName("Should return base amount for zero years")
        void zeroYears() {
            BigDecimal baseAmount = new BigDecimal("40000");

            BigDecimal result = calculator.adjustForInflation(baseAmount, new BigDecimal("0.03"), 0);

            assertEquals(0, new BigDecimal("40000.00").compareTo(result));
        }

        @Test
        @DisplayName("Should handle null inflation rate")
        void nullInflationRate() {
            BigDecimal baseAmount = new BigDecimal("40000");

            BigDecimal result = calculator.adjustForInflation(baseAmount, null, 5);

            assertEquals(0, new BigDecimal("40000.00").compareTo(result));
        }

        @Test
        @DisplayName("Should throw for negative years")
        void negativeYears() {
            assertThrows(CalculationException.class, () ->
                calculator.adjustForInflation(new BigDecimal("40000"), new BigDecimal("0.03"), -1));
        }
    }

    @Nested
    @DisplayName("calculateWithdrawal")
    class CalculateWithdrawalTests {

        @Test
        @DisplayName("Should calculate full withdrawal when funds available")
        void fullWithdrawal() {
            InvestmentAccount account = createAccount("401k", AccountType.TRADITIONAL_401K, 100000);

            WithdrawalResult result = calculator.calculateWithdrawal(account, new BigDecimal("5000"));

            assertEquals(0, new BigDecimal("5000.00").compareTo(result.withdrawalAmount()));
            assertEquals(0, new BigDecimal("95000.00").compareTo(result.newBalance()));
            assertFalse(result.isPartialWithdrawal());
            assertFalse(result.isDepleted());
            assertTrue(result.isTaxable()); // PRE_TAX account
        }

        @Test
        @DisplayName("Should calculate partial withdrawal when insufficient funds")
        void partialWithdrawal() {
            InvestmentAccount account = createAccount("401k", AccountType.TRADITIONAL_401K, 3000);

            WithdrawalResult result = calculator.calculateWithdrawal(account, new BigDecimal("5000"));

            assertEquals(0, new BigDecimal("3000.00").compareTo(result.withdrawalAmount()));
            assertEquals(0, new BigDecimal("5000.00").compareTo(result.requestedAmount()));
            assertEquals(0, BigDecimal.ZERO.compareTo(result.newBalance()));
            assertTrue(result.isPartialWithdrawal());
            assertTrue(result.isDepleted());
        }

        @Test
        @DisplayName("Should return zero for depleted account")
        void depletedAccount() {
            InvestmentAccount account = createAccount("401k", AccountType.TRADITIONAL_401K, 0);

            WithdrawalResult result = calculator.calculateWithdrawal(account, new BigDecimal("5000"));

            assertEquals(0, BigDecimal.ZERO.compareTo(result.withdrawalAmount()));
            assertTrue(result.isDepleted());
            assertFalse(result.hasWithdrawal());
        }

        @Test
        @DisplayName("Should flag Roth withdrawal as non-taxable")
        void rothNotTaxable() {
            InvestmentAccount account = createAccount("Roth", AccountType.ROTH_IRA, 50000);

            WithdrawalResult result = calculator.calculateWithdrawal(account, new BigDecimal("5000"));

            assertFalse(result.isTaxable());
        }
    }

    @Nested
    @DisplayName("applyWithdrawal")
    class ApplyWithdrawalTests {

        @Test
        @DisplayName("Should return account with reduced balance")
        void applyWithdrawal() {
            InvestmentAccount account = createAccount("401k", AccountType.TRADITIONAL_401K, 100000);

            InvestmentAccount updated = calculator.applyWithdrawal(account, new BigDecimal("5000"));

            assertEquals(0, new BigDecimal("95000.00").compareTo(updated.getBalance()));
        }

        @Test
        @DisplayName("Should not go below zero")
        void notBelowZero() {
            InvestmentAccount account = createAccount("401k", AccountType.TRADITIONAL_401K, 3000);

            InvestmentAccount updated = calculator.applyWithdrawal(account, new BigDecimal("5000"));

            assertEquals(0, BigDecimal.ZERO.compareTo(updated.getBalance()));
        }
    }

    @Nested
    @DisplayName("Four Percent Rule")
    class FourPercentRuleTests {

        @Test
        @DisplayName("Should calculate monthly 4% rule amount")
        void monthlyFourPercent() {
            BigDecimal portfolio = new BigDecimal("1000000");

            BigDecimal result = calculator.calculateFourPercentRuleMonthly(portfolio);

            // 1000000 * 0.04 / 12 = 3333.33
            assertEquals(0, new BigDecimal("3333.33").compareTo(result));
        }

        @Test
        @DisplayName("Should calculate annual 4% with inflation")
        void annualWithInflation() {
            BigDecimal portfolio = new BigDecimal("1000000");
            BigDecimal inflation = new BigDecimal("0.03");

            BigDecimal year0 = calculator.calculateFourPercentRuleAnnual(portfolio, inflation, 0);
            BigDecimal year1 = calculator.calculateFourPercentRuleAnnual(portfolio, inflation, 1);

            assertEquals(0, new BigDecimal("40000.00").compareTo(year0));
            assertEquals(0, new BigDecimal("41200.00").compareTo(year1)); // 40000 * 1.03
        }
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should return calculator from factory")
        void factoryReturnsCalculator() {
            WithdrawalCalculator factoryCalculator = CalculatorFactory.withdrawalCalculator();
            BigDecimal result = factoryCalculator.calculateFourPercentRuleMonthly(new BigDecimal("100000"));
            assertEquals(0, new BigDecimal("333.33").compareTo(result));
        }
    }

    @Nested
    @DisplayName("WithdrawalResult")
    class WithdrawalResultTests {

        @Test
        @DisplayName("Should calculate shortfall correctly")
        void shortfall() {
            WithdrawalResult result = WithdrawalResult.partial(
                new BigDecimal("3000"),
                new BigDecimal("5000"),
                "acc-1",
                AccountType.TRADITIONAL_401K
            );

            assertEquals(0, new BigDecimal("2000").compareTo(result.getShortfall()));
        }

        @Test
        @DisplayName("Should return zero shortfall for full withdrawal")
        void noShortfall() {
            WithdrawalResult result = WithdrawalResult.full(
                new BigDecimal("5000"),
                "acc-1",
                AccountType.TRADITIONAL_401K,
                new BigDecimal("95000")
            );

            assertEquals(0, BigDecimal.ZERO.compareTo(result.getShortfall()));
        }
    }
}
