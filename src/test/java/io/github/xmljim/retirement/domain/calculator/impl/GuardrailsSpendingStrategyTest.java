package io.github.xmljim.retirement.domain.calculator.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.calculator.GuardrailsConfiguration;
import io.github.xmljim.retirement.domain.calculator.StubSimulationView;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.value.SpendingContext;
import io.github.xmljim.retirement.domain.value.SpendingPlan;

@DisplayName("GuardrailsSpendingStrategy Tests")
class GuardrailsSpendingStrategyTest {

    private static final BigDecimal MILLION = new BigDecimal("1000000");

    @Nested
    @DisplayName("First Year Tests")
    class FirstYearTests {

        @Test
        @DisplayName("First year uses initial withdrawal rate")
        void firstYearUsesInitialRate() {
            GuardrailsSpendingStrategy strategy = new GuardrailsSpendingStrategy(
                    GuardrailsConfiguration.guytonKlinger());
            SpendingContext context = createContext(MILLION, BigDecimal.ZERO, new BigDecimal("0.08"));

            SpendingPlan plan = strategy.calculateWithdrawal(context);

            assertEquals("true", plan.metadata().get("firstYear"));
            assertTrue(plan.meetsTarget());
        }
    }

    @Nested
    @DisplayName("Guyton-Klinger Tests")
    class GuytonKlingerTests {

        @Test
        @DisplayName("Normal year applies inflation")
        void normalYearAppliesInflation() {
            GuardrailsSpendingStrategy strategy = new GuardrailsSpendingStrategy(
                    GuardrailsConfiguration.guytonKlinger());
            SpendingContext context = createContext(MILLION, new BigDecimal("52000"), new BigDecimal("0.08"));

            SpendingPlan plan = strategy.calculateWithdrawal(context);

            assertTrue(plan.targetWithdrawal().compareTo(BigDecimal.ZERO) > 0);
            assertEquals("Guardrails", plan.strategyUsed());
        }

        @Test
        @DisplayName("Down year with high rate skips inflation")
        void downYearHighRateSkipsInflation() {
            GuardrailsSpendingStrategy strategy = new GuardrailsSpendingStrategy(
                    GuardrailsConfiguration.guytonKlinger());
            // Prior spending $65k on $1M = 6.5% rate > 5.2% initial
            SpendingContext context = createContext(MILLION, new BigDecimal("65000"), new BigDecimal("-0.10"));

            SpendingPlan plan = strategy.calculateWithdrawal(context);

            assertNotNull(plan);
            assertTrue(plan.meetsTarget());
        }

        @Test
        @DisplayName("Prosperity rule triggers increase")
        void prosperityRuleTriggersIncrease() {
            GuardrailsSpendingStrategy strategy = new GuardrailsSpendingStrategy(
                    GuardrailsConfiguration.guytonKlinger());
            // $40k spending on $1M = 4% rate < 4.16% (80% of 5.2%)
            SpendingContext context = createContext(MILLION, new BigDecimal("40000"), new BigDecimal("0.30"));

            SpendingPlan plan = strategy.calculateWithdrawal(context);

            assertEquals("increase", plan.metadata().get("adjustment"));
        }

        @Test
        @DisplayName("Capital preservation rule triggers decrease")
        void capitalPreservationTriggersDecrease() {
            GuardrailsSpendingStrategy strategy = new GuardrailsSpendingStrategy(
                    GuardrailsConfiguration.guytonKlinger());
            // $70k spending on $1M = 7% rate > 6.24% (120% of 5.2%)
            SpendingContext context = createContext(MILLION, new BigDecimal("70000"), new BigDecimal("-0.20"));

            SpendingPlan plan = strategy.calculateWithdrawal(context);

            assertEquals("decrease", plan.metadata().get("adjustment"));
        }
    }

    @Nested
    @DisplayName("Kitces Ratcheting Tests")
    class KitcesRatchetingTests {

        @Test
        @DisplayName("No ratchet when portfolio below trigger")
        void noRatchetBelowTrigger() {
            GuardrailsSpendingStrategy strategy = new GuardrailsSpendingStrategy(
                    GuardrailsConfiguration.kitcesRatcheting());
            // Current $1.4M vs initial $1M - not at 150% yet
            SpendingContext context = createContextWithInitial(
                    new BigDecimal("1400000"), MILLION, new BigDecimal("40000"), new BigDecimal("0.10"));

            SpendingPlan plan = strategy.calculateWithdrawal(context);

            assertNull(plan.metadata().get("adjustment"));
        }

        @Test
        @DisplayName("Ratchet triggers when rate drops below threshold")
        void ratchetTriggersAtThreshold() {
            GuardrailsSpendingStrategy strategy = new GuardrailsSpendingStrategy(
                    GuardrailsConfiguration.kitcesRatcheting());
            // Current $2M vs initial $1M
            // Rate = $40k / $2M = 2% < 66.7% of 4% = 2.67%
            SpendingContext context = createContextWithInitial(
                    new BigDecimal("2000000"), MILLION, new BigDecimal("40000"), new BigDecimal("0.20"));

            SpendingPlan plan = strategy.calculateWithdrawal(context);

            assertEquals("increase", plan.metadata().get("adjustment"));
        }

        @Test
        @DisplayName("No ratchet when too soon since last")
        void noRatchetWhenTooSoon() {
            GuardrailsSpendingStrategy strategy = new GuardrailsSpendingStrategy(
                    GuardrailsConfiguration.kitcesRatcheting());

            StubSimulationView sim = StubSimulationView.builder()
                    .addAccount(StubSimulationView.createTestAccount(
                            "401k", AccountType.TRADITIONAL_401K, new BigDecimal("1500000")))
                    .initialPortfolioBalance(MILLION)
                    .priorYearSpending(new BigDecimal("40000"))
                    .priorYearReturn(new BigDecimal("0.15"))
                    .lastRatchetMonth(YearMonth.of(2024, 1)) // Too recent
                    .build();

            SpendingContext context = SpendingContext.builder()
                    .simulation(sim)
                    .date(LocalDate.of(2025, 6, 1))
                    .retirementStartDate(LocalDate.of(2020, 1, 1))
                    .totalExpenses(new BigDecimal("5000"))
                    .otherIncome(BigDecimal.ZERO)
                    .build();

            SpendingPlan plan = strategy.calculateWithdrawal(context);

            assertNull(plan.metadata().get("adjustment"));
        }
    }

    @Nested
    @DisplayName("Vanguard Dynamic Tests")
    class VanguardDynamicTests {

        @Test
        @DisplayName("Vanguard strategy is dynamic")
        void vanguardIsDynamic() {
            GuardrailsSpendingStrategy strategy = new GuardrailsSpendingStrategy(
                    GuardrailsConfiguration.vanguardDynamic());
            assertTrue(strategy.isDynamic());
            assertTrue(strategy.requiresPriorYearState());
        }
    }

    @Nested
    @DisplayName("Interface Contract Tests")
    class InterfaceTests {

        @Test
        @DisplayName("Strategy returns correct name")
        void correctName() {
            GuardrailsSpendingStrategy strategy = new GuardrailsSpendingStrategy(
                    GuardrailsConfiguration.guytonKlinger());
            assertEquals("Guardrails", strategy.getName());
        }

        @Test
        @DisplayName("Strategy is dynamic")
        void isDynamic() {
            GuardrailsSpendingStrategy strategy = new GuardrailsSpendingStrategy(
                    GuardrailsConfiguration.guytonKlinger());
            assertTrue(strategy.isDynamic());
        }

        @Test
        @DisplayName("Strategy requires prior year state")
        void requiresPriorYearState() {
            GuardrailsSpendingStrategy strategy = new GuardrailsSpendingStrategy(
                    GuardrailsConfiguration.guytonKlinger());
            assertTrue(strategy.requiresPriorYearState());
        }
    }

    // Helper methods

    private SpendingContext createContext(BigDecimal balance, BigDecimal priorSpending, BigDecimal priorReturn) {
        return createContextWithInitial(balance, balance, priorSpending, priorReturn);
    }

    private SpendingContext createContextWithInitial(BigDecimal balance, BigDecimal initial,
            BigDecimal priorSpending, BigDecimal priorReturn) {
        StubSimulationView sim = StubSimulationView.builder()
                .addAccount(StubSimulationView.createTestAccount(
                        "401k", AccountType.TRADITIONAL_401K, balance))
                .initialPortfolioBalance(initial)
                .priorYearSpending(priorSpending)
                .priorYearReturn(priorReturn)
                .build();

        return SpendingContext.builder()
                .simulation(sim)
                .date(LocalDate.of(2025, 6, 1))
                .retirementStartDate(LocalDate.of(2020, 1, 1))
                .totalExpenses(new BigDecimal("10000"))
                .otherIncome(BigDecimal.ZERO)
                .build();
    }
}
