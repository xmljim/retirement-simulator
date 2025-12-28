package io.github.xmljim.retirement.domain.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.config.FederalTaxRules;
import io.github.xmljim.retirement.domain.config.FederalTaxRules.BracketConfig;
import io.github.xmljim.retirement.domain.enums.FilingStatus;
import io.github.xmljim.retirement.domain.value.TaxBracket;
import io.github.xmljim.retirement.domain.value.TaxCalculationResult;

@DisplayName("FederalTaxCalculator Tests")
class FederalTaxCalculatorTest {

    private FederalTaxCalculator calculator;
    private FederalTaxRules rules;

    @BeforeEach
    void setUp() {
        rules = createTestRules();
        calculator = CalculatorFactory.federalTaxCalculator(rules);
    }

    @Nested
    @DisplayName("Standard Deduction Tests")
    class StandardDeductionTests {

        @Test
        @DisplayName("Single under 65")
        void singleUnder65() {
            BigDecimal deduction = calculator.getStandardDeduction(FilingStatus.SINGLE, 60, 2024);
            assertEquals(0, new BigDecimal("14600").compareTo(deduction));
        }

        @Test
        @DisplayName("Single 65+")
        void single65Plus() {
            BigDecimal deduction = calculator.getStandardDeduction(FilingStatus.SINGLE, 67, 2024);
            // 14600 + 1950 = 16550
            assertEquals(0, new BigDecimal("16550").compareTo(deduction));
        }

        @Test
        @DisplayName("MFJ both under 65")
        void mfjBothUnder65() {
            BigDecimal deduction = calculator.getStandardDeductionMfj(60, 58, 2024);
            assertEquals(0, new BigDecimal("29200").compareTo(deduction));
        }

        @Test
        @DisplayName("MFJ one spouse 65+")
        void mfjOneSpouse65Plus() {
            BigDecimal deduction = calculator.getStandardDeductionMfj(67, 60, 2024);
            // 29200 + 1550 = 30750
            assertEquals(0, new BigDecimal("30750").compareTo(deduction));
        }

        @Test
        @DisplayName("MFJ both 65+")
        void mfjBoth65Plus() {
            BigDecimal deduction = calculator.getStandardDeductionMfj(67, 68, 2024);
            // 29200 + 1550 + 1550 = 32300
            assertEquals(0, new BigDecimal("32300").compareTo(deduction));
        }
    }

    @Nested
    @DisplayName("Tax Calculation Tests")
    class TaxCalculationTests {

        @Test
        @DisplayName("Single $50K income")
        void single50kIncome() {
            TaxCalculationResult result = calculator.calculateTax(
                new BigDecimal("50000"), FilingStatus.SINGLE, 60, 2024);

            assertTrue(result.hasTaxLiability());
            // Taxable: 50000 - 14600 = 35400
            assertEquals(0, new BigDecimal("35400").compareTo(result.taxableIncome()));
        }

        @Test
        @DisplayName("Zero taxable income returns no tax")
        void zeroTaxableIncomeNoTax() {
            TaxCalculationResult result = calculator.calculateTax(
                new BigDecimal("10000"), FilingStatus.SINGLE, 60, 2024);

            assertFalse(result.hasTaxLiability());
            assertEquals(0, BigDecimal.ZERO.compareTo(result.federalTax()));
        }

        @Test
        @DisplayName("Tax calculation with bracket breakdown")
        void taxWithBracketBreakdown() {
            TaxCalculationResult result = calculator.calculateTax(
                new BigDecimal("75000"), FilingStatus.SINGLE, 60, 2024);

            // Taxable: 75000 - 14600 = 60400
            assertEquals(0, new BigDecimal("60400").compareTo(result.taxableIncome()));

            // Should have entries in 10%, 12%, and 22% brackets
            assertTrue(result.bracketBreakdown().size() >= 3);
        }
    }

    @Nested
    @DisplayName("Marginal Rate Tests")
    class MarginalRateTests {

        @Test
        @DisplayName("Income in 10% bracket")
        void incomeIn10Bracket() {
            BigDecimal rate = calculator.getMarginalTaxRate(
                new BigDecimal("10000"), FilingStatus.SINGLE, 2024);
            assertEquals(0, new BigDecimal("0.10").compareTo(rate));
        }

        @Test
        @DisplayName("Income in 22% bracket")
        void incomeIn22Bracket() {
            BigDecimal rate = calculator.getMarginalTaxRate(
                new BigDecimal("60000"), FilingStatus.SINGLE, 2024);
            assertEquals(0, new BigDecimal("0.22").compareTo(rate));
        }

        @Test
        @DisplayName("Income in top bracket")
        void incomeInTopBracket() {
            BigDecimal rate = calculator.getMarginalTaxRate(
                new BigDecimal("700000"), FilingStatus.SINGLE, 2024);
            assertEquals(0, new BigDecimal("0.37").compareTo(rate));
        }
    }

    @Nested
    @DisplayName("Chained CPI Projection Tests")
    class ChainedCpiTests {

        @Test
        @DisplayName("Project value to future year")
        void projectToFutureYear() {
            // Base: 14600, 2.5% for 5 years = 14600 * 1.025^5 ≈ 16522
            BigDecimal projected = calculator.projectValueWithChainedCpi(
                new BigDecimal("14600"), 2024, 2029);

            // Should be rounded to nearest $50
            assertTrue(projected.remainder(new BigDecimal("50")).compareTo(BigDecimal.ZERO) == 0);
            assertTrue(projected.compareTo(new BigDecimal("14600")) > 0);
        }

        @Test
        @DisplayName("Same year returns rounded base value")
        void sameYearReturnsBase() {
            BigDecimal projected = calculator.projectValueWithChainedCpi(
                new BigDecimal("14600"), 2024, 2024);
            assertEquals(0, new BigDecimal("14600").compareTo(projected));
        }
    }

    @Nested
    @DisplayName("Tax Bracket Tests")
    class TaxBracketTests {

        @Test
        @DisplayName("Get brackets for single filer")
        void getBracketsForSingle() {
            List<TaxBracket> brackets = calculator.getBrackets(FilingStatus.SINGLE, 2024);
            assertEquals(7, brackets.size());

            // First bracket is 10%
            assertEquals(0, new BigDecimal("0.10").compareTo(brackets.getFirst().rate()));

            // Last bracket is 37% with no upper bound
            assertTrue(brackets.getLast().isTopBracket());
            assertEquals(0, new BigDecimal("0.37").compareTo(brackets.getLast().rate()));
        }

        @Test
        @DisplayName("Brackets are progressive")
        void bracketsAreProgressive() {
            List<TaxBracket> brackets = calculator.getBrackets(FilingStatus.SINGLE, 2024);

            for (int i = 1; i < brackets.size(); i++) {
                assertTrue(brackets.get(i).rate().compareTo(brackets.get(i - 1).rate()) > 0);
            }
        }
    }

    @Nested
    @DisplayName("MFJ Calculation Tests")
    class MfjCalculationTests {

        @Test
        @DisplayName("MFJ $100K income both under 65")
        void mfj100kBothUnder65() {
            TaxCalculationResult result = calculator.calculateTaxMfj(
                new BigDecimal("100000"), 60, 58, 2024);

            // Taxable: 100000 - 29200 = 70800
            assertEquals(0, new BigDecimal("70800").compareTo(result.taxableIncome()));
            assertEquals(FilingStatus.MARRIED_FILING_JOINTLY, result.filingStatus());
        }
    }

    private FederalTaxRules createTestRules() {
        FederalTaxRules testRules = new FederalTaxRules();
        testRules.setBaseYear(2024);
        testRules.setChainedCpiRate(new BigDecimal("0.025"));

        // Standard deductions
        testRules.setStandardDeductions(Map.of(
            FilingStatus.SINGLE, new BigDecimal("14600"),
            FilingStatus.MARRIED_FILING_JOINTLY, new BigDecimal("29200"),
            FilingStatus.HEAD_OF_HOUSEHOLD, new BigDecimal("21900")
        ));

        // Age 65+ additional
        testRules.setAge65Additional(Map.of(
            FilingStatus.SINGLE, new BigDecimal("1950"),
            FilingStatus.MARRIED_FILING_JOINTLY, new BigDecimal("1550"),
            FilingStatus.HEAD_OF_HOUSEHOLD, new BigDecimal("1950")
        ));

        // Single brackets
        testRules.setBrackets(Map.of(
            FilingStatus.SINGLE, createSingleBrackets()
        ));

        return testRules;
    }

    private List<BracketConfig> createSingleBrackets() {
        return List.of(
            createBracket("0.10", "11600"),
            createBracket("0.12", "47150"),
            createBracket("0.22", "100525"),
            createBracket("0.24", "191950"),
            createBracket("0.32", "243725"),
            createBracket("0.35", "609350"),
            createBracketNoUpper("0.37")
        );
    }

    private BracketConfig createBracket(String rate, String upperBound) {
        BracketConfig config = new BracketConfig();
        config.setRate(new BigDecimal(rate));
        config.setUpperBound(new BigDecimal(upperBound));
        return config;
    }

    private BracketConfig createBracketNoUpper(String rate) {
        BracketConfig config = new BracketConfig();
        config.setRate(new BigDecimal(rate));
        return config;
    }
}
