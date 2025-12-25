

package io.github.xmljim.retirement.model;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates all parameters required to model a retirement portfolio, including investments,
 * contributions, working income, withdrawal income, and monthly retirement income.
 *
 * <p><b>Migration Guide:</b> This class is being replaced by the new domain model classes:
 * <ul>
 *   <li>{@code dateOfBirth}, {@code plannedRetirementDate} →
 *       {@link io.github.xmljim.retirement.domain.model.PersonProfile}</li>
 *   <li>{@code Investments} →
 *       {@link io.github.xmljim.retirement.domain.model.InvestmentAccount}</li>
 *   <li>{@code Contribution} →
 *       {@link io.github.xmljim.retirement.domain.value.ContributionConfig}</li>
 *   <li>{@code WorkingIncome} →
 *       {@link io.github.xmljim.retirement.domain.value.WorkingIncome}</li>
 *   <li>{@code WithdrawalIncome} →
 *       {@link io.github.xmljim.retirement.domain.value.WithdrawalStrategy}</li>
 *   <li>{@code MonthlyRetirementIncome} →
 *       {@link io.github.xmljim.retirement.domain.value.SocialSecurityIncome} and
 *       {@link io.github.xmljim.retirement.domain.value.RetirementIncome}</li>
 * </ul>
 *
 * @deprecated This class will be removed in a future release. Use the domain model classes instead.
 *             See migration guide above for replacement classes.
 */
@Deprecated
public class PortfolioParameters {

    private final LocalDate dateOfBirth;
    private final LocalDate plannedRetirementDate;
    private final Investments investments;
    private final List<Contribution> contributions;
    private final WorkingIncome workingIncome;
    private final WithdrawalIncome withdrawalIncome;
    private final MonthlyRetirementIncome monthlyRetirementIncome;

    private PortfolioParameters(Investments investments, List<Contribution> contributions,
                                WorkingIncome workingIncome, WithdrawalIncome withdrawalIncome,
                                MonthlyRetirementIncome monthlyIncome, LocalDate dateOfBirth,
                                LocalDate plannedRetirementDate) {
        this.investments = investments;
        this.contributions = contributions;
        this.workingIncome = workingIncome;
        this.withdrawalIncome = withdrawalIncome;
        this.monthlyRetirementIncome = monthlyIncome;
        this.dateOfBirth = dateOfBirth;
        this.plannedRetirementDate = plannedRetirementDate;
    }

    /**
     * Returns the investments parameters for the portfolio.
     *
     * @return the investments
     */
    public Investments getInvestments() {
        return investments;
    }

    /**
     * Returns the list of contributions to the portfolio.
     *
     * @return the contributions list
     */
    public List<Contribution> getContributions() {
        return contributions;
    }

    /**
     * Returns the working income parameters.
     *
     * @return the working income
     */
    public WorkingIncome getWorkingIncome() {
        return workingIncome;
    }

    /**
     * Returns the withdrawal income parameters.
     *
     * @return the withdrawal income
     */
    public WithdrawalIncome getWithdrawalIncome() {
        return withdrawalIncome;
    }

    /**
     * Returns the monthly retirement income parameters.
     *
     * @return the monthly retirement income
     */
    public MonthlyRetirementIncome getMonthlyRetirementIncome() {
        return monthlyRetirementIncome;
    }

    /**
     * Returns the date of birth
     * @return the date of birth
     */
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    /**
     * Returns the planned retirement date
     * @return the planned retirement date
     */
    public LocalDate getPlannedRetirementDate() {
        return plannedRetirementDate;
    }

    /**
     * Return the builder for parameters
     * @return a builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link PortfolioParameters}. Use to construct a PortfolioParameters instance.
     */
    public static class Builder {
        private Investments investments;
        private final List<Contribution> contributions = new ArrayList<>();
        private WorkingIncome workingIncome;
        private WithdrawalIncome withdrawalIncome;
        private MonthlyRetirementIncome monthlyRetirementIncome;
        private LocalDate dateOfBirth;
        private LocalDate plannedRetirementDate;

        private Builder() {
            //no-op
        }

        /**
         * Set the investments parameters.
         *
         * @param investments the investments
         * @return this builder
         */
        public Builder investments(Investments investments) {
            this.investments = investments;
            return this;
        }

        /**
         * Add a contribution to the portfolio.
         *
         * @param contribution the contribution
         * @return this builder
         */
        public Builder addContribution(Contribution contribution) {
            contributions.add(contribution);
            return this;
        }

        /**
         * Set the working income parameters.
         *
         * @param workingIncome the working income
         * @return this builder
         */
        public Builder workingIncome(WorkingIncome workingIncome) {
            this.workingIncome = workingIncome;
            return this;
        }

        /**
         * Set the withdrawal income parameters.
         *
         * @param withdrawalIncome the withdrawal income
         * @return this builder
         */
        public Builder withdrawalIncome(WithdrawalIncome withdrawalIncome) {
            this.withdrawalIncome = withdrawalIncome;
            return this;
        }

        /**
         * Set the monthly retirement income parameters.
         *
         * @param monthlyIncome the monthly retirement income
         * @return this builder
         */
        public Builder monthlyRetirementIncome(MonthlyRetirementIncome monthlyIncome) {
            this.monthlyRetirementIncome = monthlyIncome;
            return this;
        }

        /**
         * Set the date of birth
         * @param dateOfBirth the date of birth
         * @return this builder
         */
        public Builder dateOfBirth(LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        /**
         * Set the planned the retirement date
         * @param plannedRetirementDate the retirement date
         * @return this builder
         */
        public Builder plannedRetirementDate(LocalDate plannedRetirementDate) {
            this.plannedRetirementDate = plannedRetirementDate;
            return this;
        }

        /**
         * Build the PortfolioParameters instance.
         *
         * @return a new PortfolioParameters
         */
        public PortfolioParameters build() {
            return new PortfolioParameters(investments, contributions, workingIncome, withdrawalIncome,
                monthlyRetirementIncome, dateOfBirth, plannedRetirementDate);
        }
    }

    /**
     * Represents investment parameters for the retirement portfolio, including starting balance
     * and rates of return before and after retirement.
     */
    public static class Investments {
        private final double startingBalance;

        private final double preRetirementRateOfReturn;
        private final double retirementRateOfReturn;

        private Investments(double startingBalance, double preRetirementRateOfReturn, double retirementRateOfReturn) {
            this.startingBalance = startingBalance;
            this.preRetirementRateOfReturn = preRetirementRateOfReturn;
            this.retirementRateOfReturn = retirementRateOfReturn;
        }

        /**
         * Returns the starting balance of the investments.
         *
         * @return the starting balance
         */
        public double getStartingBalance() {
            return startingBalance;
        }

        /**
         * Returns the rate of return before retirement.
         *
         * @return the pre-retirement rate of return
         */
        public double getPreRetirementRateOfReturn() {
            return preRetirementRateOfReturn;
        }

        /**
         * Returns the rate of return during retirement.
         *
         * @return the retirement rate of return
         */
        public double getRetirementRateOfReturn() {
            return retirementRateOfReturn;
        }

        /**
         * Returns a new Investments builder.
         *
         * @return a builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder for {@link Investments}.
         */
        public static class Builder {
            private double startingBalance;
            private double preRetirementRateOfReturn;
            private double retirementRateOfReturn;

            private Builder() {
                //no-op
            }

            /**
             * Set the starting balance.
             *
             * @param startingBalance the starting balance
             * @return this builder
             */
            public Builder startingBalance(double startingBalance) {
                this.startingBalance = startingBalance;
                return this;
            }

            /**
             * Set the pre-retirement rate of return.
             *
             * @param preRetirementRateOfReturn the rate
             * @return this builder
             */
            public Builder preRetirementRateOfReturn(double preRetirementRateOfReturn) {
                this.preRetirementRateOfReturn = preRetirementRateOfReturn;
                return this;
            }

            /**
             * Set the retirement rate of return.
             *
             * @param retirementRateOfReturn the rate
             * @return this builder
             */
            public Builder retirementRateOfReturn(double retirementRateOfReturn) {
                this.retirementRateOfReturn = retirementRateOfReturn;
                return this;
            }

            /**
             * Build the Investments instance.
             *
             * @return a new Investments
             */
            public Investments build() {
                return new Investments(startingBalance, preRetirementRateOfReturn, retirementRateOfReturn);
            }
        }
    }

    /**
     * Represents a contribution to the retirement portfolio, including type, rate, and increment details.
     */
    public static class Contribution {
        private final ContributionType contributionType;
        private final double contributionRate;
        private final double incrementContributionRate;
        private final Month incrementMonth;

        private Contribution(ContributionType contributionType, double contributionRate,
                             double incrementContributionRate, Month incrementMonth) {
            this.contributionType = contributionType;
            this.contributionRate = contributionRate;
            this.incrementContributionRate = incrementContributionRate;
            this.incrementMonth = incrementMonth;
        }

        /**
         * Returns the type of contribution.
         *
         * @return the contribution type
         */
        public ContributionType getContributionType() {
            return contributionType;
        }

        /**
         * Returns the contribution rate.
         *
         * @return the contribution rate
         */
        public double getContributionRate() {
            return contributionRate;
        }

        /**
         * Returns the increment contribution rate.
         *
         * @return the increment contribution rate
         */
        public double getIncrementContributionRate() {
            return incrementContributionRate;
        }

        /**
         * Returns the month when the increment is applied.
         *
         * @return the increment month
         */
        public Month getIncrementMonth() {
            return incrementMonth;
        }

        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder for {@link Contribution}.
         */
        public static class Builder {
            private ContributionType contributionType;
            private double contributionRate = 0.0;
            private double incrementContributionRate = 0.0;
            private Month incrementMonth = Month.JANUARY;

            private Builder() {
                //no-op
            }

            /**
             * Set the contribution type.
             *
             * @param contributionType the type
             * @return this builder
             */
            public Builder contributionType(ContributionType contributionType) {
                this.contributionType = contributionType;
                return this;
            }

            /**
             * Set the contribution rate.
             *
             * @param contributionRate the rate
             * @return this builder
             */
            public Builder contributionRate(double contributionRate) {
                this.contributionRate = contributionRate;
                return this;
            }

            /**
             * Set the increment contribution rate.
             *
             * @param incrementContributionRate the increment rate
             * @return this builder
             */
            public Builder incrementContributionRate(double incrementContributionRate) {
                this.incrementContributionRate = incrementContributionRate;
                return this;
            }

            /**
             * Set the month for the increment.
             *
             * @param incrementMonth the month
             * @return this builder
             */
            public Builder incrementMonth(Month incrementMonth) {
                this.incrementMonth = incrementMonth;
                return this;
            }

            /**
             * Build the Contribution instance.
             *
             * @return a new Contribution
             */
            public Contribution build() {
                if (contributionType == null) {
                    throw new NullPointerException("contributionType is null");
                }

                return new Contribution(contributionType, contributionRate, incrementContributionRate, incrementMonth);
            }
        }
    }

    /**
     * Represents working income parameters, including base salary and cost-of-living adjustment percentage.
     */
    public static class WorkingIncome {
        private final double baseSalary;
        private final double colaPct;

        private WorkingIncome(double baseSalary, double colaPct) {
            this.baseSalary = baseSalary;
            this.colaPct = colaPct;
        }

        /**
         * Returns the base salary.
         *
         * @return the base salary
         */
        public double getBaseSalary() {
            return baseSalary;
        }

        /**
         * Returns the cost-of-living adjustment percentage.
         *
         * @return the COLA percentage
         */
        public double getColaPct() {
            return colaPct;
        }

        /**
         * Returns a new WorkingIncome builder.
         *
         * @return a builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder for {@link WorkingIncome}.
         */
        public static class Builder {
            private double baseSalary;
            private double colaPct;

            private Builder() {
                //no-op
            }

            /**
             * Set the base salary.
             *
             * @param baseSalary the base salary
             * @return this builder
             */
            public Builder baseSalary(double baseSalary) {
                this.baseSalary = baseSalary;
                return this;
            }

            /**
             * Set the cost-of-living adjustment percentage.
             *
             * @param colaPct the COLA percentage
             * @return this builder
             */
            public Builder colaPct(double colaPct) {
                this.colaPct = colaPct;
                return this;
            }

            /**
             * Build the WorkingIncome instance.
             *
             * @return a new WorkingIncome
             */
            public WorkingIncome build() {
                return new WorkingIncome(baseSalary, colaPct);
            }
        }
    }

    /**
     * Represents withdrawal income parameters, including withdrawal type and rate.
     */
    public static class WithdrawalIncome {
        private final WithdrawalType withdrawalType;
        private final double withdrawalRate;

        /**
         * Returns the withdrawal type.
         *
         * @return the withdrawal type
         */
        public WithdrawalType getWithdrawalType() {
            return withdrawalType;
        }

        /**
         * Returns the withdrawal rate.
         *
         * @return the withdrawal rate
         */
        public double getWithdrawalRate() {
            return withdrawalRate;
        }

        /**
         * Returns a new WithdrawalIncome builder.
         *
         * @return a builder
         */
        public static Builder builder() {
            return new Builder();
        }


        private WithdrawalIncome(WithdrawalType withdrawalType, double withdrawalRate) {
            this.withdrawalType = withdrawalType;
            this.withdrawalRate = withdrawalRate;
        }

        /**
         * Builder for {@link WithdrawalIncome}.
         */
        public static class Builder {
            private WithdrawalType withdrawalType;
            private double withdrawalRate;

            private Builder() {
                //no-op
            }

            /**
             * Set the withdrawal type.
             *
             * @param withdrawalType the type
             * @return this builder
             */
            public Builder withdrawalType(WithdrawalType withdrawalType) {
                this.withdrawalType = withdrawalType;
                return this;
            }

            /**
             * Set the withdrawal rate.
             *
             * @param withdrawalRate the rate
             * @return this builder
             */
            public Builder withdrawalRate(double withdrawalRate) {
                this.withdrawalRate = withdrawalRate;
                return this;
            }

            /**
             * Build the WithdrawalIncome instance.
             *
             * @return a new WithdrawalIncome
             */
            public WithdrawalIncome build() {
                return new WithdrawalIncome(withdrawalType, withdrawalRate);
            }
        }
    }

    /**
     * Represents monthly retirement income parameters, including Social Security and other income sources.
     */
    public static class MonthlyRetirementIncome {
        private final double socialSecurity;
        private final double socialSecurityAdjustmentRate;
        private final double otherMonthlyIncome;
        private final double otherMonthlyIncomeAdjustmentRate;
        private final LocalDate startSocialSecurity;
        private final LocalDate startOtherMonthlyIncome;

        private MonthlyRetirementIncome(double socialSecurity, double socialSecurityAdjustmentRate,
                                        double otherMonthlyIncome, double otherMonthlyIncomeAdjustmentRate,
                                        LocalDate startSocialSecurity, LocalDate startOtherMonthlyIncome) {
            this.socialSecurity = socialSecurity;
            this.socialSecurityAdjustmentRate = socialSecurityAdjustmentRate;
            this.otherMonthlyIncome = otherMonthlyIncome;
            this.otherMonthlyIncomeAdjustmentRate = otherMonthlyIncomeAdjustmentRate;
            this.startSocialSecurity = startSocialSecurity;
            this.startOtherMonthlyIncome = startOtherMonthlyIncome;
        }

        /**
         * Returns the monthly Social Security income.
         *
         * @return the Social Security income
         */
        public double getSocialSecurity() {
            return socialSecurity;
        }

        /**
         * Returns the adjustment rate for Social Security income.
         *
         * @return the Social Security adjustment rate
         */
        public double getSocialSecurityAdjustmentRate() {
            return socialSecurityAdjustmentRate;
        }

        /**
         * Returns the other monthly income amount.
         *
         * @return the other monthly income
         */
        public double getOtherMonthlyIncome() {
            return otherMonthlyIncome;
        }

        /**
         * Returns the adjustment rate for other monthly income.
         *
         * @return the other monthly income adjustment rate
         */
        public double getOtherMonthlyIncomeAdjustmentRate() {
            return otherMonthlyIncomeAdjustmentRate;
        }

        /**
         * Returns the start date for Social Security
         *
         * @return the date when Social Security distributions will start
         */
        public LocalDate getStartSocialSecurity() {
            return startSocialSecurity;
        }

        /**
         * Returns the start date for other monthly retirement income
         * @return the date to begin distributing other montly income
         */
        public LocalDate getStartOtherMonthlyIncome() {
            return startOtherMonthlyIncome;
        }

        /**
         * Returns a new MonthlyRetirementIncome builder.
         *
         * @return a builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder for {@link MonthlyRetirementIncome}.
         */
        public static class Builder {
            private double socialSecurity;
            private double socialSecurityAdjustmentRate;
            private double otherMonthlyIncome;
            private double otherMonthlyIncomeAdjustmentRate;
            private LocalDate startSocialSecurity;
            private LocalDate startOtherMonthlyIncome;

            private Builder() {
                //no-op
            }

            /**
             * Set the Social Security income amount.
             *
             * @param socialSecurity the amount
             * @return this builder
             */
            public Builder socialSecurity(double socialSecurity) {
                this.socialSecurity = socialSecurity;
                return this;
            }

            /**
             * Set the Social Security adjustment rate.
             *
             * @param socialSecurityAdjustmentRate the adjustment rate
             * @return this builder
             */
            public Builder socialSecurityAdjustmentRate(double socialSecurityAdjustmentRate) {
                this.socialSecurityAdjustmentRate = socialSecurityAdjustmentRate;
                return this;
            }

            /**
             * Set the other monthly income amount.
             *
             * @param otherMonthlyIncome the amount
             * @return this builder
             */
            public Builder otherMonthlyIncome(double otherMonthlyIncome) {
                this.otherMonthlyIncome = otherMonthlyIncome;
                return this;
            }

            /**
             * Set the other monthly income adjustment rate.
             *
             * @param otherMonthlyIncomeAdjustmentRate the adjustment rate
             * @return this builder
             */
            public Builder otherMonthlyIncomeAdjustmentRate(double otherMonthlyIncomeAdjustmentRate) {
                this.otherMonthlyIncomeAdjustmentRate = otherMonthlyIncomeAdjustmentRate;
                return this;
            }

            public Builder startSocialSecurity(LocalDate startSocialSecurity) {
                this.startSocialSecurity = startSocialSecurity;
                return this;
            }

            public Builder startOtherMonthlyIncome(LocalDate startOtherMonthlyIncome) {
                this.startOtherMonthlyIncome = startOtherMonthlyIncome;
                return this;
            }

            /**
             * Build the MonthlyRetirementIncome instance.
             *
             * @return a new MonthlyRetirementIncome
             */
            public MonthlyRetirementIncome build() {
                return new MonthlyRetirementIncome(socialSecurity, socialSecurityAdjustmentRate,
                    otherMonthlyIncome, otherMonthlyIncomeAdjustmentRate,
                    startSocialSecurity, startOtherMonthlyIncome);
            }
        }
    }
}
