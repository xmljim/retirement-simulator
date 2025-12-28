package io.github.xmljim.retirement.domain.config;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.xmljim.retirement.domain.enums.ExpenseCategory;

/**
 * Configuration for survivor expense adjustments.
 *
 * <p>Defines multipliers applied to each expense category when
 * transitioning from couple to single (survivor) status.
 *
 * <p>Research indicates survivors typically need 70-80% of couple expenses,
 * but adjustments vary by category:
 * <ul>
 *   <li>Fixed costs (housing): Unchanged</li>
 *   <li>Variable costs (food): ~60%</li>
 *   <li>Personal costs (healthcare): ~50% (one person)</li>
 * </ul>
 *
 * @see <a href="https://www.ebri.org">EBRI Research</a>
 */
@Configuration
@ConfigurationProperties(prefix = "survivor-expense")
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP",
    justification = "Spring configuration bean with mutable properties"
)
public class SurvivorExpenseRules {

    private BigDecimal defaultMultiplier = new BigDecimal("0.75");
    private Map<String, BigDecimal> categoryMultipliers = new HashMap<>();

    /**
     * Returns the default multiplier for uncategorized expenses.
     *
     * @return the default multiplier (typically 0.75)
     */
    public BigDecimal getDefaultMultiplier() {
        return defaultMultiplier;
    }

    /**
     * Sets the default multiplier.
     *
     * @param defaultMultiplier the default multiplier
     */
    public void setDefaultMultiplier(BigDecimal defaultMultiplier) {
        this.defaultMultiplier = defaultMultiplier;
    }

    /**
     * Returns the category-specific multipliers.
     *
     * @return map of category name to multiplier
     */
    public Map<String, BigDecimal> getCategoryMultipliers() {
        return categoryMultipliers;
    }

    /**
     * Sets the category-specific multipliers.
     *
     * @param categoryMultipliers map of category name to multiplier
     */
    public void setCategoryMultipliers(Map<String, BigDecimal> categoryMultipliers) {
        this.categoryMultipliers = categoryMultipliers;
    }

    /**
     * Returns the multiplier for a specific expense category.
     *
     * @param category the expense category
     * @return the multiplier, or default if not configured
     */
    public BigDecimal getMultiplier(ExpenseCategory category) {
        String key = category.name().toLowerCase().replace("_", "-");
        return categoryMultipliers.getOrDefault(key, defaultMultiplier);
    }

    /**
     * Creates default rules with standard multipliers.
     *
     * @return a new SurvivorExpenseRules with defaults
     */
    public static SurvivorExpenseRules withDefaults() {
        SurvivorExpenseRules rules = new SurvivorExpenseRules();
        rules.setDefaultMultiplier(new BigDecimal("0.75"));

        Map<String, BigDecimal> multipliers = new HashMap<>();
        // Fixed costs - unchanged
        multipliers.put("housing", new BigDecimal("1.00"));
        multipliers.put("debt-payments", new BigDecimal("1.00"));

        // Utilities - slight reduction
        multipliers.put("utilities", new BigDecimal("0.85"));

        // Variable costs - significant reduction
        multipliers.put("food", new BigDecimal("0.60"));
        multipliers.put("transportation", new BigDecimal("0.70"));
        multipliers.put("travel", new BigDecimal("0.60"));
        multipliers.put("entertainment", new BigDecimal("0.65"));
        multipliers.put("hobbies", new BigDecimal("0.65"));
        multipliers.put("insurance", new BigDecimal("0.75"));

        // Healthcare - one person
        multipliers.put("healthcare-oop", new BigDecimal("0.50"));
        multipliers.put("medicare-premiums", new BigDecimal("0.50"));
        multipliers.put("ltc-premiums", new BigDecimal("0.50"));
        multipliers.put("ltc-care", new BigDecimal("0.50"));

        // Contingency
        multipliers.put("home-repairs", new BigDecimal("1.00"));
        multipliers.put("vehicle-replacement", new BigDecimal("0.70"));
        multipliers.put("emergency-reserve", new BigDecimal("0.75"));

        // Other
        multipliers.put("gifts", new BigDecimal("0.75"));
        multipliers.put("taxes", new BigDecimal("0.75"));

        rules.setCategoryMultipliers(multipliers);
        return rules;
    }
}
