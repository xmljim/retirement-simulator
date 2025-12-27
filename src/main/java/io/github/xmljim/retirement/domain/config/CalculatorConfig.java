package io.github.xmljim.retirement.domain.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.xmljim.retirement.domain.calculator.ContributionCalculator;
import io.github.xmljim.retirement.domain.calculator.IncomeCalculator;
import io.github.xmljim.retirement.domain.calculator.InflationCalculator;
import io.github.xmljim.retirement.domain.calculator.IrsContributionRules;
import io.github.xmljim.retirement.domain.calculator.ReturnCalculator;
import io.github.xmljim.retirement.domain.calculator.SocialSecurityCalculator;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultContributionCalculator;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultIncomeCalculator;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultInflationCalculator;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultReturnCalculator;
import io.github.xmljim.retirement.domain.calculator.impl.DefaultSocialSecurityCalculator;
import io.github.xmljim.retirement.domain.calculator.impl.Secure2ContributionRules;

/**
 * Spring configuration for calculator beans.
 *
 * <p>This configuration provides singleton calculator instances for use
 * throughout the application. All calculators are stateless and thread-safe.
 *
 * <p>Calculator beans available:
 * <ul>
 *   <li>{@link IrsContributionRules} - SECURE 2.0 contribution rules</li>
 *   <li>{@link ContributionCalculator} - Basic contribution rate calculations</li>
 *   <li>{@link InflationCalculator} - Inflation and COLA calculations</li>
 *   <li>{@link IncomeCalculator} - Income projections</li>
 *   <li>{@link ReturnCalculator} - Investment return calculations</li>
 *   <li>{@link SocialSecurityCalculator} - Social Security benefit calculations</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties({IrsContributionLimits.class, SocialSecurityRules.class})
public class CalculatorConfig {

    /**
     * Creates the IRS contribution rules bean (SECURE 2.0 implementation).
     *
     * @param limits the IRS contribution limits from configuration
     * @return the contribution rules calculator
     */
    @Bean
    public IrsContributionRules irsContributionRules(IrsContributionLimits limits) {
        return new Secure2ContributionRules(limits);
    }

    /**
     * Creates the contribution calculator bean.
     *
     * @return the contribution calculator
     */
    @Bean
    public ContributionCalculator contributionCalculator() {
        return new DefaultContributionCalculator();
    }

    /**
     * Creates the inflation calculator bean.
     *
     * @return the inflation calculator
     */
    @Bean
    public InflationCalculator inflationCalculator() {
        return new DefaultInflationCalculator();
    }

    /**
     * Creates the income calculator bean.
     *
     * @param inflationCalculator the inflation calculator dependency
     * @return the income calculator
     */
    @Bean
    public IncomeCalculator incomeCalculator(InflationCalculator inflationCalculator) {
        return new DefaultIncomeCalculator(inflationCalculator);
    }

    /**
     * Creates the return calculator bean.
     *
     * @return the return calculator
     */
    @Bean
    public ReturnCalculator returnCalculator() {
        return new DefaultReturnCalculator();
    }

    /**
     * Creates the Social Security calculator bean.
     *
     * @param rules the Social Security rules from configuration
     * @return the Social Security calculator
     */
    @Bean
    public SocialSecurityCalculator socialSecurityCalculator(SocialSecurityRules rules) {
        return new DefaultSocialSecurityCalculator(rules);
    }
}
