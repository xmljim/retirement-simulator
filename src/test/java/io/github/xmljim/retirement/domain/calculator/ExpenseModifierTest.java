package io.github.xmljim.retirement.domain.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.calculator.impl.AgeBasedModifier;
import io.github.xmljim.retirement.domain.calculator.impl.PayoffModifier;
import io.github.xmljim.retirement.domain.calculator.impl.SpendingCurveModifier;
import io.github.xmljim.retirement.domain.enums.SpendingPhase;

@DisplayName("ExpenseModifier Tests")
class ExpenseModifierTest {

    private static final BigDecimal BASE = new BigDecimal("1000");

    @Nested
    @DisplayName("PayoffModifier Tests")
    class PayoffModifierTests {

        @Test
        @DisplayName("Returns full amount before payoff date")
        void beforePayoff() {
            PayoffModifier mod = PayoffModifier.onDate(LocalDate.of(2035, 1, 1));
            BigDecimal result = mod.modify(BASE, LocalDate.of(2034, 12, 31), 64);
            assertEquals(0, BASE.compareTo(result));
        }

        @Test
        @DisplayName("Returns zero on payoff date")
        void onPayoff() {
            PayoffModifier mod = PayoffModifier.onDate(LocalDate.of(2035, 1, 1));
            BigDecimal result = mod.modify(BASE, LocalDate.of(2035, 1, 1), 65);
            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        @DisplayName("Returns zero after payoff date")
        void afterPayoff() {
            PayoffModifier mod = PayoffModifier.onDate(LocalDate.of(2035, 1, 1));
            BigDecimal result = mod.modify(BASE, LocalDate.of(2035, 6, 1), 65);
            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        @DisplayName("isPaidOff returns correct status")
        void isPaidOffStatus() {
            PayoffModifier mod = PayoffModifier.onDate(LocalDate.of(2035, 1, 1));
            assertFalse(mod.isPaidOff(LocalDate.of(2034, 12, 31)));
            assertTrue(mod.isPaidOff(LocalDate.of(2035, 1, 1)));
        }
    }

    @Nested
    @DisplayName("SpendingCurveModifier Tests")
    class SpendingCurveTests {

        @Test
        @DisplayName("Go-Go phase returns full amount")
        void goGoPhase() {
            SpendingCurveModifier mod = SpendingCurveModifier.withDefaults();
            BigDecimal result = mod.modify(BASE, LocalDate.now(), 70);
            assertEquals(0, BASE.compareTo(result));
        }

        @Test
        @DisplayName("Slow-Go phase returns 80%")
        void slowGoPhase() {
            SpendingCurveModifier mod = SpendingCurveModifier.withDefaults();
            BigDecimal result = mod.modify(BASE, LocalDate.now(), 80);
            assertEquals(0, new BigDecimal("800.00").compareTo(result));
        }

        @Test
        @DisplayName("No-Go phase returns 50%")
        void noGoPhase() {
            SpendingCurveModifier mod = SpendingCurveModifier.withDefaults();
            BigDecimal result = mod.modify(BASE, LocalDate.now(), 90);
            assertEquals(0, new BigDecimal("500.00").compareTo(result));
        }

        @Test
        @DisplayName("Custom age ranges work")
        void customAgeRanges() {
            SpendingCurveModifier mod = SpendingCurveModifier.builder()
                    .slowGoStartAge(70)
                    .noGoStartAge(80)
                    .build();
            assertEquals(SpendingPhase.SLOW_GO, mod.getPhaseForAge(75));
            assertEquals(SpendingPhase.NO_GO, mod.getPhaseForAge(85));
        }
    }

    @Nested
    @DisplayName("AgeBasedModifier Tests")
    class AgeBasedTests {

        @Test
        @DisplayName("Healthcare defaults work correctly")
        void healthcareDefaults() {
            AgeBasedModifier mod = AgeBasedModifier.forHealthcare();
            assertEquals(0, new BigDecimal("1000.00").compareTo(mod.modify(BASE, LocalDate.now(), 70)));
            assertEquals(0, new BigDecimal("1500.00").compareTo(mod.modify(BASE, LocalDate.now(), 80)));
            assertEquals(0, new BigDecimal("2000.00").compareTo(mod.modify(BASE, LocalDate.now(), 90)));
        }

        @Test
        @DisplayName("Custom brackets work")
        void customBrackets() {
            AgeBasedModifier mod = AgeBasedModifier.builder()
                    .addBracket(60, 1.0)
                    .addBracket(70, 1.25)
                    .addBracket(80, 1.75)
                    .build();
            assertEquals(0, new BigDecimal("1250.00").compareTo(mod.modify(BASE, LocalDate.now(), 75)));
        }
    }

    @Nested
    @DisplayName("Modifier Chaining Tests")
    class ChainingTests {

        @Test
        @DisplayName("andThen chains modifiers")
        void andThenChaining() {
            ExpenseModifier half = ExpenseModifier.multiplier(new BigDecimal("0.5"));
            ExpenseModifier doubled = ExpenseModifier.multiplier(new BigDecimal("2.0"));
            ExpenseModifier combined = half.andThen(doubled);
            BigDecimal result = combined.modify(BASE, LocalDate.now(), 65);
            assertEquals(0, BASE.compareTo(result)); // 0.5 * 2.0 = 1.0
        }

        @Test
        @DisplayName("combine creates composite modifier")
        void combineModifiers() {
            ExpenseModifier m1 = ExpenseModifier.multiplier(new BigDecimal("0.8"));
            ExpenseModifier m2 = ExpenseModifier.multiplier(new BigDecimal("0.5"));
            ExpenseModifier combined = ExpenseModifier.combine(m1, m2);
            BigDecimal result = combined.modify(BASE, LocalDate.now(), 65);
            assertEquals(0, new BigDecimal("400").compareTo(result)); // 0.8 * 0.5 = 0.4
        }

        @Test
        @DisplayName("identity returns unchanged amount")
        void identityModifier() {
            ExpenseModifier identity = ExpenseModifier.identity();
            assertEquals(0, BASE.compareTo(identity.modify(BASE, LocalDate.now(), 65)));
        }
    }

    @Nested
    @DisplayName("SpendingPhase Enum Tests")
    class SpendingPhaseTests {

        @Test
        @DisplayName("forAge returns correct phase")
        void forAgeReturnsCorrectPhase() {
            assertEquals(SpendingPhase.GO_GO, SpendingPhase.forAge(70));
            assertEquals(SpendingPhase.SLOW_GO, SpendingPhase.forAge(80));
            assertEquals(SpendingPhase.NO_GO, SpendingPhase.forAge(90));
        }

        @Test
        @DisplayName("isInRange checks correctly")
        void isInRangeChecks() {
            assertTrue(SpendingPhase.GO_GO.isInRange(70));
            assertFalse(SpendingPhase.GO_GO.isInRange(80));
        }
    }
}
