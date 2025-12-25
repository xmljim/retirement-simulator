package io.github.xmljim.retirement.functions;

import io.github.xmljim.retirement.model.PortfolioParameters;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A utility class providing a set of common financial and time-based calculations
 * implemented as functional interfaces and lambda expressions.
 *
 * @deprecated This class uses deprecated PortfolioParameters. Will be refactored in Issue #8.
 *
 * <p>The class provides constants for common operations such as calculating
 * inflation, cost of living adjustments, and checking retirement status.</p>
 *
 * <p>This class cannot be instantiated.</p>
 *
 * @since 1.0
 */
@SuppressWarnings("deprecation")
public final class Functions {

    /**
     * Represents a calculation for an inflation multiplier or future value factor.
     *
     * <p>This is a functional interface that extends {@link BiFunction}. Its primary
     * method, inherited from the parent interface, takes an annual inflation/growth
     * rate and a number of years, returning a multiplier representing the growth factor.
     *
     * <p>The implemented function calculates `(1 + rate)^years` or `rate^years`
     * depending on how the rate is interpreted by the implementing class.
     *
     * @see BiFunction
     * @since 1.0
     */
    @FunctionalInterface
    public interface Inflation extends BiFunction<Double, Number, Double> {
        // The apply method signature is implicitly inherited:
        // Double apply(Double rate, Number years);
    }

    @FunctionalInterface
    public interface InflatedValue {
        Double apply(Double value, Double rate, Number years);
    }

    /**
     * Represents a calculation for a cost-of-living adjusted (COLA) salary.
     *
     * <p>This is a functional interface whose abstract method {@link #apply(Double, Double, Number)}
     * takes an initial salary, an annual inflation/adjustment rate, and a number of years,
     * and returns the future value of the salary as a {@code Double}.
     *
     * @since 1.0
     */
    @FunctionalInterface
    public interface Cola {
        /**
         * Calculates the future value of a salary after applying a cost-of-living adjustment (COLA).
         *
         * @param salary The initial base salary amount.
         * @param rate   The annual adjustment rate (e.g., 0.03 for 3% or 1.03 as a factor).
         * @param years  The number of years over which the adjustment is applied. Can be an integer or a double.
         * @return The calculated future value of the salary as a {@code Double}.
         */
        Double apply(Double salary, Double rate, Number years);

    }

    /**
     * Represents a calculation for an individual's contribution amount based on
     * various parameters.
     *
     * <p>This is a functional interface whose abstract method {@link #apply(LocalDate, LocalDate, Double, Double, Month)}
     * takes a con
     */
    @FunctionalInterface
    public interface IndividualContribution {
        /**
         * Calculates an individual's contribution amount.
         *
         * @param contributionDate The specific date of the contribution being evaluated.
         * @param retirementDate   The individual's projected retirement date.
         * @param basePercent      The initial base percentage rate of the contribution.
         * @param incrementPct     The percentage by which the contribution rate increases periodically.
         * @param incrementMonth   The specific month during which the increment percentage is applied.
         * @return The calculated contribution amount as a {@code Double}.
         */
        Double apply(LocalDate contributionDate,
                     LocalDate retirementDate,
                     Double basePercent,
                     Double incrementPct, Month incrementMonth);
    }

    /**
     * Represents a calculation for an employer's contribution amount or percentage.
     *
     * <p>This is a functional interface whose abstract method {@link #apply(LocalDate, LocalDate, Double)}
     * takes the current contribution date, the employee's retirement date, and a
     * base contribution percentage, returning a calculated contribution value as a {@code Double}.
     *
     * @since 1.0
     */
    @FunctionalInterface
    public interface EmployerContribution {
        /**
         * Calculates the employer's contribution amount or percentage based on the
         * current date relative to the retirement date.
         *
         * <p>Typically, this calculation will return {@code 0.0} if the employee is
         * already retired.
         *
         * @param contributionDate The specific date of the contribution being evaluated.
         * @param retirementDate   The employee's projected or actual retirement date.
         * @param contributionPct  The base or standard employer contribution percentage/rate.
         * @return The calculated employer contribution value as a {@code Double}.
         */
        Double apply(LocalDate contributionDate, LocalDate retirementDate, Double contributionPct);
    }

    /**
     * Represents a complex calculation for an individual's specific monthly salary
     * amount, considering cost-of-living adjustments (COLA), inflation effects,
     * retirement status, and specific dates.
     *
     * <p>This is a functional interface whose abstract method {@link #apply(Double, Double, Double, Double, LocalDate, LocalDate)}
     * orchestrates several inputs and helper functions (provided as {@link Supplier}s)
     * to determine the actual take-home or current salary value for a given month.
     *
     * @since 1.0
     */
    @FunctionalInterface
    public interface MonthlySalary {
        /**
         * Calculates the effective monthly salary amount for a given date.
         *
         * <p>The suppliers for COLA and Inflation allow the calculation logic within the
         * implementation to defer exactly *how* those rates are determined until execution time.
         *
         * @param salary         The base annual salary amount before adjustments.
         * @param colaPct        A {@link Supplier} that provides the current COLA adjustment factor or percentage.
         * @param inflationRate  A {@link Supplier} that provides the current inflation multiplier/rate.
         * @param retirementPct  The percentage to apply to the salary if the individual is retired (e.g., a pension factor).
         * @param salaryDate     The specific date for which the salary is being calculated (e.g., current month).
         * @param retirementDate The individual's projected or actual retirement date.
         * @return The calculated effective monthly salary amount as a {@code Double}.
         */
        Double apply(Double salary, Double colaPct,
                     Double inflationRate,
                     Double retirementPct,
                     LocalDate salaryDate,
                     LocalDate retirementDate);


    }

    /**
     * Represents a calculation for a monthly social security distribution amount.
     *
     * <p>This is a functional interface whose abstract method {@link #apply(Double, Double, LocalDate, LocalDate)}
     * takes a base social security amount, an inflation rate, the specific distribution date,
     * and the individual's retirement date, returning the adjusted monthly amount as a {@code Double}.
     *
     * @since 1.0
     */
    @FunctionalInterface
    public interface SocialSecurity {
        /**
         * Calculates the monthly social security distribution amount, adjusted for inflation.
         *
         * @param monthlySocialSecurityBase The initial base amount of social security benefit before inflation adjustments.
         * @param inflationRate             The annual inflation rate to apply to the base amount.
         * @param distributionDate          The specific date for which the distribution is being calculated (the current date).
         * @param retirementDate            The individual's projected or actual retirement date.
         * @return The calculated, inflation-adjusted monthly social security amount as a {@code Double}.
         */
        Double apply(Double monthlySocialSecurityBase, Double inflationRate, LocalDate distributionDate,
                     LocalDate retirementDate);

        /**
         * Calculates the monthly Social Security distribution for a given portfolio and date.
         * <p>
         * Uses the {@link SocialSecurity#apply} function with the portfolio's Social Security base amount,
         * adjustment rate, the specified distribution date, and the start date for Social Security.
         * </p>
         *
         * @param parameters         the {@link PortfolioParameters} containing retirement income details
         * @param distributionDate   the date for which the distribution is being calculated
         * @return the inflation-adjusted monthly Social Security amount for the given date
         */
        static Double getDistribution(PortfolioParameters parameters, LocalDate distributionDate) {
            return SOCIAL_SECURITY.apply(parameters.getMonthlyRetirementIncome().getSocialSecurity(),
                parameters.getMonthlyRetirementIncome().getSocialSecurityAdjustmentRate(), distributionDate,
                parameters.getMonthlyRetirementIncome().getStartSocialSecurity());
        }
    }

    /**
     * Represents a calculation for other monthly retirement income, such as pensions or annuities.
     * <p>
     * The abstract method {@link #apply(Double, Double, LocalDate, LocalDate)} takes the base other retirement income amount,
     * an annual increment percentage, the distribution date, and the retirement date, returning the adjusted monthly amount.
     */
    public interface OtherRetirementIncome {
        /**
         * Calculates the monthly other retirement income amount, adjusted for annual increments.
         *
         * @param otherRetirementIncome The initial base amount of other retirement income before adjustments.
         * @param incrementPct          The annual increment percentage to apply to the base amount.
         * @param distributionDate      The specific date for which the distribution is being calculated.
         * @param retirementDate        The individual's projected or actual retirement date.
         * @return The calculated, increment-adjusted monthly other retirement income as a {@code Double}.
         */
        Double apply(Double otherRetirementIncome, Double incrementPct, LocalDate distributionDate,
                     LocalDate retirementDate);

        static Double getDistribution(PortfolioParameters parameters, LocalDate distributionDate) {
            return OTHER_RETIREMENT_INCOME.apply(parameters.getMonthlyRetirementIncome().getOtherMonthlyIncome(),
                parameters.getMonthlyRetirementIncome().getOtherMonthlyIncomeAdjustmentRate(),
                distributionDate,
                parameters.getMonthlyRetirementIncome().getStartOtherMonthlyIncome());
        }
    }

    private Functions() {
        // Prevents instantiation
    }

    /**
     * A {@link Functions.Inflation} function that calculates the future value multiplier
     * based on an annual rate and a number of years.
     *
     * <p>The rate can be provided as a percentage (e.g., 0.03 for 3%) or a factor (e.g., 1.03).
     *
     * <p>Example: `INFLATION.apply(0.03, 10)` returns approximately `1.3439`.
     */
    public static final Inflation INFLATION = (rate, years) ->
        Math.pow(rate < 1 ? 1 + rate : rate, years.doubleValue());

    /**
     * A {@link BiFunction} that calculates the present value adjustment factor (the inverse of inflation).
     *
     * <p>This is used to discount future values back to their present value equivalent.
     *
     * <p>Example: `ADJUSTED.apply(0.03, 10)` returns approximately `0.7441`.
     */
    public static final BiFunction<Double, Number, Double> ADJUSTED = (rate, years) ->
        1 / INFLATION.apply(rate, years);

    /**
     * A {@link Functions.Cola} function that calculates a cost-of-living adjusted
     * (COLA) salary after a number of years.
     *
     * <p>Example: `COLA.apply(50000.0, 0.03, 10)` returns approximately `67195.82`.
     */
    public static final Cola COLA = (salary, rate, years) ->
        salary * INFLATION.apply(rate, years);

    public static final InflatedValue INFLATED = (salary, rate, years) ->
        salary * INFLATION.apply(rate, years);

    /**
     * A {@link BiFunction} that checks if the {@code currentDate} is on or after
     * the {@code retirementDate}.
     *
     * <p>Returns {@code true} if retired, {@code false} otherwise.
     */
    public static final BiFunction<LocalDate, LocalDate, Boolean> IS_RETIRED = (currentDate, retirementDate) ->
        currentDate.isAfter(retirementDate) || currentDate.isEqual(retirementDate);

    /**
     * An {@link IndividualContribution} function that calculates the individual's
     * contribution percentage based on their current status relative to retirement.
     *
     * <p>If retired (as determined by {@link #IS_RETIRED}), the contribution is {@code 0.0}.
     * Otherwise, it calculates a tiered contribution based on the elapsed time until retirement,
     * a base percentage, a periodic increment, and the specific month the increment applies.
     */
    public static final IndividualContribution INDIVIDUAL_CONTRIBUTION =
        (contributionDate,
         retirementDate,
         basePercent,
         incrementPct, incrementMonth) -> {
            return IS_RETIRED.andThen(retired -> {
                if (retired) {
                    return 0.0;
                } else {
                    double base = basePercent;
                    double increment = incrementPct;

                    // Calculate the number of full years until retirement to apply increments
                    long months = Math.abs(ChronoUnit.MONTHS.between(LocalDate.now(), contributionDate));
                    double multiplier = Math.floor((float) months / 12);
                    return base + (increment * multiplier) +
                        (contributionDate.getMonth().getValue() >= incrementMonth.getValue() ? increment : 0);
                }
            }).apply(contributionDate, retirementDate);
        };

    /**
     * An {@link EmployerContribution} function that determines the employer's
     * contribution percentage based strictly on the employee's retirement status.
     *
     * <p>This function uses the {@link #IS_RETIRED} predicate internally.
     * If the {@code contributionDate} is on or after the {@code retirementDate},
     * the contribution is {@code 0.0}; otherwise, the full {@code contributionPct}
     * is returned.
     *
     * @see #IS_RETIRED
     */
    public static EmployerContribution EMPLOYER_CONTRIBUTION =
        (contributionDate, retirementDate, contributionPct) ->
            IS_RETIRED.andThen(retired -> retired ? 0.0 : contributionPct)
                .apply(contributionDate, retirementDate);


    /**
     * A {@link MonthlySalary} function that calculates an individual's monthly salary
     * based on whether they are actively working or retired.
     *
     * <p>This function uses the {@link #IS_RETIRED} predicate to branch the calculation logic:
     * <ul>
     *     <li>**If retired:** The monthly payment is calculated as the base annual salary, divided by 12,
     *     adjusted by the current {@code inflation} factor, and then multiplied by the
     *     {@code retirementPct} (pension factor).</li>
     *     <li>**If actively working:** The monthly salary is calculated as the base annual salary,
     *     divided by 12, adjusted only by the current {@code cola} (Cost of Living Adjustment) factor.</li>
     * </ul>
     *
     * @see #IS_RETIRED
     */
    public static final MonthlySalary MONTHLY_SALARY =
        (salary, cola, inflation, retirementPct, salaryDate,
         retirementDate) ->
            IS_RETIRED.andThen(retired -> {
                LocalDate base = LocalDate.of(LocalDate.now().getYear(), 1, 1);
                if (retired) {
                    return INFLATED.apply(salary/12, inflation, salaryDate.getYear() - base.getYear())
                        * retirementPct;
                } else {
                    return COLA.apply(salary / 12, cola, salaryDate.getYear() - base.getYear());
                }
            }).apply(salaryDate, retirementDate);


    /**
     * A {@link SocialSecurity} function that calculates the inflation-adjusted social security
     * benefit, paid only if the individual is retired.
     *
     * <p>This function uses the {@link #IS_RETIRED} predicate to determine eligibility.
     * If the individual is retired ({@code distributionDate} is on or after {@code retirementDate}),
     * the {@code monthlySocialSecurityBase} is adjusted for inflation using the
     * {@code INFLATED} function (assumed to be a helper function defined elsewhere in `Functions`).
     * The number of years for inflation is calculated as the difference in years between
     * the current date (`LocalDate.now()`) and the {@code distributionDate}.
     *
     * <p>If the individual is not retired, the benefit is {@code 0.0}.
     *
     * @see #IS_RETIRED
     * @see Functions.Inflation (assumed contract for INFLATED helper)
     */
    public static final SocialSecurity SOCIAL_SECURITY =
        (monthlySocialSecurityBase, inflationRate, distributionDate, retirementDate) ->
            IS_RETIRED.andThen(retired -> {
                return retired ? INFLATED.apply(monthlySocialSecurityBase, inflationRate,
                    distributionDate.getYear() - LocalDate.now().getYear()) : 0.0;
            }).apply(distributionDate, retirementDate);
    
    /**
     * An {@link OtherRetirementIncome} function that calculates the increment-adjusted other monthly retirement income,
     * paid only if the individual is retired.
     *
     * <p>This function uses the {@link #IS_RETIRED} predicate to determine eligibility. If the individual is retired
     * ({@code distributionDate} is on or after {@code retirementDate}), the {@code otherRetirementIncome} is adjusted
     * for annual increments using the {@code INFLATED} function. The number of years for increment is calculated as the
     * difference in years between the distribution date and the retirement date. If not retired, the benefit is {@code 0.0}.
     */
    public static final OtherRetirementIncome OTHER_RETIREMENT_INCOME =
        (otherRetirementIncome, incrementPct, distributionDate, retirementDate) ->
            IS_RETIRED.andThen(retired -> {
                return retired && otherRetirementIncome > 0 ? INFLATED.apply(otherRetirementIncome, incrementPct,
                    distributionDate.getYear() - retirementDate.getYear()) : 0.0;
            }).apply(distributionDate, retirementDate);
}
