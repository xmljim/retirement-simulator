package io.github.xmljim.retirement.simulation.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.model.InvestmentAccount;
import io.github.xmljim.retirement.domain.model.PersonProfile;
import io.github.xmljim.retirement.domain.model.Portfolio;
import io.github.xmljim.retirement.domain.value.RoutingConfiguration;
import io.github.xmljim.retirement.simulation.config.PersonFinancialConfig;

/**
 * Tests for SimulationConfig with PersonFinancialConfig integration.
 */
@DisplayName("Contribution Routing Integration")
class ContributionRoutingIntegrationTest {

    private PersonProfile createPerson(String name) {
        return PersonProfile.builder()
            .name(name)
            .dateOfBirth(LocalDate.of(1970, 6, 15))
            .retirementDate(LocalDate.of(2035, 1, 1))
            .lifeExpectancy(90)
            .build();
    }

    private InvestmentAccount createAccount(String name, AccountType type) {
        return InvestmentAccount.builder()
            .id(UUID.randomUUID().toString())
            .name(name)
            .accountType(type)
            .balance(new BigDecimal("100000"))
            .preRetirementReturnRate(new BigDecimal("0.07"))
            .postRetirementReturnRate(new BigDecimal("0.05"))
            .build();
    }

    @Nested
    @DisplayName("SimulationConfig with PersonFinancialConfig")
    class SimulationConfigFinancialTests {

        @Test
        @DisplayName("should allow empty financial configs")
        void shouldAllowEmptyFinancialConfigs() {
            PersonProfile person = createPerson("Test Person");
            InvestmentAccount account = createAccount("401k", AccountType.TRADITIONAL_401K);
            Portfolio portfolio = Portfolio.builder().owner(person).addAccount(account).build();

            SimulationConfig config = SimulationConfig.builder()
                .portfolio(portfolio)
                .startMonth(YearMonth.of(2025, 1))
                .endMonth(YearMonth.of(2025, 12))
                .build();

            assertTrue(config.getFinancialConfig(person).isEmpty());
        }

        @Test
        @DisplayName("should return financial config when set")
        void shouldReturnFinancialConfigWhenSet() {
            PersonProfile person = createPerson("Test Person");
            InvestmentAccount account = createAccount("401k", AccountType.TRADITIONAL_401K);
            Portfolio portfolio = Portfolio.builder().owner(person).addAccount(account).build();

            RoutingConfiguration routingConfig = RoutingConfiguration.singleAccount(account.getId());
            PersonFinancialConfig financialConfig = PersonFinancialConfig.builder()
                .routingConfig(routingConfig)
                .build();

            SimulationConfig config = SimulationConfig.builder()
                .portfolio(portfolio)
                .startMonth(YearMonth.of(2025, 1))
                .endMonth(YearMonth.of(2025, 12))
                .addFinancialConfig(person, financialConfig)
                .build();

            assertTrue(config.getFinancialConfig(person).isPresent());
            assertEquals(financialConfig, config.getFinancialConfig(person).orElseThrow());
        }

        @Test
        @DisplayName("should support multiple persons with different configs")
        void shouldSupportMultiplePersonsWithDifferentConfigs() {
            PersonProfile person = createPerson("Test Person");
            PersonProfile spouse = createPerson("Spouse");

            InvestmentAccount account1 = createAccount("401k", AccountType.TRADITIONAL_401K);
            InvestmentAccount account2 = createAccount("Spouse 401k", AccountType.TRADITIONAL_401K);

            Portfolio portfolio1 = Portfolio.builder().owner(person).addAccount(account1).build();
            Portfolio portfolio2 = Portfolio.builder().owner(spouse).addAccount(account2).build();

            RoutingConfiguration routingConfig1 = RoutingConfiguration.singleAccount(account1.getId());
            RoutingConfiguration routingConfig2 = RoutingConfiguration.singleAccount(account2.getId());

            PersonFinancialConfig config1 = PersonFinancialConfig.builder()
                .routingConfig(routingConfig1)
                .build();
            PersonFinancialConfig config2 = PersonFinancialConfig.builder()
                .routingConfig(routingConfig2)
                .build();

            SimulationConfig config = SimulationConfig.builder()
                .portfolios(List.of(portfolio1, portfolio2))
                .startMonth(YearMonth.of(2025, 1))
                .endMonth(YearMonth.of(2025, 12))
                .addFinancialConfig(person, config1)
                .addFinancialConfig(spouse, config2)
                .build();

            assertTrue(config.getFinancialConfig(person).isPresent());
            assertTrue(config.getFinancialConfig(spouse).isPresent());
            assertEquals(routingConfig1, config.getFinancialConfig(person).orElseThrow().routingConfig());
            assertEquals(routingConfig2, config.getFinancialConfig(spouse).orElseThrow().routingConfig());
        }

        @Test
        @DisplayName("should return empty for unknown person")
        void shouldReturnEmptyForUnknownPerson() {
            PersonProfile person = createPerson("Test Person");
            PersonProfile unknown = createPerson("Unknown");

            InvestmentAccount account = createAccount("401k", AccountType.TRADITIONAL_401K);
            Portfolio portfolio = Portfolio.builder().owner(person).addAccount(account).build();

            SimulationConfig config = SimulationConfig.builder()
                .portfolio(portfolio)
                .startMonth(YearMonth.of(2025, 1))
                .endMonth(YearMonth.of(2025, 12))
                .build();

            assertTrue(config.getFinancialConfig(unknown).isEmpty());
        }
    }

    @Nested
    @DisplayName("PersonFinancialConfig")
    class PersonFinancialConfigTests {

        @Test
        @DisplayName("should report hasRoutingConfig correctly")
        void shouldReportHasRoutingConfig() {
            InvestmentAccount account = createAccount("401k", AccountType.TRADITIONAL_401K);
            RoutingConfiguration routingConfig = RoutingConfiguration.singleAccount(account.getId());

            PersonFinancialConfig withRouting = PersonFinancialConfig.builder()
                .routingConfig(routingConfig)
                .build();

            PersonFinancialConfig withoutRouting = PersonFinancialConfig.builder().build();

            assertTrue(withRouting.hasRoutingConfig());
            assertTrue(!withoutRouting.hasRoutingConfig());
        }
    }
}
