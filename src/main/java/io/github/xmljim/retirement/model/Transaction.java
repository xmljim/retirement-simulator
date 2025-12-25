package io.github.xmljim.retirement.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;


import static io.github.xmljim.retirement.functions.Functions.*;

/**
 * Represents a single transaction in a retirement portfolio simulation.
 *
 * @deprecated This class uses deprecated PortfolioParameters. Will be refactored in Issue #8.
 */
@SuppressWarnings("deprecation")
public class Transaction {

    private final PortfolioParameters portfolioParameters;

    private final LocalDate transactionDate;
    private final Optional<Transaction> previous;

    public Transaction(PortfolioParameters portfolioParameters, LocalDate transactionDate) {
        this.portfolioParameters = portfolioParameters;
        this.transactionDate = transactionDate;
        this.previous = Optional.empty();
    }

    public Transaction(PortfolioParameters portfolioParameters, LocalDate transactionDate, Transaction previous) {
        this.portfolioParameters = portfolioParameters;
        this.transactionDate = transactionDate;
        this.previous = Optional.ofNullable(previous);
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public long getAge() {
        return ChronoUnit.YEARS.between(portfolioParameters.getDateOfBirth(), transactionDate);
    }

    public boolean isRetired() {
        return IS_RETIRED.apply(transactionDate, portfolioParameters.getPlannedRetirementDate());
    }

    public Double getStartBalance() {
        return previous.map(Transaction::getEndBalance)
            .orElse(portfolioParameters.getInvestments().getStartingBalance());
    }

    public Double getEndBalance() {
        return 0.0;
    }

    public TransactionType getType() {
        return isRetired() ? TransactionType.WITHDRAWAL : TransactionType.CONTRIBUTION;
    }

    public Double getSocialSecurity() {
        return SocialSecurity.getDistribution(portfolioParameters, transactionDate);
    }

    public Double getOtherRetirement() {
        return OtherRetirementIncome.getDistribution(portfolioParameters, transactionDate);
    }

    public Double getPersonalContributionRate() {
        return portfolioParameters.getContributions().stream()
            .filter(contribution -> contribution.getContributionType() == ContributionType.PERSONAL)
            .mapToDouble(c -> INDIVIDUAL_CONTRIBUTION.apply(transactionDate,
                portfolioParameters.getPlannedRetirementDate(), c.getContributionRate(), c.getIncrementContributionRate(),
                c.getIncrementMonth())).sum();
    }

    public Double getEmployerContributionRate() {
        return portfolioParameters.getContributions().stream()
            .filter(contribution -> contribution.getContributionType() == ContributionType.EMPLOYER)
            .mapToDouble(c ->
                EMPLOYER_CONTRIBUTION.apply(transactionDate, portfolioParameters.getPlannedRetirementDate(),
                    c.getContributionRate())).sum();
    }

    public Double getTargetMonthlyIncome() {
        return MONTHLY_SALARY.apply(portfolioParameters.getWorkingIncome().getBaseSalary(),
            portfolioParameters.getWorkingIncome().getColaPct(),
            portfolioParameters.getMonthlyRetirementIncome().getSocialSecurityAdjustmentRate(),
            portfolioParameters.getWithdrawalIncome().getWithdrawalRate(),
            transactionDate, portfolioParameters.getPlannedRetirementDate());
    }

    public Double getRetirementWithdrawal() {
        return isRetired() ? Math.min(
            getTargetMonthlyIncome()  - (getSocialSecurity() + getOtherRetirement()),
            getEndBalance()): 0.0;
    }

}
