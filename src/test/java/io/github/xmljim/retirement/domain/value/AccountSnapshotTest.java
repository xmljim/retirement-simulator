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

import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.model.InvestmentAccount;

@DisplayName("AccountSnapshot Tests")
class AccountSnapshotTest {

    private InvestmentAccount traditional401k;
    private InvestmentAccount rothIra;
    private InvestmentAccount hsa;
    private InvestmentAccount taxableBrokerage;
    private AssetAllocation defaultAllocation;

    @BeforeEach
    void setUp() {
        defaultAllocation = AssetAllocation.of(60, 35, 5);

        traditional401k = InvestmentAccount.builder()
                .name("My 401(k)")
                .accountType(AccountType.TRADITIONAL_401K)
                .balance(new BigDecimal("500000.00"))
                .allocation(defaultAllocation)
                .preRetirementReturnRate(0.07)
                .build();

        rothIra = InvestmentAccount.builder()
                .name("Roth IRA")
                .accountType(AccountType.ROTH_IRA)
                .balance(new BigDecimal("150000.00"))
                .allocation(AssetAllocation.of(70, 25, 5))
                .preRetirementReturnRate(0.08)
                .build();

        hsa = InvestmentAccount.builder()
                .name("Health Savings")
                .accountType(AccountType.HSA)
                .balance(new BigDecimal("25000.00"))
                .allocation(AssetAllocation.of(50, 40, 10))
                .preRetirementReturnRate(0.06)
                .build();

        taxableBrokerage = InvestmentAccount.builder()
                .name("Brokerage")
                .accountType(AccountType.TAXABLE_BROKERAGE)
                .balance(new BigDecimal("100000.00"))
                .allocation(AssetAllocation.of(80, 15, 5))
                .preRetirementReturnRate(0.07)
                .build();
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create snapshot from Traditional 401k")
        void createsFromTraditional401k() {
            AccountSnapshot snapshot = AccountSnapshot.from(traditional401k);

            assertEquals(traditional401k.getId(), snapshot.accountId());
            assertEquals("My 401(k)", snapshot.accountName());
            assertEquals(AccountType.TRADITIONAL_401K, snapshot.accountType());
            assertEquals(0, new BigDecimal("500000.00").compareTo(snapshot.balance()));
            assertEquals(AccountType.TaxTreatment.PRE_TAX, snapshot.taxTreatment());
            assertTrue(snapshot.subjectToRmd());
            assertEquals(defaultAllocation, snapshot.allocation());
        }

        @Test
        @DisplayName("Should create snapshot from Roth IRA")
        void createsFromRothIra() {
            AccountSnapshot snapshot = AccountSnapshot.from(rothIra);

            assertEquals(AccountType.ROTH_IRA, snapshot.accountType());
            assertEquals(AccountType.TaxTreatment.ROTH, snapshot.taxTreatment());
            assertFalse(snapshot.subjectToRmd());
        }

        @Test
        @DisplayName("Should create snapshot from HSA")
        void createsFromHsa() {
            AccountSnapshot snapshot = AccountSnapshot.from(hsa);

            assertEquals(AccountType.HSA, snapshot.accountType());
            assertEquals(AccountType.TaxTreatment.HSA, snapshot.taxTreatment());
            assertFalse(snapshot.subjectToRmd());
        }

        @Test
        @DisplayName("Should create snapshot from Taxable Brokerage")
        void createsFromTaxableBrokerage() {
            AccountSnapshot snapshot = AccountSnapshot.from(taxableBrokerage);

            assertEquals(AccountType.TAXABLE_BROKERAGE, snapshot.accountType());
            assertEquals(AccountType.TaxTreatment.TAXABLE, snapshot.taxTreatment());
            assertFalse(snapshot.subjectToRmd());
        }

        @Test
        @DisplayName("Should throw for null account")
        void throwsForNullAccount() {
            assertThrows(MissingRequiredFieldException.class, () ->
                    AccountSnapshot.from(null));
        }
    }

    @Nested
    @DisplayName("Direct Construction Tests")
    class DirectConstructionTests {

        @Test
        @DisplayName("Should create valid snapshot with constructor")
        void createsWithConstructor() {
            AccountSnapshot snapshot = new AccountSnapshot(
                    "test-id",
                    "Test Account",
                    AccountType.TRADITIONAL_IRA,
                    new BigDecimal("100000"),
                    AccountType.TaxTreatment.PRE_TAX,
                    true,
                    defaultAllocation
            );

            assertEquals("test-id", snapshot.accountId());
            assertEquals("Test Account", snapshot.accountName());
            assertEquals(AccountType.TRADITIONAL_IRA, snapshot.accountType());
            assertTrue(snapshot.subjectToRmd());
        }

        @Test
        @DisplayName("Should throw for null accountId")
        void throwsForNullAccountId() {
            assertThrows(MissingRequiredFieldException.class, () ->
                    new AccountSnapshot(
                            null,
                            "Test",
                            AccountType.TRADITIONAL_IRA,
                            BigDecimal.ZERO,
                            AccountType.TaxTreatment.PRE_TAX,
                            true,
                            defaultAllocation
                    ));
        }

        @Test
        @DisplayName("Should throw for null accountName")
        void throwsForNullAccountName() {
            assertThrows(MissingRequiredFieldException.class, () ->
                    new AccountSnapshot(
                            "id",
                            null,
                            AccountType.TRADITIONAL_IRA,
                            BigDecimal.ZERO,
                            AccountType.TaxTreatment.PRE_TAX,
                            true,
                            defaultAllocation
                    ));
        }

        @Test
        @DisplayName("Should throw for null accountType")
        void throwsForNullAccountType() {
            assertThrows(MissingRequiredFieldException.class, () ->
                    new AccountSnapshot(
                            "id",
                            "Test",
                            null,
                            BigDecimal.ZERO,
                            AccountType.TaxTreatment.PRE_TAX,
                            true,
                            defaultAllocation
                    ));
        }

        @Test
        @DisplayName("Should throw for null balance")
        void throwsForNullBalance() {
            assertThrows(MissingRequiredFieldException.class, () ->
                    new AccountSnapshot(
                            "id",
                            "Test",
                            AccountType.TRADITIONAL_IRA,
                            null,
                            AccountType.TaxTreatment.PRE_TAX,
                            true,
                            defaultAllocation
                    ));
        }

        @Test
        @DisplayName("Should throw for null taxTreatment")
        void throwsForNullTaxTreatment() {
            assertThrows(MissingRequiredFieldException.class, () ->
                    new AccountSnapshot(
                            "id",
                            "Test",
                            AccountType.TRADITIONAL_IRA,
                            BigDecimal.ZERO,
                            null,
                            true,
                            defaultAllocation
                    ));
        }

        @Test
        @DisplayName("Should throw for null allocation")
        void throwsForNullAllocation() {
            assertThrows(MissingRequiredFieldException.class, () ->
                    new AccountSnapshot(
                            "id",
                            "Test",
                            AccountType.TRADITIONAL_IRA,
                            BigDecimal.ZERO,
                            AccountType.TaxTreatment.PRE_TAX,
                            true,
                            null
                    ));
        }
    }

    @Nested
    @DisplayName("Helper Method Tests")
    class HelperMethodTests {

        @Test
        @DisplayName("hasBalance should return true for positive balance")
        void hasBalancePositive() {
            AccountSnapshot snapshot = AccountSnapshot.from(traditional401k);
            assertTrue(snapshot.hasBalance());
        }

        @Test
        @DisplayName("hasBalance should return false for zero balance")
        void hasBalanceZero() {
            InvestmentAccount emptyAccount = InvestmentAccount.builder()
                    .name("Empty")
                    .accountType(AccountType.TRADITIONAL_IRA)
                    .balance(BigDecimal.ZERO)
                    .allocation(defaultAllocation)
                    .preRetirementReturnRate(0.07)
                    .build();

            AccountSnapshot snapshot = AccountSnapshot.from(emptyAccount);
            assertFalse(snapshot.hasBalance());
        }

        @Test
        @DisplayName("isTaxable should return true for PRE_TAX accounts")
        void isTaxablePreTax() {
            AccountSnapshot snapshot = AccountSnapshot.from(traditional401k);
            assertTrue(snapshot.isTaxable());
        }

        @Test
        @DisplayName("isTaxable should return false for Roth accounts")
        void isTaxableRoth() {
            AccountSnapshot snapshot = AccountSnapshot.from(rothIra);
            assertFalse(snapshot.isTaxable());
        }

        @Test
        @DisplayName("isTaxable should return false for HSA accounts")
        void isTaxableHsa() {
            AccountSnapshot snapshot = AccountSnapshot.from(hsa);
            assertFalse(snapshot.isTaxable());
        }

        @Test
        @DisplayName("isTaxable should return false for Taxable accounts")
        void isTaxableTaxable() {
            // Taxable brokerage has TAXABLE treatment, not PRE_TAX
            AccountSnapshot snapshot = AccountSnapshot.from(taxableBrokerage);
            assertFalse(snapshot.isTaxable());
        }

        @Test
        @DisplayName("isEmployerSponsored should return true for 401k")
        void isEmployerSponsored401k() {
            AccountSnapshot snapshot = AccountSnapshot.from(traditional401k);
            assertTrue(snapshot.isEmployerSponsored());
        }

        @Test
        @DisplayName("isEmployerSponsored should return false for IRA")
        void isEmployerSponsoredIra() {
            AccountSnapshot snapshot = AccountSnapshot.from(rothIra);
            assertFalse(snapshot.isEmployerSponsored());
        }
    }

    @Nested
    @DisplayName("RMD Subject Tests")
    class RmdSubjectTests {

        @Test
        @DisplayName("Traditional 401k should be subject to RMD")
        void traditional401kSubjectToRmd() {
            AccountSnapshot snapshot = AccountSnapshot.from(traditional401k);
            assertTrue(snapshot.subjectToRmd());
        }

        @Test
        @DisplayName("Roth IRA should not be subject to RMD")
        void rothIraNotSubjectToRmd() {
            AccountSnapshot snapshot = AccountSnapshot.from(rothIra);
            assertFalse(snapshot.subjectToRmd());
        }

        @Test
        @DisplayName("Traditional IRA should be subject to RMD")
        void traditionalIraSubjectToRmd() {
            InvestmentAccount traditionalIra = InvestmentAccount.builder()
                    .name("Traditional IRA")
                    .accountType(AccountType.TRADITIONAL_IRA)
                    .balance(new BigDecimal("100000"))
                    .allocation(defaultAllocation)
                    .preRetirementReturnRate(0.07)
                    .build();

            AccountSnapshot snapshot = AccountSnapshot.from(traditionalIra);
            assertTrue(snapshot.subjectToRmd());
        }

        @Test
        @DisplayName("Roth 401k should be subject to RMD")
        void roth401kSubjectToRmd() {
            InvestmentAccount roth401k = InvestmentAccount.builder()
                    .name("Roth 401(k)")
                    .accountType(AccountType.ROTH_401K)
                    .balance(new BigDecimal("100000"))
                    .allocation(defaultAllocation)
                    .preRetirementReturnRate(0.07)
                    .build();

            AccountSnapshot snapshot = AccountSnapshot.from(roth401k);
            assertTrue(snapshot.subjectToRmd());
        }

        @Test
        @DisplayName("HSA should not be subject to RMD")
        void hsaNotSubjectToRmd() {
            AccountSnapshot snapshot = AccountSnapshot.from(hsa);
            assertFalse(snapshot.subjectToRmd());
        }

        @Test
        @DisplayName("Taxable brokerage should not be subject to RMD")
        void taxableBrokerageNotSubjectToRmd() {
            AccountSnapshot snapshot = AccountSnapshot.from(taxableBrokerage);
            assertFalse(snapshot.subjectToRmd());
        }
    }
}
