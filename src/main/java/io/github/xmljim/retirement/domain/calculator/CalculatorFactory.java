package io.github.xmljim.retirement.domain.calculator;

import io.github.xmljim.retirement.domain.calculator.impl.DefaultContributionCalculator;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultContributionLimitChecker;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultContributionRouter;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultIncomeCalculator;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultInflationCalculator;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultMAGICalculator;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultReturnCalculator;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultWithdrawalCalculator;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultYTDContributionTracker;
import io.github.xmljim.retirement.domain.config.IrsContributionLimits;

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
}
