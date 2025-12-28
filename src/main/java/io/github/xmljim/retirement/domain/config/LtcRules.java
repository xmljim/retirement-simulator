package io.github.xmljim.retirement.domain.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Configuration for Long-Term Care insurance rules and actuarial data.
 *
 * <p>Contains probability data for LTC needs by age, used in probabilistic
 * Monte Carlo simulations. Data derived from actuarial studies.
 *
 * @see <a href="https://www.aaltci.org">AALTCI</a>
 */
@Configuration
@ConfigurationProperties(prefix = "ltc")
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP",
    justification = "Spring configuration bean with mutable properties"
)
public class LtcRules {

    private List<AgeProbability> probabilities = new ArrayList<>();
    private BigDecimal defaultDailyBenefit = new BigDecimal("200");
    private int defaultBenefitPeriodYears = 3;
    private int defaultEliminationDays = 90;
    private BigDecimal defaultInflationRate = new BigDecimal("0.03");

    public List<AgeProbability> getProbabilities() {
        return probabilities;
    }

    public void setProbabilities(List<AgeProbability> probabilities) {
        this.probabilities = probabilities;
    }

    public BigDecimal getDefaultDailyBenefit() {
        return defaultDailyBenefit;
    }

    public void setDefaultDailyBenefit(BigDecimal defaultDailyBenefit) {
        this.defaultDailyBenefit = defaultDailyBenefit;
    }

    public int getDefaultBenefitPeriodYears() {
        return defaultBenefitPeriodYears;
    }

    public void setDefaultBenefitPeriodYears(int defaultBenefitPeriodYears) {
        this.defaultBenefitPeriodYears = defaultBenefitPeriodYears;
    }

    public int getDefaultEliminationDays() {
        return defaultEliminationDays;
    }

    public void setDefaultEliminationDays(int defaultEliminationDays) {
        this.defaultEliminationDays = defaultEliminationDays;
    }

    public BigDecimal getDefaultInflationRate() {
        return defaultInflationRate;
    }

    public void setDefaultInflationRate(BigDecimal defaultInflationRate) {
        this.defaultInflationRate = defaultInflationRate;
    }

    /**
     * Returns the annual LTC need probability for a given age.
     */
    public BigDecimal getProbabilityForAge(int age) {
        return probabilities.stream()
            .filter(p -> age >= p.getMinAge() && age <= p.getMaxAge())
            .findFirst()
            .map(AgeProbability::getAnnualProbability)
            .orElse(BigDecimal.ZERO);
    }

    /**
     * Age-based probability of needing LTC in a given year.
     */
    public static class AgeProbability {
        private int minAge;
        private int maxAge;
        private BigDecimal annualProbability;

        public int getMinAge() {
            return minAge;
        }

        public void setMinAge(int minAge) {
            this.minAge = minAge;
        }

        public int getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(int maxAge) {
            this.maxAge = maxAge;
        }

        public BigDecimal getAnnualProbability() {
            return annualProbability;
        }

        public void setAnnualProbability(BigDecimal annualProbability) {
            this.annualProbability = annualProbability;
        }
    }
}
