package io.github.xmljim.retirement.domain.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AssetAllocation Tests")
class AssetAllocationTest {

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create allocation with of() method")
        void createWithOfMethod() {
            AssetAllocation allocation = AssetAllocation.of(60, 30, 10);

            assertEquals(new BigDecimal("60.0000"), allocation.getStocksPercentage());
            assertEquals(new BigDecimal("30.0000"), allocation.getBondsPercentage());
            assertEquals(new BigDecimal("10.0000"), allocation.getCashPercentage());
        }

        @Test
        @DisplayName("Should create all-stocks allocation")
        void createAllStocks() {
            AssetAllocation allocation = AssetAllocation.allStocks();

            assertEquals(0, allocation.getStocksPercentage().compareTo(new BigDecimal("100")));
            assertEquals(0, allocation.getBondsPercentage().compareTo(BigDecimal.ZERO));
            assertEquals(0, allocation.getCashPercentage().compareTo(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Should create all-bonds allocation")
        void createAllBonds() {
            AssetAllocation allocation = AssetAllocation.allBonds();

            assertEquals(0, allocation.getStocksPercentage().compareTo(BigDecimal.ZERO));
            assertEquals(0, allocation.getBondsPercentage().compareTo(new BigDecimal("100")));
            assertEquals(0, allocation.getCashPercentage().compareTo(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Should create all-cash allocation")
        void createAllCash() {
            AssetAllocation allocation = AssetAllocation.allCash();

            assertEquals(0, allocation.getStocksPercentage().compareTo(BigDecimal.ZERO));
            assertEquals(0, allocation.getBondsPercentage().compareTo(BigDecimal.ZERO));
            assertEquals(0, allocation.getCashPercentage().compareTo(new BigDecimal("100")));
        }

        @Test
        @DisplayName("Should create balanced 60/40 allocation")
        void createBalanced() {
            AssetAllocation allocation = AssetAllocation.balanced();

            assertEquals(0, allocation.getStocksPercentage().compareTo(new BigDecimal("60")));
            assertEquals(0, allocation.getBondsPercentage().compareTo(new BigDecimal("40")));
            assertEquals(0, allocation.getCashPercentage().compareTo(BigDecimal.ZERO));
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should build allocation with builder")
        void buildWithBuilder() {
            AssetAllocation allocation = AssetAllocation.builder()
                    .stocks(70)
                    .bonds(20)
                    .cash(10)
                    .build();

            assertEquals(0, allocation.getStocksPercentage().compareTo(new BigDecimal("70")));
            assertEquals(0, allocation.getBondsPercentage().compareTo(new BigDecimal("20")));
            assertEquals(0, allocation.getCashPercentage().compareTo(new BigDecimal("10")));
        }

        @Test
        @DisplayName("Should support decimal percentages")
        void decimalPercentages() {
            AssetAllocation allocation = AssetAllocation.of(33.33, 33.33, 33.34);

            BigDecimal total = allocation.getStocksPercentage()
                    .add(allocation.getBondsPercentage())
                    .add(allocation.getCashPercentage());

            assertEquals(0, total.compareTo(new BigDecimal("100")));
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw exception when percentages don't sum to 100")
        void invalidSum() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> AssetAllocation.of(50, 30, 10)
            );

            assertTrue(exception.getMessage().contains("sum to 100%"));
        }

        @Test
        @DisplayName("Should throw exception for negative stocks percentage")
        void negativeStocks() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> AssetAllocation.of(-10, 60, 50)
            );

            assertTrue(exception.getMessage().contains("Stocks"));
            assertTrue(exception.getMessage().contains("between 0 and 100"));
        }

        @Test
        @DisplayName("Should throw exception for percentage over 100")
        void overOneHundred() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> AssetAllocation.builder().stocks(110).bonds(0).cash(-10).build()
            );

            assertTrue(exception.getMessage().contains("between 0 and 100"));
        }
    }

    @Nested
    @DisplayName("Blended Return Calculation Tests")
    class BlendedReturnTests {

        @Test
        @DisplayName("Should calculate blended return correctly")
        void calculateBlendedReturn() {
            AssetAllocation allocation = AssetAllocation.of(60, 30, 10);

            BigDecimal blended = allocation.calculateBlendedReturn(
                    new BigDecimal("0.07"),
                    new BigDecimal("0.04"),
                    new BigDecimal("0.02")
            );

            // 60% * 0.07 + 30% * 0.04 + 10% * 0.02 = 0.042 + 0.012 + 0.002 = 0.056
            assertEquals(0, blended.compareTo(new BigDecimal("0.056")));
        }

        @Test
        @DisplayName("Should return stock return for all-stocks allocation")
        void allStocksReturn() {
            AssetAllocation allocation = AssetAllocation.allStocks();

            BigDecimal blended = allocation.calculateBlendedReturn(
                    new BigDecimal("0.10"),
                    new BigDecimal("0.05"),
                    new BigDecimal("0.02")
            );

            assertEquals(0, blended.compareTo(new BigDecimal("0.10")));
        }

        @Test
        @DisplayName("Should return bond return for all-bonds allocation")
        void allBondsReturn() {
            AssetAllocation allocation = AssetAllocation.allBonds();

            BigDecimal blended = allocation.calculateBlendedReturn(
                    new BigDecimal("0.10"),
                    new BigDecimal("0.05"),
                    new BigDecimal("0.02")
            );

            assertEquals(0, blended.compareTo(new BigDecimal("0.05")));
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("Equal allocations should be equal")
        void equalAllocations() {
            AssetAllocation a1 = AssetAllocation.of(60, 30, 10);
            AssetAllocation a2 = AssetAllocation.of(60, 30, 10);

            assertEquals(a1, a2);
            assertEquals(a1.hashCode(), a2.hashCode());
        }

        @Test
        @DisplayName("Different allocations should not be equal")
        void differentAllocations() {
            AssetAllocation a1 = AssetAllocation.of(60, 30, 10);
            AssetAllocation a2 = AssetAllocation.of(70, 20, 10);

            assertNotEquals(a1, a2);
        }

        @Test
        @DisplayName("Factory methods with same values should be equal")
        void factoryMethodsEqual() {
            AssetAllocation balanced1 = AssetAllocation.balanced();
            AssetAllocation balanced2 = AssetAllocation.of(60, 40, 0);

            assertEquals(balanced1, balanced2);
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should produce readable string")
        void toStringFormat() {
            AssetAllocation allocation = AssetAllocation.of(60, 30, 10);
            String str = allocation.toString();

            assertTrue(str.contains("60"));
            assertTrue(str.contains("30"));
            assertTrue(str.contains("10"));
            assertTrue(str.contains("stocks"));
            assertTrue(str.contains("bonds"));
            assertTrue(str.contains("cash"));
        }
    }
}
