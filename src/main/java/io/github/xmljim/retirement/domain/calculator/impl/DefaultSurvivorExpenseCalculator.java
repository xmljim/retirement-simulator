package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.xmljim.retirement.domain.calculator.SurvivorExpenseCalculator;
import io.github.xmljim.retirement.domain.config.SurvivorExpenseRules;
import io.github.xmljim.retirement.domain.enums.ExpenseCategory;
import io.github.xmljim.retirement.domain.enums.SurvivorRole;
import io.github.xmljim.retirement.domain.value.SurvivorAdjustment;

/**
 * Default implementation of survivor expense calculator.
 *
 * <p>Applies category-specific multipliers to calculate survivor expenses
 * after a spouse passes away.
 *
 * @see SurvivorExpenseCalculator
 * @see SurvivorExpenseRules
 */
@Service
public class DefaultSurvivorExpenseCalculator implements SurvivorExpenseCalculator {

    private static final int SCALE = 2;

    private final SurvivorExpenseRules rules;

    /**
     * Creates a calculator with Spring-managed rules.
     *
     * @param rules the survivor expense rules
     */
    @Autowired
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-managed beans")
    public DefaultSurvivorExpenseCalculator(SurvivorExpenseRules rules) {
        this.rules = rules;
    }

    /**
     * Creates a calculator with default rules.
     */
    public DefaultSurvivorExpenseCalculator() {
        this.rules = SurvivorExpenseRules.withDefaults();
    }

    @Override
    public Map<ExpenseCategory, BigDecimal> calculateSurvivorExpenses(
            Map<ExpenseCategory, BigDecimal> coupleExpenses,
            SurvivorRole survivorRole) {

        Map<ExpenseCategory, BigDecimal> survivorExpenses = new HashMap<>();

        coupleExpenses.forEach((category, amount) -> {
            BigDecimal adjusted = adjustExpense(category, amount);
            survivorExpenses.put(category, adjusted);
        });

        return survivorExpenses;
    }

    @Override
    public BigDecimal adjustExpense(ExpenseCategory category, BigDecimal coupleAmount) {
        BigDecimal multiplier = getMultiplier(category);
        return coupleAmount.multiply(multiplier).setScale(SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal getMultiplier(ExpenseCategory category) {
        return rules.getMultiplier(category);
    }

    @Override
    public SurvivorAdjustment getAdjustment(ExpenseCategory category) {
        BigDecimal multiplier = getMultiplier(category);
        String rationale = getRationale(category, multiplier);
        return new SurvivorAdjustment(category, multiplier, rationale);
    }

    @Override
    public BigDecimal getTotalCoupleExpenses(Map<ExpenseCategory, BigDecimal> coupleExpenses) {
        return coupleExpenses.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal getTotalSurvivorExpenses(
            Map<ExpenseCategory, BigDecimal> coupleExpenses,
            SurvivorRole survivorRole) {

        Map<ExpenseCategory, BigDecimal> survivorExpenses =
            calculateSurvivorExpenses(coupleExpenses, survivorRole);

        return survivorExpenses.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal getOverallReductionPercentage(
            Map<ExpenseCategory, BigDecimal> coupleExpenses,
            SurvivorRole survivorRole) {

        BigDecimal coupleTotal = getTotalCoupleExpenses(coupleExpenses);
        if (coupleTotal.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal survivorTotal = getTotalSurvivorExpenses(coupleExpenses, survivorRole);
        BigDecimal reduction = coupleTotal.subtract(survivorTotal);

        return reduction.divide(coupleTotal, 4, RoundingMode.HALF_UP);
    }

    private String getRationale(ExpenseCategory category, BigDecimal multiplier) {
        if (multiplier.compareTo(BigDecimal.ONE) == 0) {
            return "Fixed cost - unchanged for survivor";
        } else if (multiplier.compareTo(new BigDecimal("0.5")) == 0) {
            return "Per-person cost - single individual";
        } else if (multiplier.compareTo(new BigDecimal("0.6")) <= 0) {
            return "Variable cost - significant reduction for single person";
        } else if (multiplier.compareTo(new BigDecimal("0.85")) <= 0) {
            return "Shared cost - moderate reduction for survivor";
        } else {
            return "Slight reduction in survivor scenario";
        }
    }
}
