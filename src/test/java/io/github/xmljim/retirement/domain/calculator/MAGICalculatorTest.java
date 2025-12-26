package io.github.xmljim.retirement.domain.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.calculator.impl.DefaultMAGICalculator;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.value.IncomeDetails;

@DisplayName("MAGICalculator Tests")
class MAGICalculatorTest {

    private MAGICalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new DefaultMAGICalculator();
    }

    @Nested
    @DisplayName("Basic MAGI Calculation")
    class BasicCalculationTests {

        @Test
        @DisplayName("MAGI equals AGI when no add-backs")
        void magiEqualsAgiWhenNoAddBacks() {
            IncomeDetails income = IncomeDetails.builder()
                .adjustedGrossIncome(new BigDecimal("150000"))
                .build();

            BigDecimal magi = calculator.calculate(income);

            assertEquals(0, new BigDecimal("150000").compareTo(magi));
        }

        @Test
        @DisplayName("MAGI includes student loan interest add-back")
        void magiIncludesStudentLoanInterest() {
            IncomeDetails income = IncomeDetails.builder()
                .adjustedGrossIncome(new BigDecimal("150000"))
                .studentLoanInterest(new BigDecimal("2500"))
                .build();

            BigDecimal magi = calculator.calculate(income);

            assertEquals(0, new BigDecimal("152500").compareTo(magi));
        }

        @Test
        @DisplayName("MAGI includes all add-backs")
        void magiIncludesAllAddBacks() {
            IncomeDetails income = IncomeDetails.builder()
                .adjustedGrossIncome(new BigDecimal("100000"))
                .studentLoanInterest(new BigDecimal("2500"))
                .foreignEarnedIncomeExclusion(new BigDecimal("120000"))
                .foreignHousingExclusion(new BigDecimal("15000"))
                .savingsBondInterestExclusion(new BigDecimal("1000"))
                .adoptionBenefitsExclusion(new BigDecimal("5000"))
                .build();

            BigDecimal magi = calculator.calculate(income);

            // 100000 + 2500 + 120000 + 15000 + 1000 + 5000 = 243500
            assertEquals(0, new BigDecimal("243500").compareTo(magi));
        }
    }

    @Nested
    @DisplayName("Simple AGI Calculation")
    class SimpleAgiTests {

        @Test
        @DisplayName("Calculate from just AGI")
        void calculateFromJustAgi() {
            BigDecimal magi = calculator.calculate(new BigDecimal("175000"));
            assertEquals(0, new BigDecimal("175000").compareTo(magi));
        }

        @Test
        @DisplayName("Null AGI returns zero")
        void nullAgiReturnsZero() {
            BigDecimal magi = calculator.calculate((BigDecimal) null);
            assertEquals(BigDecimal.ZERO, magi);
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Throws for null IncomeDetails")
        void throwsForNullIncomeDetails() {
            assertThrows(MissingRequiredFieldException.class, () ->
                calculator.calculate((IncomeDetails) null));
        }
    }

    @Nested
    @DisplayName("Factory Tests")
    class FactoryTests {

        @Test
        @DisplayName("Factory returns calculator instance")
        void factoryReturnsInstance() {
            MAGICalculator calc = CalculatorFactory.magiCalculator();
            BigDecimal magi = calc.calculate(IncomeDetails.ofAgi(new BigDecimal("100000")));
            assertEquals(0, new BigDecimal("100000").compareTo(magi));
        }
    }
}
