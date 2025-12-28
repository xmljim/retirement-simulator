package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.xmljim.retirement.domain.calculator.MedicareCalculator;
import io.github.xmljim.retirement.domain.config.MedicareRules;
import io.github.xmljim.retirement.domain.config.MedicareRules.IrmaaBracketConfig;
import io.github.xmljim.retirement.domain.enums.FilingStatus;
import io.github.xmljim.retirement.domain.value.MedicarePremiums;
import io.github.xmljim.retirement.domain.value.MedicarePremiums.IrmaaBracket;

/**
 * Default implementation of Medicare premium calculator.
 *
 * <p>Calculates Medicare premiums including IRMAA surcharges based on
 * MAGI and filing status. Uses configuration from YAML files for
 * year-specific premium tables and IRMAA brackets.
 *
 * <p>Key features:
 * <ul>
 *   <li>Supports multiple years of premium data</li>
 *   <li>Handles all filing statuses with appropriate thresholds</li>
 *   <li>Calculates Part A, Part B, and Part D components</li>
 * </ul>
 *
 * <p><b>Important:</b> IRMAA is based on MAGI from 2 years prior.
 * The caller is responsible for providing the correct MAGI year.
 *
 * @see MedicareCalculator
 * @see MedicareRules
 */
@Service
public class DefaultMedicareCalculator implements MedicareCalculator {

    private static final int PREMIUM_FREE_QUARTERS = 40;
    private static final int REDUCED_PREMIUM_QUARTERS = 30;
    private static final BigDecimal DEFAULT_PART_B_BASE = new BigDecimal("185.00");
    private static final BigDecimal DEFAULT_PART_A_FULL = new BigDecimal("518");
    private static final BigDecimal DEFAULT_PART_A_REDUCED = new BigDecimal("285");

    private final MedicareRules rules;

    /**
     * Creates a calculator with Spring-managed rules.
     *
     * @param rules the Medicare rules configuration
     */
    @Autowired
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-managed beans")
    public DefaultMedicareCalculator(MedicareRules rules) {
        this.rules = rules;
    }

    /**
     * Creates a calculator with default rules.
     */
    public DefaultMedicareCalculator() {
        this.rules = new MedicareRules();
    }

    @Override
    public MedicarePremiums calculatePremiums(
            BigDecimal magi,
            FilingStatus filingStatus,
            int year,
            boolean premiumFreePartA) {

        BigDecimal partAPremium = premiumFreePartA
            ? BigDecimal.ZERO
            : getPartAPremium(year, 0);  // Assume under 30 quarters if not premium-free

        BigDecimal partBBase = getPartBBasePremium(year);
        IrmaaBracket bracket = getIrmaaBracket(magi, filingStatus, year);

        if (bracket == IrmaaBracket.BRACKET_0) {
            return MedicarePremiums.standard(partAPremium, partBBase, year);
        }

        // Find matching IRMAA bracket from configuration
        Optional<IrmaaBracketConfig> bracketConfig = findBracketConfig(magi, filingStatus, year);

        BigDecimal partBIrmaa = bracketConfig
            .map(config -> config.getPartBIrmaa(partBBase))
            .orElse(BigDecimal.ZERO);

        BigDecimal partDIrmaa = bracketConfig
            .map(IrmaaBracketConfig::getPartDIrmaa)
            .orElse(BigDecimal.ZERO);

        return MedicarePremiums.withIrmaa(
            partAPremium,
            partBBase,
            partBIrmaa,
            partDIrmaa,
            bracket,
            year
        );
    }

    @Override
    public BigDecimal getPartBBasePremium(int year) {
        return rules.getPartBBase(year);
    }

    @Override
    public BigDecimal getPartBPremium(
            BigDecimal magi,
            FilingStatus filingStatus,
            int year) {

        BigDecimal basePremium = getPartBBasePremium(year);

        Optional<IrmaaBracketConfig> bracketConfig = findBracketConfig(magi, filingStatus, year);
        return bracketConfig
            .map(IrmaaBracketConfig::getPartBPremium)
            .orElse(basePremium);
    }

    @Override
    public BigDecimal getPartDIrmaa(
            BigDecimal magi,
            FilingStatus filingStatus,
            int year) {

        return findBracketConfig(magi, filingStatus, year)
            .map(IrmaaBracketConfig::getPartDIrmaa)
            .orElse(BigDecimal.ZERO);
    }

    @Override
    public IrmaaBracket getIrmaaBracket(
            BigDecimal magi,
            FilingStatus filingStatus,
            int year) {

        List<IrmaaBracketConfig> brackets = rules.getIrmaaBrackets(year);
        if (brackets.isEmpty()) {
            return IrmaaBracket.BRACKET_0;
        }

        int bracketIndex = 0;
        for (int i = 0; i < brackets.size(); i++) {
            if (isInBracket(magi, filingStatus, brackets.get(i))) {
                bracketIndex = i;
                break;
            }
            // If not in this bracket and there are more brackets, continue
            // If we reach the end, use the last bracket (highest tier)
            if (i == brackets.size() - 1) {
                bracketIndex = i;
            }
        }

        return IrmaaBracket.fromLevel(Math.min(bracketIndex, 5));
    }

    @Override
    public BigDecimal getPartAPremium(int year, int workQuarters) {
        if (workQuarters >= PREMIUM_FREE_QUARTERS) {
            return BigDecimal.ZERO;
        }

        return rules.getYearConfig(year)
            .map(config -> {
                if (workQuarters >= REDUCED_PREMIUM_QUARTERS) {
                    return config.getPartAPremiumReduced();
                }
                return config.getPartAPremiumFull();
            })
            .orElse(workQuarters >= REDUCED_PREMIUM_QUARTERS
                ? DEFAULT_PART_A_REDUCED
                : DEFAULT_PART_A_FULL);
    }

    /**
     * Finds the IRMAA bracket configuration for the given MAGI and filing status.
     *
     * @param magi the Modified Adjusted Gross Income
     * @param filingStatus the filing status
     * @param year the Medicare year
     * @return optional containing the matching bracket config
     */
    private Optional<IrmaaBracketConfig> findBracketConfig(
            BigDecimal magi,
            FilingStatus filingStatus,
            int year) {

        List<IrmaaBracketConfig> brackets = rules.getIrmaaBrackets(year);

        for (IrmaaBracketConfig bracket : brackets) {
            if (isInBracket(magi, filingStatus, bracket)) {
                return Optional.of(bracket);
            }
        }

        // If MAGI exceeds all brackets, return the highest bracket
        if (!brackets.isEmpty()) {
            return Optional.of(brackets.get(brackets.size() - 1));
        }

        return Optional.empty();
    }

    /**
     * Checks if the MAGI falls within the given bracket for the filing status.
     *
     * @param magi the Modified Adjusted Gross Income
     * @param filingStatus the filing status
     * @param bracket the bracket configuration
     * @return true if MAGI is within the bracket thresholds
     */
    private boolean isInBracket(
            BigDecimal magi,
            FilingStatus filingStatus,
            IrmaaBracketConfig bracket) {

        BigDecimal min;
        BigDecimal max;

        // Use joint thresholds for MFJ and QSS, single for others
        if (filingStatus.usesJointThresholds()) {
            min = bracket.getJointMin();
            max = bracket.getJointMax();
        } else {
            min = bracket.getSingleMin();
            max = bracket.getSingleMax();
        }

        // Check if MAGI is at or above minimum
        boolean aboveMin = magi.compareTo(min) >= 0;

        // Check if MAGI is at or below maximum (if max is set)
        boolean belowMax = max == null || magi.compareTo(max) <= 0;

        return aboveMin && belowMax;
    }
}
