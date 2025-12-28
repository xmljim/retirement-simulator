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
    private List<PremiumEstimate> premiumEstimates = new ArrayList<>();
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

    public List<PremiumEstimate> getPremiumEstimates() {
        return premiumEstimates;
    }

    public void setPremiumEstimates(List<PremiumEstimate> premiumEstimates) {
        this.premiumEstimates = premiumEstimates;
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
     *
     * @param age the person's age
     * @return the annual probability of needing LTC
     */
    public BigDecimal getProbabilityForAge(int age) {
        return probabilities.stream()
            .filter(p -> age >= p.getMinAge() && age <= p.getMaxAge())
            .findFirst()
            .map(AgeProbability::getAnnualProbability)
            .orElse(BigDecimal.ZERO);
    }

    /**
     * Returns the estimated annual premium for a given age and gender.
     *
     * @param age the person's age at policy purchase
     * @param isFemale true if female (higher premiums due to longevity)
     * @return the estimated annual premium (midpoint of range)
     */
    public BigDecimal getEstimatedPremium(int age, boolean isFemale) {
        return premiumEstimates.stream()
            .filter(p -> age >= p.getMinAge() && age <= p.getMaxAge())
            .findFirst()
            .map(p -> isFemale ? p.getFemaleMidpoint() : p.getMaleMidpoint())
            .orElse(BigDecimal.ZERO);
    }

    /**
     * Returns the estimated annual premium for a couple.
     *
     * @param age the older spouse's age at policy purchase
     * @return the estimated couple annual premium (midpoint of range)
     */
    public BigDecimal getEstimatedCouplePremium(int age) {
        return premiumEstimates.stream()
            .filter(p -> age >= p.getMinAge() && age <= p.getMaxAge())
            .findFirst()
            .map(PremiumEstimate::getCoupleMidpoint)
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

    /**
     * Age-based premium estimates by gender and couple status.
     * Source: AALTCI 2023 LTC Insurance Price Index.
     */
    public static class PremiumEstimate {
        private int minAge;
        private int maxAge;
        private BigDecimal maleLow;
        private BigDecimal maleHigh;
        private BigDecimal femaleLow;
        private BigDecimal femaleHigh;
        private BigDecimal coupleLow;
        private BigDecimal coupleHigh;

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

        public BigDecimal getMaleLow() {
            return maleLow;
        }

        public void setMaleLow(BigDecimal maleLow) {
            this.maleLow = maleLow;
        }

        public BigDecimal getMaleHigh() {
            return maleHigh;
        }

        public void setMaleHigh(BigDecimal maleHigh) {
            this.maleHigh = maleHigh;
        }

        public BigDecimal getFemaleLow() {
            return femaleLow;
        }

        public void setFemaleLow(BigDecimal femaleLow) {
            this.femaleLow = femaleLow;
        }

        public BigDecimal getFemaleHigh() {
            return femaleHigh;
        }

        public void setFemaleHigh(BigDecimal femaleHigh) {
            this.femaleHigh = femaleHigh;
        }

        public BigDecimal getCoupleLow() {
            return coupleLow;
        }

        public void setCoupleLow(BigDecimal coupleLow) {
            this.coupleLow = coupleLow;
        }

        public BigDecimal getCoupleHigh() {
            return coupleHigh;
        }

        public void setCoupleHigh(BigDecimal coupleHigh) {
            this.coupleHigh = coupleHigh;
        }

        /** Returns the midpoint of male premium range. */
        public BigDecimal getMaleMidpoint() {
            return maleLow.add(maleHigh).divide(BigDecimal.valueOf(2), 0, java.math.RoundingMode.HALF_UP);
        }

        /** Returns the midpoint of female premium range. */
        public BigDecimal getFemaleMidpoint() {
            return femaleLow.add(femaleHigh).divide(BigDecimal.valueOf(2), 0, java.math.RoundingMode.HALF_UP);
        }

        /** Returns the midpoint of couple premium range. */
        public BigDecimal getCoupleMidpoint() {
            return coupleLow.add(coupleHigh).divide(BigDecimal.valueOf(2), 0, java.math.RoundingMode.HALF_UP);
        }
    }
}
