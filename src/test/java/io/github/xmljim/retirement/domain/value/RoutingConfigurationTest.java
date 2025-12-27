package io.github.xmljim.retirement.domain.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.exception.ValidationException;

@DisplayName("RoutingConfiguration Tests")
class RoutingConfigurationTest {

    @Nested
    @DisplayName("Construction")
    class ConstructionTests {

        @Test
        @DisplayName("Should create single account configuration")
        void singleAccountConfig() {
            RoutingConfiguration config = RoutingConfiguration.singleAccount("account-1");

            assertEquals(1, config.size());
            assertTrue(config.hasRules());
            assertEquals(BigDecimal.ONE, config.getTotalPercentage());
        }

        @Test
        @DisplayName("Should create split configuration with builder")
        void splitConfigWithBuilder() {
            RoutingConfiguration config = RoutingConfiguration.builder()
                .addRule("account-1", 0.80, 1)
                .addRule("account-2", 0.20, 2)
                .build();

            assertEquals(2, config.size());
            assertEquals(0, BigDecimal.ONE.compareTo(config.getTotalPercentage()));
        }

        @Test
        @DisplayName("Should create with percentage helper method")
        void percentageHelperMethod() {
            RoutingConfiguration config = RoutingConfiguration.builder()
                .addRulePercent("account-1", 60.0, 1)
                .addRulePercent("account-2", 40.0, 2)
                .build();

            assertEquals(2, config.size());
            assertEquals(0, BigDecimal.ONE.compareTo(config.getTotalPercentage()));
        }
    }

    @Nested
    @DisplayName("Priority Ordering")
    class PriorityTests {

        @Test
        @DisplayName("Should sort rules by priority")
        void rulesSortedByPriority() {
            RoutingConfiguration config = RoutingConfiguration.builder()
                .addRule("low-priority", 0.20, 3)
                .addRule("high-priority", 0.50, 1)
                .addRule("mid-priority", 0.30, 2)
                .build();

            List<RoutingRule> rules = config.getRulesByPriority();

            assertEquals("high-priority", rules.get(0).accountId());
            assertEquals("mid-priority", rules.get(1).accountId());
            assertEquals("low-priority", rules.get(2).accountId());
        }
    }

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        @DisplayName("Should throw for empty rules")
        void emptyRulesThrows() {
            assertThrows(ValidationException.class, () ->
                RoutingConfiguration.builder().build()
            );
        }

        @Test
        @DisplayName("Should throw when percentages don't sum to 100%")
        void percentagesNotSumTo100Throws() {
            assertThrows(ValidationException.class, () ->
                RoutingConfiguration.builder()
                    .addRule("account-1", 0.50, 1)
                    .addRule("account-2", 0.30, 2)
                    .build()
            );
        }

        @Test
        @DisplayName("Should throw when percentages exceed 100%")
        void percentagesExceed100Throws() {
            assertThrows(ValidationException.class, () ->
                RoutingConfiguration.builder()
                    .addRule("account-1", 0.80, 1)
                    .addRule("account-2", 0.30, 2)
                    .build()
            );
        }

        @Test
        @DisplayName("Should allow small rounding tolerance")
        void allowsSmallRoundingTolerance() {
            // 0.333... + 0.333... + 0.334 should work
            RoutingConfiguration config = RoutingConfiguration.builder()
                .addRule("account-1", 0.3333, 1)
                .addRule("account-2", 0.3333, 2)
                .addRule("account-3", 0.3334, 3)
                .build();

            assertEquals(3, config.size());
        }
    }

    @Nested
    @DisplayName("Immutability")
    class ImmutabilityTests {

        @Test
        @DisplayName("Rules list should be unmodifiable")
        void rulesListUnmodifiable() {
            RoutingConfiguration config = RoutingConfiguration.singleAccount("account-1");

            assertThrows(UnsupportedOperationException.class, () ->
                config.getRules().add(new RoutingRule("account-2", new BigDecimal("0.50"), 2))
            );
        }
    }
}
