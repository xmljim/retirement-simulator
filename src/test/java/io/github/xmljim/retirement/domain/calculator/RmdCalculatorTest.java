package io.github.xmljim.retirement.domain.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.github.xmljim.retirement.domain.config.RmdRules;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.value.RmdProjection;

@DisplayName("RmdCalculator Tests")
class RmdCalculatorTest {

    private RmdCalculator calculator;
    private RmdRules rules;

    @BeforeEach
    void setUp() {
        rules = createTestRules();
        calculator = CalculatorFactory.rmdCalculator(rules);
    }

    @Nested
    @DisplayName("Start Age Tests (SECURE 2.0)")
    class StartAgeTests {

        @ParameterizedTest
        @DisplayName("Birth year determines RMD start age")
        @CsvSource({
            "1945, 72",
            "1950, 72",
            "1951, 73",
            "1955, 73",
            "1959, 73",
            "1960, 75",
            "1970, 75",
            "1990, 75"
        })
        void startAgeByBirthYear(int birthYear, int expectedAge) {
            assertEquals(expectedAge, calculator.getRmdStartAge(birthYear));
        }
    }

    @Nested
    @DisplayName("Distribution Factor Tests")
    class DistributionFactorTests {

        @ParameterizedTest
        @DisplayName("Uniform Lifetime Table factors")
        @CsvSource({
            "72, 27.4",
            "75, 24.6",
            "80, 20.2",
            "85, 16.0",
            "90, 12.2",
            "95, 8.9",
            "100, 6.4"
        })
        void uniformLifetimeTableFactors(int age, String expectedFactor) {
            assertEquals(0, new BigDecimal(expectedFactor).compareTo(
                calculator.getDistributionFactor(age)));
        }
    }

    @Nested
    @DisplayName("RMD Calculation Tests")
    class RmdCalculationTests {

        @Test
        @DisplayName("Calculate RMD at age 75")
        void calculateRmdAt75() {
            BigDecimal balance = new BigDecimal("500000");
            BigDecimal rmd = calculator.calculateRmd(balance, 75);
            // 500000 / 24.6 = 20325.20
            assertEquals(0, new BigDecimal("20325.20").compareTo(rmd));
        }

        @Test
        @DisplayName("Calculate RMD at age 80")
        void calculateRmdAt80() {
            BigDecimal balance = new BigDecimal("400000");
            BigDecimal rmd = calculator.calculateRmd(balance, 80);
            // 400000 / 20.2 = 19801.98
            assertEquals(0, new BigDecimal("19801.98").compareTo(rmd));
        }

        @Test
        @DisplayName("Zero balance returns zero RMD")
        void zeroBalanceReturnsZeroRmd() {
            BigDecimal rmd = calculator.calculateRmd(BigDecimal.ZERO, 75);
            assertEquals(0, BigDecimal.ZERO.compareTo(rmd));
        }
    }

    @Nested
    @DisplayName("RMD Projection Tests")
    class RmdProjectionTests {

        @Test
        @DisplayName("Before RMD age returns not required")
        void beforeRmdAgeNotRequired() {
            RmdProjection projection = calculator.calculate(
                new BigDecimal("500000"), 70, 1960, 2035);

            assertFalse(projection.isRequired());
            assertEquals(0, BigDecimal.ZERO.compareTo(projection.rmdAmount()));
            assertNull(projection.deadline());
        }

        @Test
        @DisplayName("First RMD year has April 1 deadline")
        void firstRmdYearHasAprilDeadline() {
            RmdProjection projection = calculator.calculate(
                new BigDecimal("500000"), 75, 1960, 2035);

            assertTrue(projection.isRequired());
            assertTrue(projection.isFirstRmd());
            assertEquals(LocalDate.of(2036, 4, 1), projection.deadline());
        }

        @Test
        @DisplayName("Subsequent RMD has December 31 deadline")
        void subsequentRmdHasDecemberDeadline() {
            RmdProjection projection = calculator.calculate(
                new BigDecimal("500000"), 76, 1960, 2036);

            assertTrue(projection.isRequired());
            assertFalse(projection.isFirstRmd());
            assertEquals(LocalDate.of(2036, 12, 31), projection.deadline());
        }

        @Test
        @DisplayName("Projection includes correct withdrawal percentage")
        void projectionIncludesWithdrawalPercentage() {
            RmdProjection projection = calculator.calculate(
                new BigDecimal("500000"), 75, 1960, 2035);

            // 20325.20 / 500000 = 0.040650
            BigDecimal percentage = projection.getWithdrawalPercentage();
            assertTrue(percentage.compareTo(new BigDecimal("0.04")) > 0);
            assertTrue(percentage.compareTo(new BigDecimal("0.05")) < 0);
        }
    }

    @Nested
    @DisplayName("Account Type Tests")
    class AccountTypeTests {

        @Test
        @DisplayName("Traditional 401k subject to RMD")
        void traditional401kSubjectToRmd() {
            assertTrue(calculator.isSubjectToRmd(AccountType.TRADITIONAL_401K));
        }

        @Test
        @DisplayName("Traditional IRA subject to RMD")
        void traditionalIraSubjectToRmd() {
            assertTrue(calculator.isSubjectToRmd(AccountType.TRADITIONAL_IRA));
        }

        @Test
        @DisplayName("Roth IRA not subject to RMD")
        void rothIraNotSubjectToRmd() {
            assertFalse(calculator.isSubjectToRmd(AccountType.ROTH_IRA));
        }

        @Test
        @DisplayName("HSA not subject to RMD")
        void hsaNotSubjectToRmd() {
            assertFalse(calculator.isSubjectToRmd(AccountType.HSA));
        }

        @Test
        @DisplayName("Taxable brokerage not subject to RMD")
        void taxableBrokerageNotSubjectToRmd() {
            assertFalse(calculator.isSubjectToRmd(AccountType.TAXABLE_BROKERAGE));
        }
    }

    @Nested
    @DisplayName("Helper Method Tests")
    class HelperMethodTests {

        @Test
        @DisplayName("isRmdRequired returns correct value")
        void isRmdRequiredReturnsCorrectValue() {
            assertFalse(calculator.isRmdRequired(74, 1960));
            assertTrue(calculator.isRmdRequired(75, 1960));
            assertTrue(calculator.isRmdRequired(76, 1960));
        }

        @Test
        @DisplayName("getFirstRmdYear calculates correctly")
        void getFirstRmdYearCalculatesCorrectly() {
            assertEquals(2035, calculator.getFirstRmdYear(1960));  // 1960 + 75 = 2035
            assertEquals(2024, calculator.getFirstRmdYear(1951));  // 1951 + 73 = 2024
            assertEquals(2022, calculator.getFirstRmdYear(1950));  // 1950 + 72 = 2022
        }
    }

    private RmdRules createTestRules() {
        // Start ages
        RmdRules.StartAgeEntry entry72 = new RmdRules.StartAgeEntry();
        entry72.setBirthYearMax(1950);
        entry72.setStartAge(72);

        RmdRules.StartAgeEntry entry73 = new RmdRules.StartAgeEntry();
        entry73.setBirthYearMin(1951);
        entry73.setBirthYearMax(1959);
        entry73.setStartAge(73);

        RmdRules.StartAgeEntry entry75 = new RmdRules.StartAgeEntry();
        entry75.setBirthYearMin(1960);
        entry75.setStartAge(75);

        // Uniform Lifetime Table (subset for testing)
        java.util.List<RmdRules.LifeTableEntry> lifeTable = java.util.List.of(
            createLifeTableEntry(72, "27.4"),
            createLifeTableEntry(73, "26.5"),
            createLifeTableEntry(74, "25.5"),
            createLifeTableEntry(75, "24.6"),
            createLifeTableEntry(76, "23.7"),
            createLifeTableEntry(80, "20.2"),
            createLifeTableEntry(85, "16.0"),
            createLifeTableEntry(90, "12.2"),
            createLifeTableEntry(95, "8.9"),
            createLifeTableEntry(100, "6.4")
        );

        RmdRules testRules = new RmdRules();
        testRules.setStartAgeByBirthYear(java.util.List.of(entry72, entry73, entry75));
        testRules.setUniformLifetimeTable(lifeTable);
        return testRules;
    }

    private RmdRules.LifeTableEntry createLifeTableEntry(int age, String factor) {
        RmdRules.LifeTableEntry entry = new RmdRules.LifeTableEntry();
        entry.setAge(age);
        entry.setFactor(new BigDecimal(factor));
        return entry;
    }
}
