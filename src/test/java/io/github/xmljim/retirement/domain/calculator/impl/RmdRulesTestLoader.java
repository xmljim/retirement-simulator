package io.github.xmljim.retirement.domain.calculator.impl;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import io.github.xmljim.retirement.domain.config.RmdRules;

/**
 * Utility to load RmdRules from application-rmd.yml for testing.
 *
 * <p>This ensures tests use the same configuration as production,
 * preventing technical debt when IRS rules change.
 */
public final class RmdRulesTestLoader {

    private RmdRulesTestLoader() {
        // Utility class
    }

    /**
     * Loads RmdRules from application-rmd.yml.
     *
     * @return configured RmdRules instance
     */
    @SuppressWarnings("unchecked")
    public static RmdRules loadFromYaml() {
        Yaml yaml = new Yaml();
        InputStream inputStream = RmdRulesTestLoader.class.getClassLoader()
                .getResourceAsStream("application-rmd.yml");

        if (inputStream == null) {
            throw new IllegalStateException("application-rmd.yml not found on classpath");
        }

        Map<String, Object> root = yaml.load(inputStream);
        Map<String, Object> rmdConfig = (Map<String, Object>) root.get("rmd");

        RmdRules rules = new RmdRules();

        // Load start ages
        List<Map<String, Object>> startAges =
                (List<Map<String, Object>>) rmdConfig.get("start-age-by-birth-year");
        for (Map<String, Object> entry : startAges) {
            RmdRules.StartAgeEntry startAgeEntry = new RmdRules.StartAgeEntry();
            if (entry.containsKey("birth-year-min")) {
                startAgeEntry.setBirthYearMin((Integer) entry.get("birth-year-min"));
            }
            if (entry.containsKey("birth-year-max")) {
                startAgeEntry.setBirthYearMax((Integer) entry.get("birth-year-max"));
            }
            startAgeEntry.setStartAge((Integer) entry.get("start-age"));
            rules.getStartAgeByBirthYear().add(startAgeEntry);
        }

        // Load uniform lifetime table
        List<Map<String, Object>> uniformTable =
                (List<Map<String, Object>>) rmdConfig.get("uniform-lifetime-table");
        for (Map<String, Object> entry : uniformTable) {
            RmdRules.LifeTableEntry lifeEntry = new RmdRules.LifeTableEntry();
            lifeEntry.setAge((Integer) entry.get("age"));
            Object factor = entry.get("factor");
            if (factor instanceof Double) {
                lifeEntry.setFactor(BigDecimal.valueOf((Double) factor));
            } else if (factor instanceof Integer) {
                lifeEntry.setFactor(BigDecimal.valueOf((Integer) factor));
            }
            rules.getUniformLifetimeTable().add(lifeEntry);
        }

        return rules;
    }
}
