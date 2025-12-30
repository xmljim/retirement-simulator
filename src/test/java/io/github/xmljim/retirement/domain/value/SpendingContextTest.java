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

import io.github.xmljim.retirement.domain.calculator.StubSimulationView;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.enums.FilingStatus;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;

@DisplayName("SpendingContext Tests")
class SpendingContextTest {

    private StubSimulationView simulation;
    private LocalDate retirementStart;

    @BeforeEach
    void setUp() {
        retirementStart = LocalDate.of(2024, 1, 1);
        simulation = StubSimulationView.builder()
                .addAccount(StubSimulationView.createTestAccount(
                        "401(k)", AccountType.TRADITIONAL_401K, new BigDecimal("500000")))
                .addAccount(StubSimulationView.createTestAccount(
                        "Roth IRA", AccountType.ROTH_IRA, new BigDecimal("200000")))
                .initialPortfolioBalance(new BigDecimal("750000"))
                .build();
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should create valid context with builder")
        void createsValidContext() {
            LocalDate currentDate = LocalDate.of(2026, 6, 1); // 29 months after retirement

            SpendingContext context = SpendingContext.builder()
                    .simulation(simulation)
                    .totalExpenses(new BigDecimal("6000.00"))
                    .otherIncome(new BigDecimal("3000.00"))
                    .date(currentDate)
                    .age(66)
                    .birthYear(1960)
                    .retirementStartDate(retirementStart)
                    .filingStatus(FilingStatus.MARRIED_FILING_JOINTLY)
                    .build();

            assertEquals(simulation, context.simulation());
            assertEquals(0, new BigDecimal("6000.00").compareTo(context.totalExpenses()));
            assertEquals(0, new BigDecimal("3000.00").compareTo(context.otherIncome()));
            assertEquals(currentDate, context.date());
            assertEquals(66, context.age());
            assertEquals(1960, context.birthYear());
            assertEquals(retirementStart, context.retirementStartDate());
            assertEquals(29, context.monthsInRetirement());
            assertEquals(2, context.yearsInRetirement());
            assertEquals(FilingStatus.MARRIED_FILING_JOINTLY, context.filingStatus());
        }

        @Test
        @DisplayName("Should default amounts to zero")
        void defaultsAmountsToZero() {
            SpendingContext context = SpendingContext.builder()
                    .simulation(simulation)
                    .date(LocalDate.now())
                    .retirementStartDate(retirementStart)
                    .build();

            assertEquals(0, BigDecimal.ZERO.compareTo(context.totalExpenses()));
            assertEquals(0, BigDecimal.ZERO.compareTo(context.otherIncome()));
            assertEquals(0, BigDecimal.ZERO.compareTo(context.currentTaxableIncome()));
        }

        @Test
        @DisplayName("Should support strategy params")
        void supportsStrategyParams() {
            SpendingContext context = SpendingContext.builder()
                    .simulation(simulation)
                    .date(LocalDate.now())
                    .retirementStartDate(retirementStart)
                    .addStrategyParam("upperGuardrail", new BigDecimal("0.05"))
                    .addStrategyParam("lowerGuardrail", new BigDecimal("0.03"))
                    .build();

            assertEquals(new BigDecimal("0.05"),
                    context.getStrategyParam("upperGuardrail", BigDecimal.ZERO));
            assertEquals(new BigDecimal("0.03"),
                    context.getStrategyParam("lowerGuardrail", BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Should return default for missing strategy param")
        void returnsDefaultForMissingParam() {
            SpendingContext context = SpendingContext.builder()
                    .simulation(simulation)
                    .date(LocalDate.now())
                    .retirementStartDate(retirementStart)
                    .build();

            assertEquals("default", context.getStrategyParam("nonexistent", "default"));
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw for null simulation")
        void nullSimulationThrows() {
            assertThrows(MissingRequiredFieldException.class, () ->
                    SpendingContext.builder()
                            .date(LocalDate.now())
                            .retirementStartDate(retirementStart)
                            .build());
        }

        @Test
        @DisplayName("Should throw for null date")
        void nullDateThrows() {
            assertThrows(MissingRequiredFieldException.class, () ->
                    SpendingContext.builder()
                            .simulation(simulation)
                            .retirementStartDate(retirementStart)
                            .build());
        }

        @Test
        @DisplayName("Should throw for null retirementStartDate")
        void nullRetirementStartDateThrows() {
            assertThrows(MissingRequiredFieldException.class, () ->
                    SpendingContext.builder()
                            .simulation(simulation)
                            .date(LocalDate.now())
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
                    .simulation(simulation)
                    .totalExpenses(new BigDecimal("6000.00"))
                    .otherIncome(new BigDecimal("4000.00"))
                    .date(LocalDate.now())
                    .retirementStartDate(retirementStart)
                    .build();

            assertEquals(0, new BigDecimal("2000.00").compareTo(context.incomeGap()));
        }

        @Test
        @DisplayName("Should return zero when income exceeds expenses")
        void returnsZeroWhenIncomeExceeds() {
            SpendingContext context = SpendingContext.builder()
                    .simulation(simulation)
                    .totalExpenses(new BigDecimal("4000.00"))
                    .otherIncome(new BigDecimal("6000.00"))
                    .date(LocalDate.now())
                    .retirementStartDate(retirementStart)
                    .build();

            assertEquals(0, BigDecimal.ZERO.compareTo(context.incomeGap()));
        }
    }

    @Nested
    @DisplayName("Portfolio Balance Tests")
    class PortfolioBalanceTests {

        @Test
        @DisplayName("Should delegate current balance to simulation")
        void delegatesCurrentBalance() {
            SpendingContext context = SpendingContext.builder()
                    .simulation(simulation)
                    .date(LocalDate.now())
                    .retirementStartDate(retirementStart)
                    .build();

            assertEquals(0, new BigDecimal("700000").compareTo(context.currentPortfolioBalance()));
        }

        @Test
        @DisplayName("Should delegate initial balance to simulation")
        void delegatesInitialBalance() {
            SpendingContext context = SpendingContext.builder()
                    .simulation(simulation)
                    .date(LocalDate.now())
                    .retirementStartDate(retirementStart)
                    .build();

            assertEquals(0, new BigDecimal("750000").compareTo(context.initialPortfolioBalance()));
        }
    }

    @Nested
    @DisplayName("Withdrawal Rate Tests")
    class WithdrawalRateTests {

        @Test
        @DisplayName("Should calculate current withdrawal rate from simulation")
        void calculatesWithdrawalRate() {
            StubSimulationView simWithHistory = StubSimulationView.builder()
                    .addAccount(StubSimulationView.createTestAccount(
                            "401(k)", AccountType.TRADITIONAL_401K, new BigDecimal("700000")))
                    .priorYearSpending(new BigDecimal("28000"))
                    .build();

            SpendingContext context = SpendingContext.builder()
                    .simulation(simWithHistory)
                    .date(LocalDate.now())
                    .retirementStartDate(retirementStart)
                    .build();

            // Rate = 28000 / 700000 = 0.04 (4%)
            assertEquals(0, new BigDecimal("0.0400").compareTo(context.currentWithdrawalRate()));
        }

        @Test
        @DisplayName("Should return zero rate for zero balance")
        void returnsZeroRateForZeroBalance() {
            StubSimulationView emptySim = StubSimulationView.builder()
                    .priorYearSpending(new BigDecimal("28000"))
                    .build();

            SpendingContext context = SpendingContext.builder()
                    .simulation(emptySim)
                    .date(LocalDate.now())
                    .retirementStartDate(retirementStart)
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
                    .simulation(simulation)
                    .date(LocalDate.now())
                    .retirementStartDate(retirementStart)
                    .strategyParams(Map.of("key", "value"))
                    .build();

            var params = context.strategyParams();
            assertThrows(UnsupportedOperationException.class, () ->
                    params.put("newKey", "newValue"));
        }
    }
}
