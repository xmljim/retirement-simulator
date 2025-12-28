package io.github.xmljim.retirement.domain.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.ExpenseCategory;
import io.github.xmljim.retirement.domain.enums.ExpenseCategory.InflationType;

@DisplayName("InflationRates Tests")
class InflationRatesTest {

    private InflationRates rates;

    @BeforeEach
    void setUp() {
        rates = new InflationRates();
    }

    @Nested
    @DisplayName("Default Value Tests")
    class DefaultValueTests {

        @Test
        @DisplayName("Default general CPI should be 2.5%")
        void defaultGeneralCpi() {
            assertEquals(new BigDecimal("0.025"), rates.getGeneralCpi());
        }

        @Test
        @DisplayName("Default healthcare should be 5.5%")
        void defaultHealthcare() {
            assertEquals(new BigDecimal("0.055"), rates.getHealthcare());
        }

        @Test
        @DisplayName("Default housing should be 3.0%")
        void defaultHousing() {
            assertEquals(new BigDecimal("0.030"), rates.getHousing());
        }

        @Test
        @DisplayName("Default LTC should be 7.0%")
        void defaultLtc() {
            assertEquals(new BigDecimal("0.070"), rates.getLtc());
        }

        @Test
        @DisplayName("defaults() factory method should return instance with defaults")
        void defaultsFactoryMethod() {
            InflationRates defaultRates = InflationRates.defaults();
            assertNotNull(defaultRates);
            assertEquals(InflationRates.DEFAULT_GENERAL_CPI, defaultRates.getGeneralCpi());
            assertEquals(InflationRates.DEFAULT_HEALTHCARE, defaultRates.getHealthcare());
            assertEquals(InflationRates.DEFAULT_HOUSING, defaultRates.getHousing());
            assertEquals(InflationRates.DEFAULT_LTC, defaultRates.getLtc());
        }
    }

    @Nested
    @DisplayName("Rate Setter Tests")
    class RateSetterTests {

        @Test
        @DisplayName("Should allow setting custom general CPI rate")
        void setCustomGeneralCpi() {
            rates.setGeneralCpi(new BigDecimal("0.03"));
            assertEquals(new BigDecimal("0.03"), rates.getGeneralCpi());
        }

        @Test
        @DisplayName("Should allow setting custom healthcare rate")
        void setCustomHealthcare() {
            rates.setHealthcare(new BigDecimal("0.06"));
            assertEquals(new BigDecimal("0.06"), rates.getHealthcare());
        }

        @Test
        @DisplayName("Should allow setting custom housing rate")
        void setCustomHousing() {
            rates.setHousing(new BigDecimal("0.035"));
            assertEquals(new BigDecimal("0.035"), rates.getHousing());
        }

        @Test
        @DisplayName("Should allow setting custom LTC rate")
        void setCustomLtc() {
            rates.setLtc(new BigDecimal("0.08"));
            assertEquals(new BigDecimal("0.08"), rates.getLtc());
        }

        @Test
        @DisplayName("Setting null general CPI should use default")
        void nullGeneralCpiUsesDefault() {
            rates.setGeneralCpi(null);
            assertEquals(InflationRates.DEFAULT_GENERAL_CPI, rates.getGeneralCpi());
        }

        @Test
        @DisplayName("Setting null healthcare should use default")
        void nullHealthcareUsesDefault() {
            rates.setHealthcare(null);
            assertEquals(InflationRates.DEFAULT_HEALTHCARE, rates.getHealthcare());
        }
    }

    @Nested
    @DisplayName("Get Rate For Category Tests")
    class GetRateForCategoryTests {

        @Test
        @DisplayName("HOUSING should return housing rate")
        void housingCategoryReturnsHousingRate() {
            assertEquals(rates.getHousing(), rates.getRateForCategory(ExpenseCategory.HOUSING));
        }

        @Test
        @DisplayName("HOME_REPAIRS should return housing rate")
        void homeRepairsReturnsHousingRate() {
            assertEquals(rates.getHousing(), rates.getRateForCategory(ExpenseCategory.HOME_REPAIRS));
        }

        @Test
        @DisplayName("MEDICARE_PREMIUMS should return healthcare rate")
        void medicareReturnsHealthcareRate() {
            assertEquals(rates.getHealthcare(), rates.getRateForCategory(ExpenseCategory.MEDICARE_PREMIUMS));
        }

        @Test
        @DisplayName("HEALTHCARE_OOP should return healthcare rate")
        void healthcareOopReturnsHealthcareRate() {
            assertEquals(rates.getHealthcare(), rates.getRateForCategory(ExpenseCategory.HEALTHCARE_OOP));
        }

        @Test
        @DisplayName("LTC_PREMIUMS should return LTC rate")
        void ltcPremiumsReturnsLtcRate() {
            assertEquals(rates.getLtc(), rates.getRateForCategory(ExpenseCategory.LTC_PREMIUMS));
        }

        @Test
        @DisplayName("LTC_CARE should return LTC rate")
        void ltcCareReturnsLtcRate() {
            assertEquals(rates.getLtc(), rates.getRateForCategory(ExpenseCategory.LTC_CARE));
        }

        @Test
        @DisplayName("FOOD should return general CPI rate")
        void foodReturnsGeneralRate() {
            assertEquals(rates.getGeneralCpi(), rates.getRateForCategory(ExpenseCategory.FOOD));
        }

        @Test
        @DisplayName("TRAVEL should return general CPI rate")
        void travelReturnsGeneralRate() {
            assertEquals(rates.getGeneralCpi(), rates.getRateForCategory(ExpenseCategory.TRAVEL));
        }

        @Test
        @DisplayName("DEBT_PAYMENTS should return zero")
        void debtPaymentsReturnsZero() {
            assertEquals(BigDecimal.ZERO, rates.getRateForCategory(ExpenseCategory.DEBT_PAYMENTS));
        }

        @Test
        @DisplayName("Null category should return general CPI")
        void nullCategoryReturnsGeneralCpi() {
            assertEquals(rates.getGeneralCpi(), rates.getRateForCategory(null));
        }
    }

    @Nested
    @DisplayName("Get Rate For Inflation Type Tests")
    class GetRateForInflationTypeTests {

        @Test
        @DisplayName("GENERAL should return general CPI rate")
        void generalReturnsGeneralCpi() {
            assertEquals(rates.getGeneralCpi(), rates.getRateForInflationType(InflationType.GENERAL));
        }

        @Test
        @DisplayName("HEALTHCARE should return healthcare rate")
        void healthcareReturnsHealthcare() {
            assertEquals(rates.getHealthcare(), rates.getRateForInflationType(InflationType.HEALTHCARE));
        }

        @Test
        @DisplayName("HOUSING should return housing rate")
        void housingReturnsHousing() {
            assertEquals(rates.getHousing(), rates.getRateForInflationType(InflationType.HOUSING));
        }

        @Test
        @DisplayName("LTC should return LTC rate")
        void ltcReturnsLtc() {
            assertEquals(rates.getLtc(), rates.getRateForInflationType(InflationType.LTC));
        }

        @Test
        @DisplayName("NONE should return zero")
        void noneReturnsZero() {
            assertEquals(BigDecimal.ZERO, rates.getRateForInflationType(InflationType.NONE));
        }

        @Test
        @DisplayName("Null inflation type should return general CPI")
        void nullReturnsGeneralCpi() {
            assertEquals(rates.getGeneralCpi(), rates.getRateForInflationType(null));
        }
    }

    @Nested
    @DisplayName("Category Override Tests")
    class CategoryOverrideTests {

        @Test
        @DisplayName("Should allow setting category override")
        void setOverride() {
            BigDecimal customRate = new BigDecimal("0.04");
            rates.setOverride(ExpenseCategory.TRAVEL, customRate);
            assertEquals(customRate, rates.getRateForCategory(ExpenseCategory.TRAVEL));
        }

        @Test
        @DisplayName("Override should take precedence over default")
        void overrideTakesPrecedence() {
            BigDecimal customRate = new BigDecimal("0.08");
            rates.setOverride(ExpenseCategory.HOUSING, customRate);

            // Should return custom rate, not default housing rate
            assertEquals(customRate, rates.getRateForCategory(ExpenseCategory.HOUSING));
        }

        @Test
        @DisplayName("Should allow removing override")
        void removeOverride() {
            BigDecimal customRate = new BigDecimal("0.04");
            rates.setOverride(ExpenseCategory.TRAVEL, customRate);
            assertEquals(customRate, rates.getRateForCategory(ExpenseCategory.TRAVEL));

            rates.removeOverride(ExpenseCategory.TRAVEL);
            // Should fall back to general CPI
            assertEquals(rates.getGeneralCpi(), rates.getRateForCategory(ExpenseCategory.TRAVEL));
        }

        @Test
        @DisplayName("Setting null override should not add to map")
        void nullOverrideNotAdded() {
            rates.setOverride(ExpenseCategory.TRAVEL, null);
            // Should return default rate
            assertEquals(rates.getGeneralCpi(), rates.getRateForCategory(ExpenseCategory.TRAVEL));
        }

        @Test
        @DisplayName("Setting override for null category should not throw")
        void nullCategoryOverrideNoOp() {
            rates.setOverride(null, new BigDecimal("0.05"));
            // Should not throw, just be a no-op
        }

        @Test
        @DisplayName("Removing override for null category should not throw")
        void removeNullCategoryNoOp() {
            rates.removeOverride(null);
            // Should not throw, just be a no-op
        }

        @Test
        @DisplayName("Category overrides map should be initialized empty")
        void overridesMapInitializedEmpty() {
            assertNotNull(rates.getCategoryOverrides());
            assertEquals(0, rates.getCategoryOverrides().size());
        }
    }
}
