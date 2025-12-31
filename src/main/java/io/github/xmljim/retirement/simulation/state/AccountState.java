package io.github.xmljim.retirement.simulation.state;

import java.math.BigDecimal;
import java.math.RoundingMode;

import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.exception.ValidationException;
import io.github.xmljim.retirement.domain.model.InvestmentAccount;
import io.github.xmljim.retirement.domain.value.AccountSnapshot;

/**
 * Mutable internal state for tracking account balances during simulation.
 *
 * <p>This class wraps an {@link InvestmentAccount} and maintains its mutable
 * balance state during simulation. The simulation engine owns AccountState
 * instances; strategies only see immutable {@link AccountSnapshot}s.
 *
 * <p>Key operations:
 * <ul>
 *   <li>{@link #deposit(BigDecimal)} - Add funds (contributions)</li>
 *   <li>{@link #withdraw(BigDecimal)} - Remove funds (distributions)</li>
 *   <li>{@link #applyReturn(BigDecimal)} - Apply investment returns</li>
 *   <li>{@link #toSnapshot()} - Create immutable snapshot for strategies</li>
 * </ul>
 *
 * <p>Thread safety: This class is NOT thread-safe. Simulation state is
 * expected to be modified by a single thread per simulation run.
 *
 * @see InvestmentAccount
 * @see AccountSnapshot
 */
public final class AccountState {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final InvestmentAccount account;
    private BigDecimal currentBalance;

    /**
     * Creates an AccountState from an InvestmentAccount.
     *
     * @param account the source account (must not be null)
     * @throws MissingRequiredFieldException if account is null
     */
    public AccountState(InvestmentAccount account) {
        MissingRequiredFieldException.requireNonNull(account, "account");
        this.account = account;
        this.currentBalance = account.getBalance().setScale(SCALE, ROUNDING);
    }

    /**
     * Returns the underlying investment account.
     *
     * @return the investment account
     */
    public InvestmentAccount getAccount() {
        return account;
    }

    /**
     * Returns the account's unique identifier.
     *
     * @return the account ID
     */
    public String getAccountId() {
        return account.getId();
    }

    /**
     * Returns the account's display name.
     *
     * @return the account name
     */
    public String getAccountName() {
        return account.getName();
    }

    /**
     * Returns the current balance.
     *
     * @return the current balance
     */
    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    /**
     * Adds funds to the account (contribution).
     *
     * @param amount the amount to deposit (must be non-negative)
     * @throws ValidationException if amount is negative
     */
    public void deposit(BigDecimal amount) {
        if (amount == null) {
            return;
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Deposit amount cannot be negative", "amount");
        }
        currentBalance = currentBalance.add(amount).setScale(SCALE, ROUNDING);
    }

    /**
     * Removes funds from the account (distribution).
     *
     * <p>If the withdrawal amount exceeds the balance, only the available
     * balance is withdrawn (partial withdrawal).
     *
     * @param amount the amount to withdraw (must be non-negative)
     * @return the actual amount withdrawn (may be less than requested)
     * @throws ValidationException if amount is negative
     */
    public BigDecimal withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Withdrawal amount cannot be negative", "amount");
        }

        BigDecimal actualWithdrawal = amount.min(currentBalance);
        currentBalance = currentBalance.subtract(actualWithdrawal).setScale(SCALE, ROUNDING);
        return actualWithdrawal;
    }

    /**
     * Applies an investment return rate to the current balance.
     *
     * <p>The return is calculated as: balance * (1 + rate) - balance
     * This ensures compounding behavior.
     *
     * @param rate the return rate as a decimal (e.g., 0.07 for 7%)
     * @return the return amount (can be negative for losses)
     */
    public BigDecimal applyReturn(BigDecimal rate) {
        if (rate == null || currentBalance.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal returnAmount = currentBalance.multiply(rate).setScale(SCALE, ROUNDING);
        currentBalance = currentBalance.add(returnAmount).setScale(SCALE, ROUNDING);

        // Ensure balance doesn't go negative from losses
        if (currentBalance.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal actualLoss = returnAmount.subtract(currentBalance);
            currentBalance = BigDecimal.ZERO;
            return actualLoss.negate();
        }

        return returnAmount;
    }

    /**
     * Sets the balance to a specific value.
     *
     * <p>Use with caution - prefer deposit/withdraw for normal operations.
     * This is intended for initialization or special adjustments.
     *
     * @param balance the new balance (must be non-negative)
     * @throws ValidationException if balance is negative
     */
    public void setBalance(BigDecimal balance) {
        BigDecimal newBalance = balance != null ? balance : BigDecimal.ZERO;
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Balance cannot be negative", "balance");
        }
        this.currentBalance = newBalance.setScale(SCALE, ROUNDING);
    }

    /**
     * Indicates whether the account has a positive balance.
     *
     * @return true if balance is greater than zero
     */
    public boolean hasBalance() {
        return currentBalance.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Indicates whether the account is depleted (zero balance).
     *
     * @return true if balance is zero
     */
    public boolean isDepleted() {
        return currentBalance.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Creates an immutable snapshot of the current account state.
     *
     * <p>This snapshot is safe to pass to strategies for read-only access.
     *
     * @return an AccountSnapshot with current balance
     */
    public AccountSnapshot toSnapshot() {
        return new AccountSnapshot(
                account.getId(),
                account.getName(),
                account.getAccountType(),
                currentBalance,
                account.getTaxTreatment(),
                account.isSubjectToRmd(),
                account.getAllocation()
        );
    }

    @Override
    public String toString() {
        return "AccountState{" +
                "accountId='" + account.getId() + '\'' +
                ", accountName='" + account.getName() + '\'' +
                ", currentBalance=" + currentBalance +
                '}';
    }
}
