package io.github.xmljim.retirement.simulation.result;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Captures the financial flows for a single account during one month.
 *
 * <p>This record is the source of truth for account-level changes within
 * a simulation month. It tracks:
 * <ul>
 *   <li>Starting balance (beginning of month)</li>
 *   <li>Contributions (deposits during month)</li>
 *   <li>Withdrawals (distributions during month)</li>
 *   <li>Investment returns (growth/loss during month)</li>
 *   <li>Ending balance (end of month)</li>
 * </ul>
 *
 * <p>The relationship between fields:
 * <pre>
 * endingBalance = startingBalance + contributions - withdrawals + returns
 * </pre>
 *
 * <p>Note: Returns are calculated on post-transaction balances (conservative
 * modeling - withdrawn money doesn't earn that month's return).
 *
 * @param accountId the unique identifier of the account
 * @param accountName the human-readable account name
 * @param startingBalance the balance at the beginning of the month
 * @param contributions deposits made during the month (zero or positive)
 * @param withdrawals distributions made during the month (zero or positive)
 * @param returns investment returns for the month (can be negative)
 * @param endingBalance the balance at the end of the month
 *
 * @see MonthlySnapshot
 */
public record AccountMonthlyFlow(
        UUID accountId,
        String accountName,
        BigDecimal startingBalance,
        BigDecimal contributions,
        BigDecimal withdrawals,
        BigDecimal returns,
        BigDecimal endingBalance
) {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /**
     * Compact constructor with validation.
     */
    public AccountMonthlyFlow {
        if (accountId == null) {
            throw new IllegalArgumentException("accountId cannot be null");
        }
        if (accountName == null || accountName.isBlank()) {
            throw new IllegalArgumentException("accountName cannot be null or blank");
        }
        if (startingBalance == null) {
            startingBalance = BigDecimal.ZERO;
        }
        if (contributions == null) {
            contributions = BigDecimal.ZERO;
        }
        if (withdrawals == null) {
            withdrawals = BigDecimal.ZERO;
        }
        if (returns == null) {
            returns = BigDecimal.ZERO;
        }
        if (endingBalance == null) {
            endingBalance = BigDecimal.ZERO;
        }
    }

    /**
     * Calculates the net flow for this month.
     *
     * <p>Net flow represents the total change in account value:
     * <pre>
     * netFlow = contributions - withdrawals + returns
     * </pre>
     *
     * <p>A positive net flow indicates the account grew; negative indicates shrinkage.
     *
     * @return the net flow amount
     */
    public BigDecimal netFlow() {
        return contributions
                .subtract(withdrawals)
                .add(returns)
                .setScale(SCALE, ROUNDING);
    }

    /**
     * Calculates the net contribution (deposits minus withdrawals).
     *
     * <p>This excludes investment returns to show pure cash movement.
     *
     * @return contributions minus withdrawals
     */
    public BigDecimal netContribution() {
        return contributions.subtract(withdrawals).setScale(SCALE, ROUNDING);
    }

    /**
     * Indicates whether this account had any activity this month.
     *
     * @return true if there were contributions, withdrawals, or non-zero returns
     */
    public boolean hasActivity() {
        return contributions.compareTo(BigDecimal.ZERO) != 0
                || withdrawals.compareTo(BigDecimal.ZERO) != 0
                || returns.compareTo(BigDecimal.ZERO) != 0;
    }

    /**
     * Indicates whether the account balance increased this month.
     *
     * @return true if ending balance exceeds starting balance
     */
    public boolean hadGrowth() {
        return endingBalance.compareTo(startingBalance) > 0;
    }

    /**
     * Calculates the return percentage for this month.
     *
     * <p>Return percentage is calculated on the post-transaction balance
     * (starting + contributions - withdrawals).
     *
     * @return the return as a decimal (e.g., 0.01 for 1%), or ZERO if no base
     */
    public BigDecimal returnPercentage() {
        BigDecimal postTransactionBalance = startingBalance
                .add(contributions)
                .subtract(withdrawals);

        if (postTransactionBalance.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return returns
                .divide(postTransactionBalance, 6, ROUNDING)
                .setScale(6, ROUNDING);
    }

    /**
     * Creates a new flow with updated returns and ending balance.
     *
     * <p>This is used when returns are calculated after the initial flow is created.
     *
     * @param newReturns the calculated returns
     * @param newEndingBalance the new ending balance after returns
     * @return a new AccountMonthlyFlow with updated values
     */
    public AccountMonthlyFlow withReturns(BigDecimal newReturns, BigDecimal newEndingBalance) {
        return new AccountMonthlyFlow(
                accountId,
                accountName,
                startingBalance,
                contributions,
                withdrawals,
                newReturns,
                newEndingBalance
        );
    }

    /**
     * Creates a builder for constructing AccountMonthlyFlow instances.
     *
     * @param accountId the account identifier
     * @param accountName the account name
     * @return a new builder
     */
    public static Builder builder(UUID accountId, String accountName) {
        return new Builder(accountId, accountName);
    }

    /**
     * Builder for AccountMonthlyFlow.
     */
    public static final class Builder {
        private final UUID accountId;
        private final String accountName;
        private BigDecimal startingBalance = BigDecimal.ZERO;
        private BigDecimal contributions = BigDecimal.ZERO;
        private BigDecimal withdrawals = BigDecimal.ZERO;
        private BigDecimal returns = BigDecimal.ZERO;

        private Builder(UUID accountId, String accountName) {
            this.accountId = accountId;
            this.accountName = accountName;
        }

        /**
         * Sets the starting balance.
         *
         * @param balance the starting balance
         * @return this builder
         */
        public Builder startingBalance(BigDecimal balance) {
            this.startingBalance = balance;
            return this;
        }

        /**
         * Sets the contributions amount.
         *
         * @param amount the contributions
         * @return this builder
         */
        public Builder contributions(BigDecimal amount) {
            this.contributions = amount;
            return this;
        }

        /**
         * Sets the withdrawals amount.
         *
         * @param amount the withdrawals
         * @return this builder
         */
        public Builder withdrawals(BigDecimal amount) {
            this.withdrawals = amount;
            return this;
        }

        /**
         * Sets the returns amount.
         *
         * @param amount the returns
         * @return this builder
         */
        public Builder returns(BigDecimal amount) {
            this.returns = amount;
            return this;
        }

        /**
         * Builds the AccountMonthlyFlow, calculating endingBalance automatically.
         *
         * @return the constructed AccountMonthlyFlow
         */
        public AccountMonthlyFlow build() {
            BigDecimal endingBalance = startingBalance
                    .add(contributions)
                    .subtract(withdrawals)
                    .add(returns)
                    .setScale(SCALE, ROUNDING);

            return new AccountMonthlyFlow(
                    accountId,
                    accountName,
                    startingBalance,
                    contributions,
                    withdrawals,
                    returns,
                    endingBalance
            );
        }
    }
}
