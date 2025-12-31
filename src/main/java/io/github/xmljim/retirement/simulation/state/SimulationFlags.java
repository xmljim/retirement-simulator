package io.github.xmljim.retirement.simulation.state;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.xmljim.retirement.domain.enums.ExpenseCategory;

/**
 * Tracks event-driven simulation flags.
 *
 * <p>SimulationFlags captures simulation-wide state that changes based on
 * events during the simulation (e.g., spouse death triggering survivor mode,
 * contingency events activating expense categories).
 *
 * <p>This record is immutable; state changes produce new instances via
 * the "with" methods. This supports Monte Carlo runs where each iteration
 * may have different event sequences.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Start with initial flags
 * SimulationFlags flags = SimulationFlags.initial();
 *
 * // Spouse death event triggers survivor mode
 * flags = flags.withSurvivorMode(true);
 *
 * // LTC event activates long-term care expenses
 * flags = flags.withContingencyActive(ExpenseCategory.LTC_CARE, true);
 * }</pre>
 *
 * @param survivorMode true if the primary person is now a survivor (spouse deceased)
 * @param contingencyActive set of expense categories with active contingencies
 * @param refillMode true if refill strategy should be applied to contingency reserves
 * @param custom custom flags for extensibility
 *
 * @see ExpenseCategory
 */
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "Collections are made unmodifiable in compact constructor"
)
public record SimulationFlags(
        boolean survivorMode,
        Set<ExpenseCategory> contingencyActive,
        boolean refillMode,
        Map<String, Object> custom
) {

    /**
     * Compact constructor with defensive copying.
     */
    public SimulationFlags {
        if (contingencyActive == null || contingencyActive.isEmpty()) {
            contingencyActive = Collections.emptySet();
        } else {
            contingencyActive = Collections.unmodifiableSet(EnumSet.copyOf(contingencyActive));
        }
        custom = custom == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(custom));
    }

    /**
     * Creates initial simulation flags with all defaults.
     *
     * <p>Default state:
     * <ul>
     *   <li>survivorMode: false</li>
     *   <li>contingencyActive: empty set</li>
     *   <li>refillMode: false</li>
     *   <li>custom: empty map</li>
     * </ul>
     *
     * @return initial flags instance
     */
    public static SimulationFlags initial() {
        return new SimulationFlags(
                false,
                Collections.emptySet(),
                false,
                Collections.emptyMap()
        );
    }

    /**
     * Returns a new instance with updated survivor mode.
     *
     * @param mode the new survivor mode value
     * @return new SimulationFlags with updated survivor mode
     */
    public SimulationFlags withSurvivorMode(boolean mode) {
        if (this.survivorMode == mode) {
            return this;
        }
        return new SimulationFlags(mode, contingencyActive, refillMode, custom);
    }

    /**
     * Returns a new instance with a contingency category activated or deactivated.
     *
     * @param category the expense category to modify
     * @param active true to activate, false to deactivate
     * @return new SimulationFlags with updated contingency state
     */
    public SimulationFlags withContingencyActive(ExpenseCategory category, boolean active) {
        if (category == null) {
            return this;
        }

        boolean currentlyActive = contingencyActive.contains(category);
        if (currentlyActive == active) {
            return this;
        }

        EnumSet<ExpenseCategory> newSet = contingencyActive.isEmpty()
                ? EnumSet.noneOf(ExpenseCategory.class)
                : EnumSet.copyOf(contingencyActive);

        if (active) {
            newSet.add(category);
        } else {
            newSet.remove(category);
        }

        return new SimulationFlags(survivorMode, newSet, refillMode, custom);
    }

    /**
     * Returns a new instance with updated refill mode.
     *
     * @param mode the new refill mode value
     * @return new SimulationFlags with updated refill mode
     */
    public SimulationFlags withRefillMode(boolean mode) {
        if (this.refillMode == mode) {
            return this;
        }
        return new SimulationFlags(survivorMode, contingencyActive, mode, custom);
    }

    /**
     * Returns a new instance with a custom flag set.
     *
     * @param key the flag key
     * @param value the flag value
     * @return new SimulationFlags with updated custom flag
     */
    public SimulationFlags withCustomFlag(String key, Object value) {
        if (key == null) {
            return this;
        }

        Map<String, Object> newCustom = new HashMap<>(custom);
        if (value == null) {
            newCustom.remove(key);
        } else {
            newCustom.put(key, value);
        }

        return new SimulationFlags(survivorMode, contingencyActive, refillMode, newCustom);
    }

    /**
     * Checks if a specific contingency category is active.
     *
     * @param category the category to check
     * @return true if the category is in the active set
     */
    public boolean isContingencyActive(ExpenseCategory category) {
        return category != null && contingencyActive.contains(category);
    }

    /**
     * Checks if any contingency is currently active.
     *
     * @return true if at least one contingency is active
     */
    public boolean hasActiveContingency() {
        return !contingencyActive.isEmpty();
    }

    /**
     * Returns a custom flag value.
     *
     * @param key the flag key
     * @param <T> the expected value type
     * @return the flag value, or null if not set
     */
    @SuppressWarnings("unchecked")
    public <T> T getCustomFlag(String key) {
        return (T) custom.get(key);
    }

    /**
     * Returns a custom flag value with a default.
     *
     * @param key the flag key
     * @param defaultValue the value to return if flag not set
     * @param <T> the value type
     * @return the flag value or default
     */
    @SuppressWarnings("unchecked")
    public <T> T getCustomFlag(String key, T defaultValue) {
        Object value = custom.get(key);
        return value != null ? (T) value : defaultValue;
    }
}
