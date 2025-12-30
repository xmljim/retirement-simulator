package io.github.xmljim.retirement.domain.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.calculator.StubSimulationView;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;

@DisplayName("AccountWithdrawal Tests")
class AccountWithdrawalTest {

    private AccountSnapshot traditional401k;
    private AccountSnapshot traditionalIra;
    private AccountSnapshot rothIra;
    private AccountSnapshot hsa;

    @BeforeEach
    void setUp() {
        traditional401k = StubSimulationView.createTestAccount(
                "Traditional 401k", AccountType.TRADITIONAL_401K, new BigDecimal("100000.00"));

        traditionalIra = StubSimulationView.createTestAccount(
                "Traditional IRA", AccountType.TRADITIONAL_IRA, new BigDecimal("50000.00"));

        rothIra = StubSimulationView.createTestAccount(
                "Roth IRA", AccountType.ROTH_IRA, new BigDecimal("75000.00"));

        hsa = StubSimulationView.createTestAccount(
                "HSA", AccountType.HSA, new BigDecimal("20000.00"));
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should create valid withdrawal with builder")
        void createsValidWithdrawal() {
            AccountWithdrawal withdrawal = AccountWithdrawal.builder()
                    .accountSnapshot(traditional401k)
                    .amount(new BigDecimal("5000.00"))
                    .priorBalance(new BigDecimal("100000.00"))
                    .newBalance(new BigDecimal("95000.00"))
                    .build();

            assertEquals(traditional401k.accountId(), withdrawal.accountId());
            assertEquals("Traditional 401k", withdrawal.accountName());
            assertEquals(AccountType.TRADITIONAL_401K, withdrawal.accountType());
            assertEquals(0, new BigDecimal("5000.00").compareTo(withdrawal.amount()));
            assertEquals(0, new BigDecimal("100000.00").compareTo(withdrawal.priorBalance()));
            assertEquals(0, new BigDecimal("95000.00").compareTo(withdrawal.newBalance()));
        }

        @Test
        @DisplayName("Should default tax treatment from account type")
        void defaultsTaxTreatment() {
            AccountWithdrawal withdrawal = AccountWithdrawal.builder()
                    .accountSnapshot(traditionalIra)
                    .amount(new BigDecimal("1000.00"))
                    .build();

            assertEquals(AccountType.TaxTreatment.PRE_TAX, withdrawal.taxTreatment());
        }

        @Test
        @DisplayName("Should allow override of tax treatment")
        void overridesTaxTreatment() {
            AccountWithdrawal withdrawal = AccountWithdrawal.builder()
                    .accountSnapshot(traditionalIra)
                    .amount(new BigDecimal("1000.00"))
                    .taxTreatment(AccountType.TaxTreatment.ROTH)
                    .build();

            assertEquals(AccountType.TaxTreatment.ROTH, withdrawal.taxTreatment());
        }

        @Test
        @DisplayName("Should default amounts to zero")
        void defaultsAmountsToZero() {
            AccountWithdrawal withdrawal = AccountWithdrawal.builder()
                    .accountSnapshot(rothIra)
                    .build();

            assertEquals(0, BigDecimal.ZERO.compareTo(withdrawal.amount()));
            assertEquals(0, BigDecimal.ZERO.compareTo(withdrawal.priorBalance()));
            assertEquals(0, BigDecimal.ZERO.compareTo(withdrawal.newBalance()));
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw for null account snapshot")
        void nullAccountThrows() {
            assertThrows(MissingRequiredFieldException.class, () ->
                    AccountWithdrawal.builder()
                            .amount(new BigDecimal("1000.00"))
                            .build());
        }
    }

    @Nested
    @DisplayName("Tax Treatment Tests")
    class TaxTreatmentTests {

        @Test
        @DisplayName("Pre-tax withdrawals should be taxable")
        void preTaxIsTaxable() {
            AccountWithdrawal withdrawal = AccountWithdrawal.builder()
                    .accountSnapshot(traditional401k)
                    .amount(new BigDecimal("5000.00"))
                    .build();

            assertTrue(withdrawal.isTaxable());
        }

        @Test
        @DisplayName("Roth withdrawals should not be taxable")
        void rothNotTaxable() {
            AccountWithdrawal withdrawal = AccountWithdrawal.builder()
                    .accountSnapshot(rothIra)
                    .amount(new BigDecimal("5000.00"))
                    .build();

            assertFalse(withdrawal.isTaxable());
        }

        @Test
        @DisplayName("HSA withdrawals should not be taxable")
        void hsaNotTaxable() {
            AccountWithdrawal withdrawal = AccountWithdrawal.builder()
                    .accountSnapshot(hsa)
                    .amount(new BigDecimal("1000.00"))
                    .build();

            assertFalse(withdrawal.isTaxable());
        }
    }

    @Nested
    @DisplayName("Depletion Tests")
    class DepletionTests {

        @Test
        @DisplayName("Should detect depleted account")
        void detectsDepleted() {
            AccountWithdrawal withdrawal = AccountWithdrawal.builder()
                    .accountSnapshot(traditionalIra)
                    .amount(new BigDecimal("10000.00"))
                    .priorBalance(new BigDecimal("10000.00"))
                    .newBalance(BigDecimal.ZERO)
                    .build();

            assertTrue(withdrawal.isDepleted());
        }

        @Test
        @DisplayName("Should detect non-depleted account")
        void detectsNotDepleted() {
            AccountWithdrawal withdrawal = AccountWithdrawal.builder()
                    .accountSnapshot(traditionalIra)
                    .amount(new BigDecimal("5000.00"))
                    .priorBalance(new BigDecimal("10000.00"))
                    .newBalance(new BigDecimal("5000.00"))
                    .build();

            assertFalse(withdrawal.isDepleted());
        }

        @Test
        @DisplayName("Should detect partial withdrawal")
        void detectsPartialWithdrawal() {
            // Requested $10000 but only $7000 available
            AccountWithdrawal withdrawal = AccountWithdrawal.builder()
                    .accountSnapshot(traditionalIra)
                    .amount(new BigDecimal("10000.00"))
                    .priorBalance(new BigDecimal("7000.00"))
                    .newBalance(BigDecimal.ZERO)
                    .build();

            assertTrue(withdrawal.isPartial());
            assertTrue(withdrawal.isDepleted());
        }

        @Test
        @DisplayName("Should not flag full withdrawal as partial")
        void fullWithdrawalNotPartial() {
            AccountWithdrawal withdrawal = AccountWithdrawal.builder()
                    .accountSnapshot(traditionalIra)
                    .amount(new BigDecimal("10000.00"))
                    .priorBalance(new BigDecimal("10000.00"))
                    .newBalance(BigDecimal.ZERO)
                    .build();

            assertFalse(withdrawal.isPartial());
            assertTrue(withdrawal.isDepleted());
        }
    }

    @Nested
    @DisplayName("Account Reference Tests")
    class AccountReferenceTests {

        @Test
        @DisplayName("Should provide access to underlying account snapshot")
        void providesAccountReference() {
            AccountWithdrawal withdrawal = AccountWithdrawal.builder()
                    .accountSnapshot(traditional401k)
                    .amount(new BigDecimal("5000.00"))
                    .build();

            assertEquals(traditional401k, withdrawal.accountSnapshot());
            assertEquals(traditional401k.accountId(), withdrawal.accountId());
            assertEquals(traditional401k.accountName(), withdrawal.accountName());
            assertEquals(traditional401k.accountType(), withdrawal.accountType());
        }
    }
}
