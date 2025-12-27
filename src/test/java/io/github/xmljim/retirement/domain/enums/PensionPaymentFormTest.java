package io.github.xmljim.retirement.domain.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PensionPaymentForm Tests")
class PensionPaymentFormTest {

    @Nested
    @DisplayName("Survivor Benefit Tests")
    class SurvivorBenefitTests {

        @Test
        @DisplayName("SINGLE_LIFE should have no survivor benefit")
        void singleLifeNoSurvivor() {
            assertFalse(PensionPaymentForm.SINGLE_LIFE.hasSurvivorBenefit());
            assertEquals(0, BigDecimal.ZERO.compareTo(
                PensionPaymentForm.SINGLE_LIFE.getSurvivorPercentage()));
        }

        @Test
        @DisplayName("JOINT_100 should have 100% survivor benefit")
        void joint100Survivor() {
            assertTrue(PensionPaymentForm.JOINT_100.hasSurvivorBenefit());
            assertEquals(0, BigDecimal.ONE.compareTo(
                PensionPaymentForm.JOINT_100.getSurvivorPercentage()));
        }

        @Test
        @DisplayName("JOINT_75 should have 75% survivor benefit")
        void joint75Survivor() {
            assertTrue(PensionPaymentForm.JOINT_75.hasSurvivorBenefit());
            assertEquals(0, new BigDecimal("0.75").compareTo(
                PensionPaymentForm.JOINT_75.getSurvivorPercentage()));
        }

        @Test
        @DisplayName("JOINT_50 should have 50% survivor benefit")
        void joint50Survivor() {
            assertTrue(PensionPaymentForm.JOINT_50.hasSurvivorBenefit());
            assertEquals(0, new BigDecimal("0.50").compareTo(
                PensionPaymentForm.JOINT_50.getSurvivorPercentage()));
        }

        @Test
        @DisplayName("PERIOD_CERTAIN should have survivor benefit")
        void periodCertainSurvivor() {
            assertTrue(PensionPaymentForm.PERIOD_CERTAIN.hasSurvivorBenefit());
        }
    }

    @Nested
    @DisplayName("Typical Reduction Tests")
    class TypicalReductionTests {

        @Test
        @DisplayName("SINGLE_LIFE should have no reduction")
        void singleLifeNoReduction() {
            assertEquals(0, BigDecimal.ZERO.compareTo(
                PensionPaymentForm.SINGLE_LIFE.getTypicalReduction()));
        }

        @Test
        @DisplayName("JOINT_100 should have highest reduction")
        void joint100HighestReduction() {
            BigDecimal reduction = PensionPaymentForm.JOINT_100.getTypicalReduction();
            assertTrue(reduction.compareTo(PensionPaymentForm.JOINT_75.getTypicalReduction()) > 0);
            assertTrue(reduction.compareTo(PensionPaymentForm.JOINT_50.getTypicalReduction()) > 0);
        }

        @Test
        @DisplayName("Reductions should decrease with survivor percentage")
        void reductionsDecreaseWithSurvivorPercentage() {
            BigDecimal joint100 = PensionPaymentForm.JOINT_100.getTypicalReduction();
            BigDecimal joint75 = PensionPaymentForm.JOINT_75.getTypicalReduction();
            BigDecimal joint50 = PensionPaymentForm.JOINT_50.getTypicalReduction();

            assertTrue(joint100.compareTo(joint75) > 0);
            assertTrue(joint75.compareTo(joint50) > 0);
        }
    }

    @Nested
    @DisplayName("Apply Reduction Tests")
    class ApplyReductionTests {

        @Test
        @DisplayName("SINGLE_LIFE should not reduce benefit")
        void singleLifeNoReduction() {
            BigDecimal benefit = new BigDecimal("3000");
            BigDecimal result = PensionPaymentForm.SINGLE_LIFE.applyTypicalReduction(benefit);
            assertEquals(0, benefit.compareTo(result));
        }

        @Test
        @DisplayName("JOINT_100 should apply 12% reduction")
        void joint100Applies12PercentReduction() {
            BigDecimal benefit = new BigDecimal("3000");
            BigDecimal result = PensionPaymentForm.JOINT_100.applyTypicalReduction(benefit);
            // 3000 * (1 - 0.12) = 2640
            assertEquals(0, new BigDecimal("2640").compareTo(result));
        }

        @Test
        @DisplayName("JOINT_75 should apply 8% reduction")
        void joint75Applies8PercentReduction() {
            BigDecimal benefit = new BigDecimal("3000");
            BigDecimal result = PensionPaymentForm.JOINT_75.applyTypicalReduction(benefit);
            // 3000 * (1 - 0.08) = 2760
            assertEquals(0, new BigDecimal("2760").compareTo(result));
        }
    }

    @Nested
    @DisplayName("Display Name Tests")
    class DisplayNameTests {

        @Test
        @DisplayName("All forms should have display names")
        void allFormsHaveDisplayNames() {
            for (PensionPaymentForm form : PensionPaymentForm.values()) {
                assertFalse(form.getDisplayName().isEmpty());
            }
        }
    }
}
