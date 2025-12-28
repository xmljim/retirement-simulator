package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.xmljim.retirement.domain.calculator.RmdCalculator;
import io.github.xmljim.retirement.domain.config.RmdRules;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.value.RmdProjection;

/**
 * Default implementation of RMD calculator.
 *
 * <p>Calculates Required Minimum Distributions using IRS Uniform Lifetime
 * Table and SECURE 2.0 start age rules.
 *
 * <p>Formula: Prior year-end balance / Distribution factor = RMD
 *
 * @see RmdCalculator
 * @see RmdRules
 */
@Service
public class DefaultRmdCalculator implements RmdCalculator {

    private static final int SCALE = 2;
    private static final int DEFAULT_START_AGE = 75;

    private final RmdRules rules;

    /**
     * Creates a calculator with Spring-managed rules.
     *
     * @param rules the RMD rules configuration
     */
    @Autowired
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-managed beans")
    public DefaultRmdCalculator(RmdRules rules) {
        this.rules = rules;
    }

    /**
     * Creates a calculator with default rules.
     */
    public DefaultRmdCalculator() {
        this.rules = new RmdRules();
    }

    @Override
    public RmdProjection calculate(
            BigDecimal priorYearEndBalance,
            int age,
            int birthYear,
            int year) {

        int startAge = getRmdStartAge(birthYear);

        // Not yet subject to RMDs
        if (age < startAge) {
            return RmdProjection.notRequired(year, priorYearEndBalance, age);
        }

        BigDecimal factor = getDistributionFactor(age);
        BigDecimal rmdAmount = calculateRmd(priorYearEndBalance, age);

        // First RMD year has special April 1 deadline
        boolean isFirstRmd = age == startAge;

        if (isFirstRmd) {
            return RmdProjection.firstYear(year, priorYearEndBalance, rmdAmount, factor, age);
        }

        return RmdProjection.standard(year, priorYearEndBalance, rmdAmount, factor, age);
    }

    @Override
    public BigDecimal calculateRmd(BigDecimal priorYearEndBalance, int age) {
        BigDecimal factor = getDistributionFactor(age);

        if (factor.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return priorYearEndBalance.divide(factor, SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal getDistributionFactor(int age) {
        BigDecimal factor = rules.getUniformFactor(age);

        // If no factor found in table, use a default minimum for very old ages
        if (factor.compareTo(BigDecimal.ZERO) == 0 && age > 120) {
            return new BigDecimal("2.0");
        }

        return factor;
    }

    @Override
    public int getRmdStartAge(int birthYear) {
        int startAge = rules.getStartAge(birthYear);
        return startAge > 0 ? startAge : DEFAULT_START_AGE;
    }

    @Override
    public boolean isSubjectToRmd(AccountType accountType) {
        return accountType.isSubjectToRmd();
    }
}
