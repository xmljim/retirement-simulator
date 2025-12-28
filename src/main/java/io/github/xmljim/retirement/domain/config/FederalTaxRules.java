package io.github.xmljim.retirement.domain.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.xmljim.retirement.domain.enums.FilingStatus;

/**
 * Federal income tax rules loaded from external YAML configuration.
 *
 * <p>This class provides configurable federal tax rules including:
 * <ul>
 *   <li>Tax brackets by filing status</li>
 *   <li>Standard deductions by filing status</li>
 *   <li>Additional deduction for age 65+</li>
 *   <li>Chained CPI rate for bracket indexing</li>
 * </ul>
 *
 * <p>Tax brackets and deductions are indexed annually using chained CPI-U,
 * which grows approximately 0.25% slower than traditional CPI.
 *
 * <p>Configuration is loaded from {@code application.yml} under the
 * {@code federal-tax} prefix.
 *
 * @see <a href="https://www.irs.gov/publications/p17">IRS Publication 17</a>
 */
@ConfigurationProperties(prefix = "federal-tax")
@Validated
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP",
    justification = "Spring @ConfigurationProperties requires mutable access for binding"
)
public class FederalTaxRules {

    private BigDecimal chainedCpiRate = new BigDecimal("0.025");
    private int baseYear = 2024;
    private Map<FilingStatus, BigDecimal> standardDeductions = new EnumMap<>(FilingStatus.class);
    private Map<FilingStatus, BigDecimal> age65Additional = new EnumMap<>(FilingStatus.class);
    private Map<FilingStatus, List<BracketConfig>> brackets = new EnumMap<>(FilingStatus.class);

    /**
     * Tax bracket configuration entry.
     */
    public static class BracketConfig {
        private BigDecimal rate;
        private Optional<BigDecimal> upperBound = Optional.empty();

        public BigDecimal getRate() {
            return rate;
        }

        public void setRate(BigDecimal rate) {
            this.rate = rate;
        }

        public Optional<BigDecimal> getUpperBound() {
            return upperBound;
        }

        public void setUpperBound(BigDecimal upperBound) {
            this.upperBound = Optional.ofNullable(upperBound);
        }
    }

    public BigDecimal getChainedCpiRate() {
        return chainedCpiRate;
    }

    public void setChainedCpiRate(BigDecimal chainedCpiRate) {
        this.chainedCpiRate = chainedCpiRate;
    }

    public int getBaseYear() {
        return baseYear;
    }

    public void setBaseYear(int baseYear) {
        this.baseYear = baseYear;
    }

    public Map<FilingStatus, BigDecimal> getStandardDeductions() {
        return standardDeductions;
    }

    public void setStandardDeductions(Map<FilingStatus, BigDecimal> standardDeductions) {
        this.standardDeductions = standardDeductions;
    }

    public Map<FilingStatus, BigDecimal> getAge65Additional() {
        return age65Additional;
    }

    public void setAge65Additional(Map<FilingStatus, BigDecimal> age65Additional) {
        this.age65Additional = age65Additional;
    }

    public Map<FilingStatus, List<BracketConfig>> getBrackets() {
        return brackets;
    }

    public void setBrackets(Map<FilingStatus, List<BracketConfig>> brackets) {
        this.brackets = brackets;
    }

    /**
     * Gets the standard deduction for a filing status in the base year.
     *
     * @param filingStatus the filing status
     * @return the standard deduction amount
     */
    public BigDecimal getStandardDeduction(FilingStatus filingStatus) {
        return standardDeductions.getOrDefault(filingStatus, BigDecimal.ZERO);
    }

    /**
     * Gets the additional deduction for age 65+ for a filing status.
     *
     * @param filingStatus the filing status
     * @return the additional deduction amount per qualifying person
     */
    public BigDecimal getAge65AdditionalDeduction(FilingStatus filingStatus) {
        return age65Additional.getOrDefault(filingStatus, BigDecimal.ZERO);
    }

    /**
     * Gets the tax brackets for a filing status in the base year.
     *
     * @param filingStatus the filing status
     * @return the list of bracket configurations
     */
    public List<BracketConfig> getBrackets(FilingStatus filingStatus) {
        return brackets.getOrDefault(filingStatus, new ArrayList<>());
    }
}
