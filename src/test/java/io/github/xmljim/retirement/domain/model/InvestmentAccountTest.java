package io.github.xmljim.retirement.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.exception.InvalidRateException;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;
import io.github.xmljim.retirement.domain.value.AssetAllocation;

@DisplayName("InvestmentAccount Tests")
class InvestmentAccountTest {

    private static final String ACCOUNT_NAME = "My 401(k)";
    private static final AccountType ACCOUNT_TYPE = AccountType.TRADITIONAL_401K;
    private static final BigDecimal BALANCE = new BigDecimal("250000");
    private static final BigDecimal PRE_RETIREMENT_RETURN = new BigDecimal("0.07");
    private static final BigDecimal POST_RETIREMENT_RETURN = new BigDecimal("0.05");

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should create account with required fields")
        void createWithRequiredFields() {
            InvestmentAccount account = InvestmentAccount.builder()
                    .name(ACCOUNT_NAME)
                    .accountType(ACCOUNT_TYPE)
                    .balance(BALANCE)
                    .preRetirementReturnRate(PRE_RETIREMENT_RETURN)
                    .build();

            assertEquals(ACCOUNT_NAME, account.getName());
            assertEquals(ACCOUNT_TYPE, account.getAccountType());
            assertEquals(0, BALANCE.compareTo(account.getBalance()));
            assertNotNull(account.getId());
            assertNotNull(account.getAllocation());
        }

        @Test
        @DisplayName("Should use provided ID when specified")
        void usesProvidedId() {
            String customId = "custom-account-id";
            InvestmentAccount account = createDefaultAccount()
                    .id(customId)
                    .build();

            assertEquals(customId, account.getId());
        }

        @Test
        @DisplayName("Should generate unique IDs when not provided")
        void generatesUniqueIds() {
            InvestmentAccount account1 = createDefaultAccount().build();
            InvestmentAccount account2 = createDefaultAccount().build();

            assertNotEquals(account1.getId(), account2.getId());
        }

        @Test
        @DisplayName("Should accept balance as double")
        void balanceAsDouble() {
            InvestmentAccount account = InvestmentAccount.builder()
                    .name(ACCOUNT_NAME)
                    .accountType(ACCOUNT_TYPE)
                    .balance(250000.50)
                    .preRetirementReturnRate(0.07)
                    .build();

            assertEquals(0, new BigDecimal("250000.50").compareTo(account.getBalance()));
        }

        @Test
        @DisplayName("Should throw exception when name is missing")
        void missingName() {
            assertThrows(MissingRequiredFieldException.class, () ->
                    InvestmentAccount.builder()
                            .accountType(ACCOUNT_TYPE)
                            .balance(BALANCE)
                            .preRetirementReturnRate(PRE_RETIREMENT_RETURN)
                            .build()
            );
        }

        @Test
        @DisplayName("Should throw exception when account type is missing")
        void missingAccountType() {
            assertThrows(MissingRequiredFieldException.class, () ->
                    InvestmentAccount.builder()
                            .name(ACCOUNT_NAME)
                            .balance(BALANCE)
                            .preRetirementReturnRate(PRE_RETIREMENT_RETURN)
                            .build()
            );
        }

        @Test
        @DisplayName("Should throw exception when return rate is missing and not using allocation-based")
        void missingReturnRate() {
            assertThrows(MissingRequiredFieldException.class, () ->
                    InvestmentAccount.builder()
                            .name(ACCOUNT_NAME)
                            .accountType(ACCOUNT_TYPE)
                            .balance(BALANCE)
                            .build()
            );
        }

        @Test
        @DisplayName("Should allow missing return rate when using allocation-based returns")
        void allocationBasedReturn() {
            InvestmentAccount account = InvestmentAccount.builder()
                    .name(ACCOUNT_NAME)
                    .accountType(ACCOUNT_TYPE)
                    .balance(BALANCE)
                    .allocation(AssetAllocation.of(60, 30, 10))
                    .useAllocationBasedReturn()
                    .build();

            assertTrue(account.isUsingAllocationBasedReturn());
            assertNotNull(account.getPreRetirementReturnRate());
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw exception for negative balance")
        void negativeBalance() {
            ValidationException exception = assertThrows(
                    ValidationException.class,
                    () -> createDefaultAccount()
                            .balance(new BigDecimal("-1000"))
                            .build()
            );

            assertTrue(exception.getMessage().contains("negative"));
        }

        @Test
        @DisplayName("Should throw exception for return rate below -50%")
        void returnRateTooLow() {
            InvalidRateException exception = assertThrows(
                    InvalidRateException.class,
                    () -> createDefaultAccount()
                            .preRetirementReturnRate(new BigDecimal("-0.60"))
                            .build()
            );

            assertTrue(exception.getMessage().contains("-50%"));
        }

        @Test
        @DisplayName("Should throw exception for return rate above 50%")
        void returnRateTooHigh() {
            InvalidRateException exception = assertThrows(
                    InvalidRateException.class,
                    () -> createDefaultAccount()
                            .preRetirementReturnRate(new BigDecimal("0.60"))
                            .build()
            );

            assertTrue(exception.getMessage().contains("50%"));
        }

        @Test
        @DisplayName("Should allow zero balance")
        void zeroBalance() {
            InvestmentAccount account = createDefaultAccount()
                    .balance(BigDecimal.ZERO)
                    .build();

            assertEquals(0, BigDecimal.ZERO.compareTo(account.getBalance()));
        }
    }

    @Nested
    @DisplayName("Return Rate Tests")
    class ReturnRateTests {

        @Test
        @DisplayName("Should return explicit pre-retirement rate")
        void explicitPreRetirementRate() {
            InvestmentAccount account = createDefaultAccount()
                    .preRetirementReturnRate(PRE_RETIREMENT_RETURN)
                    .build();

            assertEquals(0, PRE_RETIREMENT_RETURN.compareTo(account.getPreRetirementReturnRate()));
        }

        @Test
        @DisplayName("Should return explicit post-retirement rate when set")
        void explicitPostRetirementRate() {
            InvestmentAccount account = createDefaultAccount()
                    .preRetirementReturnRate(PRE_RETIREMENT_RETURN)
                    .postRetirementReturnRate(POST_RETIREMENT_RETURN)
                    .build();

            assertEquals(0, POST_RETIREMENT_RETURN.compareTo(account.getPostRetirementReturnRate()));
        }

        @Test
        @DisplayName("Should use pre-retirement rate for post-retirement when not set")
        void postRetirementDefaultsToPreRetirement() {
            InvestmentAccount account = createDefaultAccount()
                    .preRetirementReturnRate(PRE_RETIREMENT_RETURN)
                    .build();

            assertEquals(0, PRE_RETIREMENT_RETURN.compareTo(account.getPostRetirementReturnRate()));
        }

        @Test
        @DisplayName("Should calculate allocation-based return")
        void allocationBasedReturn() {
            AssetAllocation allocation = AssetAllocation.of(60, 30, 10);
            InvestmentAccount account = InvestmentAccount.builder()
                    .name(ACCOUNT_NAME)
                    .accountType(ACCOUNT_TYPE)
                    .balance(BALANCE)
                    .allocation(allocation)
                    .useAllocationBasedReturn()
                    .build();

            // 60% * 0.07 + 30% * 0.04 + 10% * 0.02 = 0.056
            BigDecimal expected = new BigDecimal("0.056");
            assertEquals(0, expected.compareTo(account.getPreRetirementReturnRate()));
        }
    }

    @Nested
    @DisplayName("Tax Treatment Tests")
    class TaxTreatmentTests {

        @Test
        @DisplayName("Traditional 401(k) should have pre-tax treatment")
        void traditional401kTaxTreatment() {
            InvestmentAccount account = createAccountWithType(AccountType.TRADITIONAL_401K);

            assertEquals(AccountType.TaxTreatment.PRE_TAX, account.getTaxTreatment());
            assertFalse(account.isTaxFreeWithdrawal());
            assertTrue(account.isSubjectToRmd());
        }

        @Test
        @DisplayName("Roth IRA should have tax-free withdrawals and no RMD")
        void rothIraTaxTreatment() {
            InvestmentAccount account = createAccountWithType(AccountType.ROTH_IRA);

            assertEquals(AccountType.TaxTreatment.ROTH, account.getTaxTreatment());
            assertTrue(account.isTaxFreeWithdrawal());
            assertFalse(account.isSubjectToRmd());
        }

        @Test
        @DisplayName("Taxable brokerage should have no tax advantages")
        void taxableBrokerageTaxTreatment() {
            InvestmentAccount account = createAccountWithType(AccountType.TAXABLE_BROKERAGE);

            assertEquals(AccountType.TaxTreatment.TAXABLE, account.getTaxTreatment());
            assertFalse(account.isTaxFreeWithdrawal());
            assertFalse(account.isSubjectToRmd());
        }
    }

    @Nested
    @DisplayName("WithBalance Tests")
    class WithBalanceTests {

        @Test
        @DisplayName("Should create new account with updated balance")
        void withBalanceCreatesNew() {
            InvestmentAccount original = createDefaultAccount().build();
            BigDecimal newBalance = new BigDecimal("300000");

            InvestmentAccount updated = original.withBalance(newBalance);

            assertEquals(0, newBalance.compareTo(updated.getBalance()));
            assertEquals(original.getId(), updated.getId());
            assertEquals(original.getName(), updated.getName());
            assertEquals(original.getAccountType(), updated.getAccountType());
        }

        @Test
        @DisplayName("Original account should be unchanged")
        void originalUnchanged() {
            InvestmentAccount original = createDefaultAccount().build();
            BigDecimal originalBalance = original.getBalance();

            original.withBalance(new BigDecimal("300000"));

            assertEquals(0, originalBalance.compareTo(original.getBalance()));
        }
    }

    @Nested
    @DisplayName("ToBuilder Tests")
    class ToBuilderTests {

        @Test
        @DisplayName("Should create builder with copied values")
        void toBuilderCopiesValues() {
            InvestmentAccount original = createDefaultAccount()
                    .postRetirementReturnRate(POST_RETIREMENT_RETURN)
                    .build();

            InvestmentAccount copy = original.toBuilder().build();

            assertEquals(original.getId(), copy.getId());
            assertEquals(original.getName(), copy.getName());
            assertEquals(original.getAccountType(), copy.getAccountType());
            assertEquals(0, original.getBalance().compareTo(copy.getBalance()));
            assertEquals(original.getAllocation(), copy.getAllocation());
        }

        @Test
        @DisplayName("Should allow modifying copied values")
        void toBuilderAllowsModification() {
            InvestmentAccount original = createDefaultAccount().build();
            InvestmentAccount modified = original.toBuilder()
                    .name("Modified Account")
                    .build();

            assertEquals("Modified Account", modified.getName());
            assertEquals(original.getAccountType(), modified.getAccountType());
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("Accounts with same ID should be equal")
        void sameIdEquals() {
            InvestmentAccount account1 = InvestmentAccount.builder()
                    .id("same-id")
                    .name(ACCOUNT_NAME)
                    .accountType(ACCOUNT_TYPE)
                    .balance(BALANCE)
                    .preRetirementReturnRate(PRE_RETIREMENT_RETURN)
                    .build();

            InvestmentAccount account2 = InvestmentAccount.builder()
                    .id("same-id")
                    .name("Different Name")
                    .accountType(AccountType.ROTH_IRA)
                    .balance(new BigDecimal("100000"))
                    .preRetirementReturnRate(new BigDecimal("0.05"))
                    .build();

            assertEquals(account1, account2);
            assertEquals(account1.hashCode(), account2.hashCode());
        }

        @Test
        @DisplayName("Accounts with different IDs should not be equal")
        void differentIdNotEquals() {
            InvestmentAccount account1 = createDefaultAccount().build();
            InvestmentAccount account2 = createDefaultAccount().build();

            assertNotEquals(account1, account2);
        }
    }

    private InvestmentAccount.Builder createDefaultAccount() {
        return InvestmentAccount.builder()
                .name(ACCOUNT_NAME)
                .accountType(ACCOUNT_TYPE)
                .balance(BALANCE)
                .preRetirementReturnRate(PRE_RETIREMENT_RETURN);
    }

    private InvestmentAccount createAccountWithType(AccountType type) {
        return InvestmentAccount.builder()
                .name("Test Account")
                .accountType(type)
                .balance(BALANCE)
                .preRetirementReturnRate(PRE_RETIREMENT_RETURN)
                .build();
    }
}
