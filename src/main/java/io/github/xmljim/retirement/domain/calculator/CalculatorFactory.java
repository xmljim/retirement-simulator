package io.github.xmljim.retirement.domain.calculator;

import io.github.xmljim.retirement.domain.calculator.impl.DefaultBenefitTaxationCalculator;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultContributionCalculator;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultContributionLimitChecker;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultContributionRouter;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultEarningsTestCalculator;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultIncomeCalculator;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultInflationCalculator;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultMAGICalculator;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultMedicareCalculator;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultPhaseOutCalculator;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultReturnCalculator;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultRmdCalculator;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultSocialSecurityCalculator;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultSpousalBenefitCalculator;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultWithdrawalCalculator;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultYTDContributionTracker;
import io.github.xmljim.retirement.domain.config.IraPhaseOutLimits;
import io.github.xmljim.retirement.domain.config.IrsContributionLimits;
import io.github.xmljim.retirement.domain.config.MedicareRules;
import io.github.xmljim.retirement.domain.config.RmdRules;

/**
 * Factory for obtaining calculator instances.
 *
 * <p>This factory provides singleton instances of the default calculator
 * implementations. All calculators are stateless, so sharing instances
 * is safe and efficient.
 *
 * <p>Usage:
 * <pre>{@code
 * InflationCalculator inflation = CalculatorFactory.inflationCalculator();
 * BigDecimal multiplier = inflation.calculateInflationMultiplier(
 *     new BigDecimal("0.03"), 10);
 * }</pre>
 *
 * <p>For testing or alternative implementations, you can instantiate
 * the calculator classes directly or create mock implementations.
 */
public final class CalculatorFactory {

    private static final InflationCalculator INFLATION_CALCULATOR =
        new DefaultInflationCalculator();

    private static final ContributionCalculator CONTRIBUTION_CALCULATOR =
        new DefaultContributionCalculator();

    private static final IncomeCalculator INCOME_CALCULATOR =
        new DefaultIncomeCalculator(INFLATION_CALCULATOR);

    private static final ReturnCalculator RETURN_CALCULATOR =
        new DefaultReturnCalculator();

    private static final WithdrawalCalculator WITHDRAWAL_CALCULATOR =
        new DefaultWithdrawalCalculator();

    private static final MAGICalculator MAGI_CALCULATOR =
        new DefaultMAGICalculator();

    private static final SocialSecurityCalculator SOCIAL_SECURITY_CALCULATOR =
        new DefaultSocialSecurityCalculator();

    private CalculatorFactory() {
        // Prevent instantiation
    }

    /**
     * Returns the inflation calculator instance.
     *
     * @return the inflation calculator
     */
    public static InflationCalculator inflationCalculator() {
        return INFLATION_CALCULATOR;
    }

    /**
     * Returns the contribution calculator instance.
     *
     * @return the contribution calculator
     */
    public static ContributionCalculator contributionCalculator() {
        return CONTRIBUTION_CALCULATOR;
    }

    /**
     * Returns the income calculator instance.
     *
     * @return the income calculator
     */
    public static IncomeCalculator incomeCalculator() {
        return INCOME_CALCULATOR;
    }

    /**
     * Returns the return calculator instance.
     *
     * @return the return calculator
     */
    public static ReturnCalculator returnCalculator() {
        return RETURN_CALCULATOR;
    }

    /**
     * Returns the withdrawal calculator instance.
     *
     * @return the withdrawal calculator
     */
    public static WithdrawalCalculator withdrawalCalculator() {
        return WITHDRAWAL_CALCULATOR;
    }

    /**
     * Returns the MAGI calculator instance.
     *
     * @return the MAGI calculator
     */
    public static MAGICalculator magiCalculator() {
        return MAGI_CALCULATOR;
    }

    /**
     * Returns the Social Security calculator instance.
     *
     * @return the Social Security calculator
     */
    public static SocialSecurityCalculator socialSecurityCalculator() {
        return SOCIAL_SECURITY_CALCULATOR;
    }

    /**
     * Returns a new spousal/survivor benefit calculator.
     *
     * <p>Unlike some other calculators, this creates a new instance each time
     * as it requires the base SS calculator and rules.
     *
     * @return a new SpousalBenefitCalculator instance
     */
    public static SpousalBenefitCalculator spousalBenefitCalculator() {
        return new DefaultSpousalBenefitCalculator();
    }

    /**
     * Creates a contribution router with the specified IRS rules.
     *
     * <p>Unlike other calculators that are singletons, the contribution router
     * requires IRS rules which are typically Spring-managed. This factory method
     * creates a new instance with the provided rules.
     *
     * <p>Usage:
     * <pre>{@code
     * // With Spring-managed rules
     * @Autowired
     * private IrsContributionRules irsRules;
     *
     * ContributionRouter router = CalculatorFactory.contributionRouter(irsRules);
     * }</pre>
     *
     * @param irsRules the IRS contribution rules for SECURE 2.0 compliance
     * @return a new ContributionRouter instance
     */
    public static ContributionRouter contributionRouter(IrsContributionRules irsRules) {
        return new DefaultContributionRouter(irsRules);
    }

    /**
     * Creates a new YTD contribution tracker.
     *
     * <p>The tracker is immutable - each {@code recordContribution()} call
     * returns a new tracker instance with the contribution added.
     *
     * <p>Usage:
     * <pre>{@code
     * YTDContributionTracker tracker = CalculatorFactory.ytdTracker(irsRules);
     *
     * // Record contributions (returns new immutable tracker)
     * tracker = tracker.recordContribution(record);
     *
     * // Get summary
     * YTDSummary summary = tracker.getSummary(2025, 55, true);
     * }</pre>
     *
     * @param irsRules the IRS contribution rules
     * @return a new empty YTDContributionTracker
     */
    public static YTDContributionTracker ytdTracker(IrsContributionRules irsRules) {
        return new DefaultYTDContributionTracker(irsRules);
    }

    /**
     * Creates a contribution limit checker.
     *
     * <p>The limit checker validates proposed contributions against IRS limits,
     * considering year-to-date contributions already made.
     *
     * <p>Usage:
     * <pre>{@code
     * ContributionLimitChecker checker = CalculatorFactory.limitChecker(irsRules, irsLimits);
     *
     * LimitCheckResult result = checker.check(
     *     tracker,                      // YTD tracker
     *     new BigDecimal("5000"),       // proposed amount
     *     AccountType.TRADITIONAL_401K, // target account
     *     ContributionType.PERSONAL,    // source
     *     2025,                         // year
     *     55,                           // age
     *     new BigDecimal("100000"),     // prior year income
     *     true                          // has spouse
     * );
     * }</pre>
     *
     * @param irsRules the IRS contribution rules
     * @param irsLimits the IRS contribution limits configuration
     * @return a new ContributionLimitChecker
     */
    public static ContributionLimitChecker limitChecker(
            IrsContributionRules irsRules, IrsContributionLimits irsLimits) {
        return new DefaultContributionLimitChecker(irsRules, irsLimits);
    }

    /**
     * Creates a phase-out calculator for IRA contributions.
     *
     * <p>The phase-out calculator determines allowed IRA contributions
     * based on MAGI and filing status, applying IRS income-based phase-out
     * rules for both Roth IRA eligibility and Traditional IRA deductibility.
     *
     * <p>Usage:
     * <pre>{@code
     * PhaseOutCalculator calculator = CalculatorFactory.phaseOutCalculator(
     *     phaseOutLimits, contributionLimits);
     *
     * PhaseOutResult result = calculator.calculateRothIraPhaseOut(
     *     new BigDecimal("155000"),      // MAGI
     *     FilingStatus.SINGLE,           // filing status
     *     2025,                          // year
     *     new BigDecimal("7000"),        // requested contribution
     *     45                             // age
     * );
     * }</pre>
     *
     * @param phaseOutLimits the IRA phase-out thresholds configuration
     * @param contributionLimits the IRS contribution limits configuration
     * @return a new PhaseOutCalculator instance
     */
    public static PhaseOutCalculator phaseOutCalculator(
            IraPhaseOutLimits phaseOutLimits, IrsContributionLimits contributionLimits) {
        return new DefaultPhaseOutCalculator(phaseOutLimits, contributionLimits);
    }

    /**
     * Returns a new Social Security earnings test calculator.
     *
     * <p>The earnings test determines how much of a beneficiary's Social Security
     * benefits are withheld when they earn income from work before Full Retirement Age.
     *
     * <p>Usage:
     * <pre>{@code
     * EarningsTestCalculator calculator = CalculatorFactory.earningsTestCalculator();
     *
     * EarningsTestResult result = calculator.calculate(
     *     new BigDecimal("50000"),  // annual earnings
     *     new BigDecimal("24000"),  // annual SS benefit
     *     780,                       // age in months (65 years)
     *     804,                       // FRA in months (67 years)
     *     2025                       // tax year
     * );
     * }</pre>
     *
     * @return a new EarningsTestCalculator instance
     */
    public static EarningsTestCalculator earningsTestCalculator() {
        return new DefaultEarningsTestCalculator();
    }

    /**
     * Returns a new Social Security benefit taxation calculator.
     *
     * <p>The taxation calculator determines what percentage of Social Security
     * benefits are taxable based on combined income and filing status.
     *
     * <p>Usage:
     * <pre>{@code
     * BenefitTaxationCalculator calculator = CalculatorFactory.benefitTaxationCalculator();
     *
     * TaxationResult result = calculator.calculate(
     *     new BigDecimal("24000"),  // annual SS benefit
     *     new BigDecimal("50000"),  // AGI
     *     new BigDecimal("1000"),   // non-taxable interest
     *     FilingStatus.SINGLE       // filing status
     * );
     * }</pre>
     *
     * @return a new BenefitTaxationCalculator instance
     */
    public static BenefitTaxationCalculator benefitTaxationCalculator() {
        return new DefaultBenefitTaxationCalculator();
    }

    /**
     * Returns a new Medicare premium calculator.
     *
     * <p>The Medicare calculator determines Part B premiums and IRMAA
     * surcharges based on MAGI and filing status.
     *
     * <p>Usage:
     * <pre>{@code
     * MedicareCalculator calculator = CalculatorFactory.medicareCalculator();
     *
     * MedicarePremiums premiums = calculator.calculatePremiums(
     *     new BigDecimal("150000"),  // MAGI (from 2 years prior)
     *     FilingStatus.SINGLE,       // filing status
     *     2025                       // Medicare year
     * );
     * }</pre>
     *
     * @return a new MedicareCalculator instance
     */
    public static MedicareCalculator medicareCalculator() {
        return new DefaultMedicareCalculator();
    }

    /**
     * Returns a new Medicare premium calculator with custom rules.
     *
     * <p>Use this when you have Spring-managed MedicareRules configuration.
     *
     * <p>Usage:
     * <pre>{@code
     * @Autowired
     * private MedicareRules medicareRules;
     *
     * MedicareCalculator calculator = CalculatorFactory.medicareCalculator(medicareRules);
     * }</pre>
     *
     * @param rules the Medicare rules configuration
     * @return a new MedicareCalculator instance with the provided rules
     */
    public static MedicareCalculator medicareCalculator(MedicareRules rules) {
        return new DefaultMedicareCalculator(rules);
    }

    /**
     * Returns a new RMD (Required Minimum Distribution) calculator.
     *
     * <p>The RMD calculator determines mandatory withdrawals from tax-deferred
     * retirement accounts based on age and IRS life expectancy tables.
     *
     * <p>Usage:
     * <pre>{@code
     * RmdCalculator calculator = CalculatorFactory.rmdCalculator();
     *
     * RmdProjection projection = calculator.calculate(
     *     new BigDecimal("500000"),  // prior year-end balance
     *     75,                         // age
     *     1950,                       // birth year
     *     2025                        // tax year
     * );
     * }</pre>
     *
     * @return a new RmdCalculator instance
     */
    public static RmdCalculator rmdCalculator() {
        return new DefaultRmdCalculator();
    }

    /**
     * Returns a new RMD calculator with custom rules.
     *
     * <p>Use this when you have Spring-managed RmdRules configuration.
     *
     * @param rules the RMD rules configuration
     * @return a new RmdCalculator instance with the provided rules
     */
    public static RmdCalculator rmdCalculator(RmdRules rules) {
        return new DefaultRmdCalculator(rules);
    }
}
