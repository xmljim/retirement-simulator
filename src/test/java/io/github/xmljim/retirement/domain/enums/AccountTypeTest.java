package io.github.xmljim.retirement.domain.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("AccountType Enum Tests")
class AccountTypeTest {

    @Test
    @DisplayName("Traditional 401(k) should be pre-tax and employer-sponsored")
    void traditional401kProperties() {
        AccountType type = AccountType.TRADITIONAL_401K;

        assertEquals("Traditional 401(k)", type.getDisplayName());
        assertEquals(AccountType.TaxTreatment.PRE_TAX, type.getTaxTreatment());
        assertTrue(type.isEmployerSponsored());
        assertTrue(type.isSubjectToRmd());
        assertTrue(type.isPreTax());
        assertFalse(type.isTaxFreeWithdrawal());
    }

    @Test
    @DisplayName("Roth 401(k) should be post-tax with tax-free withdrawals")
    void roth401kProperties() {
        AccountType type = AccountType.ROTH_401K;

        assertEquals("Roth 401(k)", type.getDisplayName());
        assertEquals(AccountType.TaxTreatment.ROTH, type.getTaxTreatment());
        assertTrue(type.isEmployerSponsored());
        assertTrue(type.isSubjectToRmd());
        assertFalse(type.isPreTax());
        assertTrue(type.isTaxFreeWithdrawal());
    }

    @Test
    @DisplayName("Roth IRA should not be subject to RMDs")
    void rothIraNoRmd() {
        AccountType type = AccountType.ROTH_IRA;

        assertFalse(type.isSubjectToRmd());
        assertFalse(type.isEmployerSponsored());
        assertTrue(type.isTaxFreeWithdrawal());
    }

    @Test
    @DisplayName("HSA should have triple tax advantage")
    void hsaTaxTreatment() {
        AccountType type = AccountType.HSA;

        assertEquals(AccountType.TaxTreatment.HSA, type.getTaxTreatment());
        assertTrue(type.isPreTax());
        assertFalse(type.isSubjectToRmd());
        assertFalse(type.isEmployerSponsored());
    }

    @Test
    @DisplayName("Taxable brokerage should have no tax advantages")
    void taxableBrokerageProperties() {
        AccountType type = AccountType.TAXABLE_BROKERAGE;

        assertEquals(AccountType.TaxTreatment.TAXABLE, type.getTaxTreatment());
        assertFalse(type.isPreTax());
        assertFalse(type.isTaxFreeWithdrawal());
        assertFalse(type.isSubjectToRmd());
        assertFalse(type.isEmployerSponsored());
    }

    @ParameterizedTest
    @EnumSource(AccountType.class)
    @DisplayName("All account types should have non-null display name")
    void allTypesHaveDisplayName(AccountType type) {
        assertNotNull(type.getDisplayName());
        assertFalse(type.getDisplayName().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(AccountType.class)
    @DisplayName("All account types should have non-null tax treatment")
    void allTypesHaveTaxTreatment(AccountType type) {
        assertNotNull(type.getTaxTreatment());
    }

    @Test
    @DisplayName("Employer-sponsored accounts should include 401k, 403b, and 457b")
    void employerSponsoredAccounts() {
        assertTrue(AccountType.TRADITIONAL_401K.isEmployerSponsored());
        assertTrue(AccountType.ROTH_401K.isEmployerSponsored());
        assertTrue(AccountType.TRADITIONAL_403B.isEmployerSponsored());
        assertTrue(AccountType.ROTH_403B.isEmployerSponsored());
        assertTrue(AccountType.TRADITIONAL_457B.isEmployerSponsored());
    }

    @Test
    @DisplayName("Individual accounts should not be employer-sponsored")
    void individualAccounts() {
        assertFalse(AccountType.TRADITIONAL_IRA.isEmployerSponsored());
        assertFalse(AccountType.ROTH_IRA.isEmployerSponsored());
        assertFalse(AccountType.HSA.isEmployerSponsored());
        assertFalse(AccountType.TAXABLE_BROKERAGE.isEmployerSponsored());
    }
}
