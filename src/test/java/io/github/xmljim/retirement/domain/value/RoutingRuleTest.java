package io.github.xmljim.retirement.domain.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;

@DisplayName("RoutingRule Tests")
class RoutingRuleTest {

    @Nested
    @DisplayName("Construction")
    class ConstructionTests {

        @Test
        @DisplayName("Should create valid routing rule")
        void validRoutingRule() {
            RoutingRule rule = new RoutingRule("account-1", new BigDecimal("0.80"), 1);

            assertEquals("account-1", rule.accountId());
            assertEquals(new BigDecimal("0.80"), rule.percentage());
            assertEquals(1, rule.priority());
        }

        @Test
        @DisplayName("Should create rule from percentage value")
        void fromPercentValue() {
            RoutingRule rule = RoutingRule.ofPercent("account-1", 80.0, 1);

            assertEquals("account-1", rule.accountId());
            assertEquals(0, new BigDecimal("0.80").compareTo(rule.percentage()));
            assertEquals(1, rule.priority());
        }

        @Test
        @DisplayName("Should create single account rule")
        void singleAccountRule() {
            RoutingRule rule = RoutingRule.singleAccount("my-account");

            assertEquals("my-account", rule.accountId());
            assertEquals(BigDecimal.ONE, rule.percentage());
            assertEquals(1, rule.priority());
        }
    }

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        @DisplayName("Should throw for null accountId")
        void nullAccountIdThrows() {
            assertThrows(MissingRequiredFieldException.class, () ->
                new RoutingRule(null, new BigDecimal("0.50"), 1)
            );
        }

        @Test
        @DisplayName("Should throw for blank accountId")
        void blankAccountIdThrows() {
            assertThrows(MissingRequiredFieldException.class, () ->
                new RoutingRule("  ", new BigDecimal("0.50"), 1)
            );
        }

        @Test
        @DisplayName("Should throw for null percentage")
        void nullPercentageThrows() {
            assertThrows(MissingRequiredFieldException.class, () ->
                new RoutingRule("account-1", null, 1)
            );
        }

        @Test
        @DisplayName("Should throw for negative percentage")
        void negativePercentageThrows() {
            assertThrows(ValidationException.class, () ->
                new RoutingRule("account-1", new BigDecimal("-0.10"), 1)
            );
        }

        @Test
        @DisplayName("Should throw for percentage greater than 1")
        void percentageOverOneThrows() {
            assertThrows(ValidationException.class, () ->
                new RoutingRule("account-1", new BigDecimal("1.50"), 1)
            );
        }

        @Test
        @DisplayName("Should throw for negative priority")
        void negativePriorityThrows() {
            assertThrows(ValidationException.class, () ->
                new RoutingRule("account-1", new BigDecimal("0.50"), -1)
            );
        }

        @Test
        @DisplayName("Should allow zero percentage")
        void zeroPercentageAllowed() {
            RoutingRule rule = new RoutingRule("account-1", BigDecimal.ZERO, 1);
            assertEquals(BigDecimal.ZERO, rule.percentage());
        }

        @Test
        @DisplayName("Should allow 100% percentage")
        void oneHundredPercentAllowed() {
            RoutingRule rule = new RoutingRule("account-1", BigDecimal.ONE, 1);
            assertEquals(BigDecimal.ONE, rule.percentage());
        }
    }
}
