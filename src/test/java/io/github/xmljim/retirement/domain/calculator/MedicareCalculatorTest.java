package io.github.xmljim.retirement.domain.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.github.xmljim.retirement.domain.config.MedicareRules;
import io.github.xmljim.retirement.domain.enums.FilingStatus;
import io.github.xmljim.retirement.domain.value.MedicarePremiums;
import io.github.xmljim.retirement.domain.value.MedicarePremiums.IrmaaBracket;

@DisplayName("MedicareCalculator Tests")
class MedicareCalculatorTest {

    private MedicareCalculator calculator;
    private MedicareRules rules;

    @BeforeEach
    void setUp() {
        rules = createTestRules();
        calculator = CalculatorFactory.medicareCalculator(rules);
    }

    @Nested
    @DisplayName("IRMAA Bracket Tests")
    class IrmaaBracketTests {

        @ParameterizedTest
        @DisplayName("Single filer brackets")
        @CsvSource({
            "50000, BRACKET_0",
            "106000, BRACKET_0",
            "106001, BRACKET_1",
            "133000, BRACKET_1",
            "133001, BRACKET_2",
            "167000, BRACKET_2",
            "167001, BRACKET_3",
            "200000, BRACKET_3",
            "200001, BRACKET_4",
            "500000, BRACKET_4",
            "500001, BRACKET_5",
            "1000000, BRACKET_5"
        })
        void singleFilerBrackets(String magi, IrmaaBracket expected) {
            IrmaaBracket bracket = calculator.getIrmaaBracket(
                new BigDecimal(magi), FilingStatus.SINGLE, 2025);
            assertEquals(expected, bracket);
        }

        @ParameterizedTest
        @DisplayName("Joint filer brackets")
        @CsvSource({
            "100000, BRACKET_0",
            "212000, BRACKET_0",
            "212001, BRACKET_1",
            "266000, BRACKET_1",
            "266001, BRACKET_2",
            "334000, BRACKET_2",
            "334001, BRACKET_3",
            "400000, BRACKET_3",
            "400001, BRACKET_4",
            "750000, BRACKET_4",
            "750001, BRACKET_5"
        })
        void jointFilerBrackets(String magi, IrmaaBracket expected) {
            IrmaaBracket bracket = calculator.getIrmaaBracket(
                new BigDecimal(magi), FilingStatus.MARRIED_FILING_JOINTLY, 2025);
            assertEquals(expected, bracket);
        }
    }

    @Nested
    @DisplayName("Premium Calculation Tests")
    class PremiumCalculationTests {

        @Test
        @DisplayName("Standard premium - no IRMAA")
        void standardPremium() {
            MedicarePremiums premiums = calculator.calculatePremiums(
                new BigDecimal("100000"), FilingStatus.SINGLE, 2025);

            assertEquals(0, BigDecimal.ZERO.compareTo(premiums.partAPremium()));
            assertEquals(0, new BigDecimal("185.00").compareTo(premiums.partBBasePremium()));
            assertEquals(0, BigDecimal.ZERO.compareTo(premiums.partBIrmaa()));
            assertEquals(0, BigDecimal.ZERO.compareTo(premiums.partDIrmaa()));
            assertEquals(IrmaaBracket.BRACKET_0, premiums.bracket());
            assertFalse(premiums.hasIrmaa());
        }

        @Test
        @DisplayName("Bracket 1 IRMAA")
        void bracket1Irmaa() {
            MedicarePremiums premiums = calculator.calculatePremiums(
                new BigDecimal("120000"), FilingStatus.SINGLE, 2025);

            assertEquals(IrmaaBracket.BRACKET_1, premiums.bracket());
            assertTrue(premiums.hasIrmaa());
            assertEquals(0, new BigDecimal("74.00").compareTo(premiums.partBIrmaa()));
            assertEquals(0, new BigDecimal("13.70").compareTo(premiums.partDIrmaa()));
        }

        @Test
        @DisplayName("Highest bracket IRMAA")
        void highestBracketIrmaa() {
            MedicarePremiums premiums = calculator.calculatePremiums(
                new BigDecimal("600000"), FilingStatus.SINGLE, 2025);

            assertEquals(IrmaaBracket.BRACKET_5, premiums.bracket());
            assertEquals(0, new BigDecimal("443.90").compareTo(premiums.partBIrmaa()));
            assertEquals(0, new BigDecimal("85.80").compareTo(premiums.partDIrmaa()));
        }

        @Test
        @DisplayName("Total monthly calculation")
        void totalMonthlyCalculation() {
            MedicarePremiums premiums = calculator.calculatePremiums(
                new BigDecimal("100000"), FilingStatus.SINGLE, 2025);

            assertEquals(0, new BigDecimal("185.00").compareTo(premiums.getTotalMonthly()));
        }

        @Test
        @DisplayName("Annual calculation")
        void annualCalculation() {
            MedicarePremiums premiums = calculator.calculatePremiums(
                new BigDecimal("100000"), FilingStatus.SINGLE, 2025);

            assertEquals(0, new BigDecimal("2220.00").compareTo(premiums.getTotalAnnual()));
        }
    }

    @Nested
    @DisplayName("Part A Premium Tests")
    class PartAPremiumTests {

        @Test
        @DisplayName("Premium-free Part A (40+ quarters)")
        void premiumFreePartA() {
            assertEquals(0, BigDecimal.ZERO.compareTo(calculator.getPartAPremium(2025, 40)));
        }

        @Test
        @DisplayName("Reduced Part A (30-39 quarters)")
        void reducedPartA() {
            assertEquals(0, new BigDecimal("285").compareTo(calculator.getPartAPremium(2025, 35)));
        }

        @Test
        @DisplayName("Full Part A (under 30 quarters)")
        void fullPartA() {
            assertEquals(0, new BigDecimal("518").compareTo(calculator.getPartAPremium(2025, 20)));
        }
    }

    @Nested
    @DisplayName("Filing Status Tests")
    class FilingStatusTests {

        @Test
        @DisplayName("Head of Household uses single thresholds")
        void headOfHouseholdUsesSingle() {
            IrmaaBracket single = calculator.getIrmaaBracket(
                new BigDecimal("120000"), FilingStatus.SINGLE, 2025);
            IrmaaBracket hoh = calculator.getIrmaaBracket(
                new BigDecimal("120000"), FilingStatus.HEAD_OF_HOUSEHOLD, 2025);
            assertEquals(single, hoh);
        }

        @Test
        @DisplayName("QSS uses joint thresholds")
        void qssUsesJoint() {
            IrmaaBracket joint = calculator.getIrmaaBracket(
                new BigDecimal("220000"), FilingStatus.MARRIED_FILING_JOINTLY, 2025);
            IrmaaBracket qss = calculator.getIrmaaBracket(
                new BigDecimal("220000"), FilingStatus.QUALIFYING_SURVIVING_SPOUSE, 2025);
            assertEquals(joint, qss);
        }
    }

    private MedicareRules createTestRules() {
        MedicareRules.YearConfig year2025 = new MedicareRules.YearConfig();
        year2025.setYear(2025);
        year2025.setPartBBase(new BigDecimal("185.00"));
        year2025.setPartAPremiumFull(new BigDecimal("518"));
        year2025.setPartAPremiumReduced(new BigDecimal("285"));
        year2025.setIrmaaBrackets(java.util.List.of(
            createBracket(0, 106000, 0, 212000, "185.00", "0"),
            createBracket(106001, 133000, 212001, 266000, "259.00", "13.70"),
            createBracket(133001, 167000, 266001, 334000, "370.00", "35.30"),
            createBracket(167001, 200000, 334001, 400000, "480.90", "57.00"),
            createBracket(200001, 500000, 400001, 750000, "591.90", "78.60"),
            createBracketNoMax(500001, 750001, "628.90", "85.80")
        ));

        MedicareRules testRules = new MedicareRules();
        testRules.setYears(java.util.List.of(year2025));
        return testRules;
    }

    private MedicareRules.IrmaaBracketConfig createBracket(
            int singleMin, int singleMax, int jointMin, int jointMax,
            String partBPremium, String partDIrmaa) {
        MedicareRules.IrmaaBracketConfig bracket = new MedicareRules.IrmaaBracketConfig();
        bracket.setSingleMin(BigDecimal.valueOf(singleMin));
        bracket.setSingleMax(BigDecimal.valueOf(singleMax));
        bracket.setJointMin(BigDecimal.valueOf(jointMin));
        bracket.setJointMax(BigDecimal.valueOf(jointMax));
        bracket.setPartBPremium(new BigDecimal(partBPremium));
        bracket.setPartDIrmaa(new BigDecimal(partDIrmaa));
        return bracket;
    }

    private MedicareRules.IrmaaBracketConfig createBracketNoMax(
            int singleMin, int jointMin, String partBPremium, String partDIrmaa) {
        MedicareRules.IrmaaBracketConfig bracket = new MedicareRules.IrmaaBracketConfig();
        bracket.setSingleMin(BigDecimal.valueOf(singleMin));
        bracket.setJointMin(BigDecimal.valueOf(jointMin));
        bracket.setPartBPremium(new BigDecimal(partBPremium));
        bracket.setPartDIrmaa(new BigDecimal(partDIrmaa));
        return bracket;
    }
}
