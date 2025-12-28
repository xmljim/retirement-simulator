package io.github.xmljim.retirement.domain.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Medicare rules loaded from external YAML configuration.
 *
 * <p>This class provides configurable Medicare rules including:
 * <ul>
 *   <li>Part B base premium by year</li>
 *   <li>IRMAA brackets for Part B and Part D adjustments</li>
 *   <li>Part A and Part B deductibles</li>
 *   <li>Part A premium for those not premium-free</li>
 * </ul>
 *
 * <p>These values are sourced from CMS (Centers for Medicare and Medicaid Services):
 * <ul>
 *   <li>Part B premiums: <a href="https://www.medicare.gov/basics/costs/medicare-costs">Medicare Costs</a></li>
 *   <li>IRMAA brackets: <a href="https://www.cms.gov/newsroom">CMS Newsroom</a></li>
 * </ul>
 *
 * <p>Configuration is loaded from {@code application.yml} under the
 * {@code medicare} prefix.
 *
 * <p>Example configuration:
 * <pre>
 * medicare:
 *   years:
 *     - year: 2025
 *       part-b-base: 185.00
 *       part-a-deductible: 1676
 *       part-b-deductible: 257
 *       part-a-premium-full: 518
 *       part-a-premium-reduced: 285
 *       irmaa-brackets:
 *         - single-max: 106000
 *           joint-max: 212000
 *           part-b-premium: 185.00
 *           part-d-irmaa: 0
 * </pre>
 */
@ConfigurationProperties(prefix = "medicare")
@Validated
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP",
    justification = "Spring @ConfigurationProperties requires mutable access for binding"
)
public class MedicareRules {

    private List<YearConfig> years = new ArrayList<>();

    /**
     * Configuration for a specific Medicare year.
     */
    public static class YearConfig {
        private int year;
        private BigDecimal partBBase = new BigDecimal("185.00");
        private BigDecimal partADeductible = new BigDecimal("1676");
        private BigDecimal partBDeductible = new BigDecimal("257");
        private BigDecimal partAPremiumFull = new BigDecimal("518");
        private BigDecimal partAPremiumReduced = new BigDecimal("285");
        private List<IrmaaBracketConfig> irmaaBrackets = new ArrayList<>();

        public int getYear() {
            return year;
        }

        public void setYear(int year) {
            this.year = year;
        }

        public BigDecimal getPartBBase() {
            return partBBase;
        }

        public void setPartBBase(BigDecimal partBBase) {
            this.partBBase = partBBase;
        }

        public BigDecimal getPartADeductible() {
            return partADeductible;
        }

        public void setPartADeductible(BigDecimal partADeductible) {
            this.partADeductible = partADeductible;
        }

        public BigDecimal getPartBDeductible() {
            return partBDeductible;
        }

        public void setPartBDeductible(BigDecimal partBDeductible) {
            this.partBDeductible = partBDeductible;
        }

        public BigDecimal getPartAPremiumFull() {
            return partAPremiumFull;
        }

        public void setPartAPremiumFull(BigDecimal partAPremiumFull) {
            this.partAPremiumFull = partAPremiumFull;
        }

        public BigDecimal getPartAPremiumReduced() {
            return partAPremiumReduced;
        }

        public void setPartAPremiumReduced(BigDecimal partAPremiumReduced) {
            this.partAPremiumReduced = partAPremiumReduced;
        }

        public List<IrmaaBracketConfig> getIrmaaBrackets() {
            return irmaaBrackets;
        }

        public void setIrmaaBrackets(List<IrmaaBracketConfig> irmaaBrackets) {
            this.irmaaBrackets = irmaaBrackets;
        }
    }

    /**
     * IRMAA bracket configuration.
     *
     * <p>IRMAA (Income-Related Monthly Adjustment Amount) increases
     * Medicare premiums for higher-income beneficiaries based on MAGI
     * from 2 years prior.
     *
     * <p>Each bracket defines:
     * <ul>
     *   <li>Income thresholds for single and joint filers</li>
     *   <li>Part B monthly premium (base + IRMAA)</li>
     *   <li>Part D IRMAA surcharge</li>
     * </ul>
     */
    public static class IrmaaBracketConfig {
        private BigDecimal singleMin = BigDecimal.ZERO;
        private BigDecimal singleMax;
        private BigDecimal jointMin = BigDecimal.ZERO;
        private BigDecimal jointMax;
        private BigDecimal partBPremium;
        private BigDecimal partDIrmaa = BigDecimal.ZERO;

        public BigDecimal getSingleMin() {
            return singleMin;
        }

        public void setSingleMin(BigDecimal singleMin) {
            this.singleMin = singleMin;
        }

        public BigDecimal getSingleMax() {
            return singleMax;
        }

        public void setSingleMax(BigDecimal singleMax) {
            this.singleMax = singleMax;
        }

        public BigDecimal getJointMin() {
            return jointMin;
        }

        public void setJointMin(BigDecimal jointMin) {
            this.jointMin = jointMin;
        }

        public BigDecimal getJointMax() {
            return jointMax;
        }

        public void setJointMax(BigDecimal jointMax) {
            this.jointMax = jointMax;
        }

        public BigDecimal getPartBPremium() {
            return partBPremium;
        }

        public void setPartBPremium(BigDecimal partBPremium) {
            this.partBPremium = partBPremium;
        }

        public BigDecimal getPartDIrmaa() {
            return partDIrmaa;
        }

        public void setPartDIrmaa(BigDecimal partDIrmaa) {
            this.partDIrmaa = partDIrmaa;
        }

        /**
         * Returns the Part B IRMAA amount (premium minus base).
         *
         * @param partBBase the base Part B premium
         * @return the IRMAA surcharge amount
         */
        public BigDecimal getPartBIrmaa(BigDecimal partBBase) {
            return partBPremium.subtract(partBBase);
        }
    }

    public List<YearConfig> getYears() {
        return years;
    }

    public void setYears(List<YearConfig> years) {
        this.years = years;
    }

    /**
     * Gets the configuration for a specific year.
     *
     * @param year the year to look up
     * @return optional containing the year config, or empty if not found
     */
    public Optional<YearConfig> getYearConfig(int year) {
        return years.stream()
            .filter(y -> y.getYear() == year)
            .findFirst();
    }

    /**
     * Gets the Part B base premium for a year.
     *
     * @param year the year
     * @return the base premium, or default of $185 if year not found
     */
    public BigDecimal getPartBBase(int year) {
        return getYearConfig(year)
            .map(YearConfig::getPartBBase)
            .orElse(new BigDecimal("185.00"));
    }

    /**
     * Gets the IRMAA brackets for a specific year.
     *
     * @param year the year
     * @return the brackets, or empty list if year not found
     */
    public List<IrmaaBracketConfig> getIrmaaBrackets(int year) {
        return getYearConfig(year)
            .map(YearConfig::getIrmaaBrackets)
            .orElse(new ArrayList<>());
    }
}
