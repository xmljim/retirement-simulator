package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import io.github.xmljim.retirement.domain.calculator.ContributionAllocation;
import io.github.xmljim.retirement.domain.calculator.ContributionRouter;
import io.github.xmljim.retirement.domain.calculator.IrsContributionRules;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.enums.ContributionType;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;
import io.github.xmljim.retirement.domain.model.InvestmentAccount;
import io.github.xmljim.retirement.domain.model.Portfolio;
import io.github.xmljim.retirement.domain.value.RoutingConfiguration;
import io.github.xmljim.retirement.domain.value.RoutingRule;

/**
 * Default implementation of {@link ContributionRouter}.
 *
 * <p>Routes contributions based on user configuration and IRS rules:
 * <ul>
 *   <li>Personal contributions follow user-defined routing percentages</li>
 *   <li>Employer contributions always go to Traditional account variant</li>
 *   <li>High-earner catch-up contributions are redirected to Roth (SECURE 2.0)</li>
 * </ul>
 */
public class DefaultContributionRouter implements ContributionRouter {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final IrsContributionRules irsRules;

    /**
     * Creates a new DefaultContributionRouter.
     *
     * @param irsRules the IRS contribution rules for SECURE 2.0 compliance
     */
    public DefaultContributionRouter(IrsContributionRules irsRules) {
        MissingRequiredFieldException.requireNonNull(irsRules, "irsRules");
        this.irsRules = irsRules;
    }

    @Override
    public ContributionAllocation route(
            BigDecimal amount,
            ContributionType source,
            Portfolio portfolio,
            RoutingConfiguration config,
            int contributionYear,
            int age,
            BigDecimal priorYearIncome) {

        // Validate inputs
        MissingRequiredFieldException.requireNonNull(amount, "amount");
        MissingRequiredFieldException.requireNonNull(source, "source");
        MissingRequiredFieldException.requireNonNull(portfolio, "portfolio");
        MissingRequiredFieldException.requireNonNull(config, "config");

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Contribution amount cannot be negative", "amount");
        }

        // Handle zero amount
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return ContributionAllocation.builder().build();
        }

        // Handle empty portfolio
        if (!portfolio.hasAccounts()) {
            return ContributionAllocation.empty(amount);
        }

        // Route based on contribution type
        if (source == ContributionType.EMPLOYER) {
            return routeEmployerContribution(amount, portfolio, config);
        } else {
            return routePersonalContribution(
                amount, portfolio, config, contributionYear, age, priorYearIncome);
        }
    }

    /**
     * Routes an employer contribution.
     *
     * <p>Employer contributions always go to the Traditional variant of the account.
     * Uses the first account in the routing configuration (highest priority).
     */
    private ContributionAllocation routeEmployerContribution(
            BigDecimal amount,
            Portfolio portfolio,
            RoutingConfiguration config) {

        ContributionAllocation.Builder builder = ContributionAllocation.builder();

        // Get the primary account from routing config
        RoutingRule primaryRule = config.getRulesByPriority().get(0);
        Optional<InvestmentAccount> accountOpt = portfolio.findAccountById(primaryRule.accountId());

        if (accountOpt.isEmpty()) {
            builder.addWarning("Primary account not found: " + primaryRule.accountId());
            builder.unallocated(amount);
            return builder.build();
        }

        InvestmentAccount account = accountOpt.get();

        // Find the Traditional variant for employer match
        AccountType targetType = irsRules.determineTargetAccountType(
            ContributionType.EMPLOYER,
            false,  // not catch-up
            BigDecimal.ZERO,
            0,  // year not relevant for employer
            account.getAccountType()
        );

        // Find account with Traditional variant
        Optional<InvestmentAccount> traditionalAccount = findAccountByType(portfolio, targetType);

        if (traditionalAccount.isPresent()) {
            builder.addAllocation(traditionalAccount.get().getId(), amount);
        } else {
            // Fall back to the configured account with a warning
            builder.addAllocation(account.getId(), amount);
            builder.addWarning("Traditional variant not found for "
                + account.getAccountType() + ", using " + account.getName());
        }

        return builder.build();
    }

    /**
     * Routes a personal contribution.
     *
     * <p>Applies the user's routing configuration percentages, checking for
     * SECURE 2.0 high-earner catch-up rules.
     */
    private ContributionAllocation routePersonalContribution(
            BigDecimal amount,
            Portfolio portfolio,
            RoutingConfiguration config,
            int contributionYear,
            int age,
            BigDecimal priorYearIncome) {

        ContributionAllocation.Builder builder = ContributionAllocation.builder();
        BigDecimal remaining = amount;

        // Determine if this is catch-up eligible and requires Roth routing
        boolean isCatchUpEligible = irsRules.isCatchUpEligible(age);
        boolean requiresRothCatchUp = isCatchUpEligible
            && irsRules.requiresRothCatchUp(contributionYear, priorYearIncome);

        for (RoutingRule rule : config.getRulesByPriority()) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            Optional<InvestmentAccount> accountOpt = portfolio.findAccountById(rule.accountId());

            if (accountOpt.isEmpty()) {
                builder.addWarning("Account not found in portfolio: " + rule.accountId());
                continue;
            }

            InvestmentAccount account = accountOpt.get();
            BigDecimal allocationAmount = amount.multiply(rule.percentage())
                .setScale(SCALE, ROUNDING);

            // Ensure we don't allocate more than remaining
            if (allocationAmount.compareTo(remaining) > 0) {
                allocationAmount = remaining;
            }

            // Check if high-earner catch-up requires Roth routing
            if (requiresRothCatchUp && isTraditionalType(account.getAccountType())) {
                // Try to find Roth equivalent
                AccountType rothType = toRothVariant(account.getAccountType());
                Optional<InvestmentAccount> rothAccount = findAccountByType(portfolio, rothType);

                if (rothAccount.isPresent()) {
                    builder.addAllocation(rothAccount.get().getId(), allocationAmount);
                    builder.addWarning("High earner catch-up: redirected "
                        + allocationAmount + " from "
                        + account.getAccountType() + " to " + rothType);
                } else {
                    // No Roth available - still allocate to Traditional with warning
                    builder.addAllocation(account.getId(), allocationAmount);
                    builder.addWarning("High earner catch-up required Roth, but "
                        + rothType + " not available. Allocated to " + account.getAccountType());
                }
            } else {
                builder.addAllocation(account.getId(), allocationAmount);
            }

            remaining = remaining.subtract(allocationAmount);
        }

        // Handle any rounding remainder
        if (remaining.compareTo(BigDecimal.ZERO) > 0
                && remaining.compareTo(new BigDecimal("0.01")) <= 0) {
            // Small rounding difference - add to first allocation
            String firstAccountId = config.getRulesByPriority().get(0).accountId();
            builder.addAllocation(firstAccountId, remaining);
            remaining = BigDecimal.ZERO;
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            builder.unallocated(remaining);
        }

        return builder.build();
    }

    /**
     * Finds an account in the portfolio by account type.
     */
    private Optional<InvestmentAccount> findAccountByType(Portfolio portfolio, AccountType type) {
        return portfolio.getAccountsByType(type).stream().findFirst();
    }

    /**
     * Checks if the account type is a Traditional (pre-tax) variant.
     */
    private boolean isTraditionalType(AccountType type) {
        return type == AccountType.TRADITIONAL_401K
            || type == AccountType.TRADITIONAL_403B
            || type == AccountType.TRADITIONAL_IRA;
    }

    /**
     * Converts an account type to its Roth variant.
     */
    private AccountType toRothVariant(AccountType type) {
        return switch (type) {
            case TRADITIONAL_401K, ROTH_401K -> AccountType.ROTH_401K;
            case TRADITIONAL_403B, ROTH_403B -> AccountType.ROTH_403B;
            case TRADITIONAL_IRA, ROTH_IRA -> AccountType.ROTH_IRA;
            default -> type;
        };
    }
}
