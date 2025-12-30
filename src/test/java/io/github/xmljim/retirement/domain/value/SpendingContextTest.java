package io.github.xmljim.retirement.domain.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.enums.FilingStatus;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.model.InvestmentAccount;
import io.github.xmljim.retirement.domain.model.PersonProfile;
import io.github.xmljim.retirement.domain.model.Portfolio;

@DisplayName("SpendingContext Tests")
class SpendingContextTest {

    private PersonProfile owner;
    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        owner = PersonProfile.builder()
                .name("John Doe")
                .dateOfBirth(LocalDate.of(1960, 5, 15))
                .retirementDate(LocalDate.of(2025, 1, 1))
                .build();

        InvestmentAccount account401k = InvestmentAccount.builder()
                .name("401(k)")
                .accountType(AccountType.TRADITIONAL_401K)
                .balance(new BigDecimal("500000"))
                .allocation(AssetAllocation.of(60, 35, 5))
                .preRetirementReturnRate(0.07)
                .build();

        InvestmentAccount rothIra = InvestmentAccount.builder()
                .name("Roth IRA")
                .accountType(AccountType.ROTH_IRA)
                .balance(new BigDecimal("200000"))
                .allocation(AssetAllocation.of(70, 25, 5))
                .preRetirementReturnRate(0.08)
                .build();

        portfolio = Portfolio.builder()
                .owner(owner)
                .addAccount(account401k)
                .addAccount(rothIra)
                .build();
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should create valid context with builder")
        void createsValidContext() {
            LocalDate retirementStart = LocalDate.of(2024, 1, 1);
            LocalDate currentDate = LocalDate.of(2026, 6, 1); // 29 months after retirement

            SpendingContext context = SpendingContext.builder()
                    .portfolio(portfolio)
                    .totalExpenses(new BigDecimal("6000.00"))
                    .otherIncome(new BigDecimal("3000.00"))
                    .date(currentDate)
                    .age(66)
                    .birthYear(1960)
                    .retirementStartDate(retirementStart)
                    .initialPortfolioBalance(new BigDecimal("750000.00"))
                    .filingStatus(FilingStatus.MARRIED_FILING_JOINTLY)
                    .build();

            assertEquals(portfolio, context.portfolio());
            assertEquals(0, new BigDecimal("6000.00").compareTo(context.totalExpenses()));
            assertEquals(0, new BigDecimal("3000.00").compareTo(context.otherIncome()));
            assertEquals(currentDate, context.date());
            assertEquals(66, context.age());
            assertEquals(1960, context.birthYear());
            assertEquals(retirementStart, context.retirementStartDate());
            assertEquals(29, context.monthsInRetirement());
            assertEquals(2, context.yearsInRetirement()); // 29 / 12 = 2
            assertEquals(FilingStatus.MARRIED_FILING_JOINTLY, context.filingStatus());
        }

        @Test
        @DisplayName("Should default amounts to zero")
        void defaultsAmountsToZero() {
            SpendingContext context = SpendingContext.builder()
                    .portfolio(portfolio)
                    .date(LocalDate.now())
                    .build();

            assertEquals(0, BigDecimal.ZERO.compareTo(context.totalExpenses()));
            assertEquals(0, BigDecimal.ZERO.compareTo(context.otherIncome()));
            assertEquals(0, BigDecimal.ZERO.compareTo(context.priorYearSpending()));
            assertEquals(0, BigDecimal.ZERO.compareTo(context.priorYearPortfolioReturn()));
            assertEquals(0, BigDecimal.ZERO.compareTo(context.currentTaxableIncome()));
        }

        @Test
        @DisplayName("Should default initial balance to portfolio balance")
        void defaultsInitialBalance() {
            SpendingContext context = SpendingContext.builder()
                    .portfolio(portfolio)
                    .date(LocalDate.now())
                    .build();

            assertEquals(0, portfolio.getTotalBalance().compareTo(context.initialPortfolioBalance()));
        }

        @Test
        @DisplayName("Should default retirementStartDate from portfolio owner")
        void defaultsRetirementStartDate() {
            SpendingContext context = SpendingContext.builder()
                    .portfolio(portfolio)
                    .date(LocalDate.of(2027, 1, 1))
                    .build();

            // Should default to owner's retirement date (2025-01-01)
            assertEquals(LocalDate.of(2025, 1, 1), context.retirementStartDate());
            // 24 months between 2025-01-01 and 2027-01-01
            assertEquals(24, context.monthsInRetirement());
            assertEquals(2, context.yearsInRetirement());
        }

        @Test
        @DisplayName("Should allow overriding retirementStartDate for scenario analysis")
        void allowsRetirementStartDateOverride() {
            // Portfolio owner's retirement date is 2025-01-01
            // But we want to model "what if I retired in 2023 instead?"
            LocalDate scenarioRetirement = LocalDate.of(2023, 1, 1);

            SpendingContext context = SpendingContext.builder()
                    .portfolio(portfolio)
                    .date(LocalDate.of(2027, 1, 1))
                    .retirementStartDate(scenarioRetirement)
                    .build();

            assertEquals(scenarioRetirement, context.retirementStartDate());
            // 48 months between 2023-01-01 and 2027-01-01
            assertEquals(48, context.monthsInRetirement());
            assertEquals(4, context.yearsInRetirement());
        }

        @Test
        @DisplayName("Should support strategy params")
        void supportsStrategyParams() {
            SpendingContext context = SpendingContext.builder()
                    .portfolio(portfolio)
                    .date(LocalDate.now())
                    .addStrategyParam("upperGuardrail", new BigDecimal("0.05"))
                    .addStrategyParam("lowerGuardrail", new BigDecimal("0.03"))
                    .build();

            assertEquals(
                    new BigDecimal("0.05"),
                    context.getStrategyParam("upperGuardrail", BigDecimal.ZERO)
            );
            assertEquals(
                    new BigDecimal("0.03"),
                    context.getStrategyParam("lowerGuardrail", BigDecimal.ZERO)
            );
        }

        @Test
        @DisplayName("Should return default for missing strategy param")
        void returnsDefaultForMissingParam() {
            SpendingContext context = SpendingContext.builder()
                    .portfolio(portfolio)
                    .date(LocalDate.now())
                    .build();

            assertEquals(
                    "default",
                    context.getStrategyParam("nonexistent", "default")
            );
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw for null portfolio")
        void nullPortfolioThrows() {
            assertThrows(MissingRequiredFieldException.class, () ->
                    SpendingContext.builder()
                            .date(LocalDate.now())
                            .build());
        }

        @Test
        @DisplayName("Should throw for null date")
        void nullDateThrows() {
            assertThrows(MissingRequiredFieldException.class, () ->
                    SpendingContext.builder()
                            .portfolio(portfolio)
                            .build());
        }
    }

    @Nested
    @DisplayName("Income Gap Tests")
    class IncomeGapTests {

        @Test
        @DisplayName("Should calculate positive income gap")
        void calculatesPositiveGap() {
            SpendingContext context = SpendingContext.builder()
                    .portfolio(portfolio)
                    .totalExpenses(new BigDecimal("6000.00"))
                    .otherIncome(new BigDecimal("4000.00"))
                    .date(LocalDate.now())
                    .build();

            // Gap = 6000 - 4000 = 2000
            assertEquals(0, new BigDecimal("2000.00").compareTo(context.incomeGap()));
        }

        @Test
        @DisplayName("Should return zero when income exceeds expenses")
        void returnsZeroWhenIncomeExceeds() {
            SpendingContext context = SpendingContext.builder()
                    .portfolio(portfolio)
                    .totalExpenses(new BigDecimal("4000.00"))
                    .otherIncome(new BigDecimal("6000.00"))
                    .date(LocalDate.now())
                    .build();

            // Gap should be zero, not negative
            assertEquals(0, BigDecimal.ZERO.compareTo(context.incomeGap()));
        }
    }

    @Nested
    @DisplayName("Withdrawal Rate Tests")
    class WithdrawalRateTests {

        @Test
        @DisplayName("Should calculate current withdrawal rate")
        void calculatesWithdrawalRate() {
            SpendingContext context = SpendingContext.builder()
                    .portfolio(portfolio)
                    .date(LocalDate.now())
                    .priorYearSpending(new BigDecimal("28000.00"))
                    .build();

            // Rate = 28000 / 700000 = 0.04 (4%)
            BigDecimal rate = context.currentWithdrawalRate();
            assertEquals(0, new BigDecimal("0.0400").compareTo(rate));
        }

        @Test
        @DisplayName("Should return zero rate for zero balance")
        void returnsZeroRateForZeroBalance() {
            Portfolio emptyPortfolio = Portfolio.builder()
                    .owner(owner)
                    .build();

            SpendingContext context = SpendingContext.builder()
                    .portfolio(emptyPortfolio)
                    .date(LocalDate.now())
                    .priorYearSpending(new BigDecimal("28000.00"))
                    .build();

            assertEquals(0, BigDecimal.ZERO.compareTo(context.currentWithdrawalRate()));
        }
    }

    @Nested
    @DisplayName("Immutability Tests")
    class ImmutabilityTests {

        @Test
        @DisplayName("Strategy params map should be unmodifiable")
        void strategyParamsUnmodifiable() {
            SpendingContext context = SpendingContext.builder()
                    .portfolio(portfolio)
                    .date(LocalDate.now())
                    .strategyParams(Map.of("key", "value"))
                    .build();

            var params = context.strategyParams();
            assertThrows(UnsupportedOperationException.class, () ->
                    params.put("newKey", "newValue")
            );
        }
    }
}
