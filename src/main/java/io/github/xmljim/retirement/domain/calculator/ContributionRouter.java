package io.github.xmljim.retirement.domain.calculator;

import java.math.BigDecimal;

import io.github.xmljim.retirement.domain.enums.ContributionType;
import io.github.xmljim.retirement.domain.model.Portfolio;
import io.github.xmljim.retirement.domain.value.RoutingConfiguration;

/**
 * Routes contributions to appropriate accounts based on configuration and IRS rules.
 *
 * <p>The contribution router determines how a contribution amount should be
 * distributed across accounts in a portfolio based on:
 * <ul>
 *   <li>User-defined routing configuration (percentages and priorities)</li>
 *   <li>IRS contribution rules (SECURE 2.0 compliance)</li>
 *   <li>Account availability in the portfolio</li>
 * </ul>
 *
 * <p>Key routing rules:
 * <ul>
 *   <li><strong>Personal contributions</strong>: Follow user's routing configuration</li>
 *   <li><strong>Employer contributions</strong>: Always route to Traditional variant</li>
 *   <li><strong>High-earner catch-up</strong>: Must go to Roth (SECURE 2.0)</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * ContributionRouter router = CalculatorFactory.contributionRouter();
 *
 * RoutingConfiguration config = RoutingConfiguration.builder()
 *     .addRule("trad-401k", 0.80, 1)
 *     .addRule("roth-401k", 0.20, 2)
 *     .build();
 *
 * ContributionAllocation allocation = router.route(
 *     new BigDecimal("5000"),
 *     ContributionType.PERSONAL,
 *     portfolio,
 *     config,
 *     2025,  // year
 *     55,    // age
 *     new BigDecimal("100000")  // prior year income
 * );
 * }</pre>
 *
 * @see ContributionAllocation
 * @see RoutingConfiguration
 * @see io.github.xmljim.retirement.domain.calculator.impl.DefaultContributionRouter
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface ContributionRouter {

    /**
     * Routes a contribution amount across accounts based on configuration and rules.
     *
     * <p>The routing process:
     * <ol>
     *   <li>Validates the portfolio contains accounts referenced in the configuration</li>
     *   <li>Applies user's routing percentages to distribute the amount</li>
     *   <li>Checks IRS rules for any required redirections (e.g., Roth catch-up)</li>
     *   <li>Generates warnings if limits are exceeded or accounts are missing</li>
     * </ol>
     *
     * @param amount the contribution amount to route
     * @param source the type of contribution (PERSONAL or EMPLOYER)
     * @param portfolio the portfolio containing target accounts
     * @param config the routing configuration defining percentages and priorities
     * @param contributionYear the tax year for the contribution
     * @param age the contributor's age (for catch-up eligibility)
     * @param priorYearIncome the contributor's prior year income (for SECURE 2.0 rules)
     * @return the allocation result showing how the amount was distributed
     * @throws io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException
     *         if required parameters are null
     * @throws io.github.xmljim.retirement.domain.exception.ValidationException
     *         if amount is negative
     */
    ContributionAllocation route(
        BigDecimal amount,
        ContributionType source,
        Portfolio portfolio,
        RoutingConfiguration config,
        int contributionYear,
        int age,
        BigDecimal priorYearIncome
    );

    /**
     * Routes a contribution with default parameters for non-catch-up scenarios.
     *
     * <p>Convenience method that uses age 40 and zero prior year income,
     * which means no catch-up contributions and no SECURE 2.0 Roth requirements.
     *
     * @param amount the contribution amount to route
     * @param source the type of contribution (PERSONAL or EMPLOYER)
     * @param portfolio the portfolio containing target accounts
     * @param config the routing configuration
     * @param contributionYear the tax year for the contribution
     * @return the allocation result
     */
    default ContributionAllocation route(
            BigDecimal amount,
            ContributionType source,
            Portfolio portfolio,
            RoutingConfiguration config,
            int contributionYear) {
        return route(amount, source, portfolio, config, contributionYear, 40, BigDecimal.ZERO);
    }
}
