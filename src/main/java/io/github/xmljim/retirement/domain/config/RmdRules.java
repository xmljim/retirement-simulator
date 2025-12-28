package io.github.xmljim.retirement.domain.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * RMD (Required Minimum Distribution) rules from external YAML configuration.
 *
 * <p>This class provides configurable RMD rules including:
 * <ul>
 *   <li>RMD start age by birth year (SECURE 2.0 changes)</li>
 *   <li>IRS Uniform Lifetime Table factors</li>
 *   <li>Joint Life Table factors (for spouse 10+ years younger)</li>
 * </ul>
 *
 * <p>These values are sourced from IRS Publication 590-B and SECURE 2.0 Act:
 * <ul>
 *   <li>Life tables: <a href="https://www.irs.gov/publications/p590b">IRS Pub 590-B</a></li>
 *   <li>Start ages: SECURE Act 2019, SECURE 2.0 Act 2022</li>
 * </ul>
 *
 * <p>Configuration is loaded from {@code application.yml} under the {@code rmd} prefix.
 */
@ConfigurationProperties(prefix = "rmd")
@Validated
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP",
    justification = "Spring @ConfigurationProperties requires mutable access for binding"
)
public class RmdRules {

    private List<StartAgeEntry> startAgeByBirthYear = new ArrayList<>();
    private List<LifeTableEntry> uniformLifetimeTable = new ArrayList<>();
    private List<LifeTableEntry> jointLifeTable = new ArrayList<>();

    /**
     * RMD start age entry for a birth year range.
     *
     * <p>SECURE 2.0 changed RMD start ages:
     * <ul>
     *   <li>Born 1950 or earlier: Age 72</li>
     *   <li>Born 1951-1959: Age 73</li>
     *   <li>Born 1960 or later: Age 75</li>
     * </ul>
     */
    public static class StartAgeEntry {
        private Optional<Integer> birthYearMin = Optional.empty();
        private Optional<Integer> birthYearMax = Optional.empty();
        private int startAge;

        public Optional<Integer> getBirthYearMin() {
            return birthYearMin;
        }

        public void setBirthYearMin(Integer birthYearMin) {
            this.birthYearMin = Optional.ofNullable(birthYearMin);
        }

        public Optional<Integer> getBirthYearMax() {
            return birthYearMax;
        }

        public void setBirthYearMax(Integer birthYearMax) {
            this.birthYearMax = Optional.ofNullable(birthYearMax);
        }

        public int getStartAge() {
            return startAge;
        }

        public void setStartAge(int startAge) {
            this.startAge = startAge;
        }
    }

    /**
     * Life expectancy table entry for RMD factor lookup.
     *
     * <p>The distribution factor is divided into the prior year-end balance
     * to calculate the RMD amount.
     */
    public static class LifeTableEntry {
        private int age;
        private BigDecimal factor;

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public BigDecimal getFactor() {
            return factor;
        }

        public void setFactor(BigDecimal factor) {
            this.factor = factor;
        }
    }

    public List<StartAgeEntry> getStartAgeByBirthYear() {
        return startAgeByBirthYear;
    }

    public void setStartAgeByBirthYear(List<StartAgeEntry> startAgeByBirthYear) {
        this.startAgeByBirthYear = startAgeByBirthYear;
    }

    public List<LifeTableEntry> getUniformLifetimeTable() {
        return uniformLifetimeTable;
    }

    public void setUniformLifetimeTable(List<LifeTableEntry> uniformLifetimeTable) {
        this.uniformLifetimeTable = uniformLifetimeTable;
    }

    public List<LifeTableEntry> getJointLifeTable() {
        return jointLifeTable;
    }

    public void setJointLifeTable(List<LifeTableEntry> jointLifeTable) {
        this.jointLifeTable = jointLifeTable;
    }

    /**
     * Gets the RMD start age for a given birth year.
     *
     * @param birthYear the birth year
     * @return the RMD start age (72, 73, or 75 per SECURE 2.0)
     */
    public int getStartAge(int birthYear) {
        return startAgeByBirthYear.stream()
            .filter(entry -> isWithinRange(entry, birthYear))
            .findFirst()
            .map(StartAgeEntry::getStartAge)
            .orElse(75);  // Default to 75 for unknown birth years
    }

    /**
     * Gets the distribution factor from the Uniform Lifetime Table.
     *
     * @param age the account owner's age
     * @return the distribution factor, or 0 if age not found
     */
    public BigDecimal getUniformFactor(int age) {
        return uniformLifetimeTable.stream()
            .filter(entry -> entry.getAge() == age)
            .findFirst()
            .map(LifeTableEntry::getFactor)
            .orElse(BigDecimal.ZERO);
    }

    /**
     * Gets the distribution factor from the Joint Life Table.
     *
     * <p>Used when spouse is sole beneficiary and more than 10 years younger.
     *
     * @param age the account owner's age
     * @return the distribution factor, or 0 if age not found
     */
    public BigDecimal getJointFactor(int age) {
        return jointLifeTable.stream()
            .filter(entry -> entry.getAge() == age)
            .findFirst()
            .map(LifeTableEntry::getFactor)
            .orElse(BigDecimal.ZERO);
    }

    private boolean isWithinRange(StartAgeEntry entry, int birthYear) {
        boolean afterMin = entry.getBirthYearMin()
            .map(min -> birthYear >= min)
            .orElse(true);

        boolean beforeMax = entry.getBirthYearMax()
            .map(max -> birthYear <= max)
            .orElse(true);

        return afterMin && beforeMax;
    }
}
