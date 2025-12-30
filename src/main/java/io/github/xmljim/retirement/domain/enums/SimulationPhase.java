package io.github.xmljim.retirement.domain.enums;

/**
 * Represents the current phase of a person within a retirement simulation.
 *
 * <p>Each person in a simulation progresses through phases based on their
 * retirement date, withdrawal start date, and life status. In a couple
 * simulation, each person may be in different phases (staggered retirement).
 *
 * <p>Phase transitions are typically triggered by events:
 * <ul>
 *   <li>Retirement date reached → ACCUMULATION to DISTRIBUTION (or TRANSITION)</li>
 *   <li>Withdrawal start date reached → TRANSITION to DISTRIBUTION</li>
 *   <li>Spouse death → SURVIVOR</li>
 * </ul>
 *
 * @see io.github.xmljim.retirement.domain.value.MonthlySnapshot
 */
public enum SimulationPhase {

    /**
     * Working and contributing to retirement accounts.
     *
     * <p>During accumulation:
     * <ul>
     *   <li>Salary income is active</li>
     *   <li>Contributions are made to retirement accounts</li>
     *   <li>No withdrawals from retirement accounts</li>
     *   <li>Employer match applies</li>
     * </ul>
     */
    ACCUMULATION("Accumulation", "Working and contributing to retirement accounts"),

    /**
     * Retired but not yet withdrawing from portfolio.
     *
     * <p>This optional bridge period occurs when:
     * <ul>
     *   <li>Person has retired (no salary income)</li>
     *   <li>But has other income covering expenses (pension, part-time, etc.)</li>
     *   <li>Portfolio continues to grow without withdrawals</li>
     * </ul>
     *
     * <p>Not all simulations have a transition phase; many go directly
     * from ACCUMULATION to DISTRIBUTION.
     */
    TRANSITION("Transition", "Retired but not yet withdrawing from portfolio"),

    /**
     * Withdrawing from portfolio to fund retirement expenses.
     *
     * <p>During distribution:
     * <ul>
     *   <li>No salary income (retired)</li>
     *   <li>Social Security, pension, annuity income may be active</li>
     *   <li>Withdrawals from retirement accounts to cover income gap</li>
     *   <li>RMDs apply if over RMD age</li>
     *   <li>Spending strategy determines withdrawal amounts</li>
     * </ul>
     */
    DISTRIBUTION("Distribution", "Withdrawing from portfolio to fund retirement"),

    /**
     * One spouse has deceased; survivor continues alone.
     *
     * <p>In survivor phase:
     * <ul>
     *   <li>Expense adjustments apply (housing 70%, food 60%, etc.)</li>
     *   <li>Survivor Social Security benefits apply</li>
     *   <li>Accounts may be inherited or rolled over</li>
     *   <li>Single tax filing status</li>
     * </ul>
     */
    SURVIVOR("Survivor", "One spouse deceased; survivor continues");

    private final String displayName;
    private final String description;

    SimulationPhase(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Returns the human-readable display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns a brief description of this phase.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Indicates whether contributions are allowed in this phase.
     *
     * @return true if contributions can be made
     */
    public boolean allowsContributions() {
        return this == ACCUMULATION;
    }

    /**
     * Indicates whether withdrawals are expected in this phase.
     *
     * @return true if withdrawals typically occur
     */
    public boolean expectsWithdrawals() {
        return this == DISTRIBUTION || this == SURVIVOR;
    }

    /**
     * Indicates whether this is a retirement phase (not working).
     *
     * @return true if retired
     */
    public boolean isRetired() {
        return this != ACCUMULATION;
    }
}
