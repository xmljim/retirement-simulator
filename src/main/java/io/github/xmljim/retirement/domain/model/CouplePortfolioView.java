package io.github.xmljim.retirement.domain.model;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import io.github.xmljim.retirement.domain.calculator.IrsContributionRules;
import io.github.xmljim.retirement.domain.calculator.YTDContributionTracker;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.value.YTDSummary;

/**
 * Aggregate view across two portfolios for a couple.
 *
 * <p>Provides combined and individual views of portfolio balances,
 * account types, and year-to-date contribution tracking.
 *
 * <p>Example usage:
 * <pre>{@code
 * CouplePortfolioView view = CouplePortfolioView.of(
 *     primaryPortfolio,
 *     secondaryPortfolio,
 *     primaryTracker,
 *     secondaryTracker
 * );
 *
 * BigDecimal combined = view.getCombinedBalance();
 * BigDecimal primaryTotal = view.getPrimaryBalance();
 *
 * // Get YTD summary for primary spouse
 * YTDSummary primarySummary = view.getPrimaryYTDSummary(2025, 55, irsRules);
 * }</pre>
 */
public final class CouplePortfolioView {

    private final Portfolio primaryPortfolio;
    private final Portfolio secondaryPortfolio;
    private final YTDContributionTracker primaryTracker;
    private final YTDContributionTracker secondaryTracker;

    /**
     * Creates a couple portfolio view.
     *
     * @param primaryPortfolio the primary spouse's portfolio
     * @param secondaryPortfolio the secondary spouse's portfolio (may be null)
     * @param primaryTracker YTD tracker for primary spouse
     * @param secondaryTracker YTD tracker for secondary spouse (may be null)
     */
    public CouplePortfolioView(
            Portfolio primaryPortfolio,
            Portfolio secondaryPortfolio,
            YTDContributionTracker primaryTracker,
            YTDContributionTracker secondaryTracker) {

        MissingRequiredFieldException.requireNonNull(primaryPortfolio, "primaryPortfolio");
        MissingRequiredFieldException.requireNonNull(primaryTracker, "primaryTracker");

        this.primaryPortfolio = primaryPortfolio;
        this.secondaryPortfolio = secondaryPortfolio;
        this.primaryTracker = primaryTracker;
        this.secondaryTracker = secondaryTracker;
    }

    /**
     * Factory method for creating a couple view.
     *
     * @param primary the primary portfolio
     * @param secondary the secondary portfolio (may be null)
     * @param primaryTracker YTD tracker for primary
     * @param secondaryTracker YTD tracker for secondary (may be null)
     * @return a new CouplePortfolioView
     */
    public static CouplePortfolioView of(
            Portfolio primary,
            Portfolio secondary,
            YTDContributionTracker primaryTracker,
            YTDContributionTracker secondaryTracker) {
        return new CouplePortfolioView(primary, secondary, primaryTracker, secondaryTracker);
    }

    /**
     * Factory method for a single-person view.
     *
     * @param portfolio the portfolio
     * @param tracker the YTD tracker
     * @return a new CouplePortfolioView with only primary populated
     */
    public static CouplePortfolioView single(Portfolio portfolio, YTDContributionTracker tracker) {
        return new CouplePortfolioView(portfolio, null, tracker, null);
    }

    /**
     * Returns whether this view includes a secondary (spouse) portfolio.
     *
     * @return true if secondary portfolio exists
     */
    public boolean isCouple() {
        return secondaryPortfolio != null;
    }

    // ==================== Balance Methods ====================

    /**
     * Returns the combined balance across both portfolios.
     *
     * @return the total balance
     */
    public BigDecimal getCombinedBalance() {
        BigDecimal total = primaryPortfolio.getTotalBalance();
        if (secondaryPortfolio != null) {
            total = total.add(secondaryPortfolio.getTotalBalance());
        }
        return total;
    }

    /**
     * Returns the primary spouse's portfolio balance.
     *
     * @return the primary balance
     */
    public BigDecimal getPrimaryBalance() {
        return primaryPortfolio.getTotalBalance();
    }

    /**
     * Returns the secondary spouse's portfolio balance.
     *
     * @return the secondary balance, or zero if no secondary
     */
    public BigDecimal getSecondaryBalance() {
        return secondaryPortfolio != null ? secondaryPortfolio.getTotalBalance() : BigDecimal.ZERO;
    }

    /**
     * Returns combined balances grouped by account type.
     *
     * @return map of account type to total balance across both portfolios
     */
    public Map<AccountType, BigDecimal> getBalancesByType() {
        Map<AccountType, BigDecimal> combined = new EnumMap<>(AccountType.class);

        // Add primary balances
        primaryPortfolio.getBalancesByType().forEach(
            (type, balance) -> combined.merge(type, balance, BigDecimal::add));

        // Add secondary balances
        if (secondaryPortfolio != null) {
            secondaryPortfolio.getBalancesByType().forEach(
                (type, balance) -> combined.merge(type, balance, BigDecimal::add));
        }

        return combined;
    }

    /**
     * Returns primary spouse's balances by account type.
     *
     * @return map of account type to balance
     */
    public Map<AccountType, BigDecimal> getPrimaryBalancesByType() {
        return primaryPortfolio.getBalancesByType();
    }

    /**
     * Returns secondary spouse's balances by account type.
     *
     * @return map of account type to balance, or empty map if no secondary
     */
    public Map<AccountType, BigDecimal> getSecondaryBalancesByType() {
        return secondaryPortfolio != null
            ? secondaryPortfolio.getBalancesByType()
            : new EnumMap<>(AccountType.class);
    }

    // ==================== Portfolio Access ====================

    /**
     * Returns the primary portfolio.
     *
     * @return the primary portfolio
     */
    public Portfolio getPrimaryPortfolio() {
        return primaryPortfolio;
    }

    /**
     * Returns the secondary portfolio, if present.
     *
     * @return the secondary portfolio, or null if single
     */
    public Portfolio getSecondaryPortfolio() {
        return secondaryPortfolio;
    }

    // ==================== YTD Summary Methods ====================

    /**
     * Returns YTD summary for the primary spouse.
     *
     * @param year the tax year
     * @param age the primary spouse's age
     * @param irsRules the IRS rules for calculating limits
     * @return the YTD summary
     */
    public YTDSummary getPrimaryYTDSummary(int year, int age, IrsContributionRules irsRules) {
        // Primary always has family coverage if this is a couple
        boolean hasSpouse = isCouple();
        return primaryTracker.getSummary(year, age, hasSpouse, irsRules);
    }

    /**
     * Returns YTD summary for the secondary spouse.
     *
     * @param year the tax year
     * @param age the secondary spouse's age
     * @param irsRules the IRS rules for calculating limits
     * @return the YTD summary, or null if no secondary
     */
    public YTDSummary getSecondaryYTDSummary(int year, int age, IrsContributionRules irsRules) {
        if (secondaryTracker == null) {
            return null;
        }
        // Secondary has family coverage (they're part of the couple)
        return secondaryTracker.getSummary(year, age, true, irsRules);
    }

    /**
     * Returns the primary YTD tracker.
     *
     * @return the primary tracker
     */
    public YTDContributionTracker getPrimaryTracker() {
        return primaryTracker;
    }

    /**
     * Returns the secondary YTD tracker.
     *
     * @return the secondary tracker, or null if single
     */
    public YTDContributionTracker getSecondaryTracker() {
        return secondaryTracker;
    }

    @Override
    public String toString() {
        return "CouplePortfolioView{" +
            "isCouple=" + isCouple() +
            ", combinedBalance=" + getCombinedBalance() +
            ", primaryBalance=" + getPrimaryBalance() +
            ", secondaryBalance=" + getSecondaryBalance() +
            '}';
    }
}
