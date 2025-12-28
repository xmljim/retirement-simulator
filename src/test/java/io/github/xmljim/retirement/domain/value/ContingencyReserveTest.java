package io.github.xmljim.retirement.domain.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.ContingencyType;
import io.github.xmljim.retirement.domain.exception.ValidationException;

@DisplayName("ContingencyReserve Tests")
class ContingencyReserveTest {

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Build basic reserve")
        void buildBasicReserve() {
            ContingencyReserve reserve = ContingencyReserve.builder()
                .name("Emergency Fund")
                .type(ContingencyType.EMERGENCY_FUND)
                .targetAmount(10000)
                .annualContribution(2000)
                .build();

            assertEquals("Emergency Fund", reserve.getName());
            assertEquals(ContingencyType.EMERGENCY_FUND, reserve.getType());
            assertEquals(0, new BigDecimal("10000").compareTo(reserve.getTargetAmount()));
        }

        @Test
        @DisplayName("Negative target throws exception")
        void negativeTargetThrows() {
            assertThrows(ValidationException.class, () ->
                ContingencyReserve.builder()
                    .targetAmount(-1000)
                    .build()
            );
        }
    }

    @Nested
    @DisplayName("Funding Tests")
    class FundingTests {

        @Test
        @DisplayName("isFullyFunded when balance >= target")
        void isFullyFunded() {
            ContingencyReserve reserve = ContingencyReserve.builder()
                .targetAmount(5000)
                .currentBalance(5000)
                .build();

            assertTrue(reserve.isFullyFunded());
            assertEquals(0, BigDecimal.ZERO.compareTo(reserve.getDeficit()));
        }

        @Test
        @DisplayName("Deficit calculation")
        void deficitCalculation() {
            ContingencyReserve reserve = ContingencyReserve.builder()
                .targetAmount(10000)
                .currentBalance(3000)
                .build();

            assertFalse(reserve.isFullyFunded());
            assertEquals(0, new BigDecimal("7000").compareTo(reserve.getDeficit()));
        }

        @Test
        @DisplayName("Funding percentage calculation")
        void fundingPercentage() {
            ContingencyReserve reserve = ContingencyReserve.builder()
                .targetAmount(10000)
                .currentBalance(2500)
                .build();

            assertEquals(0, new BigDecimal("0.2500").compareTo(reserve.getFundingPercentage()));
        }
    }

    @Nested
    @DisplayName("Contribution Tests")
    class ContributionTests {

        @Test
        @DisplayName("Monthly contribution calculation")
        void monthlyContribution() {
            ContingencyReserve reserve = ContingencyReserve.builder()
                .annualContribution(6000)
                .build();

            assertEquals(0, new BigDecimal("500.00").compareTo(reserve.getMonthlyContribution()));
        }

        @Test
        @DisplayName("withContribution adds to balance")
        void withContribution() {
            ContingencyReserve reserve = ContingencyReserve.builder()
                .currentBalance(1000)
                .build();

            ContingencyReserve updated = reserve.withContribution(new BigDecimal("500"));

            assertEquals(0, new BigDecimal("1500").compareTo(updated.getCurrentBalance()));
        }
    }

    @Nested
    @DisplayName("Expense Tests")
    class ExpenseTests {

        @Test
        @DisplayName("withExpense deducts from balance")
        void withExpense() {
            ContingencyReserve reserve = ContingencyReserve.builder()
                .currentBalance(5000)
                .build();

            ContingencyReserve updated = reserve.withExpense(new BigDecimal("2000"));

            assertEquals(0, new BigDecimal("3000").compareTo(updated.getCurrentBalance()));
        }

        @Test
        @DisplayName("withExpense floors at zero")
        void withExpenseFloorsAtZero() {
            ContingencyReserve reserve = ContingencyReserve.builder()
                .currentBalance(1000)
                .build();

            ContingencyReserve updated = reserve.withExpense(new BigDecimal("5000"));

            assertEquals(0, BigDecimal.ZERO.compareTo(updated.getCurrentBalance()));
        }

        @Test
        @DisplayName("getCoverableAmount returns min of expense and balance")
        void getCoverableAmount() {
            ContingencyReserve reserve = ContingencyReserve.builder()
                .currentBalance(3000)
                .build();

            assertEquals(0, new BigDecimal("2000").compareTo(
                reserve.getCoverableAmount(new BigDecimal("2000"))));
            assertEquals(0, new BigDecimal("3000").compareTo(
                reserve.getCoverableAmount(new BigDecimal("5000"))));
        }
    }

    @Nested
    @DisplayName("Asset Value Reserve Tests")
    class AssetValueTests {

        @Test
        @DisplayName("Create reserve from home value")
        void createFromHomeValue() {
            ContingencyReserve reserve = ContingencyReserve.ofAssetValue(
                "Home Repair",
                ContingencyType.HOME_REPAIR,
                new BigDecimal("400000"),
                new BigDecimal("0.015")
            );

            assertEquals(0, new BigDecimal("6000.00").compareTo(reserve.getTargetAmount()));
            assertEquals(0, new BigDecimal("6000.00").compareTo(reserve.getAnnualContribution()));
        }
    }
}
