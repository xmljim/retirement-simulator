package io.github.xmljim.retirement.domain.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("LimitCategory Tests")
class LimitCategoryTest {

    @Test
    @DisplayName("Should map 401k accounts to EMPLOYER_401K category")
    void traditional401kMapsToEmployer401k() {
        assertEquals(LimitCategory.EMPLOYER_401K,
            LimitCategory.fromAccountType(AccountType.TRADITIONAL_401K));
        assertEquals(LimitCategory.EMPLOYER_401K,
            LimitCategory.fromAccountType(AccountType.ROTH_401K));
    }

    @Test
    @DisplayName("Should map 403b accounts to EMPLOYER_401K category")
    void accounts403bMapToEmployer401k() {
        assertEquals(LimitCategory.EMPLOYER_401K,
            LimitCategory.fromAccountType(AccountType.TRADITIONAL_403B));
        assertEquals(LimitCategory.EMPLOYER_401K,
            LimitCategory.fromAccountType(AccountType.ROTH_403B));
    }

    @Test
    @DisplayName("Should map 457b accounts to EMPLOYER_401K category")
    void accounts457bMapsToEmployer401k() {
        assertEquals(LimitCategory.EMPLOYER_401K,
            LimitCategory.fromAccountType(AccountType.TRADITIONAL_457B));
    }

    @Test
    @DisplayName("Should map IRA accounts to IRA category")
    void iraAccountsMapToIra() {
        assertEquals(LimitCategory.IRA,
            LimitCategory.fromAccountType(AccountType.TRADITIONAL_IRA));
        assertEquals(LimitCategory.IRA,
            LimitCategory.fromAccountType(AccountType.ROTH_IRA));
    }

    @Test
    @DisplayName("Should map HSA to HSA category")
    void hsaMapsToHsa() {
        assertEquals(LimitCategory.HSA,
            LimitCategory.fromAccountType(AccountType.HSA));
    }

    @Test
    @DisplayName("Should return null for taxable brokerage")
    void taxableBrokerageReturnsNull() {
        assertNull(LimitCategory.fromAccountType(AccountType.TAXABLE_BROKERAGE));
    }

    @Test
    @DisplayName("Should return null for null account type")
    void nullAccountTypeReturnsNull() {
        assertNull(LimitCategory.fromAccountType(null));
    }

    @ParameterizedTest
    @EnumSource(LimitCategory.class)
    @DisplayName("All categories should have display names")
    void allCategoriesHaveDisplayNames(LimitCategory category) {
        String displayName = category.getDisplayName();
        assertEquals(false, displayName == null || displayName.isBlank());
    }
}
