package io.github.xmljim.retirement.domain.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.calculator.impl.DefaultReturnCalculator;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.exception.CalculationException;
import io.github.xmljim.retirement.domain.exception.InvalidDateRangeException;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.model.InvestmentAccount;
import io.github.xmljim.retirement.domain.value.AssetAllocation;

@DisplayName("ReturnCalculator Tests")
class ReturnCalculatorTest {

    private ReturnCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new DefaultReturnCalculator();
    }

    @Nested
    @DisplayName("calculateBlendedReturn")
    class BlendedReturnTests {

        @Test
        @DisplayName("Should calculate blended return for balanced allocation")
        void blendedReturnBalanced() {
            AssetAllocation allocation = AssetAllocation.balanced(); // 60/40/0
            BigDecimal stockReturn = new BigDecimal("0.07");
            BigDecimal bondReturn = new BigDecimal("0.04");
            BigDecimal cashReturn = new BigDecimal("0.02");

            BigDecimal result = calculator.calculateBlendedReturn(
                allocation, stockReturn, bondReturn, cashReturn);

            // Expected: (0.60 * 0.07) + (0.40 * 0.04) + (0 * 0.02) = 0.042 + 0.016 = 0.058
            assertEquals(0, new BigDecimal("0.058").compareTo(
                result.setScale(3, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should calculate blended return for all stocks")
        void blendedReturnAllStocks() {
            AssetAllocation allocation = AssetAllocation.allStocks();
            BigDecimal stockReturn = new BigDecimal("0.10");
            BigDecimal bondReturn = new BigDecimal("0.05");
            BigDecimal cashReturn = new BigDecimal("0.02");

            BigDecimal result = calculator.calculateBlendedReturn(
                allocation, stockReturn, bondReturn, cashReturn);

            // Expected: 1.0 * 0.10 = 0.10
            assertEquals(0, new BigDecimal("0.10").compareTo(
                result.setScale(2, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should calculate blended return for all bonds")
        void blendedReturnAllBonds() {
            AssetAllocation allocation = AssetAllocation.allBonds();
            BigDecimal stockReturn = new BigDecimal("0.10");
            BigDecimal bondReturn = new BigDecimal("0.05");
            BigDecimal cashReturn = new BigDecimal("0.02");

            BigDecimal result = calculator.calculateBlendedReturn(
                allocation, stockReturn, bondReturn, cashReturn);

            // Expected: 1.0 * 0.05 = 0.05
            assertEquals(0, new BigDecimal("0.05").compareTo(
                result.setScale(2, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should calculate blended return for custom allocation")
        void blendedReturnCustom() {
            AssetAllocation allocation = AssetAllocation.of(70, 20, 10);
            BigDecimal stockReturn = new BigDecimal("0.07");
            BigDecimal bondReturn = new BigDecimal("0.04");
            BigDecimal cashReturn = new BigDecimal("0.02");

            BigDecimal result = calculator.calculateBlendedReturn(
                allocation, stockReturn, bondReturn, cashReturn);

            // Expected: (0.70 * 0.07) + (0.20 * 0.04) + (0.10 * 0.02) = 0.049 + 0.008 + 0.002 = 0.059
            assertEquals(0, new BigDecimal("0.059").compareTo(
                result.setScale(3, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should throw for null allocation")
        void nullAllocation() {
            assertThrows(IllegalArgumentException.class, () ->
                calculator.calculateBlendedReturn(null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Should handle null return rates as zero")
        void nullReturnRates() {
            AssetAllocation allocation = AssetAllocation.balanced();

            BigDecimal result = calculator.calculateBlendedReturn(
                allocation, null, null, null);

            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }
    }

    @Nested
    @DisplayName("calculateAccountGrowth")
    class AccountGrowthTests {

        @Test
        @DisplayName("Should calculate account growth over 12 months")
        void accountGrowth12Months() {
            BigDecimal balance = new BigDecimal("100000");
            BigDecimal annualRate = new BigDecimal("0.06"); // 6% annual

            BigDecimal result = calculator.calculateAccountGrowth(balance, annualRate, 12);

            // With monthly compounding at ~0.5% per month
            // Result should be greater than 100000 but less than 106000
            assertTrue(result.compareTo(balance) > 0);
            assertTrue(result.compareTo(new BigDecimal("107000")) < 0);
        }

        @Test
        @DisplayName("Should return same balance for zero months")
        void accountGrowthZeroMonths() {
            BigDecimal balance = new BigDecimal("100000");
            BigDecimal annualRate = new BigDecimal("0.06");

            BigDecimal result = calculator.calculateAccountGrowth(balance, annualRate, 0);

            assertEquals(0, balance.compareTo(result));
        }

        @Test
        @DisplayName("Should throw MissingRequiredFieldException for null balance")
        void nullBalance() {
            assertThrows(MissingRequiredFieldException.class, () ->
                calculator.calculateAccountGrowth(null, new BigDecimal("0.06"), 12));
        }

        @Test
        @DisplayName("Should throw CalculationException for negative months")
        void negativeMonths() {
            BigDecimal balance = new BigDecimal("100000");

            assertThrows(CalculationException.class, () ->
                calculator.calculateAccountGrowth(balance, new BigDecimal("0.06"), -1));
        }
    }

    @Nested
    @DisplayName("toMonthlyRate")
    class ToMonthlyRateTests {

        @Test
        @DisplayName("Should convert annual rate to monthly rate using true annual compounding")
        void annualToMonthlyTrueCompounding() {
            BigDecimal annualRate = new BigDecimal("0.10"); // 10% annual

            BigDecimal result = calculator.toMonthlyRate(annualRate);

            // True compounding: (1.10)^(1/12) - 1 = 0.00797 (approximately 0.797%)
            // This is different from simple division (0.10/12 = 0.00833)
            BigDecimal expected = new BigDecimal("0.00797");
            assertEquals(0, expected.compareTo(
                result.setScale(5, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should return zero for null rate")
        void nullRate() {
            BigDecimal result = calculator.toMonthlyRate(null);

            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        @DisplayName("Should return zero for zero rate")
        void zeroRate() {
            BigDecimal result = calculator.toMonthlyRate(BigDecimal.ZERO);

            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }
    }

    @Nested
    @DisplayName("True Annual Compounding Tests")
    class TrueAnnualCompoundingTests {

        @Test
        @DisplayName("Should calculate true annual compounding: $100 at 10% for 2 months")
        void trueAnnualCompounding2Months() {
            BigDecimal balance = new BigDecimal("100");
            BigDecimal annualRate = new BigDecimal("0.10"); // 10%

            BigDecimal result = calculator.calculateAccountGrowth(balance, annualRate, 2);

            // Formula: 100 * (1.10)^(2/12) = 100 * 1.0160 = 101.60
            BigDecimal expected = new BigDecimal("101.60");
            assertEquals(0, expected.compareTo(result.setScale(2, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should calculate true annual compounding: $100 at 10% for 12 months")
        void trueAnnualCompounding12Months() {
            BigDecimal balance = new BigDecimal("100");
            BigDecimal annualRate = new BigDecimal("0.10"); // 10%

            BigDecimal result = calculator.calculateAccountGrowth(balance, annualRate, 12);

            // Formula: 100 * (1.10)^(12/12) = 100 * 1.10 = 110.00
            BigDecimal expected = new BigDecimal("110.00");
            assertEquals(0, expected.compareTo(result.setScale(2, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should calculate true annual compounding: $100 at 10% for 6 months")
        void trueAnnualCompounding6Months() {
            BigDecimal balance = new BigDecimal("100");
            BigDecimal annualRate = new BigDecimal("0.10"); // 10%

            BigDecimal result = calculator.calculateAccountGrowth(balance, annualRate, 6);

            // Formula: 100 * (1.10)^(6/12) = 100 * (1.10)^0.5 = 100 * 1.0488 = 104.88
            BigDecimal expected = new BigDecimal("104.88");
            assertEquals(0, expected.compareTo(result.setScale(2, RoundingMode.HALF_UP)));
        }
    }

    @Nested
    @DisplayName("calculateReturn (YearMonth)")
    class CalculateReturnYearMonthTests {

        @Test
        @DisplayName("Should calculate return between two year-months")
        void calculateReturnBetweenMonths() {
            BigDecimal balance = new BigDecimal("100");
            BigDecimal annualRate = new BigDecimal("0.10");
            YearMonth start = YearMonth.of(2025, 1);
            YearMonth end = YearMonth.of(2025, 2); // 2 months inclusive

            BigDecimal result = calculator.calculateReturn(balance, annualRate, start, end);

            // 2 months inclusive: 100 * (1.10)^(2/12)
            BigDecimal expected = new BigDecimal("101.60");
            assertEquals(0, expected.compareTo(result.setScale(2, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should calculate return for same month (1 month)")
        void calculateReturnSameMonth() {
            BigDecimal balance = new BigDecimal("100");
            BigDecimal annualRate = new BigDecimal("0.10");
            YearMonth month = YearMonth.of(2025, 1);

            BigDecimal result = calculator.calculateReturn(balance, annualRate, month, month);

            // 1 month: 100 * (1.10)^(1/12)
            BigDecimal expected = new BigDecimal("100.80");
            assertEquals(0, expected.compareTo(result.setScale(2, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should throw InvalidDateRangeException when end before start")
        void endBeforeStart() {
            BigDecimal balance = new BigDecimal("100");
            BigDecimal annualRate = new BigDecimal("0.10");
            YearMonth start = YearMonth.of(2025, 6);
            YearMonth end = YearMonth.of(2025, 1);

            assertThrows(InvalidDateRangeException.class, () ->
                calculator.calculateReturn(balance, annualRate, start, end));
        }

        @Test
        @DisplayName("Should throw MissingRequiredFieldException for null parameters")
        void nullParameters() {
            BigDecimal balance = new BigDecimal("100");
            BigDecimal rate = new BigDecimal("0.10");
            YearMonth month = YearMonth.of(2025, 1);

            assertThrows(MissingRequiredFieldException.class, () ->
                calculator.calculateReturn(null, rate, month, month));
            assertThrows(MissingRequiredFieldException.class, () ->
                calculator.calculateReturn(balance, rate, null, month));
            assertThrows(MissingRequiredFieldException.class, () ->
                calculator.calculateReturn(balance, rate, month, null));
        }
    }

    @Nested
    @DisplayName("calculateMonthlyReturn (balance, rate)")
    class CalculateMonthlyReturnTests {

        @Test
        @DisplayName("Should calculate monthly return amount")
        void calculateMonthlyReturn() {
            BigDecimal balance = new BigDecimal("100");
            BigDecimal annualRate = new BigDecimal("0.10");

            BigDecimal result = calculator.calculateMonthlyReturn(balance, annualRate);

            // Monthly return = 100 * ((1.10)^(1/12) - 1) = 100 * 0.00797 = 0.797
            BigDecimal expected = new BigDecimal("0.80");
            assertEquals(0, expected.compareTo(result.setScale(2, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should return zero for zero balance")
        void zeroBalance() {
            BigDecimal result = calculator.calculateMonthlyReturn(BigDecimal.ZERO, new BigDecimal("0.10"));

            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        @DisplayName("Should return zero for zero rate")
        void zeroRate() {
            BigDecimal result = calculator.calculateMonthlyReturn(new BigDecimal("100"), BigDecimal.ZERO);

            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        @DisplayName("Should throw CalculationException for negative balance")
        void negativeBalance() {
            assertThrows(CalculationException.class, () ->
                calculator.calculateMonthlyReturn(new BigDecimal("-100"), new BigDecimal("0.10")));
        }
    }

    @Nested
    @DisplayName("calculateMonthlyReturn (InvestmentAccount)")
    class CalculateMonthlyReturnAccountTests {

        @Test
        @DisplayName("Should use pre-retirement rate when not retired")
        void preRetirementRate() {
            InvestmentAccount account = InvestmentAccount.builder()
                .name("401k")
                .accountType(AccountType.TRADITIONAL_401K)
                .balance(new BigDecimal("100000"))
                .allocation(AssetAllocation.balanced())
                .preRetirementReturnRate(new BigDecimal("0.08"))
                .postRetirementReturnRate(new BigDecimal("0.05"))
                .build();

            BigDecimal result = calculator.calculateMonthlyReturn(account, false);

            // Monthly return at 8%: 100000 * ((1.08)^(1/12) - 1) = ~$643
            assertTrue(result.compareTo(BigDecimal.ZERO) > 0);
            assertTrue(result.compareTo(new BigDecimal("600")) > 0);
            assertTrue(result.compareTo(new BigDecimal("700")) < 0);
        }

        @Test
        @DisplayName("Should use post-retirement rate when retired")
        void postRetirementRate() {
            InvestmentAccount account = InvestmentAccount.builder()
                .name("401k")
                .accountType(AccountType.TRADITIONAL_401K)
                .balance(new BigDecimal("100000"))
                .allocation(AssetAllocation.balanced())
                .preRetirementReturnRate(new BigDecimal("0.08"))
                .postRetirementReturnRate(new BigDecimal("0.05"))
                .build();

            BigDecimal preRetirement = calculator.calculateMonthlyReturn(account, false);
            BigDecimal postRetirement = calculator.calculateMonthlyReturn(account, true);

            // Post-retirement return should be lower (5% vs 8%)
            assertTrue(postRetirement.compareTo(preRetirement) < 0);
        }

        @Test
        @DisplayName("Should throw MissingRequiredFieldException for null account")
        void nullAccount() {
            assertThrows(MissingRequiredFieldException.class, () ->
                calculator.calculateMonthlyReturn((InvestmentAccount) null, false));
        }
    }

    @Nested
    @DisplayName("applyMonthlyReturn")
    class ApplyMonthlyReturnTests {

        @Test
        @DisplayName("Should return new account with updated balance")
        void applyMonthlyReturnUpdatesBalance() {
            InvestmentAccount account = InvestmentAccount.builder()
                .name("401k")
                .accountType(AccountType.TRADITIONAL_401K)
                .balance(new BigDecimal("100000"))
                .allocation(AssetAllocation.balanced())
                .preRetirementReturnRate(new BigDecimal("0.10"))
                .build();

            InvestmentAccount updated = calculator.applyMonthlyReturn(account, false);

            // Should be a new instance
            assertNotSame(account, updated);

            // Balance should have increased
            assertTrue(updated.getBalance().compareTo(account.getBalance()) > 0);

            // Original should be unchanged (immutability)
            assertEquals(0, new BigDecimal("100000").compareTo(
                account.getBalance().setScale(0, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should apply correct return amount")
        void applyCorrectReturnAmount() {
            BigDecimal initialBalance = new BigDecimal("100");
            InvestmentAccount account = InvestmentAccount.builder()
                .name("Test")
                .accountType(AccountType.TAXABLE_BROKERAGE)
                .balance(initialBalance)
                .allocation(AssetAllocation.balanced())
                .preRetirementReturnRate(new BigDecimal("0.10"))
                .build();

            InvestmentAccount updated = calculator.applyMonthlyReturn(account, false);

            // New balance = 100 + (100 * ((1.10)^(1/12) - 1)) = 100.80
            BigDecimal expected = new BigDecimal("100.80");
            assertEquals(0, expected.compareTo(updated.getBalance().setScale(2, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should throw MissingRequiredFieldException for null account")
        void nullAccount() {
            assertThrows(MissingRequiredFieldException.class, () ->
                calculator.applyMonthlyReturn(null, false));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should throw CalculationException for negative balance in calculateAccountGrowth")
        void negativeBalanceAccountGrowth() {
            CalculationException ex = assertThrows(CalculationException.class, () ->
                calculator.calculateAccountGrowth(new BigDecimal("-100"), new BigDecimal("0.10"), 12));

            assertTrue(ex.getMessage().contains("negative"));
        }

        @Test
        @DisplayName("Should handle null annual rate as zero return")
        void nullAnnualRate() {
            BigDecimal balance = new BigDecimal("100");

            BigDecimal result = calculator.calculateAccountGrowth(balance, null, 12);

            // Should return original balance (no growth)
            assertEquals(0, balance.compareTo(result));
        }

        @Test
        @DisplayName("Should handle large balance amounts")
        void largeBalance() {
            BigDecimal balance = new BigDecimal("10000000000"); // 10 billion
            BigDecimal annualRate = new BigDecimal("0.07");

            BigDecimal result = calculator.calculateAccountGrowth(balance, annualRate, 12);

            // Should complete without overflow
            assertTrue(result.compareTo(balance) > 0);
        }

        @Test
        @DisplayName("Should return original balance for 12 months with zero rate")
        void zeroRateForFullYear() {
            BigDecimal balance = new BigDecimal("100");

            // This exercises the exponent == 1.0 case in MathUtils (12/12 = 1.0)
            BigDecimal result = calculator.calculateAccountGrowth(balance, BigDecimal.ZERO, 12);

            assertEquals(0, balance.compareTo(result));
        }

        @Test
        @DisplayName("Should handle zero months (exponent 0.0 case)")
        void zeroMonthsExponent() {
            BigDecimal balance = new BigDecimal("100");
            BigDecimal annualRate = new BigDecimal("0.10");

            // This exercises the months == 0 case which returns early
            BigDecimal result = calculator.calculateAccountGrowth(balance, annualRate, 0);

            assertEquals(0, balance.compareTo(result));
        }

        @Test
        @DisplayName("Should calculate correctly for exactly 12 months (exponent 1.0)")
        void exactlyTwelveMonths() {
            BigDecimal balance = new BigDecimal("100");
            BigDecimal annualRate = new BigDecimal("0.10");

            // 12 months = exponent of 1.0, should give exactly 10% growth
            BigDecimal result = calculator.calculateAccountGrowth(balance, annualRate, 12);

            // 100 * (1.10)^1 = 110
            assertEquals(0, new BigDecimal("110.00").compareTo(result.setScale(2, RoundingMode.HALF_UP)));
        }
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should return calculator from factory")
        void factoryReturnsCalculator() {
            ReturnCalculator factoryCalculator = CalculatorFactory.returnCalculator();
            AssetAllocation allocation = AssetAllocation.balanced();

            BigDecimal result = factoryCalculator.calculateBlendedReturn(
                allocation,
                new BigDecimal("0.07"),
                new BigDecimal("0.04"),
                new BigDecimal("0.02"));

            assertTrue(result.compareTo(BigDecimal.ZERO) > 0);
        }
    }
}
