package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import io.github.xmljim.retirement.domain.enums.ContingencyType;

/**
 * A scheduled large expense that occurs at regular intervals.
 *
 * <p>Examples include vehicle replacement (every 7-10 years),
 * major home projects, or other predictable large purchases.
 *
 * @param name the expense name
 * @param type the contingency type
 * @param amount the expense amount
 * @param intervalYears how often the expense occurs (in years)
 * @param lastOccurrence when the expense last occurred
 * @param nextOccurrence when the expense will next occur
 */
public record ScheduledExpense(
    String name,
    ContingencyType type,
    BigDecimal amount,
    int intervalYears,
    LocalDate lastOccurrence,
    LocalDate nextOccurrence
) {

    private static final int MONTHS_PER_YEAR = 12;

    /**
     * Creates a scheduled expense with calculated next occurrence.
     *
     * @param name the expense name
     * @param type the contingency type
     * @param amount the expense amount
     * @param intervalYears how often (in years)
     * @param lastOccurrence when it last occurred
     * @return a new ScheduledExpense
     */
    public static ScheduledExpense of(
            String name,
            ContingencyType type,
            BigDecimal amount,
            int intervalYears,
            LocalDate lastOccurrence) {
        LocalDate next = lastOccurrence.plusYears(intervalYears);
        return new ScheduledExpense(name, type, amount, intervalYears, lastOccurrence, next);
    }

    /**
     * Creates a scheduled expense starting from now.
     *
     * @param name the expense name
     * @param type the contingency type
     * @param amount the expense amount
     * @param intervalYears how often (in years)
     * @return a new ScheduledExpense with next occurrence in intervalYears
     */
    public static ScheduledExpense startingNow(
            String name,
            ContingencyType type,
            BigDecimal amount,
            int intervalYears) {
        LocalDate now = LocalDate.now();
        return new ScheduledExpense(name, type, amount, intervalYears, now, now.plusYears(intervalYears));
    }

    /**
     * Returns the monthly amount to set aside for this expense.
     *
     * @return amount / (intervalYears * 12)
     */
    public BigDecimal getMonthlySetAside() {
        int totalMonths = intervalYears * MONTHS_PER_YEAR;
        return amount.divide(BigDecimal.valueOf(totalMonths), 2, RoundingMode.HALF_UP);
    }

    /**
     * Returns the annual amount to set aside for this expense.
     *
     * @return amount / intervalYears
     */
    public BigDecimal getAnnualSetAside() {
        return amount.divide(BigDecimal.valueOf(intervalYears), 2, RoundingMode.HALF_UP);
    }

    /**
     * Returns whether this expense is due within the given date range.
     *
     * @param from the start date (inclusive)
     * @param to the end date (inclusive)
     * @return true if nextOccurrence falls within the range
     */
    public boolean isDueWithin(LocalDate from, LocalDate to) {
        return !nextOccurrence.isBefore(from) && !nextOccurrence.isAfter(to);
    }

    /**
     * Returns whether this expense is due in the given year.
     *
     * @param year the year to check
     * @return true if nextOccurrence is in that year
     */
    public boolean isDueInYear(int year) {
        return nextOccurrence.getYear() == year;
    }

    /**
     * Creates a new ScheduledExpense after the expense has occurred.
     *
     * @return a new expense with updated dates
     */
    public ScheduledExpense afterOccurrence() {
        return new ScheduledExpense(
            name,
            type,
            amount,
            intervalYears,
            nextOccurrence,
            nextOccurrence.plusYears(intervalYears)
        );
    }

    /**
     * Creates a new ScheduledExpense with an updated amount.
     *
     * @param newAmount the new expense amount
     * @return a new expense with updated amount
     */
    public ScheduledExpense withAmount(BigDecimal newAmount) {
        return new ScheduledExpense(name, type, newAmount, intervalYears, lastOccurrence, nextOccurrence);
    }
}
